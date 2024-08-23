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

package com.badlogic.gdx.backends;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.awt.AWTApplication;
import com.badlogic.gdx.backends.awt.AWTDialog;
import com.badlogic.gdx.backends.lwjgl3.AWTApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationGL10;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.device.LwjglMidiMusicDevice;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.device.SoundBankDevice;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import ru.m210projects.Build.Architecture.DialogUtil;
import ru.m210projects.Build.Architecture.common.RenderChanger;
import ru.m210projects.Build.Architecture.common.ResolutionUtils;
import ru.m210projects.Build.Architecture.common.audio.MidiDevice;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.exceptions.RendererException;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ru.m210projects.Build.Architecture.common.ResolutionUtils.getDisplayModeAsString;

public abstract class LwjglLauncherUtil {

    public static Map<String, List<Graphics.DisplayMode>> getDisplayModes() {
        return ResolutionUtils.getDisplayModes(Lwjgl3ApplicationConfiguration.getDisplayModes());
    }

    public static List<MidiDevice> getMidiDevices() {
        List<MidiDevice> midiDevices = Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(e -> {
            try {
                return MidiSystem.getMidiDevice(e);
            } catch (MidiUnavailableException ex) {
                return null;
            }
        }).filter(device -> !(device instanceof Sequencer)).map(LwjglMidiMusicDevice::new).collect(Collectors.toList());
        midiDevices.add(0, MidiDevice.DUMMY);
        return midiDevices;
    }

    public static void launch(BuildGame main, String[] icons) {
        GameConfig gameConfig = main.pCfg;

        for (Path path : gameConfig.getSoundBankPaths()) {
            if (Files.exists(path)) {
                try {
                    gameConfig.addSoundBank(new SoundBankDevice(path));
                } catch (Exception e) {
                    Console.out.println(String.format("Can't add soundbank %s: %s", path, e), OsdColor.RED);
                }
            }
        }

        new RenderChanger(main) {
            @Override
            public void startApplication(Renderer.RenderType type) {
                Thread.currentThread().setName("Main Thread");
                Thread.currentThread().setUncaughtExceptionHandler(game);
                Lwjgl3ApplicationConfiguration lwjglConfig = new Lwjgl3ApplicationConfiguration();

                // We enable it later
                lwjglConfig.disableAudio(true);
                lwjglConfig.setBackBufferConfig(8, 8, 8, 8, 24, 0, 0);
                lwjglConfig.setResizable(gameConfig.isResizable());
                lwjglConfig.setTitle(main.getTitle());
                lwjglConfig.setForegroundFPS(gameConfig.getFpslimit());
                lwjglConfig.setIdleFPS(gameConfig.getFpslimit());
                lwjglConfig.useVsync(gameConfig.isVSync());
                lwjglConfig.setWindowedMode(gameConfig.getScreenWidth(), gameConfig.getScreenHeight());
                lwjglConfig.setWindowListener(new WindowListener(main));
                lwjglConfig.setDecorated(!gameConfig.isBorderless());
                if (Platform.get() == Platform.MACOSX) {
                    Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
                }

                if (icons != null) {
                    lwjglConfig.setWindowIcon(icons);
                }
                DialogUtil.init(new AWTDialog(icons));

//                Graphics.DisplayMode fullscreenMode = getFullscreenDisplayMode(type, gameConfig);

                try {
                    switch (type) {
                        case PolyGDX:
    //                        lwjglConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
    //                        new Lwjgl3ApplicationGL10(this, lwjglConfig);
    //                        break;
                        case Polymost:
//                            if (fullscreenMode != null) {
//                                lwjglConfig.setFullscreenMode(fullscreenMode);
//                            }

                            lwjglConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL20, 3, 2);
                            new Lwjgl3ApplicationGL10(this, lwjglConfig);
                            break;
                        case Software:
                            AWTApplicationConfiguration awtConfig = new AWTApplicationConfiguration(lwjglConfig);
//                            if (fullscreenMode != null) {
//                                awtConfig.setFullscreenMode(fullscreenMode);
//                            }
                            new AWTApplication(this, awtConfig);
                            break;
                        default:
                            throw new RendererException("Unsupported renderer type " + type);
                    }
                } catch (RendererException | Error e) { // UnsatisfiedLinkError, NoClassDefFoundError
                    e.printStackTrace();
                    if (type != Renderer.RenderType.Software) {
                        Console.out.println("Can't set renderer to " + type, OsdColor.RED);
                        config.setRenderType(Renderer.RenderType.Software);
                        if (Gdx.app == null) { // First start
                            startApplication(Renderer.RenderType.Software);
                        }
                        return;
                    }
                    game.ThrowError(e.toString(), e.getStackTrace());
                }
                // application exited or crashed
            }

            private Graphics.DisplayMode getFullscreenDisplayMode(Renderer.RenderType type, GameConfig gameConfig) {
                Map<String, List<Graphics.DisplayMode>> displayModes;
                if (gameConfig.isFullscreen()) {
                    if (type == Renderer.RenderType.Software) {
                        displayModes = gameConfig.getResolutions();
                    } else {
                        displayModes = Arrays.stream(Lwjgl3ApplicationConfiguration.getDisplayModes()).sorted(Comparator.comparingInt((Graphics.DisplayMode mode) -> mode.width)
                                .thenComparingInt(mode -> mode.height)).collect(Collectors.groupingBy(e -> getDisplayModeAsString(e.width, e.height), LinkedHashMap::new, Collectors.toList()));
                    }
                    return ResolutionUtils.findDisplayMode(displayModes, gameConfig.getScreenWidth(), gameConfig.getScreenHeight());
                }
                return null;
            }

        }.startApplication(gameConfig.getRenderType());
    }
}
