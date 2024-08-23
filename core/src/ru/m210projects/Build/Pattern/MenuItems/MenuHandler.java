package ru.m210projects.Build.Pattern.MenuItems;

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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.input.keymap.Keymap;
import ru.m210projects.Build.settings.GameKeys;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.Console;

import java.util.Arrays;

import static ru.m210projects.Build.Gameutils.BClipLow;
import static ru.m210projects.Build.Pragmas.*;

public abstract class MenuHandler implements InputListener {

    public int mCount = 0;
    public BuildMenu[] mMenuHistory;
    public BuildMenu[] mMenus;
    public boolean gShowMenu;
    protected boolean mUseMouse;
    protected BuildGame game;

    public enum MenuOpt {
        NONE, //0
        ANY, //1
        UP, //2
        DW, //3
        LEFT, //4
        RIGHT, //5
        ENTER, //6
        ESC, //7
        SPACE, //8
        BSPACE, //9 backspace
        DELETE, //10
        LMB, //11
        PGUP, //12
        PGDW, //13
        HOME, //14
        END, //15
        MWUP, //16 mouse wheel up
        MWDW, //17 mouse wheel down
        RMB, //18
        MCHANGE,

        Open, //0x8000
        Close //0x8001
    }

    public MenuHandler(BuildGame game) {
        this.mMenuHistory = new BuildMenu[10];
        this.game = game;
    }

    //item == m_pMenu.m_pItems[m_pMenu.m_nFocus] for get focused shade
    public abstract int getShade(MenuItem item);

    public abstract int getPal(Font font, MenuItem item);

    public abstract void mPostDraw(MenuItem item);

    public abstract void mDrawMouse(int x, int y);

    public abstract void mDrawBackButton();

    public abstract boolean mCheckBackButton(int x, int y);

    public abstract void mSound(MenuItem item, MenuOpt opt);

    public Renderer getRenderer() {
        return game.getRenderer();
    }

    public void mOpen(BuildMenu pMenu, int nItem) {
        if (pMenu == null || mCount == 8) {
            return;
        }

        mMenuHistory[0] = pMenu;
        mMenuHistory[++mCount] = pMenu;

        pMenu.open(this, nItem);
        gShowMenu = true;

        Gdx.input.setCursorCatched(false);
    }

    public boolean isShowing() {
        return gShowMenu;
    }

    public void mClose() {
        Arrays.fill(mMenuHistory, null);
        mCount = 0;

        gShowMenu = false;

        if (!game.isActive()) {
            return;
        }

        Renderer ren = game.getRenderer();

        Gdx.input.setCursorCatched(true);
        Gdx.input.setCursorPosition(ren.getWidth() / 2, ren.getHeight() / 2);
        game.getProcessor().resetPollingStates();
    }

    public void mMenuBack() {
        if (mCount > 0) {
            if (mMenuHistory[0] != null) {
                mMenuHistory[0].mLoadRes(this, MenuOpt.Close);
            }
            mCount = BClipLow(mCount - 1, 0);
            if (mCount > 0) {
                mMenuHistory[0] = mMenuHistory[mCount];
            } else {
                mClose();
            }
        }
    }

//    public void mMenuBack(MenuOpt opt) {
//        if (mCount > 0) {
//            if (mMenuHistory[0] != null) {
//                mMenuHistory[0].mLoadRes(this, MenuOpt.Close);
//            }
//            mCount = BClipLow(mCount - 1, 0);
//            if (mCount > 0) {
//                mMenuHistory[0] = mMenuHistory[mCount];
//                mMenuHistory[0].mLoadRes(this, opt);
//            } else {
//                mClose();
//            }
//        }
//    }

    @Deprecated
    protected boolean onEvent(MenuOpt opt) {
        if (mMenuHistory[0] == null) {
            return false;
        }

        if (mMenuHistory[0].mLoadRes(this, opt)) {
            mMenuBack();
            return true;
        }
        return false;
    }

    public BuildMenu getCurrentMenu() {
        return mMenuHistory[0];
    }

    public BuildMenu getLastMenu() {
        if (mCount > 0) {
            return mMenuHistory[mCount - 1];
        }
        return getCurrentMenu();
    }

    public boolean isOpened(BuildMenu pMenu) {
        return pMenu != null && getCurrentMenu() == pMenu;
    }

    public void mDrawMenu() {
        if (mMenuHistory[0] != null) {
            mMenuHistory[0].mDraw(this);
        }

        mDrawBackButton();
        if (mUseMouse) {
            mDrawMouse(Gdx.input.getX(), Gdx.input.getY());
        }
    }

