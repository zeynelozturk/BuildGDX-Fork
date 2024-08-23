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

package ru.m210projects.Build.Types;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.osd.Console;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.pow;
import static ru.m210projects.Build.Engine.MAXPALOOKUPS;

public class DefaultPaletteLoader implements PaletteManager.PaletteLoader {

    private final byte[] basePalette;
    private final int shadesCount;
    private final byte[][] palookup;
    private final byte[] transluc;

    public DefaultPaletteLoader(Entry entry) throws IOException {
        if (!entry.exists()) {
            throw new FileNotFoundException("Failed to load \"palette.dat\"!");
        }

        try (InputStream is = entry.getInputStream()) {
            Console.out.println("Loading palettes");
            this.basePalette = StreamUtils.readBytes(is, 768);
            Console.out.println("Loading palookup tables");
            this.shadesCount = StreamUtils.readShort(is);
            this.palookup = new byte[MAXPALOOKUPS][];
            this.palookup[0] = StreamUtils.readBytes(is, shadesCount << 8);
            Console.out.println("Loading translucency table");
            this.transluc = StreamUtils.readBytes(is, 65536);
        }
    }

    @Override
    public byte[] getBasePalette() {
        return basePalette;
    }

    @Override
    public int getShadeCount() {
        return shadesCount;
    }

    @Override
    public byte[][] getPalookup() {
        return palookup;
    }

    @Override
    public byte[] getTransluc() {
        return transluc;
    }

}
