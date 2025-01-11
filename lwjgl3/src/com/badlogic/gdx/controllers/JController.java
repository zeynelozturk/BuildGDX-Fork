package com.badlogic.gdx.controllers;

import com.badlogic.gdx.utils.IntMap;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import com.badlogic.gdx.utils.Array;
import ru.m210projects.Build.input.GameController;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.util.*;

import static net.java.games.input.Component.POV.*;

public class JController extends com.badlogic.gdx.controllers.AbstractController implements GameController {

    protected net.java.games.input.Controller controller;

    protected Array<ButtonComponent> buttons;
    protected Array<AxisComponent> axes;
    protected Array<PovComponent> povs;

    private final IntMap<Boolean> buttonState = new IntMap<>();
    private final IntMap<Float> axisState = new IntMap<>();

    public JController(net.java.games.input.Controller controller) {
        this.controller = controller;
        createComponents(controller);
        initializeState();
    }

    protected void createComponents(net.java.games.input.Controller controller) {
        Component[] components = controller.getComponents();

        buttons = new Array<>();
        axes = new Array<>();
        povs = new Array<>();

        int buttonCount = 0;
        int povCount = 0;
        int axisCount = 0;

        // Add buttons first
        List<Component> otherComponents = new ArrayList<>(8);
        for (Component component : components) {
            Identifier componentIdentifier = component.getIdentifier();
            if (componentIdentifier instanceof Button) {
                buttons.add(new ButtonComponent(component));
                buttonCount++;
            } else {
                otherComponents.add(component);
            }
        }

        for (Component component : otherComponents) {
            Identifier componentIdentifier = component.getIdentifier();
            if (componentIdentifier == Axis.POV) {
                povs.add(new PovComponent(component));
                povCount++;
            } else if (component.isAnalog()) {
                if (axisCount == 4) {
                    buttons.add(new TriggerComponent(component, true));
                    buttons.add(new TriggerComponent(component, false));
                } else {
                    axes.add(new AxisComponent(component));
                }
                axisCount++;
            }
        }

        Console.out.println("Found dinput controller: \"" + getName() + "\" [buttons: " + buttonCount + " axises: " + axisCount + " povs: " + povCount + "]", OsdColor.YELLOW);
    }

    public boolean update() {
        if (!controller.poll()) {
            return false;
        }

        updateButtonsState();
        updateAxisState();
        updatePovState();

        return true;
    }

    @Override
    public String getAxisName(int axisCode) {
        return axes.get(axisCode).getName();
    }

    @Override
    public String getButtonName(int buttonCode) {
        if (buttonCode >= buttons.size) {
            int povIds = (buttonCode - buttons.size);
            int povId = povIds / 4;
            return povs.get(povId).getPovName(povIds % 4) + povId;
        }
        return buttons.get(buttonCode).getName();
    }

    protected void updatePovState() {
        for(int id = 0; id < povs.size; id++) {
            PovComponent pov = povs.get(id);
            updateButtonState(4 * id + buttons.size, pov.isUpPressed());
            updateButtonState(4 * id + buttons.size + 1, pov.isLeftPressed());
            updateButtonState(4 * id + buttons.size + 2, pov.isDownPressed());
            updateButtonState(4 * id + buttons.size + 3, pov.isRightPressed());
        }
    }

    protected void updateButtonState(int id, boolean pressed) {
        if (pressed != buttonState.get(id)) {
            if (pressed) {
                notifyListenersButtonDown(id);
            } else {
                notifyListenersButtonUp(id);
            }
        }
        buttonState.put(id, pressed);
    }

    protected void updateButtonsState() {
        for(int id = 0; id < buttons.size; id++) {
            updateButtonState(id, getButton(id));
        }
    }

    protected void updateAxisState() {
        for(int id = 0; id < axes.size; id++) {
            float value = getAxis(id);
            if (value != axisState.get(id)) {
                notifyListenersAxisMoved(id, value);
            }
            axisState.put(id, value);
        }
    }

    private void initializeState() {
        for(int id = 0; id < axes.size; id++) {
            axisState.put(id, 0.0f);
        }

        int len = buttons.size + 4 * povs.size;
        for(int id = 0; id < len; id++) {
            buttonState.put(id, false);
        }
    }

    @Override
    public boolean getButton(int buttonCode) {
        return buttons.get(buttonCode).isButtonPressed();
    }

    @Override
    public float getAxis(int axisCode) {
        return axes.get(axisCode).getValue();
    }

    @Override
    public String getName() {
        return controller.getName();
    }

    @Override
    public String getUniqueId() {
        return "";
    }

    @Override
    public int getMinButtonIndex() {
        return 0;
    }

    @Override
    public int getMaxButtonIndex() {
        return buttons.size;
    }

    @Override
    public int getAxisCount() {
        return axes.size;
    }

    @Override
    public ControllerMapping getMapping() {
        return null;
    }

    @Override
    public ControllerPowerLevel getPowerLevel() {
        return ControllerPowerLevel.POWER_UNKNOWN;
    }

    protected interface JComponent {
        float getValue();

        String getName();
    }

    protected static class ButtonComponent implements JComponent {
        protected final Component component;

        public ButtonComponent(Component component) {
            this.component = component;
        }

        public boolean isButtonPressed() {
            return getValue() == 1.0f;
        }

        @Override
        public float getValue() {
            return component.getPollData();
        }

        @Override
        public String getName() {
            return component.getName();
        }
    }

    protected static class PovComponent implements JComponent {

        protected final Component component;

        public boolean isUpPressed() {
            float value = getValue();
            return Float.compare(UP, value) == 0 || Float.compare(UP_LEFT, value) == 0 || Float.compare(UP_RIGHT, value) == 0;
        }

        public boolean isDownPressed() {
            float value = getValue();
            return Float.compare(DOWN, value) == 0 || Float.compare(DOWN_LEFT, value) == 0 || Float.compare(DOWN_RIGHT, value) == 0;
        }

        public boolean isLeftPressed() {
            float value = getValue();
            return Float.compare(LEFT, value) == 0 || Float.compare(UP_LEFT, value) == 0 || Float.compare(DOWN_LEFT, value) == 0;
        }

        public boolean isRightPressed() {
            float value = getValue();
            return Float.compare(RIGHT, value) == 0 || Float.compare(UP_RIGHT, value) == 0 || Float.compare(DOWN_RIGHT, value) == 0;
        }

        public PovComponent(Component component) {
            this.component = component;
        }

        @Override
        public float getValue() {
            return component.getPollData();
        }

        @Override
        public String getName() {
            return component.getName();
        }

        public String getPovName(int direction) {
            switch (direction) {
                case 0:
                    return "pu";
                case 1:
                    return "pl";
                case 2:
                    return "pd";
                case 3:
                    return "pr";
            }
            return getName();
        }
    }

    protected static class AxisComponent implements JComponent {
        protected final Component component;

        public AxisComponent(Component component) {
            this.component = component;
        }

        @Override
        public float getValue() {
            return component.getPollData();
        }

        @Override
        public String getName() {
            return component.getName();
        }
    }

    protected static class TriggerComponent extends ButtonComponent {
        private final boolean direction;
        public TriggerComponent(Component component, boolean direction) {
            super(component);
            this.direction = direction;
        }

        public boolean isButtonPressed() {
            float value = getValue();
            if (direction) {
                return value >= 0.9f;
            }
            return value <= -0.9f;
        }
    }
}
