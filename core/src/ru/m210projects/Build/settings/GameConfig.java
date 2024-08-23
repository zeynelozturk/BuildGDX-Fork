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
import ru.m210projects.Build.Architecture.common.audio.AudioDriver;
import ru.m210projects.Build.Architecture.common.audio.AudioResampler;
import ru.m210projects.Build.Architecture.common.audio.BuildAudio;
import ru.m210projects.Build.Architecture.common.audio.MidiDevice;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.m210projects.Build.net.Mmulti.NETPORT;
import static ru.m210projects.Build.filehandle.fs.FileEntry.DUMMY_PATH;

public abstract class GameConfig implements ConfigContext {

    public static GameConfig DUMMY_CONFIG = new GameConfig(null) {
        @Override
        protected InputContext createDefaultInputContext() {
            return null;
        }

        @Override
        protected ConfigContext createDefaultGameContext() {
            return null;
        }
    };

    public static final int VERSION = 2401; // year XX, num XX

    // main context
    protected final Path cfgPath;
    protected ApplicationContext applicationContext;
    protected VideoContext videoContext;
    protected AudioContext audioContext;
    protected final InputContext inputContext;
    protected final ConfigContext gameContext;
    protected Path gamePath = DUMMY_PATH;
    protected boolean startup = true;
    protected boolean autoloadFolder = true;
    // Menu context
    protected int gMouseCursor = 0;
    protected int gMouseCursorSize = 65536;
    protected boolean legacyTimer = false;
    // Network context
    protected String pName = "";
    protected String mAddress = "localhost";
    protected int mPort = NETPORT;

    public GameConfig(Path cfgPath) {
        this.cfgPath = cfgPath;
        this.applicationContext = new ApplicationContext();
        this.videoContext = createDefaultVideoContext();
        this.audioContext = new AudioContext();
        this.inputContext = createDefaultInputContext();
        this.gameContext = createDefaultGameContext();
    }

    public void load() {
        try (InputStream is = new FileInputStream(cfgPath.toFile())) {
            this.load(new Properties(new InputStreamReader(is)));
            return;
        } catch (FileNotFoundException e) {
            Console.out.println("File not found: " + cfgPath, OsdColor.RED);
        } catch (Exception e) {
            e.printStackTrace();
            Console.out.println("Read file error: " + e, OsdColor.RED);
        }
        // if can't read config file, fill input by default
        inputContext.resetInput(false);
    }

    protected abstract InputContext createDefaultInputContext();

    protected abstract ConfigContext createDefaultGameContext();

    protected VideoContext createDefaultVideoContext() {
        return new VideoContext();
    }

    @Override
    public void load(Properties prop) {
        int version = VERSION;
        if (prop.setContext("Main")) {
            version = prop.getIntValue("ConfigVersion", 0);
            startup = prop.getBooleanValue("Startup", startup);
            autoloadFolder = prop.getBooleanValue("AutoloadFolder", autoloadFolder);
            gamePath = prop.getPathValue("Path", DUMMY_PATH);

            Console.out.setValue("osdtextscale", prop.getFloatValue("ConsoleTextScale", 1.0f));
            gMouseCursor = prop.getIntValue("MouseCursor", gMouseCursor);
            gMouseCursorSize = prop.getIntValue("MouseCursorSize", gMouseCursorSize);
            legacyTimer = prop.getBooleanValue("UseLegacyTimer", legacyTimer);
            pName = prop.getStringValue("Player_name", pName);
            mAddress = prop.getStringValue("IP_Address", mAddress);
            mPort = prop.getIntValue("Port", mPort);
        }

        applicationContext.load(prop);
        if (version == VERSION) {
            videoContext.load(prop);
            audioContext.load(prop);
            inputContext.load(prop);
            gameContext.load(prop);
        } else {
            inputContext.resetInput(false);
        }
    }

