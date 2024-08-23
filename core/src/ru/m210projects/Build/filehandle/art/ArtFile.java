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

package ru.m210projects.Build.filehandle.art;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class ArtFile implements Group {

    public static final DynamicArtEntry DUMMY_ART_FILE = new DynamicArtEntry(null, MAXTILES, new byte[0], 0, 0, 0) {
        @Override
        public boolean exists() {
            return false;
        }
    };

    protected static final String HEADER = "BUILDART";
    protected final int tileStart;
    protected final String name;
    protected final List<ArtEntry> entries;

    public ArtFile(String name, InputStreamProvider provider) {
        this.name = name;
        List<ArtEntry> entries = null;
        int tileStart = -1;

        try (InputStream is = new BufferedInputStream(provider.newInputStream())) {
            int version = checkVersion(is);
            if (version == -1) {
                throw new RuntimeException("Unsupported ART file");
            }

            StreamUtils.readInt(is); //numtiles
            tileStart = StreamUtils.readInt(is);
            int tileEnd = StreamUtils.readInt(is);
            int numTiles = tileEnd - tileStart + 1;
            entries = new ArrayList<>(numTiles);

            int[] sizx = new int[numTiles];
            int[] sizy = new int[numTiles];
            int[] flags = new int[numTiles];

            for (int i = 0; i < numTiles; i++) {
                sizx[i] = StreamUtils.readShort(is);
            }

            for (int i = 0; i < numTiles; i++) {
                sizy[i] = StreamUtils.readShort(is);
            }

            for (int i = 0; i < numTiles; i++) {
                flags[i] = StreamUtils.readInt(is);
            }

            int num = tileStart;
            int offset = 4 + 4 + 4 + 4 + ((tileEnd - tileStart + 1) << 3);
            for (int i = 0; i < numTiles; i++) {
                int width = sizx[i];
                int height = sizy[i];
                ArtEntry entry = createArtEntry(provider, num++, offset, width, height, flags[i]);
                entries.add(entry);
                offset += (int) entry.getSize();
            }
        } catch(Exception e) {
            Console.out.println("Can't load ART file: " + name + ", " + e, OsdColor.RED);
            if(entries == null) {
                entries = Collections.unmodifiableList(new ArrayList<>(0));
            }
        }

        this.entries = entries;
        this.tileStart = tileStart;
    }

    protected ArtEntry createArtEntry(InputStreamProvider provider, int num, int offset, int width, int height, int flags) {
        return new ArtEntry(provider, num, offset, width, height, flags);
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Entry getEntry(String fileName) {
        return DUMMY_ENTRY;
    }

    @Override
    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public Entry getEntry(int tileNum) {
        int index = tileNum - tileStart;
        if(index < 0 || index >= entries.size()) {
            return DUMMY_ENTRY;
        }
        return entries.get(index);
    }

    private int checkVersion(InputStream is) throws IOException {
        int b = StreamUtils.readByte(is);
        if (b == 1) {
            if ((StreamUtils.readByte(is) | StreamUtils.readByte(is) | StreamUtils.readByte(is)) == 0) {
                return 1;
            }
        } else if (b == HEADER.charAt(0)) {
            int c = 1;
            while (c < HEADER.length()) {
                if (HEADER.charAt(c) != StreamUtils.readByte(is)) {
                    break;
                }
                c++;
            }

            if (c == HEADER.length()) {
                return StreamUtils.readInt(is);
            }
        }
        return -1;
    }
}

