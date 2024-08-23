// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import ru.m210projects.Build.Types.ConvertType;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static ru.m210projects.Build.Engine.MAXSTATUS;
import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.Pragmas.scale;

public class Gameutils {

    public static final float degreesToBuildAngle = 1024.0f / 180.0f;
    public static final float radiansToBuildAngle = 1024.0f / MathUtils.PI;

    public static final float buildAngleToDegrees = 180.0f / 1024.0f;
    public static final float buildAngleToRadians = MathUtils.PI / 1024.0f;

    public static void fill(byte[] array, int value) {
        int len = array.length;
        if (len > 0) {
            array[0] = (byte) value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, Math.min((len - i), i));
        }
    }

    public static void fill(byte[] array, int start, int end, int value) {
        if (array.length > 0) {
            array[start] = (byte) value;
        }

        int len = end - start;

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, start, array, start + i, Math.min((len - i), i));
        }
    }

    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x *= (1.5f - xhalf * x * x);
        return x;
    }

    public static float Sqrt(float x) {
        return x > 0.0f ? x * invSqrt(x) : 0.0f;
    }

    public static double angle(double dx, double dy, boolean deg) {
        double k = deg ? 90.0 : Math.PI / 2.0;
        if (dy >= 0.0) {
            return k * ((dx >= 0 ? dy / (dx + dy) : 1 - dx / (-dx + dy)));
        }
        return k * ((dx < 0 ? 2 - dy / (-dx - dy) : 3 + dx / (dx - dy)));
    }

    public static float BClampAngle(float angle) {
        return angle < 0 ? (angle % 2048) + 2048 : angle % 2048;
    }

    public static float BClipRange(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);

    }

    public static int BClipRange(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);

    }

    public static short BClipLow(short value, short min) {
        return value < min ? min : value;
    }

    public static short BClipHigh(short value, short max) {
        return value > max ? max : value;
    }

    public static int BClipLow(int value, int min) {
        return Math.max(value, min);
    }

    public static int BClipHigh(int value, int max) {
        return Math.min(value, max);
    }

    public static float BClipLow(float value, int min) {
        return value < min ? min : value;
    }

    public static float BClipHigh(float value, int max) {
        return value > max ? max : value;
    }

    public static double BSinAngle(double daang) {
        double rad_ang = daang * Math.PI * (1.0 / 1024.0);
        return (Math.sin(rad_ang) * 16384.0);
    }

    public static double BCosAngle(double daang) {
        double rad_ang = daang * Math.PI * (1.0 / 1024.0);
        return (Math.cos(rad_ang) * 16384.0);
    }

    public static double AngleToDegrees(double ang) {
        return (ang * 360) * (1.0 / 2048.0);
    }

    public static double AngleToRadians(double ang) {
        return (ang * Math.PI) * (1.0 / 1024.0);
    }

    public static boolean isValidStat(int i) {
        return i >= 0 && i <= MAXSTATUS;
    }

    public static boolean isValidTile(int tile) {
        return tile >= 0 && tile < MAXTILES;
    }

    public static ConvertType getType(int nFlags) {
        ConvertType type = ConvertType.Normal;
        if ((nFlags & 256) != 0) type = ConvertType.AlignLeft;
        if ((nFlags & 512) != 0) type = ConvertType.AlignRight;
        if ((nFlags & 1024) != 0) type = ConvertType.Stretch;

        return type;
    }

    public static <T> int arrayIndexOf(T[] array, IntPredicate predicate) {
        return IntStream.range(0, array.length).filter(predicate).findAny().orElse(-1);
    }

    public static <T> int listIndexOf(List<T> list, IntPredicate predicate) {
        return IntStream.range(0, list.size()).filter(predicate).findAny().orElse(-1);
    }

    public static int coordsConvertXScaled(int coord, ConvertType type) {
        int ydim = Gdx.graphics.getHeight();
        int oxdim = Gdx.graphics.getWidth();

        int xdim = (4 * ydim) / 3;
        if (4 * oxdim / 5 == ydim) // 1280 : 1024
        {
            xdim = (5 * ydim) / 4;
        }

        int offset = oxdim - xdim;
        int buildim = 320;
        if (type == ConvertType.Stretch) {
            buildim = buildim * xdim / oxdim;
        }

        int normxofs = coord - (buildim << 15);
        int wx = (xdim << 15) + scale(normxofs, xdim, buildim);

        if (type == ConvertType.Stretch) {
            return wx - 1;
        }

        wx += (oxdim - xdim) / 2;

        if (type == ConvertType.AlignLeft) {
            return wx - offset / 2 - 1;
        }
        if (type == ConvertType.AlignRight) {
            return wx + offset / 2 - 1;
        }

        return wx - 1;
    }

    public static float convertSquareResolutionScale(float scale) {
        int xdim = (4 * Gdx.graphics.getHeight()) / 3;
        return (scale * xdim) / 320.0f;
    }

    public static int coordsConvertYScaled(/*Renderer renderer, */int coord) {
        int oydim = Math.max(1, Gdx.graphics.getHeight());
        int ydim = (3 * Math.max(1, Gdx.graphics.getWidth())) / 4;
        int buildim = Math.max(1, 200 * ydim / oydim);
        int normxofs = coord - (buildim << 15);

        return (ydim << 15) + scale(normxofs, ydim, buildim);
    }

    public static boolean isSquareResolution(int width, int height) {
        return (3 * width / 4) == height || (4 * width / 5) == height;
    }
}
