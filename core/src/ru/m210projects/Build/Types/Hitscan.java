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

package ru.m210projects.Build.Types;

public class Hitscan {
	public int hitx = -1, hity = -1, hitz = -1;
	public int hitsect = -1, hitwall = -1, hitsprite = -1;

	public int getX() {
		return hitx;
	}

	public void setX(int x) {
		this.hitx = x;
	}

	public int getY() {
		return hity;
	}

	public void setY(int y) {
		this.hity = y;
	}

	public int getZ() {
		return hitz;
	}

	public void setZ(int z) {
		this.hitz = z;
	}

	public void set(int x, int y, int z, int sector, int wall, int sprite) {
		this.hitx = x;
		this.hity = y;
		this.hitz = z;

		this.hitsect = sector;
		this.hitwall = wall;
		this.hitsprite = sprite;
	}
}
