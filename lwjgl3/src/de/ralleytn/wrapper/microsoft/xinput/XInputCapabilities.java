package de.ralleytn.wrapper.microsoft.xinput;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class XInputCapabilities extends Struct<XInputCapabilities> implements NativeResource {

    public static final int SIZEOF;
    public static final int ALIGNOF;

    public static final int
            TYPE,
            SUBTYPE,
            FLAGS,
            GAMEPAD,
            VIBRATION;

    static {
        Layout layout = __struct(
                __member(1), // type
                __member(1), // subtype
                __member(2), // flags
                __member(XInputGamepad.SIZEOF, XInputGamepad.ALIGNOF), // Gamepad
                __member(XInputVibration.SIZEOF, XInputVibration.ALIGNOF) // Vibration
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        TYPE = layout.offsetof(0);
        SUBTYPE = layout.offsetof(1);
        FLAGS = layout.offsetof(2);
        GAMEPAD = layout.offsetof(3);
        VIBRATION = layout.offsetof(4);
    }

    public byte Type;
    public byte SubType;
    public short Flags;
    public XInputGamepad Gamepad;
    public XInputVibration Vibration;

    protected XInputCapabilities(long address, ByteBuffer container) {
        super(address, container);

        if (container != null) {
            read(container);
        }
    }

    // Used by XInputEnvironmentPlugin (don't remove!)
    public XInputCapabilities() {
        this(nmemAllocChecked(SIZEOF), ByteBuffer.allocate(SIZEOF));
    }

    @Override
    protected @NotNull XInputCapabilities create(long address, ByteBuffer container) {
        return new XInputCapabilities(address, container);
    }

    @Override
    public int sizeof() { return SIZEOF; }

    public void read(ByteBuffer byteBuffer) {
        this.Type = byteBuffer.get();
        this.SubType = byteBuffer.get();
        this.Flags = byteBuffer.getShort();
        this.Gamepad = new XInputGamepad(address, byteBuffer);
        this.Vibration = new XInputVibration(address, byteBuffer);
    }
}
