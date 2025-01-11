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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.NumberUtils;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager.Shader;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.GLSurface;
import ru.m210projects.Build.Render.IOverheadMapSettings;
import ru.m210projects.Build.Render.IOverheadMapSettings.MapView;
import ru.m210projects.Build.Render.OrphoRenderer;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Render.TextureHandle.DummyTileData;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.IndexedShader;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.Types.Hudtyp;
import ru.m210projects.Build.Render.Types.Tile2model;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Types.font.*;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.nio.ByteBuffer;

import static com.badlogic.gdx.graphics.GL20.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.net.Mmulti.connecthead;
import static ru.m210projects.Build.net.Mmulti.connectpoint2;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Types.font.FontType.BITMAP_FONT;

public class GDXOrtho extends OrphoRenderer {

    protected final float[] vertices;
    protected final Matrix4 projectionMatrix = new Matrix4();
    protected final GDXRenderer parent;
    private final BoardService boardService;
    private final int maxSpriteCount = 128;
    protected Mesh mesh;
    protected Mesh linesMesh;
    protected int idx = 0;
    protected GLTile lastTexture = null;
    protected GLTile lineTile = null;
    protected int lastType = GL20.GL_TRIANGLES;
    protected float invTexWidth = 0, invTexHeight = 0;
    protected boolean drawing = false;
    protected boolean blendingDisabled = false;
    protected float color = com.badlogic.gdx.graphics.Color.WHITE_FLOAT_BITS;
    protected int cx1, cy1, cx2, cy2;
    protected ShaderManager manager;
    private Sprite hudsprite;

    public GDXOrtho(GDXRenderer parent, IOverheadMapSettings settings) {
        super(parent, settings);
        this.parent = parent;
        this.manager = parent.manager;
        this.boardService = parent.getEngine().getBoardService();

        int VERTEX_SIZE = 2 + 1 + 2;
        int SPRITE_SIZE = 4 * VERTEX_SIZE;
        this.vertices = new float[maxSpriteCount * SPRITE_SIZE];
    }

    @Override
    public void init() {
        int size = maxSpriteCount;

        this.mesh = new Mesh(false, size * 4, size * 6,
                new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        this.linesMesh = new Mesh(false, size * 2, 0,
                new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));

        resize(xdim, ydim);

        int len = size * 6;
        short[] indices = new short[len];
        short j = 0;
        for (int i = 0; i < len; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 2);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = j;
        }
        mesh.setIndices(indices);

