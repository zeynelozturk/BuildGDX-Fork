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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class LinkedMap<T> {

    protected final LinkedList<T>[] basket;
    protected final int poolIndex;
    protected ListNode<T>[] nodeMap;
    protected final List<T> list;
    protected final ValueSetter<T> valueSetter;

    public LinkedMap(int stateCount, int initialCount) {
        this(stateCount, new ArrayList<>(), initialCount, null);
    }

    @SuppressWarnings("unchecked")
    public LinkedMap(int stateCount, List<T> list, int initialCount, ValueSetter<T> valueSetter) {
        this.list = list;
        this.poolIndex = stateCount;
        this.basket = new LinkedList[poolIndex + 1];
        this.valueSetter = valueSetter;

        this.nodeMap = new ListNode[Math.max(1, initialCount)];
        for(int i = 0; i < basket.length; i++) {
            basket[i] = new LinkedList<>();
        }

        fill(0);
    }

    /**
     * used in fill method for each new line
     */
    protected abstract T getInstance();

    public int insert(int value) {
        if (value < 0 || value >= basket.length) {
            value = poolIndex;
        }
        return insert(obtain(), value);
    }

    public boolean set(int element, int value) {
        if (value < 0 || value >= basket.length) {
            value = poolIndex;
        }

        if (element < 0 || element >= nodeMap.length) {
            increase(element);
        }

        ListNode<T> node = nodeMap[element];
        LinkedList<T> list = node.getParent();
        if (basket[value] == list) {
            // already in this list
            setValue(node, value);
            return false;
        }

        list.unlink(node);
        insert(node, value);
        return true;
    }

    public boolean remove(int element) {
        if (element < 0) {
            return false;
        }

        if (element >= nodeMap.length) {
            increase(element);
        }

        ListNode<T> node = nodeMap[element];
        LinkedList<T> list = node.getParent();
        if(list == basket[poolIndex]) {
            // already deleted
            setValue(node, -1);
            return false;
        }

        list.unlink(node);
        list = basket[poolIndex];
        list.addFirst(node);
        setValue(node, -1);
        return true;
    }

    public LinkedList<T> get(int value) {
        return basket[value];
    }

    public ListNode<T> getFirst(int value) {
        if (value < 0 || value >= basket.length) {
            return null;
        }

        return basket[value].getFirst();
    }

    protected int insert(ListNode<T> node, int value) {
        final LinkedList<T> list = basket[value];
        list.addFirst(node);
        setValue(node, value);
        return node.index;
    }

    protected void setValue(ListNode<T> node, int value) {
        valueSetter.setValue(node.get(), value);
    }

    protected ListNode<T> obtain() {
        final LinkedList<T> list = basket[poolIndex];
        if(list.getSize() == 0) {
            increase(0);
        }
        return list.removeFirst();
    }

    protected void fill(int from) {
        final LinkedList<T> list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            ListNode<T> newNode = new ListNode<T>(i) {
                @Override
                public T get() {
                    return LinkedMap.this.list.get(index);
                }
            };
            list.addLast(newNode);
            if (i >= this.list.size()) {
                this.list.add(getInstance());
                setValue(newNode, -1);
            }
            nodeMap[i] = newNode;
        }
    }

    protected void increase(int toIndex) {
        final int size = nodeMap.length;
        int requiredSize = Math.max(toIndex + (toIndex >> 1), size + (size >> 1));
        this.nodeMap = Arrays.copyOf(nodeMap, requiredSize);
        fill(size);
    }
}
