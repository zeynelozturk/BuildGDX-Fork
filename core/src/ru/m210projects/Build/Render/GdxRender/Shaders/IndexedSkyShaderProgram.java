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

package ru.m210projects.Build.Render.GdxRender.Shaders;

import com.badlogic.gdx.math.Matrix4;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Render.TextureHandle.IndexedShader;

public abstract class IndexedSkyShaderProgram extends IndexedShader {

    protected int cameraloc;
    protected int projTransloc;
    protected int transformloc;
    protected int mirrorloc;

    public IndexedSkyShaderProgram(int numshades) throws Exception {
        super(SkyShader.vertex, SkyShader.fragment, numshades);
    }

    @Override
    protected void init(int numshades) throws Exception {
        if (!isCompiled()) {
            throw new Exception("Shader compile error: " + getLog());
        }

        this.numshades = numshades;
        this.paletteloc = getUniformLocation("u_palette");
        this.palookuploc = getUniformLocation("u_palookup");
        this.numshadesloc = getUniformLocation("u_numshades");
        this.shadeloc = getUniformLocation("u_shade");
        this.alphaloc = getUniformLocation("u_alpha");

        this.cameraloc = getUniformLocation("u_camera");
        this.projTransloc = getUniformLocation("u_projTrans");
        this.transformloc = getUniformLocation("u_transform");
        this.mirrorloc = getUniformLocation("u_mirror");
    }

    public void prepare(BuildCamera cam) {
        setUniformf(cameraloc, cam.position.x, cam.position.y, cam.position.z);
        setUniformMatrix(projTransloc, cam.combined);
    }

    public void transform(Matrix4 transform) {
        setUniformMatrix(transformloc, transform);
    }

    public void mirror(boolean mirror) {
        setUniformi(mirrorloc, mirror ? 1 : 0);
    }

}
