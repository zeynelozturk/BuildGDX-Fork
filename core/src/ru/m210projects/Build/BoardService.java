// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
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
//
//Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.Render.listeners.WorldListener;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.LinkedMap;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.collections.SpriteMap;
import ru.m210projects.Build.Types.collections.ValueSetter;
import ru.m210projects.Build.exceptions.WarningException;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Engine.*;

public class BoardService {

    protected final AtomicInteger floorz = new AtomicInteger();
    protected final AtomicInteger ceilingz = new AtomicInteger();
    protected final Map<Integer, Class<? extends Sector>> versionToSectorClass = new HashMap<>();
    protected final Map<Integer, Class<? extends Wall>> versionToWallClass = new HashMap<>();
    protected final Map<Integer, Class<? extends Sprite>> versionToSpriteClass = new HashMap<>();
    protected final Set<Integer> versions = new HashSet<>();
    protected Board board;

    protected LinkedMap<Sprite> spriteStatMap = createSpriteMap(1, new ArrayList<>(), 1, Sprite::setStatnum);
    protected LinkedMap<Sprite> spriteSectMap = createSpriteMap(1, new ArrayList<>(), 1, Sprite::setSectnum);
    protected WorldListener listener = WorldListener.DUMMY_LISTENER;

    public BoardService() {
        registerBoard(7, Sector.class, Wall.class, Sprite.class);
    }

    public void setListener(WorldListener listener) {
        this.listener = listener;
    }

    protected void initSpriteLists(Board board) {
        List<Sprite> sprites = board.getSprites();
        this.spriteStatMap = createSpriteMap(MAXSTATUS, sprites, MAXSPRITESV7, Sprite::setStatnum);
        this.spriteSectMap = createSpriteMap(board.getSectorCount(), sprites, MAXSPRITESV7, Sprite::setSectnum);
    }

    protected void registerBoard(final int version, Class<? extends Sector> sectorClass, Class<? extends Wall> wallClass, Class<? extends Sprite> spriteClass) {
        versionToSectorClass.put(version, sectorClass);
        versionToWallClass.put(version, wallClass);
        versionToSpriteClass.put(version, spriteClass);
        versions.add(version);
    }

    protected Board loadBoard(Entry entry) throws IOException {
        try (InputStream is = entry.getInputStream()) {
            int version = StreamUtils.readInt(is);
            if (!versions.contains(version)) {
                throw new WarningException("Wrong version: " + version);
            }

            BuildPos startPos = new BuildPos(StreamUtils.readInt(is),
                    StreamUtils.readInt(is),
                    StreamUtils.readInt(is),
                    StreamUtils.readShort(is),
                    StreamUtils.readShort(is));

            List<Sprite> sprites = new ArrayList<>();
            Constructor<? extends Sector> sectorConstructor = versionToSectorClass.get(version).getConstructor();
            Constructor<? extends Wall> wallConstructor = versionToWallClass.get(version).getConstructor();
            Constructor<? extends Sprite> spriteConstructor = versionToSpriteClass.get(version).getConstructor();

            Sector[] sectors = new Sector[StreamUtils.readShort(is)];
            for (int i = 0; i < sectors.length; i++) {
                sectors[i] = sectorConstructor.newInstance().readObject(is);
            }

            Wall[] walls = new Wall[StreamUtils.readShort(is)];
            for (int i = 0; i < walls.length; i++) {
                walls[i] = wallConstructor.newInstance().readObject(is);
            }

            int numSprites = StreamUtils.readShort(is);
            for (int i = 0; i < numSprites; i++) {
                sprites.add(spriteConstructor.newInstance().readObject(is));
            }

            return new Board(startPos, sectors, walls, sprites);
        } catch (ReflectiveOperationException e) {
            throw new WarningException(e.toString());
        }
    }

    /**
     * Set new board
     */
    public Board prepareBoard(Board board) throws WarningException {
        Wall[] walls = board.getWalls();
        Sector[] sectors = board.getSectors();
        List<Sprite> sprites = board.getSprites();

        this.board = board;
        initSpriteLists(board);

        // init maps for new board
        for (Sprite spr : sprites) {
            insertsprite(spr.getSectnum(), spr.getStatnum());
        }

        BuildPos startPos = board.getPos();

        // Must be after loading sectors, etc!
        int sectnum = updatesector(startPos.getX(), startPos.getY(), startPos.getSectnum());
        if (sectnum != startPos.getSectnum()) {
            startPos = new BuildPos(startPos.getX(), startPos.getY(), startPos.getZ(), startPos.getAng(), sectnum);
            this.board = new Board(startPos, sectors, walls, sprites);
        }

        Sector startSector = this.board.getSector(startPos.getSectnum());
        if (startSector == null || !startSector.inside(startPos.getX(), startPos.getY())) {
            throw new WarningException("Player should be in a sector!");
        }

        return this.board;
    }

