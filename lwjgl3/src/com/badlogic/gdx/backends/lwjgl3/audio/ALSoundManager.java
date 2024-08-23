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

import com.badlogic.gdx.utils.IntMap;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.SOFTEventProc;
import org.lwjgl.openal.SOFTEvents;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.Architecture.common.audio.LoopInfo;
import ru.m210projects.Build.Architecture.common.audio.SoundManager;
import ru.m210projects.Build.Architecture.common.audio.Source;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.SOFTEvents.AL_EVENT_TYPE_SOURCE_STATE_CHANGED_SOFT;

public class ALSoundManager extends SoundManager {

    private final IntMap<Integer> soundIdToSourceIndex;
    private final OpenALAudio audio;

    public ALSoundManager(OpenALAudio audio, int sourceCount) {
        this.audio = audio;
        init(sourceCount);

        this.soundIdToSourceIndex = new IntMap<>();
        for (Source allSource : allSources) {
            ALSource alSource = (ALSource) allSource;
            soundIdToSourceIndex.put(alSource.soundId, alSource.sourceIndex);
        }

        if (AL.getCapabilities().AL_SOFT_events) {
            SOFTEvents.alEventControlSOFT(new int[]{AL_EVENT_TYPE_SOURCE_STATE_CHANGED_SOFT}, true);
            SOFTEvents.alEventCallbackSOFT(new SOFTEventProc() {
                @Override
                public void invoke(int eventType, int soundId, int param, int length, long message, long userParam) {
                    if (eventType == AL_EVENT_TYPE_SOURCE_STATE_CHANGED_SOFT) {
                        switch (alGetSourcei(soundId, AL_SOURCE_STATE)) {
                            case AL_PLAYING: {
                                break;
                            }
                            case AL_STOPPED: {
                                ALSource source = (ALSource) allSources[soundIdToSourceIndex.get(soundId)];

                                LoopInfo loopInfo = source.loopInfo;
                                if (loopInfo.looped && alGetSourcef(source.soundId, AL_GAIN) > 0.0f) {
                                    int bufferID = alSourceUnqueueBuffers(source.soundId);
                                    alBufferData(bufferID, loopInfo.format, loopInfo.getData(), loopInfo.sampleRate);
                                    alSourceQueueBuffers(source.soundId, bufferID);
                                    alSourcei(source.soundId, AL_LOOPING, AL_TRUE);
                                    alSourcePlay(source.soundId);
                                } else {
                                    synchronized (sourcesToFree) {
                                        sourcesToFree.add(source.sourceIndex);
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }, null);
        }
    }

    public void update() {
        super.update();
//        System.out.println("sources: ");
//        int frees = 0;
//        for(ManageableSource source : allSources) {
//            System.out.println(source.getIndex() + ": free[" + isFreeSource(source.getIndex()) + "] playing " + source.isPlaying());
//            if (isFreeSource(source.getIndex())) {
//                frees++;
//            }
//        }
//        System.out.println("Free sources " + frees);
    }

    public ALSource getSource(int sourceIndex) {
        return (ALSource) allSources[sourceIndex];
    }

    @Override
    protected ManageableSource generateSource(int sourceIndex) {
        try {
            return new ALSource(audio, sourceIndex);
        } catch (InitializationException e) {
            Console.out.println(e.toString(), OsdColor.RED);
            return null;
        }
    }

}
