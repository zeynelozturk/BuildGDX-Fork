// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
// This file has been modified from Ken Silverman's original release
// by Jonathon Fowler (jf@jonof.id.au)
// by the EDuke32 team (development@voidpoint.com)
// by Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildNet;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.collections.BitMap;
import ru.m210projects.Build.exceptions.WarningException;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.osd.Console;

import java.util.concurrent.atomic.AtomicInteger;
import static ru.m210projects.Build.net.Mmulti.*;

public class Engine {

    /*
     * Changes v1.18
     * Bilinear filtration for palette emulation mode
     * Palette emulation soft shading option
     * XInput gamepads support (windows only)
     * Update libgdx to version 1.13.0, jinput to 2.0.10, lwjgl to 3.3.3
     * Rebind "ESC" in keys configurations is fixed for menu_toggle func
     * Mouse sensitive option is textfield now (instead of slider from 0 to 2)
     * Set "useHighTiles" and "useModels" parameters back to true by default when new config is created (affect on skyboxes)
     * Crash fix when delete all save files and trying to save new one (Index -2 out of bounds for length 0 MenuSlotList.onEnter(MenuSlotList.java:493)
     * Crash fix when game has 1 demo with "randomly" playback mode (FatalError: IndexOutOfBoundsException[BuildGame]: Index -1 out of bounds for length 1)
     * Crash fix in Polygdx renderer when load map with broken sectors (ru.m210projects.Build.Render.GdxRender.Tesselator.initZoids(Tesselator.java:191))
     * Midi isOpen() now checks sequencer to fix exception: javax.sound.midi.MidiUnavailableException: MIDI IN receiver not available
     * Software renderer set resolution check (resolutions bigger than 4096x3072 isn't supported)
     *
     * OSX sky rendering is fixed
     * OSX resolution changing is fixed
     * Changed input method algorithm (attempt to fix OSX input)
     * BloodGDX:
     *      Fixed loading extra data in savegame files (after remove support of old dos saves)
     * DukeGDX:
     *      Improved con addon finding (when definevolumename is not defined, but definelevelname is)
     *      DK / RR setMapInfo index protect
     *      DK / RR genspriteremaps() corrupted lookup.dat doesn't close the port now
     *      DK / RR sndPlayMusic with oggmusic crash fix
     * TekwarGDX:
     *      Fixed crash: ru.m210projects.Tekwar.Teksnd.playCDtrack(Teksnd.java:183)
     * WangGDX:
     *      Leave vehicle fix in map2
     * LSPGDX:
     *      Show exits on automap doesn't crash the game when not in a game mode
     *
     * TODO:
     * сделать менеджер сохранений - много общего кода, lsReadLoadData не static lsinfo
     * Сделать AudioEntry, который содержит SoundData и используется для кэширования newSound
     * настройки консоли в меню
     * бесконечные тайлы для рендера. убрать maxtiles
     * RenderingType delete indexes
     * Переделать меню на ООП
     *
     * RRGDX: (+)
     *      mve video support
     *      as I said once, you cannot pickup a weapon if you already have it (in MP?)
     * DukeGDX (OK++):
     *      autoaim won't disable in DukeME with 1.17.
     *      Let´s add Duke!Zone II to the Official Addons List xD
     *      One test I've found is shooting a rocket at a wall to kill Duke. The red background will gradually fade from the beginning with the software renderer, while it'll stay for just a bit longer before fading with Polymost
     *      defscript command to play anmfiles before an episode starts
     *      Check WT music/sound. Why music loaded from WT music folder if there is music addon in autoload folder
     *      caribian e1m1 camera bug
     * BloodGDX: (OK)
     *      ogv video support
     *      record demo
     *      Сделать классический полный HUD, который скрывает часть экрана
     *      Фанатик кричик двумя голосами при сгорании
     *      При быстром переключении на несуществующее оружие, другое не выбирается
     *      Fanatics have blue pixels on skin when using my Cultists addon.
     *  WangGDX: (OK+)
     *      Unusually slow gameplay (slow motion) on ARM64 devices (Raspberry Pi 5), even when FPS are stable at 60 FPS
     * PSGDX: (OK+)
     *      не включилась музыка после 2й катсцены на выборе карты (карта 5)
     *      Если включить прошлый уровень и пройти его снова, нельзя вернуться к последним
     *      other autoaim method
     *      The enemies that throw blue fireballs don't attack when I'm underwater
     *  LSPGDX: (OK+)
     *
     *  TekwarGDX: (OK+)
     *      PolyGDX rearmirror doesn't work
     *      software background
     *  WHGDX: (OK+)
     *      Проверить урон врагов в досе и порте
     *      в PolyGDX глючит зеркальный пол
     *      не работает changemap - сбрасывает уровень игрока
     *      ?после загрузки отображается неверное положение камеры на мгновенье
     *
     * Tile in VoxelSkin
     *
     * Software renderer: and the draw distance for voxel detail is really low
     * Software renderer: You might want to look at wall sprites. I noticed a lot of them clipping through geometry in classic render
     * Software renderer: Voxel is not clipped by ceiling geometry
     *
     * osdrows в сохранения конфига
     * Туман зависит от разрешения экрана (Polymost)
     * History list for last used IP's (client could be better for multiplayer) or copy paste IP if possible.
     * brz фильтр
     * broadcast
     * Some sort of anti-aliasing option. The NVidia control panel's anti-aliasing works, but it introduces artifacting on voxels.
     * бинд в консоль
     * сохранения в папку (почему то не находит файл)
     * 2д карта подглюкивает вылазиют
     * полигоны за скайбокс потолок
     * floor-alignment voxels for maphack
     */

