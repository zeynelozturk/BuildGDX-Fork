// This file is part of Gdx.
// Copyright (C) 2017-2021  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// Gdx is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Gdx is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Gdx.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Render.TextureHandle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Script.TextureHDInfo;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.Types.font.BitmapFont;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TileFontData;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.graphics.GL20.*;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE0;
import static ru.m210projects.Build.Render.Types.GL10.*;

public class TextureManager {

    protected final Engine engine;
    protected final GLTileArray cache;
    protected final ExpandTexture expand;
    protected final Map<Font, GLTile> fontAtlas;
    protected TextureHDInfo info;
    protected GLTile glPalette; // to shader
    protected GLTile[] glPalookups; // to shader
    protected int texunits = 0;
    protected TexFilter textureFilter = TexFilter.NONE;
    private GLTile bindedTile;

    public TextureManager(Engine engine, ExpandTexture opt) {
        this.engine = engine;
        this.cache = new GLTileArray(MAXTILES);
        this.glPalookups = new GLTile[MAXPALOOKUPS];
        this.expand = opt;
        this.fontAtlas = new HashMap<>();
    }

    public GLTile getBitmapFontAtlas(BitmapFont font) {
        if (!fontAtlas.containsKey(font)) {
            // computeIfAbsent creates lambda every frame
            return fontAtlas.computeIfAbsent(font, e -> createGLBitmap(font));
        }
        return  fontAtlas.get(font);
    }

    protected GLTile createGLBitmap(BitmapFont bitmapFont) {
        final int sizx = bitmapFont.getAtlasWidth();
        final int sizy = bitmapFont.getAtlasHeight();
        final byte[] textfont = bitmapFont.getData();

        TileFontData dat = new TileFontData(sizx, sizy) {
            @Override
            public ByteBuffer buildAtlas(ByteBuffer data) {
                for (int h = 0; h < 256; h++) {
                    int tptr = (h % 16) * 8 + (h / 16) * sizx * 8;
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            data.put(tptr + j, (textfont[h * 8 + i] & pow2char[7 - j]) != 0 ? (byte) 255 : 0);
                        }
                        tptr += sizx;
                    }
                }
                return data;
            }

            @Override
            public int getGLInternalFormat() {
                return GL_ALPHA;
            }

