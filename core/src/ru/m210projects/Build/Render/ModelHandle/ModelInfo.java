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

package ru.m210projects.Build.Render.ModelHandle;

import ru.m210projects.Build.filehandle.Entry;

public class ModelInfo {

	public static final int MD_ROTATE = 2;

	public enum Type {
		Voxel, Md2, Md3
	}

	protected final Entry fileEntry;
	protected final Type type;

	protected int flags;
	protected float scale;
	// yoffset differs from zadd in that it does not follow cstat&8 y-flipping
	protected float yoffset, zadd;

	public ModelInfo(Entry file, Type type) {
		this.fileEntry = file;
		this.type = type;
	}

	public Entry getFileEntry() {
		return fileEntry;
	}

	public Type getType() {
		return type;
	}

	public int getFlags() {
		return flags;
	}

	public void setMisc(float scale, float zadd, float yoffset, int flags) {
		this.scale = scale;
		this.zadd = zadd;
		this.yoffset = yoffset;
		this.flags = flags;
	}

	public boolean isRotating() {
		return (flags & MD_ROTATE) != 0;
	}

	public float getScale() {
		return scale;
	}

	public float getYOffset(boolean yflipping) {
		return !yflipping ? yoffset : zadd;
	}
}
