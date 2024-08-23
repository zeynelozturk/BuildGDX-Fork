// A.ASM replacement using C
// Mainly by Ken Silverman, with things melded with my port by
// Jonathon Fowler (jonof@edgenetwork.org)
//
// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
// This file has been modified from Ken Silverman's original release
// by Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build.Render.Software;

import ru.m210projects.Build.Gameutils;

import static ru.m210projects.Build.Pragmas.divscale;

public class Ac implements A {

    private final int[] reciptable;
    private int transmode = 0;
    private int gbxinc, gbyinc, glogx, glogy;
    private int gshade;
    private int bpl, gpinc;
    private byte[] gtrans, gbuf, frameplace, gpal, ghlinepal, hlinepal;
    private int bzinc;
    private int asm1, asm2;
    private int hlineshade;

    public Ac() {
        this.reciptable = new int[2048];
        for (int i = 0; i < 2048; i++) {
            reciptable[i] = divscale(2048, i + 2048, 30);
        }
    }

    public static long toUnsignedLong(long x) {
        return (x) & 0xffffffffL;
    }

    @Override
    public int krecipasm(int i) {
        i = Float.floatToIntBits(i);
        return (reciptable[(i >> 12) & 2047] >> (((i - 0x3f800000) >> 23) & 31)) ^ (i >> 31);
    }

    // Global variable functions
    @Override
    public void setvlinebpl(int dabpl) {
        bpl = dabpl;
    }

    @Override
    public void fixtransluscence(byte[] datrans) {
        gtrans = datrans;
    }

    @Override
    public void settransnormal() {
        transmode = 0;
    }

    @Override
    public void settransreverse() {
        transmode = 1;
    }

    // Ceiling/floor horizontal line functions
    @Override
    public void sethlinesizes(int logx, int logy, byte[] bufplc) {
        glogx = logx;
        glogy = logy;
        gbuf = bufplc;
    }

    @Override
    public void setpalookupaddress(byte[] paladdr) {
        ghlinepal = paladdr;
    }

    @Override
    public void setuphlineasm4(int bxinc, int byinc) {
        gbxinc = bxinc;
        gbyinc = byinc;
    }

    @Override
    public void hlineasm4(int cnt, int skiploadincs, int paloffs, int by, int bx, int p) {
        if (skiploadincs == 0) {
            gbxinc = asm1;
            gbyinc = asm2;
        }

        byte[] remap = ghlinepal;
        int shiftx = 32 - glogx;
        int shifty = 32 - glogy;

        int xinc = gbxinc; //it affects on fps
        int yinc = gbyinc;

        try {
            for (; cnt >= 0; cnt--) {
                int index = ((bx >>> shiftx) << glogy) + (by >>> shifty);
                if (index >= gbuf.length) {
                    return;
                }

                drawpixel(p, remap[(gbuf[index] & 0xFF) + paloffs]); //XXX
                bx -= xinc;
                by -= yinc;
                p--;
            }
        } catch (Throwable e) {
        }
    }

    // Sloped ceiling/floor vertical line functions
    @Override
    public void setupslopevlin(int logylogx, byte[] bufplc, int pinc, int bzinc) {
        this.glogx = (logylogx & 255);
        this.glogy = (logylogx >> 8);
        this.gbuf = bufplc;
        this.gpinc = pinc;
        this.bzinc = (bzinc >> 3);
    }

    @Override
    public void slopevlin(int p, byte[] pal, int slopaloffs, int cnt, int bx, int by, int x3, int y3, int[] slopalookup,
                          int bz) {

        int u, v, i, index;
        int shiftx = 32 - glogx;
        int shifty = 32 - glogy;
        int inc = gpinc; // it affects on fps
        int zinc = bzinc;
        final int logy = glogy;
        final byte[] buf = gbuf;

        try {
            for (; cnt > 0; cnt--) {
                i = krecipasm(bz >> 6);
                bz += zinc;
                u = bx + x3 * i;
                v = by + y3 * i;

                index = ((u >>> shiftx) << logy) + (v >>> shifty);
                if (index >= buf.length) {
                    return;
                }

                drawpixel(p, pal[(buf[index] & 0xFF) + slopalookup[slopaloffs]]);
                slopaloffs--;
                p += inc;
            }
        } catch (Throwable e) {
        }
    }

    // Wall,face sprite/wall sprite vertical line functions
    @Override
    public void setupvlineasm(int neglogy) {
        glogy = neglogy;
    }

