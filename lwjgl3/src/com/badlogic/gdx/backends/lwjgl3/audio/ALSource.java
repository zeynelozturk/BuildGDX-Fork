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

import org.lwjgl.openal.SOFTDirectChannels;
import org.lwjgl.openal.SOFTDirectChannelsRemix;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.Architecture.common.audio.LoopInfo;
import ru.m210projects.Build.Architecture.common.audio.SoundManager;
import ru.m210projects.Build.Architecture.common.audio.SourceListener;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL10.*;
import static ru.m210projects.Build.Architecture.common.audio.SoundManager.FREE_SOURCE_PRIORITY;

public class ALSource implements SoundManager.ManageableSource {

    private SourceListener listener;

    int soundId;
    int buffedId;
    int sourceIndex;
    private int priority;
    final LoopInfo loopInfo;
    OpenALAudio audio;
    boolean musicSource = false;

    public ALSource(OpenALAudio audio, int sourceIndex) throws InitializationException {
        this.audio = audio;
        this.sourceIndex = sourceIndex;
        this.loopInfo = new LoopInfo();

        int soundId = alGenSources();
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new InitializationException("Source initialization failed: 0x" + Integer.toHexString(error));
        }

        this.buffedId = alGenBuffers();
        error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new InitializationException("Source buffer initialization failed: 0x" + Integer.toHexString(error));
        }
        this.soundId = soundId;
        reset();
    }

    @Override
    public void setMusicSource(boolean musicSource) {
        this.musicSource = musicSource;
    }

    @Override
    public boolean isMusicSource() {
        return musicSource;
    }

    @Override
    public void setPosition(int x, int y, int z) {
        alSource3f(soundId, AL_POSITION, x, y, z);
        alSourcei(soundId, AL_SOURCE_RELATIVE, AL_FALSE); // setGlobal(false)
        checkErrors("setPosition");
    }

    @Override
    public void setVolume(float volume) {
        volume = Math.min(Math.max(volume, 0.0f), 1.0f);
        if (musicSource) {
            volume *= audio.config.getMusicVolume();
        } else {
            volume *= audio.config.getSoundVolume();
        }
        alSourcef(soundId, AL_GAIN, volume);
        checkErrors("setVolume");
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(priority, 0);
    }

    @Override
    public void setPitch(float pitch) {
        alSourcef(soundId, AL_PITCH, pitch);
        checkErrors("setPitch");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isActive() {
        return /*isPlaying() || alGetSourcei(soundId, AL_SOURCE_STATE) == AL_PAUSED && */priority > FREE_SOURCE_PRIORITY;
    }

    @Override
    public boolean isPlaying() {
        return alGetSourcei(soundId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public boolean isGlobal() {
        return alGetSourcei(soundId, AL_SOURCE_RELATIVE) == AL_TRUE;
    }

    @Override
    public void setListener(SourceListener listener) {
        this.listener = listener;
    }

    @Override
    public long play() {
        return play(1.0f);
    }

    @Override
    public long play(float volume) {
        setVolume(volume);
        alSourcePlay(soundId);
        if (listener != null) {
            listener.onPlay();
        }
        checkErrors("play");
        return sourceIndex;
    }

    @Override
    public long loop() {
        return loop(1.0f);
    }

    @Override
    public long loop(float volume) {
        return loop(volume, 0, -1);
    }

    @Override
    public int loop(float volume, int from, int to) {
        int start = 0;

        ByteBuffer data = loopInfo.data;
        int end = data.capacity();
        if (from >= 0 && from < data.capacity()) {
            start = from;
        }

        if (to < data.capacity()) {
            end = to;
        }

        if (start > 0) {
            alSourcei(soundId, AL_BUFFER, 0);
            alSourcei(soundId, AL_LOOPING, AL_FALSE);
            alSourceQueueBuffers(soundId, buffedId);
            loopInfo.start(start, end);
        } else {
            if (end > 0) {
                data.limit(end);
            }
            alSourcei(soundId, AL_LOOPING, AL_TRUE);
            alSourcei(soundId, AL_BUFFER, buffedId);
        }

        checkErrors("loop");
        play(volume);

        return sourceIndex;
    }

    @Override
    public void stop() {
        alSourceStop(soundId);
        alSourcei(soundId, AL_BUFFER, 0);
        audio.soundManager.freeSource(sourceIndex);
        checkErrors("stop");
    }

    @Override
    public void pause() {
        alSourcePause(soundId);
        if (listener != null) {
            listener.onPause();
        }
        checkErrors("pause");
    }

    @Override
    public void resume() {
        if (alGetSourcei(soundId, AL_SOURCE_STATE) == AL_PAUSED) {
            alSourcePlay(soundId);
        }
        checkErrors("resume");
    }

    @Override
    @Deprecated
    public void dispose() {
        alDeleteSources(soundId);
        alDeleteBuffers(buffedId);
        checkErrors("dispose");
    }

    @Override
    public void reset() {
        if (listener != null && isActive()) {
            listener.onStop();
        }

        this.priority = FREE_SOURCE_PRIORITY;
        this.listener = null;

        // set default values for AL sources
        alSourceStop(soundId);
        alSourceRewind(soundId);
        alSourcei(soundId, AL_BUFFER, 0);
        alSourcef(soundId, AL_GAIN, 0.0f);
        alSourcef(soundId, AL_PITCH, 1.0f);
        alSource3f(soundId, AL_POSITION, 0, 0, 0);
        alSourcei(soundId, AL_SOURCE_RELATIVE, AL_TRUE);
        alSourcei(soundId, AL_LOOPING, AL_FALSE);
        alSourcei(soundId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, SOFTDirectChannelsRemix.AL_REMIX_UNMATCHED_SOFT);

        loopInfo.clear();

        checkErrors("reset");
    }

    @Override
    public int getIndex() {
        return sourceIndex;
    }

    void checkErrors(String funcName) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            Console.out.println(String.format("ALSource::%s error: %d(0x%s)", funcName, error, Integer.toHexString(error)), OsdColor.RED);
        }
    }

    @Override
    public String toString() {
        return "ALSource{" +
                "listener=" + listener +
                ", soundId=" + soundId +
                ", buffedId=" + buffedId +
                ", sourceIndex=" + sourceIndex +
                ", priority=" + priority +
                ", loopInfo=" + loopInfo +
                ", musicSource=" + musicSource +
                '}';
    }
}
