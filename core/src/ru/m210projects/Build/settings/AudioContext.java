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

import ru.m210projects.Build.Architecture.common.audio.AudioResampler;
import ru.m210projects.Build.Gameutils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Audio settings class. Will be changed by audio driver
 */
public class AudioContext implements ConfigContext {

    private static final String NAME = "SoundSetup";

    protected float soundVolume = 1.00f;
    protected float musicVolume = 1.00f;
    protected boolean noSound = false;
    protected boolean muteMusic = false;
    protected String resampler = AudioResampler.DUMMY_RESAMPLER.getName();
    protected int musicType = 0;
    protected int maxVoices = 16;

    /**
     * @param audioContext source context that have already loaded settings
     * @return this context object
     */
    AudioContext init(AudioContext audioContext) {
        if (audioContext != null) {
            this.soundVolume = audioContext.soundVolume;
            this.musicVolume = audioContext.musicVolume;
            this.noSound = audioContext.noSound;
            this.muteMusic = audioContext.muteMusic;
            this.resampler = audioContext.resampler;
            this.musicType = audioContext.musicType;
            this.maxVoices = audioContext.maxVoices;
        }
        return this;
    }

    @Override
    public void load(Properties prop) {
        if (prop.setContext(NAME)) {
            noSound = prop.getBooleanValue("NoSound", isNoSound());
            muteMusic = prop.getBooleanValue("NoMusic", isMuteMusic());
            musicType = prop.getIntValue("MusicType", musicType);
            soundVolume = Gameutils.BClipRange(prop.getFloatValue("SoundVolume", getSoundVolume()), 0.0f, 1.0f);
            musicVolume = Gameutils.BClipRange(prop.getFloatValue("MusicVolume", getMusicVolume()), 0.0f, 1.0f);
            resampler = prop.getStringValue("Resampler", resampler);
            maxVoices = Gameutils.BClipRange(prop.getIntValue("MaxVoices", getMaxVoices()), 4, 256);
        }
    }

    @Override
    public void save(OutputStream os) throws IOException {
        putString(os, ";\r\n");
        putString(os, String.format("[%s]\r\n", NAME));
        putBoolean(os, "NoSound", noSound);
        putBoolean(os, "NoMusic", muteMusic);
        putInteger(os, "MusicType", musicType);
        putFloat(os, "SoundVolume", soundVolume);
        putFloat(os, "MusicVolume", musicVolume);
        putInteger(os, "MaxVoices", maxVoices);
        putString(os, "Resampler", resampler);
        putString(os, ";\r\n;\r\n");
    }

    public AudioResampler getResampler() {
        return AudioResampler.DUMMY_RESAMPLER;
    }

    public void setResampler(AudioResampler resampler) {
        this.resampler = resampler.getName();
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public boolean isNoSound() {
        return noSound;
    }

    public void setNoSound(boolean noSound) {
        this.noSound = noSound;
    }

    public boolean isMuteMusic() {
        return muteMusic;
    }

    public void setMuteMusic(boolean muteMusic) {
        this.muteMusic = muteMusic;
    }

    public int getMaxVoices() {
        return maxVoices;
    }

    public boolean setMaxVoices(int maxVoices) {
        this.maxVoices = maxVoices;
        return true;
    }
}
