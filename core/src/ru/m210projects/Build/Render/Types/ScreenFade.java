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

package ru.m210projects.Build.Render.Types;

public interface ScreenFade {

    default int getRed() {
        return getIntensive();
    }

    default int getGreen() {
        return getIntensive();
    }

    default int getBlue() {
        return getIntensive();
    }

    /**
     * @return intensive level to detect, should we show fade effect or not
     */
    int getIntensive();

    /**
     * @param intensive sets intensive level of fade effect
     * @return this object
     */
    ScreenFade setIntensive(int intensive);

    /**
     * @return fade name
     */
    String getName();
}
