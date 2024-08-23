// This file is part of BuildGDX.
// Copyright (C) 2017-2020  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.filehandle.Entry;

import java.nio.ByteBuffer;

public class SoundData {

    public static final Decoder DUMMY_DECODER = new DummyDecoder();

    private final int rate;
    private final int channels;
    private final int bits;
    private final ByteBuffer data;

    public SoundData(int rate, int channels, int bits, ByteBuffer data) {
        this.rate = rate;
        this.channels = channels;
        this.bits = bits;
        this.data = data;
    }

    public int getRate() {
        return rate;
    }

    public int getChannels() {
        return channels;
    }

    public int getBits() {
        return bits;
    }

    public ByteBuffer getData() {
        return data;
    }

    public interface Decoder {
        @Nullable
        SoundData decode(Entry entry);
    }

    public static final class DummyDecoder implements Decoder {
        @Override
        public SoundData decode(Entry entry) {
            return null;
        }
    }
}
