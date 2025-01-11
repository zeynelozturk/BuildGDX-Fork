// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
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

package ru.m210projects.Build.Render.GdxRender;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.NumberUtils;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;

public class Tesselator {

    public final List<Zoid_t> zoids = new ArrayList<Zoid_t>();
    protected final VertexAttribute[] attributes;
    private final Pool<Zoid_t> pZoidsPool = new Pool<>(Zoid_t::new);
    private final Engine engine;
    private final SurfaceInfo surf;
    private final int vertexSize;
    private final Vertex[] pol = new Vertex[]{new Vertex(0, 0), new Vertex(1, 0), new Vertex(1, 1),
            new Vertex(0, 1)};
    private final Vector3 norm = new Vector3();
    private final Vertex[] vertex = new Vertex[3];
    private final IntArray secy = new IntArray();
    private final ArrayList<Float> trapx0 = new ArrayList<Float>();
    private final ArrayList<Float> trapx1 = new ArrayList<Float>();
    private final WorldMesh mesh;
    private int sectnum = -1;

    public Tesselator(WorldMesh mesh, VertexAttribute... attributes) {
        this.mesh = mesh;
        this.engine = mesh.engine;
        this.attributes = attributes;

        int size = 0;
        for (int i = 0; i < attributes.length; i++) {
            if (!attributes[i].equals(VertexAttribute.ColorPacked())) {
                size += attributes[i].numComponents;
            } else {
                size++;
            }
        }

        this.vertexSize = size;
        this.surf = new SurfaceInfo();
    }

    public int getVertexSize() {
        return vertexSize;
    }

    public void setSector(int sectnum, boolean initZoids) {
        if (initZoids && this.sectnum != sectnum) {
            initZoids(sectnum);
        }
        this.sectnum = sectnum;
    }

    public int getSector() {
        return sectnum;
    }

    private void initZoids(final int sectnum) {
        final Sector sec = mesh.getSector(sectnum);
        final int n = sec.getWallnum();

        zoids.clear();
        secy.clear();
        pZoidsPool.reset();

        if (n < 3) {
            return;
        }

        float f, x0, y0, x1, y1, sy0, sy1;
        int g, s, secn, ntrap, cury;

        int startwall = sec.getWallptr();
        int endwall = startwall + sec.getWallnum() - 1;
        int i = 0;
        for (int w = startwall; w <= endwall; w++) {
            secy.add(mesh.getWall(w).getY());
        }

        secy.sort();
        for (i = 0, secn = 0, cury = Integer.MIN_VALUE; i < n; i++) {// remove dups
            int val = secy.get(i);
            if (val > cury) {
                secy.set(secn++, (cury = val));
            }
        }

        for (s = 0; s < secn - 1; s++) {
            sy0 = secy.get(s);
            sy1 = secy.get(s + 1);

            trapx0.clear();
            trapx1.clear();
            ntrap = 0;

            startwall = sec.getWallptr();
            endwall = startwall + sec.getWallnum() - 1;
            for (int w = startwall; w <= endwall; w++) {
                Wall wal = mesh.getWall(w);
                Wall wal2 = mesh.getWall(wal.getPoint2());

                x0 = wal.getX();
                y0 = wal.getY();
                x1 = wal2.getX();
                y1 = wal2.getY();

                if (y0 > y1) {
                    f = x0;
                    x0 = x1;
                    x1 = f;
                    f = y0;
                    y0 = y1;
                    y1 = f;
                }
                if ((y0 >= sy1) || (y1 <= sy0)) {
                    continue;
                }
                if (y0 < sy0) {
                    x0 = (sy0 - wal.getY()) * (wal2.getX() - wal.getX()) / (wal2.getY() - wal.getY()) + wal.getX();
                }
                if (y1 > sy1) {
                    x1 = (sy1 - wal.getY()) * (wal2.getX() - wal.getX()) / (wal2.getY() - wal.getY()) + wal.getX();
                }

                trapx0.add(x0);
                trapx1.add(x1);
                ntrap++;
            }

            int j;
            for (g = (ntrap >> 1); g != 0; g >>= 1) {
                for (i = 0; i < ntrap - g; i++) {
                    for (j = i; j >= 0; j -= g) {

                        if (trapx0.get(j) + trapx1.get(j) <= trapx0.get(j + g) + trapx1.get(j + g)) {
                            break;
                        }

                        f = trapx0.get(j);
                        trapx0.set(j, trapx0.get(j + g));
                        trapx0.set(j + g, f);
                        f = trapx1.get(j);
                        trapx1.set(j, trapx1.get(j + g));
                        trapx1.set(j + g, f);
                    }
                }
            }

            if (ntrap < 2) {
                continue;
            }

            for (i = 0; i < ntrap; i = j + 1) {
                j = i + 1;

                if (i >= trapx0.size() || j >= trapx0.size()) {
                    Console.out.println(String.format("Sector %d is corrupt", sectnum), OsdColor.RED);
                    sec.setAsBroken();
                    zoids.clear();
                    return;
                }

                if ((trapx0.get(j) <= trapx0.get(i))
                        && (trapx1.get(j) <= trapx1.get(i))) {
                    continue;
                }

                while ((j + 2 < ntrap) && (trapx0.get(j + 1) <= trapx0.get(j)) && (trapx1.get(j + 1) <= trapx1.get(j))) {
                    j += 2;
                }

                Zoid_t zoid = pZoidsPool.obtain();

                zoid.x[0] = trapx0.get(i);
                zoid.x[1] = trapx0.get(j);
                zoid.y[0] = sy0;
                zoid.x[3] = trapx1.get(i);
                zoid.x[2] = trapx1.get(j);
                zoid.y[1] = sy1;

                zoids.add(zoid);
            }
        }
    }

