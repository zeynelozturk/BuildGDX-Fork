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

package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Types.font.Font;

public abstract class FontHandler {

	protected Font[] fonts;

	public FontHandler(int nFonts) {
		fonts = new Font[nFonts];
	}
	
	protected abstract Font init(int i);
	
	public Font getFont(int i) {
		if(i < 0 || i >= fonts.length) {
			throw new IllegalArgumentException("Wrong font number " + i);
		}
		
		if(fonts[i] == null) {
			fonts[i] = init(i);
		}

		return fonts[i];
	}

	public int getSize() {
		return fonts.length;
	}

}
