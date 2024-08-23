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

import static ru.m210projects.Build.net.Mmulti.NETPORT;
import static ru.m210projects.Build.Pattern.MenuItems.MenuTextField.LETTERS;
import static ru.m210projects.Build.Pattern.MenuItems.MenuTextField.NUMBERS;

import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.BuildMenu;
import ru.m210projects.Build.Pattern.MenuItems.MenuButton;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;
import ru.m210projects.Build.Pattern.MenuItems.MenuItem;
import ru.m210projects.Build.Pattern.MenuItems.MenuProc;
import ru.m210projects.Build.Pattern.MenuItems.MenuSlider;
import ru.m210projects.Build.Pattern.MenuItems.MenuSwitch;
import ru.m210projects.Build.Pattern.MenuItems.MenuTextField;
import ru.m210projects.Build.Pattern.MenuItems.MenuTitle;
import ru.m210projects.Build.Pattern.ScreenAdapters.ConnectAdapter;
import ru.m210projects.Build.Types.font.Font;

public abstract class MenuCreate extends BuildMenu {
	
	protected int mPlayers = 2;
	protected boolean mUseFakeMultiplayer = false;
	
	public MenuSlider mPlayerNum;
	public MenuTextField mPortnum;
	public MenuTextField mPlayer;
	public MenuSwitch mMenuFakeMM;
	public MenuButton mCreate;
	
	public MenuCreate(final BuildGame app, int posx, int posy, int menuHeight, int width, Font style, int kMaxPlayers)
	{
		super(app.pMenu);
		addItem(getTitle(app, "Multiplayer"), false);
		
		mPlayerNum = new MenuSlider(app.pSlider, "Number of players", style, posx, posy += menuHeight, width, mPlayers, 1,
				kMaxPlayers, 1, new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSlider slider = (MenuSlider) pItem;
						mPlayers = slider.value;
					}
				}, true);

		mPortnum = new MenuTextField("Network socket number", "", style, posx, posy += menuHeight, width,
				NUMBERS, (handler, pItem) -> {
                    MenuTextField item = (MenuTextField) pItem;
                    String numbers = item.getText();
                    if(numbers.length() < 8) {
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

		mPlayer = new MenuTextField("Player name", "", style, posx, posy += menuHeight, width, NUMBERS | LETTERS,
                (handler, pItem) -> app.pCfg.setpName(((MenuTextField) pItem).getText())) {
			
			@Override
			public void open() {
				setText(app.pCfg.getpName());
			}
		};
		
		mMenuFakeMM = new MenuSwitch("Use fake multiplayer", style, posx, posy += menuHeight, width, false, new MenuProc() {
			@Override
			public void run(MenuHandler handler, MenuItem pItem) {
				MenuSwitch sw = (MenuSwitch) pItem;
				mUseFakeMultiplayer = sw.value;
			}
		}, "Yes", "No") {
			@Override
			public void open() {
				value = mUseFakeMultiplayer;
			}
		};

		mCreate = new MenuButton("Create", style, 0, posy + (2 * menuHeight), 320, 1, 0, null, -1, new MenuProc() {
			@Override
			public void run(MenuHandler handler, MenuItem pItem) {
				if (app.getScreen() instanceof ConnectAdapter) {
					return;
				}

				String[] param = new String[] { "-n0" + (mPlayers != 2 ? (":" + mPlayers) : ""),
						(app.pCfg.getPort() != NETPORT ? ("-p " + app.pCfg.getPort()) : null) };

				createGame(mPlayers, mUseFakeMultiplayer, param);
			}
		}, 0);

		addItem(mPlayerNum, true);
		addItem(mPortnum, false);
		addItem(mPlayer, false);
		addItem(mMenuFakeMM, false);
		addItem(mCreate, false);
	}
	
	public abstract MenuTitle getTitle(BuildGame app, String text);
	
	public abstract void createGame(int mPlayers, boolean mUseFakeMultiplayer, String[] param);

}
