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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GdxRender.Tesselator.SurfaceInfo;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Type;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Vertex;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Timer;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Engine.MAXSECTORS;
import static ru.m210projects.Build.Engine.MAXWALLS;

public class WorldMesh {

    private static final int CEILING1 = 0;
    private static final int CEILING2 = 1;
    private static final int FLOOR2 = 2;
    private static final int FLOOR1 = 3;
    protected final float scalexy = 512.0f;
    protected final float scalez = 8192.0f;
    private final Tesselator tess;
    private final Mesh mesh;
    private final BoardService boardService;
    private final FloatArray vertices = new FloatArray();
    private final int[] floorhash = new int[MAXSECTORS];
    private final int[] ceilinghash = new int[MAXSECTORS];
    private final int[] wallhash = new int[MAXWALLS];
    private final GLSurface[] walls = new GLSurface[MAXWALLS];
    private final GLSurface[] upper_walls = new GLSurface[MAXWALLS];
    private final GLSurface[] lower_walls = new GLSurface[MAXWALLS];
    private final GLSurface[] maskwalls = new GLSurface[MAXWALLS];
    private final GLSurface[] upper_skies = new GLSurface[MAXWALLS];
    private final GLSurface[] lower_skies = new GLSurface[MAXWALLS];
    private final GLSurface[] floors = new GLSurface[MAXSECTORS];
    private final GLSurface[] ceilings = new GLSurface[MAXSECTORS];
    private final GLSurface quad;
    private final Vertex[] pol = new Vertex[]{new Vertex(0, 0), new Vertex(1, 0), new Vertex(1, 1),
            new Vertex(0, 1)};
    private final ArrayList<Vertex> pointList = new ArrayList<>();
    protected Engine engine;
    protected GLSurface lastSurf;
    private ByteBuffer meshByteBuffer;
	private int meshOffset;
    private boolean validateMesh = false;
    private final AtomicInteger floorz = new AtomicInteger();
    private final AtomicInteger ceilz = new AtomicInteger();

    public WorldMesh(Engine engine) {
        this.engine = engine;
        this.boardService = engine.getBoardService();
        this.tess = new Tesselator(this, VertexAttribute.Position(), VertexAttribute.ColorPacked(),
                VertexAttribute.TexCoords(0));

        Timer.start();
        FloatArray vertices = new FloatArray();
        lastSurf = null;
		int maxVertices = 0;
        meshOffset = 0;

        quad = addQuad(vertices);

        for (short s = 0; s < boardService.getSectorCount(); s++) {
            Sector sec = boardService.getSector(s);
            if (sec.getFloorz() == sec.getCeilingz()) {
                continue;
            }

            tess.setSector(s, true);

            if (tess.zoids.isEmpty()) {
                continue;
            }

            addFloor(vertices, s);
            floorhash[s] = getFloorHash(s);
            addCeiling(vertices, s);
            ceilinghash[s] = getCeilingHash(s);

            for (int w = sec.getWallptr(); w < sec.getWallptr() + sec.getWallnum(); w++) {
                wallhash[w] = getWallHash(s, w);

                addMiddle(vertices, s, w);
                addUpper(vertices, s, w);
                addLower(vertices, s, w);
                addMaskedWall(vertices, s, w);
            }

            if (sec.isParallaxCeiling() || sec.isParallaxFloor()) {
                for (int w = sec.getWallptr(); w < sec.getWallptr() + sec.getWallnum(); w++) {
                    addParallaxCeiling(vertices, s, w);
                    addParallaxFloor(vertices, s, w);
                }
            }

            maxVertices += tess.getMaxVertices();
        }

        Timer.result("WorldMesh built in: ");

        mesh = new MyMesh(makeVertexBuffer(maxVertices, new VertexAttributes(tess.attributes)));

        int size = Math.min(maxVertices * tess.getVertexSize(), vertices.items.length);
        mesh.setVertices(vertices.items, 0, size);

        this.validateMesh = false;
    }

