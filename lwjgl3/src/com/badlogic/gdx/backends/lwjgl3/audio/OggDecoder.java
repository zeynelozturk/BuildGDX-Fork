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

import com.badlogic.gdx.utils.StreamUtils;
import ru.m210projects.Build.Architecture.common.audio.SoundData;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OggDecoder implements SoundData.Decoder {

    @Override
    public SoundData decode(Entry entry) {
        byte[] data = entry.getBytes();
        if(data.length > 2 && data[0] == 0x4F && data[1] == 0x67 && data[2] == 0x67) { // OGG
            OggInputStream input = null;
            try {
                input = new OggInputStream(new ByteArrayInputStream(data, 0, data.length));
                ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
                byte[] buffer = new byte[2048];
                while (!input.atEnd()) {
                    int length = input.read(buffer);
                    if (length == -1) break;
                    output.write(buffer, 0, length);
                }
                ByteBuffer pcmData = ByteBuffer.allocateDirect(output.size()).order(ByteOrder.LITTLE_ENDIAN);
                pcmData.put(output.toByteArray());
                pcmData.rewind();
                return new SoundData(input.getSampleRate(), input.getChannels(), 16, pcmData);
            }
            catch (Exception e) {
                Console.out.print("OGG loader error: " + e, OsdColor.RED);
            }
            finally {
                StreamUtils.closeQuietly(input);
            }
        }
        return null;
    }
}
