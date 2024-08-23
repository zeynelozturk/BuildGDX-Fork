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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildNet;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.input.keymap.ControllerAxis;
import ru.m210projects.Build.input.keymap.Keymap;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.ControllerMapping;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.settings.MouseAxis;

import java.util.Arrays;

import static ru.m210projects.Build.input.keymap.Keymap.MOUSE_WHELLDN;
import static ru.m210projects.Build.input.keymap.Keymap.MOUSE_WHELLUP;

public abstract class GameProcessor implements InputListener {

    // key repeating
    static public float keyRepeatInitialTime = 0.4f;
    static public float keyRepeatTime = 0.05f;
    float deltaTime;
    long lastTime;

    protected final BuildGame game;
    // gameKey once pressing handler
    protected final boolean[] justPressedGameKeys;
    // keyCode once pressing handler
    protected final boolean[] justPressedKeys;
    protected final Vector2 mouseDelta;
    protected final Vector2 moveStick;
    protected final Vector2 lookStick;
    private final Vector2 leftStick;
    private final Vector2 rightStick;
    protected int lastKeyPressed;
    // gameKey states
    protected boolean[] gameKeyState;
    // keyCode states (Gdx's states doesn't fit, because of game loop (30 fps))
    protected boolean[] keyState;
    protected boolean keyJustPressed;
    float keyRepeatTimer;

    protected ObjectSet<GameKey> mouseAxisKey = new ObjectSet<>(4);

    public GameProcessor(BuildGame game) {
        this.game = game;
        int lastIndex = Arrays.stream(game.pCfg.getKeymap()).map(GameKey::getNum).max(Integer::compareTo).orElse(0);
        this.gameKeyState = new boolean[lastIndex + 1];
        this.justPressedKeys = new boolean[Input.Keys.MAX_KEYCODE + 1];
        this.keyState = new boolean[Input.Keys.MAX_KEYCODE + 1];
        this.justPressedGameKeys = new boolean[lastIndex + 1];
        this.mouseDelta = new Vector2();
        this.rightStick = new Vector2();
        this.leftStick = new Vector2();
        this.moveStick = new Vector2();
        this.lookStick = new Vector2();
    }

    /**
     * @param input network empty input to fill by game
     */
    public abstract void fillInput(BuildNet.NetInput input);

    /**
     called by syncInput (in faketimehandler) by BuildGame (30 times per sec with network sync)
     */
    public void processInput(BuildNet.NetInput input) {
        input.reset();
        fillInput(input);
        prepareNext();
    }

    public void update() {
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            inputListener.processInput(this);
        }

        long thisTime = System.nanoTime();
        deltaTime = (thisTime - lastTime) / 1000000000.0f;
        lastTime = thisTime;

        if (lastKeyPressed != -1) {
            keyRepeatTimer -= deltaTime;
            if (keyRepeatTimer < 0) {
                keyRepeatTimer = keyRepeatTime;
                keyRepeat(lastKeyPressed);
            }
        }

        GameConfig config = game.pCfg;
        if (!config.getControllerName().isEmpty()) {
            updateStickValue(moveStick.set(leftStick), config.getJoyDeadZone());
            updateStickValue(lookStick.set(rightStick), config.getJoyDeadZone());
            if (config.isJoyInvert()) {
                lookStick.y *= -1;
            }
        }

