package de.ralleytn.wrapper.microsoft.xinput;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class XInputGamepad extends Struct<XInputGamepad> implements NativeResource {

    public static final int SIZEOF;
    public static final int ALIGNOF;

    public static final int BUTTONS,
            LEFT_TRIGGER,
            RIGHT_TRIGGER,
            THUMB_LX,
            THUMB_LY,
            THUMB_RX,
            THUMB_RY;

    static {
        Layout layout = __struct(
                __member(2), // wButtons
                __member(1), // bLeftTrigger
                __member(1), // bRightTrigger
                __member(2), // sThumbLX
                __member(2), // sThumbLY
                __member(2), // sThumbRX
                __member(2) // sThumbRY
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        BUTTONS = layout.offsetof(0);
        LEFT_TRIGGER = layout.offsetof(1);
        RIGHT_TRIGGER = layout.offsetof(2);
        THUMB_LX = layout.offsetof(3);
        THUMB_LY = layout.offsetof(4);
        THUMB_RX = layout.offsetof(5);
        THUMB_RY = layout.offsetof(6);
    }

    public short wButtons;
    public byte bLeftTrigger;
    public byte bRightTrigger;
    public short sThumbLX;
    public short sThumbLY;
    public short sThumbRX;
    public short sThumbRY;

    protected XInputGamepad(long address, ByteBuffer container) {
        super(address, container);

        if (container != null) {
            wButtons = container.getShort();
            bLeftTrigger = container.get();
            bRightTrigger = container.get();
            sThumbLX = container.getShort();
            sThumbLY = container.getShort();
            sThumbRX = container.getShort();
            sThumbRY = container.getShort();
        }
    }

    @Override
    protected @NotNull XInputGamepad create(long address, ByteBuffer container) {
        return new XInputGamepad(address, container);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    @Override
    public @NotNull String toString() {
        return "XInputGamepad{" +
                "wButtons=" + wButtons +
                ", bLeftTrigger=" + bLeftTrigger +
                ", bRightTrigger=" + bRightTrigger +
                ", sThumbLX=" + sThumbLX +
                ", sThumbLY=" + sThumbLY +
                ", sThumbRX=" + sThumbRX +
                ", sThumbRY=" + sThumbRY +
                '}';
    }
}