    public static final String version = "25.011"; // XX. - year, XX - month, X - build
    public static final int CLIPMASK0 = (((1) << 16) + 1);
    public static final int CLIPMASK1 = (((256) << 16) + 64);
    public static final int MAXPSKYTILES = 256;
    public static final int MAXPALOOKUPS = 256;
    public static final int MAXSTATUS = 1024;
    public static final int DETAILPAL = (MAXPALOOKUPS - 1);
    public static final int GLOWPAL = (MAXPALOOKUPS - 2);
    public static final int SPECULARPAL = (MAXPALOOKUPS - 3);
    public static final int NORMALPAL = (MAXPALOOKUPS - 4);
    public static final int RESERVEDPALS = 4; // don't forget to increment this when adding reserved pals
    public static final int MAXSECTORSV8 = 4096;
    public static final int MAXWALLSV8 = 16384;
    public static final int MAXSPRITESV8 = 16384;
    public static final int MAXSECTORSV7 = 1024;
    public static final int MAXWALLSV7 = 8192;
    public static final int MAXSPRITESV7 = 4096;
    public static final int MAXSPRITESONSCREEN = 1024;
//    public static final int MAXUNIQHUDID = 256; // Extra slots so HUD models can store animation state without messing game sprites
//    public static final int MAXPSKYMULTIS = 8;
    public static final int MAXPLAYERS = 16;
    public static final short[] pow2char = {1, 2, 4, 8, 16, 32, 64, 128};
    public static final int[] pow2long = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483647,};

    // Constants
    public static final int HIT_TYPE_MASK = 0xE000_0000;
    public static final int HIT_INDEX_MASK = 0x1FFF_FFFF;
    public static final int HIT_SECTOR = 0x4000_0000;
    public static final int HIT_WALL = 0x8000_0000;
    public static final int HIT_SPRITE = 0xC000_0000;

    public static Hitscan pHitInfo;
    public static Neartag neartag;

    @Deprecated
    public static int clipmoveboxtracenum = 3;
    public static int hitscangoalx = (1 << 29) - 1, hitscangoaly = (1 << 29) - 1;
    public static int zr_ceilz, zr_ceilhit, zr_florz, zr_florhit;
    public static int clipmove_x, clipmove_y, clipmove_z;
    public static short clipmove_sectnum;
    public static int pushmove_x, pushmove_y, pushmove_z;
    public static short pushmove_sectnum;
    public static int USERTILES = 256;
    public static int MAXTILES = 9216 + USERTILES;
    public static int MAXSECTORS = MAXSECTORSV7;
    public static int MAXWALLS = MAXWALLSV7;
    public static int MAXSPRITES = MAXSPRITESV7;
