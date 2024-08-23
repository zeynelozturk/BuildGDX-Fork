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

package ru.m210projects.Build.Pattern.MenuItems;

import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;

public class DummyItem extends MenuItem {

	public DummyItem() {
		super(null, null);
	}

	@Override
	public void draw(MenuHandler handler) {
	}

	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		return false;
	}

	@Override
	public boolean mouseAction(int mx, int my) {
		return false;
	}

	@Override
	public void open() {
	}

	@Override
	public void close() {
	}
}
