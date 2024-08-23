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

import ru.m210projects.Build.Render.listeners.TileListener;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class DynamicArtEntry extends ArtEntry {

    private final byte[] data;
    private final TileListener listener;

    public DynamicArtEntry(TileListener listener, int num, byte[] data, int width, int height, int flags) {
        super(() -> new ByteArrayInputStream(data), num, 0, width, height, flags);
        this.data = data;
        this.listener = listener;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    public void copyData(byte[] data) {
        if (data.length < size) {
            throw new RuntimeException("Wrong tile data length");
        }
        System.arraycopy(data, 0, this.data, 0, size);
        invalidate();
    }

    public void clearData() {
        Arrays.fill(data, (byte) 0);
        invalidate();
    }

    /**
     * in case if byte array was modified by getData
     */
    public void invalidate() {
        if (listener != null) {
            listener.onInvalidate(num);
        }
    }
}
