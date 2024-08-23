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

package ru.m210projects.Build.Render.Types;

public class DefaultScreenFade implements ScreenFade {

    private int red;
    private int green;
    private int blue;
    private int intensive;
    private final String name;

    public DefaultScreenFade(String name) {
        this.name = name;
    }

    public DefaultScreenFade set(int r, int g, int b, int intensive) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.intensive = intensive;
        return this;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public int getIntensive() {
        return intensive;
    }

    @Override
    public ScreenFade setIntensive(int intensive) {
        this.intensive = Math.min(255, intensive);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }
}
