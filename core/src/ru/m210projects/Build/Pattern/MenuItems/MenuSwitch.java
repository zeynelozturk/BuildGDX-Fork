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

public class MenuSwitch extends MenuItem
{
	public boolean value;
	public MenuProc callback;
	char[] onMessage, offMessage;
	public Font switchFont;
	
	public MenuSwitch(Object text, Font font, int x, int y, int width, boolean value,
			MenuProc callback, String onMessage, String offMessage) 
	{
		super(text, font);
		this.flags = 3 | 4;
		this.x = x;
		this.y = y;
		this.width = width;
		this.value = value;
		this.callback = callback;
		if(onMessage != null) {
			this.onMessage = onMessage.toCharArray();
		} else {
			this.onMessage = new char[]{ 'O', 'n' };
		}
		if(offMessage != null) {
			this.offMessage = offMessage.toCharArray();
		} else {
			this.offMessage = new char[]{ 'O', 'f', 'f' };
		}
		
		this.switchFont = font;
	}
	
	@Override
	public void draw(MenuHandler handler) {
		int shade = handler.getShade(this);
		int pal = handler.getPal(font, this);
		
		if ( text != null ) {
			font.drawTextScaled(handler.getRenderer(), x, y, text, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal,  fontShadow);
		}
		char[] sw = offMessage;
		if(value) {
			sw = onMessage;
		}
		
		switchFont.drawTextScaled(handler.getRenderer(), x + width - 1 - switchFont.getWidth(sw, 1.0f), y + (font.getSize() - switchFont.getSize()) / 2, sw, 1.0f, shade, handler.getPal(switchFont, this), TextAlign.Left, Transparent.None, ConvertType.Normal,  fontShadow);
		handler.mPostDraw(this);
	}
	
	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		if(opt == MenuOpt.LEFT || opt == MenuOpt.RIGHT || opt == MenuOpt.ENTER || opt == MenuOpt.LMB)
		{
			if ( (flags & 4) == 0 ) {
				return false;
			}
			
			value = !value;
			if(callback != null) {
				callback.run(handler, this);
			}
			return false;
		} else {
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
		
		char[] sw = offMessage;
		if(value) {
			sw = onMessage;
		}

		int fontx = font.getWidth(sw, 1.0f);
		int px = x + width - 1 - fontx;
		if(mx > px && mx < px + fontx) {
			return my > y && my < y + font.getSize();
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