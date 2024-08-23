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

package ru.m210projects.Build.settings;

import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.keymap.ControllerAxis;
import ru.m210projects.Build.input.keymap.ControllerButton;
import ru.m210projects.Build.input.keymap.Keymap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static com.badlogic.gdx.Input.Keys.UNKNOWN;
import static ru.m210projects.Build.input.keymap.Keymap.*;

public class InputContext implements ConfigContext {

    public static final int PRIMARY_KEYS_INDEX = 0;
    public static final int SECONDARY_KEYS_INDEX = 1;
    public static final int MOUSE_KEYS_INDEX = 2;
    public static final int GAMEPAD_KEYS_INDEX = 3;

    protected final int[][] primarykeys;
    final int[] defkeys;
    final int[] defclassickeys;
    protected final GameKey[] keymap;
    GameKey[] arrayPressedKey = new GameKey[256];
    boolean useMouse = true;
    boolean menuMouse = true;
    int gSensitivity = 69632;
    int gMouseTurnSpeed = 65536;
    int gMouseLookSpeed = 65536;
    int gMouseMoveSpeed = 65536;
    int gMouseStrafeSpeed = 131072;
    boolean gMouseAim = true;
    boolean gInvertmouse = false;
    GameKey[] mouseaxis = new GameKey[MouseAxis.values().length];
    boolean rawInput = true;

    String controllerName = "";
    float joyDeadZone = 0.1f;
    float joyTurnSpeed = 2.5f;
    float joyLookSpeed = 1.0f;
    boolean joyInvert = false;

    Map<String, ControllerMapping> controllerMappingMap = new HashMap<>();

    public InputContext(GameKey[] keymap, int[] defkeys, int[] defclassickeys) {
        this.keymap = keymap;
        this.defkeys = defkeys;
        this.defclassickeys = defclassickeys;
        this.primarykeys = new int[4][defkeys.length];
    }

    @Override
    public void load(Properties prop) {
        clearInput();
        if (prop.setContext("KeyDefinitions")) {
            for (GameKey gameKey : keymap) {
                for (int j = PRIMARY_KEYS_INDEX; j <= GAMEPAD_KEYS_INDEX; j++) {
                    String keyValue = prop.getStringValue(gameKey.getName(), j, "");
                    int keyCode = Keymap.valueOf(keyValue);
                    if (keyCode != UNKNOWN) {
                        if (j != MOUSE_KEYS_INDEX) {
                            bindKey(gameKey, keyCode);
                        } else {
                            bindMouse(gameKey, keyCode);
                        }
                    }
                }
            }

            final int menuToggle = GameKeys.Menu_Toggle.getNum();
            if (menuToggle != -1 && primarykeys[PRIMARY_KEYS_INDEX][menuToggle] == 0) {
                bindKey(GameKeys.Menu_Toggle, defclassickeys[menuToggle]);
            }

            // left, right, up, down
            for (int j = MouseAxis.LEFT.ordinal(); j <= MouseAxis.DOWN.ordinal(); j++) {
                String keyName = prop.getStringValue("MouseDigitalAxes", j, "N/A");
                mouseaxis[j] = Arrays.stream(keymap).filter(e -> e.getName().equalsIgnoreCase(keyName)).findFirst().orElse(GameKey.UNKNOWN_KEY);
            }
        } else {
            resetInput(false);
        }

        if (prop.setContext("Controls")) {
            rawInput = prop.getBooleanValue("RawMouseInput", rawInput);
            useMouse = prop.getBooleanValue("UseMouse", useMouse);
            menuMouse = prop.getBooleanValue("UseMouseInMenu", menuMouse);
            gSensitivity = prop.getIntValue("MouseSensitivity", gSensitivity);
            gMouseAim = prop.getBooleanValue("MouseAiming", gMouseAim);
            gInvertmouse = prop.getBooleanValue("MouseAimingFlipped", gInvertmouse);

            gMouseTurnSpeed = prop.getIntValue("MouseTurnSpeed", gMouseTurnSpeed);
            gMouseLookSpeed = prop.getIntValue("MouseLookSpeed", gMouseLookSpeed);
            gMouseMoveSpeed = prop.getIntValue("MouseMoveSpeed", gMouseMoveSpeed);
            gMouseStrafeSpeed = prop.getIntValue("MouseStrafeSpeed", gMouseStrafeSpeed);

            controllerName = prop.getStringValue("ControllerName", controllerName);
//            joyTurnAxis = prop.getIntValue("JoyTurnAxis", joyTurnAxis);
//            joyMoveAxis = prop.getIntValue("JoyMoveAxis", joyMoveAxis);
//            joyStrafeAxis = prop.getIntValue("JoyStrafeAxis", joyStrafeAxis);
//            joyLookAxis = prop.getIntValue("JoyLookAxis", joyLookAxis);
            joyTurnSpeed = prop.getFloatValue("JoyTurnSpeed", joyTurnSpeed);
            joyLookSpeed = prop.getFloatValue("JoyLookSpeed", joyLookSpeed);
            joyInvert = prop.getBooleanValue("JoyInvertLook", joyInvert);
            joyDeadZone = prop.getFloatValue("JoyDeadZone", joyDeadZone);
        }

        int count = 1;
        final String valueName = "ControllerMapping";
        do {
            if(!prop.setContext(valueName + count++)) {
                break;
            }

            String name = prop.getStringValue("ControllerName", "");
            if (name.isEmpty()) {
                break;
            }

            ControllerMapping controllerMapping = new ControllerMapping();
            for (ControllerAxis axis : ControllerAxis.values()) {
                int id = prop.getIntValue(axis.name(), -1);
                if (id != -1) {
                    controllerMapping.putAxis(axis, id);
                }
            }

            for (ControllerButton button : ControllerButton.values()) {
                int id = prop.getIntValue(button.name(), -1);
                if (id != -1) {
                    controllerMapping.putButton(button, id);
                }
            }
            controllerMappingMap.put(name.toUpperCase(), controllerMapping);
        } while (count < 16);
    }

