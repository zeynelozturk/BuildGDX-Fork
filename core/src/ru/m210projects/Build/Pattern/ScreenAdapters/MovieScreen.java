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

package ru.m210projects.Build.Pattern.ScreenAdapters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.PaletteManager;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;

import java.util.Arrays;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Engine.RESERVEDPALS;

public abstract class MovieScreen extends SkippableAdapter {

    protected final int TILE_MOVIE;
    protected Runnable callback;
    protected int gCutsClock;
    protected long LastMS;
    protected MovieFile mvfil;
    protected int frame;
    protected long mvtime;
    protected byte[] opalookup;
    protected int nFlags = 2 | 8 | 64, nScale = 65536, nPosX = 160, nPosY = 100;

    public MovieScreen(BuildGame game, int nTile) {
        super(game);

        TILE_MOVIE = nTile;
    }

    protected abstract MovieFile GetFile(String file);

    protected abstract void StopAllSounds();

    protected abstract byte[] DoDrawFrame(int num);

    protected abstract Font GetFont();

    protected abstract void DrawEscText(Font font, int pal);

    @Override
    public void show() {
        if (game.pMenu.gShowMenu) {
            game.pMenu.mClose();
        }

        StopAllSounds();
        game.getProcessor().resetPollingStates();
        LastMS = engine.getCurrentTimeMillis();
        game.pNet.ResetTimers();
        gCutsClock = 0;

        PaletteManager paletteManager = engine.getPaletteManager();
        byte[] palookup = paletteManager.makePalookup(0, null, 0, 0, 0, 0);
        opalookup = new byte[palookup.length];
        System.arraycopy(palookup, 0, opalookup, 0, opalookup.length);

        byte[] remapbuf = new byte[256];
        for (int i = 0; i < remapbuf.length; i++) {
            remapbuf[i] = (byte) i;
        }
        paletteManager.makePalookup(0, remapbuf, 0, 0, 0, 2);
        changepalette(mvfil.getPalette());

        mvfil.playAudio();
    }

    public MovieScreen setCallback(Runnable callback) {
        this.callback = callback;
        this.setSkipping(callback);
        return this;
    }

    public boolean init(String fn) {
        if (isInited()) {
            return false;
        }
        return open(fn);
    }

    protected boolean open(String fn) {
        if (mvfil != null) {
            return false;
        }

        mvfil = GetFile(fn);
        if (mvfil == null) {
            return false;
        }

        Renderer renderer = game.getRenderer();
        ArtEntry pic = renderer.getTile(TILE_MOVIE);
        if (!(pic instanceof DynamicArtEntry) || !pic.exists() || pic.getWidth() != mvfil.getHeight() || pic.getHeight() != mvfil.getWidth()) {
            pic = engine.allocatepermanenttile(TILE_MOVIE, mvfil.getHeight(), mvfil.getWidth());
            if (!pic.exists()) {
                return false;
            }
        }

        // we should call invalidate in all cases
        ((DynamicArtEntry) pic).clearData();

        int xdim = renderer.getWidth();
        int ydim = renderer.getHeight();
        float kt = pic.getHeight() / (float) pic.getWidth();
        float kv = xdim / (float) ydim;

        float scale;
        if (kv >= kt) {
            scale = (ydim / (float) pic.getWidth());
            scale /= (ydim / (float) 200);
        } else {
            scale = (xdim / (float) pic.getHeight());
            scale /= ((4 * ydim) / (float) (3 * 320));
        }
        nScale = (int) (scale * 65536);

        frame = 0;
        mvtime = 0;
        LastMS = -1;

        // In case switching between several movie screens
        if (game.isCurrentScreen(this)) {
            show();
        }

        return true;
    }

    public boolean isInited() {
        return mvfil != null;
    }

    protected void changepalette(byte[] pal) {
        if (pal == null || pal.length != 768) {
            return;
        }

        PaletteManager paletteManager = engine.getPaletteManager();
        paletteManager.changePalette(pal);

        int white = -1;
        int k = 0;
        for (int i = 0; i < 256; i += 3) {
            int j = (pal[3 * i] & 0xFF) + (pal[3 * i + 1] & 0xFF) + (pal[3 * i + 2] & 0xFF);
            if (j > k) {
                k = j;
                white = i;
            }
        }

        if (white == -1) {
            return;
        }

        int palnum = MAXPALOOKUPS - RESERVEDPALS - 1;
        byte[] remapbuf = new byte[768];
        Arrays.fill(remapbuf, (byte) white);
        paletteManager.makePalookup(palnum, remapbuf, 0, 1, 0, 1);
    }

    protected boolean play() {
        if (game.getProcessor().isKeyJustPressed(Input.Keys.ANY_KEY)) {
            anyKeyPressed();
        }

        Renderer renderer = game.getRenderer();
        if (mvfil != null) {
            if (LastMS == -1) {
                LastMS = engine.getCurrentTimeMillis();
            }
            DynamicArtEntry pic = (DynamicArtEntry) renderer.getTile(TILE_MOVIE);

            long ms = engine.getCurrentTimeMillis();
            long dt = ms - LastMS;
            mvtime += dt;
            float tick = mvfil.getRate();
            if (mvtime >= tick) {
                if (frame < mvfil.getFrames()) {
                    pic.copyData(DoDrawFrame(frame));
                    frame++;
                } else {
                    return false;
                }
                mvtime -= (long) tick;
            }
            LastMS = ms;

            if (!pic.hasSize()) {
                return false;
            }

            renderer.rotatesprite(nPosX << 16, nPosY << 16, nScale, 512, TILE_MOVIE, 0, 0, nFlags);
            return true;
        }
        return false;
    }

    @Override
    public void skip() {
        close();
        super.skip();
    }

    protected void callback() {
        close();
        if (callback != null) {
            Gdx.app.postRunnable(callback);
            callback = null;
        }
    }

    @Override
    public void draw(float delta) {
        if (!play() && skipCallback != null) {
            callback();
        }

        if (engine.getTotalClock() - gCutsClock < 200 && escSkip) // 2 sec
        {
            DrawEscText(GetFont(), MAXPALOOKUPS - RESERVEDPALS - 1);
        }
    }


    public void anyKeyPressed() {
        game.getProcessor().prepareNext();
        gCutsClock = engine.getTotalClock();
    }

    protected void close() {
        if (mvfil != null) {
            PaletteManager paletteManager = engine.getPaletteManager();
            if (opalookup != null) {
                paletteManager.makePalookup(0, opalookup, 0, 0, 0, 2);
            }
            paletteManager.setbrightness(paletteManager.getPaletteGamma(), paletteManager.getBasePalette());
            mvfil.close();
        }

        mvfil = null;
        LastMS = -1;
        frame = 0;
    }

    public interface MovieFile {

        int getFrames();

        float getRate();

        byte[] getFrame(int num);

        byte[] getPalette();

        int getWidth();

        int getHeight();

        void close();

        void playAudio();

    }
}
