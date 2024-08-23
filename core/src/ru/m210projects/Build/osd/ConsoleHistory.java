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

import java.util.ArrayList;
import java.util.List;

public class ConsoleHistory {
    private final List<String> history;
    private final int depth;
    private int pos;

    public ConsoleHistory(int depth) {
        this.depth = depth;
        this.history = new ArrayList<>(depth);
    }

    public boolean add(String text) {
        pos = -1;
        if (!history.isEmpty() && text.equals(history.get(0))) {
            return false;
        }

        if (history.size() >= depth) {
            history.remove(depth - 1);
        }
        history.add(0, text);
        return true;
    }

    public boolean hasPrev() {
        return pos < history.size() - 1;
    }

    public String prev() {
        if (!hasPrev()) {
            if (history.isEmpty()) {
                return "";
            }
            return history.get(pos);
        }

        pos++;
        return history.get(pos);
    }

    public boolean hasNext() {
        return pos > 0;
    }

    public String next() {
        if (!hasNext()) {
            pos = -1;
            return "";
        }

        pos--;
        return history.get(pos);
    }
}
