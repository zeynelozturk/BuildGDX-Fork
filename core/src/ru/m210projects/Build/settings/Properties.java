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

package ru.m210projects.Build.settings;

import ru.m210projects.Build.filehandle.FileUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static ru.m210projects.Build.Strhandler.toLowerCase;

public class Properties {

    private final Map<String, java.util.Properties> contextMap = new HashMap<>();
    private java.util.Properties currentContext = new java.util.Properties();

    public Properties(Reader r) throws IOException {
        try (BufferedReader reader = new BufferedReader(r)) {
            String lastContext = "";
            while (reader.ready()) {
                String line = reader.readLine();
                if (isContext(line)) {
                    String context = toLowerCase(line.replaceAll("[^a-zA-Z0-9_-]", ""));

                    contextMap.put(context, new java.util.Properties());
                    lastContext = context;
                    continue;
                }

                if (!lastContext.isEmpty() && !line.startsWith(";")) {
                    if (!line.contains("\\\\")) {
                        line = line.replace("\\", "\\\\");
                    }
                    contextMap.get(lastContext).load(new StringReader(line));
                }
            }
        }
    }

    public Path getPathValue(String key, Path defaultValue) {
        String valuePath = getStringValue(key, "");
        if (valuePath.isEmpty()) {
            return defaultValue;
        }
        return FileUtils.getPath(valuePath);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        String s = getStringValue(key, "");
        if (!s.isEmpty()) {
            if (s.equalsIgnoreCase("true")) {
                return true;
            } else if (s.equalsIgnoreCase("false")) {
                return false;
            } else {
                s = s.replaceAll("[^0-9-]", "");
                if (!s.isEmpty()) {
                    return Integer.parseInt(s) == 1;
                }
            }
        }
        return defaultValue;
    }

    public float getFloatValue(String key, float defaultValue) {
        String s = getStringValue(key, "");
        if (!s.isEmpty()) {
            s = s.replaceAll("[^0-9-.,]", "");
            if (!s.isEmpty()) {
                return Float.parseFloat(s);
            }
        }
        return defaultValue;
    }

    public int getIntValue(String key, int defaultValue) {
        String s = getStringValue(key, "");
        if (!s.isEmpty()) {
            s = s.replaceAll("[^0-9-]", "");
            if (!s.isEmpty()) {
                return Integer.parseInt(s);
            }
        }
        return defaultValue;
    }

    public String getStringValue(String key, String defaultValue) {
        String value = currentContext.getProperty(key);
        if (value != null) {
            return value.trim();
        }

        // Not matching with the actual key then
        for (Map.Entry<Object, Object> entry : currentContext.entrySet()) {
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                return ((String) entry.getValue()).trim();
            }
        }
        return defaultValue;
    }

    public String getStringValue(String key, int arrayIndex, String defaultValue) {
        String line = getStringValue(key, "");
        if (line.isEmpty()) {
            return defaultValue;
        }
        String[] values = line.split(",");
        if (values.length <= arrayIndex) {
            return defaultValue;
        }

        line = values[arrayIndex];
        line = line.replaceAll("\"", "");
        line = line.trim();
        return line;
    }

    public boolean setContext(String context) {
        context = toLowerCase(context);
        currentContext = contextMap.getOrDefault(context, new java.util.Properties());
        return !currentContext.isEmpty();
    }

    private boolean isContext(String line) {
        line = line.trim();
        return line.startsWith("[") && line.endsWith("]");
    }
}
