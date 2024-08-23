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

public enum AudioDriver {

    DUMMY_AUDIO("Dummy audio"),
    OPENAL_AUDIO("OpenAL Audio");

    final String name;

    AudioDriver(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AudioDriver parseType(String name) {
        switch (name) {
            case "Dummy audio":
                return DUMMY_AUDIO;
            case "OpenAL Audio":
                return OPENAL_AUDIO;
        }
        return AudioDriver.DUMMY_AUDIO;
    }

}
