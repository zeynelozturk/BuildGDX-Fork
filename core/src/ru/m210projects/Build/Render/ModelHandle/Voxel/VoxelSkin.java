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

package ru.m210projects.Build.Render.ModelHandle.Voxel;

import ru.m210projects.Build.Render.TextureHandle.DummyTileData;
import ru.m210projects.Build.Types.Palette;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.Types.Tile;

public class VoxelSkin extends DummyTileData {

    public VoxelSkin(PixelFormat fmt, PaletteManager paletteManager, Tile tile, int dapal) {
        super(fmt, tile.getWidth(), tile.getHeight());

        if (fmt != PixelFormat.Pal8) {
            int wpptr, wp, dacol;
            byte[][] palookup = paletteManager.getPalookupBuffer();
            Palette curpalette = paletteManager.getCurrentPalette();
            for (int x, y = 0; y < height; y++) {
                wpptr = y * width;
                for (x = 0; x < width; x++, wpptr++) {
                    wp = wpptr << 2;
                    dacol = tile.data[wpptr] & 0xFF;
                    dacol = palookup[dapal][dacol] & 0xFF;

                    data.putInt(wp, curpalette.getRGB(dacol) + (255 << 24));
                }
            }
        } else {
            data.put(tile.data, 0, width * height);
        }
    }

    @Override
    public boolean isClamped() {
        return true;
    }

    @Override
    public boolean hasAlpha() {
        return false;
    }

}
