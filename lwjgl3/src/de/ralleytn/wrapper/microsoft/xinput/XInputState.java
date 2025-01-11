package de.ralleytn.wrapper.microsoft.xinput;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class XInputState extends Struct<XInputState> implements NativeResource {

    public static final int SIZEOF;
    public static final int ALIGNOF;
    public static final int
            DW_PACKET_NUMBER,
            GAMEPAD;

    static {
        Layout layout = __struct(
                __member(4),
                __member(XInputGamepad.SIZEOF, XInputGamepad.ALIGNOF)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        DW_PACKET_NUMBER = layout.offsetof(0);
        GAMEPAD = layout.offsetof(1);
    }

    public int dwPacketNumber;
    public XInputGamepad Gamepad;

    protected XInputState(long address, ByteBuffer container) {
        super(address, container);

        if (container != null) {
            read(container);
        }
    }

    // Used by XInputEnvironmentPlugin (don't remove!)
    public XInputState() {
        this(nmemAllocChecked(SIZEOF), ByteBuffer.allocate(SIZEOF));
    }

    @Override
    protected @NotNull XInputState create(long address, ByteBuffer container) {
        return new XInputState(address, container);
    }

    @Override
    public int sizeof() { return SIZEOF; }

    public void read(ByteBuffer byteBuffer) {
        this.dwPacketNumber = byteBuffer.getInt();
        this.Gamepad = new XInputGamepad(address, byteBuffer);
    }

    @Override
    public @NotNull String toString() {
        return "XInputState{" +
                "dwPacketNumber=" + dwPacketNumber +
                ", Gamepad=" + Gamepad +
                '}';
    }

}
