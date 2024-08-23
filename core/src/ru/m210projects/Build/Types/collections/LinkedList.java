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

import java.util.NoSuchElementException;

public class LinkedList<T> {

    protected ListNode<T> first;
    protected ListNode<T> last;
    protected int size;

    public void addFirst(ListNode<T> newNode) {
        final ListNode<T> f = first;
        first = newNode.link(this, null, f);
        if (f == null) {
            last = newNode;
        } else {
            f.prev = newNode;
        }
        size++;
    }

    public void addLast(ListNode<T> newNode) {
        final ListNode<T> l = last;
        last = newNode.link(this, l, null);
        if (l == null) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    public ListNode<T> removeFirst() {
        final ListNode<T> f = first;
        if (f == null) {
            throw new NoSuchElementException();
        }
        unlink(f);
        return f;
    }

    public ListNode<T> removeLast() {
        final ListNode<T> l = last;
        if (l == null) {
            throw new NoSuchElementException();
        }
        unlink(l);
        return l;
    }

    public boolean remove(ListNode<T> item) {
        if (size == 0) {
            return false;
        }

        if (item.equals(last)) {
            removeLast();
            return true;
        }

        if (item.equals(first)) {
            removeFirst();
            return true;
        }

        for (ListNode<T> node = first; node != null; node = node.getNext()) {
            if (item.equals(node)) {
                unlink(node);
                return true;
            }
        }

        return false;
    }

    public int getSize() {
        return size;
    }

    protected void unlink(ListNode<T> x) {
        x.parent = null;
        final ListNode<T> next = x.next;
        final ListNode<T> prev = x.prev;

        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
        size--;
    }

    public ListNode<T> getFirst() {
        return first;
    }

    public ListNode<T> getLast() {
        return last;
    }

    @Override
    public String toString() {
        if(size == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (ListNode<T> x = first; x != null; x = x.next) {
            sb.append(x.index);
            if (x.next != null) {
                sb.append(',').append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static <T> LinkedList<T> toImmutableList(LinkedList<T> list) {
        return new ImmutableList<>(list);
    }

    private static class ImmutableList<T> extends LinkedList<T> {

        public ImmutableList(LinkedList<T> list) {
            this.first = list.getFirst();
            this.last = list.getLast();
            this.size = list.getSize();
        }

        @Override
        public void addFirst(ListNode<T> newNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLast(ListNode<T> newNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListNode<T> removeFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListNode<T> removeLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(ListNode<T> item) {
            throw new UnsupportedOperationException();
        }
    }
}

