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
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.input.keymap.Keymap;
import ru.m210projects.Build.settings.GameConfig;

import static ru.m210projects.Build.Gameutils.BClipLow;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.settings.InputContext.*;

public abstract class MenuKeyboardList extends MenuList implements ScrollableMenuItem, InputListener {
    public int l_set = 0;
    public MenuOpt l_pressedId;
    public int pal_left;
    public int pal_right;
    protected GameKey[] keynames;
    protected GameConfig cfg;
    protected SliderDrawable slider;
    protected int scrollerX, scrollerHeight;
    protected boolean isLocked;

    public MenuKeyboardList(SliderDrawable slider, GameConfig cfg, Font font, int x, int y, int width, int len, MenuProc callback) {
        super(null, font, x, y, width, 0, callback, len);
        this.slider = slider;
        this.cfg = cfg;
        this.keynames = cfg.getKeymap();
        this.len = keynames.length;
    }

    public abstract String getKeyName(int keycode);

    @Override
    public void draw(MenuHandler handler) {
        int px = x, py = y;
        int totalclock = handler.game.pEngine.getTotalClock();
        for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < len; i++) {
            int shade = handler.getShade(i == l_nFocus ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
            int pal1 = this.pal_left;
            int pal2 = this.pal_right;

            if (i == l_nFocus) {
                pal2 = pal1 = handler.getPal(font, m_pMenu.m_pItems[m_pMenu.m_nFocus]);
            }

            String text = keynames[i].getName();
            String key = getKeyName(cfg.getKeyCode(keynames[i], PRIMARY_KEYS_INDEX));

            int secondKey = cfg.getKeyCode(keynames[i], SECONDARY_KEYS_INDEX);
            if (secondKey > 0) {
                key += " or " + getKeyName(secondKey);
            }

            if (i == l_nFocus) {
                if (l_set == 1 && (totalclock & 0x20) != 0) {
                    key = "____";
                }
            }

            char[] k = key.toCharArray();
            font.drawTextScaled(handler.getRenderer(), px, py, text.toCharArray(), 1.0f, shade, pal1, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);

            font.drawTextScaled(handler.getRenderer(), x + width / 2 - 1 + 40, py, k, 1.0f, shade, pal2, TextAlign.Right, Transparent.None, ConvertType.Normal, fontShadow);

            int mouseCode = cfg.getKeyCode(keynames[i], MOUSE_KEYS_INDEX);
            int joyCode = cfg.getKeyCode(keynames[i], GAMEPAD_KEYS_INDEX);
            key = (mouseCode > 0 ? getKeyName(mouseCode) : "") + ((mouseCode > 0 && joyCode > 0) ? " or " : "") + (joyCode > 0 ? getKeyName(joyCode) : "");
            key = key.isEmpty() ? "-" : key;

            if (i == l_nFocus) {
                if (l_set == 1 && (totalclock & 0x20) != 0) {
                    key = "____";
                }
            }
            k = key.toCharArray();
            font.drawTextScaled(handler.getRenderer(), x + width - slider.getScrollerWidth() - 2, py, k, 1.0f, shade, pal2, TextAlign.Right, Transparent.None, ConvertType.Normal, fontShadow);

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


    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        return l_set == 1; // block handler when key setting
    }

    @Override
    public boolean keyDown(int keycode) {
        if (l_set == 1) {
            if (keycode != Input.Keys.ESCAPE && l_nFocus != -1) {
                cfg.bindKey(keynames[l_nFocus], keycode);
            }
            l_set = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (l_set == 1) {
            if (l_nFocus != -1) {
                cfg.bindMouse(keynames[l_nFocus], Keymap.MOUSE_LBUTTON + button);
            }
            l_set = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (l_set == 1) {
            if (l_nFocus != -1) {
                cfg.bindMouse(keynames[l_nFocus], amountY > 0 ? Keymap.MOUSE_WHELLUP : Keymap.MOUSE_WHELLDN);
            }
            l_set = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        if (l_set == 0) {
            switch (opt) {
                case MWUP:
                    ListMouseWheelUp(handler);
                    return false;
                case MWDW:
                    ListMouseWheelDown(handler, len);
                    return false;
                case UP:
                    ListUp(handler, len);
                    return false;
                case DW:
                    ListDown(handler, len);
                    return false;
                case ENTER:
                case LMB:
                    if ((flags & 4) == 0) {
                        return false;
                    }

                    if (l_nFocus != -1 && callback != null) {
                        callback.run(handler, this);
                    }

                    return false;
                case DELETE:
                    if (l_nFocus == -1) {
                        return false;
                    }

                    cfg.unbindAll(keynames[l_nFocus]);
                    return false;
                case PGUP:
                    ListPGUp(handler);
                    return false;
                case PGDW:
                    ListPGDown(handler, len);
                    return false;
                case HOME:
                    ListHome(handler);
                    return false;
                case END:
                    ListEnd(handler, len);
                    return false;
                default:
                    return m_pMenu.mNavigation(opt);
            }
        } else {
            l_pressedId = opt;
//			if((flags & 4) != 0 && callback != null) {
//				callback.run(handler, this);
//			}
            return false;
        }
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
