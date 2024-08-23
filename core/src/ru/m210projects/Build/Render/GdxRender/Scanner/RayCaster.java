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

package ru.m210projects.Build.Render.GdxRender.Scanner;

import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Types.collections.Pool.Poolable;

import java.util.Comparator;

import static ru.m210projects.Build.Engine.MAXWALLS;
import static ru.m210projects.Build.Engine.pow2char;

public class RayCaster {

    private static int SEGMENT_INDEX = 0; // used by debug

    protected final BoardService boardService;
    private final Pool<Segment> pSegmentPool = new Pool<>(Segment::new);
    private final Pool<EndPoint> pEndpointPool = new Pool<>(EndPoint::new);
    protected Array<Segment> segments = new Array<>(true, 128, Segment.class); // WALLS
    protected Array<EndPoint> endpoints = new Array<>(true, 128, EndPoint.class); // wall points (coordinates)
    protected SegmentList open = new SegmentList();
    protected byte[] gotwall = new byte[MAXWALLS >> 3];
    protected byte[] handled = new byte[MAXWALLS >> 3];
    protected boolean globalcheck;
    protected BuildCamera camera;
    protected Comparator<EndPoint> comparator = (a, b) -> {
        // XXX Comparison method violates its general contract!
        if (a.angle != b.angle) {
            return a.angle > b.angle ? 1 : -1;
        }
        if (a.begin != b.begin) {
            return !a.begin ? 1 : -1;
        }
        return 0;
    };

    protected Comparator<Segment> wallfront = (a, b) -> {
        float x11 = a.p1.x;
        float y11 = a.p1.y;
        float x21 = a.p2.x;
        float y21 = a.p2.y;

        float x12 = b.p1.x;
        float y12 = b.p1.y;
        float x22 = b.p2.x;
        float y22 = b.p2.y;

        float dx = x21 - x11;
        float dy = y21 - y11;

        final double f = 0.001;
        final double invf = 1.0 - f;
        double px = (x12 * invf) + (x22 * f);
        double py = (y12 * invf) + (y22 * f);

        double cross = dx * (py - y11) - dy * (px - x11);
//		double cross = dx * (y12 - y11) - dy * (x12 - x11);
        boolean t1 = (cross < 0.00001); // p1(l2) vs. l1

        px = (x22 * invf) + (x12 * f);
        py = (y22 * invf) + (y12 * f);
        double cross1 = dx * (py - y11) - dy * (px - x11);
//		double cross1 = dx * (y22 - y11) - dy * (x22 - x11);
        boolean t2 = (cross1 < 0.00001); // p2(l2) vs. l1

        if (t1 == t2) {
            t1 = (dx * (camera.getY() - y11) - dy * (camera.getX() - x11) < 0.00001); // pos vs. l1
            if (t2 == t1) {
                return 0;
            }
        }

        dx = x22 - x12;
        dy = y22 - y12;

        px = (x11 * invf) + (x21 * f);
        py = (y11 * invf) + (y21 * f);

        double cross3 = dx * (py - y12) - dy * (px - x12);
//		double cross3 = dx * (y11 - y12) - dy * (x11 - x12);
        t1 = (cross3 < 0.00001); // p1(l1) vs. l2

        px = (x21 * invf) + (x11 * f);
        py = (y21 * invf) + (y11 * f);
        double cross4 = dx * (py - y12) - dy * (px - x12);
//		double cross4 = dx * (y21 - y12) - dy * (x21 - x12);
        t2 = (cross4 < 0.00001); // p2(l1) vs. l2

        if (t1 == t2) {
            if (globalcheck) {
                if (Math.abs(cross) < 0.00001 && Math.abs(cross1) < 0.00001 && Math.abs(cross3) < 0.00001
                        && Math.abs(cross4) < 0.00001) {
                    if (a.isPortal() && !b.isPortal()) // e2l8
                    {
                        return -1;
                    }
                }
            }

            t1 = (dx * (camera.getY() - y12) - dy * (camera.getX() - x12) < 0.00001); // pos vs. l2
            return (t2 != t1) ? 0 : -1;
        }

        return 1;
    };

