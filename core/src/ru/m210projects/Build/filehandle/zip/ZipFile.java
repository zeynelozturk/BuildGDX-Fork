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

package ru.m210projects.Build.filehandle.zip;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipInputStream;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;
import static ru.m210projects.Build.filehandle.fs.FileEntry.DUMMY_PATH;

public class ZipFile implements Group {

    private final Map<String, Entry> entries;
    private final Map<String, ZipFile> directories;
    private final String name;

    private ZipFile(String name) {
        this.name = name;
        this.entries = new LinkedHashMap<>();
        this.directories = new LinkedHashMap<>();
    }

    public ZipFile(String name, InputStreamProvider provider) throws IOException {
        this.name = name;

        try (FastZipInputStream zis = new FastZipInputStream(new BufferedInputStream(provider.newInputStream()))) {
            this.entries = new LinkedHashMap<>();
            this.directories = new LinkedHashMap<>();

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path path = FileUtils.getPath(entry.getName());
                if (path.equals(DUMMY_PATH)) {
                    continue;
                }

                Path file = path.getFileName();
                Entry zipEntry = newEntry(provider, file.toString(), entry, zis);

                if (path.getParent() == null) {
                    entries.put(file.toString().toUpperCase(), zipEntry);
                    zipEntry.setParent(this);
                } else {
                    ZipFile dir = this;
                    for (Path p : path) {
                        if (p.equals(file)) {
                            break;
                        }

                        String dirName = p.toString();
                        Map<String, ZipFile> directories = dir.directories;
                        String key = dirName.toUpperCase();

                        dir = directories.getOrDefault(key, new ZipFile(dirName));
                        directories.putIfAbsent(key, dir);
                    }

                    dir.entries.put(zipEntry.getName().toUpperCase(), zipEntry);
                    zipEntry.setParent(dir);
                }

                zis.skipEntry();
            }
        }
    }

    protected Entry newEntry(InputStreamProvider provider, String name, java.util.zip.ZipEntry entry, ZipInputStream zis) throws IOException {
//        byte[] data = StreamUtils.readBytes(zis, (int) entry.getSize());
//        return new ZipEntry(provider, name, entry) {
//            @Override
//            public InputStream getInputStream() {
//                return new ByteArrayInputStream(data);
//            }
//        };

        return new ZipEntry(provider, name, entry);
    }

    @Override
    public synchronized int getSize() {
        return entries.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Entry getEntry(Path path) {
        ZipFile dir = this;
        Entry result = DUMMY_ENTRY;
        for (Path p : path) {
            Entry entry = dir.entries.getOrDefault(p.toString().toUpperCase(), DUMMY_ENTRY);
            if (!entry.exists()) {
                return DUMMY_ENTRY;
            }

            if (entry.isDirectory()) {
                dir = dir.directories.getOrDefault(entry.getName().toUpperCase(), this);
            }
            result = entry;
        }

        return result;
    }

    @NotNull
    @Override
    public Entry getEntry(String name) {
        return getEntry(FileUtils.getPath(name));
    }

    private void fillList(ZipFile dir, List<Entry> list) {
        list.addAll(dir.entries.values());
        for (ZipFile zip : dir.directories.values()) {
            fillList(zip, list);
        }
    }

    @Override
    public synchronized List<Entry> getEntries() {
        List<Entry> list = new ArrayList<>();
        fillList(this, list);
        return list;
    }
}
