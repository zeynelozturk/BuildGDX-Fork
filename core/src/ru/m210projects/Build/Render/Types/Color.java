//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Render.Types;

import java.util.Objects;

public class Color {
	public final short r;
	public final short g;
	public final short b;
	public final byte f;

	public Color(int r, int g, int b, int f) {
		this.r = (short) r;
		this.g = (short) g;
		this.b = (short) b;
		this.f = (byte) f;
	}

	public int getRed() {
		return r;
	}

	public int getGreen() {
		return g;
	}

	public int getBlue() {
		return b;
	}

	public int getF() {
		return f & 0xFF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Color color = (Color) o;
		return r == color.r && g == color.g && b == color.b && f == color.f;
	}

	@Override
	public int hashCode() {
		return Objects.hash(r, g, b, f);
	}

	@Override
	public String toString() {
		return "Color{" +
				"r=" + r +
				", g=" + g +
				", b=" + b +
				", f=" + f +
				'}';
	}
}
