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

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.filehandle.fs.FileEntry;

public abstract class MenuLoadSave extends BuildMenu {

    public MenuPicnum picnum;
    public MenuSlotList list;
    public MenuText mInfo;

    public MenuLoadSave(BuildGame app, Font style, int posx, int posy, int posyHelp, int width, int nItems, int listPal, int specPal, int nBackground, MenuProc confirm, boolean saveMenu) {
        super(app.pMenu);
        addItem(getTitle(app, saveMenu ? "Save game" : "Load game"), false);

        picnum = getPicnum(app.pEngine, posx, posy);

        MenuProc updateCallback = (handler, pItem) -> {
            MenuSlotList pSlot = (MenuSlotList) pItem;
            if (loadData(pSlot.getFileEntry())) {
                picnum.nTile = SaveManager.Screenshot;
            } else {
                picnum.nTile = picnum.defTile;
            }
        };

        list = new MenuSlotList(app.getRenderer(), app.pSavemgr, style, posx, posy, posyHelp, width, nItems, updateCallback, confirm, listPal, specPal, nBackground, saveMenu) {
            @Override
            public boolean checkFile(FileEntry entry) {
                return MenuLoadSave.this.checkFile(entry);
            }

            @Override
            protected void drawOwnCursor(int x, int y) {
                MenuLoadSave.this.drawOwnCursor(x, y);
            }
        };

        // doenst work
        MenuScroller slider = new MenuScroller(app.pSlider, list, width + posx - app.pSlider.getScrollerWidth());
        mInfo = getInfo(app, posx, posy);

        addItem(picnum, false);
        addItem(mInfo, false);
        addItem(list, true);
        addItem(slider, false);
    }

    public boolean checkFile(FileEntry entry) {
        return entry.exists();
    }

    protected void drawOwnCursor(int x, int y) {
    }

    public abstract boolean loadData(FileEntry entry);

    public abstract MenuTitle getTitle(BuildGame app, String text);

    public abstract MenuPicnum getPicnum(Engine draw, int x, int y);

    public abstract MenuText getInfo(BuildGame app, int x, int y);

}
