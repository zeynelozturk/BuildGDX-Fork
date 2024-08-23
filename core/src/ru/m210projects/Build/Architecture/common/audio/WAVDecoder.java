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

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WAVDecoder implements SoundData.Decoder {

    @Override
    public SoundData decode(Entry entry) {
        if (entry.getSize() <= 44) {
            Console.out.print("WAV loader error: file size is too small", OsdColor.RED);
        }

        try (InputStream is = entry.getInputStream()) {
            String riffId = StreamUtils.readString(is, 4);
            if (!riffId.equals("RIFF")) {
                throw new Exception("RIFF header not found");
            }

            StreamUtils.skip(is, 4); // chunk size
            String format = StreamUtils.readString(is, 4);
            if (!format.equals("WAVE")) {
                throw new Exception("Invalid wave file header");
            }

            String fmtId = StreamUtils.readString(is, 4);
            if (!fmtId.equals("fmt ")) {
                throw new Exception("Invalid wave file header");
            }

            int pcmId = StreamUtils.readInt(is);
            if (pcmId != 16) {
                throw new Exception("WAV files must be PCM: " + pcmId);
            }

            StreamUtils.skip(is, 2); // audio format
            int channels = StreamUtils.readShort(is);
            if (channels != 1 && channels != 2) {
                throw new Exception("WAV files must have 1 or 2 channels: " + channels);
            }

            int rate = StreamUtils.readInt(is);
            StreamUtils.skip(is, 4); // byte rate
            StreamUtils.skip(is, 2); // block align
            int bits = StreamUtils.readShort(is);
            StreamUtils.skip(is, 8); // subchunk2

            int length;
            byte[] data = new byte[4096];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
            while ((length = is.read(data)) != -1) {
                buffer.write(data, 0, length);
            }

            ByteBuffer pcmData = ByteBuffer.allocateDirect(buffer.size()).order(ByteOrder.LITTLE_ENDIAN);
            pcmData.put(buffer.toByteArray());
            pcmData.rewind();

            return new SoundData(rate, channels, bits, pcmData);
        } catch (Exception e) {
            Console.out.print("WAV loader error: " + e, OsdColor.RED);
        }

        return null;
    }
}
