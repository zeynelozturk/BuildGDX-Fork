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

import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Arrays;

/**
 * Java Receiver, that scans midi note control volume and change
 * it to master volume coefficient
 */
public class LwjglMidiReceiver implements Receiver {

    /**
     * Midi node control volume index
     */
    private static final int CONTROL_VOLUME = 7;

    /**
     * Maximum channels count in midi file
     */
    private static final int MAX_CHANNELS = 16;

    /**
     * Maximum volume level for midi node
     */
    private static final byte MAX_VOLUME = 127;

    /**
     * Java midi receiver
     */
    private final Receiver receiver;
    /**
     * Current channel volume level. Changes while midi playing when the control volume sets it
     */
    private final byte[] chVolume;

    /**
     * Master volume coefficient
     */
    private float masterVolume;

    public LwjglMidiReceiver(Receiver receiver) {
        this.receiver = receiver;
        chVolume = new byte[MAX_CHANNELS];
        Arrays.fill(chVolume, MAX_VOLUME);
    }

    @Override
    public void close() {
        receiver.close();
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.min(Math.max(volume, 0.0f), 1.0f);
        for (int i = 0; i < MAX_CHANNELS; i++) {
            ShortMessage message = new ShortMessage();
            try {
                message.setMessage(ShortMessage.CONTROL_CHANGE | i, CONTROL_VOLUME, (byte) (chVolume[i] * volume));
                receiver.send(message, -1);
            } catch (InvalidMidiDataException ignored) {
            }
        }
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        ShortMessage volumeMessage;
        if ((volumeMessage = getVolumeMessage(message)) != null) {
            byte[] data = volumeMessage.getMessage();
            chVolume[volumeMessage.getChannel()] = data[2];
            try {
                volumeMessage.setMessage(data[0], data[1], (byte) (data[2] * masterVolume));
            } catch (InvalidMidiDataException e) {
                Console.out.println(e.toString(), OsdColor.RED);
            }
            receiver.send(volumeMessage, timeStamp);
        } else {
            receiver.send(message, timeStamp);
        }
    }

    private ShortMessage getVolumeMessage(MidiMessage message) {
        if ((message.getStatus() & 0xF0) == ShortMessage.CONTROL_CHANGE) {
            ShortMessage m = (ShortMessage) message;
            if (m.getData1() == CONTROL_VOLUME) {
                return m;
            }
        }

        return null;
    }
}