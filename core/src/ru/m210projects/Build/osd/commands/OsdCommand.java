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

package ru.m210projects.Build.osd.commands;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.osd.CommandResponse;
import ru.m210projects.Build.osd.Console;

public abstract class OsdCommand {

    private final String name;
    private final String description;
    protected Console parent;

    public OsdCommand(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }

    public OsdCommand(@NotNull String name) {
        this.name = name;
        this.description = "";
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract CommandResponse execute(String[] argv);

    public void setParent(Console parent) {
        this.parent = parent;
    }
}
