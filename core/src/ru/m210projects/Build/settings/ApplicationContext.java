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

package ru.m210projects.Build.settings;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import ru.m210projects.Build.Architecture.common.audio.AudioDriver;
import ru.m210projects.Build.Architecture.common.audio.BuildAudio;
import ru.m210projects.Build.Architecture.common.audio.MidiDevice;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;

import static ru.m210projects.Build.filehandle.fs.FileEntry.DUMMY_PATH;
import static ru.m210projects.Build.settings.ApplicationContext.ApplicationChangeListener.DUMMY_LISTENER;

/**
 * Default application context is need just for load settings from file.
 * When AWTApplication / LwjglApplication will be created
 * it will build own application context
 */
public class ApplicationContext implements ConfigContext {

    private static final String NAME = "ScreenSetup";
    private static final String SOUND_BANKS_SETUP = "SoundBanksSetup";
    /** Supported audio drivers */
    protected transient final Map<AudioDriver, BuildAudio> audioDrivers = new LinkedHashMap<>();
    /** Available midi devices and soundBanks */
    protected transient final Set<MidiDevice> midiDevices = new LinkedHashSet<>();
    /** User sound banks paths */
    protected final Set<Path> soundBanks = new LinkedHashSet<>();
    protected boolean fullscreen = false;
    protected int screenWidth = 320;
    protected boolean borderless = false;
    protected int screenHeight = 200;
    protected Renderer.RenderType renderType = Renderer.RenderType.Polymost;
    protected AudioDriver audioDriver = AudioDriver.DUMMY_AUDIO;
    protected String midiDevice = MidiDevice.DUMMY.getName();
    private ApplicationChangeListener applicationChangeListener = DUMMY_LISTENER;
    protected boolean resizable = true;

    @Override
    public void load(Properties prop) {
        if (prop.setContext(SOUND_BANKS_SETUP)) {
            Path path;
            int count = 1;
            String valueName = "SoundBankPath";

            do {
                path = prop.getPathValue(valueName + count++, DUMMY_PATH);
                if (!path.equals(DUMMY_PATH)) {
                    soundBanks.add(path);
                } else {
                    break;
                }
            } while (count < 1000);
        }


        if (prop.setContext(NAME)) {
            renderType = Renderer.RenderType.parseType(prop.getStringValue("Render", renderType.getName()));
            audioDriver = AudioDriver.parseType(prop.getStringValue("Audio", audioDriver.getName()));
            midiDevice = prop.getStringValue("MidiDriver", midiDevice);
            fullscreen = prop.getBooleanValue("Fullscreen", fullscreen);
            screenWidth = Math.max(320, prop.getIntValue("ScreenWidth", screenWidth));
            screenHeight = Math.max(200, prop.getIntValue("ScreenHeight", screenHeight));
            borderless = prop.getBooleanValue("BorderlessMode", borderless);
        }
    }

    public void setControllerListener(GameProcessor processor) {
        Controllers.addListener(processor);
    }

    public List<Controller> getControllers() {
        return Arrays.asList(Controllers.getControllers().toArray(Controller.class));
    }

    public BuildAudio getAudio() {
        return audioDrivers.get(audioDriver);
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        putString(outputStream, String.format("[%s]\r\n", SOUND_BANKS_SETUP));
        int count = 1;
        for (MidiDevice device : midiDevices) {
            if (device instanceof MidiDevice.SoundBank) {
                putPath(outputStream, "SoundBankPath" + count++, ((MidiDevice.SoundBank) device).getPath());
            }
        }

        putString(outputStream, String.format("[%s]\r\n", NAME));
        putString(outputStream, "Render", renderType.getName());
        putString(outputStream, "Audio", audioDriver.getName());
        putString(outputStream, "MidiDriver", getMidiDevice().getName());
        putBoolean(outputStream, "Fullscreen", fullscreen);
        putInteger(outputStream, "ScreenWidth", screenWidth);
        putInteger(outputStream, "ScreenHeight", screenHeight);
        putBoolean(outputStream, "BorderlessMode", borderless);
        putString(outputStream, ";\r\n;\r\n");
    }

