// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.
//
//Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static ru.m210projects.Build.Pragmas.scale;

public class Tables {

    protected final short[] sintable;
    protected final short[] radarang;
    protected final short[] sqrtable;
    protected final short[] shlookup;
    protected final byte[] textfont;
    protected final byte[] smalltextfont;

    public Tables(Entry entry) throws IOException {
        sqrtable = new short[4096];
        shlookup = new short[4096 + 256];
        sintable = new short[2048];
        radarang = new short[1280];
        textfont = new byte[2048];
        smalltextfont = new byte[2048];

        // init sqrt table
        int j = 1, k = 0;
        for (int i = 0; i < 4096; i++) {
            if (i >= j) {
                j <<= 2;
                k++;
            }

            sqrtable[i] = (short) ((int) Math.sqrt(((i << 18) + 131072)) << 1);
            shlookup[i] = (short) ((k << 1) + ((10 - k) << 8));
            if (i < 256) {
                shlookup[i + 4096] = (short) (((k + 6) << 1) + ((10 - (k + 6)) << 8));
            }
        }

        read(entry);
    }

    protected void read(Entry entry) throws IOException {
        if(!entry.exists()) {
            throw new FileNotFoundException("Failed to load \"tables.dat\"!");
        }

        try(InputStream is = entry.getInputStream()) {
            ByteBuffer.wrap(StreamUtils.readBytes(is, sintable.length * 2)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sintable);
            ByteBuffer.wrap(StreamUtils.readBytes(is, radarang.length)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(radarang, 0,  radarang.length / 2);
            StreamUtils.readBytes(is, textfont, 1024);
            StreamUtils.readBytes(is, smalltextfont, 1024);
        }

        for (int i = 0; i < 640; i++) {
            radarang[1279 - i] = (short) -radarang[i];
        }
    }

    public int sin(int angle) {
        return sintable[angle & 2047];
    }

    public int cos(int angle) {
        return sin(angle + 512);
    }

    public int sqrt(int a) {
        long out = a & 0xFFFFFFFFL;
        int value;
        if ((out & 0xFF000000L) != 0) {
            value = shlookup[(int) ((out >> 24) + 4096)] & 0xFFFF;
        } else {
            value = shlookup[(int) (out >> 12)] & 0xFFFF;
        }

        out >>= value & 0xff;
        out = (out & 0xffff0000L) | (sqrtable[(int) out] & 0xFFFF);
        out >>= ((value & 0xff00) >> 8);

        return (int) out;
    }

    public int getAngle(int xvect, int yvect) { // jfBuild
        if ((xvect | yvect) == 0) {
            return (0);
        }

        if (xvect == 0) {
            return (short) (512 + ((yvect < 0 ? 1 : 0) << 10));
        }

        if (yvect == 0) {
            return (short) ((xvect < 0 ? 1 : 0) << 10);
        }

        if (xvect == yvect) {
            return (short) (256 + ((xvect < 0 ? 1 : 0) << 10));
        }

        if (xvect == -yvect) {
            return (short) (768 + ((xvect > 0 ? 1 : 0) << 10));
        }

        if (Math.abs((long) xvect) > Math.abs((long) yvect)) { // GDX 26.11.2021 Integer.MIN issue fix
            return (((radarang[640 + scale(160, yvect, xvect)] >> 6) + ((xvect < 0 ? 1 : 0) << 10)) & 2047);
        }
        return (((radarang[640 - scale(160, xvect, yvect)] >> 6) + 512 + ((yvect < 0 ? 1 : 0) << 10)) & 2047);
    }

    public int getRadarAng(int value) {
        if (value < 0 || value >= radarang.length) {
            return 0;
        }

        return radarang[value];
    }
}
