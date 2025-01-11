// This file is part of BuildGDX.
// Copyright (C) 2017-2021  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import ru.m210projects.Build.Board;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.*;
import ru.m210projects.Build.Render.GdxRender.Scanner.SectorScanner;
import ru.m210projects.Build.Render.GdxRender.Scanner.VisibleSector;
import ru.m210projects.Build.Render.GdxRender.Shaders.FadeShader;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager.Shader;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.GLSurface;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.ModelManager;
import ru.m210projects.Build.Render.ModelHandle.Voxel.GLVoxel;
import ru.m210projects.Build.Render.TextureHandle.*;
import ru.m210projects.Build.Render.TextureHandle.TextureManager.ExpandTexture;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.Types.ScreenFade;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Render.listeners.PaletteListener;
import ru.m210projects.Build.Render.listeners.PrecacheListener;
import ru.m210projects.Build.Render.listeners.TileListener;
import ru.m210projects.Build.Render.listeners.WorldListener;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Script.ModelsInfo.SpriteAnim;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static com.badlogic.gdx.graphics.GL20.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Render.ModelHandle.MDModel.MDAnimation.*;

public class GDXRenderer extends AbstractRenderer implements PaletteListener, WorldListener, TileListener, PrecacheListener {

//	TODO:
//  Skies panning
//  Tekwar skies bug
//	Hires detail, glow
//  Overheadmap sector visible check
//  Duke E2L2 model invisible

//	Sprite ZFighting
//	Blood drunk effect
//	Skyboxes
//  Shadow warrior sprite visible bug
//  Duke E2L7 wall vis bug (scanner bug)
//  Duke E4L11 wall vis bug (scanner bug)

    protected final float FULLVIS_BEGIN = (float) 2.9e30;
    protected final float FULLVIS_END = (float) 3.0e30;
    protected final GLTileArray skycache = new GLTileArray(MAXTILES);
    private final ArrayList<GLSurface> bunchfirst = new ArrayList<>();
    public RenderingType renderingType = RenderingType.Nothing;
    protected TextureManager textureCache;
    protected ModelManager modelManager;
    protected boolean isInited = false;
    protected GL20 gl;
    protected float defznear = 0.001f;
    protected float defzfar = 1.0f;
    protected float fov = 90;
    protected float fovFactor = 1.0f;
    protected float gtang = 0.0f;
    protected WorldMesh world;
    protected SectorScanner scanner;
    protected BuildCamera cam;
    protected SpriteRenderer sprR;
    protected GDXModelRenderer mdR;
    protected GDXOrtho orphoRen; // GdxOrphoRen
    protected DefScript defs;
    protected ShaderManager manager;
    protected boolean isUseIndexedTextures;
    protected Matrix4 transform = new Matrix4();
    protected Matrix3 texture_transform = new Matrix3();
    protected Matrix4 identity = new Matrix4();
    protected ArrayList<VisibleSector> sectors = new ArrayList<VisibleSector>();
    protected boolean[] mirrorTextures = new boolean[MAXTILES];
    protected int FOGDISTCONST = 48;
    private boolean clearStatus = false;
    private float glox1, gloy1, glox2, gloy2;
    private Mesh fadeMesh;

    private float ALPHA_CUT_DISABLE = 0.01f;

    public GDXRenderer(GameConfig config) {
        super(config);
        this.config.setVideoContext(new GDXVideoContext(this));
        this.manager = new ShaderManager();
        Arrays.fill(mirrorTextures, false);
        int[] mirrors = getMirrorTextures();
        if (mirrors != null) {
            for (int mirror : mirrors) {
                mirrorTextures[mirror] = true;
            }
        }
    }

    public PaletteManager getPaletteManager() {
        return paletteManager;
    }

