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

import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.settings.MouseAxis;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.input.GameKey;

import static ru.m210projects.Build.Pattern.MenuItems.MenuTextField.NUMBERS;
import static ru.m210projects.Build.Pattern.MenuItems.MenuTextField.POINT;

public abstract class MenuMouse extends BuildMenu {
	
	public BuildMenu advancedMenu;
	
	public MenuSwitch mEnable;
	public MenuSwitch mMenuEnab;
	public MenuSwitch mRawInput;
	public MenuTextField mSens;
	public MenuSlider mTurn;
	public MenuSlider mLook;
	public MenuSlider mMove;
	public MenuSlider mStrafe;
	public MenuSwitch mAiming;
	public MenuSwitch mInvert;
	public MenuButton mAdvance;
	
	public MenuConteiner mAxisUp;
	public MenuConteiner mAxisDown;
	public MenuConteiner mAxisLeft;
	public MenuConteiner mAxisRight;

	public abstract MenuTitle getTitle(BuildGame app, String text);

	public MenuMouse(final BuildGame app, int posx, int posy, int width, int menuHeight, int separatorHeight, Font style, int buttonPal)
	{
		super(app.pMenu);
		addItem(getTitle(app, "Mouse setup"), false);
		
		final GameConfig cfg = app.pCfg;
		
		advancedMenu = buildAdvancedAxisMenu(app, posx, posy, width, menuHeight, style);
		
		mEnable = new MenuSwitch("Mouse in game", style, posx, posy += menuHeight, width, cfg.isUseMouse(), new MenuProc() {
			@Override
			public void run(MenuHandler handler, MenuItem pItem) {
				MenuSwitch sw = (MenuSwitch) pItem;
				cfg.setUseMouse(sw.value);
			}
		}, "Yes", "No");
		mEnable.pal = buttonPal;

		mMenuEnab = new MenuSwitch("Mouse in menu", style, posx, posy += menuHeight, width, cfg.isMenuMouse(),
				new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSwitch sw = (MenuSwitch) pItem;
						cfg.setMenuMouse(sw.value);
					}
				}, "Yes", "No");
		mMenuEnab.pal = buttonPal;

		mRawInput = new MenuSwitch("Raw Mouse Input", style, posx, posy += menuHeight, width, cfg.isRawInput(), (handler, pItem) -> {
            MenuSwitch sw = (MenuSwitch) pItem;
            cfg.setRawInput(sw.value);
        }, "Yes", "No") {
			@Override
			public void draw(MenuHandler handler) {
				super.draw(handler);
				this.mCheckEnableItem(cfg.isRawInputSupported());
			}
		};
		mRawInput.pal = buttonPal;
		
		posy += separatorHeight;
		mSens = new MenuTextField("Mouse Sensitivity", "", style, posx, posy += menuHeight, width,
				NUMBERS | POINT, (handler, pItem) -> {
			MenuTextField item = (MenuTextField) pItem;
			String numbers = item.getText();
			double mouseSens = Double.parseDouble(numbers);

			cfg.setgSensitivity((int) (mouseSens * 65536.0f));
		}) {
			@Override
			public void open() {
				setText("" + (cfg.getSensitivity() / 65536.0f));
			}
		};
		mSens.pal = buttonPal;

		mTurn = new MenuSlider(app.pSlider, "Turning speed", style, posx, posy += menuHeight, width, cfg.getgMouseTurnSpeed(), 0,
				0x28000, 4096, (handler, pItem) -> {
                    MenuSlider slider = (MenuSlider) pItem;
                    cfg.setgMouseTurnSpeed(slider.value);
                }, true);
		mTurn.digitalMax = 65536f;
		mTurn.pal = buttonPal;

		mLook = new MenuSlider(app.pSlider, "Aiming up/down speed", style, posx, posy += menuHeight, width, cfg.getgMouseLookSpeed(), 0,
				0x28000, 4096, new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSlider slider = (MenuSlider) pItem;
						cfg.setgMouseLookSpeed(slider.value);
					}
		}, true);
		mLook.digitalMax = 65536f;
		mLook.pal = buttonPal;

		mMove = new MenuSlider(app.pSlider, "Forward/Backward speed", style, posx, posy += menuHeight, width, cfg.getgMouseMoveSpeed(),
				0, 0x28000, 4096, new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSlider slider = (MenuSlider) pItem;
						cfg.setgMouseMoveSpeed(slider.value);
					}
		}, true);
		mMove.digitalMax = 65536f;
		mMove.pal = buttonPal;

		mStrafe = new MenuSlider(app.pSlider, "Strafing speed", style, posx, posy += menuHeight, width, cfg.getgMouseStrafeSpeed(), 0,
				0x28000, 4096, new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSlider slider = (MenuSlider) pItem;
						cfg.setgMouseStrafeSpeed(slider.value);
					}
		}, true);
		mStrafe.digitalMax = 65536f;
		mStrafe.pal = buttonPal;

		posy += separatorHeight;

		mAiming = new MenuSwitch("Mouse aiming", style, posx, posy += menuHeight, width, cfg.isgMouseAim(), new MenuProc() {
			@Override
			public void run(MenuHandler handler, MenuItem pItem) {
				MenuSwitch sw = (MenuSwitch) pItem;
				cfg.setgMouseAim(sw.value);
			}
		}, null, null);
		mAiming.pal = buttonPal;
		
		mInvert = new MenuSwitch("Invert mouse aim", style, posx, posy += menuHeight, width, cfg.isgInvertmouse(),
				new MenuProc() {
					@Override
					public void run(MenuHandler handler, MenuItem pItem) {
						MenuSwitch sw = (MenuSwitch) pItem;
						cfg.setgInvertmouse(sw.value);
					}
				}, null, null);
		mInvert.pal = buttonPal;

		posy += separatorHeight;

		mAdvance = new MenuButton("Digital axis setup", style, posx, posy + menuHeight, width, 1, buttonPal, advancedMenu, -1, null, 0);
		
		addItem(mEnable, true);
		addItem(mMenuEnab, false);
		addItem(mRawInput, false);
		addItem(mSens, false);

		addItem(mTurn, false);
		addItem(mLook, false);
		addItem(mMove, false);
		addItem(mStrafe, false);

		addItem(mAiming, false);
		addItem(mInvert, false);
		addItem(mAdvance, false);
	}
	
	protected BuildMenu buildAdvancedAxisMenu(BuildGame app, int posx, int posy, int width, int menuHeight, Font style) {
		BuildMenu advancedMenu = new BuildMenu(app.pMenu);
		
		advancedMenu.addItem(getTitle(app, "Digital axis"), false);
		final GameConfig cfg = app.pCfg;

		GameKey[] gameKeys = cfg.getKeymap();
		char[][] keymaplist = new char[gameKeys.length + 1][];
		keymaplist[0] = "None".toCharArray();
		for (int i = 1; i < keymaplist.length; i++) {
			keymaplist[i] = gameKeys[i - 1].getName().toCharArray();
		}

		mAxisUp = new MenuConteiner("Digital up", style, posx, posy += 10, width, null, 0, null) {
			@Override
			public void open() {
				num = Gameutils.arrayIndexOf(gameKeys, i -> gameKeys[i].equals(cfg.getMouseAxis(MouseAxis.UP))) + 1;
			}
			
			@Override
			public boolean callback(MenuHandler handler, MenuOpt opt) {
				return mAdvancedCallback(handler, cfg, this, opt, MouseAxis.UP);
			}
		};
		
		mAxisDown = new MenuConteiner("Digital down", style, posx, posy += 10, width, null, 0, null) {
			@Override
			public void open() {
				num = Gameutils.arrayIndexOf(gameKeys, i -> gameKeys[i].equals(cfg.getMouseAxis(MouseAxis.DOWN))) + 1;
			}
			
			@Override
			public boolean callback(MenuHandler handler, MenuOpt opt) {
				return mAdvancedCallback(handler, cfg, this, opt, MouseAxis.DOWN);
			}
		};
		
		mAxisLeft = new MenuConteiner("Digital left", style, posx, posy += 10, width, null, 0, null) {
			@Override
			public void open() {
				num = Gameutils.arrayIndexOf(gameKeys, i -> gameKeys[i].equals(cfg.getMouseAxis(MouseAxis.LEFT))) + 1;
			}
			
			@Override
			public boolean callback(MenuHandler handler, MenuOpt opt) {
				return mAdvancedCallback(handler, cfg, this, opt, MouseAxis.LEFT);
			}
		};
		
		mAxisRight = new MenuConteiner("Digital right", style, posx, posy += 10, width, null, 0, null) {
			@Override
			public void open() {
				num = Gameutils.arrayIndexOf(gameKeys, i -> gameKeys[i].equals(cfg.getMouseAxis(MouseAxis.RIGHT))) + 1;
			}
			
			@Override
			public boolean callback(MenuHandler handler, MenuOpt opt) {
				return mAdvancedCallback(handler, cfg, this, opt, MouseAxis.RIGHT);
			}
		};
		
		mAxisUp.list = mAxisDown.list = mAxisLeft.list = mAxisRight.list = keymaplist;
		
		advancedMenu.addItem(mAxisUp, true);
		advancedMenu.addItem(mAxisDown, false);
		advancedMenu.addItem(mAxisLeft, false);
		advancedMenu.addItem(mAxisRight, false);
		
		return advancedMenu;
	}
	
	private boolean mAdvancedCallback(MenuHandler handler, GameConfig cfg, MenuConteiner item, MenuOpt opt, MouseAxis mouseAxis) {
		switch(opt)
		{
		case LEFT:
		case MWDW:
			if ( (item.flags & 4) == 0 ) {
				return false;
			}
			if(item.num > 0) {
				item.num--;
			} else {
				item.num = 0;
			}

			cfg.setMouseAxis(mouseAxis, item.num > 0 ? cfg.getKeymap()[item.num - 1] : GameKey.UNKNOWN_KEY);
			return false;
		case RIGHT:
		case MWUP:
			if ( (item.flags & 4) == 0 ) {
				return false;
			}
			if(item.num < item.list.length - 1) {
				item.num++;
			} else {
				item.num = item.list.length - 1;
			}
			cfg.setMouseAxis(mouseAxis, item.num > 0 ? cfg.getKeymap()[item.num - 1] : GameKey.UNKNOWN_KEY);
			return false;
		case ENTER:
		case LMB:
			if ( (item.flags & 4) == 0 ) {
				return false;
			}
			if(item.num < item.list.length - 1) {
				item.num++;
			} else {
				item.num = 0;
			}
			cfg.setMouseAxis(mouseAxis, item.num > 0 ? cfg.getKeymap()[item.num - 1] : GameKey.UNKNOWN_KEY);
			return false;
		default:
			return item.m_pMenu.mNavigation(opt);
		}
	}
}
