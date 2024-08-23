package com.badlogic.gdx.controllers;

import com.badlogic.gdx.utils.IntMap;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import com.badlogic.gdx.utils.Array;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.util.ArrayList;
import java.util.List;

import static net.java.games.input.Component.POV.*;

public class JController extends com.badlogic.gdx.controllers.AbstractController {

    private final JControllerListener listener = new JControllerListener();

    net.java.games.input.Controller controller;
    protected Array<ButtonComponent> buttons;
    protected Array<Component> axes;

    private final IntMap<Boolean> buttonState = new IntMap<>();
    private final IntMap<Float> axisState = new IntMap<>();

    public JController(net.java.games.input.Controller controller) {
        this.controller = controller;

        Component[] components = controller.getComponents();

        buttons = new Array<>();
        axes = new Array<>();

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
                buttons.add(new PovComponent(component, UP, UP_LEFT, UP_RIGHT)); // 0.125(1), 0.25(2), 0.375(3)
                buttons.add(new PovComponent(component, LEFT, UP_LEFT, DOWN_LEFT)); // 0.125(1), 0.875(7), 1.0 (8)
                buttons.add(new PovComponent(component, DOWN, DOWN_RIGHT, DOWN_LEFT)); // 0.625(5), 0.75(6), 0.875(7)
                buttons.add(new PovComponent(component, RIGHT, DOWN_RIGHT, UP_RIGHT)); // 0.375(3), 0.5(4), 0.625(5)
                povCount++;
            } else if (component.isAnalog()) {
                if (axisCount == 4) {
                    buttons.add(new TriggerComponent(component, true));
                    buttons.add(new TriggerComponent(component, false));
                } else {
                    axes.add(component);
                }
                axisCount++;
            }
        }

        initializeState();

        Console.out.println("Found controller: \"" + getName() + "\" [buttons: " + buttonCount + " axises: " + axisCount + " povs: " + povCount + "]", OsdColor.YELLOW);
    }

    public boolean update() {
        if (!controller.poll()) {
            return false;
        }

        updateButtonsState();
        updateAxisState();

        return true;
    }

    private void updateButtonsState() {
        for(int id = 0; id < buttons.size; id++) {
            boolean pressed = getButton(id);
            if (pressed != buttonState.get(id)) {
                if (pressed) {
                    notifyListenersButtonDown(id);
                } else {
                    notifyListenersButtonUp(id);
                }
            }
            buttonState.put(id, pressed);
        }
    }

    private void updateAxisState() {
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

        for(int id = 0; id < buttons.size; id++) {
            buttonState.put(id, false);
        }
    }

    @Override
    public boolean getButton(int buttonCode) {
        return buttons.get(buttonCode).isButtonPressed();
    }

    @Override
    public float getAxis(int axisCode) {
        return axes.get(axisCode).getPollData();
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
        return null;
    }

    @Override
    protected void notifyListenersButtonUp(int button) {
        listener.buttonUp(this, button);
    }

    @Override
    protected void notifyListenersButtonDown(int button) {
        listener.buttonDown(this, button);
    }

    @Override
    protected void notifyListenersAxisMoved(int axisNum, float value) {
        listener.axisMoved(this, axisNum, value);
    }

    @Override
    public void addListener(ControllerListener controllerListener) {
        listener.addListener(controllerListener);
    }

    @Override
    public void removeListener(ControllerListener controllerListener) {
        listener.removeListener(controllerListener);
    }

    protected static class ButtonComponent {
        protected final Component component;

        public ButtonComponent(Component component) {
            this.component = component;
        }

        public boolean isButtonPressed() {
            return component.getPollData() == 1.0f;
        }
    }

    private static class PovComponent extends ButtonComponent {
        private final float[] direction;
        public PovComponent(Component component, float... direction) {
            super(component);
            this.direction = direction;
        }

        public boolean isButtonPressed() {
            for (float v : direction) {
                if (Float.compare(component.getPollData(), v) == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class TriggerComponent extends ButtonComponent {
        private final boolean direction;
        public TriggerComponent(Component component, boolean direction) {
            super(component);
            this.direction = direction;
        }

        public boolean isButtonPressed() {
            float value = component.getPollData();
            if (direction) {
                return value >= 0.9f;
            }
            return value <= -0.9f;
        }
    }
}
