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

package ru.m210projects.Build.Script;

import static ru.m210projects.Build.Strhandler.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Types.MD4;
import ru.m210projects.Build.filehandle.Entry;

public class MapHackInfo {

	protected Maphack maphack;
	protected Map<String, Entry> hacklist;

	public MapHackInfo() {
		hacklist = new HashMap<>();
	}

	public MapHackInfo(MapHackInfo src) {
		hacklist = new HashMap<>(src.hacklist);
	}

	public boolean addMapInfo(Entry map, Entry mhkscript, String md4) {
		if(map.exists() && mhkscript.exists()) {
			if(md4 == null || MD4.getChecksum(map.getBytes()).equals(md4.toUpperCase())) {
				hacklist.put(map.getName().toLowerCase(Locale.ROOT), mhkscript);
				return true;
			}
		}
		return false;
	}

	public boolean load(Entry map) {
		unload();

		Entry mhk = hacklist.get(map.getName().toLowerCase(Locale.ROOT));
		if(mhk != null) {
			this.maphack = new Maphack(mhk);
			return true;
		}

		return false;
	}

	public boolean isLoaded() {
		return maphack != null;
	}

	public void load(Maphack info) {
		this.maphack = info;
	}

	public void unload() {
		this.maphack = null;
	}

	public boolean hasMaphack(String mapname) {
		return hacklist.get(toLowerCase(mapname)) != null;
	}

	public Spriteext getSpriteInfo(int spriteid) {
		if(maphack != null) {
			return maphack.getSpriteInfo(spriteid);
		}
		return null;
	}

}
