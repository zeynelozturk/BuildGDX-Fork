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

package ru.m210projects.Build.Types;

import java.util.Map.Entry;
import java.util.Properties;

public class PropertyIgnoreCase extends Properties {
    public String getPropertyIgnoreCase(String key) {
        return getPropertyIgnoreCase(key, null);
    }

    public String getPropertyIgnoreCase(String key, String defaultV) {
        String value = getProperty(key);
        if (value != null) {
            return value.trim();
        }

        // Not matching with the actual key then
        for (Entry<Object, Object> entry : entrySet()) {
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                return ((String) entry.getValue()).trim();
            }
        }
        return defaultV;
    }
}