    public void notifyBoardChanged() {
        listener.onLoadBoard(this.board);
    }

    protected SpriteMap createSpriteMap(int listCount, List<Sprite> spriteList, int spriteCount, ValueSetter<Sprite> valueSetter) {
        return new SpriteMap(listCount, spriteList, spriteCount, valueSetter);
    }

    public int insertspritesect(int sectnum) {
        return spriteSectMap.insert(sectnum);
    }

    public void insertspritestat(int newstatnum) {
        spriteStatMap.insert(newstatnum);
    }

    public int insertsprite(int sectnum, int statnum) {
        insertspritestat(statnum);
        int spritenum = (insertspritesect(sectnum));
        listener.onAddSprite(spritenum);
        return spritenum;
    }

    public boolean deletesprite(int spritenum) {
        listener.onRemoveSprite(spritenum);
        deletespritestat(spritenum);
        return (deletespritesect(spritenum));
    }

    public boolean changespritesect(int spritenum, int newsectnum) {
        if ((newsectnum < 0) || (newsectnum > board.getSectorCount())) {
            return false;
        }

        Sprite sprite = board.getSprite(spritenum);
        if (sprite == null || !isValidSector(sprite.getSectnum())) {
            return false;
        }

        return spriteSectMap.set(spritenum, newsectnum);
    }

    public boolean changespritestat(int spritenum, int newstatnum) {
        if ((newstatnum < 0) || (newstatnum > MAXSTATUS)) {
            return false;
        }

        Sprite sprite = board.getSprite(spritenum);
        if (sprite == null || sprite.getStatnum() == MAXSTATUS) {
            return false;
        }

        return spriteStatMap.set(spritenum, newstatnum);
    }

    public boolean deletespritesect(int spritenum) {
        return spriteSectMap.remove(spritenum);
    }

    public void deletespritestat(int spritenum) {
        spriteStatMap.remove(spritenum);
    }

    public boolean setSprite(int spritenum, int newx, int newy, int newz, boolean checkZ) {
        Sprite sprite = board.getSprite(spritenum);
        if (sprite == null) {
            return false;
        }

        sprite.setX(newx);
        sprite.setY(newy);
        sprite.setZ(newz);
        int sectnum = sprite.getSectnum();
        if (checkZ) {
            sectnum = updatesectorz(newx, newy, newz, sectnum);
        } else {
            sectnum = updatesector(newx, newy, sectnum);
        }

        if (sectnum < 0) {
            return false;
        }

        if (sectnum != sprite.getSectnum()) {
            changespritesect(spritenum, sectnum);
        }

        return true;
    }

    public int getLastWall(int point) {
        final Wall[] walls = board.getWalls();

        if ((point > 0) && (walls[point - 1].getPoint2() == point)) {
            return (point - 1);
        }

        int i = point, j;
        int cnt = board.getWallCount();
        do {
            j = walls[i].getPoint2();
            if (j == point) {
                return (i);
            }
            i = j;
            cnt--;
        } while (cnt > 0);

        return (point);
    }

    ////////// SECTOR MANIPULATION FUNCTIONS ////////

    public int updatesector(int x, int y, int sector) {
        Sector sec = board.getSector(sector);
        if (sec != null) {
            if (sec.inside(x, y)) {
                return sector;
            }

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wall = wn.get();
                sector = wall.getNextsector();
                sec = board.getSector(sector);
                if (sec != null && sec.inside(x, y)) {
                    return sector;
                }
            }
        }

        Sector[] sectors = board.getSectors();
        for (int i = sectors.length - 1; i >= 0; i--) {
            if (sectors[i].inside(x, y)) {
                return i;
            }
        }

