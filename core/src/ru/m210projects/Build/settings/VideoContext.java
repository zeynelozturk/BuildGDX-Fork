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

package ru.m210projects.Build.settings;

import com.badlogic.gdx.Gdx;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.TexFilter;

import java.io.IOException;
import java.io.OutputStream;

public class VideoContext implements ConfigContext {

    public static final int MINFOV = 60;
    public static final int MAXFOV = 140;
    private static final String NAME = "RenderSetup";
    protected boolean gVSync = false;
    protected int fpslimit = 0;
    protected int paletteGamma = 0;
    protected float gFpsScale = 1.0f;
    protected boolean useVoxels = true;
    protected boolean useHighTiles = true;
    protected boolean useModels = true;
    protected boolean widescreen = true;
    protected boolean gShowFPS = true;
    protected TexFilter glfilter = TexFilter.NONE;
    protected boolean paletteFiltered = false;
    protected boolean softShading = false;
    protected int gFov = 90;
    protected float fgamma = 1;
    protected boolean paletteEmulation = true;
    protected boolean detailMapping = true;
    protected boolean glowMapping = true;
    protected boolean animSmoothing = true;

    @Override
    public void load(Properties prop) {
        if (prop.setContext(NAME)) {
            gVSync = prop.getBooleanValue("VSync", gVSync);
            fpslimit = Math.max(0, prop.getIntValue("FPSLimit", fpslimit));
            glfilter = TexFilter.valueOf(prop.getIntValue("GLFilterMode", glfilter.ordinal()));
            paletteFiltered = prop.getBooleanValue("PaletteFiltered", paletteFiltered);
            softShading = prop.getBooleanValue("SoftShading", softShading);
            widescreen = prop.getBooleanValue("WideScreen", widescreen);
            gFov = Gameutils.BClipRange(prop.getIntValue("FieldOfView", gFov), MINFOV, MAXFOV);
            gFpsScale = Gameutils.BClipRange(prop.getFloatValue("FpsScale", gFpsScale), 0.3f, 10.0f);
            paletteEmulation = prop.getBooleanValue("PaletteEmulation", paletteEmulation);
            fgamma = Gameutils.BClipRange(prop.getFloatValue("GLGamma", fgamma), 0.0f, 1.0f);
            paletteGamma = Gameutils.BClipRange(prop.getIntValue("PaletteGamma", paletteGamma), 0, 255);
            gShowFPS = prop.getBooleanValue("ShowFPS", gShowFPS);
            useVoxels = prop.getBooleanValue("UseVoxels", useVoxels);
            useModels = prop.getBooleanValue("UseModels", useModels);
            useHighTiles = prop.getBooleanValue("UseHighTiles", useHighTiles);
            detailMapping = prop.getBooleanValue("DetailMapping", detailMapping);
            glowMapping = prop.getBooleanValue("GlowMapping", glowMapping);
            animSmoothing = prop.getBooleanValue("AnimSmoothing", animSmoothing);
        }
    }

    /**
     * @param videoContext source context that have already loaded settings
     * @return this context object
     */
    VideoContext init(VideoContext videoContext) {
        if (videoContext != null) {
            this.gVSync = videoContext.gVSync;
            this.fpslimit = videoContext.fpslimit;
            this.paletteGamma = videoContext.paletteGamma;
            this.gFpsScale = videoContext.gFpsScale;
            this.widescreen = videoContext.widescreen;
            this.gShowFPS = videoContext.gShowFPS;
            this.glfilter = videoContext.glfilter;
            this.paletteFiltered = videoContext.paletteFiltered;
            this.softShading = videoContext.softShading;
            this.gFov = videoContext.gFov;
            this.fgamma = videoContext.fgamma;
            this.paletteEmulation = videoContext.paletteEmulation;
            this.useVoxels = videoContext.useVoxels;
            this.useModels = videoContext.useModels;
            this.useHighTiles = videoContext.useHighTiles;
            this.detailMapping = videoContext.detailMapping;
            this.glowMapping = videoContext.glowMapping;
            this.animSmoothing = videoContext.animSmoothing;
        }
        return this;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        putString(outputStream, String.format("[%s]\r\n", NAME));
        putBoolean(outputStream, "VSync", gVSync);
        putInteger(outputStream, "FPSLimit", fpslimit);
        putInteger(outputStream, "GLFilterMode", glfilter.ordinal());
        putBoolean(outputStream, "PaletteFiltered", paletteFiltered);
        putBoolean(outputStream, "SoftShading", softShading);
        putBoolean(outputStream, "WideScreen", widescreen);
        putInteger(outputStream, "FieldOfView", gFov);
        putFloat(outputStream, "FpsScale", gFpsScale);
        putBoolean(outputStream, "PaletteEmulation", paletteEmulation);
        putFloat(outputStream, "GLGamma", fgamma);
        putInteger(outputStream, "PaletteGamma", paletteGamma);
        putBoolean(outputStream, "ShowFPS", gShowFPS);
        putBoolean(outputStream, "UseVoxels", useVoxels);
        putBoolean(outputStream, "UseModels", useModels);
        putBoolean(outputStream, "UseHighTiles", useHighTiles);
        putBoolean(outputStream, "SetailMapping", detailMapping);
        putBoolean(outputStream, "GlowMapping", glowMapping);
        putBoolean(outputStream, "AnimSmoothing", animSmoothing);
        putString(outputStream, ";\r\n;\r\n");
    }