    public int getMaxVertices() {
        int vertices = 2 * zoids.size() * 6; // ceiling and floor
        vertices += 30 * mesh.getSector(sectnum).getWallnum(); // (6 - top, 6 - middle, 6 bottom, 12 sky)
        return 2 * vertices;
    }

    public SurfaceInfo getSurface(Type type, int num, FloatArray vertices) {
        surf.clear();

        int count = 0;
        switch (type) {
            case Quad: {
                float SIZEX = 1.0f / 2;
                float SIZEY = 1.0f / 2;
                ArrayList<Vertex> pol = new ArrayList<Vertex>();
                pol.add((Vertex) new Vertex(1, 1).set(-SIZEX, SIZEY, 0));
                pol.add((Vertex) new Vertex(0, 1).set(SIZEX, SIZEY, 0));
                pol.add((Vertex) new Vertex(0, 0).set(SIZEX, -SIZEY, 0));
                pol.add((Vertex) new Vertex(1, 0).set(-SIZEX, -SIZEY, 0));

                for (int v = 0; v < 4; v++) {
                    Vertex vx = pol.get(v);
                    for (int a = 0; a < attributes.length; a++) {
                        switch (attributes[a].usage) {
                            case Usage.Position:
                                vertices.addAll(vx.x, vx.y, vx.z);
                                break;
                            case Usage.TextureCoordinates:
                                vertices.addAll(vx.u, vx.v);
                                break;
                            case Usage.ColorUnpacked:
                                vertices.addAll(1, 1, 1, 1);
                                break;
                            case Usage.ColorPacked:
                                vertices.add(NumberUtils.intToFloatColor(-1));
                                break;
                            case Usage.Normal:
                                vertices.addAll(norm.x, norm.y, norm.z);
                                break;
                        }
                    }
                    count++;
                }
                break;
            }
            case Floor:
            case Ceiling: {
                Sector sec = mesh.getSector(num);
                surf.picnum = type == Type.Floor ? sec.getFloorpicnum() : sec.getCeilingpicnum();
                surf.obj = sec;
//			surf.shade = type == Type.Floor ? sec.floorshade : sec.ceilingshade;
//			surf.pal = type == Type.Floor ? sec.floorpal : sec.ceilingpal;
                ArtEntry pic = engine.getTile(surf.picnum);

                int n = 0, j = 0;
                for (int i = 0; i < zoids.size(); i++) {
                    for (j = 0, n = 0; j < 4; j++) {
                        pol[n].x = zoids.get(i).x[j];
                        pol[n].y = zoids.get(i).y[j >> 1];

                        if ((n == 0) || (pol[n].x != pol[n - 1].x) || (pol[n].y != pol[n - 1].y)) {
                            pol[n].z = (type == Type.Floor)
                                    ? engine.getflorzofslope((short) num, (int) (pol[n].x), (int) (pol[n].y))
                                    : engine.getceilzofslope((short) num, (int) (pol[n].x), (int) (pol[n].y));
                            n++;
                        }
                    }

                    if (n < 3) {
                        continue;
                    }

                    vertex[0] = pol[0];
                    for (j = 2; j < n; j++) {
                        if (type == Type.Floor) {
                            vertex[1] = pol[j - 1];
                            vertex[2] = pol[j];
                        } else {
                            vertex[1] = pol[j];
                            vertex[2] = pol[j - 1];
                        }

                        norm.set(vertex[2]).sub(vertex[0]);
                        float dx = (vertex[1].x - vertex[0].x);
                        float dy = (vertex[1].y - vertex[0].y);
                        float dz = (vertex[1].z - vertex[0].z);
                        norm.crs(dx, dy, dz).nor();

                        for (int v = 0; v < 3; v++) {
                            Vertex vx = vertex[v];
                            for (int a = 0; a < attributes.length; a++) {
                                switch (attributes[a].usage) {
                                    case Usage.Position:
                                        vertices.addAll(vx.x / mesh.scalexy, vx.y / mesh.scalexy, vx.z / mesh.scalez);
                                        break;
                                    case Usage.TextureCoordinates:
                                        float uptr = vx.x;
                                        float vptr = vx.y;
                                        if (!pic.hasSize()) {
                                            vertices.addAll(0.0f, 0.0f);
                                            break;
                                        }

                                        // Texture Relativity
                                        if ((type == Type.Floor) ? sec.isRelativeTexFloor() : sec.isRelativeTexCeiling()) {
                                            Wall wal = mesh.getWall(sec.getWallptr());
                                            Wall wal2 = mesh.getWall(wal.getPoint2());

                                            float vecx = wal.getX() - wal2.getX();
                                            float vecy = wal.getY() - wal2.getY();
                                            float len = (float) sqrt(vecx * vecx + vecy * vecy);
                                            vecx /= len;
                                            vecy /= len;

                                            float nuptr = uptr - wal.getX();
                                            float nvptr = vptr - wal.getY();

                                            uptr = -nuptr * vecx - vecy * nvptr;
                                            vptr = nvptr * vecx - vecy * nuptr;

                                            // Hack for slopes w/ relative alignment
                                            if ((type == Type.Floor) ? sec.isFloorSlope() : sec.isCeilingSlope()) {
                                                double r = ((type == Type.Floor) ? sec.getFloorheinum() : sec.getCeilingheinum())
                                                        / 4096.0;
                                                r = sqrt(r * r + 1);
                                                if ((type == Type.Floor) ? !sec.isTexSwapedFloor() : !sec.isTexSwapedCeiling()) {
                                                    vptr *= r;
                                                } else {
                                                    uptr *= r;
                                                }
                                            }
                                        }

                                        float uCoff = 16.0f * pic.getWidth();
                                        float vCoff = -16.0f * pic.getHeight();

                                        // Texture's x & y is swapped
                                        if ((type == Type.Floor) ? sec.isTexSwapedFloor() : sec.isTexSwapedCeiling()) {
                                            float tmp = uptr;
                                            uptr = -vptr;
                                            vptr = -tmp;
                                        }

                                        // Texture Expansion
                                        if ((type == Type.Floor) ? sec.isTexSmooshedFloor() : sec.isTexSmooshedCeiling()) {
                                            uCoff /= 2;
                                            vCoff /= 2;
                                        }

                                        // Texture's x is flipped
                                        if ((type == Type.Floor) ? sec.isTexXFlippedFloor() : sec.isTexXFlippedCeiling()) {
                                            uCoff *= -1;
                                        }

                                        // Texture's y is flipped
                                        if ((type == Type.Floor) ? sec.isTexYFlippedFloor() : sec.isTexYFlippedCeiling()) {
                                            vCoff *= -1;
                                        }

                                        float uPanning = ((type == Type.Floor) ? sec.getFloorxpanning() : sec.getCeilingxpanning())
                                                / 255.0f;
                                        float vPanning = ((type == Type.Floor) ? sec.getFloorypanning() : sec.getCeilingypanning())
                                                / 255.0f;

                                        vertices.addAll(uptr / uCoff + uPanning, vptr / vCoff + vPanning);
                                        break;
                                    case Usage.ColorUnpacked:
                                        vertices.addAll(1, 1, 1, 1);
                                        break;
                                    case Usage.ColorPacked:
                                        vertices.add(NumberUtils.intToFloatColor(-1));
                                        break;
                                    case Usage.Normal:
                                        vertices.addAll(norm.x, norm.y, norm.z);
                                        break;
                                }
                            }
                            count++;
                        }
                    }
                }
                break;
            }
            case Wall:
            case Sky: {
                Wall wal = mesh.getWall(num);
                Heinum heinum = type.getHeinum();

                ArrayList<Vertex> pol;
                if ((pol = mesh.getPoints(type.getHeinum(), sectnum, num)) == null || pol.size() < 3) {
                    return null;
                }

                float uCoff = 1, vCoff = 1, uPanning = 0, vPanning = 0, vOffs = 0;
                if (type != Type.Sky) {
                    int k = 0;
                    if (heinum == Heinum.Portal) {
                        k = -1;
                    } else if (heinum == Heinum.Lower) {
                        k = 1;
                    }

                    Wall ptr = wal;
                    if (k == 1 && wal.getNextsector() != -1 && wal.isSwapped()) {
                        ptr = mesh.getWall(wal.getNextwall());
                    }
                    vOffs = getVCoord(wal, sectnum, k);

                    int picnum = (k == -1 ? ptr.getOverpicnum() : ptr.getPicnum());
                    surf.picnum = picnum;
                    surf.obj = ptr;

                    ArtEntry pic = engine.getTile(picnum);
                    if (pic.hasSize()) {
                        uCoff = wal.getXrepeat() * 8.0f / pic.getWidth();
                        vCoff = -wal.getYrepeat() * 4.0f / GLInfo.calcSize(pic.getHeight());
                        uPanning = ptr.getXpanning() / (float) pic.getWidth();
                        vPanning = ptr.getYpanning() / 256.0f;

                        if (wal.isXFlip()) {
                            uCoff *= -1;
                            uPanning = (wal.getXrepeat() * 8.0f + ptr.getXpanning()) / pic.getWidth();
                        }

                        if (ptr.isYFlip()) {
                            vCoff *= -1;
                            vPanning *= -1;
                        }
                    }
                } else {
                    surf.picnum = heinum == Heinum.SkyLower ? mesh.getSector(sectnum).getFloorpicnum() : mesh.getSector(sectnum).getCeilingpicnum();
                    surf.obj = mesh.getSector(sectnum);
                }

                vertex[0] = pol.get(0);
                for (int j = 2; j < pol.size(); j++) {
                    vertex[1] = pol.get(j - 1);
                    vertex[2] = pol.get(j);

                    norm.set(vertex[2]).sub(vertex[0]);
                    float dx = (vertex[1].x - vertex[0].x);
                    float dy = (vertex[1].y - vertex[0].y);
                    float dz = (vertex[1].z - vertex[0].z);
                    norm.crs(dx, dy, dz).nor();

                    for (int i = 0; i < 3; i++) {
                        Vertex ver = vertex[i];
                        for (int a = 0; a < attributes.length; a++) {
                            switch (attributes[a].usage) {
                                case Usage.Position:
                                    vertices.addAll(ver.x / mesh.scalexy, ver.y / mesh.scalexy, ver.z / mesh.scalez);
                                    break;
                                case Usage.TextureCoordinates:
                                    if (type != Type.Sky) {
                                        vertices.addAll(uCoff * ver.u + uPanning,
                                                (vCoff * (vOffs - ver.z) / mesh.scalez) + vPanning);
                                    } else {
                                        vertices.addAll(ver.u, ver.v);
                                    }
                                    break;
                                case Usage.ColorUnpacked:
                                    vertices.addAll(1, 1, 1, 1);
                                    break;
                                case Usage.ColorPacked:
                                    vertices.add(NumberUtils.intToFloatColor(-1));
                                    break;
                                case Usage.Normal:
                                    vertices.addAll(norm.x, norm.y, norm.z);
                                    break;
                            }
                        }
                        count++;
                    }
                }
                break;
            }
            // end switch
        }

        if (count == 0) {
            return null;
        }

        surf.size = count;
        if (type == Type.Wall || type == Type.Sky) {
            surf.limit = 6; // Walls are quads (3 + 3 tris)
            if (mesh.getMesh() == null) { // when initializing
                int pads = 6 - count;
                for (int i = 0; i < pads; i++) {
                    for (int j = 0; j < getVertexSize(); j++) {
                        vertices.add(-1);
                    }
                }
            }
        } else {
            surf.limit = count;
        }

        return surf;
    }