        return -1;
    }

    public int updatesectorz(int x, int y, int z, int sectnum) {
        Sector sec = board.getSector(sectnum);
        if (sec != null) {
            if (insidez(x, y, z, sec)) {
                return sectnum;
            }

            for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
                Wall wall = wn.get();
                sectnum = wall.getNextsector();
                if (insidez(x, y, z, board.getSector(sectnum))) {
                    return sectnum;
                }
            }
        }

        for (int i = (board.getSectorCount() - 1); i >= 0; i--) {
            if (insidez(x, y, z, board.getSector(i))) {
                return i;
            }
        }

        return -1;
    }

    public boolean getzsofslope(Sector sec, int dax, int day, AtomicInteger floorZ, AtomicInteger ceilingZ) {
        if (sec != null) {
            sec.getzsofslope(dax, day, floorZ, ceilingZ);
            return true;
        }
        return false;
    }

    public int getceilzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, null, ceilingz);
        return ceilingz.get();
    }

    public int getflorzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, floorz, null);
        return floorz.get();
    }

    public int sectorOfWall(int theline) {
        Wall wall = board.getWall(theline);
        if (wall == null) {
            return -1;
        }

        Wall nextWall = board.getWall(wall.getNextwall());
        if (nextWall != null) {
            return nextWall.getNextsector();
        }

        Sector[] sectors = board.getSectors();
        int gap = (board.getSectorCount() >> 1);
        int i = gap;
        while (gap > 1) {
            gap >>= 1;
            if (sectors[i].getWallptr() < theline) {
                i += gap;
            } else {
                i -= gap;
            }
        }

        while (sectors[i].getWallptr() > theline) {
            i--;
        }

        while (sectors[i].getWallptr() + sectors[i].getWallnum() <= theline) {
            i++;
        }
        return (i);
    }

    public boolean insidez(int x, int y, int z, Sector sector) {
        if (sector == null) {
            return false;
        }

        if (getzsofslope(sector, x, y, floorz, ceilingz)
                && (z >= ceilingz.get()) && (z <= floorz.get())) {
            return inside(x, y, sector);
        }
        return sector.inside(x, y, z);
    }

    public boolean inside(int x, int y, Sector sector) {
        if (sector == null) {
            return false;
        }

        int cnt = 0;
        for (ListNode<Wall> wn = sector.getWallNode(); wn != null; wn = wn.getNext()) {
            Wall wal = wn.get();
            Wall wal2 = getNextWall(wal);
            int y1 = wal.getY() - y;
            int y2 = wal2.getY() - y;

            if ((y1 ^ y2) < 0) {
                int x1 = wal.getX() - x;
                int x2 = wal2.getX() - x;
                if ((x1 ^ x2) >= 0) {
                    cnt ^= x1;
                } else {
                    cnt ^= (x1 * y2 - x2 * y1) ^ y2;
                }
            }
        }

        return (cnt >>> 31) != 0;
    }

    public int nextSectorNeighborZ(int sectnum, int thez, int topbottom, int direction) { // jfBuild
        Sector sec = board.getSector(sectnum);
        if (sec == null) {
            return -1;
        }

        int sectorToUse = -1, testz;
        int nextz = Integer.MIN_VALUE;
        if (direction == 1) {
            nextz = Integer.MAX_VALUE;
        }

        for (ListNode<Wall> wn = sec.getWallNode(); wn != null; wn = wn.getNext()) {
            Wall wal = wn.get();
            int nextSectNum = wal.getNextsector();
            Sector nextSector = board.getSector(nextSectNum);
            if (nextSector != null) {
                if (topbottom == 1) {
                    testz = nextSector.getFloorz();
                } else {
                    testz = nextSector.getCeilingz();
                }

                if (direction == 1) {
                    if ((testz > thez) && (testz < nextz)) {
                        nextz = testz;
                        sectorToUse = nextSectNum;
                    }
                } else {
                    if ((testz < thez) && (testz > nextz)) {
                        nextz = testz;
                        sectorToUse = nextSectNum;
                    }
                }
            }
        }

        return sectorToUse;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Set board (when load game)
     */
    public void setBoard(Board board) {
        this.board = board;
        initSpriteLists(board);

        List<Sprite> sprites = board.getSprites();
        for (int i = 0; i < sprites.size(); i++) {
            Sprite spr = sprites.get(i);
            changespritestat(i, spr.getStatnum());
            changespritesect(i, spr.getSectnum());
        }
    }

    public ListNode<Sprite> getSectNode(int sector) {
        return spriteSectMap.getFirst(sector);
    }

    public ListNode<Sprite> getStatNode(int statnum) {
        return spriteStatMap.getFirst(statnum);
    }

    public boolean isValidSector(int index) {
        return board.isValidSector(index);
    }

    public boolean isValidWall(int index) {
        return board.isValidWall(index);
    }

    public boolean isValidSprite(int index) {
        return board.isValidSprite(index);
    }

    @Nullable
    public Sector getSector(int index) {
        return board.getSector(index);
    }

    @Nullable
    public Wall getWall(int index) {
        return board.getWall(index);
    }

    @Nullable
    public Sprite getSprite(int index) {
        return board.getSprite(index);
    }

    public Wall getNextWall(Wall wall) {
        return board.getWall(wall.getPoint2());
    }

    public int getSectorCount() {
        return board.getSectorCount();
    }

    public int getWallCount() {
        return board.getWallCount();
    }

    public int getSpriteCount() {
        return board.getSpriteCount();
    }
}
