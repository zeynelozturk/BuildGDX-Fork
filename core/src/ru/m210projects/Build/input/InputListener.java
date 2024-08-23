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

package ru.m210projects.Build.input;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import ru.m210projects.Build.osd.Console;

public interface InputListener extends InputProcessor, ControllerListener {

    /**
     * Calls 30 times per seconds before fillInput (in faketimehandler)
     * @param processor game processor
     */
    default void processInput(GameProcessor processor) {
    }

    default boolean keyRepeat(int keycode) {
        return false;
    }

    /**
     * Calls when GameKey pressed
     */
    default boolean gameKeyDown(GameKey gameKey) {
        return false;
    }

    /**
     * In case if you want to override listener priority (for example, console, menu, screen)
     * @return
     */
    default InputListener getInputListener() {
        if (Console.out.isShowing()) {
            return Console.out;
        }

        return this;
    }

    @Override
    default boolean keyDown(int keycode) {
        return false;
    }

    @Override
    default boolean keyUp(int keycode) {
        return false;
    }

    @Override
    default boolean keyTyped(char character) {
        return false;
    }

    @Override
    default boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    default boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    default boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    default boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    default boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    default boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    default void connected(Controller controller) {
    }

    @Override
    default void disconnected(Controller controller) {
    }

    @Override
    default boolean buttonDown(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    default boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    default boolean axisMoved(Controller controller, int axisCode, float value) {
        return false;
    }
}
