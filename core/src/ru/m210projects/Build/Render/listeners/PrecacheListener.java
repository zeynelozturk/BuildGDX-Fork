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
 * Uses by precache adapter and OpenGL renderers
 */
public interface PrecacheListener {

    PrecacheListener DUMMY_PRECACHE_LISTENER = new PrecacheListener() {

        @Override
        public void loadModels() {
        }

        @Override
        public void onPrecacheTile(int tileNum, boolean ifSprite) {
        }
    };

    /**
     * Calls in precache screen to load all models
     */
    void loadModels();

    /**
     * @param tileNum number of art entry tile
     * @param ifSprite load tiles for sprites and models only (exclude wall, floor, ceiling tiles)
     * basically this just means walls are repeating, while sprites are clamped
     */
    void onPrecacheTile(int tileNum, boolean ifSprite);

    /**
     * Calls when precaching is done (uses by RenderGDX to build world mesh)
     */
    default void onPrecacheReady() {
    }
}