    @Override
    public void save(OutputStream os) throws IOException {
        putString(os, "[KeyDefinitions]\r\n");

        for (GameKey gameKey : keymap) {
            StringJoiner keyValue = new StringJoiner(", ");
            for (int j = PRIMARY_KEYS_INDEX; j <= GAMEPAD_KEYS_INDEX; j++) {
                keyValue.add("\"" + Keymap.toString(primarykeys[j][gameKey.getNum()]) + "\"");
            }
            putString(os, gameKey.getName(), keyValue.toString());
        }

        putString(os, ";Left, Right, Up, Down\r\n");
        StringJoiner keyValue = new StringJoiner(", ");
        for (int j = MouseAxis.LEFT.ordinal(); j <= MouseAxis.DOWN.ordinal(); j++) {
            keyValue.add("\"" + mouseaxis[j].getName() + "\"");
        }
        putString(os, "MouseDigitalAxes", keyValue.toString());

        putString(os, ";\r\n");

        putString(os, "[Controls]\r\n");
        putBoolean(os, "RawMouseInput", rawInput);
        putBoolean(os, "UseMouse", useMouse);
        putBoolean(os, "UseMouseInMenu", menuMouse);
        putInteger(os, "MouseSensitivity", gSensitivity);
        putBoolean(os, "MouseAiming", gMouseAim);
        putBoolean(os, "MouseAimingFlipped", gInvertmouse);
        putInteger(os, "MouseTurnSpeed", gMouseTurnSpeed);
        putInteger(os, "MouseLookSpeed", gMouseLookSpeed);
        putInteger(os, "MouseMoveSpeed", gMouseMoveSpeed);
        putInteger(os, "MouseStrafeSpeed", gMouseStrafeSpeed);

        putString(os, "ControllerName", controllerName);
//        putInteger(os, "JoyTurnAxis", joyTurnAxis);
//        putInteger(os, "JoyMoveAxis", joyMoveAxis);
//        putInteger(os, "JoyStrafeAxis", joyStrafeAxis);
//        putInteger(os, "JoyLookAxis", joyLookAxis);
        putFloat(os, "JoyTurnSpeed", joyTurnSpeed);
        putFloat(os, "JoyLookSpeed", joyLookSpeed);
        putBoolean(os, "JoyInvertLook", joyInvert);
        putFloat(os, "JoyDeadZone", joyDeadZone);
        putString(os, ";\r\n");

        int mappingCount = 1;
        for(String controllerName : controllerMappingMap.keySet()) {
            putString(os, "[ControllerMapping" + mappingCount + "]\r\n");
            putString(os, "ControllerName", controllerName);
            ControllerMapping controllerMapping = controllerMappingMap.get(controllerName);

            for (ControllerAxis axis : ControllerAxis.values()) {
                if (axis == ControllerAxis.NULL) {
                    continue;
                }
                putInteger(os, axis.name(), controllerMapping.getAxisCode(axis));
            }

            for (ControllerButton button : ControllerButton.values()) {
                if (button == ControllerButton.NULL) {
                    continue;
                }
                putInteger(os, button.name(), controllerMapping.getButtonCode(button));
            }

            mappingCount++;
        }
        putString(os, ";\r\n;\r\n");
    }