    @Override
    public void init(Engine engine) {
        super.init(engine);

        try {
            this.textureCache = getTextureManager();
            this.paletteManager.setListener(this);
            this.boardService.setListener(this);
            this.tileManager.setTileListener(this);
            this.modelManager = new GDXModelManager(this);
            this.sprR = new SpriteRenderer(engine, this);
            this.mdR = new GDXModelRenderer(this);
            this.orphoRen = allocOrphoRenderer(engine);
            this.scanner = new SectorScanner(engine) {
                @Override
                protected Matrix4 getSpriteMatrix(Sprite tspr) {
                    ArtEntry pic = getTile(tspr.getPicnum());
                    return sprR.getMatrix(tspr, pic.getWidth(), pic.getHeight());
                }
            };
            this.setDefs(engine.getDefs());

            this.gl = Gdx.graphics.getGL20();
            GLInfo.init(gl);

            gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            gl.glPixelStorei(GL_PACK_ALIGNMENT, 1);

            this.cam = new BuildCamera(fov, xdim, ydim, 512, 8192);
            PaletteManager paletteManager = engine.getPaletteManager();
            this.manager.init(textureCache, paletteManager.getShadeCount());
            if (!this.manager.isInited()) {
                return;
            }

            this.fadeMesh = new Mesh(true, 3, 0, new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE)).setVertices(new float[]{-2.5f, 1.0f, 2.5f, 1.0f, 0.0f, -2.5f});
            this.textureCache.changePalette(paletteManager.getCurrentPalette().getBytes());

            Console.out.println("Polygdx renderer is initialized", OsdColor.GREEN);
            Console.out.println(Gdx.graphics.getGLVersion().getRendererString() + " " + gl.glGetString(GL_VERSION),
                    OsdColor.YELLOW);

            orphoRen.init();

            if (world != null && world.isInvalid()) {
                world = new WorldMesh(engine);
            }

            // init gl settings
            config.setGlfilter(config.getGlfilter());
            config.setPaletteEmulation(config.isPaletteEmulation());
            config.setUseHighTiles(config.isUseHighTiles());
            config.setDetailMapping(config.isDetailMapping());
            config.setGlowMapping(config.isGlowMapping());

            isInited = true;
        } catch (Throwable t) {
            isInited = false;
        }
    }

    @Override
    public void resize(int width, int height) {
        if ((width == xdim) && (height == ydim)) {
            return;
        }

        xdim = width;
        ydim = height;

        config.setgFov(config.getgFov());
        setview(0, 0, xdim - 1, ydim - 1);
        resizeglcheck();
    }

    @Override
    public int getWidth() {
        return xdim;
    }

    @Override
    public int getHeight() {
        return ydim;
    }

    @Override
    public void uninit() {
        System.err.println("uninit");
        paletteManager.setListener(PaletteListener.DUMMY_PALETTE_CHANGE_LISTENER);
        engine.getBoardService().setListener(WorldListener.DUMMY_LISTENER);
        engine.getTileManager().setTileListener(TileListener.DUMMY_LISTENER);
        isInited = false;
        if (world != null) {
            world.dispose();
        }
        orphoRen.uninit();
        manager.dispose();
        fadeMesh.dispose();
        texturesUninit();
        modelManager.dispose();
    }

    private void texturesUninit() {
        textureCache.uninit();
        for (int i = MAXTILES - 1; i >= 0; i--) {
            skycache.dispose(i);
        }
    }

    @Override
    public void drawrooms() {
        if (orphoRen.isDrawing()) {
            orphoRen.flush(); // #GDX 30.07.2024 was end()
        }

        // Temporaly code (Tekwar issue)
//		else if (!clearStatus) { // once at frame
//			gl.glClearColor(0.0f, 0.5f, 0.5f, 1);
//			gl.glClear(GL_COLOR_BUFFER_BIT);
//			clearStatus = true;
//		}
        gl.glClear(GL_DEPTH_BUFFER_BIT);

//		if (shape == null) {
//			shape = new ShapeRenderer();
//			shape.setProjectionMatrix(shape.getProjectionMatrix().setToOrtho(0, xdim, ydim, 0, -1, 1));
//		}
//		shape.begin(ShapeType.Line);

        gl.glDisable(GL_BLEND);
        gl.glEnable(GL_TEXTURE_2D);
        gl.glEnable(GL_DEPTH_TEST);

        gl.glDepthFunc(GL_LESS);
        gl.glDepthRangef(defznear, defzfar);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CW);
        resizeglcheck();

        cam.setPosition(globalposx, globalposy, globalposz);
        cam.setDirection(globalang, globalhoriz, inpreparemirror ? -gtang : gtang);
        cam.update(true);

        globalvisibility = visibility << 2;
        if (globalcursectnum >= boardService.getSectorCount()) {
            globalcursectnum -= boardService.getSectorCount();
        } else {
            int i = globalcursectnum;
            globalcursectnum = engine.updatesectorz(globalposx, globalposy, globalposz, globalcursectnum);
            if (globalcursectnum < 0) {
                globalcursectnum = i;
            }
        }

        sectors.clear();
        scanner.clear();
        scanner.setShowInvisibility(showinvisibility);
        scanner.process(sectors, cam, world, globalcursectnum, windowx2 + 1, windowy2 + 1);

        renderingType = RenderingType.Nothing;
        if (inpreparemirror) {
            gl.glCullFace(GL_FRONT);
        } else {
            gl.glCullFace(GL_BACK);
        }

        prerender(sectors);
        drawbackground();

        // пройтись по всем секторам с небом, создать лист отображаемых текстур
        // пройтись по листу, отрисовать все меши одной текстуры с записью в глубину
        // отрисовать скайбокс с совпадением по глубине
        // после отрисовки, отчистить буфер глубины и записать значения mirrors

        for (int i = inpreparemirror ? 1 : 0; i < sectors.size(); i++) {
            drawSector(sectors.get(i));
        }