    @Override
    public void save(OutputStream os) throws IOException {
        putString(os, "[Main]\r\n");

        putInteger(os, "ConfigVersion", VERSION);
        putBoolean(os, "Startup", startup);
        putBoolean(os, "AutoloadFolder", autoloadFolder);
        putPath(os, "Path", gamePath);
        putString(os, ";\r\n");

        putFloat(os, "ConsoleTextScale", Console.out.getValue("osdtextscale"));
        putInteger(os, "MouseCursor", gMouseCursor);
        putInteger(os, "MouseCursorSize", gMouseCursorSize);
        putBoolean(os, "UseLegacyTimer", legacyTimer);
        putString(os, "Player_name", pName);
        putString(os, "IP_Address", mAddress);
        putInteger(os, "Port", mPort);

        applicationContext.save(os);
        videoContext.save(os);
        audioContext.save(os);
        inputContext.save(os);
        gameContext.save(os);
    }

    /**
     * @return is application window frame resizable
     */
    public boolean isResizable() {
        return applicationContext.isResizable();
    }

    /**
     * @return available video resolutions map
     * key: resolution name (WxH bpp)
     * value: list of {@link Graphics.DisplayMode}
     */
    public Map<String, List<Graphics.DisplayMode>> getResolutions() {
        return applicationContext.getResolutions();
    }

    /**
     * Initializes game midi devices. Should be called at application start before load the config
     * @param midiDevices midi devices list
     */
    public void addMidiDevices(List<MidiDevice> midiDevices) {
        applicationContext.addMidiDevices(midiDevices);
    }

    /**
     * Adds audio device to use as sound driver. Should be called at application start before load the config
     * @param audioDriver audio driver enum
     * @param audio Build audio object (OpenAL, DummyAudio, etc.)
     */
    public void registerAudioDriver(AudioDriver audioDriver, BuildAudio audio) {
        applicationContext.audioDrivers.put(audioDriver, audio);
    }

    /**
     * Adds a soundBank midi device to devices list. Should be called after addMidiDevices
     * @param soundBank soundBank midi device
     */
    public void addSoundBank(MidiDevice.SoundBank soundBank) {
        applicationContext.addSoundBank(soundBank);
    }

    /**
     * @return list of available midi devices
     */
    public List<MidiDevice> getMidiDevices() {
        return new ArrayList<>(applicationContext.getMidiDevices());
    }

    /**
     * @return list of available audio devices
     */
    public List<BuildAudio> getAudioDevices() {
        return new ArrayList<>(applicationContext.audioDrivers.values());
    }

    public AudioResampler getResampler() {
        return audioContext.getResampler();
    }

    public void setResampler(AudioResampler resampler) {
        audioContext.setResampler(resampler);
    }

    public GameKey convertToGameKey(int keycode) {
        GameKey gameKey = inputContext.arrayPressedKey[keycode];
        if (gameKey != null) {
            return gameKey;
        }
        return GameKey.UNKNOWN_KEY;
    }

    public ControllerMapping getControllerMapping(String controllerName) {
        return inputContext.getControllerMapping(controllerName);
    }

    public void resetInput(boolean classicKeys) {
        inputContext.resetInput(classicKeys);
    }

    // getters and setters

    public List<Controller> getControllers() {
        return applicationContext.getControllers();
    }

    public String getControllerName() {
        return inputContext.getControllerName();
    }

    public void setControllerName(String controllerName) {
        inputContext.setControllerName(controllerName);
    }

    public void setVideoContext(VideoContext videoContext) {
        this.videoContext = videoContext.init(this.videoContext);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext.init(this.applicationContext);
    }

    public Path getGamePath() {
        return gamePath;
    }

    public void setGamePath(Path gamePath) {
        this.gamePath = gamePath;
    }

    public Path getCfgPath() {
        return cfgPath;
    }

    public void bindKey(GameKey gameKey, int keyCode) {
        inputContext.bindKey(gameKey, keyCode);
    }

    public void bindMouse(GameKey gameKey, int keyCode) {
        inputContext.bindMouse(gameKey, keyCode);
    }

