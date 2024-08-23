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

public class CharInfo {

    public static final CharInfo DUMMY_CHAR_INFO = new CharInfo(null, -1, 1.0f, 1) {
        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public FontType getFontType() {
            return FontType.NULL_FONT;
        }
    };

    public final int tile;
    public final int width; // symbol width
    public final short cellSize; // width + gap
    public final short xOffset, yOffset;
    protected final float tileScale;
    protected final Font parent;

    public CharInfo(Font parent, int tile, float tileScale, int cellSize, int width, int xOffset, int yOffset) {
        this.parent = parent;

        this.tile = tile;
        this.xOffset = (short) xOffset;
        this.yOffset = (short) yOffset;
        this.cellSize = (short) (cellSize * tileScale);
        this.width = (int) (width * tileScale);
        this.tileScale = tileScale;
    }

    public CharInfo(Font parent, int tile, int cellSize, int width) {
        this(parent, tile, 1.0f, cellSize, width, 0, 0);
    }

    public CharInfo(Font parent, int tile, float tileScale, int cellSize) {
        this(parent, tile, tileScale, cellSize, cellSize, 0, 0);
    }

    public CharInfo(Font parent, int tile, int cellSize, int xOffset, int yOffset) {
        this(parent, tile, 1.0f, cellSize, cellSize, xOffset, yOffset);
    }

    public FontType getFontType() {
        return parent.getFontType();
    }

    public Font getParent() {
        return parent;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return parent.getSize();
    }

    public short getCellSize() {
        return cellSize;
    }

    public int getTile() {
        return tile;
    }

    public float getTileScale() {
        return tileScale;
    }
}
