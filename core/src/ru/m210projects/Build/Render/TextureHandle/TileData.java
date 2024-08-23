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
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.filehandle.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class TileData {

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract ByteBuffer getPixels();

    public abstract int getGLType();

    public abstract int getGLInternalFormat();

    public abstract int getGLFormat();

    public abstract PixelFormat getPixelFormat();

    public abstract boolean hasAlpha();

    public abstract boolean isClamped();

    public abstract boolean isHighTile();

    public void dispose() {
        /* for implementing */
    }

    public void save(String name) {
        int width = getWidth();
        int height = getHeight();

        ByteBuffer pixels = getPixels();
        int bytes = getPixelFormat().getLength();
        if (bytes == 1) {
			Path path = FileUtils.getPath(name + ".raw");
            try (OutputStream os = Files.newOutputStream(path)) {
                while (pixels.position() < pixels.capacity()) {
                    os.write(pixels.get());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(path + " saved!");
            return;
        }

        name += ".png";
        Pixmap pixmap = new Pixmap(width, height,
                getPixelFormat() == PixelFormat.Rgba ? Format.RGBA8888 : Format.RGB888);
        float[] color = new float[4];
        if (bytes == 3) {
            color[3] = 1.0f;
        }

        for (int i = 0; i < (width * height); i++) {
            for (int c = 0; c < bytes; c++) {
                color[c] = (pixels.get((i * bytes) + c) & 0xFF) / 255.f;
            }

            pixmap.setColor(color[0], color[1], color[2], color[3]);
            int row = i / width;
            int col = i % width;
            pixmap.drawPixel(col, row);
        }

        FileHandle f = new FileHandle(name);
        PixmapIO.writePNG(f, pixmap);

        System.out.println(f.file().getAbsolutePath() + " saved!");
        pixmap.dispose();
    }

    protected int calcSize(int size) {
        int nsize = 1;
        if (GLInfo.texnpot == 0) {
            for (; nsize < size; nsize *= 2) {
                ;
            }
            return nsize;
        }
        return size == 0 ? 1 : size;
    }

    public enum PixelFormat {
        Rgb(3), Rgba(4), Pal8(1), Bitmap(1);

        private final int bytes;

        PixelFormat(int bytes) {
            this.bytes = bytes;
        }

        public int getLength() {
            return bytes;
        }
    }
}