    @Override
    public void vlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p) {
        vplc = toUnsignedLong(vplc);
        final int pl = bpl; //it affects on fps
        final int logy = glogy;

        try {
            for (; cnt >= 0; cnt--) {
                int index = (int) (bufoffs + (vplc >> logy));
                if (index < 0 || index >= bufplc.length) {
                    // buffofs can be negative
                    return;
                }

                int ch = (bufplc[index] & 0xFF) + shade;
                drawpixel(p, pal[ch]);
                p += pl;
                vplc = toUnsignedLong(vplc + vinc);
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void setupmvlineasm(int neglogy) {
        glogy = neglogy;
    }

    @Override
    public void mvlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p) {
        final int pl = bpl; //it affects on fps
        final int logy = glogy;
        vplc = toUnsignedLong(vplc);
        try {
            for (; cnt >= 0; cnt--) {
                int index = (int) (bufoffs + (vplc >> logy));
                if (index < 0 || index >= bufplc.length) {
                    // buffofs can be negative
                    return;
                }

                int ch = bufplc[index] & 0xFF;
                if (ch != 255) {
                    drawpixel(p, pal[ch + shade]);
                }
                p += pl;
                vplc = toUnsignedLong(vplc + vinc);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void setuptvlineasm(int neglogy) {
        glogy = neglogy;
    }

    // Floor sprite horizontal line functions

    @Override
    public void tvlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p) {
        final int pl = bpl; //it affects on fps
        final int logy = glogy;
        final byte[] trans = gtrans;
        final byte[] frameptr = frameplace;

        vplc = toUnsignedLong(vplc);
        try {
            if (transmode != 0) {
                for (; cnt >= 0; cnt--) {
                    int index = (int) (bufoffs + (vplc >> logy));
                    if (index < 0 || index >= bufplc.length) {
                        // buffofs can be negative
                        return;
                    }

                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        int dacol = pal[ch + shade] & 0xFF;
                        drawpixel(p, trans[(frameptr[p] & 0xFF) + (dacol << 8)]);
                    }
                    p += pl;
                    vplc = toUnsignedLong(vplc + vinc);
                }
            } else {
                for (; cnt >= 0; cnt--) {
                    int index = (int) (bufoffs + (vplc >> logy));
                    if (index < 0 || index >= bufplc.length) {
                        // buffofs can be negative
                        return;
                    }
                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        int dacol = pal[ch + shade] & 0xFF;
                        drawpixel(p, trans[((frameptr[p] & 0xFF) << 8) + dacol]);
                    }
                    p += pl;
                    vplc = toUnsignedLong(vplc + vinc);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void sethlineincs(int x, int y) {
        asm1 = x;
        asm2 = y;
    }

    @Override
    public void setuphline(byte[] pal, int shade) {
        hlinepal = pal;
        hlineshade = shade;
    }

    @Override
    public void msethlineshift(int logx, int logy) {
        glogx = logx;
        glogy = logy;
    }

    @Override
    public void mhline(byte[] bufplc, long bx, int cntup16, int junk, long by, int p) {
        bx = toUnsignedLong(bx);
        by = toUnsignedLong(by);

        byte[] remap = hlinepal;
        int shiftx = 32 - glogx;
        int shifty = 32 - glogy;

        int xinc = asm1; //it affects on fps
        int yinc = asm2;
        int shade = hlineshade;

        try {
            for (cntup16 >>= 16; cntup16 > 0; cntup16--) {
                int index = (int) (((bx >>> shiftx) << glogy) + (by >>> shifty));
                if (index >= bufplc.length) {
                    return;
                }
                int ch = bufplc[index] & 0xFF;
                if (ch != 255) {
                    drawpixel(p, remap[ch + shade]);
                }

                bx = toUnsignedLong(bx + xinc);
                by = toUnsignedLong(by + yinc);
                p++;
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void tsethlineshift(int logx, int logy) {
        glogx = logx;
        glogy = logy;
    }

    @Override
    public void thline(byte[] bufplc, long bx, int cntup16, int junk, long by, int p) {
        bx = toUnsignedLong(bx);
        by = toUnsignedLong(by);

        int dacol;
        byte[] remap = hlinepal;
        int shiftx = 32 - glogx;
        int shifty = 32 - glogy;

        int xinc = asm1; //it affects on fps
        int yinc = asm2;
        int shade = hlineshade;

        try {
            if (transmode != 0) {
                for (cntup16 >>= 16; cntup16 > 0; cntup16--) {
                    int index = (int) (((bx >> shiftx) << glogy) + (by >> shifty));
                    if (index >= bufplc.length) {
                        return;
                    }
                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        dacol = remap[ch + shade] & 0xFF;
                        drawpixel(p, gtrans[(frameplace[p] & 0xFF) + (dacol << 8)]);
                    }
                    bx = toUnsignedLong(bx + xinc);
                    by = toUnsignedLong(by + yinc);
                    p++;
                }
            } else {
                for (cntup16 >>= 16; cntup16 > 0; cntup16--) {
                    int index = (int) (((bx >> shiftx) << glogy) + (by >> shifty));
                    if (index >= bufplc.length) {
                        return;
                    }
                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        dacol = remap[ch + shade] & 0xFF;
                        drawpixel(p, gtrans[((frameplace[p] & 0xFF) << 8) + dacol]);
                    }
                    bx = toUnsignedLong(bx + xinc);
                    by = toUnsignedLong(by + yinc);
                    p++;
                }
            }
        } catch (Throwable e) {
        }
    }

    // Rotatesprite vertical line functions
    @Override
    public void setupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz) {
        gpal = pal;
        gshade = Math.max(0, shade);
        gbxinc = bxinc;
        gbyinc = byinc;
        glogy = ysiz;
    }

    @Override
    public void spritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p) {
        byte[] remap = gpal;
        int xinc = gbxinc; //it affects on fps
        int yinc = gbyinc;
        int pl = bpl;
        int shade = gshade;

        try {
            for (; cnt > 1; cnt--) {
                int index = bufoffs + (bx >> 16) * glogy + (by >> 16);
                if (index < 0 || index >= bufplc.length) {
                    return;
                }

                drawpixel(p, remap[(bufplc[index] & 0xFF) + shade]);

                bx += xinc;
                by += yinc;
                p += pl;
            }
        } catch (Exception ignored) {
        }
    }

    // Rotatesprite vertical line functions
    @Override
    public void msetupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz) {
        gpal = pal;
        gshade = Math.max(0, shade);
        gbxinc = bxinc;
        gbyinc = byinc;
        glogy = ysiz;
    }

    @Override
    public void mspritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p) {
        byte[] remap = gpal;
        int xinc = gbxinc; //it affects on fps
        int yinc = gbyinc;
        int pl = bpl;
        int shade = gshade;

        try {
            for (; cnt > 1; cnt--) {
                int index = bufoffs + (bx >> 16) * glogy + (by >> 16);
                if (index < 0 || index >= bufplc.length) {
                    return;
                }

                int ch = bufplc[index] & 0xFF;
                if (ch != 255) {
                    drawpixel(p, remap[ch + shade]);
                }

                bx += xinc;
                by += yinc;
                p += pl;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void tsetupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz) {
        gpal = pal;
        gshade = Math.max(0, shade);
        gbxinc = bxinc;
        gbyinc = byinc;
        glogy = ysiz;
    }

    @Override
    public void tspritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p) {
        int dacol;
        byte[] remap = gpal;
        int xinc = gbxinc; //it affects on fps
        int yinc = gbyinc;
        int pl = bpl;
        int shade = gshade;
        try {

            if (transmode != 0) {
                for (; cnt > 1; cnt--) {
                    int index = bufoffs + (bx >> 16) * glogy + (by >> 16);
                    if (index < 0 || index >= bufplc.length) {
                        return;
                    }

                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        dacol = remap[ch + shade] & 0xFF;
                        drawpixel(p, gtrans[(frameplace[p] & 0xFF) + (dacol << 8)]);
                    }
                    bx += xinc;
                    by += yinc;
                    p += pl;
                }
            } else {
                for (; cnt > 1; cnt--) {
                    int index = bufoffs + (bx >> 16) * glogy + (by >> 16);
                    if (index < 0 || index >= bufplc.length) {
                        return;
                    }

                    int ch = bufplc[index] & 0xFF;
                    if (ch != 255) {
                        dacol = remap[ch + shade] & 0xFF;
                        drawpixel(p, gtrans[((frameplace[p] & 0xFF) << 8) + dacol]);
                    }
                    bx += xinc;
                    by += yinc;
                    p += pl;
                }
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void setupdrawslab(int dabpl, byte[] pal, int shade, int trans) {
        bpl = dabpl;
        gpal = pal;
        gshade = Math.max(0, shade);
        transmode = trans;
    }

    @Override
    public void drawslab(int dx, int v, int dy, int vi, byte[] data, int vptr, int p) {
        int x;
        int dacol;
        byte[] remap = gpal;
        int pl = bpl;
        int shade = gshade;

        try {
            switch (transmode) {
                case 0:
                    while (dy > 0) {
                        for (x = 0; x < dx; x++) {
                            drawpixel(p + x, remap[(data[(v >>> 16) + vptr] & 0xFF) + shade]);
                        }
                        p += pl;
                        v += vi;
                        dy--;
                    }
                    break;
                case 1:
                    while (dy > 0) {
                        for (x = 0; x < dx; x++) {
                            dacol = remap[(data[(v >>> 16) + vptr] & 0xFF) + shade] & 0xFF;
                            drawpixel(p + x, gtrans[(frameplace[p + x] & 0xFF) + (dacol << 8)]);
                        }
                        p += pl;
                        v += vi;
                        dy--;
                    }
                    break;
                case 2:
                    while (dy > 0) {
                        for (x = 0; x < dx; x++) {
                            dacol = remap[(data[(v >>> 16) + vptr] & 0xFF) + shade] & 0xFF;
                            drawpixel(p + x, gtrans[((frameplace[p + x] & 0xFF) << 8) + dacol]);
                        }
                        p += pl;
                        v += vi;
                        dy--;
                    }
                    break;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void drawpixel(int ptr, byte col) {
        try { //still has crashes
            frameplace[ptr] = col;
        } catch (Exception e) {
        }
    }

    @Override
    public void setframeplace(byte[] newframeplace) {
        this.frameplace = newframeplace;
    }

    @Override
    public byte[] getframeplace() {
        return frameplace;
    }

    @Override
    public void clearframe(byte dacol) {
        Gameutils.fill(frameplace, dacol);
    }
}
