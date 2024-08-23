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

public abstract class ListNode<T> {

    protected final int index;
    protected LinkedList<T> parent;
    protected ListNode<T> next;
    protected ListNode<T> prev;

    protected ListNode(int index) {
        this.index = index;
    }

    public abstract T get();

    public ListNode<T> getNext() {
        return next;
    }

    public ListNode<T> getPrev() {
        return prev;
    }

    public boolean hasNext() {
        return next != null;
    }

    public boolean hasPrev() {
        return prev != null;
    }

    protected ListNode<T> link(LinkedList<T> parent, ListNode<T> prev, ListNode<T> next) {
        this.parent = parent;
        this.next = next;
        this.prev = prev;
        return this;
    }

    protected LinkedList<T> getParent() {
        return parent;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        if (parent != null) {
            return parent.toString();
        }
        return "[]";
    }
}