            @Override
            public int getGLFormat() {
                return GL_ALPHA;
            }
        };

        GLTile atlas = newTile(dat, 0, TexFilter.NONE);
        atlas.setupTextureFilter(TexFilter.NONE);
        return atlas;
    }

    public void setTextureInfo(TextureHDInfo info) {
        this.info = info;
    }

    /**
     * @param artEntry tile entry
     * @param dapalnum tile palette
     * @param skybox skybox surface index
     * @param method  0: solid, 1: masked(255 is transparent), 2: transluscent #1,
     *                3: transluscent #2, 4: it's a sprite, so wraparound isn't
     *                needed
     * @return GLTile
     */

    public GLTile get(PixelFormat fmt, ArtEntry artEntry, int dapalnum, int skybox, int method) {
        boolean clamping = clampingMode(method);
        boolean alpha = alphaMode(method);

        Hicreplctyp si = (engine.getConfig().isUseHighTiles() && info != null) ? info.findTexture(artEntry.getNum(), dapalnum, skybox)
                : null;

        if (si == null) {
            if (skybox != 0 || dapalnum >= (MAXPALOOKUPS - RESERVEDPALS)) {
                return null;
            }
        }

        GLTile tile = cache.get(artEntry.getNum(), dapalnum, clamping, skybox);
        if (si != null && tile != null && tile.hicr == null && si.skybox == null) { // GDX 29.05.2020 skybox check added
            // (if you're switching between 8bit and hrp textures, old loaded texture should
            // be disposed. Addon HRP support)
            cache.dispose(artEntry.getNum()); // old 8-bit texture
            tile = null;
        }

        if (tile != null /* && tile.getPixelFormat() == fmt */) {
            if (tile.isInvalidated()) {
                tile.setInvalidated(false);

                TileData data = loadPic(fmt, si, artEntry, dapalnum, clamping, alpha, skybox, textureFilter);
                tile.update(data, fmt == PixelFormat.Pal8 ? 0 : dapalnum, textureFilter);
            }
        } else {
//			if (tile != null)
//				cache.dispose(dapicnum); // old texture

            if (si != null && dapalnum != 0 && info.findTexture(artEntry.getNum(), 0, skybox) == si
                    && (tile = cache.get(artEntry.getNum(), 0, clamping, skybox)) != null) {
                return tile;
            }

            TileData data = loadPic(fmt, si, artEntry, dapalnum, clamping, alpha, skybox, textureFilter);
            if (data == null) {
                return null;
            }

            tile = allocTile(data, si, artEntry, fmt == PixelFormat.Pal8 ? 0 : dapalnum, skybox, alpha, textureFilter);
        }

        if (dapalnum >= (MAXPALOOKUPS - RESERVEDPALS)) {
            activateEffect();
        }

        return tile;
    }

    public PixelFormat getFmt(int dapicnum) {
        GLTile tile = cache.get(dapicnum);
        if (tile != null) {
            return tile.getPixelFormat();
        }
        return null;
    }

    public boolean bind(GLTile tile) {
        if (bindedTile == tile) {
            return false;
        }

        tile.bind();
        return true;
    }

    public void unbind() {
        if (bindedTile != null) {
            bindedTile.unbind();
        }
        bindedTile = null;
    }

    public void precache(PixelFormat fmt, ArtEntry pic, int dapalnum, int method) {
        get(fmt, pic, dapalnum, 0, method);
    }

    public int getTextureUnits() {
        return texunits;
    }

    protected TileData loadPic(PixelFormat fmt, Hicreplctyp hicr, ArtEntry artEntry, int dapalnum, boolean clamping,
                               boolean alpha, int skybox, TexFilter textureFilter) {

        // System.err.println("loadPic " + dapicnum + " " + dapalnum + " clamping: " +
        // clamping);
        if (hicr != null) {
            Entry entry = checkResource(hicr, artEntry.getNum(), skybox);
            if (entry != null && entry.exists()) {
                try {
                    byte[] data = entry.getBytes();
                    return new PixmapTileData(new Pixmap(data, 0, data.length), clamping, expand.get());
                } catch (Throwable t) {
                    t.printStackTrace();
                    if (skybox != 0) {
                        return null;
                    }
                }
            }
        }

        if (fmt == PixelFormat.Pal8) {
            return new IndexedTileData(artEntry, clamping, alpha, expand.get());
        }
        return new RGBTileData(engine.getPaletteManager(), artEntry, dapalnum, clamping, alpha, expand.get(), textureFilter != TexFilter.NONE);
    }

    @Nullable
    protected Entry checkResource(Hicreplctyp hicr, int dapic, int facen) {
        if (hicr == null) {
            return null;
        }

        Entry fn;
        if (facen > 0) {
            if (hicr.skybox == null || facen > 6 || hicr.skybox.face[facen - 1] == null) {
                return null;
            }

            fn = hicr.skybox.face[facen - 1];
        } else {
            fn = hicr.filename;
        }

        if (fn == null || !fn.exists()) {
            Console.out.println("Hightile[" + dapic + "]: File \"" + fn + "\" not found");
            if (facen > 0) {
                hicr.skybox.ignore = 1;
            } else {
                hicr.ignore = 1;
            }
            return null;
        }

        return fn;
    }

    public GLTile newTile(TileData pic, int palnum, TexFilter textureFilter) {
        return new GLTile(pic, palnum, textureFilter) {
            @Override
            public void bind() {
                super.bind();
                bindedTile = this;
            }

            @Override
            public void unbind() {
                super.unbind();
                bindedTile = null;
            }

            @Override
            public void bind(int unit) {
                // Gdx.gl.glActiveTexture(unit);
                Gdx.gl.glBindTexture(glTarget, glHandle);
            }
        };
    }

    public GLTile newTile(PixelFormat fmt, int width, int height) {
        return new GLTile(fmt, width, height) {
            @Override
            public void bind() {
                super.bind();
                bindedTile = this;
            }

            @Override
            public void bind(int unit) {
                // Gdx.gl.glActiveTexture(unit);
                Gdx.gl.glBindTexture(glTarget, glHandle);
            }

            @Override
            public void unbind() {
                super.unbind();
                bindedTile = null;
            }
        };
    }

    protected GLTile allocTile(TileData data, Hicreplctyp si, ArtEntry artEntry, int dapalnum, int skybox, boolean alpha,
                               TexFilter textureFilter) {

        GLTile tile = newTile(data, data.isHighTile() ? si.palnum : dapalnum, textureFilter);
        tile.entry = artEntry;
        if (data.isHighTile()) {
            tile.setHighTile(si);
            tile.setHasAlpha(alpha);
            tile.setSkyboxFace(skybox);

            if (skybox > 0) {
                tile.scalex = tile.getWidth() / 64.0f;
                tile.scaley = tile.getHeight() / 64.0f;
            } else {
                if (data instanceof PixmapTileData) {
                    tile.width = ((PixmapTileData) data).getTileWidth();
                    tile.height = ((PixmapTileData) data).getTileHeight();
                }
                int width = tile.getWidth();
                int height = tile.getHeight();

                tile.scalex = width / ((float) artEntry.getWidth());
                tile.scaley = height / ((float) artEntry.getHeight());
            }
        }
        data.dispose();

        cache.add(tile, artEntry.getNum());
        return tile;
    }

    public void setFilter(TexFilter textureFilter) {
        this.textureFilter = textureFilter;
        for (int i = MAXTILES - 1; i >= 0; i--) {
            cache.setFilter(i, textureFilter);
        }
    }

    public void invalidate(int dapicnum, int dapalnum, boolean clamped) {
        GLTile tile = cache.get(dapicnum, dapalnum, clamped, 0);
        if (tile == null) {
            return;
        }

        if (!tile.isHighTile()) {
            tile.setInvalidated(true);
        }
    }

    public void invalidateall() {
        for (int j = MAXTILES - 1; j >= 0; j--) {
            cache.invalidate(j);
        }
    }

    public boolean clampingMode(int dameth) {
        return ((dameth & 4) >> 2) == 1;
    }

    public boolean alphaMode(int dameth) {
        return (dameth & 256) == 0;
    }

    public void uninit() {
//        Console.out.println("TextureCache uninited!", OsdColor.RED);

        for (int i = MAXTILES - 1; i >= 0; i--) {
            cache.dispose(i);
        }

        // GLAtlas dispose
        for (GLTile tile : fontAtlas.values()) {
            tile.delete();
        }
        fontAtlas.clear();
    }

    public GLTile getLastBinded() {
        return bindedTile;
    }

    public void activateEffect() {
        if (GLInfo.multisample == 0) {
            return;
        }

        Gdx.gl.glActiveTexture(GL_TEXTURE0 + ++texunits);
        Gdx.gl.glEnable(GL_TEXTURE_2D);
    }

    public void deactivateEffects(GL10 gl10) {
        if (GLInfo.multisample == 0) {
            return;
        }

        while (texunits >= 0) {
            gl10.glActiveTexture(GL_TEXTURE0 + texunits);
            gl10.glMatrixMode(GL_TEXTURE);
            gl10.glLoadIdentity();
            gl10.glMatrixMode(GL_MODELVIEW);
            if (texunits > 0) {
                gl10.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0f);
                gl10.glDisable(GL_TEXTURE_2D);
            }
            texunits--;
        }
        texunits = 0;
    }

    public GLTile getPalette() {
        return glPalette;
    }

    // Indexed texture params and methods

    @Nullable
    public GLTile getPalookup(int pal) {
        PaletteManager paletteManager = engine.getPaletteManager();
        if (glPalookups[pal] == null || glPalookups[pal].isInvalidated()) {
            if (!paletteManager.isValidPalette(pal)) {
                return glPalookups[0];
            }

            TileData dat = new LookupData(paletteManager.getPalookupBuffer()[pal]);
            if (glPalookups[pal] != null) {
                glPalookups[pal].setInvalidated(false);
                glPalookups[pal].update(dat, 0, TexFilter.NONE);
            } else {
                glPalookups[pal] = newTile(dat, 0, TexFilter.NONE);
            }

            glPalookups[pal].unsafeSetFilter(TextureFilter.Nearest, TextureFilter.Nearest, true);
        }

        return glPalookups[pal];
    }

    public void disposePalette() {
        glPalette.dispose();
        glPalette = null;
        for (int i = 0; i < MAXPALOOKUPS; i++) {
            if (glPalookups[i] != null) {
                glPalookups[i].dispose();
                glPalookups[i] = null;
            }
        }
    }

    public void changePalette(byte[] pal) {
        TileData dat = new PaletteData(pal);
        if (glPalette != null) {
            glPalette.update(dat, 0, TexFilter.NONE);
        } else {
            glPalette = newTile(dat, 0, TexFilter.NONE);
        }

        glPalette.unsafeSetFilter(TextureFilter.Nearest, TextureFilter.Nearest, true);
    }

    public void invalidatepalookup(int pal) {
        if (glPalookups[pal] != null) {
            glPalookups[pal].setInvalidated(true);
        }
    }

    public enum ExpandTexture {
        Horizontal(1), Vertical(2), Both(1 | 2);

        private final byte bit;

        ExpandTexture(int bit) {
            this.bit = (byte) bit;
        }

        public byte get() {
            return bit;
        }
    }

    private abstract static class ShaderData extends DummyTileData {

        public ShaderData(byte[] buf, int w, int h, int bytes) {
            super(bytes != 1 ? PixelFormat.Rgb : PixelFormat.Pal8, w, h);
            data.clear();
            data.put(buf, 0, Math.min(data.capacity(), buf.length));
        }

        @Override
        public boolean hasAlpha() {
            return false;
        }
    }

    private static class PaletteData extends ShaderData {
        public PaletteData(byte[] data) {
            super(data, 256, 1, 3);
        }

        @Override
        public PixelFormat getPixelFormat() {
            return PixelFormat.Pal8;
        }
    }

    private static class LookupData extends ShaderData {
        public LookupData(byte[] data) {
            super(data, 256, 64, 1);
        }

        @Override
        public int getGLFormat() {
            return GL_LUMINANCE;
        }
    }
}
