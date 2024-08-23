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

package ru.m210projects.Build.filehandle.fs;

import java.io.IOException;
import java.nio.file.Path;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;

public class AbsoluteFileEntry extends FileEntry {

    public AbsoluteFileEntry(Path path) throws IOException {
        super(path);
        this.physicalDirectory = DUMMY_DIRECTORY;
    }
}