    public boolean isgVSync() {
        return gVSync;
    }

    public void setgVSync(boolean gVSync) {
        try { // crash if hires textures loaded
            Gdx.graphics.setVSync(gVSync);
            this.gVSync = gVSync;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getFpslimit() {
        return fpslimit;
    }

    public void setFpslimit(int fpslimit) {
        this.fpslimit = fpslimit;
        Gdx.graphics.setForegroundFPS(fpslimit);
    }

    public int getPaletteGamma() {
        return paletteGamma;
    }

    public void setPaletteGamma(int paletteGamma) {
        this.paletteGamma = paletteGamma;
    }

    public float getFgamma() {
        return fgamma;
    }

    public void setFgamma(float fgamma) {
        this.fgamma = fgamma;
    }

    public float getgFpsScale() {
        return gFpsScale;
    }

    public void setgFpsScale(float gFpsScale) {
        this.gFpsScale = gFpsScale;
    }

    public boolean isWidescreen() {
        return widescreen;
    }

    public void setWidescreen(boolean widescreen) {
        this.widescreen = widescreen;
    }

    public TexFilter getTextureFilter() {
        return glfilter;
    }

    public void setTextureFilter(TexFilter textureFilter) {
        this.glfilter = textureFilter;
    }

    public boolean getPaletteFiltered() { return this.paletteFiltered; }

    public void setPaletteFiltered(boolean enabled) {
        this.paletteFiltered = enabled;
    }

    public boolean getSmoothShading() { return this.softShading; }

    public void setSmoothShading(boolean enabled) {
        this.softShading = enabled;
    }

    public boolean isgShowFPS() {
        return gShowFPS;
    }

    public void setgShowFPS(boolean gShowFPS) {
        this.gShowFPS = gShowFPS;
    }

    public int getgFov() {
        return gFov;
    }

    public void setgFov(int gFov) {
        this.gFov = gFov;
    }

    public boolean isPaletteEmulation() {
        return paletteEmulation;
    }

    public void setPaletteEmulation(boolean paletteEmulation) {
        this.paletteEmulation = paletteEmulation;
    }

    public boolean isUseVoxels() {
        return useVoxels;
    }

    public void setUseVoxels(boolean useVoxels) {
        this.useVoxels = useVoxels;
    }

    public boolean isUseHighTiles() {
        return useHighTiles;
    }

    public void setUseHighTiles(boolean useHighTiles) {
        this.useHighTiles = useHighTiles;
    }

    public boolean isUseModels() {
        return useModels;
    }

    public void setUseModels(boolean useModels) {
        this.useModels = useModels;
    }

    public boolean isDetailMapping() {
        return detailMapping;
    }

    public void setDetailMapping(boolean detailMapping) {
        this.detailMapping = detailMapping;
    }

    public boolean isGlowMapping() {
        return glowMapping;
    }

    public void setGlowMapping(boolean glowMapping) {
        this.glowMapping = glowMapping;
    }

    public boolean isAnimSmoothing() {
        return animSmoothing;
    }

    public void setAnimSmoothing(boolean animSmoothing) {
        this.animSmoothing = animSmoothing;
    }
}
