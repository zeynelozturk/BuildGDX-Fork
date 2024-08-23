//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Pattern.Tools;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.mulscale;

import java.util.Arrays;

import com.badlogic.gdx.utils.Array;
import ru.m210projects.Build.Types.collections.BitMap;
import ru.m210projects.Build.Types.collections.DynamicArray;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.osd.Console;import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.osd.OsdColor;

public class Interpolation {

	public enum InterpolationType {
		WallX, WallY, FloorZ, CeilZ, FloorH, CeilH
	}

	public static class IData {
		public Object ptr;
		public InterpolationType type;
		public int oldpos;
		public int bakpos;
	}

	public static class ILoc {
		public int x, y, z;
		public short ang;
	}

	protected final int MAXINTERPOLATIONS = 4096;
	protected Array<ILoc> gOldSpriteLoc = new DynamicArray<>(MAXSPRITESV7, ILoc.class);
	protected int InterpolationCount = 0;
	protected IData[] gInterpolationData = new IData[MAXINTERPOLATIONS];

	protected BitMap gWallLoc = new BitMap(MAXWALLSV7);
	protected int[] gFloorHeinumLoc = new int[MAXSECTORS >> 3];
	protected int[] gFloorLoc = new int[MAXSECTORS >> 3];
	protected int[] gCeilLoc = new int[MAXSECTORS >> 3];
	protected int[] gCeilHeinumLoc = new int[MAXSECTORS >> 3];
	protected BitMap gSpriteLoc = new BitMap(MAXSPRITESV7);

	public Interpolation() {
		for (int i = 0; i < MAXINTERPOLATIONS; i++) {
			gInterpolationData[i] = new IData();
		}
	}

	protected void setinterpolation(Object obj, InterpolationType type) {
		if (InterpolationCount == MAXINTERPOLATIONS) {
			Console.out.println("Too many interpolations", OsdColor.RED);
			return;
		}

		IData data = gInterpolationData[InterpolationCount++];

		data.ptr = obj;
		data.type = type;

		switch (type) {
		case WallX:
			data.oldpos = ((Wall) obj).getX();
			break;
		case WallY:
			data.oldpos = ((Wall) obj).getY();
			break;
		case FloorZ:
			data.oldpos = ((Sector) obj).getFloorz();
			break;
		case CeilZ:
			data.oldpos = ((Sector) obj).getCeilingz();
			break;
		case FloorH:
			data.oldpos = ((Sector) obj).getFloorheinum();
			break;
		case CeilH:
			data.oldpos = ((Sector) obj).getCeilingheinum();
			break;
		}
	}

	protected void stopinterpolation(Object obj, InterpolationType type) {
		for (int i = InterpolationCount - 1; i >= 0; i--) {
			IData data = gInterpolationData[i];
			if (obj == data.ptr && data.type == type) {
				InterpolationCount--;
				gInterpolationData[i] = gInterpolationData[InterpolationCount];
			}
		}
	}

	public void clearinterpolations() {
		InterpolationCount = 0;
		gWallLoc.clear();
		Arrays.fill(gFloorHeinumLoc, 0);
		Arrays.fill(gCeilHeinumLoc, 0);
		Arrays.fill(gFloorLoc, 0);
		Arrays.fill(gCeilLoc, 0);
		gSpriteLoc.clear();
	}

	public void dospriteinterp(Sprite tsp, int smoothratio) {
		ILoc oldLoc = getsprinterpolate(tsp.getOwner());
		if (oldLoc != null) {
			int x = oldLoc.x;
			int y = oldLoc.y;
			int z = oldLoc.z;
			short nAngle = oldLoc.ang;

			// interpolate sprite position
			x += mulscale(tsp.getX() - oldLoc.x, smoothratio, 16);
			y += mulscale(tsp.getY() - oldLoc.y, smoothratio, 16);
			z += mulscale(tsp.getZ() - oldLoc.z, smoothratio, 16);
			nAngle += mulscale(((tsp.getAng() - oldLoc.ang + 1024) & 0x7FF) - 1024, smoothratio, 16);

			tsp.setX(x);
			tsp.setY(y);
			tsp.setZ(z);
			tsp.setAng(nAngle);
		}
	}

	public int getValue(IData obj) {
		switch (obj.type) {
		case WallX:
			return ((Wall) obj.ptr).getX();
		case WallY:
			return ((Wall) obj.ptr).getY();
		case FloorZ:
			return ((Sector) obj.ptr).getFloorz();
		case CeilZ:
			return ((Sector) obj.ptr).getCeilingz();
		case FloorH:
			return ((Sector) obj.ptr).getFloorheinum();
		case CeilH:
			return ((Sector) obj.ptr).getCeilingheinum();
		}

		return 0;
	}

	public void dointerpolations(float smoothratio) {
		for (int i = 0; i < InterpolationCount; i++) {
			IData gInt = gInterpolationData[i];
			Object obj = gInt.ptr;

			int value = gInt.bakpos = getValue(gInt);
			value = (int) (gInt.oldpos + ((value - gInt.oldpos) * smoothratio / 65536.0f));

			switch (gInt.type) {
			case WallX:
				((Wall) obj).setX(value);
				break;
			case WallY:
				((Wall) obj).setY(value);
				break;
			case FloorZ:
				((Sector) obj).setFloorz(value);
				break;
			case CeilZ:
				((Sector) obj).setCeilingz(value);
				break;
			case FloorH:
				((Sector) obj).setFloorheinum((short) value);
				break;
			case CeilH:
				((Sector) obj).setCeilingheinum((short) value);
				break;
			}
		}
	}

