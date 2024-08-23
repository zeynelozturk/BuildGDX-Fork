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

package com.badlogic.gdx.backends.awt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.AWTApplicationConfiguration;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.*;

public class AWTGraphics implements Graphics {

    private final ComponentAdapter resizeCallback = new ComponentAdapter() {

        @Override
        public void componentResized(ComponentEvent evt) {
            if (fullscreen || !raster.isResizable()) {
                return;
            }

            Dimension size = raster.getSize();
            if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
                size.setSize(size.getWidth() / window.scaleFactor, size.getHeight() / window.scaleFactor);
            }

            updateFramebufferInfo(size.width, size.height);
        }
    };
    protected AWTWindow window;
    Raster raster;
    private volatile int backBufferWidth;
    private volatile int backBufferHeight;
    private volatile int logicalWidth;
    private volatile int logicalHeight;
    private long lastFrameTime = -1;
    private float deltaTime;
    private boolean resetDeltaTime = false;
    private long frameId;
    private long frameCounterStart = 0;
    private int frames;
    private int fps;
    private int windowPosXBeforeFullscreen;
    private int windowPosYBeforeFullscreen;
    private int windowWidthBeforeFullscreen;
    private int windowHeightBeforeFullscreen;
    private BufferFormat bufferFormat;
    private volatile boolean isContinuous = true;
    private DisplayMode displayModeBeforeFullscreen;
    private boolean fullscreen;

    public AWTGraphics(AWTWindow window) {
        this.window = window;

        com.badlogic.gdx.graphics.Color color = window.getConfig().getInitialBackgroundColor();
        this.raster = new Raster(new Color(color.r, color.g, color.b, color.a));
        AWTApplicationConfiguration config = window.getConfig();
        if (config.isWindowResizable()) {
            this.raster.addComponentListener(resizeCallback);
        }

        window.getWindowHandle().add(raster);
        updateFramebufferInfo(config.getWidth(), config.getHeight());
    }

    void updateFramebufferInfo(int width, int height) {
        AWTApplicationConfiguration config = window.getConfig();
        this.backBufferWidth = width;
        this.backBufferHeight = height;
        if (fullscreen) {
            this.logicalWidth = backBufferWidth;
            this.logicalHeight = backBufferHeight;
        } else {
            this.logicalWidth = (int) (backBufferWidth * window.scaleFactor);
            this.logicalHeight = (int) (backBufferHeight * window.scaleFactor);
        }
        bufferFormat = new BufferFormat(config.getR(), config.getG(), config.getB(), config.getA(),
                config.getDepth(), config.getStencil(), config.getSamples(), false);
        raster.update(logicalWidth, logicalHeight);

        window.getWindowHandle().pack();
        window.getWindowHandle().requestFocus();
        window.getWindowHandle().toFront();
    }

    public void repaint() {
        raster.repaint();
    }

    public void update() {
        long time = System.nanoTime();
        if (lastFrameTime == -1) lastFrameTime = time;
        if (resetDeltaTime) {
            resetDeltaTime = false;
            deltaTime = 0;
        } else
            deltaTime = (time - lastFrameTime) / 1000000000.0f;
        lastFrameTime = time;

        if (time - frameCounterStart >= 1000000000) {
            fps = frames;
            frames = 0;
            frameCounterStart = time;
        }
        frames++;
        frameId++;
    }


    @Override
    public int getWidth() {
        return backBufferWidth;
    }

    @Override
    public int getHeight() {
        return backBufferHeight;
    }

    @Override
    public int getBackBufferWidth() {
        return backBufferWidth;
    }

    @Override
    public int getBackBufferHeight() {
        return backBufferHeight;
    }


    public float getBackBufferScale() {
        return window.scaleFactor;
    }

    @Override
    public int getSafeInsetLeft() {
        return 0;
    }

    @Override
    public int getSafeInsetTop() {
        return 0;
    }

    @Override
    public int getSafeInsetBottom() {
        return 0;
    }

    @Override
    public int getSafeInsetRight() {
        return 0;
    }

    public int getLogicalWidth() {
        return logicalWidth;
    }

    public int getLogicalHeight() {
        return logicalHeight;
    }

    @Override
    public long getFrameId() {
        return frameId;
    }

    @Override
    public float getDeltaTime() {
        return deltaTime;
    }

    @Override
    @Deprecated
    public float getRawDeltaTime() {
        return 0;
    }

    @Override
    public int getFramesPerSecond() {
        return fps;
    }

    @Override
    public GraphicsType getType() {
        return GraphicsType.LWJGL3;
    }

    @Override
    public float getPpiX() {
        return getPpcX() * 2.54f;
    }

    @Override
    public float getPpiY() {
        return getPpcY() * 2.54f;
    }

    @Override
    public float getPpcX() {
        float pixelPerMm = Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
        DisplayMode mode = getDisplayMode();
        int sizeX = (int) (mode.width / pixelPerMm * 10);
        return mode.width / (float) sizeX * 10;
    }

    @Override
    public float getPpcY() {
        float pixelPerMm = Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
        DisplayMode mode = getDisplayMode();
        int sizeY = (int) (mode.height / pixelPerMm * 10);
        return mode.height / (float) sizeY * 10;
    }

    @Override
    public boolean supportsDisplayModeChange() {
        AWTMonitor monitor = (AWTMonitor) getMonitor();
        return monitor.getMonitorHandle().isDisplayChangeSupported();
    }

    @Override
    public Monitor getPrimaryMonitor() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        return new AWTMonitor(gd, bounds.x, bounds.y, gd.getIDstring());
    }

    @Override
    public Monitor getMonitor() {
        Monitor[] monitors = getMonitors();
        Monitor result = monitors[0];

        JFrame windowHandle = window.getWindowHandle();

        int windowX = windowHandle.getX();
        int windowY = windowHandle.getY();
        int windowWidth = windowHandle.getWidth();
        int windowHeight = windowHandle.getHeight();
        int overlap;
        int bestOverlap = 0;

        for (Monitor monitor : monitors) {
            DisplayMode mode = getDisplayMode(monitor);

            overlap = Math.max(0,
                    Math.min(windowX + windowWidth, monitor.virtualX + mode.width) - Math.max(windowX, monitor.virtualX))
                    * Math.max(0, Math.min(windowY + windowHeight, monitor.virtualY + mode.height) - Math.max(windowY, monitor.virtualY));

            if (bestOverlap < overlap) {
                bestOverlap = overlap;
                result = monitor;
            }
        }
        return result;
    }

    @Override
    public Monitor[] getMonitors() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Monitor[] monitors = new Monitor[gs.length];
        for (int i = 0; i < monitors.length; i++) {
            GraphicsDevice gd = gs[i];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            monitors[i] = new AWTMonitor(gd, bounds.x, bounds.y, gd.getIDstring());
        }
        return monitors;
    }

    @Override
    public DisplayMode[] getDisplayModes() {
        return getDisplayModes(getMonitor());
    }

    @Override
    public DisplayMode[] getDisplayModes(Monitor monitor) {
        java.awt.DisplayMode[] videoModes = ((AWTMonitor) monitor).getMonitorHandle().getDisplayModes();
        Set<AWTDisplayMode> modes = new LinkedHashSet<>();
        for (java.awt.DisplayMode videoMode : videoModes) {
            modes.add(new AWTDisplayMode((AWTMonitor) monitor, videoMode.getWidth(),
                    videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getBitDepth()));
        }
        DisplayMode[] result = new DisplayMode[modes.size()];
        modes.toArray(result);
        Arrays.sort(result, Comparator.comparingInt((DisplayMode mode) -> mode.width)
                .thenComparingInt(mode -> mode.height)
                .thenComparingInt(mode -> mode.refreshRate));
        return result;
    }

    @Override
    public DisplayMode getDisplayMode() {
        return getDisplayMode(getMonitor());
    }

    @Override
    public DisplayMode getDisplayMode(Monitor monitor) {
        java.awt.DisplayMode mode = ((AWTMonitor) monitor).getMonitorHandle().getDisplayMode();
        return new AWTDisplayMode((AWTMonitor) monitor, mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth());
    }

    @Override
    public boolean setFullscreenMode(DisplayMode displayMode) {
        window.getInput().resetPollingStates();
        AWTDisplayMode newMode = (AWTDisplayMode) displayMode;
        if (!isFullscreen()) {
            // store window position so we can restore it when switching from fullscreen to windowed later
            storeCurrentWindowPositionAndDisplayMode();
        }

        GraphicsDevice device = newMode.getMonitor().getMonitorHandle();
        if (!device.isFullScreenSupported()) {
            return false;
        }

        raster.setResizable(false); // disable resize listener
        window.getWindowHandle().dispose();
        window.getWindowHandle().setUndecorated(true);
        window.getWindowHandle().setVisible(true);
        device.setFullScreenWindow(window.getWindowHandle());
        if (device.isDisplayChangeSupported()) {
            fullscreen = true;
            device.setDisplayMode(new java.awt.DisplayMode(newMode.width, newMode.height, newMode.bitsPerPixel, newMode.refreshRate));
            updateFramebufferInfo(newMode.width, newMode.height);
            setVSync(window.getConfig().isVSyncEnabled());
        }
        raster.setResizable(true); // enable it back

        return fullscreen;
    }

    private void storeCurrentWindowPositionAndDisplayMode() {
        windowPosXBeforeFullscreen = window.getPositionX();
        windowPosYBeforeFullscreen = window.getPositionY();
        windowWidthBeforeFullscreen = logicalWidth;
        windowHeightBeforeFullscreen = logicalHeight;
        displayModeBeforeFullscreen = getDisplayMode();
    }

    @Override
    public boolean setWindowedMode(int width, int height) {
        raster.setResizable(false); // disable resize listener
        window.getInput().resetPollingStates();
        GraphicsDevice device = ((AWTMonitor) getMonitor()).getMonitorHandle();

        if (!isFullscreen()) { // switch between windowed modes
//            int refreshRate = device.getDisplayMode().getRefreshRate();
//            int bpp = device.getDisplayMode().getBitDepth();
//            if (device.isDisplayChangeSupported()) {
//                device.setDisplayMode(new java.awt.DisplayMode(width, height, bpp, refreshRate));
//            }

            window.getWindowHandle().setLocationRelativeTo(null);
        } else { // switch from fullscreen mode
            setUndecorated(!window.getConfig().isWindowDecorated());
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(null);
            }

            if (displayModeBeforeFullscreen == null) {
                storeCurrentWindowPositionAndDisplayMode();
            }

            int x = windowPosXBeforeFullscreen;
            int y = windowPosYBeforeFullscreen;
            if (width != windowWidthBeforeFullscreen || height != windowHeightBeforeFullscreen) { // Center window
                GraphicsConfiguration gc = device.getDefaultConfiguration();
                Rectangle bounds = gc.getBounds();
                x = Math.max(0, bounds.x + (bounds.width - width) / 2);
                y = Math.max(0, bounds.y + (bounds.height - height) / 2);
            }

//            if (device.isDisplayChangeSupported()) {
//                device.setDisplayMode(new java.awt.DisplayMode(width, height,
//                        device.getDisplayMode().getBitDepth(),
//                        java.awt.DisplayMode.REFRESH_RATE_UNKNOWN));
//            }
            window.setPosition(x, y);
        }

        fullscreen = false;
        updateFramebufferInfo(width, height);
        raster.setResizable(true); // enable it back
        return true;
    }

    @Override
    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        window.getWindowHandle().setTitle(title);
    }

    @Override
    public void setUndecorated(boolean undecorated) {
        window.getConfig().setDecorated(!undecorated);
        JFrame windowHandle = window.getWindowHandle();
        if (windowHandle.isUndecorated() == undecorated) {
            return;
        }

        windowHandle.dispose();
        windowHandle.setUndecorated(undecorated);
        windowHandle.setVisible(true);
    }

    @Override
    public void setResizable(boolean resizable) {
        window.getConfig().setResizable(resizable);
        window.getWindowHandle().setResizable(resizable);
    }

    @Override
    public void setVSync(boolean vsync) {
        window.getConfig().useVsync(vsync);
    }

    /**
     * Sets the target framerate for the application, when using continuous rendering. Must be positive. The cpu sleeps as needed.
     * Use 0 to never sleep. If there are multiple windows, the value for the first window created is used for all. Default is 0.
     *
     * @param fps fps
     */

    public void setForegroundFPS(int fps) {
        window.getConfig().setForegroundFPS(fps);
    }

    @Override
    public BufferFormat getBufferFormat() {
        return bufferFormat;
    }

    @Override
    public boolean isContinuousRendering() {
        return isContinuous;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
        this.isContinuous = isContinuous;
    }

    @Override
    public void requestRendering() {
        window.requestRendering();
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    public void dispose() {
        raster.removeComponentListener(resizeCallback);
        window.getWindowHandle().setVisible(false);
        window.getWindowHandle().dispose();
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        // FIXME: 07.10.2022
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {
        // GLFW.glfwSetCursor(getWindow().getWindowHandle(), ((Lwjgl3Cursor)cursor).glfwCursor);
    }

    @Override
    public void setSystemCursor(Cursor.SystemCursor systemCursor) {
        // Lwjgl3Cursor.setSystemCursor(getWindow().getWindowHandle(), systemCursor);
    }

    @Override
    public float getDensity () {
        float ppiX = getPpiX();
        return (ppiX > 0 && ppiX <= Float.MAX_VALUE) ? ppiX / 160f : 1f;
    }

    // Unsupported
    @Override
    public boolean supportsExtension(String extension) {
        return false;
    }

    @Override
    public boolean isGL30Available() {
        return false;
    }

    @Override
    public boolean isGL31Available() {
        return false;
    }

    @Override
    public boolean isGL32Available() {
        return false;
    }

    @Override
    public GL20 getGL20() {
        return null;
    }

    @Override
    public void setGL20(GL20 gl20) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GL30 getGL30() {
        return null;
    }

    @Override
    public void setGL30(GL30 gl30) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GL31 getGL31() {
        return null;
    }

    @Override
    public void setGL31(GL31 gl31) {

    }

    @Override
    public GL32 getGL32() {
        return null;
    }

    @Override
    public void setGL32(GL32 gl32) {

    }

    @Override
    public GLVersion getGLVersion() {
        return null;
    }

    public byte[] getFrameBuffer() {
        return raster.getFrameBuffer();
    }

    public void changePalette(byte[] palette) {
        raster.changePalette(palette);
    }

    private static class AWTDisplayMode extends DisplayMode {
        final AWTMonitor monitorHandle;

        public AWTDisplayMode(AWTMonitor monitorHandle, int width, int height, int refreshRate, int bitsPerPixel) {
            super(width, height, refreshRate, bitsPerPixel);
            this.monitorHandle = monitorHandle;
        }

        public AWTMonitor getMonitor() {
            return monitorHandle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AWTDisplayMode)) return false;
            AWTDisplayMode that = (AWTDisplayMode) o;
            return Objects.equals(monitorHandle, that.monitorHandle)
                    && width == that.width
                    && height == that.height
                    && refreshRate == that.refreshRate
                    && bitsPerPixel == that.bitsPerPixel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(monitorHandle, width, height, refreshRate, bitsPerPixel);
        }
    }

    private static class AWTMonitor extends Monitor {
        private final GraphicsDevice monitorHandle;

        AWTMonitor(GraphicsDevice monitorHandle, int virtualX, int virtualY, String name) {
            super(virtualX, virtualY, name);
            this.monitorHandle = monitorHandle;
        }

        public GraphicsDevice getMonitorHandle() {
            return monitorHandle;
        }
    }

    class Raster extends JPanel {
        // TODO Canvas has better performance, but mouse listener doesn't work with it
        // JFrame listener can be replaced to AWTListener
        private final Color background;
        private BufferedImage display;
        private byte[] data;
        private IndexColorModel paletteModel;
        private int width, height;
        private boolean resizable = true;

        public Raster(Color background) {
            this.background = background;
            this.paletteModel = new IndexColorModel(8, 256, new byte[768], 0, false);
            this.display = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
            this.data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            this.setFocusable(false);
        }

        @Override
        public void update( java.awt.Graphics g ) {
            paint(g);
        }

        @Override
        public void paint( java.awt.Graphics g ) {
            g.drawImage(display, 0, 0, null);
        }

//        @Override
//        public void paint(java.awt.Graphics g) {
//            Dimension size = getPreferredSize();
//            g.drawImage(display, 0, 0, size.width, size.height, null);
//        }

        public byte[] getFrameBuffer() {
            return data;
        }

        public void changePalette(byte[] palette) {
            paletteModel = new IndexColorModel(8, 256, palette, 0, false);
            display = new BufferedImage(paletteModel, display.getRaster(), false, null);
        }

        public void update(int width, int height) {
            if (this.width == width && this.height == height) {
                return;
            }

            this.width = width;
            this.height = height;

            Dimension size = new Dimension(width, height);
            setSize(size);
            setPreferredSize(size);
            setBackground(background);

            display = new BufferedImage(backBufferWidth, backBufferHeight, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
            data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            validate();

            if (window.isListenerInitialized()) {
                Gdx.app.postRunnable(() -> {
                    window.makeCurrent();
                    window.getListener().resize(backBufferWidth, backBufferHeight);
                    window.getListener().render();
                });
            }
        }

        /**
         * Control resize listener.
         * @return check if resize listener is active
         */
        public boolean isResizable() {
            return resizable;
        }

        /**
         * Control resize listener.
         * @param resizable true to activate resize listener
         *                  false to deactivate it (when resolution is changing)
         */
        public void setResizable(boolean resizable) {
            this.resizable = resizable;
        }
    }
}
