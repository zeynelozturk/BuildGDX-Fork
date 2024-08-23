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

package ru.m210projects.Build.Render.ModelHandle.Voxel;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class VoxelData {

	public static final int MAXVOXMIPS = 5;

	public int[] xsiz, ysiz, zsiz;
	public int[] xpiv, ypiv, zpiv;
	public short[][][] xyoffs;
	public int[][] slabxoffs;
	public byte[][] data;
	public int[] pal;

	public VoxelData(Entry entry) throws Exception {
		int mip = 0;

		xsiz = new int[MAXVOXMIPS];
		ysiz = new int[MAXVOXMIPS];
		zsiz = new int[MAXVOXMIPS];

		xpiv = new int[MAXVOXMIPS];
		ypiv = new int[MAXVOXMIPS];
		zpiv = new int[MAXVOXMIPS];

		xyoffs = new short[MAXVOXMIPS][][];
		slabxoffs = new int[MAXVOXMIPS][];
		data = new byte[MAXVOXMIPS][];

		try (InputStream is = entry.getInputStream()) {
			long size = entry.getSize();
			if (is.available() != size) {
				throw new IOException("Can't read the voxel");
			}

			while (is.available() > 768) {
				int mip1leng = StreamUtils.readInt(is);
				int xs = xsiz[mip] = StreamUtils.readInt(is);
				int ys = ysiz[mip] = StreamUtils.readInt(is);
				zsiz[mip] = StreamUtils.readInt(is);

				xpiv[mip] = StreamUtils.readInt(is);
				ypiv[mip] = StreamUtils.readInt(is);
				zpiv[mip] = StreamUtils.readInt(is);

				int offset = ((xs + 1) << 2) + (xs * (ys + 1) << 1);
				slabxoffs[mip] = new int[xs + 1];
				for (int i = 0; i <= xs; i++) {
					slabxoffs[mip][i] = StreamUtils.readInt(is) - offset;
				}

				xyoffs[mip] = new short[xs][ys + 1];
				for (int i = 0; i < xs; ++i) {
					for (int j = 0; j <= ys; ++j) {
						xyoffs[mip][i][j] = (short) StreamUtils.readShort(is);
					}
				}

				int i = is.available() - 768;
				if (i < mip1leng - (24 + offset)) {
					break;
				}

				data[mip] = StreamUtils.readBytes(is, mip1leng - (24 + offset));
				mip++;
			}

			if (mip == 0) {
				throw new Exception("Can't load voxel");
			}

			this.pal = new int[256];
			if (is.available() != 768) {
				int skip = is.available() - 768;
				StreamUtils.skip(is, skip);
			}

			byte[] buf = StreamUtils.readBytes(is, 768);
			for (int i = 0; i < 256; i++) {
				pal[i] = ((buf[3 * i + 0]) << 18) + ((buf[3 * i + 1]) << 10) + ((buf[3 * i + 2]) << 2) + (i << 24);
			}
		}
	}
}