//    public static final int MAXVOXELS = MAXSPRITES;
    // board variables
    public static short[] pskyoff;
    public static short[] zeropskyoff;
    public static short pskybits;
    public static byte parallaxtype;
    public static int visibility, parallaxvisibility;
    // automapping
    public static byte automapping;
    public static BitMap show2dsector;
    public static BitMap show2dwall;
    public static BitMap show2dsprite;
    protected static int SETSPRITEZ = 0;

    protected final GetZRange getZRange = new GetZRange(this);
    protected final EngineService engineService = new EngineService(this);
    protected final PushMover pushMover = new PushMover(this);
    protected final BoardService boardService;
    private final HitScanner scanner = new HitScanner(this);
    private final NearScanner nearScanner = new NearScanner(this);
    protected ClipMover clipmove = new ClipMover(this);
    protected PaletteManager paletteManager;
    protected final TileManager tileManager;
    protected final BuildGame game;
    // timer
    protected Timer timer;

    // Engine.c
    protected int randomseed;
    private DefScript defs;

    public Engine(BuildGame game) throws Exception { // gdxBuild
        InitArrays();
        this.game = game;
        this.tileManager = loadTileManager();
        this.paletteManager = loadpalette();
        this.boardService = createBoardService();

        EngineUtils.init(this); // loadtables

        parallaxtype = 2;
        pskybits = 0;
        automapping = 0;
        visibility = 512;
        parallaxvisibility = 512;

        randomseed = 1; // random for voxels
    }

    public GameConfig getConfig() {
        return game.pCfg;
    }

    public void InitArrays() { // gdxBuild
        pskyoff = new short[MAXPSKYTILES];
        zeropskyoff = new short[MAXPSKYTILES];

        show2dsector = new BitMap(MAXSECTORSV7);
        show2dwall = new BitMap(MAXWALLSV7);
        show2dsprite = new BitMap(MAXSPRITESV7);

        pHitInfo = new Hitscan();
        neartag = new Neartag();
    }

    protected Tables loadtables() throws Exception {
        return new Tables(game.getCache().getEntry("tables.dat", true));
    }

    protected TileManager loadTileManager() {
        return new TileManager();
    }

    public void faketimerhandler() {
        BuildNet net = game.pNet;
        if (net == null) {
            return; // not initialized yet
        }

        if (timer.getTotalClock() < net.ototalclock) {
            return;
        }

        game.getProcessor().update();
        if (!net.ready2send) {
            return;
        }

        net.ototalclock += timer.getFrameTicks();
        game.syncInput(net);
    }

    //
    // Exported Engine Functions
    //

    protected BoardService createBoardService() {
        return new BoardService();
    }

    public PaletteManager loadpalette() throws Exception { // jfBuild
        return new DefaultPaletteManager(this, new DefaultPaletteLoader(game.getCache().getEntry("palette.dat", true)));
    }

    public void uninit() {// gdxBuild
        uninitmultiplayer();
    }

    public long getCurrentTimeMillis() { // gdxBuild
        return System.currentTimeMillis();
    }

    public int nextsectorneighborz(int sectnum, int thez, int topbottom, int direction) { // jfBuild
        return boardService.nextSectorNeighborZ(sectnum, thez, topbottom, direction);
    }

    public void nextpage(float delta) { // gdxBuild
        timer.update(delta, game.pCfg.isVSync());
        faketimerhandler();
        Console.out.draw();


//        int chartY = 0;
//        timer.smoothRate.render(game.getRenderer(), 0, chartY);
//        chartY += timer.smoothRate.getHeight() + 20;
//        timer.timerCount.render(game.getRenderer(), 0, chartY);
    }

    public int neartag(int xs, int ys, int zs, int sectnum, int ange, Neartag near, int neartagrange, int tagsearch) { // jfBuild
        int result = nearScanner.scan(xs, ys, zs, sectnum, ange, neartagrange, tagsearch);
        NearInfo info = nearScanner.getInfo();

        near.tagsector = (short) info.getSector();
        near.tagwall = (short) info.getWall();
        near.tagsprite = (short) info.getSprite();
        near.taghitdist = info.getDistance();

        return result;
    }

    public int lastwall(int point) { // jfBuild
        return boardService.getLastWall(point);
    }

    public void srand(int seed) // gdxBuild
    {
        randomseed = seed;
    }

    public int getrand() // gdxBuild
    {
        return randomseed;
    }

    public int krand() { // jfBuild
        randomseed = (randomseed * 27584621) + 1;
        return (int) ((randomseed & 0xFFFFFFFFL) >> 16);
    }

    public int rand() // gdxBuild
    {
        return (int) (Math.random() * 32767);
    }

    public DefScript getDefs() {
        return defs;
    }

    public void setDefs(DefScript defs) {
        this.defs = defs;
    }

    public void dragpoint(int pointhighlight, int dax, int day) {
        Wall wal =  boardService.getWall(pointhighlight);
        if (wal == null) {
            return;
        }

        game.pInt.setwallinterpolate(pointhighlight, wal);
        wal.setX(dax);
        wal.setY(day);

        int cnt = MAXWALLS;
        int tempshort = pointhighlight; // search points CCW
        do {
            Wall wal2 = boardService.getWall(tempshort);
            if (wal2 != null && wal2.getNextwall() >= 0) {
                Wall nextWall = boardService.getWall(wal2.getNextwall());
                if (nextWall != null) {
                    tempshort = nextWall.getPoint2();
                    wal2 = nextWall.getWall2();
                    game.pInt.setwallinterpolate(tempshort, wal2);
                    wal2.setX(dax);
                    wal2.setY(day);
                }
            } else {
                tempshort = pointhighlight; // search points CW if not searched all the way around
                do {
                    Wall lastWall = boardService.getWall(lastwall(tempshort));
                    if (lastWall != null && lastWall.getNextwall() >= 0) {
                        tempshort = lastWall.getNextwall();
                        wal2 = boardService.getWall(tempshort);
                        if (wal2 != null) {
                            game.pInt.setwallinterpolate(tempshort, wal2);
                            wal2.setX(dax);
                            wal2.setY(day);
                        }
                    } else {
                        break;
                    }

                    cnt--;
                } while ((tempshort != pointhighlight) && (cnt > 0));
                break;
            }
            cnt--;
        } while ((tempshort != pointhighlight) && (cnt > 0));
    }

    public void setbrightness(int dabrightness, byte[] dapal) {
        paletteManager.setbrightness(dabrightness, dapal);
    }

    public boolean loadpic(Entry artFile) { // gdxBuild
        return tileManager.loadpic(artFile);
    }

    public int loadpics() {
        return tileManager.loadpics(game.getCache());
    }

    @Deprecated
    public byte[] loadtile(int tilenume) { // jfBuild
        return tileManager.loadtile(tilenume);
    }

    @NotNull
    public DynamicArtEntry allocatepermanenttile(int tilenume, int xsiz, int ysiz) { // jfBuild
        return tileManager.allocatepermanenttile(tilenume, xsiz, ysiz);
    }

    @NotNull
    public DynamicArtEntry allocatepermanenttile(ArtEntry artEntry) {
        return tileManager.allocatepermanenttile(artEntry);
    }

    @NotNull
    public ArtEntry getTile(int tilenum) {
        return tileManager.getTile(tilenum);
    }

    public TileManager getTileManager() {
        return tileManager;
    }

    public PaletteManager getPaletteManager() {
        return paletteManager;
    }

    public void inittimer(boolean isLegacy, int tickspersecond, int frameTicks) { // jfBuild
        int totalclock = 0;
        if (timer != null) {
            // reinit timer
            totalclock = timer.getTotalClock();
        }

        if (isLegacy) {
            this.timer = new LegacyTimer(tickspersecond, frameTicks);
        } else {
            this.timer = new Timer(tickspersecond, frameTicks);
        }
        timer.setTotalClock(totalclock);

//        Thread timerThread = new Thread(() -> {
//            try {
//                while (true) {
//                    long time = System.nanoTime();
//                    if (lastFrameTime == -1) lastFrameTime = time;
//
//                    timer.update((time - lastFrameTime) / 1000000000.0f, false);
//                    faketimerhandler();
//
//                    lastFrameTime = time;
//                    //noinspection BusyWait
//                    Thread.sleep(1);
//                }
//            } catch (InterruptedException ignored) {
//            }
//        });
//        timerThread.setDaemon(true);
//        timerThread.setName("Timer thread");
//        timerThread.start();
    }

    public Timer getTimer() {
        return timer;
    }

    public int getTotalClock() {
        return timer.getTotalClock();
    }

    public boolean notIntersect(int xs, int ys, int zs, int vx, int vy, int vz, int x1, int y1, int x2, int y2, Variable rx, Variable ry, Variable rz) {
        return !engineService.rIntersect(xs, ys, zs, vx, vy, vz, x1, y1, x2, y2, rx, ry, rz);
    }

    public int clipInsideBox(int x, int y, int wallnum, int walldist) {
        return clipinsidebox(x, y, wallnum, walldist);
    }

    public int clipinsidebox(int x, int y, int wallnum, int walldist) { // jfBuild
        return engineService.clipInsideBox(x, y, wallnum, walldist);
    }

    public int clipInsideBoxLine(int x, int y, int x1, int y1, int x2, int y2, int walldist) { // jfBuild
        return clipinsideboxline(x, y, x1, y1, x2, y2, walldist);
    }

    public int clipinsideboxline(int x, int y, int x1, int y1, int x2, int y2, int walldist) { // jfBuild
        return engineService.clipInsideBoxLine(x, y, x1, y1, x2, y2, walldist);
    }

    public boolean cansee(int x1, int y1, int z1, int sect1, int x2, int y2, int z2, int sect2) { // eduke32
        return engineService.canSee(x1, y1, z1, sect1, x2, y2, z2, sect2);
    }

    public boolean lIntersect(int x1, int y1, int z1, int x2, int y2, int z2, int x3, // jfBuild
                              int y3, int x4, int y4, Variable x, Variable y, Variable z) {
        return engineService.lIntersect(x1, y1, z1, x2, y2, z2, x3, y3, x4, y4, x, y, z);
    }

    public int hitscan(int xs, int ys, int zs, int sectnum, int vx, int vy, int vz, // jfBuild
                       Hitscan hit, int cliptype) {
        scanner.setGoal(hitscangoalx, hitscangoaly);
        boolean result = scanner.run(xs, ys, zs, sectnum, vx, vy, vz, cliptype);
        HitInfo is = scanner.getInfo();
        hit.set(is.x, is.y, is.z, is.sector, is.wall, is.sprite);
        return result ? 0 : -1;
    }

    public int clipmove(int x, int y, int z, int sectnum, // jfBuild
                        long xvect, long yvect, int walldist, int ceildist, int flordist, int cliptype) {
        int result = clipmove.invoke(x, y, z, sectnum, xvect, yvect, walldist, ceildist, flordist, cliptype);

        ClipInfo info = clipmove.getInfo();
        clipmove_x = info.getX();
        clipmove_y = info.getY();
        clipmove_z = info.getZ();
        clipmove_sectnum = (short) info.getSectnum();

        return result;
    }

    public int pushmove(int x, int y, int z, int sectnum, // jfBuild
                        int walldist, int ceildist, int flordist, int cliptype) {
        int result = pushMover.move(x, y, z, sectnum, walldist, ceildist, flordist, cliptype);

        ClipInfo info = pushMover.getInfo();
        pushmove_x = info.getX();
        pushmove_y = info.getY();
        pushmove_z = info.getZ();
        pushmove_sectnum = (short) info.getSectnum();

        return result;
    }

    ////////// BOARD MANIPULATION FUNCTIONS //////////

    public void getzrange(int x, int y, int z, int sectnum, // jfBuild
                          int walldist, int cliptype) {

        RangeZInfo info = getZRange.invoke(x, y, z, sectnum, walldist, cliptype);
        zr_ceilz = info.getCeilz();
        zr_ceilhit = info.getCeilhit();
        zr_florz = info.getFlorz();
        zr_florhit = info.getFlorhit();
    }

    public Board loadboard(Entry fil) throws WarningException {
        Board board;
        try {
            board = boardService.loadBoard(fil);
        } catch (Exception e) {
            throw new WarningException("Failed to load the map: " + fil.getName() + ".\r\n" + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }

        // #GDX board object can be changed because of update start sector
        board = boardService.prepareBoard(board);

        show2dsprite.clear();
        show2dsector.clear();
        show2dwall.clear();

        return board;
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public int insertsprite(int sectnum, int statnum) { // jfBuild
        return boardService.insertsprite(sectnum, statnum);
    }

    public int deletesprite(int spritenum) { // jfBuild
        boardService.deletesprite(spritenum);
        return 0;
    }

    public int changespritesect(int spritenum, int newsectnum) { // jfBuild
        boardService.changespritesect(spritenum, newsectnum);
        return (0);
    }

    public boolean changespritestat(int spritenum, int newstatnum) { // jfBuild
        return boardService.changespritestat(spritenum, newstatnum);
    }

    public int inside(int x, int y, int sectnum) { // jfBuild
        return boardService.inside(x, y, boardService.getSector(sectnum)) ? 1 : 0;
    }

    public boolean setsprite(int spritenum, int newx, int newy, int newz) { // jfBuild
        return boardService.setSprite(spritenum, newx, newy, newz, SETSPRITEZ != 0);
    }

    public int sectorofwall(int theline) { // jfBuild
        return boardService.sectorOfWall(theline);
    }

    public int getceilzofslope(int sectnum, int dax, int day) { // jfBuild
        return boardService.getceilzofslope(boardService.getSector(sectnum), dax, day);
    }

    public int getflorzofslope(int sectnum, int dax, int day) { // jfBuild
        return boardService.getflorzofslope(boardService.getSector(sectnum), dax, day);
    }

    public void getzsofslope(int sectnum, int dax, int day, AtomicInteger fz, AtomicInteger cz) {
        Sector sec = boardService.getSector(sectnum);
        if (sec != null) {
            fz.set(sec.getFloorz());
            cz.set(sec.getCeilingz());
            boardService.getzsofslope(sec, dax, day, fz, cz);
        }
    }

    public void alignceilslope(int dasect, int x, int y, int z) { // jfBuild
        Sector sec = boardService.getSector(dasect);
        if (sec != null) {
            sec.alignSlope(x, y, z, true);
        }
    }

    public void alignflorslope(int dasect, int x, int y, int z) { // jfBuild
        Sector sec = boardService.getSector(dasect);
        if (sec != null) {
            sec.alignSlope(x, y, z, false);
        }
    }

    public int updatesector(int x, int y, int sectnum) { // jfBuild
        return boardService.updatesector(x, y, sectnum);
    }

    public int updatesectorz(int x, int y, int z, int sectnum) { // jfBuild
        return boardService.updatesectorz(x, y, z, sectnum);
    }

}
