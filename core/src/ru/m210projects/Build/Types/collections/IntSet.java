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

import com.badlogic.gdx.utils.IntArray;

public class IntSet {

    private final BitMap bitmap;
    private final IntArray array;

    public IntSet(int capacity) {
        this.bitmap = new BitMap(capacity);
        this.array = new IntArray(capacity);
    }

    public int size() {
        return array.size;
    }

    public boolean isEmpty() {
        return array.isEmpty();
    }

    public boolean contains(int value) {
        return bitmap.getBit(value);
    }

    public boolean addValue(int value) {
        if (!bitmap.getBit(value)) {
            bitmap.setBit(value);
            array.add(value);
            return true;
        }
        return false;
    }

    public int getValue(int index) {
        return array.get(index);
    }

    public boolean removeValue(int value) {
        if (bitmap.getBit(value)) {
            array.removeValue(value);
            bitmap.clearBit(value);
            return true;
        }
        return false;
    }

    public void clear() {
        array.clear();
        bitmap.clear();
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
