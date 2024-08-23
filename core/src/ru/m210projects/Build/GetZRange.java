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

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;

public class GetZRange {

    protected static final int MAXCLIPDIST = 1024;
    private final Engine engine;
    private final RangeZInfo info;
    private final IntSet sectorSet;

    public GetZRange(Engine engine) {
        this.engine = engine;
        this.info = new RangeZInfo();
        this.sectorSet = new IntSet(MAXSECTORS);
    }

    public RangeZInfo invoke(int x, int y, int z, int sectnum, int walldist, int cliptype) {
        BoardService service = engine.getBoardService();
        info.init();
        if (!service.isValidSector(sectnum)) {
            return info;
        }

        // Extra walldist for sprites on sector lines
        int i = walldist + MAXCLIPDIST + 1;
        int xmin = x - i;
        int ymin = y - i;
        int xmax = x + i;
        int ymax = y + i;

        service.getzsofslope(service.getSector(sectnum), x, y, service.floorz, service.ceilingz);

        info.setCeilhit(sectnum | HIT_SECTOR);
        info.setFlorhit(sectnum | HIT_SECTOR);
        info.setCeilz(service.ceilingz.get());
        info.setFlorz(service.floorz.get());

        final int dawalclipmask = (cliptype & 0xFFFF);
        final int dasprclipmask = (cliptype >> 16);

        sectorSet.clear();
        sectorSet.addValue(sectnum);

        // Collect sectors inside your square first
        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
            int dasector = sectorSet.getValue(dacnt);
            Sector s = service.getSector(dasector);
            if (s == null) {
                continue;
            }

            for (ListNode<Wall> wn = s.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wal = wn.get();
                int k = wal.getNextsector();
                if (k >= 0) {
                    Wall wal2 = service.getNextWall(wal);
                    int x1 = wal.getX();
                    int x2 = wal2.getX();
                    if ((x1 < xmin) && (x2 < xmin)) {
                        continue;
                    }

                    if ((x1 > xmax) && (x2 > xmax)) {
                        continue;
                    }

                    int y1 = wal.getY();
                    int y2 = wal2.getY();
                    if ((y1 < ymin) && (y2 < ymin)) {
                        continue;
                    }

                    if ((y1 > ymax) && (y2 > ymax)) {
                        continue;
                    }

                    int dx = x2 - x1;
                    int dy = y2 - y1;
                    if (dx * (y - y1) < (x - x1) * dy) {
                        continue; // back
                    }

                    int dax = dx > 0 ? dx * (ymin - y1) : dx * (ymax - y1);
                    int day = dy > 0 ? dy * (xmax - x1) : dy * (xmin - x1);

                    if (dax >= day) {
                        continue;
                    }

                    if ((wal.getCstat() & dawalclipmask) != 0) {
                        continue;
                    }

                    Sector sec = service.getSector(k);
                    if (sec == null) {
                        continue;
                    }

                    if (!sec.isParallaxCeiling() && (z <= sec.getCeilingz() + (3 << 8))) {
                        continue;
                    }

                    if (!sec.isParallaxFloor() && (z >= sec.getFloorz() - (3 << 8))) {
                        continue;
                    }

                    sectorSet.addValue(k);

                    if ((x1 < xmin + MAXCLIPDIST) && (x2 < xmin + MAXCLIPDIST)) {
                        continue;
                    }

                    if ((x1 > xmax - MAXCLIPDIST) && (x2 > xmax - MAXCLIPDIST)) {
                        continue;
                    }

                    if ((y1 < ymin + MAXCLIPDIST) && (y2 < ymin + MAXCLIPDIST)) {
                        continue;
                    }

                    if ((y1 > ymax - MAXCLIPDIST) && (y2 > ymax - MAXCLIPDIST)) {
                        continue;
                    }

                    if (dx > 0) {
                        dax += dx * MAXCLIPDIST;
                    } else {
                        dax -= dx * MAXCLIPDIST;
                    }

                    if (dy > 0) {
                        day -= dy * MAXCLIPDIST;
                    } else {
                        day += dy * MAXCLIPDIST;
                    }

                    if (dax >= day) {
                        continue;
                    }

                    // It actually got here, through all the continue's!!!
                    service.getzsofslope(sec, x, y, service.floorz, service.ceilingz);
                    if (service.ceilingz.get() > info.getCeilz()) {
                        info.setCeilz(service.ceilingz.get());
                        info.setCeilhit(k | HIT_SECTOR);
                    }

                    if (service.floorz.get() < info.getFlorz()) {
                        info.setFlorz(service.floorz.get());
                        info.setFlorhit(k | HIT_SECTOR);
                    }
                }
            }
        }

