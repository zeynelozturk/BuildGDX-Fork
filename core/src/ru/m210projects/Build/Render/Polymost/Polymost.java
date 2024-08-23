/*
 * "POLYMOST" code originally written by Ken Silverman
 * Ken Silverman's official web site: "http://www.advsys.net/ken"
 * See the included license file "BUILDLIC.TXT" for license info.
 *
 * This file has been modified from Ken Silverman's original release
 * by Jonathon Fowler (jf@jonof.id.au)
 * by the EDuke32 team (development@voidpoint.com)
 * by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Render.Polymost;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import ru.m210projects.Build.Board;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.*;
import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.ModelManager;
import ru.m210projects.Build.Render.ModelHandle.Voxel.GLVoxel;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.IndexedShader;
import ru.m210projects.Build.Render.TextureHandle.TextureManager;
import ru.m210projects.Build.Render.TextureHandle.TextureManager.ExpandTexture;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Render.Types.ScreenFade;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Render.listeners.PaletteListener;
import ru.m210projects.Build.Render.listeners.PrecacheListener;
import ru.m210projects.Build.Render.listeners.TileListener;
import ru.m210projects.Build.Render.listeners.WorldListener;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Script.ModelsInfo.SpriteAnim;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.DynamicArray;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.badlogic.gdx.graphics.GL20.*;
import static java.lang.Math.*;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Render.ModelHandle.MDModel.MDAnimation.*;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE0;
import static ru.m210projects.Build.Render.Types.GL10.*;

public class Polymost extends AbstractRenderer implements PaletteListener, WorldListener, TileListener, PrecacheListener {

    protected static final float vertices[] = new float[]{-2.5f, 1.0f, 2.5f, 1.0f, 0.0f, -2.5f};

    protected final static int MAXWALLSB = ((MAXWALLS >> 2) + (MAXWALLS >> 3));
    private static final Vector2 projPoint = new Vector2();
    // PLAG: sorting stuff
    private static final Vector3 drawmasks_maskeq = new Vector3();
    private static final Vector3 drawmasks_p1eq = new Vector3();
    private static final Vector3 drawmasks_p2eq = new Vector3();
    private static final Vector2 drawmasks_dot = new Vector2(), drawmasks_dot2 = new Vector2(),
            drawmasks_middle = new Vector2(), drawmasks_pos = new Vector2(), drawmasks_spr = new Vector2();
    public static float MAXDRUNKANGLE = 2.5f;
    public static int r_parallaxskyclamping = 1; // OSD CVAR XXX
    public static int r_parallaxskypanning = 0; // XXX
    public static int r_vertexarrays = 1; // Vertex Array model drawing cvar
    public static int r_vbos = 1; // Vertex Buffer Objects model drawing cvars
    private static float dceilzsofslope, dfloorzsofslope;
    protected final com.badlogic.gdx.graphics.Color polyColor = new com.badlogic.gdx.graphics.Color();
    protected final float[][] matrix = new float[4][4];
    private final int[] spritesx = new int[MAXSPRITESONSCREEN + 1];
    private final int[] spritesy = new int[MAXSPRITESONSCREEN + 1];
    private final int[] spritesz = new int[MAXSPRITESONSCREEN + 1];
    private final short[] p2 = new short[MAXWALLSB];
    private final short[] thesector = new short[MAXWALLSB];
    private final short[] thewall = new short[MAXWALLSB];
    private final short[] maskwall = new short[MAXWALLSB];
    private final short[] bunchfirst = new short[MAXWALLSB];
    private final short[] bunchlast = new short[MAXWALLSB];
    private final int[] sectorborder = new int[256];
    private final double[] dxb1 = new double[MAXWALLSB];
    private final double[] dxb2 = new double[MAXWALLSB];
    private final byte[] ptempbuf = new byte[MAXWALLSB << 1];
    private final float SCISDIST = 1.0f; // 1.0: Close plane clipping distance
    private final PolyClipper clipper;
    private final Polygon[] drawpoly = new Polygon[16];
    private final double[] nonparallaxed_ft = new double[4], nonparallaxed_px = new double[3],
            nonparallaxed_py = new double[3], nonparallaxed_dd = new double[3], nonparallaxed_uu = new double[3],
            nonparallaxed_vv = new double[3];
    private final double[] drawalls_dd = new double[3], drawalls_vv = new double[3], drawalls_ft = new double[4];
    private final Wall drawalls_nwal = new Wall();
    // fighting

    //	public static short drawingskybox = 0;
    private final int[] skywalx = {-512, 512, 512, -512}, skywaly = {-512, -512, 512, 512};
    private final double[] drawrooms_px = new double[6], drawrooms_py = new double[6], drawrooms_pz = new double[6],
            drawrooms_px2 = new double[6], drawrooms_py2 = new double[6], drawrooms_pz2 = new double[6],
            drawrooms_sx = new double[6], drawrooms_sy = new double[6];
    private final Surface[] dmaskwall = new Surface[8];
    //	private int framesize;
    private final float[] drawmaskwall_csy = new float[4], drawmaskwall_fsy = new float[4];
    private final int[] drawmaskwall_cz = new int[4], drawmaskwall_fz = new int[4];
    private final float TSPR_OFFSET_FACTOR = 0.000008f;
    private final Surface[] dsprite = new Surface[6];
    private final float[] drawsprite_ft = new float[4];
    private final Array<Vector2> dsin = new DynamicArray<>(MAXSPRITESV7, Vector2.class);
    private final Array<Vector2> dcoord = new DynamicArray<>(MAXSPRITESV7, Vector2.class);
    private final Array<AtomicBoolean> spritewall = new DynamicArray<>(MAXSPRITESV7, AtomicBoolean.class);
    public RenderingType renderingType = RenderingType.Nothing;
    public GLFog globalfog;
    public int gltexcacnum = -1;
    public double defznear = 0.1;
    public double defzfar = 0.9;
    protected float fovFactor = 1.0f;
    protected short globalpicnum;
    protected int globalorientation;
    protected Sprite[] tspriteptr = new Sprite[MAXSPRITESONSCREEN + 1];
    protected float[] alphahackarray = new float[MAXTILES];
    protected float shadescale = 1.1f;
    protected int shadescale_unbounded = 0;
    protected float curpolygonoffset; // internal polygon offset stack for drawing flat sprites to avoid depth
    protected float gyxscale, gviewxrange, ghalfx, grhalfxdown10, grhalfxdown10x;
    protected double gxyaspect;
    protected float ghoriz;
    protected float gcosang, gsinang, gcosang2, gsinang2;
    protected float gchang, gshang, ogshang, gctang, gstang;
    protected float gtang = 0.0f;
    protected double guo, gux; // Screen-based texture mapping parameters
    protected double guy;
    protected double gvo;
    protected double gvx;
    protected double gvy;
    protected double gdo, gdx, gdy;
    protected DefScript defs;
    protected GL10 gl;
    protected OrphoRenderer ortho;
    protected PolymostModelRenderer mdrenderer;
    protected boolean isInited = false;
    protected TextureManager textureCache;
    protected ModelManager modelManager;
    protected IndexedShader texshader;
    float glox1, gloy1, glox2, gloy2;
    int pow2xsplit = 0;
    int skyclamphack = 0;
    private boolean drawRooms;
    private int numscans, numbunches;
    private int maskwallcnt;
    private int global_cf_z;
    private float global_cf_xpanning, global_cf_ypanning, global_cf_heinum;
    private int global_cf_shade, global_cf_pal;

    // (dpx,dpy) specifies an n-sided polygon. The polygon must be a convex
    // clockwise loop.
    // n must be <= 8 (assume clipping can double number of vertices)
    private int srepeat = 0, trepeat = 0;
    private int glmultisample, glnvmultisamplehint;

    public Polymost(GameConfig config) {
        super(config);
        config.setVideoContext(new PolymostVideoContext(this));
        this.clipper = new PolyClipper(this);
        IntStream.range(0, drawpoly.length).forEach(i -> drawpoly[i] = new Polygon());
        IntStream.range(0, dmaskwall.length).forEach(i -> dmaskwall[i] = new Surface());
        IntStream.range(0, dsprite.length).forEach(i -> dsprite[i] = new Surface());
        this.globalfog = new GLFog();
    }

    // variables that are set to ceiling- or floor-members, depending
    // on which one is processed right now

    public static void equation(Vector3 ret, float x1, float y1, float x2, float y2) {
        if ((x2 - x1) != 0) {
            ret.x = (y2 - y1) / (x2 - x1);
            ret.y = -1;
            ret.z = (y1 - (ret.x * x1));
        } else // vertical
        {
            ret.x = 1;
            ret.y = 0;
            ret.z = -x1;
        }
    }

    @Override
    public boolean isInited() {
        return isInited;
    }

    @Override
    public void uninit() {
        // Reset if this is -1 (meaning 1st texture call ever), or > 0 (textures
        // in memory)

        if (gltexcacnum < 0) {
            gltexcacnum = 0;

            // Hack for polymost_dorotatesprite calls before 1st polymost_drawrooms()
            gcosang = gcosang2 = 16384 / 262144.0f;
            gsinang = gsinang2 = 0.0f;
        }

        gl.glDisable(GL_ALPHA_TEST);
        gl.glDisable(GL_MULTISAMPLE);
        gl.glDisable(GL_FOG);
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL_TEXTURE);
        gl.glLoadIdentity();

        paletteManager.setListener(PaletteListener.DUMMY_PALETTE_CHANGE_LISTENER);
        boardService.setListener(WorldListener.DUMMY_LISTENER);
        tileManager.setTileListener(TileListener.DUMMY_LISTENER);
        textureCache.uninit();
        modelManager.dispose();
        clearskins(false);

        ortho.uninit();

        //
        // Cachefile_Free();
        // polymost_cachesync();

        isInited = false;
    }

    @Override
    public void init(Engine engine) {
        super.init(engine);
        try {
            this.gl = GL10.instance;
            this.textureCache = getTextureManager();
            // palette / palookup listener
            this.paletteManager.setListener(this);
            // new board, add / remove sprite listener
            this.boardService.setListener(this);
            this.tileManager.setTileListener(this);
            this.modelManager = new PolymostModelManager(this);
            this.mdrenderer = new PolymostModelRenderer(this);
            this.ortho = allocOrphoRenderer(engine);
            this.setDefs(engine.getDefs());

            ortho.init();
            mdrenderer.init();
            globalfog.init(gl, paletteManager);
            GLInfo.init(gl);
            gl.glShadeModel(GL_SMOOTH); // GL_FLAT
            gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // Use FASTEST for ortho!
            gl.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
            gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            gl.glPixelStorei(GL_PACK_ALIGNMENT, 1);
            if (glmultisample > 0 && GLInfo.multisample != 0) {
                if (GLInfo.nvmultisamplehint != 0) {
                    gl.glHint(GL_MULTISAMPLE_FILTER_HINT_NV, glnvmultisamplehint != 0 ? GL_NICEST : GL_FASTEST);
                }
                gl.glEnable(GL_MULTISAMPLE);
            }

            if (r_vbos != 0 && (GLInfo.vbos == 0)) {
                Console.out.println("Your OpenGL implementation doesn't support Vertex Buffer Objects. Disabling...");
                r_vbos = 0;
            }

            Console.out.println("Polymost renderer is initialized", OsdColor.GREEN);
            Console.out.println(Gdx.graphics.getGLVersion().getRendererString() + " " + gl.glGetString(GL_VERSION), OsdColor.YELLOW);

            // init gl settings
            config.setGlfilter(config.getGlfilter());
            config.setPaletteEmulation(config.isPaletteEmulation());
            config.setUseHighTiles(config.isUseHighTiles());
            config.setDetailMapping(config.isDetailMapping());
            config.setGlowMapping(config.isGlowMapping());

            isInited = true;
        } catch (Throwable t) {
            t.printStackTrace();
            isInited = false;
            Console.out.println("Polymost renderer initialization error!", OsdColor.RED);
        }
    }

    @Override
    public void resize(int width, int height) {
        if ((width == xdim) && (height == ydim)) {
            return;
        }

        xdim = width;
        ydim = height;
        ortho.resize(width, height);

        config.setgFov(config.getgFov());
        setview(0, 0, xdim - 1, ydim - 1);
        resizeglcheck();
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
    public void setviewback() { // jfBuild
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

    public BoardService getBoardService() {
        return engine.getBoardService();
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
    public int getWidth() {
        return xdim;
    }

    @Override
    public int getHeight() {
        return ydim;
    }

    protected TextureManager newTextureManager(Engine engine) {
        return new TextureManager(engine, ExpandTexture.Both);
    }

    public void setTextureParameters(GLTile tile, ArtEntry artEntry, int pal, int shade, int skybox, int method) {
        if (tile.getPixelFormat() == PixelFormat.Pal8) {
            if (!texshader.isBinded()) {
                Gdx.gl.glActiveTexture(GL_TEXTURE0);
                texshader.bind();
            }
            texshader.setTextureParams(pal, shade);

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
                alpha = 0.01f; // Hack to update Z-buffer for invalid mirror textures
            }

            texshader.setDrawLastIndex((method & 3) == 0 || !textureCache.alphaMode(method));
            texshader.setTransparent(alpha);
        } else {
            // texture scale by parkar request
            if (tile.isHighTile() && ((tile.getHiresXScale() != 1.0f) || (tile.getHiresYScale() != 1.0f))
                    && RenderingType.Skybox.getIndex() == 0) {
                gl.glMatrixMode(GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glScalef(tile.getHiresXScale(), tile.getHiresYScale(), 1.0f);
                gl.glMatrixMode(GL_MODELVIEW);
            }

            if (GLInfo.multisample != 0 && config.isUseHighTiles() && RenderingType.Skybox.getIndex() == 0) {
                if (config.isDetailMapping()) {
                    GLTile detail = textureCache.get(tile.getPixelFormat(), artEntry, DETAILPAL, 0, method);
                    if (detail != null) {
                        textureCache.bind(detail);
                        setupTextureDetail(detail);

                        gl.glMatrixMode(GL_TEXTURE);
                        gl.glLoadIdentity();
                        if (detail.isHighTile() && (detail.getHiresXScale() != 1.0f)
                                || (detail.getHiresYScale() != 1.0f)) {
                            gl.glScalef(detail.getHiresXScale(), detail.getHiresYScale(), 1.0f);
                        }
                        gl.glMatrixMode(GL_MODELVIEW);
                    }
                }

                if (config.isGlowMapping()) {
                    GLTile glow = textureCache.get(tile.getPixelFormat(), artEntry, GLOWPAL, 0, method);
                    if (glow != null) {
                        textureCache.bind(glow);
                        setupTextureGlow(glow);
                    }
                }
            }

            com.badlogic.gdx.graphics.Color c = getshadefactor(shade, method);
            if (defs != null && tile.isHighTile() && defs.texInfo != null) {
                if (tile.getPal() != pal) {
                    // apply tinting for replaced textures

                    Color p = defs.texInfo.getTints(pal);
                    c.r *= p.r / 255.0f;
                    c.g *= p.g / 255.0f;
                    c.b *= p.b / 255.0f;
                }

                Color pdetail = defs.texInfo.getTints(MAXPALOOKUPS - 1);
                if (pdetail.r != 255 || pdetail.g != 255 || pdetail.b != 255) {
                    c.r *= pdetail.r / 255.0f;
                    c.g *= pdetail.g / 255.0f;
                    c.b *= pdetail.b / 255.0f;
                }
            }

            if (!artEntry.exists()) {
                c.a = 0.01f; // Hack to update Z-buffer for invalid mirror textures
            }
            gl.glColor4f(c.r, c.g, c.b, c.a); // GL30 exception
        }
    }

    /**
     *
     * Attension! In some game this method uses globalpicnum and globalshade in rgb-palette mode
     */
    public com.badlogic.gdx.graphics.Color getshadefactor(int shade, int method) {
        int numshades = paletteManager.getShadeCount();
        float fshade = min(max(shade * 1.04f, 0), numshades);
        float f = (numshades - fshade) / numshades;

        polyColor.r = polyColor.g = polyColor.b = f;

        switch (method & 3) {
            default:
            case 0:
            case 1:
                polyColor.a = 1.0f;
                break;
            case 2:
                polyColor.a = TRANSLUSCENT1;
                break;
            case 3:
                polyColor.a = TRANSLUSCENT2;
                break;
        }

        return polyColor;
    }

    protected GLTile bind(ArtEntry dapicnum, int dapalnum, int dashade, int skybox, int method) {
        GLTile pth = textureCache.get(texshader != null ? PixelFormat.Pal8 : PixelFormat.Rgba, dapicnum, dapalnum,
                skybox, method);
        if (pth == null) {
            return null;
        }

        bind(pth);
        setTextureParameters(pth, dapicnum, dapalnum, dashade, skybox, method);

        return pth;
    }

    protected void bind(GLTile tile) {
        GLTile bindedTile = textureCache.getLastBinded();
        boolean res = tile != bindedTile && ((bindedTile == null
                || (tile.getPixelFormat() == PixelFormat.Pal8 && bindedTile.getPixelFormat() != PixelFormat.Pal8)
                || (tile.getPixelFormat() != PixelFormat.Pal8 && bindedTile.getPixelFormat() == PixelFormat.Pal8)));
        textureCache.bind(tile);

        if (res && texshader != null) {
            gl.glActiveTexture(GL_TEXTURE0);
            if (tile.getPixelFormat() != PixelFormat.Pal8) {
                texshader.unbind();
            } else {
                texshader.bind();
            }
        }
    }

    public TextureManager getTextureManager() {
        if (textureCache == null) {
            return newTextureManager(engine);
        }
        return textureCache;
    }

    protected boolean enableIndexedShader(boolean enable) {
        boolean isChanged = false;
        if (enable) {
            if (texshader == null) {
                texshader = allocIndexedShader();
                if (texshader != null) {
                    textureCache.changePalette(paletteManager.getCurrentPalette().getBytes());
                    isChanged = true;
                }
            }
        } else if (texshader != null) {
            texshader.dispose();
            texshader = null;

            textureCache.disposePalette();
            isChanged = true;
        }

        if (isChanged && isInited) {
            textureCache.uninit();
            clearskins(false);
        }

        return !enable || texshader != null;
    }

    @Override
    public void setDefs(DefScript defs) {
        this.textureCache.setTextureInfo(defs != null ? defs.texInfo : null);
        this.modelManager.setModelsInfo(defs != null ? defs.mdInfo : null);
        if (this.defs != null) {
            textureCache.uninit();
            gltexinvalidateall();
        }
        this.defs = defs;
    }

    /**
     * @return true if shader was binded
     */
    protected boolean beginShowFade() {
        gl.glMatrixMode(GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL_DEPTH_TEST);
        gl.glDisable(GL_ALPHA_TEST);
        gl.glDisable(GL_TEXTURE_2D);
        globalfog.disable();

        gl.glEnable(GL_BLEND);
        boolean hasShader = texshader != null && texshader.isBinded();
        if (hasShader) {
            texshader.unbind();
        }
        return hasShader;
    }

    protected void endShowFade(boolean hasShader) {
        if (hasShader) {
            texshader.bind();
        }

        gl.glMatrixMode(GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL_PROJECTION);
        gl.glPopMatrix();

        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        globalfog.enable();
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

        boolean hasShader = beginShowFade();

        gl.glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4ub(min(63, fr) << 2, min(63, fg) << 2, min(63, fb) << 2, (1 << 2));

        gl.glBegin(GL_TRIANGLES);
        for (int i = 0; i < 6; i += 2) {
            gl.glVertex2f(vertices[i], vertices[i + 1]);
        }
        gl.glEnd();

        endShowFade(hasShader);
    }

    //
    // invalidatetile
    // pal: pass -1 to invalidate all palettes for the tile, or >=0 for a particular
    // palette
    // how: pass -1 to invalidate all instances of the tile in texture memory, or a
    // bitfield
    // bit 0: opaque or masked (non-translucent) texture, using repeating
    // bit 1: ignored
    // bit 2: ignored (33% translucence, using repeating)
    // bit 3: ignored (67% translucence, using repeating)
    // bit 4: opaque or masked (non-translucent) texture, using clamping
    // bit 5: ignored
    // bit 6: ignored (33% translucence, using clamping)
    // bit 7: ignored (67% translucence, using clamping)
    // clamping is for sprites, repeating is for walls
    //

    public void invalidatetile(int tilenume, int pal, int how) {
        int numpal, firstpal, np;
        int hp;

        PixelFormat fmt = textureCache.getFmt(tilenume);
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

    // Make all textures "dirty" so they reload, but not re-allocate
    // This should be much faster than polymost_glreset()
    // Use this for palette effects ... but not ones that change every frame!
    public void gltexinvalidateall() {
        textureCache.invalidateall();
        clearskins(true);
    }

    @Override
    public void changepalette(final byte[] palette) {
        super.changepalette(palette);
        gltexinvalidateall();
        if (texshader != null) {
            textureCache.changePalette(palette);
        }
        DEFAULT_SCREEN_FADE.set(0, 0, 0, 0);
    }

    public void clearskins(boolean bit8only) {
        for (int i = MAXTILES - 1; i >= 0; i--) {
            modelManager.clearSkins(i, bit8only);
        }
    }

    public void gltexapplyprops() {
        TexFilter filter = config.getGlfilter();
        textureCache.setFilter(filter);
        modelManager.setTextureFilter(filter);
    }

    public void setupTextureGlow(GLTile tex) {
        if (!tex.isGlowTexture()) {
            return;
        }

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_INTERPOLATE_ARB);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_PREVIOUS_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_TEXTURE);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE2_RGB_ARB, GL_TEXTURE);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND2_RGB_ARB, GL_ONE_MINUS_SRC_ALPHA);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_ALPHA_ARB, GL_REPLACE);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA_ARB, GL_PREVIOUS_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA_ARB, GL_SRC_ALPHA);

        tex.setupTextureWrap(TextureWrap.Repeat);
    }

    public void setupTextureDetail(GLTile tex) {
        if (!tex.isDetailTexture()) {
            return;
        }

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_MODULATE);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_PREVIOUS_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_TEXTURE);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_ALPHA_ARB, GL_REPLACE);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA_ARB, GL_PREVIOUS_ARB);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA_ARB, GL_SRC_ALPHA);

        gl.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 2.0f);

        tex.setupTextureWrap(TextureWrap.Repeat);
    }

    protected void glViewport(float x, float y, float x2, float y2) {
        x *= backBufferScale;
        y *= backBufferScale;
        x2 *= backBufferScale;
        y2 *= backBufferScale;
        gl.glViewport((int) x, (int) y, (int) x2, (int) y2);
    }

    public void resizeglcheck() // Ken Build method
    {
        if ((glox1 != windowx1) || (gloy1 != windowy1) || (glox2 != windowx2) || (gloy2 != windowy2)) {
            glox1 = windowx1;
            gloy1 = windowy1;
            glox2 = windowx2;
            gloy2 = windowy2;

            glViewport(windowx1, (ydim - (windowy2 + 1)), (windowx2 - windowx1 + 1), (windowy2 - windowy1 + 1));

            gl.glMatrixMode(GL_PROJECTION);
            gl.glLoadIdentity();

//			glPerspective(65, xdimen / (float) ydimen, 0.0001f, 2000);
            float ang = (87 - (24 * gshang * gshang)) * 320.0f / xdimen;
            glPerspective(ang / 256.0f, xdimen / (float) (ydimen), -ydimen, ydimen);

            gl.glMatrixMode(GL_MODELVIEW);
            gl.glLoadIdentity();

            globalfog.enable();
        }
    }

    public void glPerspective(float fovyInDegrees, float aspectRatio, float znear, float zfar) {
        float ymax = (float) (znear * Math.tan(fovyInDegrees * Math.PI / 360.0));
        float xmax = ymax * aspectRatio;
//	    gl.glFrustumf(-xmax, xmax, -ymax, ymax, znear, zfar);
        glFrustumf(-xmax, xmax, -ymax, ymax, znear, zfar);
    }

    public void glFrustumf(float left, float right, float bottom, float top, float znear, float zfar) {
        float A = (right + left) / (right - left);
        float B = (top + bottom) / (top - bottom);
        float C = -(zfar + znear) / (zfar - znear);
        float D = -(2 * zfar * znear) / (zfar - znear);

        matrix[0][0] = 2.0f * znear / (right - left); // 0
        matrix[0][1] = 0.0f; // 4
        matrix[0][2] = A; // 8
        matrix[0][3] = 0.0f; // 12
        matrix[1][0] = 0.0f; // 1
        matrix[1][1] = 2.0f * znear / (top - bottom); // 5
        matrix[1][2] = B; // 9
        matrix[1][3] = 0.0f; // 13
        matrix[2][0] = 0.0f; // 2
        matrix[2][1] = 0.0f; // 6
        matrix[2][2] = C; // 2000 * C // 10
        matrix[2][3] = D; // 14
        matrix[3][0] = 0.0f; // 3
        matrix[3][1] = 0.0f; // 7
        matrix[3][2] = -1.0f; // 1f - matrix[2][2]; // 11
        matrix[3][3] = 0.0f; // 15

        gl.glLoadMatrixf(matrix);
    }

    private int drawpoly_math(int nn, int i, int j, double ngux, double ngdx, double nguy, double ngdy, double nguo,
                              double ngdo, double var) {
        double f = -(drawpoly[i].px * (ngux - ngdx * var) + drawpoly[i].py * (nguy - ngdy * var) + (nguo - ngdo * var))
                / ((drawpoly[j].px - drawpoly[i].px) * (ngux - ngdx * var)
                + (drawpoly[j].py - drawpoly[i].py) * (nguy - ngdy * var));
        drawpoly[nn].uu = ((drawpoly[j].px - drawpoly[i].px) * f + drawpoly[i].px);
        drawpoly[nn].vv = ((drawpoly[j].py - drawpoly[i].py) * f + drawpoly[i].py);
        ++nn;

        return nn;
    }

    protected void drawpoly(Surface[] dm, int n, int method) {
        double ngdx, ngdy, ngdo, ngux, nguy, nguo;
        double ngvx, ngvy, ngvo, dp, up, vp, du0 = 0.0, du1 = 0.0, dui, duj;
        double f, r, ox, oy, oz, ox2, oy2, oz2, uoffs, ix0, ix1;
        int i, j, k, nn, tsizx, tsizy, xx, yy;

        boolean dorot;

        if (method == -1) {
            return;
        }

        if (n == 3) {
            if ((dm[0].px - dm[1].px) * (dm[2].py - dm[1].py) >= (dm[2].px - dm[1].px) * (dm[0].py - dm[1].py)) {
                return; // for triangle
            }
        } else {
            f = 0; // f is area of polygon / 2
            for (i = n - 2, j = n - 1, k = 0; k < n; i = j, j = k, k++) {
                if (i < 0) {
                    return;
                }
                f += (dm[i].px - dm[k].px) * dm[j].py;
            }
            if (f <= 0) {
                return;
            }
        }

        // Load texture (globalpicnum)
        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }

        setgotpic(globalpicnum);
        ArtEntry pic = getTile(globalpicnum);
        tsizx = pic.getWidth();
        tsizy = pic.getHeight();

        if (!paletteManager.isValidPalette(globalpal)) {
            globalpal = 0;
        }

        boolean HOM = false;
        if (!pic.exists()) {
            tsizx = tsizy = 1;
            HOM = true;
        }

        j = 0;
        dorot = ((gchang != 1.0) || (gctang != 1.0));
        if (dorot) {
            for (i = 0; i < n; i++) {
                ox = dm[i].px - ghalfx;
                oy = dm[i].py - ghoriz;
                oz = ghalfx;

                // Up/down rotation
                ox2 = ox;
                oy2 = oy * gchang - oz * gshang;
                oz2 = oy * gshang + oz * gchang;

                // Tilt rotation
                ox = ox2 * gctang - oy2 * gstang;
                oy = ox2 * gstang + oy2 * gctang;
                oz = oz2;

                r = ghalfx / oz;
                drawpoly[j].dd = (dm[i].px * gdx + dm[i].py * gdy + gdo) * r;
                drawpoly[j].uu = (dm[i].px * gux + dm[i].py * guy + guo) * r;
                drawpoly[j].vv = (dm[i].px * gvx + dm[i].py * gvy + gvo) * r;

                drawpoly[j].px = ox * r + ghalfx;
                drawpoly[j].py = oy * r + ghoriz;
                if ((j == 0) || (drawpoly[j].px != drawpoly[j - 1].px) || (drawpoly[j].py != drawpoly[j - 1].py)) {
                    j++;
                }
            }
        } else {
            for (i = 0; i < n; i++) {
                drawpoly[j].px = dm[i].px;
                drawpoly[j].py = dm[i].py;
                if ((j == 0) || (drawpoly[j].px != drawpoly[j - 1].px) || (drawpoly[j].py != drawpoly[j - 1].py)) {
                    j++;
                }
            }
        }
        while ((j >= 3) && (drawpoly[j - 1].px == drawpoly[0].px) && (drawpoly[j - 1].py == drawpoly[0].py)) {
            j--;
        }
        if (j < 3) {
            return;
        }
        n = j;

        if (skyclamphack != 0) {
            method |= 4;
        }

        GLTile pth = bind(pic, globalpal, globalshade,
                RenderingType.Skybox.getIndex(), method);
        if (pth == null) {
            return;
        }

        int texunits = textureCache.getTextureUnits();

        if (srepeat != 0) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        }
        if (trepeat != 0) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        }

        float hackscx = 1.0f;
        float hackscy = 1.0f;
        if (pth.isHighTile()) {
            hackscx = pth.getXScale();
            hackscy = pth.getYScale();
            tsizx = pth.getWidth();
            tsizy = pth.getHeight();
            HOM = false;
        }

        if (GLInfo.texnpot == 0) {
            for (xx = 1; xx < tsizx; xx += xx) ;
            ox2 = 1.0 / xx;
            for (yy = 1; yy < tsizy; yy += yy) ;
            oy2 = 1.0 / yy;
        } else {
            xx = tsizx;
            ox2 = 1.0 / xx;
            yy = tsizy;
            oy2 = 1.0 / yy;
        }

        if (((method & 3) == 0) && !HOM) {
            gl.glDisable(GL_BLEND);
            gl.glDisable(GL_ALPHA_TEST); // alpha_test
        } else {
            float al = 0.0f; // PLAG : default alphacut was 0.32 before goodalpha
            if (pth != null && pth.getAlphaCut() >= 0.0f) {
                al = pth.getAlphaCut();
            }
            if (alphahackarray[globalpicnum] != 0) {
                al = alphahackarray[globalpicnum];
            }
            if (HOM) {
                al = 0.0f; // invalid textures ignore the alpha cutoff settings
            }

            gl.glAlphaFunc(GL_GREATER, al);
            gl.glEnable(GL_BLEND);
            gl.glEnable(GL_ALPHA_TEST);
        }

        if (!dorot) {
            for (i = n - 1; i >= 0; i--) {
                drawpoly[i].dd = drawpoly[i].px * gdx + drawpoly[i].py * gdy + gdo;
                drawpoly[i].uu = drawpoly[i].px * gux + drawpoly[i].py * guy + guo;
                drawpoly[i].vv = drawpoly[i].px * gvx + drawpoly[i].py * gvy + gvo;
            }
        }

        if (pth.getPixelFormat() == PixelFormat.Pal8) {
            texshader.setVisibility((int) globalfog.combvis);
        }
        globalfog.apply();

        // Hack for walls&masked walls which use textures that are not a power of 2
        if ((pow2xsplit != 0) && (tsizx != xx)) {
            if (!dorot) {
                ngdx = gdx;
                ngdy = gdy;
                ngdo = gdo + (ngdx + ngdy) * .5;
                ngux = gux;
                nguy = guy;
                nguo = guo + (ngux + nguy) * .5;
                ngvx = gvx;
                ngvy = gvy;
                ngvo = gvo + (ngvx + ngvy) * .5;
            } else {
                ox = drawpoly[1].py - drawpoly[2].py;
                oy = drawpoly[2].py - drawpoly[0].py;
                oz = drawpoly[0].py - drawpoly[1].py;
                r = 1.0 / (ox * drawpoly[0].px + oy * drawpoly[1].px + oz * drawpoly[2].px);
                ngdx = (ox * drawpoly[0].dd + oy * drawpoly[1].dd + oz * drawpoly[2].dd) * r;
                ngux = (ox * drawpoly[0].uu + oy * drawpoly[1].uu + oz * drawpoly[2].uu) * r;
                ngvx = (ox * drawpoly[0].vv + oy * drawpoly[1].vv + oz * drawpoly[2].vv) * r;
                ox = drawpoly[2].px - drawpoly[1].px;
                oy = drawpoly[0].px - drawpoly[2].px;
                oz = drawpoly[1].px - drawpoly[0].px;
                ngdy = (ox * drawpoly[0].dd + oy * drawpoly[1].dd + oz * drawpoly[2].dd) * r;
                nguy = (ox * drawpoly[0].uu + oy * drawpoly[1].uu + oz * drawpoly[2].uu) * r;
                ngvy = (ox * drawpoly[0].vv + oy * drawpoly[1].vv + oz * drawpoly[2].vv) * r;
                ox = drawpoly[0].px - .5;
                oy = drawpoly[0].py - .5; // .5 centers texture nicely
                ngdo = drawpoly[0].dd - ox * ngdx - oy * ngdy;
                nguo = drawpoly[0].uu - ox * ngux - oy * nguy;
                ngvo = drawpoly[0].vv - ox * ngvx - oy * ngvy;
            }

            ngux *= hackscx;
            nguy *= hackscx;
            nguo *= hackscx;
            ngvx *= hackscy;
            ngvy *= hackscy;
            ngvo *= hackscy;
            uoffs = ((xx - tsizx) * .5);
            ngux -= ngdx * uoffs;
            nguy -= ngdy * uoffs;
            nguo -= ngdo * uoffs;

            // Find min&max u coordinates (du0...du1)
            for (i = 0; i < n; i++) {
                ox = drawpoly[i].px;
                oy = drawpoly[i].py;
                f = (ox * ngux + oy * nguy + nguo) / (ox * ngdx + oy * ngdy + ngdo);

//				if (abs(f) > 2000) - GDX 17.10.2021 "Polymost wall non2power textures hack protected" in 24 June 2020 - makes bugs with hires textures
//					f = 2000;

                if (i == 0) {
                    du0 = du1 = f;
                    continue;
                }
                if (f < du0) {
                    du0 = f;
                } else if (f > du1) {
                    du1 = f;
                }
            }

            if (tsizx != 0) {
                f = 1.0 / tsizx;
                ix0 = floor(du0 * f);
                ix1 = floor(du1 * f);

                for (; ix0 <= ix1; ix0++) {
                    du0 = (ix0) * tsizx;
                    du1 = (ix0 + 1) * tsizx;

                    i = 0;
                    nn = 0;
                    duj = (drawpoly[i].px * ngux + drawpoly[i].py * nguy + nguo)
                            / (drawpoly[i].px * ngdx + drawpoly[i].py * ngdy + ngdo);
                    do {
                        j = i + 1;
                        if (j == n) {
                            j = 0;
                        }
                        dui = duj;
                        duj = (drawpoly[j].px * ngux + drawpoly[j].py * nguy + nguo)
                                / (drawpoly[j].px * ngdx + drawpoly[j].py * ngdy + ngdo);

                        if ((du0 <= dui) && (dui <= du1)) {
                            drawpoly[nn].uu = drawpoly[i].px;
                            drawpoly[nn].vv = drawpoly[i].py;
                            nn++;
                        }

                        if (duj <= dui) {
                            if ((du1 < duj) != (du1 < dui)) {
                                nn = drawpoly_math(nn, i, j, ngux, ngdx, nguy, ngdy, nguo, ngdo, du1);
                            }
                            if ((du0 < duj) != (du0 < dui)) {
                                nn = drawpoly_math(nn, i, j, ngux, ngdx, nguy, ngdy, nguo, ngdo, du0);
                            }
                        } else {
                            if ((du0 < duj) != (du0 < dui)) {
                                nn = drawpoly_math(nn, i, j, ngux, ngdx, nguy, ngdy, nguo, ngdo, du0);
                            }
                            if ((du1 < duj) != (du1 < dui)) {
                                nn = drawpoly_math(nn, i, j, ngux, ngdx, nguy, ngdy, nguo, ngdo, du1);
                            }
                        }
                        i = j;
                    } while (i != 0);
                    if (nn < 3) {
                        continue;
                    }

                    if (HOM) {
                        gl.glDisable(GL_TEXTURE_2D);
                    }
                    gl.glBegin(GL_TRIANGLE_FAN);
                    for (i = 0; i < nn; i++) {
                        Polygon dpoly = drawpoly[i];
                        ox = dpoly.uu;
                        oy = dpoly.vv;
                        dp = ox * ngdx + oy * ngdy + ngdo;
                        up = ox * ngux + oy * nguy + nguo;
                        vp = ox * ngvx + oy * ngvy + ngvo;
                        r = 1.0 / dp;
                        if (texunits > 0) {
                            j = 0;
                            while (j <= texunits) {
                                gl.glMultiTexCoord2d(GL_TEXTURE0 + j++, (up * r - du0 + uoffs) * ox2, vp * r * oy2);
                            }
                        } else {
                            gl.glTexCoord2d((up * r - du0 + uoffs) * ox2, vp * r * oy2);
                        }
                        gl.glVertex3d((ox - ghalfx) * r * grhalfxdown10x, (ghoriz - oy) * r * grhalfxdown10,
                                r * (1.0 / 1024.0));
                    }
                    gl.glEnd();
                    if (HOM) {
                        gl.glEnable(GL_TEXTURE_2D);
                    }
                }
            }
        } else {
            ox2 *= hackscx;
            oy2 *= hackscy;

            if (HOM) {
                gl.glDisable(GL_TEXTURE_2D);
            }
            gl.glBegin(GL_TRIANGLE_FAN);
            for (i = 0; i < n; i++) {
                Polygon dpoly = drawpoly[i];

                r = 1.0f / dpoly.dd;
                if (texunits > 0) {
                    j = 0;
                    while (j <= texunits) {
                        gl.glMultiTexCoord2d(GL_TEXTURE0 + j++, dpoly.uu * r * ox2, dpoly.vv * r * oy2);
                    }
                } else {
                    gl.glTexCoord2d(dpoly.uu * r * ox2, dpoly.vv * r * oy2);
                }

                gl.glVertex3d((dpoly.px - ghalfx) * r * grhalfxdown10x, (ghoriz - dpoly.py) * r * grhalfxdown10,
                        r * (1.f / 1024.f));
            }
            gl.glEnd();
            if (HOM) {
                gl.glEnable(GL_TEXTURE_2D);
            }
        }

        textureCache.deactivateEffects(gl);

        if (srepeat != 0) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        }
        if (trepeat != 0) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
    }

    private void nonparallaxed(double nx0, double ny0, double nx1, double ny1, double ryp0, double ryp1, float x0,
                               float x1, float cf_y0, float cf_y1, int have_floor, int sectnum, boolean floor) {
        double fx, fy, ox, oy, oz, ox2, oy2, r;
        int i;

        BoardService boardService = engine.getBoardService();
        Sector sec = boardService.getSector(sectnum);

        if ((globalorientation & 64) == 0) {
            nonparallaxed_ft[0] = globalposx;
            nonparallaxed_ft[1] = globalposy;
            nonparallaxed_ft[2] = cosglobalang;
            nonparallaxed_ft[3] = singlobalang;
        } else {
            // relative alignment
            fx = boardService.getWall(boardService.getWall(sec.getWallptr()).getPoint2()).getX() - boardService.getWall(sec.getWallptr()).getX();
            fy = boardService.getWall(boardService.getWall(sec.getWallptr()).getPoint2()).getY() - boardService.getWall(sec.getWallptr()).getY();
            r = 1.0 / sqrt(fx * fx + fy * fy);
            fx *= r;
            fy *= r;

            nonparallaxed_ft[2] = cosglobalang * fx + singlobalang * fy;
            nonparallaxed_ft[3] = singlobalang * fx - cosglobalang * fy;
            nonparallaxed_ft[0] = (globalposx - boardService.getWall(sec.getWallptr()).getX()) * fx + (globalposy - boardService.getWall(sec.getWallptr()).getY()) * fy;
            nonparallaxed_ft[1] = (globalposy - boardService.getWall(sec.getWallptr()).getY()) * fx - (globalposx - boardService.getWall(sec.getWallptr()).getX()) * fy;
            if ((globalorientation & 4) == 0) {
                globalorientation ^= 32;
            } else {
                globalorientation ^= 16;
            }
        }

        gdx = 0;
        gdy = gxyaspect;
        if ((globalorientation & 2) == 0) {
            if (global_cf_z - globalposz != 0) // PK 2012: don't allow div by zero
            {
                gdy /= global_cf_z - globalposz;
            }
        }
        gdo = -ghoriz * gdy;
        if ((globalorientation & 8) != 0) {
            nonparallaxed_ft[0] /= 8;
            nonparallaxed_ft[1] /= -8;
            nonparallaxed_ft[2] /= 2097152;
            nonparallaxed_ft[3] /= 2097152;
        } else {
            nonparallaxed_ft[0] /= 16;
            nonparallaxed_ft[1] /= -16;
            nonparallaxed_ft[2] /= 4194304;
            nonparallaxed_ft[3] /= 4194304;
        }
        gux = nonparallaxed_ft[3] * (viewingrange) / -65536.0;
        gvx = nonparallaxed_ft[2] * (viewingrange) / -65536.0;
        guy = nonparallaxed_ft[0] * gdy;
        gvy = nonparallaxed_ft[1] * gdy;
        guo = nonparallaxed_ft[0] * gdo;
        gvo = nonparallaxed_ft[1] * gdo;
        guo += (nonparallaxed_ft[2] - gux) * ghalfx;
        gvo -= (nonparallaxed_ft[3] + gvx) * ghalfx;

        // Texture flipping
        if ((globalorientation & 4) != 0) {
            r = gux;
            gux = gvx;
            gvx = r;
            r = guy;
            guy = gvy;
            gvy = r;
            r = guo;
            guo = gvo;
            gvo = r;
        }
        if ((globalorientation & 16) != 0) {
            gux = -gux;
            guy = -guy;
            guo = -guo;
        }
        if ((globalorientation & 32) != 0) {
            gvx = -gvx;
            gvy = -gvy;
            gvo = -gvo;
        }

        // Texture panning
        ArtEntry art = getTile(globalpicnum);
        fx = global_cf_xpanning * (1 << art.getSizex()) / 256.0;
        fy = global_cf_ypanning * (1 << art.getSizey()) / 256.0;

        if ((globalorientation & (2 + 64)) == (2 + 64)) // Hack for panning for slopes w/ relative alignment
        {
            r = global_cf_heinum / 4096.0;
            r = 1.0 / sqrt(r * r + 1);
            if ((globalorientation & 4) == 0) {
                fy *= r;
            } else {
                fx *= r;
            }
        }

        guy += gdy * fx;
        guo += gdo * fx;
        gvy += gdy * fy;
        gvo += gdo * fy;

        if ((globalorientation & 2) != 0) // slopes
        {
            nonparallaxed_px[0] = x0;
            nonparallaxed_py[0] = ryp0 + ghoriz;
            nonparallaxed_px[1] = x1;
            nonparallaxed_py[1] = ryp1 + ghoriz;

            // Pick some point guaranteed to be not collinear to the 1st two
            // points
            ox = nx0 + (ny1 - ny0);
            oy = ny0 + (nx0 - nx1);
            ox2 = (oy - globalposy) * gcosang - (ox - globalposx) * gsinang;
            oy2 = (ox - globalposx) * gcosang2 + (oy - globalposy) * gsinang2;
            oy2 = 1.0 / oy2;
            nonparallaxed_px[2] = ghalfx * ox2 * oy2 + ghalfx;
            oy2 *= gyxscale;
            nonparallaxed_py[2] = oy2 + ghoriz;

            for (i = 0; i < 3; i++) {
                nonparallaxed_dd[i] = nonparallaxed_px[i] * gdx + nonparallaxed_py[i] * gdy + gdo;
                nonparallaxed_uu[i] = nonparallaxed_px[i] * gux + nonparallaxed_py[i] * guy + guo;
                nonparallaxed_vv[i] = nonparallaxed_px[i] * gvx + nonparallaxed_py[i] * gvy + gvo;
            }

            nonparallaxed_py[0] = cf_y0;
            nonparallaxed_py[1] = cf_y1;
            if (floor) {
                nonparallaxed_py[2] = (polymost_getflorzofslope(sectnum, ox, oy) - globalposz) * oy2 + ghoriz;
            } else {
                nonparallaxed_py[2] = (polymost_getceilzofslope(sectnum, ox, oy) - globalposz) * oy2 + ghoriz;
            }

            ox = nonparallaxed_py[1] - nonparallaxed_py[2];
            oy = nonparallaxed_py[2] - nonparallaxed_py[0];
            oz = nonparallaxed_py[0] - nonparallaxed_py[1];
            r = 1.0 / (ox * nonparallaxed_px[0] + oy * nonparallaxed_px[1] + oz * nonparallaxed_px[2]);

            gdx = (ox * nonparallaxed_dd[0] + oy * nonparallaxed_dd[1] + oz * nonparallaxed_dd[2]) * r;
            gux = (ox * nonparallaxed_uu[0] + oy * nonparallaxed_uu[1] + oz * nonparallaxed_uu[2]) * r;
            gvx = (ox * nonparallaxed_vv[0] + oy * nonparallaxed_vv[1] + oz * nonparallaxed_vv[2]) * r;
            ox = nonparallaxed_px[2] - nonparallaxed_px[1];
            oy = nonparallaxed_px[0] - nonparallaxed_px[2];
            oz = nonparallaxed_px[1] - nonparallaxed_px[0];
            gdy = (ox * nonparallaxed_dd[0] + oy * nonparallaxed_dd[1] + oz * nonparallaxed_dd[2]) * r;
            guy = (ox * nonparallaxed_uu[0] + oy * nonparallaxed_uu[1] + oz * nonparallaxed_uu[2]) * r;
            gvy = (ox * nonparallaxed_vv[0] + oy * nonparallaxed_vv[1] + oz * nonparallaxed_vv[2]) * r;
            gdo = nonparallaxed_dd[0] - nonparallaxed_px[0] * gdx - nonparallaxed_py[0] * gdy;
            guo = nonparallaxed_uu[0] - nonparallaxed_px[0] * gux - nonparallaxed_py[0] * guy;
            gvo = nonparallaxed_vv[0] - nonparallaxed_px[0] * gvx - nonparallaxed_py[0] * gvy;

            if ((globalorientation & 64) != 0) // Hack for relative alignment on slopes
            {
                r = global_cf_heinum / 4096.0;
                r = sqrt(r * r + 1);
                if ((globalorientation & 4) == 0) {
                    gvx *= r;
                    gvy *= r;
                    gvo *= r;
                } else {
                    gux *= r;
                    guy *= r;
                    guo *= r;
                }
            }
        }

        clipper.setMethod((globalorientation >> 7) & 3);
        if (have_floor != 0) {
            if (globalposz >= polymost_getflorzofslope(sectnum, globalposx, globalposy)) {
                clipper.setMethod(-1); // Back-face culling
            }
        } else {
            if (globalposz <= polymost_getceilzofslope(sectnum, globalposx, globalposy)) {
                clipper.setMethod(-1); // Back-face culling
            }
        }

        calc_and_apply_fog(global_cf_shade, sec.getVisibility(), global_cf_pal);

        pow2xsplit = 0;
        if (have_floor != 0) {
            clipper.domost(x0, cf_y0, x1, cf_y1); // flor
        } else {
            clipper.domost(x1, cf_y1, x0, cf_y0); // ceil
        }

        clipper.setMethod(0);
    }

    private void calc_ypanning(int refposz, double ryp0, double ryp1, double x0, double x1, short ypan, short yrepeat,
                               boolean dopancor) {
        double t0 = (refposz - globalposz) * ryp0 + ghoriz;
        double t1 = (refposz - globalposz) * ryp1 + ghoriz;
        double t = ((gdx * x0 + gdo) * yrepeat) / ((x1 - x0) * ryp0 * 2048.f);

        ArtEntry pic = getTile(globalpicnum);
        int i = (1 << pic.getSizey());
        if (i < pic.getHeight()) {
            i <<= 1;
        }

        if (GLInfo.texnpot != 0) {
            if (!dopancor) // texture scaled, it's need to fix
            {
                t *= (float) pic.getHeight() / i;
            }
            i = pic.getHeight();
        }
//		else if (dopancor && defs != null && defs.texInfo.isHighTile(globalpicnum)) {
//			// Carry out panning "correction" to make it look like classic in some
//	        // cases, but failing in the general case.
//
//	    	int yoffs = (int) ((i - tilesizy[globalpicnum]) * (255.0f / i));
//			if (ypan > 256 - yoffs)
//				ypan -= yoffs;
//		}

        float fy = ypan * i / 256.0f;
        gvx = (t0 - t1) * t;
        gvy = (x1 - x0) * t;
        gvo = -gvx * x0 - gvy * t0 + fy * gdo;
        gvx += fy * gdx;
        gvy += fy * gdy;
    }

    private void drawalls(int bunch) {
        Sector sec, nextsec;
        Wall wal, wal2;
        float x0, x1, cy0, cy1, fy0, fy1, xp0, yp0, xp1, yp1, ryp0, ryp1, nx0, ny0, nx1, ny1;
        float t, t0, t1, ocy0, ocy1, ofy0, ofy1, oxp0, oyp0, fwalxrepeat;
        double oguo, ogux, oguy;
        int i, x, y, z, wallnum, sectnum, nextsectnum;

        BoardService boardService = engine.getBoardService();
        sectnum = thesector[bunchfirst[bunch]];
        sec = boardService.getSector(sectnum);

        calc_and_apply_fog(sec.getFloorshade(), sec.getVisibility(), sec.getFloorpal());

        for (z = bunchfirst[bunch]; z >= 0; z = p2[z]) {
            // DRAW WALLS SECTION!

            wallnum = thewall[z];

            wal = boardService.getWall(wallnum);
            wal2 = boardService.getWall(wal.getPoint2());
            nextsectnum = wal.getNextsector();
            nextsec = nextsectnum >= 0 ? boardService.getSector(nextsectnum) : null;

            fwalxrepeat = wal.getXrepeat() & 0xFF;

            // Offset&Rotate 3D coordinates to screen 3D space
            x = wal.getX() - globalposx;
            y = wal.getY() - globalposy;
            xp0 = y * gcosang - x * gsinang;
            yp0 = x * gcosang2 + y * gsinang2;
            x = wal2.getX() - globalposx;
            y = wal2.getY() - globalposy;
            xp1 = y * gcosang - x * gsinang;
            yp1 = x * gcosang2 + y * gsinang2;

            oxp0 = xp0;
            oyp0 = yp0;

            // Clip to close parallel-screen plane
            if (yp0 < SCISDIST) {
                if (yp1 < SCISDIST) {
                    continue;
                }
                t0 = (SCISDIST - yp0) / (yp1 - yp0);
                xp0 = (xp1 - xp0) * t0 + xp0;
                yp0 = SCISDIST;
                nx0 = (wal2.getX() - wal.getX()) * t0 + wal.getX();
                ny0 = (wal2.getY() - wal.getY()) * t0 + wal.getY();
            } else {
                t0 = 0.f;
                nx0 = wal.getX();
                ny0 = wal.getY();
            }
            if (yp1 < SCISDIST) {
                t1 = (SCISDIST - oyp0) / (yp1 - oyp0);
                xp1 = (xp1 - oxp0) * t1 + oxp0;
                yp1 = SCISDIST;
                nx1 = (wal2.getX() - wal.getX()) * t1 + wal.getX();
                ny1 = (wal2.getY() - wal.getY()) * t1 + wal.getY();
            } else {
                t1 = 1.f;
                nx1 = wal2.getX();
                ny1 = wal2.getY();
            }

            ryp0 = 1.f / yp0;
            ryp1 = 1.f / yp1;

            // Generate screen coordinates for front side of wall
            x0 = ghalfx * xp0 * ryp0 + ghalfx;
            x1 = ghalfx * xp1 * ryp1 + ghalfx;
            if (x1 <= x0) {
                continue;
            }

            ryp0 *= gyxscale;
            ryp1 *= gyxscale;

            polymost_getzsofslope(sectnum, nx0, ny0);
            cy0 = (dceilzsofslope - globalposz) * ryp0 + ghoriz;
            fy0 = (dfloorzsofslope - globalposz) * ryp0 + ghoriz;
            polymost_getzsofslope(sectnum, nx1, ny1);
            cy1 = (dceilzsofslope - globalposz) * ryp1 + ghoriz;
            fy1 = (dfloorzsofslope - globalposz) * ryp1 + ghoriz;

            { // DRAW FLOOR
                renderingType = RenderingType.Floor.setIndex(sectnum);
                globalpicnum = sec.getFloorpicnum();
                globalshade = sec.getFloorshade();
                globalpal = sec.getFloorpal();
                globalorientation = sec.getFloorstat();
                if (getTile(globalpicnum).getType() != AnimType.NONE) {
                    globalpicnum += animateoffs(globalpicnum, sectnum);
                }

                global_cf_shade = sec.getFloorshade();
                global_cf_pal = sec.getFloorpal();
                global_cf_z = sec.getFloorz();
                global_cf_xpanning = sec.getFloorxpanning();
                global_cf_ypanning = sec.getFloorypanning();
                global_cf_heinum = sec.getFloorheinum();

                if ((globalorientation & 1) == 0) {
                    nonparallaxed(nx0, ny0, nx1, ny1, ryp0, ryp1, x0, x1, fy0, fy1, 1, sectnum, true);
                } else if ((nextsectnum < 0) || ((boardService.getSector(nextsectnum).getFloorstat() & 1) == 0)) {
                    drawbackground(sectnum, x0, x1, fy0, fy1, true);
                }

            } // END DRAW FLOOR

            { // DRAW CEILING
                renderingType = RenderingType.Ceiling.setIndex(sectnum);
                globalpicnum = sec.getCeilingpicnum();
                globalshade = sec.getCeilingshade();
                globalpal = sec.getCeilingpal() & 0xFF;
                globalorientation = sec.getCeilingstat();
                if (getTile(globalpicnum).getType() != AnimType.NONE) {
                    globalpicnum += animateoffs(globalpicnum, sectnum);
                }

                global_cf_shade = sec.getCeilingshade();
                global_cf_pal = sec.getCeilingpal();
                global_cf_z = sec.getCeilingz();
                global_cf_xpanning = sec.getCeilingxpanning();
                global_cf_ypanning = sec.getCeilingypanning();
                global_cf_heinum = sec.getCeilingheinum();

                if ((globalorientation & 1) == 0) {
                    nonparallaxed(nx0, ny0, nx1, ny1, ryp0, ryp1, x0, x1, cy0, cy1, 0, sectnum, false);
                } else if ((nextsectnum < 0) || ((boardService.getSector(nextsectnum).getCeilingstat() & 1) == 0)) {
                    drawbackground(sectnum, x0, x1, cy0, cy1, false);
                }

            } // END DRAW CEILING

            renderingType = RenderingType.Wall.setIndex(wallnum);
            gdx = (ryp0 - ryp1) * gxyaspect / (x0 - x1);
            gdy = 0;
            gdo = ryp0 * gxyaspect - gdx * x0;
            gux = (t0 * ryp0 - t1 * ryp1) * gxyaspect * fwalxrepeat * 8.0 / (x0 - x1);
            guo = t0 * ryp0 * gxyaspect * fwalxrepeat * 8.0 - gux * x0;
            guo += wal.getXpanning() * gdo;
            gux += wal.getXpanning() * gdx;
            guy = 0;
            ogux = gux;
            oguy = guy;
            oguo = guo;

            if (nextsectnum >= 0) {
                polymost_getzsofslope(nextsectnum, nx0, ny0);
                ocy0 = (dceilzsofslope - globalposz) * ryp0 + ghoriz;
                ofy0 = (dfloorzsofslope - globalposz) * ryp0 + ghoriz;
                polymost_getzsofslope(nextsectnum, nx1, ny1);
                ocy1 = (dceilzsofslope - globalposz) * ryp1 + ghoriz;
                ofy1 = (dfloorzsofslope - globalposz) * ryp1 + ghoriz;

                if ((wal.getCstat() & 48) == 16) {
                    maskwall[maskwallcnt++] = (short) z;
                }

                if (((cy0 < ocy0) || (cy1 < ocy1))
                        && (((sec.getCeilingstat() & boardService.getSector(nextsectnum).getCeilingstat()) & 1)) == 0) {
                    globalpicnum = wal.getPicnum();
                    globalshade = wal.getShade();
                    globalpal = wal.getPal() & 0xFF;
                    if (getTile(globalpicnum).getType() != AnimType.NONE) {
                        globalpicnum += animateoffs(globalpicnum, wallnum + 16384);
                    }

                    if (!wal.isBottomAligned()) {
                        i = boardService.getSector(nextsectnum).getCeilingz();
                    } else {
                        i = sec.getCeilingz();
                    }

                    // over
                    calc_ypanning(i, ryp0, ryp1, x0, x1, wal.getYpanning(), wal.getYrepeat(), wal.isBottomAligned());

                    if (wal.isXFlip()) {
                        t = fwalxrepeat * 8 + wal.getXpanning() * 2;
                        gux = gdx * t - gux;
                        guy = gdy * t - guy;
                        guo = gdo * t - guo;
                    }

                    if (wal.isYFlip()) {
                        gvx = -gvx;
                        gvy = -gvy;
                        gvo = -gvo;
                    }

                    int shade = wal.getShade();
                    calc_and_apply_fog(shade, sec.getVisibility(), sec.getFloorpal());

                    pow2xsplit = 1;
                    clipper.domost(x1, ocy1, x0, ocy0);
                    if (wal.isXFlip()) {
                        gux = ogux;
                        guy = oguy;
                        guo = oguo;
                    }
                }
                if (((ofy0 < fy0) || (ofy1 < fy1)) && (((sec.getFloorstat() & boardService.getSector(nextsectnum).getFloorstat()) & 1)) == 0) {
                    if (!wal.isSwapped()) {
                        drawalls_nwal.set(wal);
                    } else {
                        drawalls_nwal.set(boardService.getWall(wal.getNextwall()));
                        guo += (drawalls_nwal.getXpanning() - wal.getXpanning()) * gdo;
                        gux += (drawalls_nwal.getXpanning() - wal.getXpanning()) * gdx;
                        guy += (drawalls_nwal.getXpanning() - wal.getXpanning()) * gdy;
                    }
                    globalpicnum = drawalls_nwal.getPicnum();
                    globalshade = drawalls_nwal.getShade();
                    globalpal = drawalls_nwal.getPal() & 0xFF;
                    if (getTile(globalpicnum).getType() != AnimType.NONE) {
                        globalpicnum += animateoffs(globalpicnum, wallnum + 16384);
                    }

                    if (!drawalls_nwal.isBottomAligned()) {
                        i = boardService.getSector(nextsectnum).getFloorz();
                    } else {
                        i = sec.getCeilingz();
                    }

                    // under
                    calc_ypanning(i, ryp0, ryp1, x0, x1, drawalls_nwal.getYpanning(), wal.getYrepeat(),
                            !drawalls_nwal.isBottomAligned());

                    if (wal.isXFlip()) {
                        t = (fwalxrepeat * 8 + drawalls_nwal.getXpanning() * 2);
                        gux = gdx * t - gux;
                        guy = gdy * t - guy;
                        guo = gdo * t - guo;
                    }
                    if (drawalls_nwal.isYFlip()) {
                        gvx = -gvx;
                        gvy = -gvy;
                        gvo = -gvo;
                    } // yflip

                    int shade = drawalls_nwal.getShade();
                    calc_and_apply_fog(shade, sec.getVisibility(), sec.getFloorpal());

                    pow2xsplit = 1;
                    clipper.domost(x0, ofy0, x1, ofy1);
                    if ((wal.getCstat() & (2 + 8)) != 0) {
                        guo = oguo;
                        gux = ogux;
                        guy = oguy;
                    }
                }
            }

            if ((nextsectnum < 0) || wal.isOneWay()) // White/1-way wall
            {
                do {
                    boolean maskingOneWay = (nextsectnum >= 0 && wal.isOneWay());
                    if (maskingOneWay) {
                        if (getclosestpointonwall(globalposx, globalposy, wallnum, projPoint) == 0
                                && klabs(globalposx - (int) projPoint.x) + klabs(globalposy - (int) projPoint.y) <= 128) {
                            break;
                        }
                    }

                    globalpicnum = (nextsectnum < 0) ? wal.getPicnum() : wal.getOverpicnum();
                    globalshade = wal.getShade();
                    globalpal = wal.getPal() & 0xFF;
                    if (getTile(globalpicnum).getType() != AnimType.NONE) {
                        globalpicnum += animateoffs(globalpicnum, wallnum + 16384);
                    }

                    boolean nwcs4 = !wal.isBottomAligned();

                    if (nextsectnum >= 0) {
                        i = nwcs4 ? nextsec.getCeilingz() : sec.getCeilingz();
                    } else {
                        i = nwcs4 ? sec.getCeilingz() : sec.getFloorz();
                    }

                    // white
                    calc_ypanning(i, ryp0, ryp1, x0, x1, wal.getYpanning(), wal.getYrepeat(), nwcs4 && !maskingOneWay);

                    if (wal.isXFlip()) {
                        t = (fwalxrepeat * 8 + wal.getXpanning() * 2);
                        gux = gdx * t - gux;
                        guy = gdy * t - guy;
                        guo = gdo * t - guo;
                    }
                    if (wal.isYFlip()) {
                        gvx = -gvx;
                        gvy = -gvy;
                        gvo = -gvo;
                    }

                    int shade = wal.getShade();
                    calc_and_apply_fog(shade, sec.getVisibility(), sec.getFloorpal());
                    pow2xsplit = 1;
                    clipper.domost(x0, cy0, x1, cy1);
                } while (false);

            }

            if (nextsectnum >= 0) {
                if (((gotsector[nextsectnum >> 3] & pow2char[nextsectnum & 7]) == 0)
                        && (clipper.testvisiblemost(x0, x1) != 0)) {
                    scansector(nextsectnum);
                }
            }
        }
    }

    private int polymost_bunchfront(int b1, int b2) {
        double x1b1, x1b2, x2b1, x2b2;
        int b1f, b2f, i;

        b1f = bunchfirst[b1];
        x1b1 = dxb1[b1f];
        x2b2 = dxb2[bunchlast[b2]];
        if (x1b1 >= x2b2) {
            return (-1);
        }
        b2f = bunchfirst[b2];
        x1b2 = dxb1[b2f];
        x2b1 = dxb2[bunchlast[b1]];
        if (x1b2 >= x2b1) {
            return (-1);
        }

        if (x1b1 >= x1b2) {
            for (i = b2f; dxb2[i] <= x1b1 && p2[i] != -1; i = p2[i]) {
                ;
            }
            return (wallfront(b1f, i));
        }

        for (i = b1f; dxb2[i] <= x1b2 && p2[i] != -1; i = p2[i]) {
            ;
        }
        return (wallfront(i, b2f));
    }

    protected int wallfront(int l1, int l2) {
        Wall wal;
        int x11, y11, x21, y21, x12, y12, x22, y22, dx, dy, t1, t2;
        BoardService boardService = engine.getBoardService();

        wal = boardService.getWall(thewall[l1]);
        x11 = wal.getX();
        y11 = wal.getY();
        wal = boardService.getWall(wal.getPoint2());
        x21 = wal.getX();
        y21 = wal.getY();
        wal = boardService.getWall(thewall[l2]);
        x12 = wal.getX();
        y12 = wal.getY();
        wal = boardService.getWall(wal.getPoint2());
        x22 = wal.getX();
        y22 = wal.getY();

        dx = x21 - x11;
        dy = y21 - y11;
        t1 = dmulscale(x12 - x11, dy, -dx, y12 - y11, 2); // p1(l2) vs. l1
        t2 = dmulscale(x22 - x11, dy, -dx, y22 - y11, 2); // p2(l2) vs. l1
        if (t1 == 0) {
            t1 = t2;
            if (t1 == 0) {
                return (-1);
            }
        }
        if (t2 == 0) {
            t2 = t1;
        }
        if ((t1 ^ t2) >= 0) {
            t2 = dmulscale(globalposx - x11, dy, -dx, globalposy - y11, 2); // pos vs. l1
            return ((t2 ^ t1) >= 0 ? 1 : 0);
        }

        dx = x22 - x12;
        dy = y22 - y12;
        t1 = dmulscale(x11 - x12, dy, -dx, y11 - y12, 2); // p1(l1) vs. l2
        t2 = dmulscale(x21 - x12, dy, -dx, y21 - y12, 2); // p2(l1) vs. l2
        if (t1 == 0) {
            t1 = t2;
            if (t1 == 0) {
                return (-1);
            }
        }
        if (t2 == 0) {
            t2 = t1;
        }
        if ((t1 ^ t2) >= 0) {
            t2 = dmulscale(globalposx - x12, dy, -dx, globalposy - y12, 2); // pos vs. l2
            return ((t2 ^ t1) < 0 ? 1 : 0);
        }
        return (-2);
    }

    private void scansector(int sectnum) {
        double d, xp1, yp1, xp2, yp2;
        Sprite spr;
        int zz, startwall, numscansbefore, scanfirst, bunchfrst, nextsectnum;
        int xs, ys, x1, y1, x2, y2;

        if (sectnum < 0) {
            return;
        }

        if (automapping == 1) {
            show2dsector.setBit(sectnum);
        }

        BoardService boardService = engine.getBoardService();
        sectorborder[0] = sectnum;
        int sectorbordercnt = 1;
        do {
            sectnum = sectorborder[--sectorbordercnt];

            for (ListNode<Sprite> node = boardService.getSectNode(sectnum); node != null; node = node.getNext()) {
                int z = node.getIndex();
                spr = node.get();
                if ((((spr.getCstat() & 0x8000) == 0) || showinvisibility) && (spr.getXrepeat() > 0) && (spr.getYrepeat() > 0)) {
                    xs = spr.getX() - globalposx;
                    ys = spr.getY() - globalposy;
                    if (((spr.getCstat() & 48) != 0) || (xs * gcosang + ys * gsinang > 0)
                            || (config.isUseModels() && modelManager.hasModelInfo(spr.getPicnum()))) {
                        if ((spr.getCstat() & (64 + 48)) != (64 + 16) || dmulscale(EngineUtils.cos(spr.getAng()), -xs,
                                EngineUtils.sin(spr.getAng()), -ys, 6) > 0) {

                            addRenderedSprite(z);
                        }
                    }
                }
            }

            gotsector[sectnum >> 3] |= pow2char[sectnum & 7];

            bunchfrst = numbunches;
            numscansbefore = numscans;

            if (boardService.getSector(sectnum) == null) {
                continue;
            }

            Sector sec = boardService.getSector(sectnum);
            scanfirst = numscans;
            xp2 = 0;
            yp2 = 0;

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wal = wn.get();
                int wallid = wn.getIndex();
                Wall wal2 = wal.getWall2();

                x1 = wal.getX() - globalposx;
                y1 = wal.getY() - globalposy;
                x2 = wal2.getX() - globalposx;
                y2 = wal2.getY() - globalposy;

                nextsectnum = wal.getNextsector(); // Scan close sectors

                if ((nextsectnum >= 0) && !wal.isOneWay() && sectorbordercnt < sectorborder.length
                        && ((gotsector[nextsectnum >> 3] & pow2char[nextsectnum & 7]) == 0)) {
                    d = x1 * y2 - x2 * y1;
                    xp1 = (x2 - x1);
                    yp1 = (y2 - y1);
                    if (d * d <= (xp1 * xp1 + yp1 * yp1) * (SCISDIST * SCISDIST * 260.0)) {
                        sectorborder[sectorbordercnt++] = nextsectnum;
                        gotsector[nextsectnum >> 3] |= pow2char[nextsectnum & 7];
                    }
                }

                if ((wallid == sec.getWallptr()) || (boardService.getWall(wallid - 1).getPoint2() != wallid)) {
                    xp1 = ((double) y1 * cosglobalang - (double) x1 * singlobalang) / 64.0;
                    yp1 = ((double) x1 * cosviewingrangeglobalang + (double) y1 * sinviewingrangeglobalang) / 64.0;
                } else {
                    xp1 = xp2;
                    yp1 = yp2;
                }
                xp2 = ((double) y2 * cosglobalang - (double) x2 * singlobalang) / 64.0;
                yp2 = ((double) x2 * cosviewingrangeglobalang + (double) y2 * sinviewingrangeglobalang) / 64.0;

                if ((yp1 >= SCISDIST) || (yp2 >= SCISDIST))
                    /* crossProduct */ {
                    if (xp1 * yp2 < xp2 * yp1) // if wall is facing you...
                    {
                        if (inviewingrange(xp1, yp1, xp2, yp2)) {
                            if (numscans >= MAXWALLSB - 1) {
                                return;
                            }

                            thesector[numscans] = (short) sectnum;
                            thewall[numscans] = (short) wallid;
                            p2[numscans] = (short) (numscans + 1);
                            numscans++;
                        }
                    }
                }

                if ((wal.getPoint2() < wallid) && (scanfirst < numscans)) {
                    p2[numscans - 1] = (short) scanfirst;
                    scanfirst = numscans;
                }
            }

            for (int z = numscansbefore; z < numscans; z++) {
                if ((boardService.getWall(thewall[z]).getPoint2() != thewall[p2[z]]) || (dxb2[z] > dxb1[p2[z]])) {
                    bunchfirst[numbunches++] = p2[z];
                    p2[z] = -1;
                }
            }

            for (int z = bunchfrst; z < numbunches; z++) {
                for (zz = bunchfirst[z]; p2[zz] >= 0; zz = p2[zz]) {
                    ;
                }
                bunchlast[z] = (short) zz;
            }
        } while (sectorbordercnt > 0);
    }

    protected boolean inviewingrange(double xp1, double yp1, double xp2, double yp2) {
        if (yp1 >= SCISDIST) {
//			if ((xp1 > yp1) || (yp1 == 0)) //disabled, viewving range is 180degs
//				return false;
            dxb1[numscans] = xp1 * ghalfx / yp1 + ghalfx;
        } else {
            dxb1[numscans] = -1e32;
        }

        if (yp2 >= SCISDIST) {
//			if ((xp2 < -yp2) || (yp2 == 0))
//				return false;

            dxb2[numscans] = xp2 * ghalfx / yp2 + ghalfx;
        } else {
            dxb2[numscans] = 1e32;
        }

        return dxb1[numscans] < dxb2[numscans];
    }

    private void drawpapersky(int sectnum, double x0, double x1, double y0, double y1, boolean floor) {
        double ox, oy, t;

        short[] dapskyoff = zeropskyoff;
        int dapskybits = pskybits;

        if (dapskybits < 0) {
            dapskybits = 0;
        }

        // Use clamping for tiled sky textures
        for (int i = (1 << dapskybits) - 1; i > 0; i--) {
            if (dapskyoff[i] != dapskyoff[i - 1]) {
                skyclamphack = r_parallaxskyclamping;
                break;
            }
        }

        BoardService boardService = engine.getBoardService();
        ArtEntry pic = getTile(globalpicnum);

        Sector sec = boardService.getSector(sectnum);
        drawalls_dd[0] = xdimen * .0000001; // Adjust sky depth based on screen size!
        t = pic.getWidth() << dapskybits; // (1 << (picsiz[globalpicnum] & 15)) << dapskybits;

        drawalls_vv[1] = drawalls_dd[0] * (xdimscale * (double) viewingrange) / (65536.0 * 65536.0);
        drawalls_vv[0] = drawalls_dd[0]
                * ((pic.getHeight() >> 1)
                + ((floor && r_parallaxskypanning == 1) ? parallaxyoffs - pic.getHeight() : parallaxyoffs))
                - drawalls_vv[1] * ghoriz;
        int i = (1 << pic.getSizey());
        if (i != pic.getHeight()) {
            i += i;
        }

        // Hack to draw black rectangle below sky when looking up/down...
        gdx = 0;
        if (floor) {
            gdy = gxyaspect / 262144.0;
        } else {
            gdy = gxyaspect / -262144.0;
        }
        gdo = -ghoriz * gdy;
        gux = 0;
        guy = 0;
        guo = 0;
        gvx = 0;
        gvy = 0;
        gvo = 0;

        int oskyclamphack = skyclamphack;
        skyclamphack = 0;
        if (floor) {
            oy = ((pic.getHeight()) * drawalls_dd[0] - drawalls_vv[0]) / drawalls_vv[1];

            if ((oy > y0) && (oy > y1)) {
                clipper.domost((float) x0, (float) oy, (float) x1, (float) oy);
            } else if ((oy > y0) != (oy > y1)) {
                // fy0 fy1
                // \ /
                // oy---------- oy----------
                // \ /
                // fy1 fy0
                ox = (oy - y0) * (x1 - x0) / (y1 - y0) + x0;
                if (oy > y0) {
                    clipper.domost((float) x0, (float) oy, (float) ox, (float) oy);
                    clipper.domost((float) ox, (float) oy, (float) x1, (float) y1);
                } else {
                    clipper.domost((float) x0, (float) y0, (float) ox, (float) oy);
                    clipper.domost((float) ox, (float) oy, (float) x1, (float) oy);
                }
            } else {
                clipper.domost((float) x0, (float) y0, (float) x1, (float) y1);
            }
        } else {
            oy = -drawalls_vv[0] / drawalls_vv[1];

            if ((oy < y0) && (oy < y1)) {
                clipper.domost((float) x1, (float) oy, (float) x0, (float) oy);
            } else if ((oy < y0) != (oy < y1)) {
                /*
                 * cy1 cy0 // / \ //oy---------- oy--------- // / \ // cy0 cy1
                 */
                ox = (oy - y0) * (x1 - x0) / (y1 - y0) + x0;
                if (oy < y0) {
                    clipper.domost((float) ox, (float) oy, (float) x0, (float) oy);
                    clipper.domost((float) x1, (float) y1, (float) ox, (float) oy);
                } else {
                    clipper.domost((float) ox, (float) oy, (float) x0, (float) y0);
                    clipper.domost((float) x1, (float) oy, (float) ox, (float) oy);
                }
            } else {
                clipper.domost((float) x1, (float) y1, (float) x0, (float) y0);
            }
        }
        skyclamphack = oskyclamphack;

        double panning = sec.getCeilingypanning();
        if (floor) {
            panning = sec.getFloorypanning();
        }

        if (r_parallaxskypanning != 0) {
            drawalls_vv[0] += drawalls_dd[0] * panning * i / 256.0;
        }

        gdx = 0;
        gdy = 0;
        gdo = drawalls_dd[0];
        gux = gdo * (t * xdimscale * yxaspect * viewingrange) / (16384.0 * 65536.0 * 65536.0 * 5.0 * 1024.0);
        guy = 0; // guo calculated later
        gvx = 0;
        gvy = drawalls_vv[1];
        gvo = drawalls_vv[0];

        i = globalpicnum;
        double r = (y1 - y0) / (x1 - x0); // slope of line
        oy = viewingrange / (ghalfx * 256.0);
        double oz = 1 / oy;

        int y = ((int) (((x0 - ghalfx) * oy) + globalang) >> (11 - dapskybits));

        panning = sec.getCeilingxpanning();
        if (floor) {
            panning = sec.getFloorxpanning();
        }

        double fx = x0;
        do {
            globalpicnum = (short) (dapskyoff[y & ((1 << dapskybits) - 1)] + i);

            guo = gdo * (t * (globalang - (y << (11 - dapskybits))) / 2048.0
                    + ((r_parallaxskypanning != 0) ? panning : 0)) - gux * ghalfx;
            y++;
            ox = fx;
            fx = ((y << (11 - dapskybits)) - globalang) * oz + ghalfx;
            if (fx > x1) {
                fx = x1;
                i = -1;
            }

            pow2xsplit = 0;
            if (floor) {
                clipper.domost((float) ox, (float) ((ox - x0) * r + y0), (float) fx, (float) ((fx - x0) * r + y0));
            } else {
                clipper.domost((float) fx, (float) ((fx - x0) * r + y0), (float) ox, (float) ((ox - x0) * r + y0));
            }
        } while (i >= 0);

    }

    private void drawskybox(double x0, double x1, double y0, double y1, boolean floor) {
        double ox, oy, t;
        int x, y;
        double _xp0, _yp0, _xp1, _yp1, _oxp0, _oyp0, _t0, _t1;
        double _ryp0, _ryp1, _x0, _x1, _cy0, _fy0, _cy1, _fy1, _ox0, _ox1;

        pow2xsplit = 0;
        skyclamphack = 1;

        for (int i = 0; i < 4; i++) {
            x = skywalx[i & 3];
            y = skywaly[i & 3];
            _xp0 = (double) y * gcosang - (double) x * gsinang;
            _yp0 = (double) x * gcosang2 + (double) y * gsinang2;
            x = skywalx[(i + 1) & 3];
            y = skywaly[(i + 1) & 3];
            _xp1 = (double) y * gcosang - (double) x * gsinang;
            _yp1 = (double) x * gcosang2 + (double) y * gsinang2;

            _oxp0 = _xp0;
            _oyp0 = _yp0;

            // Clip to close parallel-screen plane
            if (_yp0 < SCISDIST) {
                if (_yp1 < SCISDIST) {
                    continue;
                }
                _t0 = (SCISDIST - _yp0) / (_yp1 - _yp0);
                _xp0 = (_xp1 - _xp0) * _t0 + _xp0;
                _yp0 = SCISDIST;
            } else {
                _t0 = 0.f;
            }
            if (_yp1 < SCISDIST) {
                _t1 = (SCISDIST - _oyp0) / (_yp1 - _oyp0);
                _xp1 = (_xp1 - _oxp0) * _t1 + _oxp0;
                _yp1 = SCISDIST;
            } else {
                _t1 = 1.f;
            }

            _ryp0 = 1.f / _yp0;
            _ryp1 = 1.f / _yp1;

            // Generate screen coordinates for front side of wall
            _x0 = ghalfx * _xp0 * _ryp0 + ghalfx;
            _x1 = ghalfx * _xp1 * _ryp1 + ghalfx;
            if (_x1 <= _x0) {
                continue;
            }
            if ((_x0 >= x1) || (x0 >= _x1)) {
                continue;
            }

            _ryp0 *= gyxscale;
            _ryp1 *= gyxscale;

            _cy0 = -8192.f * _ryp0 + ghoriz;
            _fy0 = 8192.f * _ryp0 + ghoriz;
            _cy1 = -8192.f * _ryp1 + ghoriz;
            _fy1 = 8192.f * _ryp1 + ghoriz;

            _ox0 = _x0;
            _ox1 = _x1;

            // Make sure: x0<=_x0<_x1<=_x1
            double ny0 = y0;
            double ny1 = y1;
            if (_x0 < x0) {
                t = (x0 - _x0) / (_x1 - _x0);
                _cy0 += (_cy1 - _cy0) * t;
                _fy0 += (_fy1 - _fy0) * t;
                _x0 = x0;
            } else if (_x0 > x0) {
                ny0 += (_x0 - x0) * (y1 - y0) / (x1 - x0);
            }
            if (_x1 > x1) {
                t = (x1 - _x1) / (_x1 - _x0);
                _cy1 += (_cy1 - _cy0) * t;
                _fy1 += (_fy1 - _fy0) * t;
                _x1 = x1;
            } else if (_x1 < x1) {
                ny1 += (_x1 - x1) * (y1 - y0) / (x1 - x0);
            }

            // floor of skybox

            drawalls_ft[0] = 512 / 16.0f;
            drawalls_ft[1] = -512 / -16.0f;
            if (floor) {
                drawalls_ft[1] = 512 / -16.0f;
            }

            drawalls_ft[2] = (cosglobalang) * (1.f / 2147483648.f);
            drawalls_ft[3] = (singlobalang) * (1.f / 2147483648.f);
            gdx = 0;
            gdy = gxyaspect * -(1.0 / 4194304.0);
            if (floor) {
                gdy = gxyaspect * (1.0 / 4194304.0);
            }
            gdo = -ghoriz * gdy;
            gux = drawalls_ft[3] * (viewingrange) / -65536.0;
            gvx = drawalls_ft[2] * (viewingrange) / -65536.0;
            guy = drawalls_ft[0] * gdy;
            gvy = drawalls_ft[1] * gdy;
            guo = drawalls_ft[0] * gdo;
            gvo = drawalls_ft[1] * gdo;
            guo += (drawalls_ft[2] - gux) * ghalfx;
            gvo -= (drawalls_ft[3] + gvx) * ghalfx;

            if (floor) {
                gvx = -gvx;
                gvy = -gvy;
                gvo = -gvo; // y-flip skybox floor

                RenderingType.Skybox.setIndex(6); // floor/6th texture/index 4 of skybox

                if ((_fy0 > ny0) && (_fy1 > ny1)) {
                    clipper.domost((float) _x0, (float) _fy0, (float) _x1, (float) _fy1);
                } else if ((_fy0 > ny0) != (_fy1 > ny1)) {
                    t = (_fy0 - ny0) / (ny1 - ny0 - _fy1 + _fy0);
                    ox = _x0 + (_x1 - _x0) * t;
                    oy = _fy0 + (_fy1 - _fy0) * t;
                    if (ny0 > _fy0) {
                        clipper.domost((float) _x0, (float) ny0, (float) ox, (float) oy);
                        clipper.domost((float) ox, (float) oy, (float) _x1, (float) _fy1);
                    } else {
                        clipper.domost((float) _x0, (float) _fy0, (float) ox, (float) oy);
                        clipper.domost((float) ox, (float) oy, (float) _x1, (float) ny1);
                    }
                } else {
                    clipper.domost((float) _x0, (float) ny0, (float) _x1, (float) ny1);
                }
            } else {

                RenderingType.Skybox.setIndex(5); // ceiling/5th texture/index 4 of skybox

                if ((_cy0 < ny0) && (_cy1 < ny1)) {
                    clipper.domost((float) _x1, (float) _cy1, (float) _x0, (float) _cy0);
                } else if ((_cy0 < ny0) != (_cy1 < ny1)) {
                    t = (_cy0 - ny0) / (ny1 - ny0 - _cy1 + _cy0);
                    ox = _x0 + (_x1 - _x0) * t;
                    oy = _cy0 + (_cy1 - _cy0) * t;
                    if (ny0 < _cy0) {
                        clipper.domost((float) ox, (float) oy, (float) _x0, (float) ny0);
                        clipper.domost((float) _x1, (float) _cy1, (float) ox, (float) oy);
                    } else {
                        clipper.domost((float) ox, (float) oy, (float) _x0, (float) _cy0);
                        clipper.domost((float) _x1, (float) ny1, (float) ox, (float) oy);
                    }
                } else {
                    clipper.domost((float) _x1, (float) ny1, (float) _x0, (float) ny0);
                }

            }

            // wall of skybox
            RenderingType.Skybox.setIndex(i + 1); // i+1th texture/index i of skybox

            gdx = (_ryp0 - _ryp1) * gxyaspect * (1.0 / 512.0) / (_ox0 - _ox1);
            gdy = 0;
            gdo = _ryp0 * gxyaspect * (1.0 / 512.0) - gdx * _ox0;
            gux = (_t0 * _ryp0 - _t1 * _ryp1) * gxyaspect * (64.0 / 512.0) / (_ox0 - _ox1);
            guo = _t0 * _ryp0 * gxyaspect * (64.0 / 512.0) - gux * _ox0;
            guy = 0;
            _t0 = -8192.0 * _ryp0 + ghoriz;
            _t1 = -8192.0 * _ryp1 + ghoriz;
            t = ((gdx * _ox0 + gdo) * 8.f) / ((_ox1 - _ox0) * _ryp0 * 2048.f);
            gvx = (_t0 - _t1) * t;
            gvy = (_ox1 - _ox0) * t;
            gvo = -gvx * _ox0 - gvy * _t0;

            if (floor) {
                if ((_cy0 > ny0) && (_cy1 > ny1)) {
                    clipper.domost((float) _x0, (float) _cy0, (float) _x1, (float) _cy1);
                } else if ((_cy0 > ny0) != (_cy1 > ny1)) {
                    t = (_cy0 - ny0) / (ny1 - ny0 - _cy1 + _cy0);
                    ox = _x0 + (_x1 - _x0) * t;
                    oy = _cy0 + (_cy1 - _cy0) * t;
                    if (ny0 > _cy0) {
                        clipper.domost((float) _x0, (float) ny0, (float) ox, (float) oy);
                        clipper.domost((float) ox, (float) oy, (float) _x1, (float) _cy1);
                    } else {
                        clipper.domost((float) _x0, (float) _cy0, (float) ox, (float) oy);
                        clipper.domost((float) ox, (float) oy, (float) _x1, (float) ny1);
                    }
                } else {
                    clipper.domost((float) _x0, (float) ny0, (float) _x1, (float) ny1);
                }
            } else {
                if ((_fy0 < ny0) && (_fy1 < ny1)) {
                    clipper.domost((float) _x1, (float) _fy1, (float) _x0, (float) _fy0);
                } else if ((_fy0 < ny0) != (_fy1 < ny1)) {
                    t = (_fy0 - ny0) / (ny1 - ny0 - _fy1 + _fy0);
                    ox = _x0 + (_x1 - _x0) * t;
                    oy = _fy0 + (_fy1 - _fy0) * t;
                    if (ny0 < _fy0) {
                        clipper.domost((float) ox, (float) oy, (float) _x0, (float) ny0);
                        clipper.domost((float) _x1, (float) _fy1, (float) ox, (float) oy);
                    } else {
                        clipper.domost((float) ox, (float) oy, (float) _x0, (float) _fy0);
                        clipper.domost((float) _x1, (float) ny1, (float) ox, (float) oy);
                    }
                } else {
                    clipper.domost((float) _x1, (float) ny1, (float) _x0, (float) ny0);
                }
            }
        }

        // Ceiling of skybox

        RenderingType.Skybox.setIndex(6); // floor/6th texture/index 5 of skybox
        if (floor) {
            RenderingType.Skybox.setIndex(5);
        }

        drawalls_ft[0] = 512 / 16.0f;
        drawalls_ft[1] = 512 / -16.0f;
        if (floor) {
            drawalls_ft[1] = -512 / -16.0f;
        }
        drawalls_ft[2] = (cosglobalang) * (1.f / 2147483648.f);
        drawalls_ft[3] = (singlobalang) * (1.f / 2147483648.f);
        gdx = 0;
        gdy = gxyaspect * (1.0 / 4194304.0);
        if (floor) {
            gdy = gxyaspect * (-1.0 / 4194304.0);
        }
        gdo = -ghoriz * gdy;
        gux = drawalls_ft[3] * (viewingrange) / -65536.0;
        gvx = drawalls_ft[2] * (viewingrange) / -65536.0;
        guy = drawalls_ft[0] * gdy;
        gvy = drawalls_ft[1] * gdy;
        guo = drawalls_ft[0] * gdo;
        gvo = drawalls_ft[1] * gdo;
        guo += (drawalls_ft[2] - gux) * ghalfx;
        gvo -= (drawalls_ft[3] + gvx) * ghalfx;

        if (floor) {
            clipper.domost((float) x0, (float) y0, (float) x1, (float) y1);
        } else {
            gvx = -gvx;
            gvy = -gvy;
            gvo = -gvo; // y-flip skybox floor
            clipper.domost((float) x1, (float) y1, (float) x0, (float) y0);
        }

        skyclamphack = 0;

        RenderingType.Skybox.setIndex(0);
    }

    private void drawbackground(int sectnum, double x0, double x1, double y0, double y1, boolean floor) {
        // Parallaxing sky... hacked for Ken's mountain texture;

        BoardService boardService = engine.getBoardService();
        Sector sec = boardService.getSector(sectnum);
        int shade = sec.getFloorshade();
        int pal = sec.getFloorpal();
        if (!floor) {
            shade = sec.getCeilingshade();
            pal = sec.getCeilingpal();
        }

        calc_and_apply_skyfog(shade, pal);

        if (!config.isUseHighTiles() || defs == null
                || defs.texInfo.findTexture(globalpicnum, globalpal, 1) == null) {
            drawpapersky(sectnum, x0, x1, y0, y1, floor);
        } else {
            drawskybox(x0, x1, y0, y1, floor);
        }

        skyclamphack = 0;
        calc_and_apply_fog(shade, sec.getVisibility(), pal);
    }

    @Override
    public void drawrooms() // eduke32
    {
        int i, j, n, n2, closest;
        double ox, oy, oz, ox2, oy2, oz2, r;

        globalvisibility = scale((long) visibility << 2, xdimen, 1027);

        globalhoriz = (globalhoriz * xdimenscale / viewingrange) + (ydimen >> 1);

        if (offscreenrendering) {
            if (setviewcnt == 1) {
                ogshang = gshang;
            }
        } else if (ogshang != -1) {
            gshang = ogshang;
        }

        resizeglcheck();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        // gl.glClear(GL_COLOR_BUFFER_BIT);
        // gl.glClearColor(0.0f, 0.5f, 0.5f, 1);
        gl.glDisable(GL_BLEND);
        gl.glEnable(GL_TEXTURE_2D);
        gl.glEnable(GL_DEPTH_TEST);

        gl.glDepthFunc(GL_LEQUAL); // NEVER,LESS,(,L)EQUAL,GREATER,(NOT,G)EQUAL,ALWAYS
        gl.glDepthRange(defznear, defzfar); // <- this is more widely supported than glPolygonOffset

        renderingType = RenderingType.Nothing;

        // Polymost supports true look up/down :) Here, we convert horizon to angle.
        // gchang&gshang are cos&sin of this angle (respectively)
        gyxscale = xdimenscale / 131072.0f;
        gxyaspect = (viewingrange / 65536.0) * xyaspect * 5.0 / 262144.0;
        gviewxrange = viewingrange * xdimen / (32768.0f * 1024.0f);
        gcosang = cosglobalang / 262144.0f;
        gsinang = singlobalang / 262144.0f;
        gcosang2 = cosviewingrangeglobalang / 262144.0f;
        gsinang2 = sinviewingrangeglobalang / 262144.0f;
        ghalfx = (xdimen >> 1);
        grhalfxdown10 = 1.0f / (ghalfx * 1024.0f); // viewport
        // global cos/sin height angle
        ghoriz = ydimen >> 1;
        r = (ghoriz - globalhoriz);
        gshang = (float) (r / sqrt(r * r + ghalfx * ghalfx));
        gchang = (float) sqrt(1.0f - gshang * gshang);

        // global cos/sin tilt angle
        gctang = (float) cos(gtang);
        gstang = (float) sin(gtang);

        if (abs(gstang) < .001) // This hack avoids nasty precision bugs in domost()
        {
            gstang = 0;
            if (gctang > 0) {
                gctang = 1.0f;
            } else {
                gctang = -1.0f;
            }
        }

        if (inpreparemirror) {
            gstang = -gstang;
        }

        // Generate viewport trapezoid (for handling screen up/down)
        drawrooms_px[0] = drawrooms_px[3] = 0 - 1;
        drawrooms_px[1] = drawrooms_px[2] = windowx2 + 1 - windowx1 + 2;
        drawrooms_py[0] = drawrooms_py[1] = 0 - 1;
        drawrooms_py[2] = drawrooms_py[3] = windowy2 + 1 - windowy1 + 2;
        n = 4;

        for (i = 0; i < n; i++) {
            ox = drawrooms_px[i] - ghalfx;
            oy = drawrooms_py[i] - ghoriz;
            oz = ghalfx;

            // Tilt rotation (backwards)
            ox2 = ox * gctang + oy * gstang;
            oy2 = oy * gctang - ox * gstang;
            oz2 = oz;

            // Up/down rotation (backwards)
            drawrooms_px[i] = ox2;
            drawrooms_py[i] = oy2 * gchang + oz2 * gshang;
            drawrooms_pz[i] = oz2 * gchang - oy2 * gshang;
        }

        // Clip to SCISDIST plane
        n2 = 0;
        for (i = 0; i < n; i++) {
            j = i + 1;
            if (j >= n) {
                j = 0;
            }
            if (drawrooms_pz[i] >= SCISDIST) {
                drawrooms_px2[n2] = drawrooms_px[i];
                drawrooms_py2[n2] = drawrooms_py[i];
                drawrooms_pz2[n2] = drawrooms_pz[i];
                n2++;
            }

            if ((drawrooms_pz[i] >= SCISDIST) != (drawrooms_pz[j] >= SCISDIST)) {
                r = (SCISDIST - drawrooms_pz[i]) / (drawrooms_pz[j] - drawrooms_pz[i]);
                drawrooms_px2[n2] = (drawrooms_px[j] - drawrooms_px[i]) * r + drawrooms_px[i];
                drawrooms_py2[n2] = (drawrooms_py[j] - drawrooms_py[i]) * r + drawrooms_py[i];
                drawrooms_pz2[n2] = SCISDIST;
                n2++;
            }
        }

        if (n2 < 3) {
            return;
        }
        for (i = 0; i < n2; i++) {
            r = ghalfx / drawrooms_pz2[i];
            drawrooms_sx[i] = drawrooms_px2[i] * r + ghalfx;
            drawrooms_sy[i] = drawrooms_py2[i] * r + ghoriz;
        }

        clipper.initmosts(drawrooms_sx, drawrooms_sy, n2);

        numscans = numbunches = 0;

        // MASKWALL_BAD_ACCESS
        // Fixes access of stale maskwall[maskwallcnt] (a "scan" index, in BUILD lingo):
        maskwallcnt = 0;
        if (globalcursectnum >= boardService.getSectorCount()) {
            globalcursectnum -= boardService.getSectorCount();
        } else {
            i = globalcursectnum;
            globalcursectnum = (short) engine.updatesectorz(globalposx, globalposy, globalposz, globalcursectnum);
            if (globalcursectnum < 0) {
                globalcursectnum = (short) i;
            }
        }

        scansector(globalcursectnum);

        grhalfxdown10x = grhalfxdown10;

        if (inpreparemirror) {
            grhalfxdown10x = -grhalfxdown10;
            inpreparemirror = false;

            // see engine.c: INPREPAREMIRROR_NO_BUNCHES
            if (numbunches > 0) {
                drawalls(0);
                numbunches--;
                bunchfirst[0] = bunchfirst[numbunches];
                bunchlast[0] = bunchlast[numbunches];
            }
        }

        while (numbunches > 0) {
            Arrays.fill(ptempbuf, 0, numbunches + 3, (byte) 0);
            ptempbuf[0] = 1;
            closest = 0; // Almost works, but not quite :(

            for (i = 1; i < numbunches; ++i) {
                j = polymost_bunchfront(i, closest);
                if (j < 0) {
                    continue;
                }
                ptempbuf[i] = 1;
                if (j == 0) {
                    ptempbuf[closest] = 1;
                    closest = i;
                }
            }

            for (i = 0; i < numbunches; ++i) // Double-check
            {
                if (ptempbuf[i] != 0) {
                    continue;
                }
                j = polymost_bunchfront(i, closest);
                if (j < 0) {
                    continue;
                }
                ptempbuf[i] = 1;
                if (j == 0) {
                    ptempbuf[closest] = 1;
                    closest = i;
                    i = 0;
                }
            }

            drawalls(closest);

            numbunches--;
            bunchfirst[closest] = bunchfirst[numbunches];
            bunchlast[closest] = bunchlast[numbunches];
        }
    }

    public void drawmaskwall(int damaskwallcnt) {
        float x0, x1, sx0, sy0, sx1, sy1, xp0, yp0, xp1, yp1, oxp0, oyp0, ryp0, ryp1;
        float r, t, t0, t1;
        int i, j, n, n2, z, sectnum, method;

        int m0, m1;
        Sector sec, nsec;
        Wall wal, wal2;
        BoardService boardService = engine.getBoardService();

        // cullcheckcnt = 0;

        z = maskwall[damaskwallcnt];
        wal = boardService.getWall(thewall[z]);
        wal2 = boardService.getWall(wal.getPoint2());
        sectnum = thesector[z];

        if (sectnum == -1 || wal.getNextsector() == -1) {
            return;
        }

        renderingType = RenderingType.MaskWall.setIndex(thewall[z]);

        sec = boardService.getSector(sectnum);
        nsec = boardService.getSector(wal.getNextsector());

        globalpicnum = wal.getOverpicnum();
        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }

        if (getTile(globalpicnum).getType() != AnimType.NONE) {
            globalpicnum += animateoffs(globalpicnum, thewall[z] + 16384);
        }
        globalshade = wal.getShade();
        globalpal = wal.getPal() & 0xFF;
        globalorientation = wal.getCstat();

        sx0 = wal.getX() - globalposx;
        sx1 = wal2.getX() - globalposx;
        sy0 = wal.getY() - globalposy;
        sy1 = wal2.getY() - globalposy;
        yp0 = sx0 * gcosang2 + sy0 * gsinang2;
        yp1 = sx1 * gcosang2 + sy1 * gsinang2;
        if ((yp0 < SCISDIST) && (yp1 < SCISDIST)) {
            return;
        }
        xp0 = sy0 * gcosang - sx0 * gsinang;
        xp1 = sy1 * gcosang - sx1 * gsinang;

        // Clip to close parallel-screen plane
        oxp0 = xp0;
        oyp0 = yp0;

        t0 = 0.f;

        if (yp0 < SCISDIST) {
            t0 = (SCISDIST - yp0) / (yp1 - yp0);
            xp0 = (xp1 - xp0) * t0 + xp0;
            yp0 = SCISDIST;
        }

        t1 = 1.f;

        if (yp1 < SCISDIST) {
            t1 = (SCISDIST - oyp0) / (yp1 - oyp0);
            xp1 = (xp1 - oxp0) * t1 + oxp0;
            yp1 = SCISDIST;
        }

        m0 = (int) ((wal2.getX() - wal.getX()) * t0 + wal.getX());
        m1 = (int) ((wal2.getY() - wal.getY()) * t0 + wal.getY());
        polymost_getzsofslope(sectnum, m0, m1);
        drawmaskwall_cz[0] = (int) dceilzsofslope;
        drawmaskwall_fz[0] = (int) dfloorzsofslope;
        polymost_getzsofslope(wal.getNextsector(), m0, m1);
        drawmaskwall_cz[1] = (int) dceilzsofslope;
        drawmaskwall_fz[1] = (int) dfloorzsofslope;
        m0 = (int) ((wal2.getX() - wal.getX()) * t1 + wal.getX());
        m1 = (int) ((wal2.getY() - wal.getY()) * t1 + wal.getY());
        polymost_getzsofslope(sectnum, m0, m1);
        drawmaskwall_cz[2] = (int) dceilzsofslope;
        drawmaskwall_fz[2] = (int) dfloorzsofslope;
        polymost_getzsofslope(wal.getNextsector(), m0, m1);
        drawmaskwall_cz[3] = (int) dceilzsofslope;
        drawmaskwall_fz[3] = (int) dfloorzsofslope;

        ryp0 = 1.f / yp0;
        ryp1 = 1.f / yp1;

        // Generate screen coordinates for front side of wall
        x0 = ghalfx * xp0 * ryp0 + ghalfx;
        x1 = ghalfx * xp1 * ryp1 + ghalfx;
        if (x1 <= x0) {
            return;
        }

        ryp0 *= gyxscale;
        ryp1 *= gyxscale;

        gdx = (ryp0 - ryp1) * gxyaspect / (x0 - x1);
        gdy = 0;
        gdo = ryp0 * gxyaspect - gdx * x0;

        gux = (t0 * ryp0 - t1 * ryp1) * gxyaspect * ((wal.getXrepeat() & 0xFF) * 8.0) / (x0 - x1);
        guo = t0 * ryp0 * gxyaspect * ((wal.getXrepeat() & 0xFF) * 8.0) - gux * x0;
        guo += wal.getXpanning() * gdo;
        gux += wal.getXpanning() * gdx;
        guy = 0;

        // mask
        calc_ypanning((!wal.isBottomAligned()) ? max(nsec.getCeilingz(), sec.getCeilingz()) : min(nsec.getFloorz(), sec.getFloorz()), ryp0,
                ryp1, x0, x1, wal.getYpanning(), wal.getYrepeat(), false);

        if (wal.isXFlip()) {
            t = (wal.getXrepeat() & 0xFF) * 8 + wal.getXpanning() * 2;
            gux = gdx * t - gux;
            guy = gdy * t - guy;
            guo = gdo * t - guo;
        }
        if (wal.isYFlip()) {
            gvx = -gvx;
            gvy = -gvy;
            gvo = -gvo;
        }

        method = 1;
        pow2xsplit = 1;
        if (wal.isTransparent()) {
            if (!wal.isTransparent2()) {
                method = 2;
            } else {
                method = 3;
            }
        }

        int shade = wal.getShade();
        calc_and_apply_fog(shade, sec.getVisibility(), sec.getFloorpal());

        drawmaskwall_csy[0] = (drawmaskwall_cz[0] - globalposz) * ryp0 + ghoriz;
        drawmaskwall_csy[1] = (drawmaskwall_cz[1] - globalposz) * ryp0 + ghoriz;
        drawmaskwall_csy[2] = (drawmaskwall_cz[2] - globalposz) * ryp1 + ghoriz;
        drawmaskwall_csy[3] = (drawmaskwall_cz[3] - globalposz) * ryp1 + ghoriz;

        drawmaskwall_fsy[0] = (drawmaskwall_fz[0] - globalposz) * ryp0 + ghoriz;
        drawmaskwall_fsy[1] = (drawmaskwall_fz[1] - globalposz) * ryp0 + ghoriz;
        drawmaskwall_fsy[2] = (drawmaskwall_fz[2] - globalposz) * ryp1 + ghoriz;
        drawmaskwall_fsy[3] = (drawmaskwall_fz[3] - globalposz) * ryp1 + ghoriz;

        // Clip 2 quadrilaterals
        // /csy3
        // / |
        // csy0------/----csy2
        // | /xxxxxxx|
        // | /xxxxxxxxx|
        // csy1/xxxxxxxxxxx|
        // |xxxxxxxxxxx/fsy3
        // |xxxxxxxxx/ |
        // |xxxxxxx/ |
        // fsy0----/------fsy2
        // | /
        // fsy1/

        dmaskwall[0].px = x0;
        dmaskwall[0].py = drawmaskwall_csy[1];
        dmaskwall[1].px = x1;
        dmaskwall[1].py = drawmaskwall_csy[3];
        dmaskwall[2].px = x1;
        dmaskwall[2].py = drawmaskwall_fsy[3];
        dmaskwall[3].px = x0;
        dmaskwall[3].py = drawmaskwall_fsy[1];
        n = 4;

        // Clip to (x0,csy[0])-(x1,csy[2])
        n2 = 0;
        t1 = (float) -((dmaskwall[0].px - x0) * (drawmaskwall_csy[2] - drawmaskwall_csy[0])
                - (dmaskwall[0].py - drawmaskwall_csy[0]) * (x1 - x0));
        for (i = 0; i < n; i++) {
            j = i + 1;
            if (j >= n) {
                j = 0;
            }

            t0 = t1;
            t1 = (float) -((dmaskwall[j].px - x0) * (drawmaskwall_csy[2] - drawmaskwall_csy[0])
                    - (dmaskwall[j].py - drawmaskwall_csy[0]) * (x1 - x0));
            if (t0 >= 0) {
                dmaskwall[n2].px2 = dmaskwall[i].px;
                dmaskwall[n2].py2 = dmaskwall[i].py;
                n2++;
            }
            if ((t0 >= 0) != (t1 >= 0)) {
                r = t0 / (t0 - t1);
                dmaskwall[n2].px2 = (dmaskwall[j].px - dmaskwall[i].px) * r + dmaskwall[i].px;
                dmaskwall[n2].py2 = (dmaskwall[j].py - dmaskwall[i].py) * r + dmaskwall[i].py;
                n2++;
            }
        }
        if (n2 < 3) {
            return;
        }

        // Clip to (x1,fsy[2])-(x0,fsy[0])
        n = 0;
        t1 = (float) -((dmaskwall[0].px2 - x1) * (drawmaskwall_fsy[0] - drawmaskwall_fsy[2])
                - (dmaskwall[0].py2 - drawmaskwall_fsy[2]) * (x0 - x1));
        for (i = 0; i < n2; i++) {
            j = i + 1;
            if (j >= n2) {
                j = 0;
            }

            t0 = t1;
            t1 = (float) -((dmaskwall[j].px2 - x1) * (drawmaskwall_fsy[0] - drawmaskwall_fsy[2])
                    - (dmaskwall[j].py2 - drawmaskwall_fsy[2]) * (x0 - x1));
            if (t0 >= 0) {
                dmaskwall[n].px = dmaskwall[i].px2;
                dmaskwall[n].py = dmaskwall[i].py2;
                n++;
            }
            if ((t0 >= 0) != (t1 >= 0)) {
                r = t0 / (t0 - t1);
                dmaskwall[n].px = (dmaskwall[j].px2 - dmaskwall[i].px2) * r + dmaskwall[i].px2;
                dmaskwall[n].py = (dmaskwall[j].py2 - dmaskwall[i].py2) * r + dmaskwall[i].py2;
                n++;
            }
        }
        if (n < 3) {
            return;
        }

        gl.glDepthRange(defznear + 0.000001, defzfar - 0.00001);
        drawpoly(dmaskwall, n, method);
        gl.glDepthRange(defznear, defzfar);
    }

    private int getclosestpointonwall(int posx, int posy, int dawall, Vector2 n) {
        BoardService boardService = engine.getBoardService();
        Wall w = boardService.getWall(dawall);
        Wall p2 = boardService.getWall(boardService.getWall(dawall).getPoint2());
        int dx = p2.getX() - w.getX();
        int dy = p2.getY() - w.getY();

        float i = dx * (posx - w.getX()) + dy * (posy - w.getY());

        if (i < 0) {
            return 1;
        }

        float j = dx * dx + dy * dy;

        if (i > j) {
            return 1;
        }

        i /= j;

        n.set(dx * i + w.getX(), dy * i + w.getY());

        return 0;
    }

    private float TSPR_OFFSET(Sprite tspr, long dist) {
        float offset = (TSPR_OFFSET_FACTOR + ((tspr.getOwner() != -1 ? tspr.getOwner() & 31 : 1) * TSPR_OFFSET_FACTOR)) * dist
                * 0.025f;
        return -offset;
    }

    private void drawsprite(int snum) {
        float f, c, s, fx, fy, sx0, sy0, sx1, xp0, yp0, xp1, yp1, oxp0, oyp0, ryp0, ryp1;
        float x0, y0, x1, y1, sc0, sf0, sc1, sf1, xv, yv, t0, t1;
        int i, j, spritenum, xoff = 0, yoff = 0, method, npoints;
        Sprite tspr;
        int tsizx, tsizy;

        tspr = tspriteptr[snum];
        BoardService boardService = engine.getBoardService();

        if (tspr.getOwner() < 0 || tspr.getPicnum() < 0 || tspr.getPicnum() >= MAXTILES || tspr.getSectnum() < 0) {
            return;
        }

        globalpicnum = tspr.getPicnum();
        globalshade = tspr.getShade();
        globalpal = tspr.getPal() & 0xFF;
        globalorientation = tspr.getCstat();
        spritenum = tspr.getOwner();
        ArtEntry pic = getTile(globalpicnum);

        if ((globalorientation & 48) != 48) {
            boolean flag;

            if (pic.getType() != AnimType.NONE) {
                globalpicnum += animateoffs(globalpicnum, spritenum + 32768);
                pic = getTile(globalpicnum);
            }

            flag = false; //(GLSettings.useHighTile.get() && h_xsize[globalpicnum] != 0);
            xoff = tspr.getXoffset();
            yoff = tspr.getYoffset();
            xoff += /*flag ? h_xoffs[globalpicnum] : */pic.getOffsetX();
            yoff += /*flag ? h_yoffs[globalpicnum] : */pic.getOffsetY();
        }

        method = 1 + 4;
        if ((tspr.getCstat() & 2) != 0) {
            if ((tspr.getCstat() & 512) == 0) {
                method = 2 + 4;
            } else {
                method = 3 + 4;
            }
        }

        Spriteext sprext = defs.mapInfo.getSpriteInfo(tspr.getOwner());

        if (sprext != null) {
//			tspr.x += sprext.xoff;
//			tspr.y += sprext.yoff;
//			tspr.z += sprext.zoff;
        }

        int posx = tspr.getX();
        int posy = tspr.getY();

        int shade = (int) (globalshade / 1.5f);

        while (sprext == null || !sprext.isNotModel()) {
            renderingType = RenderingType.Model.setIndex(snum);

            if (config.isUseModels()) {
                GLModel md = modelManager.getModel(globalpicnum, globalpal);
                if (md != null) {
                    calc_and_apply_fog(shade, boardService.getSector(tspr.getSectnum()).getVisibility(), boardService.getSector(tspr.getSectnum()).getFloorpal());

                    if (boardService.isValidSprite(tspr.getOwner())) {
                        if (mdrenderer.mddraw(md, tspr, xoff, yoff) != 0) {
                            return;
                        }
                        break; // else, render as flat sprite
                    }

                    if (mdrenderer.mddraw(md, tspr, xoff, yoff) != 0) {
                        return;
                    }
                    break; // else, render as flat sprite
                }
            }

            if (engine.getConfig().isUseVoxels()) {
                int dist = (posx - globalposx) * (posx - globalposx) + (posy - globalposy) * (posy - globalposy);
                if (dist < 48000L * 48000L) {
                    GLVoxel vox = (GLVoxel) modelManager.getVoxel(globalpicnum);
                    if (vox != null) {
                        calc_and_apply_fog(shade, boardService.getSector(tspr.getSectnum()).getVisibility(), boardService.getSector(tspr.getSectnum()).getFloorpal());

                        if ((tspr.getCstat() & 48) != 48) {
                            if (mdrenderer.voxdraw(vox, tspr) != 0) {
                                return;
                            }
                            break; // else, render as flat sprite
                        }

                        if ((tspr.getCstat() & 48) == 48) {
                            mdrenderer.voxdraw(vox, tspr);
                            return;
                        }
                    }
                }
            }
            break;
        }

        renderingType = RenderingType.Sprite.setIndex(snum);
        calc_and_apply_fog(shade, boardService.getSector(tspr.getSectnum()).getVisibility(), boardService.getSector(tspr.getSectnum()).getFloorpal());

//		if (sprext != null) {
//			if ((sprext.flags & SPREXT_AWAY1) != 0) {
//				posx += (EngineUtils.sin((tspr.ang + 512) & 2047) >> 13);
//				posy += (EngineUtils.sin((tspr.ang) & 2047) >> 13);
//
//			} else if ((sprext.flags & SPREXT_AWAY2) != 0) {
//				posx -= (EngineUtils.sin((tspr.ang + 512) & 2047) >> 13);
//				posy -= (EngineUtils.sin((tspr.ang) & 2047) >> 13);
//			}
//		}

        tsizx = pic.getWidth();
        tsizy = pic.getHeight();

        if (tsizx <= 0 || tsizy <= 0) {
            return;
        }

        long dist;

        float foffs, offsx, offsy;
        int ang;
        switch ((globalorientation >> 4) & 3) {
            case 0: // Face sprite
                // Project 3D to 2D
                if ((globalorientation & 4) != 0) {
                    xoff = -xoff;
                }
                // NOTE: yoff not negated not for y flipping, unlike wall and floor
                // aligned sprites.

                dist = EngineUtils.qdist(globalposx - tspr.getX(), globalposy - tspr.getY());
                ang = (EngineUtils.getAngle(tspr.getX() - globalposx, tspr.getY() - globalposy) + 1024) & 2047;
                foffs = TSPR_OFFSET(tspr, dist);
                dist *= (dist >> 7);

                offsx = (EngineUtils.cos(ang) >> 6) * foffs;
                offsy = (EngineUtils.sin(ang) >> 6) * foffs;

                sx0 = tspr.getX() - globalposx - offsx;
                sy0 = tspr.getY() - globalposy - offsy;
                xp0 = sy0 * gcosang - sx0 * gsinang;
                yp0 = sx0 * gcosang2 + sy0 * gsinang2;

                if (yp0 <= SCISDIST) {
                    return;
                }
                ryp0 = 1.0f / yp0;
                sx0 = ghalfx * xp0 * ryp0 + ghalfx;
                sy0 = (tspr.getZ() - globalposz) * gyxscale * ryp0 + ghoriz;

                f = ryp0 * xdimen * (1.0f / 160.f);
                fx = (tspr.getXrepeat()) * f;
                fy = (tspr.getYrepeat()) * f * (yxaspect * (1.0f / 65536.f));

                sx0 -= fx * xoff;
                if ((tsizx & 1) != 0) {
                    sx0 += fx * 0.5f;
                }
                sy0 -= fy * yoff;
                if ((globalorientation & 128) != 0 && (tsizy & 1) != 0) {
                    sy0 += fy * 0.5f;
                }

                fx *= (tsizx);
                fy *= (tsizy);

                dsprite[0].px = dsprite[3].px = sx0 - fx * .5f;
                dsprite[1].px = dsprite[2].px = sx0 + fx * .5f;
                if ((globalorientation & 128) == 0) {
                    dsprite[0].py = dsprite[1].py = sy0 - fy;
                    dsprite[2].py = dsprite[3].py = sy0;
                } else {
                    dsprite[0].py = dsprite[1].py = sy0 - fy * .5f;
                    dsprite[2].py = dsprite[3].py = sy0 + fy * .5f;
                }

                gdx = gdy = guy = gvx = 0;
                gdo = ryp0 * gviewxrange;
                if ((globalorientation & 4) == 0) {
                    gux = tsizx * gdo / (dsprite[1].px - dsprite[0].px + .002);
                    guo = -gux * (dsprite[0].px - .001);
                } else {
                    gux = tsizx * gdo / (dsprite[0].px - dsprite[1].px - .002);
                    guo = -gux * (dsprite[1].px + .001);
                }
                if ((globalorientation & 8) == 0) {
                    gvy = tsizy * gdo / (dsprite[3].py - dsprite[0].py + .002);
                    gvo = -gvy * (dsprite[0].py - .001);
                } else {
                    gvy = tsizy * gdo / (dsprite[0].py - dsprite[3].py - .002);
                    gvo = -gvy * (dsprite[3].py + .001);
                }

                // sprite panning
                if (sprext != null) {
                    if (sprext.xpanning != 0) {
                        guy -= gdy * ((sprext.xpanning) / 255.f) * tsizx;
                        guo -= gdo * ((sprext.xpanning) / 255.f) * tsizx;
                        srepeat = 1;
                    }
                    if (sprext.ypanning != 0) {
                        gvy -= gdy * ((sprext.ypanning) / 255.f) * tsizy;
                        gvo -= gdo * ((sprext.ypanning) / 255.f) * tsizy;
                        trepeat = 1;
                    }
                }

                // Clip sprites to ceilings/floors when no parallaxing and not
                // sloped
                if ((boardService.getSector(tspr.getSectnum()).getCeilingstat() & 3) == 0) {
                    sy0 = ((boardService.getSector(tspr.getSectnum()).getCeilingz() - globalposz)) * gyxscale * ryp0 + ghoriz;
                    if (dsprite[0].py < sy0) {
                        dsprite[0].py = dsprite[1].py = sy0;
                    }
                }
                if ((boardService.getSector(tspr.getSectnum()).getFloorstat() & 3) == 0) {
                    sy0 = ((boardService.getSector(tspr.getSectnum()).getFloorz() - globalposz)) * gyxscale * ryp0 + ghoriz;
                    if (dsprite[2].py > sy0) {
                        dsprite[2].py = dsprite[3].py = sy0;
                    }
                }

                gl.glDepthRange(defznear, defzfar - (10f / (dist + 1)));

                pow2xsplit = 0;
                drawpoly(dsprite, 4, method);

                gl.glDepthRange(defznear, defzfar);

                srepeat = 0;
                trepeat = 0;
                break;

            case 1: // Wall sprite

                // Project 3D to 2D
                if ((globalorientation & 4) != 0) {
                    xoff = -xoff;
                }
                if ((globalorientation & 8) != 0) {
                    yoff = -yoff;
                }

                Vector2 vec2 = dcoord.get(tspr.getOwner());
                posx += (int) vec2.x;
                posy += (int) vec2.y;
                Vector2 sin2 = dsin.get(tspr.getOwner());

                xv = tspr.getXrepeat() * (EngineUtils.cos(tspr.getAng() - 512) * (1.0f / 65536.f) - sin2.x);
                yv = tspr.getXrepeat() * (EngineUtils.sin(tspr.getAng() - 512) * (1.0f / 65536.f) - sin2.y);

                f = (float) (tsizx >> 1) + (float) xoff;
                x0 = posx - globalposx - xv * f;
                x1 = xv * tsizx + x0;
                y0 = posy - globalposy - yv * f;
                y1 = yv * tsizx + y0;

                yp0 = x0 * gcosang2 + y0 * gsinang2;
                yp1 = x1 * gcosang2 + y1 * gsinang2;
                if ((yp0 <= SCISDIST) && (yp1 <= SCISDIST)) {
                    return;
                }
                xp0 = y0 * gcosang - x0 * gsinang;
                xp1 = y1 * gcosang - x1 * gsinang;

                // Clip to close parallel-screen plane
                oxp0 = xp0;
                oyp0 = yp0;
                if (yp0 < SCISDIST) {
                    t0 = (SCISDIST - yp0) / (yp1 - yp0);
                    xp0 = (xp1 - xp0) * t0 + xp0;
                    yp0 = SCISDIST;
                } else {
                    t0 = 0.f;
                }
                if (yp1 < SCISDIST) {
                    t1 = (SCISDIST - oyp0) / (yp1 - oyp0);
                    xp1 = (xp1 - oxp0) * t1 + oxp0;
                    yp1 = SCISDIST;
                } else {
                    t1 = 1.f;
                }

                f = ((float) tspr.getYrepeat()) * (float) tsizy * 4;

                ryp0 = 1.0f / yp0;
                ryp1 = 1.0f / yp1;
                sx0 = ghalfx * xp0 * ryp0 + ghalfx;
                sx1 = ghalfx * xp1 * ryp1 + ghalfx;
                ryp0 *= gyxscale;
                ryp1 *= gyxscale;

                tspr.setZ(tspr.getZ() - ((yoff * tspr.getYrepeat()) << 2));
                if ((globalorientation & 128) != 0) {
                    tspr.setZ(tspr.getZ() + ((tsizy * tspr.getYrepeat()) << 1));
                    if ((tsizy & 1) != 0) {
                        tspr.setZ(tspr.getZ() + (tspr.getYrepeat() << 1)); // Odd yspans
                    }
                }

                sc0 = ((tspr.getZ() - globalposz - f)) * ryp0 + ghoriz;
                sc1 = ((tspr.getZ() - globalposz - f)) * ryp1 + ghoriz;
                sf0 = ((tspr.getZ() - globalposz)) * ryp0 + ghoriz;
                sf1 = ((tspr.getZ() - globalposz)) * ryp1 + ghoriz;

                gdx = (ryp0 - ryp1) * gxyaspect / (sx0 - sx1);
                gdy = 0;
                gdo = ryp0 * gxyaspect - gdx * sx0;

                if ((globalorientation & 4) != 0) {
                    t0 = 1.f - t0;
                    t1 = 1.f - t1;
                }

                // sprite panning
                if (sprext != null && sprext.xpanning != 0) {
                    t0 -= ((sprext.xpanning) / 255.f);
                    t1 -= ((sprext.xpanning) / 255.f);
                    srepeat = 1;
                }

                gux = (t0 * ryp0 - t1 * ryp1) * gxyaspect * tsizx / (sx0 - sx1);
                guy = 0;
                guo = t0 * ryp0 * gxyaspect * tsizx - gux * sx0;

                f = (float) ((tsizy) * (gdx * sx0 + gdo) / ((sx0 - sx1) * (sc0 - sf0)));
                if ((globalorientation & 8) == 0) {
                    gvx = (sc0 - sc1) * f;
                    gvy = (sx1 - sx0) * f;
                    gvo = -gvx * sx0 - gvy * sc0;
                } else {
                    gvx = (sf1 - sf0) * f;
                    gvy = (sx0 - sx1) * f;
                    gvo = -gvx * sx0 - gvy * sf0;
                }

                // sprite panning
                if (sprext != null && sprext.ypanning != 0) {
                    gvx -= gdx * ((sprext.ypanning) / 255.f) * tsizy;
                    gvy -= gdy * ((sprext.ypanning) / 255.f) * tsizy;
                    gvo -= gdo * ((sprext.ypanning) / 255.f) * tsizy;
                    trepeat = 1;
                }

                // Clip sprites to ceilings/floors when no parallaxing
                if (tspr.getSectnum() != -1 && (boardService.getSector(tspr.getSectnum()).getCeilingstat() & 1) == 0) {
                    f = ((float) tspr.getYrepeat()) * (float) tsizy * 4;
                    if (boardService.getSector(tspr.getSectnum()).getCeilingz() > tspr.getZ() - f) {
                        sc0 = ((boardService.getSector(tspr.getSectnum()).getCeilingz() - globalposz)) * ryp0 + ghoriz;
                        sc1 = ((boardService.getSector(tspr.getSectnum()).getCeilingz() - globalposz)) * ryp1 + ghoriz;
                    }
                }
                if (tspr.getSectnum() != -1 && (boardService.getSector(tspr.getSectnum()).getFloorstat() & 1) == 0) {
                    if (boardService.getSector(tspr.getSectnum()).getFloorz() < tspr.getZ()) {
                        sf0 = ((boardService.getSector(tspr.getSectnum()).getFloorz() - globalposz)) * ryp0 + ghoriz;
                        sf1 = ((boardService.getSector(tspr.getSectnum()).getFloorz() - globalposz)) * ryp1 + ghoriz;
                    }
                }

                if (sx0 > sx1) {
                    if ((globalorientation & 64) != 0) {
                        return; // 1-sided sprite
                    }
                    f = sx0;
                    sx0 = sx1;
                    sx1 = f;
                    f = sc0;
                    sc0 = sc1;
                    sc1 = f;
                    f = sf0;
                    sf0 = sf1;
                    sf1 = f;
                }

                dsprite[0].px = sx0;
                dsprite[0].py = sc0;
                dsprite[1].px = sx1;
                dsprite[1].py = sc1;
                dsprite[2].px = sx1;
                dsprite[2].py = sf1;
                dsprite[3].px = sx0;
                dsprite[3].py = sf0;

                boolean spriteWall = spritewall.get(tspr.getOwner()).get();
                if (spriteWall && (tspr.getCstat() & 2) != 0) {
                    gl.glDepthMask(false);
                }

                dist = EngineUtils.qdist(globalposx - tspr.getX(), globalposy - tspr.getY());
                dist *= (dist >> 7);

                if (spriteWall && dist > 0) {
                    int dang = (((int) globalang - tspr.getAng()) & 2047) - 1024;
                    if (dang >= -512 && dang <= 512) {
                        gl.glDepthRange(defznear, defzfar - (Math.min(dist / 16384f, 40) / dist));
                    }
                }

                curpolygonoffset += 0.01f;
                gl.glPolygonOffset(-curpolygonoffset, -curpolygonoffset);

                pow2xsplit = 0;
                drawpoly(dsprite, 4, method);

                gl.glPolygonOffset(0, 0);
                gl.glDepthRange(defznear, defzfar);
                if (spriteWall && (tspr.getCstat() & 2) != 0) {
                    gl.glDepthMask(true);
                }

                srepeat = 0;
                trepeat = 0;

                break;

            case 2: // Floor sprite

                if ((globalorientation & 64) != 0) {
                    if ((globalposz > tspr.getZ()) == ((globalorientation & 8) == 0)) {
                        return;
                    }
                }
                if ((globalorientation & 4) > 0) {
                    xoff = -xoff;
                }
                if ((globalorientation & 8) > 0) {
                    yoff = -yoff;
                }

//			if (tspr.z < getSector()[tspr.sectnum].ceilingz)
//				tspr.z += ((tspr.owner) & 31);
//			if (tspr.z > getSector()[tspr.sectnum].floorz)
//				tspr.z -= ((tspr.owner) & 31);

                i = (tspr.getAng() & 2047);
                c = (float) (EngineUtils.cos(i) / 65536.0);
                s = (float) (EngineUtils.sin(i) / 65536.0);
                x0 = (float) ((tsizx >> 1) - xoff) * tspr.getXrepeat();
                y0 = (float) ((tsizy >> 1) - yoff) * tspr.getYrepeat();
                x1 = (float) ((tsizx >> 1) + xoff) * tspr.getXrepeat();
                y1 = (float) ((tsizy >> 1) + yoff) * tspr.getYrepeat();

                // Project 3D to 2D
                for (j = 0; j < 4; j++) {
                    sx0 = tspr.getX() - globalposx;
                    sy0 = tspr.getY() - globalposy;
                    if (((j + 0) & 2) != 0) {
                        sy0 -= s * y0;
                        sx0 -= c * y0;
                    } else {
                        sy0 += s * y1;
                        sx0 += c * y1;
                    }
                    if (((j + 1) & 2) != 0) {
                        sx0 -= s * x0;
                        sy0 += c * x0;
                    } else {
                        sx0 += s * x1;
                        sy0 -= c * x1;
                    }

                    dsprite[j].px = sy0 * gcosang - sx0 * gsinang;
                    dsprite[j].py = sx0 * gcosang2 + sy0 * gsinang2;
                }

                if (tspr.getZ() < globalposz) // if floor sprite is above you, reverse order of points
                {
                    f = (float) dsprite[0].px;
                    dsprite[0].px = dsprite[1].px;
                    dsprite[1].px = f;
                    f = (float) dsprite[0].py;
                    dsprite[0].py = dsprite[1].py;
                    dsprite[1].py = f;
                    f = (float) dsprite[2].px;
                    dsprite[2].px = dsprite[3].px;
                    dsprite[3].px = f;
                    f = (float) dsprite[2].py;
                    dsprite[2].py = dsprite[3].py;
                    dsprite[3].py = f;
                }

                // Clip to SCISDIST plane
                npoints = 0;
                for (i = 0; i < 4; i++) {
                    j = ((i + 1) & 3);
                    if (dsprite[i].py >= SCISDIST) {
                        dsprite[npoints].px2 = dsprite[i].px;
                        dsprite[npoints].py2 = dsprite[i].py;
                        npoints++;
                    }
                    if ((dsprite[i].py >= SCISDIST) != (dsprite[j].py >= SCISDIST)) {
                        f = (float) ((SCISDIST - dsprite[i].py) / (dsprite[j].py - dsprite[i].py));
                        dsprite[npoints].px2 = (float) ((dsprite[j].px - dsprite[i].px) * f + dsprite[i].px);
                        dsprite[npoints].py2 = (float) ((dsprite[j].py - dsprite[i].py) * f + dsprite[i].py);
                        npoints++;
                    }
                }

                if (npoints < 3) {
                    return;
                }

                // Project rotated 3D points to screen
                f = (tspr.getZ() - globalposz) * gyxscale;
                for (j = 0; j < npoints; j++) {
                    ryp0 = (float) (1.0 / dsprite[j].py2);
                    dsprite[j].px = ghalfx * dsprite[j].px2 * ryp0 + ghalfx;
                    dsprite[j].py = f * ryp0 + ghoriz;
                }

                // gd? Copied from floor rendering code
                gdx = 0;
                gdy = gxyaspect / (tspr.getZ() - globalposz);
                gdo = -ghoriz * gdy;
                // copied&modified from relative alignment
                xv = tspr.getX() + s * x1 + c * y1;
                fx = -(x0 + x1) * s;
                yv = tspr.getY() + s * y1 - c * x1;
                fy = +(x0 + x1) * c;
                f = (float) (1.0f / sqrt(fx * fx + fy * fy));
                fx *= f;
                fy *= f;
                drawsprite_ft[2] = singlobalang * fy + cosglobalang * fx;
                drawsprite_ft[3] = singlobalang * fx - cosglobalang * fy;
                drawsprite_ft[0] = (globalposy - yv) * fy + (globalposx - xv) * fx;
                drawsprite_ft[1] = (globalposx - xv) * fy - (globalposy - yv) * fx;
                gux = (double) drawsprite_ft[3] * ((double) viewingrange) / (-65536.0 * 262144.0);
                gvx = (double) drawsprite_ft[2] * ((double) viewingrange) / (-65536.0 * 262144.0);
                guy = drawsprite_ft[0] * gdy;
                gvy = drawsprite_ft[1] * gdy;
                guo = drawsprite_ft[0] * gdo;
                gvo = drawsprite_ft[1] * gdo;
                guo += (drawsprite_ft[2] / 262144.0 - gux) * ghalfx;
                gvo -= (drawsprite_ft[3] / 262144.0 + gvx) * ghalfx;
                f = 4.0f / tspr.getXrepeat();
                gux *= f;
                guy *= f;
                guo *= f;
                f = -4.0f / tspr.getYrepeat();
                gvx *= f;
                gvy *= f;
                gvo *= f;
                if ((globalorientation & 4) != 0) {
                    gux = (tsizx) * gdx - gux;
                    guy = (tsizx) * gdy - guy;
                    guo = (tsizx) * gdo - guo;
                }

                // sprite panning
                if (sprext != null) {
                    if (sprext.xpanning != 0) {
                        guy -= gdy * ((sprext.xpanning) / 255.f) * tsizx;
                        guo -= gdo * ((sprext.xpanning) / 255.f) * tsizx;
                        srepeat = 1;
                    }
                    if (sprext.ypanning != 0) {
                        gvy -= gdy * ((sprext.ypanning) / 255.f) * tsizy;
                        gvo -= gdo * ((sprext.ypanning) / 255.f) * tsizy;
                        trepeat = 1;
                    }
                }

                if ((tspr.getCstat() & 2) != 0) {
                    gl.glDepthMask(false);
                }

                gl.glDepthRange(defznear + 0.000001, defzfar - 0.00001);

                curpolygonoffset += 0.01f;
                gl.glPolygonOffset(-curpolygonoffset, -curpolygonoffset);

                pow2xsplit = 0;
                drawpoly(dsprite, npoints, method);

                gl.glDepthRange(defznear, defzfar);

                if ((tspr.getCstat() & 2) != 0) {
                    gl.glDepthMask(true);
                }

                srepeat = 0;
                trepeat = 0;

                break;
            case 3: // Voxel sprite
                break;
        }

        if (automapping == 1) {
            show2dsprite.setBit(snum);
        }
    }

    protected void calc_and_apply_fog(int shade, int vis, int pal) {
        globalfog.shade = shade;
        // globalfog.combvis = globalvisibility * ((vis + 16) & 0xFF);

        globalfog.combvis = globalvisibility;
        if (vis != 0) {
            globalfog.combvis = mulscale(globalvisibility, (vis + 16) & 0xFF, 4);
        }

        globalfog.pal = pal;
        globalfog.calc();
    }

    protected void calc_and_apply_skyfog(int shade, int pal) {
        globalfog.shade = shade;
        globalfog.combvis = 0;
        globalfog.pal = pal;
        globalfog.calc();
    }

    public boolean sameside(Vector3 eq, Vector2 p1, Vector2 p2) {
        float sign1, sign2;

        sign1 = eq.x * p1.x + eq.y * p1.y + eq.z;
        sign2 = eq.x * p2.x + eq.y * p2.y + eq.z;

        sign1 = sign1 * sign2;
        // OSD_Printf("SAME SIDE !\n");
        return sign1 > 0;
        // OSD_Printf("OPPOSITE SIDE !\n");
    }

    public void swapsprite(int k, int l, boolean z) {
        Sprite stmp = tspriteptr[k];
        tspriteptr[k] = tspriteptr[l];
        tspriteptr[l] = stmp;

        int tmp = spritesx[k];
        spritesx[k] = spritesx[l];
        spritesx[l] = tmp;
        tmp = spritesy[k];
        spritesy[k] = spritesy[l];
        spritesy[l] = tmp;

        if (z) {
            tmp = spritesz[k];
            spritesz[k] = spritesz[l];
            spritesz[l] = tmp;
        }
    }

    @Override
    public void drawmasks() {
        int i, j, k, l, gap, xs, ys, xp, yp, yoff, yspan;
        boolean modelp;

        int spritesortcnt = Math.min(tspriteptr.length - 1, tSpriteList.getSize());
        for (i = spritesortcnt - 1; i >= 0; i--) {
            tspriteptr[i] = tSpriteList.get(i);
            if (tspriteptr[i].getPicnum() < 0 || tspriteptr[i].getPicnum() >= MAXTILES) {
                continue;
            }

            xs = tspriteptr[i].getX() - globalposx;
            ys = tspriteptr[i].getY() - globalposy;
            yp = dmulscale(xs, cosviewingrangeglobalang, ys, sinviewingrangeglobalang, 6);

            modelp = (config.isUseModels() && defs != null
                    && defs.mdInfo.getModelInfo(tspriteptr[i].getPicnum()) != null);

            if (yp > (4 << 8)) {
                xp = dmulscale(ys, cosglobalang, -xs, singlobalang, 6);
                if (mulscale(abs(xp + yp), xdimen, 24) >= yp) {
                    spritesortcnt--; // Delete face sprite if on wrong side!
                    if (i == spritesortcnt) {
                        continue;
                    }
                    tspriteptr[i] = tspriteptr[spritesortcnt];
                    spritesx[i] = spritesx[spritesortcnt];
                    spritesy[i] = spritesy[spritesortcnt];
                    continue;
                }
                spritesx[i] = scale(xp + yp, (long) xdimen << 7, yp);
            } else if ((tspriteptr[i].getCstat() & 48) == 0) {
                if (!modelp) {
                    spritesortcnt--; // Delete face sprite if on wrong side!
                    if (i != spritesortcnt) {
                        tspriteptr[i] = tspriteptr[spritesortcnt];
                        spritesx[i] = spritesx[spritesortcnt];
                        spritesy[i] = spritesy[spritesortcnt];
                    }
                    continue;
                }
            }
            spritesy[i] = yp;
        }

        gap = 1;
        while (gap < spritesortcnt) {
            gap = (gap << 1) + 1;
        }
        for (gap >>= 1; gap > 0; gap >>= 1) // Sort sprite list
        {
            for (i = 0; i < spritesortcnt - gap; i++) {
                for (l = i; l >= 0; l -= gap) {
                    if (spritesy[l] <= spritesy[l + gap]) {
                        break;
                    }
                    swapsprite(l, l + gap, false);
                }
            }
        }
        if (spritesortcnt > 0) {
            spritesy[spritesortcnt] = (spritesy[spritesortcnt - 1] ^ 1);
        }

        ys = spritesy[0];
        i = 0;
        for (j = 1; j <= spritesortcnt; j++) {
            if (spritesy[j] == ys) {
                continue;
            }
            ys = spritesy[j];
            if (j > i + 1) {
                for (k = i; k < j; k++) {
                    spritesz[k] = tspriteptr[k].getZ();
                    if (tspriteptr[k].getPicnum() < 0 || tspriteptr[k].getPicnum() > MAXTILES) {
                        continue;
                    }

                    if ((tspriteptr[k].getCstat() & 48) != 32) {
                        ArtEntry pic = getTile(tspriteptr[k].getPicnum());
                        yoff = (byte) (pic.getOffsetY() + tspriteptr[k].getYoffset());
                        spritesz[k] -= ((yoff * tspriteptr[k].getYrepeat()) << 2);
                        yspan = (pic.getHeight() * tspriteptr[k].getYrepeat() << 2);
                        if ((tspriteptr[k].getCstat() & 128) == 0) {
                            spritesz[k] -= (yspan >> 1);
                        }
                        if (klabs(spritesz[k] - globalposz) < (yspan >> 1)) {
                            spritesz[k] = globalposz;
                        }
                    }
                }
                for (k = i + 1; k < j; k++) {
                    for (l = i; l < k; l++) {
                        if (klabs(spritesz[k] - globalposz) < klabs(spritesz[l] - globalposz)) {
                            swapsprite(k, l, true);
                        }
                    }
                }
                for (k = i + 1; k < j; k++) {
                    for (l = i; l < k; l++) {
                        if (tspriteptr[k].getStatnum() < tspriteptr[l].getStatnum()) {
                            swapsprite(k, l, false);
                        }
                        if ((tspriteptr[k].getCstat() & 2) != 0) // transparent sort
                        {
                            swapsprite(k, l, true);
                        }
                    }
                }
            }
            i = j;
        }

        curpolygonoffset = 0;

        drawmasks_pos.x = globalposx;
        drawmasks_pos.y = globalposy;

        gl.glEnable(GL10.GL_POLYGON_OFFSET_FILL);

        BoardService boardService = engine.getBoardService();
        while (maskwallcnt != 0) {

            maskwallcnt--;

            drawmasks_dot.x = boardService.getWall(thewall[maskwall[maskwallcnt]]).getX();
            drawmasks_dot.y = boardService.getWall(thewall[maskwall[maskwallcnt]]).getY();
            drawmasks_dot2.x = boardService.getWall(boardService.getWall(thewall[maskwall[maskwallcnt]]).getPoint2()).getX();
            drawmasks_dot2.y = boardService.getWall(boardService.getWall(thewall[maskwall[maskwallcnt]]).getPoint2()).getY();

            equation(drawmasks_maskeq, drawmasks_dot.x, drawmasks_dot.y, drawmasks_dot2.x, drawmasks_dot2.y);
            equation(drawmasks_p1eq, drawmasks_pos.x, drawmasks_pos.y, drawmasks_dot.x, drawmasks_dot.y);
            equation(drawmasks_p2eq, drawmasks_pos.x, drawmasks_pos.y, drawmasks_dot2.x, drawmasks_dot2.y);

            drawmasks_middle.x = (drawmasks_dot.x + drawmasks_dot2.x) / 2;
            drawmasks_middle.y = (drawmasks_dot.y + drawmasks_dot2.y) / 2;

            i = spritesortcnt;
            while (i != 0) {
                i--;
                if (tspriteptr[i] != null) {
                    drawmasks_spr.x = tspriteptr[i].getX();
                    drawmasks_spr.y = tspriteptr[i].getY();

                    if (!sameside(drawmasks_maskeq, drawmasks_spr, drawmasks_pos)
                            && sameside(drawmasks_p1eq, drawmasks_middle, drawmasks_spr)
                            && sameside(drawmasks_p2eq, drawmasks_middle, drawmasks_spr)) {
                        drawsprite(i);
                        tspriteptr[i] = null;
                    }
                }
            }

            // finally safe to draw the masked wall
            drawmaskwall(maskwallcnt);
        }

        while (spritesortcnt != 0) {
            spritesortcnt--;
            if (tspriteptr[spritesortcnt] != null) {
                drawsprite(spritesortcnt);
            }
        }

        gl.glDisable(GL10.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(0, 0);
    }

    @Override
    public void clearview(int dacol) {
        Palette curpalette = paletteManager.getCurrentPalette();
        gl.glClearColor(curpalette.getRed(dacol) / 255.0f, curpalette.getGreen(dacol) / 255.0f,
                curpalette.getBlue(dacol) / 255.0f, 0);
        gl.glClear(GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void nextpage() {
        // set drawrooms to true to make sure
        drawRooms = true;
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
        ogshang = -1;

        super.nextpage();
        gl.glFlush();
    }

    @Override
    public ByteBuffer getFrame(PixelFormat format, int xsiz, int ysiz) {
        boolean reverse = false;
        if (ysiz < 0) {
            ysiz *= -1;
            reverse = true;
        }

        int byteperpixel = 3;
        int fmt = GL10.GL_RGB;
        if (Gdx.app.getType() == ApplicationType.Android) {
            byteperpixel = 4;
            fmt = GL10.GL_RGBA;
        }

        ByteBuffer frameBuffer = ByteBuffer.allocateDirect(xsiz * ysiz * byteperpixel);
        gl.glPixelStorei(GL10.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, ydim - ysiz, xsiz, ysiz, fmt, GL10.GL_UNSIGNED_BYTE, frameBuffer);

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

    public int nearwall(int i, int range) {
        BoardService boardService = engine.getBoardService();
        Vector2 dcoord = this.dcoord.get(i);

        Sprite spr = boardService.getSprite(i);
        short sectnum = spr.getSectnum();
        int xs = spr.getX();
        int ys = spr.getY();

        int vx = mulscale(EngineUtils.cos(spr.getAng()), range, 14);
        int xe = xs + vx;
        int vy = mulscale(EngineUtils.sin(spr.getAng()), range, 14);
        int ye = ys + vy;

        short startwall = boardService.getSector(sectnum).getWallptr();
        int endwall = (startwall + boardService.getSector(sectnum).getWallnum() - 1);
        for (int z = startwall; z <= endwall; z++) {
            Wall wal = boardService.getWall(z);
            Wall wal2 = boardService.getWall(wal.getPoint2());
            int x1 = wal.getX();
            int y1 = wal.getY();
            int x2 = wal2.getX();
            int y2 = wal2.getY();

            if ((x1 - xs) * (y2 - ys) < (x2 - xs) * (y1 - ys)) {
                continue;
            }

            if (wal.getNextsector() != -1) {
                int daz = engine.getflorzofslope(sectnum, xs, ys);
                int daz2 = engine.getflorzofslope(wal.getNextsector(), xs, ys);

                ArtEntry pic = getTile(spr.getPicnum());
                boolean clipyou = false;
                int z1 = spr.getZ(), z2 = spr.getZ();
                int yoff = pic.getOffsetY();

                if ((spr.getCstat() & 128) != 0) {
                    z1 -= (yoff + pic.getHeight() / 2) * (spr.getYrepeat() << 2);
                    z2 += (pic.getHeight() - (pic.getHeight() / 2 + yoff)) * (spr.getYrepeat() << 2);
                } else {
                    z1 -= (yoff + pic.getHeight()) * (spr.getYrepeat() << 2);
                }

                if (daz2 < daz - (1 << 8)) {
                    if (z2 >= daz2) {
                        clipyou = true;
                    }
                }

                if (!clipyou) {
                    daz = engine.getceilzofslope(sectnum, xs, ys);
                    daz2 = engine.getceilzofslope(wal.getNextsector(), xs, ys);
                    if (daz2 > daz + (1 << 8)) {
                        if (z1 <= daz2) {
                            clipyou = true;
                        }
                    }
                }

                if (!clipyou) {
                    continue;
                }
            }

            Variable vvx = new Variable(); //FIXME
            Variable vvy = new Variable();
            Variable vvz = new Variable();
            if (engine.lIntersect(xs, ys, 0, xe, ye, 0, x1, y1, x2, y2, vvx, vvy, vvz)) {
                int dist = dmulscale(vvx.get() - xs, EngineUtils.cos(spr.getAng()), vvy.get() - ys,
                        EngineUtils.sin(spr.getAng()), 14);
                if (klabs(dist) <= 8) {
                    int wallang = EngineUtils.getAngle(boardService.getWall(wal.getPoint2()).getX() - wal.getX(), boardService.getWall(wal.getPoint2()).getY() - wal.getY()) - 512;
                    int nx = vvx.get() - mulscale(EngineUtils.cos(wallang), 4, 14);
                    int ny = vvy.get() - mulscale(EngineUtils.sin(wallang), 4, 14);
                    dcoord.x = nx - spr.getX();
                    dcoord.y = ny - spr.getY();
                }
                return z;
            }
        }

        return -1;
    }

    @Override
    public void loadModels() {
        for (int i = MAXTILES - 1; i >= 0; i--) {
            int pal = 0;
            modelManager.preload(i, pal, true);
        }
    }

    @Override
    public void onPrecacheTile(int dapicnum, boolean ifSprite) {
        int dapalnum = 0;
        int datype = ifSprite ? 4 : 0;

        // dapicnum and dapalnum are like you'd expect
        // datype is 0 for a wall/floor/ceiling and 1 for a sprite
        // basically this just means walls are repeating
        // while sprites are clamped

//		Console.out.println("precached " + dapicnum + " " + dapalnum + " type " + datype);
        textureCache.precache(texshader != null ? PixelFormat.Pal8 : PixelFormat.Rgba, getTile(dapicnum), dapalnum, datype);
    }

    public void addSpriteCorr(int snum) {
        int spr_wall;
        BoardService boardService = engine.getBoardService();
        Sprite spr = boardService.getSprite(snum);
        if (spr == null || spr.getStatnum() == MAXSTATUS) {
            removeSpriteCorr(snum);
            return;
        }

        if ((spr_wall = nearwall(snum, -64)) == -1) {
            if ((spr.getCstat() & 64) != 0 || (spr_wall = nearwall(snum, 64)) == -1) {
                return;
            }
        }

        spritewall.get(snum).set(true);
        float sang = spr.getAng() * 360 / 2048.0f;
        int wdx = boardService.getWall(spr_wall).getX() - boardService.getWall(boardService.getWall(spr_wall).getPoint2()).getX();
        int wdy = boardService.getWall(spr_wall).getY() - boardService.getWall(boardService.getWall(spr_wall).getPoint2()).getY();
        float wang = new Vector2(wdx, wdy).angleDeg() - 90;
        if (wang < 0) {
            wang += 360;
        }
        wang = BClipRange(wang, 0, 360);
        if (Math.abs(wang - sang) > 10) {
            return;
        }

        Vector2 dsin = this.dsin.get(snum);
        dsin.x = (EngineUtils.cos(spr.getAng() - 512) / 65536.0f) - (float) (Math.sin(Math.toRadians(wang)) / 4);
        dsin.y = (EngineUtils.sin(spr.getAng() - 512) / 65536.0f)
                - (float) (Math.sin(Math.toRadians(wang + 270)) / 4);
    }

    public IndexedShader getShader() {
        return texshader;
    }

    public PixelFormat getTextureFormat() {
        return texshader != null ? PixelFormat.Pal8 : PixelFormat.Rgba;
    }

    public void removeSpriteCorr(int snum) {
        dcoord.get(snum).setZero();
        dsin.get(snum).setZero();
        spritewall.get(snum).set(false);
    }

    @Override
    public void settiltang(int tilt) {
        if (tilt == 0) {
            gtang = 0.0f;
        } else {
            gtang = (float) (PI * tilt / 1024.0);
        }
    }

    public double polymost_getflorzofslope(int sectnum, double dax, double day) {
        BoardService boardService = engine.getBoardService();
        if (boardService.getSector(sectnum) == null) {
            return 0;
        }
        if ((boardService.getSector(sectnum).getFloorstat() & 2) == 0) {
            return (boardService.getSector(sectnum).getFloorz());
        }

        Wall wal = boardService.getWall(boardService.getSector(sectnum).getWallptr());
        int dx = boardService.getWall(wal.getPoint2()).getX() - wal.getX();
        int dy = boardService.getWall(wal.getPoint2()).getY() - wal.getY();
        long i = ((long) EngineUtils.sqrt(dx * dx + dy * dy) << 5);
        if (i == 0) {
            return (boardService.getSector(sectnum).getFloorz());
        }

        double j = (dx * (day - wal.getY()) - dy * (dax - wal.getX())) / 8;
        return boardService.getSector(sectnum).getFloorz() + boardService.getSector(sectnum).getFloorheinum() * j / i;
    }

    public double polymost_getceilzofslope(int sectnum, double dax, double day) {
        BoardService boardService = engine.getBoardService();
        if ((boardService.getSector(sectnum).getCeilingstat() & 2) == 0) {
            return (boardService.getSector(sectnum).getCeilingz());
        }

        Wall wal = boardService.getWall(boardService.getSector(sectnum).getWallptr());
        int dx = boardService.getWall(wal.getPoint2()).getX() - wal.getX();
        int dy = boardService.getWall(wal.getPoint2()).getY() - wal.getY();
        long i = ((long) EngineUtils.sqrt(dx * dx + dy * dy) << 5);
        if (i == 0) {
            return (boardService.getSector(sectnum).getCeilingz());
        }

        double j = (dx * (day - wal.getY()) - dy * (dax - wal.getX())) / 8;
        return boardService.getSector(sectnum).getCeilingz() + boardService.getSector(sectnum).getCeilingheinum() * j / i;
    }

    public void polymost_getzsofslope(int sectnum, double dax, double day) {
        BoardService boardService = engine.getBoardService();
        Sector sec = boardService.getSector(sectnum);
        if (sec == null) {
            return;
        }
        dceilzsofslope = sec.getCeilingz();
        dfloorzsofslope = sec.getFloorz();
        if (((sec.getCeilingstat() | sec.getFloorstat()) & 2) != 0) {
            Wall wal = boardService.getWall(sec.getWallptr());
            Wall wal2 = boardService.getWall(wal.getPoint2());
            int dx = wal2.getX() - wal.getX();
            int dy = wal2.getY() - wal.getY();
            long i = ((long) EngineUtils.sqrt(dx * dx + dy * dy) << 5);
            if (i == 0) {
                return;
            }
            double j = (dx * (day - wal.getY()) - dy * (dax - wal.getX())) / 8;

            if ((sec.getCeilingstat() & 2) != 0) {
                dceilzsofslope += boardService.getSector(sectnum).getCeilingheinum() * j / i;
            }
            if ((sec.getFloorstat() & 2) != 0) {
                dfloorzsofslope += boardService.getSector(sectnum).getFloorheinum() * j / i;
            }
        }
    }

    /**
     * Detecting when draw rooms was rendered to call {@link #onRoomsRenderingComplete}
     */
    private void checkDrawRoomsComplete() {
        if (drawRooms) {
            onRoomsRenderingComplete();
            drawRooms = false;
        }
    }

    public void onRoomsRenderingComplete() {
        // show fade here
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                             int cy1, int cx2, int cy2) {
        checkDrawRoomsComplete();
        globalfog.disable();
        ortho.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
        globalfog.enable();
    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {
        checkDrawRoomsComplete();
        globalfog.disable();

        Arrays.fill(gotsector, (byte) 0);

        ortho.drawmapview(dax, day, zoome, ang);
        globalfog.enable();
    }

    @Override
    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) {
        checkDrawRoomsComplete();
        globalfog.disable();
        ortho.drawoverheadmap(engine.getBoardService(), cposx, cposy, czoom, cang);
        globalfog.enable();
    }

    @Override
    public int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        checkDrawRoomsComplete();
        globalfog.disable();
        int width = ortho.printext(font, x, y, text, scale, shade, palnum, align, transparent, shadow);
        globalfog.enable();
        return width;
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int col) {
        checkDrawRoomsComplete();
        globalfog.disable();
        ortho.drawline256(x1, y1, x2, y2, col);
        globalfog.enable();
    }

    private IndexedShader allocIndexedShader() {
        try {
            String ver = Gdx.gl.glGetString(GL_SHADING_LANGUAGE_VERSION);
            if (ver == null) {
                return null;
            }

            return new IndexedShader(engine.getPaletteManager().getShadeCount()) {
                @Override
                public void bindPalette(int unit) {
                    Gdx.gl.glActiveTexture(unit);
                    textureCache.getPalette().bind(0);
                }

                @Override
                public void bindPalookup(int unit, int pal) {
                    Gdx.gl.glActiveTexture(unit);
                    // FIXME getPalookup == null at initscreen
                    textureCache.getPalookup(pal).bind(0);
                }
            };
        } catch (Throwable e) {
            Console.out.println("AllocIndexedShader error: " + e, OsdColor.RED);
        }

        return null;
    }

    protected Polymost2D allocOrphoRenderer(Engine engine) {
        return new Polymost2D(this, new DefaultMapSettings(engine.getBoardService()));
    }

    @Override
    public RenderType getType() {
        return RenderType.Polymost;
    }

    @Override
    public PixelFormat getTexFormat() {
        return PixelFormat.Rgb; // textureCache.getShader() != null ? PixelFormat.Pal8 : PixelFormat.Rgb;
    }

    @Override
    public void completemirror() {
        /* nothing */
    }

    @Override
    public void setview(int x1, int y1, int x2, int y2) {
        xdimen = (x2 - x1) + 1;
        ydimen = (y2 - y1) + 1;

        super.setview(x1, y1, x2, y2);
    }

    @Override
    public void setFieldOfView(int fovDegrees) {
        this.fovFactor = (float) Math.tan(fovDegrees * Math.PI / 360.0);
        setaspect();
    }

    protected void setaspect(int daxrange, int daaspect) {
        viewingrange = offscreenrendering ? daxrange : (int) (daxrange * fovFactor);

        yxaspect = daaspect;
        xyaspect = divscale(1, yxaspect, 32);
        xdimenscale = scale(xdimen, yxaspect, 320);
        xdimscale = scale(320, xyaspect, xdimen);
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
    public void onPalookupChanged(int palnum) {
        System.out.println("palette changed " + palnum);
        if (texshader != null) {
            textureCache.invalidatepalookup(palnum);
        } else {
            for (int j = MAXTILES - 1; j >= 0; j--) {
                invalidatetile(j, palnum, -1);
            }
        }
    }

    @Override
    public void onChangePalette(byte[] palette) {
        changepalette(palette);
    }

    @Override
    public void onLoadBoard(Board board) {
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

    @Override
    public PaletteManager getPaletteManager() {
        return engine.getPaletteManager();
    }
}
