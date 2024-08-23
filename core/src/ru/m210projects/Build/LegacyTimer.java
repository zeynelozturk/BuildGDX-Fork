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

public class LegacyTimer extends Timer {

    protected final int timerfreq;
    protected long timerlastsample;

    public LegacyTimer(int tickspersecond, int frameTicks) {
        super(tickspersecond, frameTicks);
        this.timerfreq = 1000;
        this.timerlastsample = System.nanoTime() * timerticspersec / (timerfreq * 1000000L);
    }

    @Override
    public boolean update(float delta, boolean ignored2) {
//        timerCount.addValue(gameTime);
//        gameTime += (delta * 1000.0f);
//        gameTime %= timerskipticks;

        long n = (System.nanoTime() * timerticspersec / (timerfreq * 1000000L)) - timerlastsample;
        if (n > 0) {
            if (skipTicks != 0) {
                totalclock += skipTicks * 4;
            } else {
                totalclock += (int) n;
            }
            timerlastsample += n;
            return true;
        }
        return false;
    }

    public void resetsmoothticks() {
        frametime = 0.0f;
    }

    @Override
    public int getFrameTicks() {
        resetsmoothticks();
        return super.getFrameTicks();
    }

    @Override
    public int getsmoothratio(float deltaTime) {
        float value = ((frametime += deltaTime * 1000.0f * 65536.0f) / timerskipticks);

//        smoothRate.addValue(value / 65536.0f);

        return (int) value;
    }
}
