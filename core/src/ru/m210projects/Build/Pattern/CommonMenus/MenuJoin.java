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
import ru.m210projects.Build.Pattern.ScreenAdapters.ConnectAdapter;
import ru.m210projects.Build.Types.font.Font;

import static ru.m210projects.Build.net.Mmulti.NETPORT;
import static ru.m210projects.Build.Pattern.MenuItems.MenuTextField.*;

public abstract class MenuJoin extends BuildMenu {

    public MenuTextField mPortnum;
    public MenuTextField mPlayer;
    public MenuTextField mIPAddress;
    public MenuButton mConnect;

    public MenuJoin(final BuildGame app, int posx, int posy, int menuHeight, int width, Font style) {
        super(app.pMenu);
        addItem(getTitle(app, "Join a game"), false);

        mPortnum = new MenuTextField("Network socket number", "", style, posx, posy += menuHeight, width,
                NUMBERS, (handler, pItem) -> {
            MenuTextField item = (MenuTextField) pItem;
            String numbers = item.getText();
            if (numbers.length() < 8) {
                app.pCfg.setPort(Integer.parseInt(numbers));
            } else {
                mPortnum.setText("" + app.pCfg.getPort());
            }
        }) {
            @Override
            public void open() {
                setText("" + app.pCfg.getPort());
            }
        };

        mPlayer = new MenuTextField("Player name", app.pCfg.getpName(), style, posx, posy += menuHeight, width, NUMBERS | LETTERS,
                (handler, pItem) -> app.pCfg.setpName(((MenuTextField) pItem).getText())) {
            @Override
            public void open() {
                setText(app.pCfg.getpName());
            }
        };

        mIPAddress = new MenuTextField("IP Address", app.pCfg.getmAddress(), style, posx, posy += menuHeight, width,
                NUMBERS | LETTERS | POINT, new MenuProc() {
            @Override
            public void run(MenuHandler handler, MenuItem pItem) {
                MenuTextField item = (MenuTextField) pItem;
                app.pCfg.setmAddress(item.getText());
            }
        }) {
            @Override
            public void open() {
                setText(app.pCfg.getmAddress());
            }
        };

        mConnect = new MenuButton("Connect", style, 0, posy + (2 * menuHeight), 320, 1, 0, null, -1, new MenuProc() {
            @Override
            public void run(MenuHandler handler, MenuItem pItem) {
                if (app.getScreen() instanceof ConnectAdapter) {
                    return;
                }

                if (app.pCfg.getmAddress().isEmpty()) {
                    return;
                }
                String[] param = new String[]{"-n0", app.pCfg.getmAddress(), (app.pCfg.getPort() != NETPORT ? ("-p " + app.pCfg.getPort()) : null)};
                joinGame(param);
            }
        }, 0);

        addItem(mPortnum, true);
        addItem(mPlayer, false);
        addItem(mIPAddress, false);
        addItem(mConnect, false);
    }

    public abstract MenuTitle getTitle(BuildGame app, String text);

    public abstract void joinGame(String[] param);
}
