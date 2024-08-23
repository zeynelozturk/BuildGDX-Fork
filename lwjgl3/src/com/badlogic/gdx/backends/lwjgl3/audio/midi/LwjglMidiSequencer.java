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

import javax.sound.midi.*;

public class LwjglMidiSequencer {

    private final Sequencer sequencer;
    private Transmitter transmitter;
    private LwjglMidiReceiver receiver;
    private LwjglMidiMusicSource currentSource;
    private float masterVolume = 0.0f;

    public LwjglMidiSequencer(LwjglMidiReceiver receiver) throws MidiUnavailableException {
        this.sequencer = MidiSystem.getSequencer(false);
        sequencer.open();

        sequencer.addControllerEventListener(message -> {
            if(isLooping()) {
                if((message.getData1() == 116 || message.getData1() == 118) && message.getData2() == 0) {
                    if(message.getChannel() == 0 && sequencer.getLoopStartPoint() == 0) {
                        sequencer.setLoopStartPoint(sequencer.getTickPosition());
                    }
                }

                if((message.getData1() == 117 || message.getData1() == 119) && message.getData2() == 127) {
                    if(message.getChannel() == 0) {
                        sequencer.setTickPosition(sequencer.getLoopStartPoint());
                    }
                }
            }
        }, null);

        setReceiver(receiver);
    }

    protected boolean isLooping() {
        return sequencer.getLoopCount() != 0;
    }

    protected void setReceiver(LwjglMidiReceiver receiver) throws MidiUnavailableException {
        if (transmitter != null) {
            transmitter.close();
        }

        if (this.receiver != null) {
            this.receiver.close();
        }

        transmitter = sequencer.getTransmitter();
        transmitter.setReceiver(receiver);
        this.receiver = receiver;
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = volume;
        if (currentSource != null) {
            notifyVolumeChanged();
        }
    }

    public void notifyVolumeChanged() {
        receiver.setMasterVolume(currentSource.getVolume() * masterVolume);
    }

    public void play(LwjglMidiMusicSource source, long position) {
        checkSource(source);
        receiver.setMasterVolume(source.getVolume() * masterVolume);
        sequencer.setMicrosecondPosition(position);
        sequencer.start();
    }

    public void play(LwjglMidiMusicSource source, long position, long start, long end) {
        checkSource(source);
        receiver.setMasterVolume(source.getVolume() * masterVolume);
        sequencer.setMicrosecondPosition(position);
        sequencer.setLoopStartPoint(start);
        sequencer.setLoopEndPoint(end);
        sequencer.start();
    }

    public void stop() {
        if (isOpen()) {
            sequencer.stop();
        }
    }

    public long getPosition() {
        return sequencer.getMicrosecondPosition();
    }

    public void setLooping(boolean looping) {
        if (looping) {
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        } else {
            sequencer.setLoopCount(0);
        }
    }

    public boolean isPlaying(LwjglMidiMusicSource source) {
        return sequencer.getSequence() == source.data && sequencer.isRunning();
    }

    private void checkSource(LwjglMidiMusicSource source) {
        if (sequencer.getSequence() != source.data) {
            try {
                if (currentSource != null) {
                    currentSource.pause();
                }
                sequencer.setSequence(source.data);
                this.currentSource = source;
            } catch (InvalidMidiDataException ignored) {
            }
        }
    }

    public boolean isOpen() {
        return sequencer.isOpen();
    }

    public void close() {
        if (sequencer != null) {
            stop();
            sequencer.close();
        }

        if (transmitter != null) {
            transmitter.close();
        }

        if (receiver != null) {
            receiver.close();
        }
    }
}
