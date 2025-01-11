package de.ralleytn.wrapper.microsoft.xinput;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class XInputBatteryInformation extends Struct<XInputBatteryInformation> implements NativeResource {

    public enum BatteryType {
        DISCONNECTED,
        WIRED,
        ALKALINE,
        NIMH,
        UNKNOWN;

        public static BatteryType parseValue(byte val) {
            switch (val) {
                case 0x00:
                    return DISCONNECTED;
                case 0x01:
                    return WIRED;
                case 0x02:
                    return ALKALINE;
                case 0x03:
                    return NIMH;
            }
            return UNKNOWN;
        }
    }

    public enum BatteryLevel {
        EMPTY,
        LOW,
        MEDIUM,
        FULL,
        UNKNOWN;

        public static BatteryLevel parseValue(byte val) {
            switch (val) {
                case 0x00:
                    return EMPTY;
                case 0x01:
                    return LOW;
                case 0x02:
                    return MEDIUM;
                case 0x03:
                    return FULL;
            }
            return UNKNOWN;
        }
    }

    public static final int BATTERY_DEVTYPE_GAMEPAD = 0x00;
    public static final int BATTERY_DEVTYPE_HEADSET = 0x01;

    public static final int SIZEOF;
    public static final int ALIGNOF;

    public static final int
            BATTERY_TYPE,
            BATTERY_LEVEL;

    static {
        Layout layout = __struct(
                __member(1),
                __member(1)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        BATTERY_TYPE = layout.offsetof(0);
        BATTERY_LEVEL = layout.offsetof(1);
    }

    public BatteryType batteryType;
    public BatteryLevel batteryLevel;

    protected XInputBatteryInformation(long address, ByteBuffer container) {
        super(address, container);

        if (container != null) {
            read(container);
        }
    }

    public XInputBatteryInformation() {
        this(nmemAllocChecked(SIZEOF), ByteBuffer.allocate(SIZEOF));
    }

    @Override
    protected @NotNull XInputBatteryInformation create(long address, ByteBuffer container) {
        return new XInputBatteryInformation(address, container);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void read(ByteBuffer byteBuffer) {
        this.batteryType =  BatteryType.parseValue(byteBuffer.get());
        this.batteryLevel =  BatteryLevel.parseValue(byteBuffer.get());
    }

    @Override
    public @NotNull String toString() {
        return "XInputBatteryInformation{" +
                "batteryType=" + batteryType +
                ", batteryLevel=" + batteryLevel +
                '}';
    }
}
