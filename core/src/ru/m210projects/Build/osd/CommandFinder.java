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

package ru.m210projects.Build.osd;

import ru.m210projects.Build.osd.commands.OsdCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandFinder {

    private int osdTabCommandIndex;
    private List<String> osdTabCommandList;
    private final Map<String, OsdCommand> osdVars;

    public CommandFinder(Map<String, OsdCommand> osdVars) {
        this.osdVars = osdVars;
    }

    /**
     * Resets command list.
     * Should be called when any other key is pressed.
     */
    public void reset() {
        osdTabCommandList = null;
    }

    public String getNextTabCommand() {
        String msg = osdTabCommandList.get(osdTabCommandIndex++);
        if(osdTabCommandIndex >= osdTabCommandList.size()) {
            osdTabCommandIndex = 0;
        }
        return msg;
    }

    public boolean isListPresent() {
        return osdTabCommandList != null;
    }

    /**
     * @param inputText reference text to search command
     * Updates osdTabCommandList if command count more then 1
     */
    public List<String> getCommands(String inputText) {
        if(inputText.isEmpty()) {
            return new ArrayList<>(1);
        }

        List<String> commands;
        commands = osdVars.keySet().stream().filter(e -> e.startsWith(inputText)).sorted().collect(Collectors.toList());
        if(commands.size() > 1) {
            osdTabCommandList = commands;
            osdTabCommandIndex = 0;
        }
        return commands;
    }
}
