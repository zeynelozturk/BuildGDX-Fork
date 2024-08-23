// This file is part of BuildGDX.
// Copyright (C) 2023-2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// BuildGDX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// BuildGDX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.filehandle.rff;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class RffFile implements Group {

    private static final String RFF_HEADER = "RFF\u001A";
    protected final List<Entry> entryList;
    protected final String name;
    protected final Map<String, Map<String, Integer>> names;
    protected final Map<String, Map<Integer, Integer>> ids;

    // cached rff file
    public RffFile(String name) {
        this.name = name;
        this.entryList = new ArrayList<>();
        this.names = new HashMap<>();
        this.ids = new HashMap<>();
    }

    public RffFile(String name, InputStreamProvider provider) throws IOException {
        this.name = name;
        int revision;
        long offFat;
        int numFiles;
        try (InputStream is = provider.newInputStream()) {
            String header = StreamUtils.readString(is, 4);
            if (header.compareTo(RFF_HEADER) != 0) {
                throw new RuntimeException("RFF header corrupted");
            }
            revision = StreamUtils.readInt(is);
            offFat = StreamUtils.readInt(is);
            numFiles = StreamUtils.readInt(is);

            this.entryList = new ArrayList<>(numFiles);
            this.names = new HashMap<>();
            this.ids = new HashMap<>();
        }

        if (numFiles != 0) {
            int key = getCryptoKey(revision, offFat);
            try (InputStream is = RffInputStream.getInputStream(provider, key != -1, offFat, key, numFiles * 48)) {
                for (int i = 0; i < numFiles; i++) {
                    long skipped = is.skip(16);
                    if (skipped != 16) {
                        throw new EOFException();
                    }

                    int offset = StreamUtils.readInt(is);
                    int size = StreamUtils.readInt(is);
                    int packedSize = StreamUtils.readInt(is);
                    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(StreamUtils.readInt(is) * 1000L), ZoneId.of("GMT"));
                    int flags = StreamUtils.readByte(is);
                    String fmt = StreamUtils.readString(is, 3);
                    String filaName = StreamUtils.readString(is, 8);
                    int id = StreamUtils.readInt(is);
                    RffEntry entry = new RffEntry(provider, id, offset, size, packedSize, date, flags, filaName, fmt);

                    addEntry(entry);
                }
            } catch (IOException e) {
                throw new RuntimeException("RFF dictionary corrupted");
            }
        }
    }

    @Override
    public synchronized int getSize() {
        return entryList.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Entry getEntry(String name) {
        if (name.contains(".")) {
            String[] split = name.split("\\.");
            return getEntry(name, split[1]);
        }
        return getEntry(name, "");
    }


    @Override
    public synchronized List<Entry> getEntries() {
        return new ArrayList<>(entryList);
    }

    public synchronized Entry getEntry(String name, String fmt) {
        Map<String, Integer> entryMap = names.get(fmt.toUpperCase());
        if (entryMap != null) {
            int entryIndex = entryMap.getOrDefault(name.toUpperCase(), -1);
            if (entryIndex != -1) {
                return entryList.get(entryIndex);
            }
        }
        return DUMMY_ENTRY;
    }

    public synchronized Entry getEntry(int id, String fmt) {
        Map<Integer, Integer> entryMap = ids.get(fmt.toUpperCase());
        if (entryMap != null) {
            Integer entryIndex = entryMap.getOrDefault(id, -1);
            if (entryIndex != -1) {
                return entryList.get(entryIndex);
            }
        }
        return DUMMY_ENTRY;
    }

    public void addEntry(RffEntry entry) {
        String fmt = entry.getExtension();
        entry.parent = this;

        entryList.add(entry);
        int entryIndex = entryList.size() - 1;
        if (entry.isIDUsed()) {
            Map<Integer, Integer> entryMap = ids.computeIfAbsent(fmt, e -> new HashMap<>());
            entryMap.put(entry.getId(), entryIndex);
        }

        Map<String, Integer> entryMap = names.computeIfAbsent(fmt, e -> new HashMap<>());
        entryMap.put(entry.getName(), entryIndex);
    }

    private int getCryptoKey(int revision, long offset) {
        // v1.00 - 768
        // v1.01 - 769
        // v1.21 - 769
        // share v0.99 - 66048
        // share v1.11 - 769
        // alpha - 378470704

        // It seems barfc can generate corrupt header revisions, like 0x580301.
        // I had to disable a check of highest bytes to fix normal reading

        int key;
        if ((revision & 0xFFFF) == 0x0300) {
            key = (int) offset;
        } else if ((revision & 0xFFFF) == 0x0301) {
            key = (int) (offset + offset * (revision & 0xFF));
        } else if (revision == 0x168f0130) {
            throw new RuntimeException("RFF alpha version is not supported!");
        } else {
            throw new RuntimeException(String.format("Unknown RFF version: 0x%x", revision));
        }

        return key;
    }

    public boolean save(Path savePath, int revision) {
        try (OutputStream os = Files.newOutputStream(savePath)) {
            List<Entry> files = entryList;
            int offFat = 32; // header offset
            int numFiles = getSize();
            for (Entry entry : files) {
                offFat += (int) entry.getSize();
            }

            os.write(RFF_HEADER.getBytes(StandardCharsets.UTF_8));
            os.write(0x1A);
            StreamUtils.writeInt(os, revision);
            StreamUtils.writeInt(os, offFat);
            StreamUtils.writeInt(os, numFiles);

            byte[] dictionary = new byte[48 * numFiles];
            ByteBuffer dictionaryBuffer = ByteBuffer.wrap(dictionary).order(ByteOrder.LITTLE_ENDIAN);
            byte[] tmpBuf = new byte[8192];
            os.write(tmpBuf, 0, 16);

            int entryOffset = 32;
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(0);
            for (Entry entry : files) {
                RffEntry rffEntry = (RffEntry) entry;
                Arrays.fill(tmpBuf, 0, 16, (byte) 0);
                dictionaryBuffer.put(tmpBuf, 0, 16);
                dictionaryBuffer.putInt(entryOffset);
                dictionaryBuffer.putInt((int) entry.getSize());
                dictionaryBuffer.putInt(rffEntry.getPackedSize());
                dictionaryBuffer.putInt((int) rffEntry.getDate().toEpochSecond(zoneOffset));
                dictionaryBuffer.put((byte) rffEntry.getFlags());
                dictionaryBuffer.put(entry.getExtension().getBytes(StandardCharsets.UTF_8));
                byte[] name = entry.getName().getBytes(StandardCharsets.UTF_8);
                System.arraycopy(name, 0, tmpBuf, 0, name.length);
                dictionaryBuffer.put(tmpBuf, 0, 8);
                dictionaryBuffer.putInt(rffEntry.getId());
                entryOffset += (int) entry.getSize();

                try (InputStream is = entry.getInputStream()) {
                    boolean encrypted = rffEntry.isEncrypted();
                    while (is.available() != 0) {
                        int len = is.read(tmpBuf);
                        if (encrypted) {
                            for (int i = 0; i < 256; i++) {
                                tmpBuf[i] ^= (byte) (i >> 1);
                            }
                            encrypted = false;
                        }
                        os.write(tmpBuf, 0, len);
                    }
                }
            }

            int key = getCryptoKey(revision, offFat);
            if (key != -1) {
                for (int i = 0; i < dictionary.length; i++) {
                    dictionary[i] ^= (byte) (key++ >> 1);
                }
            }

            os.write(dictionary);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
