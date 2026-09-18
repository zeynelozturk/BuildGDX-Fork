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

package ru.m210projects.Build.filehandle.fs;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static ru.m210projects.Build.filehandle.fs.FileEntry.DUMMY_PATH;

public class Directory implements Group {

    public static final FileEntry DUMMY_ENTRY = new NotFoundEntry("dummy");

    public static final Directory DUMMY_DIRECTORY = new Directory() {
        @Override
        public List<Entry> getEntries() {
            return new ArrayList<>();
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public @NotNull FileEntry getEntry(String name) {
            return DUMMY_ENTRY;
        }
    };

    final Map<String, FileEntry> entries = new HashMap<>();
    private final Map<String, Directory> directories = new HashMap<>();
    private final Path path;
    FileEntry directoryEntry = DUMMY_ENTRY;

    protected Directory() {
        this.path = DUMMY_PATH;
    }

    public Directory(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            this.path = dir;
            stream.forEach(this::addEntry);
        }
    }

    public Directory(Path dir, boolean createIfNotExists) throws IOException {
        if (createIfNotExists && !Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            this.path = dir;
            stream.forEach(this::addEntry);
        }
    }

    /**
     * Directory files quantity
     */
    @Override
    public synchronized int getSize() {
        return entries.size();
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    public Path getPath() {
        return path;
    }

    @Override
    public synchronized List<Entry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public FileEntry getDirectoryEntry() {
        return directoryEntry;
    }

    @NotNull
    public FileEntry getEntry(String name) {
        Objects.requireNonNull(name, "name");
        FileEntry entry;
        synchronized (this) {
            entry = entries.getOrDefault(name.toUpperCase(Locale.ROOT), DUMMY_ENTRY);
            if (entry.isDirectory()) {
                addDirectory(entry);
            }
        }
        return entry;
    }

    @Override
    public synchronized FileEntry getEntry(Path relPath) {
        Directory dir = this;
        FileEntry result = DUMMY_ENTRY;
        for (Path p : relPath) {
            FileEntry entry = dir.getEntry(p.toString());
            if (!entry.exists()) {
                return DUMMY_ENTRY;
            }

            if (entry.isDirectory()) {
                dir = dir.addDirectory(entry);
            }
            result = entry;
        }
        return result;
    }
    @NotNull
    public Directory getDirectory(FileEntry dirEntry) {
        Directory directory = DUMMY_DIRECTORY;
        if (dirEntry != null && dirEntry.isDirectory()) {
            if (this.path.equals(dirEntry.getPath())) {
                return this;
            }

            try {
                Path relPath = this.path.relativize(dirEntry.getPath());
                Directory dir = this;
                for (Path p : relPath) {
                    Map<String, Directory> directories = dir.directories;
                    String key = p.toString().toUpperCase(Locale.ROOT);

                    // if key is not exists and directory contains this entry, add entry to map
                    if (!directories.containsKey(key) && dir.entries.containsValue(dirEntry)) {
                        Directory subDir = new Directory(dirEntry.getPath());
                        subDir.directoryEntry = dirEntry;
                        directories.put(key, subDir);
                    }
                    dir = directories.getOrDefault(key, DUMMY_DIRECTORY);
                }
                directory = dir;
            } catch (Exception ignored) {
                return DUMMY_DIRECTORY;
            }
        }
        return directory;
    }

    @NotNull
    private Directory addDirectory(FileEntry entry) {
        String key = entry.getName().toUpperCase(Locale.ROOT);
        return directories.computeIfAbsent(key, e -> {
            try {
                Directory subDir = new Directory(entry.getPath());
                subDir.directoryEntry = entry;
                return subDir;
            } catch (IOException ignored) {
            }
            return DUMMY_DIRECTORY;
        });
    }

    public FileEntry addEntry(Path path) {
        FileEntry entry = newEntry(path);
        if (entry.exists()) {
            entries.put(entry.getName().toUpperCase(Locale.ROOT), entry);
            entry.setParent(this);
        }
        return entry;
    }

    private String[] getFileList() {
        if (path.equals(DUMMY_PATH)) {
            return new String[0];
        }
        return path.toFile().list();
    }

    private boolean isInvalid() {
        int fileCount = entries.size();
        String[] list = getFileList();
        if (list != null) {
            if (fileCount != list.length) {
                return true;
            }

            return Arrays.stream(list).anyMatch(e -> !entries.containsKey(e.toUpperCase(Locale.ROOT)));
        }
        return false;
    }

    public boolean revalidate() {
        final int files = entries.size();
        if (isInvalid()) {
            directories.clear();
            entries.clear();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                stream.forEach(this::addEntry);
            } catch (IOException e) {
                Console.out.println(String.format("Directory %s invalidate failed!", path), OsdColor.RED);
            }
            return true;
        }
        return files != entries.size();
    }

    @NotNull
    private FileEntry newEntry(Path path) {
        try {
            return new FileEntry(path);
        } catch (NoSuchFileException e) {
            Console.out.println(String.format("Path \"%s\" is not found.", path), OsdColor.RED);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DUMMY_ENTRY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Directory)) return false;
        Directory directory = (Directory) o;
        return Objects.equals(path, directory.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return path.toString();
    }

    public static class Game extends Directory {
        public Game(Path dir) throws IOException {
            super(dir);
            this.directoryEntry = new FileEntry(dir, dir.getFileName().toString(), 0);
            this.directoryEntry.setParent(this);
            this.directoryEntry.physicalDirectory = this;
        }
    }
}