    private void checkFocus(BuildMenu pMenu, int x, int y) {
        Renderer ren = game.getRenderer();

        int oxdim = ren.getWidth();
        int xdim = (4 * ren.getHeight()) / 3;
        int normxofs = x - oxdim / 2;
        int touchX = scale(normxofs, 320, xdim) + 320 / 2;
        int touchY = mulscale(y, divscale(200, ren.getHeight(), 16), 16);

        for (short i = 0; i < pMenu.m_pItems.length; i++) {
            if (pMenu.mCheckMouseFlag(i) && pMenu.mCheckItemsFlags(i) && pMenu.m_pItems[i].mouseAction(touchX, touchY)) {
                if (pMenu.m_nFocus != i) {
                    onEvent(MenuOpt.MCHANGE);
                }
                pMenu.m_nFocus = i;
                return;
            }
        }
    }

    protected ScrollableMenuItem getSliderItem() {
        BuildMenu pMenu = mMenuHistory[0];
        if (pMenu.m_nFocus != -1 && pMenu.mCheckMouseFlag(pMenu.m_nFocus) && pMenu.m_pItems[pMenu.m_nFocus] instanceof ScrollableMenuItem) {
            return (ScrollableMenuItem) pMenu.m_pItems[pMenu.m_nFocus];
        }
        return null;
    }

    protected InputListener getFocusedGameKeyListener() {
        MenuItem item = getFocusedItem();
        if (item instanceof InputListener) {
            return (InputListener) item;
        }
        return null;
    }

    protected MenuItem getFocusedItem() {
        BuildMenu pMenu = mMenuHistory[0];
        if (pMenu.m_nFocus != -1) {
            return pMenu.m_pItems[pMenu.m_nFocus];
        }
        return null;
    }

    // Menu controller:

    @Override
    public boolean scrolled (float amountX, float amount) {
        if (!game.pCfg.isMenuMouse()) {
            return false;
        }

        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.scrolled(amountX, amount)) {
            return true;
        }

        if (amount < 0) {
            onEvent(MenuOpt.MWUP);
            return true;
        } else if (amount > 0) {
            onEvent(MenuOpt.MWDW);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int mx, int my) {
        if (!game.pCfg.isMenuMouse()) {
            return false;
        }
        mUseMouse = true;

        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.mouseMoved(mx, my)) {
            return true;
        }

        checkFocus(mMenuHistory[0], mx, my);
        return true;
    }

    @Override
    public boolean touchDown(int mx, int my, int pointer, int button) {
        if (!game.pCfg.isMenuMouse()) {
            return false;
        }

        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.touchDown(mx, my, pointer, button)) {
            return true;
        }

        switch (button) {
            case Input.Buttons.LEFT:
                if (mCount > 1 && mCheckBackButton(mx, my)) {
                    mMenuBack();
                    return true;
                }

                ScrollableMenuItem slider = getSliderItem();
                if (slider != null) {
                    Renderer ren = game.getRenderer();

                    int oxdim = ren.getWidth();
                    int xdim = (4 * ren.getHeight()) / 3;
                    int normxofs = mx - oxdim / 2;
                    int touchX = scale(normxofs, 320, xdim) + 320 / 2;
                    int touchY = mulscale(my, divscale(200, ren.getHeight(), 16), 16);
                    if (slider.onLockSlider(this, touchX, touchY)) {
                        return true;
                    }
                }

                if (onEvent(MenuOpt.LMB)) {
                    checkFocus(mMenuHistory[0], mx, my);
                    return true;
                }

                return false;
            case Input.Buttons.RIGHT:
                return onEvent(MenuOpt.RMB);
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!game.pCfg.isMenuMouse()) {
            return false;
        }

        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.touchUp(screenX, screenY, pointer, button)) {
            return true;
        }


        ScrollableMenuItem slider = getSliderItem();
        if (slider != null) {
            slider.onUnlockSlider();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int mx, int my, int pointer) {
        if (!game.pCfg.isMenuMouse()) {
            return false;
        }

        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.touchDragged(mx, my, pointer)) {
            return true;
        }

        ScrollableMenuItem slider = getSliderItem();
        if (slider != null) {
            Renderer ren = game.getRenderer();

            int oxdim = ren.getWidth();
            int xdim = (4 * ren.getHeight()) / 3;
            int normxofs = mx - oxdim / 2;
            int touchX = scale(normxofs, 320, xdim) + 320 / 2;
            int touchY = mulscale(my, divscale(200, ren.getHeight(), 16), 16);

            slider.onMoveSlider(this, touchX, touchY);
            return true;
        }
        return false;
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        InputListener focusedListener = getFocusedGameKeyListener();
        if (focusedListener instanceof MenuKeyboardList && focusedListener.gameKeyDown(gameKey)) {
            return false;
        }

        if (GameKeys.Show_Console.equals(gameKey)) {
            Console.out.onToggle();
            return true;
        }

        if (focusedListener != null && focusedListener.gameKeyDown(gameKey)) {
            return true;
        }

        // Gamepad menu_toggle special handling
