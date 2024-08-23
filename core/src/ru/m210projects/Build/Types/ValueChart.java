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

package ru.m210projects.Build.Types;

import com.badlogic.gdx.utils.FloatArray;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

public class ValueChart {

    private final FloatArray valueList = new FloatArray();
    private final Font font = EngineUtils.getLargeFont();
    private final StringBuilder stringBuilder = new StringBuilder();
    private final char[] charBuffer = new char[32];

    private final int width;
    private final int height;
    private final String name;
    private final String measUnit;
    private float minValue = Float.MAX_VALUE;
    private float maxValue = Float.MIN_VALUE;
    private float averageValue = 0;
    private float maxAllowedValue;
    private int pointsCount;
    private int chartColor = 31;
    private boolean showAverage = true;

    public ValueChart(String name, int width, int height, float maxAllowedValue, String measUnit) {
        this.width = width;
        this.height = height;
        this.name = name;
        this.maxAllowedValue = maxAllowedValue;
        this.measUnit = measUnit;
        this.pointsCount = height;
    }

    public ValueChart setPointsCount(int pointsCount) {
        this.pointsCount = pointsCount;
        return this;
    }

    public void addValue(float value) {
        if (valueList.size >= pointsCount) {
            float removedValue = valueList.removeIndex(0);
            if (removedValue == minValue || removedValue == maxValue) {
                recalculateMinMax();
            }
        }
        updateMinMax(value);
        valueList.add(value);

        if (showAverage) {
            averageValue = 0;
            for (int i = 0; i < valueList.size; i++) {
                averageValue += valueList.items[i];
            }
            averageValue /= valueList.size;
        }
    }

    public void render(Renderer renderer, int chartX, int chartY) {
        font.drawText(renderer, chartX, chartY, name, 1.0f, 0, chartColor, TextAlign.Left, Transparent.None, false);

        float pointWidth = (float) width / pointsCount;
        int y = chartY + height;
        for (int i = 1; i < valueList.size; i++) {
            if (valueList.items[i] <= maxAllowedValue && valueList.items[i - 1] <= maxAllowedValue) {
                int y1 = y - scaleValue(valueList.items[i]);
                int y2 = y - scaleValue(valueList.items[i - 1]);
                renderer.drawline256((int) ((chartX + (i - 1) * pointWidth) * 4096), y2 * 4096, (int) ((chartX + i * pointWidth) * 4096), y1 * 4096, chartColor);
            }
        }

        drawMinMax(renderer, chartX + width + 5, chartY);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private int scaleValue(float value) {
        return (int) ((value / maxAllowedValue) * height);
    }

    private void drawMinMax(Renderer renderer, int chartX, int chartY) {
        font.drawText(renderer, chartX, chartY, buildString("Min: ", minValue), 1.0f, 0, chartColor, TextAlign.Left, Transparent.None, false);
        font.drawText(renderer, chartX, chartY + font.getSize() + 2, buildString("Max: ", maxValue), 1.0f, 0, chartColor, TextAlign.Left, Transparent.None, false);
        if (showAverage) {
            font.drawText(renderer, chartX, chartY + 2 * font.getSize() + 4, buildString("Avg: ", averageValue), 1.0f, 0, chartColor, TextAlign.Left, Transparent.None, false);
        }
    }

    private char[] buildString(String name, float value) {
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append(name).append(Math.round(value * 10) / 10.0).append(measUnit);
        stringBuilder.getChars(0, stringBuilder.length(), charBuffer, 0);
        charBuffer[stringBuilder.length()] = 0;
        return charBuffer;
    }

    private void updateMinMax(float value) {
        if (value < minValue) {
            minValue = value;
        }

        if (value > maxValue) {
            maxValue = value;
//            if (maxValue > maxAllowedValue) {
//                maxAllowedValue = maxValue * 1.5f;
//            }
        }
    }

    public ValueChart setChartColor(int chartColor) {
        this.chartColor = chartColor;
        return this;
    }

    public ValueChart setShowAverage(boolean showAverage) {
        this.showAverage = showAverage;
        return this;
    }

    private void recalculateMinMax() {
        minValue = maxValue = valueList.first();
        for (int i = 0; i < valueList.size; i++) {
            float value = valueList.items[i];
            if (value < minValue) {
                minValue = value;
            }

            if (value > maxValue) {
                maxValue = value;
            }
        }
    }
}