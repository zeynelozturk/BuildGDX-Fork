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

package ru.m210projects.Build.Types;

public enum AnimType {
    OSCIL(1 << 6), FORWARD(2 << 6), BACKWARD(3 << 6), NONE(0);

    private final int bit;

    AnimType(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    public static AnimType findAnimType(int flags) {
        switch (flags & 192) {
            case 64:
                return AnimType.OSCIL;
            case 128:
                return AnimType.FORWARD;
            case 192:
                return AnimType.BACKWARD;
        }
        return AnimType.NONE;
    }
}
