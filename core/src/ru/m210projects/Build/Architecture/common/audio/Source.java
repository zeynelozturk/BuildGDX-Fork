// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Architecture.common.audio;

import com.badlogic.gdx.audio.Sound;

public interface Source extends Sound  {

    int loop(float volume, int from, int to);
    void setPosition(int x, int y, int z);
    void setVolume(float volume);
    void setPriority(int priority);
    void setPitch(float pitch);

    int getPriority();
    boolean isActive();
    boolean isPlaying();
    boolean isGlobal();

    void setListener(SourceListener callback);

    @Override
    default long loop(float volume, float pitch, float pan) {
        // while we're using surround sound, the pan is not supported
        throw new UnsupportedOperationException();
    }

    @Override
    default long play(float volume, float pitch, float pan) {
        // while we're using surround sound, the pan is not supported
        throw new UnsupportedOperationException();
    }


    // stupidest things

    @Override
    default void stop(long sourceIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void pause(long sourceIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void resume(long sourceIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setVolume(long sourceIndex, float volume) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setPan(long sourceIndex, float pan, float volume) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setLooping(long sourceIndex, boolean looping) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setPitch(long sourceIndex, float pitch) {
        throw new UnsupportedOperationException();
    }

    @Override
    void dispose();
}
