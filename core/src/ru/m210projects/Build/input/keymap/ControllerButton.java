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

import com.badlogic.gdx.Input;

/**
 * Dedicated controller button map
 */
public enum ControllerButton {

    NULL(Input.Keys.UNKNOWN),
    BUTTON_A(Input.Keys.BUTTON_A),
    BUTTON_B(Input.Keys.BUTTON_B),
    BUTTON_X(Input.Keys.BUTTON_X),
    BUTTON_Y(Input.Keys.BUTTON_Y),
    BACK(Input.Keys.BUTTON_SELECT),
    GUIDE(Input.Keys.BUTTON_MODE),
    START(Input.Keys.BUTTON_START),
    LEFT_STICK(Input.Keys.BUTTON_THUMBL),
    RIGHT_STICK(Input.Keys.BUTTON_THUMBR),
    LEFT_BUMPER(Input.Keys.BUTTON_L1),
    RIGHT_BUMPER(Input.Keys.BUTTON_R1),
    LEFT_TRIGGER(Input.Keys.BUTTON_L2),
    RIGHT_TRIGGER(Input.Keys.BUTTON_R2),
    DPAD_UP(Keymap.BUTTON_UP),
    DPAD_DOWN(Keymap.BUTTON_DOWN),
    DPAD_LEFT(Keymap.BUTTON_LEFT),
    DPAD_RIGHT(Keymap.BUTTON_RIGHT);

    private final int keyCode;

    ControllerButton(int keyCode) {
        this.keyCode = keyCode;
    }

    public static ControllerButton valueOf(int keyCode) {
        switch (keyCode) {
            case (Input.Keys.BUTTON_A):
                return BUTTON_A;
            case (Input.Keys.BUTTON_B):
                return BUTTON_B;
            case (Input.Keys.BUTTON_X):
                return BUTTON_X;
            case (Input.Keys.BUTTON_Y):
                return BUTTON_Y;
            case (Input.Keys.BUTTON_SELECT):
                return BACK;
            case (Input.Keys.BUTTON_MODE):
                return GUIDE;
            case (Input.Keys.BUTTON_START):
                return START;
            case (Input.Keys.BUTTON_THUMBL):
                return LEFT_STICK;
            case (Input.Keys.BUTTON_THUMBR):
                return RIGHT_STICK;
            case (Input.Keys.BUTTON_L1):
                return LEFT_BUMPER;
            case (Input.Keys.BUTTON_R1):
                return RIGHT_BUMPER;
            case (Input.Keys.BUTTON_L2):
                return LEFT_TRIGGER;
            case (Input.Keys.BUTTON_R2):
                return RIGHT_TRIGGER;
            case (Keymap.BUTTON_UP):
                return DPAD_UP;
            case (Keymap.BUTTON_DOWN):
                return DPAD_DOWN;
            case (Keymap.BUTTON_LEFT):
                return DPAD_LEFT;
            case (Keymap.BUTTON_RIGHT):
                return DPAD_RIGHT;
        }

        return ControllerButton.NULL;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
