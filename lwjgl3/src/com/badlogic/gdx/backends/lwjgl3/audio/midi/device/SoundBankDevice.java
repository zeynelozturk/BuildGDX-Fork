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

import javax.sound.midi.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class SoundBankDevice implements MidiDevice.SoundBank {

    private final Soundbank soundbank;
    private final Synthesizer synthesizer;

    private final Path path;
    private LwjglMidiSequencer sequencer;

    public SoundBankDevice(Path path) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        this.path = path;
        this.synthesizer = MidiSystem.getSynthesizer();
        this.soundbank = MidiSystem.getSoundbank(path.toFile());
    }

    @Override
    public String getName() {
        return soundbank.getName().trim();
    }

    @Override
    public boolean open() {
        Console.out.println("Initialization " + soundbank.getName() + "...");
        try {
            synthesizer.open();
            sequencer = new LwjglMidiSequencer(new LwjglMidiReceiver(synthesizer.getReceiver()));
            synthesizer.loadAllInstruments(soundbank);
            return true;
        } catch (Exception e) {
            Console.out.println(e.toString(), OsdColor.RED);
        }
        return false;
    }

    @Override
    public boolean isOpen() {
        return synthesizer.isOpen();
    }

    @Override
    public void close() {
        if (!isOpen()) {
            return;
        }
        sequencer.close();
        synthesizer.close();
        sequencer = null;
    }

    @Override
    public void setMasterVolume(float volume) {
        sequencer.setMasterVolume(volume);
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
    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SoundBankDevice that = (SoundBankDevice) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
