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

package ru.m210projects.Build.Render.TextureHandle;

import ru.m210projects.Build.Types.Palette;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.badlogic.gdx.graphics.GL20.*;
import static java.lang.Math.min;

public class RGBTileData extends TileData {

    public final ByteBuffer data;
    public final boolean hasalpha;
    public final int width, height;
    public final boolean clamped;

    public RGBTileData(PaletteManager paletteManager, ArtEntry tile, int dapal, boolean clamped, boolean alpha, int expflag, boolean fixtransparency) {
        byte[] data = tile.getBytes();
        int tsizx = tile.getWidth();
        int tsizy = tile.getHeight();

        if (data != null && (data.length == 0 || tile.getSize() > data.length)) {
            data = null;
        }

        int xsiz = tsizx;
        int ysiz = tsizy;
        if ((expflag & 1) != 0) {
            xsiz = calcSize(tsizx);
        }
        if ((expflag & 2) != 0) {
            ysiz = calcSize(tsizy);
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(data != null ? xsiz * ysiz * 4 : 4).order(ByteOrder.LITTLE_ENDIAN);
        boolean hasalpha = false;

        if (data == null) {
            buffer.putInt(0, 0);
            tsizx = tsizy = xsiz = ysiz = 1;
            hasalpha = true;
        } else {
            int pix_len = getPixelFormat().getLength();

            if (alpha) {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == (byte) 255) {
                        hasalpha = true;
                        break;
                    }
                }
            }

            int dptr = 0;
            int sptr = 0;
            int xoffs = xsiz * pix_len;
            if (clamped) {
                for (int y = (ysiz - 1); y >= 0; y--) {
                    sptr = y >= tsizy ? 0 : tsizx;
                    dptr = (xsiz * y + (sptr - 1)) * pix_len;
                    for (int x = sptr; x < xsiz; x++) {
                        buffer.putInt(dptr += pix_len, 0);
                    }
                }

                sptr = 0;
                for (int i = 0, j; i < tsizx * pix_len; i += pix_len) {
                    dptr = i;
                    for (j = 0; j < tsizy; j++) {
                        buffer.putInt(dptr, getColor(paletteManager, data[sptr++], dapal, alpha));
                        dptr += xoffs;
                    }
                }
                hasalpha = true;
            } else {
                int p, len = data.length;
                for (int i = 0, j; i < xoffs; i += pix_len) {
                    p = 0;
                    dptr = i;
                    for (j = 0; j < ysiz; j++) {
                        buffer.putInt(dptr, getColor(paletteManager, data[sptr + p++], dapal, alpha));
                        dptr += xoffs;
                        if (p >= tsizy) {
                            p = 0;
                        }
                    }
                    if ((sptr += tsizy) >= len) {
                        sptr = 0;
                    }
                }
            }

            if (hasalpha && fixtransparency) {
                fixtransparency(buffer, tsizx, tsizy, xsiz, ysiz, clamped);
            }
        }

        this.width = xsiz;
        this.height = ysiz;
        this.hasalpha = hasalpha;
        this.data = buffer;
        this.clamped = clamped;
    }

    @Override
    public int getGLType() {
        return GL_UNSIGNED_BYTE;
    }

    @Override
    public ByteBuffer getPixels() {
        data.rewind();
        return data;
    }

    @Override
    public int getGLInternalFormat() {
        return (hasalpha ? GL_RGBA : GL_RGB);
    }

    @Override
    public int getGLFormat() {
        return GL_RGBA;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public PixelFormat getPixelFormat() {
        return PixelFormat.Rgba;
    }

    protected int getColor(PaletteManager paletteManager, int dacol, int dapal, boolean alphaMode) {
        Palette curpalette = paletteManager.getCurrentPalette();
        byte[][] palookup = paletteManager.getPalookupBuffer();

        dacol &= 0xFF;
        if (alphaMode && dacol == 255) {
            return curpalette.getRGBA(0, (byte) 0);
        }

        if (dacol >= palookup[dapal].length) {
            return 0;
        }

        dacol = palookup[dapal][dacol] & 0xFF;
        return curpalette.getRGBA(dacol, (byte) 0xFF);
    }

    protected void fixtransparency(ByteBuffer dapic, int daxsiz, int daysiz, int daxsiz2, int daysiz2, boolean clamping) {
        int dox = daxsiz2 - 1;
        int doy = daysiz2 - 1;
        if (clamping) {
            dox = min(dox, daxsiz);
            doy = min(doy, daysiz);
        } else {
            daxsiz = daxsiz2;
            daysiz = daysiz2;
        } // Make repeating textures duplicate top/left parts

        daxsiz--;
        daysiz--;

        // Set transparent pixels to average color of neighboring opaque pixels
        // Doing this makes bilinear filtering look much better for masked
        // textures (I.E. sprites)
        int r, g, b, j, index, wp, wpptr, rgb;
        for (int y = doy, x; y >= 0; y--) {
            wpptr = y * daxsiz2 + dox;
            for (x = dox; x >= 0; x--, wpptr--) {
                wp = (wpptr << 2);
                if (dapic.get(wp + 3) != 0) {
                    continue;
                }

                r = g = b = j = 0;
                index = wp - 4;
                if ((x > 0) && (dapic.get(index + 3) != 0)) {
                    r += dapic.get(index + 0) & 0xFF;
                    g += dapic.get(index + 1) & 0xFF;
                    b += dapic.get(index + 2) & 0xFF;
                    j++;
                }
                index = wp + 4;
                if ((x < daxsiz) && (dapic.get(index + 3) != 0)) {
                    r += dapic.get(index + 0) & 0xFF;
                    g += dapic.get(index + 1) & 0xFF;
                    b += dapic.get(index + 2) & 0xFF;
                    j++;
                }
                index = wp - (daxsiz2 << 2);
                if ((y > 0) && (dapic.get(index + 3) != 0)) {
                    r += dapic.get(index + 0) & 0xFF;
                    g += dapic.get(index + 1) & 0xFF;
                    b += dapic.get(index + 2) & 0xFF;
                    j++;
                }
                index = wp + (daxsiz2 << 2);
                if ((y < daysiz) && (dapic.get(index + 3) != 0)) {
                    r += dapic.get(index + 0) & 0xFF;
                    g += dapic.get(index + 1) & 0xFF;
                    b += dapic.get(index + 2) & 0xFF;
                    j++;
                }

                switch (j) {
                    case 0:
                    case 1:
                        rgb = ((dapic.get(wp + 3) & 0xFF) << 24) + (b << 16) + (g << 8) + (r);
                        break;
                    case 2:
                        rgb = ((dapic.get(wp + 3) & 0xFF) << 24) + (((b + 1) >> 1) << 16) + (((g + 1) >> 1) << 8)
                                + (((r + 1) >> 1));
                        break;
                    case 3:
                        rgb = ((dapic.get(wp + 3) & 0xFF) << 24) + (((b * 85 + 128) >> 8) << 16)
                                + (((g * 85 + 128) >> 8) << 8) + (((r * 85 + 128) >> 8));
                        break;
                    case 4:
                        rgb = ((dapic.get(wp + 3) & 0xFF) << 24) + (((b + 2) >> 2) << 16) + (((g + 2) >> 2) << 8)
                                + (((r + 2) >> 2));
                        break;
                    default:
                        continue;
                }

                dapic.putInt(wp, rgb);
            }
        }
    }

    @Override
    public boolean hasAlpha() {
        return hasalpha;
    }

    @Override
    public boolean isClamped() {
        return clamped;
    }

    @Override
    public boolean isHighTile() {
        return false;
    }
}
