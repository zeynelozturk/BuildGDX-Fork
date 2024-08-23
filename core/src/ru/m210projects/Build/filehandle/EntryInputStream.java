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

package ru.m210projects.Build.filehandle;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EntryInputStream extends BufferedInputStream {
    private int remaining;
    private final int size;

    public EntryInputStream(@NotNull InputStream entryInputStream, int size) {
        super(entryInputStream, Math.min(size, 8192));
        this.remaining = size;
        this.size = size;
    }

    @Override
    public synchronized int read(byte @NotNull [] b, int off, int len) throws IOException {
        if (remaining == 0) {
            return -1;
        }

        if (len > remaining) {
            len = remaining;
        }
        len = super.read(b, off, len);
        remaining -= len;
        return len;
    }

    @Override
    public synchronized int read() throws IOException {
        if (remaining > 0) {
            remaining--;
            return super.read();
        }
        return -1;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (n > remaining) {
            n = remaining;
        }
        long total = super.skip(n);
        remaining -= (int) total;
        return total;
    }

    @Override
    public synchronized int available() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed");
        }
        return remaining;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        remaining = size;
    }
}
