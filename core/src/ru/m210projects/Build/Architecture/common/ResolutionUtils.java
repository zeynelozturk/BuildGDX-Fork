// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.Graphics;

import java.util.*;
import java.util.stream.Collectors;

public class ResolutionUtils {

    public static String getDisplayModeAsString(Graphics.DisplayMode mode) {
        return String.format("%sx%s", mode.width, mode.height);
    }

    public static String getDisplayModeAsString(int width, int height) {
        return String.format("%sx%s", width, height);
    }

    public static Map<String, List<Graphics.DisplayMode>> getDisplayModes(Graphics.DisplayMode[] modes) {
        return Arrays
                .stream(modes)
                .sorted(Comparator.comparingInt(a -> a.width))
                .collect(Collectors.groupingBy(e -> getDisplayModeAsString(e.width, e.height), LinkedHashMap::new, Collectors.toList()));
    }

    public static List<Graphics.DisplayMode> getBestDisplayModes(Map<String, List<Graphics.DisplayMode>> resolutions, int bpp) {
        return resolutions.keySet().stream().map(e -> {
            List<Graphics.DisplayMode> modes = resolutions.get(e);
            return modes.stream().sorted(Comparator.comparingInt(mode -> Math.abs(mode.bitsPerPixel - bpp))).max(Comparator.comparingInt(a -> a.refreshRate)).orElse(null);
        }).collect(Collectors.toList());
    }

    public static Graphics.DisplayMode findDisplayMode(Graphics.DisplayMode[] modes, String resolution) {
        return Arrays.stream(modes)
                .filter(e -> getDisplayModeAsString(e.width, e.height).equals(resolution))
                .max(Comparator.comparingInt(a -> a.refreshRate)).orElse(null);
    }

    public static Graphics.DisplayMode findDisplayMode(Graphics.DisplayMode[] modes, int width, int height) {
        return Arrays.stream(modes)
                .filter(e -> getDisplayModeAsString(e.width, e.height).equals(getDisplayModeAsString(width, height)))
                .max(Comparator.comparingInt(a -> a.refreshRate)).orElse(null);
    }

    public static Graphics.DisplayMode findDisplayMode(Map<String, List<Graphics.DisplayMode>> resolutions, int width, int height) {
        return getBestDisplayModes(resolutions, 32).stream()
                .filter(e -> getDisplayModeAsString(e.width, e.height).equals(getDisplayModeAsString(width, height)))
                .max(Comparator.comparingInt(a -> a.refreshRate)).orElse(null);
    }

    public static class UserDisplayMode extends Graphics.DisplayMode {

        public UserDisplayMode(int width, int height, int rate, int bpp) {
            super(width, height, rate, bpp);
        }

        public UserDisplayMode(int width, int height) {
            super(width, height, -1, -1);
        }

        public boolean isFullScreenSupport() {
            return bitsPerPixel != -1;
        }

        @Override
        public String toString() {
            return ResolutionUtils.getDisplayModeAsString(width, height);
        }
    }
}
