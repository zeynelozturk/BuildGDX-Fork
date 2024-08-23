//This file is part of BuildGDX.
//Copyright (C) 2024  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Pattern.ScreenAdapters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.listeners.PrecacheListener;
import ru.m210projects.Build.Types.AnimType;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.Engine.pow2char;

public class DefaultPrecacheScreen extends ScreenAdapter implements InputListener {
    // implement GameKeyListener to override key inputs

    protected final Runnable readyCallback;
    protected final BuildGame game;
    protected final PrecacheListener listener;
    private final byte[] tiles;
    private final List<PrecacheQueue> queues = new ArrayList<>();
    private int currentIndex = 0;

    public DefaultPrecacheScreen(BuildGame game, Runnable readyCallback, PrecacheListener listener) {
        this.game = game;
        this.listener = listener;
        this.readyCallback = readyCallback;
        this.tiles = new byte[MAXTILES >> 3];
        queues.add(new PrecacheQueue("Preload world...", () -> game.pEngine.getBoardService().notifyBoardChanged()));
        queues.add(new PrecacheQueue("Preload models...", listener::loadModels));
        queues.add(new PrecacheQueue("Preload fonts...", () -> {
            for (int i = 0; i < game.pFonts.getSize(); i++) {
                Font font = game.getFont(i);
                for (int c = 0; c < 255; c++) {
                    int tile = font.getCharInfo((char) c).tile;
                    if (tile != -1) {
                        addTile(tile);
                    }
                }
            }
        }));
    }

    public void addQueue(String name, Runnable runnable) {
        queues.add(new PrecacheQueue(name, runnable));
    }

    @Override
    public void show() {
        currentIndex = 0;
        game.getProcessor().resetPollingStates();
        Arrays.fill(tiles, (byte) 0);
    }

    public void addTile(int tile) {
        Renderer renderer = game.getRenderer();
        ArtEntry pic = renderer.getTile(tile);
        if (pic.getType() != AnimType.NONE) {
            int frames = pic.getAnimFrames();
            for (int i = frames; i >= 0; i--) {
                if (pic.getType() == AnimType.BACKWARD) {
                    tiles[(tile - i) >> 3] |= (byte) (1 << ((tile - i) & 7));
                } else {
                    tiles[(tile + i) >> 3] |= (byte) (1 << ((tile + i) & 7));
                }
            }
        } else {
            tiles[tile >> 3] |= (byte) pow2char[tile & 7];
        }
    }

    protected void draw(String title, int index) {
        // Should be overwritten
    }

    @Override
    public void render(float delta) {
        game.getRenderer().clearview(0);
        if (currentIndex >= queues.size()) {
            draw("Getting ready...", -1);
            listener.onPrecacheReady();
            Console.out.println("Done", OsdColor.BLUE);
            if (readyCallback != null) {
                Gdx.app.postRunnable(readyCallback);
            }
        } else {
            PrecacheQueue current = queues.get(currentIndex);
            draw(current.name, currentIndex);
            Gdx.app.postRunnable(current.runnable);
            currentIndex++;
        }

        game.pEngine.nextpage(delta);
    }

    protected void doprecache(int method) {
        for (int i = 0; i < MAXTILES; i++) {
            if (tiles[i >> 3] == 0) {
                i += 7;
                continue;
            }

            if ((tiles[i >> 3] & pow2char[i & 7]) != 0) {
                listener.onPrecacheTile(i, method == 1);
            }
        }
        Arrays.fill(tiles, (byte) 0);
    }

    private static class PrecacheQueue {
        private final String name;
        private final Runnable runnable;

        public PrecacheQueue(String name, Runnable runnable) {
            this.name = name;
            this.runnable = runnable;
        }
    }

    @Override
    public InputListener getInputListener() {
        // lock all inputs
        return null;
    }
}
