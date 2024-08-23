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

package ru.m210projects.Build.Render.TextureHandle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.MathUtils;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.nio.ByteBuffer;

import static com.badlogic.gdx.graphics.GL20.*;
import static ru.m210projects.Build.Engine.DETAILPAL;
import static ru.m210projects.Build.Engine.GLOWPAL;
import static ru.m210projects.Build.Render.GLInfo.gltexmaxsize;
import static ru.m210projects.Build.Render.GLInfo.supportsGenerateMipmaps;

public class GLTile extends GLTexture implements Comparable<GLTile> {

    protected int width, height;
    protected PixelFormat fmt;
    protected int flags;
    protected byte skyface;
    protected Hicreplctyp hicr;
    protected float scalex, scaley;
    protected int palnum;
    protected GLTile next;
    private boolean isAllocated;
    protected ArtEntry entry; // for debug info. Actually is not used

    protected GLTile(PixelFormat fmt, int width, int height) {
        super(GL_TEXTURE_2D);
        this.width = width;
        this.height = height;
        this.fmt = fmt;
        this.isAllocated = false;

        this.scalex = this.scaley = 1.0f;
    }

    protected GLTile(GLTile src) {
        super(src.glTarget, src.glHandle);
        this.next = null;
        this.width = src.width;
        this.height = src.height;
        this.fmt = src.fmt;
        this.isAllocated = src.isAllocated;
        this.palnum = src.palnum;
        this.anisotropicFilterLevel = src.anisotropicFilterLevel;
        this.flags = src.flags;
        this.skyface = src.skyface;
        this.hicr = src.hicr;

        this.scalex = src.scalex;
        this.scaley = src.scaley;
    }

    public GLTile(TileData pic, int palnum, TexFilter textureFilter) {
        this(pic.getPixelFormat(), pic.getWidth(), pic.getHeight());
        this.palnum = palnum;

        alloc(pic, textureFilter);
        if (textureFilter.isMipmaps()) {
            generateMipmap(pic, true);
        }

        setClamped(pic.isClamped());
        setHasAlpha(pic.hasAlpha());

        this.scalex = this.scaley = 1.0f;
    }

    protected void alloc(TileData pic, TexFilter filter) {
        Gdx.gl.glBindTexture(glTarget, glHandle);

        Gdx.gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        Gdx.gl.glTexImage2D(glTarget, 0, pic.getGLInternalFormat(), pic.getWidth(), pic.getHeight(), 0,
                pic.getGLFormat(), pic.getGLType(), pic.getPixels());

        setupTextureFilter(filter);
        setupTextureWrap(!pic.isClamped() ? TextureWrap.Repeat : TextureWrap.ClampToEdge);

        this.isAllocated = true;
    }

    public PixelFormat getPixelFormat() {
        return fmt;
    }

    public void update(TileData pic, int pal, TexFilter textureFilter) {
        this.bind();

        if (pic != null) {
            int width = pic.getWidth();
            int height = pic.getHeight();
            if (pic instanceof PixmapTileData) {
                width = ((PixmapTileData) pic).getTileWidth();
                height = ((PixmapTileData) pic).getTileHeight();
            }

            // Realloc, because the texture size isn't match
            if ((getWidth() != width || getHeight() != height)) {
                delete();

                this.glHandle = Gdx.gl.glGenTexture();
                this.width = pic.getWidth();
                this.height = pic.getHeight();

                alloc(pic, textureFilter);
            } else {
                if (!isAllocated) {
                    alloc(pic, textureFilter);
                } else {
                    Gdx.gl.glTexSubImage2D(glTarget, 0, 0, 0, pic.getWidth(), pic.getHeight(), pic.getGLFormat(),
                            GL_UNSIGNED_BYTE, pic.getPixels());
                }
            }

            if (textureFilter.isMipmaps()) {
                generateMipmap(pic, false);
            }

            setClamped(pic.isClamped());
            setHasAlpha(pic.hasAlpha());

            pic.dispose();
        }

        this.palnum = pal;
    }

    protected int calcMipLevel(int xsiz, int ysiz, int maxsize) {
        int mipLevel = 0;
        while ((xsiz >> mipLevel) > (1 << maxsize) || (ysiz >> mipLevel) > (1 << maxsize)) {
            mipLevel++;
        }
        return mipLevel;
    }

