// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Types.collections;

import ru.m210projects.Build.Types.Sprite;

import java.util.List;

public class SpriteMap extends LinkedMap<Sprite> {

    public SpriteMap(int listCount, List<Sprite> spriteList, int spriteCount, ValueSetter<Sprite> valueSetter) {
        super(listCount, spriteList, spriteCount, valueSetter);
    }

    @Override
    protected Sprite getInstance() {
        return new Sprite();
    }

    @Override
    protected void setValue(ListNode<Sprite> node, int value) {
        super.setValue(node, (value == -1) ? poolIndex : value);
    }
}
