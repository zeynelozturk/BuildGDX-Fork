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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import ru.m210projects.Build.Architecture.DialogUtil;
import ru.m210projects.Build.Architecture.MessageType;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildFactory;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.Tools.Interpolation;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Types.MemLog;
import ru.m210projects.Build.exceptions.InitializationException;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.osd.CommandResponse;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.osd.commands.OsdCommand;
import ru.m210projects.Build.osd.commands.OsdValueRange;
import ru.m210projects.Build.settings.GameConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import static ru.m210projects.Build.net.Mmulti.uninitmultiplayer;
import static ru.m210projects.Build.filehandle.CacheResourceMap.CachePriority.NORMAL;

public class InitScreen extends ScreenAdapter {

    private final BuildFactory factory;
    private final BuildGame game;
    private int frames;
    private Engine engine;
    private Thread thread;
    private boolean gameInitialized;
    private boolean disposing;

    public InitScreen(final BuildGame game) {
        this.game = game;

        GameConfig cfg = game.pCfg;
        factory = game.getFactory();

        Directory gameDirectory = game.cache.getGameDirectory();

        Console.out.println("BUILD engine by Ken Silverman (http://www.advsys.net/ken) \r\n" + game.getTitle() + "(BuildGdx v" + Engine.version + ") by [M210�] (http://m210.duke4.net)");

        Console.out.println("Current date " + game.date.getLaunchDate());

        String osver = System.getProperty("os.version");
        String jrever = System.getProperty("java.version");

        Console.out.println("Running on " + game.OS + " (version " + osver + ")");
        Console.out.println("\t with JRE version: " + jrever + "\r\n");

        Console.out.println("Initializing resource archives");

        try {
            for (String res : factory.resources) {
                Entry entry = gameDirectory.getEntry(res);
                if (entry.exists() && !entry.isDirectory()) {
                    game.cache.addGroup(entry, NORMAL);
                }
            }

            Console.out.println("Initializing Build 3D engine");
            this.engine = game.pEngine = factory.engine();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showMessage("Build Engine Initialization Error!",
            String.format("There was a problem initialising the Build engine:\r\n%s\r\nat %s", e, e.getStackTrace()[0]),
            MessageType.Info);

            forceExit("Build Engine Initialization Error! " + String.format("There was a problem initialising the Build engine:\r\n%s\r\nat %s", e, e.getStackTrace()[0]));
            return;
        }

        if (engine.loadpics() == 0) {
            DialogUtil.showMessage("Build Engine Initialization Error!",
                    "ART files not found " + gameDirectory.getPath().resolve(engine.getTileManager().getTilesPath()),
                    MessageType.Info);

            forceExit("ART files not found " + gameDirectory.getPath().resolve(engine.getTileManager().getTilesPath()));
            return;
        }

        game.pFonts = factory.fonts();
        game.initRenderer();
        game.pInt = new Interpolation();
        game.pSavemgr = new SaveManager();

        if (cfg.isAutoloadFolder()) {
            Entry entry = gameDirectory.getEntry("autoload");
            if (!entry.exists()) {
                File f = new File(gameDirectory.getPath().resolve("autoload").toString());
                if (!f.exists() && !f.mkdirs() && !f.isDirectory()) {
                    Console.out.println("Can't create autoload folder", OsdColor.RED);
                }
            }
        }

        thread = new Thread(() -> {
            try {
                game.pNet = factory.net();
                game.pSlider = factory.slider();
                game.pMenu = factory.menus();
                game.baseDef = factory.getBaseDef(engine);

                uninitmultiplayer();
                gameInitialized = game.init();

                ConsoleInit();
            } catch (InitializationException ie) {
                ie.printStackTrace();

                DialogUtil.showMessage("Initialization exception!", ie.getMessage(), MessageType.Info);

                forceExit("Initialization exception! " + ie.getMessage() + " " + Arrays.toString(ie.getStackTrace()));
            } catch (OutOfMemoryError me) {
                me.printStackTrace();

                String message = "Memory used: [ " + MemLog.used() + " / " + MemLog.total()
                        + " mb ] \r\nPlease, increase the java's heap size.";
                DialogUtil.showMessage("OutOfMemory!", message, MessageType.Info);

                forceExit(message);
            } catch (FileNotFoundException fe) {
                fe.printStackTrace();
                String message = fe.toString();
                DialogUtil.showMessage("File not found!", message, MessageType.Info);

                forceExit("File not found! " + message);
            } catch (Throwable e) {
                if (!disposing) {
                    game.ThrowError("InitScreen error " + "[" + e.getClass().getSimpleName() + "]: ", e.getStackTrace());
                }
            }
        });
        thread.setName("InitEngine thread");
        thread.setDaemon(true); // to make the thread as background process and kill it if the app was closed
    }