    protected void clearInput() {
        Arrays.fill(primarykeys[PRIMARY_KEYS_INDEX], -1);
        Arrays.fill(primarykeys[SECONDARY_KEYS_INDEX], -1);
        Arrays.fill(primarykeys[MOUSE_KEYS_INDEX], -1);
        Arrays.fill(primarykeys[GAMEPAD_KEYS_INDEX], -1);
        Arrays.fill(arrayPressedKey, GameKey.UNKNOWN_KEY);
        Arrays.fill(mouseaxis, GameKey.UNKNOWN_KEY);
    }

    public void resetInput(boolean classicKeys) {
        int[] keys = classicKeys ? defclassickeys : defkeys;
        clearInput();

        for (GameKey gameKey : keymap) {
            int num = gameKey.getNum();
            if (num != -1) {
                bind(gameKey, keys[num], PRIMARY_KEYS_INDEX);
            }
        }

        bindMouse(GameKeys.Weapon_Fire, MOUSE_LBUTTON);
        bindMouse(GameKeys.Open, MOUSE_MBUTTON);
        bindMouse(GameKeys.Next_Weapon, MOUSE_WHELLUP);
        bindMouse(GameKeys.Previous_Weapon, MOUSE_WHELLDN);

        bindKey(GameKeys.Jump, ControllerButton.BUTTON_A);
        bindKey(GameKeys.Crouch, ControllerButton.BUTTON_B);
        bindKey(GameKeys.Open, ControllerButton.BUTTON_Y);
        bindKey(GameKeys.Weapon_Fire, ControllerButton.RIGHT_TRIGGER);
        bindKey(GameKeys.Menu_Toggle, ControllerButton.START);
        bindKey(GameKeys.Next_Weapon, ControllerButton.DPAD_UP);
        bindKey(GameKeys.Previous_Weapon, ControllerButton.DPAD_DOWN);
    }

    public int getKeyCode(GameKey gameKey, int index) {
        return primarykeys[index][gameKey.getNum()];
    }

    public void bindKey(GameKey gameKey, int keyCode) {
        if (keyCode == UNKNOWN) {
            return;
        }

        ControllerButton controllerButton = ControllerButton.valueOf(keyCode);
        if (controllerButton != ControllerButton.NULL) {
            bindKey(gameKey, controllerButton);
            return;
        }

        int index = gameKey.getNum();
        if (index == -1) {
            return;
        }

        GameKey oldBindKey = getBindKey(keyCode);
        if (oldBindKey != GameKey.UNKNOWN_KEY) {
            // if key exists in the slots, bind the key to primary slot and clear secondary slot
            if (oldBindKey == gameKey && primarykeys[PRIMARY_KEYS_INDEX][index] > 0 && primarykeys[SECONDARY_KEYS_INDEX][index] > 0) {
                bind(gameKey, keyCode, PRIMARY_KEYS_INDEX);
                unbind(gameKey, SECONDARY_KEYS_INDEX);
                return;
            }
            unbind(keyCode);
        }

        // if the slots is empty, bind the key to primary slot
        if (primarykeys[PRIMARY_KEYS_INDEX][index] <= 0 && primarykeys[SECONDARY_KEYS_INDEX][index] <= 0) {
            bind(gameKey, keyCode, PRIMARY_KEYS_INDEX);
        } else {
            unbind(gameKey, SECONDARY_KEYS_INDEX);
            // primary, secondary slot swap or substitution
            bind(gameKey, primarykeys[PRIMARY_KEYS_INDEX][index], SECONDARY_KEYS_INDEX);
            bind(gameKey, keyCode, PRIMARY_KEYS_INDEX);
        }
    }

