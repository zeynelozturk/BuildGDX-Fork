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

public enum OsdColor {

    DEFAULT(""),
    RESET("\u001B[0m"),
    RED("\u001B[91m"),
    BLUE("\u001B[94m"),
    GREEN("\u001B[92m"),
    YELLOW("\u001B[93m"),
    BROWN("\u001B[33m"),
    WHITE("\u001B[97m"),
//    PURPLE("\u001B[95m"),
//    CYAN("\u001B[96m"),
    GREY("\u001B[90m");

    private int pal;
    private final String ansi;

    OsdColor(String ansi) {
        this.ansi = ansi;
        this.pal = -1;
    }

    public static OsdColor findColor(int pal) {
        OsdColor[] values = OsdColor.values();
        for(OsdColor color : values) {
            if(color.pal == pal) {
                return color;
            }
        }
        return OsdColor.DEFAULT;
    }

    public int getPal() {
        return pal;
    }

    public void setPal(int pal) {
        this.pal = pal;
    }

    @Override
    public String toString() {
        return ansi;
    }
}
