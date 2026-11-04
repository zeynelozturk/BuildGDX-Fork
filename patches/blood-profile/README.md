# Blood multiplayer profile packet fix

`src/ru/m210projects/Blood/Factory/BloodNetwork.java` is the matching upstream
BloodGDX v1.18 source, with one behavioral correction in `InitProfile`: clear
`PROFILE.sizeof + 2` bytes, the full transmitted packet, rather than
`PROFILE.sizeof + 1`. The final nickname byte otherwise retains previous packet
data. For short names this can leave embedded NUL padding before a stale byte,
causing received chat to stop displaying before the colon and message.

Upstream source (GPL-3.0-or-later, original header retained):
https://gitlab.com/m210/BloodGDX/-/blob/6a2e22d8044d080664a8fc8ee213c2d9bda879a4/core/src/ru/m210projects/Blood/Factory/BloodNetwork.java

This shared-engine repository does not otherwise contain Blood game sources.
`build-tr-locale-fixed.bat` compiles this file separately against the base JAR,
packages its class, and runs `tests/BloodProfilePacketTest.java` against the
output JAR. Packet size, layout, and network version are unchanged.

The regression covers every stale byte value and ASCII nickname length 1–14,
checks the profile header and visible chat length, and checks actual chat-buffer
reuse before a profile resend. It does not replace live multiplayer validation.
