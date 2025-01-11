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

package ru.m210projects.Build.osd;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConsoleLogger {

    private final OutputStream out;
    private final Path path;
    private boolean closed;

    public ConsoleLogger(Path path) throws IOException {
        this.path = path;
        this.out = new BufferedOutputStream(Files.newOutputStream(path));
        this.closed = false;
    }

    public void write(String message) {
        if(closed) {
            return;
        }

        try {
            out.write(String.format("%s\r\n", message).getBytes());
        } catch (IOException ignored) {
        }
    }

    @Override
    public String toString() {
        try {
            out.flush(); // #GDX 31.12.2024
        } catch (IOException ignored) {
        }

        try(InputStream is = Files.newInputStream(path)) {
            byte[] data = new byte[is.available()];
            int len = is.read(data);
            if (len > 0) {
                return new String(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Can't read the log file";
    }

    public void close() {
        if(closed) {
            return;
        }

        try {
            out.close();
            closed = true;
        } catch (Exception ignored) {
        }
    }

}
