// This file is part of BuildGDX.
// Copyright (C) 2023  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

import com.badlogic.gdx.audio.Music;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.filehandle.Entry;

import java.nio.file.Path;

/**
 * BuildGdx midi device wrapper interface
 */
public interface MidiDevice {

    String MIDI_EXTENSION = "mid";

    /**
     * Default midi device if all other is unavailable
     */
    MidiDevice DUMMY = new MidiDevice() {
        @Override
        public String getName() {
            return "Dummy midi driver";
        }

        @Override
        public boolean open() {
            return true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public void setMasterVolume(float volume) {
        }

        @Override
        public @Nullable Music newMusic(Entry file) {
            return null;
        }
    };

    /**
     * @return midi device name
     */
    String getName();

    /**
     * Opens the device, indicating that it should now acquire any system resources it requires and become operational.
     * @return true if the device is open, otherwise false
     */
    boolean open();

    /**
     * Reports whether the device is open
     * @return true if the device is open, otherwise false
     */
    boolean isOpen();

    /**
     * Closes the device, indicating that the device should now release any system resources it is using.
     */
    void close();

    void setMasterVolume(float volume);

    @Nullable Music newMusic(Entry file);

    /**
     * SoundBank midi device
     */
    interface SoundBank extends MidiDevice {

        /**
         * @return Path to sound bank file
         */
        Path getPath();
    }

}
