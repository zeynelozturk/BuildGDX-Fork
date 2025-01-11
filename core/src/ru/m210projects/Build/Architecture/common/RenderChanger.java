// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.settings.ApplicationContext;
import ru.m210projects.Build.settings.GameConfig;

import static ru.m210projects.Build.Render.DummyRenderer.DUMMY_RENDERER;

public abstract class RenderChanger implements ApplicationListener, ApplicationContext.ApplicationChangeListener {

    /**
     * BuildGdx struct
     *
     * new GameConfig
     *     Config.setGamePath()
     *     Config.registerAudioDriver()
     *     Config.addMidiDevices()
     *     Config.load() (read settings from file)
     * new BuildGame(Config)
     * Config.addSoundBank(new SoundBankDevice(Config.SoundPath)) // TODO: to AppCon?
     * new RenderChanger(BuildGame)
     * RenderChanger.startApplication(RenderType)
     * 	    new App
     * 		App -> RenderChanger.setApplicationContext(AppCon)
     * 			AppCon.setApplicationChangeListener
     * 			AppCon.setControllerListener
     * 			Config.setApplicationContext(AppCon)
     * 		App -> new Renderer(RenderType)
     * 			Config.setVideoContext()
     *
     * Game.create() -> new InitScreen -> new Engine
     * Game.initRenderer()
     * Config.setAudioDriver() (by init sound -> new OpenAL)
     *      Config.setAudioContext()
     *
     * RenderChanger.onChangeApplication()
     *      RenderChanger.startApplication() -> LifecycleListeners.dispose
     * 		    Game.initRenderer()
     *
     * Game.initRenderer()
     * 		Gdx.input.setInputProcessor()
     * 		pCfg.setRawInput(pCfg.isRawInput());
     * 		renderer.init(pEngine);
     * 		Console.out.setFunc(getFactory().getOsdFunc());
     * 		resize(pCfg.getScreenWidth(), pCfg.getScreenHeight());
     */

    protected BuildGame game;
    protected GameConfig config;
    protected boolean renderChanging = false;
    protected boolean created = false;

    public RenderChanger(BuildGame listener) {
        this.game = listener;
        this.config = game.pCfg;
    }

    /**
     * Calls when new Application created
     * @param applicationContext Config module with application dependent settings
     *                           like change midi device, change resolution or mouse raw input
     * <br>
     * Sets {@link ru.m210projects.Build.settings.ApplicationContext.ApplicationChangeListener} to listen
     *                           when renderer is changing
     * and {@link ru.m210projects.Build.input.GameProcessor} to listen gamePad events.
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        applicationContext.setApplicationChangeListener(this);
        applicationContext.setControllerListener(game.getProcessor());
        if (!config.getControllerName().isEmpty()) {
            config.getControllers(); // to load controllerList (because of extra threads, that don't need for people, who don't use joysticks)
        }
        this.config.setApplicationContext(applicationContext);
    }

    /**
     * @param type renderer type. Created Application depends on {@link ru.m210projects.Build.Render.Renderer.RenderType}
     */
    protected abstract void startApplication(Renderer.RenderType type);

    @Override
    public void create() {
        if (renderChanging) {
            game.resume();
            game.initRenderer();
            renderChanging = false;
            return;
        }

        game.create();
        created = true;
    }

    @Override
    public void resize(int width, int height) {
        if (width == 0 && height == 0) {
            return; // fuck you!
        }

        // I disabled setScreenMode in resize func (it seems better?)
        game.resize(width, height);
    }

    @Override
    public void render() {
        Renderer renderer = game.getRenderer();
        game.render();
        renderer.nextpage();
    }

    @Override
    public void pause() {
        game.pause();
    }

    @Override
    public void resume() {
        game.resume();
    }

    @Override
    public void dispose() {
        if (renderChanging) {
            return;
        }

        game.dispose();
    }

    public BuildGame getGame() {
        return game;
    }

    @Override
    public void onChangeApplication(Renderer.RenderType type) {
        if (created) {
            renderChanging = true;
            // Change renderer to Dummy renderer for safe changing
            game.setRenderer(DUMMY_RENDERER);
            Gdx.app.exit();
        }

        final Thread oldThread = Thread.currentThread();
        new Thread(() -> {
            try {
                oldThread.join();
            } catch (InterruptedException ignore) {
            }
            startApplication(type);
        }).start();
    }

    public boolean isRendererChanging() {
        return renderChanging;
    }
}
