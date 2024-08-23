package com.badlogic.gdx.backends.lwjgl3.audio;

import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.LwjglMidiMusicSource;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.Architecture.common.audio.*;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static ru.m210projects.Build.Architecture.common.audio.SoundData.DUMMY_DECODER;
import static ru.m210projects.Build.Architecture.common.audio.SoundManager.FREE_SOURCE_PRIORITY;

public class OpenALAudio implements BuildAudio, Lwjgl3Audio {

    private final ObjectMap<String, Class<? extends Music>> extensionToMusicClass = new ObjectMap<>();

    final GameConfig config;

    protected long alDevice;
    protected long alContext;

    private String name = "OpenAL Sound";
    protected AudioResampler alAudioResampler = AudioResampler.DUMMY_RESAMPLER;
    private boolean opened = false;

    private final Map<String, SoundData.Decoder> decoders = new HashMap<>();
    protected List<AudioResampler> alAudioResaplerList;
    protected ALEchoEffect alEchoEffect;
    protected ALReverbEffect alReverbEffect;
    protected ALSoundManager soundManager;
    private final FloatBuffer orientation = BufferUtils.createFloatBuffer(6);
    Array<ALMusic> music = new Array<>(false, 1, ALMusic.class);
    ScheduledExecutorService audioExecutor;

    /**
     * OpenAL sound driver constructor.
     * @param config current game configuration object
     */
    public OpenALAudio(GameConfig config) {
        this.config = config;

        registerMusic("ogg", ALOgg.Music.class);
        registerMusic(MidiDevice.MIDI_EXTENSION, LwjglMidiMusicSource.class);

        registerDecoder("wav", new WAVDecoder());
        registerDecoder("ogg", new OggDecoder());
    }

    @Override
    public List<AudioResampler> getResamplerList() {
        return alAudioResaplerList;
    }

    public void registerMusic (String extension, Class<? extends Music> musicClass) {
        if (extension == null) {
            throw new IllegalArgumentException("extension cannot be null.");
        }
        if (musicClass == null) {
            throw new IllegalArgumentException("musicClass cannot be null.");
        }
        extensionToMusicClass.put(extension, musicClass);
    }

    @Override
    public void dispose() {
        if (opened) {
            audioExecutor.shutdown();
            music.clear();
            soundManager.dispose();
            if (alEchoEffect != null) {
                alEchoEffect.dispose();
            }

            if (alReverbEffect != null) {
                alReverbEffect.dispose();
            }
            alcDestroyContext(alContext);
            alcCloseDevice(alDevice);
            opened = false;
        }
    }

