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

import java.text.NumberFormat;

public abstract class OsdValue extends OsdCommand {

    private final ValueChecker checker;

    public OsdValue(@NotNull String name, @NotNull String description) {
        this(name, description, null);
    }

    public OsdValue(@NotNull String name, @NotNull String description, ValueChecker checker) {
        super(name, description);
        if(checker == null) {
            checker = value -> true;
        }
        this.checker = checker;
    }

    public abstract float getValue();

    protected abstract void setCheckedValue(float value);

    public boolean setValue(float value) {
        if(checker.checkValue(value)) {
            setCheckedValue(value);
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        return String.format("\"%s\" is \"%s\"\n%s\n", getName(), nf.format(getValue()).replace(',', '.'), super.getDescription());
    }

    public CommandResponse execute(String[] argv) {
        if(argv.length != 1) {
            return CommandResponse.DESCRIPTION_RESPONSE;
        }

        try {
            if(setValue(Float.parseFloat(argv[0]))) {
                return CommandResponse.OK_RESPONSE;
            }
            return CommandResponse.OUT_OF_RANGE;
        } catch (NumberFormatException ignore) {
        }
        return CommandResponse.BAD_ARGUMENT_RESPONSE;
    }
}
