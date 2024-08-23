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

package ru.m210projects.Build.Pattern.ScreenAdapters;

import com.badlogic.gdx.ScreenAdapter;
import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.BuildMenu;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.settings.GameKeys;

public abstract class MenuAdapter extends ScreenAdapter implements InputListener {

    protected BuildGame game;
    protected MenuHandler menu;
    protected Engine engine;
    protected GameConfig cfg;
    protected BuildMenu mainMenu;

    public MenuAdapter(final BuildGame game, @NotNull BuildMenu mainMenu) {
        this.game = game;
        this.menu = game.pMenu;
        this.engine = game.pEngine;
        this.cfg = game.pCfg;
        this.mainMenu = mainMenu;
    }

    public abstract void draw(float delta);

    public void process(float delta) {
    }

    @Override
    public void render(float delta) {
        game.getRenderer().clearview(0);

        draw(delta);

        if (menu.gShowMenu) {
            menu.mDrawMenu();
        }

        process(delta);

        engine.nextpage(delta);
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        if (GameKeys.Show_Console.equals(gameKey)) {
            Console.out.onToggle();
            return true;
        }

        if (GameKeys.Menu_Toggle.equals(gameKey) && !menu.isShowing()) {
            game.pMenu.mOpen(mainMenu, -1);
            return true;
        }
        return false;
    }

    @Override
    public InputListener getInputListener() {
        if (Console.out.isShowing()) {
            return Console.out;
        }

        if (game.pMenu.isShowing()) {
            return game.pMenu;
        }

        return this;
    }
}