    private int getVCoord(Wall wal, int sectnum, int k) {
        short nextsectnum = wal.getNextsector();
        int s3 = sectnum;
        int align = 0;

        if (nextsectnum != -1) {
            if (k == -1) { // masked wall
                if (wal.isOneWay()) {
                    if (!wal.isBottomAligned()) {
                        s3 = nextsectnum;
                    }
                } else {
                    align = wal.isBottomAligned() ? 1 : 0;
                    if (wal.isBottomAligned()) {
                        s3 = (mesh.getSector(sectnum).getFloorz() < mesh.getSector(nextsectnum).getFloorz()) ? sectnum : nextsectnum;
                    } else {
                        s3 = (mesh.getSector(sectnum).getCeilingz() < mesh.getSector(nextsectnum).getCeilingz()) ? nextsectnum : sectnum;
                    }
                }
            } else {
                if (k == 1) { // under
                    if (wal.isSwapped()) {
                        wal = mesh.getWall(wal.getNextwall());
                    }
                    align = wal.isBottomAligned() ? 0 : 1;
                }

                if (!wal.isBottomAligned()) {
                    s3 = nextsectnum;
                }
            }
        } else {
            align = wal.isBottomAligned() ? 1 : 0;
        }

        Wall ptr = mesh.getWall(mesh.getSector(s3).getWallptr());
        if (align == 0) {
            return engine.getceilzofslope((short) s3, ptr.getX(), ptr.getY());
        }
        return engine.getflorzofslope((short) s3, ptr.getX(), ptr.getY());
    }

    public enum Type {
        Floor, Ceiling, Wall, Sky, Quad;

        private Heinum heinum;

        public Heinum getHeinum() {
            return heinum;
        }

        public Type setHeinum(Heinum heinum) {
            this.heinum = heinum;
            return this;
        }
    }

    public static class Vertex extends Vector3 {
        private static final long serialVersionUID = 2322711328782046279L;
        protected float u, v;

        public Vertex(int u, int v) {
            this.u = u;
            this.v = v;
        }

        protected void set(Wall i, float z, float u, float v) {
            this.set(i.getX(), i.getY(), z);
            this.u = u;
            this.v = v;
        }
    }

    protected static class SurfaceInfo {
        public int picnum;
        public Object obj;
        public int size, limit;

        protected void clear() {
            picnum = -1;
            obj = null;
            size = 0;
            limit = 0;
        }

        protected int getSize() {
            return size;
        }

        protected int getLimit() {
            return limit;
        }
    }

    private static class Zoid_t {
        private final float[] x;
        private final float[] y;

        public Zoid_t() {
            x = new float[4];
            y = new float[2];
        }
    }
}
