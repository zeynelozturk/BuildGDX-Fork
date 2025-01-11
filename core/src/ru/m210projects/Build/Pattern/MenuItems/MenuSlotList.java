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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Pattern.Tools.SaveManager.SaveInfo;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.input.keymap.Keymap;

import java.util.ArrayList;
import java.util.List;

import static ru.m210projects.Build.Gameutils.coordsConvertXScaled;
import static ru.m210projects.Build.Gameutils.coordsConvertYScaled;
import static ru.m210projects.Build.Strhandler.toCharArray;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public abstract class MenuSlotList extends MenuList implements InputListener {
    protected final boolean saveList;
    public boolean deleteQuestion;
    public List<SaveInfo> text;
    public MenuProc updateCallback;
    public MenuProc confirmCallback;
    public List<SaveInfo> displayed;
    public int nListOffset;

    public String typed;
    private final MenuPrompt prompt;

    public int yHelpText;
    public int helpPal;
    public Font questionFont;
    public Font desriptionFont;
    public int specPal, backgroundPal;
    public int transparent = 1;

    //to draw own cursor in mPostDraw (MenuHandler)
    public boolean owncursor = false;

    protected SaveManager saveManager;
    protected Renderer draw;
    protected int nBackground;
    protected int listPal;

    public MenuSlotList(Renderer draw, SaveManager saveManager, Font font, int x, int y, int yHelpText, int width,
                        int nListItems, MenuProc updateCallback, MenuProc confirmCallback, int listPal, int specPal, int nBackground, boolean saveList) {

        super(null, font, x, y, width, 0, null, nListItems);
        this.draw = draw;
        this.prompt = new MenuPrompt(16, 1);
        this.saveManager = saveManager;
        this.nBackground = nBackground;

        this.text = saveManager.getList();
        this.rowCount = nListItems;
        this.nListOffset = 0;

        this.updateCallback = updateCallback;
        this.confirmCallback = confirmCallback;
        this.saveList = saveList;
        this.displayed = new ArrayList<>();
        this.yHelpText = yHelpText;
        this.questionFont = font;
        this.helpPal = listPal;
        this.specPal = specPal;
        this.listPal = listPal;

        this.desriptionFont = font;
    }

    public boolean isTyping() {
        return prompt.isCaptured();
    }

    public FileEntry getFileEntry() {
        int ptr = l_nFocus;
        if (saveList) {
            ptr--;
        }
        if (ptr == -1 || displayed.isEmpty()) {
            return DUMMY_ENTRY;
        }
        return displayed.get(ptr).entry;
    }

    public String SaveName() {
        int ptr = l_nFocus;
        if (saveList) {
            ptr--;
        }
        if (ptr == -1 || displayed.isEmpty()) {
            return "Empty slot";
        }
        return displayed.get(ptr).name;
    }

    @Override
    public void draw(MenuHandler handler) {
        len = getListSize();

        handler.game.getRenderer().rotatesprite((x + width / 2 - 5) << 16, (y - 3) << 16, 65536, 0, nBackground, 128, backgroundPal, 10 | 16 | transparent, 0, 0, coordsConvertXScaled(x + width, ConvertType.Normal), coordsConvertYScaled(y + rowCount * mFontOffset() + 3));

        if (!displayed.isEmpty()) {
            int py = y, pal;

            for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < len; i++) {
                int ptr = i;
                if (saveList) {
                    ptr -= 1;
                }

                int shade = handler.getShade(i == l_nFocus && !deleteQuestion ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
                char[] rtext;
                if (i == 0 && saveList) {
                    rtext = toCharArray("New savegame");
                } else {
                    rtext = toCharArray(displayed.get(ptr).name);
                }
                pal = listPal;

                if (ptr >= 0) {
                    SaveInfo info = displayed.get(ptr);
                    if (info.entry == null || !info.entry.exists()) {
                        continue;
                    }

                    if (info.entry.getName().equalsIgnoreCase("autosave.sav") || info.entry.getName().startsWith("quicksav")) {
                        pal = specPal;
                    }
                }

                if (prompt.isCaptured() && i == l_nFocus && m_pMenu.mGetFocusedItem(this)) {
                    drawPrompt(handler.getRenderer(), x + width / 2 + nListOffset, py, pal);
                } else {
                    font.drawTextScaled(handler.getRenderer(), x + width / 2 + nListOffset, py, rtext, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
                }

                py += mFontOffset();
            }
        } else {
            int py = y;
            int shade = handler.getShade(l_nFocus != -1 ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);

            if (saveList) {
                if (prompt.isCaptured()) {
                    drawPrompt(handler.getRenderer(), x + width / 2 + nListOffset, py, listPal);
                } else {
                    font.drawTextScaled(handler.getRenderer(), x + width / 2 + nListOffset, py, "New saved game", 1.0f, shade, listPal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
                }
            } else {
                font.drawTextScaled(handler.getRenderer(), x + width / 2 + nListOffset, py, "List is empty", 1.0f, shade, listPal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
            }
        }

        pal = helpPal;
        if (deleteQuestion) {
            int xdim = draw.getWidth();
            int ydim = draw.getHeight();
            int tile = nBackground;
            ArtEntry pic = draw.getTile(tile);

            float kt = xdim / (float) ydim;
            float kv = pic.getWidth() / (float) pic.getHeight();
            float scale;
            if (kv >= kt) {
                scale = (ydim + 1) / (float) pic.getHeight();
            } else {
                scale = (xdim + 1) / (float) pic.getWidth();
            }

            handler.game.getRenderer().rotatesprite(0, 0, (int) (scale * 65536), 0, tile, 127, 4, 8 | 16 | transparent);

            int shade = handler.getShade(m_pMenu.m_pItems[m_pMenu.m_nFocus]);

            char[] ctext = toCharArray("Do you want to delete \"" + SaveName() + "\"");
            questionFont.drawTextScaled(handler.getRenderer(), 160 - questionFont.getWidth(ctext, 1.0f) / 2, 100, ctext, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
            ctext = toCharArray("[Y/N]");
            questionFont.drawTextScaled(handler.getRenderer(), 160 - questionFont.getWidth(ctext, 1.0f) / 2, 110, ctext, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
        } else {
            char[] ctext = toCharArray("Press \"DELETE\" to remove the savegame file");

            desriptionFont.drawTextScaled(handler.getRenderer(), 160 - desriptionFont.getWidth(ctext, 1.0f) / 2, yHelpText, ctext, 1.0f, 0, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
        }

        handler.mPostDraw(this);
    }

    protected void drawPrompt(Renderer renderer, int x, int y, int pal) {
        final String input = prompt.getTextInput();
        final int cursorPos = prompt.getCursorPosition();
        int curX = x;

        for (int i = 0; i < input.length(); i++) {
            x += font.drawCharScaled(renderer, x, y, input.charAt(i), 1.0f, -128, pal, Transparent.None, ConvertType.Normal, false);
            if (i == cursorPos - 1) {
                curX = x;
            }
        }

        if (owncursor) {
            drawOwnCursor(curX, y + 1);
            return;
        }

        if (prompt.isCaptured() && (System.currentTimeMillis() & 0x100) == 0) {
            char ch = '_';
            if (prompt.isOsdOverType()) {
                ch = '#';
            }
            font.drawCharScaled(renderer, curX, y + 1, ch, 1.0f, -128, pal, Transparent.None, ConvertType.Normal, false);
        }
    }

    protected void drawOwnCursor(int x, int y) {

    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        switch (opt) {
            case ESC:
            case RMB:
                return !prompt.isCaptured() && !deleteQuestion;
        }
        return false;
    }

    @Override
    public void open() {
        l_nMin = l_nFocus = 0;

        text.removeIf(s -> !checkFile(s.entry));

        updateList();

        if (updateCallback != null) {
            updateCallback.run(menuHandler, this);
        }
    }

    protected int getListSize() {
        int len = displayed.size();
        if (saveList && !displayed.isEmpty()) {
            len += 1;
        }
        return len;
    }

    public abstract boolean checkFile(FileEntry entry);

    public void updateList() {
        displayed.clear();
        displayed.addAll(text);
        if (saveList) {
            displayed.removeIf(s -> s.name.equals("autosave.sav")
                    || s.name.startsWith("quicksav"));
        }
    }

    @Override
    public void close() {
        deleteQuestion = false;
        onCancel();
    }

    @Override
    public boolean mouseMoved(int mx, int my) {
        return prompt.isCaptured() || deleteQuestion;
    }

    @Override
    public boolean mouseAction(int mx, int my) {
        if (deleteQuestion || prompt.isCaptured()) {
            return false;
        }

        if (!displayed.isEmpty()) {
            int px = x, py = y;
            int len = getListSize();

            int ol_nFocus = l_nFocus;
            for (int i = l_nMin; i >= 0 && i < l_nMin + rowCount && i < len; i++) {

                if (mx > px && mx < px + width - 14) {
                    if (my > py && my < py + font.getSize()) {
                        l_nFocus = i;
                        if (ol_nFocus != i && updateCallback != null) {
                            updateCallback.run(menuHandler, this);
                        }
                        return true;
                    }
                }

                py += mFontOffset();
            }
        }
        return false;
    }

    @Override
    public boolean scrolled (float amountX, float amount) {
        if (prompt.isCaptured() || deleteQuestion) {
            return true;
        }

        if (amount < 0) {
            ListMouseWheelUp(menuHandler);
            return true;
        } else if (amount > 0) {
            ListMouseWheelDown(menuHandler, getListSize());
            return true;
        }
        return false;
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
            }
            return prompt.keyDown(i);
        }

        switch (i) {
            case Keys.Y:
                if (deleteQuestion) {
                    onEnter();
                    return true;
                }
                break;
            case Keys.N:
                if (deleteQuestion) {
                    onCancel();
                    return true;
                }
                break;
            case Keys.FORWARD_DEL:
                if ((!saveList && (!displayed.isEmpty() && l_nFocus != -1)) || saveList && l_nFocus != 0) {
                    deleteQuestion = true;
                }
                return true;
            case Keys.UP:
            case Keymap.BUTTON_UP:
                ListUp(menuHandler, getListSize());
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.DOWN:
            case Keymap.BUTTON_DOWN:
                ListDown(menuHandler, getListSize());
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.LEFT:
            case Keymap.BUTTON_LEFT:
                ListLeft(menuHandler);
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.RIGHT:
            case Keymap.BUTTON_RIGHT:
                ListRight(menuHandler);
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.ENTER:
            case Keys.BUTTON_A:
                onEnter();
                return true;
            case Keys.ESCAPE:
                onCancel();
                return true;
            case Keys.PAGE_UP:
                ListPGUp(menuHandler);
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.PAGE_DOWN:
                ListPGDown(menuHandler, getListSize());
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.HOME:
                ListHome(menuHandler);
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
            case Keys.END:
                ListEnd(menuHandler, getListSize());
                if (updateCallback != null) {
                    updateCallback.run(menuHandler, this);
                }
                return true;
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
            onEnter();
            return true;
        } else if (button == Input.Buttons.RIGHT) {
            return onCancel();
        }
        return false;
    }

    protected void onEnter() {
        if (deleteQuestion) {
            saveManager.delete(getFileEntry());
            updateList();
            if (l_nFocus >= displayed.size()) {
                int len = getListSize();
                l_nFocus = len - 1;
                l_nMin = len - rowCount;
                if (l_nMin < 0) {
                    l_nMin = 0;
                }
            }
            if (updateCallback != null) {
                updateCallback.run(menuHandler, this);
            }
            deleteQuestion = false;
            return;
        }

        if (saveList) {
            if (!prompt.isCaptured()) {
                if (l_nFocus > 0 && l_nFocus < displayed.size()) { //#GDX 11.10.2024 Crash fix when delete all save files and trying to save new one
                    prompt.setTextInput(displayed.get(l_nFocus - 1).name);
                }
                prompt.setCaptureInput(true);
            }
        } else {
            onConfirm();
        }
    }

    protected void onConfirm() {
        this.typed = prompt.getTextInput();
        prompt.clear();
        prompt.setCaptureInput(false);
        if (l_nFocus != -1 /*&& getListSize() > 0*/) { // cant save in empty list
            if (confirmCallback != null) {
                confirmCallback.run(menuHandler, this);
                updateList();
            }
        }
    }

    protected boolean onCancel() {
        if (prompt.isCaptured()) {
            prompt.clear();
            prompt.setCaptureInput(false);
            return true;
        } else if (deleteQuestion) {
            deleteQuestion = false;
            return true;
        } else {
            ListEscape(menuHandler, MenuOpt.ESC); // FIXME MenuOpt.ESC
        }
        return false;
    }
}