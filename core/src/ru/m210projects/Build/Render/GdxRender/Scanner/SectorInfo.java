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

import ru.m210projects.Build.Board;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Wall;

import static ru.m210projects.Build.Engine.*;

public class SectorInfo {

	public enum Clockdir {
		CW(0), CCW(1);

		private final int value;

		Clockdir(int val) {
			this.value = val;
		}

		public int getValue() {
			return value;
		}
	}

	public Clockdir[] loopinfo = new Clockdir[MAXWALLS];
	public boolean[] isOccluder = new boolean[MAXWALLS];
	public boolean[] isContour = new boolean[MAXWALLS];
	public int[] numloops = new int[MAXSECTORS];
	public boolean[] hasOccluders = new boolean[MAXSECTORS];
	public boolean[] isCorrupt = new boolean[MAXSECTORS];

	public void init(Engine engine) {
		Board board = engine.getBoardService().getBoard();
		for (int i = 0; i < board.getSectorCount(); i++) {
			Sector sec = board.getSector(i);

			hasOccluders[i] = false;
			Clockdir dir = clockdir(board.getWall(sec.getWallptr()));
			boolean hasContour = false;
			int numloops = 0;
			int startwall = sec.getWallptr();
			int endwall = sec.getEndWall();
			int z = startwall;
			while (z <= endwall) {
				Wall wal = board.getWall(z);
				loopinfo[z] = dir == null ? (dir = clockdir(wal)) : dir;
				int nextsector = wal.getNextsector();
				if (dir == Clockdir.CCW && nextsector == -1) {
					isOccluder[z] = true;
					hasOccluders[i] = true;
				} else {
					isOccluder[z] = false;
				}

                isContour[z] = dir == Clockdir.CW && nextsector == -1;

				if (wal.getPoint2() < z) {
					numloops++;
					if (dir == Clockdir.CW) {
						hasContour = true;
					}
					dir = null;
				}
				z++;
			}

			this.numloops[i] = numloops;
			if (numloops > 0 && !hasContour) {
				isCorrupt[i] = true;
				System.err.println("Error: sector " + i + " doesn't have contour!");
			} else {
				isCorrupt[i] = false;
			}
		}
	}

//	public Clockdir clockdir(int wallstart) // Returns: 0 is CW, 1 is CCW
//	{
//		int minx = 0x7fffffff;
//		int themin = -1;
//		int i = wallstart - 1;
//		do {
//			if (!Gameutils.isValidWall(++i))
//				break;
//
//			if (wall[wall[i].point2].x < minx) {
//				minx = wall[wall[i].point2].x;
//				themin = i;
//			}
//		} while (wall[i].point2 != wallstart);
//
//		int x0 = wall[themin].x;
//		int y0 = wall[themin].y;
//		int x1 = wall[wall[themin].point2].x;
//		int y1 = wall[wall[themin].point2].y;
//		int x2 = wall[wall[wall[themin].point2].point2].x;
//		int y2 = wall[wall[wall[themin].point2].point2].y;
//
//		if ((y1 >= y2) && (y1 <= y0))
//			return Clockdir.CW;
//		if ((y1 >= y0) && (y1 <= y2))
//			return Clockdir.CCW;
//
//		int templong = (x0 - x1) * (y2 - y1) - (x2 - x1) * (y0 - y1);
//		if (templong < 0)
//			return Clockdir.CW;
//		else
//			return Clockdir.CCW;
//	}

	public boolean isEqualsWalls(Wall w1, Wall w2) {
		return w1.getX() == w2.getX() && w1.getY() == w2.getY() && w1.getPoint2() == w2.getPoint2();
	}

	public Clockdir clockdir(Wall wallstart) { // Returns: 0 is CW, 1 is CCW
		int minx = Integer.MAX_VALUE;
		Wall themin = null;
		Wall wal = wallstart;

		do {
			Wall wal2 = wal.getWall2();
			int x = wal2.getX();
			if (x < minx) {
				minx = x;
				themin = wal;
			}
			wal = wal2;
		} while (!isEqualsWalls(wal, wallstart));

		if (themin == null) {
			return Clockdir.CW;
		}

		int x0 = themin.getX();
		int y0 = themin.getY();
		int x1 = themin.getWall2().getX();
		int y1 = themin.getWall2().getY();
		int x2 = themin.getWall2().getWall2().getX();
		int y2 = themin.getWall2().getWall2().getY();

		if ((y1 >= y2) && (y1 <= y0)) {
			return Clockdir.CW;
		}
		if ((y1 >= y0) && (y1 <= y2)) {
			return Clockdir.CCW;
		}

		int templong = (x0 - x1) * (y2 - y1) - (x2 - x1) * (y0 - y1);
		if (templong < 0) {
			return Clockdir.CW;
		} else {
			return Clockdir.CCW;
		}
	}


//	public Clockdir clockdir(Sector sec, int wallstart) { // Returns: 0 is CW, 1 is CCW
//		Wall[] walls = sec.getWalls();
//
//		int minx = Integer.MAX_VALUE;
//		Wall themin = walls[0];
//		int i = wallstart - sec.getWallptr();
//		if (i < 0 || i >= walls.length) {
//			return Clockdir.CW;
//		}
//
//		do {
//			Wall wal = walls[i++];
//			int x = wal.getWall2().getX();
//			if (x < minx) {
//				minx = x;
//				themin = wal;
//			}
//		} while (walls[i].getPoint2() != wallstart);
//
//		int x0 = themin.getX();
//		int y0 = themin.getY();
//		int x1 = themin.getWall2().getX();
//		int y1 = themin.getWall2().getY();
//		int x2 = themin.getWall2().getWall2().getX();
//		int y2 = themin.getWall2().getWall2().getY();
//
//		if ((y1 >= y2) && (y1 <= y0)) {
//			return Clockdir.CW;
//		}
//		if ((y1 >= y0) && (y1 <= y2)) {
//			return Clockdir.CCW;
//		}
//
//		int templong = (x0 - x1) * (y2 - y1) - (x2 - x1) * (y0 - y1);
//		if (templong < 0) {
//			return Clockdir.CW;
//		} else {
//			return Clockdir.CCW;
//		}
//	}

	public boolean isCorruptSector(int i) {
		return isCorrupt[i];
	}

	public boolean isInnerWall(int i) {
		return loopinfo[i] == Clockdir.CCW;
	}

	public boolean isContourWall(int i) {
		return isContour[i];
	}

	public boolean isOccluderWall(int i) {
		return isOccluder[i];
	}

	public boolean hasOccluders(int sectnum) {
		return hasOccluders[sectnum];
	}

	public int getNumloops(int sectnum) {
		return numloops[sectnum];
	}
}