    protected void generateMipmap(TileData data, boolean doalloc) {
        if (supportsGenerateMipmaps) {
            Gdx.gl.glGenerateMipmap(glTarget);
            return;
        }

        int mipLevel = calcMipLevel(data.getWidth(), data.getHeight(), gltexmaxsize);

        int x2 = data.getWidth(), x3;
        int y2 = data.getHeight(), y3;
        int r, g, b, a, k, wpptr, rpptr, wp, rp, index, rgb;

        ByteBuffer pic = data.getPixels();
        for (int j = 1, x, y; (x2 > 1) || (y2 > 1); j++) {
            x3 = Math.max(1, x2 >> 1);
            y3 = Math.max(1, y2 >> 1); // this came from the GL_ARB_texture_non_power_of_two spec
            for (y = 0; y < y3; y++) {
                wpptr = y * x3;
                rpptr = (y << 1) * x2;
                for (x = 0; x < x3; x++, wpptr++, rpptr += 2) {
                    wp = wpptr << 2;
                    rp = rpptr << 2;
                    r = g = b = a = k = 0;

                    index = rp;
                    if (pic.get(index + 3) != 0) {
                        r += pic.get(index + 0) & 0xFF;
                        g += pic.get(index + 1) & 0xFF;
                        b += pic.get(index + 2) & 0xFF;
                        a += pic.get(index + 3) & 0xFF;
                        k++;
                    }
                    index = rp + 4;
                    if (((x << 1) + 1 < x2) && (pic.get(index + 3) != 0)) {
                        r += pic.get(index + 0) & 0xFF;
                        g += pic.get(index + 1) & 0xFF;
                        b += pic.get(index + 2) & 0xFF;
                        a += pic.get(index + 3) & 0xFF;
                        k++;
                    }
                    if ((y << 1) + 1 < y2) {
                        index = rp + (x2 << 2);
                        if (pic.get(index + 3) != 0) {
                            r += pic.get(index + 0) & 0xFF;
                            g += pic.get(index + 1) & 0xFF;
                            b += pic.get(index + 2) & 0xFF;
                            a += pic.get(index + 3) & 0xFF;
                            k++;
                        }

                        index = rp + ((x2 + 1) << 2);
                        if (((x << 1) + 1 < x2) && pic.get(index + 3) != 0) {
                            r += pic.get(index + 0) & 0xFF;
                            g += pic.get(index + 1) & 0xFF;
                            b += pic.get(index + 2) & 0xFF;
                            a += pic.get(index + 3) & 0xFF;
                            k++;
                        }
                    }
                    switch (k) {
                        case 0:
                        case 1:
                            rgb = ((a) << 24) + ((b) << 16) + ((g) << 8) + ((r));
                            break;
                        case 2:
                            rgb = (((a + 1) >> 1) << 24) + (((b + 1) >> 1) << 16) + (((g + 1) >> 1) << 8)
                                    + (((r + 1) >> 1));
                            break;
                        case 3:
                            rgb = (((a * 85 + 128) >> 8) << 24) + (((b * 85 + 128) >> 8) << 16)
                                    + (((g * 85 + 128) >> 8) << 8) + (((r * 85 + 128) >> 8));
                            break;
                        case 4:
                            rgb = (((a + 2) >> 2) << 24) + (((b + 2) >> 2) << 16) + (((g + 2) >> 2) << 8)
                                    + (((r + 2) >> 2));
                            break;
                        default:
                            continue;
                    }

                    pic.putInt(wp, rgb);
                }
            }

            if (j >= mipLevel) {
                if (doalloc) {
                    Gdx.gl.glTexImage2D(GL_TEXTURE_2D, j - mipLevel, data.getGLInternalFormat(), x3, y3, 0,
                            data.getGLFormat(), GL_UNSIGNED_BYTE, pic); // loading 1st time
                } else {
                    Gdx.gl.glTexSubImage2D(GL_TEXTURE_2D, j - mipLevel, 0, 0, x3, y3, data.getGLFormat(),
                            GL_UNSIGNED_BYTE, pic); // overwrite old texture
                }
            }
            x2 = x3;
            y2 = y3;
        }

    }

    public void setupTextureWrap(TextureWrap wrap) {
        unsafeSetWrap(wrap, wrap, true);
    }

