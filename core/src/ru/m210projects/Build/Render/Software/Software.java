/*
 * Software renderer code originally written by Ken Silverman
 * Ken Silverman's official web site: "http://www.advsys.net/ken"
 * See the included license file "BUILDLIC.TXT" for license info.
 *
 * This file has been modified from Ken Silverman's original release
 * by Jonathon Fowler (jf@jonof.id.au)
 * by the EDuke32 team (development@voidpoint.com)
 * by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Render.Software;

import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.AbstractRenderer;
import ru.m210projects.Build.Render.DefaultMapSettings;
import ru.m210projects.Build.Render.ModelHandle.Voxel.VoxelData;
import ru.m210projects.Build.Render.ModelHandle.VoxelInfo;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.ScreenFade;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Render.Types.Tile2model;
import ru.m210projects.Build.Render.listeners.PaletteListener;
import ru.m210projects.Build.Render.listeners.PrecacheListener;
import ru.m210projects.Build.Render.listeners.TileListener;
import ru.m210projects.Build.Render.listeners.WorldListener;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Tables;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.settings.VideoContext;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;

public class Software extends AbstractRenderer implements PaletteListener, TileListener, PrecacheListener {

    public static final int MAXXDIM = 4096;
    public static final int MAXYDIM = 3072;
    public static final int MAXVOXMIPS = 5;
    public final int BITSOFPRECISION = 3;
    protected final int MAXPERMS = 512;
    protected final int MAXWALLSB = ((MAXWALLS >> 2) + (MAXWALLS >> 3));
    protected final int MAXYSAVES = ((MAXXDIM * MAXSPRITES) >> 7); // FIXME
    private final int[] spritesx = new int[MAXSPRITESONSCREEN];
    private final int[] spritesy = new int[MAXSPRITESONSCREEN + 1];
    private final int[] spritesz = new int[MAXSPRITESONSCREEN];
    private final Sprite[] tspriteptr = new Sprite[MAXSPRITESONSCREEN + 1];
    private final AtomicInteger[] cz = new AtomicInteger[5];
    private final AtomicInteger[] fz = new AtomicInteger[5];
    private final int MAXXSIZ = 256;
    private final int[] ggxinc = new int[MAXXSIZ + 1];
    private final int[] ggyinc = new int[MAXXSIZ + 1];
    public int bytesperline, frameoffset;
    public short[] umost = new short[MAXXDIM], dmost = new short[MAXXDIM];
    public short[] uplc = new short[MAXXDIM], dplc = new short[MAXXDIM];
    public short[] startumost = new short[MAXXDIM], startdmost = new short[MAXXDIM];
    public int[] ylookup = new int[MAXYDIM + 1];
    public int[] lookups;
    public byte[][] bakframeplace = new byte[4][];
    public short[] bakxsiz = new short[4], bakysiz = new short[4];
    public int globaluclip, globaldclip;
    public int globalpisibility, globalhisibility, globalcisibility;
    public int globparaceilclip, globparaflorclip;
    public int globalorientation, globvis, globalyscale;
    public int globalxpanning, globalypanning;
    public int globalx1, globaly1, globalx2, globaly2;
    public int globalx;
    public int globaly;
    public int globalx3, globaly3;
    public int globalzd, globalzx, globalz;
    public byte[] globalbufplc;
    public int globalpalwritten;
    public int numscans, numhits, numbunches;
    public int[] xb1 = new int[MAXWALLSB], yb1 = new int[MAXWALLSB], xb2 = new int[MAXWALLSB], yb2 = new int[MAXWALLSB];
    public int[] rx1 = new int[MAXWALLSB], ry1 = new int[MAXWALLSB], rx2 = new int[MAXWALLSB], ry2 = new int[MAXWALLSB];
    public int[] rxi = new int[8], ryi = new int[8], rzi = new int[8], rxi2 = new int[8], ryi2 = new int[8],
            rzi2 = new int[8];
    public int[] xsi = new int[8], ysi = new int[8];
    public int horizlookup2;
    public int horizycent;
    public int[] p2 = new int[MAXWALLSB], thesector = new int[MAXWALLSB], thewall = new int[MAXWALLSB];
    public int[] bunchfirst = new int[MAXWALLSB], bunchlast = new int[MAXWALLSB];
    public short[] radarang2 = new short[MAXXDIM];
    public short[] uwall = new short[MAXXDIM], dwall = new short[MAXXDIM];
    public int[] swall = new int[MAXXDIM], lwall = new int[MAXXDIM + 4];
    public int[] swplc = new int[MAXXDIM], lplc = new int[MAXXDIM];
    public int[] lastx = new int[MAXYDIM];
    public int[] slopalookup = new int[16384]; // was 2048
    public int xdimenrecip;
    public int[] smostwall = new int[MAXWALLSB];
    public int smostwallcnt = -1;
    public int smostcnt;
    public short[] smost = new short[MAXYSAVES];
    public int[] smoststart = new int[MAXWALLSB];
    public byte[] smostwalltype = new byte[MAXWALLSB];
    public int[] maskwall = new int[MAXWALLSB];
    public int maskwallcnt;
    public int[] sectorborder = new int[256];
    public short sectorbordercnt;
    public int mirrorsx1, mirrorsy1, mirrorsx2, mirrorsy2;
    public byte[] tempbuf = new byte[MAXWALLS];
    public boolean novoxmips = false;
    public boolean isInited = false;
    protected int halfxdimen;
    protected int viewingrangerecip;
    protected byte[] temppal = new byte[768];
    protected A a;
    protected DefScript defs;
    protected int guniqhudid;
    protected float fovFactor = 1.0f;
    protected PermFifo[] permfifo = new PermFifo[MAXPERMS];
    protected int permhead = 0, permtail = 0;
    protected short globalpicnum, globalshiftval;
    protected byte globalxshift;
    protected byte globalyshift;
    protected int oxyaspect, oxdimen, oviewingrange;
    protected int[] lowrecip = new int[1024];
    protected int nytooclose;
    protected int nytoofar;
    protected int[] distrecip = new int[65536];
    protected SoftwareOrpho ortho;
    protected byte[][] textureCache;
    private final AtomicInteger floorz = new AtomicInteger();
    private final AtomicInteger ceilz = new AtomicInteger();
    private ByteBuffer indexbuffer;
    private ByteBuffer rgbbuffer;
    private ChangeListener changeListener;

    public Software(GameConfig config) {
        super(config);
        this.config.setVideoContext(new VideoContext() {
            @Override
            public void setgFov(int gFov) {
                super.setgFov(gFov);
                setFieldOfView(gFov);
            }
        });
        this.textureCache = new byte[MAXTILES][];
        for (int i = 0; i < 5; i++) {
            cz[i] = new AtomicInteger();
            fz[i] = new AtomicInteger();
        }
        this.a = new Ac();
        for (int i = 1; i < 1024; i++) {
            lowrecip[i] = ((1 << 24) - 1) / i;
        }
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public static long toUnsignedLong(int x) {
        return (x) & 0xffffffffL;
    }

    @Override
    public void init(Engine engine) {
        super.init(engine);
        this.paletteManager.setListener(this);
        this.tileManager.setTileListener(this);
        changepalette(paletteManager.getCurrentPalette().getBytes());

        try {
            a.fixtransluscence(paletteManager.getTranslucBuffer());
            a.setpalookupaddress(getPalookupBuffer(globalpalwritten));

            this.ortho = allocOrphoRenderer(engine);
            this.setDefs(engine.getDefs());

            Console.out.println("Software renderer is initialized", OsdColor.GREEN);
            isInited = true;
        } catch (Throwable t) {
            isInited = false;
        }
    }

    protected void updateFov() {
        config.setgFov(config.getgFov());
    }

    protected boolean isWidescreen() {
        return config.getWidescreen() == 1;
    }

    protected boolean isUseVoxels() {
        return config.isUseVoxels();
    }

    @Override
    public void resize(int width, int height) {
        if ((width == xdim) && (height == ydim)) {
            return;
        }

        xdim = width;
        ydim = height;

        updateFov();
        setview(0, 0, xdim - 1, ydim - 1);

        bytesperline = width;
        int j = height * 4 * 4;
        lookups = new int[j << 1];
        horizlookup2 = j;
        horizycent = ((height * 4) >> 1);

        // Force drawrooms to call dosetaspect & recalculate stuff
        oxyaspect = oxdimen = oviewingrange = -1;
        globalpalwritten = 0;
        j = 0;
        for (int i = 0; i <= height; i++) {
            ylookup[i] = j;
            j += bytesperline;
        }
        updateview();
        a.setframeplace(new byte[width * height]);
        a.setvlinebpl(bytesperline);
    }

    protected A getA() {
        return a;
    }

    @Override
    public boolean isInited() {
        return isInited;
    }

    public void updateview() {
        xdimenrecip = divscale(1, xdimen, 32);

        for (int i = 0; i < windowx1; i++) {
            startumost[i] = 1;
            startdmost[i] = 0;
        }
        for (int i = windowx1; i <= windowx2; i++) {
            startumost[i] = (short) windowy1;
            startdmost[i] = (short) (windowy2 + 1);
        }
        for (int i = windowx2 + 1; i < xdim; i++) {
            startumost[i] = 1;
            startdmost[i] = 0;
        }
    }

    @Override
    public void setview(int x1, int y1, int x2, int y2) {
        xdimen = (x2 - x1) + 1;
        halfxdimen = (xdimen >> 1);
        ydimen = (y2 - y1) + 1;

        super.setview(x1, y1, x2, y2);
        updateview();
        ortho.resize(xdim, ydim);
    }

    @Override
    public void setFieldOfView(int fovDegrees) {
        this.fovFactor = (float) Math.tan(fovDegrees * Math.PI / 360.0);
        setaspect();
    }

    protected void setaspect(int daxrange, int daaspect) {
        viewingrange = offscreenrendering ? daxrange : (int) (daxrange * fovFactor);
        viewingrangerecip = divscale(1, viewingrange, 32);

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

        if (isWidescreen() && (4 * xdim / 5) != ydim) {
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
    public void setviewtotile(DynamicArtEntry pic) {
        // DRAWROOMS TO TILE BACKUP&SET CODE
        int xsiz = pic.getWidth();
        int ysiz = pic.getHeight();

        bakxsiz[setviewcnt] = (short) xsiz;
        bakysiz[setviewcnt] = (short) ysiz;
        bakframeplace[setviewcnt] = a.getframeplace();
        a.setframeplace(pic.getBytes());

        bakwindowx1[setviewcnt] = windowx1;
        bakwindowy1[setviewcnt] = windowy1;
        bakwindowx2[setviewcnt] = windowx2;
        bakwindowy2[setviewcnt] = windowy2;
        setviewcnt++;

        setview(0, 0, ysiz - 1, xsiz - 1);
        setaspect(65536, 65536);

        int j = 0;
        for (int i = 0; i <= xsiz; i++) {
            ylookup[i] = j;
            j += ysiz;
        }
        a.setvlinebpl(ysiz);
    }

    public void setviewback() {
        if (setviewcnt <= 0) {
            offscreenrendering = false;
            setaspect();
            return;
        }

        setviewcnt--;
        offscreenrendering = (setviewcnt > 0);
        setview(bakwindowx1[setviewcnt], bakwindowy1[setviewcnt], bakwindowx2[setviewcnt], bakwindowy2[setviewcnt]);
        a.setframeplace(bakframeplace[setviewcnt]);

        int k;
        if (setviewcnt == 0) {
            k = bakxsiz[0];
        } else {
            k = Math.max(bakxsiz[setviewcnt - 1], bakxsiz[setviewcnt]);
        }

        int j = 0;
        for (int i = 0; i <= k; i++) {
            ylookup[i] = j;
            j += bytesperline;
        }
        a.setvlinebpl(bytesperline);
    }

    @Override
    public void uninit() {
        isInited = false;
        paletteManager.setListener(PaletteListener.DUMMY_PALETTE_CHANGE_LISTENER);
        boardService.setListener(WorldListener.DUMMY_LISTENER);
        tileManager.setTileListener(TileListener.DUMMY_LISTENER);
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
        int spritesortcnt = Math.min(tspriteptr.length, tSpriteList.getSize());

        for (i = spritesortcnt - 1; i >= 0; i--) {
            tspriteptr[i] = tSpriteList.get(i);
            if (tspriteptr[i].getPicnum() < 0 || tspriteptr[i].getPicnum() > MAXTILES) {
                continue;
            }

            if (tspriteptr[i].getOwner() == 153) {
                tspriteptr[i].setPal(1);
            }

            xs = tspriteptr[i].getX() - globalposx;
            ys = tspriteptr[i].getY() - globalposy;
            yp = dmulscale(xs, cosviewingrangeglobalang, ys, sinviewingrangeglobalang, 6);

            if (yp > (4 << 8)) {
                xp = dmulscale(ys, cosglobalang, -xs, singlobalang, 6);
                spritesx[i] = scale(xp + yp, (long) xdimen << 7, yp);
            } else if ((tspriteptr[i].getCstat() & 48) == 0) {
                spritesortcnt--; // Delete face sprite if on wrong side!
                if (i != spritesortcnt) {
                    tspriteptr[i] = tspriteptr[spritesortcnt];
                    spritesx[i] = spritesx[spritesortcnt];
                    spritesy[i] = spritesy[spritesortcnt];
                }
                continue;
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
                        yoff = (byte) (pic.getOffsetY() + (tspriteptr[k].getYoffset()));
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
                    }
                }
            }
            i = j;
        }

        while ((spritesortcnt > 0) && (maskwallcnt > 0)) // While BOTH > 0
        {
            j = maskwall[maskwallcnt - 1];
            if (!spritewallfront(tspriteptr[spritesortcnt - 1], thewall[j])) {
                drawsprite(--spritesortcnt);
            } else {
                // Check to see if any sprites behind the masked wall...
                k = -1;
                gap = 0;
                for (i = spritesortcnt - 2; i >= 0; i--) {
                    if ((xb1[j] <= (spritesx[i] >> 8)) && ((spritesx[i] >> 8) <= xb2[j])) {
                        if (!spritewallfront(tspriteptr[i], thewall[j])) {
                            drawsprite(i);
                            tspriteptr[i] = null;
                            k = i;
                            gap++;
                        }
                    }
                }
                if (k >= 0) // remove holes in sprite list
                {
                    for (i = k; i < spritesortcnt; i++) {
                        if (tspriteptr[i] != null && tspriteptr[i].getOwner() >= 0) {
                            if (i > k) {
                                tspriteptr[k] = tspriteptr[i];
                                spritesx[k] = spritesx[i];
                                spritesy[k] = spritesy[i];
                                tspriteptr[i] = null;
                            }
                            k++;
                        }
                    }
                    spritesortcnt -= gap;
                }

                // finally safe to draw the masked wall
                drawmaskwall(--maskwallcnt);
            }
        }

        while (spritesortcnt != 0) {
            spritesortcnt--;
            if (tspriteptr[spritesortcnt] != null) {
                drawsprite(spritesortcnt);
            }
        }
        while (maskwallcnt > 0) {
            drawmaskwall(--maskwallcnt);
        }
    }

    @Override
    public void drawrooms() {
        globalhoriz = (globalhoriz * xdimenscale / viewingrange) + (ydimen >> 1);

        globaluclip = (-(int) globalhoriz) * xdimscale;
        globaldclip = (ydimen - (int) globalhoriz) * xdimscale;

        int i = mulscale(xdimenscale, viewingrangerecip, 16);
        globalpisibility = mulscale(parallaxvisibility, i, 16);
        globalvisibility = mulscale(visibility, i, 16);

        globalhisibility = mulscale(globalvisibility, xyaspect, 16);
        globalcisibility = mulscale(globalhisibility, 320, 8);

        if ((xyaspect != oxyaspect) || (xdimen != oxdimen) || (viewingrange != oviewingrange)) {
            dosetaspect();
        }

        i = xdimen - 1;
        do {
            umost[i] = (short) (startumost[windowx1 + i] - windowy1);
            dmost[i] = (short) (startdmost[windowx1 + i] - windowy1);
            i--;
        } while (i != 0);
        umost[0] = (short) (startumost[windowx1] - windowy1);
        dmost[0] = (short) (startdmost[windowx1] - windowy1);

        frameoffset = windowy1 * bytesperline + windowx1;

        numhits = xdimen;
        numscans = 0;
        numbunches = 0;
        maskwallcnt = 0;
        smostwallcnt = 0;
        smostcnt = 0;

        if (globalcursectnum >= boardService.getSectorCount()) {
            globalcursectnum -= boardService.getSectorCount();
        } else {
            i = globalcursectnum;
            globalcursectnum = engine.updatesector(globalposx, globalposy, globalcursectnum);
            if (globalcursectnum < 0) {
                globalcursectnum = i;
            }
        }

        globparaceilclip = 1;
        globparaflorclip = 1;
        engine.getzsofslope(globalcursectnum, globalposx, globalposy, floorz, ceilz);

        if (globalposz < ceilz.get()) {
            globparaceilclip = 0;
        }
        if (globalposz > floorz.get()) {
            globparaflorclip = 0;
        }

        scansector(globalcursectnum);

        if (inpreparemirror) {
            inpreparemirror = false;
            mirrorsx1 = xdimen - 1;
            mirrorsx2 = 0;
            for (i = numscans - 1; i >= 0; i--) {
                if (boardService.getWall(thewall[i]).getNextsector() < 0) {
                    continue;
                }
                if (xb1[i] < mirrorsx1) {
                    mirrorsx1 = xb1[i];
                }
                if (xb2[i] > mirrorsx2) {
                    mirrorsx2 = xb2[i];
                }
            }

            for (i = 0; i < mirrorsx1; i++) {
                if (umost[i] <= dmost[i]) {
                    umost[i] = 1;
                    dmost[i] = 0;
                    numhits--;
                }
            }
            for (i = mirrorsx2 + 1; i < xdimen; i++) {
                if (umost[i] <= dmost[i]) {
                    umost[i] = 1;
                    dmost[i] = 0;
                    numhits--;
                }
            }

            drawalls(0);
            numbunches--;

            mirrorsy1 = Math.min(umost[mirrorsx1], umost[mirrorsx2]);
            mirrorsy2 = Math.max(dmost[mirrorsx1], dmost[mirrorsx2]);

            if (numbunches < 0) {
                return;
            }
            bunchfirst[0] = bunchfirst[numbunches];
            bunchlast[0] = bunchlast[numbunches];
        }

        while ((numbunches > 0) && (numhits > 0)) {
            Arrays.fill(tempbuf, 0, numbunches + 3, (byte) 0);
            tempbuf[0] = 1;

            int closest = 0, j; // Almost works, but not quite :(
            for (i = 1; i < numbunches; ++i) {
                j = bunchfront(i, closest);
                if (j < 0) {
                    continue;
                }
                tempbuf[i] = 1;
                if (j == 0) {
                    tempbuf[closest] = 1;
                    closest = i;
                }
            }

            for (i = 0; i < numbunches; ++i) // Double-check
            {
                if (tempbuf[i] != 0) {
                    continue;
                }
                j = bunchfront(i, closest);
                if (j < 0) {
                    continue;
                }
                tempbuf[i] = 1;
                if (j == 0) {
                    tempbuf[closest] = 1;
                    closest = i;
                    i = 0;
                }
            }

            drawalls(closest);

            if (automapping != 0) {
                for (int z = bunchfirst[closest]; z >= 0; z = p2[z]) {
                    show2dwall.setBit(thewall[z]);
                }
            }

            numbunches--;
            bunchfirst[closest] = bunchfirst[numbunches];
            bunchlast[closest] = bunchlast[numbunches];
        }
    }

    @Override
    public void completemirror() {
        // Can't reverse with uninitialized data
        if (inpreparemirror) {
            inpreparemirror = false;
            return;
        }
        if (mirrorsx1 > 0) {
            mirrorsx1--;
        }
        if (mirrorsx2 < windowx2 - windowx1 - 1) {
            mirrorsx2++;
        }
        if (mirrorsx2 < mirrorsx1) {
            return;
        }

        int p = ylookup[windowy1 + mirrorsy1] + windowx1 + mirrorsx1;
        int i = windowx2 - windowx1 - mirrorsx2 - mirrorsx1;
        mirrorsx2 -= mirrorsx1;
        byte[] frameplace = a.getframeplace();
        for (int dy = mirrorsy2 - mirrorsy1; dy >= 0; dy--) {
            if (mirrorsx2 + 1 + p + 1 >= frameplace.length) {
                return;
            }

            System.arraycopy(frameplace, p + 1, tempbuf, 0, mirrorsx2 + 1);
            tempbuf[mirrorsx2] = tempbuf[mirrorsx2 - 1];
            copybufreverse(tempbuf, mirrorsx2, frameplace, p + i, mirrorsx2 + 1);
            p += ylookup[1];
        }
    }

    private void drawalls(int bunch) {
        int z = bunchfirst[bunch];
        int sectnum = thesector[z];
        Sector sec = boardService.getSector(sectnum);

        int andwstat1 = 0xff;
        int andwstat2 = 0xff;
        for (; z >= 0; z = p2[z]) // uplc/dplc calculation
        {
            andwstat1 &= wallmost(uplc, z, sectnum, 0);
            andwstat2 &= wallmost(dplc, z, sectnum, 1);
        }

        if ((andwstat1 & 3) != 3) // draw ceilings
        {
            if ((sec.getCeilingstat() & 3) == 2) {
                grouscan(xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], sectnum, 0);
            } else if ((sec.getCeilingstat() & 1) == 0) {
                ceilscan(xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], sectnum);
            } else {
                parascan(/*xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], */0, bunch);
            }
        }
        if ((andwstat2 & 12) != 12) // draw floors
        {
            if ((sec.getFloorstat() & 3) == 2) // slopes
            {
                grouscan(xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], sectnum, 1);
            } else if ((sec.getFloorstat() & 1) == 0) // solid
            {
                florscan(xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], sectnum);
            } else // background
            {
                parascan(/*xb1[bunchfirst[bunch]], xb2[bunchlast[bunch]], */1, bunch);
            }
        }

        // DRAW WALLS SECTION!
        for (z = bunchfirst[bunch]; z >= 0; z = p2[z]) {
            int x1 = xb1[z];
            int x2 = xb2[z], x;
            if (umost[x2] >= dmost[x2]) {
                for (x = x1; x < x2; x++) {
                    if (umost[x] < dmost[x]) {
                        break;
                    }
                }
                if (x >= x2) {
                    smostwall[smostwallcnt] = z;
                    smostwalltype[smostwallcnt] = 0;
                    smostwallcnt++;
                    continue;
                }
            }

            int wallnum = thewall[z];
            Wall wal = boardService.getWall(wallnum);
            short nextsectnum = wal.getNextsector();
            Sector nextsec = null;

            int gotswall = 0;

            int startsmostwallcnt = smostwallcnt;
            int startsmostcnt = smostcnt;

            if (nextsectnum >= 0) {
                nextsec = boardService.getSector(nextsectnum);
                engine.getzsofslope(sectnum, wal.getX(), wal.getY(), fz[0], cz[0]);
                engine.getzsofslope(sectnum, boardService.getWall(wal.getPoint2()).getX(), boardService.getWall(wal.getPoint2()).getY(), fz[1], cz[1]);
                engine.getzsofslope(nextsectnum, wal.getX(), wal.getY(), fz[2], cz[2]);
                engine.getzsofslope(nextsectnum, boardService.getWall(wal.getPoint2()).getX(), boardService.getWall(wal.getPoint2()).getY(), fz[3], cz[3]);
                engine.getzsofslope(nextsectnum, globalposx, globalposy, fz[4], cz[4]);

                if ((wal.getCstat() & 48) == 16) {
                    maskwall[maskwallcnt++] = z;
                }

                if (((sec.getCeilingstat() & 1) == 0) || ((nextsec.getCeilingstat() & 1) == 0)) {
                    if ((cz[2].get() <= cz[0].get()) && (cz[3].get() <= cz[1].get())) {
                        if (globparaceilclip != 0) {
                            for (x = x1; x <= x2; x++) {
                                if (uplc[x] > umost[x]) {
                                    if (umost[x] <= dmost[x]) {
                                        umost[x] = uplc[x];
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        wallmost(dwall, z, nextsectnum, (char) 0);
                        if ((cz[2].get() > fz[0].get()) || (cz[3].get() > fz[1].get())) {
                            for (int i = x1; i <= x2; i++) {
                                if (dwall[i] > dplc[i]) {
                                    dwall[i] = dplc[i];
                                }
                            }
                        }

                        globalorientation = wal.getCstat();
                        globalpicnum = wal.getPicnum();
                        if (globalpicnum >= MAXTILES) {
                            globalpicnum = 0;
                        }
                        ArtEntry pic = getTile(globalpicnum);
                        globalxpanning = wal.getXpanning() & 0xFF;
                        globalypanning = wal.getYpanning() & 0xFF;
                        globalshiftval = (short) pic.getSizey();

                        if (pow2long[globalshiftval] != pic.getHeight()) {
                            globalshiftval++;
                        }
                        globalshiftval = (short) (32 - globalshiftval);
                        if (pic.getType() != AnimType.NONE) {
                            globalpicnum += (short) animateoffs(globalpicnum, wallnum + 16384);
                        }
                        globalshade = wal.getShade();
                        globvis = globalvisibility;
                        if (sec.getVisibility() != 0) {
                            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                        }
                        globalpal = wal.getPal();
                        if (!engine.getPaletteManager().isValidPalette(globalpal)) {
                            globalpal = 0; // JBF: fixes crash
                        }
                        globalyscale = (wal.getYrepeat() << (globalshiftval - 19));
                        if ((globalorientation & 4) == 0) {
                            globalzd = (((globalposz - nextsec.getCeilingz()) * globalyscale) << 8);
                        } else {
                            globalzd = (((globalposz - sec.getCeilingz()) * globalyscale) << 8);
                        }
                        globalzd += (globalypanning << 24);
                        if ((globalorientation & 256) != 0) {
                            globalyscale = -globalyscale;
                            globalzd = -globalzd;
                        }

                        //if (gotswall == 0) { always true
                            gotswall = 1;
                            prepwall(z, wal);
                        //}
                        wallscan(x1, x2, uplc, dwall, swall, lwall);

                        if ((cz[2].get() >= cz[0].get()) && (cz[3].get() >= cz[1].get())) {
                            for (x = x1; x <= x2; x++) {
                                if (dwall[x] > umost[x]) {
                                    if (umost[x] <= dmost[x]) {
                                        umost[x] = dwall[x];
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (x = x1; x <= x2; x++) {
                                if (umost[x] <= dmost[x]) {
                                    int i = Math.max(uplc[x], dwall[x]);
                                    if (i > umost[x]) {
                                        umost[x] = (short) i;
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if ((cz[2].get() < cz[0].get()) || (cz[3].get() < cz[1].get()) || (globalposz < cz[4].get())) {
                        int i = x2 - x1 + 1;
                        if (smostcnt + i < MAXYSAVES) {
                            smoststart[smostwallcnt] = smostcnt;
                            smostwall[smostwallcnt] = z;
                            smostwalltype[smostwallcnt] = 1; // 1 for umost
                            smostwallcnt++;
                            System.arraycopy(umost, x1, smost, smostcnt, i);
                            smostcnt += i;
                        }
                    }
                }
                if (((sec.getFloorstat() & 1) == 0) || ((nextsec.getFloorstat() & 1) == 0)) {
                    if ((fz[2].get() >= fz[0].get()) && (fz[3].get() >= fz[1].get())) {
                        if (globparaflorclip != 0) {
                            for (x = x1; x <= x2; x++) {
                                if (dplc[x] < dmost[x]) {
                                    if (umost[x] <= dmost[x]) {
                                        dmost[x] = dplc[x];
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        wallmost(uwall, z, nextsectnum, (char) 1);
                        if ((fz[2].get() < cz[0].get()) || (fz[3].get() < cz[1].get())) {
                            for (int i = x1; i <= x2; i++) {
                                if (uwall[i] < uplc[i]) {
                                    uwall[i] = uplc[i];
                                }
                            }
                        }

                        ArtEntry art;
                        if ((wal.getCstat() & 2) > 0) {
                            wallnum = wal.getNextwall();
                            wal = boardService.getWall(wallnum);
                            globalorientation = wal.getCstat();
                            globalpicnum = wal.getPicnum();
                            if (globalpicnum >= MAXTILES) {
                                globalpicnum = 0;
                            }
                            art = getTile(globalpicnum);
                            globalxpanning = wal.getXpanning() & 0xFF;
                            globalypanning = wal.getYpanning() & 0xFF;
                            if (art.getType() != AnimType.NONE) {
                                globalpicnum += (short) animateoffs(globalpicnum, wallnum + 16384);
                            }
                            globalshade = wal.getShade();
                            globalpal = wal.getPal();
                            wallnum = thewall[z];
                            wal = boardService.getWall(wallnum);
                        } else {
                            globalorientation = wal.getCstat();
                            globalpicnum = wal.getPicnum();
                            if (globalpicnum >= MAXTILES) {
                                globalpicnum = 0;
                            }
                            art = getTile(globalpicnum);
                            globalxpanning = wal.getXpanning() & 0xFF;
                            globalypanning = wal.getYpanning() & 0xFF;
                            if (art.getType() != AnimType.NONE) {
                                globalpicnum += (short) animateoffs(globalpicnum, wallnum + 16384);
                            }
                            globalshade = wal.getShade();
                            globalpal = wal.getPal();
                        }
                        if (!engine.getPaletteManager().isValidPalette(globalpal)) {
                            globalpal = 0; // JBF: fixes crash
                        }
                        globvis = globalvisibility;
                        if (sec.getVisibility() != 0) {
                            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                        }
                        globalshiftval = (short) (art.getSizey());
                        if (pow2long[globalshiftval] != art.getHeight()) {
                            globalshiftval++;
                        }
                        globalshiftval = (short) (32 - globalshiftval);
                        globalyscale = (wal.getYrepeat() << (globalshiftval - 19));
                        if ((globalorientation & 4) == 0) {
                            globalzd = (((globalposz - nextsec.getFloorz()) * globalyscale) << 8);
                        } else {
                            globalzd = (((globalposz - sec.getCeilingz()) * globalyscale) << 8);
                        }
                        globalzd += (globalypanning << 24);
                        if ((globalorientation & 256) != 0) {
                            globalyscale = -globalyscale;
                            globalzd = -globalzd;
                        }

                        if (gotswall == 0) {
                            gotswall = 1;
                            prepwall(z, wal);
                        }

                        wallscan(x1, x2, uwall, dplc, swall, lwall);

                        if ((fz[2].get() <= fz[0].get()) && (fz[3].get() <= fz[1].get())) {
                            for (x = x1; x <= x2; x++) {
                                if (uwall[x] < dmost[x]) {
                                    if (umost[x] <= dmost[x]) {
                                        dmost[x] = uwall[x];
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (x = x1; x <= x2; x++) {
                                if (umost[x] <= dmost[x]) {
                                    int i = Math.min(dplc[x], uwall[x]);
                                    if (i < dmost[x]) {
                                        dmost[x] = (short) i;
                                        if (umost[x] > dmost[x]) {
                                            numhits--;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if ((fz[2].get() > fz[0].get()) || (fz[3].get() > fz[1].get()) || (globalposz > fz[4].get())) {
                        int i = x2 - x1 + 1;
                        if (smostcnt + i < MAXYSAVES) {
                            smoststart[smostwallcnt] = smostcnt;
                            smostwall[smostwallcnt] = z;
                            smostwalltype[smostwallcnt] = 2; // 2 for dmost
                            smostwallcnt++;
                            System.arraycopy(dmost, x1, smost, smostcnt, i);
                            smostcnt += i;
                        }
                    }
                }
                if (numhits < 0) {
                    return;
                }
                if (((wal.getCstat() & 32) == 0) && ((gotsector[nextsectnum >> 3] & pow2char[nextsectnum & 7]) == 0)) {
                    if (umost[x2] < dmost[x2]) {
                        scansector(nextsectnum);
                    } else {
                        for (x = x1; x < x2; x++) {
                            if (umost[x] < dmost[x]) {
                                scansector(nextsectnum);
                                break;
                            }
                        }

                        // If can't see sector beyond, then cancel smost array and just
                        // store wall!
                        if (x == x2) {
                            smostwallcnt = startsmostwallcnt;
                            smostcnt = startsmostcnt;
                            smostwall[smostwallcnt] = z;
                            smostwalltype[smostwallcnt] = 0;
                            smostwallcnt++;
                        }
                    }
                }
            }
            if ((nextsectnum < 0) || (wal.getCstat() & 32) != 0) // White/1-way wall
            {
                globalorientation = wal.getCstat();
                if (nextsectnum < 0) {
                    globalpicnum = wal.getPicnum();
                } else {
                    globalpicnum = wal.getOverpicnum();
                }
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }
                globalxpanning = wal.getXpanning() & 0xFF;
                globalypanning = wal.getYpanning() & 0xFF;
                ArtEntry pic = getTile(globalpicnum);

                if (pic.getType() != AnimType.NONE) {
                    globalpicnum += (short) animateoffs(globalpicnum, wallnum + 16384);
                    pic = getTile(globalpicnum);
                }
                globalshade = wal.getShade();
                globvis = globalvisibility;
                if (sec.getVisibility() != 0) {
                    globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                }
                globalpal = wal.getPal();
                if (!engine.getPaletteManager().isValidPalette(globalpal)) {
                    globalpal = 0; // JBF: fixes crash
                }
                globalshiftval = (short) (pic.getSizey());
                if (pow2long[globalshiftval] != pic.getHeight()) {
                    globalshiftval++;
                }
                globalshiftval = (short) (32 - globalshiftval);
                globalyscale = (wal.getYrepeat() << (globalshiftval - 19));
                if (nextsectnum >= 0) {
                    if ((globalorientation & 4) == 0) {
                        globalzd = globalposz - nextsec.getCeilingz();
                    } else {
                        globalzd = globalposz - sec.getCeilingz();
                    }
                } else {
                    if ((globalorientation & 4) == 0) {
                        globalzd = globalposz - sec.getCeilingz();
                    } else {
                        globalzd = globalposz - sec.getFloorz();
                    }
                }
                globalzd = ((globalzd * globalyscale) << 8) + (globalypanning << 24);
                if ((globalorientation & 256) != 0) {
                    globalyscale = -globalyscale;
                    globalzd = -globalzd;
                }

                if (gotswall == 0) {
                    gotswall = 1;
                    prepwall(z, wal);
                }

                wallscan(x1, x2, uplc, dplc, swall, lwall);

                for (x = x1; x <= x2; x++) {
                    if (umost[x] <= dmost[x]) {
                        umost[x] = 1;
                        dmost[x] = 0;
                        numhits--;
                    }
                }
                smostwall[smostwallcnt] = z;
                smostwalltype[smostwallcnt] = 0;
                smostwallcnt++;
            }
        }
    }

    private void drawsprite(int snum) {
        Sprite tspr = tspriteptr[snum];

        int xb = spritesx[snum];
        int yp = spritesy[snum];

        if (tspr == null || tspr.getOwner() < 0 || tspr.getPicnum() < 0 || tspr.getPicnum() >= MAXTILES || tspr.getSectnum() < 0) {
            return;
        }

        int tilenum = tspr.getPicnum();
        VoxelInfo vtilenum = null;
        short spritenum = tspr.getOwner();
        short cstat = tspr.getCstat();

        ArtEntry pic = getTile(tilenum);
        if (pic.getType() != AnimType.NONE) {
            tilenum += animateoffs(tilenum, spritenum + 32768);
            pic = getTile(tilenum);
        }

        if (!pic.hasSize() || (spritenum < 0)) {
            return;
        }

        if ((tspr.getXrepeat() <= 0) || (tspr.getYrepeat() <= 0)) {
            return;
        }

        if (isUseVoxels()) {
            Tile2model entry = defs != null ? defs.mdInfo.getParams(tilenum) : null;
            if (entry != null && entry.voxel != null) {
                if ((boardService.getSprite(tspr.getOwner()).getCstat() & 48) != 32) {
                    vtilenum = entry.voxel;
                    cstat |= 48;
                }
            }
        }

        short sectnum = tspr.getSectnum();
        Sector sec = boardService.getSector(sectnum);
        globalpal = tspr.getPal();
        if (!engine.getPaletteManager().isValidPalette(globalpal)) {
            globalpal = 0; // JBF: fixes null-pointer crash
        }
        globalshade = tspr.getShade();
        if ((cstat & 2) != 0) {
            if ((cstat & 512) != 0) {
                a.settransreverse();
            } else {
                a.settransnormal();
            }
        }

        int xoff = (byte) (pic.getOffsetX() + (tspr.getXoffset()));
        int yoff = (byte) (pic.getOffsetY() + (tspr.getYoffset()));

        int xv, yv, x1, y1, x2, y2, dax, day, dax1, dax2, dalx2, darx2;
        int i, j, k, x, y, z, zz, z1, z2, xp1, yp1, xp2, yp2, xspan, yspan, xsiz, ysiz;
        int siz, bot;
        int lx, rx, yinc;

        switch ((cstat >> 4) & 3) {
            case 0: // Face sprite
                if (yp <= (4 << 8)) {
                    return;
                }

                siz = divscale(xdimenscale, yp, 19);

                xv = mulscale((tspr.getXrepeat()) << 16, xyaspect, 16);

                xspan = pic.getWidth();
                yspan = pic.getHeight();
                xsiz = mulscale(siz, xv * xspan, 30);
                ysiz = mulscale(siz, tspr.getYrepeat() * yspan, 14);

                if (((xspan >> 11) >= xsiz) || (yspan >= (ysiz >> 1))) {
                    return; // Watch out for divscale overflow
                }

                x1 = xb - (xsiz >> 1);
                if ((xspan & 1) != 0) {
                    x1 += mulscale(siz, xv, 31); // Odd xspans
                }
                i = mulscale(siz, xv * xoff, 30);
                if ((cstat & 4) == 0) {
                    x1 -= i;
                } else {
                    x1 += i;
                }

                y1 = mulscale(tspr.getZ() - globalposz, siz, 16);
                y1 -= mulscale(siz, tspr.getYrepeat() * yoff, 14);
                y1 += ((int) globalhoriz << 8) - ysiz;
                if ((cstat & 128) != 0) {
                    y1 += (ysiz >> 1);
                    if ((yspan & 1) != 0) {
                        y1 += mulscale(siz, tspr.getYrepeat(), 15); // Odd yspans
                    }
                }

                x2 = x1 + xsiz - 1;
                y2 = y1 + ysiz - 1;
                if ((y1 | 255) >= (y2 | 255)) {
                    return;
                }

                lx = (x1 >> 8) + 1;
                if (lx < 0) {
                    lx = 0;
                }
                rx = (x2 >> 8);
                if (rx >= xdimen) {
                    rx = xdimen - 1;
                }
                if (lx > rx) {
                    return;
                }

                long startum = 0;
                if ((sec.getCeilingstat() & 3) == 0) {
                    startum = (long) globalhoriz + mulscale(siz, sec.getCeilingz() - globalposz, 24); // - 1;
                }

                long startdm = 0x7fffffff;
                if ((sec.getFloorstat() & 3) == 0) {
                    startdm = (long) globalhoriz + mulscale(siz, sec.getFloorz() - globalposz, 24); // + 1;
                }

                if ((y1 >> 8) > startum) {
                    startum = (y1 >> 8);
                }
                if ((y2 >> 8) < startdm) {
                    startdm = (y2 >> 8);
                }

                if (startum < -32768) {
                    startum = -32768;
                }
                if (startdm > 32767) {
                    startdm = 32767;
                }
                if (startum >= startdm) {
                    return;
                }

                int linum;
                int linuminc;
                if ((cstat & 4) == 0) {
                    linuminc = divscale(xspan, xsiz, 24);
                    linum = mulscale((lx << 8) - x1, linuminc, 8);
                } else {
                    linuminc = -divscale(xspan, xsiz, 24);
                    linum = mulscale((lx << 8) - x2, linuminc, 8);
                }

                for (x = lx; x <= rx; x++) {
                    uwall[x] = (short) Math.max(startumost[x + windowx1] - windowy1, (short) startum + 1);
                    dwall[x] = (short) Math.min(startdmost[x + windowx1] - windowy1, (short) startdm - 1);
                }

                int daclip = 0;
                for (i = smostwallcnt - 1; i >= 0; i--) {
                    if ((smostwalltype[i] & daclip) != 0) {
                        continue;
                    }
                    j = smostwall[i];
                    if ((xb1[j] > rx) || (xb2[j] < lx)) {
                        continue;
                    }
                    if ((yp <= yb1[j]) && (yp <= yb2[j])) {
                        continue;
                    }
                    if (spritewallfront(tspr, thewall[j]) && ((yp <= yb1[j]) || (yp <= yb2[j]))) {
                        continue;
                    }

                    dalx2 = Math.max(xb1[j], lx);
                    darx2 = Math.min(xb2[j], rx);

                    switch (smostwalltype[i]) {
                        case 0:
                            if (dalx2 <= darx2) {
                                if ((dalx2 == lx) && (darx2 == rx)) {
                                    return;
                                }
                                for (k = dalx2; k <= darx2; k++) {
                                    dwall[k] = 0;
                                }
                            }
                            break;
                        case 1:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] > uwall[x]) {
                                    uwall[x] = smost[k + x];
                                }
                            }
                            if ((dalx2 == lx) && (darx2 == rx)) {
                                daclip |= 1;
                            }
                            break;
                        case 2:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] < dwall[x]) {
                                    dwall[x] = smost[k + x];
                                }
                            }
                            if ((dalx2 == lx) && (darx2 == rx)) {
                                daclip |= 2;
                            }
                            break;
                    }
                }

                if (uwall[rx] >= dwall[rx]) {
                    for (x = lx; x < rx; x++) {
                        if (uwall[x] < dwall[x]) {
                            break;
                        }
                    }
                    if (x == rx) {
                        return;
                    }
                }

                z2 = tspr.getZ() - ((yoff * tspr.getYrepeat()) << 2);
                if ((cstat & 128) != 0) {
                    z2 += ((yspan * tspr.getYrepeat()) << 1);
                    if ((yspan & 1) != 0) {
                        z2 += (tspr.getYrepeat() << 1); // Odd yspans
                    }
                }
                z1 = z2 - ((yspan * tspr.getYrepeat()) << 2);

                globalorientation = 0;
                globalpicnum = (short) tilenum;
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }
                globalxpanning = 0;
                globalypanning = 0;
                globvis = globalvisibility;
                if (sec.getVisibility() != 0) {
                    globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                }
                globalshiftval = (short) (pic.getSizey());
                if (pow2long[globalshiftval] != pic.getHeight()) {
                    globalshiftval++;
                }

                globalshiftval = (short) (32 - globalshiftval);
                globalyscale = divscale(512, tspr.getYrepeat(), globalshiftval - 19);
                globalzd = (((globalposz - z1) * globalyscale) << 8);
                if ((cstat & 8) > 0) {
                    globalyscale = -globalyscale;
                    globalzd = (((globalposz - z2) * globalyscale) << 8);
                }

                qinterpolatedown16(lwall, lx, rx - lx + 1, linum, linuminc);

                Arrays.fill(swall, lx, rx + 1, mulscale(yp, xdimscale, 19));
                if ((cstat & 2) == 0) {
                    maskwallscan(lx, rx, uwall, dwall, swall, lwall);
                } else {
                    transmaskwallscan(lx, rx);
                }

                break;
            case 1: // Wall sprite
                if ((cstat & 4) > 0) {
                    xoff = -xoff;
                }
                if ((cstat & 8) > 0) {
                    yoff = -yoff;
                }

                xspan = pic.getWidth();
                yspan = pic.getHeight();
                xv = tspr.getXrepeat() * EngineUtils.cos(tspr.getAng() + 2048 + 1536);
                yv = tspr.getXrepeat() * EngineUtils.sin(tspr.getAng() + 2048 + 1536);
                i = (xspan >> 1) + xoff;
                x1 = tspr.getX() - globalposx - mulscale(xv, i, 16);
                x2 = x1 + mulscale(xv, xspan, 16);
                y1 = tspr.getY() - globalposy - mulscale(yv, i, 16);
                y2 = y1 + mulscale(yv, xspan, 16);

                yp1 = dmulscale(x1, cosviewingrangeglobalang, y1, sinviewingrangeglobalang, 6);
                yp2 = dmulscale(x2, cosviewingrangeglobalang, y2, sinviewingrangeglobalang, 6);
                if ((yp1 <= 0) && (yp2 <= 0)) {
                    return;
                }
                xp1 = dmulscale(y1, cosglobalang, -x1, singlobalang, 6);
                xp2 = dmulscale(y2, cosglobalang, -x2, singlobalang, 6);

                x1 += globalposx;
                y1 += globalposy;
                x2 += globalposx;
                y2 += globalposy;

                int swapped = 0;
                if (dmulscale(xp1, yp2, -xp2, yp1, 32) >= 0) // If wall's NOT facing you
                {
                    if ((cstat & 64) != 0) {
                        return;
                    }
                    i = xp1;
                    xp1 = xp2;
                    xp2 = i;
                    i = yp1;
                    yp1 = yp2;
                    yp2 = i;
                    i = x1;
                    x1 = x2;
                    x2 = i;
                    i = y1;
                    y1 = y2;
                    y2 = i;
                    swapped = 1;
                }

                if (xp1 >= -yp1) {
                    if (xp1 > yp1) {
                        return;
                    }

                    if (yp1 == 0) {
                        return;
                    }
                    xb1[MAXWALLSB - 1] = halfxdimen + scale(xp1, halfxdimen, yp1);
                    if (xp1 >= 0) {
                        xb1[MAXWALLSB - 1]++; // Fix for SIGNED divide
                    }
                    if (xb1[MAXWALLSB - 1] >= xdimen) {
                        xb1[MAXWALLSB - 1] = xdimen - 1;
                    }
                    yb1[MAXWALLSB - 1] = yp1;
                } else {
                    if (xp2 < -yp2) {
                        return;
                    }
                    xb1[MAXWALLSB - 1] = 0;
                    i = yp1 - yp2 + xp1 - xp2;
                    if (i == 0) {
                        return;
                    }
                    yb1[MAXWALLSB - 1] = yp1 + scale(yp2 - yp1, xp1 + yp1, i);
                }
                if (xp2 <= yp2) {
                    if (xp2 < -yp2) {
                        return;
                    }

                    if (yp2 == 0) {
                        return;
                    }
                    xb2[MAXWALLSB - 1] = halfxdimen + scale(xp2, halfxdimen, yp2) - 1;
                    if (xp2 >= 0) {
                        xb2[MAXWALLSB - 1]++; // Fix for SIGNED divide
                    }
                    if (xb2[MAXWALLSB - 1] >= xdimen) {
                        xb2[MAXWALLSB - 1] = xdimen - 1;
                    }
                    yb2[MAXWALLSB - 1] = yp2;
                } else {
                    if (xp1 > yp1) {
                        return;
                    }

                    xb2[MAXWALLSB - 1] = xdimen - 1;
                    i = xp2 - xp1 + yp1 - yp2;
                    if (i == 0) {
                        return;
                    }
                    yb2[MAXWALLSB - 1] = yp1 + scale(yp2 - yp1, yp1 - xp1, i);
                }

                if ((yb1[MAXWALLSB - 1] < 256) || (yb2[MAXWALLSB - 1] < 256) || (xb1[MAXWALLSB - 1] > xb2[MAXWALLSB - 1])) {
                    return;
                }

                int topinc = -mulscale(yp1, xspan, 10);
                int top = (((mulscale(xp1, xdimen, 10) - mulscale(xb1[MAXWALLSB - 1] - halfxdimen, yp1, 9)) * xspan) >> 3);
                int botinc = ((yp2 - yp1) >> 8);
                bot = mulscale(xp1 - xp2, xdimen, 11) + mulscale(xb1[MAXWALLSB - 1] - halfxdimen, botinc, 2);

                j = xb2[MAXWALLSB - 1] + 3;
                z = mulscale(top, a.krecipasm(bot), 20);
                lwall[xb1[MAXWALLSB - 1]] = (z >> 8);
                for (x = xb1[MAXWALLSB - 1] + 4; x <= j; x += 4) {
                    top += topinc;
                    bot += botinc;
                    zz = z;
                    z = mulscale(top, a.krecipasm(bot), 20);
                    lwall[x] = (z >> 8);
                    i = ((z + zz) >> 1);
                    lwall[x - 2] = (i >> 8);
                    lwall[x - 3] = ((i + zz) >> 9);
                    lwall[x - 1] = ((i + z) >> 9);
                }

                if (lwall[xb1[MAXWALLSB - 1]] < 0) {
                    lwall[xb1[MAXWALLSB - 1]] = 0;
                }
                if (lwall[xb2[MAXWALLSB - 1]] >= xspan) {
                    lwall[xb2[MAXWALLSB - 1]] = xspan - 1;
                }

                if ((swapped ^ (((cstat & 4) > 0) ? 1 : 0)) > 0) {
                    j = xspan - 1;
                    for (x = xb1[MAXWALLSB - 1]; x <= xb2[MAXWALLSB - 1]; x++) {
                        lwall[x] = j - lwall[x];
                    }
                }

                rx1[MAXWALLSB - 1] = xp1;
                ry1[MAXWALLSB - 1] = yp1;
                rx2[MAXWALLSB - 1] = xp2;
                ry2[MAXWALLSB - 1] = yp2;

                int hplc = divscale(xdimenscale, yb1[MAXWALLSB - 1], 19);
                long hinc = divscale(xdimenscale, yb2[MAXWALLSB - 1], 19);
                hinc = (hinc - hplc) / (xb2[MAXWALLSB - 1] - xb1[MAXWALLSB - 1] + 1);

                z2 = tspr.getZ() - ((yoff * tspr.getYrepeat()) << 2);
                if ((cstat & 128) != 0) {
                    z2 += ((yspan * tspr.getYrepeat()) << 1);
                    if ((yspan & 1) != 0) {
                        z2 += (tspr.getYrepeat() << 1); // Odd yspans
                    }
                }
                z1 = z2 - ((yspan * tspr.getYrepeat()) << 2);

                globalorientation = 0;
                globalpicnum = (short) tilenum;
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }
                globalxpanning = 0;
                globalypanning = 0;
                globvis = globalvisibility;

                if (sec.getVisibility() != 0) {
                    globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                }
                globalshiftval = (short) (pic.getSizey());
                if (pow2long[globalshiftval] != pic.getHeight()) {
                    ++globalshiftval;
                }
                globalshiftval = (short) (32 - globalshiftval);
                globalyscale = divscale(512, tspr.getYrepeat(), globalshiftval - 19);
                globalzd = (((globalposz - z1) * globalyscale) << 8);
                if ((cstat & 8) > 0) {
                    globalyscale = -globalyscale;
                    globalzd = (((globalposz - z2) * globalyscale) << 8);
                }

                if (((sec.getCeilingstat() & 1) == 0) && (z1 < sec.getCeilingz())) {
                    z1 = sec.getCeilingz();
                }
                if (((sec.getFloorstat() & 1) == 0) && (z2 > sec.getFloorz())) {
                    z2 = sec.getFloorz();
                }

                owallmost(uwall, (MAXWALLSB - 1), z1 - globalposz + 1);
                owallmost(dwall, (MAXWALLSB - 1), z2 - globalposz - 1);
                for (i = xb1[MAXWALLSB - 1]; i <= xb2[MAXWALLSB - 1]; i++) {
                    swall[i] = (a.krecipasm(hplc) << 2);
                    hplc += (int) hinc;
                }

                for (i = smostwallcnt - 1; i >= 0; i--) {
                    j = smostwall[i];

                    if ((xb1[j] > xb2[MAXWALLSB - 1]) || (xb2[j] < xb1[MAXWALLSB - 1])) {
                        continue;
                    }

                    dalx2 = xb1[j];
                    darx2 = xb2[j];
                    if (Math.max(yb1[MAXWALLSB - 1], yb2[MAXWALLSB - 1]) > Math.min(yb1[j], yb2[j])) {
                        if (Math.min(yb1[MAXWALLSB - 1], yb2[MAXWALLSB - 1]) > Math.max(yb1[j], yb2[j])) {
                            x = 0x80000000;
                        } else {
                            x = thewall[j];
                            xp1 = boardService.getWall(x).getX();
                            yp1 = boardService.getWall(x).getY();
                            x = boardService.getWall(x).getPoint2();
                            xp2 = boardService.getWall(x).getX();
                            yp2 = boardService.getWall(x).getY();

                            z1 = (xp2 - xp1) * (y1 - yp1) - (yp2 - yp1) * (x1 - xp1);
                            z2 = (xp2 - xp1) * (y2 - yp1) - (yp2 - yp1) * (x2 - xp1);
                            if ((z1 ^ z2) >= 0) {
                                x = (z1 + z2);
                            } else {
                                z1 = (x2 - x1) * (yp1 - y1) - (y2 - y1) * (xp1 - x1);
                                z2 = (x2 - x1) * (yp2 - y1) - (y2 - y1) * (xp2 - x1);

                                if ((z1 ^ z2) >= 0) {
                                    x = -(z1 + z2);
                                } else {
                                    if ((xp2 - xp1) * (tspr.getY() - yp1) == (tspr.getX() - xp1) * (yp2 - yp1)) {
                                        if (boardService.getWall(thewall[j]).getNextsector() == tspr.getSectnum()) {
                                            x = 0x80000000;
                                        } else {
                                            x = 0x7fffffff;
                                        }
                                    } else { // INTERSECTION!
                                        x = (xp1 - globalposx) + scale(xp2 - xp1, z1, z1 - z2);
                                        y = (yp1 - globalposy) + scale(yp2 - yp1, z1, z1 - z2);

                                        yp1 = dmulscale(x, cosglobalang, y, singlobalang, 14);
                                        if (yp1 > 0) {
                                            xp1 = dmulscale(y, cosglobalang, -x, singlobalang, 14);

                                            x = halfxdimen + scale(xp1, halfxdimen, yp1);
                                            if (xp1 >= 0) {
                                                x++; // Fix for SIGNED divide
                                            }

                                            if (z1 < 0) {
                                                if (dalx2 < x) {
                                                    dalx2 = x;
                                                }
                                            } else {
                                                if (darx2 > x) {
                                                    darx2 = x;
                                                }
                                            }
                                            x = 0x80000001;
                                        } else {
                                            x = 0x7fffffff;
                                        }
                                    }
                                }
                            }
                        }
                        if (x < 0) {
                            if (dalx2 < xb1[MAXWALLSB - 1]) {
                                dalx2 = xb1[MAXWALLSB - 1];
                            }
                            if (darx2 > xb2[MAXWALLSB - 1]) {
                                darx2 = xb2[MAXWALLSB - 1];
                            }
                            switch (smostwalltype[i]) {
                                case 0:
                                    if (dalx2 <= darx2) {
                                        if ((dalx2 == xb1[MAXWALLSB - 1]) && (darx2 == xb2[MAXWALLSB - 1])) {
                                            return;
                                        }
                                        for (k = dalx2; k <= darx2; k++) {
                                            dwall[k] = 0;
                                        }
                                    }
                                    break;
                                case 1:
                                    k = smoststart[i] - xb1[j];
                                    for (x = dalx2; x <= darx2; x++) {
                                        if (smost[k + x] > uwall[x]) {
                                            uwall[x] = smost[k + x];
                                        }
                                    }
                                    break;
                                case 2:
                                    k = smoststart[i] - xb1[j];
                                    for (x = dalx2; x <= darx2; x++) {
                                        if (smost[k + x] < dwall[x]) {
                                            dwall[x] = smost[k + x];
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }

                if ((cstat & 2) == 0) {
                    maskwallscan(xb1[MAXWALLSB - 1], xb2[MAXWALLSB - 1], uwall, dwall, swall, lwall);
                } else {
                    transmaskwallscan(xb1[MAXWALLSB - 1], xb2[MAXWALLSB - 1]);
                }
                break;
            case 2: // Floor sprite
                if ((cstat & 64) != 0) {
                    if ((globalposz > tspr.getZ()) == ((cstat & 8) == 0)) {
                        return;
                    }
                }

                if ((cstat & 4) > 0) {
                    xoff = -xoff;
                }
                if ((cstat & 8) > 0) {
                    yoff = -yoff;
                }
                xspan = pic.getWidth();
                yspan = pic.getHeight();

                // Rotate center point
                dax = tspr.getX() - globalposx;
                day = tspr.getY() - globalposy;
                rzi[0] = dmulscale(cosglobalang, dax, singlobalang, day, 10);
                rxi[0] = dmulscale(cosglobalang, day, -singlobalang, dax, 10);

                // Get top-left corner
                i = ((tspr.getAng() + 2048 - (int) globalang) & 2047);
                int cosang = EngineUtils.cos(i);
                int sinang = EngineUtils.sin(i);
                dax = ((xspan >> 1) + xoff) * tspr.getXrepeat();
                day = ((yspan >> 1) + yoff) * tspr.getYrepeat();
                rzi[0] += dmulscale(sinang, dax, cosang, day, 12);
                rxi[0] += dmulscale(sinang, day, -cosang, dax, 12);

                // Get other 3 corners
                dax = xspan * tspr.getXrepeat();
                day = yspan * tspr.getYrepeat();
                rzi[1] = rzi[0] - mulscale(sinang, dax, 12);
                rxi[1] = rxi[0] + mulscale(cosang, dax, 12);
                dax = -mulscale(cosang, day, 12);
                day = -mulscale(sinang, day, 12);
                rzi[2] = rzi[1] + dax;
                rxi[2] = rxi[1] + day;
                rzi[3] = rzi[0] + dax;
                rxi[3] = rxi[0] + day;

                // Put all points on same z
                ryi[0] = scale((tspr.getZ() - globalposz), yxaspect, 320 << 8);
                if (ryi[0] == 0) {
                    return;
                }
                ryi[1] = ryi[2] = ryi[3] = ryi[0];

                if ((cstat & 4) == 0) {
                    z = 0;
                    z1 = 1;
                    z2 = 3;
                } else {
                    z = 1;
                    z1 = 0;
                    z2 = 2;
                }

                dax = rzi[z1] - rzi[z];
                day = rxi[z1] - rxi[z];
                bot = dmulscale(dax, dax, day, day, 8);
                if (((klabs(dax) >> 13) >= bot) || ((klabs(day) >> 13) >= bot)) {
                    return;
                }
                globalx1 = divscale(dax, bot, 18);
                globalx2 = divscale(day, bot, 18);

                dax = rzi[z2] - rzi[z];
                day = rxi[z2] - rxi[z];
                bot = dmulscale(dax, dax, day, day, 8);
                if (((klabs(dax) >> 13) >= bot) || ((klabs(day) >> 13) >= bot)) {
                    return;
                }
                globaly1 = divscale(dax, bot, 18);
                globaly2 = divscale(day, bot, 18);

                // Calculate globals for hline texture mapping function
                globalxpanning = (rxi[z] << 12);
                globalypanning = (rzi[z] << 12);
                globalzd = (ryi[z] << 12);

                rzi[0] = mulscale(rzi[0], viewingrange, 16);
                rzi[1] = mulscale(rzi[1], viewingrange, 16);
                rzi[2] = mulscale(rzi[2], viewingrange, 16);
                rzi[3] = mulscale(rzi[3], viewingrange, 16);

                if (ryi[0] < 0) // If ceilsprite is above you, reverse order of points
                {
                    i = rxi[1];
                    rxi[1] = rxi[3];
                    rxi[3] = i;
                    i = rzi[1];
                    rzi[1] = rzi[3];
                    rzi[3] = i;
                }

                // Clip polygon in 3-space
                int npoints = 4;

                // Clip edge 1
                int npoints2 = 0;
                int zzsgn = rxi[0] + rzi[0];
                for (z = 0; z < npoints; z++) {
                    zz = z + 1;
                    if (zz == npoints) {
                        zz = 0;
                    }
                    int zsgn = zzsgn;
                    zzsgn = rxi[zz] + rzi[zz];
                    if (zsgn >= 0) {
                        rxi2[npoints2] = rxi[z];
                        ryi2[npoints2] = ryi[z];
                        rzi2[npoints2] = rzi[z];
                        npoints2++;
                    }
                    if ((zsgn ^ zzsgn) < 0) {
                        int t = divscale(zsgn, zsgn - zzsgn, 30);
                        rxi2[npoints2] = rxi[z] + mulscale(t, rxi[zz] - rxi[z], 30);
                        ryi2[npoints2] = ryi[z] + mulscale(t, ryi[zz] - ryi[z], 30);
                        rzi2[npoints2] = rzi[z] + mulscale(t, rzi[zz] - rzi[z], 30);
                        npoints2++;
                    }
                }
                if (npoints2 <= 2) {
                    return;
                }

                // Clip edge 2
                npoints = 0;
                zzsgn = rxi2[0] - rzi2[0];
                for (z = 0; z < npoints2; z++) {
                    zz = z + 1;
                    if (zz == npoints2) {
                        zz = 0;
                    }
                    int zsgn = zzsgn;
                    zzsgn = rxi2[zz] - rzi2[zz];
                    if (zsgn <= 0) {
                        rxi[npoints] = rxi2[z];
                        ryi[npoints] = ryi2[z];
                        rzi[npoints] = rzi2[z];
                        npoints++;
                    }
                    if ((zsgn ^ zzsgn) < 0) {
                        int t = divscale(zsgn, zsgn - zzsgn, 30);
                        rxi[npoints] = rxi2[z] + mulscale(t, rxi2[zz] - rxi2[z], 30);
                        ryi[npoints] = ryi2[z] + mulscale(t, ryi2[zz] - ryi2[z], 30);
                        rzi[npoints] = rzi2[z] + mulscale(t, rzi2[zz] - rzi2[z], 30);
                        npoints++;
                    }
                }
                if (npoints <= 2) {
                    return;
                }

                // Clip edge 3
                npoints2 = 0;
                zzsgn = (ryi[0] * halfxdimen + (rzi[0] * ((int) globalhoriz)));
                for (z = 0; z < npoints; z++) {
                    zz = z + 1;
                    if (zz == npoints) {
                        zz = 0;
                    }
                    int zsgn = zzsgn;
                    zzsgn = ryi[zz] * halfxdimen + (rzi[zz] * ((int) globalhoriz));
                    if (zsgn >= 0) {
                        rxi2[npoints2] = rxi[z];
                        ryi2[npoints2] = ryi[z];
                        rzi2[npoints2] = rzi[z];
                        npoints2++;
                    }
                    if ((zsgn ^ zzsgn) < 0) {
                        int t = divscale(zsgn, zsgn - zzsgn, 30);
                        rxi2[npoints2] = rxi[z] + mulscale(t, rxi[zz] - rxi[z], 30);
                        ryi2[npoints2] = ryi[z] + mulscale(t, ryi[zz] - ryi[z], 30);
                        rzi2[npoints2] = rzi[z] + mulscale(t, rzi[zz] - rzi[z], 30);
                        npoints2++;
                    }
                }
                if (npoints2 <= 2) {
                    return;
                }

                // Clip edge 4
                npoints = 0;
                zzsgn = ryi2[0] * halfxdimen + (rzi2[0] * ((int) globalhoriz - ydimen));
                for (z = 0; z < npoints2; z++) {
                    zz = z + 1;
                    if (zz == npoints2) {
                        zz = 0;
                    }
                    int zsgn = zzsgn;
                    zzsgn = ryi2[zz] * halfxdimen + (rzi2[zz] * ((int) globalhoriz - ydimen));
                    if (zsgn <= 0) {
                        rxi[npoints] = rxi2[z];
                        ryi[npoints] = ryi2[z];
                        rzi[npoints] = rzi2[z];
                        npoints++;
                    }
                    if ((zsgn ^ zzsgn) < 0) {
                        int t = divscale(zsgn, zsgn - zzsgn, 30);
                        rxi[npoints] = rxi2[z] + mulscale(t, rxi2[zz] - rxi2[z], 30);
                        ryi[npoints] = ryi2[z] + mulscale(t, ryi2[zz] - ryi2[z], 30);
                        rzi[npoints] = rzi2[z] + mulscale(t, rzi2[zz] - rzi2[z], 30);
                        npoints++;
                    }
                }
                if (npoints <= 2) {
                    return;
                }

                // Project onto screen
                int lpoint = -1;
                int lmax = 0x7fffffff;
                int rpoint = -1;
                int rmax = 0x80000000;
                for (z = 0; z < npoints; z++) {
                    if (rzi[z] == 0) {
                        continue;
                    }
                    xsi[z] = scale(rxi[z], xdimen << 15, rzi[z]) + (xdimen << 15);
                    ysi[z] = scale(ryi[z], xdimen << 15, rzi[z]) + ((int) globalhoriz << 16);
                    if (xsi[z] < 0) {
                        xsi[z] = 0;
                    }
                    if (xsi[z] > (xdimen << 16)) {
                        xsi[z] = (xdimen << 16);
                    }
                    if (ysi[z] < (0)) {
                        ysi[z] = (0);
                    }
                    if (ysi[z] > (ydimen << 16)) {
                        ysi[z] = (ydimen << 16);
                    }
                    if (xsi[z] < lmax) {
                        lmax = xsi[z];
                        lpoint = z;
                    }
                    if (xsi[z] > rmax) {
                        rmax = xsi[z];
                        rpoint = z;
                    }
                }

                // Get uwall arrays
                for (z = lpoint; z != rpoint; z = zz) {
                    zz = z + 1;
                    if (zz == npoints) {
                        zz = 0;
                    }

                    dax1 = ((xsi[z] + 65535) >> 16);
                    dax2 = ((xsi[zz] + 65535) >> 16);
                    if (dax2 > dax1) {
                        yinc = divscale(ysi[zz] - ysi[z], xsi[zz] - xsi[z], 16);
                        y = ysi[z] + mulscale((dax1 << 16) - xsi[z], yinc, 16);
                        qinterpolatedown16short(uwall, dax1, dax2 - dax1, y, yinc);
                    }
                }

                // Get dwall arrays
                for (; z != lpoint; z = zz) {
                    zz = z + 1;
                    if (zz == npoints) {
                        zz = 0;
                    }

                    dax1 = ((xsi[zz] + 65535) >> 16);
                    dax2 = ((xsi[z] + 65535) >> 16);
                    if (dax2 > dax1) {
                        yinc = divscale(ysi[zz] - ysi[z], xsi[zz] - xsi[z], 16);
                        y = ysi[zz] + mulscale((dax1 << 16) - xsi[zz], yinc, 16);
                        qinterpolatedown16short(dwall, dax1, dax2 - dax1, y, yinc);
                    }
                }

                lx = (lmax + 65535) >> 16;
                rx = (rmax + 65535) >> 16;
                for (x = lx; x <= rx; x++) {
                    uwall[x] = (short) Math.max(uwall[x] + 1, startumost[x + windowx1] - windowy1);
                    dwall[x] = (short) Math.min(dwall[x] - 1, startdmost[x + windowx1] - windowy1);
                }

                // Additional uwall/dwall clipping goes here
                for (i = smostwallcnt - 1; i >= 0; i--) {
                    j = smostwall[i];
                    if ((xb1[j] > rx) || (xb2[j] < lx)) {
                        continue;
                    }
                    if ((yp <= yb1[j]) && (yp <= yb2[j])) {
                        continue;
                    }

                    // if (spritewallfront(tspr,thewall[j]) == 0)
                    x = thewall[j];
                    xp1 = boardService.getWall(x).getX();
                    yp1 = boardService.getWall(x).getY();
                    x = boardService.getWall(x).getPoint2();
                    xp2 = boardService.getWall(x).getX();
                    yp2 = boardService.getWall(x).getY();
                    x = (xp2 - xp1) * (tspr.getY() - yp1) - (tspr.getX() - xp1) * (yp2 - yp1);
                    if ((yp > yb1[j]) && (yp > yb2[j])) {
                        x = -1;
                    }
                    if ((x >= 0) && ((x != 0) || (boardService.getWall(thewall[j]).getNextsector() != tspr.getSectnum()))) {
                        continue;
                    }

                    dalx2 = Math.max(xb1[j], lx);
                    darx2 = Math.min(xb2[j], rx);

                    switch (smostwalltype[i]) {
                        case 0:
                            if (dalx2 <= darx2) {
                                if ((dalx2 == lx) && (darx2 == rx)) {
                                    return;
                                }
                                for (x = dalx2; x <= darx2; x++) {
                                    dwall[x] = 0;
                                }
                            }
                            break;
                        case 1:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] > uwall[x]) {
                                    uwall[x] = smost[k + x];
                                }
                            }
                            break;
                        case 2:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] < dwall[x]) {
                                    dwall[x] = smost[k + x];
                                }
                            }
                            break;
                    }
                }

                globalorientation = cstat;
                globalpicnum = (short) tilenum;
                if (globalpicnum >= MAXTILES) {
                    globalpicnum = 0;
                }

                setgotpic(globalpicnum);
                globalbufplc = getTileBuffer(pic);

                globvis = mulscale(globalhisibility, viewingrange, 16);
                if (sec.getVisibility() != 0) {
                    globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                }

                x = pic.getSizex();
                y = pic.getSizey();
                if (pow2long[x] != xspan) {
                    x++;
                    globalx1 = mulscale(globalx1, xspan, x);
                    globalx2 = mulscale(globalx2, xspan, x);
                }

                dax = globalxpanning;
                day = globalypanning;
                globalxpanning = -dmulscale(globalx1, day, globalx2, dax, 6);
                globalypanning = -dmulscale(globaly1, day, globaly2, dax, 6);

                globalx2 = mulscale(globalx2, viewingrange, 16);
                globaly2 = mulscale(globaly2, viewingrange, 16);
                globalzd = mulscale(globalzd, viewingrangerecip, 16);

                globalx1 = (globalx1 - globalx2) * halfxdimen;
                globaly1 = (globaly1 - globaly2) * halfxdimen;

                if ((cstat & 2) == 0) {
                    a.msethlineshift(x, y);
                } else {
                    a.tsethlineshift(x, y);
                }

                // Draw it!
                ceilspritescan(lx, rx - 1);
                break;

            case 3: // Voxel sprite
                int nyrepeat;

                xoff = tspr.getXoffset();
                yoff = tspr.getYoffset();
                if ((cstat & 8) > 0) {
                    yoff = -yoff;
                }

                lx = 0;
                rx = xdim - 1;
                for (x = lx; x <= rx; x++) {
                    lwall[x] = startumost[x + windowx1] - windowy1;
                    swall[x] = startdmost[x + windowx1] - windowy1;
                }
                for (i = smostwallcnt - 1; i >= 0; i--) {
                    j = smostwall[i];
                    if ((xb1[j] > rx) || (xb2[j] < lx)) {
                        continue;
                    }
                    if ((yp <= yb1[j]) && (yp <= yb2[j])) {
                        continue;
                    }
                    if (spritewallfront(tspr, thewall[j]) && ((yp <= yb1[j]) || (yp <= yb2[j]))) {
                        continue;
                    }

                    dalx2 = Math.max(xb1[j], lx);
                    darx2 = Math.min(xb2[j], rx);

                    switch (smostwalltype[i]) {
                        case 0:
                            if (dalx2 <= darx2) {
                                if ((dalx2 == lx) && (darx2 == rx)) {
                                    return;
                                }
                                for (x = dalx2; x <= darx2; x++) {
                                    swall[x] = 0;
                                }
                            }
                            break;
                        case 1:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] > lwall[x]) {
                                    lwall[x] = smost[k + x];
                                }
                            }
                            break;
                        case 2:
                            k = smoststart[i] - xb1[j];
                            for (x = dalx2; x <= darx2; x++) {
                                if (smost[k + x] < swall[x]) {
                                    swall[x] = smost[k + x];
                                }
                            }
                            break;
                    }
                }

                if (lwall[rx] >= swall[rx]) {
                    for (x = lx; x < rx; x++) {
                        if (lwall[x] < swall[x]) {
                            break;
                        }
                    }
                    if (x == rx) {
                        return;
                    }
                }

                if (vtilenum == null) {
                    break;
                }

                if (vtilenum.getScale() == 65536) {
                    nyrepeat = ((tspr.getYrepeat()) << 16);
                } else {
                    nyrepeat = (int) (tspr.getYrepeat() * vtilenum.getScale());
                }
                xv = (int) ((tspr.getXrepeat() * EngineUtils.cos(tspr.getAng() + 2048 + 1536)) * (vtilenum.getScale() / 65536.0f));
                yv = (int) ((tspr.getXrepeat() * EngineUtils.sin(tspr.getAng() + 2048 + 1536)) * (vtilenum.getScale() / 65536.0f));

                tspr.setX((int) (tspr.getX() - mulscale(xoff, xv, 16) / 1.25f));
                tspr.setY((int) (tspr.getY() - mulscale(xoff, yv, 16) / 1.25f));
                tspr.setZ(tspr.getZ() - mulscale(yoff, nyrepeat, 14));

                if ((cstat & 128) == 0)
                // tspr.z -= mulscale(tilesizy[tspr.picnum], nyrepeat, 15); // GDX this more
                // correct, but disabled for compatible with eduke
                {
                    tspr.setZ(tspr.getZ() - mulscale(vtilenum.getData().zpiv[0], nyrepeat, 22));
                }

                if ((cstat & 8) != 0 && (cstat & 16) != 0) {
                    tspr.setZ(tspr.getZ() + mulscale((pic.getHeight() / 2) - vtilenum.getData().zpiv[0], nyrepeat, 36));
                }

                globvis = globalvisibility;
                globalorientation = cstat;
                if (sec.getVisibility() != 0) {
                    globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
                }

                i = tspr.getAng() + 1536;
                if (vtilenum.isRotating()) {
                    i -= (5 * engine.getTotalClock()) & 2047;
                }

                Spriteext sprext = defs.mapInfo.getSpriteInfo(tspr.getOwner());
                if (sprext != null) {
                    i += sprext.angoff;
                }

                float f = 1.0f;
                if ((boardService.getSprite(tspr.getOwner()).getCstat() & 48) == 16 || (boardService.getSprite(tspr.getOwner()).getCstat() & 48) == 32) {
                    f *= 1.25f;
                }

                voxdraw(tspr, i, (int) (tspr.getXrepeat() * f), tspr.getYrepeat(), vtilenum, lwall, swall);
                break;
        }
        if (automapping == 1) {
            show2dsprite.setBit(spritenum);
        }
    }

    private void voxdraw(Sprite daspr, int dasprang, int daxscale, int dayscale, VoxelInfo daindex, int[] daumost,
                         int[] dadmost) {
        int i, j, k, x, y, syoff, nxoff;
        int cosang, sinang, sprcosang, sprsinang, backx, backy, gxinc, gyinc;
        int daxsiz, daysiz, daxpivot, daypivot, dazpivot;
        int daxscalerecip, dayscalerecip, cnt;
        long  gxstart, gystart, ggxstart, ggystart, nx, ny;
        int l1, l2, slabxoffs;
        int lx, rx, x1 = 0, y1 = 0, z1, x2 = 0, y2 = 0, z2, yplc, yinc = 0;
        int yoff, xs = 0, ys = 0, xe, ye, xi = 0, yi = 0, cbackx, cbacky, dagxinc, dagyinc;
        int voxptr, voxend, zleng, mip;

        int dasprx = daspr.getX();
        int daspry = daspr.getY();
        int dasprz = daspr.getZ();
        int dashade = daspr.getShade();
        int dapal = daspr.getPal();
        Sector sec = boardService.getSector(daspr.getSectnum());

        cosang = EngineUtils.cos((int) (globalang));
        sinang = EngineUtils.sin((int) globalang);
        sprcosang = EngineUtils.cos(dasprang);
        sprsinang = EngineUtils.sin(dasprang);

        mip = klabs(dmulscale(dasprx - globalposx, cosang, daspry - globalposy, sinang, 6));
        j = paletteManager.getPalookup(mulscale(globvis, mip, 21), dashade) << 8;

        int trans = 0;
        if ((globalorientation & 2) != 0) {
            if ((globalorientation & 512) != 0) {
                trans = 2;
            } else {
                trans = 1;
            }
        }
        a.setupdrawslab(ylookup[1], getPalookupBuffer(dapal), j, trans);

        if (!novoxmips) {
            j = 1310720;
            j *= Math.min(daxscale, dayscale);
            j >>= 6; // New hacks (for sized-down voxels)
            for (k = 0; k < MAXVOXMIPS; k++) {
                if (mip < j) {
                    mip = k;
                    break;
                }
                j <<= 1;
            }
            if (k >= MAXVOXMIPS) {
                mip = MAXVOXMIPS - 1;
            }
        } else {
            mip = 0;
        }

        VoxelData davox = daindex.getData();
        int scale = (int) daindex.getScale();
        if (davox == null) {
            return;
        }

        if (davox.data[mip] == null && mip > 0) {
            mip = 0;
        }

        if (scale == 65536) {
            daxscale <<= (mip + 8);
            dayscale <<= (mip + 8);
        } else {
            daxscale = mulscale((long) daxscale << mip, scale, 8);
            dayscale = mulscale((long) dayscale << mip, scale, 8);
        }

        int odayscale = dayscale; // tspr.yrepeat
        daxscale = mulscale(daxscale, xyaspect, 16);
        daxscale = scale(daxscale, xdimenscale, (long) xdimen << 8);
        dayscale = scale(dayscale, mulscale(xdimenscale, viewingrangerecip, 16), (long) xdimen << 8);

        if (daxscale == 0 || dayscale == 0) {
            return;
        }

        daxscalerecip = (1 << 30) / daxscale;
        dayscalerecip = (1 << 30) / dayscale;

        daxsiz = davox.xsiz[mip];
        daysiz = davox.ysiz[mip];

        daxpivot = davox.xpiv[mip];
        daypivot = davox.ypiv[mip];
        dazpivot = davox.zpiv[mip];

        x = mulscale(globalposx - dasprx, daxscalerecip, 16);
        y = mulscale(globalposy - daspry, daxscalerecip, 16);
        backx = ((dmulscale(x, sprcosang, y, sprsinang, 10) + daxpivot) >> 8);
        backy = ((dmulscale(y, sprcosang, x, -sprsinang, 10) + daypivot) >> 8);
        cbackx = Math.min(Math.max(backx, 0), daxsiz - 1);
        cbacky = Math.min(Math.max(backy, 0), daysiz - 1);

        sprcosang = mulscale(daxscale, sprcosang, 14);
        sprsinang = mulscale(daxscale, sprsinang, 14);

        x = (dasprx - globalposx) - dmulscale(daxpivot, sprcosang, daypivot, -sprsinang, 18);
        y = (daspry - globalposy) - dmulscale(daypivot, sprcosang, daxpivot, sprsinang, 18);

        cosang = mulscale(cosang, dayscalerecip, 16);
        sinang = mulscale(sinang, dayscalerecip, 16);

        gxstart = (long) y * cosang - (long) x * sinang;
        gystart = (long) x * cosang + (long) y * sinang;
        gxinc = dmulscale(sprsinang, cosang, sprcosang, -sinang, 10);
        gyinc = dmulscale(sprcosang, cosang, sprsinang, sinang, 10);

        x = 0;
        y = 0;
        j = Math.max(daxsiz, daysiz);
        for (i = 0; i <= j; i++) {
            ggxinc[i] = x;
            x += gxinc;
            ggyinc[i] = y;
            y += gyinc;
        }

        if ((klabs(globalposz - dasprz) >> 10) >= klabs(odayscale)) {
            return;
        }

        syoff = divscale(globalposz - dasprz, odayscale, 21) + (dazpivot << 7);
        yoff = ((klabs(gxinc) + klabs(gyinc)) >> 1);

        boolean xflip = (globalorientation & 4) != 0;
        boolean yflip = (globalorientation & 8) != 0;
        int xptr, dazsiz = davox.zsiz[mip];
        short[] shortptr;

        int dm = divscale(sec.getFloorz() - globalposz, odayscale, 21);
        int um = divscale(sec.getCeilingz() - globalposz, odayscale, 21);
        for (cnt = 0; cnt < 8; cnt++) {
            switch (cnt) {
                case 0:
                    xs = 0;
                    ys = 0;
                    xi = 1;
                    yi = 1;
                    break;
                case 1:
                    xs = daxsiz - 1;
                    ys = 0;
                    xi = -1;
                    yi = 1;
                    break;
                case 2:
                    xs = 0;
                    ys = daysiz - 1;
                    xi = 1;
                    yi = -1;
                    break;
                case 3:
                    xs = daxsiz - 1;
                    ys = daysiz - 1;
                    xi = -1;
                    yi = -1;
                    break;
                case 4:
                    xs = 0;
                    ys = cbacky;
                    xi = 1;
                    yi = 2;
                    break;
                case 5:
                    xs = daxsiz - 1;
                    ys = cbacky;
                    xi = -1;
                    yi = 2;
                    break;
                case 6:
                    xs = cbackx;
                    ys = 0;
                    xi = 2;
                    yi = 1;
                    break;
                case 7:
                    xs = cbackx;
                    ys = daysiz - 1;
                    xi = 2;
                    yi = -1;
                    break;
            }
            xe = cbackx;
            ye = cbacky;
            if (cnt < 4) {
                if ((xi < 0) && (xe >= xs)) {
                    continue;
                }
                if ((xi > 0) && (xe <= xs)) {
                    continue;
                }
                if ((yi < 0) && (ye >= ys)) {
                    continue;
                }
                if ((yi > 0) && (ye <= ys)) {
                    continue;
                }
            } else {
                if ((xi < 0) && (xe > xs)) {
                    continue;
                }
                if ((xi > 0) && (xe < xs)) {
                    continue;
                }
                if ((yi < 0) && (ye > ys)) {
                    continue;
                }
                if ((yi > 0) && (ye < ys)) {
                    continue;
                }
                xe += xi;
                ye += yi;
            }

            i = ksgn(ys - backy) + ksgn(xs - backx) * 3 + 4;
            switch (i) {
                case 6:
                case 7:
                    x1 = 0;
                    y1 = 0;
                    break;
                case 8:
                case 5:
                    x1 = gxinc;
                    y1 = gyinc;
                    break;
                case 0:
                case 3:
                    x1 = gyinc;
                    y1 = -gxinc;
                    break;
                case 2:
                case 1:
                    x1 = gxinc + gyinc;
                    y1 = gyinc - gxinc;
                    break;
            }
            switch (i) {
                case 2:
                case 5:
                    x2 = 0;
                    y2 = 0;
                    break;
                case 0:
                case 1:
                    x2 = gxinc;
                    y2 = gyinc;
                    break;
                case 8:
                case 7:
                    x2 = gyinc;
                    y2 = -gxinc;
                    break;
                case 6:
                case 3:
                    x2 = gxinc + gyinc;
                    y2 = gyinc - gxinc;
                    break;
            }
            short oand = (short) (pow2char[((xs < backx) ? 1 : 0)] + pow2char[((ys < backy) ? 1 : 0) + 2]);
            if (xflip) {
                oand ^= 3;
            }

            short oand16 = (short) (oand + 16);
            short oand32 = (short) (oand + 32);

            if (yflip) {
                oand16 = (short) (oand + 32);
                oand32 = (short) (oand + 16);
            }

            if (yi > 0) {
                dagxinc = gxinc;
                dagyinc = mulscale(gyinc, viewingrangerecip, 16);
            } else {
                dagxinc = -gxinc;
                dagyinc = -mulscale(gyinc, viewingrangerecip, 16);
            }

            // Fix for non 90 degree viewing ranges
            nxoff = mulscale(x2 - x1, viewingrangerecip, 16);
            x1 = mulscale(x1, viewingrangerecip, 16);

            ggxstart = gxstart + ggyinc[ys];
            ggystart = gystart - ggxinc[ys];

            for (x = xs; x != xe; x += xi) {
                xptr = xflip ? daxsiz - 1 - x : x;
                slabxoffs = davox.slabxoffs[mip][xptr];
                shortptr = davox.xyoffs[mip][xptr];

                nx = mulscale(ggxstart + ggxinc[x], viewingrangerecip, 16) + x1;
                ny = ggystart + ggyinc[x];
                for (y = ys; y != ye; y += yi, nx += dagyinc, ny -= dagxinc) {
                    if ((ny <= nytooclose) || (ny >= nytoofar)) {
                        continue;
                    }

                    voxptr = slabxoffs + shortptr[y];
                    voxend = slabxoffs + shortptr[y + 1];

                    if (voxptr == voxend) {
                        continue;
                    }

                    if ((ny + y1) < 0 || (ny + y2) < 0) {
                        continue;
                    }

                    if ((ny + y1) >> 14 >= distrecip.length || (ny + y2) >> 14 >= distrecip.length) {
                        continue;
                    }

                    lx = mulscale(nx >> 3, distrecip[(int) ((ny + y1) >> 14)], 32) + halfxdimen;
                    if (lx < 0) {
                        lx = 0;
                    }
                    rx = mulscale((nx + nxoff) >> 3, distrecip[(int) ((ny + y2) >> 14)], 32) + halfxdimen;
                    if (rx > xdimen) {
                        rx = xdimen;
                    }
                    if (rx <= lx) {
                        continue;
                    }
                    rx -= lx;

                    int index = (int) ((ny - yoff) >> 14);
                    if (index < 0 || index >= distrecip.length) {
                        continue;
                    }
                    l1 = distrecip[index];

                    index = (int) ((ny + yoff) >> 14);
                    if (index < 0 || index >= distrecip.length) {
                        continue;
                    }
                    l2 = distrecip[(int) ((ny + yoff) >> 14)];

                    int umz = 0;
                    if ((sec.getCeilingstat() & 3) == 0) {
                        umz = mulscale((um < 0) ? l1 : l2, um, 32) + (int) globalhoriz;
                    }

                    int dmz = 0x7fffffff;
                    if ((sec.getFloorstat() & 3) == 0) {
                        dmz = mulscale((dm < 0) ? l2 : l1, dm, 32) + (int) globalhoriz;
                    }

                    for (; voxptr < voxend; voxptr += zleng + 3) {
                        zleng = davox.data[mip][voxptr + 1] & 0xFF;

                        if (yflip) {
                            j = ((dazsiz - (davox.data[mip][voxptr] & 0xFF) - zleng) << 15) - syoff;
                        } else {
                            j = ((davox.data[mip][voxptr] & 0xFF) << 15) - syoff;
                        }

                        if (j < 0) {
                            k = j + (zleng << 15);
                            if (k < 0) {
                                if ((davox.data[mip][voxptr + 2] & oand32) == 0) {
                                    continue;
                                }
                                z2 = mulscale(l2, k, 32) + (int) globalhoriz; // Below slab
                            } else {
                                if ((davox.data[mip][voxptr + 2] & oand) == 0) {
                                    continue; // Middle of slab
                                }
                                z2 = mulscale(l1, k, 32) + (int) globalhoriz;
                            }
                            z1 = mulscale(l1, j, 32) + (int) globalhoriz;
                        } else {
                            if ((davox.data[mip][voxptr + 2] & oand16) == 0) {
                                continue;
                            }
                            z1 = mulscale(l2, j, 32) + (int) globalhoriz; // Above slab
                            z2 = mulscale(l1, j + (zleng << 15), 32) + (int) globalhoriz;

                        }

                        int umost = Math.max(daumost[lx], umz);
                        int dmost = Math.min(dadmost[lx], dmz);

                        if (zleng == 1) {
                            yplc = 0;
                            yinc = 0;
                            if (z1 < umost) {
                                z1 = umost;
                            }
                        } else {
                            if (z2 - z1 >= 1024) {
                                yinc = divscale(zleng, z2 - z1, 16);
                            } else if (z2 > z1) {
                                yinc = (lowrecip[z2 - z1] * zleng >> 8);
                            }
                            if (z1 < umost) {
                                yplc = yinc * (umost - z1);
                                z1 = umost;
                            } else {
                                yplc = 0;
                            }

                            if (yflip) {
                                yinc = -yinc;
                                yplc = (zleng << 16) - yplc + yinc;
                            }
                        }
                        if (z2 > dmost) {
                            z2 = dmost;
                        }

                        z2 -= z1;
                        if (z2 <= 0) {
                            continue;
                        }
                        a.drawslab(rx, yplc, z2, yinc, davox.data[mip], voxptr + 3, ylookup[z1] + lx + frameoffset);
                    }
                }
            }
        }
    }

    private void drawmaskwall(int damaskwallcnt) {
        int i, j, k, x, z, z1, z2, lx, rx;
        int sectnum;
        Sector sec, nsec;
        Wall wal;

        z = maskwall[damaskwallcnt];
        wal = boardService.getWall(thewall[z]);
        sectnum = thesector[z];
        sec = boardService.getSector(sectnum);
        nsec = boardService.getSector(wal.getNextsector());
        z1 = Math.max(nsec.getCeilingz(), sec.getCeilingz());
        z2 = Math.min(nsec.getFloorz(), sec.getFloorz());

        wallmost(uwall, z, sectnum, 0);
        wallmost(uplc, z, wal.getNextsector(), 0);
        for (x = xb1[z]; x <= xb2[z]; x++) {
            if (uplc[x] > uwall[x]) {
                uwall[x] = uplc[x];
            }
        }
        wallmost(dwall, z, sectnum, 1);
        wallmost(dplc, z, wal.getNextsector(), 1);
        for (x = xb1[z]; x <= xb2[z]; x++) {
            if (dplc[x] < dwall[x]) {
                dwall[x] = dplc[x];
            }
        }
        prepwall(z, wal);

        globalorientation = wal.getCstat();
        globalpicnum = wal.getOverpicnum();
        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }
        globalxpanning = wal.getXpanning() & 0xFF;
        globalypanning = wal.getYpanning() & 0xFF;

        ArtEntry pic = getTile(globalpicnum);
        if (pic.getType() != AnimType.NONE) {
            globalpicnum += (short) animateoffs(globalpicnum, thewall[z] + 16384);
            pic = getTile(globalpicnum);
        }
        globalshade = wal.getShade();
        globvis = globalvisibility;
        if (sec.getVisibility() != 0) {
            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
        }
        globalpal = wal.getPal();
        globalshiftval = (short) pic.getSizey();
        if (pow2long[globalshiftval] != pic.getHeight()) {
            globalshiftval++;
        }
        globalshiftval = (short) (32 - globalshiftval);
        globalyscale = (wal.getYrepeat() << (globalshiftval - 19));
        if ((globalorientation & 4) == 0) {
            globalzd = (((globalposz - z1) * globalyscale) << 8);
        } else {
            globalzd = (((globalposz - z2) * globalyscale) << 8);
        }
        globalzd += (globalypanning << 24);
        if ((globalorientation & 256) != 0) {
            globalyscale = -globalyscale;
            globalzd = -globalzd;
        }

        for (i = smostwallcnt - 1; i >= 0; i--) {
            j = smostwall[i];
            if ((xb1[j] > xb2[z]) || (xb2[j] < xb1[z])) {
                continue;
            }
            if (wallfront(j, z) != 0) {
                continue;
            }

            lx = Math.max(xb1[j], xb1[z]);
            rx = Math.min(xb2[j], xb2[z]);

            switch (smostwalltype[i]) {
                case 0:
                    if (lx <= rx) {
                        if ((lx == xb1[z]) && (rx == xb2[z])) {
                            return;
                        }
                        // clearbufbyte(&dwall[lx],(rx-lx+1)*sizeof(dwall[0]),0L);
                        for (x = lx; x <= rx; x++) {
                            dwall[x] = 0;
                        }
                    }
                    break;
                case 1:
                    k = smoststart[i] - xb1[j];
                    for (x = lx; x <= rx; x++) {
                        if (smost[k + x] > uwall[x]) {
                            uwall[x] = smost[k + x];
                        }
                    }
                    break;
                case 2:
                    k = smoststart[i] - xb1[j];
                    for (x = lx; x <= rx; x++) {
                        if (smost[k + x] < dwall[x]) {
                            dwall[x] = smost[k + x];
                        }
                    }
                    break;
            }
        }

        // maskwall
        if ((globalorientation & 128) == 0) {
            maskwallscan(xb1[z], xb2[z], uwall, dwall, swall, lwall);
        } else {
            if ((globalorientation & 128) != 0) {
                if ((globalorientation & 512) != 0) {
                    a.settransreverse();
                } else {
                    a.settransnormal();
                }
            }
            transmaskwallscan(xb1[z], xb2[z]);
        }
    }

    private void transmaskwallscan(int x1, int x2) {
        setgotpic(globalpicnum);
        ArtEntry pic = getTile(globalpicnum);

        if (!pic.hasSize()) {
            return;
        }

        a.setuptvlineasm(globalshiftval);

        int x = x1;
        while ((startumost[x + windowx1] > startdmost[x + windowx1]) && (x <= x2)) {
            x++;
        }

        while (x <= x2) {
            transmaskvline(x);
            x++;
        }
    }

    private void transmaskvline(int x) {
        if ((x < 0) || (x >= xdimen)) {
            return;
        }

        int y1v = Math.max(uwall[x], startumost[x + windowx1] - windowy1);
        int y2v = Math.min(dwall[x], startdmost[x + windowx1] - windowy1);
        y2v--;
        if (y2v < y1v) {
            return;
        }

        int vinc = swall[x] * globalyscale;
        long vplc = globalzd + (vinc & 0xFFFFFFFFL) * (y1v - (int) globalhoriz + 1);

        ArtEntry pic = getTile(globalpicnum);
        byte[] bufplc = getTileBuffer(pic);

        int i = lwall[x] + globalxpanning;
        if (i >= pic.getWidth()) {
            i %= pic.getWidth();
        }
        int bufoffs = i * pic.getHeight();

        int p = ylookup[y1v] + x + frameoffset;

        a.tvlineasm1(vinc, getPalookupBuffer(globalpal), (paletteManager.getPalookup(mulscale(swall[x], globvis, 16), globalshade) << 8),
                y2v - y1v, vplc, bufplc, bufoffs, p);
    }

    private void prepwall(int z, Wall wal) {
        int l = 0, ol = 0;

        int walxrepeat = (wal.getXrepeat() << 3);

        // lwall calculation
        int i = (xb1[z] - halfxdimen);
        int topinc = -(ry1[z] >> 2);
        int botinc = ((ry2[z] - ry1[z]) >> 8);
        int top = (mulscale(rx1[z], xdimen, 5) + mulscale(topinc, i, 2));
        int bot = (mulscale(rx1[z] - rx2[z], xdimen, 11) + mulscale(botinc, i, 2));

        int splc = mulscale(ry1[z], xdimscale, 19);
        int sinc = mulscale(ry2[z] - ry1[z], xdimscale, 16);

        int x = xb1[z];
        if (bot != 0) {
            l = divscale(top, bot, 12);
            swall[x] = mulscale(l, sinc, 21) + splc;
            l *= walxrepeat;
            lwall[x] = (l >> 18);
        }
        while (x + 4 <= xb2[z]) {
            top += topinc;
            bot += botinc;
            if (bot != 0) {
                ol = l;
                l = divscale(top, bot, 12);
                swall[x + 4] = mulscale(l, sinc, 21) + splc;
                l *= walxrepeat;
                lwall[x + 4] = (l >> 18);
            }
            i = ((ol + l) >> 1);
            lwall[x + 2] = (i >> 18);
            lwall[x + 1] = ((ol + i) >> 19);
            lwall[x + 3] = ((l + i) >> 19);
            swall[x + 2] = ((swall[x] + swall[x + 4]) >> 1);
            swall[x + 1] = ((swall[x] + swall[x + 2]) >> 1);
            swall[x + 3] = ((swall[x + 4] + swall[x + 2]) >> 1);
            x += 4;
        }
        if (x + 2 <= xb2[z]) {
            top += (topinc >> 1);
            bot += (botinc >> 1);
            if (bot != 0) {
                ol = l;
                l = divscale(top, bot, 12);
                swall[x + 2] = mulscale(l, sinc, 21) + splc;
                l *= walxrepeat;
                lwall[x + 2] = (l >> 18);
            }
            lwall[x + 1] = ((l + ol) >> 19);
            swall[x + 1] = ((swall[x] + swall[x + 2]) >> 1);
            x += 2;
        }
        if (x + 1 <= xb2[z]) {
            bot += (botinc >> 2);
            if (bot != 0) {
                l = divscale(top + (topinc >> 2), bot, 12);
                swall[x + 1] = mulscale(l, sinc, 21) + splc;
                lwall[x + 1] = mulscale(l, walxrepeat, 18);
            }
        }

        if (lwall[xb1[z]] < 0) {
            lwall[xb1[z]] = 0;
        }
        if ((lwall[xb2[z]] >= walxrepeat) && (walxrepeat != 0)) {
            lwall[xb2[z]] = walxrepeat - 1;
        }
        if ((wal.getCstat() & 8) != 0) {
            walxrepeat--;
            for (x = xb1[z]; x <= xb2[z]; x++) {
                lwall[x] = walxrepeat - lwall[x];
            }
        }
    }

    private void maskwallscan(int x1, int x2, short[] uwal, short[] dwal, int[] swal, int[] lwal) {
        int x, startx;
        int y1ve, y2ve, tsizx, tsizy, cnt;

        ArtEntry pic = getTile(globalpicnum);

        tsizx = pic.getWidth();
        tsizy = pic.getHeight();
        setgotpic(globalpicnum);
        if (!pic.hasSize()) {
            return;
        }
        if ((uwal[x1] > ydimen) && (uwal[x2] > ydimen)) {
            return;
        }
        if ((dwal[x1] < 0) && (dwal[x2] < 0)) {
            return;
        }

        byte[] picBuffer = getTileBuffer(pic);

        startx = x1;

        boolean xnice = (pow2long[pic.getSizex()] == tsizx);
        if (xnice) {
            tsizx = (tsizx - 1);
        }
        boolean ynice = (pow2long[pic.getSizey()] == tsizy);
        if (ynice) {
            tsizy = pic.getSizey();
        }

        a.setupmvlineasm(globalshiftval);

        int bufplce, shade, vince;
        long vplce;
        for (x = startx; x < x2; x++) {
            bufplce = lwal[x] + globalxpanning;
            if (bufplce < 0) {
                break;
            }

            y1ve = Math.max(uwal[x], startumost[x + windowx1] - windowy1);
            y2ve = Math.min(dwal[x], startdmost[x + windowx1] - windowy1);
            if (y2ve <= y1ve) {
                continue;
            }

            shade = (paletteManager.getPalookup(mulscale(swal[x], globvis, 16), globalshade) << 8);
            if (bufplce >= tsizx) {
                if (!xnice) {
                    bufplce %= tsizx;
                } else {
                    bufplce &= tsizx;
                }
            }
            if (!ynice) {
                bufplce *= tsizy;
            } else {
                bufplce <<= tsizy;
            }

            vince = swal[x] * globalyscale;
            vplce = globalzd + (vince & 0xFFFFFFFFL) * (y1ve - (int) globalhoriz + 1);
            cnt = y2ve - y1ve - 1;
            a.mvlineasm1(vince, getPalookupBuffer(globalpal), shade, cnt, vplce, picBuffer, bufplce,
                    x + frameoffset + ylookup[y1ve]);
        }
    }

    private void wallscan(int x1, int x2, short[] uwal, short[] dwal, int[] swal, int[] lwal) {
        int x, fpalookup;
        boolean ynice;
        boolean xnice;
        int y1ve, y2ve;

        ArtEntry pic = getTile(globalpicnum);

        int tsizx = pic.getWidth();
        int tsizy = pic.getHeight();
        setgotpic(globalpicnum);
        if (!pic.hasSize()) {
            return;
        }
        if (x1 >= uwal.length || x2 >= uwal.length || (uwal[x1] > ydimen) && (uwal[x2] > ydimen)) {
            return;
        }
        if (x1 >= dwal.length || x2 >= dwal.length || (dwal[x1] < 0) && (dwal[x2] < 0)) {
            return;
        }

        byte[] picBuffer = getTileBuffer(pic);

        xnice = (pow2long[pic.getSizex()] == tsizx);
        if (xnice) {
            tsizx--;
        }
        ynice = (pow2long[pic.getSizey()] == tsizy);
        if (ynice) {
            tsizy = pic.getSizey();
        }

        fpalookup = globalpal;

        a.setupvlineasm(globalshiftval);

        for (x = x1; x <= x2; x++) {
            y1ve = Math.max(uwal[x], umost[x]);
            y2ve = Math.min(dwal[x], dmost[x]);
            if (y2ve <= y1ve) {
                continue;
            }

            int shade = (paletteManager.getPalookup(mulscale(swal[x], globvis, 16), globalshade) << 8);

            int bufplce = lwal[x] + globalxpanning;
            if (bufplce >= tsizx) {
                if (!xnice) {
                    bufplce %= tsizx;
                } else {
                    bufplce &= tsizx;
                }
            }
            if (!ynice) {
                bufplce *= tsizy;
            } else {
                bufplce <<= tsizy;
            }

            int vince = swal[x] * globalyscale;
            long vplce = globalzd + (vince & 0xFFFFFFFFL) * (y1ve - (int) globalhoriz + 1);

            a.vlineasm1(vince, getPalookupBuffer(fpalookup), shade, y2ve - y1ve - 1, vplce, picBuffer, bufplce,
                    x + frameoffset + ylookup[y1ve]);
        }
    }

    private void florscan(int x1, int x2, int sectnum) {
        int i, j, ox, oy, x, y1, y2, twall, bwall;
        Sector sec;

        sec = boardService.getSector(sectnum);
        if (sec.getFloorpal() != globalpalwritten) {
            globalpalwritten = sec.getFloorpal();
            a.setpalookupaddress(getPalookupBuffer(globalpalwritten));
        }

        globalzd = globalposz - sec.getFloorz();
        if (globalzd > 0) {
            return;
        }
        globalpicnum = sec.getFloorpicnum();
        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }
        setgotpic(globalpicnum);
        ArtEntry pic = getTile(globalpicnum);

        if (!pic.hasSize()) {
            return;
        }
        if (pic.getType() != AnimType.NONE) {
            globalpicnum += (short) animateoffs(globalpicnum, sectnum);
            pic = getTile(globalpicnum);
        }

        globalbufplc = getTileBuffer(pic);

        globalshade = sec.getFloorshade();
        globvis = globalcisibility;
        if (sec.getVisibility() != 0) {
            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
        }
        globalorientation = sec.getFloorstat();

        if ((globalorientation & 64) == 0) {
            globalx1 = singlobalang;
            globalx2 = singlobalang;
            globaly1 = cosglobalang;
            globaly2 = cosglobalang;
            globalxpanning = (globalposx << 20);
            globalypanning = -(globalposy << 20);
        } else {
            j = sec.getWallptr();
            ox = boardService.getWall(boardService.getWall(j).getPoint2()).getX() - boardService.getWall(j).getX();
            oy = boardService.getWall(boardService.getWall(j).getPoint2()).getY() - boardService.getWall(j).getY();
            i = EngineUtils.sqrt(ox * ox + oy * oy);
            if (i == 0) {
                i = 1024;
            } else {
                i = 1048576 / i;
            }
            globalx1 = mulscale(dmulscale(ox, singlobalang, -oy, cosglobalang, 10), i, 10);
            globaly1 = mulscale(dmulscale(ox, cosglobalang, oy, singlobalang, 10), i, 10);
            globalx2 = -globalx1;
            globaly2 = -globaly1;

            ox = ((boardService.getWall(j).getX() - globalposx) << 6);
            oy = ((boardService.getWall(j).getY() - globalposy) << 6);
            i = dmulscale(oy, cosglobalang, -ox, singlobalang, 14);
            j = dmulscale(ox, cosglobalang, oy, singlobalang, 14);
            ox = i;
            oy = j;
            globalxpanning = (globalx1 * ox - globaly1 * oy);
            globalypanning = (globaly2 * ox + globalx2 * oy);
        }
        globalx2 = mulscale(globalx2, viewingrangerecip, 16);
        globaly1 = mulscale(globaly1, viewingrangerecip, 16);
        globalxshift = (byte) (8 - pic.getSizex());
        globalyshift = (byte) (8 - pic.getSizey());
        if ((globalorientation & 8) != 0) {
            globalxshift++;
            globalyshift++;
        }

        if ((globalorientation & 0x4) > 0) {
            i = globalxpanning;
            globalxpanning = globalypanning;
            globalypanning = i;
            i = globalx2;
            globalx2 = -globaly1;
            globaly1 = -i;
            i = globalx1;
            globalx1 = globaly2;
            globaly2 = i;
        }
        if ((globalorientation & 0x10) > 0) {
            globalx1 = -globalx1;
            globaly1 = -globaly1;
            globalxpanning = -globalxpanning;
        }
        if ((globalorientation & 0x20) > 0) {
            globalx2 = -globalx2;
            globaly2 = -globaly2;
            globalypanning = -globalypanning;
        }
        globalx1 <<= globalxshift;
        globaly1 <<= globalxshift;
        globalx2 <<= globalyshift;
        globaly2 <<= globalyshift;
        globalxpanning <<= globalxshift;
        globalypanning <<= globalyshift;
        globalxpanning += ((sec.getFloorxpanning()) << 24);
        globalypanning += ((sec.getFloorypanning()) << 24);
        globaly1 = (-globalx1 - globaly1) * halfxdimen;
        globalx2 = (globalx2 - globaly2) * halfxdimen;

        a.sethlinesizes(pic.getSizex(), pic.getSizey(), globalbufplc);

        globalx2 += globaly2 * (x1 - 1);
        globaly1 += globalx1 * (x1 - 1);
        globalx1 = mulscale(globalx1, globalzd, 16);
        globalx2 = mulscale(globalx2, globalzd, 16);
        globaly1 = mulscale(globaly1, globalzd, 16);
        globaly2 = mulscale(globaly2, globalzd, 16);
        globvis = klabs(mulscale(globvis, globalzd, 10));

        if ((globalorientation & 0x180) == 0) {
            y1 = Math.max(dplc[x1], umost[x1]);
            y2 = y1;
            for (x = x1; x <= x2; x++) {
                twall = Math.max(dplc[x], umost[x]) - 1;
                bwall = dmost[x];
                if (twall < bwall - 1) {
                    if (twall >= y2) {
                        while (y1 < y2 - 1) {
                            hline(x - 1, ++y1);
                        }
                        y1 = twall;
                    } else {
                        while (y1 < twall) {
                            hline(x - 1, ++y1);
                        }
                        while (y1 > twall) {
                            lastx[y1--] = x;
                        }
                    }
                    while (y2 > bwall) {
                        hline(x - 1, --y2);
                    }
                    while (y2 < bwall) {
                        lastx[y2++] = x;
                    }
                } else {
                    while (y1 < y2 - 1) {
                        hline(x - 1, ++y1);
                    }
                    if (x == x2) {
                        globalx2 += globaly2;
                        globaly1 += globalx1;
                        break;
                    }
                    y1 = Math.max(dplc[x + 1], umost[x + 1]);
                    y2 = y1;
                }
                globalx2 += globaly2;
                globaly1 += globalx1;
            }
            while (y1 < y2 - 1) {
                hline(x2, ++y1);
            }
            return;
        }

        switch (globalorientation & 0x180) {
            case 128:
                a.msethlineshift(pic.getSizex(), pic.getSizey());
                break;
            case 256:
                a.settransnormal();
                a.tsethlineshift(pic.getSizex(), pic.getSizey());
                break;
            case 384:
                a.settransreverse();
                a.tsethlineshift(pic.getSizex(), pic.getSizey());
                break;
        }

        y1 = Math.max(dplc[x1], umost[x1]);
        y2 = y1;
        for (x = x1; x <= x2; x++) {
            twall = Math.max(dplc[x], umost[x]) - 1;
            bwall = dmost[x];
            if (twall < bwall - 1) {
                if (twall >= y2) {
                    while (y1 < y2 - 1) {
                        slowhline(x - 1, ++y1);
                    }
                    y1 = twall;
                } else {
                    while (y1 < twall) {
                        slowhline(x - 1, ++y1);
                    }
                    while (y1 > twall) {
                        lastx[y1--] = x;
                    }
                }
                while (y2 > bwall) {
                    slowhline(x - 1, --y2);
                }
                while (y2 < bwall) {
                    lastx[y2++] = x;
                }
            } else {
                while (y1 < y2 - 1) {
                    slowhline(x - 1, ++y1);
                }
                if (x == x2) {
                    globalx2 += globaly2;
                    globaly1 += globalx1;
                    break;
                }
                y1 = Math.max(dplc[x + 1], umost[x + 1]);
                y2 = y1;
            }
            globalx2 += globaly2;
            globaly1 += globalx1;
        }
        while (y1 < y2 - 1) {
            slowhline(x2, ++y1);
        }
    }

    private void parascan(/*int dax1, int dax2, */int dastat, int bunch) {
        Sector sec;
        int j, k, l, m, n, x, z, wallnum, nextsectnum, globalhorizbak;
        short[] topptr, botptr;

        int sectnum = thesector[bunchfirst[bunch]];
        sec = boardService.getSector(sectnum);
        globalhorizbak = (int) globalhoriz;
        if (parallaxyscale != 65536) {
            globalhoriz = mulscale((int) globalhoriz - (ydimen >> 1), parallaxyscale, 16) + (ydimen >> 1);
        }
        globvis = globalpisibility;

        if (sec.getVisibility() != 0) {
            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
        }

        if (dastat == 0) {
            globalpal = sec.getCeilingpal();
            globalpicnum = sec.getCeilingpicnum();
            globalshade = sec.getCeilingshade();
            globalxpanning = sec.getCeilingxpanning();
            globalypanning = sec.getCeilingypanning();
            topptr = umost;
            botptr = uplc;
        } else {
            globalpal = sec.getFloorpal();
            globalpicnum = sec.getFloorpicnum();
            globalshade = sec.getFloorshade();
            globalxpanning = sec.getFloorxpanning();
            globalypanning = sec.getFloorypanning();
            topptr = dplc;
            botptr = dmost;
        }

        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }

        ArtEntry pic = getTile(globalpicnum);
        if (pic.getType() != AnimType.NONE) {
            globalpicnum += (short) animateoffs(globalpicnum, sectnum);
            pic = getTile(globalpicnum);
        }

        globalshiftval = (short) (pic.getSizey());
        if (pow2long[globalshiftval] != pic.getHeight()) {
            globalshiftval++;
        }
        globalshiftval = (short) (32 - globalshiftval);
        globalzd = (((pic.getHeight() >> 1) + parallaxyoffs) << globalshiftval) + (globalypanning << 24);
        globalyscale = (8 << (globalshiftval - 19));

        k = 11 - (pic.getSizex()) - pskybits;
        x = -1;

        for (z = bunchfirst[bunch]; z >= 0; z = p2[z]) {
            wallnum = thewall[z];
            nextsectnum = boardService.getWall(wallnum).getNextsector();

            j = 0;
            if (nextsectnum != -1) {
                if (dastat == 0) {
                    j = boardService.getSector(nextsectnum).getCeilingstat();
                } else {
                    j = boardService.getSector(nextsectnum).getFloorstat();
                }
            }

            if ((nextsectnum < 0) || ((boardService.getWall(wallnum).getCstat() & 32) != 0) || ((j & 1) == 0)) {
                if (x == -1) {
                    x = xb1[z];
                }
                if (parallaxtype == 0) {
                    n = mulscale(xdimenrecip, viewingrange, 16);
                    for (j = xb1[z]; j <= xb2[z]; j++) {
                        lplc[j] = (((mulscale(j - halfxdimen, n, 23) + (int) globalang) & 2047) >> k);
                    }
                } else {
                    for (j = xb1[z]; j <= xb2[z]; j++) {
                        lplc[j] = (((radarang2[j] + (int) globalang) & 2047) >> k);
                    }
                }

                if (parallaxtype == 2) {
                    n = mulscale(xdimscale, viewingrange, 16);
                    for (j = xb1[z]; j <= xb2[z]; j++) {
                        swplc[j] = mulscale(EngineUtils.cos(radarang2[j]), n, 14);
                    }
                } else {
                    Arrays.fill(swplc, xb1[z], xb2[z] + 1, mulscale(xdimscale, viewingrange, 16));
                }
            } else if (x >= 0) {
                l = globalpicnum;
                m = (pic.getSizex());
                globalpicnum = (short) (l + pskyoff[(lplc[x] >> m)]);

                if (((lplc[x] ^ lplc[(xb1[z] - 1)]) >> m) == 0) {
                    wallscan(x, xb1[z] - 1, topptr, botptr, swplc, lplc);
                } else {
                    j = x;
                    while (x < xb1[z]) {
                        n = l + pskyoff[(lplc[x] >> m)];
                        if (n != globalpicnum) {
                            wallscan(j, x - 1, topptr, botptr, swplc, lplc);
                            j = x;
                            globalpicnum = (short) n;
                        }
                        x++;
                    }
                    if (j < x) {
                        wallscan(j, x - 1, topptr, botptr, swplc, lplc);
                    }
                }

                globalpicnum = (short) l;
                x = -1;
            }
        }

        if (x >= 0) {
            l = globalpicnum;
            m = (pic.getSizex());
            globalpicnum = (short) (l + pskyoff[(lplc[x] >> m)]);
            if (((lplc[x] ^ lplc[xb2[bunchlast[bunch]]]) >> m) == 0) {
                wallscan(x, xb2[bunchlast[bunch]], topptr, botptr, swplc, lplc);
            } else {
                j = x;
                while (x <= xb2[bunchlast[bunch]]) {
                    n = l + pskyoff[(lplc[x] >> m)];
                    if (n != globalpicnum) {
                        wallscan(j, x - 1, topptr, botptr, swplc, lplc);
                        j = x;
                        globalpicnum = (short) n;
                    }
                    x++;
                }
                if (j <= x) {
                    wallscan(j, x, topptr, botptr, swplc, lplc);
                }
            }
            globalpicnum = (short) l;
        }
        globalhoriz = globalhorizbak;
    }

    private void ceilspritescan(int x1, int x2) {
        int x, y1, y2, twall, bwall;

        y1 = uwall[x1];
        y2 = y1;
        for (x = x1; x <= x2; x++) {
            twall = uwall[x] - 1;
            bwall = dwall[x];
            if (twall < bwall - 1) {
                if (twall >= y2) {
                    while (y1 < y2 - 1) {
                        ceilspritehline(x - 1, ++y1);
                    }
                    y1 = twall;
                } else {
                    while (y1 < twall) {
                        ceilspritehline(x - 1, ++y1);
                    }
                    while (y1 > twall) {
                        lastx[y1--] = x;
                    }
                }
                while (y2 > bwall) {
                    ceilspritehline(x - 1, --y2);
                }
                while (y2 < bwall) {
                    lastx[y2++] = x;
                }
            } else {
                while (y1 < y2 - 1) {
                    ceilspritehline(x - 1, ++y1);
                }
                if (x == x2) {
                    break;
                }
                y1 = uwall[x + 1];
                y2 = y1;
            }
        }
        while (y1 < y2 - 1) {
            ceilspritehline(x2, ++y1);
        }
    }

    private void ceilspritehline(int x2, int y) {
        int x1, v, bx, by;

        // x = x1 + (x2-x1)t + (y1-y2)u � x = 160v
        // y = y1 + (y2-y1)t + (x2-x1)u � y = (scrx-160)v
        // z = z1 = z2 � z = posz + (scry-horiz)v

        x1 = lastx[y];
        if (x2 < x1) {
            return;
        }

        if (y - (int) globalhoriz + horizycent < 0 || y - (int) globalhoriz + horizycent >= lookups.length) {
            Console.out.println("Crash catcher:");
            Console.out.println("y = " + y);
            Console.out.println("globalhoriz = " + globalhoriz);
            Console.out.println("horizycent = " + horizycent);
            Console.out.println("globalzd = " + globalzd);
        }

        v = mulscale(globalzd, lookups[y - (int) globalhoriz + horizycent], 20);
        bx = mulscale((long) globalx2 * x1 + globalx1, v, 14) + globalxpanning;
        by = mulscale((long) globaly2 * x1 + globaly1, v, 14) + globalypanning;

        a.sethlineincs(mulscale(globalx2, v, 14), mulscale(globaly2, v, 14));
        a.setuphline(getPalookupBuffer(globalpal), paletteManager.getPalookup(mulscale(klabs(v), globvis, 28), globalshade) << 8);

        if ((globalorientation & 2) == 0) {
            a.mhline(globalbufplc, bx, (x2 - x1) << 16, 0, by, ylookup[y] + x1 + frameoffset);
        } else {
            a.thline(globalbufplc, bx, (x2 - x1) << 16, 0, by, ylookup[y] + x1 + frameoffset);
        }
    }

    private void grouscan(int dax1, int dax2, int sectnum, int dastat) {
        int i, j, l, x, y, dx, dy, wx, wy, y1, y2, daz;
        int daslope, dasqr;
        int shoffs, shinc, m1, m2;
        int mptr1, mptr2, nptr1, nptr2;
        Wall wal;
        Sector sec = boardService.getSector(sectnum);

        if (dastat == 0) {
            if (globalposz <= engine.getceilzofslope(sectnum, globalposx, globalposy)) {
                return; // Back-face culling
            }
            globalorientation = sec.getCeilingstat();
            globalpicnum = sec.getCeilingpicnum();
            globalshade = sec.getCeilingshade();
            globalpal = sec.getCeilingpal();
            daslope = sec.getCeilingheinum();
            daz = sec.getCeilingz();
        } else {
            if (globalposz >= engine.getflorzofslope(sectnum, globalposx, globalposy)) {
                return; // Back-face culling
            }
            globalorientation = sec.getFloorstat();
            globalpicnum = sec.getFloorpicnum();
            globalshade = sec.getFloorshade();
            globalpal = sec.getFloorpal();
            daslope = sec.getFloorheinum();
            daz = sec.getFloorz();
        }

        ArtEntry pic = getTile(globalpicnum);
        if (pic.getType() != AnimType.NONE) {
            globalpicnum += (short) animateoffs(globalpicnum, sectnum);
            pic = getTile(globalpicnum);
        }

        setgotpic(globalpicnum);
        if (!pic.hasSize()) {
            return;
        }

        byte[] picBuffer = getTileBuffer(pic);

        wal = boardService.getWall(sec.getWallptr());
        wx = boardService.getWall(wal.getPoint2()).getX() - wal.getX();
        wy = boardService.getWall(wal.getPoint2()).getY() - wal.getY();
        dasqr = a.krecipasm(EngineUtils.sqrt(wx * wx + wy * wy));
        i = mulscale(daslope, dasqr, 21);
        wx *= i;
        wy *= i;

        globalx = -mulscale(singlobalang, xdimenrecip, 19);
        globaly = mulscale(cosglobalang, xdimenrecip, 19);
        globalx1 = (globalposx << 8);
        globaly1 = -(globalposy << 8);
        i = (dax1 - halfxdimen) * xdimenrecip;
        globalx2 = mulscale((long) cosglobalang << 4, viewingrangerecip, 16) - mulscale(singlobalang, i, 27);
        globaly2 = mulscale((long) singlobalang << 4, viewingrangerecip, 16) + mulscale(cosglobalang, i, 27);

        globalzd = (xdimscale << 9);
        globalzx = -dmulscale(wx, globaly2, -wy, globalx2, 17) + mulscale(1 - (int) globalhoriz, globalzd, 10);
        globalz = -dmulscale(wx, globaly, -wy, globalx, 25);

        if ((globalorientation & 64) != 0) // Relative alignment
        {
            dx = mulscale(boardService.getWall(wal.getPoint2()).getX() - wal.getX(), dasqr, 14);
            dy = mulscale(boardService.getWall(wal.getPoint2()).getY() - wal.getY(), dasqr, 14);

            i = EngineUtils.sqrt(daslope * daslope + 16777216);

            x = globalx;
            y = globaly;
            globalx = dmulscale(x, dx, y, dy, 16);
            globaly = mulscale(dmulscale(-y, dx, x, dy, 16), i, 12);
            x = ((wal.getX() - globalposx) << 8);
            y = ((wal.getY() - globalposy) << 8);
            globalx1 = dmulscale(-x, dx, -y, dy, 16);
            globaly1 = mulscale(dmulscale(-y, dx, x, dy, 16), i, 12);
            x = globalx2;
            y = globaly2;
            globalx2 = dmulscale(x, dx, y, dy, 16);
            globaly2 = mulscale(dmulscale(-y, dx, x, dy, 16), i, 12);
        }
        if ((globalorientation & 0x4) != 0) {
            i = globalx;
            globalx = -globaly;
            globaly = -i;
            i = globalx1;
            globalx1 = globaly1;
            globaly1 = i;
            i = globalx2;
            globalx2 = -globaly2;
            globaly2 = -i;
        }
        if ((globalorientation & 0x10) != 0) {
            globalx1 = -globalx1;
            globalx2 = -globalx2;
            globalx = -globalx;
        }
        if ((globalorientation & 0x20) != 0) {
            globaly1 = -globaly1;
            globaly2 = -globaly2;
            globaly = -globaly;
        }

        daz = dmulscale(wx, globalposy - wal.getY(), -wy, globalposx - wal.getX(), 9) + ((daz - globalposz) << 8);
        globalx2 = mulscale(globalx2, daz, 20);
        globalx = mulscale(globalx, daz, 28);
        globaly2 = mulscale(globaly2, -daz, 20);
        globaly = mulscale(globaly, -daz, 28);

        i = 8 - (pic.getSizex());
        j = 8 - (pic.getSizey());
        if ((globalorientation & 8) != 0) {
            i++;
            j++;
        }
        globalx1 <<= (i + 12);
        globalx2 <<= i;
        globalx <<= i;
        globaly1 <<= (j + 12);
        globaly2 <<= j;
        globaly <<= j;

        if (dastat == 0) {
            globalx1 += ((sec.getCeilingxpanning()) << 24);
            globaly1 += ((sec.getCeilingypanning()) << 24);
        } else {
            globalx1 += ((sec.getFloorxpanning()) << 24);
            globaly1 += ((sec.getFloorypanning()) << 24);
        }

        globvis = globalvisibility;
        if (sec.getVisibility() != 0) {
            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
        }
        globvis = mulscale(globvis, daz, 13);
        globvis = mulscale(globvis, xdimscale, 16);

        j = globalpal;
        a.setupslopevlin((pic.getSizex()) + (((pic.getSizey())) << 8), picBuffer, -ylookup[1],
                -(globalzd >> (16 - BITSOFPRECISION)));

        l = (globalzd >> 16);

        shinc = mulscale(globalz, xdimenscale, 16);
        if (shinc > 0) {
            shoffs = (4 << 15);
        } else {
            shoffs = ((16380 - ydimen) << 15); // JBF: was 2044
        }
        if (dastat == 0) {
            y1 = umost[dax1];
        } else {
            y1 = Math.max(umost[dax1], dplc[dax1]);
        }
        m1 = mulscale(y1, globalzd, 16) + (globalzx >> 6);
        // Avoid visibility overflow by crossing horizon
        if (globalzd > 0) {
            m1 += (globalzd >> 16);
        } else {
            m1 -= (globalzd >> 16);
        }
        m2 = m1 + l;
        mptr1 = y1 + (shoffs >> 15);
        mptr2 = mptr1 + 1;

        for (x = dax1; x <= dax2; x++) {
            if (dastat == 0) {
                y1 = umost[x];
                y2 = Math.min(dmost[x], uplc[x]) - 1;
            } else {
                y1 = Math.max(umost[x], dplc[x]);
                y2 = dmost[x] - 1;
            }
            if (y1 <= y2) {
                nptr1 = y1 + (shoffs >> 15);
                nptr2 = y2 + (shoffs >> 15);

                while (nptr1 <= mptr1) {
                    slopalookup[mptr1--] = paletteManager.getPalookup(mulscale(a.krecipasm(m1), globvis, 24), globalshade) << 8;
                    m1 -= l;
                }
                while (nptr2 >= mptr2) {
                    slopalookup[mptr2++] = paletteManager.getPalookup(mulscale(a.krecipasm(m2), globvis, 24), globalshade) << 8;
                    m2 += l;
                }

                globalx3 = globalx2 >> 10;
                globaly3 = globaly2 >> 10;
                a.slopevlin(ylookup[y2] + x + frameoffset, getPalookupBuffer(j), nptr2, y2 - y1 + 1, globalx1, globaly1,
                        globalx3, globaly3, slopalookup, mulscale(y2, globalzd, 16) + (globalzx >> 6));
            }
            globalx2 += globalx;
            globaly2 += globaly;
            globalzx += globalz;
            shoffs += shinc;
        }
    }

    private void ceilscan(int x1, int x2, int sectnum) {
        int i, j, ox, oy, x, y1, y2, twall, bwall;
        Sector sec;

        sec = boardService.getSector(sectnum);
        if (sec.getCeilingpal() != globalpalwritten) {
            globalpalwritten = sec.getCeilingpal();
            a.setpalookupaddress(getPalookupBuffer(globalpalwritten));
        }

        globalzd = sec.getCeilingz() - globalposz;
        if (globalzd > 0) {
            return;
        }
        globalpicnum = sec.getCeilingpicnum();
        if (globalpicnum >= MAXTILES) {
            globalpicnum = 0;
        }

        ArtEntry pic = getTile(globalpicnum);
        setgotpic(globalpicnum);
        if (!pic.hasSize()) {
            return;
        }
        if (pic.getType() != AnimType.NONE) {
            globalpicnum += (short) animateoffs(globalpicnum, sectnum);
            pic = getTile(globalpicnum);
        }

        globalbufplc = getTileBuffer(pic);

        globalshade = sec.getCeilingshade();
        globvis = globalcisibility;
        if (sec.getVisibility() != 0) {
            globvis = mulscale(globvis, (sec.getVisibility() + 16) & 0xFF, 4);
        }
        globalorientation = sec.getCeilingstat();

        if ((globalorientation & 64) == 0) {
            globalx1 = singlobalang;
            globalx2 = singlobalang;
            globaly1 = cosglobalang;
            globaly2 = cosglobalang;
            globalxpanning = (globalposx << 20);
            globalypanning = -(globalposy << 20);
        } else {
            j = sec.getWallptr();
            ox = boardService.getWall(boardService.getWall(j).getPoint2()).getX() - boardService.getWall(j).getX();
            oy = boardService.getWall(boardService.getWall(j).getPoint2()).getY() - boardService.getWall(j).getY();
            i = EngineUtils.sqrt(ox * ox + oy * oy);
            if (i == 0) {
                i = 1024;
            } else {
                i = 1048576 / i;
            }
            globalx1 = mulscale(dmulscale(ox, singlobalang, -oy, cosglobalang, 10), i, 10);
            globaly1 = mulscale(dmulscale(ox, cosglobalang, oy, singlobalang, 10), i, 10);
            globalx2 = -globalx1;
            globaly2 = -globaly1;

            ox = ((boardService.getWall(j).getX() - globalposx) << 6);
            oy = ((boardService.getWall(j).getY() - globalposy) << 6);
            i = dmulscale(oy, cosglobalang, -ox, singlobalang, 14);
            j = dmulscale(ox, cosglobalang, oy, singlobalang, 14);
            ox = i;
            oy = j;
            globalxpanning = (globalx1 * ox - globaly1 * oy);
            globalypanning = (globaly2 * ox + globalx2 * oy);
        }
        globalx2 = mulscale(globalx2, viewingrangerecip, 16);
        globaly1 = mulscale(globaly1, viewingrangerecip, 16);
        globalxshift = (byte) (8 - (pic.getSizex()));
        globalyshift = (byte) (8 - (pic.getSizey()));
        if ((globalorientation & 8) != 0) {
            globalxshift++;
            globalyshift++;
        }

        if ((globalorientation & 0x4) > 0) {
            i = globalxpanning;
            globalxpanning = globalypanning;
            globalypanning = i;
            i = globalx2;
            globalx2 = -globaly1;
            globaly1 = -i;
            i = globalx1;
            globalx1 = globaly2;
            globaly2 = i;
        }
        if ((globalorientation & 0x10) > 0) {
            globalx1 = -globalx1;
            globaly1 = -globaly1;
            globalxpanning = -globalxpanning;
        }
        if ((globalorientation & 0x20) > 0) {
            globalx2 = -globalx2;
            globaly2 = -globaly2;
            globalypanning = -globalypanning;
        }
        globalx1 <<= globalxshift;
        globaly1 <<= globalxshift;
        globalx2 <<= globalyshift;
        globaly2 <<= globalyshift;
        globalxpanning <<= globalxshift;
        globalypanning <<= globalyshift;
        globalxpanning += ((sec.getCeilingxpanning()) << 24);
        globalypanning += ((sec.getCeilingypanning()) << 24);
        globaly1 = (-globalx1 - globaly1) * halfxdimen;
        globalx2 = (globalx2 - globaly2) * halfxdimen;

        a.sethlinesizes(pic.getSizex(), pic.getSizey(), globalbufplc);

        globalx2 += globaly2 * (x1 - 1);
        globaly1 += globalx1 * (x1 - 1);
        globalx1 = mulscale(globalx1, globalzd, 16);
        globalx2 = mulscale(globalx2, globalzd, 16);
        globaly1 = mulscale(globaly1, globalzd, 16);
        globaly2 = mulscale(globaly2, globalzd, 16);
        globvis = klabs(mulscale(globvis, globalzd, 10));

        if ((globalorientation & 0x180) == 0) {
            y1 = umost[x1];
            y2 = y1;
            for (x = x1; x <= x2; x++) {
                twall = umost[x] - 1;
                bwall = Math.min(uplc[x], dmost[x]);
                if (twall < bwall - 1) {
                    if (twall >= y2) {
                        while (y1 < y2 - 1) {
                            hline(x - 1, ++y1);
                        }
                        y1 = twall;
                    } else {
                        while (y1 < twall) {
                            hline(x - 1, ++y1);
                        }
                        while (y1 > twall) {
                            lastx[y1--] = x;
                        }
                    }
                    while (y2 > bwall) {
                        hline(x - 1, --y2);
                    }
                    while (y2 < bwall) {
                        lastx[y2++] = x;
                    }
                } else {
                    while (y1 < y2 - 1) {
                        hline(x - 1, ++y1);
                    }
                    if (x == x2) {
                        globalx2 += globaly2;
                        globaly1 += globalx1;
                        break;
                    }
                    y1 = umost[x + 1];
                    y2 = y1;
                }
                globalx2 += globaly2;
                globaly1 += globalx1;
            }
            while (y1 < y2 - 1) {
                hline(x2, ++y1);
            }
            return;
        }

        int forswitch = globalorientation & 0x180;
        switch (forswitch) {
            case 128:
                a.msethlineshift(pic.getSizex(), pic.getSizey());
                break;
            case 256:
                a.settransnormal();
                a.tsethlineshift(pic.getSizex(), pic.getSizey());
                break;
            case 384:
                a.settransreverse();
                a.tsethlineshift(pic.getSizex(), pic.getSizey());
                break;
        }

        y1 = umost[x1];
        y2 = y1;
        for (x = x1; x <= x2; x++) {
            twall = umost[x] - 1;
            bwall = Math.min(uplc[x], dmost[x]);
            if (twall < bwall - 1) {
                if (twall >= y2) {
                    while (y1 < y2 - 1) {
                        slowhline(x - 1, ++y1);
                    }
                    y1 = twall;
                } else {
                    while (y1 < twall) {
                        slowhline(x - 1, ++y1);
                    }
                    while (y1 > twall) {
                        lastx[y1--] = x;
                    }
                }
                while (y2 > bwall) {
                    slowhline(x - 1, --y2);
                }
                while (y2 < bwall) {
                    lastx[y2++] = x;
                }
            } else {
                while (y1 < y2 - 1) {
                    slowhline(x - 1, ++y1);
                }
                if (x == x2) {
                    globalx2 += globaly2;
                    globaly1 += globalx1;
                    break;
                }
                y1 = umost[x + 1];
                y2 = y1;
            }
            globalx2 += globaly2;
            globaly1 += globalx1;
        }
        while (y1 < y2 - 1) {
            slowhline(x2, ++y1);
        }
    }

    @Override
    public void clearview(int dacol) {
        a.clearframe((byte) dacol);
    }

    @Override
    public void nextpage() {
        super.nextpage();
        changeListener.onNextPage(a.getframeplace());
    }

    @Override
    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                             int cy1, int cx2, int cy2) {
        ortho.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
    }

    protected void qinterpolatedown16short(short[] bufptr, int offset, int num, long val, long add) { // ...maybe the
        // same person
        // who provided
        // this too?
        int len = num + offset;
        if (offset < 0 || len == 0 || len >= bufptr.length) {
            return;
        }
        for (int i = offset; i < len; i++) {
            bufptr[i] = (short) (val >> 16);
            val += add;
        }
    }

    protected void qinterpolatedown16(int[] bufptr, int offset, int num, long val, long add) { // gee, I wonder who
        // could have provided
        // this...
        int len = num + offset;
        if (offset < 0 || len == 0 || len >= bufptr.length) {
            return;
        }

        for (int i = offset; i < len; i++) {
            bufptr[i] = (int) (val >> 16);
            val += add;
        }
    }

    @Override
    public void drawmapview(int dax, int day, int zoome, int ang) {
        Arrays.fill(gotsector, (byte) 0);
        ortho.drawmapview(dax, day, zoome, ang);
    }

    @Override
    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) {
        ortho.drawoverheadmap(boardService, cposx, cposy, czoom, cang);
    }

    @Override
    public int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        return ortho.printext(font, x, y, text, scale, shade, palnum, align, transparent, shadow);
    }

    @Override
    public ByteBuffer getFrame(PixelFormat format, int xsiz, int ysiz) {
        if (ysiz < 0) {
            ysiz *= -1;
        }

        byte[] frameplace = a.getframeplace();
        if (format == PixelFormat.Pal8) {
            if (indexbuffer != null) {
                indexbuffer.clear();
            }
            if (indexbuffer == null || indexbuffer.capacity() < xsiz * ysiz) {
                indexbuffer = ByteBuffer.allocateDirect(xsiz * ysiz);
            }

            indexbuffer.put(frameplace);
            indexbuffer.rewind();
            return indexbuffer;
        } else if (format == PixelFormat.Rgb) {
            if (rgbbuffer != null) {
                rgbbuffer.clear();
            }
            if (rgbbuffer == null || rgbbuffer.capacity() < xsiz * ysiz * 3) {
                rgbbuffer = ByteBuffer.allocateDirect(xsiz * ysiz * 3);
            }

            Palette curpalette = paletteManager.getCurrentPalette();
            for (int i = 0; i < xsiz * ysiz; i++) {
                int dacol = frameplace[i] & 0xFF;
                rgbbuffer.put((byte) curpalette.getRed(dacol));
                rgbbuffer.put((byte) curpalette.getGreen(dacol));
                rgbbuffer.put((byte) curpalette.getBlue(dacol));
            }

            rgbbuffer.rewind();
            return rgbbuffer;
        }

        return null;
    }

    @Override
    public void drawline256(int x1, int y1, int x2, int y2, int c) {
        ortho.drawline256(x1, y1, x2, y2, c);
    }

    public void dosetaspect() {
        int i, j, k;

        if (xyaspect != oxyaspect) {
            oxyaspect = xyaspect;
            j = xyaspect * 320;
            lookups[horizlookup2 + horizycent - 1] = divscale(131072, j, 26);
            for (i = ydim * 4 - 1; i >= 0; i--) {
                if (i != (horizycent - 1)) {
                    lookups[i] = divscale(1, i - (horizycent - 1), 28);
                    lookups[horizlookup2 + i] = divscale(klabs(lookups[i]), j, 14);
                }
            }
        }

        if ((xdimen != oxdimen) || (viewingrange != oviewingrange)) {
            oxdimen = xdimen;
            oviewingrange = viewingrange;
            int xinc = mulscale(viewingrange * 320L, xdimenrecip, 32);
            int x = (640 << 16) - mulscale(xinc, xdimen, 1);


            Tables tables = EngineUtils.getTables();
            for (i = 0; i < xdimen; i++) {
                j = x & 65535;
                k = x >> 16;
                x += xinc;

                if (j != 0) {
                    j = mulscale(tables.getRadarAng(k + 1) - tables.getRadarAng(k), j, 16);
                }
                radarang2[i] = (short) ((tables.getRadarAng(k) + j) >> 6);
            }

            for (i = 1; i < 65536; i++) {
                distrecip[i] = divscale(xdimen, i, 20);
            }
            nytooclose = xdimen * 2100;
            nytoofar = 65536 * 16384 - 1048576;
        }
    }

    @Override
    public void settiltang(int tilt) {
    }

    @Override
    public void changepalette(byte[] palette) {
        super.changepalette(palette);
        changeListener.onChangePalette(palette);
        DEFAULT_SCREEN_FADE.set(0, 0, 0, 0);
    }

    private void scansector(int sectnum) {
        Wall wal, wal2;
        Sprite spr;
        int xs, ys, x1, y1, x2, y2, xp1, yp1, xp2 = 0, yp2 = 0, templong;
        int z, zz, startwall, endwall, numscansbefore, scanfirst, bunchfrst;
        short nextsectnum;
        BoardService service = engine.getBoardService();
        if (sectnum < 0) {
            return;
        }

        if (automapping != 0) {
            show2dsector.setBit(sectnum);
        }

        sectorborder[0] = sectnum;
        sectorbordercnt = 1;
        do {
            sectnum = sectorborder[--sectorbordercnt];

            for (ListNode<Sprite> node = service.getSectNode(sectnum); node != null; node = node.getNext()) {
                int z1 = node.getIndex();
                spr = node.get();
                if ((((spr.getCstat() & 0x8000) == 0) || (showinvisibility)) && (spr.getXrepeat() > 0) && (spr.getYrepeat() > 0)) {
                    xs = spr.getX() - globalposx;
                    ys = spr.getY() - globalposy;
                    if (((spr.getCstat() & 48) != 0) || (xs * cosglobalang + ys * singlobalang > 0)) {
                        addRenderedSprite(z1);
                    }
                }
            }

            gotsector[sectnum >> 3] |= (byte) pow2char[sectnum & 7];

            bunchfrst = numbunches;
            numscansbefore = numscans;

            if (boardService.getSector(sectnum) == null) {
                continue;
            }

            startwall = boardService.getSector(sectnum).getWallptr();
            endwall = startwall + boardService.getSector(sectnum).getWallnum();
            scanfirst = numscans;

            if (startwall < 0 || endwall < 0) {
                continue;
            }
            for (z = startwall; z < endwall; z++) {
                wal = boardService.getWall(z);
                if (wal == null || wal.getPoint2() < 0 || wal.getPoint2() >= MAXWALLS) {
                    continue;
                }
                nextsectnum = wal.getNextsector();

                wal2 = boardService.getWall(wal.getPoint2());
                if (wal2 == null) {
                    continue;
                }
                x1 = wal.getX() - globalposx;
                y1 = wal.getY() - globalposy;
                x2 = wal2.getX() - globalposx;
                y2 = wal2.getY() - globalposy;

                if ((nextsectnum >= 0) && ((wal.getCstat() & 32) == 0) && sectorbordercnt < sectorborder.length
                        && ((gotsector[nextsectnum >> 3] & pow2char[nextsectnum & 7]) == 0)) {
                    templong = x1 * y2 - x2 * y1;

                    if ((toUnsignedLong(templong) + 262144) < 524288) {
                        if (mulscale(templong, templong, 5) <= (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) {
                            sectorborder[sectorbordercnt++] = nextsectnum;
                        }
                    }
                }

                if ((z == startwall) || (boardService.getWall(z - 1).getPoint2() != z)) {
                    xp1 = dmulscale(y1, cosglobalang, -x1, singlobalang, 6);
                    yp1 = dmulscale(x1, cosviewingrangeglobalang, y1, sinviewingrangeglobalang, 6);
                } else {
                    xp1 = xp2;
                    yp1 = yp2;
                }
                xp2 = dmulscale(y2, cosglobalang, -x2, singlobalang, 6);
                yp2 = dmulscale(x2, cosviewingrangeglobalang, y2, sinviewingrangeglobalang, 6);

                do {
                    if ((yp1 >= 256) || (yp2 >= 256)) {
                        if (dmulscale(xp1, yp2, -xp2, yp1, 32) < 0) { // If wall's facing you
                            if (numscans >= MAXWALLSB - 1) {
                                break;
                            }

                            if (xp1 >= -yp1) {
                                if ((xp1 > yp1) || (yp1 == 0)) {
                                    break;
                                }
                                xb1[numscans] = halfxdimen + scale(xp1, halfxdimen, yp1);
                                if (xp1 >= 0) {
                                    xb1[numscans]++; // Fix for SIGNED divide
                                }
                                if (xb1[numscans] >= xdimen) {
                                    xb1[numscans] = xdimen - 1;
                                }
                                yb1[numscans] = yp1;
                            } else {
                                if (xp2 < -yp2) {
                                    break;
                                }
                                xb1[numscans] = 0;
                                templong = yp1 - yp2 + xp1 - xp2;
                                if (templong == 0) {
                                    break;
                                }
                                yb1[numscans] = yp1 + scale((yp2 - yp1), (xp1 + yp1), templong);
                            }

                            if (yb1[numscans] >= 256) {
                                if (xp2 <= yp2) {
                                    if ((xp2 < -yp2) || (yp2 == 0)) {
                                        break;
                                    }
                                    xb2[numscans] = halfxdimen + scale(xp2, halfxdimen, yp2) - 1;
                                    if (xp2 >= 0) {
                                        xb2[numscans]++; // Fix for SIGNED divide
                                    }
                                    if (xb2[numscans] >= xdimen) {
                                        xb2[numscans] = xdimen - 1;
                                    }
                                    yb2[numscans] = yp2;
                                } else {
                                    if (xp1 > yp1) {
                                        break;
                                    }
                                    xb2[numscans] = xdimen - 1;
                                    templong = xp2 - xp1 + yp1 - yp2;
                                    if (templong == 0) {
                                        break;
                                    }
                                    yb2[numscans] = yp1 + scale((yp2 - yp1), (yp1 - xp1), templong);
                                }

                                if ((yb2[numscans] >= 256) && (xb1[numscans] <= xb2[numscans])) {
                                    // Made it all the way!
                                    thesector[numscans] = sectnum;
                                    thewall[numscans] = (short) z;
                                    rx1[numscans] = xp1;
                                    ry1[numscans] = yp1;
                                    rx2[numscans] = xp2;
                                    ry2[numscans] = yp2;
                                    p2[numscans] = (short) (numscans + 1);
                                    numscans++;
                                }
                            }
                        }
                    }
                } while (false);

                if ((boardService.getWall(z).getPoint2() < z) && (scanfirst < numscans)) {
                    p2[numscans - 1] = (short) scanfirst;
                    scanfirst = numscans;
                }
            }

            for (z = numscansbefore; z < numscans; z++) {
                if (z >= MAXWALLSB || p2[z] >= MAXWALLSB) {
                    continue;
                }
                if ((boardService.getWall(thewall[z]).getPoint2() != thewall[p2[z]]) || (xb2[z] >= xb1[p2[z]])) {
                    bunchfirst[numbunches++] = p2[z];
                    p2[z] = -1;
                }
            }

            for (z = bunchfrst; z < numbunches; z++) {
                if (p2[z] >= MAXWALLSB) {
                    continue;
                }

                zz = bunchfirst[z];
                while(p2[zz] >= 0) {
                    zz = p2[zz];
                }
                bunchlast[z] = (short) zz;
            }
        } while (sectorbordercnt > 0);
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

    private int bunchfront(int b1, int b2) {
        int x1b1, x2b1, x1b2, x2b2, b1f, b2f, i;

        b1f = bunchfirst[b1];
        x1b1 = xb1[b1f];
        x2b2 = (xb2[bunchlast[b2]] + 1);
        if (x1b1 >= x2b2) {
            return (-1);
        }
        b2f = bunchfirst[b2];
        x1b2 = xb1[b2f];
        x2b1 = (xb2[bunchlast[b1]] + 1);
        if (x1b2 >= x2b1) {
            return (-1);
        }

        if (x1b1 >= x1b2) {
            i = b2f;
            while (xb2[i] <= x1b1 && p2[i] != -1) {
                i = p2[i];
            }
            return (wallfront(b1f, i));
        }

        i = b1f;
        while (xb2[i] <= x1b2 && p2[i] != -1) {
            i = p2[i];
        }
        return (wallfront(i, b2f));
    }

    private int wallfront(int l1, int l2) {
        Wall wal;
        int x11, y11, x21, y21, x12, y12, x22, y22, dx, dy, t1, t2;

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

    private int owallmost(short[] mostbuf, int w, int z) {
        int bad, inty, xcross, y;
        int s1, s2, s3, s4, ix1, ix2, iy1, iy2;
        int i;

        z <<= 7;
        s1 = mulscale(globaluclip, yb1[w], 20);
        s2 = mulscale(globaluclip, yb2[w], 20);
        s3 = mulscale(globaldclip, yb1[w], 20);
        s4 = mulscale(globaldclip, yb2[w], 20);
        bad = (z < s1 ? 1 : 0) + ((z < s2 ? 1 : 0) << 1) + ((z > s3 ? 1 : 0) << 2) + ((z > s4 ? 1 : 0) << 3);

        ix1 = xb1[w];
        iy1 = yb1[w];
        ix2 = xb2[w];
        iy2 = yb2[w];

        if (ix1 < 0 || iy1 < 0 || ix2 < 0 || iy2 < 0) {
            return bad;
        }

        if ((bad & 3) == 3) {
            for (i = ix1; i <= ix2; i++) {
                mostbuf[i] = 0;
            }
            return (bad);
        }

        if ((bad & 12) == 12) {
            for (i = ix1; i <= ix2; i++) {
                mostbuf[i] = (short) ydimen;
            }
            return (bad);
        }

        if ((bad & 3) != 0) {
            int t = divscale((z - s1), (s2 - s1), 30);
            inty = yb1[w] + mulscale(yb2[w] - yb1[w], t, 30);
            xcross = xb1[w] + scale(mulscale(yb2[w], t, 30), (xb2[w] - xb1[w]), inty);

            if ((bad & 3) == 2) {
                if (xb1[w] <= xcross) {
                    iy2 = inty;
                    ix2 = xcross;
                }
                for (i = (xcross + 1); i <= xb2[w]; i++) {
                    if (i < 0 || i >= mostbuf.length) {
                        return (bad);
                    }
                    mostbuf[i] = 0;
                }
            } else {
                if (xcross <= xb2[w]) {
                    iy1 = inty;
                    ix1 = xcross;
                }
                for (i = xb1[w]; i <= xcross; i++) {
                    if (i < 0 || i >= mostbuf.length) {
                        return (bad);
                    }
                    mostbuf[i] = 0;
                }
            }
        }

        if ((bad & 12) != 0) {
            int t = divscale((z - s3), (s4 - s3), 30);
            inty = yb1[w] + mulscale(yb2[w] - yb1[w], t, 30);
            xcross = xb1[w] + scale(mulscale(yb2[w], t, 30), xb2[w] - xb1[w], inty);

            if ((bad & 12) == 8) {
                if (xb1[w] <= xcross) {
                    iy2 = inty;
                    ix2 = xcross;
                }
                for (i = (xcross + 1); i <= xb2[w]; i++) {
                    if (i < 0 || i >= mostbuf.length) {
                        return (bad);
                    }
                    mostbuf[i] = (short) ydimen;
                }
            } else {
                if (xcross <= xb2[w]) {
                    iy1 = inty;
                    ix1 = xcross;
                }

                for (i = xb1[w]; i <= xcross; i++) {
                    if (i < 0 || i >= mostbuf.length) {
                        return (bad);
                    }
                    mostbuf[i] = (short) ydimen;
                }
            }
        }

        y = (scale(z, xdimenscale, iy1) << 4);
        long yinc = (((long) scale(z, xdimenscale, iy2) << 4) - y) / (ix2 - ix1 + 1);

        qinterpolatedown16short(mostbuf, ix1, ix2 - ix1 + 1, y + ((long) globalhoriz << 16), yinc);

        if (ix1 < 0 || (ix1 < mostbuf.length && mostbuf[ix1] < 0)) {
            if (ix1 < 0) {
                ix1 = 0;
            }
            mostbuf[ix1] = 0;
        }
        if (ix1 >= mostbuf.length || mostbuf[ix1] > ydimen) {
            if (ix1 >= mostbuf.length) {
                ix1 = mostbuf.length - 1;
            }
            mostbuf[ix1] = (short) ydimen;
        }
        if (ix2 < 0 || (ix2 < mostbuf.length && mostbuf[ix2] < 0)) {
            if (ix2 < 0) {
                ix2 = 0;
            }
            mostbuf[ix2] = 0;
        }
        if (ix2 >= mostbuf.length || mostbuf[ix2] > ydimen) {
            if (ix2 >= mostbuf.length) {
                ix2 = mostbuf.length - 1;
            }
            mostbuf[ix2] = (short) ydimen;
        }

        return (bad);
    }

    private int wallmost(short[] mostbuf, int w, int sectnum, int dastat) {
        int bad, i, j, y, z, inty, intz, xcross, yinc, fw;
        int x1, y1, z1, x2, y2, z2, xv, yv, dx, dy, dasqr, oz1, oz2;
        int s1, s2, s3, s4, ix1, ix2, iy1, iy2;
        int t;

        if (dastat == 0) {
            z = (boardService.getSector(sectnum).getCeilingz() - globalposz);
            if ((boardService.getSector(sectnum).getCeilingstat() & 2) == 0) {
                return (owallmost(mostbuf, w, z));
            }
        } else {
            z = (boardService.getSector(sectnum).getFloorz() - globalposz);
            if ((boardService.getSector(sectnum).getFloorstat() & 2) == 0) {
                return (owallmost(mostbuf, w, z));
            }
        }

        i = thewall[w];
        if (i == boardService.getSector(sectnum).getWallptr()) {
            return (owallmost(mostbuf, w, z));
        }

        x1 = boardService.getWall(i).getX();
        x2 = boardService.getWall(boardService.getWall(i).getPoint2()).getX() - x1;
        y1 = boardService.getWall(i).getY();
        y2 = boardService.getWall(boardService.getWall(i).getPoint2()).getY() - y1;

        fw = boardService.getSector(sectnum).getWallptr();
        i = boardService.getWall(fw).getPoint2();
        dx = boardService.getWall(i).getX() - boardService.getWall(fw).getX();
        dy = boardService.getWall(i).getY() - boardService.getWall(fw).getY();
        dasqr = a.krecipasm(EngineUtils.sqrt(dx * dx + dy * dy));

        if (xb1[w] == 0) {
            xv = cosglobalang + sinviewingrangeglobalang;
            yv = singlobalang - cosviewingrangeglobalang;
        } else {
            xv = x1 - globalposx;
            yv = y1 - globalposy;
        }
        i = (xv * (y1 - globalposy) - yv * (x1 - globalposx));
        j = (yv * x2 - xv * y2);
        if (klabs(j) > klabs(i >> 3)) {
            i = divscale(i, j, 28);
        }
        if (dastat == 0) {
            t = mulscale(boardService.getSector(sectnum).getCeilingheinum(), dasqr, 15);
            z1 = boardService.getSector(sectnum).getCeilingz();
        } else {
            t = mulscale(boardService.getSector(sectnum).getFloorheinum(), dasqr, 15);
            z1 = boardService.getSector(sectnum).getFloorz();
        }
        z1 = dmulscale((long) dx * t, mulscale(y2, i, 20) + ((long) (y1 - boardService.getWall(fw).getY()) << 8), (long) -dy * t,
                mulscale(x2, i, 20) + ((long) (x1 - boardService.getWall(fw).getX()) << 8), 24) + ((z1 - globalposz) << 7);

        if (xb2[w] == xdimen - 1) {
            xv = cosglobalang - sinviewingrangeglobalang;
            yv = singlobalang + cosviewingrangeglobalang;
        } else {
            xv = (x2 + x1) - globalposx;
            yv = (y2 + y1) - globalposy;
        }
        i = (xv * (y1 - globalposy) - yv * (x1 - globalposx));
        j = (yv * x2 - xv * y2);
        if (klabs(j) > klabs(i >> 3)) {
            i = divscale(i, j, 28);
        }
        if (dastat == 0) {
            t = mulscale(boardService.getSector(sectnum).getCeilingheinum(), dasqr, 15);
            z2 = boardService.getSector(sectnum).getCeilingz();
        } else {
            t = mulscale(boardService.getSector(sectnum).getFloorheinum(), dasqr, 15);
            z2 = boardService.getSector(sectnum).getFloorz();
        }
        z2 = dmulscale((long) dx * t, mulscale(y2, i, 20) + ((long) (y1 - boardService.getWall(fw).getY()) << 8), (long) -dy * t,
                mulscale(x2, i, 20) + ((long) (x1 - boardService.getWall(fw).getX()) << 8), 24) + ((z2 - globalposz) << 7);

        s1 = mulscale(globaluclip, yb1[w], 20);
        s2 = mulscale(globaluclip, yb2[w], 20);
        s3 = mulscale(globaldclip, yb1[w], 20);
        s4 = mulscale(globaldclip, yb2[w], 20);
        bad = (z1 < s1 ? 1 : 0) + ((z2 < s2 ? 1 : 0) << 1) + ((z1 > s3 ? 1 : 0) << 2) + ((z2 > s4 ? 1 : 0) << 3);

        ix1 = xb1[w];
        ix2 = xb2[w];
        iy1 = yb1[w];
        iy2 = yb2[w];
        oz1 = z1;
        oz2 = z2;

        if ((bad & 3) == 3) {
            for (i = ix1; i <= ix2; i++) {
                mostbuf[i] = 0;
            }
            return (bad);
        }

        if ((bad & 12) == 12) {
            for (i = ix1; i <= ix2; i++) {
                mostbuf[i] = (short) ydimen;
            }
            return (bad);
        }

        if ((bad & 3) != 0) {
            t = divscale((oz1 - s1), (s2 - s1 + oz1 - oz2), 30);
            inty = (yb1[w] + mulscale(yb2[w] - yb1[w], t, 30));
            intz = (oz1 + mulscale(oz2 - oz1, t, 30));
            xcross = (xb1[w] + scale(mulscale(yb2[w], t, 30), xb2[w] - xb1[w], inty));

            if ((bad & 3) == 2) {
                if (xb1[w] <= xcross) {
                    z2 = intz;
                    iy2 = inty;
                    ix2 = xcross;
                }
                for (i = xcross + 1; i <= xb2[w]; i++) {
                    mostbuf[i] = 0;
                }
            } else {
                if (xcross <= xb2[w]) {
                    z1 = intz;
                    iy1 = inty;
                    ix1 = xcross;
                }
                for (i = xb1[w]; i <= xcross; i++) {
                    mostbuf[i] = 0;
                }
            }
        }

        if ((bad & 12) != 0) {
            t = divscale((oz1 - s3), (s4 - s3 + oz1 - oz2), 30);
            inty = (yb1[w] + mulscale(yb2[w] - yb1[w], t, 30));
            intz = oz1 + mulscale(oz2 - oz1, t, 30);
            xcross = (xb1[w] + scale(mulscale(yb2[w], t, 30), (xb2[w] - xb1[w]), inty));
            if ((bad & 12) == 8) {
                if (xb1[w] <= xcross) {
                    z2 = intz;
                    iy2 = inty;
                    ix2 = xcross;
                }
                for (i = xcross + 1; i <= xb2[w]; i++) {
                    mostbuf[i] = (short) ydimen;
                }
            } else {
                if (xcross <= xb2[w]) {
                    z1 = intz;
                    iy1 = inty;
                    ix1 = xcross;
                }
                for (i = xb1[w]; i <= xcross; i++) {
                    mostbuf[i] = (short) ydimen;
                }
            }
        }

        y = scale(z1, xdimenscale, iy1) << 4;
        yinc = ((scale(z2, xdimenscale, iy2) << 4) - y) / (ix2 - ix1 + 1);

        qinterpolatedown16short(mostbuf, ix1, ix2 - ix1 + 1, y + ((long) globalhoriz << 16), yinc);

        if (mostbuf[ix1] < 0) {
            mostbuf[ix1] = 0;
        }
        if (mostbuf[ix1] > ydimen) {
            mostbuf[ix1] = (short) ydimen;
        }
        if (mostbuf[ix2] < 0) {
            mostbuf[ix2] = 0;
        }
        if (mostbuf[ix2] > ydimen) {
            mostbuf[ix2] = (short) ydimen;
        }

        return (bad);
    }

    private void hline(int xr, int yp) {
        int xl = lastx[yp];
        if (xl > xr) {
            return;
        }

        int r = lookups[horizlookup2 + yp - (int) globalhoriz + horizycent];
        a.sethlineincs(globalx1 * r, globaly2 * r);

        int shade = paletteManager.getPalookup(mulscale(r, globvis, 16), globalshade) << 8;

        a.hlineasm4(xr - xl, 0, shade, globalx2 * r + globalypanning, globaly1 * r + globalxpanning,
                ylookup[yp] + xr + frameoffset);
    }

    private void slowhline(int xr, int yp) {
        int xl = lastx[yp];
        if (xl > xr) {
            return;
        }

        int r = lookups[horizlookup2 + yp - (int) globalhoriz + horizycent];
        int xinc = globalx1 * r;
        int yinc = globaly2 * r;

        a.sethlineincs(globalx1 * r, globaly2 * r);

        a.setuphline(getPalookupBuffer(globalpalwritten), paletteManager.getPalookup(mulscale(r, globvis, 16), globalshade) << 8);

        if ((globalorientation & 256) == 0) {
            a.mhline(globalbufplc, (long) globaly1 * r + globalxpanning - (long) xinc * (xr - xl), (xr - xl) << 16, 0,
                    (long) globalx2 * r + globalypanning - (long) yinc * (xr - xl), ylookup[yp] + xl + frameoffset);
            return;
        }
        a.thline(globalbufplc, (long) globaly1 * r + globalxpanning - (long) xinc * (xr - xl), (xr - xl) << 16, 0,
                (long) globalx2 * r + globalypanning - (long) yinc * (xr - xl), ylookup[yp] + xl + frameoffset);
    }

    protected SoftwareOrpho allocOrphoRenderer(Engine engine) {
        return new SoftwareOrpho(this, new DefaultMapSettings(engine.getBoardService()));
    }

    private void copybufreverse(byte[] s, int sptr, byte[] d, int dptr, int c) {
        while ((c--) > 0) {
            d[dptr++] = s[sptr--];
        }
    }

    @Override
    public RenderType getType() {
        return RenderType.Software;
    }

    @Override
    public PixelFormat getTexFormat() {
        return PixelFormat.Pal8;
    }

    @Override
    public void setDefs(DefScript defs) {
        if (this.defs != null) {
            Arrays.fill(textureCache, null);
        }
        this.defs = defs;
    }

    @Override
    public void showScreenFade(ScreenFade screenFade) {
        int red = min(63, screenFade.getRed()) << 2;
        int green = min(63, screenFade.getGreen()) << 2;
        int blue = min(63, screenFade.getBlue()) << 2;
        int intensive = screenFade.getIntensive() & 127;

        Palette curpalette = paletteManager.getCurrentPalette();

        int k = 0;
        for (int i = 0; i < 256; i++) {
            int offsetR = ((red - curpalette.getRed(i)) * intensive) >> 6;
            int offsetG = ((green - curpalette.getGreen(i)) * intensive) >> 6;
            int offsetB = ((blue - curpalette.getBlue(i)) * intensive) >> 6;

            temppal[k++] = (byte) (curpalette.getRed(i) + offsetR);
            temppal[k++] = (byte) (curpalette.getGreen(i) + offsetG);
            temppal[k++] = (byte) (curpalette.getBlue(i) + offsetB);
        }

        changepalette(temppal);
    }

    public byte[] getTileBuffer(final ArtEntry pic) {
        final int tilenum = pic.getNum();
        if (textureCache[tilenum] == null) {
            textureCache[tilenum] = pic.getBytes();
        }
        return textureCache[tilenum];
    }

    protected byte[] getPalookupBuffer(int paladdr) {
        if (paletteManager.isValidPalette(paladdr)) {
            return paletteManager.getPalookupBuffer()[paladdr];
        }
        return paletteManager.getPalookupBuffer()[0];
    }

    @Override
    public void onPalookupChanged(int palnum) {
    }

    @Override
    public void onChangePalette(byte[] palette) {
        changepalette(palette);
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public PaletteManager getPaletteManager() {
        return engine.getPaletteManager();
    }

    @Override
    public void onInvalidate(int tileNum) {
        textureCache[tileNum] = null;
    }

    @Override
    public void loadModels() {
    }

    @Override
    public void onPrecacheTile(int tileNum, boolean ifSprite) {
        getTileBuffer(getTile(tileNum));
    }

    public interface ChangeListener {
        void onChangePalette(byte[] palette);

        void onNextPage(byte[] frameBuffer);
    }
}
