// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Script;

import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.Render.ModelHandle.MDInfo;
import ru.m210projects.Build.Render.ModelHandle.ModelInfo;
import ru.m210projects.Build.Render.ModelHandle.VoxelInfo;
import ru.m210projects.Build.Render.Types.Hudtyp;
import ru.m210projects.Build.Render.Types.Tile2model;
import ru.m210projects.Build.Types.collections.DynamicArray;

import static ru.m210projects.Build.Engine.MAXSPRITESV7;
import static ru.m210projects.Build.Engine.MAXTILES;

public class ModelsInfo {

    private final DynamicArray<Tile2model> cache = new DynamicArray<>(MAXTILES, Tile2model.class, false);
    private final Hudtyp[][] hudInfo = new Hudtyp[2][MAXTILES];
    private final Array<Spritesmooth> spritesmooth = new DynamicArray<>(MAXSPRITESV7, Spritesmooth.class);
    private final Array<SpriteAnim> spriteanim = new DynamicArray<>(MAXSPRITESV7, SpriteAnim.class);

    public ModelsInfo() {
    }

    public ModelsInfo(ModelsInfo src, boolean disposable) {
        for (int i = 0; i < src.cache.size; i++) {
            Tile2model t2m = src.cache.get(i);
            if (t2m != null) {
                cache.set(i, t2m.clone(disposable));
            }
        }

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < MAXTILES; j++) {
                if (src.hudInfo[i] != null && src.hudInfo[i][j] != null) {
                    hudInfo[i][j] = src.hudInfo[i][j].clone();
                }
            }
        }
    }

    public Spritesmooth getSmoothParams(int i) {
        return spritesmooth.get(i);
    }

    public SpriteAnim getAnimParams(int i) {
        return spriteanim.get(i);
    }

    public ModelInfo getModelInfo(int picnum) {
        Tile2model t2m = cache.get(picnum);
        if (t2m != null) {
            return t2m.model;
        }
        return null;
    }

    public VoxelInfo getVoxelInfo(int picnum) {
        Tile2model t2m = cache.get(picnum);
        if (t2m != null) {
            return t2m.voxel;
        }
        return null;
    }

    public int getTile(ModelInfo model) {
        for (int i = cache.size - 1; i >= 0; i--) {
            Tile2model t2m = cache.get(i);
            if (t2m != null && t2m.model == model) {
                return i;
            }
        }

        return -1;
    }

    @Nullable
    public Tile2model getParams(int picnum) {
        return cache.get(picnum);
    }

    public Hudtyp getHudInfo(int picnum, int flags) {
        if (hudInfo[(flags >> 2) & 1] != null) {
            return hudInfo[(flags >> 2) & 1][picnum];
        }

        return null;
    }

    protected Tile2model getCache(int picnum, int pal) {
        Tile2model t2m = cache.get(picnum);
        if (t2m == null) {
            t2m = cache.getInstance(picnum);
            t2m.palette = pal;
            return t2m;
        } else {
            if (t2m.palette == pal) {
                return t2m;
            }

            Tile2model n = t2m;
            while (n.next != null) {
                n = n.next;
                if (n.palette == pal) {
                    return n;
                }
            }

            Tile2model current = new Tile2model();
            current.palette = pal;
            n.next = current;
            return current;
        }
    }

    public int addModelInfo(ModelInfo md, int picnum, String framename, int skinnum, float smooth, int pal) {
        if (md == null) {
            return -1;
        }

        int i = -3;
        switch (md.getType()) {
            case Voxel:
                smooth = skinnum = i = 0;
                break;
            case Md2:
            case Md3:
                if (framename == null) {
                    return (-3);
                }

                i = ((MDInfo) md).getFrameIndex(framename);
                break;
        }

        Tile2model current = getCache(picnum, pal);
        current.model = md;
        current.framenum = i;
        current.skinnum = skinnum;
        current.smoothduration = smooth;

        return i;
    }

    public int addVoxelInfo(VoxelInfo md, int picnum) {
        if (md == null) {
            return -1;
        }

        Tile2model t2m = cache.getInstance(picnum);
        t2m.voxel = md;
        return 0;
    }

    public void removeModelInfo(ModelInfo md) {
        for (int i = cache.size - 1; i >= 0; i--) {
            Tile2model t2m = cache.get(i);
            if (t2m != null && t2m.model == md) {
                t2m.model = null;
            }
        }
    }

    public int addHudInfo(int tilex, double xadd, double yadd, double zadd, short angadd, int flags, int fov) {
        if (tilex >= MAXTILES) {
            return -2;
        }

        if (hudInfo[(flags >> 2) & 1] == null || hudInfo[(flags >> 2) & 1][tilex] == null) {
            hudInfo[(flags >> 2) & 1][tilex] = new Hudtyp();
        }

        Hudtyp hud = hudInfo[(flags >> 2) & 1][tilex];

        hud.xadd = (float) xadd;
        hud.yadd = (float) yadd;
        hud.zadd = (float) zadd;
        hud.angadd = (short) (angadd | 2048);
        hud.flags = (short) flags;
        hud.fov = (short) fov;

        return 0;
    }

    public void dispose() {
        for (int i = cache.size - 1; i >= 0; i--) {
            Tile2model t2m = cache.get(i);
            if (t2m == null) {
                continue;
            }

            if (!t2m.disposable) {
                continue;
            }

            cache.set(i, null);
        }
    }

    public static class Spritesmooth {
        public float smoothduration;
        public short mdcurframe;
        public short mdoldframe;
        public short mdsmooth;
    }

    public static class SpriteAnim {
        public long mdanimtims;
        public short mdanimcur;

    }
}
