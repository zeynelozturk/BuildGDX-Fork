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

package ru.m210projects.Build.Types;

import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.listeners.PaletteListener;

import static java.lang.Math.*;
import static java.lang.Math.pow;

public interface PaletteManager {

    Color getFogColor(int pal);
    boolean isValidPalette(int paletteIndex);

    boolean changePalette(final byte[] palette);

    byte[] makePalookup(final int palnum, byte[] remapbuf, int r, int g, int b, int dastat);

    default int getPalookup(int davis, int dashade) { // jfBuild
        return (min(max(dashade + (davis >> 8), 0), getShadeCount() - 1));
    }

    int getPaletteGamma();

    byte[] getBasePalette();

    Palette getCurrentPalette();

    FastColorLookup getFastColorLookup();

    int getColorIndex(int pal, int colorIndex);

    int getColorIndex(int pal, int colorIndex, int shade);

    byte[][] getPalookupBuffer();

    byte[] getTranslucBuffer();

    int getShadeCount();

    byte[][] getBritableBuffer();

    void setListener(PaletteListener listener);

    void setbrightness(int dabrightness, byte[] dapal);

    interface PaletteLoader {
        byte[] getBasePalette();
        int getShadeCount();
        byte[][] getPalookup();
        byte[] getTransluc();

        default FastColorLookup getFastColorLookup() {
            return new DefaultFastColorLookup(getBasePalette(), 30, 59, 11);
        }

        default byte[][] getBritable() {
            byte[][] britable = new byte[16][256];
            for (int i = 0; i < 16; i++) {
                float a = 8.0f / (i + 8);
                float b = (float) (255.0f / pow(255.0f, a));
                for (int j = 0; j < 256; j++) {// JBF 20040207: full 8bit precision
                    britable[i][j] = (byte) (pow(j, a) * b);
                }
            }
            return britable;
        }
    }
}
