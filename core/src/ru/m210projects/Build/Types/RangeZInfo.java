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

package ru.m210projects.Build.Types;

public class RangeZInfo {

    private int ceilz = Integer.MIN_VALUE;
    private int ceilhit = -1;
    private int florz = Integer.MAX_VALUE;
    private int florhit = -1;

    public void init() {
        this.ceilz = Integer.MIN_VALUE;
        this.ceilhit = -1;
        this.florz = Integer.MAX_VALUE;
        this.florhit = -1;
    }

    public int getCeilz() {
        return ceilz;
    }

    public void setCeilz(int ceilz) {
        this.ceilz = ceilz;
    }

    public int getCeilhit() {
        return ceilhit;
    }

    public void setCeilhit(int ceilhit) {
        this.ceilhit = ceilhit;
    }

    public int getFlorz() {
        return florz;
    }

    public void setFlorz(int florz) {
        this.florz = florz;
    }

    public int getFlorhit() {
        return florhit;
    }

    public void setFlorhit(int florhit) {
        this.florhit = florhit;
    }
}
