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

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.AWTApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AWTWindow extends WindowAdapter implements Disposable {

    private JFrame windowHandle;

    float scaleFactor;
    final ApplicationListener listener;
    private final AWTApplicationConfiguration config;
    private AWTGraphics graphics;
    private AWTInput input;
    Lwjgl3WindowListener windowListener;
    private boolean listenerInitialized = false;
    private final Array<Runnable> runnables = new Array<>();
    private final Array<Runnable> executedRunnables = new Array<>();
    boolean iconified = false;
    boolean focused = false;
    boolean activated = false;
    boolean closing = false;
    private boolean requestRendering = false;

    @Override
    public void windowIconified(WindowEvent e) {
        postRunnable(() -> {
            iconified = true;
            if (windowListener != null) {
                windowListener.iconified(true);
            }
            listener.pause();
        });
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        postRunnable(() -> {
            iconified = false;
            if (windowListener != null) {
                windowListener.iconified(false);
            }
            listener.resume();
        });
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        postRunnable(() -> {
            focused = true;
            if (windowListener != null) {
                windowListener.focusGained();
            }
        });
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        postRunnable(() -> {
            focused = false;
            if (windowListener != null) {
                windowListener.focusLost();
            }
        });
    }

    @Override
    public void windowActivated(WindowEvent e) {
        activated = true;
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        activated = false;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        postRunnable(() -> {
            closing = true;
            if (windowListener != null) {
                windowListener.closeRequested();
            }
        });
    }

    public AWTWindow(ApplicationListener listener, AWTApplicationConfiguration config) {
        this.listener = listener;
        this.config = config;
        this.windowListener = config.getWindowListener();
    }

    /**
     * @return the {@link ApplicationListener} associated with this window
     **/
    public ApplicationListener getListener() {
        return listener;
    }

    boolean isListenerInitialized() {
        return listenerInitialized;
    }

    void initializeListener() {
        if (!listenerInitialized) {
            listener.create();
            listener.resize(graphics.getWidth(), graphics.getHeight());
            listenerInitialized = true;
        }
    }

    public void swapBuffers() {
        graphics.repaint();

        if (config.isVSyncEnabled()) {
            Toolkit.getDefaultToolkit().sync();
        }
    }

    void makeCurrent() {
        Gdx.graphics = graphics;
        Gdx.input = input;
    }

    @Override
    public void dispose() {
        windowHandle.removeWindowListener(this);
        windowHandle.removeWindowFocusListener(this);
        listener.pause();
        listener.dispose();
        graphics.dispose();
        input.dispose();
        windowHandle.setVisible(false);
        windowHandle.dispose();
    }

    public boolean update() {
        if (!listenerInitialized) {
            initializeListener();
        }
        synchronized (runnables) {
            executedRunnables.addAll(runnables);
            runnables.clear();
        }
        for (Runnable runnable : executedRunnables) {
            runnable.run();
        }
        boolean shouldRender = executedRunnables.size > 0 || graphics.isContinuousRendering();
        executedRunnables.clear();

        if (!iconified) {
            input.update();
        }

        synchronized (this) {
            shouldRender |= requestRendering && !iconified;
            requestRendering = false;
        }

        if (shouldRender) {
            graphics.update();
            listener.render();
            swapBuffers();
        }

        if (!iconified) {
            input.prepareNext();
        }
        input.pollEvents();

        return shouldRender;
    }

    public boolean shouldClose() {
        return closing;
    }

    void requestRendering() {
        synchronized (this) {
            this.requestRendering = true;
        }
    }

    public void create() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();

        scaleFactor = 1.0f;
        if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
            int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
            scaleFactor = 96.0f / resolution;
        }

        windowHandle = new JFrame(device.getDefaultConfiguration());
        windowHandle.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        windowHandle.setTitle(config.getTitle());
        windowHandle.setMinimumSize(new Dimension(320, 200));
        windowHandle.setVisible(false);
        windowHandle.setResizable(config.isWindowResizable());
        windowHandle.setUndecorated(!config.isWindowDecorated());
        windowHandle.setFocusable(true);

        try {
            String[] iconPaths = config.getWindowIconPaths();
            if (iconPaths != null && iconPaths.length > 0) {
                Files.FileType iconFileType = config.getWindowIconFileType();
                List<Image> icons = new ArrayList<>();
                for (String iconPath : iconPaths) {
                    FileHandle file = Gdx.files.getFileHandle(iconPath, iconFileType);
                    ImageIcon icon = new ImageIcon(file.readBytes());
                    icons.add(icon.getImage());
                }
                windowHandle.setIconImages(icons);
            }
        } catch (Exception ignored) {
        }

        this.graphics = new AWTGraphics(this);
        this.input = new AWTInput(this);

        windowHandle.addWindowListener(this);
        windowHandle.addWindowFocusListener(this);

        new DropTarget(windowHandle, new DropTargetAdapter() {
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = event.getTransferable();
                DataFlavor[] flavors = transferable.getTransferDataFlavors();
                for (DataFlavor flavor : flavors) {
                    try {
                        if (flavor.isFlavorJavaFileListType()) {
                            final String[] files = ((java.util.List<File>) transferable.getTransferData(flavor)).stream().map(File::getPath).toArray(String[]::new);
                            postRunnable(() -> {
                                if (windowListener != null) {
                                    windowListener.filesDropped(files);
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                event.dropComplete(true);
            }
        });

        if (config.getFullscreenMode() == null) {
            windowHandle.setLocationRelativeTo(null);
        }

        if (windowListener != null) {
            windowListener.created(null);
        }
    }

    public void postRunnable (Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
        }
    }

    public JFrame getWindowHandle() {
        return windowHandle;
    }

    public void setVisible(boolean initialVisible) {
        windowHandle.setVisible(initialVisible);
    }

    public void setPosition(int x, int y) {
        windowHandle.setLocation(x, y);
    }

    public int getPositionX() {
        return windowHandle.getX();
    }

    public int getPositionY() {
        return windowHandle.getY();
    }

    public AWTInput getInput() {
        return input;
    }

    public AWTGraphics getGraphics() {
        return graphics;
    }

    public AWTApplicationConfiguration getConfig() {
        return config;
    }

}
