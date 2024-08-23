//This file is part of BuildGDX.
//Copyright (C) 2023-2024  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.filehandle;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.filehandle.fs.AbsoluteFileEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.filehandle.grp.GrpFile;
import ru.m210projects.Build.filehandle.rff.RffFile;
import ru.m210projects.Build.filehandle.zip.ZipEntry;
import ru.m210projects.Build.filehandle.zip.ZipFile;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.m210projects.Build.filehandle.CacheResourceMap.CachePriority.*;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class Cache {

    protected final CacheResourceMap cacheResourceMap = new CacheResourceMap();

    public Cache(Directory gameDirectory) {
        CacheHolder.HOLDER_INSTANCE = this;
        addGroup(gameDirectory, HIGH);
    }

    public static Cache getInstance() {
        return CacheHolder.HOLDER_INSTANCE;
    }

    public boolean addGroup(Group group, CacheResourceMap.CachePriority priority) {
        if (Objects.nonNull(group) && group.getSize() > 0) {
            cacheResourceMap.addGroup(group, priority);
            return true;
        }
        return false;
    }

    public boolean isGameDirectory(Group group) {
        if (!(group instanceof Directory.Game) || group.equals(DUMMY_DIRECTORY)) {
            return false;
        }

        return group.equals(getGameDirectory());
    }

    public void removeGroup(Group group) {
        cacheResourceMap.removeGroup(group);
    }

    @NotNull
    public Directory getGameDirectory() {
        Group group = cacheResourceMap.getGroup(0);
        if (group instanceof Directory.Game) {
            return (Directory) group;
        }
        return DUMMY_DIRECTORY;
    }

    public CacheResourceMap.CachePriority getPriority(Group group) {
        return cacheResourceMap.getPriority(group);
    }

    public boolean addGroup(Entry groupEntry, CacheResourceMap.CachePriority priority) {
        Group group = newGroup(groupEntry);
        return addGroup(group, priority);
    }

    @NotNull
    public Entry getEntry(Path path, boolean searchFirst) {
        if (path.isAbsolute() && path.startsWith(getGameDirectory().getPath())) {
            // #GDX 22.07.2024 Relative absolute paths
            path = getGameDirectory().getPath().relativize(path);
        }

        Entry entry = DUMMY_ENTRY;
        int i = searchFirst ? HIGHEST.getLevel() : NORMAL.getLevel();
        for (; i >= NORMAL.getLevel(); i--) {
            for (ListNode<Group> node = cacheResourceMap.getFirst(i); node != null; node = node.getNext()) {
                Group group = node.get();
                entry = group.getEntry(path);
                if (entry.exists()) {
                    return entry;
                }
            }
        }

        // #GDX 22.07.2024 Trying to find absolute path file
        if (!entry.exists()) {
            try {
                FileEntry fileEntry = new AbsoluteFileEntry(path);
                if (fileEntry.exists() && !fileEntry.isDirectory()) {
                    entry = fileEntry;
                }
            } catch (IOException ignored) {
            }
        }

        return entry;
    }

    @NotNull
    public Entry getEntry(String name, boolean searchFirst) {
        return getEntry(FileUtils.getPath(name), searchFirst);
    }

    @NotNull
    public Group newGroup(Entry entry) {
        Group group = DUMMY_DIRECTORY;

        GroupType type = getGroupType(entry);
        try {
            switch (type) {
                case DIRECTORY:
                    group = getGameDirectory().getDirectory((FileEntry) entry);
                    break;
                case GRP:
                    group = new GrpFile(entry.getName(), entry::getInputStream);
                    break;
                case RFF:
                    group = new RffFile(entry.getName(), entry::getInputStream);
                    break;
                case ZIP:
                    group = new ZipFile(entry.getName(), entry::getInputStream);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return group;
    }

    @NotNull
    public Group getGroup(String groupName) {
        Group group = cacheResourceMap.getGroup(groupName);
        if (group.getSize() > 0) {
            return group;
        }
        return DUMMY_DIRECTORY;
    }

    @NotNull
    public GroupType getGroupType(Entry entry) {
        if (entry instanceof FileEntry && entry.isDirectory()) {
            return GroupType.DIRECTORY;
        }

        if (entry.getSize() < 4) {
            return GroupType.NONE;
        }

        try (InputStream is = entry.getInputStream()) {
            switch (StreamUtils.readByte(is)) {
                case 'K':
                    String value = StreamUtils.readString(is, 11);
                    if (value.equals("enSilverman")) {
                        return GroupType.GRP;
                    }
                    break;
                case 'R':
                    if (StreamUtils.readByte(is) == 'F' && StreamUtils.readByte(is) == 'F' && StreamUtils.readByte(is) == 0x1A) {
                        return GroupType.RFF;
                    }
                    break;
                case 'P':
                    if (StreamUtils.readByte(is) == 'K' && StreamUtils.readByte(is) == 0x03 && StreamUtils.readByte(is) == 0x04) {
                        return GroupType.ZIP;
                    }
                    break;
            }
        } catch (IOException ignored) {
        }
        return GroupType.NONE;
    }

    public List<Group> getGroups() {
        List<Group> list = new ArrayList<>();
        for (int i = NORMAL.getLevel(); i <= HIGHEST.getLevel(); i++) {
            for (ListNode<Group> node = cacheResourceMap.getFirst(i); node != null; node = node.getNext()) {
                list.add(node.get());
            }
        }
        return list;
    }

    public void loadGdxDef(DefScript baseDef, String appdef, String resname) {
        URL url = Cache.class.getClassLoader().getResource(resname);
        if (url != null) {
            try (InputStream input = url.openConnection().getInputStream()) {
                byte[] data = new byte[1024];
                int length;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                while ((length = input.read(data)) != -1) {
                    buffer.write(data, 0, length);
                }

                if (buffer.size() > 0) {
                    ZipFile zipFile = new ZipFile(resname, () -> new ByteArrayInputStream(buffer.toByteArray()));
                    if (addGroup(zipFile, NORMAL)) {
                        Entry def = zipFile.getEntry(appdef);
                        if (def.exists() && baseDef.loadScript(resname, def)) {
                            for (Entry entry : zipFile.getEntries()) {
                                if (entry instanceof ZipEntry && !entry.isDirectory()) {
                                    ((ZipEntry) entry).load();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Console.out.println("Can't load " + resname + ". " + e, OsdColor.RED);
                e.printStackTrace();
            }
        }
    }

    public enum GroupType {
        NONE, DIRECTORY, RFF, GRP, ZIP
    }

    private static class CacheHolder {
        public static Cache HOLDER_INSTANCE;
    }
}
