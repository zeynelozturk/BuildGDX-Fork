// This file is part of BuildGDX.
// Copyright (C) 2023-2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// BuildGDX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// BuildGDX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package com.badlogic.gdx.backends;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

public class WindowListener implements Lwjgl3WindowListener {

    private final BuildGame game;

    public WindowListener(BuildGame game) {
        this.game = game;
    }

    @Override
    public void created(Lwjgl3Window window) {
        game.setActive(true);
    }

    @Override
    public void iconified(boolean isIconified) {
        if (isIconified) {
            focusLost();
        } else {
            focusGained();
        }
    }

    @Override
    public void maximized(boolean isMaximized) {
    }

    @Override
    public void focusLost() {
        game.setActive(false);
        game.pause();
    }

    @Override
    public void focusGained() {
        game.setActive(true);
        game.resume();
    }

    @Override
    public boolean closeRequested() {
        return false;
    }

    @Override
    public void filesDropped(String[] files) {
        game.onFilesDropped(files);
    }

    @Override
    public void refreshRequested() {

    }
}
