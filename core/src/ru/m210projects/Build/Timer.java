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

import ru.m210projects.Build.osd.Console;

public class Timer {

//    public ValueChart timerCount = new ValueChart("Count",400, 50, 60, "t")
//            .setPointsCount(500)
//            .setShowAverage(false);
//    public ValueChart smoothRate = new ValueChart("Smoothratio",400, 50, 2.0f, "")
//            .setShowAverage(false);//.setPointsCount(500);

    protected final int timerticspersec;
    protected int totalclock;
    protected int frameTicks;
    protected final float timerskipticks;
    protected float frametime;
    protected float gameTime = 0;
    protected int skipTicks = 0;

    public Timer(int tickspersecond, int frameTicks) {
        Console.out.println("Initializing timer");
        this.totalclock = 0;
        this.frameTicks = frameTicks; // 4
        this.timerticspersec = tickspersecond; // 120
        this.timerskipticks = (1000.0f / timerticspersec) * frameTicks;
        this.frametime = 0;
    }

    public void setSkipTicks(int skipTicks) {
        this.skipTicks = skipTicks;
    }

    public boolean update(float delta, boolean vsync) {
        delta *= 1000.0f; // to ms
//        timerCount.addValue(gameTime);
        gameTime += delta;
        if (delta > timerskipticks) {
            totalclock += frameTicks * (int) (gameTime / timerskipticks);
            gameTime %= timerskipticks;
            frametime = 0;

            return true;
        }

        // In normal cases this update is smoother for interpolation
        // I can't explain this shit...why vsync affects on the timer
        if (gameTime >= (vsync ? (timerskipticks - frameTicks) : timerskipticks)) {
            totalclock += frameTicks;
            gameTime = 0;
            frametime = 0;

            return true;
        }

        if (skipTicks != 0) {
            totalclock += skipTicks * frameTicks;
            return true;
        }

        return false;
    }

    public int getsmoothratio(float deltaTime) {
        float value = (frametime += ((deltaTime * 1000.0f) / timerskipticks) * 65536.0f); // (gameTime / timerskipticks * 65536.0f);

//        smoothRate.addValue(value / 65536.0f);

        return (int) value;
    }

    public int getFrameTicks() {
        return frameTicks;
    }

    @Deprecated
    public int getTicsPerSecond() { // used only by model update animation. Check if it really neccesary
        return timerticspersec;
    }

    public void reset() {
        frametime = 0;
        gameTime = 0;
        totalclock = 0;
        skipTicks = 0;
    }

    public void calcLag(int lag) {
        totalclock -= lag;
    }

    public int getTotalClock() {
        return totalclock;
    }

    public void setTotalClock(int totalclock) {
        this.totalclock = totalclock;
    }

}