	public void restoreinterpolations() {
		for (int i = 0; i < InterpolationCount; i++) {
			IData gInt = gInterpolationData[i];
			Object obj = gInt.ptr;
			switch (gInt.type) {
			case WallX:
				((Wall) obj).setX(gInt.bakpos);
				break;
			case WallY:
				((Wall) obj).setY(gInt.bakpos);
				break;
			case FloorZ:
				((Sector) obj).setFloorz(gInt.bakpos);
				break;
			case CeilZ:
				((Sector) obj).setCeilingz(gInt.bakpos);
				break;
			case FloorH:
				((Sector) obj).setFloorheinum((short) gInt.bakpos);
				break;
			case CeilH:
				((Sector) obj).setCeilingheinum((short) gInt.bakpos);
				break;
			}
		}
	}

	public boolean setsprinterpolate(int nSprite, Sprite pSprite) {
		if (!gSpriteLoc.getBit(nSprite)) {
			ILoc pLocation = gOldSpriteLoc.get(nSprite);
			pLocation.x = pSprite.getX();
			pLocation.y = pSprite.getY();
			pLocation.z = pSprite.getZ();
			pLocation.ang = pSprite.getAng();
			gSpriteLoc.setBit(nSprite);
			return true;
		}

		return false;
	}

	public void clearspriteinterpolate(int nSprite) {
		gSpriteLoc.clearBit(nSprite);
	}

	public ILoc getsprinterpolate(int nSprite) {
		if (gSpriteLoc.getBit(nSprite)) {
			return gOldSpriteLoc.get(nSprite);
		}
		return null;
	}

	public void setwallinterpolate(int nWall, Wall pWall) {
		if (!gWallLoc.getBit(nWall)) {
			setinterpolation(pWall, InterpolationType.WallX);
			setinterpolation(pWall, InterpolationType.WallY);
			gWallLoc.setBit(nWall);
		}
	}

	public void clearwallinterpolate(int nWall, Wall pWall) {
		if (gWallLoc.getBit(nWall)) {
			stopinterpolation(pWall, InterpolationType.WallX);
			stopinterpolation(pWall, InterpolationType.WallY);
			gWallLoc.clearBit(nWall);
		}
	}

	public void setfheinuminterpolate(int nSector, Sector pSector) {
		if ((gFloorHeinumLoc[nSector >> 3] & pow2char[nSector & 7]) == 0) {
			setinterpolation(pSector, InterpolationType.FloorH);
			gFloorHeinumLoc[nSector >> 3] |= pow2char[nSector & 7];
		}
	}

	public void clearfheinuminterpolate(int nSector, Sector pSector) {
		if ((gFloorHeinumLoc[nSector >> 3] & pow2char[nSector & 7]) != 0) {
			stopinterpolation(pSector, InterpolationType.FloorH);
			gFloorHeinumLoc[nSector >> 3] &= ~pow2char[nSector & 7];
		}
	}

	public void setcheinuminterpolate(int nSector, Sector pSector) {
		if ((gCeilHeinumLoc[nSector >> 3] & pow2char[nSector & 7]) == 0) {
			setinterpolation(pSector, InterpolationType.CeilH);
			gCeilHeinumLoc[nSector >> 3] |= pow2char[nSector & 7];
		}
	}

	public void clearcheinuminterpolate(int nSector, Sector pSector) {
		if ((gCeilHeinumLoc[nSector >> 3] & pow2char[nSector & 7]) != 0) {
			stopinterpolation(pSector, InterpolationType.CeilH);
			gCeilHeinumLoc[nSector >> 3] &= ~pow2char[nSector & 7];
		}
	}

	public boolean setfloorinterpolate(int nSector, Sector pSector) {
		if ((gFloorLoc[nSector >> 3] & pow2char[nSector & 7]) == 0) {
			setinterpolation(pSector, InterpolationType.FloorZ);
			gFloorLoc[nSector >> 3] |= pow2char[nSector & 7];
			return true;
		}
		return false;
	}

	public void clearfloorinterpolate(int nSector, Sector pSector) {
		if ((gFloorLoc[nSector >> 3] & pow2char[nSector & 7]) != 0) {
			stopinterpolation(pSector, InterpolationType.FloorZ);
			gFloorLoc[nSector >> 3] &= ~pow2char[nSector & 7];
		}
	}

	public boolean setceilinterpolate(int nSector, Sector pSector) {
		if ((gCeilLoc[nSector >> 3] & pow2char[nSector & 7]) == 0) {
			setinterpolation(pSector, InterpolationType.CeilZ);
			gCeilLoc[nSector >> 3] |= pow2char[nSector & 7];
			return true;
		}
		return false;
	}

	public void clearceilinterpolate(int nSector, Sector pSector) {
		if ((gCeilLoc[nSector >> 3] & pow2char[nSector & 7]) != 0) {
			stopinterpolation(pSector, InterpolationType.CeilZ);
			gCeilLoc[nSector >> 3] &= ~pow2char[nSector & 7];
		}
	}

}