    @Override
    public void show() {
        frames = 0;
        Console.out.setFullscreen(true);
        gameInitialized = false;
        disposing = false;
    }

    @Override
    public void hide() {
        Console.out.setFullscreen(false);
    }

    private void ConsoleInit() {
        Console.out.registerCommand(new OsdValueRange("r_texturemode", "", 0, 2) {

            @Override
            public String getDescription() {
                return "Current texturing mode is " + game.pCfg.getGlfilter().name();
            }

            @Override
            protected void setCheckedValue(float value) {
                game.pCfg.setGlfilter(TexFilter.valueOf((int) value));
            }
        });

        Console.out.registerCommand(new OsdValueRange("r_detailmapping", "r_detailmapping: use detail textures", 0, 1) {
            @Override
            public float getValue() {
                return game.pCfg.isDetailMapping() ? 1 : 0;
            }

            @Override
            protected void setCheckedValue(float value) {
                game.pCfg.setDetailMapping(value != 0);
            }
        });

        Console.out.registerCommand(new OsdValueRange("r_glowmapping", "r_detailmapping: use detail textures", 0, 1) {
            @Override
            public float getValue() {
                return game.pCfg.isGlowMapping() ? 1 : 0;
            }

            @Override
            protected void setCheckedValue(float value) {
                game.pCfg.setGlowMapping(value != 0);
            }
        });

        Console.out.registerCommand(new OsdCommand("memusage", "mem usage / total") {
            @Override
            public CommandResponse execute(String[] argv) {
                Console.out.println("Memory used: " + MemLog.used() + " / " + MemLog.total() + " mb");
                return CommandResponse.SILENT_RESPONSE;
            }
        });

        Console.out.registerCommand(new OsdCommand("net_bufferjitter", "net_bufferjitter") {
            @Override
            public CommandResponse execute(String[] argv) {
                Console.out.println("bufferjitter: " + game.pNet.bufferJitter);
                return CommandResponse.SILENT_RESPONSE;
            }
        });

        Console.out.registerCommand(new OsdCommand("deb_filelist", "deb_filelist") {
            @Override
            public CommandResponse execute(String[] argv) {
                for (Group g : game.cache.getGroups()) {
                    Console.out.println(String.format("group: \"%s\" priority: %s", g.getName(), game.cache.getPriority(g)), OsdColor.BLUE);
                    for (Entry res : g.getEntries()) {
                        String descr;
                        if (res.isDirectory()) {
                            descr = "directory";
                        } else {
                            descr = "file";
                        }
                        Console.out.println(String.format("\t    %s: \"%s\"", descr, res));
                    }
                }
                return CommandResponse.SILENT_RESPONSE;
            }
        });

        Console.out.registerCommand(new OsdCommand("quit", "") {
            @Override
            public CommandResponse execute(String[] argv) {
                game.gExit = true;
                return CommandResponse.SILENT_RESPONSE;
            }
        });
    }

    public void start() {
        if (thread != null) {
            thread.start();
        }
    }

    @Override
    public synchronized void dispose() {
        disposing = true;
    }

    @Override
    public synchronized void render(float delta) {
        Renderer renderer = game.getRenderer();
        if (!disposing && renderer.isInited()) { // don't draw anything after disposed
            renderer.clearview(0);
            factory.drawInitScreen();

            if (frames++ > 3) {
                if (!thread.isAlive()) {
                    if (gameInitialized) {
                        game.show();
                    } else {
                        DialogUtil.showMessage("Initialization Error", "InitScreen unknown error!", MessageType.Error);
                        Gdx.app.exit();
                    }
                }
            }
            Console.out.draw();
        }
    }

    private void forceExit(String consoleMessage) {
        Console.out.println(consoleMessage, OsdColor.RED);
        Console.out.getLogger().close();
        System.exit(1);
    }
}
