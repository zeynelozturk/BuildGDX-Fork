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

import static ru.m210projects.Build.Gameutils.*;

import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;

public class MenuScroller extends MenuItem implements ScrollableMenuItem {

	protected MenuList parent;
	private boolean isLocked;
	protected SliderDrawable slider;
	protected int height;
	
	public MenuScroller(SliderDrawable slider, MenuList parent, int x) {
		super(null, null);
		
		this.flags = 2;
		this.parent = parent;
		this.slider = slider;
		this.x = x;
		this.y = parent.y;

		this.height = parent.rowCount * parent.mFontOffset();
		this.width = slider.getScrollerWidth();
	}

	@Override
	public void draw(MenuHandler handler) {
		int nList = BClipLow(parent.len - parent.rowCount, 1);
		int nRange = height - slider.getScrollerHeight();
		int posy = y + nRange * parent.l_nMin / nList;
		
		slider.drawScrollerBackground(x, y, height, 0, pal);
		slider.drawScroller(x, posy, 0, pal);
	}

	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		return m_pMenu.mNavigation(opt);
	}

	@Override
	public boolean mouseAction(int mx, int my) {
		if(mx >= x && mx < x + width) {
			if(my >= y && my < y + height) {
				if(m_pMenu.m_pItems[m_pMenu.m_nFocus] != this) {
					for ( short i = 0; i < m_pMenu.m_nItems; ++i ) {
						if(m_pMenu.m_pItems[i] == this) {
							m_pMenu.m_nFocus = i; // autofocus to scroller
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean onMoveSlider(MenuHandler handler, int scaledX, int scaledY) {
		if (isLocked) {
			int nList = BClipLow(parent.len - parent.rowCount, 1);
			int nRange = height - slider.getScrollerHeight();

			parent.l_nFocus = -1;
			parent.l_nMin = BClipRange(((scaledY - y) * nList) / nRange, 0, nList);
			return true;
		}
		return false;
	}

	@Override
	public boolean onLockSlider(MenuHandler handler, int mx, int my) {
		int cx = x + width - slider.getSliderRange();
		if(mx > cx && mx < cx + slider.getSliderRange()) {
			if(my >= y && my < y + height) {
				isLocked = true;
				onMoveSlider(handler, mx, my);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onUnlockSlider() {
		isLocked = false;
	}

	@Override
	public void open() {
	}

	@Override
	public void close() {
	}

}
