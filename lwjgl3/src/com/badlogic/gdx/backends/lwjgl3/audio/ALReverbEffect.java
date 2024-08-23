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

package com.badlogic.gdx.backends.lwjgl3.audio;

import org.lwjgl.openal.EXTEfx;

import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL11.alSource3i;
import static ru.m210projects.Build.Gameutils.BClipRange;

public class ALReverbEffect {

    private int alEffectSlot = -1;
    private int alEffect = -1;

    private float delay;
    private boolean enabled;

    public ALReverbEffect() {
        this.alEffectSlot = EXTEfx.alGenAuxiliaryEffectSlots();
        this.alEffect = EXTEfx.alGenEffects();
        if (alGetError() == AL_NO_ERROR) {
            EXTEfx.alEffecti(alEffect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_CHORUS);
        }
    }

    public float getDelay() {
        return delay;
    }

    public void setReverb(boolean enabled, float delay) {
        this.enabled = enabled;
        if (enabled) {
            this.delay = BClipRange(delay, 0.0f, 10.0f);
        } else {
            this.delay = 0;
        }
    }

    public void setToSource(int soundId) {
        if (!isEFXSupport()) {
            return;
        }

        if (enabled) {
//			EXTEfx.alEffecti(alEffect, EXTEfx.AL_CHORUS_WAVEFORM, 0);
            EXTEfx.alEffectf(alEffect, EXTEfx.AL_CHORUS_RATE, 0);
            EXTEfx.alEffectf(alEffect, EXTEfx.AL_CHORUS_FEEDBACK, delay);
            EXTEfx.alEffectf(alEffect, EXTEfx.AL_CHORUS_DEPTH, 0.0f);
            EXTEfx.alEffectf(alEffect, EXTEfx.AL_CHORUS_DELAY, 0.012f);
            EXTEfx.alAuxiliaryEffectSloti(alEffectSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, alEffect);
            alSource3i(soundId, EXTEfx.AL_AUXILIARY_SEND_FILTER, alEffectSlot, 0, EXTEfx.AL_FILTER_NULL);
        } else {
            EXTEfx.alAuxiliaryEffectSloti(alEffectSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, EXTEfx.AL_EFFECT_NULL);
            alSource3i(soundId, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
        }
    }

    public boolean isEFXSupport() {
        return alEffect != -1;
    }

    public void dispose() {
        if (isEFXSupport()) {
            EXTEfx.alDeleteEffects(alEffect);
            EXTEfx.alDeleteAuxiliaryEffectSlots(alEffectSlot);
        }
    }
}
