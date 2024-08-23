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

import ru.m210projects.Build.Render.ModelHandle.MDInfo;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class MD3Info extends MDInfo {

	public final MD3Header header;

	public MD3Info(Entry res) throws Exception {
		super(res, Type.Md3);

		this.header = loadHeader(res);
		if ((header.ident != 0x33504449) || (header.version != 15)) {
			throw new Exception("Wrong file header IDP3"); //"IDP3"
		}

		try(InputStream is = res.getInputStream()) {
			StreamUtils.skip(is, header.offsetFrames);
			this.frames = new String[header.numFrames];
			this.numframes = header.numFrames;

			for (int i = 0; i < header.numFrames; i++) {
				StreamUtils.skip(is, 40);
				frames[i] = StreamUtils.readString(is, 16);
			}
		}
	}

	protected MD3Header loadHeader (Entry res) throws IOException {
		try(InputStream is = res.getInputStream()) {
			MD3Header header = new MD3Header();

			header.ident = StreamUtils.readInt(is);
			header.version = StreamUtils.readInt(is);
			header.filename = StreamUtils.readString(is, 64);
			header.flags = StreamUtils.readInt(is);
			header.numFrames = StreamUtils.readInt(is);
			header.numTags = StreamUtils.readInt(is);
			header.numSurfaces = StreamUtils.readInt(is);
			header.numSkins = StreamUtils.readInt(is);
			header.offsetFrames = StreamUtils.readInt(is);
			header.offsetTags = StreamUtils.readInt(is);
			header.offsetSurfaces = StreamUtils.readInt(is);
			header.offsetEnd = StreamUtils.readInt(is);

			return header;
		}
	}
}
