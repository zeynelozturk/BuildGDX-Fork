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

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import ru.m210projects.Build.Architecture.common.RenderChanger;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Software.Software;

import java.io.File;

public class AWTApplication implements Application {

    public static HdpiMode hdpiMode = HdpiMode.Logical;
    final Array<AWTWindow> windows = new Array<>();
    private final RenderChanger gameListener;
    private final AWTApplicationConfiguration config;
    private final Lwjgl3Audio audio;
    private final Files files;
    private final Net net;
    private final ObjectMap<String, Preferences> preferences = new ObjectMap<>();
    private final Lwjgl3Clipboard clipboard;
    private final Array<Runnable> runnables = new Array<>();
    private final Array<Runnable> executedRunnables = new Array<>();
    private final Array<LifecycleListener> lifecycleListeners = new Array<>();
    private volatile AWTWindow currentWindow;
    private int logLevel = LOG_INFO;
    private ApplicationLogger applicationLogger;
    private volatile boolean running = true;

    public AWTApplication(RenderChanger listener, AWTApplicationConfiguration config) {
        this.config = config;
        this.gameListener = listener;
        setApplicationLogger(new Lwjgl3ApplicationLogger());

        // Need for GDX Pixmap
        Lwjgl3NativesLoader.load();
        if (config.getTitle() == null) {
            config.setTitle(listener.getClass().getSimpleName());
        }

        Gdx.app = this;
        Gdx.audio = this.audio = new MockAudio();
        this.files = Gdx.files = createFiles();
        this.net = Gdx.net = new Lwjgl3Net(config);
        this.clipboard = new Lwjgl3Clipboard();

        AWTWindow window = createWindow(config, listener);
        windows.add(window);
        try {
            loop();
            cleanupWindows();
        } catch (Throwable t) {
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else
                throw new GdxRuntimeException(t);
        } finally {
            cleanup();
        }
    }

    protected void loop() {
        Array<AWTWindow> closedWindows = new Array<>();
        while (running && windows.size > 0) {
            if (!config.isAudioDisabled()) {
                audio.update();
            }

            boolean haveWindowsRendered = false;
            closedWindows.clear();
            int targetFramerate = -2;
            for (AWTWindow window : windows) {
                window.makeCurrent();
                currentWindow = window;
                if (targetFramerate == -2) targetFramerate = window.getConfig().getForegroundFPS();
                synchronized (lifecycleListeners) {
                    haveWindowsRendered |= window.update();
                }
                if (window.shouldClose()) {
                    closedWindows.add(window);
                }
            }

            boolean shouldRequestRendering;
            synchronized (runnables) {
                shouldRequestRendering = runnables.size > 0;
                executedRunnables.clear();
                executedRunnables.addAll(runnables);
                runnables.clear();
            }
            for (Runnable runnable : executedRunnables) {
                runnable.run();
            }
            if (shouldRequestRendering) {
                // Must follow Runnables execution so changes done by Runnables are reflected
                // in the following render.
                for (AWTWindow window : windows) {
                    if (!window.getGraphics().isContinuousRendering()) {
                        window.requestRendering();
                    }
                }
            }

            for (AWTWindow closedWindow : closedWindows) {
                if (windows.size == 1) {
                    // Lifecycle listener methods have to be called before ApplicationListener methods. The
                    // application will be disposed when _all_ windows have been disposed, which is the case,
                    // when there is only 1 window left, which is in the process of being disposed.
                    for (int i = lifecycleListeners.size - 1; i >= 0; i--) {
                        LifecycleListener l = lifecycleListeners.get(i);
                        l.pause();
                        l.dispose();
                    }
                    lifecycleListeners.clear();
                }
                closedWindow.dispose();

                windows.removeValue(closedWindow, false);
            }

            if (!haveWindowsRendered) {
                // Sleep a few milliseconds in case no rendering was requested
                // with continuous rendering disabled.
                try {
                    Thread.sleep(1000 / config.getIdleFPS());
                } catch (InterruptedException ignore) {
                }
            } else if (targetFramerate > 0) {
                AWTSync.sync(targetFramerate); // sleep as needed to meet the target framerate
            }
        }
    }