    private VertexData makeVertexBuffer(int maxVertices, VertexAttributes vertexAttributes) {
        this.meshByteBuffer = BufferUtils.newUnsafeByteBuffer(vertexAttributes.vertexSize * maxVertices);
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectWithVAO(false, meshByteBuffer, vertexAttributes);
        } else {
            return new MyVertexBufferObject(meshByteBuffer, vertexAttributes);
        }
    }

    public ArrayList<Vertex> getPoints(Heinum heinum, int sectnum, int z) {
        int fz1, fz2, cz1, cz2;
        Sector sec = boardService.getSector(sectnum);
        Wall wal = boardService.getWall(z);
        Wall wal2 = boardService.getWall(wal.getPoint2());
        int nextsector = wal.getNextsector();

        switch (heinum) {
            case Max:
            case MaxWall:
                engine.getzsofslope((short) sectnum, wal.getX(), wal.getY(), floorz, ceilz);
                pol[CEILING1].set(wal, ceilz.get(), 0, 0);
                pol[FLOOR1].set(wal, floorz.get(), 0, 1);

                engine.getzsofslope((short) sectnum, wal2.getX(), wal2.getY(), floorz, ceilz);
                pol[FLOOR2].set(wal2, floorz.get(), 1, 1);
                pol[CEILING2].set(wal2, ceilz.get(), 1, 0);

                if (heinum == Heinum.Max) {
                    if (sec.isParallaxCeiling()) {
                        pol[CEILING1].z = pol[CEILING2].z = Integer.MIN_VALUE;
                    }
                    if (sec.isParallaxFloor()) {
                        pol[FLOOR1].z = pol[FLOOR2].z = Integer.MAX_VALUE;
                    }
                }
                break;
            case Lower:
                fz1 = engine.getflorzofslope((short) sectnum, wal.getX(), wal.getY());
                cz1 = engine.getflorzofslope((short) nextsector, wal.getX(), wal.getY());
                fz2 = engine.getflorzofslope((short) sectnum, wal2.getX(), wal2.getY());
                cz2 = engine.getflorzofslope((short) nextsector, wal2.getX(), wal2.getY());

                if (fz1 < cz1 && fz2 < cz2) {
                    return null;
                }

                pol[CEILING1].set(wal, cz1, 0, 0);
                pol[FLOOR1].set(wal, fz1, 0, 1);
                pol[FLOOR2].set(wal2, fz2, 1, 1);
                pol[CEILING2].set(wal2, cz2, 1, 0);
                break;
            case SkyLower:
                fz1 = engine.getflorzofslope((short) sectnum, wal.getX(), wal.getY());
                pol[CEILING1].set(wal, fz1, 0, 1);
                pol[FLOOR1].set(wal, fz1 + 0x8000000, 0, 0);

                fz1 = engine.getflorzofslope((short) sectnum, wal2.getX(), wal2.getY());
                pol[FLOOR2].set(wal2, fz1 + 0x8000000, 1, 1);
                pol[CEILING2].set(wal2, fz1, 1, 0);
                break;
            case SkyUpper:
                cz1 = engine.getceilzofslope((short) sectnum, wal.getX(), wal.getY());
                pol[FLOOR1].set(wal, cz1, 0, 0);
                pol[CEILING1].set(wal, cz1 - 0x8000000, 0, 1);

                cz1 = engine.getceilzofslope((short) sectnum, wal2.getX(), wal2.getY());
                pol[FLOOR2].set(wal2, cz1, 1, 1);
                pol[CEILING2].set(wal2, cz1 - 0x8000000, 1, 0);
                break;
            case Upper:
                fz1 = engine.getceilzofslope((short) sectnum, wal.getX(), wal.getY());
                cz1 = engine.getceilzofslope((short) nextsector, wal.getX(), wal.getY());
                fz2 = engine.getceilzofslope((short) sectnum, wal2.getX(), wal2.getY());
                cz2 = engine.getceilzofslope((short) nextsector, wal2.getX(), wal2.getY());

                if (fz1 >= cz1 && fz2 >= cz2) {
                    return null;
                }

                pol[CEILING1].set(wal, fz1, 0, 0);
                pol[FLOOR1].set(wal, cz1, 0, 1);
                pol[FLOOR2].set(wal2, cz2, 1, 1);
                pol[CEILING2].set(wal2, fz2, 1, 0);
                break;
            case Portal:
                engine.getzsofslope((short) nextsector, wal.getX(), wal.getY(), floorz, ceilz);
                fz1 = floorz.get();
                cz1 = ceilz.get();
                engine.getzsofslope((short) nextsector, wal2.getX(), wal2.getY(), floorz, ceilz);
                fz2 = floorz.get();
                cz2 = ceilz.get();

                engine.getzsofslope((short) sectnum, wal.getX(), wal.getY(), floorz, ceilz);
                int fz3 = floorz.get();
                int cz3 = ceilz.get();
                engine.getzsofslope((short) sectnum, wal2.getX(), wal2.getY(), floorz, ceilz);
                int fz4 = floorz.get();
                int cz4 = ceilz.get();

                if (fz3 <= fz1 && fz4 <= fz2) {
                    fz1 = fz3;
                    fz2 = fz4;
                }

                if (cz3 >= cz1 && cz4 >= cz2) {
                    cz1 = cz3;
                    cz2 = cz4;
                }

                pol[CEILING1].set(wal, cz1, 0, 0);
                pol[FLOOR1].set(wal, fz1, 0, 1);
                pol[FLOOR2].set(wal2, fz2, 1, 1);
                pol[CEILING2].set(wal2, cz2, 1, 0);
                break;
        }

        pointList.clear();
        if (pol[FLOOR1].z == pol[CEILING1].z && pol[FLOOR2].z == pol[CEILING2].z) {
            if (sec.isParallaxFloor() || sec.isParallaxCeiling()) {
                pointList.add(pol[CEILING1]);
                pointList.add(pol[CEILING2]);
                return pointList;
            }
            return null;
        }

        float dz0 = pol[FLOOR1].z - pol[CEILING1].z;
        float dz1 = pol[FLOOR2].z - pol[CEILING2].z;
        if (dz0 > 0.0f) {
            pointList.add(pol[CEILING1]);
            if (dz1 > 0.0f) {
                pointList.add(pol[CEILING2]);
                pointList.add(pol[FLOOR2]);
                pointList.add(pol[FLOOR1]);
                return pointList; // 4
            } else {
                float f = dz0 / (dz0 - dz1);
                pol[CEILING2].x = (pol[CEILING2].x - pol[CEILING1].x) * f + pol[CEILING1].x;
                pol[CEILING2].y = (pol[CEILING2].y - pol[CEILING1].y) * f + pol[CEILING1].y;
                pol[CEILING2].z = (pol[CEILING2].z - pol[CEILING1].z) * f + pol[CEILING1].z;
                pol[CEILING2].u = (pol[CEILING2].u - pol[CEILING1].u) * f + pol[CEILING1].u;
                pol[CEILING2].v = (pol[CEILING2].v - pol[CEILING1].v) * f + pol[CEILING1].v;
                pointList.add(pol[CEILING2]);
                pointList.add(pol[FLOOR1]);
                return pointList; // 3
            }
        }
        if (dz1 <= 0.0f) {
            return null; // do not include null case for rendering
        }

        float f = dz0 / (dz0 - dz1);
        pol[CEILING1].x = (pol[CEILING2].x - pol[CEILING1].x) * f + pol[CEILING1].x;
        pol[CEILING1].y = (pol[CEILING2].y - pol[CEILING1].y) * f + pol[CEILING1].y;
        pol[CEILING1].z = (pol[CEILING2].z - pol[CEILING1].z) * f + pol[CEILING1].z;
        pol[CEILING1].u = (pol[CEILING2].u - pol[CEILING1].u) * f + pol[CEILING1].u;
        pol[CEILING1].v = (pol[CEILING2].v - pol[CEILING1].v) * f + pol[CEILING1].v;
        pointList.add(pol[CEILING1]);
        pointList.add(pol[CEILING2]);
        pointList.add(pol[FLOOR2]);

        return pointList; // 3
    }

    public Mesh getMesh() {
        return mesh;
    }

    private GLSurface addParallaxFloor(FloatArray vertices, int sectnum, int wallnum) {
        final Wall wal = boardService.getWall(wallnum);
        final Sector sec = boardService.getSector(sectnum);

        boolean isParallaxFloor = sec.isParallaxFloor();
        if (!isParallaxFloor) {
            return setNull(lower_skies, wallnum);
        }

        int nextsector = wal.getNextsector();
        boolean isParallaxNext = nextsector != -1 && (boardService.getSector(nextsector).isParallaxFloor());

        GLSurface surf = null;
        if (nextsector == -1 || !isParallaxNext) {
            SurfaceInfo info = tess.getSurface(Type.Sky.setHeinum(Heinum.SkyLower), wallnum, vertices);
            if (info == null) {
                return setNull(lower_skies, wallnum);
            }

            surf = getSurface(lower_skies, wallnum, info.getSize(), info.getLimit());
            if (surf != null) {
                surf.picnum = info.picnum;
                surf.ptr = info.obj;
                surf.type = Type.Floor;
                surf.vis_ptr = sectnum;
                surf.visflag = 0;
            }
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addParallaxCeiling(FloatArray vertices, int sectnum, int wallnum) {
        final Wall wal = boardService.getWall(wallnum);
        final Sector sec = boardService.getSector(sectnum);

        boolean isParallaxCeiling = sec.isParallaxCeiling();
        if (!isParallaxCeiling) {
            return setNull(upper_skies, wallnum);
        }

        int nextsector = wal.getNextsector();
        boolean isParallaxNext = nextsector != -1 && (boardService.getSector(nextsector).isParallaxCeiling());

        GLSurface surf = null;
        if (nextsector == -1 || !isParallaxNext) {
            SurfaceInfo info = tess.getSurface(Type.Sky.setHeinum(Heinum.SkyUpper), wallnum, vertices);

            if (info == null) {
                return setNull(upper_skies, wallnum);
            }

            surf = getSurface(upper_skies, wallnum, info.getSize(), info.getLimit());
            if (surf != null) {
                surf.picnum = info.picnum;
                surf.ptr = info.obj;
                surf.type = Type.Ceiling;
                surf.vis_ptr = sectnum;
                surf.visflag = 0;
            }
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addMiddle(FloatArray vertices, int sectnum, int wallnum) {
        final int nextsector = boardService.getWall(wallnum).getNextsector();
        if (nextsector != -1) {
            return setNull(walls, wallnum);
        }

        SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.MaxWall), wallnum, vertices);
        if (info == null) {
            return setNull(walls, wallnum);
        }

		GLSurface surf = getSurface(walls, wallnum, info.getSize(), info.getLimit());
        if (surf != null) {
            surf.picnum = info.picnum;
            surf.ptr = info.obj;
            surf.type = Type.Wall;
            surf.vis_ptr = sectnum;

            surf.visflag = 0;
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addUpper(FloatArray vertices, int sectnum, int wallnum) {
        final int nextsector = boardService.getWall(wallnum).getNextsector();
        if (nextsector == -1 || (boardService.getSector(nextsector).isParallaxCeiling() && boardService.getSector(sectnum).isParallaxCeiling())) {
            return setNull(upper_walls, wallnum);
        }

        SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Upper), wallnum, vertices);
        if (info == null) {
            return setNull(upper_walls, wallnum);
        }

        GLSurface surf = getSurface(upper_walls, wallnum, info.getSize(), info.getLimit());
        if (surf != null) {
            surf.picnum = info.picnum;
            surf.ptr = info.obj;
            surf.type = Type.Wall;
            surf.vis_ptr = sectnum;

            surf.visflag = 2;
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addLower(FloatArray vertices, int sectnum, int wallnum) {
        final int nextsector = boardService.getWall(wallnum).getNextsector();
        if (nextsector == -1 || (boardService.getSector(nextsector).isParallaxFloor() && boardService.getSector(sectnum).isParallaxFloor())) {
            return setNull(lower_walls, wallnum);
        }

        SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Lower), wallnum, vertices);
        if (info == null) {
            return setNull(lower_walls, wallnum);
        }

        GLSurface surf = getSurface(lower_walls, wallnum, info.getSize(), info.getLimit());
        if (surf != null) {
            surf.picnum = info.picnum;
            surf.ptr = info.obj;
            surf.type = Type.Wall;
            surf.vis_ptr = sectnum;

            surf.visflag = 1;
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addMaskedWall(FloatArray vertices, int sectnum, int wallnum) {
        final Wall wal = boardService.getWall(wallnum);
        GLSurface surf = null;

        if ((wal.isMasked() || wal.isOneWay()) && wal.getNextsector() != -1) {
            SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Portal), wallnum, vertices);
            if (info == null) {
                return setNull(maskwalls, wallnum);
            }

            surf = getSurface(maskwalls, wallnum, info.getSize(), info.getLimit());
            if (surf != null) {
                surf.picnum = info.picnum;
                surf.ptr = info.obj;
                surf.type = Type.Wall;
                surf.vis_ptr = sectnum;
                surf.visflag = 4;
            }
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addFloor(FloatArray vertices, int sectnum) {
        if (boardService.getSector(sectnum).isParallaxFloor()) {
            return setNull(floors, sectnum);
        }

        SurfaceInfo info = tess.getSurface(Type.Floor, sectnum, vertices);
        if (info == null) {
            return setNull(floors, sectnum);
        }

        GLSurface surf = getSurface(floors, sectnum, info.getSize(), info.getLimit());
        if (surf != null) {
            surf.picnum = info.picnum;
            surf.ptr = info.obj;
            surf.type = Type.Floor;
            surf.vis_ptr = sectnum;
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private GLSurface addQuad(FloatArray vertices) {
        SurfaceInfo info = tess.getSurface(Type.Quad, 0, vertices);
        GLSurface surf = new GLSurface(meshOffset);
        surf.count = info.getSize();
        surf.limit = info.getLimit();
        surf.type = Type.Quad;
        surf.primitiveType = GL20.GL_TRIANGLE_FAN;
        meshOffset += surf.limit;
        return surf;
    }

    private GLSurface addCeiling(FloatArray vertices, int sectnum) {
        if (boardService.getSector(sectnum).isParallaxCeiling()) {
            return setNull(ceilings, sectnum);
        }

        SurfaceInfo info = tess.getSurface(Type.Ceiling, sectnum, vertices);
        if (info == null) {
            return setNull(ceilings, sectnum);
        }

        GLSurface surf = getSurface(ceilings, sectnum, info.getSize(), info.getLimit());
        if (surf != null) {
            surf.picnum = info.picnum;
            surf.ptr = info.obj;
            surf.type = Type.Ceiling;
            surf.vis_ptr = sectnum;
        }

        if (surf != null && surf.count == 0) {
            return null;
        }

        return surf;
    }

    private void updateVertices(final int targetOffset, final float[] source, final int sourceOffset, final int count) {
        if (meshByteBuffer.limit() < targetOffset * 4) {
            // Shouldn't be here, but sometimes it's happen. I still don't know why. This limit control by mesh.bind()
            checkValidate();
            meshByteBuffer.limit(targetOffset * 4);
        }
        mesh.updateVertices(targetOffset, source, sourceOffset, count);
    }

    public GLSurface getWall(int wallnum, int sectnum) {
        int hash = getWallHash(sectnum, wallnum);
        if (wallhash[wallnum] != hash) {
            wallhash[wallnum] = hash;

            tess.setSector(sectnum, false);

            vertices.clear();
            GLSurface surf = addMiddle(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            vertices.clear();
            surf = addUpper(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            vertices.clear();
            surf = addLower(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            vertices.clear();
            surf = addMaskedWall(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            vertices.clear();
            surf = addParallaxCeiling(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            vertices.clear();
            surf = addParallaxFloor(vertices, sectnum, wallnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            checkValidate();
        }

        return walls[wallnum];
    }

    protected void checkValidate() {
        if (validateMesh) {
            FloatBuffer buffer = mesh.getVerticesBuffer(true);
            int newLimit = (meshOffset + tess.getMaxVertices()) * tess.getVertexSize();
            if (newLimit > buffer.capacity()) {
                newLimit = buffer.capacity();
            }
            buffer.limit(newLimit);
            validateMesh = false;
        }
    }

    public GLSurface getUpper(int wallnum) {
        return upper_walls[wallnum];
    }

    public GLSurface getLower(int wallnum) {
        return lower_walls[wallnum];
    }

    public GLSurface getMaskedWall(int wallnum) {
        return maskwalls[wallnum];
    }

    public GLSurface getParallaxFloor(int wallnum) {
        return lower_skies[wallnum];
    }

    public GLSurface getParallaxCeiling(int wallnum) {
        return upper_skies[wallnum];
    }

    public GLSurface getQuad() {
        return quad;
    }

    public GLSurface getFloor(int sectnum) {
        int hash = getFloorHash(sectnum);
        GLSurface surf = floors[sectnum];
        if (floorhash[sectnum] != hash) {
            floorhash[sectnum] = hash;

            tess.setSector(sectnum, true);
            vertices.clear();
            surf = addFloor(vertices, sectnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            checkValidate();
        }

        return surf;
    }

    public GLSurface getCeiling(int sectnum) {
        int hash = getCeilingHash(sectnum);
        GLSurface surf = ceilings[sectnum];
        if (ceilinghash[sectnum] != hash) {
            ceilinghash[sectnum] = hash;

            tess.setSector(sectnum, true);
            vertices.clear();
            surf = addCeiling(vertices, sectnum);
            if (surf != null) {
                updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
            }

            checkValidate();
        }

        return surf;
    }

    private int getCeilingHash(int sectnum) {
        int hash = 1;
        final int prime = 31;
        final Sector sec = boardService.getSector(sectnum);

        final int startwall = sec.getWallptr();
        final int endwall = sec.getWallnum() + startwall;
        for (int z = startwall; z < endwall; z++) {
            Wall wal = boardService.getWall(z);
            hash = prime * hash + NumberUtils.floatToIntBits(wal.getX());
            hash = prime * hash + NumberUtils.floatToIntBits(wal.getY());
        }

        hash = prime * hash + NumberUtils.floatToIntBits(sec.getCeilingz());
        hash = prime * hash + sec.getCeilingstat();
        hash = prime * hash + NumberUtils.floatToIntBits(sec.getCeilingheinum());
        hash = prime * hash + sec.getCeilingpicnum();
        hash = prime * hash + sec.getCeilingxpanning();
        hash = prime * hash + sec.getCeilingypanning();

        return hash;
    }

    private int getFloorHash(int sectnum) {
        int hash = 1;
        final int prime = 31;
        final Sector sec = boardService.getSector(sectnum);

        final int startwall = sec.getWallptr();
        final int endwall = sec.getWallnum() + startwall;
        for (int z = startwall; z < endwall; z++) {
            Wall wal = boardService.getWall(z);
            hash = prime * hash + NumberUtils.floatToIntBits(wal.getX());
            hash = prime * hash + NumberUtils.floatToIntBits(wal.getY());
        }

        hash = prime * hash + NumberUtils.floatToIntBits(sec.getFloorz());
        hash = prime * hash + sec.getFloorstat();
        hash = prime * hash + NumberUtils.floatToIntBits(sec.getFloorheinum());
        hash = prime * hash + sec.getFloorpicnum();
        hash = prime * hash + sec.getFloorxpanning();
        hash = prime * hash + sec.getFloorypanning();

        return hash;
    }

    private int getWallHash(int sectnum, int z) {
        final Sector sec = boardService.getSector(sectnum);
        final Wall wal = boardService.getWall(z);
        if (sec == null || wal == null) {
            return 0;
        }

        int hash = 1;
        final int prime = 31;

        hash = prime * hash + NumberUtils.floatToIntBits(wal.getX());
        hash = prime * hash + NumberUtils.floatToIntBits(wal.getY());
        hash = prime * hash + NumberUtils.floatToIntBits(wal.getWall2().getX());
        hash = prime * hash + NumberUtils.floatToIntBits(wal.getWall2().getY());
        hash = prime * hash + wal.getCstat();
        hash = prime * hash + wal.getXpanning();
        hash = prime * hash + wal.getYpanning();
        hash = prime * hash + wal.getXrepeat();
        hash = prime * hash + wal.getYrepeat();
        hash = prime * hash + wal.getPicnum(); // upper texture
        hash = prime * hash + wal.getOverpicnum(); // middle texture

        if (wal.isSwapped() && wal.getNextwall() != -1) {
            final Wall swal = boardService.getWall(wal.getNextwall());
            if (swal != null) {
                hash = prime * hash + swal.getCstat();
                hash = prime * hash + swal.getXpanning();
                hash = prime * hash + swal.getYpanning();
                hash = prime * hash + swal.getXrepeat();
                hash = prime * hash + swal.getYrepeat();
                hash = prime * hash + swal.getPicnum();
            }
        }

        if (((sec.getCeilingstat() | sec.getFloorstat()) & 2) != 0) {
            Wall wal2 = boardService.getWall(sec.getWallptr());
            if (wal2 != null) {
                hash = prime * hash + NumberUtils.floatToIntBits(wal2.getX());
                hash = prime * hash + NumberUtils.floatToIntBits(wal2.getY());
            }
        }

        hash = prime * hash + NumberUtils.floatToIntBits(sec.getFloorz());
        hash = prime * hash + NumberUtils.floatToIntBits(sec.getFloorheinum());
        hash = prime * hash + (sec.isFloorSlope() ? 1 : 0);
        hash = prime * hash + (sec.isParallaxFloor() ? 1 : 0);

        hash = prime * hash + NumberUtils.floatToIntBits(sec.getCeilingz());
        hash = prime * hash + NumberUtils.floatToIntBits(sec.getCeilingheinum());
        hash = prime * hash + (sec.isCeilingSlope() ? 1 : 0);
        hash = prime * hash + (sec.isParallaxCeiling() ? 1 : 0);

        final Sector nsec = boardService.getSector(wal.getNextsector());
        if (nsec != null) {
            hash = prime * hash + NumberUtils.floatToIntBits(nsec.getFloorz());
            hash = prime * hash + NumberUtils.floatToIntBits(nsec.getFloorheinum());
            hash = prime * hash + (nsec.isFloorSlope() ? 1 : 0);
            hash = prime * hash + (nsec.isParallaxFloor() ? 1 : 0);

            hash = prime * hash + NumberUtils.floatToIntBits(nsec.getCeilingz());
            hash = prime * hash + NumberUtils.floatToIntBits(nsec.getCeilingheinum());
            hash = prime * hash + (nsec.isCeilingSlope() ? 1 : 0);
            hash = prime * hash + (nsec.isParallaxCeiling() ? 1 : 0);

            if (((nsec.getCeilingstat() | nsec.getFloorstat()) & 2) != 0) {
                Wall wal2 = boardService.getWall(nsec.getWallptr());
                if (wal2 != null) {
                    hash = prime * hash + NumberUtils.floatToIntBits(wal2.getX());
                    hash = prime * hash + NumberUtils.floatToIntBits(wal2.getY());
                }
            }
        }

        return hash;
    }

    public Wall getWall(int index) {
        return boardService.getWall(index);
    }

    public Sector getSector(int index) {
        return boardService.getSector(index);
    }

    private GLSurface setNull(GLSurface[] array, int num) {
        GLSurface src = array[num];
        if (src != null) {
            src.count = 0;
        }
        return null;
    }

    private GLSurface getSurface(GLSurface[] array, int num, int count, int limit) {
        if (array[num] == null) {
            if (count == 0) {
                return null;
            }

            GLSurface surf = new GLSurface(meshOffset);
            surf.count = count;
            surf.limit = limit;
            meshOffset += surf.limit;
            array[num] = surf;
            if (lastSurf != null) {
                lastSurf.next = surf;
            }
            lastSurf = surf;
            validateMesh = true;

//			if (mesh != null)
//				System.err.println("new meshOffset: " + (meshOffset * tess.getVertexSize()) + " "
//						+ mesh.getVerticesBuffer().limit() + " size: " + mesh.getVerticesBuffer().capacity());
            return surf;
        } else {
            if (mesh == null) { // when initializing
                Console.out.println("Error: Unexpected behavior in mesh initialization, perhaps the map is corrupt",
                        OsdColor.RED);
                meshOffset += limit;
                return null;
            }

            if (array[num].limit < count) {
                int shift = count - array[num].limit;
                shiftFrom(array[num].next, shift);
                meshOffset += shift;
                array[num].limit = count;
            }

            array[num].count = count;
            return array[num];
        }
    }

    public void nextpage() {
        tess.setSector(-1, false);
    }

    private void shiftFrom(GLSurface surf, int shift) {
        if (surf == null) {
            return;
        }

        int size = meshOffset;
        int newSize = size - surf.offset;
        float[] newItems = new float[newSize * tess.getVertexSize()];
        mesh.getVertices(surf.offset * tess.getVertexSize(), newItems);
        surf.offset += shift;
        validateMesh = true;
        updateVertices(surf.offset * tess.getVertexSize(), newItems, 0, newItems.length);

        surf = surf.next;
        while (surf != null) {
            surf.offset += shift;
            surf = surf.next;
        }
    }

    public Vector3[] getPositions(int offset, int count) {
        Vector3[] out = new Vector3[count];
        FloatBuffer buffer = mesh.getVerticesBuffer(false);
        for (int i = 0; i < count; i++) {
            int offs = (offset + i) * tess.getVertexSize();
            out[i] = new Vector3(buffer.get(offs++), buffer.get(offs++), buffer.get(offs++));
        }
        return out;
    }

    public boolean isInvalid() {
        return validateMesh;
    }

    public void dispose() {
        mesh.dispose();
        meshByteBuffer = null;
        validateMesh = true;
    }

    public enum Heinum {
        MaxWall, Max, Lower, Upper, Portal, SkyLower, SkyUpper
    }

    private static class MyMesh extends Mesh {
        public MyMesh(VertexData vertices) {
            super(vertices, new IndexBufferObject(false, 0), false);
        }
    }

    private static class MyVertexBufferObject extends VertexBufferObject {
        protected MyVertexBufferObject(ByteBuffer data, VertexAttributes attributes) {
            super(GL20.GL_DYNAMIC_DRAW, data, false, attributes);
        }
    }

    public class GLSurface {
        public int offset;
        public int count, limit;
        public int visflag = 0; // 1 - lower, 2 - upper, 4 - masked, 0 - white
        public int primitiveType = GL20.GL_TRIANGLES;

        public int picnum;
        protected GLSurface next;
        private Object ptr;
        private Type type;
        private int vis_ptr;

        public GLSurface(int offset) {
            this.offset = offset;
        }

        public int getVisibility() {
            return boardService.getSector(vis_ptr).getVisibility();
        }

        public void render(ShaderProgram shader) {
            mesh.render(shader, primitiveType, offset, count);
        }

        public int getMethod() {
            switch (type) {
                case Floor:
                    return (((Sector) ptr).getFloorstat() >> 7) & 3;
                case Ceiling:
                    return (((Sector) ptr).getCeilingstat() >> 7) & 3;
                case Wall:
                    int method = 0;
                    Wall wal = (Wall) ptr;
                    if (wal.isMasked() && visflag == 4) {
                        method = 1;
                        if (!wal.isOneWay() && wal.isTransparent()) {
                            if (!wal.isTransparent2()) {
                                method = 2;
                            } else {
                                method = 3;
                            }
                        }
                    }
                    return method;
                default:
                    return 0;
            }
        }

        public short getPal() {
            switch (type) {
                case Floor:
                    return ((Sector) ptr).getFloorpal();
                case Ceiling:
                    return ((Sector) ptr).getCeilingpal();
                case Wall:
                    return ((Wall) ptr).getPal();
                default:
                    return 0;
            }
        }

        public byte getShade() {
            switch (type) {
                case Floor:
                    return ((Sector) ptr).getFloorshade();
                case Ceiling:
                    return ((Sector) ptr).getCeilingshade();
                case Wall:
                    return ((Wall) ptr).getShade();
                default:
                    return 0;
            }
        }
    }
}
