import java.lang.reflect.Field;
import java.util.Arrays;

import ru.m210projects.Blood.PROFILE;
import ru.m210projects.Blood.Factory.BloodNetwork;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.net.Mmulti;
import sun.misc.Unsafe;

/** Regression test against the actual BloodNetwork class packaged in the JAR. */
public final class BloodProfilePacketTest {
    public static final class CaptureNetwork extends BloodNetwork {
        byte[] sent;

        public CaptureNetwork() { super(null); }

        @Override
        public void sendtoall(byte[] packet, int length) {
            sent = Arrays.copyOf(packet, length);
        }
    }

    public static void main(String[] args) throws Exception {
        // Skip game startup; initialize only the state used by InitProfile.
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        CaptureNetwork net = (CaptureNetwork) unsafe.allocateInstance(CaptureNetwork.class);
        net.packbuf = new byte[1024];
        net.gProfile = new PROFILE[1];
        Mmulti.numplayers = 2;
        Mmulti.myconnectindex = 0;
        Font font = new Font();
        int checks = 0;

        for (int length = 1; length <= 14; length++) {
            String name = "thewinner12345".substring(0, length);
            net.gProfile[0] = new PROFILE(name, true, false);
            for (int stale = 0; stale <= 255; stale++) {
                Arrays.fill(net.packbuf, (byte) stale);
                net.InitProfile(0);
                String received = new String(net.sent, 6, 14).trim();
                if (net.sent.length != 20 || !received.equals(name)) {
                    throw new AssertionError("Corrupted name: length=" + length + ", stale=" + stale);
                }
                if (net.sent[0] != (byte) 251 || net.sent[1] != 0 || net.sent[2] != 1
                        || net.sent[3] != 0 || net.sent[4] != 2 || net.sent[5] != 0) {
                    throw new AssertionError("Profile header changed");
                }
                String chat = received + " :hello";
                if (font.getWidth(chat, 1.0f) != chat.length()) {
                    throw new AssertionError("Chat truncated");
                }
                checks++;
            }
        }

        // Reproduce the observed H using the real chat writer, then resend a profile.
        net.gProfile[0] = new PROFILE("thewinner", true, false);
        net.SendNetMessage("1234567890123H");
        net.InitProfile(0);
        if (!new String(net.sent, 6, 14).trim().equals("thewinner")) {
            throw new AssertionError("Earlier chat contaminated the profile");
        }
        System.out.println("Blood profile packet regression passed: " + checks
                + " length/stale-byte cases plus chat-to-profile reuse.");
    }
}
