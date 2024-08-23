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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.CRC32;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.TextureHandle.PixmapOutputStream;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.Types.DefaultScreenFade;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Strhandler.buildString;

public abstract class AbstractRenderer implements Renderer {

    public static DefaultScreenFade DEFAULT_SCREEN_FADE = new DefaultScreenFade("Default");

    protected final GameConfig config;
    private final char[] fpsbuffer = new char[32];
    private final byte[] shortbuf = new byte[2];
    private final Mirror mirror = new Mirror();
    public int fpscol = 31;
    public int parallaxyoffs, parallaxyscale;
    public boolean offscreenrendering;
    public boolean showinvisibility;
    public float TRANSLUSCENT1 = 0.66f;
    public float TRANSLUSCENT2 = 0.33f;

    public int xdimen = -1, ydimen, xdimenscale, xdimscale;
    public int viewingrange;
    public int yxaspect;
    public int xyaspect;
    public int wx1, wy1, wx2, wy2;
    public int windowx1, windowy1, windowx2, windowy2;
    /**
     * Bitarray of rendered textures
     */
    public byte[] gotpic;
    /**
     * Bitarray of rendered maps sectors
     */
    public byte[] gotsector;
    public RenderedSpriteList tSpriteList;
    public int globalposx, globalposy, globalposz;
    public float globalhoriz, globalang;
    public float pitch;
    public int globalcursectnum;
    public int globalvisibility;
    public int globalshade, globalpal, cosglobalang, singlobalang;
    public int cosviewingrangeglobalang, sinviewingrangeglobalang;
    public int beforedrawrooms = 1;
    public boolean inpreparemirror = false;
    /**
     * Engine object. Should be set in {@link #init(Engine)}
     */
    protected Engine engine;
    /**
     * Renderer dimensions (width, height)
     */
    protected int xdim, ydim;
    protected int totalclocklock;
    protected int setviewcnt = 0; // interface layers use this now
    protected int[] bakwindowx1, bakwindowy1;
    protected int[] bakwindowx2, bakwindowy2;
    protected DynamicArtEntry baktile;
    protected TileManager tileManager;
    protected PaletteManager paletteManager;
    protected BoardService boardService;
    private long fpstime = 0;
    public float backBufferScale = 1.0f;

    public AbstractRenderer(GameConfig config) {
        this.config = config;
        gotpic = new byte[(MAXTILES + 7) >> 3];
        gotsector = new byte[(MAXSECTORS + 7) >> 3];
        tSpriteList = new RenderedSpriteList();

        bakwindowx1 = new int[4];
        bakwindowy1 = new int[4];
        bakwindowx2 = new int[4];
        bakwindowy2 = new int[4];

        parallaxyoffs = 0;
        parallaxyscale = 65536;
    }

    public GameConfig getConfig() {
        return config;
    }

    protected boolean isShowFPS() {
        return config.isgShowFPS();
    }

    protected float getFpsScale() {
        return config.getgFpsScale();
    }

    public Engine getEngine() {
        return engine;
    }

    public ArtEntry getTile(int tilenum) {
        return tileManager.getTile(tilenum);
    }

    @Override
    public int getParallaxScale() {
        return parallaxyscale;
    }

    @Override
    public void setParallaxScale(int parallaxScale) {
        this.parallaxyscale = parallaxScale;
    }

    public int getParallaxOffset() {
        return parallaxyoffs;
    }

    public void setParallaxOffset(int offset) {
        this.parallaxyoffs = offset;
    }

    /**
     * Renderer initialization method (attach it to the game)
     * @param engine {@link Engine} object
     */
    @Override
    public void init(Engine engine) {
        this.engine = engine;
        this.tileManager = engine.getTileManager();
        this.paletteManager = engine.getPaletteManager();
        this.boardService = engine.getBoardService();

        byte[] pal = paletteManager.getBasePalette();
        fpscol = 0;
        int k = 0;
        for (int i = 0; i < 256; i += 3) {
            int j = (pal[3 * i] & 0xFF) + (pal[3 * i + 1] & 0xFF) + (pal[3 * i + 2] & 0xFF);
            if (j > k) {
                k = j;
                fpscol = i;
            }
        }
        this.backBufferScale = Gdx.graphics.getBackBufferScale();
    }

