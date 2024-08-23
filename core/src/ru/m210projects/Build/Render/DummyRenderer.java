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

package ru.m210projects.Build.Render;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.Types.ScreenFade;
import ru.m210projects.Build.Render.listeners.PaletteListener;
import ru.m210projects.Build.Render.listeners.WorldListener;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;


import java.io.OutputStream;

import static ru.m210projects.Build.filehandle.art.ArtFile.DUMMY_ART_FILE;

public class DummyRenderer implements Renderer {

    public static final Renderer DUMMY_RENDERER = new DummyRenderer();

    @Override
    public TileData.PixelFormat getTexFormat() {
        return TileData.PixelFormat.Bitmap;
    }

    @Override
    public void init(Engine engine) {
        engine.getPaletteManager().setListener(PaletteListener.DUMMY_PALETTE_CHANGE_LISTENER);
        engine.getBoardService().setListener(WorldListener.DUMMY_LISTENER);
    }

    @Override
    public void uninit() {

    }

    @Override
    public boolean isInited() {
        return true;
    }

    @Override
    public void drawmasks() {

    }

    @Override
    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) {
        return 0;
    }

    @Override
    public void clearview(int dacol) {

    }

    @Override
    public void changepalette(byte[] palette) {

    }

    @Override
    public void nextpage() {

    }

    @Override
    public void setview(int x1, int y1, int x2, int y2) {

    }

    @Override
    public void setFieldOfView(int fovDegrees) {

    }

    @Override
    public void setaspect() {

    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1, int cy1, int cx2, int cy2) {

    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat) {

    }

    @Override
    public Mirror preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector) {
        return null;
    }

    @Override
    public void completemirror() {

    }

    @Override
    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) {

    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {

    }

    @Override
    public int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        return 0;
    }

    @Override
    public boolean screencapture(OutputStream os, int newwidth, int newheight, TileData.PixelFormat pixelFormat) {
        return false;
    }

    @Override
    public String screencapture(Directory dir, String fn) {
        return null;
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int col) {

    }

    @Override
    public void settiltang(int tilt) {

    }

    @Override
    public void setDefs(DefScript defs) {

    }

    @Override
    public void showScreenFade(ScreenFade screenFade) {

    }

    @Override
    public RenderType getType() {
        return RenderType.Software;
    }

    @Override
    public int animateoffs(int tilenum, int nInfo) {
        return 0;
    }

    @Override
    public void setviewtotile(DynamicArtEntry dynamicArtEntry) {

    }

    @Override
    public void setviewback() {

    }

    @Override
    public ArtEntry getTile(int tileNum) {
        return DUMMY_ART_FILE;
    }

    @Override
    public int getParallaxScale() {
        return 0;
    }

    @Override
    public void setParallaxScale(int parallaxScale) {

    }

    @Override
    public int getParallaxOffset() {
        return 0;
    }

    @Override
    public void setParallaxOffset(int offset) {

    }

    @Override
    public byte[] getRenderedPics() {
        return new byte[0];
    }

    @Override
    public byte[] getRenderedSectors() {
        return new byte[0];
    }

    @Override
    public RenderedSpriteList getRenderedSprites() {
        return new RenderedSpriteList();
    }

    @Override
    public void addRenderedSprite(int spritenum) {

    }

    @Override
    public boolean gotPic(int pic) {
        return false;
    }

    @Override
    public boolean gotSector(int sect) {
        return false;
    }

    @Override
    public PaletteManager getPaletteManager() {
        return null;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
