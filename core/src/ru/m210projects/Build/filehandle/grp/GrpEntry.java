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

package ru.m210projects.Build.filehandle.grp;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.EntryInputStream;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class GrpEntry implements Entry {
    private final int offset;
    private final int size;
    private final String name;
    private final String extension;
    private final InputStreamProvider provider;
    Group parent;

    public GrpEntry(InputStreamProvider provider, String name, int offset, int size) {
        this.provider = provider;
        this.offset = offset;
        this.size = size;
        this.name = name;
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
        } else {
            this.extension = "";
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = provider.newInputStream();
        if (is.skip(offset) != offset) {
            throw new EOFException();
        }
        return new EntryInputStream(is, size);
    }

    @Override
    public long getSize() {
        return size;
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
    public boolean exists() {
        return true;
    }

    @Override
    public Group getParent() {
        return parent;
    }

    @Override
    public void setParent(Group parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format("%s size=%d", name, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GrpEntry)) return false;
        GrpEntry grpEntry = (GrpEntry) o;
        return offset == grpEntry.offset && size == grpEntry.size && Objects.equals(name, grpEntry.name) && Objects.equals(provider, grpEntry.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, size, name, provider);
    }
}