    public RayCaster(BoardService boardService) {
        this.boardService = boardService;
    }

    public void init(BuildCamera camera, boolean globalcheck) {
        this.camera = camera;
        this.segments.clear();
        this.endpoints.clear();
        Gameutils.fill(gotwall, (byte) 0);
        Gameutils.fill(handled, (byte) 0);
        this.globalcheck = globalcheck;

        this.pSegmentPool.reset();
        this.pEndpointPool.reset();
    }

    public void addSegment(int id, float x1, float y1, float x2, float y2) {
        EndPoint p1 = pEndpointPool.obtain();
        EndPoint p2 = pEndpointPool.obtain();
        Segment segment = pSegmentPool.obtain();

        p1.x = x1;
        p1.y = y1;
        p2.x = x2;
        p2.y = y2;
        p1.segment = segment;
        p2.segment = segment;
        segment.p1 = p1;
        segment.p2 = p2;
        segment.wallid = id;

        this.segments.add(segment);
        this.endpoints.add(p1, p2);

        handled[id >> 3] |= (byte) pow2char[id & 7];
    }

    public void update() {
        float cameraX = camera.getX();
        float cameraY = camera.getY();
        for (int i = 0; i < segments.size; i++) {
            Segment segment = segments.items[i];

            segment.p1.angle = atan2((segment.p1.y - cameraY), (segment.p1.x - cameraX));
            segment.p2.angle = atan2((segment.p2.y - cameraY), (segment.p2.x - cameraX));

            float dAngle = (segment.p2.angle - segment.p1.angle);
            if ((dAngle <= -2)) {
                dAngle += 4;
            }
            if ((dAngle > 2)) {
                dAngle -= 4;
            }

            segment.p1.begin = (dAngle > 0.0);
            segment.p2.begin = !(segment.p1.begin);
        }

        sweep();
    }

