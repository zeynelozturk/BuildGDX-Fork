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

import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;

import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.Strhandler.toCharArray;
import static ru.m210projects.Build.Types.font.CharInfo.DUMMY_CHAR_INFO;

public class Font {

    protected CharInfo[] charInfo;
    protected int size = 1;
    protected boolean verticalScaled = true;

    public Font() {
        this.charInfo = new CharInfo[256];
        this.charInfo[0] = DUMMY_CHAR_INFO;
    }

    protected void addCharInfo(char ch, CharInfo charInfo) {
        this.charInfo[ch] = charInfo;
    }

    public FontType getFontType() {
        return FontType.TILE_FONT;
    }

    public CharInfo getCharInfo(char ch) {
        if (ch >= charInfo.length || charInfo[ch] == null) {
            return charInfo[0];
        }

        return this.charInfo[ch];
    }

    public boolean isVerticalScaled() {
        return verticalScaled;
    }

    public void setVerticalScaled(boolean verticalScaled) {
        this.verticalScaled = verticalScaled;
    }

    public int getWidth(char[] text, float scale) {
        int width = 0;
        for (int pos = 0; text != null && pos < text.length && text[pos] != 0; pos++) {
            width += (int) (getCharInfo(text[pos]).getCellSize() * scale);
        }
        return width;
    }

    public int getWidth(String text, float scale) {
        return getWidth(toCharArray(text), scale);
    }

    public int getSize() {
        return size;
    }

    public int drawText(Renderer renderer, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        return renderer.printext(this, x, y, text, scale, shade, palnum, align, transparent, shadow);
    }

    public int drawText(Renderer renderer, int x, int y, String text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        return drawText(renderer, x, y, toCharArray(text), scale, shade, palnum, align, transparent, shadow);
    }

    public int drawTextScaled(Renderer renderer, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, ConvertType type, boolean shadow) {
        x = coordsConvertXScaled(x, type);
        y = coordsConvertYScaled(y);

        int xdim = (4 * renderer.getHeight()) / 3;
        return drawText(renderer, x, y, text, (scale * xdim) / 320.0f, shade, palnum, align, transparent, shadow);
    }

    public int drawTextScaled(Renderer renderer, int x, int y, String text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, ConvertType type, boolean shadow) {
        return drawTextScaled(renderer, x, y, toCharArray(text), scale, shade, palnum, align, transparent, type, shadow);
    }

    public int drawCharScaled(Renderer renderer, int x, int y, char symb, float scale, int shade, int palnum, Transparent transparent, ConvertType type, boolean shadow) {
        CharInfo charInfo = getCharInfo(symb);
        drawTextScaled(renderer, x, y, new char[] { symb }, scale, shade, palnum, TextAlign.Left, transparent, type, shadow);
        return (int) (charInfo.getCellSize() * scale);
    }
}
