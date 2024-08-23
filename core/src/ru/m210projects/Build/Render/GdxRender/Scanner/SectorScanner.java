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

import static ru.m210projects.Build.Engine.*;

import java.util.ArrayList;
import java.util.Arrays;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Plane.PlaneSide;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import ru.m210projects.Build.*;
import ru.m210projects.Build.Render.RenderedSpriteList;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.QuickSort.IntComparator;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Vertex;
import ru.m210projects.Build.Render.GdxRender.WorldMesh;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Types.collections.ListNode;

public abstract class SectorScanner {

	private final Pool<WallFrustum3d> pFrustumPool = new Pool<>(WallFrustum3d::new);

	private final Pool<VisibleSector> pSectorPool = new Pool<>(VisibleSector::new);

	private final Vector2 projPoint = new Vector2();

	private final PotentiallyVisibleSet pvs;

	private final WallFrustum3d[] portqueue; // to linkedlist
	private final int queuemask; // pay attention!

	private final VisibleSector[] handled;
	private final WallFrustum3d[] gotviewport;
//	private final WallFrustum3d[] skyviewport;
	private final byte[] gotwall;
	private final byte[] wallflags;
	protected Engine engine;

	public int[] maskwall = new int[MAXWALLS];
	public int maskwallcnt;

	private Sector skyFloor, skyCeiling;

	private final PolygonClipper cl = new PolygonClipper();
	private final RenderedSpriteList tSpriteList;
	private BuildCamera camera;
	private boolean showinvisibility = false;

	public SectorScanner(Engine engine) {
		this.engine = engine;
		pvs = new PotentiallyVisibleSet(engine);

		portqueue = new WallFrustum3d[512];
		queuemask = portqueue.length - 1;
		tSpriteList = new RenderedSpriteList();

		gotviewport = new WallFrustum3d[MAXSECTORS];
//		skyviewport = new WallFrustum3d[MAXSECTORS];
		handled = new VisibleSector[MAXSECTORS];
		gotwall = new byte[MAXWALLS >> 3];
		wallflags = new byte[MAXWALLS];
	}

	public void setShowInvisibility(boolean showinvisibility) {
		this.showinvisibility = showinvisibility;
	}

	public void init() {
		pvs.info.init(engine);
	}

	public void clear() {
		pSectorPool.reset();
		pFrustumPool.reset();
	}

