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
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.osd.Console;import ru.m210projects.Build.Types.BuildPos;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.osd.OsdColor;

import java.util.List;

public class Board {

    private final BuildPos pos;
    private final Sector[] sectors;
    private final Wall[] walls;
    private final List<Sprite> sprites;

    public Board(BuildPos pos, Sector[] sectors, Wall[] walls, List<Sprite> sprites) {
        this.pos = pos;
        this.sectors = sectors;
        this.walls = walls;
        this.sprites = sprites;

        // sector wall array init with wall checking
        for (int s = 0; s < sectors.length; s++) {
            Sector sec = sectors[s];
            LinkedList<Wall> wallList = new LinkedList<>();

            int startWall = sec.getWallptr();
            int endWall = startWall + sec.getWallnum() - 1;
            for (int i = startWall; i <= endWall; i++) {
                Wall wal = getWall(i);
                Wall wal2;
                if (wal == null || (wal2 = getWall(wal.getPoint2())) == null) {
                    Console.out.println(String.format("Sector %d has corrupt contour", s), OsdColor.RED);
                    sec.setWallnum(0);
                    sec.setWallptr(0);
                    wallList = new LinkedList<>();
                    break;
                }
                wal.setWall2(wal2);
                wallList.addLast(wal.buildNode(i));
            }
            sec.setWallList(wallList);
        }
    }

    public BuildPos getPos() {
        return pos;
    }

    public List<Sprite> getSprites() {
        return sprites;
    }

    public Sector[] getSectors() {
        return sectors;
    }

    public Wall[] getWalls() {
        return walls;
    }

    @Nullable
    public Sprite getSprite(int index) {
        if(!isValidSprite(index)) {
            return null;
        }
        return sprites.get(index);
    }

    @Nullable
    public Wall getWall(int index) {
        if(!isValidWall(index)) {
            return null;
        }
        return walls[index];
    }

    @Nullable
    public Sector getSector(int index) {
        if(!isValidSector(index)) {
            return null;
        }
        return sectors[index];
    }

    public int getWallCount() {
        return walls.length;
    }

    public int getSectorCount() {
        return sectors.length;
    }

    public int getSpriteCount() {
        return sprites.size();
    }

    public boolean isValidSector(int sectnum) {
        return sectnum >= 0 && sectnum < sectors.length;
    }

    public boolean isValidWall(int wallnum) {
        return wallnum >= 0 && wallnum < walls.length;
    }

    public boolean isValidSprite(int spritenum) {
        return spritenum >= 0 && spritenum < sprites.size();
    }
}
