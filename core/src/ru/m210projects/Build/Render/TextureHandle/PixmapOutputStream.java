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

package ru.m210projects.Build.Render.TextureHandle;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class PixmapOutputStream extends OutputStream {

    private final Pixmap pixmap;
    private final ByteBuffer pixelsBuffer;
    private final Path filePath;

    public PixmapOutputStream(Pixmap pixmap, Path filePath) {
        this.pixmap = pixmap;
        this.pixelsBuffer = pixmap.getPixels();
        this.filePath = filePath;
    }

    @Override
    public void write(int b) throws IOException {
        pixelsBuffer.put((byte) b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        pixelsBuffer.put(b, off, len);
    }

    @Override
    public void close() throws IOException {
        PixmapIO.writePNG(new FileHandle(filePath.toFile()), pixmap);
        pixmap.dispose();
        super.close();
    }
}
