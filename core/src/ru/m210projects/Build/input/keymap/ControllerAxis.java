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

package ru.m210projects.Build.input.keymap;

public enum ControllerAxis {
    NULL,
    LEFT_STICK_X,
    LEFT_STICK_Y,
    RIGHT_STICK_X,
    RIGHT_STICK_Y;

    public static ControllerAxis valueOf(int ordinal) {
        switch (ordinal) {
            case 1:
                return LEFT_STICK_X;
            case 2:
                return LEFT_STICK_Y;
            case 3:
                return RIGHT_STICK_X;
            case 4:
                return RIGHT_STICK_Y;
        }
        return NULL;
    }
}