        if (!mouseAxisKey.isEmpty()) {
            for (GameKey key : mouseAxisKey) {
                gameKeyDown(key);
            }
        }
    }

    public void prepareNext() {
        mouseDelta.setZero();
        if (keyJustPressed) {
            Arrays.fill(justPressedGameKeys, false);
            Arrays.fill(justPressedKeys, false);
            keyJustPressed = false;
        }

        if (!mouseAxisKey.isEmpty()) {
            for (GameKey key : mouseAxisKey) {
                gameKeyUp(key);
            }
            mouseAxisKey.clear();
        }
    }

    public boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode > keyState.length) {
            return false;
        }
        return keyState[keyCode];
    }

    public boolean isKeyJustPressed(int key) {
        if (key == Input.Keys.ANY_KEY) {
            return keyJustPressed;
        }

        if (key < 0 || key > justPressedKeys.length) {
            return false;
        }

        return justPressedKeys[key];
    }

    public boolean isGameKeyJustPressed(GameKey gameKey) {
        int key = gameKey.getNum();
        if (key < 0 || key > justPressedGameKeys.length) {
            return false;
        }

        return justPressedGameKeys[key];
    }

    public boolean isGameKeyPressed(GameKey gameKey) {
        int key = gameKey.getNum();
        if (key < 0 || key >= gameKeyState.length) {
            return false;
        }
        return gameKeyState[key];
    }

    @Override
    public InputListener getInputListener() {
        Screen screen = game.getScreen();
        if (screen instanceof InputListener) {
            return ((InputListener) screen).getInputListener();
        }

        if (Console.out.isShowing()) {
            return Console.out;
        }

        return null;
    }

    public void resetMousePos() {
        if (game.isActive()) {
            Renderer ren = game.getRenderer();
            Gdx.input.setCursorPosition(ren.getWidth() / 2, ren.getHeight() / 2);
        }
    }

    public void resetPollingStates() {
        prepareNext();
        Arrays.fill(gameKeyState, false);
        Arrays.fill(keyState, false);
        leftStick.setZero();
        rightStick.setZero();
        lastKeyPressed = -1;
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        if (gameKey.equals(GameKey.UNKNOWN_KEY)) {
            return false;
        }

        int key = gameKey.getNum();
        gameKeyState[key] = true;
        justPressedGameKeys[key] = true;
        keyJustPressed = true;
        return true;
    }

    public void gameKeyUp(GameKey gameKey) {
        if (gameKey.equals(GameKey.UNKNOWN_KEY)) {
            return;
        }

        gameKeyState[gameKey.getNum()] = false;
    }

    @Override
    public boolean keyRepeat(int keycode) {
        InputListener inputListener = getInputListener();
        return inputListener != null && inputListener.keyRepeat(keycode);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.UNKNOWN) {
            return false;
        }

        if (lastKeyPressed == -1) {
            keyRepeatTimer = keyRepeatInitialTime;
            lastKeyPressed = keycode;
        }

        justPressedKeys[keycode] = true;
        keyJustPressed = true;
        keyState[keycode] = true;

        GameKey gameKey = game.pCfg.convertToGameKey(keycode);
        gameKeyDown(gameKey);

        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            if (inputListener.gameKeyDown(gameKey)) {
                return true;
            }
            return inputListener.keyDown(keycode);
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        if (i == Input.Keys.UNKNOWN) {
            return false;
        }

        lastKeyPressed = -1;
        keyState[i] = false;
        gameKeyUp(game.pCfg.convertToGameKey(i));
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.keyUp(i);
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        gameKeyDown(game.pCfg.convertToGameKey(Keymap.MOUSE_LBUTTON + button));
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.touchDown(screenX, screenY, pointer, button);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        gameKeyUp(game.pCfg.convertToGameKey(Keymap.MOUSE_LBUTTON + button));
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.touchUp(screenX, screenY, pointer, button);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseDelta.add(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());

        if (mouseDelta.x != 0) {
            GameKey axisKey = game.pCfg.getMouseAxis(mouseDelta.x < 0 ? MouseAxis.LEFT : MouseAxis.RIGHT);
            if (axisKey != GameKey.UNKNOWN_KEY) {
                mouseAxisKey.add(axisKey);
            }
        }

        if (mouseDelta.y != 0) {
            GameKey axisKey = game.pCfg.getMouseAxis(mouseDelta.y < 0 ? MouseAxis.UP : MouseAxis.DOWN);
            if (axisKey != GameKey.UNKNOWN_KEY) {
                mouseAxisKey.add(axisKey);
            }
        }

        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.mouseMoved(screenX, screenY);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amount) {
        int keyCode = amount < 0 ? MOUSE_WHELLUP : MOUSE_WHELLDN;
        keyDown(keyCode);
        keyUp(keyCode);

        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.scrolled(amountX, amount);
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mouseMoved(screenX, screenY);
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.touchDragged(screenX, screenY, pointer);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        InputListener inputListener = getInputListener();
        if (inputListener != null) {
            return inputListener.keyTyped(c);
        }
        return false;
    }

    public float ctrlGetMouseMove() {
        return mouseDelta.y * getCommonMouseSensitivity() * game.pCfg.getgMouseMoveSpeed() / 65536f;
    }

    public float ctrlGetMouseLook(boolean invert) {
        float value = mouseDelta.y * getCommonMouseSensitivity() * game.pCfg.getgMouseLookSpeed() / 65536.f;
        return invert ? -value : value;
    }

    public float ctrlGetMouseTurn() {
        return mouseDelta.x * getCommonMouseSensitivity() * game.pCfg.getgMouseTurnSpeed() / 65536f;
    }

    public float ctrlGetMouseStrafe() {
        return mouseDelta.x * getCommonMouseSensitivity() * game.pCfg.getgMouseStrafeSpeed() / 2097152f;
    }

    public Vector2 ctrlGetStick(JoyStick stick) {
        if (stick == JoyStick.LOOKING) {
            return lookStick;
        }

        return moveStick;
    }

    public float getCommonMouseSensitivity() {
        return game.pCfg.getSensitivity() / 65536.0f;
    }

    @Override
    public void connected(Controller controller) {
        Console.out.println(String.format("Controller %s is connected", controller.getName()), OsdColor.BLUE);
        if (game.pCfg.getControllerName().isEmpty()) {
            game.pCfg.setControllerName(controller.getName());
        }
    }

    @Override
    public void disconnected(Controller controller) {
        Console.out.println(String.format("Controller %s is disconnected", controller.getName()), OsdColor.BLUE);
        if (controller.getName().equalsIgnoreCase(game.pCfg.getControllerName())) {
            game.pCfg.setControllerName("");
        }
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (controller.getName().equalsIgnoreCase(game.pCfg.getControllerName())) {
            InputListener inputListener = getInputListener();
            if (inputListener != null && inputListener.buttonDown(controller, buttonCode)) {
                return true;
            }
            ControllerMapping controllerMapping = game.pCfg.getControllerMapping(controller.getName());
            return keyDown(controllerMapping.getKeyCode(buttonCode));
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (controller.getName().equalsIgnoreCase(game.pCfg.getControllerName())) {
            InputListener inputListener = getInputListener();
            if (inputListener != null && inputListener.buttonUp(controller, buttonCode)) {
                return true;
            }
            ControllerMapping controllerMapping = game.pCfg.getControllerMapping(controller.getName());
            return keyUp(controllerMapping.getKeyCode(buttonCode));
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (controller.getName().equalsIgnoreCase(game.pCfg.getControllerName())) {
            ControllerMapping controllerMapping = game.pCfg.getControllerMapping(controller.getName());
            ControllerAxis axis = controllerMapping.getAxis(axisCode);
            switch (axis) {
                case LEFT_STICK_X:
                    leftStick.x = value;
                    break;
                case LEFT_STICK_Y:
                    leftStick.y = value;
                    break;
                case RIGHT_STICK_X:
                    rightStick.x = value;
                    break;
                case RIGHT_STICK_Y:
                    rightStick.y = value;
                    break;
            }

            InputListener inputListener = getInputListener();
            return inputListener != null && inputListener.axisMoved(controller, axisCode, value);
        }
        return false;
    }

    protected void updateStickValue(Vector2 stick, float deadZone) {
        float mag = stick.len();
        if (mag > deadZone) {
            stick.scl(1.0f / mag); // nor()
            mag = Math.min(1.0f, mag) - deadZone;
            stick.scl(mag / (1.0f - deadZone));
        } else {
            stick.setZero();
        }
    }

    public enum JoyStick {
        LOOKING, MOVING
    }
}