    @Override
    public void changepalette(byte[] palette) {
    }

    @Override
    public void setview(int x1, int y1, int x2, int y2) {
        windowx1 = x1;
        wx1 = (x1 << 12);
        windowy1 = y1;
        wy1 = (y1 << 12);
        windowx2 = x2;
        wx2 = ((x2 + 1) << 12);
        windowy2 = y2;
        wy2 = ((y2 + 1) << 12);

        setaspect();
    }

    /**
     * @return renderer display width
     */
    @Override
    public int getWidth() {
        return xdim;
    }

    /**
     * @return renderer display height
     */
    @Override
    public int getHeight() {
        return ydim;
    }

    /**
     * Sets bitarray of rendered textures in {@link #drawrooms()}
     * @param tilenume number of tile
     */
    public void setgotpic(int tilenume) {
        gotpic[tilenume >> 3] |= (byte) pow2char[tilenume & 7];
    }

    @Override
    public int animateoffs(int tilenum, int nInfo) { // jfBuild + gdxBuild
        long clock, index = 0;
        ArtEntry tile = getTile(tilenum);

        int speed = tile.getAnimSpeed();
        if ((nInfo & 0xC000) == 0x8000) { // sprite
            // hash sprite frame by info variable

            shortbuf[0] = (byte) ((nInfo) & 0xFF);
            shortbuf[1] = (byte) ((nInfo >>> 8) & 0xFF);

            clock = (totalclocklock + CRC32.getChecksum(shortbuf)) >> speed;
        } else {
            clock = totalclocklock >> speed;
        }

        int frames = tile.getAnimFrames();

        if (frames > 0) {
            switch (tile.getType()) {
                case OSCIL:
                    index = clock % (frames * 2L);
                    if (index >= frames) {
                        index = frames * 2L - index;
                    }
                    break;
                case FORWARD:
                    index = clock % (frames + 1);
                    break;
                case BACKWARD:
                    index = -(clock % (frames + 1));
                    break;
                default: // None
                    break;
            }
        }
        return (int) index;
    }

