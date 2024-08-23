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

package ru.m210projects.Build.Pattern.Tools;

import ru.m210projects.Build.filehandle.fs.FileEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class SaveManager {

    private final List<SaveInfo> saveInfoList = new ArrayList<>();
    private final HashMap<String, SaveInfo> SavHash = new HashMap<>();
    public static final int Screenshot = MAXTILES - 1;

    public static class SaveInfo implements Comparable<SaveInfo> {
        public String name;
        public long time;
        public FileEntry entry;

        public SaveInfo(String name, long time, FileEntry entry) {
            update(name, time, entry);
        }

        public void update(String name, long time, FileEntry entry) {
            this.name = name;
            this.time = time;
            this.entry = entry;
        }

        @Override
        public int compareTo(SaveInfo obj) {
            if (obj.time == this.time) {
                return 0;
            }

            return (obj.time < this.time) ? -1 : 1;
        }
    }

    public SaveInfo getSlot(int num) {
        return saveInfoList.get(num);
    }

    public List<SaveInfo> getList() {
        return saveInfoList;
    }

    public void add(String savname, long time, FileEntry entry) {
        String filename = entry.getName();
        SaveInfo info;
        if ((info = SavHash.get(filename)) == null) {
            info = new SaveInfo(savname, time, entry);
            saveInfoList.add(0, info);
            SavHash.put(filename, info);
        } else {
            saveInfoList.remove(info);
            info.update(savname, time, entry);
            saveInfoList.add(0, info);
        }
    }

    public void delete(FileEntry entry) {
        SaveInfo info;
        if ((info = SavHash.get(entry.getName())) != null) {
            if (entry.exists() && entry.delete()) {
                saveInfoList.remove(info);
                entry.getParent().revalidate();
            }
        }
    }

    public FileEntry getLast() {
        if (!saveInfoList.isEmpty()) {
            return saveInfoList.get(0).entry;
        }
        return DUMMY_ENTRY;
    }

    public void sort() {
        Collections.sort(saveInfoList);
    }
}
