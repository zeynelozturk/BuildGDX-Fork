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

package ru.m210projects.Build.Render;

import static ru.m210projects.Build.Engine.show2dsector;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.Pragmas.klabs;

import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;

public class DefaultMapSettings implements IOverheadMapSettings {

	protected final BoardService boardService;
	public DefaultMapSettings(BoardService boardService) {
		this.boardService = boardService;
	}

	@Override
	public boolean isShowSprites(MapView view) {
		return false;
	}

	@Override
	public boolean isShowFloorSprites() { // in Polygon mode
		return false;
	}

	@Override
	public boolean isShowRedWalls() {
		return true;
	}

	@Override
	public boolean isShowAllPlayers() {
		return false;
	}

	@Override
	public boolean isSpriteVisible(MapView view, int index) {
		if (view == MapView.Polygons) {
			return false;
		}

		switch (boardService.getSprite(index).getCstat() & 48) {
		case 0:
			return true;
		case 16: // wall sprites
			return false;
		case 32: // floor sprites
			return false;
		}
		return true;
	}

	@Override
	public boolean isWallVisible(int w, int s) {
		Wall wal = boardService.getWall(w);
		Sector sec = boardService.getSector(s);
		if (wal.getNextsector() != 0) // red wall
		{
			return (wal.getNextwall() <= w && ((boardService.getSector(wal.getNextsector()).getCeilingz() != sec.getCeilingz() //
					|| boardService.getSector(wal.getNextsector()).getFloorz() != sec.getFloorz() //
					|| ((wal.getCstat() | boardService.getWall(wal.getNextwall()).getCstat()) & (16 + 32)) != 0) //
					|| (!isFullMap() && !show2dsector.getBit(wal.getNextsector()))));
		}
		return true;
	}

	@Override
	public int getWallColor(int w, int sec) {
		Wall wal = boardService.getWall(w);
//		if (Gameutils.isValidSector(wal.nextsector)) // red wall
//			return 31;
		return 31; // white wall
	}

	@Override
	public int getSpriteColor(int s) {
		Sprite spr = boardService.getSprite(s);
//		switch (spr.cstat & 48) {
//		case 0:
//			return 31;
//		case 16:
//			return 31;
//		case 32:
//			return 31;
//		}

		return 31;
	}

	@Override
	public int getPlayerSprite(int player) {
		return -1;
	}

	@Override
	public int getPlayerPicnum(int player) {
		int spr = getPlayerSprite(player);
		return spr != -1 ? boardService.getSprite(spr).getPicnum() : -1;
	}

	@Override
	public int getPlayerZoom(int player, int czoom) {
		Sprite pPlayer = boardService.getSprite(getPlayerSprite(player));
		int nZoom = czoom * (klabs((boardService.getSector(pPlayer.getSectnum()).getFloorz() - pPlayer.getZ()) >> 8) + pPlayer.getYrepeat());
		return BClipRange(nZoom, 22000, 0x20000);
	}

	@Override
	public boolean isFullMap() {
		return false;
	}

	@Override
	public boolean isScrollMode() {
		return false;
	}

	@Override
	public int getViewPlayer() {
		return 0;
	}

	@Override
	public int getSpriteX(int spr) {
		return boardService.getSprite(spr).getX();
	}

	@Override
	public int getSpriteY(int spr) {
		return boardService.getSprite(spr).getY();
	}

	@Override
	public int getSpritePicnum(int spr) {
		return boardService.getSprite(spr).getPicnum();
	}

	@Override
	public int getWallX(int w) {
		return boardService.getWall(w).getX();
	}

	@Override
	public int getWallY(int w) {
		return boardService.getWall(w).getY();
	}
}
