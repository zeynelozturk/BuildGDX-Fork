/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.lwjgl3.audio;

import com.badlogic.gdx.utils.StreamUtils;
import ru.m210projects.Build.filehandle.Entry;

import java.io.ByteArrayInputStream;

public class ALOgg {
    static public class Music extends ALMusic {
        private OggInputStream input;
        private OggInputStream previousInput;

        public Music (OpenALAudio audio, Entry entry) {
            super(audio, entry);
            input = new OggInputStream(new ByteArrayInputStream(entry.getBytes()));
            setup(input.getChannels(), input.getSampleRate());
        }

        public int read (byte[] buffer) {
            if (input == null) {
                input = new OggInputStream(new ByteArrayInputStream(entry.getBytes()), previousInput);
                setup(input.getChannels(), input.getSampleRate());
                previousInput = null; // release this reference
            }
            return input.read(buffer);
        }

        public void reset () {
            StreamUtils.closeQuietly(input);
            previousInput = null;
            input = null;
        }

        @Override
        protected void loop () {
            StreamUtils.closeQuietly(input);
            previousInput = input;
            input = null;
        }
    }
}
