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

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.listeners.PaletteListener;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Pragmas.mulscale;

public class DefaultPaletteManager implements PaletteManager {

    protected static final Color DEFAULT_COLOR = new Color(0, 0, 0, 0);
    protected final Palette currentPalette;
    protected final FastColorLookup fastColorLookup;
    protected final byte[] basePalette;
    protected final byte[][] palookup;
    protected final byte[] transluc;
    protected final int shadesCount;
    protected final byte[][] britable;
    protected final Color[] palookupfog;
    private final Engine engine;
    protected PaletteListener listener = PaletteListener.DUMMY_PALETTE_CHANGE_LISTENER;

    public DefaultPaletteManager(Engine engine, PaletteLoader loader) {
        this.engine = engine;
        this.basePalette = loader.getBasePalette();
        this.shadesCount = loader.getShadeCount();
        this.palookup = loader.getPalookup();
        this.transluc = loader.getTransluc();
        this.britable = loader.getBritable();
        this.fastColorLookup = loader.getFastColorLookup();

        this.palookupfog = new Color[MAXPALOOKUPS];
        this.currentPalette = new Palette();
        byte[] currPalette = currentPalette.getBytes();
        for (int i = 0; i < basePalette.length; i++) {
            currPalette[i] = (byte) ((basePalette[i] & 0xFF) << 2);
        }
    }

    public void setListener(PaletteListener listener) {
        this.listener = listener;
    }

    @Override
    public void setbrightness(int dabrightness, byte[] dapal) {
        int curbrightness = BClipRange(dabrightness, 0, 15);
        byte[] temppal = new byte[768];
        if (curbrightness != 0) {
            for (int i = 0; i < dapal.length; i++) {
                temppal[i] = britable[curbrightness][(dapal[i] & 0xFF) << 2];
            }
        } else {
            System.arraycopy(dapal, 0, temppal, 0, dapal.length);
            for (int i = 0; i < dapal.length; i++) {
                temppal[i] <<= 2;
            }
        }
        int r = britable[curbrightness][1] >> 2;
        int g = britable[curbrightness][1] >> 2;
        int b = britable[curbrightness][1] >> 2;
        setFogColor(0, new Color(r, g, b, 0));
        changePalette(temppal);
    }

    public int getPaletteGamma() {
        return engine.getConfig().getPaletteGamma();
    }

    @Override
    public Color getFogColor(int pal) {
        if (palookupfog[pal] == null) {
            return DEFAULT_COLOR;
        }
        return palookupfog[pal];
    }

    @Override
    public boolean isValidPalette(int paletteIndex) {
        if (paletteIndex < 0 || paletteIndex >= palookup.length) {
            return false;
        }
        return palookup[paletteIndex] != null;
    }

    @Override
    public boolean changePalette(final byte[] palette) {
        if (!currentPalette.update(palette)) {
            return false;
        }

        fastColorLookup.invalidate();
        listener.onChangePalette(palette);
        return true;
    }

    @Override
    public byte[] makePalookup(final int palnum, byte[] remapbuf, int r, int g, int b, int dastat) { // jfBuild
        if (!isValidPalette(palnum)) {
            palookup[palnum] = new byte[shadesCount << 8];
        }

        if (dastat == 0 || (r | g | b | 63) != 63) {
            return palookup[palnum];
        }

        if (dastat == 2) {
            int len = Math.min(palookup[palnum].length, remapbuf.length);
            System.arraycopy(remapbuf, 0, palookup[palnum], 0, len);
        } else {
            if ((r | g | b) == 0) {
                for (int i = 0; i < 256; i++) {
                    for (int j = 0; j < shadesCount; j++) {
                        palookup[palnum][i + j * 256] = palookup[0][(remapbuf[i] & 0xFF) + j * 256];
                    }
                }
            } else {
                byte[] palette = new byte[768];
                System.arraycopy(currentPalette.getBytes(), 0, palette, 0, 768);
                for (int j = 0; j < 768; j++)
                    palette[j] = (byte) ((palette[j] & 0xFF) >> 2);

                for (int i = 0; i < shadesCount; i++) {
                    int palscale = divscale(i, shadesCount, 16);
                    for (int j = 0; j < 256; j++) {
                        int rptr = palette[3 * (remapbuf[j] & 0xFF)] & 0xFF;
                        int gptr = palette[3 * (remapbuf[j] & 0xFF) + 1] & 0xFF;
                        int bptr = palette[3 * (remapbuf[j] & 0xFF) + 2] & 0xFF;

                        palookup[palnum][j + i * 256] = fastColorLookup.getClosestColorIndex(palette, rptr + mulscale(r - rptr, palscale, 16), gptr + mulscale(g - gptr, palscale, 16), bptr + mulscale(b - bptr, palscale, 16));
                    }
                }
            }
        }

        setFogColor(palnum, new Color(r, g, b, 0));
        listener.onPalookupChanged(palnum);
        return palookup[palnum];
    }

    protected void setFogColor(int palnum, Color fogColor) {
        if (!fogColor.equals(palookupfog[palnum])) {
            palookupfog[palnum] = fogColor;
        }
    }

    @Override
    public byte[] getBasePalette() {
        return basePalette;
    }

    @Override
    public Palette getCurrentPalette() {
        return currentPalette;
    }

    @Override
    public FastColorLookup getFastColorLookup() {
        return fastColorLookup;
    }

    @Override
    public int getColorIndex(int pal, int colorIndex) {
        if (!isValidPalette(pal)) {
            pal = 0;
        }

        if (colorIndex >= palookup[pal].length) {
            return 0;
        }

        return palookup[pal][colorIndex] & 0xFF;
    }

    @Override
    public int getColorIndex(int pal, int color, int shade) {
        if (!isValidPalette(pal)) {
            pal = 0;
        }

        if (color >= palookup[pal].length) {
            return 0;
        }

        shade = (min(max(shade, 0), shadesCount - 1));
        return palookup[pal][color + (shade << 8)] & 0xFF;
    }

    @Override
    public byte[][] getPalookupBuffer() {
        return palookup;
    }

    @Override
    public byte[] getTranslucBuffer() {
        return transluc;
    }

    @Override
    public int getShadeCount() {
        return shadesCount;
    }

    @Override
    public byte[][] getBritableBuffer() {
        return britable;
    }
}
