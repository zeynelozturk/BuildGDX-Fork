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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.RawMouseLwjgl3Input;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.JControllerManager;
import ru.m210projects.Build.Architecture.common.ResolutionUtils;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.settings.ApplicationContext;

import java.util.*;

public class LwjglApplicationContext extends ApplicationContext {

    private final Application application;

    public LwjglApplicationContext(Application application) {
        this.application = application;
    }

    @Override
    public void setControllerListener(GameProcessor processor) {
        Controllers.preferredManager = JControllerManager.class.getName();
        Controllers.addListener(processor);
    }

    @Override
    public void setScreenMode(int width, int height, boolean fullscreen) {
        if (fullscreen) {
            String configResolution = ResolutionUtils.getDisplayModeAsString(width, height);
            Map<String, List<Graphics.DisplayMode>> resolutions = getResolutions();
            Optional<Graphics.DisplayMode> displayMode =
                    resolutions
                            .getOrDefault(configResolution, new ArrayList<>())
                            .stream()
                            .max(Comparator.comparingInt(a -> a.refreshRate));
            this.fullscreen = displayMode.isPresent();
            if (this.fullscreen) {
                Graphics.DisplayMode mode = displayMode.get();
                if (Gdx.graphics.setFullscreenMode(mode)) {
                    super.setScreenMode(mode.width, mode.height, true);
                    return;
                }
            }
        }
        Gdx.graphics.setWindowedMode(width, height);
        super.setScreenMode(width, height, false);
    }

    @Override
    public Map<String, List<Graphics.DisplayMode>> getResolutions() {
        return ResolutionUtils.getDisplayModes(Gdx.graphics.getDisplayModes());
    }

    @Override
    public boolean onRawInputChanged(boolean rawInput) {
        Input input = application.getInput();
        if (input instanceof RawMouseLwjgl3Input) {
            return ((RawMouseLwjgl3Input) input).setRawInput(rawInput);
        }
        return false;
    }

    @Override
    public boolean isRawInputSupported() {
        Input input = application.getInput();
        if (input instanceof RawMouseLwjgl3Input) {
            return ((RawMouseLwjgl3Input) input).isRawInputSupported();
        }
        return false;
    }
}
