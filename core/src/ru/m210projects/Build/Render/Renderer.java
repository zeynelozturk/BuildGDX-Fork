// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.ScreenFade;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;

import java.io.OutputStream;

public interface Renderer {

    PixelFormat getTexFormat();

    void init(Engine engine);

    void uninit();

    boolean isInited();

    void resize(int width, int height);

    int getWidth();

    int getHeight();

    void drawmasks();

    int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum);

    void clearview(int dacol);

    void changepalette(byte[] palette);

    /**
     * Renderer next frame. Should be call manually, if don't use RenderChanger
     */
    void nextpage();

    void setview(int x1, int y1, int x2, int y2);

    void setFieldOfView(int fovDegrees);

    void setaspect();

    /**
     dastat&1 :translucence
     dastat&2 :auto-scale mode (use 320*200 coordinates)
     dastat&4 :y-flip
     dastat&8 :don't clip to startumost/startdmost
     dastat&16 :force point passed to be top-left corner, 0:Editart center
     dastat&32 :reverse translucence
     dastat&64 :non-masked, 0:masked
     dastat&128 :draw all pages (permanent)
     dastat&256 :align to the left (widescreen support)
     dastat&512 :align to the right (widescreen support)
     dastat&1024 :stretch to screen resolution (distorts aspect ration)
     */

    void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                      int cy1, int cx2, int cy2);

    void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat);

    Mirror preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector);

    void completemirror();

    void drawoverheadmap(int cposx, int cposy, int czoom, short cang);

    void drawmapview(int dax, int day, int zoome, int ang);

    int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow);

    boolean screencapture(OutputStream os, int newwidth, int newheight, PixelFormat pixelFormat);

    String screencapture(Directory dir, String fn);

    void drawline256(int x1, int y1, int x2, int y2, int col);

    void settiltang(int tilt);

    void setDefs(DefScript defs);

    void showScreenFade(ScreenFade screenFade);

    RenderType getType();

    int animateoffs(int tilenum, int nInfo);

    void setviewtotile(DynamicArtEntry dynamicArtEntry);

    void setviewback();

    ArtEntry getTile(int tileNum);

    int getParallaxScale();

    void setParallaxScale(int parallaxScale);

    int getParallaxOffset();

    void setParallaxOffset(int offset);

    byte[] getRenderedPics();

    byte[] getRenderedSectors();

    RenderedSpriteList getRenderedSprites();

    void addRenderedSprite(int spritenum);

    boolean gotPic(int pic);

    boolean gotSector(int sect);

    PaletteManager getPaletteManager();

    enum RenderType {
        Software("Classic"), Polymost("Polymost"), PolyGDX("PolyGDX");

        final String name;

        RenderType(String name) {
            this.name = name;
        }

        public static RenderType parseType(String name) {
            switch (name) {
                case "Classic":
                case "Software":
                    return Software;
                case "Polymost":
                    return Polymost;
                case "PolyGDX":
                    return PolyGDX;
            }
            return RenderType.Polymost;
        }

        public String getName() {
            return name;
        }
    }
}
