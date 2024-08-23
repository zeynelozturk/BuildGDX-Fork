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

package ru.m210projects.Build.Render.Polymost;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MD2.MD2Info;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MD2.MD2ModelGL10;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MD3.MD3Info;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MD3.MD3ModelGL10;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDModel;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDSkinmap;
import ru.m210projects.Build.Render.ModelHandle.ModelInfo;
import ru.m210projects.Build.Render.ModelHandle.ModelManager;
import ru.m210projects.Build.Render.ModelHandle.Voxel.GLVoxel;
import ru.m210projects.Build.Render.ModelHandle.Voxel.VoxelData;
import ru.m210projects.Build.Render.ModelHandle.Voxel.VoxelGL10;
import ru.m210projects.Build.Render.ModelHandle.Voxel.VoxelSkin;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.Hicreplctyp;
import ru.m210projects.Build.Render.TextureHandle.PixmapTileData;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.filehandle.Cache;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Render.Types.GL10.GL_MODELVIEW;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE0;

public class PolymostModelManager extends ModelManager {

    protected Polymost parent;

    public PolymostModelManager(Polymost parent) {
        super(parent.getEngine());
        this.parent = parent;
    }

    @Override
    public GLVoxel allocateVoxel(VoxelData vox, int voxmip, int flags) {
        return new VoxelGL10(parent.gl, vox, voxmip, flags, true) {

            @Override
            public GLTile getSkin(int pal) {
                PixelFormat fmt = parent.getTextureFormat();
                if (!parent.getPaletteManager().isValidPalette(pal) || fmt == PixelFormat.Pal8) {
                    pal = 0;
                }

                if (texid[pal] == null) {
//					long startticks = System.nanoTime();
                    TileData dat = new VoxelSkin(fmt, parent.getPaletteManager(), skinData, pal);
                    GLTile dst = parent.textureCache.newTile(dat, pal, TexFilter.NONE);

                    dst.unsafeSetFilter(TextureFilter.Nearest, TextureFilter.Nearest, true);
                    dst.unsafeSetAnisotropicFilter(1, true);
                    texid[pal] = dst;
//					long etime = System.nanoTime() - startticks;
//					System.out.println("Load voxskin: pal" + pal + " for tile " + getTile(this) + "... "
//							+ (etime / 1000000.0f) + " ms");
                }

                return texid[pal];
            }

            @Override
            public void setTextureParameters(GLTile tile, int pal, int shade, int visibility, float alpha) {
                if (tile.getPixelFormat() == PixelFormat.Pal8) {
                    if (!parent.getShader().isBinded()) {
                        parent.getShader().bind();
                    }
                    parent.getShader().setTextureParams(pal, shade);
                    parent.getShader().setDrawLastIndex(true);
                    parent.getShader().setTransparent(alpha);
                    parent.getShader().setVisibility(visibility);
                }
            }
        };
    }

    @Override
    public GLModel allocateModel(ModelInfo modelInfo) {
        try {
            switch (modelInfo.getType()) {
                case Md3:
                    return new MD3ModelGL10(parent.gl, (MD3Info) modelInfo) {
                        @Override
                        protected GLTile loadTexture(String skinfile, int palnum) {
                            return loadMDTexture(this, skinfile, palnum);
                        }

                        @Override
                        protected int bindSkin(int pal, int skinnum, int surfnum) {
                            return bindMDSkin(this, pal, skinnum, surfnum);
                        }
                    };
                case Md2:
                    return new MD2ModelGL10(parent.gl, (MD2Info) modelInfo) {
                        @Override
                        protected int bindSkin(int pal, int skinnum) {
                            return bindMDSkin(this, pal, skinnum, 0);
                        }

                        @Override
                        protected GLTile loadTexture(String skinfile, int palnum) {
                            return loadMDTexture(this, skinfile, palnum);
                        }

                    };
                default:
                    return null;
            }
        } catch (Exception e) {
            Console.out.println(e.toString(), OsdColor.RED);
        }
        return null;
    }