    public void setupTextureWrapS(TextureWrap wrap) {
        unsafeSetWrap(wrap, null, true);
    }

    public void setupTextureWrapT(TextureWrap wrap) {
        unsafeSetWrap(null, wrap, true);
    }

    public void setupTextureFilter(TexFilter filter) {
        if (fmt == PixelFormat.Pal8) {
            filter = TexFilter.NONE;
        }
        unsafeSetFilter(filter.getMin(), filter.getMag(), true);
        unsafeSetAnisotropicFilter(filter.getAnisotropy(), true);
    }

    public float unsafeSetAnisotropicFilter(float level, boolean force) {
        if (fmt == PixelFormat.Pal8) {
            return 1.0f;
        }

        // 1 if you want to disable anisotropy
        float max = GLInfo.getMaxAnisotropicFilterLevel();
        if (max == 1.0f) {
            return 1.0f;
        }

        level = Math.min(level, max);
        if (!force && MathUtils.isEqual(level, anisotropicFilterLevel, 0.1f)) {
            return anisotropicFilterLevel;
        }
        Gdx.gl.glTexParameterf(glTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, level);
        return anisotropicFilterLevel = level;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void unbind() {
        Gdx.gl.glBindTexture(glTarget, 0);
    }

    @Override
    public void bind() {
        Gdx.gl.glBindTexture(glTarget, glHandle);
    }

    @Override
    public void delete() {
        if (glHandle != 0) {
            if (Gdx.gl != null) {
                Gdx.gl.glDeleteTexture(glHandle);
            }
            glHandle = 0;
        }
    }

    @Override
    public GLTile clone() {
        return new GLTile(this);
    }

    public boolean isClamped() {
        return FlagType.Clamped.hasBit(flags);
    }

    public void setClamped(boolean mode) {
        setBit(mode, FlagType.Clamped);
    }

    public boolean isHighTile() {
        return FlagType.HighTile.hasBit(flags);
    }

    public void setHighTile(Hicreplctyp si) {
        this.hicr = si;
        setBit(si != null, FlagType.HighTile);
    }

    public boolean isSkyboxFace() {
        return FlagType.SkyboxFace.hasBit(flags);
    }

    public void setSkyboxFace(int facen) {
        this.skyface = (byte) facen;
        if (facen > 0) {
            setBit(true, FlagType.SkyboxFace);
        }
    }

    public boolean hasAlpha() {
        return FlagType.HasAlpha.hasBit(flags);
    }

    public void setHasAlpha(boolean mode) {
        setBit(mode, FlagType.HasAlpha);
    }

    public boolean isInvalidated() {
        return FlagType.Invalidated.hasBit(flags);
    }

    public void setInvalidated(boolean mode) {
        setBit(mode, FlagType.Invalidated);
    }

    public int getPal() {
        return palnum;
    }

    public float getHiresXScale() {
        return hicr.xscale;
    }

    public float getHiresYScale() {
        return hicr.yscale;
    }

    public boolean isGlowTexture() {
        return hicr != null && (hicr.palnum == GLOWPAL);
    }

    public boolean isDetailTexture() {
        return hicr != null && (hicr.palnum == DETAILPAL);
    }

    public float getXScale() {
        return scalex;
    }

    public float getYScale() {
        return scaley;
    }

    public float getAlphaCut() {
        return hicr != null ? hicr.alphacut : 0.0f;
    }

    private void setBit(boolean mode, FlagType bit) {
        if (mode) {
            flags |= bit.getBit();
        } else {
            flags &= ~bit.getBit();
        }
    }

    @Override
    public String toString() {
        String out = "id = " + glHandle + " [" + width + "x" + height + ", ";
        out += "pal = " + palnum + ", ";
        out += "clamp = " + isClamped() + "]";

        return out;
    }

    @Override
    public int compareTo(GLTile src) {
        if (src == null) {
            return 0;
        }

        return this.palnum - src.palnum;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {
    }

    public enum FlagType {
        Clamped(0), HighTile(1), SkyboxFace(2), HasAlpha(3), Invalidated(7);

        private final int bit;

        FlagType(int bit) {
            this.bit = (1 << bit);
        }

        public int getBit() {
            return bit;
        }

        public boolean hasBit(int flags) {
            return (flags & bit) != 0;
        }
    }
}
