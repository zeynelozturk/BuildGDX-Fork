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

public interface IOverheadMapSettings {

	enum MapView {
		Polygons, Lines
	}

    boolean isFullMap();

	boolean isScrollMode();

	int getViewPlayer();

	boolean isShowSprites(MapView view);

	boolean isShowFloorSprites();

	boolean isShowRedWalls();

	boolean isShowAllPlayers();

	boolean isSpriteVisible(MapView view, int index);

	boolean isWallVisible(int w, int ses);

	int getWallColor(int w, int sec);

	int getWallX(int w);

	int getWallY(int w);

	int getSpriteColor(int s);

	int getSpriteX(int spr);

	int getSpriteY(int spr);

	int getSpritePicnum(int spr);

	int getPlayerSprite(int player);

	int getPlayerPicnum(int player);

	int getPlayerZoom(int player, int czoom);

}
