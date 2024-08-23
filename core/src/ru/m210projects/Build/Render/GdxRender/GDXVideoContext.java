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

package ru.m210projects.Build.Render.GdxRender;

import com.badlogic.gdx.Gdx;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.settings.VideoContext;

public class GDXVideoContext extends VideoContext {

    private final GDXRenderer gdxRenderer;

    public GDXVideoContext(GDXRenderer gdxRenderer) {
        this.gdxRenderer = gdxRenderer;
    }

    @Override
    public void setgFov(int gFov) {
        this.gFov = gFov;
        gdxRenderer.setFieldOfView(gFov);
    }

    @Override
    public void setTextureFilter(TexFilter textureFilter) {
       int anisotropy = textureFilter.getAnisotropy();
        if (anisotropy > 1) {
            int maxAnisotropy = (int) GLInfo.getMaxAnisotropicFilterLevel();
            if (anisotropy > maxAnisotropy) {
                TexFilter bestFilter = TexFilter.TRILINEAR;
                for (TexFilter filter : TexFilter.values()) {
                    if (filter.getAnisotropy() == 1) {
                        continue;
                    }

                    if (filter.getAnisotropy() <= maxAnisotropy) {
                        bestFilter = filter;
                    }
                }
                textureFilter = bestFilter;
            }
        }
        gdxRenderer.textureCache.setFilter(textureFilter);
        gdxRenderer.modelManager.setTextureFilter(textureFilter);

        super.setTextureFilter(textureFilter);
    }

    @Override
    public void setUseHighTiles(boolean useHighTiles) {
        if (isUseHighTiles() == useHighTiles) {
            // already sets
            return;
        }

        // Must be called in GL thread
        Gdx.app.postRunnable(() -> {
            gdxRenderer.textureCache.uninit();
            gdxRenderer.clearskins(true);
            super.setUseHighTiles(useHighTiles);
        });
    }

    @Override
    public void setPaletteEmulation(boolean paletteEmulation) {
        gdxRenderer.enableIndexedShader(paletteEmulation);
        super.setPaletteEmulation(paletteEmulation);
    }
}
