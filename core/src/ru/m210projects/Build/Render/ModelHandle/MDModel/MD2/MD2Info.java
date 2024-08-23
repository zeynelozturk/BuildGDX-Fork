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

package ru.m210projects.Build.Render.ModelHandle.MDModel.MD2;

import ru.m210projects.Build.Render.ModelHandle.MDInfo;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class MD2Info extends MDInfo {

	public final MD2Header header;

	public MD2Info(Entry res) throws Exception {
		super(res, Type.Md2);

		this.header = loadHeader(res);
		if ((header.ident != 0x32504449) || (header.version != 8)) {
			throw new Exception("Wrong file header IDP2"); //"IDP2"
		}

		try(InputStream is = res.getInputStream()) {
			StreamUtils.skip(is, header.offsetFrames);
			this.frames = new String[header.numFrames];
			this.numframes = header.numFrames;

			for (int i = 0; i < header.numFrames; i++) {
				StreamUtils.skip(is, 24);
				frames[i] = StreamUtils.readString(is, 16);
				StreamUtils.skip(is, header.numVertices * 4);
			}
		}
	}

	protected MD2Header loadHeader(Entry res) throws IOException {
		try(InputStream is = res.getInputStream()) {
			MD2Header header = new MD2Header();

			header.ident = StreamUtils.readInt(is);
			header.version = StreamUtils.readInt(is);
			header.skinWidth = StreamUtils.readInt(is);
			header.skinHeight = StreamUtils.readInt(is);
			header.frameSize = StreamUtils.readInt(is);
			header.numSkins = StreamUtils.readInt(is);
			header.numVertices = StreamUtils.readInt(is);
			header.numTexCoords = StreamUtils.readInt(is);
			header.numTriangles = StreamUtils.readInt(is);
			header.numGLCommands = StreamUtils.readInt(is);
			header.numFrames = StreamUtils.readInt(is);
			header.offsetSkin = StreamUtils.readInt(is);
			header.offsetTexCoords = StreamUtils.readInt(is);
			header.offsetTriangles = StreamUtils.readInt(is);
			header.offsetFrames = StreamUtils.readInt(is);
			header.offsetGLCommands = StreamUtils.readInt(is);
			header.offsetEnd = StreamUtils.readInt(is);

			return header;
		}
	}
}
