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
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.CharInfo;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

public class MenuConteiner extends MenuItem
{
	public int num;
	public MenuProc callback;
	public char[][] list;
	public Font listFont;
	public boolean listShadow;
	
	public MenuConteiner(Object text, Font font, int x, int y, int width, String[] list, int num, MenuProc callback)
	{
		super(text, font);
		this.listFont = font;
		this.flags = 3 | 4;
		if(list != null)
		{
			this.list = new char[list.length][];
			for(int i = 0; i < list.length; i++) {
				this.list[i] = list[i].toCharArray();
			}
		}

		this.x = x;
		this.y = y;
		this.width = width;
		this.callback = callback;
		this.num = num;
		this.pal = 0;
	}
	
	public MenuConteiner(Object text, Font font, Font listFont, int x, int y, int width, String[] list, int num, MenuProc callback)
	{
		this(text, font, x, y, width, list, num, callback);
		this.listFont = listFont;
	}
	
	@Override
	public void draw(MenuHandler handler) {
		int px = x, py = y;
		
		char[] key = null;
		if(list != null && num != -1 && num < list.length) {
			key = list[num];
		}

		int pal = handler.getPal(font, this);
		int shade = handler.getShade(this);
		int w1 = font.drawTextScaled(handler.getRenderer(), px, py, text, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
		
		if(key != null) {
			int bound = 10;
			int tx = px + w1 + bound;
			int ty = py + (font.getSize() - listFont.getSize()) / 2;
			brDrawText(handler.getRenderer(), listFont, key, tx, ty, shade, handler.getPal(listFont, this), px + width - 1);
		}
		handler.mPostDraw(this);
	}
	
	protected void brDrawText(Renderer renderer, Font font, char[] text, int x, int y, int shade, int pal, int x2) {
		int tptr = text.length - 1;
		while(tptr >= 0 && x2 > x) {
			CharInfo charInfo = font.getCharInfo(text[tptr]);
			x2 -= charInfo.getCellSize();
			font.drawCharScaled(renderer, x2, y, text[tptr], 1.0f, shade, pal, Transparent.None, ConvertType.Normal, listShadow);
			tptr--;
		}
	}

	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		
		switch(opt)
		{
		case LEFT:
		case MWDW:
			if ( (flags & 4) == 0 ) {
				return false;
			}
			if(num > 0) {
				num--;
			} else {
				num = 0;
			}
			if(callback != null) {
				callback.run(handler, this);
			}
			return false;
		case RIGHT:
		case MWUP:
			if ( (flags & 4) == 0 ) {
				return false;
			}
			if (list != null) {
				if (num < list.length - 1) {
					num++;
				} else {
					num = list.length - 1;
				}
			}
			if(callback != null) {
				callback.run(handler, this);
			}
			return false;
		case ENTER:
		case LMB:
			if ( (flags & 4) == 0 ) {
				return false;
			}
			if (list != null) {
				if (num < list.length - 1) {
					num++;
				} else {
					num = 0;
				}
			}
			if(callback != null) {
				callback.run(handler, this);
			}
			return false;
		default:
			return m_pMenu.mNavigation(opt);
		}
	}

	@Override
	public boolean mouseAction(int mx, int my) {
		if(text != null)
		{
			if(mx > x && mx < x + font.getWidth(text, 1.0f)) {
				if(my > y && my < y + font.getSize()) {
					return true;
				}
			}
		}
		
		if(list == null) {
			return false;
		}
		
		char[] key;
		if(num != -1 && num < list.length) {
			key = list[num];
			int fontx =  listFont.getWidth(key, 1.0f);
			int px = x + width - 1 - fontx;
			if(mx > px && mx < px + fontx) {
				return my > y && my < y + font.getSize();
			}
		}
		
		return false;
	}

	@Override
	public void open() {
	}

	@Override
	public void close() {
	}	
}
