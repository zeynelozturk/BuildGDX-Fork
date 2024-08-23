//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.net.Mmulti.*;
import static ru.m210projects.Build.Pattern.BuildNet.*;

import com.badlogic.gdx.ScreenAdapter;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.settings.GameKeys;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildNet;
import ru.m210projects.Build.Pattern.BuildGame.NetMode;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;

import java.io.ByteArrayOutputStream;

public abstract class GameAdapter extends ScreenAdapter implements InputListener {

	protected BuildGame game;
	protected BuildNet pNet;
	protected MenuHandler pMenu;
	protected Engine pEngine;
	protected GameConfig pCfg;
	protected Runnable gScreenCapture;
	protected LoadingAdapter load;
	public byte[] captBuffer;

//	protected static Thread gameThread;

	public GameAdapter(final BuildGame game, LoadingAdapter load) {
		this.game = game;
		this.pNet = game.pNet;
		this.pMenu = game.pMenu;
		this.pEngine = game.pEngine;
		this.pCfg = game.pCfg;
		this.load = load;

//		if(gameThread == null) {
//		gameThread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				while(!game.gExit) {
//					if(!pNet.ready2send) continue;
//
//					int i;
//					while (pNet.gNetFifoHead[myconnectindex] - pNet.gNetFifoTail > pNet.bufferJitter && !game.gExit) {
//						for (i = connecthead; i >= 0; i = connectpoint2[i])
//							if (pNet.gNetFifoTail == pNet.gNetFifoHead[i]) break;
//						if (i >= 0) break;
//
//						synchronized(GameAdapter.this) {
//							pEngine.faketimerhandler(); //game timer sync
//							ProcessFrame(pNet);
//						}
//					}
//				}
//			}
//		});
//		gameThread.setName("BuildGDX Game Thread");
//		gameThread.start();
//		}
	}

	public void PreFrame(BuildNet net) {
		/* nothing */ }

	public void PostFrame(BuildNet net) {
		/* nothing */
	}

	public abstract void ProcessFrame(BuildNet net);

	/**
	 * Don't use DrawWorld() for save game!
	 */
	public abstract void DrawWorld(float smooth);

	public abstract void DrawHud(float smooth);

	public abstract void sndHandlePause(boolean pause);

	protected abstract boolean prepareboard(Entry entry);

	public GameAdapter setTitle(String title) {
		load.setTitle(title);
		return this;
	}

	public GameAdapter loadboard(final Entry mapEntry, final Runnable prestart) {
		pNet.ready2send = false;
		game.changeScreen(load);
		load.init(() -> {
            if (prepareboard(mapEntry)) {
                if (prestart != null) {
                    prestart.run();
                }

                if (game.currentDef.mapInfo.load(mapEntry)) {
                    System.err.println("Maphack loaded for map: " + mapEntry.getName());
                }

                startboard(startboard);
            } // do nothing, it's better to handle it in each game manualy;
        });

		return this;
	}

	private final Runnable startboard = new Runnable() {
		@Override
		public void run() {
			pNet.WaitForAllPlayers(0);
			pNet.ResetTimers();
			pNet.ready2send = true;
			game.changeScreen(GameAdapter.this);

//			pEngine.faketimerhandler();
		}
	};

	protected void startboard(Runnable startboard) {
		game.doPrecache(startboard);
	}

	@Override
	public void show() {
		game.getProcessor().resetPollingStates();
		pMenu.mClose();
		pNet.ready2send = true;
	}

	@Override
	public void hide() {
		pNet.ready2send = false;
	}

	@Override
	public synchronized void render(float delta) {
		if (numplayers > 1) {
			// pEngine.faketimerhandler();

			pNet.GetPackets();
			while (pNet.gPredictTail < pNet.gNetFifoHead[myconnectindex] && !game.gPaused) {
				pNet.UpdatePrediction(pNet.gFifoInput[pNet.gPredictTail & kFifoMask][myconnectindex]);
			}
		} else {
			pNet.bufferJitter = 0;
		}

		PreFrame(pNet);

		int i;
		while (pNet.gNetFifoHead[myconnectindex] - pNet.gNetFifoTail > pNet.bufferJitter && !game.gExit) {
			for (i = connecthead; i >= 0; i = connectpoint2[i]) {
				if (pNet.gNetFifoTail == pNet.gNetFifoHead[i]) {
					break;
				}
			}
			if (i >= 0) {
				break;
			}

			pEngine.faketimerhandler(); // game timer sync
			game.pInt.clearinterpolations();
			ProcessFrame(pNet);
		}

		pNet.CheckSync();

		float smoothratio = 65536;
		if (!game.gPaused && (game.nNetMode != NetMode.Single || !pMenu.gShowMenu && !Console.out.isShowing())) {
			smoothratio = pEngine.getTimer().getsmoothratio(delta);
			if (smoothratio < 0 || smoothratio > 0x10000) {
//					Console.out.println("Interpolation error " + smoothratio);
				smoothratio = BClipRange(smoothratio, 0, 0x10000);
			}
		}

		game.pInt.dointerpolations(smoothratio);
		DrawWorld(smoothratio); // smooth sprites

		if (gScreenCapture != null) {
			gScreenCapture.run();
			gScreenCapture = null;
		}

		DrawHud(smoothratio);
		game.pInt.restoreinterpolations();

		if (pMenu.gShowMenu) {
			pMenu.mDrawMenu();
		}

		PostFrame(pNet);

		pEngine.nextpage(delta);
	}

	public void capture(final int width, final int height) {
		Renderer renderer = game.getRenderer();
		gScreenCapture = () -> {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			renderer.screencapture(os, width, height, TileData.PixelFormat.Pal8);
			captBuffer = os.toByteArray();
		};
	}

	public boolean isScreenSaving() {
		return gScreenCapture != null;
	}

	@Override
	public void pause() {
		if (game.nNetMode == NetMode.Single && numplayers < 2) {
			game.gPaused = true;
			sndHandlePause(true);
		}
	}

	@Override
	public void resume() {
		if (game.nNetMode == NetMode.Single && numplayers < 2) {
			game.gPaused = false;
			pNet.ototalclock = game.pEngine.getTotalClock();
			sndHandlePause(game.gPaused);
		}
	}

	@Override
	public boolean gameKeyDown(GameKey gameKey) {
		if (GameKeys.Show_Console.equals(gameKey)) {
			Console.out.onToggle();
			return true;
		}
		return false;
	}

	@Override
	public InputListener getInputListener() {
		if (Console.out.isShowing()) {
			return Console.out;
		}

		if (game.pMenu.isShowing()) {
			return game.pMenu;
		}

		return this;
	}
}
