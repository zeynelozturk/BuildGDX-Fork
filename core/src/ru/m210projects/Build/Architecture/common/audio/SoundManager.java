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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public abstract class SoundManager implements Runnable {

    public static final int FREE_SOURCE_PRIORITY = -1;
    /**
     * Array to free sources from non-main thread
     */
    protected final Array<Integer> sourcesToFree = new Array<>(false, 16, Integer.class);
    protected ManageableSource[] allSources;
    protected PriorityQueue<Integer> priorityQueue;

    protected void init(int sourceCount) {
        this.priorityQueue = new PriorityQueue<>(sourceCount, new SourceComparator());
        List<ManageableSource> sources = new ArrayList<>();
        for (int i = 0; i < sourceCount; i++) {
            ManageableSource source = generateSource(i);
            if (source == null) {
                break;
            }
            sources.add(source);
        }

        this.allSources = new ManageableSource[sources.size()];
        sources.toArray(allSources);
        for (ManageableSource s : sources) {
            priorityQueue.add(s.getIndex());
        }
    }

    public Source getSource(int sourceIndex) {
        return allSources[sourceIndex];
    }

    protected abstract ManageableSource generateSource(int soundIndex);

    public int getSize() {
        return allSources.length;
    }

    /**
     * Should be called from game thread!
     * @param sourceIndex index in manager
     */
    public void freeSource(int sourceIndex) {
        if (!isFreeSource(sourceIndex) && priorityQueue.remove(sourceIndex)) {
            ManageableSource source = allSources[sourceIndex];
            source.reset();
            priorityQueue.add(sourceIndex);
        }
    }

    public int element() {
        Integer index = priorityQueue.peek();
        if (index != null) {
            return index;
        }
        return -1;
    }

    public void setMusicSource(ManageableSource source, boolean music) {
        source.setMusicSource(music);
        if (music) {
            priorityQueue.remove(source.getIndex());
        } else {
            priorityQueue.add(source.getIndex());
        }
    }

    /**
     * @param priority sound priority
     * @return source index in manager
     */
    public synchronized int obtainSource(int priority) {
        if (allSources.length == 0) {
            return -1;
        }

        int sourceIndex = priorityQueue.element();
        ManageableSource source = allSources[sourceIndex];
        if (source.getPriority() < priority) {
            priorityQueue.remove(sourceIndex);
            if (source.isPlaying()) {
                source.stop();
            }
            source.reset();
            source.setPriority(priority);
            priorityQueue.add(sourceIndex);
            return sourceIndex;
        }

        return -1;
    }

    public void stopAllSounds() {
        for (ManageableSource source : allSources) {
            if (!source.isMusicSource()) {
                source.stop();
            }
        }
    }

    public void dispose() {
        for (Source allSource : allSources) {
            allSource.dispose();
        }
        allSources = new ManageableSource[0];
        priorityQueue.clear();
    }

    public boolean isFreeSource(int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= allSources.length) {
            return false; // #GDX 31.12.2024
        }

        ManageableSource source = allSources[sourceIndex];
        return source.getPriority() <= FREE_SOURCE_PRIORITY;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (ManageableSource source : allSources) {
            result.append(source.getIndex()).append(" free[").append(isFreeSource(source.getIndex())).append("], ");
        }
        return result.toString();
    }

    @Override
    public void run() {
        synchronized (sourcesToFree) {
            for (int i = 0; i < sourcesToFree.size; i++) {
                freeSource(sourcesToFree.items[i]);
            }
            sourcesToFree.clear();
        }
    }

    public void update() {
        if (!sourcesToFree.isEmpty()) {
            Gdx.app.postRunnable(this);
        }
    }

    public interface ManageableSource extends Source {

        void reset();

        int getIndex();

        boolean isMusicSource();

        void setMusicSource(boolean musicSource);

    }

    public class SourceComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            ManageableSource s1 = allSources[o1];
            ManageableSource s2 = allSources[o2];
            return s1.getPriority() - s2.getPriority();
        }
    }
}