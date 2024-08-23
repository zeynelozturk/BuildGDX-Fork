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

import static com.badlogic.gdx.graphics.GL20.GL_BLEND;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;

import ru.m210projects.Build.Render.TextureHandle.GLTile;

public abstract class VoxelGL20 extends GLVoxel {

	private final Mesh mesh;

	public VoxelGL20(VoxelData vox, int voxmip, int flags) {
		super(flags);

		VoxelBuilder builder = new VoxelBuilder(vox, voxmip);
		float[] vertices = builder.getVertices();
		short[] indices = builder.getIndices();

		this.xsiz = builder.xsiz;
		this.ysiz = builder.ysiz;
		this.zsiz = builder.zsiz;

		this.xpiv = vox.xpiv[voxmip] / 256.0f;
		this.ypiv = vox.ypiv[voxmip] / 256.0f;
		this.zpiv = vox.zpiv[voxmip] / 256.0f;

		int size = builder.getVertexSize();
		mesh = new Mesh(true, vertices.length / size, indices.length, builder.getAttributes());
		mesh.setVertices(vertices);
		mesh.setIndices(indices);

		skinData = builder.getTexture();
	}

	@Override
	public boolean render(int pal, int shade, int pad, int visibility, float alpha) {
		GLTile skin = getSkin(pal);
		if (skin == null) {
			return false;
		}

		if (alpha != 1.0f) {
			Gdx.gl.glEnable(GL_BLEND);
		} else {
			Gdx.gl.glDisable(GL_BLEND);
		}

		skin.bind();
		setTextureParameters(skin, pal, shade, visibility, alpha);

		mesh.render(getShader(), GL20.GL_TRIANGLES);
		return true;
	}

	@Override
	public void dispose() {
		mesh.dispose();
		clearSkins();
	}
}
