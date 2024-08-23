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

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

public class AWTApplicationConfiguration extends Lwjgl3ApplicationConfiguration {

    private final Lwjgl3ApplicationConfiguration config;

    private Graphics.DisplayMode fullscreenMode;

    public AWTApplicationConfiguration(Lwjgl3ApplicationConfiguration config) {
        this.config = config;
    }

    public int getWidth() {
        return config.windowWidth;
    }

    public int getHeight() {
        return config.windowHeight;
    }

    public String getTitle() {
        return config.title;
    }

    public boolean isVSyncEnabled() {
        return config.vSyncEnabled;
    }

    public boolean isAudioDisabled() {
        return config.disableAudio;
    }

    public int getForegroundFPS() {
        return config.foregroundFPS;
    }

    public int getIdleFPS() {
        return config.idleFPS;
    }

    public int getR() {
        return config.r;
    }

    public int getG() {
        return config.g;
    }

    public int getB() {
        return config.b;
    }

    public int getA() {
        return config.a;
    }

    public int getDepth() {
        return config.depth;
    }

    public int getStencil() {
        return config.stencil;
    }

    public int getSamples() {
        return config.samples;
    }

    public String getPreferencesDirectory() {
        return config.preferencesDirectory;
    }

    public Files.FileType getPreferencesFileType() {
        return config.preferencesFileType;
    }

    public int getAudioDeviceSimultaneousSources() {
        return config.audioDeviceSimultaneousSources;
    }

    public int getAudioDeviceBufferSize() {
        return config.audioDeviceBufferSize;
    }

    public int getAudioDeviceBufferCount() {
        return config.audioDeviceBufferCount;
    }

    public Color getInitialBackgroundColor() {
        return config.initialBackgroundColor;
    }

    public boolean isWindowResizable() {
        return config.windowResizable;
    }

    public boolean isWindowDecorated() {
        return config.windowDecorated;
    }

    public int getWindowX() {
        return config.windowX;
    }

    public int getWindowY() {
        return config.windowY;
    }

    public Graphics.DisplayMode getFullscreenMode() {
        return fullscreenMode;
    }

    public Lwjgl3WindowListener getWindowListener() {
        return config.windowListener;
    }

    public String[] getWindowIconPaths() {
        return config.windowIconPaths;
    }

    public Files.FileType getWindowIconFileType() {
        return config.windowIconFileType;
    }


    @Override
    void set(Lwjgl3ApplicationConfiguration config) {
        this.config.set(config);
    }

    @Override
    public void setIdleFPS(int fps) {
        config.setIdleFPS(fps);
    }

    @Override
    public void setForegroundFPS(int fps) {
        config.setForegroundFPS(fps);
    }

    @Override
    public void setPreferencesConfig(String preferencesDirectory, Files.FileType preferencesFileType) {
        config.setPreferencesConfig(preferencesDirectory, preferencesFileType);
    }

    @Override
    public void setHdpiMode(HdpiMode mode) {
        config.setHdpiMode(mode);
    }


    @Override
    void setWindowConfiguration(Lwjgl3WindowConfiguration config) {
        this.config.setWindowConfiguration(config);
    }

    @Override
    public void setWindowedMode(int width, int height) {
        config.setWindowedMode(width, height);
    }

    @Override
    public void setResizable(boolean resizable) {
        config.setResizable(resizable);
    }

    @Override
    public void setDecorated(boolean decorated) {
        config.setDecorated(decorated);
    }

    @Override
    public void setMaximized(boolean maximized) {
        config.setMaximized(maximized);
    }

    @Override
    public void setMaximizedMonitor(Graphics.Monitor monitor) {
        config.setMaximizedMonitor(monitor);
    }

    @Override
    public void setAutoIconify(boolean autoIconify) {
        config.setAutoIconify(autoIconify);
    }

    @Override
    public void setWindowPosition(int x, int y) {
        config.setWindowPosition(x, y);
    }

    @Override
    public void setWindowSizeLimits(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        config.setWindowSizeLimits(minWidth, minHeight, maxWidth, maxHeight);
    }

    @Override
    public void setWindowIcon(String... filePaths) {
        config.setWindowIcon(filePaths);
    }

    @Override
    public void setWindowIcon(Files.FileType fileType, String... filePaths) {
        config.setWindowIcon(fileType, filePaths);
    }

    @Override
    public void setWindowListener(Lwjgl3WindowListener windowListener) {
        config.setWindowListener(windowListener);
    }

    @Override
    public void setFullscreenMode(Graphics.DisplayMode mode) {
        this.fullscreenMode = mode;
    }

    @Override
    public void setTitle(String title) {
        config.setTitle(title);
    }

    @Override
    public void setInitialBackgroundColor(Color color) {
        config.setInitialBackgroundColor(color);
    }

    @Override
    public void useVsync(boolean vsync) {
        config.useVsync(vsync);
    }
}
