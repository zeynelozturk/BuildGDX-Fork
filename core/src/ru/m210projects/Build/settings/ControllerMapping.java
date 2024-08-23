//This file is part of BuildGDX.
//Copyright (C) 2023-2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.settings;

import com.badlogic.gdx.Input;
import ru.m210projects.Build.input.keymap.ControllerAxis;
import ru.m210projects.Build.input.keymap.ControllerButton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ControllerMapping {

    private final Map<Integer, Integer> buttonMap = new HashMap<>(32);
    private final int[] controllerButtonMap = new int[ControllerButton.values().length];

    private final Map<Integer, ControllerAxis> axisMap = new HashMap<>(8);
    private final int[] controllerAxisMap = new int[ControllerAxis.values().length];

    public ControllerMapping() {
        Arrays.fill(controllerButtonMap, -1);
        Arrays.fill(controllerAxisMap, -1);
    }

    /**
     * Links dedicated button with controller button code
     * @param button dedicated {@link ru.m210projects.Build.input.keymap.ControllerButton}
     * @param buttonCode raw button id that is different on each controller
     */
    public void putButton(ControllerButton button, int buttonCode) {
        removeButton(button);
        removeButton(ControllerButton.valueOf(getKeyCode(buttonCode)));
        buttonMap.put(buttonCode, button.getKeyCode());
        controllerButtonMap[button.ordinal()] = buttonCode;
    }

    /**
     * Removes mapping from dedicated button
     * @param button dedicated {@link ru.m210projects.Build.input.keymap.ControllerButton}
     */
    public void removeButton(ControllerButton button) {
        int id = controllerButtonMap[button.ordinal()];
        if (id == -1) {
            return;
        }

        buttonMap.remove(id);
        controllerButtonMap[button.ordinal()] = -1;
    }


    /**
     * Links dedicated axis with controller raw axis code
     * @param axis dedicated {@link ru.m210projects.Build.input.keymap.ControllerAxis}
     * @param axisCode raw axis id that is different on each controller
     */
    public void putAxis(ControllerAxis axis, int axisCode) {
        removeAxis(axis);
        removeAxis(getAxis(axisCode));
        axisMap.put(axisCode, axis);
        controllerAxisMap[axis.ordinal()] = axisCode;
    }

    /**
     * Removes mapping from dedicated axis
     * @param axis dedicated {@link ru.m210projects.Build.input.keymap.ControllerAxis}
     */
    public void removeAxis(ControllerAxis axis) {
        int axisCode = controllerAxisMap[axis.ordinal()];
        if (axisCode == -1) {
            return;
        }

        axisMap.remove(axisCode);
        controllerAxisMap[axis.ordinal()] = -1;
    }

    /**
     * This method calls when saving controller mapping to the file
     * @param button dedicated {@link ru.m210projects.Build.input.keymap.ControllerButton}
     * @return Controller raw buttonCode
     */
    public int getButtonCode(ControllerButton button) {
        return controllerButtonMap[button.ordinal()];
    }

    /**
     * This method calls when saving controller mapping to the file
     * @param axis dedicated {@link ru.m210projects.Build.input.keymap.ControllerAxis}
     * @return Controller raw axisCode
     */
    public int getAxisCode(ControllerAxis axis) {
        return controllerAxisMap[axis.ordinal()];
    }

    /**
     * @param axisCode controller raw axisCode
     * @return dedicated {@link ru.m210projects.Build.input.keymap.ControllerAxis}
     */
    public ControllerAxis getAxis(int axisCode) {
        return axisMap.getOrDefault(axisCode, ControllerAxis.NULL);
    }

    /**
     *
     * @param buttonCode controller raw buttonCode
     * @return dedicated {@link ru.m210projects.Build.input.keymap.ControllerButton}
     */
    public ControllerButton getButton(int buttonCode) {
        return ControllerButton.valueOf(getKeyCode(buttonCode));
    }

    /**
     * @param buttonCode controller raw buttonCode
     * @return key code from {@link ru.m210projects.Build.input.keymap.Keymap} that converted by {@link ru.m210projects.Build.input.keymap.ControllerButton}
     */
    public int getKeyCode(int buttonCode) {
        return buttonMap.getOrDefault(buttonCode, Input.Keys.UNKNOWN);
    }

}