    public float atan2(float dx, float dy) {
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) {
            return 0;
        }
        dx /= len;
        if (dy >= 0) {
            return dx;
        }
        if (dx >= 0) {
            return 2 - dx;
        }
        return -(2 + dx);
    }

    public void add(int cursectnum, int z, WallFrustum2d frust) {
        Wall wal = boardService.getWall(z);
        Wall wal2 = boardService.getWall(wal.getPoint2());

        if (frust == null || frust.sectnum == cursectnum) {
            addSegment(z, wal.getX(), wal.getY(), wal2.getX(), wal2.getY());
        } else {
            WallFrustum2d f = frust;
            do {
                if (f.isGreater180()) {
                    addSegment(z, wal.getX(), wal.getY(), wal2.getX(), wal2.getY());
                } else {
                    addClippedSegment(f, z);
                }
                f = f.next;
            } while (f != null);
        }

//		addSegment(z, wal.x, wal.y, wal2.x, wal2.y);
    }

    public void addClippedSegment(WallFrustum2d frustum, int z) {
        Plane[] planes = frustum.planes;
        Wall p1 = boardService.getWall(z);
        Wall p2 = boardService.getWall(p1.getPoint2());

        float p1x = p1.getX();
        float p1y = p1.getY();
        float p2x = p2.getX();
        float p2y = p2.getY();
        float cameraX = camera.getX();
        float cameraY = camera.getY();

        for (int i = 0; i < 2; i++) {
            Vector3 p = planes[i].normal;

            float t1 = p.dot(p1x - cameraX, p1y - cameraY, 0);
            float t2 = p.dot(p2x - cameraX, p2y - cameraY, 0);

            if (t1 < 0.0001f && t2 < 0.0001f) {
                float angle1 = atan2((p1y - cameraY), (p1x - cameraX));
                float angle2 = atan2((p2y - cameraY), (p2x - cameraX));

                float dAngle = (angle2 - angle1);
                if (dAngle >= 0.01f) // XXX E3L2 line wall bug fix
                {
                    handled[z >> 3] |= (byte) pow2char[z & 7];
                }
                return;
            }

            if ((t1 >= -0.0001f) != (t2 >= -0.0001f)) {
                float r = t1 / (t1 - t2);
                float dx = (p2x - p1x);
                float dy = (p2y - p1y);

                if (t1 >= -0.0001f) {
                    p2x = (float) Math.ceil(dx * r + p1x);
                    p2y = (float) Math.ceil(dy * r + p1y);
                } else {
                    p1x = (float) Math.ceil(dx * r + p1x);
                    p1y = (float) Math.ceil(dy * r + p1y);
                }
            }
        }

        addSegment(z, p1x, p1y, p2x, p2y);
    }

    public boolean check(int z) {
        if ((handled[z >> 3] & pow2char[z & 7]) == 0) {
            return true;
        }

        return (gotwall[z >> 3] & pow2char[z & 7]) != 0;
    }

    protected void sweep() {
        endpoints.sort(comparator);

        open.clear();

        for (int i = 0; i < 2; i++) {
            for (int pi = 0; pi < endpoints.size; pi++) {
                EndPoint p = endpoints.items[pi];
                Segment current_old = (Segment) open.getFirst();

                while (current_old != null && current_old.isPortal()) {
                    if (i == 1) {
                        int wallid = current_old.wallid;
                        gotwall[wallid >> 3] |= (byte) pow2char[wallid & 7];
                    }
                    open.remove(current_old);
                    current_old = (Segment) open.getFirst();
                }

                if (p.begin) {
                    open.add(p.segment, wallfront);
                } else {
                    open.remove(p.segment);
                }

                if (current_old != null) {
                    if (!current_old.equals(open.getFirst())) {
                        if (i == 1) {
                            if (current_old != null) {
                                int z = current_old.wallid;
                                gotwall[z >> 3] |= (byte) pow2char[z & 7];
                            }
                        }
                    }
                }
            }
        }
    }

    public class EndPoint {
        public float x, y;
        public boolean begin;
        public Segment segment;
        public float angle;

    }

    public class SegmentList extends LinkedList<Segment> {

        public void add(Segment item, Comparator<Segment> c) {
            if(item == null || this.equals(item.getParent())) { // already in the list
                return;
            }

            Segment listNode = compare(item, c);
            if (listNode == null) {
                addLast(item);
            } else {
                item.insertBefore(listNode);
                if (listNode.equals(first)) {
                    first = item;
                }
                size++;
            }
        }

        @Nullable
        protected Segment compare(ListNode<Segment> item, Comparator<Segment> c) {
            ListNode<Segment> node = first;
            while (node != null) {
                if (c.compare(item.get(), node.get()) < 0) {
                    break;
                }
                node = node.getNext();
            }
            return (Segment) node;
        }

        public void clear() {
//        for (MapNode<Segment> node = first; node != null; node = node.next) {
//            node.prev = null;
//            node.next = null;
//        }
            first = last = null;
            size = 0;
        }
    }

    public class Segment extends
//            LinkedList.Node<Segment>
            ListNode<Segment>
            implements Poolable { // WALL
        public EndPoint p1, p2;
        public int wallid;

        protected Segment() {
            super(SEGMENT_INDEX++);
        }

        public boolean isPortal() {
            return boardService.getWall(wallid).getNextsector() != -1;
        }

        public void insertBefore(Segment listNode) {
            this.link(listNode.parent, listNode.prev, listNode);
            listNode.prev = this;
            if (this.prev != null) {
                this.getPrev().next = this;
            }
        }

        public Segment getPrev() {
            return (Segment) prev;
        }

        @Override
        public void reset() {
            p1 = p2 = null;
            next = prev = null;
            parent = null;
            wallid = -1;
        }

        public LinkedList<Segment> getParent() {
            return parent;
        }

        public Segment get() {
            return this;
        }

        public Segment getValue() {
            return this;
        }
    }
}
