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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.CharInfo;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.OsdCommandPrompt;

import static ru.m210projects.Build.Strhandler.isalpha;
import static ru.m210projects.Build.Strhandler.isdigit;

public class MenuTextField extends MenuItem implements InputListener {

    public static final int LETTERS = 1;
    public static final int NUMBERS = 2;
    public static final int SYMBOLS = 4;
    public static final int POINT = 8;

    private final OsdCommandPrompt prompt;
    protected String oldInput = "";

    public MenuTextField(Object text, String input, Font font, int x, int y, int width, final int charFlag, MenuProc confirmCallback) {
        super(text, font);

        this.flags = 3 | 4;
        this.m_pMenu = null;

        this.x = x;
        this.y = y;
        this.width = width;

        this.prompt = new MenuPrompt(32, 32) {
            @Override
            public boolean isCharacterAllowed(char ch) {
                return (isalpha(ch) && (charFlag & LETTERS) != 0)
                        || (isdigit(ch) && (charFlag & NUMBERS) != 0)
                        || (!isdigit(ch) && !isalpha(ch)
                        && ((charFlag & SYMBOLS) != 0 || (charFlag & POINT) != 0 && ch == '.'));
            }
        };
        prompt.setActionListener(i -> confirmCallback.run(menuHandler, MenuTextField.this));
        prompt.setTextInput(input);
    }

    public String getText() {
        return prompt.getTextInput();
    }

    public void setText(String text) {
        prompt.setTextInput(text);
    }

    @Override
    public void draw(MenuHandler handler) {
        if (text != null) {
            int pal = handler.getPal(font, this);
            int shade = handler.getShade(this);

            font.drawTextScaled(handler.getRenderer(), x, y, text, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
            int px = x + width - 1;
            if (prompt.isCaptured()) {
                shade = -128;
            }

            drawPrompt(handler.getRenderer(), px, y, shade, pal);
        }
        handler.mPostDraw(this);
    }

    protected void drawPrompt(Renderer renderer, int pos, int y, int shade, int pal) {
        final String input = prompt.getTextInput();
        final int cursorPos = prompt.getCursorPosition();
        int fptr = input.length() - 1;
        int curX = pos;

        for (int i = fptr; i >= 0; i--) {
            CharInfo charInfo = font.getCharInfo(input.charAt(i));
            pos -= charInfo.getCellSize();
            font.drawCharScaled(renderer, pos, y, input.charAt(i), 1.0f, shade, pal, Transparent.None, ConvertType.Normal, false);
            if (i == cursorPos) {
                curX = pos;
            }
        }

        if (prompt.isCaptured() && (System.currentTimeMillis() & 0x100) == 0) {
            char ch = '_';
            if (prompt.isOsdOverType()) {
                ch = '#';
            }
            font.drawCharScaled(renderer, curX, y, ch, 1.0f, shade, pal, Transparent.None, ConvertType.Normal, false);
        }
    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        if (prompt.isCaptured()) {
            return false;
        }

        switch (opt) {
            case ESC:
            case RMB:
                return true;
            case UP:
                m_pMenu.mNavUp();
                return false;
            case DW:
                m_pMenu.mNavDown();
                return false;
        }

        return false;
    }

    @Override
    public boolean mouseAction(int mx, int my) {
        if (text != null) {
            if (mx > x && mx < x + font.getWidth(text, 1.0f)) {
                if (my > y && my < y + font.getSize()) {
                    return true;
                }
            }

            if (mx > x + width - font.getWidth(prompt.getTextInput(), 1.0f) && mx < x + width - 1) {
                return my > y && my < y + font.getSize();
            }
        }

        return false;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
        onCancel();
    }

    @Override
    public boolean keyRepeat(int keycode) {
        if (prompt.isCaptured()) {
            return prompt.keyRepeat(keycode);
        }
        return false;
    }

    @Override
    public boolean keyDown(int i) {
        if (prompt.isCaptured()) {
            switch (i) {
                case Keys.ENTER:
                case Keys.BUTTON_A:
                    onConfirm();
                    return true;
                case Keys.ESCAPE:
                    onCancel();
                    return true;
                default:
                    return prompt.keyDown(i);
            }
        }

        if (i == Keys.ENTER) {
            onStartEdit();
        }

        return false;
    }

    @Override
    public boolean keyUp(int i) {
        prompt.keyUp(i);
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        if (prompt.isCaptured()) {
            prompt.keyTyped(c);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            if (!prompt.isCaptured()) {
                onStartEdit();
            }
        } else if (button == Input.Buttons.RIGHT) {
            onCancel();
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int mx, int my) {
		return prompt.isCaptured();
	}

    public void onCancel() {
        if (!prompt.isCaptured()) {
            return;
        }

        prompt.setTextInput(oldInput);
        prompt.setCaptureInput(false);
    }

    protected void onConfirm() {
        prompt.onEnter();
        prompt.setCaptureInput(false);
    }

    protected void onStartEdit() {
        oldInput = prompt.getTextInput();
        prompt.setCaptureInput(true);
    }

}
