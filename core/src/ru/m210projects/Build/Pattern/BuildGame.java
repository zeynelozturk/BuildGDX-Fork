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

package ru.m210projects.Build.Pattern;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Architecture.DialogUtil;
import ru.m210projects.Build.Architecture.MessageType;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.CommonMenus.MenuVideoMode;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;
import ru.m210projects.Build.Pattern.MenuItems.SliderDrawable;
import ru.m210projects.Build.Pattern.ScreenAdapters.DefaultPrecacheScreen;
import ru.m210projects.Build.Pattern.ScreenAdapters.GameAdapter;
import ru.m210projects.Build.Pattern.ScreenAdapters.InitScreen;
import ru.m210projects.Build.Pattern.ScreenAdapters.MessageScreen;
import ru.m210projects.Build.Pattern.Tools.Interpolation;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Render.DummyRenderer;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Renderer.RenderType;
import ru.m210projects.Build.exceptions.RendererException;
import ru.m210projects.Build.Render.listeners.PrecacheListener;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.exceptions.AssertException;
import ru.m210projects.Build.exceptions.WarningException;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.filehandle.Cache;
import ru.m210projects.Build.filehandle.FileUtils;
import ru.m210projects.Build.filehandle.fs.AbsoluteFileEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.input.GameProcessor;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.ConsoleLogger;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.settings.GameConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.m210projects.Build.net.Mmulti.*;
import static ru.m210projects.Build.Pattern.ScreenAdapters.DummyScreen.DUMMY_SCREEN;
import static ru.m210projects.Build.Render.DummyRenderer.DUMMY_RENDERER;
import static ru.m210projects.Build.Strhandler.toLowerCase;

public abstract class BuildGame extends Game implements Thread.UncaughtExceptionHandler {

    public final String appName;
    public final String appVersion;
    public final boolean release;
    public final String OS = System.getProperty("os.name");
    public final Date date;

    public Engine pEngine;
    public GameConfig pCfg;
    public MenuHandler pMenu;
    public FontHandler pFonts;
    public BuildNet pNet;
    public Interpolation pInt;
    public SaveManager pSavemgr;
    public SliderDrawable pSlider;
    public boolean gExit = false;
    public boolean gPaused = false;
    public DefScript baseDef;
    public DefScript currentDef;
    public NetMode nNetMode;
    public Cache cache;
    protected GameProcessor controller;
    protected boolean active;
    @NotNull
    protected Renderer renderer;
    Screen gCurrScreen;
    Screen gPrevScreen;
    private Directory userDirectory;
    protected Map<String, String> args = new HashMap<>();

    public BuildGame(GameConfig cfg, String appName, String appVersion, boolean release) throws IOException {
        this(new ArrayList<>(), cfg, appName, appVersion, release);
    }

