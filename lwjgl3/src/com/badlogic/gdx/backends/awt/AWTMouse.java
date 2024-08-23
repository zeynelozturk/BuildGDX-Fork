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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import static java.awt.event.MouseEvent.MOUSE_MOVED;

public class AWTMouse implements Mouse, MouseListener {

    AWTWindow window;
    Robot robot;
    private final CatchRunnable catchRunnable;
    private Cursor transparentCursor;

    public AWTMouse(AWTWindow window) {
        try {
            this.robot = new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        this.window = window;
        this.catchRunnable = new CatchRunnable();

        // This is a way to track mouse point when it outs of the frame
        Thread catchThread = new Thread(catchRunnable);
        catchThread.setName("Catch Cursor Thread");
        catchThread.setDaemon(true);
        catchThread.start();

        window.getWindowHandle().addMouseListener(this);
    }

    @Override
    public void setCursorPosition(int x, int y) {
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

    private Cursor getTransparentCursor() {
        if (transparentCursor == null) {
            BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            cursorImage.setRGB(0, 0, 0);
            transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "none");
        }
        return transparentCursor;
    }

    @Override
    public void clipCursor(Component c) {
        /* unsupported */
    }

    @Override
    public void dispose() {
        window.getWindowHandle().removeMouseListener(this);
        catchRunnable.dispose();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        catchRunnable.pause();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (window.getInput().caught) {
            catchRunnable.resume();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    private class CatchRunnable implements Runnable {

        int lastX = 0;
        int lastY = 0;
        private boolean running = true;
        private boolean paused = true;

        @Override
        public void run() {
            while (running) {
                if (paused) {
                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException ex) {
                        break;
                    }
                }

                if (!window.focused) {
                    pause();
                    continue;
                }

                Point pointLocation = MouseInfo.getPointerInfo().getLocation();
                if (lastX != pointLocation.x && lastY != pointLocation.y) {
                    lastX = pointLocation.x;
                    lastY = pointLocation.y;

                    final Component windowHandle = window.getWindowHandle();
                    if (windowHandle.isShowing()) {
                        Point windowLocation = windowHandle.getLocationOnScreen();
                        pointLocation.translate(-windowLocation.x, -windowLocation.y);

                        window.getInput().cursorPosCallback.mouseMoved(new MouseEvent(windowHandle, MOUSE_MOVED, System.currentTimeMillis(), 0, pointLocation.x, pointLocation.y, 0, false));
                    }
                }
            }
        }

        public void dispose() {
            running = false;
            resume();
        }

        public void pause() {
            paused = true;
        }

        public synchronized void resume() {
            paused = false;
            notify();
        }
    }
}
