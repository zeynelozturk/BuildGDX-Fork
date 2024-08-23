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

package ru.m210projects.Build.settings;

import ru.m210projects.Build.input.GameKey;

public enum GameKeys implements GameKey {
    Move_Forward,
    Move_Backward,
    Turn_Left,
    Turn_Right,
    Turn_Around,
    Strafe,
    Strafe_Left,
    Strafe_Right,
    Jump,
    Crouch,
    Run,
    Open,
    Weapon_Fire,
    Next_Weapon,
    Previous_Weapon,
    Look_Up,
    Look_Down,
    Map_Toggle,
    Enlarge_Screen,
    Shrink_Screen,
    Send_Message,
    Mouse_Aiming,
    Menu_Toggle,
    Show_Console;

    private String name;

    @Override
    public int getNum() {
        return ordinal();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return (name != null) ? name : name();
    }

}
