// This file is part of BuildGDX.
// Copyright (C) 2024  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Types.collections;

import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.m210projects.Build.Engine.pow2char;

public class BitMap {

    private byte[] array;

    public BitMap(int capacity) {
        this.array = new byte[capacity >> 3];
    }

    public BitMap() {
        this.array = new byte[128];
    }

    public void clear() {
        Arrays.fill(array, (byte) 0);
    }

    public boolean getBit(int index) {
        checkIndex(index);
        if (index >= 0) {
            return (array[index >> 3] & (1 << (index & 7))) != 0;
        }
        return false;
    }

    public void setBit(int index) {
        checkIndex(index);
        if (index >= 0) {
            array[index >> 3] |= (byte) pow2char[index & 7];
        }
    }

    public void clearBit(int index) {
        checkIndex(index);
        if (index >= 0) {
            array[index >> 3] &= (byte) ~pow2char[index & 7];
        }
    }

    private void checkIndex(int index) {
        int i = (index >> 3);
        if (i >= array.length) {
            int size = 1;
            while (size <= i) {
                size <<= 1;
            }

            byte[] newArray = new byte[size];
            System.arraycopy(array, 0, newArray, 0, array.length);
            this.array = newArray;
        }
    }

    public byte[] getArray() {
        return array;
    }

    public BitMap readObject(InputStream is) throws IOException {
        this.array = new byte[StreamUtils.readInt(is)];
        StreamUtils.readBytes(is, array);
        return this;
    }

    public void writeObject(OutputStream os) throws IOException {
        StreamUtils.writeInt(os, array.length);
        StreamUtils.writeBytes(os, array);
    }

    public void copy(BitMap src) {
        this.array = Arrays.copyOf(src.array, src.array.length);
    }

    @Override
    public String toString() {
        List<Integer> list = new ArrayList<>();
        final int size = (array.length << 3);
        for (int i = 0; i < size; i++) {
            if (getBit(i)) {
                list.add(i);
            }
        }
        return list.toString();
    }
}
