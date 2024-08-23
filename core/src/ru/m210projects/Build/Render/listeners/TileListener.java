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

package ru.m210projects.Build.Render.listeners;

/**
 * Uses by tile manager and OpenGL renderers
 */
public interface TileListener {

    TileListener DUMMY_LISTENER = new TileListener() {
        @Override
        public void onInvalidate(int tileNum) {
        }
    };

    /**
     * Calls if byte array of tile is changed
     * @param tileNum number of art entry tile
     */
    void onInvalidate(int tileNum);
}
