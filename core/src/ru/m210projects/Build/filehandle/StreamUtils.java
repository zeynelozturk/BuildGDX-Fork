// This file is part of BuildGDX.
// Copyright (C) 2023-2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// BuildGDX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// BuildGDX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.filehandle;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class StreamUtils {

    public static String readString(InputStream in, int length) throws IOException {
        byte[] buf = readBytes(in, length);
        for (length = 0; length < buf.length; length++) {
            if(buf[length] == 0) {
                break;
            }
        }
        return new String(buf, 0, length);
    }

    public static String readDataString(InputStream in) throws IOException {
        int length = readInt(in);
        if (length == 0) {
            return "";
        }

        if (length > in.available()) {
            throw new EOFException();
        }

        byte[] buf = readBytes(in, length);
        return new String(buf, 0, length);
    }

    public static int readUnsignedByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    public static byte readByte(InputStream in) throws IOException {
        return (byte) readUnsignedByte(in);
    }

    public static boolean readBoolean(InputStream in) throws IOException {
        return in.read() == 1;
    }
    public static float readFloat(InputStream in) throws IOException {
        return Float.intBitsToFloat(readInt(in));
    }

    public static long readLong(InputStream in) throws IOException {
        long ch1 = in.read();
        long ch2 = in.read();
        long ch3 = in.read();
        long ch4 = in.read();
        long ch5 = in.read();
        long ch6 = in.read();
        long ch7 = in.read();
        long ch8 = in.read();

        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }

        return (ch1 | (ch2 << 8) | (ch3 << 16) | (ch4 << 24) | (ch5 << 32) | (ch6 << 40) | (ch7 << 48) | (ch8 << 56));
    }

    public static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 | (ch2 << 8) | (ch3 << 16) | (ch4 << 24));
    }

    public static int readUnsignedShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 | (ch2 << 8));
    }

    public static short readShort(InputStream in) throws IOException {
        return (short) readUnsignedShort(in);
    }

    public static byte[] readBytes(InputStream in, int len) throws IOException {
        return readBytes(in, new byte[len]);
    }

    public static byte[] readBytes(InputStream in, byte[] data, int len) throws IOException {
        int pos = 0;
        while (len > 0) {
            int l = in.read(data, pos, len);
            if (l == -1) {
                throw new EOFException();
            }
            len -= l;
            pos += l;
        }
        return data;
    }

    public static byte[] readBytes(InputStream in, byte[] data) throws IOException {
        return readBytes(in, data, data.length);
    }

    public static int readBuffer(InputStream is, ByteBuffer buffer) throws IOException {
        int len = 0;
        int remaining = buffer.remaining();
        byte[] data = new byte[8192];
        while (remaining > 0) {
            int l = is.read(data, 0, Math.min(remaining, 8192));
            if (l > 0) {
                buffer.put(data, 0, l);
                remaining -= l;
                len += l;
            } else {
                break;
            }
        }
        buffer.rewind();
        return len;
    }

    public static void skip(InputStream in, int n) throws IOException {
        while (n > 0) {
            long i = in.skip(n);
            if (i == 0) {
                throw new EOFException();
            }
            n -= (int) i;
        }
    }

    public static void writeByte(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
    }

    public static void writeInt(OutputStream out, long v) throws IOException {
        out.write((int) (v & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
    }

    public static void writeShort(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
    }

    public static void writeLong(OutputStream out, long v) throws IOException {
        out.write((int) (v & 0xFF));
        out.write((int) ((v >>> 8) & 0xFF));
        out.write((int) ((v >>> 16) & 0xFF));
        out.write((int) ((v >>> 24) & 0xFF));
        out.write((int) ((v >>> 32) & 0xFF));
        out.write((int) ((v >>> 40) & 0xFF));
        out.write((int) ((v >>> 48) & 0xFF));
        out.write((int) ((v >>> 56) & 0xFF));
    }

    /**
     * Saves length of String value to int field and String bytes to output stream
     * @param out Output stream
     * @param v - String value
     */
    public static void writeDataString(OutputStream out, String v) throws IOException {
        writeInt(out, v != null ? v.length() : 0);
        if (v != null && !v.isEmpty()) {
            writeString(out, v);
        }
    }

    /**
     * Saves String bytes to output stream
     * @param out Output stream
     * @param v - String value
     */
    public static void writeString(OutputStream out, String v) throws IOException {
        out.write(v.getBytes());
    }

    public static void writeString(OutputStream out, String v, int len) throws IOException {
        if (v == null) {
            writeBytes(out, new byte[len], len);
            return;
        }
        writeBytes(out, v.getBytes(), len);
    }

    public static void writeBytes(OutputStream out, byte[] data) throws IOException {
        out.write(data);
    }

    public static void writeFloat(OutputStream os, float v) throws IOException {
        writeInt(os, Float.floatToRawIntBits(v));
    }

    public static void writeBytes(OutputStream out, byte[] data, int len) throws IOException {
        final byte[] buf = new byte[len];
        System.arraycopy(data, 0, buf, 0, Math.min(len, data.length));
        out.write(buf);
    }

    public static void writeBoolean(OutputStream out, boolean val) throws IOException {
        out.write(val ? 1 : 0);
    }

    public static void seek(FileOutputStream out, int n) throws IOException {
        FileChannel channel = out.getChannel();
        channel.position(n);
    }
}