    protected GLTile loadMDTexture(MDModel m, String skinfile, int palnum) {
        GLTile texidx = findLoadedMultitexture(skinfile, palnum);
        if (texidx != null) {
            return texidx;
        }

        Entry res = Cache.getInstance().getEntry(skinfile, true);
        if (!res.exists()) {
            Console.out.println("Skin " + skinfile + " not found.", OsdColor.YELLOW);
            return null;
        }

//		long startticks = System.currentTimeMillis();
        try {
            byte[] data = res.getBytes();
            Pixmap pix = new Pixmap(data, 0, data.length);
            texidx = parent.textureCache.newTile(new PixmapTileData(pix, true, 0), 0, parent.getConfig().getGlfilter());
            if (palnum == DETAILPAL || palnum == GLOWPAL) {
                texidx.setHighTile(new Hicreplctyp(palnum));
            }
            m.usesalpha = true;
        } catch (Exception e) {
            Console.out.println("Couldn't load file: " + skinfile, OsdColor.YELLOW);
            return null;
        }
        texidx.setupTextureWrap(TextureWrap.Repeat);

//		long etime = System.currentTimeMillis() - startticks;
//		System.out.println(
//				"Load skin: p" + palnum + " \"" + skinfile + "\"... " + etime + " ms");

        return texidx;
    }

    protected int bindMDSkin(MDModel m, int pal, int skinnum, int surfnum) {
        int texunits = -1;
        GLTile texid = m.getSkin(pal, skinnum, surfnum);
        if (texid != null) {
            parent.bind(texid);

            texunits = GL_TEXTURE0;
            if (parent.getConfig().isDetailMapping()) {
                if ((texid = m.getSkin(DETAILPAL, skinnum, surfnum)) != null) {
                    if (!texid.isDetailTexture()) {
                        System.err.println("Wtf detail!");
                    }
                    Gdx.gl.glActiveTexture(++texunits);
                    Gdx.gl.glEnable(GL_TEXTURE_2D);
                    parent.bind(texid);
                    parent.setupTextureDetail(texid);

                    for (MDSkinmap sk = m.skinmap; sk != null; sk = sk.next) {
                        if (sk.palette == DETAILPAL && skinnum == sk.skinnum && surfnum == sk.surfnum) {
                            float f = sk.param;
                            parent.gl.glMatrixMode(GL_TEXTURE);
                            parent.gl.glLoadIdentity();
                            parent.gl.glScalef(f, f, 1.0f);
                            parent.gl.glMatrixMode(GL_MODELVIEW);
                        }
                    }
                }
            }

            if (parent.getConfig().isGlowMapping()) {
                if ((texid = m.getSkin(GLOWPAL, skinnum, surfnum)) != null) {
                    if (!texid.isGlowTexture()) {
                        System.err.println("Wtf glow! " + surfnum);
                    }

                    Gdx.gl.glActiveTexture(++texunits);
                    Gdx.gl.glEnable(GL_TEXTURE_2D);
                    parent.bind(texid);
                    parent.setupTextureGlow(texid);
                }
            }
        }

        return texunits;
    }

    protected GLTile findLoadedMultitexture(String skinfile, int palnum) {
        // possibly fetch an already loaded multitexture :_)
        if (palnum >= (MAXPALOOKUPS - RESERVEDPALS)) {
            for (int i = MAXTILES - 1; i >= 0; i--) {
                GLModel m = models[i];
                if (!(m instanceof MDModel)) {
                    continue;
                }

                for (MDSkinmap sk = ((MDModel) m).skinmap; sk != null; sk = sk.next) {
                    if (sk.fn.equalsIgnoreCase(skinfile) && sk.texid != null) {
                        if (sk.palette != palnum) {
                            GLTile texidx = sk.texid.clone();
                            if (palnum == DETAILPAL || palnum == GLOWPAL) {
                                texidx.setHighTile(new Hicreplctyp(palnum));
                                texidx.update(null, palnum, parent.getConfig().getGlfilter());
                                return texidx;
                            }
                        }

                        return sk.texid;
                    }
                }
            }
        }

        return null;
    }

}
