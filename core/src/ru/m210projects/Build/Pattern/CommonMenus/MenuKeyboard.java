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

package ru.m210projects.Build.Pattern.CommonMenus;

import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Types.font.Font;

public abstract class MenuKeyboard extends BuildMenu {

    public MenuKeyboardList mList;
    public MenuText mText;
    public MenuText mText2;

    public MenuKeyboard(final BuildGame app, int posx, int posy, int width, int list_len, Font style) {
        super(app.pMenu);
        addItem(getTitle(app, "Configure keys"), false);

        MenuProc callback = (handler, pItem) -> {
            MenuKeyboardList item = (MenuKeyboardList) pItem;
            if (item.l_set == 0) {
                item.l_pressedId = null;
                item.l_set = 1;
            }
        };

        mList = new MenuKeyboardList(app.pSlider, app.pCfg, style, posx, posy, width, list_len, callback) {
            @Override
            public String getKeyName(int keycode) {
                return keyNames(keycode);
            }
        };

        posy += mList.mFontOffset() * list_len;

        mText = new MenuText("UP/DOWN = Select action", style, 160, posy += 2 * mList.mFontOffset(), 1);
        mText2 = new MenuText("Enter = modify  Delete = clear", style, 160, posy += mList.mFontOffset(), 1);

        addItem(mList, true);
        addItem(mText, false);
        addItem(mText2, false);
    }

    public abstract MenuTitle getTitle(BuildGame app, String text);

    public abstract String keyNames(int keycode);

}