    public void unbindAll(GameKey gameKey) {
        inputContext.unbindAll(gameKey);
    }

    public boolean isLegacyTimer() {
        return legacyTimer;
    }

    public void setLegacyTimer(boolean legacyTimer) {
        this.legacyTimer = legacyTimer;
    }

    /**
     * Method to show bindings in setup menu
     * @param gameKey game func key
     * @param index (key slot)
     * @return keyCode if exists
     */
    public int getKeyCode(GameKey gameKey, int index) {
        return inputContext.getKeyCode(gameKey, index);
    }

    public GameKey[] getKeymap() {
        return inputContext.keymap;
    }

    public GameKey getMouseAxis(MouseAxis mouseAxis) {
        GameKey gameKey = inputContext.mouseaxis[mouseAxis.ordinal()];
        if (gameKey != null) {
            return gameKey;
        }
        return GameKey.UNKNOWN_KEY;
    }

    public void setMouseAxis(MouseAxis mouseAxis, GameKey key) {
        inputContext.mouseaxis[mouseAxis.ordinal()] = key;
    }

    public boolean isStartup() {
        return startup;
    }

    public void setStartup(boolean startup) {
        this.startup = startup;
    }

    public boolean isAutoloadFolder() {
        return autoloadFolder;
    }

    public void setAutoloadFolder(boolean autoloadFolder) {
        this.autoloadFolder = autoloadFolder;
    }

    public Set<Path> getSoundBankPaths() {
        return applicationContext.getSoundBankPaths();
    }

    public boolean isFullscreen() {
        return applicationContext.isFullscreen();
    }

    public int getScreenWidth() {
        return applicationContext.getScreenWidth();
    }

    public void setScreenMode(int width, int height, boolean fullscreen) {
        applicationContext.setScreenMode(width, height, fullscreen);

//        if (Gdx.app != null) {
//            Gdx.app.postRunnable(() -> applicationContext.setScreenMode(width, height, fullscreen));
//        } else { // set screen from launcher
//            applicationContext.setScreenMode(width, height, fullscreen);
//        }
    }

    public int getScreenHeight() {
        return applicationContext.getScreenHeight();
    }

    public boolean isVSync() {
        return videoContext.isgVSync();
    }

    public void setgVSync(boolean gVSync) {
        videoContext.setgVSync(gVSync);
    }

    public int getFpslimit() {
        return videoContext.getFpslimit();
    }

    public void setFpslimit(int fpslimit) {
        videoContext.setFpslimit(fpslimit);
    }

    public boolean isBorderless() {
        return applicationContext.isBorderless();
    }

    public void setBorderless(boolean borderless) {
        applicationContext.setBorderless(borderless);
    }

    public BuildAudio getAudio() {
        return applicationContext.getAudio();
    }

    public AudioDriver getAudioDriver() {
        return applicationContext.getAudioDriver();
    }

    public void setAudioDriver(AudioDriver audioDriver) {
        applicationContext.setAudioDriver(audioDriver);
    }

    public void setAudioContext(AudioContext audioContext) {
        this.audioContext = audioContext.init(this.audioContext);
    }

    public MidiDevice getMidiDevice() {
        return applicationContext.getMidiDevice();
    }

    public boolean setMidiDevice(MidiDevice middrv) {
        applicationContext.setMidiDevice(middrv);
        MidiDevice currentMidi = applicationContext.getMidiDevice();
        if (currentMidi.isOpen()) {
            currentMidi.setMasterVolume(audioContext.musicVolume);
        }
        return currentMidi == middrv;
    }

    public int getPaletteGamma() {
        return videoContext.getPaletteGamma();
    }

    public void setPaletteGamma(int paletteGamma) {
        videoContext.setPaletteGamma(paletteGamma);
    }

    public float getFgamma() {
        return videoContext.getFgamma();
    }

    public void setFgamma(float fgamma) {
        videoContext.setFgamma(fgamma);
    }