	public void process(ArrayList<VisibleSector> sectors, BuildCamera cam, WorldMesh mesh, int sectnum, int width, int height) {
		BoardService boardService = engine.getBoardService();
		this.camera = cam;
		if (!boardService.isValidSector(sectnum)) {
			return;
		}

		pvs.process(cam, mesh, sectnum);
		Arrays.fill(gotviewport, null);
		Gameutils.fill(gotwall, (byte) 0);
		Gameutils.fill(wallflags, (byte) 0);
		Arrays.fill(handled, null);

		skyFloor = skyCeiling = null;

		maskwallcnt = 0;
		tSpriteList.reset();

		int pqtail;
		int pqhead = pqtail = 0;

        portqueue[(pqtail++) & queuemask] = pFrustumPool.obtain().set(cam, sectnum, width, height);
		WallFrustum3d pFrustum = portqueue[pqhead];
		gotviewport[sectnum] = pFrustum;

		while (pqhead != pqtail) {
			final int frustumSectnum = pFrustum.sectnum;
			VisibleSector sec = handled[frustumSectnum];
			if (handled[frustumSectnum] == null) {
				sec = pSectorPool.obtain().set(frustumSectnum);
			}

			if (!pFrustum.handled) {
				pFrustum.handled = true;
				Sector sector = boardService.getSector(frustumSectnum);
				if (sector != null) {
					for (ListNode<Wall> wn = sector.getWallNode(); wn != null; wn = wn.getNext()) {
						Wall wal = wn.get();
						int z = wn.getIndex();
						if (!pvs.checkWall(z)) {
							continue;
						}

						int nextsectnum = wal.getNextsector();
						if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Max, frustumSectnum, z))) {
							gotwall[z >> 3] |= (byte) pow2char[z & 7];

							Sector nextSector = boardService.getSector(nextsectnum);
							if ((sector.isParallaxFloor()
									&& (nextSector == null || !nextSector.isParallaxFloor()))
									&& pFrustum.wallInFrustum(mesh.getPoints(Heinum.SkyLower, frustumSectnum, z))) {
								wallflags[z] |= 8;
							}
							if ((sector.isParallaxCeiling()
									&& (nextSector == null || !nextSector.isParallaxCeiling()))
									&& pFrustum.wallInFrustum(mesh.getPoints(Heinum.SkyUpper, frustumSectnum, z))) {
								wallflags[z] |= 16;
							}

							if (nextSector != null) {
								// XXX GDX 12.07.2024 Have issue in Witchaven on map04
								// Anyway a renderer shouldn't affect to the game

//								if (!checkWallRange(nextSector, wal.getNextwall())) {
//									nextsectnum = boardService.sectorOfWall(wal.getNextwall());
//									nextSector = boardService.getSector(nextsectnum);
//									// System.err.println("Sector scanner error on sectnum");
//									wal.setNextsector(nextsectnum);
//								}

								if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Lower, frustumSectnum, z))) {
									wallflags[z] |= 1;
								}
								if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Upper, frustumSectnum, z))) {
									wallflags[z] |= 2;
								}

								if (!pvs.checkSector(nextsectnum)) {
									continue;
								}

								WallFrustum3d portal;
								if (nextSector != null && ((((sector.getCeilingstat() & nextSector.getCeilingstat()) & 1) != 0)
										|| (((sector.getFloorstat() & nextSector.getFloorstat()) & 1) != 0))) {
									portal = pFrustum.clone(pFrustumPool);
									portal.sectnum = nextsectnum;
								} else {
									// Handle the next portal
									ArrayList<Vertex> points;
									if ((points = mesh.getPoints(Heinum.Portal, frustumSectnum, z)) == null) {
										continue;
									}

									WallFrustum3d clip = null;
									boolean bNearPlaneClipped = NearPlaneCheck(cam, points);
									if (bNearPlaneClipped) {
										float posx = cam.getX();
										float posy = cam.getY();

										if ((sector.isParallaxCeiling()) || (sector.isParallaxFloor())
												|| (projectionToWall(posx, posy, wal, projPoint)
												&& Math.abs(posx - projPoint.x) + Math
												.abs(posy - projPoint.y) <= cam.near * cam.xscale * 2)) {
											clip = pFrustum.clone(pFrustumPool);
											clip.sectnum = nextsectnum;
										}
									}

									if ((frustumSectnum == sectnum || bNearPlaneClipped) && clip == null) {
										points = cl.ClipPolygon(cam.frustum, points);
										if (points.size() < 3) {
											continue;
										}
									}

									if (wal.isOneWay() && clip == null) {
										continue;
									}

									if (clip != null) {
										portal = clip;
									} else {
                                        if (nextSector != null && !nextSector.isParallaxCeiling() && !nextSector.isParallaxFloor()) {
											if (!pFrustum.wallInFrustum(points)) {
												continue;
											}
										}
										portal = pFrustum.build(cam, pFrustumPool, points, nextsectnum);
									}
								}

								if (portal != null) { // is in frustum
									wallflags[z] |= 4;
									if (gotviewport[nextsectnum] == null) {
										portqueue[(pqtail++) & queuemask] = (gotviewport[nextsectnum] = portal);
									} else {
										WallFrustum3d nextp = gotviewport[nextsectnum];
										if ((nextp = nextp.expand(portal)) != null) {
											if (handled[nextsectnum] != null) {
												// Warning! queuemask can overwrite non-handled sectors
												portqueue[(pqtail++) & queuemask] = nextp;
											}
										}
									}
								}
							}
						}
					}
				}
			}

			if (handled[frustumSectnum] == null) {
				handled[frustumSectnum] = sec;
			}

			if (pFrustum.next != null) {
				pFrustum = pFrustum.next;
			} else {
				pFrustum = portqueue[(++pqhead) & queuemask];
			}
		}

		pqhead = pqtail = 0;
		portqueue[(pqtail++) & queuemask] = gotviewport[sectnum];
