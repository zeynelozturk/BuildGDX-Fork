package de.ralleytn.wrapper.microsoft.xinput;

import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

public interface XInput {

    static XInput create() {
        try {
            if (SharedLibraryLoader.os == Os.Windows) {
                return new WinXInput();
            }
        } catch (Exception ignored) {
        }
        Console.out.println("XInput isn't supported", OsdColor.RED);
        return null;
    }

    void XInputEnable(boolean enable);

    int XInputGetCapabilities(int dwUserIndex, int dwFlags, XInputCapabilities pCapabilities);

    int XInputGetBatteryInformation(int dwUserIndex, int devType, XInputBatteryInformation pBatteryInformation);

    int XInputGetState(int dwUserIndex, XInputState pState);

    int XInputSetState(int dwUserIndex, XInputVibration pVibration);

}