    public float getgFpsScale() {
        return videoContext.getgFpsScale();
    }

    public void setgFpsScale(float gFpsScale) {
        videoContext.setgFpsScale(gFpsScale);
    }

    public boolean isUseMouse() {
        return inputContext.useMouse;
    }

    public void setUseMouse(boolean useMouse) {
        inputContext.useMouse = useMouse;
    }

    public boolean isMenuMouse() {
        return inputContext.menuMouse;
    }

    public void setMenuMouse(boolean menuMouse) {
        inputContext.menuMouse = menuMouse;
    }

    public int getSensitivity() {
        return inputContext.gSensitivity;
    }

    public void setgSensitivity(int gSensitivity) {
        inputContext.gSensitivity = gSensitivity;
    }

    public int getgMouseTurnSpeed() {
        return inputContext.gMouseTurnSpeed;
    }

    public void setgMouseTurnSpeed(int gMouseTurnSpeed) {
        inputContext.gMouseTurnSpeed = gMouseTurnSpeed;
    }

    public int getgMouseLookSpeed() {
        return inputContext.gMouseLookSpeed;
    }

    public void setgMouseLookSpeed(int gMouseLookSpeed) {
        inputContext.gMouseLookSpeed = gMouseLookSpeed;
    }

    public int getgMouseMoveSpeed() {
        return inputContext.gMouseMoveSpeed;
    }

    public void setgMouseMoveSpeed(int gMouseMoveSpeed) {
        inputContext.gMouseMoveSpeed = gMouseMoveSpeed;
    }

    public int getgMouseStrafeSpeed() {
        return inputContext.gMouseStrafeSpeed;
    }

    public void setgMouseStrafeSpeed(int gMouseStrafeSpeed) {
        inputContext.gMouseStrafeSpeed = gMouseStrafeSpeed;
    }

    public int getgMouseCursor() {
        return gMouseCursor;
    }

    public void setgMouseCursor(int gMouseCursor) {
        this.gMouseCursor = gMouseCursor;
    }

    public int getgMouseCursorSize() {
        return gMouseCursorSize;
    }

    public void setgMouseCursorSize(int gMouseCursorSize) {
        this.gMouseCursorSize = gMouseCursorSize;
    }

    public boolean isgMouseAim() {
        return inputContext.gMouseAim;
    }

    public void setgMouseAim(boolean gMouseAim) {
        inputContext.gMouseAim = gMouseAim;
    }

    public boolean isgInvertmouse() {
        return inputContext.gInvertmouse;
    }

    public void setgInvertmouse(boolean gInvertmouse) {
        inputContext.gInvertmouse = gInvertmouse;
    }

    public boolean isRawInput() {
        return inputContext.isRawInput();
    }

    public void setRawInput(boolean rawInput) {
        this.inputContext.setRawInput(isRawInputSupported() && applicationContext.onRawInputChanged(rawInput));
    }

    public boolean isRawInputSupported() {
        return applicationContext.isRawInputSupported();
    }

    public float getSoundVolume() {
        return audioContext.getSoundVolume();
    }

    public void setSoundVolume(float soundVolume) {
        audioContext.setSoundVolume(soundVolume);
    }

    public float getMusicVolume() {
        return audioContext.getMusicVolume();
    }

    public void setMusicVolume(float musicVolume) {
        audioContext.setMusicVolume(musicVolume);
        MidiDevice midiDevice = applicationContext.getMidiDevice();
        if (!midiDevice.isOpen()) {
            // you should call setMidiDevice() before use sound driver
            throw new RuntimeException("Midi device is not opened!");
        }
        midiDevice.setMasterVolume(musicVolume);
    }

    public boolean isNoSound() {
        return audioContext.isNoSound();
    }

    public void setNoSound(boolean noSound) {
        audioContext.setNoSound(noSound);
    }

    public boolean isMuteMusic() {
        return audioContext.isMuteMusic();
    }

    public void setMuteMusic(boolean muteMusic) {
        audioContext.setMuteMusic(muteMusic);
    }