//        spritesortcnt = scanner.getSpriteCount();
        tSpriteList = scanner.getSprites();

        manager.unbind();
    }

    @Override
    public void drawmasks() {
//		for (int i = inpreparemirror ? 1 : 0; i < sectors.size(); i++) {
//			VisibleSector sec = sectors.get(i);
//
//			// TODO: make stencil buffer
//			int sectnum = sec.index;
//			for(int s = 0; s < spritesortcnt; s++) {
//				if (tsprite[s] != null && tsprite[s].sectnum == sectnum) {
//					drawsprite(s);
//				}
//			}
//
//			drawSector(sec);
//			// TODO: clear stencil buffer
//		}
//
//		int[] maskwalls = scanner.getMaskwalls();
//		int maskwallcnt = scanner.getMaskwallCount();
//		while (maskwallcnt > 0)
//			drawmaskwall(--maskwallcnt);

        int[] maskwalls = scanner.getMaskwalls();
        int maskwallcnt = scanner.getMaskwallCount();

        RenderedSpriteList spriteList = getRenderedSprites();
        int spritesortcnt = spriteList.getSize();
        sprR.sort(spriteList.getArray(), spritesortcnt);

        while ((spritesortcnt > 0) && (maskwallcnt > 0)) { // While BOTH > 0
            int j = maskwalls[maskwallcnt - 1];

            if (!spritewallfront(spriteList.get(spritesortcnt - 1), j)) {
                spriteList.removeLast();
                drawsprite(--spritesortcnt);
            } else {
                // Check to see if any sprites behind the masked wall...
                for (int i = spritesortcnt - 2; i >= 0; i--) {
                    TSprite tsprite = spriteList.get(i);
                    if (!spritewallfront(tsprite, j)) {
                        drawsprite(i);
                        tsprite.setOwner(-1); //  tsprite[i] = null;
                    }
                }
                // finally safe to draw the masked wall
                drawmaskwall(--maskwallcnt);
            }
        }

        while (spritesortcnt != 0) {
            spritesortcnt--;
            TSprite tsprite = spriteList.get(spritesortcnt);
            if (tsprite.getOwner() != -1) {
                drawsprite(spritesortcnt);
            }
        }

        while (maskwallcnt > 0) {
            drawmaskwall(--maskwallcnt);
        }

        renderDrunkEffect();
        manager.unbind();
    }

    private void drawMask(int w) {
        gl.glDepthFunc(GL20.GL_LESS);
        gl.glDepthRangef(0.0001f, 0.99999f);

        drawSurf(world.getMaskedWall(w), 0, null, null);

        gl.glDepthFunc(GL20.GL_LESS);
        gl.glDepthRangef(defznear, defzfar);
    }

    protected void renderDrunkEffect() { // TODO: to shader
        /*
         * if (drunk) { set2dview();
         *
         * gl.glActiveTexture(GL_TEXTURE0); boolean hasShader = texshader != null &&
         * texshader.isBinded(); if (hasShader) texshader.end();
         *
         * if (frameTexture == null || framew != xdim || frameh != ydim) { int size = 1;
         * for (size = 1; size < Math.max(xdim, ydim); size <<= 1) ;
         *
         * if (frameTexture != null) frameTexture.dispose(); else frameTexture = new
         * GLTile(PixelFormat.Rgb, size, size);
         *
         * frameTexture.bind(); gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
         * frameTexture.getWidth(), frameTexture.getHeight(), 0, GL_RGB,
         * GL_UNSIGNED_BYTE, null); frameTexture.unsafeSetFilter(TextureFilter.Linear,
         * TextureFilter.Linear); framew = xdim; frameh = ydim; }
         *
         * textureCache.bind(frameTexture); gl.glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0,
         * 0, 0, 0, frameTexture.getWidth(), frameTexture.getHeight());
         *
         * gl.glDisable(GL_DEPTH_TEST); gl.glDisable(GL_CULL_FACE);
         *
         * float tiltang = (drunkIntensive * 360) / 2048f; float tilt = min(max(tiltang,
         * -MAXDRUNKANGLE), MAXDRUNKANGLE); float u = (float) xdim /
         * frameTexture.getWidth(); float v = (float) ydim / frameTexture.getHeight();
         *
         * int originX = xdim / 2; int originY = ydim / 2; float width = xdim * 1.05f;
         * float height = ydim * 1.05f;
         *
         * float xoffs = width / 2; float yoffs = height / 2;
         *
         * final float rotation = 360.0f * tiltang / 2048.0f; final float cos =
         * MathUtils.cosDeg(rotation); final float sin = MathUtils.sinDeg(rotation);
         *
         * float x1 = originX + (sin * yoffs - cos * xoffs); float y1 = originY - xoffs
         * * sin - yoffs * cos;
         *
         * float x4 = x1 + width * cos; float y4 = y1 + width * sin;
         *
         * float x2 = x1 - height * sin; float y2 = y1 + height * cos;
         *
         * float x3 = x2 + (x4 - x1); float y3 = y2 + (y4 - y1);
         *
         * orphoRen.begin(); // XXX // orphoRen.setColor(1, 1, 1, abs(tilt) / (2 *
         * MAXDRUNKANGLE)); // orphoRen.setTexture(frameTexture); //
         * orphoRen.addVertex(x1, ydim - y1, 0, 0); // orphoRen.addVertex(x2, ydim - y2,
         * 0, v); // orphoRen.addVertex(x3, ydim - y3, u, v); // orphoRen.addVertex(x4,
         * ydim - y4, u, 0); orphoRen.end();
         *
         * gl.glEnable(GL_DEPTH_TEST); gl.glEnable(GL_CULL_FACE);
         *
         * if (hasShader) texshader.begin(); }
         */
    }

    public void drawsprite(int i) {
        Sprite tspr = tSpriteList.get(i);
        if (tspr == null || tspr.getOwner() == -1) {
            return;
        }

        Spriteext sprext = defs.mapInfo.getSpriteInfo(tspr.getOwner());
        while (sprext == null || !sprext.isNotModel()) {
            renderingType = RenderingType.Model.setIndex(i);

            if (engine.getConfig().isUseModels()) {
                GLModel md = modelManager.getModel(tspr.getPicnum(), tspr.getPal());
                if (md != null) {
                    if (boardService.isValidSprite(tspr.getOwner())) {
                        if (mdR.mddraw(md, tspr)) {
                            return;
                        }
                        break; // else, render as flat sprite
                    }

                    if (mdR.mddraw(md, tspr)) {
                        return;
                    }
                    break; // else, render as flat sprite
                }
            }

            if (engine.getConfig().isUseVoxels()) {
                int picnum = tspr.getPicnum();
                if (getTile(picnum).getType() != AnimType.NONE) {
                    picnum += animateoffs(picnum, tspr.getOwner() + 32768);
                }

                int dist = (tspr.getX() - globalposx) * (tspr.getX() - globalposx)
                        + (tspr.getY() - globalposy) * (tspr.getY() - globalposy);
                if (dist < 48000L * 48000L) {
                    GLVoxel vox = (GLVoxel) modelManager.getVoxel(picnum);
                    if (vox != null) {
                        if ((tspr.getCstat() & 48) != 48) {
                            if (mdR.mddraw(vox, tspr)) {
                                return;
                            }
                            break; // else, render as flat sprite
                        }

                        if ((tspr.getCstat() & 48) == 48) {
                            mdR.mddraw(vox, tspr);
                            return;
                        }
                    }
                }
            }
            break;
        }

        renderingType = RenderingType.Sprite.setIndex(i);
        sprR.begin(cam);
        sprR.draw(tspr);
        sprR.end();
    }

    private void drawmaskwall(int i) {
        renderingType = RenderingType.MaskWall.setIndex(i);
        drawMask(scanner.getMaskwalls()[i]);
    }

    protected void drawbackground() {
        renderingType = RenderingType.Skybox;
        drawSkyPlanes();
        for (int i = inpreparemirror ? 1 : 0; i < sectors.size(); i++) {
            drawSkySector(sectors.get(i));
        }
    }

    private void prerender(ArrayList<VisibleSector> sectors) {
        if (inpreparemirror) {
            return;
        }

        bunchfirst.clear();

        for (int i = 0; i < sectors.size(); i++) {
            VisibleSector sec = sectors.get(i);

            int sectnum = sec.index;
            if ((sec.secflags & 1) != 0) {
                checkMirror(world.getFloor(sectnum));
            }

            if ((sec.secflags & 2) != 0) {
                checkMirror(world.getCeiling(sectnum));
            }

            for (int w = 0; w < sec.walls.size; w++) {
                int z = sec.walls.get(w);
                int flags = sec.wallflags.get(w);

                checkMirror(world.getWall(z, sectnum));
                if ((flags & 1) != 0) {
                    checkMirror(world.getLower(z));
                }
                if ((flags & 2) != 0) {
                    checkMirror(world.getUpper(z));
                }
                checkMirror(world.getMaskedWall(z));
            }

            for (int w = 0; w < sec.skywalls.size; w++) {
                int z = sec.skywalls.get(w);
                checkMirror(world.getParallaxCeiling(z));
                checkMirror(world.getParallaxFloor(z));
            }
        }

        for (int i = 0; i < bunchfirst.size(); i++) {
            drawSurf(bunchfirst.get(i), 0, null, null);
        }
    }

    private void checkMirror(GLSurface surf) {
        if (surf == null) {
            return;
        }

        int picnum = surf.picnum;
        if (mirrorTextures[picnum]) {
            bunchfirst.add(surf);
        }
    }

    private void drawSkyPlanes() {
        gl.glDisable(GL_CULL_FACE);
        gl.glDepthMask(false);

        Sector skysector;
        if ((skysector = scanner.getLastSkySector(Heinum.SkyUpper)) != null) {
            int pal = skysector.getCeilingpal();
            int shade = skysector.getCeilingshade();
            int picnum = skysector.getCeilingpicnum();

            drawSky(world.getQuad(), picnum, shade, pal, 0,
                    transform.setToTranslation(cam.position.x, cam.position.y, cam.position.z - 100).scale(cam.far,
                            cam.far, 1.0f));
        }

        if ((skysector = scanner.getLastSkySector(Heinum.SkyLower)) != null) {
            int pal = skysector.getFloorpal();
            int shade = skysector.getFloorshade();
            int picnum = skysector.getFloorpicnum();

            drawSky(world.getQuad(), picnum, shade, pal, 0,
                    transform.setToTranslation(cam.position.x, cam.position.y, cam.position.z + 100).scale(cam.far,
                            cam.far, 1.0f));
        }

        gl.glDepthMask(true);
        gl.glEnable(GL_CULL_FACE);
    }

    private void drawSector(VisibleSector sec) {
        int sectnum = sec.index;
        gotsector[sectnum >> 3] |= pow2char[sectnum & 7];

        if ((sec.secflags & 1) != 0) {
            renderingType = RenderingType.Floor.setIndex(sectnum);
            drawSurf(world.getFloor(sectnum), 0, null, sec.clipPlane);
        }

        if ((sec.secflags & 2) != 0) {
            renderingType = RenderingType.Ceiling.setIndex(sectnum);
            drawSurf(world.getCeiling(sectnum), 0, null, sec.clipPlane);
        }

        for (int w = 0; w < sec.walls.size; w++) {
            int flags = sec.wallflags.get(w);
            int z = sec.walls.get(w);
            renderingType = RenderingType.Wall.setIndex(z);
            drawSurf(world.getWall(z, sectnum), flags, null, sec.clipPlane);
            drawSurf(world.getUpper(z), flags, null, sec.clipPlane);
            drawSurf(world.getLower(z), flags, null, sec.clipPlane);
        }
    }

    public void drawSkySector(VisibleSector sec) {
        for (int w = 0; w < sec.skywalls.size; w++) {
            int z = sec.skywalls.get(w);
            GLSurface ceil = world.getParallaxCeiling(z);
            if (ceil != null) {
                drawSky(ceil, ceil.picnum, ceil.getShade(), ceil.getPal(), ceil.getMethod(), identity);
            }

            GLSurface floor = world.getParallaxFloor(z);
            if (floor != null) {
                drawSky(floor, floor.picnum, floor.getShade(), floor.getPal(), floor.getMethod(), identity);
            }
        }
    }

    private void drawSky(GLSurface surf, int picnum, int shade, int palnum, int method, Matrix4 worldTransform) {
        if (surf.count == 0) {
            return;
        }

        if (getTile(picnum).getType() != AnimType.NONE) {
            picnum += animateoffs(picnum, 0);
        }

        ArtEntry pic = getTile(picnum);
        if (!pic.exists()) {
            method = 1; // invalid data, HOM
        }

        setgotpic(picnum);
        if (!engine.getPaletteManager().isValidPalette(palnum)) {
            palnum = 0;
        }

        GLTile pth = bindSky(pic, palnum, shade, method);
        if (pth != null) {
            Gdx.gl.glDisable(GL_BLEND);
            if ((method & 3) != 0) {
                Gdx.gl.glEnable(GL_BLEND);
            }

            manager.fog(false, 0, 0, 0, 0, 0);
            manager.transform(worldTransform);
            manager.frustum(null);

            surf.render(manager.getProgram());
        }
    }

    protected void drawSurf(GLSurface surf, int flags, Matrix4 worldTransform, Plane[] clipPlane) {
        if (surf == null) {
            return;
        }

        if (surf.count != 0 && (flags == 0 || (surf.visflag & flags) != 0)) {
            int picnum = surf.picnum;

            if (getTile(picnum).getType() != AnimType.NONE) {
                picnum += animateoffs(picnum, 0);
            }

            ArtEntry pic = getTile(picnum);
            int method = surf.getMethod();
            if (!pic.exists()) {
                method = 1; // invalid data, HOM
            }

            setgotpic(picnum);
            GLTile pth = bind(pic, surf.getPal(), surf.getShade(), 0, method);
            if (pth != null) {
                int combvis = globalvisibility;
                int vis = surf.getVisibility();
                if (vis != 0) {
                    combvis = mulscale(globalvisibility, (vis + 16) & 0xFF, 4);
                }

                if (pth.getPixelFormat() == PixelFormat.Pal8) {
                    ((IndexedShader) manager.getProgram()).setVisibility((int) (-combvis / 64.0f));
                } else {
                    calcFog(surf.getPal(), surf.getShade(), combvis);
                }

                manager.color(1.0f, 1.0f, 1.0f, 1.0f);
                if (pth.isHighTile()) {
                    int tsizy = 1;
                    for (; tsizy < pic.getHeight(); tsizy += tsizy) {
                        ;
                    }
                    if ((pic.getWidth() / (float) pic.getHeight()) != (pth.getWidth() / (float) pth.getHeight())) {
                        texture_transform.scale(1.0f, (tsizy * pth.getYScale()) / pth.getHeight());
                    }
                    manager.textureTransform(texture_transform, 0);

                    if (defs != null && defs.texInfo != null) {
                        float r = 1, g = 1, b = 1;
                        if (pth.getPal() != surf.getPal()) {
                            // apply tinting for replaced textures

                            Color p = defs.texInfo.getTints(surf.getPal());
                            r *= p.r / 255.0f;
                            g *= p.g / 255.0f;
                            b *= p.b / 255.0f;
                        }

                        Color pdetail = defs.texInfo.getTints(MAXPALOOKUPS - 1);
                        if (pdetail.r != 255 || pdetail.g != 255 || pdetail.b != 255) {
                            r *= pdetail.r / 255.0f;
                            g *= pdetail.g / 255.0f;
                            b *= pdetail.b / 255.0f;
                        }
                        manager.color(r, g, b, 1.0f);
                    }
                }

                if (worldTransform == null) {
                    manager.transform(identity);
                } else {
                    manager.transform(worldTransform);
                }

                if (clipPlane != null && !inpreparemirror) {
                    manager.frustum(clipPlane);
                } else {
                    manager.frustum(null);
                }

                if ((method & 3) == 0) {
                    Gdx.gl.glDisable(GL_BLEND);
                } else {
                    Gdx.gl.glEnable(GL_BLEND);
                }

                surf.render(manager.getProgram());
            }
        }
    }

    protected void calcFog(int pal, int shade, float combvis) {
        float start = FULLVIS_BEGIN;
        float end = FULLVIS_END;
        PaletteManager paletteManager = engine.getPaletteManager();
        if (combvis != 0) {
            if (shade >= paletteManager.getShadeCount() - 1) {
                start = -1;
                end = 0.001f;
            } else {
                start = (shade > 0) ? 0 : -(FOGDISTCONST * shade) / combvis;
                end = (FOGDISTCONST * (paletteManager.getShadeCount() - 1 - shade)) / combvis;
            }
        }

        Color palookupfog = paletteManager.getFogColor(pal);
        float r = (palookupfog.r / 63.f);
        float g = (palookupfog.g / 63.f);
        float b = (palookupfog.b / 63.f);

        manager.fog(true, start, end, r, g, b);
    }

    @Override
    public void clearview(int dacol) {
        PaletteManager paletteManager = engine.getPaletteManager();
        Palette curpalette = paletteManager.getCurrentPalette();
        gl.glClearColor(curpalette.getRed(dacol) / 255.0f, //
                curpalette.getGreen(dacol) / 255.0f, //
                curpalette.getBlue(dacol) / 255.0f, 0); //
        gl.glClear(GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void changepalette(byte[] palette) {
        super.changepalette(palette);
        textureCache.changePalette(palette);
        textureCache.invalidateall();
        clearskins(true);
        DEFAULT_SCREEN_FADE.set(0, 0, 0, 0);
    }

    @Override
    public void nextpage() {
        super.nextpage();
        clearStatus = false;
        if (world != null) {
            world.nextpage();
        }
        orphoRen.nextpage();
        manager.reset();
        textureCache.unbind();

        omdtims = mdtims;
        mdtims = engine.getCurrentTimeMillis();

        if (boardService.getBoard() != null) {
            for (int i = 0; i < boardService.getSpriteCount(); i++) {
                if (mdpause != 0) {
                    SpriteAnim sprext = defs.mdInfo.getAnimParams(i);
                    if (sprext == null) {
                        continue;
                    }

                    boolean isAnimationDisabled = false;
                    Spriteext inf = defs.mapInfo.getSpriteInfo(i);
                    if (inf != null) {
                        isAnimationDisabled = inf.isAnimationDisabled();
                    }

                    if ((mdpause != 0 && sprext.mdanimtims != 0) || isAnimationDisabled) {
                        sprext.mdanimtims += mdtims - omdtims;
                    }
                }
            }
        }

        beforedrawrooms = 1;

//		if (shape != null)
//			shape.end();

        gl.glFlush();
    }

    @Override
    public void setview(int x1, int y1, int x2, int y2) {
        xdimen = (x2 - x1) + 1;
        ydimen = (y2 - y1) + 1;

        super.setview(x1, y1, x2, y2);

        orphoRen.resize(x2, y2);
    }

    @Override
    public void setviewtotile(DynamicArtEntry pic) { // jfBuild}
        // DRAWROOMS TO TILE BACKUP&SET CODE
        bakwindowx1[setviewcnt] = windowx1;
        bakwindowy1[setviewcnt] = windowy1;
        bakwindowx2[setviewcnt] = windowx2;
        bakwindowy2[setviewcnt] = windowy2;

        if (setviewcnt == 0) {
            baktile = pic;
        }

        offscreenrendering = true;

        setviewcnt++;
        setview(0, 0, pic.getHeight() - 1, pic.getWidth() - 1);
    }

    @Override
    public void setviewback() {// jfBuild
        if (setviewcnt <= 0) {
            offscreenrendering = false;
            setaspect();
            return;
        }

        setviewcnt--;
        offscreenrendering = (setviewcnt > 0);
        if (setviewcnt == 0) {
            if (baktile.exists()) {
                baktile.copyData(setviewbuf());
            }
        }

        setview(bakwindowx1[setviewcnt], bakwindowy1[setviewcnt], bakwindowx2[setviewcnt], bakwindowy2[setviewcnt]);
    }

    protected byte[] setviewbuf() { // gdxBuild
        int width = baktile.getWidth();
        int heigth = baktile.getHeight();
        byte[] data = baktile.getBytes();

        ByteBuffer frame = getFrame(TileData.PixelFormat.Pal8, width, heigth);

        int dptr;
        int sptr = 0;
        for (int i = width - 1, j; i >= 0; i--) {
            dptr = i;
            for (j = 0; j < heigth; j++) {
                data[dptr] = frame.get(sptr++);
                dptr += width;
            }
        }

        return data;
    }

    @Override
    public void setFieldOfView(int fov) {
        this.fovFactor = (float) Math.tan(fov * Math.PI / 360.0);
        setaspect();
    }

    protected void setaspect(int daxrange, int daaspect) {
        viewingrange = offscreenrendering ? daxrange : (int) (daxrange * fovFactor);

        yxaspect = daaspect;
        xyaspect = divscale(1, yxaspect, 32);
        xdimenscale = scale(xdimen, yxaspect, 320);
        xdimscale = scale(320, xyaspect, xdimen);

        int w = 320;
        if ((4 * xdim / 5) == ydim) {
            w = 300;
        }
        float k = daxrange / (float) divscale(xdim * 240L, (long) ydim * w, 16);
        float fov = offscreenrendering ? 110 : (float) Math.toDegrees(2 * Math.atan(k * fovFactor));
        cam.setFieldOfView(fov);
    }

    @Override
    public void setaspect() {
        if (offscreenrendering) {
            setaspect(65536, 65536);
            return;
        }

        if (config.getWidescreen() == 1 && (4 * xdim / 5) != ydim) {
            // the correction factor 100/107 has been found
            // out experimentally. squares ftw!
            int yx = (65536 * 4 * 100) / (3 * 107);
            int vr = divscale(xdim * 3L, ydim * 4L, 16);

            setaspect(vr, yx);
        } else {
            setaspect(65536, divscale(ydim * 320L, xdim * 200L, 16));
        }
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                             int cy1, int cx2, int cy2) {
        renderingType = RenderingType.Tile.setIndex(picnum);
        set2dview();
        orphoRen.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {
        set2dview();
        Arrays.fill(gotsector, (byte) 0);
        orphoRen.drawmapview(dax, day, zoome, ang);
    }

    @Override
    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) {
        set2dview();
        orphoRen.drawoverheadmap(boardService, cposx, cposy, czoom, cang);
    }

    @Override
    public int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        renderingType = RenderingType.Tile.setIndex(0);
        set2dview();
        return orphoRen.printext(font, x, y, text, scale, shade, palnum, align, transparent, shadow);
    }

    @Override
    public ByteBuffer getFrame(PixelFormat format, int xsiz, int ysiz) {
        boolean reverse = false;
        if (ysiz < 0) {
            ysiz *= -1;
            reverse = true;
        }

        int byteperpixel = 3;
        int fmt = GL_RGB;
        if (Gdx.app.getType() == ApplicationType.Android) {
            byteperpixel = 4;
            fmt = GL_RGBA;
        }
        ByteBuffer frameBuffer = ByteBuffer.allocateDirect(xsiz * ysiz * byteperpixel);

        gl.glPixelStorei(GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, ydim - ysiz, xsiz, ysiz, fmt, GL_UNSIGNED_BYTE, frameBuffer);

        if (format == PixelFormat.Rgb) {
            if (reverse) {
                int b1, b2 = 0;
                for (int p, x, y = 0; y < ysiz / 2; y++) {
                    b1 = byteperpixel * (ysiz - y - 1) * xsiz;
                    for (x = 0; x < xsiz; x++) {
                        for (p = 0; p < byteperpixel; p++) {
                            byte tmp = frameBuffer.get(b1 + p);
                            frameBuffer.put(b1 + p, frameBuffer.get(b2 + p));
                            frameBuffer.put(b2 + p, tmp);
                        }
                        b1 += byteperpixel;
                        b2 += byteperpixel;
                    }
                }
            }
            frameBuffer.rewind();
            return frameBuffer;
        }

        ByteBuffer pix8Buffer = ByteBuffer.allocateDirect(xsiz * ysiz);

        PaletteManager paletteManager = engine.getPaletteManager();
        byte[] basePalette = paletteManager.getBasePalette();
        FastColorLookup fastColorLookup = paletteManager.getFastColorLookup();

        int base = 0, r, g, b;
        if (reverse) {
            for (int x, y = 0; y < ysiz; y++) {
                base = byteperpixel * (ysiz - y - 1) * xsiz;
                for (x = 0; x < xsiz; x++) {
                    r = (frameBuffer.get(base++) & 0xFF) >> 2;
                    g = (frameBuffer.get(base++) & 0xFF) >> 2;
                    b = (frameBuffer.get(base++) & 0xFF) >> 2;
                    pix8Buffer.put(fastColorLookup.getClosestColorIndex(basePalette, r, g, b));
                }
            }
        } else {
            for (int i = 0; i < pix8Buffer.capacity(); i++) {
                r = (frameBuffer.get(base++) & 0xFF) >> 2;
                g = (frameBuffer.get(base++) & 0xFF) >> 2;
                b = (frameBuffer.get(base++) & 0xFF) >> 2;
                if (byteperpixel == 4) {
                    base++; // Android
                }
                pix8Buffer.put(fastColorLookup.getClosestColorIndex(basePalette, r, g, b));
            }
        }

        pix8Buffer.rewind();
        return pix8Buffer;
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int col) {
        set2dview();
        orphoRen.drawline256(x1, y1, x2, y2, col);
    }

    @Override
    public void settiltang(int tilt) {
        if (tilt == 0) {
            gtang = 0.0f;
        } else {
            gtang = (float) Gameutils.AngleToDegrees(tilt);
        }
    }

    @Override
    public void setDefs(DefScript defs) {
        this.textureCache.setTextureInfo(defs != null ? defs.texInfo : null);
        this.modelManager.setModelsInfo(defs != null ? defs.mdInfo : null);
        if (this.defs != null) {
            texturesUninit();
            textureCache.invalidateall();
            clearskins(false);
        }
        this.defs = defs;
    }

    @Override
    public void showScreenFade(ScreenFade screenFade) {
        int count = screenFade.getIntensive();
        int r = screenFade.getRed();
        int g = screenFade.getGreen();
        int b = screenFade.getBlue();

        int fr = 0, fg = 0, fb = 0;
        if (r > 0) {
            fr = Math.min(count - 128, r / 2);
        }
        if (g > 0) {
            fg = Math.min(count - 128, g / 2);
        }
        if (b > 0) {
            fb = Math.min(count - 128, b / 2);
        }

        if (orphoRen.isDrawing()) {
            orphoRen.flush(); // #GDX 30.07.2024 was end()
        }

        gl.glDisable(GL_DEPTH_TEST);
        gl.glDisable(GL_TEXTURE_2D);

        gl.glEnable(GL_BLEND);

        set2dview();

        FadeShader shader = (FadeShader) manager.bind(Shader.FadeShader);
        gl.glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.setColor(min(63, fr) << 2, min(63, fg) << 2, min(63, fb) << 2, (1 << 2));
        fadeMesh.render(shader, GL_TRIANGLES);

        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public TextureManager getTextureManager() {
        if (textureCache == null) {
            textureCache = new TextureManager(engine, ExpandTexture.Vertical);
        }
        return textureCache;
    }

    public void enableIndexedShader(boolean enable) {
        if (isUseIndexedTextures != enable) {
            if (isInited) {
                texturesUninit();
            }

            clearskins(false);
            this.isUseIndexedTextures = enable;
        }
    }

    public void clearskins(boolean bit8only) {
        for (int i = MAXTILES - 1; i >= 0; i--) {
            modelManager.clearSkins(i, bit8only);
        }
    }

    @Override
    public void loadModels() {
        for (int i = MAXTILES - 1; i >= 0; i--) {
            int pal = 0;
            modelManager.preload(i, pal, false);
        }
    }

    @Override
    public void onPrecacheTile(int dapicnum, boolean ifSprite) {
        int dapalnum = 0;
        int datype = ifSprite ? 4 : 0;
        if ((!engine.getPaletteManager().isValidPalette(dapalnum)) && (dapalnum < (MAXPALOOKUPS - RESERVEDPALS))) {
            return;
        }

        textureCache.precache(getTexFormat(), getTile(dapicnum), dapalnum, datype);

        if (datype == 0) {
            return;
        }

        modelManager.preload(dapicnum, dapalnum, true);
    }

    public void gltexapplyprops() {
        TexFilter filter = config.getGlfilter();
        textureCache.setFilter(filter);
        for (int i = MAXTILES - 1; i >= 0; i--) {
            skycache.setFilter(i, filter);
        }

        modelManager.setTextureFilter(filter);
    }

    public void invalidatetile(int tilenume, int pal, int how) { // jfBuild
        int numpal, firstpal, np;
        int hp;

        PixelFormat fmt = textureCache.getFmt(tilenume);
        if (fmt == null) {
            return;
        }

        if (fmt == PixelFormat.Pal8) {
            numpal = 1;
            firstpal = 0;
        } else {
            if (pal < 0) {
                numpal = MAXPALOOKUPS;
                firstpal = 0;
            } else {
                numpal = 1;
                firstpal = pal % MAXPALOOKUPS;
            }
        }

        for (hp = 0; hp < 8; hp += 4) {
            if ((how & pow2long[hp]) == 0) {
                continue;
            }

            for (np = firstpal; np < firstpal + numpal; np++) {
                textureCache.invalidate(tilenume, np, textureCache.clampingMode(hp));
            }
        }
    }

    protected GLTile bind(ArtEntry tile, int dapalnum, int dashade, int skybox, int method) {
        if (!engine.getPaletteManager().isValidPalette(dapalnum)) {
            dapalnum = 0;
        }

        GLTile pth = textureCache.get(getTexFormat(), tile, dapalnum, skybox, method);
        if (pth == null) {
            return null;
        }

        textureCache.bind(pth);
        if (manager.getShader() == null || isSkyShader() || pth.getPixelFormat() != manager.getPixelFormat()) {
            switchShader(pth.getPixelFormat() != PixelFormat.Pal8 ? Shader.RGBWorldShader : Shader.IndexedWorldShader);
        }
        setTextureParameters(pth, tile, dapalnum, dashade, skybox, method);

        return pth;
    }

    protected GLTile bindSky(ArtEntry tile, int dapalnum, int dashade, int method) {
        if (!engine.getPaletteManager().isValidPalette(dapalnum)) {
            dapalnum = 0;
        }

        GLTile pth = getSkyTexture(getTexFormat(), tile, dapalnum);
        if (pth == null) {
            return null;
        }

        textureCache.bind(pth);
        if (manager.getShader() == null || !isSkyShader() || pth.getPixelFormat() != manager.getPixelFormat()) {
            switchShader(pth.getPixelFormat() != PixelFormat.Pal8 ? Shader.RGBSkyShader : Shader.IndexedSkyShader);
        }
        setTextureParameters(pth, tile, dapalnum, dashade, 0, 0);
        return pth;
    }

    public void setTextureParameters(GLTile tile, ArtEntry artEntry, int pal, int shade, int skybox, int method) {
        float alpha = 1.0f;
        switch (method & 3) {
            case 2:
                alpha = TRANSLUSCENT1;
                break;
            case 3:
                alpha = TRANSLUSCENT2;
                break;
        }

        if (!artEntry.exists()) {
            alpha = ALPHA_CUT_DISABLE; // Hack to update Z-buffer for invalid mirror textures
        }

        if (tile.getPixelFormat() == TileData.PixelFormat.Pal8) {
            manager.textureSize(tile.getWidth(), tile.getHeight());
            manager.paletteFiltered(config.getPaletteFiltered());
            manager.softShading(config.getSoftShading());
            manager.textureTransform(texture_transform.idt(), 0);
            manager.textureParams8(pal, shade, alpha, (method & 3) == 0 || !textureCache.alphaMode(method));
        } else {
            texture_transform.idt();
            if (tile.isHighTile() && ((tile.getHiresXScale() != 1.0f) || (tile.getHiresYScale() != 1.0f))
                    && RenderingType.Skybox.getIndex() == 0) {
                texture_transform.scale(tile.getHiresXScale(), tile.getHiresYScale());
            }
            manager.textureTransform(texture_transform, 0);

            if (GLInfo.multisample != 0 && config.isUseHighTiles() && RenderingType.Skybox.getIndex() == 0) {
//				if (Console.Geti("r_detailmapping") != 0) {
//					GLTile detail = textureCache.get(tile.getPixelFormat(), tilenum, DETAILPAL, 0, method);
//					if (detail != null) {
//						textureCache.bind(detail);
//						//setupTextureDetail(detail); XXX
//
//						texture_transform.idt();
//						if (detail.isHighTile() && (detail.getHiresXScale() != 1.0f)
//								|| (detail.getHiresYScale() != 1.0f))
//							texture_transform.scale(detail.getHiresXScale(), detail.getHiresYScale());
//						manager.textureTransform(texture_transform, 1);
//					}
//				}
//
//				if (Console.Geti("r_glowmapping") != 0) {
//					GLTile glow = textureCache.get(tile.getPixelFormat(), tilenum, GLOWPAL, 0, method);
//					if (glow != null) {
//						textureCache.bind(glow);
//						//setupTextureGlow(glow); XXX
//					}
//				}
            }

            float r, b, g;
            int numshades = engine.getPaletteManager().getShadeCount();
            float fshade = min(max(shade * 1.04f, 0), numshades);
            r = g = b = (numshades - fshade) / numshades;

            if (defs != null && tile.isHighTile() && defs.texInfo != null) {
                if (tile.getPal() != pal) {
                    // apply tinting for replaced textures

                    Color p = defs.texInfo.getTints(pal);
                    r *= p.r / 255.0f;
                    g *= p.g / 255.0f;
                    b *= p.b / 255.0f;
                }

                Color pdetail = defs.texInfo.getTints(MAXPALOOKUPS - 1);
                if (pdetail.r != 255 || pdetail.g != 255 || pdetail.b != 255) {
                    r *= pdetail.r / 255.0f;
                    g *= pdetail.g / 255.0f;
                    b *= pdetail.b / 255.0f;
                }
            }

            manager.color(r, g, b, alpha);
        }
    }

    public void addSpriteCorr(int snum) {
        // TODO Auto-generated method stub
    }

    public void removeSpriteCorr(int snum) {
        // TODO Auto-generated method stub
    }

    @Override
    public void completemirror() {
        inpreparemirror = false;
    }

    private boolean spritewallfront(Sprite s, int w) {
        if (s == null) {
            return false;
        }

        Wall wal = boardService.getWall(w);
        int x1 = wal.getX();
        int y1 = wal.getY();
        wal = boardService.getWall(wal.getPoint2());
        return (dmulscale(wal.getX() - x1, s.getY() - y1, -(s.getX() - x1), wal.getY() - y1, 32) >= 0);
    }

    protected void glViewport(float x, float y, float x2, float y2) {
        x *= backBufferScale;
        y *= backBufferScale;
        x2 *= backBufferScale;
        y2 *= backBufferScale;
        gl.glViewport((int) x, (int) y, (int) x2, (int) y2);
    }

    protected void set2dview() {
        if (gloy1 != -1) {
            glViewport(0, 0, xdim, ydim);
            orphoRen.resize(xdim, ydim);
        }
        gloy1 = -1;
    }

    protected void resizeglcheck() {
        if ((glox1 != windowx1) || (gloy1 != windowy1) || (glox2 != windowx2) || (gloy2 != windowy2)) {
            glox1 = windowx1;
            gloy1 = windowy1;
            glox2 = windowx2;
            gloy2 = windowy2;

            glViewport(windowx1, ydim - (windowy2 + 1), windowx2 - windowx1 + 1, windowy2 - windowy1 + 1);

            cam.viewportWidth = windowx2;
            cam.viewportHeight = windowy2;
        }
    }

    protected GDXOrtho allocOrphoRenderer(Engine engine) {
        return new GDXOrtho(this, new DefaultMapSettings(engine.getBoardService()));
    }

    protected int[] getMirrorTextures() {
        return null;
    }

    @Override
    public RenderType getType() {
        return RenderType.PolyGDX;
    }

    @Override
    public PixelFormat getTexFormat() {
        return isUseIndexedTextures ? PixelFormat.Pal8 : PixelFormat.Rgba;
    }

    @Override
    public boolean isInited() {
        return isInited;
    }

    protected void switchShader(Shader shader) {
        if (orphoRen.isDrawing()) {
            orphoRen.flush(); // #GDX 30.07.2024 was end()
        }

        manager.bind(shader);
        manager.mirror(inpreparemirror);
        manager.prepare(cam,
                (int) (windowx1 * backBufferScale),
                (int) (windowy1 * backBufferScale),
                (int) (windowx2 * backBufferScale),
                (int) (windowy2 * backBufferScale));
        // TODO: add texture transform here

        if (orphoRen.isDrawing()) {
            orphoRen.setupMatrices();
        }
    }

    protected GLTile getSkyTexture(PixelFormat fmt, ArtEntry artEntry, int palnum) {
        if (!artEntry.hasSize()) {
            return textureCache.get(fmt, artEntry, palnum, 0, 0);
        }

        GLTile tile = skycache.get(artEntry.getNum(), palnum, false, 0);
        if (tile != null /* && tile.getPixelFormat() == fmt */) {
            if (tile.isInvalidated()) {
                tile.setInvalidated(false);

                TileData data = loadPic(fmt, artEntry, palnum);
                tile.update(data, palnum, config.getGlfilter());
            }
        } else {
//			if (tile != null)
//				skycache.dispose(picnum); // old texture

            TileData data = loadPic(fmt, artEntry, palnum);
            if (data == null) {
                return null;
            }

            skycache.add(textureCache.newTile(data, fmt == PixelFormat.Pal8 ? 0 : palnum, config.getGlfilter()), artEntry.getNum());
        }

        return tile;
    }

    protected TileData loadPic(PixelFormat fmt, ArtEntry tile, int palnum) {
        short[] dapskyoff = zeropskyoff;
        int dapskybits = pskybits;

        if (dapskybits < 0) {
            dapskybits = 0;
        }

        TileAtlas sky = new TileAtlas(fmt, tile.getWidth() * (1 << dapskybits), tile.getHeight(), tile.getWidth(),
                tile.getHeight(), false);

        final int tileNum = tile.getNum();
        for (int i = 0; i < (1 << dapskybits); i++) {
            tile = getTile(dapskyoff[i] + tileNum);

            TileData dat;
            if (fmt == PixelFormat.Pal8) {
                dat = new IndexedTileData(tile, false, false, 0);
            } else {
                dat = new RGBTileData(engine.getPaletteManager(), tile, palnum, false, false, 0, config.getGlfilter() != TexFilter.NONE);
            }
            sky.addTile(tile.getNum(), dat);
        }

        return sky.atlas.get(0);
    }

    protected boolean isSkyShader() {
        return manager.getShader() == Shader.RGBSkyShader || manager.getShader() == Shader.IndexedSkyShader;
    }

    @Override
    public void onPalookupChanged(int palnum) {
        textureCache.invalidatepalookup(palnum);
    }

    @Override
    public void onChangePalette(byte[] palette) {
        changepalette(palette);
    }

    @Override
    public void onLoadBoard(Board board) {
        if (world != null) {
            world.dispose();
        }
        world = new WorldMesh(engine);
        scanner.init();

        for (int i = 0; i < board.getSpriteCount(); i++) {
            removeSpriteCorr(i);
            Sprite spr = board.getSprite(i);
            if (spr == null || ((spr.getCstat() >> 4) & 3) != 1 || spr.getStatnum() == MAXSTATUS) {
                continue;
            }

            addSpriteCorr(i);
        }
    }

    @Override
    public void onAddSprite(int spriteNum) {
        addSpriteCorr(spriteNum);
    }

    @Override
    public void onRemoveSprite(int spriteNum) {
        removeSpriteCorr(spriteNum);
    }

    @Override
    public void onInvalidate(int tileNum) {
        invalidatetile(tileNum, -1, -1);
    }

    // Debug 2.5D renderer

//	private boolean WallFacingCheck(WALL wal) {
//		float x1 = wal.x - globalposx;
//		float y1 = wal.y - globalposy;
//		float x2 = boardService.getWall(wal.point2).x - globalposx;
//		float y2 = boardService.getWall(wal.point2).y - globalposy;
//
//		return (x1 * y2 - y1 * x2) >= 0;
//	}
//
//	private boolean NearPlaneCheck(BuildCamera cam, ArrayList<? extends Vector3> points) {
//		Plane near = cam.frustum.planes[0];
//		for (int i = 0; i < points.size(); i++) {
//			if (near.testPoint(points.get(i)) == PlaneSide.Back)
//				return true;
//		}
//		return false;
//	}
//
//	private void projectToScreen(BuildCamera cam, ArrayList<Vertex> points) {
//		for (int i = 0; i < points.size(); i++)
//			cam.project(points.get(i));
//	}
//
//	private ArrayList<Vertex> project(BuildCamera cam, int z, int sectnum, Heinum h) {
//		WALL wal = boardService.getWall(z);
//		if (!WallFacingCheck(wal))
//			return null;
//
//		ArrayList<Vertex> vertex = world.getPoints(h, sectnum, z);
//		if (!cam.polyInCamera(vertex))
//			return null;
//
//		if (NearPlaneCheck(cam, vertex)) {
//			PolygonClipper cl = new PolygonClipper();
//			vertex = cl.ClipPolygon(cam.frustum, vertex);
//			if (vertex.size() < 3)
//				return null;
//		}
//
//		projectToScreen(cam, vertex);
//		return vertex;
//	}
//
//	public ShapeRenderer shape;
//
//	private void draw2dSurface(int z, int sectnum, Heinum heinum) {
//		ArrayList<Vertex> coords = project(cam, z, (short) sectnum, heinum);
//		if (coords != null) {
//			if (heinum == Heinum.MaxWall)
//				shape.setColor(0.8f, 0.8f, 0.8f, 1);
//			else if (heinum == Heinum.Upper || heinum == Heinum.Lower)
//				shape.setColor(0.8f, 0.8f, 0.0f, 1);
//			else if (heinum == Heinum.Portal)
//				shape.setColor(0.8f, 0, 0, 1);
//
//			for (int i = 0; i < coords.size(); i++) {
//				int next = (i + 1) % coords.size();
//				shape.line(coords.get(i).x, coords.get(i).y, coords.get(next).x, coords.get(next).y);
//			}
//		}
//	}

}
