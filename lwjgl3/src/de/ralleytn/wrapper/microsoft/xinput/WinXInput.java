package de.ralleytn.wrapper.microsoft.xinput;

import com.badlogic.gdx.utils.Architecture;
import org.lwjgl.system.Library;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.SharedLibrary;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.badlogic.gdx.utils.SharedLibraryLoader.bitness;
import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;

public class WinXInput implements XInput {

    private static SharedLibrary XINPUT_LIB;

    private static SharedLibrary getInstance(String dllName) {
        if (XINPUT_LIB == null) {
            XINPUT_LIB = Library.loadNative(XInput.class, "org.lwjgl", dllName);
        }
        return XINPUT_LIB;
    }

    public WinXInput() {
        if (XINPUT_LIB != null) {
            return;
        }

        final String[] dlls = {
                "xinput1_4.dll",
                "xinput1_3.dll",
                "xinput9_1_0.dll"
        };

        String directory = System.getProperty("os.arch.dir");
        if (directory == null) {
            directory = "c:\\windows";
        }

        Path path = Paths.get(directory, bitness == Architecture.Bitness._64 ? "SysWOW64" : "System32");
        for (String dll : dlls) {
            if (Files.exists(path.resolve(dll))) {
                XINPUT_LIB = getInstance(dll);
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static final class Functions {

        private Functions() {
        }

        private static final long
                XInputEnable = getFunctionAddress("XInputEnable"),
                XInputGetCapabilities = getFunctionAddress("XInputGetCapabilities"),
                XInputGetBatteryInformation = getFunctionAddress("XInputGetBatteryInformation"),
                XInputGetState = getFunctionAddress("XInputGetState"),
                XInputSetState = getFunctionAddress("XInputSetState");

        private static long getFunctionAddress(String funcName) {
            try {
                return apiGetFunctionAddress(XINPUT_LIB, funcName);
            } catch (Exception e) {
                Console.out.println(e.getMessage(), OsdColor.RED);
            }
            return -1;
        }
    }

    @Override
    public void XInputEnable(@NativeType("BOOL") boolean enable) {
        long __functionAddress = Functions.XInputEnable;
        callV(enable, __functionAddress);
    }

    @Override
    public int XInputGetCapabilities(int dwUserIndex, int dwFlags, XInputCapabilities pCapabilities) {
        long __functionAddress = Functions.XInputGetCapabilities;
        int out = callPI(dwUserIndex, dwFlags, pCapabilities.address(), __functionAddress);
        pCapabilities.read(memByteBuffer(pCapabilities.address(), XInputCapabilities.SIZEOF));
        return out;
    }

    @Override
    public int XInputGetBatteryInformation(int dwUserIndex, int devType, XInputBatteryInformation pBatteryInformation) {
        long __functionAddress = WinXInput.Functions.XInputGetBatteryInformation;
        int out = callPI(dwUserIndex, devType, pBatteryInformation.address(), __functionAddress);
        pBatteryInformation.read(memByteBuffer(pBatteryInformation.address(), XInputBatteryInformation.SIZEOF));
        return out;
    }

    @Override
    public int XInputGetState(int dwUserIndex, @NativeType("XInputState") XInputState pState) {
        long __functionAddress = WinXInput.Functions.XInputGetState;
        int out = callPI(dwUserIndex, pState.address(), __functionAddress);
        pState.read(memByteBuffer(pState.address(), XInputState.SIZEOF));
        return out;
    }

    @Override
    public int XInputSetState(int dwUserIndex, XInputVibration pVibration) {
        long __functionAddress = Functions.XInputSetState;
        return callPI(dwUserIndex, pVibration.address(), __functionAddress);
    }
}
