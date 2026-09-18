//This file is part of BuildGDX.
//Copyright (C) 2023-2024  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.filehandle;

import ru.m210projects.Build.Types.collections.LinkedMap;
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.Types.collections.ListNode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;

public class CacheResourceMap extends LinkedMap<Group> {

    /**
     * key: group name
     * value: index in groups list
     */
    protected final Map<String, Integer> groupMap = new LinkedHashMap<>();

    public CacheResourceMap() {
        super(3, null, 16, null);
    }

    public void addGroup(Group group, CachePriority priority) {
        if (group.getSize() > 0) {
            GroupNode node = (GroupNode) obtain();
            node.group = group;
            node.priority = priority;
            groupMap.put(group.getName().toUpperCase(Locale.ROOT), node.getIndex());
            insert(node, priority.getLevel());
        }
    }

    public void removeGroup(Group group) {
        final String key = group.getName().toUpperCase(Locale.ROOT);
        int index = groupMap.getOrDefault(key, -1);
        if (index != -1) {
            groupMap.remove(key);
            remove(index);
        }
    }

    public CachePriority getPriority(Group group) {
        final String key = group.getName().toUpperCase(Locale.ROOT);
        int index = groupMap.getOrDefault(key, -1);
        if (index != -1) {
            GroupNode node = (GroupNode) nodeMap[index];
            return node.priority;
        }
        return CachePriority.NULL;
    }

    public Group getGroup(String groupName) {
        int index = groupMap.getOrDefault(groupName.toUpperCase(Locale.ROOT), -1);
        if (index != -1) {
            return nodeMap[index].get();
        }
        return DUMMY_DIRECTORY;
    }

    Group getGroup(int index) {
        return nodeMap[index].get();
    }

    @Override
    protected Group getInstance() {
        return DUMMY_DIRECTORY;
    }

    @Override
    protected void fill(int from) {
        final LinkedList<Group> list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            ListNode<Group> newNode = new GroupNode(i);
            list.addLast(newNode);
            nodeMap[i] = newNode;
        }
    }

    @Override
    protected void setValue(ListNode<Group> node, int value) {
        if (value == -1) {
            ((GroupNode) node).group = DUMMY_DIRECTORY;
            ((GroupNode) node).priority = CachePriority.NULL;
        }
    }

    private final static class GroupNode extends ListNode<Group> {
        private Group group = DUMMY_DIRECTORY;
        private CachePriority priority = CachePriority.NULL;

        private GroupNode(int index) {
            super(index);
        }

        @Override
        public Group get() {
            return group;
        }
    }

    public enum CachePriority {
        NULL(-1), NORMAL(0), HIGH(1), HIGHEST(2);

        private final int level;

        CachePriority(int level) {
            this.level = level;
        }

        public final int getLevel() {
            return level;
        }
    }
}