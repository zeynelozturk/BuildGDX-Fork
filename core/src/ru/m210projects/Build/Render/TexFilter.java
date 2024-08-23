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

package ru.m210projects.Build.Render;

import com.badlogic.gdx.graphics.Texture;

public enum TexFilter {

    NONE(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, 1),
    BILINEAR(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, 1),
    TRILINEAR(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear, 1),
    ANISOTROPY_2X(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear, 2),
    ANISOTROPY_4X(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear, 4),
    ANISOTROPY_8X(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear, 8),
    ANISOTROPY_16X(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear, 16);

    private final Texture.TextureFilter min;
    private final Texture.TextureFilter mag;
    private final boolean mipmaps;
    private final int anisotropy;

    TexFilter(Texture.TextureFilter min, Texture.TextureFilter mag, int anisotropy) {
        this.min = min;
        this.mag = mag;

        this.mipmaps = (min.isMipMap() || mag.isMipMap());
        this.anisotropy = anisotropy;
    }

    public int getAnisotropy() {
        return anisotropy;
    }

    public Texture.TextureFilter getMin() {
        return min;
    }

    public Texture.TextureFilter getMag() {
        return mag;
    }

    public boolean isMipmaps() {
        return mipmaps;
    }

    public static TexFilter valueOf(int ordinal) {
        if (ordinal < 0 || ordinal >= TexFilter.values().length) {
            return TexFilter.NONE;
        }
        return TexFilter.values()[ordinal];
    }
}