//		skyviewport[sectnum] = gotviewport[sectnum];
		gotviewport[sectnum] = null;

		do {
			pFrustum = portqueue[(pqhead++) & queuemask];
			final int frustumSectnum = pFrustum.sectnum;
			Sector sector = boardService.getSector(frustumSectnum);
			VisibleSector sec = handled[frustumSectnum];
			if (sec == null || sector == null) {
				// Can happen if queuemask overwrite non-handled sectors
				continue;
			}

			if (automapping == 1) {
				show2dsector.setBit(frustumSectnum);
			}

			boolean isParallaxCeiling = sector.isParallaxCeiling();
			boolean isParallaxFloor = sector.isParallaxFloor();
			for (ListNode<Wall> wn = sector.getWallNode(); wn != null; wn = wn.getNext()) {
				Wall wal = wn.get();
				int z = wn.getIndex();
				int nextsectnum = wal.getNextsector();

				if ((gotwall[z >> 3] & pow2char[z & 7]) == 0) {
					continue;
				}

				if (nextsectnum != -1) {
					if (gotviewport[nextsectnum] != null) {
						portqueue[(pqtail++) & queuemask] = gotviewport[nextsectnum];
//						skyviewport[nextsectnum] = gotviewport[nextsectnum];
						gotviewport[nextsectnum] = null;
					}
				}
				if (wal.isMasked() || wal.isOneWay()) {
					maskwall[maskwallcnt++] = z;
				}

				if ((wallflags[z] & (8 | 16)) != 0) {
					wallflags[z] &= ~(8 | 16);

					if (isParallaxCeiling) {
						if (engine.getTile(sector.getCeilingpicnum()).hasSize()) {
							skyCeiling = sector;
						}
					}

					if (isParallaxFloor) {
						if (engine.getTile(sector.getFloorpicnum()).hasSize()) {
							skyFloor = sector;
						}
					}

					sec.skywalls.add(z);
				}

				sec.walls.add(z);
				sec.wallflags.add(wallflags[z]);
			}

			byte secflags = 0;
			if (!isParallaxFloor && isSectorVisible(pFrustum, cam.frustum.planes[0], true, frustumSectnum)) {
				secflags |= 1;
			}
			if (!isParallaxCeiling && isSectorVisible(pFrustum, cam.frustum.planes[0], false, frustumSectnum)) {
				secflags |= 2;
			}

			checkSprites(pFrustum, frustumSectnum);

			sec.secflags = secflags;
			sec.setFrustum(pFrustum.getPlanes());
			sectors.add(sec);
		} while (pqhead != pqtail);

		QuickSort.sort(maskwall, maskwallcnt, wallcomp);
	}

	protected IntComparator wallcomp = (o1, o2) -> {
        if (!wallfront(o1, o2)) {
            return -1;
        }
        return 0;
    };

	protected boolean wallfront(int o1, int o2) {
		BoardService boardService = engine.getBoardService();
		Wall w1 = boardService.getWall(o1);
		Wall w2 = boardService.getWall(o2);
		if (w1 == null || w2 == null) {
			return true;
		}

		Wall wp1 = w1.getWall2();
		float x11 = w1.getX();
		float y11 = w1.getY();
		float x21 = wp1.getX();
		float y21 = wp1.getY();

		Wall wp2 = w2.getWall2();
		float x12 = w2.getX();
		float y12 = w2.getY();
		float x22 = wp2.getX();
		float y22 = wp2.getY();

		float dx = x21 - x11;
		float dy = y21 - y11;

		final double f = 0.001;
		final double invf = 1.0 - f;
		double px = (x12 * invf) + (x22 * f);
		double py = (y12 * invf) + (y22 * f);

		double cross = dx * (py - y11) - dy * (px - x11);
		boolean t1 = (cross < 0.00001); // p1(l2) vs. l1

		px = (x22 * invf) + (x12 * f);
		py = (y22 * invf) + (y12 * f);
		double cross1 = dx * (py - y11) - dy * (px - x11);
		boolean t2 = (cross1 < 0.00001); // p2(l2) vs. l1

		if (t1 == t2) {
			t1 = (dx * (camera.getY() - y11) - dy * (camera.getX() - x11) < 0.00001); // pos vs. l1
			if (t2 == t1) {
				return true;
			}
		}

		dx = x22 - x12;
		dy = y22 - y12;

		px = (x11 * invf) + (x21 * f);
		py = (y11 * invf) + (y21 * f);

		double cross3 = dx * (py - y12) - dy * (px - x12);
		t1 = (cross3 < 0.00001); // p1(l1) vs. l2

		px = (x21 * invf) + (x11 * f);
		py = (y21 * invf) + (y11 * f);
		double cross4 = dx * (py - y12) - dy * (px - x12);
		t2 = (cross4 < 0.00001); // p2(l1) vs. l2

		if (t1 == t2) {
			t1 = (dx * (camera.getY() - y12) - dy * (camera.getX() - x12) < 0.00001); // pos vs. l2
			return t2 != t1;
		}

		return false;
	}

	public Sector getLastSkySector(Heinum h) {
		if (h == Heinum.SkyLower) {
			return skyFloor;
		}
		return skyCeiling;
	}

	private boolean checkWallRange(Sector sector, int z) {
		return z >= sector.getWallptr() && z < (sector.getWallptr() + sector.getWallnum());
	}

	private void checkSprites(WallFrustum3d pFrustum, int sectnum) {
		BoardService service = engine.getBoardService();
		float x = pFrustum.getCamera().getX();
		float y = pFrustum.getCamera().getY();

		for (ListNode<Sprite> node = service.getSectNode(sectnum); node != null; node = node.getNext()) {
			int z = node.getIndex();
			Sprite spr = node.get();

			if ((((spr.getCstat() & 0x8000) == 0) || showinvisibility) && (spr.getXrepeat() > 0) && (spr.getYrepeat() > 0)) {
				int xs = (int) (spr.getX() - x);
				int ys = (int) (spr.getY() - y);
				if ((spr.getCstat() & (64 + 48)) != (64 + 16) || Pragmas.dmulscale(EngineUtils.cos(spr.getAng()), -xs,
						EngineUtils.sin(spr.getAng()), -ys, 6) > 0) {
					if (spriteInFrustum(pFrustum, spr)) {
						addTSprite(z);
					}
				}
			}
		}
	}

	private static final Vector3[] tmpVec = { new Vector3(), new Vector3(), new Vector3(), new Vector3() };

	public boolean spriteInFrustum(WallFrustum3d frustum, Sprite tspr) {
		Vector3[] points = tmpVec;
		float SIZEX = 0.5f;
		float SIZEY = 0.5f;

		Matrix4 mat = getSpriteMatrix(tspr);
		if (mat != null) {
			points[0].set(-SIZEX, SIZEY, 0).mul(mat);
			points[1].set(SIZEX, SIZEY, 0).mul(mat);
			points[2].set(SIZEX, -SIZEY, 0).mul(mat);
			points[3].set(-SIZEX, -SIZEY, 0).mul(mat);

			WallFrustum3d n = frustum;
			do {
				if (n.wallInFrustum(points, 4)) {
					return true;
				}
				n = n.next;
			} while (n != null);
		}

		return false;
	}

	protected abstract Matrix4 getSpriteMatrix(Sprite tspr);

	private void addTSprite(int spritenum) {
		Sprite spr = engine.getBoardService().getSprite(spritenum);
		if (spr != null) {
			TSprite tspr = tSpriteList.obtain();
			tspr.set(spr);
			tspr.setOwner(spritenum);
		}
	}

	private boolean isSectorVisible(WallFrustum3d frustum, Plane near, boolean isFloor, final int sectnum) {
		frustum.rebuild();
		BoardService boardService = engine.getBoardService();
		Sector sec = boardService.getSector(sectnum);
		if (sec == null) {
			return false;
		}

		float positionZ = frustum.getCamera().getZ();
		Plane: for (int i = near == null ? 0 : -1; i < frustum.planes.length; i++) {
			Plane plane = (i == -1) ? near : frustum.planes[i];

			for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
				Wall wal = wn.get();
				int wz = isFloor ? engine.getflorzofslope((short) sectnum, wal.getX(), wal.getY())
						: engine.getceilzofslope((short) sectnum, wal.getX(), wal.getY());

				if ((isFloor && !sec.isFloorSlope() && positionZ > wz)
						|| (!isFloor && !sec.isCeilingSlope() && positionZ < wz)) {
					continue;
				}

				if (plane != null && plane.testPoint(wal.getX(), wal.getY(), wz) != PlaneSide.Back) {
					continue Plane;
				}
			}

			if (frustum.next != null) {
				return isSectorVisible(frustum.next, null, isFloor, sectnum);
			}

			return false;
		}
		return true;
	}

	private boolean NearPlaneCheck(BuildCamera cam, ArrayList<? extends Vector3> points) {
		Plane near = cam.frustum.planes[0];
		for (int i = 0; i < points.size(); i++) {
			if (near.testPoint(points.get(i)) == PlaneSide.Back) {
				return true;
			}
		}
		return false;
	}

	public boolean projectionToWall(float posx, float posy, Wall w, Vector2 n) {
		Wall p2 = w.getWall2();
		int dx = p2.getX() - w.getX();
		int dy = p2.getY() - w.getY();

		float i = dx * (posx - w.getX()) + dy * (posy - w.getY());

		if (i < 0) {
			n.set(w.getX(), w.getY());
			return false;
		}

		float j = dx * dx + dy * dy;
		if (i > j) {
			n.set(p2.getX(), p2.getY());
			return false;
		}

		i /= j;

		n.set(dx * i + w.getX(), dy * i + w.getY());
		return true;
	}

	public int getSpriteCount() {
		return tSpriteList.getSize();
	}

	public RenderedSpriteList getSprites() {
		return tSpriteList;
	}

	public int getMaskwallCount() {
		return maskwallcnt;
	}

	public int[] getMaskwalls() {
		return maskwall;
	}
}
