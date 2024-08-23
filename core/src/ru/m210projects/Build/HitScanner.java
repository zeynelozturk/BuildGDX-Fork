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

import static java.lang.Math.abs;
import static ru.m210projects.Build.Engine.MAXSECTORS;
import static ru.m210projects.Build.Pragmas.*;

public class HitScanner {

    private final IntSet sectorSet;
    private final Engine engine;
    private final HitInfo info;
    private final Variable rx = new Variable();
    private final Variable ry = new Variable();
    private final Variable rz = new Variable();
    private int goalx = (1 << 29) - 1, goaly = (1 << 29) - 1;

    public HitScanner(Engine engine) {
        this.engine = engine;
        this.info = new HitInfo();
        this.sectorSet = new IntSet(MAXSECTORS);
    }

    public void setGoal(int x, int y) {
        goalx = x;
        goaly = y;
    }

    public boolean run(int xs, int ys, int zs, int sectnum, int vx, int vy, int vz, int cliptype) {

        BoardService service = engine.getBoardService();
        info.init();
        if (!service.isValidSector(sectnum)) {
            return false;
        }

        info.setX(goalx);
        info.setY(goaly);
        sectorSet.clear();

        int dawalclipmask = (cliptype & 65535);
        int dasprclipmask = (cliptype >> 16);

        int y1 = 0;
        int z1 = 0;
        sectorSet.addValue(sectnum);

        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
            int dasector = sectorSet.getValue(dacnt);
            Sector sec = service.getSector(dasector);
            if (sec == null) {
                continue;
            }

            ListNode<Wall> wn = sec.getWallNode();
            if (wn == null) {
                continue;
            }

            int x1 = Integer.MAX_VALUE;
            if (sec.isCeilingSlope()) {
                Wall wal = wn.get();
                Wall wal2 = service.getNextWall(wal);
                if (wal2 == null) {
                    continue;
                }

                int dax = wal2.getX() - wal.getX();
                int day = wal2.getY() - wal.getY();
                int i = EngineUtils.sqrt(dax * dax + day * day);
                if (i == 0) {
                    continue;
                }
                i = divscale(sec.getCeilingheinum(), i, 15);
                dax *= i;
                day *= i;

                int j = (vz << 8) - dmulscale(dax, vy, -day, vx, 15);
                if (j != 0) {
                    i = ((sec.getCeilingz() - zs) << 8) + dmulscale(dax, ys - wal.getY(), -day, xs - wal.getX(), 15);
                    if (((i ^ j) >= 0) && ((abs(i) >> 1) < abs(j))) {
                        i = divscale(i, j, 30);
                        x1 = xs + mulscale(vx, i, 30);
                        y1 = ys + mulscale(vy, i, 30);
                        z1 = zs + mulscale(vz, i, 30);
                    }
                }
            } else if ((vz < 0) && (zs >= sec.getCeilingz())) {
                z1 = sec.getCeilingz();
                int i = z1 - zs;
                if ((abs(i) >> 1) < -vz) {
                    i = divscale(i, vz, 30);
                    x1 = xs + mulscale(vx, i, 30);
                    y1 = ys + mulscale(vy, i, 30);
                }
            }

            if ((x1 != Integer.MAX_VALUE) && (abs(x1 - xs) + abs(y1 - ys) < abs((info.getX()) - xs) + abs((info.getY()) - ys))) {
                if (service.inside(x1, y1, sec)) {
                    info.set(x1, y1, z1, dasector, -1, -1);
                }
            }

            x1 = Integer.MAX_VALUE;
            if (sec.isFloorSlope()) {
                Wall wal = wn.get();
                Wall wal2 = service.getNextWall(wal);
                if (wal2 == null) {
                    continue;
                }

                int dax = wal2.getX() - wal.getX();
                int day = wal2.getY() - wal.getY();
                int i = EngineUtils.sqrt(dax * dax + day * day);
                if (i == 0) {
                    continue;
                }
                i = divscale(sec.getFloorheinum(), i, 15);
                dax *= i;
                day *= i;

                int j = (vz << 8) - dmulscale(dax, vy, -day, vx, 15);
                if (j != 0) {
                    i = ((sec.getFloorz() - zs) << 8) + dmulscale(dax, ys - wal.getY(), -day, xs - wal.getX(), 15);
                    if (((i ^ j) >= 0) && ((abs(i) >> 1) < abs(j))) {
                        i = divscale(i, j, 30);
                        x1 = xs + mulscale(vx, i, 30);
                        y1 = ys + mulscale(vy, i, 30);
                        z1 = zs + mulscale(vz, i, 30);
                    }
                }
            } else if ((vz > 0) && (zs <= sec.getFloorz())) {
                z1 = sec.getFloorz();
                int i = z1 - zs;
                if ((abs(i) >> 1) < vz) {
                    i = divscale(i, vz, 30);
                    x1 = xs + mulscale(vx, i, 30);
                    y1 = ys + mulscale(vy, i, 30);
                }
            }

            if ((x1 != Integer.MAX_VALUE)
                    && (abs(x1 - xs) + abs(y1 - ys) < abs((info.getX()) - xs) + abs((info.getY()) - ys))) {
                if (service.inside(x1, y1, sec)) {
                    info.set(x1, y1, z1, dasector, -1, -1);
                }
            }

            for (ListNode<Wall> node = sec.getWallNode(); node != null; node = node.getNext()) {
                Wall wal = node.get();
                Wall wal2 = service.getNextWall(wal);
                if (wal2 == null) {
                    continue;
                }

                x1 = wal.getX();
                y1 = wal.getY();
                int x2 = wal2.getX();
                int y2 = wal2.getY();

                if ((x1 - xs) * (y2 - ys) < (x2 - xs) * (y1 - ys)) {
                    continue;
                }

                if (engine.notIntersect(xs, ys, zs, vx, vy, vz, x1, y1, x2, y2, rx, ry, rz)) {
                    continue;
                }

                int intx = rx.get();
                int inty = ry.get();
                int intz = rz.get();

                if (abs(intx - xs) + abs(inty - ys) >= abs((info.getX()) - xs) + abs((info.getY()) - ys)) {
                    continue;
                }

                int z = node.getIndex();
                int nextsector = wal.getNextsector();
                if ((nextsector < 0) || ((wal.getCstat() & dawalclipmask) != 0)) {
                    info.set(intx, inty, intz, dasector, z, -1);
                    continue;
                }

                service.getzsofslope(service.getSector(nextsector), intx, inty, service.floorz, service.ceilingz);
                if ((intz <= service.ceilingz.get()) || (intz >= service.floorz.get())) {
                    info.set(intx, inty, intz, dasector, z, -1);
                    continue;
                }

                sectorSet.addValue(nextsector);
            }

            for (ListNode<Sprite> node = service.getSectNode(dasector); node != null; node = node.getNext()) {
                int z = node.getIndex();
                Sprite spr = node.get();

//                int hitallsprites = 0;
//                if (hitallsprites == 0) {
                if ((spr.getCstat() & dasprclipmask) == 0) {
                    continue;
                }
//                }

                x1 = spr.getX();
                y1 = spr.getY();
                z1 = spr.getZ();
                ArtEntry pic = engine.getTile(spr.getPicnum());

                int intx;
                int inty;
                int intz;
                switch (spr.getCstat() & 48) {
                    case 0:
                        int topt = vx * (x1 - xs) + vy * (y1 - ys);
                        if (topt <= 0) {
                            continue;
                        }

                        int bot = vx * vx + vy * vy;
                        if (bot == 0) {
                            continue;
                        }

                        intz = zs + scale(vz, topt, bot);
                        int i = (pic.getHeight() * spr.getYrepeat() << 2);
                        if ((spr.getCstat() & 128) != 0) {
                            z1 += (i >> 1);
                        }

                        if (pic.hasYOffset()) {
                            z1 -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                        }

                        if ((intz > z1) || (intz < z1 - i)) {
                            continue;
                        }

                        int topu = vx * (y1 - ys) - vy * (x1 - xs);

                        int offx = scale(vx, topu, bot);
                        int offy = scale(vy, topu, bot);
                        int dist = offx * offx + offy * offy;
                        i = pic.getWidth() * spr.getXrepeat();
                        i *= i;
                        if (dist > (i >> 7)) {
                            continue;
                        }

                        intx = xs + scale(vx, topt, bot);
                        inty = ys + scale(vy, topt, bot);

                        if (abs(intx - xs) + abs(inty - ys) > abs((info.getX()) - xs) + abs((info.getY()) - ys)) {
                            continue;
                        }

                        info.set(intx, inty, intz, dasector, -1, z);
                        break;
                    case 16: {
                        // These lines get the 2 points of the rotated sprite
                        // Given: (x1, y1) starts out as the center point
                        int xoff = (byte) (pic.getOffsetX() + (spr.getXoffset()));
                        if ((spr.getCstat() & 4) > 0) {
                            xoff = -xoff;
                        }

                        int k = spr.getAng();
                        int l = spr.getXrepeat();
                        int dax = EngineUtils.sin(k) * l;
                        int day = EngineUtils.cos(k + 1024) * l;
                        l = pic.getWidth();
                        k = (l >> 1) + xoff;
                        x1 -= mulscale(dax, k, 16);
                        int x2 = x1 + mulscale(dax, l, 16);
                        y1 -= mulscale(day, k, 16);
                        int y2 = y1 + mulscale(day, l, 16);

                        if ((spr.getCstat() & 64) != 0) {// back side of 1-way sprite
                            if ((x1 - xs) * (y2 - ys) < (x2 - xs) * (y1 - ys)) {
                                continue;
                            }
                        }

                        if (engine.notIntersect(xs, ys, zs, vx, vy, vz, x1, y1, x2, y2, rx, ry, rz)) {
                            continue;
                        }

                        intx = rx.get();
                        inty = ry.get();
                        intz = rz.get();

                        if (abs(intx - xs) + abs(inty - ys) > abs((info.getX()) - xs) + abs((info.getY()) - ys)) {
                            continue;
                        }

                        int cz = spr.getZ();
                        k = ((pic.getHeight() * spr.getYrepeat()) << 2);
                        if ((spr.getCstat() & 128) != 0) {
                            cz = spr.getZ() + (k >> 1);
                        }

                        if (pic.hasYOffset()) {
                            cz -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                        }

                        if ((intz < cz) && (intz > cz - k)) {
                            info.set(intx, inty, intz, dasector, -1, z);
                        }
                    }
                    break;
                    case 32: {
                        if (vz == 0) {
                            continue;
                        }
                        intz = z1;
                        if (((intz - zs) ^ vz) < 0) {
                            continue;
                        }
                        if ((spr.getCstat() & 64) != 0) {
                            if ((zs > intz) == ((spr.getCstat() & 8) == 0)) {
                                continue;
                            }
                        }

                        intx = xs + scale(intz - zs, vx, vz);
                        inty = ys + scale(intz - zs, vy, vz);

                        if (abs(intx - xs) + abs(inty - ys) > abs((info.getX()) - xs) + abs((info.getY()) - ys)) {
                            continue;
                        }

                        int xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                        int yoff = (byte) (pic.getOffsetY() + spr.getYoffset());
                        if ((spr.getCstat() & 4) > 0) {
                            xoff = -xoff;
                        }

                        if ((spr.getCstat() & 8) > 0) {
                            yoff = -yoff;
                        }

                        int cosang = EngineUtils.cos(spr.getAng());
                        int sinang = EngineUtils.sin(spr.getAng());
                        int xspan = pic.getWidth();
                        int yspan = pic.getHeight();

                        int dax = ((xspan >> 1) + xoff) * spr.getXrepeat();
                        int day = ((yspan >> 1) + yoff) * spr.getYrepeat();
                        x1 += dmulscale(sinang, dax, cosang, day, 16) - intx;
                        y1 += dmulscale(sinang, day, -cosang, dax, 16) - inty;
                        int l = xspan * spr.getXrepeat();
                        int x2 = x1 - mulscale(sinang, l, 16);
                        int y2 = y1 + mulscale(cosang, l, 16);
                        l = yspan * spr.getYrepeat();
                        int k = -mulscale(cosang, l, 16);
                        int x3 = x2 + k;
                        int x4 = x1 + k;
                        k = -mulscale(sinang, l, 16);
                        int y3 = y2 + k;
                        int y4 = y1 + k;

                        int clipyou = getClip(x2, y2, x1, y1, 0);
                        clipyou = getClip(x3, y3, x2, y2, clipyou);
                        clipyou = getClip(x4, y4, x3, y3, clipyou);
                        clipyou = getClip(x1, y1, x4, y4, clipyou);

                        if (clipyou != 0) {
                            info.set(intx, inty, intz, dasector, -1, z);
                        }
                    }
                    break;
                }
            }
        }

        return true;
    }

    private int getClip(int x1, int y1, int x2, int y2, int clipyou) {
        clipyou = GetZRange.calcClipYou(x2, y2, x1, y1, clipyou);
        return clipyou;
    }

    public HitInfo getInfo() {
        return info;
    }
}
