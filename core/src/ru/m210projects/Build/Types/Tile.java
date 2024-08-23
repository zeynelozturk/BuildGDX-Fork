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

//FIXME To remove
@Deprecated
public class Tile {

	private int width, height;
	public int anm;
	public byte[] data;

	public int getSize() {
		return width * height;
	}

	public boolean hasSize() {
		return width > 0 && height > 0;
	}

	public Tile allocate(int xsiz, int ysiz) {
		int dasiz = xsiz * ysiz;

		data = new byte[dasiz];
		width = xsiz;
		height = ysiz;
		anm = 0;

		return this;
	}

	public Tile clear() {
		data = null;
		width = height = 0;
		anm = 0;

		return this;
	}

	public boolean isLoaded() {
		return data != null;
	}

	public byte getOffsetX() {
		return (byte) ((anm >> 8) & 0xFF);
	}

	public byte getOffsetY() {
		return (byte) ((anm >> 16) & 0xFF);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Tile setWidth(int width) {
		this.width = width;

		return this;
	}

	public Tile setHeight(int height) {
		this.height = height;

		return this;
	}

	public int getFrames() {
		return anm & 0x3F;
	}

	public int getSpeed() {
		return (anm >> 24) & 15;
	}

	public AnimType getType() {
		switch (anm & 192) {
		case 64:
			return AnimType.OSCIL;
		case 128:
			return AnimType.FORWARD;
		case 192:
			return AnimType.BACKWARD;
		}
		return AnimType.NONE;
	}

	public boolean hasYOffset() {
		return (anm & 0x00FF0000) != 0;
	}

}
