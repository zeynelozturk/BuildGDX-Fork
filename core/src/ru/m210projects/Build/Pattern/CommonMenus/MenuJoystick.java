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

import com.badlogic.gdx.controllers.Controller;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.settings.GameConfig;

import java.util.List;
import java.util.stream.IntStream;

public abstract class MenuJoystick extends BuildMenu {

    public MenuConteiner mJoyDevices;
    public MenuButton mJoyKey;
    public MenuSlider mDeadZone;
    public MenuSlider mLookSpeed;
    public MenuSlider mTurnSpeed;
    public MenuSwitch mInvert;
    public MenuJoyList mList;

    public MenuText mText;
    public MenuText mText2;

    public BuildMenu joyButtons;

    public MenuJoystick(final BuildGame app, int posx, int posy, int width, int menuHeight, int separatorHeight, Font style, int list_len) {
        super(app.pMenu);
        addItem(getTitle(app, "Joystick setup"), false);

        GameConfig cfg = app.pCfg;

        joyButtons = getJoyButtonsMenu(this, app, width, style, posx, posy, list_len);

        mJoyDevices = new MenuConteiner("Device", style, posx, posy, width, null, 0,
                (handler, pItem) -> {
                    MenuConteiner item = (MenuConteiner) pItem;
                    if (item.num == 0) {
                        cfg.setControllerName("");
                    } else {
                        String controllerName = new String(mJoyDevices.list[item.num]);
                        Controller controller = cfg.getControllers().stream().filter(e -> e.getName().equalsIgnoreCase(controllerName)).findAny().orElse(null);
                        if (controller != null && controller.isConnected()) {
                            cfg.setControllerName(controllerName);
                            return;
                        }

                        item.num = 0;
                        cfg.setControllerName("");
                        updateControllerList(cfg.getControllers());
                    }
                }) {
            @Override
            public void open() {
                updateControllerList(cfg.getControllers());
                String controllerName = cfg.getControllerName();
                num = IntStream.range(0, list.length).filter(e -> new String(list[e]).equalsIgnoreCase(controllerName)).boxed().findAny().orElse(0);
            }
        };

        mJoyKey = new MenuButton("Configure buttons", style, posx, posy += separatorHeight, width, 1, 0, joyButtons, -1,
                null, 0) {
            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(mJoyDevices.num != 0);
                super.draw(handler);
            }
        };

        posy += 5;
        mDeadZone = new MenuSlider(app.pSlider, "Dead zone", style, posx, posy += menuHeight, width, (int) (cfg.getJoyDeadZone() * 65536), 0, 65536,
                2048, (handler, pItem) -> {
                    MenuSlider slider = (MenuSlider) pItem;
                    cfg.setJoyDeadZone(slider.value / 65536.0f);
                }, true);
        mDeadZone.digitalMax = 65536.0f;

        mLookSpeed = new MenuSlider(app.pSlider, "Look speed", style, posx, posy += menuHeight, width, (int) (cfg.getJoyLookSpeed() * 65536), 0,
                999424, 4096, (handler, pItem) -> {
                    MenuSlider slider = (MenuSlider) pItem;
                    cfg.setJoyLookSpeed(slider.value / 65536.0f);
                }, true);
        mLookSpeed.digitalMax = 65536.0f;

        mTurnSpeed = new MenuSlider(app.pSlider, "Turn speed", style, posx, posy += menuHeight, width, (int) (cfg.getJoyTurnSpeed() * 65536), 0,
                999424, 4096, (handler, pItem) -> {
                    MenuSlider slider = (MenuSlider) pItem;
                    cfg.setJoyTurnSpeed(slider.value / 65536.0f);
                }, true);
        mTurnSpeed.digitalMax = 65536.0f;

        mInvert = new MenuSwitch("Invert look axis", style, posx, posy + separatorHeight, width, cfg.isJoyInvert(), (handler, pItem) -> {
            MenuSwitch sw = (MenuSwitch) pItem;
            cfg.setJoyInvert(sw.value);
        }, "Yes", "No");

        addItem(mJoyDevices, true);
        addItem(mJoyKey, false);
        addItem(mDeadZone, false);
        addItem(mLookSpeed, false);
        addItem(mTurnSpeed, false);
        addItem(mInvert, false);
    }

    public abstract MenuTitle getTitle(BuildGame app, String text);

    public abstract String keyNames(int keycode);

    private void updateControllerList(List<Controller> controllers) {
        if (!controllers.isEmpty()) {
            mJoyDevices.list = new char[controllers.size() + 1][];
            mJoyDevices.list[0] = "Disabled".toCharArray();
            for (int i = 0; i < controllers.size(); i++) {
                mJoyDevices.list[i + 1] = controllers.get(i).getName().toCharArray();
            }
        } else {
            mJoyDevices.list = new char[][]{"No joystick devices found".toCharArray()};
        }
    }

    public BuildMenu getJoyButtonsMenu(MenuJoystick parent, final BuildGame app, int width, Font style, int posx, int posy, int list_len) {
        BuildMenu menu = new BuildMenu(app.pMenu);

        menu.addItem(parent.getTitle(app, "Config. buttons"), false);

        MenuProc callback = (handler, pItem) -> {
            MenuJoyList item = (MenuJoyList) pItem;
            if (item.l_set == 0) {
                item.l_set = 1;
            }
        };

        mList = new MenuJoyList(app, style, posx, posy, width, list_len, callback);

        posy += mList.mFontOffset() * list_len;

        mText = new MenuText("UP/DOWN = Select action", style, 160, posy += 2 * mList.mFontOffset(), 1);
        mText2 = new MenuText("Enter = modify  Delete = clear", style, 160, posy += mList.mFontOffset(), 1);

        menu.addItem(mList, true);
        menu.addItem(mText, false);
        menu.addItem(mText2, false);

        return menu;
    }

}
