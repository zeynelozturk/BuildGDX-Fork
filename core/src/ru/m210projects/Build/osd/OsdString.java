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

package ru.m210projects.Build.osd;

import com.badlogic.gdx.utils.ShortArray;

import java.util.ArrayList;
import java.util.List;

public class OsdString {

    private final StringBuilder text = new StringBuilder();
    private final ShortArray fmt = new ShortArray();

    public void clear() {
        this.text.setLength(0);
        this.fmt.clear();
    }

    public void insert(int pos, char ch, int pal, int shade) {
        if(pos < text.length()) {
            text.setCharAt(pos, ch);
            fmt.set(pos, (short) (pal + (shade << 8)));
        } else {
            text.append(ch);
            fmt.add((short) (pal + (shade << 8)));
        }
    }

    public int getLength() {
        return text.length();
    }

    public int getPal(int pos) {
        if (pos >= fmt.size) {
            return 0;
        }

        return fmt.get(pos) & 0xFF;
    }

    public int getShade(int pos) {
        if (pos >= fmt.size) {
            return 0;
        }

        return (fmt.get(pos) >> 8);
    }

    public char getCharAt(int pos) {
        return text.charAt(pos);
    }

    public boolean isEmpty() {
        return fmt.isEmpty();
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
