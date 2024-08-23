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

package ru.m210projects.Build.Render.GdxRender.Scanner;

import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Render.GdxRender.WorldMesh;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.Pool;

import java.util.Arrays;

import static ru.m210projects.Build.Engine.*;

public class PotentiallyVisibleSet {

    private final WallFrustum2d[] portqueue;
    private final int queuemask; // pay attention!
    private final byte[] handled;
    private final WallFrustum2d[] gotviewport;
    private final byte[] gotwall;
    private final RayCaster ray;
    private final Pool<WallFrustum2d> pWallFrustumPool = new Pool<>(WallFrustum2d::new);
    protected SectorInfo info = new SectorInfo();
    private int pqhead, pqtail;
    private int[] sectorqueue;
    private int secindex = 0;
    private Engine engine;

    public PotentiallyVisibleSet(Engine engine) {
        this.engine = engine;
        this.ray = new RayCaster(engine.getBoardService());
        portqueue = new WallFrustum2d[512];
        queuemask = portqueue.length - 1;
        gotviewport = new WallFrustum2d[MAXSECTORS];
        sectorqueue = new int[MAXSECTORS];
        handled = new byte[MAXSECTORS >> 3];
        gotwall = new byte[MAXWALLS >> 3];
        sectorqueue = new int[MAXSECTORS];
    }

    public void process(BuildCamera cam, WorldMesh mesh, int sectnum) {
        BoardService boardService = engine.getBoardService();
        if (!boardService.isValidSector(sectnum)) {
            return;
        }

        final int cursectnum = sectnum;
        final float cameraX = cam.getX();
        final float cameraY = cam.getY();
        Arrays.fill(gotviewport, null);
        Gameutils.fill(gotwall, (byte) 0);
        Gameutils.fill(handled, (byte) 0);
        pWallFrustumPool.reset();

        secindex = 0;
        pqhead = pqtail = 0;

        portqueue[(pqtail++) & queuemask] = pWallFrustumPool.obtain().set(cam, sectnum);
        WallFrustum2d pFrustum = portqueue[pqhead];
        gotviewport[sectnum] = pFrustum;

        while (pqhead != pqtail) {
            sectnum = pFrustum.sectnum;

            if (!pFrustum.handled) {
                pFrustum.handled = true;

                if (info.hasOccluders(sectnum)) {
                    ray.init(cam, false);

                    int startwall = boardService.getSector(sectnum).getWallptr();
                    int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
                    for (int z = startwall; z < endwall; z++) {
                        if (!WallFacingCheck(cameraX, cameraY, boardService.getWall(z))) {
                            continue;
                        }
                        ray.add(cursectnum, z, null);
                    }
                    ray.update();
                }

                int startwall = boardService.getSector(sectnum).getWallptr();
                int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
                for (int z = startwall; z < endwall; z++) {
                    Wall wal = boardService.getWall(z);
                    int nextsectnum = wal.getNextsector();

                    if (!WallFacingCheck(cameraX, cameraY, wal)) {
                        continue;
                    }

                    if (!cam.polyInCamera(mesh.getPoints(Heinum.Max, sectnum, z))) {
                        continue;
                    }

                    if (pFrustum.wallInFrustum(wal)) {
                        if (info.hasOccluders(sectnum) && !ray.check(z)) {
                            continue;
                        }

                        if (nextsectnum != -1) {
                            WallFrustum2d wallFrustum = pWallFrustumPool.obtain().set(cam, wal);
                            if (wallFrustum != null && wallFrustum.fieldOfViewClipping(pFrustum)) {
                                if (gotviewport[nextsectnum] == null) {
                                    portqueue[(pqtail++) & queuemask] = wallFrustum;
                                    gotviewport[nextsectnum] = wallFrustum;
                                } else {
                                    WallFrustum2d nextp = gotviewport[nextsectnum];
                                    if ((nextp = nextp.fieldOfViewExpand(wallFrustum)) != null) {
                                        if ((handled[nextsectnum >> 3] & pow2char[nextsectnum & 7]) != 0) {
                                            portqueue[(pqtail++) & queuemask] = nextp;
                                        }
                                    }
                                }
                            }
                        }
                        gotwall[z >> 3] |= pow2char[z & 7];
                    }
                }
            }

            if (pFrustum.next != null) {
                pFrustum = pFrustum.next;
            } else {
                pFrustum = portqueue[(++pqhead) & queuemask];
            }

            if ((handled[sectnum >> 3] & pow2char[sectnum & 7]) == 0) {
                sectorqueue[secindex++] = sectnum;
            }
            handled[sectnum >> 3] |= pow2char[sectnum & 7];
        }

        ray.init(cam, true);
//		for (int i = secindex - 1; i >= 0; i--) {
//		for (int i = 0; i < secindex; i++) {
//			sectnum = sectorqueue[i];
//			if ((pFrustum = gotviewport[sectnum]) != null) {
//				if(!World.info.isCorruptSector(sectnum)) {
//					int startwall = boardService.getSector(sectnum).wallptr;
//					int endwall = boardService.getSector(sectnum).wallnum + startwall;
//					for (int z = startwall; z < endwall; z++) {
//						if ((gotwall[z >> 3] & pow2char[z & 7]) != 0) {
//							ray.add(z, pFrustum);
//						}
//					}
//				}
//			}
//		}

        for (sectnum = 0; sectnum < MAXSECTORS; sectnum++) {
            if ((pFrustum = gotviewport[sectnum]) != null) {
                if (!info.isCorruptSector(sectnum)) {
                    int startwall = boardService.getSector(sectnum).getWallptr();
                    int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
                    for (int z = startwall; z < endwall; z++) {
                        if ((gotwall[z >> 3] & pow2char[z & 7]) != 0) {
                            Wall w = boardService.getWall(z);
//							if(w.nextsector != -1) { //XXX E2L8 near wall bug fix
                            Wall p2 = boardService.getWall(w.getPoint2());
                            int dx = p2.getX() - w.getX();
                            int dy = p2.getY() - w.getY();
                            float i = dx * (cameraX - w.getX()) + dy * (cameraY - w.getY());
                            if (i >= 0.0f) {
                                float j = dx * dx + dy * dy;
                                if (i < j) {
                                    i /= j;
                                    int px = (int) (dx * i + w.getX());
                                    int py = (int) (dy * i + w.getY());

                                    dx = (int) Math.abs(px - cameraX);
                                    dy = (int) Math.abs(py - cameraY);

                                    // closest to camera portals should be rendered
                                    if (dx + dy < 128) {
                                        continue;
                                    }
                                }
                            }
//							}

                            ray.add(cursectnum, z, pFrustum);
                        }
                    }
                }
            }
        }

        ray.update();
    }

    public boolean checkWall(int z) {
        if ((gotwall[z >> 3] & pow2char[z & 7]) != 0) {
            return ray.check(z);
        }
        return false; // (gotwall[z >> 3] & pow2char[z & 7]) != 0;

//		return (gotwall[z >> 3] & pow2char[z & 7]) != 0;
    }

    public boolean checkSector(int z) {
        return gotviewport[z] != null;
    }

    private boolean WallFacingCheck(float cameraX, float cameraY, Wall wal) {
        float x1 = wal.getX() - cameraX;
        float y1 = wal.getY() - cameraY;
        float x2 = wal.getWall2().getX() - cameraX;
        float y2 = wal.getWall2().getY() - cameraY;

        return (x1 * y2 - y1 * x2) >= 0;
    }
}