//        if (MenuKeys.Menu_Cancel.equals(gameKey)) {
//            mClose();
//            return true;
//        }

        if (GameKeys.Menu_Toggle.equals(gameKey)) {
            return onEvent(MenuOpt.ESC);
        }

        MenuOpt opt = MenuOpt.ANY;
//        if (MenuKeys.Menu_Up.equals(gameKey)) {
//            opt = MenuOpt.UP;
//        } else if (MenuKeys.Menu_Down.equals(gameKey)) {
//            opt = MenuOpt.DW;
//        } else if (MenuKeys.Menu_Left.equals(gameKey)) {
//            opt = MenuOpt.LEFT;
//        } else if (MenuKeys.Menu_Right.equals(gameKey)) {
//            opt = MenuOpt.RIGHT;
//        } else if (MenuKeys.Menu_Enter.equals(gameKey)) {
//            opt = MenuOpt.ENTER;
//        }

        if (opt != MenuOpt.ANY) {
            mUseMouse = false;
            onEvent(opt);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyRepeat(int keycode) {
        return keyDown(keycode);
    }

    @Override
    public boolean keyDown(int keycode) {
        InputListener focusedListener = getFocusedGameKeyListener();
        if (focusedListener != null && focusedListener.keyDown(keycode)) {
            return true;
        }

        MenuOpt opt = MenuOpt.ANY;
        switch (keycode) {
            case Keys.UP:
            case Keymap.BUTTON_UP:
                opt = MenuOpt.UP;
                break;
            case Keys.DOWN:
            case Keymap.BUTTON_DOWN:
                opt = MenuOpt.DW;
                break;
            case Keys.LEFT:
            case Keymap.BUTTON_LEFT:
                opt = MenuOpt.LEFT;
                break;
            case Keys.RIGHT:
            case Keymap.BUTTON_RIGHT:
                opt = MenuOpt.RIGHT;
                break;
            case Keys.ENTER:
            case Keys.BUTTON_A:
//                if (gameKeyDown(MenuKeys.Menu_Enter)) {
//                    return true;
//                }
                opt = MenuOpt.ENTER;
                game.getProcessor().resetPollingStates(); // avoid repeating
                break;
            case Keys.SPACE:
                opt = MenuOpt.SPACE;
                break;
            case Keys.BACKSPACE:
                opt = MenuOpt.BSPACE;
                break;
            case Keys.FORWARD_DEL:
                opt = MenuOpt.DELETE;
                break;
            case Keys.PAGE_UP:
                opt = MenuOpt.PGUP;
                break;
            case Keys.PAGE_DOWN:
                opt = MenuOpt.PGDW;
                break;
            case Keys.HOME:
                opt = MenuOpt.HOME;
                break;
            case Keys.END:
                opt = MenuOpt.END;
                break;
        }

        if (opt != MenuOpt.ANY) {
            mUseMouse = false;
            onEvent(opt);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        InputListener focusedItem = getFocusedGameKeyListener();
        return focusedItem != null && focusedItem.keyUp(keycode);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        InputListener focusedListener = getFocusedGameKeyListener();
        return focusedListener != null && focusedListener.buttonDown(controller, buttonCode);
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        InputListener focusedListener = getFocusedGameKeyListener();
        return focusedListener != null && focusedListener.buttonUp(controller, buttonCode);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        InputListener focusedListener = getFocusedGameKeyListener();
        return focusedListener != null && focusedListener.axisMoved(controller, axisCode, value);
    }

    @Override
    public void processInput(GameProcessor processor) {
        InputListener focusedListener = getFocusedGameKeyListener();
        if (focusedListener != null) {
            focusedListener.processInput(processor);
        }
    }

    //    @Override
//    public boolean keyRepeat(int i) {
//        GameKeyListener focusedItem = getFocusedGameKeyListener();
//        if (focusedItem != null && focusedItem.keyRepeat(i)) {
//            return true;
//        }
//
//        return keyDown(i);
//    }

    @Override
    public boolean keyTyped(char character) {
        InputListener focusedItem = getFocusedGameKeyListener();
        if (focusedItem != null && focusedItem.keyTyped(character)) {
            return true;
        }

        return false;
    }
}
