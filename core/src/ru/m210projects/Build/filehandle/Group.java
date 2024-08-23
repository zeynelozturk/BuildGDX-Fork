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

package ru.m210projects.Build.filehandle;

import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Group extends Iterable<Entry> {
    List<Entry> getEntries();
    int getSize();
    String getName();
    Entry getEntry(String name);

    default boolean isEmpty() {
        return getSize() == 0;
    }

    default Entry getEntry(Path path) {
        return getEntry(path.toString());
    }

    default Iterator<Entry> iterator() {
        return getEntries().iterator();
    }

    default Spliterator<Entry> spliterator() {
        return getEntries().spliterator();
    }

    default Stream<Entry> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default void extract(Path directory) throws NotDirectoryException {
        if (!Files.isDirectory(directory)) {
            throw new NotDirectoryException(directory.toString());
        }

        for(Entry entry : getEntries()) {
            Path entryPath = directory.resolve(entry.getName());
            try (OutputStream os = Files.newOutputStream(entryPath)) {
                StreamUtils.writeBytes(os, entry.getBytes());
                Console.out.println("Extracted " + entryPath, OsdColor.GREEN);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