    public int getMaxvoices() {
        return audioContext.getMaxVoices();
    }

    public boolean setMaxvoices(int maxvoices) {
        return audioContext.setMaxVoices(maxvoices);
    }

    public int getMusicType() {
        return audioContext.musicType;
    }

    public void setMusicType(int musicType) {
        audioContext.musicType = musicType;
    }

    public boolean isPaletteEmulation() {
        return videoContext.isPaletteEmulation();
    }

    public void setPaletteEmulation(boolean paletteEmulation) {
        videoContext.setPaletteEmulation(paletteEmulation);
    }

    public int getWidescreen() {
        return videoContext.isWidescreen() ? 1 : 0;
    }

    public void setWidescreen(int widescreen) {
        videoContext.setWidescreen(widescreen == 1);
    }

    public TexFilter getGlfilter() {
        return videoContext.getTextureFilter();
    }

    public void setGlfilter(TexFilter glfilter) {
        videoContext.setTextureFilter(glfilter);
    }

    public boolean isgShowFPS() {
        return videoContext.isgShowFPS();
    }

    public void setgShowFPS(boolean gShowFPS) {
        videoContext.setgShowFPS(gShowFPS);
    }

    public int getgFov() {
        return videoContext.getgFov();
    }

    public void setgFov(int gFov) {
        videoContext.setgFov(gFov);
    }

    public String getpName() {
        return pName;
    }

    public void setpName(String pName) {
        this.pName = pName;
    }

    public String getmAddress() {
        return mAddress;
    }

    public void setmAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int mPort) {
        this.mPort = mPort;
    }

    public Renderer.RenderType getRenderType() {
        return applicationContext.getRenderType();
    }

    public void setRenderType(Renderer.RenderType renderType) {
        applicationContext.setRenderType(renderType);
    }

    public boolean isUseVoxels() {
        return videoContext.isUseVoxels();
    }

    public void setUseVoxels(boolean useVoxels) {
        videoContext.setUseVoxels(useVoxels);
    }


    public boolean isDetailMapping() {
        return videoContext.isDetailMapping();
    }

    public void setDetailMapping(boolean detailMapping) {
        videoContext.setDetailMapping(detailMapping);
    }

    public boolean isGlowMapping() {
        return videoContext.isGlowMapping();
    }

    public void setGlowMapping(boolean glowMapping) {
        videoContext.setGlowMapping(glowMapping);
    }

    public boolean isUseHighTiles() {
        return videoContext.isUseHighTiles();
    }

    public void setUseHighTiles(boolean useHighTiles) {
        videoContext.setUseHighTiles(useHighTiles);
    }

    public boolean isUseModels() {
        return videoContext.isUseModels();
    }

    public void setUseModels(boolean useModels) {
        videoContext.setUseModels(useModels);
    }

    public boolean isAnimSmoothing() {
        return videoContext.isAnimSmoothing();
    }

    public void setAnimSmoothing(boolean animSmoothing) {
        videoContext.setAnimSmoothing(animSmoothing);
    }

    public boolean isJoyInvert() {
        return inputContext.isJoyInvert();
    }

    public void setJoyInvert(boolean value) {
        inputContext.setJoyInvert(value);
    }

    public float getJoyLookSpeed() {
        return inputContext.getJoyLookSpeed();
    }

    public float getJoyTurnSpeed() {
        return inputContext.getJoyTurnSpeed();
    }

    public float getJoyDeadZone() {
        return inputContext.getJoyDeadZone();
    }

    public void setJoyDeadZone(float joyDeadZone) {
        inputContext.setJoyDeadZone(joyDeadZone);
    }

    public void setJoyTurnSpeed(float joyTurnSpeed) {
        inputContext.setJoyTurnSpeed(joyTurnSpeed);
    }

    public void setJoyLookSpeed(float joyLookSpeed) {
        inputContext.setJoyLookSpeed(joyLookSpeed);
    }
}
