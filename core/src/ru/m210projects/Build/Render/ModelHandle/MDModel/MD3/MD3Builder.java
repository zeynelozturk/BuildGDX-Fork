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

package ru.m210projects.Build.Render.ModelHandle.MDModel.MD3;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;

public class MD3Builder {

    public MD3Header head;
    public MD3Frame[] frames;
    public HashMap<String, Matrix4>[] tags;
    public MD3Surface[] surfaces;

    public MD3Builder(MD3Info md) throws IOException {
        Entry bb = md.getFileEntry();
        this.head = md.header;

        MD3Frame[] frames = loadFrames(head, bb);
        HashMap<String, Matrix4>[] tags = loadTags(head, bb);
        MD3Surface[] surfaces = loadSurfaces(head, bb);

        this.frames = frames;
        this.tags = tags;
        this.surfaces = surfaces;
    }

    private MD3Frame[] loadFrames(MD3Header header, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, header.offsetFrames);
            MD3Frame[] out = new MD3Frame[header.numFrames];
            for (int i = 0; i < header.numFrames; i++) {
                MD3Frame frame = new MD3Frame();
                frame.min = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                frame.max = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                frame.origin = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                frame.radius = StreamUtils.readFloat(is);
                frame.name = StreamUtils.readString(is, 16);
                out[i] = frame;
            }
            return out;
        }
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Matrix4>[] loadTags(MD3Header header, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, header.offsetTags);
            HashMap<String, Matrix4>[] out = new HashMap[header.numFrames];
            for (int k = 0; k < header.numFrames; k++) {
                out[k] = new HashMap<>();
                for (int i = 0; i < header.numTags; i++) {
                    String tagName = StreamUtils.readString(is, 64);

                    Vector3 pos = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                    Vector3 xAxis = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                    Vector3 yAxis = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                    Vector3 zAxis = new Vector3(StreamUtils.readFloat(is), StreamUtils.readFloat(is), StreamUtils.readFloat(is));
                    Matrix4 mat = new Matrix4();
                    mat.set(xAxis, yAxis, zAxis, pos);

                    out[k].put(tagName, mat);
                }
            }
            return out;
        }
    }

    private MD3Surface[] loadSurfaces(MD3Header header, Entry bb) throws IOException {

        int offsetSurfaces = header.offsetSurfaces;
        MD3Surface[] out = new MD3Surface[header.numSurfaces];
        for (int i = 0; i < header.numSurfaces; i++) {
            try(InputStream is = bb.getInputStream()) {
                StreamUtils.skip(is, offsetSurfaces);
                MD3Surface surf = new MD3Surface();
                surf.id = StreamUtils.readInt(is);
                surf.nam = StreamUtils.readString(is, 64);
                surf.flags = StreamUtils.readInt(is);
                surf.numframes = StreamUtils.readInt(is);
                surf.numshaders = StreamUtils.readInt(is);
                surf.numverts = StreamUtils.readInt(is);
                surf.numtris = StreamUtils.readInt(is);
                surf.ofstris = StreamUtils.readInt(is);
                surf.ofsshaders = StreamUtils.readInt(is);
                surf.ofsuv = StreamUtils.readInt(is);
                surf.ofsxyzn = StreamUtils.readInt(is);
                surf.ofsend = StreamUtils.readInt(is);

                surf.tris = loadTriangles(surf, offsetSurfaces, bb);
                surf.shaders = loadShaders(surf, offsetSurfaces, bb);
                surf.uv = loadUVs(surf, offsetSurfaces, bb);
                surf.xyzn = loadVertices(surf, offsetSurfaces, bb);
                offsetSurfaces += surf.ofsend;

                out[i] = surf;
            }
        }
        return out;
    }

    private int[][] loadTriangles(MD3Surface surf, int offsetSurfaces, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, offsetSurfaces + surf.ofstris);
            int[][] out = new int[surf.numtris][3];
            for (int i = 0; i < surf.numtris; i++) {
                out[i][0] = StreamUtils.readInt(is);
                out[i][1] = StreamUtils.readInt(is);
                out[i][2] = StreamUtils.readInt(is);
            }
            return out;
        }
    }

    private FloatBuffer loadUVs(MD3Surface surf, int offsetSurfaces, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, offsetSurfaces + surf.ofsuv);
            FloatBuffer out = BufferUtils.newFloatBuffer(2 * surf.numverts);
            for (int i = 0; i < surf.numverts; i++) {
                out.put(StreamUtils.readFloat(is));
                out.put(StreamUtils.readFloat(is));
            }
            out.flip();
            return out;
        }
    }

//	private MD3Vertice[] loadVertices(MD3Surface surf, int offsetSurfaces, Entry bb) throws IOException {
//		try(InputStream is = bb.getInputStream()) {
//			StreamUtils.skip(is, offsetSurfaces + surf.ofsxyzn);
//			MD3Vertice[] out = new MD3Vertice[surf.numframes * surf.numverts];
//			for (int i = 0; i < out.length; i++) {
//				MD3Vertice xyzn = new MD3Vertice();
//				xyzn.x = (short) StreamUtils.readShort(is);
//				xyzn.y = (short) StreamUtils.readShort(is);
//				xyzn.z = (short) StreamUtils.readShort(is);
//				xyzn.nlat = (short) (StreamUtils.readByte(is));
//				xyzn.nlng = (short) (StreamUtils.readByte(is));
//				out[i] = xyzn;
//			}
//			return out;
//		}
//	}

    private MD3Vertice[][] loadVertices(MD3Surface surf, int offsetSurfaces, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, offsetSurfaces + surf.ofsxyzn);
            MD3Vertice[][] out = new MD3Vertice[surf.numframes][surf.numverts];
            for (int i = 0; i < surf.numframes; i++) {
                for (int j = 0; j < surf.numverts; j++) {
                    MD3Vertice xyzn = new MD3Vertice();
                    xyzn.x = (short) StreamUtils.readShort(is);
                    xyzn.y = (short) StreamUtils.readShort(is);
                    xyzn.z = (short) StreamUtils.readShort(is);
                    xyzn.nlat = (short) StreamUtils.readUnsignedByte(is);
                    xyzn.nlng = (short) StreamUtils.readUnsignedByte(is);
                    out[i][j] = xyzn;
                }
            }
            return out;
        }
    }

    private MD3Shader[] loadShaders(MD3Surface surf, int offsetSurfaces, Entry bb) throws IOException {
        try (InputStream is = bb.getInputStream()) {
            StreamUtils.skip(is, offsetSurfaces + surf.ofsshaders);
            MD3Shader[] out = new MD3Shader[surf.numshaders];
            for (int i = 0; i < surf.numshaders; i++) {
                MD3Shader shader = new MD3Shader();
                shader.name = StreamUtils.readString(is, 64);
                shader.index = StreamUtils.readInt(is);
                out[i] = shader;
            }
            return out;
        }
    }
}
