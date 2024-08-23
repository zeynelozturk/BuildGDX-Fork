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

package ru.m210projects.Build.Render.ModelHandle.MDModel.MD3;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.NumberUtils;

import ru.m210projects.Build.Render.ModelHandle.ModelInfo.Type;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDModel;

public abstract class MD3ModelGL20 extends MDModel {

	private final Mesh mesh;
	private final MD3Surface[] surfaces;
	private final int numSurfaces;

	public MD3ModelGL20(MD3Info md) throws IOException {
		super(md);

		MD3Builder builder = new MD3Builder(md);

		this.surfaces = builder.surfaces;
		this.numSurfaces = builder.head.numSurfaces;

		int maxtris = 0;
		int maxverts = 0;
		for (int i = 0; i < this.numSurfaces; i++) {
			MD3Surface surf = surfaces[i];
			maxtris = Math.max(maxtris, surf.numtris);
			maxverts = Math.max(maxverts, surf.numverts);
		}

		mesh = new Mesh(false, maxverts * 6, maxtris * 3, VertexAttribute.Position(), VertexAttribute.ColorPacked(), VertexAttribute.TexCoords(0));
	}

	protected abstract int bindSkin(final int pal, int skinnum, int surfnum);

	@Override
	public boolean render(int pal, int pad1, int skinnum, int pad2, float pad3) {
		float f = interpol;
		float g = 1 - f;

		boolean isRendered = false;

		for (int surfi = 0; surfi < numSurfaces; surfi++) {
			MD3Surface s = surfaces[surfi];

			int texunits = bindSkin(pal, skinnum, surfi);
			if (texunits != -1) {
				FloatBuffer vertices = mesh.getVerticesBuffer();
				ShortBuffer indices = mesh.getIndicesBuffer();
				s.uv.rewind();

				vertices.clear();
				for (int i = 0; i < s.numverts; i++) {
					MD3Vertice v0 = s.xyzn[cframe][i];
					MD3Vertice v1 = s.xyzn[nframe][i];

					vertices.put(v0.x * g + v1.x * f);
					vertices.put(v0.z * g + v1.z * f);
					vertices.put(v0.y * g + v1.y * f);
					vertices.put(NumberUtils.intToFloatColor(-1));
					vertices.put(s.uv.get());
					vertices.put(s.uv.get());
				}
				vertices.flip();

				indices.clear();
				for (int i = s.numtris - 1; i >= 0; i--) {
					for (int j = 0; j < 3; j++) {
						indices.put((short) s.tris[i][j]);
					}
				}
				indices.flip();

				mesh.render(getShader(), GL20.GL_TRIANGLES);
				isRendered = true;
			} else {
				break;
			}
		}

		return isRendered;
	}

	@Override
	public void loadSkins(int pal, int skinnum) {
		for (int surfi = 0; surfi < numSurfaces; surfi++) {
			getSkin(pal, skinnum, surfi);
		}
	}

	@Override
	public Type getType() {
		return Type.Md3;
	}
}
