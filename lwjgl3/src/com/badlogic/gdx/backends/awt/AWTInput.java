//This file is part of BuildGDX.
//Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package com.badlogic.gdx.backends.awt;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.RawInputEventQueue;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.input.NativeInputConfiguration;
import com.badlogic.gdx.utils.IntSet;
import org.lwjgl.system.Platform;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class AWTInput implements Input {

    public static final int MAX_KEYCODE = 255;
    protected final boolean[] pressedKeys;
    protected final boolean[] justPressedKeys;
    final RawInputEventQueue eventQueue;
    final boolean[] justPressedButtons = new boolean[5];
    private final IntSet keysToCatch = new IntSet();
    private final Mouse mouse;
    protected int pressedKeyCount;
    protected boolean keyJustPressed;
    AWTWindow window;
    int mouseX, mouseY;
    int logicalMouseX, logicalMouseY;
    int virtualX, virtualY;
    int mousePressed;
    int deltaX, deltaY;
    boolean justTouched;
    boolean caught = false;
    boolean lastCaught = false;
    char lastCharacter;
    int buttonCount;
    IntSet pressedButtons = new IntSet();
    KeyListener keyCallback = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
            lastCharacter = e.getKeyChar();
            AWTInput.this.window.getGraphics().requestRendering();
            eventQueue.keyTyped(lastCharacter, System.nanoTime());
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = translateKeyCode(e);
            if (pressedKeys[key]) {
                return;
            }

            eventQueue.keyDown(key, System.nanoTime());
            pressedKeyCount++;
            keyJustPressed = true;
            pressedKeys[key] = true;
            justPressedKeys[key] = true;
            AWTInput.this.window.getGraphics().requestRendering();
            lastCharacter = 0;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = translateKeyCode(e);
            pressedKeyCount--;
            pressedKeys[key] = false;
            AWTInput.this.window.getGraphics().requestRendering();
            eventQueue.keyUp(key, System.nanoTime());
        }
    };
    MouseMotionListener cursorPosCallback = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
            updateCursorPosition(e.getX(), e.getY());
            eventQueue.touchDragged(deltaX, deltaY, 0, System.nanoTime());
            setCursorToCenter(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursorPosition(e.getX(), e.getY());
            eventQueue.mouseMoved(deltaX, deltaY, System.nanoTime());
            setCursorToCenter(e);
        }

        private void setCursorToCenter(MouseEvent e) {
            // #GDX 28.12.2024 Return mouse cursor to screen center
            if (caught && window.focused /*&& window.activated*/) {
                Component windowHandle = window.getWindowHandle();
                if (windowHandle.isShowing()) {
                    int centerX = window.getGraphics().getWidth() / 2;
                    int centerY = window.getGraphics().getHeight() / 2;
                    // to avoid recursion
                    if (e.getX() != centerX || e.getY() != centerY) {
                        setCursorPosition(centerX, centerY);
                    }
                }
            }
        }
    };

    MouseAdapter mouseButtonCallback = new MouseAdapter() {
        private int toGdxButton(int button) {
            if (button == 1) return Buttons.LEFT;
            if (button == 2) return Buttons.MIDDLE;
            if (button == 3) return Buttons.RIGHT;
            if (button == 4) return Buttons.BACK;
            if (button == 5) return Buttons.FORWARD;
            return -1;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            AWTInput.this.window.getGraphics().requestRendering();
            eventQueue.scrolled(0, e.getWheelRotation(), System.nanoTime());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int button = e.getButton();
            int gdxButton = toGdxButton(button);
            if (button != -1 && gdxButton == -1) {
                return;
            }

            long time = System.nanoTime();
            mousePressed++;
            justTouched = true;
            justPressedButtons[gdxButton] = true;
            pressedButtons.add(gdxButton);
            AWTInput.this.window.getGraphics().requestRendering();
            eventQueue.touchDown(getX(), getY(), 0, gdxButton, time);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int button = e.getButton();
            int gdxButton = toGdxButton(button);
            if (button != -1 && gdxButton == -1) {
                return;
            }
            long time = System.nanoTime();
            mousePressed = Math.max(0, mousePressed - 1);
            pressedButtons.remove(gdxButton);
            AWTInput.this.window.getGraphics().requestRendering();
            eventQueue.touchUp(getX(), getY(), 0, gdxButton, time);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            updateCursorPosition(e.getX(), e.getY());
            window.activated = true;
            if (window.focused) {
                mouse.setCursorVisible(false);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            window.activated = false;
        }
    };
    private InputProcessor inputProcessor;

    public AWTInput(AWTWindow window) {
        this.window = window;
        this.pressedKeys = new boolean[MAX_KEYCODE + 1];
        this.justPressedKeys = new boolean[MAX_KEYCODE + 1];
        this.eventQueue = new RawInputEventQueue();
        this.buttonCount = MouseInfo.getNumberOfButtons();
        this.mouse = createMouse(window);

        resetPollingStates();
        window.getWindowHandle().setFocusTraversalKeysEnabled(false);
        window.getWindowHandle().addKeyListener(keyCallback);
        window.getGraphics().raster.addMouseListener(mouseButtonCallback);
        window.getGraphics().raster.addMouseMotionListener(cursorPosCallback);
        window.getGraphics().raster.addMouseWheelListener(mouseButtonCallback);
    }

    private Mouse createMouse(AWTWindow window) {
        try {
            if (Platform.get() == Platform.WINDOWS) {
                return new WinMouse(window);
            }
        } catch (InitializationException e) {
            Console.out.println("Error to initialize windows mouse", OsdColor.RED);
        }
        return new AWTMouse(window);
    }

    private void updateCursorPosition(int x, int y) {
        if (checkCaught(x, y)) {
            x = mouseX;
            y = mouseY;
        }

        deltaX = (x - logicalMouseX);
        deltaY = (y - logicalMouseY);

        mouseX = logicalMouseX = x;
        mouseY = logicalMouseY = y;

        if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
            float xScale = window.getGraphics().getBackBufferWidth() / (float) window.getGraphics().getLogicalWidth();
            float yScale = window.getGraphics().getBackBufferHeight() / (float) window.getGraphics().getLogicalHeight();
            deltaX = (int) (deltaX * xScale);
            deltaY = (int) (deltaY * yScale);
            mouseX = (int) (mouseX * xScale);
            mouseY = (int) (mouseY * yScale);
        }

        if (caught) {
            virtualX += deltaX;
            virtualY += deltaY;
        }

        window.getGraphics().requestRendering();
    }

    public void pollEvents() {
    }

    private boolean checkCaught(int x, int y) {
        if (caught && window.focused /*&& window.activated*/) {
            Rectangle frame = window.getGraphics().raster.getBounds();
            if (!frame.contains(x, y)) {
                setCursorCatched(true);
                return true;
            }
        }
        return false;
    }

    public void resetPollingStates() {
        justTouched = false;
        keyJustPressed = false;
        Arrays.fill(justPressedKeys, false);
        Arrays.fill(justPressedButtons, false);
        eventQueue.drain(null);
    }

    public void update() {
        eventQueue.drain(inputProcessor);
    }

    public void prepareNext() {
        if (justTouched) {
            justTouched = false;
            Arrays.fill(justPressedButtons, false);
        }

        if (keyJustPressed) {
            keyJustPressed = false;
            Arrays.fill(justPressedKeys, false);
        }
        deltaX = 0;
        deltaY = 0;
    }

    @Override
    public int getMaxPointers() {
        return 1;
    }

    @Override
    public int getX() {
        if (caught) {
            return virtualX;
        }
        return mouseX;
    }

    @Override
    public int getX(int pointer) {
        return pointer == 0 ? getX() : 0;
    }

    @Override
    public int getDeltaX() {
        return deltaX;
    }

    @Override
    public int getDeltaX(int pointer) {
        return pointer == 0 ? getDeltaX() : 0;
    }

    @Override
    public int getY() {
        if (caught) {
            return virtualY;
        }
        return mouseY;
    }

    @Override
    public int getY(int pointer) {
        return pointer == 0 ? getY() : 0;
    }

    @Override
    public int getDeltaY() {
        return deltaY;
    }

    @Override
    public int getDeltaY(int pointer) {
        return pointer == 0 ? getDeltaY() : 0;
    }

    @Override
    public boolean isTouched() {
        return mousePressed > 0;
    }

    @Override
    public boolean justTouched() {
        return justTouched;
    }

    @Override
    public boolean isTouched(int pointer) {
        return pointer == 0 && isTouched();
    }

    @Override
    public float getPressure() {
        return getPressure(0);
    }

    @Override
    public float getPressure(int pointer) {
        return isTouched(pointer) ? 1 : 0;
    }

    @Override
    public boolean isButtonPressed(int button) {
        return pressedButtons.contains(button);
    }

    @Override
    public boolean isButtonJustPressed(int button) {
        if (button < 0 || button >= justPressedButtons.length) {
            return false;
        }
        return justPressedButtons[button];
    }

    @Override
    public boolean isKeyPressed(int key) {
        if (key == Input.Keys.ANY_KEY) {
            return pressedKeyCount > 0;
        }
        if (key < 0 || key > MAX_KEYCODE) {
            return false;
        }
        return pressedKeys[key];
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        if (key == Input.Keys.ANY_KEY) {
            return keyJustPressed;
        }
        if (key < 0 || key > MAX_KEYCODE) {
            return false;
        }
        return justPressedKeys[key];
    }

    @Override
    public void setCatchKey(int keycode, boolean catchKey) {
        if (!catchKey) {
            keysToCatch.remove(keycode);
        } else {
            keysToCatch.add(keycode);
        }
    }

    @Override
    public boolean isCatchKey(int keycode) {
        return keysToCatch.contains(keycode);
    }

    @Override
    public void getTextInput(TextInputListener listener, String title, String text, String hint) {
        listener.canceled();
    }

    @Override
    public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) {

    }

    @Override
    public long getCurrentEventTime() {
        return eventQueue.getCurrentEventTime();
    }

    @Override
    public InputProcessor getInputProcessor() {
        return inputProcessor;
    }

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.inputProcessor = processor;
    }

    @Override
    public boolean isCursorCatched() {
        return caught;
    }

    @Override
    public void setCursorCatched(boolean caught) {
        this.lastCaught = this.caught;
        this.caught = caught;

        if (caught) {
            mouse.setCursorVisible(false);
            if (!lastCaught) {
                virtualX = mouseX;
                virtualY = mouseY;
            }
            setCursorPosition(window.getGraphics().getWidth() / 2, window.getGraphics().getHeight() / 2);
            mouse.clipCursor(window.getGraphics().raster);
        } else {
            mouse.setCursorVisible(!window.activated || !window.focused);
            mouse.clipCursor(null);
        }
    }

    @Override
    public void setCursorPosition(int x, int y) {
        Component windowHandle = window.getGraphics().raster;
        if (!windowHandle.isShowing()) {
            return;
        }

        Point windowLocation = windowHandle.getLocationOnScreen();
        if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
            float xScale = window.getGraphics().getLogicalWidth() / (float) window.getGraphics().getBackBufferWidth();
            float yScale = window.getGraphics().getLogicalHeight() / (float) window.getGraphics().getBackBufferHeight();
            x = (int) (x * xScale);
            y = (int) (y * yScale);
        }

        logicalMouseX = x;
        logicalMouseY = y;
        updateCursorPosition(x, y);

        x += windowLocation.x;
        y += windowLocation.y;

        mouse.setCursorPosition(x, y);
    }

    protected int translateKeyCode(KeyEvent ke) {
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_MULTIPLY:
                return Keys.STAR;
            case KeyEvent.VK_DECIMAL:
                return Keys.NUMPAD_DOT;
            case KeyEvent.VK_PAUSE:
                return Keys.PAUSE;
            case KeyEvent.VK_CAPS_LOCK:
                return Keys.CAPS_LOCK;
            case KeyEvent.VK_SCROLL_LOCK:
                return Keys.SCROLL_LOCK;
            case KeyEvent.VK_BACK_SPACE:
                return Keys.BACKSPACE;
            case KeyEvent.VK_LEFT:
                return Keys.LEFT;
            case KeyEvent.VK_RIGHT:
                return Keys.RIGHT;
            case KeyEvent.VK_UP:
                return Keys.UP;
            case KeyEvent.VK_DOWN:
                return Keys.DOWN;
            case KeyEvent.VK_QUOTE:
                return Keys.APOSTROPHE;
            case KeyEvent.VK_OPEN_BRACKET:
                return Keys.LEFT_BRACKET;
            case KeyEvent.VK_CLOSE_BRACKET:
                return Keys.RIGHT_BRACKET;
            case KeyEvent.VK_BACK_QUOTE:
                return Keys.GRAVE;
            case KeyEvent.VK_NUM_LOCK:
                return Keys.NUM;
            case KeyEvent.VK_EQUALS:
                return Keys.EQUALS;
            case KeyEvent.VK_0:
                return Keys.NUM_0;
            case KeyEvent.VK_1:
                return Keys.NUM_1;
            case KeyEvent.VK_2:
                return Keys.NUM_2;
            case KeyEvent.VK_3:
                return Keys.NUM_3;
            case KeyEvent.VK_4:
                return Keys.NUM_4;
            case KeyEvent.VK_5:
                return Keys.NUM_5;
            case KeyEvent.VK_6:
                return Keys.NUM_6;
            case KeyEvent.VK_7:
                return Keys.NUM_7;
            case KeyEvent.VK_8:
                return Keys.NUM_8;
            case KeyEvent.VK_9:
                return Keys.NUM_9;
            case KeyEvent.VK_A:
                return Keys.A;
            case KeyEvent.VK_B:
                return Keys.B;
            case KeyEvent.VK_C:
                return Keys.C;
            case KeyEvent.VK_D:
                return Keys.D;
            case KeyEvent.VK_E:
                return Keys.E;
            case KeyEvent.VK_F:
                return Keys.F;
            case KeyEvent.VK_G:
                return Keys.G;
            case KeyEvent.VK_H:
                return Keys.H;
            case KeyEvent.VK_I:
                return Keys.I;
            case KeyEvent.VK_J:
                return Keys.J;
            case KeyEvent.VK_K:
                return Keys.K;
            case KeyEvent.VK_L:
                return Keys.L;
            case KeyEvent.VK_M:
                return Keys.M;
            case KeyEvent.VK_N:
                return Keys.N;
            case KeyEvent.VK_O:
                return Keys.O;
            case KeyEvent.VK_P:
                return Keys.P;
            case KeyEvent.VK_Q:
                return Keys.Q;
            case KeyEvent.VK_R:
                return Keys.R;
            case KeyEvent.VK_S:
                return Keys.S;
            case KeyEvent.VK_T:
                return Keys.T;
            case KeyEvent.VK_U:
                return Keys.U;
            case KeyEvent.VK_V:
                return Keys.V;
            case KeyEvent.VK_W:
                return Keys.W;
            case KeyEvent.VK_X:
                return Keys.X;
            case KeyEvent.VK_Y:
                return Keys.Y;
            case KeyEvent.VK_Z:
                return Keys.Z;
            case KeyEvent.VK_ALT:
                ke.consume();
                if (ke.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT)
                    return Keys.ALT_LEFT;
                return Keys.ALT_RIGHT;
            case KeyEvent.VK_BACK_SLASH:
                return Keys.BACKSLASH;
            case KeyEvent.VK_COMMA:
                return Keys.COMMA;
            case KeyEvent.VK_DELETE:
                return Keys.FORWARD_DEL;
            case KeyEvent.VK_ENTER:
                return Keys.ENTER;
            case KeyEvent.VK_HOME:
                return Keys.HOME;
            case KeyEvent.VK_END:
                return Keys.END;
            case KeyEvent.VK_PAGE_DOWN:
                return Keys.PAGE_DOWN;
            case KeyEvent.VK_PAGE_UP:
                return Keys.PAGE_UP;
            case KeyEvent.VK_INSERT:
                return Keys.INSERT;
            case KeyEvent.VK_SUBTRACT:
            case KeyEvent.VK_MINUS:
                return Keys.MINUS;
            case KeyEvent.VK_PERIOD:
                return Keys.PERIOD;
            case KeyEvent.VK_ADD:
            case KeyEvent.VK_PLUS:
                return Keys.PLUS;
            case KeyEvent.VK_SEMICOLON:
                return Keys.SEMICOLON;
            case KeyEvent.VK_SHIFT:
                if (ke.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT)
                    return Keys.SHIFT_LEFT;
                return Keys.SHIFT_RIGHT;
            case KeyEvent.VK_SLASH:
            case KeyEvent.VK_DIVIDE:
                return Keys.SLASH;
            case KeyEvent.VK_SPACE:
                return Keys.SPACE;
            case KeyEvent.VK_TAB:
                return Keys.TAB;
            case KeyEvent.VK_CONTROL:
                if (ke.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT)
                    return Keys.CONTROL_LEFT;
                return Keys.CONTROL_RIGHT;
            case KeyEvent.VK_ESCAPE:
                return Keys.ESCAPE;
            case KeyEvent.VK_F1:
                return Keys.F1;
            case KeyEvent.VK_F2:
                return Keys.F2;
            case KeyEvent.VK_F3:
                return Keys.F3;
            case KeyEvent.VK_F4:
                return Keys.F4;
            case KeyEvent.VK_F5:
                return Keys.F5;
            case KeyEvent.VK_F6:
                return Keys.F6;
            case KeyEvent.VK_F7:
                return Keys.F7;
            case KeyEvent.VK_F8:
                return Keys.F8;
            case KeyEvent.VK_F9:
                return Keys.F9;
            case KeyEvent.VK_F10:
                return Keys.F10;
            case KeyEvent.VK_F11:
                return Keys.F11;
            case KeyEvent.VK_F12:
                return Keys.F12;
            case KeyEvent.VK_COLON:
                return Keys.COLON;
            case KeyEvent.VK_NUMPAD0:
                return Keys.NUMPAD_0;
            case KeyEvent.VK_NUMPAD1:
                return Keys.NUMPAD_1;
            case KeyEvent.VK_NUMPAD2:
                return Keys.NUMPAD_2;
            case KeyEvent.VK_NUMPAD3:
                return Keys.NUMPAD_3;
            case KeyEvent.VK_NUMPAD4:
                return Keys.NUMPAD_4;
            case KeyEvent.VK_NUMPAD5:
                return Keys.NUMPAD_5;
            case KeyEvent.VK_NUMPAD6:
                return Keys.NUMPAD_6;
            case KeyEvent.VK_NUMPAD7:
                return Keys.NUMPAD_7;
            case KeyEvent.VK_NUMPAD8:
                return Keys.NUMPAD_8;
            case KeyEvent.VK_NUMPAD9:
                return Keys.NUMPAD_9;
        }
        return Input.Keys.UNKNOWN;
    }


    // --------------------------------------------------------------------------
    // -------------------------- Nothing to see below this line except for stubs
    // --------------------------------------------------------------------------

    @Override
    public float getAccelerometerX() {
        return 0;
    }

    @Override
    public float getAccelerometerY() {
        return 0;
    }

    @Override
    public float getAccelerometerZ() {
        return 0;
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) {
        return peripheral == Peripheral.HardwareKeyboard;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public Orientation getNativeOrientation() {
        return Orientation.Landscape;
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) {
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) {

    }

    @Override
    public void openTextInputField(NativeInputConfiguration configuration) {

    }

    @Override
    public void closeTextInputField(boolean sendReturn) {

    }

    @Override
    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {

    }

    @Override
    public void vibrate(int milliseconds) {
    }

    @Override
    public void vibrate(int milliseconds, boolean fallback) {

    }

    @Override
    public void vibrate(int milliseconds, int amplitude, boolean fallback) {

    }

    @Override
    public void vibrate(VibrationType vibrationType) {

    }

    @Override
    public float getAzimuth() {
        return 0;
    }

    @Override
    public float getPitch() {
        return 0;
    }

    @Override
    public float getRoll() {
        return 0;
    }

    @Override
    public void getRotationMatrix(float[] matrix) {
    }

    @Override
    public float getGyroscopeX() {
        return 0;
    }

    @Override
    public float getGyroscopeY() {
        return 0;
    }

    @Override
    public float getGyroscopeZ() {
        return 0;
    }

    public void dispose() {
        caught = false;
        mouse.dispose();

        window.getWindowHandle().removeKeyListener(keyCallback);
        window.getWindowHandle().removeMouseListener(mouseButtonCallback);
        window.getWindowHandle().removeMouseMotionListener(cursorPosCallback);
        window.getWindowHandle().removeMouseWheelListener(mouseButtonCallback);
    }
}
