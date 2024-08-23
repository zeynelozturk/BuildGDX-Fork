// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Render.IOverheadMapSettings.MapView;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import static ru.m210projects.Build.Engine.show2dsector;
import static ru.m210projects.Build.net.Mmulti.connecthead;
import static ru.m210projects.Build.net.Mmulti.connectpoint2;
import static ru.m210projects.Build.Pragmas.dmulscale;
import static ru.m210projects.Build.Pragmas.mulscale;

public abstract class OrphoRenderer {

    protected final AbstractRenderer renderer;
    protected IOverheadMapSettings mapSettings;
    protected int xdim, ydim;

    public OrphoRenderer(AbstractRenderer renderer, IOverheadMapSettings mapSettings) {
        this.renderer = renderer;
        this.mapSettings = mapSettings;
        this.xdim = renderer.getWidth();
        this.ydim = renderer.getHeight();
    }

    public abstract void init();

    public abstract void uninit();

    public abstract int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow);

    public abstract void drawline256(int x1, int y1, int x2, int y2, int col);

    public abstract void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat,
                                      int cx1, int cy1, int cx2, int cy2);

    public abstract void nextpage();

    public abstract void drawmapview(int dax, int day, int zoome, int ang);

    protected int getclipmask(int a, int b, int c, int d) { // Ken did this
        int bA = a < 0 ? 1 : 0;
        int bB = b < 0 ? 1 : 0;
        int bC = c < 0 ? 1 : 0;
        int bD = d < 0 ? 1 : 0;

        d = (bA * 8) + (bB * 4) + (bC * 2) + bD;
        return (((d << 4) ^ 0xf0) | d);
    }

    public void drawoverheadmap(BoardService boardService, int cposx, int cposy, int czoom, short cang) {
        int i, k, x1, y1, x2 = 0, y2 = 0, ox, oy;
        int startwall, endwall;
        int xvect, yvect, xvect2, yvect2;

        Wall wal;
        int numsectors = boardService.getSectorCount();

        xvect = EngineUtils.sin(-cang) * czoom;
        yvect = EngineUtils.sin(1536 - cang) * czoom;
        xvect2 = mulscale(xvect, renderer.yxaspect, 16);
        yvect2 = mulscale(yvect, renderer.yxaspect, 16);

        // Draw red lines
        for (i = 0; i < numsectors; i++) {
            if ((!mapSettings.isFullMap() && !show2dsector.getBit(i))
                    || !boardService.isValidSector(i)) {
                continue;
            }

            Sector sec = boardService.getSector(i);
            if (!boardService.isValidWall(sec.getWallptr()) || sec.getWallnum() < 3) {
                continue;
            }

            startwall = sec.getWallptr();
            endwall = sec.getWallptr() + sec.getWallnum();

            if (startwall < 0 || endwall < 0) {
                continue;
            }

            for (int j = startwall; j < endwall; j++) {
                if (!boardService.isValidWall(j) || !boardService.isValidWall(boardService.getWall(j).getPoint2())) {
                    continue;
                }

                wal = boardService.getWall(j);
                if (mapSettings.isShowRedWalls() && boardService.isValidWall(wal.getNextwall())) {
                    if (boardService.isValidSector(wal.getNextsector())) {
                        if (mapSettings.isWallVisible(j, i)) {
                            ox = mapSettings.getWallX(j) - cposx;
                            oy = mapSettings.getWallY(j) - cposy;
                            x1 = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                            y1 = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);

                            ox = mapSettings.getWallX(wal.getPoint2()) - cposx;
                            oy = mapSettings.getWallY(wal.getPoint2()) - cposy;
                            x2 = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                            y2 = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);

                            int col = mapSettings.getWallColor(j, i);
                            if (col < 0) {
                                continue;
                            }

                            drawline256(x1, y1, x2, y2, col);
                        }
                    }
                }
            }
        }

        // Draw sprites
        if (mapSettings.isShowSprites(MapView.Lines)) {
            for (i = 0; i < numsectors; i++) {
                if (!mapSettings.isFullMap() && !show2dsector.getBit(i)) {
                    continue;
                }

                for (ListNode<Sprite> node = boardService.getSectNode(i); node != null; node = node.getNext()) {
                    int j = node.getIndex();
                    Sprite spr = node.get();

                    if ((spr.getCstat() & 0x8000) != 0 || spr.getXrepeat() == 0 || spr.getYrepeat() == 0
                            || !mapSettings.isSpriteVisible(MapView.Lines, j)) {
                        continue;
                    }

                    switch (spr.getCstat() & 48) {
                        case 0:
                            if (mapSettings.isShowSprites(MapView.Lines) && renderer.gotSector(i) && (czoom > 96)) {
                                ox = mapSettings.getSpriteX(j) - cposx;
                                oy = mapSettings.getSpriteY(j) - cposy;
                                x1 = dmulscale(ox, xvect, -oy, yvect, 16);
                                y1 = dmulscale(oy, xvect2, ox, yvect2, 16);
                                int daang = (spr.getAng() - cang) & 2047;
                                rotatesprite((x1 << 4) + (xdim << 15), (y1 << 4) + (ydim << 15),
                                        mulscale((long) czoom * spr.getYrepeat(), renderer.yxaspect, 16), daang, spr.getPicnum(), spr.getShade(), spr.getPal(),
                                        (spr.getCstat() & 2) >> 1, renderer.windowx1, renderer.windowy1, renderer.windowx2, renderer.windowy2);
                            }
                            break;
                        case 16: {
                            ArtEntry pic = renderer.getTile(spr.getPicnum());

                            x1 = mapSettings.getSpriteX(j);
                            y1 = mapSettings.getSpriteY(j);
                            byte xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                            if ((spr.getCstat() & 4) > 0) {
                                xoff = (byte) -xoff;
                            }
                            k = spr.getAng();
                            int l = spr.getXrepeat();
                            int dax = EngineUtils.cos(k - 512) * l;
                            int day = EngineUtils.sin(k - 512) * l;
                            l = pic.getWidth();
                            k = (l >> 1) + xoff;
                            x1 -= mulscale(dax, k, 16);
                            x2 = x1 + mulscale(dax, l, 16);
                            y1 -= mulscale(day, k, 16);
                            y2 = y1 + mulscale(day, l, 16);

                            ox = x1 - cposx;
                            oy = y1 - cposy;
                            x1 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y1 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            ox = x2 - cposx;
                            oy = y2 - cposy;
                            x2 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y2 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            int col = mapSettings.getSpriteColor(j);
                            if (col < 0) {
                                break;
                            }

                            drawline256(x1 + (xdim << 11), y1 + (ydim << 11), x2 + (xdim << 11), y2 + (ydim << 11), col);
                        }
                        break;
                        case 32: {
                            ArtEntry pic = renderer.getTile(spr.getPicnum());

                            byte xoff = (byte) (pic.getOffsetX() + spr.getXoffset());
                            byte yoff = (byte) (pic.getOffsetY() + spr.getYoffset());
                            if ((spr.getCstat() & 4) > 0) {
                                xoff = (byte) -xoff;
                            }
                            if ((spr.getCstat() & 8) > 0) {
                                yoff = (byte) -yoff;
                            }

                            k = spr.getAng();
                            int cosang = EngineUtils.cos(k);
                            int sinang = EngineUtils.sin(k);
                            int xspan = pic.getWidth();
                            int xrepeat = spr.getXrepeat();
                            int yspan = pic.getHeight();
                            int yrepeat = spr.getYrepeat();

                            int dax = ((xspan >> 1) + xoff) * xrepeat;
                            int day = ((yspan >> 1) + yoff) * yrepeat;
                            x1 = mapSettings.getSpriteX(j) + dmulscale(sinang, dax, cosang, day, 16);
                            y1 = mapSettings.getSpriteY(j) + dmulscale(sinang, day, -cosang, dax, 16);
                            int l = xspan * xrepeat;
                            x2 = x1 - mulscale(sinang, l, 16);
                            y2 = y1 + mulscale(cosang, l, 16);
                            l = yspan * yrepeat;
                            k = -mulscale(cosang, l, 16);
                            int x3 = x2 + k;
                            int x4 = x1 + k;
                            k = -mulscale(sinang, l, 16);
                            int y3 = y2 + k;
                            int y4 = y1 + k;

                            ox = x1 - cposx;
                            oy = y1 - cposy;
                            x1 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y1 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            ox = x2 - cposx;
                            oy = y2 - cposy;
                            x2 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y2 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            ox = x3 - cposx;
                            oy = y3 - cposy;
                            x3 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y3 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            ox = x4 - cposx;
                            oy = y4 - cposy;
                            x4 = dmulscale(ox, xvect, -oy, yvect, 16);
                            y4 = dmulscale(oy, xvect2, ox, yvect2, 16);

                            int col = mapSettings.getSpriteColor(j);
                            if (col < 0) {
                                break;
                            }

                            drawline256(x1 + (xdim << 11), y1 + (ydim << 11), x2 + (xdim << 11), y2 + (ydim << 11), col);

                            drawline256(x2 + (xdim << 11), y2 + (ydim << 11), x3 + (xdim << 11), y3 + (ydim << 11), col);

                            drawline256(x3 + (xdim << 11), y3 + (ydim << 11), x4 + (xdim << 11), y4 + (ydim << 11), col);

                            drawline256(x4 + (xdim << 11), y4 + (ydim << 11), x1 + (xdim << 11), y1 + (ydim << 11), col);
                        }
                        break;
                    }
                }
            }
        }

        // Draw white lines
        for (i = 0; i < numsectors; i++) {

            if ((!mapSettings.isFullMap() && !show2dsector.getBit(i))
                    || !boardService.isValidSector(i)) {
                continue;
            }

            startwall = boardService.getSector(i).getWallptr();
            endwall = boardService.getSector(i).getWallptr() + boardService.getSector(i).getWallnum();

            if (startwall < 0 || endwall < 0) {
                continue;
            }

            k = -1;
            for (int j = startwall; j < endwall; j++) {
                wal = boardService.getWall(j);
                if (!boardService.isValidWall(j) || !boardService.isValidWall(boardService.getWall(j).getPoint2())) {
                    continue;
                }

                if (wal.getNextwall() >= 0) {
                    continue;
                }
                ArtEntry pic = renderer.getTile(wal.getPicnum());
                if (!pic.hasSize()) {
                    continue;
                }

                if (j == k) {
                    x1 = x2;
                    y1 = y2;
                } else {
                    ox = mapSettings.getWallX(j) - cposx;
                    oy = mapSettings.getWallY(j) - cposy;
                    x1 = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                    y1 = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
                }

                k = wal.getPoint2();
                if (boardService.getWall(k) == null) {
                    continue;
                }

                ox = mapSettings.getWallX(k) - cposx;
                oy = mapSettings.getWallY(k) - cposy;
                x2 = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
                y2 = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);

                int col = mapSettings.getWallColor(j, i);
                if (col < 0) {
                    continue;
                }

                drawline256(x1, y1, x2, y2, col);
            }
        }

        // draw player
        for (i = connecthead; i >= 0; i = connectpoint2[i]) {
            int spr = mapSettings.getPlayerSprite(i);
            if (spr == -1 || !boardService.isValidSector(boardService.getSprite(spr).getSectnum())) {
                continue;
            }

            Sprite pPlayer = boardService.getSprite(spr);
            ox = mapSettings.getSpriteX(spr) - cposx;
            oy = mapSettings.getSpriteY(spr) - cposy;

            int dx = mulscale(ox, xvect, 16) - mulscale(oy, yvect, 16);
            int dy = mulscale(oy, xvect2, 16) + mulscale(ox, yvect2, 16);

            int dang = (pPlayer.getAng() - cang) & 0x7FF;
            int viewindex = mapSettings.getViewPlayer();
            if (i == viewindex && !mapSettings.isScrollMode()) {
                dx = 0;
                dy = viewindex ^ i;
                dang = 0;
            }

            if (i == viewindex || mapSettings.isShowAllPlayers()) {
                int picnum = mapSettings.getPlayerPicnum(i);
                if (picnum == -1) { // draw it with lines
//					ox = (EngineUtils.sin((pPlayer.ang + 512) & 2047) >> 7);
//					oy = (EngineUtils.sin((pPlayer.ang) & 2047) >> 7);
                    x2 = 0;
                    y2 = -(mapSettings.getPlayerZoom(i, czoom) << 1);

                    int x3 = mulscale(x2, renderer.yxaspect, 16);
                    int y3 = mulscale(y2, renderer.yxaspect, 16);

                    int col = mapSettings.getSpriteColor(spr);
                    if (col < 0) {
                        continue;
                    }

                    drawline256(dx - x2 + (xdim << 11), dy - y3 + (ydim << 11), dx + x2 + (xdim << 11),
                            dy + y3 + (ydim << 11), col);
                    drawline256(dx - y2 + (xdim << 11), dy + x3 + (ydim << 11), dx + x2 + (xdim << 11),
                            dy + y3 + (ydim << 11), col);
                    drawline256(dx + y2 + (xdim << 11), dy - x3 + (ydim << 11), dx + x2 + (xdim << 11),
                            dy + y3 + (ydim << 11), col);
                } else {
                    int nZoom = mapSettings.getPlayerZoom(i, czoom);
                    int sx = (dx << 4) + (xdim << 15);
                    int sy = (dy << 4) + (ydim << 15);

                    rotatesprite(sx, sy, nZoom, (short) dang, mapSettings.getPlayerPicnum(i), pPlayer.getShade(),
                            pPlayer.getPal(), (pPlayer.getCstat() & 2) >> 1, renderer.windowx1, renderer.windowy1, renderer.windowx2, renderer.windowy2);
                }
            }
        }
    }

    public void resize(int width, int height) {
        this.xdim = width;
        this.ydim = height;
    }
}
