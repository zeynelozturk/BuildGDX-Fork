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

import ru.m210projects.Build.Types.ClipInfo;
import ru.m210projects.Build.Types.collections.IntSet;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Wall;

import static ru.m210projects.Build.Engine.MAXSECTORS;
import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Pragmas.mulscale;

public class PushMover {

    private final ClipInfo info;
    private final Engine engine;
    private final IntSet sectorSet;

    public PushMover(Engine engine) {
        this.engine = engine;
        this.info = new ClipInfo();
        this.sectorSet = new IntSet(MAXSECTORS);
    }

    public int move(int x, int y, int z, int sectnum, // jfBuild
                        final int walldist, final int ceildist, final int flordist, final int cliptype) {

        info.set(x, y, z, sectnum);
        BoardService service = engine.getBoardService();
        if (!service.isValidSector(sectnum)) {
            return -1;
        }

        int k = 32;
        int dir = 1;
        int bad;
        final int dawalclipmask = (cliptype & 65535); // CLIPMASK0 = 0x00010001
        do {
            bad = 0;
            sectorSet.clear();
            sectorSet.addValue(sectnum);

            for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
                int dasect = sectorSet.getValue(dacnt);
                Sector sec = service.getSector(dasect);
                if (sec == null) {
                    continue;
                }

                int startwall = dir > 0 ? sec.getWallptr() : (sec.getWallptr() + sec.getWallnum());
                int endwall = dir > 0 ? (sec.getWallptr() + sec.getWallnum()) : sec.getWallptr();

                for (int i = startwall; i != endwall; i += dir) {
                    Wall wal = service.getWall(i);
                    if (wal != null && engine.clipInsideBox(x, y, i, walldist - 4) == 1) {
                        int j = 0;
                        Wall wal2 = service.getNextWall(wal);
                        if (wal.getNextsector() < 0 || (wal.getCstat() & dawalclipmask) != 0) {
                            j = 1;
                        }

                        if (j == 0) {
                            Sector sec2 = service.getSector(wal.getNextsector());
                            if (sec2 == null) {
                                continue;
                            }

                            // Find the closest point on wall (dax, day) to (*x, *y)
                            int dax = wal2.getX() - wal.getX();
                            int day = wal2.getY() - wal.getY();
                            int daz = dax * ((x) - wal.getX()) + day * ((y) - wal.getY());

                            int t = 0;
                            if (daz > 0) {
                                int daz2 = dax * dax + day * day;
                                if (daz >= daz2) {
                                    t = (1 << 30);
                                } else {
                                    t = divscale(daz, daz2, 30);
                                }
                            }

                            dax = wal.getX() + mulscale(dax, t, 30);
                            day = wal.getY() + mulscale(day, t, 30);

                            daz = service.getflorzofslope(sec, dax, day);
                            int daz2 = service.getflorzofslope(sec2, dax, day);
                            if ((daz2 < daz - (1 << 8)) && ((sec2.getFloorstat() & 1) == 0)) {
                                if (z >= daz2 - (flordist - 1)) {
                                    j = 1;
                                }
                            }

                            daz = service.getceilzofslope(sec, dax, day);
                            daz2 = service.getceilzofslope(sec2, dax, day);
                            if ((daz2 > daz + (1 << 8)) && ((sec2.getCeilingstat() & 1) == 0)) {
                                if (z <= daz2 + (ceildist - 1)) {
                                    j = 1;
                                }
                            }
                        }

                        if (j != 0) {
                            j = EngineUtils.getAngle(wal2.getX() - wal.getX(), wal2.getY() - wal.getY());
                            int dx = EngineUtils.cos(j + 512) >> 11;
                            int dy = EngineUtils.sin(j + 512) >> 11;
                            int bad2 = 16;
                            do {
                                x += dx;
                                y += dy;
                                bad2--;
                                if (bad2 == 0) {
                                    break;
                                }
                            } while (engine.clipInsideBox(x, y, i, walldist - 4) != 0);
                            bad = -1;
                            k--;

                            if (k <= 0) {
                                info.set(x, y, z, sectnum);
                                return (bad);
                            }

                            sectnum = service.updatesector(x, y, sectnum);
                            if (sectnum < 0) {
                                info.set(x, y, z, sectnum);
                                return -1;
                            }
                        } else {
                            sectorSet.addValue(wal.getNextsector());
                        }
                    }
                }
            }
            dir = -dir;
        } while (bad != 0);

        info.set(x, y, z, sectnum);
        return (bad);
    }

    public ClipInfo getInfo() {
        return info;
    }
}
