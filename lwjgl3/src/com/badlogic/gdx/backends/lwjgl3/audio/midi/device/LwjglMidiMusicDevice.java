//This file is part of BuildGDX.
//Copyright (C) 2023  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package com.badlogic.gdx.backends.lwjgl3.audio.midi.device;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.LwjglMidiMusicSource;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.LwjglMidiReceiver;
import com.badlogic.gdx.backends.lwjgl3.audio.midi.LwjglMidiSequencer;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.Architecture.common.audio.MidiDevice;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.util.Objects;

public class LwjglMidiMusicDevice implements MidiDevice {

    protected final javax.sound.midi.MidiDevice device;
    private LwjglMidiSequencer sequencer;

    public LwjglMidiMusicDevice(javax.sound.midi.MidiDevice device) {
        this.device = device;
    }

    @Override
    public String getName() {
        return device.getDeviceInfo().getName();
    }

    @Override
    public boolean open() {
        Console.out.println("Initialization " + getName() + "...");
        try {
            device.open();
            sequencer = new LwjglMidiSequencer(new LwjglMidiReceiver(device.getReceiver()));
            return true;
        } catch (Exception e) {
            Console.out.println(e.toString(), OsdColor.RED);
        }
        return false;
    }

    @Override
    public boolean isOpen() {
        // #GDX 31.12.2024 Added sequencer != null
        return device.isOpen() && sequencer != null;
    }

    @Override
    public void close() {
        // #GDX 31.12.2024 Added sequencer != null
        if (sequencer != null) {
            sequencer.close();
            sequencer = null;
        }
        device.close();
    }

    @Override
    public void setMasterVolume(float volume) {
        sequencer.setMasterVolume(volume);// FIXME:  MIDI IN receiver not available
    }

    @Override
    public @Nullable Music newMusic(Entry file) {
        if (isOpen()) { // #GDX 11.10.2024 Don't create new MIDI music if device isn't opened
            try {
                return new LwjglMidiMusicSource(sequencer, file);
            } catch (IOException | InvalidMidiDataException e) {
                Console.out.println(e.toString(), OsdColor.RED);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LwjglMidiMusicDevice that = (LwjglMidiMusicDevice) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