    /**
     * Calls when frame rendering is completed
     */
    @Override
    public void nextpage() {
        if (isShowFPS()) {
            printfps(getFpsScale());
        }
        totalclocklock = engine.getTotalClock();
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat) {
        this.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, 0, 0, xdim - 1, ydim - 1);
    }

    protected abstract void drawrooms();

    protected abstract ByteBuffer getFrame(TileData.PixelFormat format, int xsiz, int ysiz);

    @Override
    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) { // eDuke32
        beforedrawrooms = 0;

        globalposx = (int) daposx;
        globalposy = (int) daposy;
        globalposz = (int) daposz;

        globalang = BClampAngle(daang);
        globalhoriz = (dahoriz - 100);
        pitch = (-EngineUtils.getAngle(160, (int) (dahoriz - 100))) / (2048.0f / 360.0f);

        globalcursectnum = (short) dacursectnum;
        totalclocklock = engine.getTotalClock();

        cosglobalang = (int) BCosAngle(globalang);
        singlobalang = (int) BSinAngle(globalang);

        cosviewingrangeglobalang = mulscale(cosglobalang, viewingrange, 16);
        sinviewingrangeglobalang = mulscale(singlobalang, viewingrange, 16);

        Arrays.fill(gotpic, (byte) 0);
        Arrays.fill(gotsector, (byte) 0);
        tSpriteList.reset();

        drawrooms();
        return 0;
    }

    @Override
    public Mirror preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector) { // jfBuild
        BoardService boardService = engine.getBoardService();
        int x = boardService.getWall(dawall).getX();
        int dx = boardService.getWall(boardService.getWall(dawall).getPoint2()).getX() - x;
        int y = boardService.getWall(dawall).getY();
        int dy = boardService.getWall(boardService.getWall(dawall).getPoint2()).getY() - y;
        int j = dx * dx + dy * dy;
        if (j == 0) {
            return mirror;
        }
        int i = (((dax - x) * dx + (day - y) * dy) << 1);
        mirror.setX((x << 1) + scale(dx, i, j) - dax);
        mirror.setY((y << 1) + scale(dy, i, j) - day);
        mirror.setAngle(BClampAngle((EngineUtils.getAngle(dx, dy) << 1) - daang));

        inpreparemirror = true;
        return mirror;
    }

    @Override
    public boolean screencapture(OutputStream os, int captureWidth, int captureHeight, TileData.PixelFormat pixelFormat) {
        int xf = divscale(xdim, captureWidth, 16);
        int yf = divscale(ydim, captureHeight, 16);

        try {
            ByteBuffer frame = getFrame(pixelFormat, xdim, -ydim);
            switch (pixelFormat) {
                case Pal8:
                    for (int fx = 0; fx < captureWidth; fx++) {
                        int base = mulscale(fx, xf, 16);
                        for (int fy = 0; fy < captureHeight; fy++) {
                            int pos = base + (mulscale(fy, yf, 16) * xdim);
                            StreamUtils.writeByte(os, frame.get(pos));
                        }
                    }
                    return true;
                case Rgb:
                    for (int fx, fy = 0; fy < captureHeight; fy++) {
                        int base = mulscale(fy, yf, 16) * xdim;
                        for (fx = 0; fx < captureWidth; fx++) {
                            int pos = 3 * (base + mulscale(fx, xf, 16));
                            StreamUtils.writeByte(os, frame.get(pos));
                            StreamUtils.writeByte(os, frame.get(pos + 1));
                            StreamUtils.writeByte(os, frame.get(pos + 2));
                        }
                    }
                    return true;
            }
        } catch (Exception e) {
            Console.out.println(e.toString(), OsdColor.RED);
        }

        return false;
    }

    @Override
    public String screencapture(Directory dir, String fn) { // jfBuild + gdxBuild (screenshot)
        int a, b, c, d;
        fn = fn.replaceAll("[^a-zA-Z0-9_. \\[\\]-]", "");
        fn = fn.substring(0, fn.lastIndexOf('.') - 4);

        int capturecount = 0;
        do { // JBF 2004022: So we don't overwrite existing screenshots
            if (capturecount > 9999) {
                return null;
            }

            a = ((capturecount / 1000) % 10);
            b = ((capturecount / 100) % 10);
            c = ((capturecount / 10) % 10);
            d = (capturecount % 10);

            if (!dir.getEntry(fn + a + b + c + d + ".png").exists()) {
                break;
            }

            capturecount++;
        } while (true);

        int w = xdim, h = ydim;
        Path path = dir.getPath().resolve(fn + a + b + c + d + ".png");
        try(OutputStream os = new PixmapOutputStream(new Pixmap(w, h, Pixmap.Format.RGB888), path)) {
            if (screencapture(os, w, h, TileData.PixelFormat.Rgb)) {
                return path.getFileName().toString();
            }
        } catch (Exception e) {
            Console.out.println(e.toString(), OsdColor.RED);
        } finally {
            dir.revalidate();
        }
        return null;
    }

    @Override
    public byte[] getRenderedPics() {
        return gotpic;
    }

    @Override
    public byte[] getRenderedSectors() {
        return gotsector;
    }

    @Override
    public RenderedSpriteList getRenderedSprites() {
        return tSpriteList;
    }

    @Override
    public void addRenderedSprite(int spritenum) {
        TSprite tspr = tSpriteList.obtain();
        tspr.set(boardService.getSprite(spritenum));
        tspr.setOwner(spritenum);
    }

    @Override
    public boolean gotPic(int picnum) {
        return (gotpic[picnum >> 3] & pow2char[picnum & 7]) != 0;
    }

    @Override
    public boolean gotSector(int sectnum) {
        return (gotsector[sectnum >> 3] & pow2char[sectnum & 7]) != 0;
    }

    protected void printfps(float scale) {
        if (System.currentTimeMillis() - fpstime >= 1000) {
            int fps = Gdx.graphics.getFramesPerSecond();
            float rate = Gdx.graphics.getDeltaTime() * 1000;
            if (fps <= 9999 && rate <= 9999) {
                buildString(fpsbuffer, 0, String.format(Locale.US, "%.2fms %dfps", Math.round(rate * 100) / 100.0, fps));
            }
            fpstime = System.currentTimeMillis();
        }
        EngineUtils.getLargeFont().drawText(this, windowx2 - 1, windowy1 + 1, fpsbuffer, scale, 0, fpscol, TextAlign.Right, Transparent.None, false);
    }
}
