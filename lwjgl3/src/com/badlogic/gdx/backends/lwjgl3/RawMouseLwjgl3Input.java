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

package com.badlogic.gdx.backends.lwjgl3;

import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

public class RawMouseLwjgl3Input extends DefaultLwjgl3Input {

    private boolean cursorCatched = false;
    private boolean rawInputEnabled = false;
    private GLFWCursorPosCallback cursorPosCallback;

    public RawMouseLwjgl3Input(Lwjgl3Window window) {
        super(window);

        GLFW.glfwSetCursorEnterCallback(window.getWindowHandle(), new GLFWCursorEnterCallback() {
            @Override
            public void invoke(long windowHandle, boolean entered) {
                handleCursor(window, entered);
            }
        });
    }

    @Override
    public void windowHandleChanged (long windowHandle) {
        super.windowHandleChanged(windowHandle);
        this.cursorPosCallback = new GLFWCursorPosCallback() {
            private int logicalMouseX;
            private int logicalMouseY;

            @Override
            public void invoke (long windowHandle, double x, double y) {
                // Make this callback because of Libgdx bug with deltaX/Y. Lots of missing events on low fps,
                // because deltas just sets to one last event between update ticks
                deltaX += (int)x - logicalMouseX;
                deltaY += (int)y - logicalMouseY;
                mouseX = logicalMouseX = (int)x;
                mouseY = logicalMouseY = (int)y;

                if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
                    float xScale = window.getGraphics().getBackBufferWidth() / (float)window.getGraphics().getLogicalWidth();
                    float yScale = window.getGraphics().getBackBufferHeight() / (float)window.getGraphics().getLogicalHeight();
                    deltaX = (int)(deltaX * xScale);
                    deltaY = (int)(deltaY * yScale);
                    mouseX = (int)(mouseX * xScale);
                    mouseY = (int)(mouseY * yScale);
                }

                window.getGraphics().requestRendering();
                long time = System.nanoTime();
                if (mousePressed > 0) {
                    eventQueue.touchDragged(mouseX, mouseY, 0, time);
                } else {
                    eventQueue.mouseMoved(mouseX, mouseY, time);
                }
            }
        };
        GLFW.glfwSetCursorPosCallback(window.getWindowHandle(), cursorPosCallback);
    }

    public boolean setRawInput(boolean enable) {
        if (rawInputEnabled != enable && GLFW.glfwRawMouseMotionSupported()) {
            this.rawInputEnabled = enable;
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_RAW_MOUSE_MOTION, enable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
            return GLFW.glfwGetInputMode(window.getWindowHandle(), GLFW.GLFW_RAW_MOUSE_MOTION) == GLFW.GLFW_TRUE;
        }
        return rawInputEnabled;
    }

    private void handleCursor(Lwjgl3Window window, boolean entered) {
        if (entered && GLFW.glfwGetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE) {
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, cursorCatched ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_HIDDEN);
        } else {
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    @Override
    public void setCursorCatched(boolean catched) {
        this.cursorCatched = catched;
        if (catched) {
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        } else {
            handleCursor(window, true);
        }
    }

    @Override
    public void setCursorPosition(int x, int y) {
        if (!rawInputEnabled) {
            if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
                float xScale = window.getGraphics().getLogicalWidth() / (float)window.getGraphics().getBackBufferWidth();
                float yScale = window.getGraphics().getLogicalHeight() / (float)window.getGraphics().getBackBufferHeight();
                x = (int)(x * xScale);
                y = (int)(y * yScale);
            }
            GLFW.glfwSetCursorPos(window.getWindowHandle(), x, y);
            cursorPosCallback.invoke(window.getWindowHandle(), x, y);
        }
    }

    public boolean isRawInputSupported() {
        return GLFW.glfwRawMouseMotionSupported();
    }

    @Override
    public void dispose () {
        super.dispose();
        cursorPosCallback.free();
    }
}
