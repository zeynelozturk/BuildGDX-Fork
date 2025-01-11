package com.badlogic.gdx.controllers;

import com.badlogic.gdx.utils.Array;
import de.ralleytn.wrapper.microsoft.xinput.XInput;
import de.ralleytn.wrapper.microsoft.xinput.XInputBatteryInformation;
import net.java.games.input.*;
import net.java.games.input.Controller;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.controllers.ControllerMapping.UNDEFINED;
import static de.ralleytn.wrapper.microsoft.xinput.XInputBatteryInformation.BATTERY_DEVTYPE_GAMEPAD;

public class JXController extends JController {

    public static final XInput XINPUT = XInput.create();
    public int playerIndex;
    private final Event tmpEvent = new Event();
    private Map<Component.Identifier, JXComponent> componentMap;
    private ControllerMapping mapping;

    public JXController(Controller controller) {
        super(controller);
        this.playerIndex = getUserIndex(controller);
    }

    private int getUserIndex(Controller controller) {
        try {
            Method method = controller.getClass().getDeclaredMethod("getUserIndex");
            method.setAccessible(true);
            return (int) method.invoke(controller);
        } catch (Exception ignored) {
        }
        return com.badlogic.gdx.controllers.Controller.PLAYER_IDX_UNSET;
    }

