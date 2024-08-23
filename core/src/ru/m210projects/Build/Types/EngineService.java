// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
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

package ru.m210projects.Build.Types;

import com.badlogic.gdx.utils.IntArray;
import ru.m210projects.Build.Types.collections.BitMap;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.collections.ListNode;

import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Pragmas.mulscale;

public class EngineService {
//    private static final int MAXCLIPNUM = 1024;

    private final AtomicInteger floorZ = new AtomicInteger();
    private final AtomicInteger ceilZ = new AtomicInteger();

    private final Engine engine;
    private final BitMap sectBitMap;
    private final IntArray tmpSectorList = new IntArray();

    public EngineService(Engine engine) {
        this.engine = engine;
        this.sectBitMap = new BitMap();
    }

    public int clipInsideBox(int x, int y, int wallnum, int walldist) { // jfBuild
        BoardService service = engine.getBoardService();
        Wall wal = service.getWall(wallnum);
        if (wal == null) {
            return 0;
        }

        Wall wal2 = service.getNextWall(wal);
        return clipInsideBoxLine(x, y, wal.getX(), wal.getY(), wal2.getX(), wal2.getY(), walldist);
    }

    public int clipInsideBoxLine(int x, int y, int x1, int y1, int x2, int y2, int walldist) { // jfBuild
        int r = walldist << 1;

        x1 += walldist - x;
        x2 += walldist - x;

        if (((x1 < 0) && (x2 < 0)) || ((x1 >= r) && (x2 >= r))) {
            return 0;
        }

        y1 += walldist - y;
        y2 += walldist - y;

        if (((y1 < 0) && (y2 < 0)) || ((y1 >= r) && (y2 >= r))) {
            return 0;
        }

        x2 -= x1;
        y2 -= y1;

        if (x2 * (walldist - y1) >= y2 * (walldist - x1)) { // Front
            x2 *= ((x2 > 0) ? (-y1) : (r - y1));
            y2 *= ((y2 > 0) ? (r - x1) : (-x1));
            return x2 < y2 ? 1 : 0;
        }

        x2 *= ((x2 > 0) ? (r - y1) : (-y1));
        y2 *= ((y2 > 0) ? (-x1) : (r - x1));
        return (x2 >= y2 ? 1 : 0) << 1;
    }

    public boolean canSee(int x1, int y1, int z1, int sect1, int x2, int y2, int z2, int sect2) { // eduke32
        sectBitMap.clear();
        tmpSectorList.clear();
        if ((x1 == x2) && (y1 == y2)) {
            return (sect1 == sect2);
        }

        BoardService service = engine.getBoardService();
        int x21 = x2 - x1;
        int y21 = y2 - y1;
        int z21 = z2 - z1;

        sectBitMap.setBit(sect1);
        tmpSectorList.add(sect1);

        for (int dacnt = 0; dacnt < tmpSectorList.size; dacnt++) {
            Sector sec = service.getSector(tmpSectorList.get(dacnt));
            if (sec == null) {
                continue;
            }

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wal = wn.get();
                Wall wal2 = service.getNextWall(wal);
                if (wal2 == null) {
                    continue;
                }

                int x31 = wal.getX() - x1;
                int x34 = wal.getX() - wal2.getX();
                int y31 = wal.getY() - y1;
                int y34 = wal.getY() - wal2.getY();

                int bot = y21 * x34 - x21 * y34;
                if (bot <= 0) {
                    continue;
                }

                int t = y21 * x31 - x21 * y31;
                if ((t & 0xFFFFFFFFL) >= (bot & 0xFFFFFFFFL)) {
                    continue;
                }

                t = y31 * x34 - x31 * y34;
                if ((t & 0xFFFFFFFFL) >= (bot & 0xFFFFFFFFL)) {
                    continue;
                }

                if (wal.isOneWay()) {
                    return false;
                }

                int nexts = wal.getNextsector();
                Sector nextSector = service.getSector(nexts);
                if (nextSector == null) {
                    return false;
                }

                t = divscale(t, bot, 24);
                int x = x1 + mulscale(x21, t, 24);
                int y = y1 + mulscale(y21, t, 24);
                int z = z1 + mulscale(z21, t, 24);

                engine.getBoardService().getzsofslope(sec, x, y, floorZ, ceilZ);
                if ((z <= ceilZ.get()) || (z >= floorZ.get())) {
                    return false;
                }

                engine.getBoardService().getzsofslope(nextSector, x, y, floorZ, ceilZ);
                if ((z <= ceilZ.get()) || (z >= floorZ.get())) {
                    return false;
                }

                if (!sectBitMap.getBit(nexts)) {
                    sectBitMap.setBit(nexts);
                    tmpSectorList.add(nexts);
                }
            }
        }

        return sectBitMap.getBit(sect2);
    }

    public boolean rIntersect(int x1, int y1, int z1, int x2, int y2, int z2, int x3, // jfBuild
                                 int y3, int x4, int y4, Variable x, Variable y, Variable z) { // p1 towards p2 is a ray
        int x34 = x3 - x4;
        int y34 = y3 - y4;
        int bot = x2 * y34 - y2 * x34;
        if (bot == 0) {
            return false;
        }

        int x31 = x3 - x1;
        int y31 = y3 - y1;
        int topt = x31 * y34 - y31 * x34;
        if (bot > 0) {
            if (topt < 0) {
                return false;
            }

            int topu = x2 * y31 - y2 * x31;
            if ((topu < 0) || (topu >= bot)) {
                return false;
            }
        } else {
            if (topt > 0) {
                return false;
            }

            int topu = x2 * y31 - y2 * x31;
            if ((topu > 0) || (topu <= bot)) {
                return false;
            }
        }

        int t = divscale(topt, bot, 16);
        x.set(x1 + mulscale(x2, t, 16));
        y.set(y1 + mulscale(y2, t, 16));
        z.set(z1 + mulscale(z2, t, 16));
        return true;
    }

    public boolean lIntersect(int x1, int y1, int z1, int x2, int y2, int z2, int x3, // jfBuild
                            int y3, int x4, int y4, Variable x, Variable y, Variable z) {

        // p1 to p2 is a line segment
        int x21 = x2 - x1, x34 = x3 - x4;
        int y21 = y2 - y1, y34 = y3 - y4;
        int bot = x21 * y34 - y21 * x34;

        if (bot == 0) {
            return false;
        }

        int x31 = x3 - x1, y31 = y3 - y1;
        int topt = x31 * y34 - y31 * x34;

        if (bot > 0) {
            if ((topt < 0) || (topt >= bot)) {
                return false;
            }

            int topu = x21 * y31 - y21 * x31;
            if ((topu < 0) || (topu >= bot)) {
                return false;
            }
        } else {
            if ((topt > 0) || (topt <= bot)) {
                return false;
            }

            int topu = x21 * y31 - y21 * x31;
            if ((topu > 0) || (topu <= bot)) {
                return false;
            }
        }

        int t = divscale(topt, bot, 24);
        x.set(x1 + mulscale(x21, t, 24));
        y.set(y1 + mulscale(y21, t, 24));
        z.set(z1 + mulscale(z2 - z1, t, 24));
        return true;
    }

}
