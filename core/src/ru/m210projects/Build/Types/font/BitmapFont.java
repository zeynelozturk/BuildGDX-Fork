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

public class BitmapFont extends Font {

    private final byte[] data;
    private final int atlasWidth, atlasHeight;

    public BitmapFont(byte[] data, int atlasWidth, int atlasHeight, int cols, int rows) {
        this.data = data;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.size = atlasHeight / rows;

        for (int i = 0; i < cols * rows; i++) {
            addCharInfo((char) i, setChar((char) i, atlasWidth, atlasHeight, cols, rows));
        }

        setVerticalScaled(false);
    }

    protected AtlasCharInfo setChar(char ch, int width, int height, int cols, int rows) {
        return new AtlasCharInfo(this, ch, 0, width, height, cols, rows);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public FontType getFontType() {
        return FontType.BITMAP_FONT;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }
}
