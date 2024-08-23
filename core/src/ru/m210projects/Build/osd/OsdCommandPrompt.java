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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.commands.OsdValueRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;

public class OsdCommandPrompt implements InputListener {

    protected final ConsoleHistory inputHistory;
    private final StringBuilder osdEditBuf;
    protected String osdVersionText;
    protected int osdVersionShade;
    protected OsdColor osdVersionPal = OsdColor.DEFAULT;
    protected OsdColor osdPromptPal = OsdColor.DEFAULT;
    protected int osdPromptShade;
    protected OsdColor osdEditPal = OsdColor.DEFAULT;
    protected int osdEditShade;
    protected int osdEditCursor = 0; // position of cursor in edit buffer
    private boolean osdCaptureInput = false;
    private boolean osdShift = false;
    private boolean osdCtrl = false;
    private boolean osdCapsLock = false;
    private boolean osdOverType = false;
    protected ActionListener actionListener;

    public OsdCommandPrompt(int editLength, int historyDepth) {
        this.inputHistory = new ConsoleHistory(historyDepth);
        this.osdEditBuf = new StringBuilder(editLength);
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    protected void registerDefaultCommands(Console parent) {
        parent.registerCommand(new OsdValueRange("osdpromptshade", "osdpromptshade: sets the shade of the OSD prompt", 0, 7) {
            @Override
            public float getValue() {
                return osdPromptShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdPromptShade = (int) value;
            }
        });
        parent.registerCommand(new OsdValueRange("osdpromptpal", "osdpromptpal: sets the palette of the OSD prompt", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdPromptPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdPromptPal = OsdColor.findColor((int) value);
            }
        });
        parent.registerCommand(new OsdValueRange("osdeditshade", "osdeditshade: sets the shade of the OSD input text", 0, 7) {
            @Override
            public float getValue() {
                return osdEditShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdEditShade = (int) value;
            }
        });
        parent.registerCommand(new OsdValueRange("osdeditpal", "osdeditpal: sets the palette of the OSD input text", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdEditPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdEditPal = OsdColor.findColor((int) value);
            }
        });
    }

    public void setCaptureInput(boolean capture) {
        this.osdCaptureInput = capture;
    }

    public void onFirstPosition() {
        osdEditCursor = 0;
    }

    public void onLastPosition() {
        osdEditCursor = osdEditBuf.length();
    }

    public void setVersion(String version, OsdColor pal, int shade) {
        this.osdVersionText = version + " (BuildGdx: " + Engine.version + ")";
        this.osdVersionShade = shade;
        this.osdVersionPal = pal;
    }

    public void onLeft() {
        if (osdEditCursor > 0) {
            if (isCtrlPressed()) {
                while (osdEditCursor > 0) {
                    if (!Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor - 1))) {
                        break;
                    }
                    osdEditCursor--;
                }
                while (osdEditCursor > 0) {
                    if (Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor - 1))) {
                        break;
                    }
                    osdEditCursor--;
                }
            } else {
                osdEditCursor--;
            }
        }
    }

    public void onRight() {
        if (osdEditCursor < osdEditBuf.length()) {
            if (isCtrlPressed()) {
                while (osdEditCursor < osdEditBuf.length()) {
                    if (Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor))) {
                        break;
                    }
                    osdEditCursor++;
                }
                while (osdEditCursor < osdEditBuf.length()) {
                    if (!Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor))) {
                        break;
                    }
                    osdEditCursor++;
                }
            } else osdEditCursor++;
        }
    }

    public void onDelete() {
        if (osdEditCursor == 0 || osdEditBuf.length() == 0) {
            return;
        }

        if (osdEditCursor <= osdEditBuf.length()) {
            osdEditBuf.deleteCharAt(--osdEditCursor);
        }
    }

    public void handleInput() {
        try {
            while (System.in.available() != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                handleInput(reader.readLine());
            }
        } catch (IOException ignore) {
        }
    }

    public void handleInput(String text) {
        osdEditBuf.append(text);
        onEnter();
    }

    public void onEnter() {
        if (!isEmpty()) {
            String input = getTextInput();
            inputHistory.add(input);
            actionListener.onEnter(input);
        }
        clear();
    }

    public void historyPrev() {
        if (inputHistory.hasPrev()) {
            applyHistory(inputHistory.prev());
        }
    }

    public void historyNext() {
        String history = inputHistory.next();
        if (history.isEmpty()) {
            osdEditBuf.setLength(0);
            osdEditCursor = 0;
            return;
        }

        applyHistory(history);
    }

    private void applyHistory(String history) {
        osdEditBuf.setLength(0);
        osdEditBuf.append(history);
        osdEditCursor = osdEditBuf.length();
    }

    public void append(char ch) {
        if (osdEditBuf.length() < osdEditBuf.capacity()) {
            if (osdEditCursor < osdEditBuf.length()) {
                if (osdOverType) {
                    osdEditBuf.deleteCharAt(osdEditCursor);
                }
                osdEditBuf.insert(osdEditCursor, ch);
            } else {
                osdEditBuf.append(ch);
            }
            osdEditCursor++;
        }
    }

    public String getTextInput() {
        return osdEditBuf.toString();
    }

    public void setTextInput(String text) {
        clear();
        osdEditBuf.append(text);
        onLastPosition();
    }

    public int getCursorPosition() {
        return osdEditCursor;
    }

    public boolean isEmpty() {
        return osdEditBuf.length() == 0;
    }

    public void clear() {
        osdEditBuf.setLength(0);
        osdEditCursor = 0;
    }

    /**
     * Toggles when insert button is pressed
     */
    public void toggleOverType() {
        osdOverType = !osdOverType;
    }

    public boolean isOsdOverType() {
        return osdOverType;
    }

    public void onResize() {
    }

    public boolean isCaptured() {
        return osdCaptureInput;
    }

    public boolean isCapsLockPressed() {
        return osdCapsLock;
    }

    public void setCapsLockPressed(boolean osdCapsLock) {
        this.osdCapsLock = osdCapsLock;
    }

    public boolean isShiftPressed() {
        return osdShift;
    }

    public void setShiftPressed(boolean osdShift) {
        this.osdShift = osdShift;
    }

    public boolean isCtrlPressed() {
        return osdCtrl;
    }

    public void setCtrlPressed(boolean osdCtrl) {
        this.osdCtrl = osdCtrl;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!isCaptured()) {
            return false;
        }

        switch (keycode) {
            case Input.Keys.V:
                if (isCtrlPressed()) {
					if(Gdx.app.getClipboard() != null) {
						String content = Gdx.app.getClipboard().getContents();
						for(int i = 0; i < content.length(); i++) {
							keyTyped(content.charAt(i));
						}
					}
                    return true;
                }
                break;
            case Input.Keys.ENTER:
                onEnter();
                return true;
            case Input.Keys.DEL: //backspace
                if (isCtrlPressed()) {
                    clear();
                } else {
                    onDelete();
                }
                return true;
            case Input.Keys.CAPS_LOCK:
                setCapsLockPressed(!isCapsLockPressed());
                return true;
            case Input.Keys.DOWN:
                historyNext();
                return true;
            case Input.Keys.UP:
                historyPrev();
                return true;
            case Input.Keys.RIGHT:
                onRight();
                return true;
            case Input.Keys.LEFT:
                onLeft();
                return true;
            case Input.Keys.INSERT:
                toggleOverType();
                return true;
            case Input.Keys.END:
                if (!isCtrlPressed()) {
                    onLastPosition();
                    return true;
                }
                break;
            case Input.Keys.HOME:
                if (!isCtrlPressed()) {
                    onFirstPosition();
                    return true;
                }
                break;
            case Input.Keys.SHIFT_LEFT:
            case Input.Keys.SHIFT_RIGHT:
                setShiftPressed(true);
                return true;
            case Input.Keys.CONTROL_LEFT:
            case Input.Keys.CONTROL_RIGHT:
                setCtrlPressed(true);
                return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.SHIFT_LEFT:
            case Input.Keys.SHIFT_RIGHT:
                setShiftPressed(false);
                break;
            case Input.Keys.CONTROL_LEFT:
            case Input.Keys.CONTROL_RIGHT:
                setCtrlPressed(false);
                break;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        if (!isCaptured()) {
            return false;
        }

        if (isCharacterAllowed(character)) {
            append(character);
            return true;
        }
        return false;
    }

    public boolean isCharacterAllowed(char character) {
//        if (Character.isLetterOrDigit(character)) {
//            return true;
//        }
        return character >= 32 && character < 127;
    }

    public interface ActionListener {
        void onEnter(String input);
    }
}
