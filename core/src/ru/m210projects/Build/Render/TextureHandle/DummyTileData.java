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

package ru.m210projects.Build.Render.TextureHandle;

import static com.badlogic.gdx.graphics.GL20.GL_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_LUMINANCE;
import static com.badlogic.gdx.graphics.GL20.GL_RGB;
import static com.badlogic.gdx.graphics.GL20.GL_RGBA;
import static com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_BYTE;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DummyTileData extends TileData {

	public final ByteBuffer data;
	public final int width, height;
	public final PixelFormat fmt;
	public int format, internalformat;

	public DummyTileData(PixelFormat fmt, int width, int height) {
		this.width = width;
		this.height = height;
		this.fmt = fmt;
		this.data = ByteBuffer.allocateDirect(width * height * fmt.getLength()).order(ByteOrder.LITTLE_ENDIAN);

		switch (fmt) {
		case Rgba:
			format = GL_RGBA;
			internalformat = GL_RGBA;
			break;
		case Rgb:
			format = GL_RGB;
			internalformat = GL_RGB;
			break;
		case Pal8:
			format = GL_LUMINANCE;
			internalformat = GL_LUMINANCE;
			break;
		case Bitmap:
			format = GL_ALPHA;
			internalformat = GL_ALPHA;
			break;
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public ByteBuffer getPixels() {
		data.rewind();
		return data;
	}

	@Override
	public int getGLType() {
		return GL_UNSIGNED_BYTE;
	}

	@Override
	public int getGLInternalFormat() {
		return internalformat;
	}

	@Override
	public int getGLFormat() {
		return format;
	}

	@Override
	public PixelFormat getPixelFormat() {
		return fmt;
	}

	@Override
	public boolean hasAlpha() {
		return true;
	}

	@Override
	public boolean isClamped() {
		return false;
	}

	@Override
	public boolean isHighTile() {
		return false;
	}
}
