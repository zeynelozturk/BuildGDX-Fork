//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package com.badlogic.gdx.backends.lwjgl3.audio.midi;

import ru.m210projects.Build.Architecture.common.audio.RangedMusic;
import ru.m210projects.Build.filehandle.Entry;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class LwjglMidiMusicSource implements RangedMusic {

    private final LwjglMidiSequencer sequencer;
    protected Sequence data;
    private boolean looping;
    private long position;
    private OnCompletionListener listener;
    private float volume = 1.0f;
    private long loopStart, loopEnd;

    public LwjglMidiMusicSource(LwjglMidiSequencer sequencer, Entry entry) throws InvalidMidiDataException, IOException {
        this.listener = null;
        this.sequencer = sequencer;
        this.data = MidiSystem.getSequence(new ByteArrayInputStream(entry.getBytes()));
        this.position = 0;
        this.loopStart = -1;
        this.loopEnd = -1;
    }

    @Override
    public void play() {
        setLooping(looping);
        if (loopStart != -1 && loopEnd != -1) {
            sequencer.play(this, position, loopStart, loopEnd);
        } else {
            sequencer.play(this, position);
        }
    }

    @Override
    public void pause() {
        position = sequencer.getPosition();
        sequencer.stop();
    }

    @Override
    public void stop() {
        position = 0;
        sequencer.stop();
        if (listener != null) {
            listener.onCompletion(this);
        }
    }

    @Override
    public boolean isPlaying() {
        return sequencer.isPlaying(this);
    }

    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
        this.sequencer.setLooping(looping);
    }

    @Override
    public void setLoopRange(long start, long end) {
        this.loopStart = start;
        this.loopEnd = end;
    }

    @Override
    public boolean isLooping() {
        return looping;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        this.sequencer.notifyVolumeChanged();
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public void setPan(float pan, float volume) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPosition(float position) {
        this.position = (long) position;
    }

    @Override
    public float getPosition() {
        return position;
    }

    @Override
    public void dispose() {
        stop();
        data = null;
        listener = null;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        this.listener = listener;
    }
}
