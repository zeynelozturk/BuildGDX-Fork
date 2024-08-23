/*
 * "POLYMOST" code originally written by Ken Silverman
 * Ken Silverman's official web site: "http://www.advsys.net/ken"
 * See the included license file "BUILDLIC.TXT" for license info.
 *
 * This file has been modified from Ken Silverman's original release
 * by Jonathon Fowler (jf@jonof.id.au)
 * by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Render.Polymost;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.BufferUtils;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.IOverheadMapSettings;
import ru.m210projects.Build.Render.IOverheadMapSettings.MapView;
import ru.m210projects.Build.Render.OrphoRenderer;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.TextureManager;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Render.Types.Hudtyp;
import ru.m210projects.Build.Render.Types.Tile2model;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Types.font.*;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.nio.FloatBuffer;

import static com.badlogic.gdx.graphics.GL20.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Render.Polymost.Polymost.MAXWALLSB;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE0;
import static ru.m210projects.Build.Render.Types.GL10.*;
import static ru.m210projects.Build.Types.font.FontType.BITMAP_FONT;

public class Polymost2D extends OrphoRenderer {

    private final Polymost parent;
    private final TextureManager textureCache;
    private final FloatBuffer vertices = BufferUtils.newFloatBuffer(8);
    private final FloatBuffer textures = BufferUtils.newFloatBuffer(8);
    private final float[] trapextx = new float[2];
    //	private final int ROTATESPRITE_MAX = 2048;
    private final int RS_CENTERORIGIN = (1 << 30);
    private final Polygon[] drawpoly = new Polygon[4];
    private final BoardService boardService;
    protected int globalx1;
    protected int globaly1;
    protected int globalx2;
    protected int globaly2;
    protected int[] xb1 = new int[MAXWALLSB];
    protected int[] xb2 = new int[MAXWALLSB];
    protected float[] rx1 = new float[MAXWALLSB];
    protected float[] ry1 = new float[MAXWALLSB];
    protected int asm1; // drawmapview
    protected int asm2; // drawmapview
    private GL10 gl;
    private int guniqhudid;
    private int allocpoints = 0;
    private int[] slist;
    private int[] npoint2;
    private raster[] rst;
    private Sprite hudsprite;
    private double guo, gux; // Screen-based texture mapping parameters
    private double guy;
    private double gvo;
    private double gvx;
    private double gvy;
    private short globalpicnum;
    private int globalorientation;

    // Overhead map settings

    public Polymost2D(Polymost parent, IOverheadMapSettings settings) {
        super(parent, settings);
        this.parent = parent;
        this.boardService = parent.getBoardService();
        this.textureCache = parent.textureCache;

        for (int i = 0; i < 4; i++) {
            drawpoly[i] = new Polygon();
        }

        vertices.put(new float[]{0, 0, 1, 0, 1, 1, 0, 1});
        textures.put(new float[]{0, 0, 1 - 0.0001f, 0, 1 - 0.0001f, 1 - 0.0001f, 0, 1 - 0.0001f});
        vertices.rewind();
        textures.rewind();
    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {
        Wall wal;
        Sector sec = null;

        int i, j, x, y, bakx1, baky1;
        int s, w, ox, oy, startwall, cx1, cy1, cx2, cy2;
        int bakgxvect, bakgyvect, npoints;
        int xvect, yvect, xvect2, yvect2, daslope;

        int xoff, yoff, k, l, cosang, sinang, xspan, yspan;
        int xrepeat, yrepeat, x1, y1, x2, y2, x3, y3, x4, y4;

        final int numsectors = boardService.getSectorCount();

        parent.beforedrawrooms = 0;
        cx1 = (parent.windowx1 << 12);
        cy1 = (parent.windowy1 << 12);
        cx2 = ((parent.windowx2 + 1) << 12) - 1;
        cy2 = ((parent.windowy2 + 1) << 12) - 1;
        zoome <<= 8;
        bakgxvect = divscale(EngineUtils.sin(1536 - ang), zoome, 28);
        bakgyvect = divscale(EngineUtils.sin(2048 - ang), zoome, 28);
        xvect = mulscale(EngineUtils.sin(2048 - ang), zoome, 8);
        yvect = mulscale(EngineUtils.sin(1536 - ang), zoome, 8);
        xvect2 = mulscale(xvect, parent.yxaspect, 16);
        yvect2 = mulscale(yvect, parent.yxaspect, 16);

        for (s = 0; s < numsectors; s++) {
            sec = boardService.getSector(s);

            if (mapSettings.isFullMap() || show2dsector.getBit(s)) {
                npoints = 0;
                i = 0;
                startwall = sec.getWallptr();

                j = startwall;
                if (startwall < 0) {
                    continue;
                }
                for (w = sec.getWallnum(); w > 0; w--, j++) {
                    wal = boardService.getWall(j);
                    if (wal == null) {
                        continue;
                    }
                    ox = wal.getX() - dax;
                    oy = wal.getY() - day;
                    x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                    y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
                    i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
                    rx1[npoints] = x;
                    ry1[npoints] = y;
                    xb1[npoints] = wal.getPoint2() - startwall;
                    if (xb1[npoints] < 0) {
                        xb1[npoints] = 0;
                    }

                    npoints++;
                }

                if ((i & 0xf0) != 0xf0) {
                    continue;
                }

                bakx1 = (int) rx1[0];
                baky1 = mulscale((int) ry1[0] - ((long) ydim << 11), parent.xyaspect, 16) + (ydim << 11);

                if (mapSettings.isShowFloorSprites()) {
                    // Collect floor sprites to draw
                    for (ListNode<Sprite> node = boardService.getSectNode(s); node != null; node = node.getNext()) {
                        int j1 = node.getIndex();
                        if ((node.get().getCstat() & 48) == 32) {
                            if ((node.get().getCstat() & (64 + 8)) == (64 + 8)
                                    || !mapSettings.isSpriteVisible(MapView.Polygons, j1)) {
                                continue;
                            }

                            parent.addRenderedSprite(j1);
                        }
                    }
                }

                if (mapSettings.isShowSprites(MapView.Polygons)) {
                    for (ListNode<Sprite> node = boardService.getSectNode(s); node != null; node = node.getNext()) {
                        int i1 = node.getIndex();
                        if (show2dsprite.getBit(i1)) {
                            if (!mapSettings.isSpriteVisible(MapView.Polygons, i1)) {
                                continue;
                            }

                            parent.addRenderedSprite(i1);
                        }
                    }
                }
                parent.gotsector[s >> 3] |= pow2char[s & 7];

                globalorientation = sec.getFloorstat();
                if ((globalorientation & 1) != 0) {
                    continue;
                }
                parent.globalpal = sec.getFloorpal();

                globalpicnum = sec.getFloorpicnum();
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }
                parent.setgotpic(globalpicnum);
                ArtEntry pic = parent.getTile(globalpicnum);

                if (!pic.hasSize()) {
                    continue;
                }

                if (pic.getType() != AnimType.NONE) {
                    globalpicnum += (short) parent.animateoffs(globalpicnum, s);
                    pic = parent.getTile(globalpicnum);
                }

                int numshades = parent.getPaletteManager().getShadeCount();
                parent.globalshade = max(min(sec.getFloorshade(), numshades - 1), 0);

                if ((globalorientation & 64) == 0) {
                    parent.globalposx = dax;
                    globalx1 = bakgxvect;
                    globaly1 = bakgyvect;
                    parent.globalposy = day;
                    globalx2 = bakgxvect;
                    globaly2 = bakgyvect;
                } else {
                    ox = boardService.getWall(boardService.getWall(startwall).getPoint2()).getX() - boardService.getWall(startwall).getX();
                    oy = boardService.getWall(boardService.getWall(startwall).getPoint2()).getY() - boardService.getWall(startwall).getY();
                    i = EngineUtils.sqrt(ox * ox + oy * oy);
                    if (i == 0) {
                        continue;
                    }
                    i = 1048576 / i;
                    globalx1 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
                    globaly1 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);
                    ox = (bakx1 >> 4) - (xdim << 7);
                    oy = (baky1 >> 4) - (ydim << 7);
                    parent.globalposx = dmulscale(-oy, globalx1, -ox, globaly1, 28);
                    parent.globalposy = dmulscale(-ox, globalx1, oy, globaly1, 28);
                    globalx2 = -globalx1;
                    globaly2 = -globaly1;

                    daslope = boardService.getSector(s).getFloorheinum();
                    i = EngineUtils.sqrt(daslope * daslope + 16777216);
                    parent.globalposy = mulscale(parent.globalposy, i, 12);
                    globalx2 = mulscale(globalx2, i, 12);
                    globaly2 = mulscale(globaly2, i, 12);
                }
                int globalxshift = (8 - pic.getSizex());
                int globalyshift = (8 - pic.getSizey());
                if ((globalorientation & 8) != 0) {
                    globalxshift++;
                    globalyshift++;
                }

                if ((globalorientation & 0x4) > 0) {
                    i = parent.globalposx;
                    parent.globalposx = -parent.globalposy;
                    parent.globalposy = -i;
                    i = globalx2;
                    globalx2 = globaly1;
                    globaly1 = i;
                    i = globalx1;
                    globalx1 = -globaly2;
                    globaly2 = -i;
                }
                if ((globalorientation & 0x10) > 0) {
                    globalx1 = -globalx1;
                    globaly1 = -globaly1;
                    parent.globalposx = -parent.globalposx;
                }
                if ((globalorientation & 0x20) > 0) {
                    globalx2 = -globalx2;
                    globaly2 = -globaly2;
                    parent.globalposy = -parent.globalposy;
                }
                asm1 = globaly1 << globalxshift;
                asm2 = globalx2 << globalyshift;
                globalx1 <<= globalxshift;
                globaly2 <<= globalyshift;
                parent.globalposx = (parent.globalposx << (20 + globalxshift)) + ((sec.getFloorxpanning()) << 24);
                parent.globalposy = (parent.globalposy << (20 + globalyshift)) - ((sec.getFloorypanning()) << 24);

                fillpolygon(npoints);
            }
        }

        if (mapSettings.isShowSprites(MapView.Polygons) || mapSettings.isShowFloorSprites()) {
            // Sort sprite list
            int gap = 1;
            Pool<TSprite> tsprite = parent.getRenderedSprites();
            int sortnum = parent.getRenderedSprites().getSize();
            while (gap < sortnum) {
                gap = (gap << 1) + 1;
            }
            for (gap >>= 1; gap > 0; gap >>= 1) {
                for (i = 0; i < sortnum - gap; i++) {
                    for (j = i; j >= 0; j -= gap) {
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

            for (s = sortnum - 1; s >= 0; s--) {
                Sprite spr = boardService.getSprite(tsprite.get(s).getOwner());
                if ((spr.getCstat() & 32768) == 0) {
                    npoints = 0;

                    if (spr.getPicnum() >= MAXTILES) {
                        spr.setPicnum(0);
                    }

                    ArtEntry pic = parent.getTile(spr.getPicnum());

                    xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                    yoff = (byte) (pic.getOffsetY() + spr.getYoffset());
                    if ((spr.getCstat() & 4) > 0) {
                        xoff = -xoff;
                    }
                    if ((spr.getCstat() & 8) > 0) {
                        yoff = -yoff;
                    }

                    cosang = EngineUtils.cos(spr.getAng());
                    sinang = EngineUtils.sin(spr.getAng());
                    xspan = pic.getWidth();
                    xrepeat = spr.getXrepeat();
                    yspan = pic.getHeight();
                    yrepeat = spr.getYrepeat();
                    ox = ((xspan >> 1) + xoff) * xrepeat;
                    oy = ((yspan >> 1) + yoff) * yrepeat;
                    x1 = spr.getX() + mulscale(sinang, ox, 16) + mulscale(cosang, oy, 16);
                    y1 = spr.getY() + mulscale(sinang, oy, 16) - mulscale(cosang, ox, 16);
                    l = xspan * xrepeat;
                    x2 = x1 - mulscale(sinang, l, 16);
                    y2 = y1 + mulscale(cosang, l, 16);
                    l = yspan * yrepeat;
                    k = -mulscale(cosang, l, 16);
                    x3 = x2 + k;
                    x4 = x1 + k;
                    k = -mulscale(sinang, l, 16);
                    y3 = y2 + k;
                    y4 = y1 + k;

                    xb1[0] = 1;
                    xb1[1] = 2;
                    xb1[2] = 3;
                    xb1[3] = 0;
                    npoints = 4;

                    i = 0;

                    ox = x1 - dax;
                    oy = y1 - day;
                    x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                    y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
                    i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
                    rx1[0] = x;
                    ry1[0] = y;

                    ox = x2 - dax;
                    oy = y2 - day;
                    x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                    y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
                    i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
                    rx1[1] = x;
                    ry1[1] = y;

                    ox = x3 - dax;
                    oy = y3 - day;
                    x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                    y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
                    i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
                    rx1[2] = x;
                    ry1[2] = y;

                    x = (int) (rx1[0] + rx1[2] - rx1[1]);
                    y = (int) (ry1[0] + ry1[2] - ry1[1]);
                    i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
                    rx1[3] = x;
                    ry1[3] = y;

                    if ((i & 0xf0) != 0xf0) {
                        continue;
                    }
                    bakx1 = (int) rx1[0];
                    baky1 = mulscale((int) ry1[0] - ((long) ydim << 11), parent.xyaspect, 16) + (ydim << 11);

                    globalpicnum = spr.getPicnum();
                    parent.globalpal = spr.getPal(); // GL needs this, software doesn't
                    parent.setgotpic(globalpicnum);
                    ArtEntry sprpic = parent.getTile(globalpicnum);

                    if (!sprpic.hasSize()) {
                        continue;
                    }
                    if (sprpic.getType() != AnimType.NONE) {
                        globalpicnum += parent.animateoffs(globalpicnum, s);
                        sprpic = parent.getTile(globalpicnum);
                    }

                    // 'loading' the tile doesn't actually guarantee that it's there afterwards.
                    // This can really happen when drawing the second frame of a floor-aligned
                    // 'storm icon' sprite (4894+1)

                    if ((boardService.getSector(spr.getSectnum()).getCeilingstat() & 1) > 0) {
                        parent.globalshade = (boardService.getSector(spr.getSectnum()).getCeilingshade());
                    } else {
                        parent.globalshade = (boardService.getSector(spr.getSectnum()).getFloorshade());
                    }
                    int numshades = parent.getPaletteManager().getShadeCount();
                    parent.globalshade = max(min(parent.globalshade + spr.getShade() + 6, numshades - 1), 0);

                    // relative alignment stuff
                    ox = x2 - x1;
                    oy = y2 - y1;
                    i = ox * ox + oy * oy;
                    if (i == 0) {
                        continue;
                    }
                    i = (65536 * 16384) / i;
                    globalx1 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
                    globaly1 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);
                    ox = y1 - y4;
                    oy = x4 - x1;
                    i = ox * ox + oy * oy;
                    if (i == 0) {
                        continue;
                    }
                    i = (65536 * 16384) / i;
                    globalx2 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
                    globaly2 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);

                    ox = pic.getSizex();
                    if (pow2long[ox] != xspan) {
                        ox++;
                        globalx1 = mulscale(globalx1, xspan, ox);
                        globaly1 = mulscale(globaly1, xspan, ox);
                    }

                    bakx1 = (bakx1 >> 4) - (xdim << 7);
                    baky1 = (baky1 >> 4) - (ydim << 7);
                    parent.globalposx = dmulscale(-baky1, globalx1, -bakx1, globaly1, 28);
                    parent.globalposy = dmulscale(bakx1, globalx2, -baky1, globaly2, 28);

                    if ((spr.getCstat() & 0x4) > 0) {
                        globalx1 = -globalx1;
                        globaly1 = -globaly1;
                        parent.globalposx = -parent.globalposx;
                    }
                    asm1 = globaly1 << 2;
                    globalx1 <<= 2;
                    parent.globalposx <<= (20 + 2);
                    asm2 = globalx2 << 2;
                    globaly2 <<= 2;
                    parent.globalposy <<= (20 + 2);

                    // so polymost can get the translucency. ignored in software mode:
                    globalorientation = ((spr.getCstat() & 2) << 7) | ((spr.getCstat() & 512) >> 2);

                    fillpolygon(npoints);
                }
            }
        }
    }

    protected void setpolymost2dview() {
        if (parent.gloy1 != -1 || parent.gloy1 != parent.windowy1) {
            parent.glViewport(0, 0, xdim, ydim);
            gl.glMatrixMode(GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0, xdim, ydim, 0, -1, 1);
            gl.glMatrixMode(GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        parent.gloy1 = -1;

        gl.glDisable(GL_DEPTH_TEST);
        gl.glDisable(GL_TEXTURE_2D);
        gl.glDisable(GL_BLEND);
    }

    protected void fillpolygon(int npoints) {
        for (int z = 0; z < npoints; z++) {
            if (xb1[z] >= npoints) {
                xb1[z] = 0;
            }
        }

        if (!parent.getPaletteManager().isValidPalette(parent.globalpal)) {
            parent.globalpal = 0;
        }

        setpolymost2dview();
        gl.glEnable(GL_ALPHA_TEST);
        gl.glEnable(GL_TEXTURE_2D);
        int method = (globalorientation >> 7) & 3;
        if (method == 0) {
            gl.glDisable(GL_BLEND);
        } else {
            gl.glEnable(GL_BLEND);
        }

        GLTile pth = parent.bind(parent.getTile(globalpicnum), parent.globalpal, parent.globalshade, 0, method);
        if (pth == null) {
            return;
        }

        textureCache.deactivateEffects(gl); // deactivate effects
        parent.bind(pth);

        globalx1 = mulscale(globalx1, parent.xyaspect, 16);
        globaly2 = mulscale(globaly2, parent.xyaspect, 16);
        gux = asm1 / 4294967296.0;
        gvx = asm2 / 4294967296.0;
        guy = globalx1 / 4294967296.0;
        gvy = -globaly2 / 4294967296.0;
        guo = (xdim * gux + ydim * guy) * -0.5 + parent.globalposx / 4294967296.0;
        gvo = (xdim * gvx + ydim * gvy) * -0.5 - parent.globalposy / 4294967296.0;

        for (int i = npoints - 1; i >= 0; i--) {
            rx1[i] /= 4096.0f;
            ry1[i] /= 4096.0f;
        }

        tessectrap(rx1, ry1, xb1, npoints); // vertices + textures
    }

    private void drawtrap(float x0, float x1, float y0, float x2, float x3, float y1) {
        if (y0 == y1) {
            return;
        }

        drawpoly[0].px = x0;
        drawpoly[0].py = y0;
        drawpoly[2].py = y1;

        int n = 3;
        if (x0 == x1) {
            drawpoly[1].px = x3;
            drawpoly[1].py = y1;
            drawpoly[2].px = x2;
        } else if (x2 == x3) {
            drawpoly[1].px = x1;
            drawpoly[1].py = y0;
            drawpoly[2].px = x3;
        } else {
            drawpoly[1].px = x1;
            drawpoly[1].py = y0;
            drawpoly[2].px = x3;
            drawpoly[3].px = x2;
            drawpoly[3].py = y1;
            n = 4;
        }

        gl.glBegin(GL_TRIANGLE_FAN);
        for (int i = 0; i < n; i++) {
            drawpoly[i].px = min(max(drawpoly[i].px, trapextx[0]), trapextx[1]);
            gl.glTexCoord2d(drawpoly[i].px * gux + drawpoly[i].py * guy + guo,
                    drawpoly[i].px * gvx + drawpoly[i].py * gvy + gvo);
            gl.glVertex2d(drawpoly[i].px, drawpoly[i].py);
        }
        gl.glEnd();
    }

    private void tessectrap(float[] px, float[] py, int[] point2, int numpoints) {
        float x0, x1, m0, m1;
        int i, j, k, z, i0, i1, i2, i3, npoints, gap, numrst;

        if (numpoints + 16 > allocpoints) // 16 for safety
        {
            allocpoints = numpoints + 16;
            rst = new raster[allocpoints];
            for (i = 0; i < allocpoints; i++) {
                rst[i] = new raster();
            }

            slist = new int[allocpoints];

            npoint2 = new int[allocpoints];
        }

        // Remove unnecessary collinear points:
        for (i = 0; i < numpoints; i++) {
            npoint2[i] = point2[i];
        }
        npoints = numpoints;
        z = 0;

        for (i = 0; i < numpoints; i++) {
            j = npoint2[i];
            if ((point2[i] < i) && (i < numpoints - 1)) {
                z = 3;
            }

            if (j < 0) {
                continue;
            }
            k = npoint2[j];
            if (k < 0) {
                continue;
            }

            m0 = (px[j] - px[i]) * (py[k] - py[j]);
            m1 = (py[j] - py[i]) * (px[k] - px[j]);
            if (m0 < m1) {
                z |= 1;
                continue;
            }
            if (m0 > m1) {
                z |= 2;
                continue;
            }
            npoint2[i] = k;
            npoint2[j] = -1;
            npoints--;
            i--; // collinear
        }

        if (z == 0) {
            return;
        }
        trapextx[0] = trapextx[1] = px[0];
        for (i = j = 0; i < numpoints; i++) {
            if (npoint2[i] < 0) {
                continue;
            }
            if (px[i] < trapextx[0]) {
                trapextx[0] = px[i];
            }
            if (px[i] > trapextx[1]) {
                trapextx[1] = px[i];
            }
            slist[j++] = i;
        }

        parent.globalfog.apply();

        if (z != 3) // Simple polygon... early out
        {
            gl.glBegin(GL_TRIANGLE_FAN);
            for (i = 0; i < npoints; i++) {
                j = slist[i];
                gl.glTexCoord2f((float) (px[j] * gux + py[j] * guy + guo), (float) (px[j] * gvx + py[j] * gvy + gvo));
                gl.glVertex2d(px[j], py[j]);
            }
            gl.glEnd();
            return;
        }

        // Sort points by y's
        for (gap = (npoints >> 1); gap != 0; gap >>= 1) {
            for (i = 0; i < npoints - gap; i++) {
                for (j = i; j >= 0; j -= gap) {
                    if (py[npoint2[slist[j]]] <= py[npoint2[slist[j + gap]]]) {
                        break;
                    }
                    k = slist[j];
                    slist[j] = slist[j + gap];
                    slist[j + gap] = k;
                }
            }
        }

        numrst = 0;
        for (z = 0; z < npoints; z++) {
            i0 = slist[z];
            i1 = npoint2[i0];
            if (py[i0] == py[i1] || npoint2[i1] == -1) {
                continue;
            }
            i2 = i1;
            i3 = npoint2[i1];
            if (py[i1] == py[i3]) {
                i2 = i3;
                i3 = npoint2[i3];
            }

            // i0 i3
            // \ /
            // i1--i2
            // / \ ~
            // i0 i3

            if ((py[i1] < py[i0]) && (py[i2] < py[i3])) // Insert raster
            {
                for (i = numrst; i > 0; i--) {
                    if (rst[i - 1].xi * (py[i1] - rst[i - 1].y) + rst[i - 1].x < px[i1]) {
                        break;
                    }
                    rst[i + 1].set(rst[i - 1]);
                }
                numrst += 2;
                if ((i & 1) != 0) // split inside area
                {
                    j = i - 1;
                    x0 = (py[i1] - rst[j].y) * rst[j].xi + rst[j].x;
                    x1 = (py[i1] - rst[j + 1].y) * rst[j + 1].xi + rst[j + 1].x;
                    drawtrap(rst[j].x, rst[j + 1].x, rst[j].y, x0, x1, py[i1]);
                    rst[j].x = x0;
                    rst[j].y = py[i1];
                    rst[j + 3].x = x1;
                    rst[j + 3].y = py[i1];
                }

                m0 = (px[i0] - px[i1]) / (py[i0] - py[i1]);
                m1 = (px[i3] - px[i2]) / (py[i3] - py[i2]);

                j = ((px[i1] > px[i2] || (i1 == i2) && (m0 >= m1)) ? 1 : 0) + i;
                if (j < 0) {
                    continue;
                }
                k = (i << 1) + 1 - j;

                rst[j].i = i0;
                rst[j].xi = m0;
                rst[j].x = px[i1];
                rst[j].y = py[i1];
                rst[k].i = i3;
                rst[k].xi = m1;
                rst[k].x = px[i2];
                rst[k].y = py[i2];
            } else {
                // NOTE:don't count backwards!
                if (i1 == i2) {
                    for (i = 0; i < numrst; i++) {
                        if (rst[i].i == i1) {
                            break;
                        }
                    }
                } else {
                    for (i = 0; i < numrst; i++) {
                        if ((rst[i].i == i1) || (rst[i].i == i2)) {
                            break;
                        }
                    }
                }
                j = i & ~1;

                if ((py[i1] > py[i0]) && (py[i2] > py[i3])) // Delete raster
                {
                    for (; j <= i + 1; j += 2) {
                        x0 = (py[i1] - rst[j].y) * rst[j].xi + rst[j].x;
                        if ((i == j) && (i1 == i2)) {
                            x1 = x0;
                        } else {
                            x1 = (py[i1] - rst[j + 1].y) * rst[j + 1].xi + rst[j + 1].x;
                        }
                        drawtrap(rst[j].x, rst[j + 1].x, rst[j].y, x0, x1, py[i1]);
                        rst[j].x = x0;
                        rst[j].y = py[i1];
                        rst[j + 1].x = x1;
                        rst[j + 1].y = py[i1];
                    }
                    numrst -= 2;
                    for (; i < numrst; i++) {
                        rst[i].set(rst[i + 2]);
                    }
                } else {
                    x0 = (py[i1] - rst[j].y) * rst[j].xi + rst[j].x;
                    x1 = (py[i1] - rst[j + 1].y) * rst[j + 1].xi + rst[j + 1].x;

                    drawtrap(rst[j].x, rst[j + 1].x, rst[j].y, x0, x1, py[i1]);
                    rst[j].x = x0;
                    rst[j].y = py[i1];
                    rst[j + 1].x = x1;
                    rst[j + 1].y = py[i1];

                    if (py[i0] < py[i3]) {
                        rst[i].x = px[i2];
                        rst[i].y = py[i2];
                        rst[i].i = i3;
                    } else {
                        rst[i].x = px[i1];
                        rst[i].y = py[i1];
                        rst[i].i = i0;
                    }
                    rst[i].xi = (px[rst[i].i] - rst[i].x) / (py[rst[i].i] - py[i1]);
                }

            }
        }
    }

    @Override
    public int printext(Font font, int xpos, int ypos, char[] text, float scale, int shade, int palnum, TextAlign textAlign, Transparent transparent, boolean shadow) {
        if (font == null || text == null || text.length == 0) {
            return 0;
        }

        if (shadow) {
            printext(font, xpos + (int) scale, ypos + (int) scale, text, scale, 127, palnum, textAlign, transparent, false);
        }

        setpolymost2dview();
        float scaleyf = scale;
        if (font.isVerticalScaled()) {
            scaleyf *= 1.2f;
        }

        gl.glDisable(GL_ALPHA_TEST);
        gl.glDepthMask(false); // disable writing to the z-buffer

        gl.glEnable(GL_TEXTURE_2D);
        gl.glEnable(GL_BLEND);
        parent.globalpal = palnum;
        parent.globalshade = shade;
        parent.globalfog.apply();

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

        GLTile pth = null;
        int currentTile = -1;
        for (int c = 0; c < text.length && text[c] != 0; c++) {
            CharInfo charInfo = font.getCharInfo(text[c]);

            final int tile = charInfo.tile;
            final float cellsizx = scale * charInfo.getCellSize();
            globalpicnum = (short) tile;

            if (tile != -1) {

                float tx1, ty1;
                float tx2, ty2;

                final int charsizex = (int) (scale * charInfo.getWidth());
                final int charsizey = (int) (scaleyf * charInfo.getHeight());
                final int xoffset = (int) (scale * charInfo.xOffset);
                final int yoffset = (int) (scaleyf * charInfo.yOffset);

                if (currentTile != tile) {

                    if (currentTile != -1) {
                        gl.glEnd();
                    }

                    Color polyColor = parent.getshadefactor(shade, 0);
                    if (transparent == Transparent.Bit1) {
                        polyColor.a = parent.TRANSLUSCENT1;
                    } else if (transparent == Transparent.Bit2) {
                        polyColor.a = parent.TRANSLUSCENT2;
                    }

                    if (charInfo.getFontType() == BITMAP_FONT) {
                        pth = textureCache.getBitmapFontAtlas((BitmapFont) charInfo.getParent());
                        parent.bind(pth);

                        palnum = parent.getPaletteManager().getColorIndex(0, palnum, shade);
                        Palette curpalette = parent.getPaletteManager().getCurrentPalette();
                        gl.glColor4ub(curpalette.getRed(palnum), curpalette.getGreen(palnum), curpalette.getBlue(palnum), (int) (polyColor.a * 255));
                    } else {
                        if (!parent.getPaletteManager().isValidPalette(palnum)) {
                            palnum = 0;
                        }

                        // tiled atlas or char tile
                        ArtEntry charTile = parent.getTile(tile);
                        if (!charTile.exists()) {
                            continue;
                        }

                        pth = parent.bind(charTile, palnum, shade, 0, 1 | 4);
                        gl.glColor4f(polyColor.r, polyColor.g, polyColor.b, polyColor.a);
                    }

                    if (pth == null) {
                        currentTile = -1;
                        continue;
                    }

                    if (pth.getPixelFormat() == PixelFormat.Pal8) {
                        parent.getShader().setTransparent(polyColor.a);
                    }

                    currentTile = tile;
                    gl.glBegin(GL_TRIANGLES);
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
                        // #GDX 29.07.2024 PixmapData didn't resize
                        tsizx = GLInfo.calcSize(pth.getWidth()) / pth.getXScale();
                        tsizy = GLInfo.calcSize(pth.getHeight()) / pth.getYScale();
                    }

                    tx2 = charInfo.getWidth() / (tsizx * tileScale);
                    ty2 = charInfo.getHeight() / (tsizy * tileScale);
                }

                gl.glTexCoord2f(tx1, ty1);
                gl.glVertex2i(vx1, vy1);
                gl.glTexCoord2f(tx1, ty2);
                gl.glVertex2i(vx1, vy2);
                gl.glTexCoord2f(tx2, ty2);
                gl.glVertex2i(vx2, vy2);

                gl.glTexCoord2f(tx1, ty1);
                gl.glVertex2i(vx1, vy1);
                gl.glTexCoord2f(tx2, ty1);
                gl.glVertex2i(vx2, vy1);
                gl.glTexCoord2f(tx2, ty2);
                gl.glVertex2i(vx2, vy2);
            }
            xpos += (int) cellsizx;
            alignx += (int) cellsizx;
        }
        gl.glEnd();

        gl.glDepthMask(true); // re-enable writing to the z-buffer

        textureCache.deactivateEffects(gl);

        return (int) (alignx / scale);
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int col) {
        setpolymost2dview(); // JBF 20040205: more efficient setup

        parent.globalfog.apply();

        boolean hasShader = parent.getShader() != null && parent.getShader().isBinded();
        if (hasShader) {
            parent.getShader().unbind();
        }

        float fx1 = x1 / 4096.0f;
        float fx2 = x2 / 4096.0f;
        float fy1 = y1 / 4096.0f;
        float fy2 = y2 / 4096.0f;

        if (!checkIntersect(fx1, fy1, fx2, fy2)) {
            return;
        }

        PaletteManager paletteManager = parent.getPaletteManager();
        Palette curpalette = paletteManager.getCurrentPalette();
        col = paletteManager.getColorIndex(0, col);
        gl.glBegin(GL_LINES);
        gl.glColor4ub(curpalette.getRed(col), curpalette.getGreen(col), curpalette.getBlue(col), 255);
        gl.glVertex2f(fx1, fy1);
        gl.glVertex2f(fx2, fy2);
        gl.glEnd();

        if (hasShader) {
            parent.getShader().bind();
        }
    }

    private boolean checkIntersect(float x1, float y1, float x2, float y2) {
        if (x1 > x2) {
            float tmp = x1;
            x1 = x2;
            x2 = tmp;
        }

        if (y1 > y2) {
            float tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        return x2 >= 0 && x1 <= xdim && y2 >= 0 && y1 <= ydim;
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                             int cy1, int cx2, int cy2) {

        if (picnum >= MAXTILES) {
            return;
        }
        if ((cx1 > cx2) || (cy1 > cy2)) {
            return;
        }
        if (z <= 16) {
            return;
        }

        ArtEntry pic = parent.getTile(picnum);

        if (pic.getType() != AnimType.NONE) {
            picnum += parent.animateoffs(picnum, 0);
            pic = parent.getTile(picnum);
        }

        if (!pic.hasSize()) {
            return;
        }

        if ((dastat & 128) == 0 || parent.beforedrawrooms != 0) {
            dorotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2, guniqhudid);
        }
    }

    protected void dorotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat,
                                  int cx1, int cy1, int cx2, int cy2, int uniqid) {

        int ourxyaspect = parent.xyaspect;
        if (parent.getConfig().isUseModels() && parent.defs != null && parent.defs.mdInfo.getHudInfo(picnum, dastat) != null
                && parent.defs.mdInfo.getHudInfo(picnum, dastat).angadd != 0) {
            Tile2model entry = parent.defs != null ? parent.defs.mdInfo.getParams(picnum) : null;
            if (entry != null && entry.model != null && entry.framenum >= 0) {
                dorotatesprite3d(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2, uniqid);
                return;
            }
        }

        short ogpicnum = globalpicnum;
        globalpicnum = (short) picnum;
        int ogshade = parent.globalshade;
        parent.globalshade = dashade;
        int ogpal = parent.globalpal;
        parent.globalpal = dapalnum & 0xFF;

        if ((dastat & 10) == 2) {
            parent.glViewport(parent.windowx1, ydim - (parent.windowy2 + 1), parent.windowx2 - parent.windowx1 + 1, parent.windowy2 - parent.windowy1 + 1);
        } else {
            parent.glViewport(0, 0, xdim, ydim);
            parent.glox1 = -1; // Force fullscreen (glox1=-1 forces it to restore)
        }

        gl.glMatrixMode(GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0, xdim - 1, ydim - 1, 0, -1, 1);
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL_DEPTH_TEST);
        gl.glDisable(GL_ALPHA_TEST);
        gl.glEnable(GL_TEXTURE_2D);

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

        ArtEntry pic = parent.getTile(globalpicnum);

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

                if ((dastat & RS_CENTERORIGIN) != 0) {
                    sx += oxdim << 15;
                }

                zoomsc = scale(xdim, ouryxaspect, 320);
                sy = (ydim << 15) + mulscale(normyofs, zoomsc, 16);
            }

            z = mulscale(z, zoomsc, 16);
        }

        gl.glEnable(GL_CLIP_PLANE0);
        gl.glClipPlanef(GL_CLIP_PLANE0, 1, 0, 0, -cx1);
        gl.glEnable(GL_CLIP_PLANE0 + 1);
        gl.glClipPlanef(GL_CLIP_PLANE0 + 1, -1, 0, 0, cx2);

        gl.glEnable(GL_CLIP_PLANE0 + 2);
        gl.glClipPlanef(GL_CLIP_PLANE0 + 2, 0, 1, 0, -cy1);
        gl.glEnable(GL_CLIP_PLANE0 + 3);
        gl.glClipPlanef(GL_CLIP_PLANE0 + 3, 0, -1, 0, cy2);

        float aspectFix = ((dastat & 2) != 0) || ((dastat & 8) == 0) ? ourxyaspect / 65536.0f : 1.0f;
        float scale = z / 65536.0f;
        float cx = sx / 65536.0f;
        float cy = sy / 65536.0f;
        gl.glTranslatef(cx, cy, 0);
        gl.glScalef(1, 1 / aspectFix, 0);
        gl.glRotatef(360.0f * a / 2048.0f, 0, 0, 1);
        gl.glScalef(scale * aspectFix, scale * aspectFix, 0);
        gl.glTranslatef(-xoff, -yoff, 0);
        gl.glScalef(xsiz, ysiz, 0);

        drawrotate(method, dastat);

        gl.glDisable(GL_CLIP_PLANE0);
        gl.glDisable(GL_CLIP_PLANE0 + 1);
        gl.glDisable(GL_CLIP_PLANE0 + 2);
        gl.glDisable(GL_CLIP_PLANE0 + 3);

        gl.glMatrixMode(GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glPopMatrix();

        globalpicnum = ogpicnum;
        parent.globalshade = ogshade;
        parent.globalpal = ogpal & 0xFF;
    }

    protected void drawrotate(int method, int dastat) {

        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }
        if (!parent.getPaletteManager().isValidPalette(parent.globalpal)) {
            parent.globalpal = 0;
        }

        parent.setgotpic(globalpicnum);
        ArtEntry pic = parent.getTile(globalpicnum);

        int tsizx = pic.getWidth();
        int tsizy = pic.getHeight();

        if (!pic.exists()) {
            tsizx = tsizy = 1;
            method = 1;
        }

        GLTile pth = parent.bind(pic, parent.globalpal, parent.globalshade, 0, method);
        if (pth == null) // hires texture not found
        {
            return;
        }

        parent.globalfog.apply();

        int texunits = textureCache.getTextureUnits(), j;
//		float hackscx = 1.0f, hackscy = 1.0f;
//		if (pth != null && pth.isHighTile()) {
//			hackscx = pth.getXScale();
//			hackscy = pth.getYScale();
//		}
//
//		float ox2 = hackscx / pth.getWidth();
//		float oy2 = hackscy / pth.getHeight();

        float ox2;
        float oy2;
        if (pth.isHighTile()) {
            tsizx = pth.getWidth();
            tsizy = pth.getHeight();
        }

        if (GLInfo.texnpot == 0) {
            int xx = 1;
            for (; xx < tsizx; xx += xx) ;
            ox2 = 1.0f / xx;

            int yy = 1;
            for (; yy < tsizy; yy += yy) ;
            oy2 = 1.0f / yy;
        } else {
            ox2 = 1.0f / tsizx;
            oy2 = 1.0f / tsizy;
        }

        if (((method & 3) == 0)) {
            gl.glDisable(GL_BLEND);
            gl.glDisable(GL_ALPHA_TEST);
        } else {
            gl.glEnable(GL_BLEND);
            gl.glEnable(GL_ALPHA_TEST);
        }

        gl.glEnableClientState(GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL_FLOAT, 0, vertices);

        j = 0;
        while (j <= texunits) {
            if (GLInfo.multisample != 0) {
                gl.glActiveTexture(GL_TEXTURE0 + j);
                gl.glClientActiveTexture(GL_TEXTURE0 + j++);
            } else {
                j++;
            }
            gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL_FLOAT, 0, textures);

            gl.glMatrixMode(GL_TEXTURE);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glScalef(tsizx, tsizy, 1.0f);
            gl.glScalef(ox2, oy2, 1.0f);

            if ((dastat & 4) != 0) {
                gl.glScalef(1, -1, 1.0f);
                gl.glTranslatef(0, -1, 0);
            }
        }

        gl.glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        if (GLInfo.multisample != 0) {
            j = 0;
            while (j <= texunits) {
                gl.glMatrixMode(GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glMatrixMode(GL_MODELVIEW);
                gl.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0f);
                gl.glDisable(GL_TEXTURE_2D);

                gl.glClientActiveTexture(GL_TEXTURE0 + j++);
                gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            }
        } else {
            gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        }

        gl.glDisableClientState(GL_VERTEX_ARRAY);

        gl.glMatrixMode(GL_TEXTURE);
        gl.glPopMatrix();

        textureCache.deactivateEffects(gl);
    }

    private void dorotatesprite3d(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat,
                                  int cx1, int cy1, int cx2, int cy2, int uniqid) {
        int xoff = 0, yoff = 0, xsiz, ysiz;
        int ogshade, ogpal;

        int oldviewingrange;
        float x1, y1, z1;
        if (hudsprite == null) {
            hudsprite = new Sprite();
        }
        hudsprite.reset((byte) 0);

        Hudtyp hudInfo = null;
        if (parent.defs == null
                || ((hudInfo = parent.defs.mdInfo.getHudInfo(picnum, dastat)) != null && (hudInfo.flags & 1) != 0)) {
            return; // "HIDE" is specified in DEF
        }

        float ogchang = parent.gchang;
        parent.gchang = 1.0f;
        float ogshang = parent.gshang;
        parent.gshang = 0.0f;
        float d = z / (65536.0f * 16384.0f);
        float ogctang = parent.gctang;
        parent.gctang = EngineUtils.cos(a) * d;
        float ogstang = parent.gstang;
        parent.gstang = EngineUtils.sin(a) * d;
        ogshade = parent.globalshade;
        parent.globalshade = dashade;
        ogpal = parent.globalpal;
        parent.globalpal = dapalnum;
        double ogxyaspect = parent.gxyaspect;
        parent.gxyaspect = 1.0f;
        oldviewingrange = parent.viewingrange;
        parent.viewingrange = 65536;

        x1 = hudInfo.xadd;
        y1 = hudInfo.yadd;
        z1 = hudInfo.zadd;

        if ((hudInfo.flags & 2) == 0) // "NOBOB" is specified in DEF
        {
            float fx = (sx) * (1.0f / 65536.0f);
            float fy = (sy) * (1.0f / 65536.0f);

            if ((dastat & 16) != 0) {
                ArtEntry pic = parent.getTile(picnum);

                xsiz = pic.getWidth();
                ysiz = pic.getHeight();
                xoff = pic.getOffsetX() + (xsiz >> 1);
                yoff = pic.getOffsetY() + (ysiz >> 1);

                d = z / (65536.0f * 16384.0f);
                float cosang, sinang;
                float cosang2 = cosang = EngineUtils.cos(a) * d;
                float sinang2 = sinang = EngineUtils.sin(a) * d;
                if ((dastat & 2) != 0 || ((dastat & 8) == 0)) // Don't aspect unscaled perms
                {
                    d = parent.xyaspect / 65536.0f;
                    cosang2 *= d;
                    sinang2 *= d;
                }
                fx += -(double) xoff * cosang2 + (double) yoff * sinang2;
                fy += -(double) xoff * sinang - (double) yoff * cosang;
            }

            if ((dastat & 2) == 0) {
                x1 += fx / ((double) (xdim << 15)) - 1.0; // -1: left of screen, +1: right of screen
                y1 += fy / ((double) (ydim << 15)) - 1.0; // -1: top of screen, +1: bottom of screen
            } else {
                x1 += fx / 160.0 - 1.0; // -1: left of screen, +1: right of screen
                y1 += fy / 100.0 - 1.0; // -1: top of screen, +1: bottom of screen
            }
        }

        if ((dastat & 4) != 0) {
            x1 = -x1;
            y1 = -y1;
        }

        hudsprite.setAng((short) (hudInfo.angadd + parent.globalang));
        hudsprite.setXrepeat(32);
        hudsprite.setYrepeat(32);

        float cos = parent.gcosang * 16.0f;
        float sin = parent.gsinang * 16.0f;

        hudsprite.setX((int) ((cos * z1 - sin * x1) * 1024.0f + parent.globalposx));
        hudsprite.setY((int) ((sin * z1 + cos * x1) * 1024.0f + parent.globalposy));
        hudsprite.setZ((int) (parent.globalposz + y1 * 16384.0f * 0.8f));

        hudsprite.setPicnum((short) picnum);
        hudsprite.setShade((byte) dashade);
        hudsprite.setPal((short) dapalnum);
        hudsprite.setOwner((short) (uniqid + boardService.getSpriteCount()));
        hudsprite.setCstat((short) ((dastat & 1) + ((dastat & 32) << 4) + ((dastat & 4) << 1)));

        if ((dastat & 10) == 2) {
            parent.glViewport(parent.windowx1, ydim - (parent.windowy2 + 1), parent.windowx2 - parent.windowx1 + 1, parent.windowy2 - parent.windowy1 + 1);
        } else {
            parent.glViewport(0, 0, xdim, ydim);
            parent.glox1 = -1; // Force fullscreen (glox1=-1 forces it to restore)
        }

        gl.glMatrixMode(GL_PROJECTION);

        float f = 1.0f;
        if (hudInfo.fov != -1) {
            f = (float) (1.0 / Math.tan((hudInfo.fov * 2.56) * ((0.5 * Math.PI) * (1.0 / 2048.0))));
        }

        if ((dastat & 10) == 2) {
            float ratioratio = (float) xdim / ydim;

            parent.matrix[0][0] = f * parent.ydimen * (ratioratio >= 1.6f ? 1.2f : 1);
            parent.matrix[0][2] = 1.0f;
            parent.matrix[1][1] = f * parent.xdimen;
            parent.matrix[1][2] = 1.0f;
            parent.matrix[2][2] = 1.0f;
            parent.matrix[2][3] = parent.ydimen * (ratioratio >= 1.6f ? 1.2f : 1);
            parent.matrix[3][2] = -1.0f;
        } else {
            parent.matrix[0][0] = parent.matrix[2][3] = 1.0f;
            parent.matrix[0][0] *= f;
            parent.matrix[1][1] = (float) xdim / ydim;
            parent.matrix[1][1] *= f;
            parent.matrix[2][2] = 1.0001f;
            parent.matrix[3][2] = 1 - parent.matrix[2][2];
        }
        gl.glLoadMatrixf(parent.matrix);
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();

        if ((hudInfo.flags & 8) != 0) // NODEPTH flag
        {
            gl.glDisable(GL_DEPTH_TEST);
        } else {
            gl.glEnable(GL_DEPTH_TEST);
            gl.glClear(GL_DEPTH_BUFFER_BIT);
        }

        parent.globalorientation = hudsprite.getCstat();
        parent.mdrenderer.mddraw(parent.modelManager.getModel(picnum, dapalnum), hudsprite, 0, 0);

        parent.viewingrange = oldviewingrange;
        parent.gxyaspect = ogxyaspect;
        parent.globalshade = ogshade;
        parent.globalpal = ogpal;
        parent.gchang = ogchang;
        parent.gshang = ogshang;
        parent.gctang = ogctang;
        parent.gstang = ogstang;
    }

    @Override
    public void init() {
        this.gl = parent.gl;
    }

    @Override
    public void uninit() {
    }

    @Override
    public void nextpage() {
    }

    static class raster {
        float x, y, xi;
        int i;

        public void set(raster src) {
            this.x = src.x;
            this.y = src.y;
            this.xi = src.xi;
            this.i = src.i;
        }
    }
}
