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

package com.badlogic.gdx.backends.lwjgl3.audio;

import ru.m210projects.Build.Architecture.common.audio.AudioResampler;
import ru.m210projects.Build.settings.AudioContext;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.settings.Properties;

import java.util.List;

import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.alGetSourcef;

/**
 * The access class for driver settings changes
 */
public class ALAudioContext extends AudioContext {

    private final OpenALAudio audio;

    public ALAudioContext(OpenALAudio openALAudio) {
        this.audio = openALAudio;
    }

    @Override
    public void load(Properties prop) {
        throw new RuntimeException("Unsupported feature");
    }

    @Override
    public AudioResampler getResampler() {
        List<AudioResampler> resamplers = audio.getResamplerList();
        if (resamplers.isEmpty()) {
            return AudioResampler.DUMMY_RESAMPLER;
        }

        return resamplers.stream().filter(e -> e.getName().equalsIgnoreCase(resampler)).findAny().orElse(resamplers.get(0));
    }

    @Override
    public void setResampler(AudioResampler resampler) {
        audio.alAudioResampler = resampler;
        this.resampler = resampler.getName();

        for (int i = 0; i < audio.soundManager.getSize(); i++) {
            ALSource source = audio.soundManager.getSource(i);
            if (!source.isMusicSource()) {
                resampler.setToSource(source.soundId);
            }
        }

        if (!resampler.equals(AudioResampler.DUMMY_RESAMPLER)) {
            Console.out.println("Using resampler: " + resampler.getName());
        }
    }

    @Override
    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;

        for (int i = 0; i < audio.soundManager.getSize(); i++) {
            ALSource source = audio.soundManager.getSource(i);
            if (!source.isMusicSource()) {
                source.setVolume(alGetSourcef(source.soundId, AL_GAIN));
            }
        }
    }

    @Override
    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;

        for (ALMusic music : audio.getMusicSources()) {
            music.setVolume(music.getVolume());
        }
    }

    @Override
    public boolean setMaxVoices(int maxVoices) {
        if (maxVoices != audio.soundManager.getSize()) {
            audio.soundManager.dispose();
            audio.soundManager = new ALSoundManager(audio, maxVoices);
            this.maxVoices = audio.soundManager.getSize();
        }
        return this.maxVoices == maxVoices;
    }
}
