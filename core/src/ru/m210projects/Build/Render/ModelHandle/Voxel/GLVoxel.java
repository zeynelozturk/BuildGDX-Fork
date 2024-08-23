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

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Render.ModelHandle.ModelInfo.MD_ROTATE;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.graphics.Color;

import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.ModelInfo.Type;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Types.Tile;

public abstract class GLVoxel implements GLModel {

	protected Tile skinData; // indexed texture data
	protected GLTile[] texid;
	public int xsiz, ysiz, zsiz;
	public float xpiv, ypiv, zpiv;
	protected final Color color = new Color();
	protected int flags;

	public GLVoxel(int flags) {
		this.texid = new GLTile[MAXPALOOKUPS];
		this.flags = flags;
	}

	public abstract GLTile getSkin(int pal);

	public abstract void setTextureParameters(GLTile tile, int pal, int shade, int visibility, float alpha);

	@Override
	public Type getType() {
		return Type.Voxel;
	}

	@Override
	public boolean isRotating() {
		return (flags & MD_ROTATE) != 0;
	}

	@Override
	public boolean isTintAffected() {
		return false;
	}

	@Override
	public float getScale() {
		return 1.0f;
	}

	public GLVoxel setColor(float r, float g, float b, float a) {
		color.set(r, g, b, a);
		return this;
	}

	@Override
	public Iterator<GLTile> getSkins() {
		ArrayList<GLTile> list = new ArrayList<GLTile>();
		for (int i = 0; i < texid.length; i++) {
			GLTile tex = texid[i];
			if (tex != null) {
				list.add(tex);
			}
		}
		return list.iterator();
	}

	@Override
	public void clearSkins() {
		for (int i = 0; i < texid.length; i++) {
			GLTile tex = texid[i];
			if (tex == null) {
				continue;
			}

			tex.delete();
			texid[i] = null;
		}
	}
}