    @Override
    protected void createComponents(net.java.games.input.Controller controller) {
        Component[] components = controller.getComponents();

        buttons = new Array<>();
        axes = new Array<>();
        povs = new Array<>();
        componentMap = new HashMap<>();

        int buttonCount = 0;
        int povCount = 0;
        int axisCount = 0;

        int axisLeftX = UNDEFINED, axisLeftY = UNDEFINED, axisRightX = UNDEFINED, axisRightY = UNDEFINED,
                buttonA = UNDEFINED, buttonB = UNDEFINED, buttonX = UNDEFINED, buttonY = UNDEFINED, buttonBack = UNDEFINED, buttonStart = UNDEFINED, buttonL1 = UNDEFINED, buttonL2 = UNDEFINED, buttonR1 = UNDEFINED, buttonR2 = UNDEFINED,
                buttonLeftStick = UNDEFINED, buttonRightStick = UNDEFINED;
        int buttonDpadUp = UNDEFINED, buttonDpadDown = UNDEFINED, buttonDpadLeft = UNDEFINED, buttonDpadRight = UNDEFINED;

        for (Component component : components) {
            Component.Identifier componentIdentifier = component.getIdentifier();
            JXComponent c = null;
            if (componentIdentifier instanceof Component.Identifier.Button) {
                c = new JXButtonComponent(component);
                buttons.add((ButtonComponent) c);
                buttonCount++;
            } else if (componentIdentifier == Component.Identifier.Axis.POV) {
                c = new JXPovComponent(component);
                povs.add((PovComponent) c);
                povCount++;
            } else if (component.isAnalog()) {
                if (componentIdentifier == Component.Identifier.Axis.Z || componentIdentifier == Component.Identifier.Axis.RZ) {
                    c = new JXTriggerComponent(component, true);
                    buttons.add((ButtonComponent) c);
                } else {
                    c = new JXAxisComponent(component, componentIdentifier == Component.Identifier.Axis.Y || componentIdentifier == Component.Identifier.Axis.RY);
                    axes.add((AxisComponent) c);
                }
                axisCount++;
            }

            if (c != null) {
                if (componentIdentifier == Component.Identifier.Axis.X) {
                    axisLeftX = axes.size - 1;
                } else if (componentIdentifier == Component.Identifier.Axis.Y) {
                    axisLeftY = axes.size - 1;
                } else if (componentIdentifier == Component.Identifier.Axis.RX) {
                    axisRightX = axes.size - 1;
                } else if (componentIdentifier == Component.Identifier.Axis.RY) {
                    axisRightY = axes.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.A.getName())) { // 0
                    buttonA = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.B.getName())) { // 1
                    buttonB = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.X.getName())) { // 2
                    buttonX = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.Y.getName())) { // 3
                    buttonY = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.BACK.getName())) { // 6
                    buttonBack = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase(Component.Identifier.Button.START.getName())) { // 7
                    buttonStart = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase("lthumb")) { // 8
                    buttonLeftStick = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase("rthumb")) { // 9
                    buttonRightStick = buttons.size - 1;
                } else if (componentIdentifier == Component.Identifier.Axis.Z) { // 10*
                    buttonL1 = buttons.size - 1;
                } else if (componentIdentifier == Component.Identifier.Axis.RZ) { // 11*
                    buttonR1 = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase("lb")) { // 4
                    buttonL2 = buttons.size - 1;
                } else if (component.getName().equalsIgnoreCase("rb")) { // 5
                    buttonR2 = buttons.size - 1;
                }

                componentMap.put(component.getIdentifier(), c);
            }
        }

        if (povs.size != 0) {
            buttonDpadUp = buttons.size;
            buttonDpadLeft = buttons.size + 1;
            buttonDpadDown = buttons.size + 2;
            buttonDpadRight = buttons.size + 3;
        }

        mapping = new ControllerMapping(axisLeftX, axisLeftY, axisRightX, axisRightY,
                buttonA, buttonB, buttonX, buttonY, buttonBack, buttonStart, buttonL1, buttonL2, buttonR1, buttonR2,
                buttonLeftStick, buttonRightStick, buttonDpadUp, buttonDpadDown, buttonDpadLeft, buttonDpadRight);

        Console.out.println("Found xinput controller: \"" + getName() + "\" [buttons: " + buttonCount + " axises: " + axisCount + " povs: " + povCount + "]", OsdColor.YELLOW);
    }

    @Override
    public boolean update() {
        if (!controller.poll()) {
            return false;
        }

        EventQueue queue = controller.getEventQueue();
        while (queue.getNextEvent(tmpEvent)) {
            Component.Identifier componentIdentifier = tmpEvent.getComponent().getIdentifier();
            JXComponent component = componentMap.get(componentIdentifier);
            component.setEvent(tmpEvent);
        }

        updateButtonsState();
        updateAxisState();
        updatePovState();

        return true;
    }

    @Override
    public ControllerMapping getMapping() {
        return mapping;
    }

    @Override
    public boolean canVibrate() {
        return controller.getRumblers().length != 0;
    }

    @Override
    public void startVibration(int duration, float strength) {
        if (canVibrate()) {
            controller.getRumblers()[0].rumble(strength);
        }
    }

    @Override
    public boolean isVibrating() {
        return false;
    }

    @Override
    public void cancelVibration() {
    }

    @Override
    public boolean supportsPlayerIndex() {
        return true;
    }

    @Override
    public int getPlayerIndex() {
        return playerIndex;
    }

    @Override
    public void setPlayerIndex(int index) {
        this.playerIndex = index;
    }

    @Override
    public ControllerPowerLevel getPowerLevel() {
        if (XINPUT != null && playerIndex != com.badlogic.gdx.controllers.Controller.PLAYER_IDX_UNSET) {
            XInputBatteryInformation information = new XInputBatteryInformation();
            int out = XINPUT.XInputGetBatteryInformation(playerIndex, BATTERY_DEVTYPE_GAMEPAD, information);
            if (out == 0) {
                if (information.batteryType == XInputBatteryInformation.BatteryType.WIRED) {
                    return ControllerPowerLevel.POWER_WIRED;
                } else {
                    switch (information.batteryLevel) {
                        case EMPTY:
                            return ControllerPowerLevel.POWER_EMPTY;
                        case LOW:
                            return ControllerPowerLevel.POWER_LOW;
                        case MEDIUM:
                            return ControllerPowerLevel.POWER_MEDIUM;
                        case FULL:
                            return ControllerPowerLevel.POWER_FULL;
                    }
                }
            }
        }
        return ControllerPowerLevel.POWER_UNKNOWN;
    }

    private interface JXComponent extends JComponent {
        void setEvent(Event event);
    }

    protected static class JXButtonComponent extends ButtonComponent implements JXComponent {
        private final Event event = new Event();

        public JXButtonComponent(Component component) {
            super(component);
        }

        @Override
        public void setEvent(Event event) {
            this.event.set(event);
        }

        @Override
        public float getValue() {
            return event.getValue();
        }
    }

    protected static class JXPovComponent extends PovComponent implements JXComponent {

        private final Event event = new Event();

        public JXPovComponent(Component component) {
            super(component);
        }

        @Override
        public void setEvent(Event event) {
            this.event.set(event);
        }

        @Override
        public float getValue() {
            return event.getValue();
        }
    }

    protected static class JXTriggerComponent extends TriggerComponent implements JXComponent {
        private final Event event = new Event();

        public JXTriggerComponent(Component component, boolean direction) {
            super(component, direction);
        }

        @Override
        public void setEvent(Event event) {
            this.event.set(event);
        }

        @Override
        public float getValue() {
            return event.getValue();
        }
    }

    protected static class JXAxisComponent extends AxisComponent implements JXComponent {
        private final Event event = new Event();
        private final boolean invert;

        public JXAxisComponent(Component component, boolean invert) {
            super(component);
            this.invert = invert;
        }

        @Override
        public void setEvent(Event event) {
            this.event.set(event);
        }

        @Override
        public float getValue() {
            return invert ? -event.getValue() : event.getValue();
        }
    }
}
