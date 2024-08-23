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

package ru.m210projects.Build.Types.font;

public class AtlasCharInfo extends CharInfo {

    protected float tx1;
    protected float ty1;
    protected float tx2;
    protected float ty2;

    public AtlasCharInfo(Font parent, char ch, int atlasTile, float tileScale, int atlasWidth, int atlasHeight, int cols, int rows) {
        super(parent, atlasTile, tileScale, (atlasWidth / cols));

        int height = atlasHeight / rows;

        this.tx1 = (float) (ch % cols) / cols;
        this.ty1 = (float) (ch / cols) / rows;
        this.tx2 = tx1 + (width / (atlasWidth * tileScale));
        this.ty2 = ty1 + (height / (float) atlasHeight);
    }

    public AtlasCharInfo(Font parent, char ch, int atlasTile, int atlasWidth, int atlasHeight, int cols, int rows) {
        this(parent, ch, atlasTile, 1.0f, atlasWidth, atlasHeight, cols, rows);
    }

    @Override
    public int getHeight() {
        return cellSize;
    }

    public float getTx1() {
        return tx1;
    }

    public float getTy1() {
        return ty1;
    }

    public float getTx2() {
        return tx2;
    }

    public float getTy2() {
        return ty2;
    }
}
