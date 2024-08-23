// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
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

import ru.m210projects.Build.EngineUtils;

import java.util.Arrays;

import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.pow2char;

public class DefaultFastColorLookup implements FastColorLookup {

    private final int FASTPALGRIDSIZ = 8;
    private final byte[] coldist = {0, 1, 2, 3, 4, 3, 2, 1};

    private int[] rdist, gdist, bdist;
    private byte[] colhere;
    private byte[] colhead;
    private short[] colnext;
    private int[] colscan;
    protected Byte[] palcache = new Byte[0x40000]; // buffer 256kb

    public DefaultFastColorLookup(byte[] basePalette, int rscale, int gscale, int bscale) {
        rdist = new int[129];
        gdist = new int[129];
        bdist = new int[129];
        colhere = new byte[((FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2)) >> 3];
        colhead = new byte[(FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2)];
        colnext = new short[256];
        colscan = new int[27];

        int j = 0;
        for (int i = 64; i >= 0; i--) {
            rdist[i] = rdist[128 - i] = j * rscale;
            gdist[i] = gdist[128 - i] = j * gscale;
            bdist[i] = bdist[128 - i] = j * bscale;
            j += 129 - (i << 1);
        }

        Arrays.fill(colhere, (byte) 0);
        Arrays.fill(colhead, (byte) 0);

        for (int i = 255; i >= 0; i--) {
            int r = basePalette[3 * i] & 0xFF;
            int g = basePalette[3 * i + 1] & 0xFF;
            int b = basePalette[3 * i + 2] & 0xFF;
            j = (r >> 3) * FASTPALGRIDSIZ * FASTPALGRIDSIZ + (g >> 3) * FASTPALGRIDSIZ + (b >> 3)
                    + FASTPALGRIDSIZ * FASTPALGRIDSIZ + FASTPALGRIDSIZ + 1;
            if ((colhere[j >> 3] & EngineUtils.powToLong(j & 7)) != 0) {
                colnext[i] = (short) (colhead[j] & 0xFF);
            } else {
                colnext[i] = -1;
            }

            colhead[j] = (byte) i;
            colhere[j >> 3] |= (byte) EngineUtils.powToLong(j & 7);
        }

        int i = 0;
        for (int x = -FASTPALGRIDSIZ * FASTPALGRIDSIZ; x <= FASTPALGRIDSIZ * FASTPALGRIDSIZ; x += FASTPALGRIDSIZ * FASTPALGRIDSIZ) {
            for (int y = -FASTPALGRIDSIZ; y <= FASTPALGRIDSIZ; y += FASTPALGRIDSIZ) {
                for (int z = -1; z <= 1; z++) {
                    colscan[i++] = x + y + z;
                }
            }
        }

        i = colscan[13];
        colscan[13] = colscan[26];
        colscan[26] = i;
    }

    public void invalidate() {
        Arrays.fill(palcache, null);
    }

    public byte getClosestColorIndex(byte[] palette, int r, int g, int b) { // jfBuild
        int i, k, dist;
        byte retcol;
        int pal1;

        int j = (r >> 3) * FASTPALGRIDSIZ * FASTPALGRIDSIZ + (g >> 3) * FASTPALGRIDSIZ + (b >> 3)
                + FASTPALGRIDSIZ * FASTPALGRIDSIZ + FASTPALGRIDSIZ + 1;

        int rgb = ((r << 12) | (g << 6) | b);

        int mindist = min(rdist[(coldist[r & 7] & 0xFF) + 64 + 8], gdist[(coldist[g & 7] & 0xFF) + 64 + 8]);
        mindist = min(mindist, bdist[(coldist[b & 7] & 0xFF) + 64 + 8]);
        mindist++;

        Byte out = palcache[rgb & (palcache.length - 1)];
        if (out != null) {
            return out;
        }

        r = 64 - r;
        g = 64 - g;
        b = 64 - b;

        retcol = -1;
        for (k = 26; k >= 0; k--) {
            i = colscan[k] + j;
            if ((colhere[i >> 3] & pow2char[i & 7]) == 0) {
                continue;
            }

            i = colhead[i] & 0xFF;
            do {
                pal1 = i * 3;
                dist = gdist[(palette[pal1 + 1] & 0xFF) + g];
                if (dist < mindist) {
                    dist += rdist[(palette[pal1] & 0xFF) + r];
                    if (dist < mindist) {
                        dist += bdist[(palette[pal1 + 2] & 0xFF) + b];
                        if (dist < mindist) {
                            mindist = dist;
                            retcol = (byte) i;
                        }
                    }
                }
                i = colnext[i];
            } while (i >= 0);
        }
        if (retcol >= 0) {
            palcache[rgb & (palcache.length - 1)] = retcol;
            return retcol;
        }

        mindist = 0x7fffffff;
        for (i = 255; i >= 0; i--) {
            pal1 = i * 3;
            dist = gdist[(palette[pal1 + 1] & 0xFF) + g];
            if (dist >= mindist) {
                continue;
            }

            dist += rdist[(palette[pal1] & 0xFF) + r];
            if (dist >= mindist) {
                continue;
            }

            dist += bdist[(palette[pal1 + 2] & 0xFF) + b];
            if (dist >= mindist) {
                continue;
            }

            mindist = dist;
            retcol = (byte) i;
        }

        palcache[rgb & (palcache.length - 1)] = retcol;
        return retcol;
    }
}
