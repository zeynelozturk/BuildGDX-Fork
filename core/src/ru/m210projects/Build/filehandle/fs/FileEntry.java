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

import ru.m210projects.Build.filehandle.Cache;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;

public class FileEntry implements Entry {

    public static final Path DUMMY_PATH = Paths.get(Arrays.toString(new String[0]));
    private final Path path;
    private final String name;
    private final String extension;
    private final boolean isDirectory;
    private final long size;
    private Group parent;
    protected Directory physicalDirectory; // if physDir is equals == null, try to use AbsoluteFileEntry

    public FileEntry(Path path) throws IOException {
        this(path, path.getFileName().toString(), Files.size(path));
    }

    public FileEntry(Path path, String name, long size) {
        this.path = path;
        this.name = name;
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1).toUpperCase(Locale.ROOT);
        } else {
            this.extension = "";
        }
        this.isDirectory = Files.isDirectory(path);
        this.size = size;
    }

    protected FileEntry(FileEntry entry) {
        this.path = entry.path;
        this.name = entry.name;
        this.extension = entry.extension;
        this.isDirectory = entry.isDirectory;
        this.size = entry.size;
        this.parent = entry.parent;
        this.physicalDirectory = entry.physicalDirectory;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    public Directory getDirectory() {
        if (physicalDirectory == null) {
            return DUMMY_DIRECTORY;
        }

        return physicalDirectory.getDirectory(this);
    }

    @SuppressWarnings("IOStreamConstructor")
    @Override
    public InputStream getInputStream() throws IOException {
        // Files.newInputStream(path) is shitty slow!!!
        return new BufferedInputStream(new FileInputStream(path.toFile()));
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean exists() {
        try {
            if (isDirectory) {
                return true;
            }
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Directory getParent() {
        if (parent instanceof Directory) {
            return (Directory) parent;
        }

        return physicalDirectory;
    }

    @Override
    public void setParent(Group parent) {
        if (parent == null || parent.equals(DUMMY_DIRECTORY)) {
            parent = physicalDirectory;
        }

        // the parent might be GRP file
        this.parent = parent;
        if (physicalDirectory == null
                && parent instanceof Directory
                && ((Directory) parent).getPath().equals(path.getParent())) {
            physicalDirectory = (Directory) parent;
        }
    }

    public boolean delete() {
        try {
            if (Files.deleteIfExists(path)) {
                physicalDirectory.entries.remove(getName().toUpperCase(Locale.ROOT));
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Path getRelativePath() {
        try {
            return Cache.getInstance().getGameDirectory().getPath().relativize(path);
        } catch (Exception e) {
            return path;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEntry)) return false;
        FileEntry fileEntry = (FileEntry) o;
        return isDirectory == fileEntry.isDirectory && size == fileEntry.size && Objects.equals(path, fileEntry.path) && Objects.equals(name, fileEntry.name) && Objects.equals(extension, fileEntry.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, extension, size, isDirectory);
    }

    @Override
    public String toString() {
        if (isDirectory()) {
            return name;
        }
        return String.format("%s size=%d", name, size);
    }
}
