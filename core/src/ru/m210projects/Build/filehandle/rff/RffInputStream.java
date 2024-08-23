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

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.InputStreamProvider;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class RffInputStream extends BufferedInputStream {

    private long cryptKey;
    private final int cryptLength;
    private int pos = 0;

    public RffInputStream(InputStream is, long cryptKey, int cryptLength) {
        super(is);
        this.cryptKey = cryptKey;
        this.cryptLength = cryptLength;
    }

    @Override
    public synchronized int read() throws IOException {
        int data = super.read();
        if (data != -1) {
            if (pos < cryptLength) {
                data ^= (int) (cryptKey++ >> 1);
            }
            pos++;
            return data & 0xFF;
        }
        return -1;
    }

    @Override
    public synchronized int read(byte @NotNull [] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            if (pos < cryptLength) {
                encrypt(b, off, Math.min(cryptLength - pos, n));
            }
            pos += n;
        }
        return n;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        long result = 0;
        do {
            long len = super.skip(n);
            pos += (int) len;
            cryptKey += len;
            n -= len;
            result += len;
        } while (n > 0);

        return result;
    }

    @Override
    public synchronized int read(byte @NotNull [] b) throws IOException {
        return read(b, 0, b.length);
    }

    private void encrypt(byte[] buffer, int offs, int size) {
        for (int i = 0; i < size; i++) {
            buffer[offs + i] ^= (byte) (cryptKey++ >> 1);
        }
    }

    static InputStream getInputStream(InputStreamProvider provider, boolean encrypted, long pos, int cryptKey, int cryptLength) throws IOException {
        InputStream is = provider.newInputStream();
        if (is.skip(pos) != pos) {
            throw new EOFException();
        }

        if (encrypted) {
            return new RffInputStream(is, cryptKey, cryptLength);
        }
        return is;
    }
}
