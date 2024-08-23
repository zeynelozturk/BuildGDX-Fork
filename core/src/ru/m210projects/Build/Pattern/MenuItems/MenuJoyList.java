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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.input.keymap.ControllerAxis;
import ru.m210projects.Build.input.keymap.ControllerButton;
import ru.m210projects.Build.settings.ControllerMapping;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.settings.GameKeys;

import java.util.ArrayList;
import java.util.List;

import static ru.m210projects.Build.Gameutils.BClipLow;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.input.GameKey.UNKNOWN_KEY;

public class MenuJoyList extends MenuList implements ScrollableMenuItem, InputListener {

    protected SliderDrawable slider;
    protected GameConfig cfg;
    protected List<String> components = new ArrayList<>();
    public int l_set = 0;
    protected int scrollerX, scrollerHeight;
    protected boolean isLocked;
    public int pal_left, pal_right, axispal;

    public MenuJoyList(BuildGame app, Font font, int x, int y, int width, int rowCount, MenuProc callback) {
        super(null, font, x, y, width, 0, callback, rowCount);
        this.slider = app.pSlider;
        this.cfg = app.pCfg;

        ControllerAxis[] axes = ControllerAxis.values();
        for(int i = 1; i < axes.length; i++) {
            components.add(axes[i].name());
        }

        ControllerButton[] buttons = ControllerButton.values();
        for(int i = 1; i < buttons.length; i++) {
            components.add(buttons[i].name());
        }

        this.len = components.size();
    }

    @Override
    public void draw(MenuHandler handler) {
        int px = x, py = y;
        int totalclock = handler.game.pEngine.getTotalClock();
        for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < len; i++) {
            int shade = handler.getShade(i == l_nFocus ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
            int pal1 = i < 4 ? axispal : this.pal_left;
            int pal2 = this.pal_right;

            if (i == l_nFocus) {
                pal2 = pal1 = handler.getPal(font, m_pMenu.m_pItems[m_pMenu.m_nFocus]);
            }

            ControllerMapping mapping = cfg.getControllerMapping(cfg.getControllerName());
            String name = components.get(i);
            String componentName = UNKNOWN_KEY.getName();
            if (i < 4) {
                ControllerAxis axis = getAxis(name);
                int id = mapping.getAxisCode(axis);
                if (id != -1) {
                    componentName = "Axis_" + id;
                }
            } else {
                ControllerButton button = getButton(name);
                int id = mapping.getButtonCode(button);
                if (id != -1) {
                    componentName = "Button_" + id;
                }
            }

            if (i == l_nFocus) {
                if (l_set == 1 && (totalclock & 0x20) != 0) {
                    componentName = "____";
                }
            }

            font.drawTextScaled(handler.getRenderer(), px, py, name.toCharArray(), 1.0f, shade, pal1, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
            font.drawTextScaled(handler.getRenderer(), x + width - slider.getScrollerWidth() - 2, py, componentName, 1.0f, shade, pal2, TextAlign.Right, Transparent.None, ConvertType.Normal, fontShadow);

            py += mFontOffset();
        }

        scrollerHeight = rowCount * mFontOffset();

        //Files scroll
        int nList = BClipLow(len - rowCount, 1);
        int posy = y + (scrollerHeight - slider.getScrollerHeight()) * l_nMin / nList;

        scrollerX = x + width - slider.getScrollerWidth() + 5;
        slider.drawScrollerBackground(scrollerX, y, scrollerHeight, 0, 0);
        slider.drawScroller(scrollerX, posy, handler.getShade(isLocked ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null), 0);

        handler.mPostDraw(this);
    }

    private ControllerAxis getAxis(String name) {
        try {
            return ControllerAxis.valueOf(name);
        } catch (Exception ignore) {
        }
        return ControllerAxis.NULL;
    }

    private ControllerButton getButton(String name) {
        try {
            return ControllerButton.valueOf(name);
        } catch (Exception ignore) {
        }
        return ControllerButton.NULL;
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        if (l_set == 1 && GameKeys.Menu_Toggle.equals(gameKey)) {
            l_set = 0;
            return true;
        }
        return l_set == 1; // block handler when key setting
    }

    @Override
    public boolean keyDown(int keycode) {
        if (l_set == 1) {
            if (keycode == Input.Keys.ESCAPE) {
                l_set = 0;
            }
            return true;
        }

        if (keycode == Input.Keys.FORWARD_DEL) {
            ControllerMapping mapping = cfg.getControllerMapping(cfg.getControllerName());
            if (l_nFocus < 4) {
                mapping.removeAxis(getAxis(components.get(l_nFocus)));
            } else {
                mapping.removeButton(getButton(components.get(l_nFocus)));
            }
        }
        return false;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (l_set == 1 && l_nFocus > 3) {
            ControllerMapping mapping = cfg.getControllerMapping(controller.getName());
            ControllerButton button = getButton(components.get(l_nFocus));
            if (button != ControllerButton.NULL) {
                mapping.putButton(button, buttonCode);
                l_set = 0;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (l_set == 1 && l_nFocus < 4 && value >= 0.5f) {
            ControllerMapping mapping = cfg.getControllerMapping(controller.getName());
            ControllerAxis axis = getAxis(components.get(l_nFocus));
            if (axis != ControllerAxis.NULL) {
                mapping.putAxis(axis, axisCode);
                l_set = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseAction(int mx, int my) {
        if (l_set != 0) {
            return false;
        }

        int py = y;
        for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < len; i++) {
            if (my >= py && my < py + font.getSize()) {
                l_nFocus = i;
                return true;
            }

            py += mFontOffset();
        }

        return false;
    }

    @Override
    public boolean onMoveSlider(MenuHandler handler, int scaledX, int scaledY) {
        if (isLocked) {

            if (len <= rowCount) {
                return false;
            }

            int nList = BClipLow(len - rowCount, 1);
            int nRange = scrollerHeight;
            int py = y;

            l_nFocus = -1;
            l_nMin = BClipRange(((scaledY - py) * nList) / nRange, 0, nList);

            return true;
        }
        return false;
    }

    @Override
    public boolean onLockSlider(MenuHandler handler, int mx, int my) {
        if (l_set != 0) {
            return false;
        }

        if (mx > scrollerX && mx < scrollerX + slider.getScrollerWidth()) {
            isLocked = true;
            onMoveSlider(handler, mx, my);
            return true;
        }
        return false;
    }

    @Override
    public void onUnlockSlider() {
        isLocked = false;
    }

    @Override
    public void close() {
        // to reset gamestate if key was rebinded
        menuHandler.game.getProcessor().resetPollingStates();
    }
}
