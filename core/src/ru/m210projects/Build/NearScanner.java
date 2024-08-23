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
import ru.m210projects.Build.Types.collections.BitMap;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static ru.m210projects.Build.Pragmas.*;

public class NearScanner {

    private final List<Integer> sectorList = new ArrayList<>();
    private final BitMap sectBitMap = new BitMap();
    private final Engine engine;
    private final NearInfo info;
    private final Variable rx = new Variable();
    private final Variable ry = new Variable();
    private final Variable rz = new Variable();

    public NearScanner(Engine engine) {
        this.engine = engine;
        this.info = new NearInfo();
    }

    public NearInfo getInfo() {
        return info;
    }

    public int scan(int xs, int ys, int zs, int sectnum, int ange, int neartagrange, int tagsearch) { // jfBuild
        BoardService service = engine.getBoardService();
        info.init(-1, -1, -1, 0);
        if (!service.isValidSector(sectnum) || (tagsearch & 3) == 0) {
            return 0;
        }

        sectorList.clear();
        sectBitMap.clear();

        final int cosang = EngineUtils.cos(ange + 2048);
        final int sinang = EngineUtils.sin(ange + 2048);

        final int vx = mulscale(cosang, neartagrange, 14);
        final int vy = mulscale(sinang, neartagrange, 14);
        final int vz = 0;
        int xe = xs + vx;
        int ye = ys + vy;
        int ze = 0;

        sectBitMap.setBit(sectnum);
        sectorList.add(sectnum);

        for (int dacnt = 0; dacnt < sectorList.size(); dacnt++) {
            int dasector = sectorList.get(dacnt);
            Sector sec = service.getSector(dasector);
            if (sec == null) {
                continue;
            }

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wal = wn.get();
                Wall wal2 = service.getNextWall(wal);
                if (wal2 == null) {
                    continue;
                }

                int x1 = wal.getX();
                int y1 = wal.getY();
                int x2 = wal2.getX();
                int y2 = wal2.getY();

                int nextsector = wal.getNextsector();

                int good = 0;
                Sector sec2 = service.getSector(nextsector);
                if (sec2 != null) {
                    if (((tagsearch & 1) != 0) && sec2.getLotag() != 0) {
                        good |= 1;
                    }
                    if (((tagsearch & 2) != 0) && sec2.getHitag() != 0) {
                        good |= 1;
                    }
                }

                if (((tagsearch & 1) != 0) && wal.getLotag() != 0) {
                    good |= 2;
                }
                if (((tagsearch & 2) != 0) && wal.getHitag() != 0) {
                    good |= 2;
                }

                if ((good == 0) && (nextsector < 0)) {
                    continue;
                }
                if ((x1 - xs) * (y2 - ys) < (x2 - xs) * (y1 - ys)) {
                    continue;
                }

                if ((engine.lIntersect(xs, ys, zs, xe, ye, ze, x1, y1, x2, y2, rx, ry, rz))) {
                    if (good != 0) {
                        if ((good & 1) != 0) {
                            info.setSector(nextsector);
                        }

                        if ((good & 2) != 0) {
                            info.setWall(wn.getIndex());
                        }

                        info.setDistance(dmulscale(rx.get() - xs, cosang, ry.get() - ys, sinang, 14));

                        xe = rx.get();
                        ye = ry.get();
                        ze = rz.get();
                    }
                    if (nextsector >= 0) {
                        if (!sectBitMap.getBit(nextsector)) {
                            sectBitMap.setBit(nextsector);
                            sectorList.add(nextsector);
                        }
                    }
                }
            }

            for (ListNode<Sprite> node = service.getSectNode(dasector); node != null; node = node.getNext()) {
                int z = node.getIndex();
                Sprite spr = node.get();

                int good = 0;
                if (((tagsearch & 1) != 0) && spr.getLotag() != 0) {
                    good |= 1;
                }

                if (((tagsearch & 2) != 0) && spr.getHitag() != 0) {
                    good |= 1;
                }

                if (good != 0) {
                    int x1 = spr.getX();
                    int y1 = spr.getY();
                    int z1 = spr.getZ();

                    int topt = vx * (x1 - xs) + vy * (y1 - ys);
                    if (topt > 0) {
                        int bot = vx * vx + vy * vy;
                        if (bot != 0) {
                            int intz = zs + scale(vz, topt, bot);
                            ArtEntry pic = engine.getTile(spr.getPicnum());

                            int i = pic.getHeight() * spr.getYrepeat();
                            if ((spr.getCstat() & 128) != 0) {
                                z1 += (i << 1);
                            }
                            if (pic.hasYOffset()) {
                                z1 -= (pic.getOffsetY() * spr.getYrepeat() << 2);
                            }

                            if ((intz <= z1) && (intz >= z1 - (i << 2))) {
                                int topu = vx * (y1 - ys) - vy * (x1 - xs);
                                int offx = scale(vx, topu, bot);
                                int offy = scale(vy, topu, bot);
                                int dist = offx * offx + offy * offy;
                                i = (pic.getWidth() * spr.getXrepeat());
                                i *= i;
                                if (dist <= (i >> 7)) {
                                    int intx = xs + scale(vx, topt, bot);
                                    int inty = ys + scale(vy, topt, bot);
                                    if (abs(intx - xs) + abs(inty - ys) < abs(xe - xs) + abs(ye - ys)) {
                                        info.setSprite(z);
                                        info.setDistance(dmulscale(intx - xs, cosang, inty - ys, sinang, 14));
                                        xe = intx;
                                        ye = inty;
                                        ze = intz;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return (0);
    }
}
