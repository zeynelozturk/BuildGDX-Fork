//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.MenuItems;

import com.badlogic.gdx.Graphics;
import ru.m210projects.Build.Architecture.common.ResolutionUtils;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

import java.util.List;
import java.util.stream.IntStream;

import static ru.m210projects.Build.Gameutils.coordsConvertXScaled;
import static ru.m210projects.Build.Gameutils.coordsConvertYScaled;

public class MenuResolutionList extends MenuList {

    private final int nBackground;
    public int transparent = 1;
    public int backgroundPal;
    protected BuildGame game;
    protected List<Graphics.DisplayMode> displayModes;

    public MenuResolutionList(BuildGame game, List<Graphics.DisplayMode> displayModes, Font font, int x, int y,
                              int width, int align,
                              MenuProc specialCall, int rowCount, int nBackground) {
        super(null, font, x, y, width, align, specialCall, rowCount);
        this.nBackground = nBackground;
        this.displayModes = displayModes;
        this.game = game;
        this.len = displayModes.size();
    }

    @Override
    public void open() {
        l_nMin = 0;

        Renderer ren = game.getRenderer();

        l_nFocus = IntStream.range(0, displayModes.size()).filter(i -> {
            Graphics.DisplayMode mode = displayModes.get(i);
            return ResolutionUtils.getDisplayModeAsString(mode).equalsIgnoreCase(ResolutionUtils.getDisplayModeAsString(ren.getWidth(), ren.getHeight()));
        }).boxed().findAny().orElse(-1);

        if (l_nFocus >= l_nMin + rowCount) {
            l_nMin = l_nFocus - rowCount + 1;
        }
    }

    public Graphics.DisplayMode getSelectedMode() {
        if (l_nFocus == -1) {
            Renderer ren = game.getRenderer();

            return new ResolutionUtils.UserDisplayMode(ren.getWidth(), ren.getHeight());
        }
        return displayModes.get(l_nFocus);
    }

    public List<Graphics.DisplayMode> getDisplayModes() {
        return displayModes;
    }

    public void setDisplayModes(List<Graphics.DisplayMode> displayModes) {
        this.displayModes = displayModes;
    }

    @Override
    public void draw(MenuHandler handler) {
        handler.game.getRenderer().rotatesprite((x - 10) << 16, (y - 8) << 16, 65536, 0, nBackground, 128, backgroundPal, 10 | 16 | transparent, 0, 0, coordsConvertXScaled(x + width, ConvertType.Normal), coordsConvertYScaled(y + rowCount * mFontOffset() + 3));

        if (!displayModes.isEmpty()) {
            int px = x, py = y;
            for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < displayModes.size(); i++) {
                int pal = handler.getPal(font, null);
                int shade = handler.getShade(null);
                Graphics.DisplayMode displayMode = displayModes.get(i);
                String modeName = displayMode.toString();

                if (i == l_nFocus) {
                    shade = handler.getShade(this);
                    pal = handler.getPal(font, this);
                }
                if (align == 1) {
                    px = width / 2 + x - font.getWidth(modeName, 1.0f) / 2;
                }
                if (align == 2) {
                    px = x + width - 1 - font.getWidth(modeName, 1.0f);
                }
                font.drawTextScaled(handler.getRenderer(), px, py, modeName, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);

                py += mFontOffset();
            }
        } else {
            int pal = handler.getPal(font, this);
            String text = "List is empty";

            int px = x, py = y;
            if (align == 1) {
                px = width / 2 + x - font.getWidth(text, 1.0f) / 2;
            }
            if (align == 2) {
                px = x + width - 1 - font.getWidth(text, 1.0f);
            }
            int shade = handler.getShade(this);

            font.drawTextScaled(handler.getRenderer(), px, py, text, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
        }

        handler.mPostDraw(this);
    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        switch (opt) {
            case MWUP:
                ListMouseWheelUp(handler);
                return false;
            case MWDW:
                ListMouseWheelDown(handler, displayModes.size());
                return false;
            case UP:
                ListUp(handler, displayModes.size());
                return false;
            case DW:
                ListDown(handler, displayModes.size());
                return false;
            case LEFT:
                ListLeft(handler);
                return false;
            case RIGHT:
                ListRight(handler);
                return false;
            case ENTER:
            case LMB:
                if ((flags & 4) == 0) {
                    return false;
                }

                if (l_nFocus != -1 && !displayModes.isEmpty() && callback != null) {
                    callback.run(handler, this);
                }
                return false;
            case ESC:
            case RMB:
                ListEscape(handler, opt);
                return true;
            case PGUP:
                ListPGUp(handler);
                return false;
            case PGDW:
                ListPGDown(handler, displayModes.size());
                return false;
            case HOME:
                ListHome(handler);
                return false;
            case END:
                ListEnd(handler, displayModes.size());
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean mouseAction(int mx, int my) {
        if (!displayModes.isEmpty()) {
            int px = x, py = y;
            for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < displayModes.size(); i++) {
                int fontx = font.getWidth(displayModes.get(i).toString(), 1.0f);
                if (align == 1) {
                    px = width / 2 + x - fontx / 2;
                }
                if (align == 2) {
                    px = x + width - 1 - fontx;
                }

                if (mx > px && mx < px + fontx) {
                    if (my > py && my < py + font.getSize()) {
                        l_nFocus = i;
                        return true;
                    }
                }

                py += mFontOffset();
            }
        }
        return false;
    }
}
