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

package ru.m210projects.Build.Pattern.MenuItems;

import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

public class MenuText extends MenuItem
{
	public MenuText(Object text, Font font, int x, int y, int align) {
		super(text, font);
		this.flags = 1;

		this.x = x;
		this.y = y;
		this.width = 0;
		this.align = align;
	}

	@Override
	public void draw(MenuHandler handler) {
		if ( text != null )
		{
		    int px = x;
		    if(align == 1) {
				px = width / 2 + x - font.getWidth(text, 1.0f) / 2;
			}
		    if(align == 2) {
				px = x + width - 1 - font.getWidth(text, 1.0f);
			}

		    font.drawTextScaled(handler.getRenderer(), px, y, text, 1.0f,-128, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
		}
		handler.mPostDraw(this);
	}

	public MenuText setFontShadow(boolean shadow) {
		this.fontShadow = shadow;
		return this;
	}

	@Override
	public boolean mouseAction(int x, int y) {
		return false;
	}

	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		return m_pMenu.mNavigation(opt);
	}

	@Override
	public void open() {}

	@Override
	public void close() {}
}