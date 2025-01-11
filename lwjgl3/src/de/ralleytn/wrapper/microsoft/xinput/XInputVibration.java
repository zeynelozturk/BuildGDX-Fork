package de.ralleytn.wrapper.microsoft.xinput;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class XInputVibration extends Struct<XInputVibration> implements NativeResource {

    public static final int SIZEOF;

    public static final int ALIGNOF;

    public static final int
            LEFT_MOTOR_SPEED,
            RIGHT_MOTOR_SPEED;

    static {
        Layout layout = __struct(
                __member(2),
                __member(2)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        LEFT_MOTOR_SPEED = layout.offsetof(0);
        RIGHT_MOTOR_SPEED = layout.offsetof(1);
    }

    public int wLeftMotorSpeed;
    public int wRightMotorSpeed;

    protected XInputVibration(long address, ByteBuffer container) {
        super(address, container);

        if (container != null) {
            wLeftMotorSpeed = container.getShort();
            wRightMotorSpeed = container.getShort();
        }
    }

    // Used by XInputEnvironmentPlugin (don't remove!)
    public XInputVibration() {
        this(nmemAllocChecked(SIZEOF), ByteBuffer.allocate(SIZEOF));
    }

    @Override
    protected @NotNull XInputVibration create(long address, ByteBuffer container) {
        return new XInputVibration(address, container);
    }

    @Override
    public int sizeof() { return SIZEOF; }

}
