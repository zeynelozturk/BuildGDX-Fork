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

import static com.badlogic.gdx.graphics.GL20.GL_CULL_FACE;
import static com.badlogic.gdx.graphics.GL20.GL_FLOAT;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
import static com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_SHORT;
import static ru.m210projects.Build.Render.Types.GL10.GL_ALPHA_TEST;
import static ru.m210projects.Build.Render.Types.GL10.GL_MODELVIEW;
import static ru.m210projects.Build.Render.Types.GL10.GL_RGB_SCALE;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE0;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_COORD_ARRAY;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_ENV;
import static ru.m210projects.Build.Render.Types.GL10.GL_VERTEX_ARRAY;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;

import ru.m210projects.Build.Render.ModelHandle.ModelInfo.Type;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDModel;
import ru.m210projects.Build.Render.Types.GL10;

public abstract class MD3ModelGL10 extends MDModel {

	private final ShortBuffer indices;
	private final FloatBuffer vertices;

	private final MD3Surface[] surfaces;
	private final int numSurfaces;
	private final GL10 gl10;

	public MD3ModelGL10(GL10 gl10, MD3Info md) throws IOException {
		super(md);
		this.gl10 = gl10;

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

		this.indices = BufferUtils.newShortBuffer(maxtris * 3);
		this.vertices = BufferUtils.newFloatBuffer(maxverts * 3);
	}

	protected abstract int bindSkin(final int pal, int skinnum, int surfnum);

	@Override
	public boolean render(int pal, int pad1, int skinnum, int pad2, float pad3) {
		boolean isRendered = false;

		for (int surfi = 0; surfi < numSurfaces; surfi++) {
			MD3Surface s = surfaces[surfi];

			int texunits = bindSkin(pal, skinnum, surfi);
			if (texunits != -1) {

				vertices.clear();
				for (int i = 0; i < s.numverts; i++) {
					MD3Vertice v0 = s.xyzn[cframe][i];
					MD3Vertice v1 = s.xyzn[nframe][i];

					vertices.put(v0.x * cScale.x + v1.x * nScale.x);
					vertices.put(v0.z * cScale.z + v1.z * nScale.z);
					vertices.put(v0.y * cScale.y + v1.y * nScale.y);
				}
				vertices.flip();

				indices.clear();
				for (int i = s.numtris - 1; i >= 0; i--) {
					for (int j = 0; j < 3; j++) {
						indices.put((short) s.tris[i][j]);
					}
				}
				indices.flip();

				int l = GL_TEXTURE0;
				do {
					gl10.glClientActiveTexture(l++);
					gl10.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
					gl10.glTexCoordPointer(2, GL_FLOAT, 0, s.uv);
				} while (l <= texunits);

				gl10.glEnableClientState(GL_VERTEX_ARRAY);
				gl10.glVertexPointer(3, GL_FLOAT, 0, vertices);
				gl10.glDrawElements(GL_TRIANGLES, 0, GL_UNSIGNED_SHORT, indices);

				while (texunits > GL_TEXTURE0) {
					gl10.glMatrixMode(GL_TEXTURE);
					gl10.glLoadIdentity();
					gl10.glMatrixMode(GL_MODELVIEW);
					gl10.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0f);
					gl10.glDisable(GL_TEXTURE_2D);

					gl10.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
					gl10.glClientActiveTexture(texunits - 1);

					gl10.glActiveTexture(--texunits);
				}
				gl10.glDisableClientState(GL_VERTEX_ARRAY);
				isRendered = true;
			} else {
				break;
			}
		}

		if (usesalpha) {
			gl10.glDisable(GL_ALPHA_TEST);
		}
		gl10.glDisable(GL_CULL_FACE);
		gl10.glLoadIdentity();

		return isRendered;
	}

	@Override
	public MDModel setScale(Vector3 cScale, Vector3 nScale) {
		this.cScale.set(cScale);
		this.nScale.set(nScale);

		this.cScale.scl(1 / 64.0f);
		this.nScale.scl(1 / 64.0f);

		return this;
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

	@Override
	public ShaderProgram getShader() {
		/* do nothing */
		return null;
	}
}