        for (i = 0; i < sectorSet.size(); i++) {
            for (ListNode<Sprite> node = service.getSectNode(sectorSet.getValue(i)); node != null; node = node.getNext()) {
                int j = node.getIndex();
                Sprite spr = node.get();
                int cstat = spr.getCstat();
                if ((cstat & dasprclipmask) == 0) {
                    continue;
                }

                ArtEntry pic = engine.getTile(spr.getPicnum());
                int x1 = spr.getX();
                int y1 = spr.getY();

                int clipyou = 0;
                int fz = 0, cz = 0;
                switch (cstat & 48) {
                    case 0: {
                        int k = walldist + (spr.getClipdist() << 2) + 1;
                        if ((klabs(x1 - x) <= k) && (klabs(y1 - y) <= k)) {
                            cz = spr.getZ();
                            k = ((pic.getHeight() * spr.getYrepeat()) << 1);
                            if ((cstat & 128) != 0) {
                                cz += k;
                            }

                            if (pic.hasYOffset()) {
                                cz -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                            }

                            fz = cz - (k << 1);
                            clipyou = 1;
                        }
                    }
                    break;
                    case 16: {
                        int xoff = (byte) (pic.getOffsetX() + (spr.getXoffset()));
                        if ((cstat & 4) > 0) {
                            xoff = -xoff;
                        }

//                        Original
//                        int dax = EngineUtils.cos(spr.getAng()) * spr.getXrepeat();
//                        int day = EngineUtils.cos(spr.getAng() + 1024) * spr.getXrepeat();

                        int dax = EngineUtils.cos(spr.getAng() - 512) * spr.getXrepeat();
                        int day = EngineUtils.sin(spr.getAng() - 512) * spr.getXrepeat();
                        int picWidth = pic.getWidth();
                        int k = (picWidth >> 1) + xoff;
                        x1 -= mulscale(dax, k, 16);
                        int x2 = x1 + mulscale(dax, picWidth, 16);
                        y1 -= mulscale(day, k, 16);
                        int y2 = y1 + mulscale(day, picWidth, 16);
                        if (engine.clipInsideBoxLine(x, y, x1, y1, x2, y2, walldist + 1) != 0) {
                            cz = spr.getZ();
                            k = ((pic.getHeight() * spr.getYrepeat()) << 1);
                            if ((cstat & 128) != 0) {
                                cz += k;
                            }

                            if (pic.hasYOffset()) {
                                cz -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                            }

                            fz = cz - (k << 1);
                            clipyou = 1;
                        }
                    }
                    break;
                    case 32:
                        fz = cz = spr.getZ();
                        if ((cstat & 64) != 0) {
                            if ((z > cz) == ((cstat & 8) == 0)) {
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

                        int ang = spr.getAng();
                        int cosang = EngineUtils.cos(ang);
                        int sinang = EngineUtils.sin(ang);
                        int xspan = pic.getWidth();
                        int xrepeat = spr.getXrepeat();
                        int yspan = pic.getHeight();
                        int yrepeat = spr.getYrepeat();

                        int dax = ((xspan >> 1) + xoff) * xrepeat;
                        int day = ((yspan >> 1) + yoff) * yrepeat;
                        x1 += dmulscale(sinang, dax, cosang, day, 16) - x;
                        y1 += dmulscale(sinang, day, -cosang, dax, 16) - y;
                        int l = xspan * xrepeat;
                        int x2 = x1 - mulscale(sinang, l, 16);
                        int y2 = y1 + mulscale(cosang, l, 16);
                        l = yspan * yrepeat;
                        int k = -mulscale(cosang, l, 16);
                        int x3 = x2 + k;
                        int x4 = x1 + k;
                        k = -mulscale(sinang, l, 16);
                        int y3 = y2 + k;
                        int y4 = y1 + k;

                        dax = mulscale(EngineUtils.cos(spr.getAng() - 256), walldist + 4, 14);
                        day = mulscale(EngineUtils.sin(spr.getAng() - 256), walldist + 4, 14);
                        x1 += dax;
                        x2 -= day;
                        x3 -= dax;
                        x4 += day;
                        y1 += day;
                        y2 += dax;
                        y3 -= day;
                        y4 -= dax;

                        clipyou = calcClipYou(x1, y1, x2, y2, clipyou);
                        clipyou = calcClipYou(x2, y2, x3, y3, clipyou);
                        clipyou = calcClipYou(x3, y3, x4, y4, clipyou);
                        clipyou = calcClipYou(x4, y4, x1, y1, clipyou);
                        break;
                }

                if (clipyou != 0) {
                    if ((z > cz) && (cz > info.getCeilz())) {
                        info.setCeilz(cz);
                        info.setCeilhit(j | HIT_SPRITE);
                    }

                    if ((z < fz) && (fz < info.getFlorz())) {
                        info.setFlorz(fz);
                        info.setFlorhit(j | HIT_SPRITE);
                    }
                }
            }
        }

        return info;
    }

    static int calcClipYou(int x1, int y1, int x2, int y2, int clipyou) {
        if ((y1 ^ y2) < 0) {
            if ((x1 ^ x2) < 0) {
                clipyou ^= (x1 * y2 < x2 * y1 ? 1 : 0) ^ (y1 < y2 ? 1 : 0);
            } else if (x1 >= 0) {
                clipyou ^= 1;
            }
        }
        return clipyou;
    }

}
