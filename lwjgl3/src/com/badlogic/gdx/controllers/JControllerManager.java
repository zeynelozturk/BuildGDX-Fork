package com.badlogic.gdx.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.utils.Architecture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import de.ralleytn.plugins.jinput.xinput.XInputEnvironmentPlugin;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.File;

import static com.badlogic.gdx.utils.SharedLibraryLoader.*;

public class JControllerManager extends AbstractControllerManager implements Runnable, net.java.games.input.ControllerListener {

    private static boolean loadNatives, controllersLoaded;
    private final JControllerListener compositeListener = new JControllerListener();

    public JControllerManager() {
        load();

        compositeListener.addListener(new ManageCurrentControllerListener());
    }

    @Override
    public Array<com.badlogic.gdx.controllers.Controller> getControllers() {
        if (!controllersLoaded) {
            ControllerEnvironment controllerEnvironment = new XInputEnvironmentPlugin();
            if(!controllerEnvironment.isSupported()) {
                controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
            }

            for (Controller input : controllerEnvironment.getControllers()) {
                Controller.Type type = input.getType();
                if (type == Controller.Type.STICK
                        || type == Controller.Type.GAMEPAD
                        || type == Controller.Type.WHEEL
                        || type == Controller.Type.FINGERSTICK) {
                    controllerAdded(new ControllerEvent(input));
                }
            }

            if (!controllers.isEmpty()) {
                controllerEnvironment.addControllerListener(this);
                Gdx.app.postRunnable(this);
            }

            Gdx.app.addLifecycleListener(new LifecycleListener() {
                @Override
                public void resume() {
                }

                @Override
                public void pause() {
                }

                @Override
                public void dispose() {
                    JControllerManager.this.dispose();
                }
            });

            controllersLoaded = true;
        }

        return super.getControllers();
    }

    private static void load() {
        if (loadNatives) {
            return;
        }

        SharedLibraryLoader loader = new SharedLibraryLoader();
        File nativesDir = null;
        try {
            if (os == Os.Windows) {
                nativesDir = loader.extractFile(bitness == Architecture.Bitness._64 ? "jinput-dx8_64.dll" : "jinput-dx8.dll", null).getParentFile();
                loader.extractFileTo(bitness == Architecture.Bitness._64 ? "jinput-raw_64.dll" : "jinput-raw.dll", nativesDir);
            } else if (os == Os.MacOsX) {
                nativesDir = loader.extractFile("libjinput-osx.jnilib", null).getParentFile();
            } else if (os == Os.Linux) {
                nativesDir = loader.extractFile(bitness == Architecture.Bitness._64 ? "libjinput-linux64.so" : "libjinput-linux.so", null)
                        .getParentFile();
                loader.extractFileTo(bitness == Architecture.Bitness._64 ? "libjinput-linux64.so" : "libjinput-linux.so", nativesDir);
            }
        } catch (Throwable ex) {
            Console.out.println("Unable to extract JInput natives.", OsdColor.RED);
            Console.out.println(ex.getMessage(), OsdColor.RED);
        }

        if (nativesDir != null) { // FreeBSD not supported
            System.setProperty("net.java.games.input.librarypath", nativesDir.getAbsolutePath());
        }
        loadNatives = true;
    }

    @Override
    public void addListener(ControllerListener listener) {
        compositeListener.addListener(listener);

        int size = controllers.size;
        Object[] items = controllers.items;
        for (int i = 0; i < size; i++) {
            Object item = items[i];
            if (item instanceof AbstractController) {
                ((AbstractController) item).addListener(listener);
            }
        }
    }

    @Override
    public void removeListener(ControllerListener listener) {
        compositeListener.removeListener(listener);

        int size = controllers.size;
        Object[] items = controllers.items;
        for (int i = 0; i < size; i++) {
            Object item = items[i];
            if (item instanceof AbstractController) {
                ((AbstractController) item).removeListener(listener);
            }
        }
    }

    @Override
    public Array<ControllerListener> getListeners() {
        Array<ControllerListener> array = new Array<>();
        array.add(compositeListener);
        return array;
    }

    @Override
    public void clearListeners() {
        compositeListener.clear();
        compositeListener.addListener(new ManageCurrentControllerListener());
    }

    @Override
    public void run() {
        int size = controllers.size;
        Object[] items = controllers.items;

        for (int i = 0; i < size; i++) {
            Object item = items[i];
            if (item instanceof JController) {
                JController controller = (JController) item;
                if (!controller.update()) {
                    controllerRemoved(new ControllerEvent(controller.controller));
                }
            }
        }

        if (!controllers.isEmpty()) {
            Gdx.app.postRunnable(this);
        }
    }

    @Override
    public void controllerRemoved(ControllerEvent controllerEvent) {
        int size = controllers.size;
        Object[] items = controllers.items;
        final Controller c = controllerEvent.getController();

        for (int i = 0; i < size; i++) {
            Object item = items[i];
            if (item instanceof JController) {
                JController controller = (JController) item;
                if (controller.controller.equals(c)) {
                    compositeListener.disconnected(controller);
                    controllers.removeIndex(i);
                    controller.dispose();
                    break;
                }
            }
        }
    }

    @Override
    public void controllerAdded(ControllerEvent controllerEvent) {
        final Controller c = controllerEvent.getController();
        AbstractController controller;
        if (c.getClass().getSimpleName().equals("XIController")) {
            controller = new JXController(c);
        } else {
            controller = new JController(c);
        }

        compositeListener.connected(controller);
        controllers.add(controller);
    }

    public void dispose() {
        // #GDX 02.01.2025 Killing of "net.java.games.input.RawInputEventQueue$QueueThread" is need in case of
        // rescan controllers, that still doesn't work in JInput. So, it's dead code and can be removed
    }
}
