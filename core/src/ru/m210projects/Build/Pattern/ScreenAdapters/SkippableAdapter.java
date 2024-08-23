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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.controllers.Controller;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.settings.GameKeys;

public abstract class SkippableAdapter extends ScreenAdapter implements InputListener {

    protected BuildGame game;
    protected Engine engine;
    protected boolean escSkip;
    protected Runnable skipCallback;

    public SkippableAdapter(BuildGame game) {
        this.game = game;
        this.engine = game.pEngine;
    }

    public SkippableAdapter setSkipping(Runnable skipCallback) {
        this.skipCallback = skipCallback;
        return this;
    }

    public SkippableAdapter escSkipping(boolean escSkip) {
        this.escSkip = escSkip;
        return this;
    }

    public abstract void draw(float delta);

    public void skip() {
        if (skipCallback != null) {
            Gdx.app.postRunnable(skipCallback);
            skipCallback = null;
        }
    }

    @Override
    public final void render(float delta) {
        Renderer renderer = game.getRenderer();
        renderer.clearview(0);

        draw(delta);

        engine.nextpage(delta);
    }

    private boolean onSkipPressed(GameKey gameKey) {
        if (!escSkip || GameKeys.Menu_Toggle.equals(gameKey)) {
            skip();
            return true;
        }
        return false;
    }

    @Override
    public InputListener getInputListener() {
        return this;
    }

    @Override
    public void processInput(GameProcessor processor) {
        processor.prepareNext();
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        return onSkipPressed(gameKey);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!escSkip || keycode == Input.Keys.ESCAPE) {
            skip();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!escSkip) {
            skip();
            return true;
        }
        return false;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (!escSkip) {
            skip();
            return true;
        }
        return false;
    }
}
