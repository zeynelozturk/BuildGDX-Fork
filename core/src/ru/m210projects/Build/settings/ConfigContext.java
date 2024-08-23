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

package ru.m210projects.Build.settings;

import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface ConfigContext {

    ConfigContext DUMMY_CONTEXT = new ConfigContext() {
        @Override
        public void load(Properties properties) {
        }

        @Override
        public void save(OutputStream os) {
        }
    };

    void load(Properties properties);

    void save(OutputStream os) throws IOException;

    default void putBoolean(OutputStream os, String name, boolean var) throws IOException {
        String line = name + " = " + (var ? "true" : "false") + "\r\n";
        StreamUtils.writeString(os, line);
    }

    default void putFloat(OutputStream os, String name, float var) throws IOException {
        String line = name + " = " + var + "\r\n";
        StreamUtils.writeString(os, line);
    }

    default void putInteger(OutputStream os, String name, int var) throws IOException {
        String line = name + " = " + var + "\r\n";
        StreamUtils.writeString(os, line);
    }

    default void putString(OutputStream os, String text) throws IOException {
        StreamUtils.writeString(os, text);
    }

    default void putPath(OutputStream os, String name, Path path) throws IOException {
        String line = name + " = " + path + "\r\n";
        StreamUtils.writeString(os, line);
    }

    default void putString(OutputStream os, String name, String var) throws IOException {
        String line = name + " = " + var + "\r\n";
        StreamUtils.writeString(os, line);
    }

}
