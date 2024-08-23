// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.
//
//Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.IntSet;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;

public class ClipMover {

    protected static final int MAXCLIPDIST = 1024;
    protected final ClipInfo info;
    protected final Engine engine;
    protected final IntSet sectorSet;
    protected final Variable rx = new Variable();
    protected final Variable ry = new Variable();
    protected final Variable rz = new Variable();
    protected final List<ClipLine> clipLines = new ArrayList<>();
    protected int clipNum = 0;
    protected int traceNum = 3;
    protected int[] hitwalls = new int[traceNum + 1];
    private boolean needUpdateSector = true;

    public ClipMover(Engine engine) {
        this.engine = engine;
        this.info = new ClipInfo();
        this.sectorSet = new IntSet(MAXSECTORS);
    }

    public void setNeedUpdateSector(boolean needUpdateSector) {
        this.needUpdateSector = needUpdateSector;
    }

    public void setTraceNum(int num) {
        this.traceNum = num;
        if (num > hitwalls.length - 1) {
            this.hitwalls = new int[num + 1];
        }
    }

    /**
     * @return Object index & mask
     */
    public int invoke(int x, int y, int z, int sectnum, // jfBuild
                        long xvect, long yvect, int walldist, int ceildist, int flordist, int cliptype) {
        BoardService service = engine.getBoardService();
        if ((xvect | yvect) == 0 || !service.isValidSector(sectnum)) {
            info.set(x, y, z, sectnum);
            return 0;
        }

        int retval = 0;
        this.clipNum = 0;

        long oxvect = xvect;
        long oyvect = yvect;

        int goalx = x + (int) (xvect >> 14);
        int goaly = y + (int) (yvect >> 14);

        int cx = (x + goalx) >> 1;
        int cy = (y + goaly) >> 1;

        // Extra walldist for sprites on sector lines
        int gx = goalx - x;
        int gy = goaly - y;
        int rad = (EngineUtils.sqrt(gx * gx + gy * gy) + MAXCLIPDIST + walldist + 8);
        int xmin = cx - rad;
        int ymin = cy - rad;
        int xmax = cx + rad;
        int ymax = cy + rad;

        final int dawalclipmask = (cliptype & 0xFFFF); // CLIPMASK0 = 0x00010001
        final int dasprclipmask = (cliptype >> 16); // CLIPMASK1 = 0x01000040

        sectorSet.clear();
        sectorSet.addValue(sectnum);

        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
            int dasect = sectorSet.getValue(dacnt);
            Sector sec = service.getSector(dasect);
            if (sec == null) {
                continue;
            }

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wal = wn.get();
                Wall wal2 = wal.getWall2();
                if ((wal.getX() < xmin) && (wal2.getX() < xmin)) {
                    continue;
                }
                if ((wal.getX() > xmax) && (wal2.getX() > xmax)) {
                    continue;
                }
                if ((wal.getY() < ymin) && (wal2.getY() < ymin)) {
                    continue;
                }
                if ((wal.getY() > ymax) && (wal2.getY() > ymax)) {
                    continue;
                }

                int x1 = wal.getX();
                int y1 = wal.getY();
                int x2 = wal2.getX();
                int y2 = wal2.getY();

                int dx = x2 - x1;
                int dy = y2 - y1;
                if (dx * (y - y1) < (x - x1) * dy) {
                    continue; // If wall's not facing you
                }

                int dax = (dx > 0) ? dx * (ymin - y1) : dx * (ymax - y1);
                int day = (dy > 0) ? dy * (xmax - x1) : dy * (xmin - x1);

                if (dax >= day) {
                    continue;
                }

                int clipyou = 0;
                if ((wal.getNextsector() < 0) || ((wal.getCstat() & dawalclipmask) != 0)) {
                    clipyou = 1;
                } else {
                    if (engine.notIntersect(x, y, 0, gx, gy, 0, x1, y1, x2, y2, rx, ry, rz)) {
                        dax = x;
                        day = y;
                    } else {
                        dax = rx.get();
                        day = ry.get();
                    }

                    Sector sec2 = service.getSector(wal.getNextsector());
                    if (sec2 == null) {
                        continue;
                    }

                    int daz = service.getflorzofslope(sec, dax, day);
                    int daz2 = service.getflorzofslope(sec2, dax, day);

                    if (daz2 < daz - (1 << 8)) {
                        if (!sec2.isParallaxFloor()) {
                            if ((z) >= daz2 - (flordist - 1)) {
                                clipyou = 1;
                            }
                        }
                    }

                    if (clipyou == 0) {
                        daz = service.getceilzofslope(sec, dax, day);
                        daz2 = service.getceilzofslope(sec2, dax, day);
                        if (daz2 > daz + (1 << 8)) {
                            if (!sec2.isParallaxCeiling()) {
                                if ((z) <= daz2 + (ceildist - 1)) {
                                    clipyou = 1;
                                }
                            }
                        }
                    }
                }

                if (clipyou == 1) {
                    int j = wn.getIndex();
                    // Add 2 boxes at endpoints
                    int bsz = walldist;
                    if (gx < 0) {
                        bsz = -bsz;
                    }
                    addclipline(x1 - bsz, y1 - bsz, x1 - bsz, y1 + bsz, j | HIT_WALL);
                    addclipline(x2 - bsz, y2 - bsz, x2 - bsz, y2 + bsz, j | HIT_WALL);
                    bsz = walldist;
                    if (gy < 0) {
                        bsz = -bsz;
                    }
                    addclipline(x1 + bsz, y1 - bsz, x1 - bsz, y1 - bsz, j | HIT_WALL);
                    addclipline(x2 + bsz, y2 - bsz, x2 - bsz, y2 - bsz, j | HIT_WALL);

                    dax = walldist;
                    if (dy > 0) {
                        dax = -dax;
                    }
                    day = walldist;
                    if (dx < 0) {
                        day = -day;
                    }
                    addclipline(x1 + dax, y1 + day, x2 + dax, y2 + day, j | HIT_WALL);
                } else {
                    int nextsector = wal.getNextsector();
                    sectorSet.addValue(nextsector);
                }
            }

