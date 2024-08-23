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

package ru.m210projects.Build.Types.collections;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Pool<T> {

	protected int objectsCount;
	protected int size;
	protected T[] instances;
	private final ObjectCreator<T> objectCreator;

	public Pool(ObjectCreator<T> objectCreator) {
		this(objectCreator, 512);
	}

	@SuppressWarnings("unchecked")
	public Pool(ObjectCreator<T> objectCreator, int length) {
		this.objectCreator = objectCreator;
		instances = (T[]) Array.newInstance(objectCreator.newInstance().getClass(), length);
		reset();
	}

	public T[] getArray() {
		return instances;
	}

	public void reset() {
		size = 0;
	}

	public T obtain() {
        T object;
        if (size == objectsCount) {
            object = objectCreator.newInstance();
			addObject(object);
        } else {
            object = get(size);
			if (object instanceof Poolable) {
				((Poolable) object).reset();
			}
        }
		size++;
        return object;
    }

	public T get(int index) {
		return instances[index];
	}

	public int getSize() {
		return size;
	}

	protected void addObject(T value) {
		if (objectsCount == instances.length) {
			grow(objectsCount + 1);
		}
		instances[objectsCount++] = value;
	}

	protected void grow(int minCapacity) {
		int oldCapacity = instances.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);
		if (newCapacity - minCapacity < 0) {
			newCapacity = minCapacity;
		}
		instances = Arrays.copyOf(instances, newCapacity);
	}

	@Override
	public String toString() {
		String text = "Pool {";
		text += "[free / size]: " + (objectsCount - size) + " / " + objectsCount;
		text += "used : " + (((size) * 100) / objectsCount) + "% }";
		return text;
	}

	public interface Poolable {
		void reset();
	}

	public interface ObjectCreator<T> {
		T newInstance();
	}
}
