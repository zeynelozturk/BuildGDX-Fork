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
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FastZipInputStream extends ZipInputStream {

    private final InputStream in;
    private ZipEntry entry;
    private int available;

    public FastZipInputStream(@NotNull InputStream in) {
        super(in);
        this.in = in;
    }

    @Override
    public int available() throws IOException {
        return available;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int l = super.read(b, off, len);
        if (l != -1) {
            available -= l;
        }
        return l;
    }

    public ZipEntry getNextEntry() throws IOException {
        entry = super.getNextEntry();
        if (entry != null) {
            available = (int) entry.getSize();
        }
        return entry;
    }

    @Override
    public void closeEntry() {
    }

    public void skipEntry() throws IOException {
        if (available != entry.getSize()) {
            super.closeEntry();
        } else {
            StreamUtils.skip(in, (int) entry.getCompressedSize());
        }
    }
}
