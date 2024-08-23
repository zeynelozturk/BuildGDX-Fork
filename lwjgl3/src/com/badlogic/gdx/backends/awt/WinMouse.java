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

import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;
import ru.m210projects.Build.exceptions.InitializationException;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WinMouse implements Mouse {

    private final RECT clipRect;
    private final AWTWindow window;
    private Cursor transparentCursor;
    private Robot robot; // it's working better then User32.SetCursorPos

    public WinMouse(AWTWindow window) throws InitializationException {
        try {
            // Check features
            this.clipRect = RECT.create();
            User32.ClipCursor(null);

            try {
                this.robot = new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
            } catch (AWTException e) {
                User32.SetCursorPos(window.getPositionX(), window.getPositionY()); // check if it's possible
            }
        } catch (Throwable e) {
            throw new InitializationException(e.getMessage());
        }
        this.window = window;
    }

    @Override
    public void clipCursor(Component component) {
        if (component != null) {
            Point loc = component.getLocationOnScreen();
            Rectangle rectangle = component.getBounds();
            User32.ClipCursor(clipRect.set(loc.x + rectangle.x, loc.y + rectangle.y, loc.x + rectangle.width - 1, loc.y + rectangle.height - 1));
        } else {
            User32.ClipCursor(null);
        }
    }

    @Override
    public void setCursorPosition(int x, int y) {
        if (robot == null) {
            User32.SetCursorPos(x, y);
            return;
        }

        if (EventQueue.isDispatchThread()) {
            robot.mouseMove(x, y);
        } else {
            try {
                EventQueue.invokeAndWait(() -> robot.mouseMove(x, y));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void setCursorVisible(boolean visible) {
        window.getWindowHandle().setCursor(!visible ? getTransparentCursor() : null);
    }

    @Override
    public void dispose() {
        clipRect.free();
        User32.ClipCursor(null);
    }

    private Cursor getTransparentCursor() {
        if (transparentCursor == null) {
            BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            cursorImage.setRGB(0, 0, 0);
            transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "none");
        }
        return transparentCursor;
    }
}