        this.lineTile = allocLineTile();
    }

    @Override
    public void uninit() {
        if (mesh != null) {
            mesh.dispose();
        }
        if (linesMesh != null) {
            linesMesh.dispose();
        }
        if (lineTile != null) {
            lineTile.dispose();
        }
        idx = 0;
        drawing = false;
    }

    @Override
    public int printext(Font font, int xpos, int ypos, char[] text, float scale, int shade, int col, TextAlign textAlign, Transparent transparent, boolean shadow) {
        if (font == null || text == null || text.length == 0) {
            return 0;
        }

		if(col < 0) {
            return 0;
        }

        if (shadow) {
            printext(font, xpos + (int) scale, ypos + (int) scale, text, scale, 127, col, textAlign, transparent, false);
        }

        float scaleyf = scale;
        if (font.isVerticalScaled()) {
            scaleyf *= 1.2f;
        }

        int alignx = 0;
        if (textAlign != TextAlign.Left) {
            int width = 0;
            for (int pos = 0; pos < text.length && text[pos] != 0; pos++) {
                CharInfo charInfo = font.getCharInfo(text[pos]);
                width += (int) (charInfo.getCellSize() * scale);
            }

            if (textAlign == TextAlign.Center) {
                width >>= 1;
            }
            xpos -= width;
        }

        parent.globalpal = col;
        parent.globalshade = shade;

        Gdx.gl.glDisable(GL_CULL_FACE);
        Gdx.gl.glDisable(GL_DEPTH_TEST);
        setType(GL20.GL_TRIANGLES);
        setViewport(0, 0, xdim - 1, ydim - 1);

        GLTile pth = null;
        int currentTile = -1;
        int numshades = parent.getPaletteManager().getShadeCount();
        for (int c = 0; c < text.length && text[c] != 0; c++) {
            CharInfo charInfo = font.getCharInfo(text[c]);

            final int tile = charInfo.tile;
            final float cellsizx = scale * charInfo.getCellSize();

            if (tile != -1) {
                float tx1, ty1;
                float tx2, ty2;

                final int charsizex = (int) (scale * charInfo.getWidth());
                final int charsizey = (int) (scaleyf * charInfo.getHeight());
                final int xoffset = (int) (scale * charInfo.xOffset);
                final int yoffset = (int) (scaleyf * charInfo.yOffset);

                if (currentTile != tile) {
                    flush();

                    float r = 0;
                    float g = 0;
                    float b = 0;
                    if (charInfo.getFontType() == BITMAP_FONT) {
                        pth = parent.textureCache.getBitmapFontAtlas((BitmapFont) charInfo.getParent());
                        parent.textureCache.bind(pth);

                        Palette curpalette = parent.getPaletteManager().getCurrentPalette();
                        col = parent.getPaletteManager().getColorIndex(0, col, shade);

                        r = curpalette.getRed(col) / 255.0f;
                        g = curpalette.getGreen(col) / 255.0f;
                        b = curpalette.getBlue(col) / 255.0f;
                    } else {
                        if (!parent.getPaletteManager().isValidPalette(col)) {
                            col = 0;
                        }

                        // tiled atlas or char tile
                        ArtEntry charTile = parent.getTile(tile);
                        if (!charTile.exists()) {
                            currentTile = -1;
                            continue;
                        }

                        pth = parent.bind(charTile, col, shade, 0, 4);
                        if (pth.getPixelFormat() != PixelFormat.Pal8) {
                            float sh = (numshades - min(max(shade, 0), numshades)) / (float) numshades;
                            r = g = b = sh;
                        }
                    }

                    if (pth == null) {
                        currentTile = -1;
                        continue;
                    }

                    currentTile = tile;
                    switchOrphoShader(getShader(pth));
                    switchTexture(pth);

                    float alpha = 1.0f;
                    if (transparent == Transparent.Bit1) {
                        alpha = parent.TRANSLUSCENT1;
                    }

                    if (transparent == Transparent.Bit2) {
                        alpha = parent.TRANSLUSCENT2;
                    }

                    if (pth.getPixelFormat() == PixelFormat.Pal8) {
                        switchTextureParams(col, shade, alpha, false);
                    } else {
                        setColor(r, g, b, alpha);
                    }
                    enableBlending();
                }

                int vx1 = xpos + xoffset;
                int vy1 = ypos + yoffset;
                int vx2 = vx1 + charsizex;
                int vy2 = vy1 + charsizey;

                if (charInfo instanceof AtlasCharInfo) {
                    // draw atlas char
                    AtlasCharInfo atlasCharInfo = (AtlasCharInfo) charInfo;

                    tx1 = atlasCharInfo.getTx1();
                    ty1 = atlasCharInfo.getTy1();
                    tx2 = atlasCharInfo.getTx2();
                    ty2 = atlasCharInfo.getTy2();
                } else {
                    tx1 = 0.0f;
                    ty1 = 0.0f;
                    float tileScale = charInfo.getTileScale();

                    float tsizx = pth.getWidth();
                    float tsizy = pth.getHeight();
                    if (pth.isHighTile()) {
                        // #GDX 29.07.2024
                        tsizx = pth.getWidth() / pth.getXScale();
                        tsizy = GLInfo.calcSize(pth.getHeight()) / pth.getYScale();
                    }

                    tx2 = charInfo.getWidth() / (tsizx * tileScale);
                    ty2 = charInfo.getHeight() / (tsizy * tileScale);
                }

                final float color = this.color;
                addVertex(vx1, vy1, color, tx1, ty1);
                addVertex(vx1, vy2, color, tx1, ty2);
                addVertex(vx2, vy2, color, tx2, ty2);
                addVertex(vx2, vy1, color, tx2, ty1);

                if (idx == vertices.length) {
                    flush();
                }
            }
            xpos += (int) cellsizx;
            alignx += (int) cellsizx;
        }

		Gdx.gl.glDepthMask(true); // re-enable writing to the z-buffer

        return (int) (alignx / scale);
    }

    private void addVertex(float vx, float vy, float color, float tx, float ty) {
        vertices[idx++] = vx;
        vertices[idx++] = vy;
        vertices[idx++] = color;
        vertices[idx++] = tx;
        vertices[idx++] = ty;
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int col) {
        float sx1 = x1 / 4096.0f;
        float sy1 = y1 / 4096.0f;
        float sx2 = x2 / 4096.0f;
        float sy2 = y2 / 4096.0f;

        if (sx1 < 0 && sx2 < 0 || sx1 > xdim && sx2 > xdim) {
            return;
        }

        if (sy1 < 0 && sy2 < 0 || sy1 > ydim && sy2 > ydim) {
            return;
        }

        byte[][] palookup = parent.getPaletteManager().getPalookupBuffer();
        col = palookup[0][col] & 0xFF;

        Gdx.gl.glDisable(GL_CULL_FACE);
        Gdx.gl.glDisable(GL_DEPTH_TEST);

        setType(GL20.GL_LINES);
        switchOrphoShader(Shader.BitmapShader);
        switchTexture(lineTile);
        Palette curpalette = parent.getPaletteManager().getCurrentPalette();
        setColor(curpalette.getRed(col) / 255.0f, curpalette.getGreen(col) / 255.0f, curpalette.getBlue(col) / 255.0f, 1.0f);
        disableBlending();
        if (idx >= 256) {
            flush();
        }

        float color = this.color;
        int idx = this.idx;
        float[] vertices = this.vertices;

        vertices[idx + 0] = sx1;
        vertices[idx + 1] = sy1;
        vertices[idx + 2] = color;

        vertices[idx + 3] = sx2;
        vertices[idx + 4] = sy2;
        vertices[idx + 5] = color;

        this.idx = idx + 6;
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                             int cy1, int cx2, int cy2) {
        if (!Gameutils.isValidTile(picnum)) {
            return;
        }
        if ((cx1 > cx2) || (cy1 > cy2)) {
            return;
        }
        if (z <= 16) {
            return;
        }

        if (parent.getConfig().isUseModels() && parent.defs != null && parent.defs.mdInfo.getHudInfo(picnum, dastat) != null
                && parent.defs.mdInfo.getHudInfo(picnum, dastat).angadd != 0) {
            Tile2model entry = parent.defs != null ? parent.defs.mdInfo.getParams(picnum) : null;
            if (entry != null && entry.model != null && entry.framenum >= 0) {
                if (dorotatesprite3d(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2)) {
                    return;
                }
            }
        }

        if (parent.getTile(picnum).getType() != AnimType.NONE) {
            picnum += parent.animateoffs(picnum, 0);
        }

        ArtEntry pic = parent.getTile(picnum);
        if (!pic.hasSize()) {
            return;
        }

        int method = 0;
        if ((dastat & 64) == 0) {
            method = 1;
            if ((dastat & 1) != 0) {
                if ((dastat & 32) == 0) {
                    method = 2;
                } else {
                    method = 3;
                }
            }
        } else {
            method |= 256; // non-transparent 255 color
        }
        method |= 4; // Use OpenGL clamping - dorotatesprite never repeats

        int xsiz = pic.getWidth();
        int ysiz = pic.getHeight();

        int xoff = 0, yoff = 0;
        if ((dastat & 16) == 0) {
            xoff = pic.getOffsetX() + (xsiz >> 1);
            yoff = pic.getOffsetY() + (ysiz >> 1);
        }

        if ((dastat & 4) != 0) {
            yoff = ysiz - yoff;
        }

        PaletteManager paletteManager = parent.getPaletteManager();
        if (!paletteManager.isValidPalette(dapalnum & 0xFF)) {
            dapalnum = 0;
        }

        parent.setgotpic(picnum);
        GLTile pth = parent.textureCache.get(parent.getTexFormat(), pic, dapalnum, 0, method);
        if (pth == null) {
            return;
        }

        pth.bind();
        if (((method & 3) == 0)) {
            disableBlending();
        } else {
            enableBlending();
        }

        float alpha = 1.0f;
        switch (method & 3) {
            case 2:
                alpha = parent.TRANSLUSCENT1;
                break;
            case 3:
                alpha = parent.TRANSLUSCENT2;
                break;
        }

        Gdx.gl.glDisable(GL_CULL_FACE);
        Gdx.gl.glDisable(GL_DEPTH_TEST);

        Shader shader = pth.getPixelFormat() != PixelFormat.Pal8 ? Shader.RGBWorldShader : Shader.IndexedWorldShader;

        switchOrphoShader(shader);
        setType(GL20.GL_TRIANGLES);
        setViewport(cx1, cy1, cx2, cy2);
        if (pth.getPixelFormat() != PixelFormat.Pal8) {
            int numshades = paletteManager.getShadeCount();
            float shade = (numshades - min(max(dashade, 0), numshades)) / (float) numshades;
            float r = shade, g = shade, b = shade;

            if (pth.isHighTile()) {
                if (parent.defs != null && parent.defs.texInfo != null) {
                    if (pth.getPal() != dapalnum) {
                        // apply tinting for replaced textures

                        Color p = parent.defs.texInfo.getTints(dapalnum);
                        r *= p.r / 255.0f;
                        g *= p.g / 255.0f;
                        b *= p.b / 255.0f;
                    }

                    Color pdetail = parent.defs.texInfo.getTints(MAXPALOOKUPS - 1);
                    if (pdetail.r != 255 || pdetail.g != 255 || pdetail.b != 255) {
                        r *= pdetail.r / 255.0f;
                        g *= pdetail.g / 255.0f;
                        b *= pdetail.b / 255.0f;
                    }
                }
            }

            setColor(r, g, b, alpha);
        } else {
            switchTextureParams(dapalnum, dashade, alpha, blendingDisabled || (method & 256) != 0);
        }

        draw(pth, sx, sy, xsiz, ysiz, xoff, yoff, a, z, dastat, cx1, cy1, cx2, cy2);
    }

    protected Shader getShader(GLTile glTile) {
        switch(glTile.getPixelFormat()) {
            case Pal8:
                return Shader.IndexedWorldShader;
            case Bitmap:
                return Shader.BitmapShader;
        }

        return Shader.RGBWorldShader;
    }

    public boolean dorotatesprite3d(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                                    int cy1, int cx2, int cy2) {

        Hudtyp hudInfo;
        if (parent.defs == null
                || ((hudInfo = parent.defs.mdInfo.getHudInfo(picnum, dastat)) != null && (hudInfo.flags & 1) != 0)) {
            return true; // "HIDE" is specified in DEF
        }

        if (isDrawing()) {
            end();
        }

        float yaw = (float) AngleToRadians(parent.globalang);
        float gcosang = MathUtils.cos(yaw);
        float gsinang = MathUtils.sin(yaw);

        float x1 = hudInfo.xadd;
        float y1 = hudInfo.yadd;
        float z1 = hudInfo.zadd;

        if ((hudInfo.flags & 2) == 0) { // "NOBOB" is specified in DEF
            float fx = sx * (1.0f / 65536.0f);
            float fy = sy * (1.0f / 65536.0f);

            if ((dastat & 16) != 0) {
                ArtEntry pic = parent.getTile(picnum);

                int xsiz = pic.getWidth();
                int ysiz = pic.getHeight();
                int xoff = pic.getOffsetX() + (xsiz >> 1);
                int yoff = pic.getOffsetY() + (ysiz >> 1);

                float d = z / (65536.0f * 16384.0f);
                float cosang, sinang;
                float cosang2 = cosang = EngineUtils.cos(a) * d;
                float sinang2 = sinang = EngineUtils.sin(a) * d;
                if ((dastat & 2) != 0 || ((dastat & 8) == 0)) { // Don't aspect unscaled perms
                    d = parent.xyaspect / 65536.0f;
                    cosang2 *= d;
                    sinang2 *= d;
                }

                fx += -xoff * cosang2 + yoff * sinang2;
                fy += -xoff * sinang - yoff * cosang;
            }

            if ((dastat & 2) == 0) {
                x1 += fx / (xdim << 15) - 1.0f; // -1: left of screen, +1: right of screen
                y1 += fy / (ydim << 15) - 1.0f; // -1: top of screen, +1: bottom of screen
            } else {
                x1 += fx / 160.0f - 1.0f; // -1: left of screen, +1: right of screen
                y1 += fy / 100.0f - 1.0f; // -1: top of screen, +1: bottom of screen
            }
        }

        if ((dastat & 4) != 0) {
            x1 = -x1;
            y1 = -y1;
        }

        if (hudsprite == null) {
            hudsprite = new Sprite();
        }
        hudsprite.reset((byte) 0);

        hudsprite.setAng((short) (hudInfo.angadd + parent.globalang));
        hudsprite.setXrepeat(32);
        hudsprite.setYrepeat(32);

        hudsprite.setX((int) ((gcosang * z1 - gsinang * x1) * 800.0f + parent.globalposx));
        hudsprite.setY((int) ((gsinang * z1 + gcosang * x1) * 800.0f + parent.globalposy));
        hudsprite.setZ((int) (parent.globalposz + y1 * 16384.0f * 0.8f));

        hudsprite.setPicnum((short) picnum);
        hudsprite.setShade((byte) dashade);
        hudsprite.setPal((short) dapalnum);
        hudsprite.setOwner(boardService.getSpriteCount());
        hudsprite.setCstat((short) ((dastat & 1) + ((dastat & 32) << 4) + (dastat & 4) << 1));

        if ((dastat & 10) == 2) {
            parent.resizeglcheck();
        } else {
            parent.set2dview();
        }

        if ((hudInfo.flags & 8) != 0) // NODEPTH flag
        {
            Gdx.gl.glDisable(GL_DEPTH_TEST);
        } else {
            Gdx.gl.glEnable(GL_DEPTH_TEST);
            Gdx.gl.glClear(GL_DEPTH_BUFFER_BIT);
        }

        BuildCamera cam = parent.cam;

        float aspect = xdim / (float) (ydim);
        float f = 1.0f;
        if (hudInfo.fov != -1) {
            f = hudInfo.fov / 420.0f;
        }
        cam.projection.setToProjection(cam.near, cam.far, 90 * f, aspect);
        cam.view.setToLookAt(cam.direction.set(gcosang, gsinang, 0), cam.up.set(0, 0, (dastat & 4) != 0 ? 1 : -1)).translate(-cam.position.x, -cam.position.y, -cam.position.z);
        cam.combined.set(cam.projection);
        Matrix4.mul(cam.combined.val, cam.view.val);

        return parent.mdR.mddraw(parent.modelManager.getModel(picnum, dapalnum), hudsprite);
    }

    public void draw(GLTile tex, int sx, int sy, int sizx, int sizy, int xoffset, int yoffset, int angle, int z,
                     int dastat, int cx1, int cy1, int cx2, int cy2) {
        this.draw(tex, sx, sy, sizx, sizy, xoffset, yoffset, 0.0f, 0.0f, sizx, sizy, angle, z, dastat, cx1, cy1, cx2,
                cy2);
    }

    @Override
    public void nextpage() {
        if (isDrawing()) {
            end();
        }
    }

    protected void drawoverheadline(int w, int cposx, int cposy, float cos, float sin, int col) {
        if (col < 0) {
            return;
        }

        Wall wal = boardService.getWall(w);

        int ox = cposx - mapSettings.getWallX(w);
        int oy = cposy - mapSettings.getWallY(w);
        float x1 = ox * cos - oy * sin + xdim * 2048;
        float y1 = ox * sin + oy * cos + ydim * 2048;

        ox = cposx - mapSettings.getWallX(wal.getPoint2());
        oy = cposy - mapSettings.getWallY(wal.getPoint2());

        float x2 = ox * cos - oy * sin + xdim * 2048;
        float y2 = ox * sin + oy * cos + ydim * 2048;

        drawline256((int) x1, (int) y1, (int) x2, (int) y2, col);
    }

    @Override
    public void drawoverheadmap(BoardService service, int cposx, int cposy, int czoom, short cang) {
        float cos = (float) Math.cos((512 - cang) * buildAngleToRadians) * czoom / 4.0f;
        float sin = (float) Math.sin((512 - cang) * buildAngleToRadians) * czoom / 4.0f;

        for (int i = 0; i < boardService.getSectorCount(); i++) {
            if ((!mapSettings.isFullMap() && !show2dsector.getBit(i))
                    || !boardService.isValidSector(i)) {
                continue;
            }

            Sector sec = boardService.getSector(i);
            if (!boardService.isValidWall(sec.getWallptr()) || sec.getWallnum() < 3) {
                continue;
            }

            int walnum = sec.getWallptr();
            for (int j = 0; j < sec.getWallnum(); j++, walnum++) {
                if (!boardService.isValidWall(walnum) || !boardService.isValidWall(boardService.getWall(walnum).getPoint2())) {
                    continue;
                }

                Wall wal = boardService.getWall(walnum);
                if (mapSettings.isShowRedWalls() && boardService.isValidWall(wal.getNextwall())) {
                    if (boardService.isValidSector(wal.getNextsector())) {
                        if (mapSettings.isWallVisible(walnum, i)) {
                            drawoverheadline(walnum, cposx, cposy, cos, sin, mapSettings.getWallColor(walnum, i));
                        }
                    }
                }

                if (wal.getNextwall() >= 0) {
                    continue;
                }

                ArtEntry pic = parent.getTile(wal.getPicnum());
                if (!pic.hasSize()) {
                    continue;
                }

                drawoverheadline(walnum, cposx, cposy, cos, sin, mapSettings.getWallColor(walnum, i));
            }
        }

        // Draw sprites
        if (mapSettings.isShowSprites(MapView.Lines)) {
            for (int i = 0; i < boardService.getSectorCount(); i++) {
                if (!mapSettings.isFullMap() && !show2dsector.getBit(i)) {
                    continue;
                }

                for (ListNode<Sprite> node = boardService.getSectNode(i); node != null; node = node.getNext()) {
                    int j = node.getIndex();
                    Sprite spr = node.get();

                    if ((spr.getCstat() & 0x8000) != 0 || spr.getXrepeat() == 0 || spr.getYrepeat() == 0
                            || !mapSettings.isSpriteVisible(MapView.Lines, j)) {
                        continue;
                    }

                    switch (spr.getCstat() & 48) {
                        case 0:
                            if (((parent.gotsector[i >> 3] & (1 << (i & 7))) > 0) && (czoom > 96)) {
                                int ox = cposx - mapSettings.getSpriteX(j);
                                int oy = cposy - mapSettings.getSpriteY(j);
                                float dx = ox * cos - oy * sin;
                                float dy = ox * sin + oy * cos;
                                int daang = (spr.getAng() - cang) & 0x7FF;
                                int nZoom = czoom * spr.getYrepeat();
                                int sx = (int) (dx + xdim * 2048);
                                int sy = (int) (dy + ydim * 2048);

                                rotatesprite(sx * 16, sy * 16, nZoom, (short) daang, spr.getPicnum(), spr.getShade(), spr.getPal(),
                                        (spr.getCstat() & 2) >> 1, parent.wx1, parent.wy1, parent.wx2, parent.wy2);
                            }
                            break;
                        case 16: {
                            ArtEntry pic = parent.getTile(spr.getPicnum());
                            int x1 = mapSettings.getSpriteX(j);
                            int y1 = mapSettings.getSpriteY(j);
                            byte xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                            if ((spr.getCstat() & 4) > 0) {
                                xoff = (byte) -xoff;
                            }

                            int dax = EngineUtils.cos(spr.getAng() - 512) * spr.getXrepeat();
                            int day = EngineUtils.sin(spr.getAng() - 512) * spr.getXrepeat();
                            int k = (pic.getWidth() >> 1) + xoff;
                            x1 -= mulscale(dax, k, 16);
                            int x2 = x1 + mulscale(dax, pic.getWidth(), 16);
                            y1 -= mulscale(day, k, 16);
                            int y2 = y1 + mulscale(day, pic.getWidth(), 16);

                            int ox = cposx - x1;
                            int oy = cposy - y1;
                            x1 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y1 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            ox = cposx - x2;
                            oy = cposy - y2;
                            x2 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y2 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            int col = mapSettings.getSpriteColor(j);
                            if (col < 0) {
                                break;
                            }

                            drawline256(x1, y1, x2, y2, col);
                        }
                        break;
                        case 32: {
                            ArtEntry pic = parent.getTile(spr.getPicnum());
                            byte xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                            byte yoff = (byte) (pic.getOffsetY() + spr.getYoffset());
                            if ((spr.getCstat() & 4) > 0) {
                                xoff = (byte) -xoff;
                            }
                            if ((spr.getCstat() & 8) > 0) {
                                yoff = (byte) -yoff;
                            }

                            int cosang = EngineUtils.cos(spr.getAng());
                            int sinang = EngineUtils.sin(spr.getAng());

                            int dax = ((pic.getWidth() >> 1) + xoff) * spr.getXrepeat();
                            int day = ((pic.getHeight() >> 1) + yoff) * spr.getYrepeat();
                            int x1 = mapSettings.getSpriteX(j) + dmulscale(sinang, dax, cosang, day, 16);
                            int y1 = mapSettings.getSpriteY(j) + dmulscale(sinang, day, -cosang, dax, 16);
                            int l = pic.getWidth() * spr.getXrepeat();
                            int x2 = x1 - mulscale(sinang, l, 16);
                            int y2 = y1 + mulscale(cosang, l, 16);
                            l = pic.getHeight() * spr.getYrepeat();
                            int k = -mulscale(cosang, l, 16);
                            int x3 = x2 + k;
                            int x4 = x1 + k;
                            k = -mulscale(sinang, l, 16);
                            int y3 = y2 + k;
                            int y4 = y1 + k;

                            int ox = cposx - x1;
                            int oy = cposy - y1;
                            x1 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y1 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            ox = cposx - x2;
                            oy = cposy - y2;
                            x2 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y2 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            ox = cposx - x3;
                            oy = cposy - y3;
                            x3 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y3 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            ox = cposx - x4;
                            oy = cposy - y4;
                            x4 = (int) (ox * cos - oy * sin) + (xdim << 11);
                            y4 = (int) (ox * sin + oy * cos) + (ydim << 11);

                            int col = mapSettings.getSpriteColor(j);
                            if (col < 0) {
                                break;
                            }

                            drawline256(x1, y1, x2, y2, col);
                            drawline256(x2, y2, x3, y3, col);
                            drawline256(x3, y3, x4, y4, col);
                            drawline256(x4, y4, x1, y1, col);
                        }
                        break;
                    }
                }
            }
        }

        // draw player
        for (int i = connecthead; i >= 0; i = connectpoint2[i]) {
            int spr = mapSettings.getPlayerSprite(i);
            if (spr == -1 || !boardService.isValidSector(boardService.getSprite(spr).getSectnum())) {
                continue;
            }

            Sprite pPlayer = boardService.getSprite(spr);
            int ox = cposx - mapSettings.getSpriteX(spr);
            int oy = cposy - mapSettings.getSpriteY(spr);

            float dx = ox * cos - oy * sin;
            float dy = ox * sin + oy * cos;

            int dang = (pPlayer.getAng() - cang) & 0x7FF;
            int viewindex = mapSettings.getViewPlayer();
            if (i == viewindex && !mapSettings.isScrollMode()) {
                dx = 0;
                dy = viewindex ^ i;
                dang = 0;
            }

            if (i == viewindex || mapSettings.isShowAllPlayers()) {
                int picnum = mapSettings.getPlayerPicnum(i);
                if (picnum == -1) { // draw it with lines
//					ox = (EngineUtils.sin((pPlayer.ang + 512) & 2047) >> 7);
//					oy = (EngineUtils.sin((pPlayer.ang) & 2047) >> 7);
                    int x2 = 0;
                    int y2 = -(mapSettings.getPlayerZoom(i, czoom) << 1);

                    int col = mapSettings.getSpriteColor(spr);
                    if (col < 0) {
                        continue;
                    }

                    int sx = (int) dx;
                    int sy = (int) dy;

                    drawline256(sx - x2 + (xdim << 11), sy - y2 + (ydim << 11), sx + x2 + (xdim << 11),
                            sy + y2 + (ydim << 11), col);
                    drawline256(sx - y2 + (xdim << 11), sy + x2 + (ydim << 11), sx + x2 + (xdim << 11),
                            sy + y2 + (ydim << 11), col);
                    drawline256(sx + y2 + (xdim << 11), sy - x2 + (ydim << 11), sx + x2 + (xdim << 11),
                            sy + y2 + (ydim << 11), col);
                } else {
                    int nZoom = mapSettings.getPlayerZoom(i, czoom);

                    int sx = (int) (dx + xdim * 2048);
                    int sy = (int) (dy + ydim * 2048);

                    rotatesprite(sx * 16, sy * 16, nZoom, (short) dang, picnum, pPlayer.getShade(), pPlayer.getPal(),
                            (pPlayer.getCstat() & 2) >> 1, parent.wx1, parent.wy1, parent.wx2, parent.wy2);
                }
            }
        }
    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {
        parent.beforedrawrooms = 0;

        if (isDrawing()) {
            end();
        }

        Matrix4 tmpMat = parent.transform; // Projection matrix
//		tmpMat.setToOrtho(xdim / 2, (-xdim / 2), -(ydim / 2), ydim / 2, 0, 1);
//		tmpMat.scale(zoome / 32.0f, zoome / 32.0f, 0);
//		manager.projection(tmpMat).view(parent.identity);
//		setViewport(0, 0, 0, 0);

        BuildCamera cam = parent.cam;
        cam.projection.setToOrtho(xdim / 2.0f, (-xdim / 2.0f), -(ydim / 2.0f), ydim / 2.0f, 0, 1);
        cam.projection.scale(zoome / 32.0f, zoome / 32.0f, 0);
        cam.view.set(parent.identity);
        cam.combined.set(cam.projection);
        Matrix4.mul(cam.combined.val, cam.view.val);

        int showSprites = mapSettings.isShowFloorSprites() ? 1 : 0;
        showSprites |= (mapSettings.isShowSprites(MapView.Polygons) ? 2 : 0);

        for (int s = 0; s < boardService.getSectorCount(); s++) {
            Sector sec = boardService.getSector(s);

            if (mapSettings.isFullMap() || show2dsector.getBit(s)) {
                if ((showSprites & 1) != 0) {
                    // Collect floor sprites to draw
                    for (ListNode<Sprite> node = boardService.getSectNode(s); node != null; node = node.getNext()) {
                        int i = node.getIndex();
                        if ((node.get().getCstat() & 48) == 32) {
                            if ((boardService.getSprite(i).getCstat() & (64 + 8)) == (64 + 8)
                                    || !mapSettings.isSpriteVisible(MapView.Polygons, i)) {
                                continue;
                            }

                            parent.addRenderedSprite(i);
                        }
                    }
                }

                if ((showSprites & 2) != 0) {
                    for (ListNode<Sprite> node = boardService.getSectNode(s); node != null; node = node.getNext()) {
                        int i = node.getIndex();
                        if ((node.get().getCstat() & 48) == 32) {
                            continue;
                        }

                        if (show2dsprite.getBit(i)) {
                            if (!mapSettings.isSpriteVisible(MapView.Polygons, i)) {
                                continue;
                            }

                            if (i == mapSettings.getPlayerSprite(mapSettings.getViewPlayer())) {
                                continue;
                            }

                            parent.addRenderedSprite(i);
                        }
                    }
                }

                parent.gotsector[s >> 3] |= pow2char[s & 7];
                if (sec.isParallaxFloor()) {
                    continue;
                }
                parent.globalpal = sec.getFloorpal();

                int globalpicnum = sec.getFloorpicnum();
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }
                parent.setgotpic(globalpicnum);
                ArtEntry pic = parent.getTile(globalpicnum);

                if (!pic.hasSize()) {
                    continue;
                }

                if (pic.getType() != AnimType.NONE) {
                    globalpicnum += parent.animateoffs(globalpicnum, s);
                    pic = parent.getTile(globalpicnum);
                }

                parent.globalshade = max(min(sec.getFloorshade(), parent.getPaletteManager().getShadeCount() - 1), 0);

                GLSurface flor = parent.world.getFloor(s);
                if (flor != null) {
                    tmpMat.setToRotation(0, 0, 1, (512 - ang) * buildAngleToDegrees);
                    tmpMat.translate(-dax / parent.cam.xscale, -day / parent.cam.xscale,
                            -boardService.getSector(s).getFloorz() / parent.cam.yscale);
                    parent.drawSurf(flor, 0, tmpMat, null);
                }
            }
        }

        if (showSprites != 0) {
            float cos = (float) Math.cos((512 - ang) * buildAngleToRadians) * zoome / 4.0f;
            float sin = (float) Math.sin((512 - ang) * buildAngleToRadians) * zoome / 4.0f;

            // Sort sprite list
            int gap = 1;
            Pool<TSprite> tsprite = parent.getRenderedSprites();
            int sortnum = parent.getRenderedSprites().getSize();
            while (gap < sortnum) {
                gap = (gap << 1) + 1;
            }
            for (gap >>= 1; gap > 0; gap >>= 1) {
                for (int i = 0; i < sortnum - gap; i++) {
                    for (int j = i; j >= 0; j -= gap) {
                        TSprite tspr1 = tsprite.get(j);
                        TSprite tspr2 = tsprite.get(j + gap);
                        if (boardService.getSprite(tspr1.getOwner()).getZ() <= boardService.getSprite(tspr2.getOwner()).getZ()) {
                            break;
                        }

                        short owner1 = tspr1.getOwner();
                        tspr1.setOwner(tspr2.getOwner());
                        tspr2.setOwner(owner1);
                    }
                }
            }

            for (int s = sortnum - 1; s >= 0; s--) {
                int j = tsprite.get(s).getOwner();
                Sprite spr = boardService.getSprite(j);
                if ((spr.getCstat() & 32768) == 0) {
                    if (spr.getPicnum() >= MAXTILES) {
                        spr.setPicnum(0);
                    }

                    int ox = dax - mapSettings.getSpriteX(j);
                    int oy = day - mapSettings.getSpriteY(j);
                    float dx = ox * cos - oy * sin;
                    float dy = ox * sin + oy * cos;
                    int daang = (spr.getAng() - ang) & 0x7FF;
                    int nZoom = zoome * spr.getYrepeat();
                    int sx = (int) (dx + xdim * 2048);
                    int sy = (int) (dy + ydim * 2048);

                    rotatesprite(sx * 16, sy * 16, nZoom, (short) daang, mapSettings.getSpritePicnum(j), spr.getShade(), spr.getPal(),
                            ((spr.getCstat() & 2) >> 1) | 8, parent.wx1, parent.wy1, parent.wx2, parent.wy2);
                }
            }
        }

        flush();
        manager.unbind();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        projectionMatrix.setToOrtho(0, width - 1, height - 1, 0, 0, 1);
    }

    protected void begin(Shader shader) {
        if (drawing) {
            throw new IllegalStateException("GdxBatch.end must be called before begin.");
        }

        Gdx.gl20.glDepthMask(false);

        manager.bind(shader);
        setupMatrices();

        drawing = true;
    }

    public void end() {
        if (!drawing) {
            throw new IllegalStateException("GdxBatch.begin must be called before end.");
        }
        if (idx > 0) {
            flush();
        }

        lastTexture = null;
        cx1 = 0;
        cy1 = 0;
        cx2 = 0;
        cy2 = 0;
        lastType = -1;

        drawing = false;

        GL20 gl = parent.gl;
        gl.glDepthMask(true);
        if (isBlendingEnabled()) {
            gl.glDisable(GL20.GL_BLEND);
        }

        manager.unbind();
    }

    protected void flush() {
        if (idx == 0) {
            return;
        }

        int count;
        Mesh mesh;

        lastTexture.bind();
        if (lastType == GL20.GL_LINES) {
            int spritesInBatch = idx / 6;
            count = spritesInBatch * 2;

            mesh = this.linesMesh;
            mesh.setVertices(vertices, 0, idx);
        } else {
            int spritesInBatch = idx / 20;
            count = spritesInBatch * 6;

            mesh = this.mesh;
            mesh.setVertices(vertices, 0, idx);
            mesh.getIndicesBuffer(true).position(0);
            mesh.getIndicesBuffer(true).limit(count);
        }

        if (blendingDisabled) {
            Gdx.gl20.glDisable(GL20.GL_BLEND);
        } else {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
        }

        manager.color(1.0f, 1.0f, 1.0f, 1.0f);
        manager.textureTransform(parent.texture_transform.idt(), 0);
        mesh.render(manager.getProgram(), lastType, 0, count);
        idx = 0;
    }

    public void draw(GLTile tex, int sx, int sy, int sizx, int sizy, int xoffset, int yoffset, float srcX, float srcY,
                     float srcWidth, float srcHeight, int angle, int z, int dastat, int cx1, int cy1, int cx2, int cy2) {
        if (!drawing) {
            throw new IllegalStateException("GdxBatch.begin must be called before draw.");
        }

        switchTexture(tex);
        if (idx == vertices.length) {
            flush();
        }

        int ourxyaspect = parent.xyaspect;
        if ((dastat & 2) == 0) {
            if ((dastat & 1024) == 0 && 4 * ydim <= 3 * xdim) {
                ourxyaspect = (10 << 16) / 12;
            }
        } else {
            // dastat&2: Auto window size scaling
            int oxdim = xdim, zoomsc;
            int xdim = oxdim; // SHADOWS global

            int ouryxaspect = parent.yxaspect;
            ourxyaspect = parent.xyaspect;

            // screen center to s[xy], 320<<16 coords.
            int normxofs = sx - (320 << 15), normyofs = sy - (200 << 15);
            if ((dastat & 1024) == 0 && 4 * ydim <= 3 * xdim) {
                xdim = (4 * ydim) / 3;

                ouryxaspect = (12 << 16) / 10;
                ourxyaspect = (10 << 16) / 12;
            }

            // nasty hacks go here
            if ((dastat & 8) == 0) {
                int twice_midcx = (cx1 + cx2) + 2;

                // screen x center to sx1, scaled to viewport
                int scaledxofs = scale(normxofs, scale(parent.xdimen, xdim, oxdim), 320);
                int xbord = 0;
                if ((dastat & (256 | 512)) != 0) {
                    xbord = scale(oxdim - xdim, twice_midcx, oxdim);
                    if ((dastat & 512) == 0) {
                        xbord = -xbord;
                    }
                }

                sx = ((twice_midcx + xbord) << 15) + scaledxofs;
                zoomsc = parent.xdimenscale;
                sy = (((cy1 + cy2) + 2) << 15) + mulscale(normyofs, zoomsc, 16);
            } else {
                // If not clipping to startmosts, & auto-scaling on, as a
                // hard-coded bonus, scale to full screen instead
                sx = (xdim << 15) + scale(normxofs, xdim, 320);
                if ((dastat & 512) != 0) {
                    sx += (oxdim - xdim) << 16;
                } else if ((dastat & 256) == 0) {
                    sx += (oxdim - xdim) << 15;
                }

                zoomsc = scale(xdim, ouryxaspect, 320);
                sy = (ydim << 15) + mulscale(normyofs, zoomsc, 16);
            }

            z = mulscale(z, zoomsc, 16);
        }

        final float aspectFix = ((dastat & 2) != 0) || ((dastat & 8) == 0) ? ourxyaspect / 65536.0f : 1.0f;
        final float scale = z / 65536.0f;

        final float xoffs = xoffset * scale;
        final float yoffs = yoffset * scale;
        final float width = scale * sizx;
        final float height = scale * sizy;

        float[] vertices = this.vertices;
        final float OriginX = sx / 65536.0f;
        final float OriginY = sy / 65536.0f;
        float x1, y1, x2, y2, x3, y3, x4, y4;

        // rotate
        if (angle != 0) {
            final float rotation = 360.0f * angle / 2048.0f;
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = OriginX + (sin * yoffs - cos * xoffs) * aspectFix;
            y1 = OriginY - xoffs * sin - yoffs * cos;

            x4 = x1 + width * cos * aspectFix;
            y4 = y1 + width * sin;

            x2 = x1 - height * sin * aspectFix;
            y2 = y1 + height * cos;

            x3 = x2 + (x4 - x1);
            y3 = y2 + (y4 - y1);
        } else {
            x1 = x2 = OriginX - xoffs * aspectFix;
            y1 = y4 = OriginY - yoffs;

            x3 = x4 = x1 + width * aspectFix;
            y2 = y3 = y1 + height;
        }

        if ((dastat & 8) == 0) {
            float yaspect = parent.windowy2 / (float) ydim;
            y1 *= yaspect;
            y2 *= yaspect;
            y3 *= yaspect;
            y4 *= yaspect;
        }

        if (tex.isHighTile()) {
            srcWidth = tex.getWidth();

//			srcHeight = sizy;
//			int yy = 1;
//			for (; yy < sizy; yy += yy);
//			invTexHeight = 1.0f / yy;

//			srcWidth = tex.getWidth();
//			for (sizy = 1; sizy < tex.getHeight(); sizy += sizy);
//			srcHeight = sizy;
//			invTexHeight = 1.0f / sizy;

            for (sizy = 1; sizy < tex.getHeight(); sizy += sizy) {
                ;
            }
            float scaley = (float) sizy / tex.getHeight();
            srcHeight = tex.getHeight() / scaley;
        }

        float v, u = srcX * invTexWidth;
        float v2, u2 = (srcX + srcWidth) * invTexWidth;
        if ((dastat & 4) == 0) {
            v = srcY * invTexHeight;
            v2 = (srcY + srcHeight) * invTexHeight;
        } else {
            v = (srcY + srcHeight) * invTexHeight;
            v2 = srcY * invTexHeight;
        }

        if (tex.isHighTile() && ((tex.getHiresXScale() != 1.0f) || (tex.getHiresYScale() != 1.0f))) {
            u *= tex.getHiresXScale();
            v *= tex.getHiresYScale();
            u2 *= tex.getHiresXScale();
            v2 *= tex.getHiresYScale();
        }

        // x2,y2-----x3,y3
        //   |         |
        // x1,y1-----x4,y4

        final float color = this.color;

        addVertex(x1, y1, color, u, v);
        addVertex(x2, y2, color, u, v2);
        addVertex(x3, y3, color, u2, v2);
        addVertex(x4, y4, color, u2, v);
    }

    protected void switchTextureParams(int pal, int shade, float alpha, boolean drawLastIndex) {
        IndexedShader shader = (IndexedShader) manager.getProgram();
        if (shader.getDrawLastIndex() == drawLastIndex && shader.getPal() == pal && shader.getShade() == shade
                && shader.getTransparent() == alpha) {
            return;
        }

        flush();
        manager.textureParams8(pal, shade, alpha, drawLastIndex);
    }

    protected void switchTexture(GLTile texture) {
        if (texture == lastTexture) {
            return;
        }

        flush();
        lastTexture = texture;
        invTexWidth = 1.0f / texture.getWidth();
        invTexHeight = 1.0f / texture.getHeight();

        manager.textureSize(texture.getWidth(), texture.getHeight());
        manager.paletteFiltered(parent.getConfig().getPaletteFiltered());
        manager.softShading(parent.getConfig().getSoftShading());
    }

    protected void setColor(float r, float g, float b, float a) {
        int intBits = (int) (255 * a) << 24 | (int) (255 * b) << 16 | (int) (255 * g) << 8 | (int) (255 * r);
        color = NumberUtils.intToFloatColor(intBits);
    }

    protected boolean isBlendingEnabled() {
        return !blendingDisabled;
    }

    public boolean isDrawing() {
        return drawing;
    }

    protected void disableBlending() {
        if (blendingDisabled) {
            return;
        }
        flush();
        blendingDisabled = true;
    }

    protected void enableBlending() {
        if (!blendingDisabled) {
            return;
        }
        flush();
        blendingDisabled = false;
    }

    protected void switchOrphoShader(Shader shader) {
        if (!isDrawing()) {
            begin(shader);
        }

        if (shader == manager.getShader()) {
            return;
        }

        if (isDrawing()) {
            flush();
        }

        manager.bind(shader);
        setupMatrices();
    }

    protected void setupMatrices() {
        manager.mirror(false);
        manager.fog(false, 0, 0, 0, 0, 0);
        if (manager.getShader() != Shader.BitmapShader) {
            manager.projection(projectionMatrix).view(parent.identity);
            manager.transform(parent.identity);
            manager.viewport(0, 0, (int) ((xdim - 1) * parent.backBufferScale), (int) ((ydim - 1) * parent.backBufferScale));
            cx1 = cy1 = 0;
            cx2 = xdim - 1;
            cy2 = ydim - 1;
        } else {
            manager.projection(projectionMatrix);
        }
    }

    protected void setType(int type) {
        if (type == lastType) {
            return;
        }

        flush();
        lastType = type;
    }

    protected GLTile allocLineTile() {
        DummyTileData data = new DummyTileData(PixelFormat.Bitmap, 1, 1);
        ByteBuffer b = data.getPixels();
        b.put((byte) 255);
        b.rewind();
        return parent.textureCache.newTile(data, 0, TexFilter.NONE);
    }

    protected void setViewport(int cx1, int cy1, int cx2, int cy2) {
        if (cx1 == this.cx1 && cx2 == this.cx2 && cy1 == this.cy1 && cy2 == this.cy2) {
            return;
        }

        flush();

        manager.viewport((int) (cx1 * parent.backBufferScale), (int) (cy1 * parent.backBufferScale), (int) (cx2 * parent.backBufferScale), (int) (cy2 * parent.backBufferScale));

        this.cx1 = cx1;
        this.cx2 = cx2;
        this.cy1 = cy1;
        this.cy2 = cy2;
    }
}
