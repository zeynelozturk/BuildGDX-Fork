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

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class FadeShader extends ShaderProgram {
    public static final String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
            + "void main()\n" //
            + "{\n" //
            + "   gl_Position =  " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
            + "}\n";

    public static final String fragmentShader = "#ifdef GL_ES\n" //
            + "#define LOWP lowp\n" //
            + "precision mediump float;\n" //
            + "#else\n" //
            + "#define LOWP \n" //
            + "#endif\n" //
            + "uniform vec4 u_color;\n" //
            + "void main()\n" //
            + "{\n" //
            + "	gl_FragColor = u_color;\n" //
            + "}\n"; //

    public final int color;

    public FadeShader() {
        super(vertexShader, fragmentShader);
        color = getUniformLocation("u_color");
    }

    public void setColor(int r, int g, int b, int a) {
        setUniformf(color, (r & 0xFF) / 255.0f, (g & 0xFF) / 255.0f, (b & 0xFF) / 255.0f, (a & 0xFF) / 255.0f);
    }
}