    private AWTWindow createWindow(AWTApplicationConfiguration config, ApplicationListener listener) {
        final AWTWindow window = new AWTWindow(listener, config);
        // the main window is created immediately
        createWindow(window);

        return window;
    }

    protected void createWindow(AWTWindow window) {
        this.gameListener.setApplicationContext(new AWTApplicationContext(this));

        window.create();

        // like in Lwjgl3Application, this is the one place, where we can get game listener
        // and create renderer

        // TODO: The renderer should be created in application context
        BuildGame game = gameListener.getGame();
        game.setRenderer(game.getFactory().renderer(game.pCfg.getRenderType()));
        Software software = (Software) game.getRenderer();
        software.setChangeListener(new Software.ChangeListener() {
            final AWTGraphics graphics = window.getGraphics();

            @Override
            public void onChangePalette(byte[] palette) {
                graphics.changePalette(palette);
            }

            @Override
            public void onNextPage(byte[] frameBuffer) {
                byte[] dst = graphics.getFrameBuffer();
                System.arraycopy(frameBuffer, 0, dst, 0, Math.min(frameBuffer.length, dst.length));
            }
        });

//        Graphics.DisplayMode displayMode = config.getFullscreenMode();
//        if (displayMode != null) {
//            game.pCfg.setScreenMode(displayMode.width, displayMode.height, true);
//        }

        window.setVisible(true);
    }

    protected void cleanupWindows() {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.pause();
                lifecycleListener.dispose();
            }
        }
        for (AWTWindow window : windows) {
            window.dispose();
        }
        windows.clear();
        running = false;
    }

    protected void cleanup() {
        if (running) {
            for (AWTWindow window : windows) {
                window.getGraphics().dispose();
            }
            windows.clear();
        }
        audio.dispose();
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return currentWindow.getListener();
    }

    @Override
    public Graphics getGraphics() {
        return currentWindow.getGraphics();
    }

    @Override
    public Audio getAudio() {
        return audio;
    }

    @Override
    public Input getInput() {
        return currentWindow.getInput();
    }

    @Override
    public Files getFiles() {
        return files;
    }

    @Override
    public Net getNet() {
        return net;
    }

    @Override
    public void debug(String tag, String message) {
        if (logLevel >= LOG_DEBUG) getApplicationLogger().debug(tag, message);
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_DEBUG) getApplicationLogger().debug(tag, message, exception);
    }

    @Override
    public void log(String tag, String message) {
        if (logLevel >= LOG_INFO) getApplicationLogger().log(tag, message);
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_INFO) getApplicationLogger().log(tag, message, exception);
    }

    @Override
    public void error(String tag, String message) {
        if (logLevel >= LOG_ERROR) getApplicationLogger().error(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_ERROR) getApplicationLogger().error(tag, message, exception);
    }

    @Override
    public int getLogLevel() {
        return logLevel;
    }

    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public ApplicationLogger getApplicationLogger() {
        return applicationLogger;
    }

    @Override
    public void setApplicationLogger(ApplicationLogger applicationLogger) {
        this.applicationLogger = applicationLogger;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.Desktop;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return getJavaHeap();
    }

    @Override
    public Preferences getPreferences(String name) {
        if (preferences.containsKey(name)) {
            return preferences.get(name);
        } else {
            Preferences prefs = new Lwjgl3Preferences(
                    new Lwjgl3FileHandle(new File(config.getPreferencesDirectory(), name), config.getPreferencesFileType()));
            preferences.put(name, prefs);
            return prefs;
        }
    }

    @Override
    public Clipboard getClipboard() {
        return clipboard;
    }

    @Override
    public void postRunnable(Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
        }
    }

    @Override
    public void exit() {
        running = false;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

    protected Files createFiles() {
        return new Lwjgl3Files();
    }

    public boolean isActive() {
        return currentWindow.activated; // FIXME
    }
}