    /**
     * Tries initializing driver and sets itself as a sound driver in current config if successfully
     */
    @Override
    public BuildAudio open() throws InitializationException {
        this.alDevice = alcOpenDevice((ByteBuffer) null);
        if (alDevice == 0L) {
            throw new InitializationException("Open device failed!");
        }

        ALCCapabilities deviceCapabilities = ALC.createCapabilities(alDevice);
        alContext = alcCreateContext(alDevice, (IntBuffer) null);
        if (alContext == 0L) {
            alcCloseDevice(alDevice);
            throw new InitializationException("Create device context failed!");
        }

        if (!alcMakeContextCurrent(alContext)) {
            alcDestroyContext(alContext);
            alcCloseDevice(alDevice);
            throw new InitializationException("Making to current device context failed!");
        }

        AL.createCapabilities(deviceCapabilities);
        alGetError();

        alDistanceModel(AL_NONE);
        orientation.clear();
        orientation.put(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
        ((Buffer) orientation).flip();
        alListenerfv(AL_ORIENTATION, orientation);
        FloatBuffer velocity = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
        ((Buffer) velocity).flip();
        alListenerfv(AL_VELOCITY, velocity);
        FloatBuffer position = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
        ((Buffer) position).flip();
        alListenerfv(AL_POSITION, position);

        String version = AL10.alGetString(AL10.AL_VERSION);
        this.name = ALC10.alcGetString(alDevice, ALC10.ALC_DEVICE_SPECIFIER) + " " + version;

        alAudioResaplerList = new ArrayList<>();

        try {
            ALCapabilities caps = AL.getCapabilities();
            int sourceCount = config.getMaxvoices();

            if (caps.AL_SOFT_source_resampler) {
                int alNumResamplers = AL10.alGetInteger(SOFTSourceResampler.AL_NUM_RESAMPLERS_SOFT);
                for (int i = 0; i < alNumResamplers; i++) {
                    String name = SOFTSourceResampler.alGetStringiSOFT(SOFTSourceResampler.AL_RESAMPLER_NAME_SOFT, i);
                    if (alGetError() != AL_NO_ERROR) {
                        break;
                    }

                    if (name != null) {
                        final int index = i;
                        AudioResampler resampler = new AudioResampler(name) {
                            @Override
                            public void setToSource(int sourceId) {
                                AL10.alSourcei(sourceId, SOFTSourceResampler.AL_SOURCE_RESAMPLER_SOFT, index);
                            }
                        };
                        alAudioResaplerList.add(resampler);
                    }
                }
            }

            boolean alEfxSupport = caps.ALC_EXT_EFX;
            if (alEfxSupport) {
                alEchoEffect = new ALEchoEffect();
                alReverbEffect = new ALReverbEffect();
            }

            this.soundManager = new ALSoundManager(this, sourceCount);

            Console.out.println(name + " initialized", OsdColor.GREEN);
            Console.out.println("\twith max voices: " + sourceCount, OsdColor.GREEN);
            Console.out.println("\tOpenAL version: " + version, OsdColor.GREEN);

            config.setAudioContext(new ALAudioContext(this));

            if (caps.AL_SOFT_source_resampler) {
                Console.out.println("AL_SOFT_Source_Resampler enabled.", OsdColor.GREEN);
                config.setResampler(config.getResampler());
            }

            if (alEfxSupport) {
                Console.out.println("ALC_EXT_EFX enabled.");
            } else {
                Console.out.println("ALC_EXT_EFX not support!");
            }

            audioExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("OpenAL Update Thread");
                thread.setDaemon(true);
                return thread;
            });
            audioExecutor.scheduleAtFixedRate(this::update, 0, 100, TimeUnit.MILLISECONDS);
            this.opened = true;
        } catch (IllegalStateException ignored) {
        }
        return this;
    }

    public Array<ALMusic> getMusicSources() {
        return music;
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public AudioDriver getAudioDriver() {
        return AudioDriver.OPENAL_AUDIO;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEFXSupport() {
        return (alEchoEffect != null && alEchoEffect.isEFXSupport()) || (alReverbEffect != null && alReverbEffect.isEFXSupport());
    }

    @Override
    public void stopAllSounds() {
        if (opened) {
            soundManager.stopAllSounds();
        }
    }

    @Override
    public float getReverb() {
        return alReverbEffect.getDelay();
    }

    @Override
    public void setReverb(boolean enable, float delay) {
        alReverbEffect.setReverb(enable, delay);
        for (int i = 0; i < soundManager.getSize(); i++) {
            ALSource source = soundManager.getSource(i);
            if (source.isMusicSource()) {
                continue;
            }
            alReverbEffect.setToSource(source.soundId);
        }
    }

    @Override
    public float getEcho() {
        return alEchoEffect.getDelay();
    }

    @Override
    public void setEcho(boolean enable, float delay) {
        alEchoEffect.setReverb(enable, delay);
        for (int i = 0; i < soundManager.getSize(); i++) {
            ALSource source = soundManager.getSource(i);
            if (source.isMusicSource()) {
                continue;
            }
            alEchoEffect.setToSource(source.soundId);
        }
    }

    @Override
    public void update() {
        soundManager.update();
        for (int i = 0; i < music.size; i++) {
            music.items[i].update();
        }
    }

    protected int getALFormat(int channels, int bits) {
        boolean stereo = (channels > 1);

        switch (bits) {
            case 16:
                if (stereo) {
                    return AL_FORMAT_STEREO16;
                } else {
                    return AL_FORMAT_MONO16;
                }
            case 8:
                if (stereo) {
                    return AL_FORMAT_STEREO8;
                } else {
                    return AL_FORMAT_MONO8;
                }
            default:
                return -1;
        }
    }

    @Override
    public @Nullable Sound newSound(ByteBuffer buffer, int rate, int bits, AudioChannel channel, int priority) {
        int sourceIndex = soundManager.obtainSource(priority);
        if (sourceIndex == -1) {
            return null;
        }

        ALSource alSource = soundManager.getSource(sourceIndex);
        int sourceId = alSource.soundId;
        alAudioResampler.setToSource(sourceId);

        if (alEchoEffect != null) {
            alEchoEffect.setToSource(sourceId);
        }

        if (alReverbEffect != null) {
            alReverbEffect.setToSource(sourceId);
        }

        alSource.setVolume(0.0f);

        int format = getALFormat(channel.getChannels(), bits);
        if (format == -1) {
            Console.out.println("OpenAL Error wrong bits: " + bits, OsdColor.RED);
            alSource.stop();
            return null;
        }

        int bufferID = alSource.buffedId;
        alBufferData(bufferID, format, buffer, rate);
        alSourcei(sourceId, AL_BUFFER, bufferID);
        alSource.loopInfo.init(buffer, format, rate);

        alSource.checkErrors("newSound");
        return alSource;
    }

    @Override
    public Sound newSound(ByteBuffer buffer, int rate, int bits, int priority) {
        return newSound(buffer, rate, bits, AudioChannel.MONO, priority);
    }

    @Override
    public Music newMusic(Entry file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }

        if (!file.exists()) {
            return null;
        }

        Class<? extends Music> musicClass = extensionToMusicClass.get(file.getExtension().toLowerCase());
        if (musicClass == null) {
            Console.out.println("Unknown file extension for music: " + file, OsdColor.RED);
            return null;
        }

        // if music class is midi music, give it to midi device
        if (musicClass.equals(LwjglMidiMusicSource.class)) {
            return config.getMidiDevice().newMusic(file);
        }

        try {
            return musicClass.getConstructor(new Class[]{OpenALAudio.class, Entry.class}).newInstance(this, file);
        } catch (Exception ex) {
            Console.out.println("Error creating music " + musicClass.getName() + " for file: " + file, OsdColor.RED);
            return null;
        }
    }

    @Override
    public boolean canPlay(int priority) {
        int index = soundManager.element();
        if (index != -1) {
            ALSource source = soundManager.getSource(index);
            return source.getPriority() == FREE_SOURCE_PRIORITY || source.getPriority() < priority;
        }
        return false;
    }

    @Override
    public void setListener(int x, int y, int z, int ang) {
        alListener3f(AL_POSITION, x, y, z);
        double angle = (ang * 2 * Math.PI) / 2048.0;
        orientation.put(0, (float) Math.cos(angle));
        orientation.put(1, 0);
        orientation.put(2, (float) Math.sin(angle));
        orientation.rewind();
        alListenerfv(AL_ORIENTATION, orientation);
    }

    @Override
    public void registerDecoder(String extension, SoundData.Decoder decoder) {
        decoders.put(extension.toUpperCase(), decoder);
    }

    @Override
    public SoundData.@NotNull Decoder getSoundDecoder(String extension) {
        return decoders.getOrDefault(extension.toUpperCase(), DUMMY_DECODER);
    }

    // not implemented methods

    @Override
    public Sound newSound(FileHandle fileHandle) {
        return null;
    }

    @Override
    public Music newMusic(FileHandle file) {
        return null;
    }

    @Override
    public AudioDevice newAudioDevice(int samplingRate, boolean isMono) {
        return null;
    }

    @Override
    public AudioRecorder newAudioRecorder(int samplingRate, boolean isMono) {
        return null;
    }

    @Override
    public boolean switchOutputDevice(String deviceIdentifier) {
        return false;
    }

    @Override
    public String[] getAvailableOutputDevices() {
        return new String[0];
    }
}
