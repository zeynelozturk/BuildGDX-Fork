// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Architecture.common.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl3.audio.OggDecoder;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.settings.AudioContext;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.filehandle.Entry;

import java.nio.ByteBuffer;
import java.util.*;

import static ru.m210projects.Build.Architecture.common.audio.SoundData.DUMMY_DECODER;

/**
 * Custom GDX Audio device (Gdx.audio)
 */
public interface BuildAudio extends Audio {

    String getName();

    void stopAllSounds();

    boolean canPlay(int priority);

    void setListener(int x, int y, int z, int ang);

    void registerDecoder(String extension, SoundData.Decoder decoder);

    @NotNull
    SoundData.Decoder getSoundDecoder(String extension);

    //EFX
    boolean isEFXSupport();

    float getReverb();

    void setReverb(boolean enable, float delay);

    float getEcho();

    void setEcho(boolean enable, float delay);

    void update();

    @Nullable
    Sound newSound(ByteBuffer buffer, int rate, int bits, AudioChannel channel, int priority);

    @Nullable
    Sound newSound(ByteBuffer buffer, int rate, int bits, int priority);

    @Nullable
    Music newMusic(Entry file);

    List<AudioResampler> getResamplerList();

    void dispose();

    BuildAudio open() throws InitializationException;

    boolean isOpen();

    AudioDriver getAudioDriver();

    class DummyAudio extends MockAudio implements BuildAudio {
        private final GameConfig config;
        private boolean opened = false;
        private final Map<String, SoundData.Decoder> decoders = new HashMap<>();

        public DummyAudio(GameConfig config) {
            this.config = config;

            registerDecoder("wav", new WAVDecoder());
            registerDecoder("ogg", new OggDecoder());
        }

        @Override
        public String getName() {
            return "Dummy audio driver";
        }

        @Override
        public void stopAllSounds() {
        }

        @Override
        public boolean canPlay(int priority) {
            return false;
        }

        @Override
        public void setListener(int x, int y, int z, int ang) {
        }

        @Override
        public void registerDecoder(String extension, SoundData.Decoder decoder) {
            decoders.put(extension.toUpperCase(Locale.ROOT), decoder);
        }

        @Override
        public SoundData.@NotNull Decoder getSoundDecoder(String extension) {
            return decoders.getOrDefault(extension.toUpperCase(Locale.ROOT), DUMMY_DECODER);
        }

        @Override
        public boolean isEFXSupport() {
            return false;
        }

        @Override
        public float getReverb() {
            return 0;
        }

        @Override
        public void setReverb(boolean enable, float delay) {
        }

        @Override
        public float getEcho() {
            return 0;
        }

        @Override
        public void setEcho(boolean enable, float delay) {

        }

        @Override
        public @Nullable Sound newSound(ByteBuffer buffer, int rate, int bits, AudioChannel channel, int priority) {
            return null;
        }

        @Override
        public @Nullable Sound newSound(ByteBuffer buffer, int rate, int bits, int priority) {
            return null;
        }

        @Override
        public @Nullable Music newMusic(Entry file) {
            if (file == null) {
                throw new IllegalArgumentException("file cannot be null.");
            }

            if (!file.exists()) {
                return null;
            }

            return config.getMidiDevice().newMusic(file);
        }

        @Override
        public List<AudioResampler> getResamplerList() {
            return Collections.singletonList(AudioResampler.DUMMY_RESAMPLER);
        }

        @Override
        public BuildAudio open() {
            Console.out.println("Initialization " + getName() + "...");
            config.setAudioContext(new AudioContext());
            opened = true;
            return this;
        }

        @Override
        public boolean isOpen() {
            return opened;
        }

        @Override
        public void dispose() {
            super.dispose();
            opened = false;
        }

        @Override
        public AudioDriver getAudioDriver() {
            return AudioDriver.DUMMY_AUDIO;
        }
    }

}
