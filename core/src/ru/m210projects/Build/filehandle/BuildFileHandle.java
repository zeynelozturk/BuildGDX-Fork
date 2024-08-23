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

package ru.m210projects.Build.filehandle;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class BuildFileHandle extends FileHandle {

    private final Entry entry;

    public BuildFileHandle(File file, Entry entry) {
        this.entry = entry;
        this.file = file;
        this.type = Files.FileType.Absolute;
    }

    @Override
    public String name() {
        return entry.getName();
    }

    @Override
    public String extension() {
        return entry.getExtension();
    }

    @Override
    public InputStream read() {
        try {
            return entry.getInputStream();
        } catch (IOException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public ByteBuffer map(FileChannel.MapMode mode) {
        ByteBuffer bb = ByteBuffer.wrap(entry.getBytes());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public long length() {
        return entry.getSize();
    }
}
