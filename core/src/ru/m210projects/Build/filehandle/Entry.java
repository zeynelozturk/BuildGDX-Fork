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

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.CRC32;
import ru.m210projects.Build.Pattern.Tools.NaturalComparator;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public interface Entry extends Comparable<Entry> {

    InputStream getInputStream() throws IOException;

    /**
     * @return full file name with extension
     */
    String getName();

    /**
     * @return file extension in upper case
     */
    String getExtension();

    long getSize();

    boolean exists();

    Group getParent();

    void setParent(Group parent);

    default long getChecksum() {
        return CRC32.getChecksum(this);
    }

    default boolean isExtension(String fmt) {
        return getExtension().equalsIgnoreCase(fmt);
    }

    default boolean isDirectory() {
        return false;
    }

    default byte[] getBytes() {
        final byte[] data = new byte[(int) getSize()];
        if (!load(is -> StreamUtils.readBytes(is, data))) {
            return new byte[0];
        }
        return data;
    }

    default void save(Path path) {
        try (InputStream inputStream = getInputStream();
             OutputStream outputStream = Files.newOutputStream(path)) {
            int len;
            byte[] data = new byte[1024];
            while ((len = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, len);
            }
        } catch (Exception e) {
            Console.out.println(String.format("Failed to load entry %s: %s", getName(), e), OsdColor.RED);
        }
    }

    default boolean load(EntryLoader loader) {
        if (exists() && getSize() > 0) {
//            long start = System.currentTimeMillis();
            try (InputStream is = getInputStream()) {
                loader.loadEntry(is);
                return true;
            } catch (Exception e) {
                Console.out.println(String.format("Failed to load entry %s: %s", getName(), e), OsdColor.RED);
            }
//            finally {
//                Console.out.println(String.format("file = %s read for %dms", this, System.currentTimeMillis() - start), OsdColor.BLUE);
//            }
        }
        return false;
    }

    @Override
    default int compareTo(@NotNull Entry f) {
        String s1 = this.getName().toLowerCase(Locale.ROOT);
        String s2 = f.getName().toLowerCase(Locale.ROOT);

        return NaturalComparator.compare(s1, s2);
    }
}
