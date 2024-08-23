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

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Render.listeners.TileListener;
import ru.m210projects.Build.filehandle.Cache;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.ArtFile;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;

import java.util.Arrays;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.filehandle.art.ArtFile.DUMMY_ART_FILE;

public class TileManager {

    protected String tilesPath = "tilesXXX.art";
    protected ArtEntry[] tiles = new ArtEntry[MAXTILES];
    protected TileListener tileListener = TileListener.DUMMY_LISTENER;

    /**
     * Should be called when renderer is created
     * @param tileListener current renderer, that will listen tile changes
     */
    public void setTileListener(TileListener tileListener) {
        this.tileListener = tileListener;

        for (ArtEntry entry : tiles) {
            if (entry instanceof DynamicArtEntry) {
                // realloc with new listener
                allocatepermanenttile(entry);
            }
        }
    }

    public String getTilesPath() {
        return tilesPath;
    }

    public boolean loadpic(Entry artFile) { // gdxBuild
        if (artFile.exists()) {
            ArtFile art = new ArtFile(artFile.getName(), artFile::getInputStream);
            for (Entry artEntry : art.getEntries()) {
                ArtEntry tile = ((ArtEntry) artEntry);
                tiles[tile.getNum()] = tile;
            }
            return true;
        }

        return false;
    }

    public int loadpics(Cache cache) {
        char[] artFileName = tilesPath.toCharArray();
        Arrays.fill(tiles, null);

        int k;
        int numtilefiles = 0;
        do {
            k = numtilefiles;

            artFileName[7] = (char) ((k % 10) + 48);
            artFileName[6] = (char) (((k / 10) % 10) + 48);
            artFileName[5] = (char) (((k / 100) % 10) + 48);
            String name = String.copyValueOf(artFileName);
            if (!loadpic(cache.getEntry(name, true))) {
                break;
            }
            numtilefiles++;
        } while (k != numtilefiles);

        return (numtilefiles);
    }

    @Deprecated
    public byte[] loadtile(int tilenume) { // jfBuild
        ArtEntry pic = getTile(tilenume);
        return pic.getBytes();
    }

    @NotNull
    public DynamicArtEntry allocatepermanenttile(int tilenume, int xsiz, int ysiz) { // jfBuild
        if ((xsiz < 0) || (ysiz < 0) || (tilenume >= MAXTILES)) {
            return DUMMY_ART_FILE;
        }

        int dasiz = xsiz * ysiz;
        byte[] data = new byte[dasiz];
        DynamicArtEntry entry = new DynamicArtEntry(tileListener, tilenume, data, xsiz, ysiz, 0);
        tiles[tilenume] = entry;
        return entry;
    }

    @NotNull
    public DynamicArtEntry allocatepermanenttile(ArtEntry artEntry) {
        if (!artEntry.exists() || !artEntry.hasSize()) {
            return DUMMY_ART_FILE;
        }

        int tilenume = artEntry.getNum();
        DynamicArtEntry entry = new DynamicArtEntry(tileListener, artEntry.getNum(), artEntry.getBytes(), artEntry.getWidth(), artEntry.getHeight(), artEntry.getFlags());
        tiles[tilenume] = entry;
        return entry;
    }

    @NotNull
    public ArtEntry getTile(int tilenum) {
        if (tilenum < 0 || tilenum >= tiles.length) {
            return DUMMY_ART_FILE;
        }

        if (tiles[tilenum] == null) {
            tiles[tilenum] = new DynamicArtEntry(null, tilenum, new byte[0], 0, 0, 0);
        }

        return tiles[tilenum];
    }

    @NotNull
    public DynamicArtEntry getDynamicTile(int tilenum) {
        ArtEntry pic = getTile(tilenum);
        if (!(pic instanceof DynamicArtEntry) || !pic.exists()) {
            pic = allocatepermanenttile(pic);
        }
        return (DynamicArtEntry) pic;
    }

    public void copytilepiece(ArtEntry pic1, int sx1, int sy1, int xsiz, int ysiz, // jfBuild
                                         DynamicArtEntry pic2, int sx2, int sy2) {
        int xsiz1 = pic1.getWidth();
        int ysiz1 = pic1.getHeight();
        int xsiz2 = pic2.getWidth();
        int ysiz2 = pic2.getHeight();
        if ((xsiz1 > 0) && (ysiz1 > 0) && (xsiz2 > 0) && (ysiz2 > 0)) {
            byte[] data1 = pic1.getBytes();
            byte[] data2 = pic2.getBytes();

            int x1 = sx1;
            for (int i = 0; i < xsiz; i++) {
                int y1 = sy1;
                for (int j = 0; j < ysiz; j++) {
                    int x2 = sx2 + i;
                    int y2 = sy2 + j;
                    if ((x2 >= 0) && (y2 >= 0) && (x2 < xsiz2) && (y2 < ysiz2)) {
                        byte ptr = data1[x1 * ysiz1 + y1];
                        if (ptr != (byte) 255) {
                            data2[x2 * ysiz2 + y2] = ptr;
                        }
                    }

                    y1++;
                    if (y1 >= ysiz1) {
                        y1 = 0;
                    }
                }
                x1++;
                if (x1 >= xsiz1) {
                    x1 = 0;
                }
            }
        }
    }

    public void squarerotatetile(DynamicArtEntry pic) {
        int xsiz = pic.getWidth();
        int ysiz = pic.getHeight();

        // supports square tiles only for rotation part
        if (pic.exists() && xsiz == ysiz) {
            int k = (xsiz << 1);
            int ptr1, ptr2;
            for (int i = xsiz - 1, j; i >= 0; i--) {
                ptr1 = i * (xsiz + 1);
                ptr2 = ptr1;
                if ((i & 1) != 0) {
                    ptr1--;
                    ptr2 -= xsiz;
                    squarerotatetileswap(pic, ptr1, ptr2);
                }
                for (j = (i >> 1) - 1; j >= 0; j--) {
                    ptr1 -= 2;
                    ptr2 -= k;
                    squarerotatetileswap(pic, ptr1, ptr2);
                    squarerotatetileswap(pic, ptr1 + 1, ptr2 + xsiz);
                }
            }
        }
    }

    private void squarerotatetileswap(DynamicArtEntry pic, int p1, int p2) {
        if (pic != null && pic.exists()) {
            byte[] data = pic.getBytes();

            if (p1 < data.length && p2 < data.length) {
                byte tmp = data[p1];
                data[p1] = data[p2];
                data[p2] = tmp;
            }
        }
    }
}
