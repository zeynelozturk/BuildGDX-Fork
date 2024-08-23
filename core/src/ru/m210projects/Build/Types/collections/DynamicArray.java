// This file is part of BuildGDX.
// Copyright (C) 2024  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Types.collections;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public class DynamicArray<T> extends Array<T> {

    private final Class<T> arrayType;
    private final boolean createInstances;

    public DynamicArray(int capacity, Class<T> arrayType) {
        this(capacity, arrayType, true);
    }

    public DynamicArray(int capacity, Class<T> arrayType, boolean createInstances) {
        super(true, capacity, arrayType);
        this.arrayType = arrayType;
        this.createInstances = createInstances;
        fill(0);
    }

    public T newInstance() {
        try {
            return ClassReflection.newInstance(arrayType);
        } catch (ReflectionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void fill(int from) {
        for (int j = from; j < items.length; j++) {
            if (createInstances) {
                items[j] = newInstance();
            }
        }
        size = items.length;
    }

    public T getInstance(int i) {
        if (i < 0) {
            return null;
        }

        checkCapacity(i);
        if (items[i] == null) {
            items[i] = newInstance();
        }
        return items[i];
    }

    @Override
    public void clear() {
        super.clear();
        if (createInstances) {
            fill(0);
        }
        // DynamicArray always should be filled to full size (by null or new objects)
        size = items.length;
    }

    @Override
    public T get(int i) {
        if (i < 0) {
            return null;
        }

        checkCapacity(i);
        return items[i];
    }

    @Override
    public void set (int i, T value) {
        checkCapacity(i);
        items[i] = value;
    }

    @Override
    public void insert (int i, T value) {
        checkCapacity(i);
        super.insert(i, value);
    }

    protected void checkCapacity(int i) {
        if (size != items.length) {
            throw new RuntimeException("DynamicArray always should be filled to full size (by null or new objects)");
        }

        if (i >= items.length) {
            int from = i - items.length;
            ensureCapacity((int) (i * 1.75f));
            fill(from);
        }
    }
}