    public BuildGame(List<String> args, GameConfig cfg, String appName, String appVersion, boolean release) throws IOException {
        this.appName = appName; // the port name without version part (to save logs, configs, etc.)
        this.appVersion = appVersion;
        this.release = release;
        this.pCfg = cfg;
        this.date = new Date("MMM dd, yyyy HH:mm:ss");
        this.controller = createGameProcessor();
        this.renderer = DUMMY_RENDERER;

        initCache(new Directory.Game(cfg.getGamePath()), new Directory(cfg.getCfgPath().getParent(), true));
        try {
            Path path = userDirectory.getPath().resolve(toLowerCase(appName + ".log"));
            Console.out.setLogger(new ConsoleLogger(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < args.size() - 1; i += 2) {
            this.args.put(args.get(i), args.get(i + 1));
        }
    }

    public String getTitle() {
        return appName + " " + appVersion;
    }

    @Override
    public final void create() {
        InitScreen scr = new InitScreen(this);
        super.setScreen(scr);
        scr.start();
    }

    protected abstract MessageScreen createMessage(String header, String text, MessageType type);

    public MessageScreen showGameMessage(String header, String text, MessageType type) {
        if (numplayers > 1) {
            pNet.NetDisconnect(myconnectindex);
        }

        MessageScreen messageScreen = createMessage(header, text, type);
        changeScreen(messageScreen);
        return messageScreen;
    }

    @NotNull
    public Renderer getRenderer() {
        if (renderer instanceof DummyRenderer) {
            throw new RuntimeException("Dummy renderer!");
        }
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        if (this.renderer.isInited()) {
            this.renderer.uninit();
        }
        this.renderer = renderer;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public GameProcessor getProcessor() {
        return controller;
    }

    public abstract GameProcessor createGameProcessor();

    public Cache createFileCache(Directory gameDirectory) {
        return new Cache(gameDirectory);
    }

    public void initCache(Directory gameDirectory, Directory userDirectory) {
        this.userDirectory = userDirectory;
        cache = createFileCache(gameDirectory);
    }

    public Directory getUserDirectory() {
        return userDirectory;
    }

    public Cache getCache() {
        return cache;
    }

    public abstract BuildFactory getFactory();

    public abstract boolean init() throws Exception;

    public abstract void show();

    /**
     * @param readyCallback will call run() when precaching is done
     * @return Precache screen
     */
    public abstract DefaultPrecacheScreen getPrecacheScreen(Runnable readyCallback, PrecacheListener listener);

    public abstract LogSender getLogSender();

    @Override
    public void dispose() {
        pCfg.getMidiDevice().close();
        pCfg.getAudio().dispose();

        try (OutputStream os = new FileOutputStream(pCfg.getCfgPath().toFile())) {
            pCfg.save(os);
        } catch (IOException e) {
            Console.out.println(e.toString());
        }
        Console.out.getLogger().close();

        if (getScreen() instanceof InitScreen) {
            getScreen().dispose();
        }
        if (numplayers > 1) {
            pNet.NetDisconnect(myconnectindex);
        }

        // Render should be uninited in GL thread
        Gdx.app.postRunnable(() -> {
            try {
                // If game crashed, it's safier to change renderer to DUMMY instead of just uninit,
                // because the renderer can try to draw some frames after uninit, and crashed again
                setRenderer(DUMMY_RENDERER);
            } catch (Exception ignored) {
            }
        });

        if (pEngine != null) {
            pEngine.uninit();
        }
        System.out.println("disposed");
    }

    public Font getFont(int i) {
        return pFonts.getFont(i);
    }


    // called by faketimehandler (30 times per sec)
    public void syncInput(BuildNet net) {
        if (numplayers > 1) {
            net.GetPackets();
        }

        for (int i = connecthead; i >= 0; i = connectpoint2[i]) {
            if (i != myconnectindex && net.gNetFifoHead[myconnectindex] - 200 > net.gNetFifoHead[i]) {
                return;
            }
        }

        controller.processInput(pNet.gInput);
        if ((net.gNetFifoHead[myconnectindex] & (net.MovesPerPacket - 1)) != 0) {
            net.gFifoInput[net.gNetFifoHead[myconnectindex] & 0xFF][myconnectindex]
                    .Copy(net.gFifoInput[(net.gNetFifoHead[myconnectindex] - 1) & 0xFF][myconnectindex]);
            net.gNetFifoHead[myconnectindex]++;
            return;
        }

        net.gFifoInput[net.gNetFifoHead[myconnectindex] & 0xFF][myconnectindex].Copy(net.gInput);
        net.gNetFifoHead[myconnectindex]++;

        if (nNetMode == BuildGame.NetMode.Multiplayer && numplayers < 2) {
            for (int i = connecthead; i >= 0; i = connectpoint2[i]) {
                if (i != myconnectindex) {
                    net.ComputerInput(i);
                    net.gNetFifoHead[i]++;
                }
            }
            return;
        }

        net.GetNetworkInput();
    }

    @Override
    public void render() {
        try {
            if (!gExit) {
                super.render();
            } else {
                Gdx.app.exit();
            }
        } catch (AssertException | WarningException assertEx) {
            softExceptionScreen(assertEx);
        } catch (Throwable e) {
            if (!release) {
                e.printStackTrace();
                dispose();
                System.exit(1);
            } else {
                ThrowError(e.getClass().getSimpleName() + "[BuildGame]: " + (e.getMessage() == null ? "" : e.getMessage()), e.getStackTrace());
            }
        }
    }

    public void changeScreen(Screen screen) {
        gPrevScreen = gCurrScreen;
        gCurrScreen = screen;
        // ability to avoid visualization of palette changing (in movies, for example, that has custom palettes)
        super.setScreen(DUMMY_SCREEN);
        Gdx.app.postRunnable(() -> super.setScreen(screen));
    }

    @Override
    public void setScreen(Screen screen) {
        throw new UnsupportedOperationException();
    }

    public boolean isCurrentScreen(Screen screen) {
        return gCurrScreen == screen;
    }

    public void setPrevScreen() {
        gCurrScreen = gPrevScreen;
        changeScreen(gPrevScreen);
    }

    public String getScrName() {
        if (gCurrScreen != null) {
            return gCurrScreen.getClass().getSimpleName();
        }

        if (Gdx.app != null) {
            return "Create frame";
        }

        return "Init frame";
    }

    public boolean setDefs(DefScript script) {
        if (currentDef != script) {
            if (currentDef != null) {
                currentDef.dispose();
            }
            currentDef = script;
            currentDef.apply();
            pEngine.setDefs(script);
            renderer.setDefs(currentDef);
            return true;
        }

        return false;
    }

    public String getStackTraceAsString(StackTraceElement[] getStackTrace, int depth) {
        StringBuilder sb = new StringBuilder();
        int depthCount = 0;
        for (StackTraceElement element : getStackTrace) {
            if (depth != -1 && depthCount >= depth) {
                break;
            }
            sb.append("\t").append(element.toString());
            sb.append("\r\n");
            depthCount++;
        }

        return sb.toString();
    }

    public void ThrowError(String msg) {
        ThrowError(msg, Thread.currentThread().getStackTrace());
    }

    /**
     * Shows error dialog and disposes the game application
     */
    public void ThrowError(String msg, StackTraceElement[] stackTrace) {
        String stackTraceMessage = msg;
        stackTraceMessage += "\r\nFull stack trace: ";
        stackTraceMessage += getStackTraceAsString(stackTrace, -1);

        Console.out.println("FatalError: " + stackTraceMessage, OsdColor.RED);
        Console.out.getLogger().close();
        if (nNetMode == NetMode.Multiplayer) {
            pNet.NetDisconnect(myconnectindex);
        }

        DialogUtil.DialogResult result = DialogUtil.showMessage("Error", msg, MessageType.Crash);
        if (result.isApproved()) {
            getLogSender().send(result.getComment());
        }

        this.dispose();
        System.exit(0);
    }

    /**
     * Shows message screen without dispose the game application
     */
    public void softExceptionScreen(Exception exception) {
        String title = exception.getMessage();
        if (title == null || title.isEmpty()) {
            title += exception.toString();
        }
        String stackTraceMessage = title;
        stackTraceMessage += "\r\nFull stack trace: ";
        stackTraceMessage += getStackTraceAsString(exception.getStackTrace(), -1);

        Console.out.println("Exception: " + stackTraceMessage, OsdColor.RED);
        String finalTitle = title;
        Gdx.app.postRunnable(() -> showGameMessage("Error!", finalTitle, MessageType.Crash).setMessageListener(approved -> {
            if (approved) {
                getLogSender().send("");
            }
            show();
        }));
    }

    public void GameMessage(String msg) {
        Console.out.println("Message: " + msg, OsdColor.YELLOW);
        Gdx.app.postRunnable(() -> showGameMessage("Message: ", msg, MessageType.Info).setMessageListener(approved -> {
            if (approved) {
                getLogSender().send("");
            }
            show();
        }));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof GdxRuntimeException) {
            if (e.getCause() != null) {
                e = e.getCause();
            }
        }

        String message = e.getMessage();
        String title = "UncaughtException[" + e.getClass().getSimpleName() + "]";
        if (message != null) {
            int maxSymbols = 128; // Set max symbols per line to avoid long messages
            StringBuilder wrappedMessage = new StringBuilder();
            int curLineWidth = 0;
            for (char c : message.toCharArray()) {
                if (curLineWidth >= maxSymbols) {
                    wrappedMessage.append("\r\n");
                    curLineWidth = 0;
                }
                wrappedMessage.append(c);
                curLineWidth++;
            }
            title += ":\r\n" + wrappedMessage;
        }

        ThrowError(title, e.getStackTrace());
    }

    public void initRenderer() {
        // Gdx.input will change with renderer (application)
        GameProcessor processor = getProcessor();
        processor.resetPollingStates();
        Gdx.input.setInputProcessor(processor);
        pCfg.setRawInput(pCfg.isRawInput());

        renderer.init(pEngine);
        pEngine.setbrightness(pEngine.getPaletteManager().getPaletteGamma(), pEngine.getPaletteManager().getBasePalette());
        if (!renderer.isInited()) {
            throw new RendererException("Renderer initialization failed");
        }

        Console.out.setFunc(getFactory().getOsdFunc());
        pCfg.setScreenMode(pCfg.getScreenWidth(), pCfg.getScreenHeight(), pCfg.isFullscreen());
        // renderer resize and revalidate should be before InitScreen will be shown
        renderer.resize(pCfg.getScreenWidth(), pCfg.getScreenHeight());
        Console.out.revalidate();

        if (pMenu != null && pMenu.getCurrentMenu() instanceof MenuVideoMode) {
            ((MenuVideoMode) pMenu.getCurrentMenu()).onRenderChanged(renderer.getType());
        }

        if (renderer instanceof PrecacheListener) {
            final Screen prevScreen = getScreen();
            if (prevScreen instanceof GameAdapter) {
                doPrecache(() -> changeScreen(prevScreen));
            }
        }
    }

    public void doPrecache(Runnable readyCallback) {
        PrecacheListener listener = PrecacheListener.DUMMY_PRECACHE_LISTENER;
        if (renderer instanceof PrecacheListener) {
            listener = (PrecacheListener) renderer;
        }
        changeScreen(getPrecacheScreen(readyCallback, listener));
    }

    public boolean isSoftwareRenderer() {
        return renderer.getType() == RenderType.Software;
        // return renderer instanceof Software;
    }

    public void onFilesDropped(String[] files) {
        final Directory gameDirectory = cache.getGameDirectory();
        for (String spath : files) {
            try {
                Path absolutePath = FileUtils.getPath(spath);
                Path gamePath = gameDirectory.getPath();
                if (absolutePath.startsWith(gamePath)) {
                    FileEntry entry = gameDirectory.getEntry(gamePath.relativize(absolutePath));
                    if (entry.exists()) {
                        onDropEntry(entry);
                        return;
                    }
                } else {
                    FileEntry entry = new AbsoluteFileEntry(absolutePath);
                    if (entry.exists()) {
                        onDropEntry(entry);
                    }
                    return;
                }
            } catch (Exception e) {
                Console.out.println("Can't handle the file " + spath, OsdColor.RED);
            }
        }
    }

    protected void parseArgumentsCommon() {
        if (!args.isEmpty()) {
            // Handle arguments at the first launch. args should be clear after handle
            Console.out.println("Parsing arguments", OsdColor.YELLOW);

            String ip = args.getOrDefault("-ip", "");
            String port = args.getOrDefault("-port", "");
            String name = args.getOrDefault("-name", "");

            if (!name.isEmpty()) {
                Console.out.println("Setting name to " + name, OsdColor.YELLOW);
                pCfg.setpName(name);
            }

            if (!ip.isEmpty()) {
                Console.out.println("Setting ip to " + ip, OsdColor.YELLOW);
                pCfg.setmAddress(ip);
            }

            if (!port.isEmpty()) {
                Console.out.println("Setting port to " + port, OsdColor.YELLOW);
                try {
                    pCfg.setPort(Integer.parseInt(port));
                } catch (Exception i) {
                    Console.out.println("Setting port failed", OsdColor.RED);
                }
            }
        }
    }

    public void onDropEntry(FileEntry entry) {
    }

    @Override
    public void resize(int width, int height) {
        if (width == renderer.getWidth() && height == renderer.getHeight()) {
            return;
        }

        width = Math.max(1, width);
        height = Math.max(1, height);

        renderer.resize(width, height);
        Console.out.revalidate();
        if (pMenu != null && pMenu.getCurrentMenu() instanceof MenuVideoMode) {
            ((MenuVideoMode) pMenu.getCurrentMenu()).onResize(width, height);
        }
    }

    public enum NetMode {Single, Multiplayer}
}
