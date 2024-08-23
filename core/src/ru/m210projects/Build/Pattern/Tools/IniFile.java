//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Pattern.Tools;

import ru.m210projects.Build.Types.PropertyIgnoreCase;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.m210projects.Build.Strhandler.toLowerCase;

public class IniFile {

    protected Map<String, Integer> context;
    protected List<Integer> pointer;
    protected List<Integer> length;
    protected int nContext = -1;
    protected PropertyIgnoreCase ini;
    protected StringReader reader;
    protected String name;
    protected byte[] data;

    protected IniFile() { /* extends */ }

    public IniFile(byte[] data, String name) {
        this.name = toLowerCase(name);

        init(data);
    }

    public String getName() {
        return name;
    }

    public String getKeyString(String key) {
        String out = ini.getPropertyIgnoreCase(key, "");
        return out.trim();
    }

    public String getKeyString(String key, int num) {
        String line = ini.getPropertyIgnoreCase(key, "");
        if (line.isEmpty()) {
            return "";
        }
        String[] values = line.split(",");
        if (values.length < num) {
            return "";
        }

        line = values[num];
        line = line.replaceAll("\"", "");
        line = line.trim();
        return line;
    }

    public int getKeyInt(String key, int num) {
        String line = ini.getPropertyIgnoreCase(key);
        if (line == null) {
            return 0;
        }
        line = line.trim();

        int startpos = 0, pos = 0, keynum = 0;
        while (pos < line.length()) {
            if (line.charAt(pos) == ',') {
                if (keynum == num) {
                    break;
                } else {
                    keynum++;
                }
                startpos = pos + 1;
            }
            pos++;
        }
        line = line.substring(startpos, pos);
        line = line.replaceAll("[^0-9]", "");
        if (!line.isEmpty()) {
            return Integer.parseInt(line);
        } else {
            return 0;
        }
    }

    public int getKeyInt(String key) {
        String s;
        if ((s = ini.getPropertyIgnoreCase(key)) != null && !s.isEmpty()) {
            s = s.replaceAll("[^0-9-]", "");
            if (!s.isEmpty()) {
                return Integer.parseInt(s);
            } else {
                return 0;
            }
        }

        return -1;
    }

    protected boolean isContext(String line) {
        line = line.trim();
        return line.startsWith("[") && line.endsWith("]");
    }

    protected void init(byte[] data) {
        this.data = data;

        if (data == null) {
            return;
        }

        ini = new PropertyIgnoreCase();
        context = new HashMap<>();
        pointer = new ArrayList<>();
        length = new ArrayList<>();

        StringBuilder line;
        char c;
        int ptr = 0;
        while (ptr < data.length) {
            line = new StringBuilder();
            while (ptr < data.length && (c = (char) data[ptr++]) != '\n') {
                line.append(c);
            }

            if (isContext(line.toString())) {
                pointer.add(ptr - line.length() - 1);
                line = new StringBuilder(toLowerCase(line.toString().replaceAll("[^a-zA-Z0-9_-]", "")));
                context.put(line.toString(), pointer.size() - 1);
            }
        }

        if (!pointer.isEmpty()) {
            for (int i = 0; i < pointer.size() - 1; i++) {
                if (i + 1 < pointer.size()) {
                    length.add(pointer.get(i + 1) - pointer.get(i));
                }
            }
            length.add(data.length - pointer.get(pointer.size() - 1));
        }
    }

    public boolean set(String context) {
        nContext = this.context.getOrDefault(toLowerCase(context), -1);
        if (nContext == -1) {
            return false;
        }

        String l = new String(data, pointer.get(nContext), length.get(nContext));

        if (!l.contains("\\\\")) {
            l = l.replace("\\", "\\\\");
        }

        if (reader != null) {
            reader.close();
        }
        reader = new StringReader(l);

        try {
            ini.clear();
            ini.load(reader);
        } catch (Exception e) {
            e.printStackTrace();
            nContext = -1;
            return false;
        }

        return true;
    }

    public void close() {
        if (ini != null) {
            ini.clear();
        }
    }
}