    public void bindMouse(GameKey gameKey, int keyCode) {
        GameKey oldBindKey = getBindKey(keyCode);
        if (oldBindKey != GameKey.UNKNOWN_KEY) {
            unbind(keyCode);
        }

        bind(gameKey, keyCode, MOUSE_KEYS_INDEX);
    }

    protected void bindKey(GameKey gameKey, ControllerButton button) {
        int keyCode = button.getKeyCode();
        GameKey oldBindKey = getBindKey(keyCode);
        if (oldBindKey != GameKey.UNKNOWN_KEY) {
            unbind(keyCode);
        }
        bind(gameKey, keyCode, GAMEPAD_KEYS_INDEX);
    }

    public ControllerMapping getControllerMapping(String controllerName) {
        return controllerMappingMap.computeIfAbsent(controllerName.toUpperCase(), e -> new ControllerMapping());
    }

    public void unbindAll(GameKey gameKey) {
        for (int j = PRIMARY_KEYS_INDEX; j <= GAMEPAD_KEYS_INDEX; j++) {
            unbind(gameKey, j);
        }
    }

    public boolean isRawInput() {
        return rawInput;
    }

    public void setRawInput(boolean rawInput) {
        this.rawInput = rawInput;
    }

    public float getJoyDeadZone() {
        return joyDeadZone;
    }

    public void setJoyDeadZone(float joyDeadZone) {
        this.joyDeadZone = joyDeadZone;
    }

    public float getJoyTurnSpeed() {
        return joyTurnSpeed;
    }

    public void setJoyTurnSpeed(float joyTurnSpeed) {
        this.joyTurnSpeed = joyTurnSpeed;
    }

    public float getJoyLookSpeed() {
        return joyLookSpeed;
    }

    public void setJoyLookSpeed(float joyLookSpeed) {
        this.joyLookSpeed = joyLookSpeed;
    }

    public boolean isJoyInvert() {
        return joyInvert;
    }

    public void setJoyInvert(boolean joyInvert) {
        this.joyInvert = joyInvert;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    protected GameKey getBindKey(int keyCode) {
        return keyCode != -1 ? arrayPressedKey[keyCode] : GameKey.UNKNOWN_KEY;
    }

    protected void unbind(int keyCode) {
        int index = getBindKey(keyCode).getNum();
        if (index >= 0) {
            for (int j = PRIMARY_KEYS_INDEX; j <= GAMEPAD_KEYS_INDEX; j++) {
                if (primarykeys[j][index] == keyCode) {
                    // Console.out.println("Unbind key " + Keymap.toString(keyCode) + " from " + arrayPressedKey[keyCode], OsdColor.BLUE);
                    primarykeys[j][index] = -1;
                    arrayPressedKey[keyCode] = GameKey.UNKNOWN_KEY;
                }
            }
        }
    }

    protected void unbind(GameKey gameKey, int keyIndex) {
        int index = gameKey.getNum();
        int keyCode = primarykeys[keyIndex][index];
        if (keyCode > 0) {
            primarykeys[keyIndex][index] = -1;
            arrayPressedKey[keyCode] = GameKey.UNKNOWN_KEY;
        }
    }

    protected void bind(GameKey gameKey, int keyCode, int keyIndex) {
        int num = gameKey.getNum();
        if (num == -1) {
            return;
        }

        if (keyCode > 0 && keyCode < arrayPressedKey.length) {
            primarykeys[keyIndex][num] = keyCode;
            arrayPressedKey[keyCode] = gameKey;
        }
    }
}
