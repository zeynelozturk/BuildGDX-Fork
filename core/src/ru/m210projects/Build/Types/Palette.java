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

import ru.m210projects.Build.CRC32;

public class Palette {
	
	private long crc32 = -1;
	private final byte[] bytes;

	public Palette() {
		bytes = new byte[768];
	}
	
	public boolean update(byte[] palette) {
		long crc32 = CRC32.getChecksum(palette);
		if (crc32 == this.crc32) {
			return false;
		}

		System.arraycopy(palette, 0, bytes, 0, palette.length);
		this.crc32 = crc32;
		return true;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public int getRed(int index) {
		return bytes[3 * index] & 0xFF;
	}

	public int getGreen(int index) {
		return bytes[3 * index + 1] & 0xFF;
	}

	public int getBlue(int index) {
		return bytes[3 * index + 2] & 0xFF;
	}
	
	public int getRGB(int p) {
		return getRed(p) | ( getGreen(p) << 8 ) | ( getBlue(p) << 16 );
	}
	
	public int getRGBA(int index, byte alphaMask) {
		return getRGB(index) | (alphaMask << 24);
	}
	
	public int getRGBA(int p) {
		return getRGBA(p, (byte) 0xFF);
	}
}