            for (ListNode<Sprite> node = service.getSectNode(dasect); node != null; node = node.getNext()) {
                int j = node.getIndex();
                Sprite spr = node.get();

                int cstat = spr.getCstat();
                if ((cstat & dasprclipmask) == 0) {
                    continue;
                }

                int x1 = spr.getX();
                int y1 = spr.getY();
                int daz, daz2, k;
                ArtEntry pic = engine.getTile(spr.getPicnum());

                switch (cstat & 48) {
                    case 0:

                        if ((x1 >= xmin) && (x1 <= xmax) && (y1 >= ymin) && (y1 <= ymax)) {
                            daz = spr.getZ();
                            k = (pic.getHeight() * spr.getYrepeat() << 2);
                            if ((spr.getCstat() & 128) != 0) {
                                daz += (k >> 1);
                            }

                            if ((pic.hasYOffset())) {
                                daz -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                            }

                            if ((z < (daz + ceildist)) && (z > (daz - k - flordist))) {
                                int bsz = (spr.getClipdist() << 2) + walldist;
                                if (gx < 0) {
                                    bsz = -bsz;
                                }
                                addclipline(x1 - bsz, y1 - bsz, x1 - bsz, y1 + bsz, j | HIT_SPRITE);
                                bsz = (spr.getClipdist() << 2) + walldist;
                                if (gy < 0) {
                                    bsz = -bsz;
                                }
                                addclipline(x1 + bsz, y1 - bsz, x1 - bsz, y1 - bsz, j | HIT_SPRITE);
                            }
                        }
                        break;
                    case 16:
                        daz = spr.getZ();
                        k = (pic.getHeight() * spr.getYrepeat() << 2);
                        if ((spr.getCstat() & 128) != 0) {
                            daz += (k >> 1);
                        }

                        if ((pic.hasYOffset())) {
                            daz -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                        }

                        daz2 = daz - k;
                        daz += ceildist;
                        daz2 -= flordist;
                        if (((z) < daz) && ((z) > daz2)) {
                            // These lines get the 2 points of the rotated sprite
                            // Given: (x1, y1) starts out as the center point
                            int xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                            if ((cstat & 4) > 0) {
                                xoff = -xoff;
                            }

                            k = spr.getAng();
                            int l = spr.getXrepeat();
                            int dax = EngineUtils.sin(k) * l;
                            int day = EngineUtils.cos(k + 1024) * l;
                            l = pic.getWidth();
                            k = (l >> 1) + xoff;
                            x1 -= mulscale(dax, k, 16);
                            int x2 = x1 + mulscale(dax, l, 16);
                            y1 -= mulscale(day, k, 16);
                            int y2 = y1 + mulscale(day, l, 16);

                            if (engine.clipInsideBoxLine(cx, cy, x1, y1, x2, y2, rad) != 0) {
                                dax = mulscale(EngineUtils.cos(spr.getAng() + 256), walldist, 14);
                                day = mulscale(EngineUtils.sin(spr.getAng() + 256), walldist, 14);

                                if ((x1 - (x)) * (y2 - (y)) >= (x2 - (x)) * (y1 - (y))) // Front
                                {
                                    addclipline(x1 + dax, y1 + day, x2 + day, y2 - dax, j | HIT_SPRITE);
                                } else {
                                    if ((cstat & 64) != 0) {
                                        continue;
                                    }
                                    addclipline(x2 - dax, y2 - day, x1 - day, y1 + dax, j | HIT_SPRITE);
                                }

                                // Side blocker
                                if ((x2 - x1) * ((x) - x1) + (y2 - y1) * ((y) - y1) < 0) {
                                    addclipline(x1 - day, y1 + dax, x1 + dax, y1 + day, j | HIT_SPRITE);
                                } else if ((x1 - x2) * ((x) - x2) + (y1 - y2) * ((y) - y2) < 0) {
                                    addclipline(x2 + day, y2 - dax, x2 - dax, y2 - day, j | HIT_SPRITE);
                                }
                            }
                        }
                        break;
                    case 32:
                        daz = spr.getZ() + ceildist;
                        daz2 = spr.getZ() - flordist;
                        if (((z) < daz) && ((z) > daz2)) {
                            if ((cstat & 64) != 0) {
                                if (((z) > spr.getZ()) == ((cstat & 8) == 0)) {
                                    continue;
                                }
                            }

                            int xoff = (byte) (pic.getOffsetX() + (spr.getXoffset()));
                            int yoff = (byte) (pic.getOffsetY() + (spr.getYoffset()));
                            if ((cstat & 4) > 0) {
                                xoff = -xoff;
                            }
                            if ((cstat & 8) > 0) {
                                yoff = -yoff;
                            }

                            k = spr.getAng();
                            int cosang = EngineUtils.cos(k);
                            int sinang = EngineUtils.sin(k);
                            int xspan = pic.getWidth();
                            int xrepeat = spr.getXrepeat();
                            int yspan = pic.getHeight();
                            int yrepeat = spr.getYrepeat();

                            int dax = ((xspan >> 1) + xoff) * xrepeat;
                            int day = ((yspan >> 1) + yoff) * yrepeat;
                            int rxi0 = x1 + dmulscale(sinang, dax, cosang, day, 16);
                            int ryi0 = y1 + dmulscale(sinang, day, -cosang, dax, 16);
                            int l = xspan * xrepeat;
                            int rxi1 = rxi0 - mulscale(sinang, l, 16);
                            int ryi1 = ryi0 + mulscale(cosang, l, 16);
                            l = yspan * yrepeat;
                            k = -mulscale(cosang, l, 16);
                            int rxi2 = rxi1 + k;
                            int rxi3 = rxi0 + k;
                            k = -mulscale(sinang, l, 16);
                            int ryi2 = ryi1 + k;
                            int ryi3 = ryi0 + k;

                            dax = mulscale(EngineUtils.cos(spr.getAng() - 256), walldist, 14);
                            day = mulscale(EngineUtils.sin(spr.getAng() - 256), walldist, 14);

                            if ((rxi0 - (x)) * (ryi1 - (y)) < (rxi1 - (x)) * (ryi0 - (y))) {
                                if (engine.clipInsideBoxLine(cx, cy, rxi1, ryi1, rxi0, ryi0, rad) != 0) {
                                    addclipline(rxi1 - day, ryi1 + dax, rxi0 + dax, ryi0 + day, j | HIT_SPRITE);
                                }
                            } else if ((rxi2 - (x)) * (ryi3 - (y)) < (rxi3 - (x)) * (ryi2 - (y))) {
                                if (engine.clipInsideBoxLine(cx, cy, rxi3, ryi3, rxi2, ryi2, rad) != 0) {
                                    addclipline(rxi3 + day, ryi3 - dax, rxi2 - dax, ryi2 - day, j | HIT_SPRITE);
                                }
                            }

                            if ((rxi1 - (x)) * (ryi2 - (y)) < (rxi2 - (x)) * (ryi1 - (y))) {
                                if (engine.clipInsideBoxLine(cx, cy, rxi2, ryi2, rxi1, ryi1, rad) != 0) {
                                    addclipline(rxi2 - dax, ryi2 - day, rxi1 - day, ryi1 + dax, j | HIT_SPRITE);
                                }
                            } else if ((rxi3 - (x)) * (ryi0 - (y)) < (rxi0 - (x)) * (ryi3 - (y))) {
                                if (engine.clipInsideBoxLine(cx, cy, rxi0, ryi0, rxi3, ryi3, rad) != 0) {
                                    addclipline(rxi0 + dax, ryi0 + day, rxi3 + day, ryi3 - dax, j | HIT_SPRITE);
                                }
                            }
                        }
                        break;
                }
            }
        }

        int cnt = traceNum;
        int hitwall;
        do {
            rx.set(goalx);
            ry.set(goaly);
            hitwall = raytrace(x, y, rx, ry);
            int intx = rx.get();
            int inty = ry.get();
            if (hitwall != -1) {
                ClipLine clipit = clipLines.get(hitwall);

                int lx = clipit.x2 - clipit.x1;
                int ly = clipit.y2 - clipit.y1;
                int templong2 = lx * lx + ly * ly;
                if (templong2 > 0) {
                    int templong1 = (goalx - intx) * lx + (goaly - inty) * ly;
                    int i = 0;
                    if ((abs(templong1) >> 11) < templong2) {
                        i = divscale(templong1, templong2, 20);
                    }
                    goalx = mulscale(lx, i, 20) + intx;
                    goaly = mulscale(ly, i, 20) + inty;
                }

                int templong1 = dmulscale(lx, oxvect, ly, oyvect, 6);
                for (int i = cnt + 1; i <= traceNum; i++) {
                    ClipLine cl = clipLines.get(hitwalls[i]);
                    templong2 = dmulscale(cl.x2 - cl.x1, oxvect, cl.y2 - cl.y1, oyvect, 6);
                    if ((templong1 ^ templong2) < 0) {
                        if (needUpdateSector) {
                            sectnum = service.updatesector(x, y, sectnum);
                        }

                        info.set(x, y, z, sectnum);
                        return (retval);
                    }
                }

                rx.set(goalx);
                ry.set(goaly);
                keepaway(rx, ry, clipit);
                goalx = rx.get();
                goaly = ry.get();

                xvect = ((long) (goalx - intx) << 14);
                yvect = ((long) (goaly - inty) << 14);

                if (cnt == traceNum) {
                    retval = clipit.objectIndex;
                }
                hitwalls[cnt] = hitwall;
            }
            cnt--;

            x = intx;
            y = inty;
        } while (((xvect | yvect) != 0) && (hitwall != -1) && (cnt > 0));

        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
            int sect = sectorSet.getValue(dacnt);
            if (service.inside(x, y, service.getSector(sect))) {
                info.set(x, y, z, sect);
                return (retval);
            }
        }

        int clipmove_sectnum = -1;
        int templong1 = Integer.MAX_VALUE;
        for (int j = (service.getSectorCount() - 1); j >= 0; j--) {
            Sector sec = service.getSector(j);
            if (sec != null && service.inside(x, y, sec)) {
                int templong2 = (sec.isCeilingSlope() ? service.getceilzofslope(sec, x, y) : sec.getCeilingz()) - z;

                if (templong2 <= 0) {
                    templong2 = z - (sec.isFloorSlope() ? service.getflorzofslope(sec, x, y) : sec.getFloorz());
                    if (templong2 <= 0) {
                        info.set(x, y, z, j);
                        return (retval);
                    }
                }

                if (templong2 < templong1) {
                    clipmove_sectnum = j;
                    templong1 = templong2;
                }
            }
        }

        info.set(x, y, z, clipmove_sectnum);
        return (retval);
    }

    protected void addclipline(int dax1, int day1, int dax2, int day2, int daoval) { // jfBuild
        if (clipNum >= clipLines.size()) {
            clipLines.add(new ClipLine());
        }

        ClipLine clipit = clipLines.get(clipNum);

        clipit.x1 = dax1;
        clipit.y1 = day1;
        clipit.x2 = dax2;
        clipit.y2 = day2;
        clipit.objectIndex = daoval;
        clipNum++;
    }

    protected int raytrace(int x3, int y3, Variable rayX, Variable rayY) { // jfBuild
        int hitwall = -1;
        for (int z = (clipNum - 1); z >= 0; z--) {
            ClipLine clipit = clipLines.get(z);

            int x1 = clipit.x1;
            int x2 = clipit.x2;
            int x21 = x2 - x1;
            int y1 = clipit.y1;
            int y2 = clipit.y2;
            int y21 = y2 - y1;

            int topu = x21 * (y3 - y1) - (x3 - x1) * y21;
            if (topu <= 0) {
                continue;
            }
            if (x21 * (rayY.get() - y1) > (rayX.get() - x1) * y21) {
                continue;
            }
            int x43 = rayX.get() - x3;
            int y43 = rayY.get() - y3;
            if (x43 * (y1 - y3) > (x1 - x3) * y43) {
                continue;
            }
            if (x43 * (y2 - y3) <= (x2 - x3) * y43) {
                continue;
            }
            int bot = x43 * y21 - x21 * y43;
            if (bot == 0) {
                continue;
            }

            int cnt = 256;
            int nintx, ninty;
            do {
                cnt--;
                if (cnt < 0) {
                    rayX.set(x3);
                    rayY.set(y3);
                    return z;
                }
                nintx = x3 + scale(x43, topu, bot);
                ninty = y3 + scale(y43, topu, bot);
                topu--;
            } while (x21 * (ninty - y1) <= (nintx - x1) * y21);

            if (abs(x3 - nintx) + abs(y3 - ninty) < abs(x3 - rayX.get()) + abs(y3 - rayY.get())) {
                rayX.set(nintx);
                rayY.set(ninty);
                hitwall = z;
            }
        }

        return hitwall;
    }

    protected void keepaway(Variable x, Variable y, ClipLine clipit) {
        int px = x.get();
        int py = y.get();

        int x1 = clipit.x1;
        int dx = clipit.x2 - x1;
        int y1 = clipit.y1;
        int dy = clipit.y2 - y1;
        int ox = Integer.compare(-dy, 0);
        int oy = Integer.compare(dx, 0);
        int first = (abs(dx) <= abs(dy) ? 1 : 0);

        while (true) {
            if (dx * (py - y1) > (px - x1) * dy) {
                x.set(px);
                y.set(py);
                return;
            }

            if (first == 0) {
                px += ox;
            } else {
                py += oy;
            }
            first ^= 1;
        }
    }

    public ClipInfo getInfo() {
        return info;
    }

    protected static class ClipLine {
        int x1, y1, x2, y2;
        int objectIndex;
    }

}
