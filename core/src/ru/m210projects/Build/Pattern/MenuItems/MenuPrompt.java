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

package ru.m210projects.Build.Pattern.MenuItems;

import ru.m210projects.Build.osd.OsdCommandPrompt;

public class MenuPrompt extends OsdCommandPrompt {
    public MenuPrompt(int editLength, int historyDepth) {
        super(editLength, historyDepth);
    }

    @Override
    public void historyNext() {
        if (!inputHistory.hasNext()) {
            return;
        }
        super.historyNext();
    }

    @Override
    public void onEnter() {
        if (!isEmpty()) {
            String input = getTextInput();
            inputHistory.add(input);
            actionListener.onEnter(input);
        }
    }

    @Override
    public void setCaptureInput(boolean capture) {
        super.setCaptureInput(capture);
        if(capture) {
            inputHistory.prev(); // trig history
        }
    }
}