    /**
     * @param frameContext source context that have already loaded settings
     * @return this context object
     */
    ApplicationContext init(ApplicationContext frameContext) {
        if (frameContext != null) {
            this.midiDevices.addAll(frameContext.midiDevices);
            this.audioDrivers.putAll(frameContext.audioDrivers);
            this.soundBanks.addAll(frameContext.soundBanks);
            this.audioDriver = frameContext.audioDriver;
            this.midiDevice = frameContext.midiDevice;
            this.fullscreen = frameContext.fullscreen;
            this.screenWidth = frameContext.screenWidth;
            this.screenHeight = frameContext.screenHeight;
            this.borderless = frameContext.borderless;
            this.renderType = frameContext.renderType;
            this.resizable = frameContext.resizable;
        }
        return this;
    }

    public boolean isResizable() {
        return resizable;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenMode(int width, int height, boolean fullscreen) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.fullscreen = fullscreen;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isBorderless() {
        return borderless;
    }

    public void setBorderless(boolean borderless) {
        this.borderless = borderless;
    }

    public Renderer.RenderType getRenderType() {
        return renderType;
    }

    public void setRenderType(Renderer.RenderType renderType) {
        this.renderType = renderType;
        this.applicationChangeListener.onChangeApplication(renderType);
    }

    public AudioDriver getAudioDriver() {
        return audioDriver;
    }

    public void setAudioDriver(AudioDriver audioDriver) {
        if (this.audioDrivers.isEmpty()) {
            // You should register drivers by own with cfg.registerAudioDriver(AudioDriver.DUMMY_AUDIO, new BuildAudio.DummyAudio(cfg));
            throw new RuntimeException("No audio drivers registered in config file");
        }

        BuildAudio currentAudio = audioDrivers.get(this.audioDriver);
        final boolean currentOpened = currentAudio.isOpen();
        if (audioDrivers.get(audioDriver).equals(currentAudio) && currentOpened) {
            return;
        }

        if (currentOpened) {
            currentAudio.dispose();
        }

        try {
            this.audioDrivers.get(audioDriver).open();
            this.audioDriver = audioDriver;
        } catch (Throwable t) {
            Console.out.println(t.toString(), OsdColor.RED);

            // If we can't open new audio device, try to rollback
            if (currentOpened) {
                try {
                    currentAudio.open();
                    this.audioDriver = currentAudio.getAudioDriver();
                    return;
                } catch (Throwable t1) {
                    Console.out.println(t1.toString(), OsdColor.RED);
                }
            }
            this.audioDriver = AudioDriver.DUMMY_AUDIO;
        }
    }

    public Map<String, List<Graphics.DisplayMode>> getResolutions() {
        return new HashMap<>();
    }

    public void setApplicationChangeListener(ApplicationChangeListener applicationChangeListener) {
        this.applicationChangeListener = applicationChangeListener;
    }

    public Set<Path> getSoundBankPaths() {
        return soundBanks;
    }

    public void addSoundBank(MidiDevice.SoundBank soundBank) {
        if (midiDevices.contains(soundBank)) {
            return;
        }

        midiDevices.add(soundBank);
        soundBanks.add(soundBank.getPath());
    }

    public void addMidiDevices(List<MidiDevice> midiDevices) {
        this.midiDevices.addAll(midiDevices);
    }

    public Set<MidiDevice> getMidiDevices() {
        return midiDevices;
    }

    public MidiDevice getMidiDevice() {
        return midiDevices.stream().filter(e -> e.getName().equalsIgnoreCase(midiDevice)).findAny().orElse(MidiDevice.DUMMY);
    }

    public void setMidiDevice(MidiDevice midiDevice) {
        MidiDevice currentMidiDevice = getMidiDevice();
        final boolean currentOpened = currentMidiDevice.isOpen();
        if (currentMidiDevice.equals(midiDevice) && currentOpened) {
            return;
        }

        // Close current device before open the new one
        // (In some cases we can't get two opened device
        // (Microsoft MIDI Mapper and Microsoft GS Wavetable Synth)
        if (currentOpened) {
            currentMidiDevice.close();
        }

        if (midiDevice.open()) {
            this.midiDevice = midiDevice.getName();
            return;
        }

        // If we can't open new midi device, try to rollback
        if (currentOpened) {
            currentMidiDevice.open();
        }
    }

    // The raw input settings is more fit to InputContext, but managed by application
    public boolean isRawInputSupported() {
        return false;
    }

    public boolean onRawInputChanged(boolean rawInput) {
        return false;
    }

    public interface ApplicationChangeListener {

        ApplicationChangeListener DUMMY_LISTENER = renderType -> {
        };

        void onChangeApplication(Renderer.RenderType renderType);
    }
}
