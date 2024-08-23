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

package ru.m210projects.Build.Render.GdxRender;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.IntArray;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.IndexedShader;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Types.AnimType;
import ru.m210projects.Build.Types.QuickSort;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.filehandle.art.ArtEntry;

import java.util.Comparator;

import static com.badlogic.gdx.graphics.GL20.*;
import static ru.m210projects.Build.Engine.MAXSPRITES;
import static ru.m210projects.Build.Engine.MAXSPRITESV7;
import static ru.m210projects.Build.Pragmas.klabs;
import static ru.m210projects.Build.Pragmas.mulscale;

public class SpriteRenderer {

    private final Matrix4 transform;
    private final SpriteComparator comp;
    private final GDXRenderer parent;
    private final Engine engine;
    private final IntArray spritesz = new IntArray(MAXSPRITESV7);
    private BuildCamera cam;

    public SpriteRenderer(Engine engine, GDXRenderer parent) {
        transform = new Matrix4();
        comp = new SpriteComparator();
        this.parent = parent;
        this.engine = engine;
    }

    public void sort(Sprite[] array, int len) {
        for (int i = 0; i < len; i++) {
            Sprite spr = array[i];
            int s = spr.getOwner();
            if (s == -1) {
                continue;
            }

            int[] items = this.spritesz.items;
            if (s >= items.length) {
                items = spritesz.ensureCapacity((int) (s * 1.75f));
            }

            items[s] = spr.getZ();
            if (!Gameutils.isValidTile(spr.getPicnum())) {
                continue;
            }

            if ((spr.getCstat() & 48) != 32) {
                ArtEntry pic = parent.getTile(spr.getPicnum());
                byte yoff = (byte) (pic.getOffsetY() + spr.getYoffset());
                items[s] -= ((yoff * spr.getYrepeat()) << 2);
                int yspan = (pic.getHeight() * spr.getYrepeat() << 2);
                if ((spr.getCstat() & 128) == 0) {
                    items[s] -= (yspan >> 1);
                }
                if (klabs(items[s] - parent.globalposz) < (yspan >> 1)) {
                    items[s] = parent.globalposz;
                }
            }
        }

        QuickSort.sort(array, len, comp);
    }

    public void begin(BuildCamera cam) {
        this.cam = cam;
    }

    public Matrix4 getMatrix(Sprite tspr, int texx, int texy) {
        int picnum = tspr.getPicnum();
        int orientation = tspr.getCstat();
        int spritenum = tspr.getOwner();
        ArtEntry pic = parent.getTile(picnum);

        int xoff = 0, yoff = 0;
        if ((orientation & 48) != 48) {
            if (pic.getType() != AnimType.NONE) {
                picnum += parent.animateoffs(picnum, spritenum + 32768);
                pic = parent.getTile(picnum);
            }

            xoff = tspr.getXoffset();
            yoff = tspr.getYoffset();
            xoff += pic.getOffsetX();
            yoff += pic.getOffsetY();
        }

        int tsizx = pic.getWidth();
        int tsizy = pic.getHeight();

        if (tsizx <= 0 || tsizy <= 0) {
            return null;
        }

        boolean xflip = (orientation & 4) != 0;
        boolean yflip = (orientation & 8) != 0;

//		float posx = tspr.x;
//		float posy = tspr.y;
//		float posz = tspr.z;
//		transform.setToTranslation(posx, posy, posz);
//
//		switch ((orientation >> 4) & 3) {
//		case 0: // Face sprite
//			int ang = ((int) globalang - 512) & 0x7FF;
//			if (xflip ^ yflip) {
//				ang += 1024;
//				if (!xflip)
//					xoff = -xoff;
//			} else if (xflip)
//				xoff = -xoff;
//
//			transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees(ang));
//			transform.translate((tspr.xrepeat * xoff) / 5.0f, 0, -(tspr.yrepeat * yoff) * 4.0f);
//
//			if ((orientation & 128) != 0) {
//				float zoffs = (tspr.yrepeat * tsizy) * 2.0f;
//				if ((tsizy & 1) != 0)
//					zoffs += (tspr.yrepeat * 2.0f); // Odd yspans
//				transform.translate(0, 0, zoffs);
//			}
//
//			if (yflip) {
//				transform.rotate(0, 1, 0, 180);
//				transform.translate(0, 0, (tspr.yrepeat * pic.getHeight()) * 4.0f);
//			} else
//				transform.translate(0, 0, (tspr.yrepeat * (pic.getHeight() - tsizy)) * 4.0f);
//
//			transform.scale((tspr.xrepeat * pic.getWidth()) / 5.0f, 0, 4 * tspr.yrepeat * pic.getHeight());
//			break;
//		case 1: // Wall sprite
//			if (yflip)
//				yoff = -yoff;
//			int wang = (int) Gameutils.AngleToDegrees((tspr.ang + ((xflip ^ yflip) ? 1536 : 512)) & 0x7FF);
//			if ((orientation & 64) == 0) {
//				int dang = (((tspr.ang - EngineUtils.getAngle(tspr.x - globalposx, tspr.y - globalposy)) & 0x7FF) - 1024);
//				if (dang > 512 || dang < -512) {
//					xflip = !xflip;
//				}
//			}
//
//			if (xflip ^ yflip) {
//				if (!xflip)
//					xoff = -xoff;
//			} else if (xflip)
//				xoff = -xoff;
//
//			transform.rotate(0, 0, 1, wang);
//			transform.translate((tspr.xrepeat * xoff) / 4.0f, 0, -(tspr.yrepeat * yoff) * 4.0f);
//			if ((orientation & 128) != 0)
//				transform.translate(0, 0, (tspr.yrepeat * tsizy) * 2.0f);
//
//			if (yflip) {
//				transform.rotate(0, 1, 0, 180);
//				transform.translate(0, 0, (tspr.yrepeat * pic.getHeight()) * 4.0f);
//			} else
//				transform.translate(0, 0, (tspr.yrepeat * (pic.getHeight() - tsizy)) * 4.0f);
//
//			transform.scale((tspr.xrepeat * pic.getWidth()) / 4.0f, 0, (tspr.yrepeat * pic.getHeight()) * 4.0f);
//			break;
//		case 2: // Floor sprite
//			if (yflip)
//				yoff = -yoff;
//
//			if ((orientation & 64) == 0) {
//				if (tspr.z < globalposz) {
//					yflip = true;
//				} else if (yflip)
//					yflip = !yflip;
//			}
//
//			transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees((tspr.ang + (xflip ? 512 : 1536)) & 0x7FF));
//			transform.rotate(1, 0, 0, xflip ? -90 : 90);
//			transform.translate(0, 0, (tspr.yrepeat * (2 * pic.getHeight() - tsizy)) / 8.0f);
//			transform.translate((tspr.xrepeat * xoff) / 4.0f, 0, -(tspr.yrepeat * yoff) / 4.0f);
//			transform.scale((tspr.xrepeat * pic.getWidth()) / 4.0f, 0, (tspr.yrepeat * pic.getHeight()) / 4.0f);
//			break;
//		}

        float xspans;
        float posx = tspr.getX();
        float posy = tspr.getY();
        float posz = tspr.getZ();
        transform.setToTranslation(posx, posy, posz);

        switch ((orientation >> 4) & 3) {
            case 0: // Face sprite
                xspans = 5.0f;
                int ang = ((int) parent.globalang - 512) & 0x7FF;
                if (xflip ^ yflip) {
                    ang += 1024;
                    if (!xflip) {
                        xoff = -xoff;
                    }
                    if (yflip) {
                        xspans = -xspans;
                    }
                } else if (xflip) {
                    xoff = -xoff;
                    xspans = -xspans;
                }

                transform.translate(0, 0, (tspr.getYrepeat() * texy * (yflip ? 2.0f : -2.0f)));
                transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees(ang));
                transform.translate((tspr.getXrepeat() * xoff) / (5.0f), 0, -((yoff * tspr.getYrepeat()) << 2));

                if ((tsizx & 1) == 0) {
                    transform.translate((tspr.getXrepeat() >> 1) / xspans, 0, 0);
                }

                if ((orientation & 128) != 0) {
                    float zoffs = ((tsizy * tspr.getYrepeat()) << 1);
                    if ((tsizy & 1) != 0) {
                        zoffs += (tspr.getYrepeat() << 1); // Odd yspans
                    }
                    transform.translate(0, 0, zoffs);
                }

                if (yflip) {
                    transform.rotate(0, 1, 0, 180);
                    transform.translate(0, 0, (tspr.getYrepeat() * texy) * 4.0f);
                } else {
                    transform.translate(0, 0, (tspr.getYrepeat() * (texy - tsizy)) * 4.0f);
                }

                transform.scale((tspr.getXrepeat() * texx) / 5.0f, 0, 4 * tspr.getYrepeat() * texy);
                break;
            case 1: // Wall sprite
                if (yflip) {
                    yoff = -yoff;
                }
                int wang = (int) Gameutils.AngleToDegrees((tspr.getAng() + ((xflip ^ yflip) ? 1536 : 512)) & 0x7FF);
                if ((orientation & 64) == 0) {
                    int dang = (((tspr.getAng() - EngineUtils.getAngle(tspr.getX() - parent.globalposx, tspr.getY() - parent.globalposy)) & 0x7FF) - 1024);
                    if (dang > 512 || dang < -512) {
                        xflip = !xflip;
                    }
                }

                xspans = 4.0f;
                if (xflip ^ yflip) {
                    if (!xflip) {
                        xoff = -xoff;
                    }
                    if (yflip) {
                        xspans = -xspans;
                    }
                } else if (xflip) {
                    xoff = -xoff;
                    xspans = -xspans;
                }

                transform.translate(0, 0, (tspr.getYrepeat() * texy * (yflip ? 2.0f : -2.0f)));
                transform.rotate(0, 0, 1, wang);
                transform.translate((tspr.getXrepeat() * xoff) / 4.0f, 0, -(tspr.getYrepeat() * yoff) * 4.0f);
                if ((orientation & 128) != 0) {
                    transform.translate(0, 0, (tspr.getYrepeat() * tsizy) * 2.0f);
                }

                if ((tsizx & 1) == 0) {
                    transform.translate((tspr.getXrepeat() >> 1) / xspans, 0, 0);
                }

                if (yflip) {
                    transform.rotate(0, 1, 0, 180);
                    transform.translate(0, 0, (tspr.getYrepeat() * texy) * 4.0f);
                } else {
                    transform.translate(0, 0, (tspr.getYrepeat() * (texy - tsizy)) * 4.0f);
                }

                transform.scale((tspr.getXrepeat() * texx) / 4.0f, 0, (tspr.getYrepeat() * texy) * 4.0f);
                break;
            case 2: // Floor sprite
                if (yflip) {
                    yoff = -yoff;
                }

                if ((orientation & 64) == 0) {
                    if (tspr.getZ() < parent.globalposz) {
                        yflip = true;
                    } else if (yflip) {
                        yflip = !yflip;
                    }
                }

                xspans = 4.0f;
                if (xflip ^ yflip) {
                    if (yflip) {
                        xspans = -xspans;
                    }
                } else if (xflip) {
                    xspans = -xspans;
                }

                transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees((tspr.getAng() + (xflip ? 512 : 1536)) & 0x7FF));
                transform.rotate(1, 0, 0, xflip ? -90 : 90);

                if ((tsizx & 1) == 0) {
                    transform.translate((tspr.getXrepeat() >> 1) / xspans, 0, 0);
                }

                transform.translate((tspr.getXrepeat() * xoff) / 4.0f, 0, -(tspr.getYrepeat() * yoff) / 4.0f);
                transform.scale((tspr.getXrepeat() * texx) / 4.0f, 0, (tspr.getYrepeat() * texy) / 4.0f);
                break;
        }

        return transform.rotate(1, 0, 0, 90);
    }

    public boolean draw(Sprite tspr) {
        BoardService boardService = engine.getBoardService();
        if (tspr.getOwner() < 0 || !Gameutils.isValidTile(tspr.getPicnum()) || !boardService.isValidSector(tspr.getSectnum())) {
            return false;
        }

//		ShaderManager manager = parent.manager;
//
//		float xspans;
//		int picnum = tspr.picnum;
//		int shade = tspr.shade;
//		int pal = tspr.pal & 0xFF;
//		int orientation = tspr.cstat;
//		int spritenum = tspr.owner;
//		ArtEntry pic = parent.getTile(picnum);
//
//		int xoff = 0, yoff = 0;
//		if ((orientation & 48) != 48) {
//			if (pic.getType() != AnimType.None) {
//				picnum += engine.animateoffs(picnum, spritenum + 32768);
//				pic = parent.getTile(picnum);
//			}
//
//			xoff = tspr.xoffset;
//			yoff = tspr.yoffset;
//			xoff += pic.getOffsetX();
//			yoff += pic.getOffsetY();
//		}
//
//		if (!pic.isLoaded())
//			engine.loadtile(picnum);
//
//		int tsizx = pic.getWidth();
//		int tsizy = pic.getHeight();
//
//		if (tsizx <= 0 || tsizy <= 0)
//			return false;
//
//		int method = 1 + 4;
//		if ((orientation & 2) != 0) {
//			if ((orientation & 512) == 0)
//				method = 2 + 4;
//			else
//				method = 3 + 4;
//		}
//
//		GLTile tex = parent.bind(picnum, pal, shade, 0, method);
//		if (tex == null)
//			return false;
//
//		if ((method & 3) == 0) {
//			Gdx.gl.glDisable(GL_BLEND);
//		} else {
//			Gdx.gl.glEnable(GL_BLEND);
//		}
//
//		int vis = globalvisibility;
//		if (getSector()[tspr.sectnum].visibility != 0)
//			vis = mulscale(globalvisibility, (getSector()[tspr.sectnum].visibility + 16) & 0xFF, 4);
//
//		if (tex.getPixelFormat() == PixelFormat.Pal8)
//			((IndexedShader) manager.getProgram()).setVisibility((int) (-vis / 64.0f));
//
//		boolean xflip = (orientation & 4) != 0;
//		boolean yflip = (orientation & 8) != 0;
//
//		float posx = tspr.x / cam.xscale;
//		float posy = tspr.y / cam.xscale;
//		float posz = tspr.z / cam.yscale;
//		transform.setToTranslation(posx, posy, posz);
//
//		switch ((orientation >> 4) & 3) {
//		case 0: // Face sprite
//
//			xspans = 2560.0f;
//			int ang = ((int) globalang - 512) & 0x7FF;
//			if (xflip ^ yflip) {
//				ang += 1024;
//				if (!xflip)
//					xoff = -xoff;
//				if (yflip)
//					xspans = -xspans;
//			} else if (xflip) {
//				xoff = -xoff;
//				xspans = -xspans;
//			}
//
//			transform.translate(0, 0, (tspr.yrepeat * tex.getHeight() / (yflip ? 4096.0f : -4096.0f)));
//			transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees(ang));
//			transform.translate((tspr.xrepeat * xoff) / 2560.0f, 0, -((yoff * tspr.yrepeat) << 2) / cam.yscale);
//			if ((tsizx & 1) == 0)
//				transform.translate((tspr.xrepeat >> 1) / xspans, 0, 0);
//
//			if ((orientation & 128) != 0) {
//				float zoffs = ((tsizy * tspr.yrepeat) << 1);
//				if ((tsizy & 1) != 0)
//					zoffs += (tspr.yrepeat << 1); // Odd yspans
//				transform.translate(0, 0, zoffs / cam.yscale);
//			}
//
//			if (yflip) {
//				transform.rotate(0, 1, 0, 180);
//				transform.translate(0, 0, (tspr.yrepeat * tex.getHeight()) / 2048.0f);
//			} else
//				transform.translate(0, 0, (tspr.yrepeat * (tex.getHeight() - tsizy)) / 2048.0f);
//
//			transform.scale((tspr.xrepeat * tex.getWidth()) / 2560.0f, 0, (tspr.yrepeat * tex.getHeight()) / 2048.0f);
//			break;
//		case 1: // Wall sprite
//			if (yflip)
//				yoff = -yoff;
//			int wang = (int) Gameutils.AngleToDegrees((tspr.ang + ((xflip ^ yflip) ? 1536 : 512)) & 0x7FF);
//			if ((orientation & 64) == 0) {
//				int dang = (((tspr.ang - EngineUtils.getAngle(tspr.x - globalposx, tspr.y - globalposy)) & 0x7FF) - 1024);
//				if (dang > 512 || dang < -512) {
//					xflip = !xflip;
//				}
//			}
//
//			xspans = 2048.0f;
//			if (xflip ^ yflip) {
//				if (!xflip)
//					xoff = -xoff;
//				if (yflip)
//					xspans = -xspans;
//			} else if (xflip) {
//				xoff = -xoff;
//				xspans = -xspans;
//			}
//
//			transform.translate(0, 0, (tspr.yrepeat * tex.getHeight() / (yflip ? 4096.0f : -4096.0f)));
//			transform.rotate(0, 0, 1, wang);
//			transform.translate((tspr.xrepeat * xoff) / 2048.0f, 0, -(tspr.yrepeat * yoff) / 2048.0f);
//			if ((orientation & 128) != 0)
//				transform.translate(0, 0, (tspr.yrepeat * tsizy) / 4096.0f);
//			if ((tsizx & 1) == 0)
//				transform.translate((tspr.xrepeat >> 1) / xspans, 0, 0);
//
//			if (yflip) {
//				transform.rotate(0, 1, 0, 180);
//				transform.translate(0, 0, (tspr.yrepeat * tex.getHeight()) / 2048.0f);
//			} else
//				transform.translate(0, 0, (tspr.yrepeat * (tex.getHeight() - tsizy)) / 2048.0f);
//
//			transform.scale((tspr.xrepeat * tex.getWidth()) / 2048.0f, 0, (tspr.yrepeat * tex.getHeight()) / 2048.0f);
//			break;
//		case 2: // Floor sprite
//			if (yflip)
//				yoff = -yoff;
//
//			if ((orientation & 64) == 0) {
//				if (tspr.z < globalposz) {
//					yflip = true;
//				} else if (yflip)
//					yflip = !yflip;
//			}
//
//			xspans = 2048.0f;
//			if (xflip ^ yflip) {
//				if (yflip)
//					xspans = -xspans;
//			} else if (xflip)
//				xspans = -xspans;
//
//			transform.rotate(0, 0, 1, (int) Gameutils.AngleToDegrees((tspr.ang + (xflip ? 512 : 1536)) & 0x7FF));
//			transform.rotate(1, 0, 0, xflip ? -90 : 90);
//			// transform.translate(0, 0, (tspr.yrepeat * (2 * tex.getHeight() - tsizy)) /
//			// 4096.0f);
//			if ((tsizx & 1) == 0)
//				transform.translate((tspr.xrepeat >> 1) / xspans, 0, 0);
//
//			transform.translate((tspr.xrepeat * xoff) / 2048.0f, 0, -(tspr.yrepeat * yoff) / 2048.0f);
//			transform.scale((tspr.xrepeat * tex.getWidth()) / 2048.0f, 0, (tspr.yrepeat * tex.getHeight()) / 2048.0f);
//			break;
//		}
//		transform.rotate(1, 0, 0, 90);
//
//		if (xflip ^ yflip) {
//			xoff = -xoff;
//			Gdx.gl.glFrontFace(GL_CCW);
//		} else
//			Gdx.gl.glFrontFace(GL_CW);
//
//		Gdx.gl.glDepthFunc(GL20.GL_LESS);
//		Gdx.gl.glDepthRangef(0.0f, 0.99999f);
//
//		Matrix4 mat = this.getMatrix(tspr, tex.getWidth(), tex.getHeight());
//		float invscalex = 1.0f / cam.xscale;
//		float invscaley = 1.0f / cam.yscale;
//
//		mat.val[Matrix4.M00] *= invscalex;
//		mat.val[Matrix4.M01] *= invscalex;
//		mat.val[Matrix4.M02] *= invscalex;
//		mat.val[Matrix4.M03] *= invscalex;
//
//		mat.val[Matrix4.M10] *= invscalex;
//		mat.val[Matrix4.M11] *= invscalex;
//		mat.val[Matrix4.M12] *= invscalex;
//		mat.val[Matrix4.M13] *= invscalex;
//
//		mat.val[Matrix4.M20] *= invscaley;
//		mat.val[Matrix4.M21] *= invscaley;
//		mat.val[Matrix4.M22] *= invscaley;
//		mat.val[Matrix4.M23] *= invscaley;
//
//		manager.transform(mat);
//		manager.frustum(null);
//		parent.world.getQuad().render(manager.getProgram());
//
//		Gdx.gl.glFrontFace(GL_CW);
//		return true;

        ShaderManager manager = parent.manager;

        int picnum = tspr.getPicnum();
        int shade = tspr.getShade();
        int pal = tspr.getPal() & 0xFF;
        int orientation = tspr.getCstat();
        int spritenum = tspr.getOwner();
        ArtEntry pic = parent.getTile(picnum);
        if ((orientation & 48) != 48) {
            if (pic.getType() != AnimType.NONE) {
                picnum += parent.animateoffs(picnum, spritenum + 32768);
                pic = parent.getTile(picnum);
            }
        }

        int tsizx = pic.getWidth();
        int tsizy = pic.getHeight();

        if (tsizx <= 0 || tsizy <= 0) {
            return false;
        }

        int method = 1 | 4;
        if ((orientation & 2) != 0) {
            if ((orientation & 512) == 0) {
                method = 2 | 4;
            } else {
                method = 3 | 4;
            }
        }

        GLTile tex = parent.bind(pic, pal, shade, 0, method);
        if (tex == null) {
            return false;
        }

        if (tex.isHighTile()) {
            for (tsizy = 1; tsizy < tex.getHeight(); tsizy += tsizy) {
                ;
            }
            tsizy /= tex.getYScale();
        } else {
            tsizx = tex.getWidth();
            tsizy = tex.getHeight();
        }

        int vis = parent.globalvisibility;
        if (boardService.getSector(tspr.getSectnum()).getVisibility() != 0) {
            vis = mulscale(parent.globalvisibility, (boardService.getSector(tspr.getSectnum()).getVisibility() + 16) & 0xFF, 4);
        }

        if (tex.getPixelFormat() == PixelFormat.Pal8) {
            ((IndexedShader) manager.getProgram()).setVisibility((int) (-vis / 64.0f));
        } else {
            parent.calcFog(pal, shade, vis);
        }

        boolean xflip = (orientation & 4) != 0;
        boolean yflip = (orientation & 8) != 0;

        switch ((orientation >> 4) & 3) {
            case 1: // Wall sprite
                if ((orientation & 64) == 0) {
                    int dang = (((tspr.getAng() - EngineUtils.getAngle(tspr.getX() - parent.globalposx, tspr.getY() - parent.globalposy)) & 0x7FF) - 1024);
                    if (dang > 512 || dang < -512) {
                        xflip = !xflip;
                    }
                }
                break;
            case 2: // Floor sprite
                if ((orientation & 64) == 0) {
                    if (tspr.getZ() < parent.globalposz) {
                        yflip = true;
                    } else if (yflip) {
                        yflip = false;
                    }
                }
                break;
        }

        if ((orientation & 2) == 0) {
            Gdx.gl.glDisable(GL_BLEND);
        } else {
            Gdx.gl.glEnable(GL_BLEND);
        }

        if (xflip ^ yflip) {
            Gdx.gl.glFrontFace(GL_CCW);
        } else {
            Gdx.gl.glFrontFace(GL_CW);
        }

        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthRangef(0.0f, 0.99999f);

        Matrix4 mat = getMatrix(tspr, tsizx, tsizy);
        float invscalex = 1.0f / cam.xscale;
        float invscaley = 1.0f / cam.yscale;

        mat.val[Matrix4.M00] *= invscalex;
        mat.val[Matrix4.M01] *= invscalex;
        mat.val[Matrix4.M02] *= invscalex;
        mat.val[Matrix4.M03] *= invscalex;

        mat.val[Matrix4.M10] *= invscalex;
        mat.val[Matrix4.M11] *= invscalex;
        mat.val[Matrix4.M12] *= invscalex;
        mat.val[Matrix4.M13] *= invscalex;

        mat.val[Matrix4.M20] *= invscaley;
        mat.val[Matrix4.M21] *= invscaley;
        mat.val[Matrix4.M22] *= invscaley;
        mat.val[Matrix4.M23] *= invscaley;

        manager.transform(mat);
        manager.frustum(null);
        parent.world.getQuad().render(manager.getProgram());

        Gdx.gl.glFrontFace(GL_CW);
        return true;
    }

    public void end() {
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glDepthRangef(parent.defznear, parent.defzfar);
    }

    public class SpriteComparator implements Comparator<Sprite> {
        @Override
        public int compare(Sprite o1, Sprite o2) {
            if (o1 == null || o2 == null) {
                return 0;
            }

            if (o1.getOwner() == o2.getOwner() || o1.getOwner() == -1 || o2.getOwner() == -1) {
                return 0;
            }

            int len1 = getDist(o1);
            int len2 = getDist(o2);
            if (len1 != len2) {
                return len1 < len2 ? -1 : 1;
            }

            if ((o1.getCstat() & 2) != 0) {
                return -1;
            }

            if ((o2.getCstat() & 2) != 0) {
                return 1;
            }

            if (o1.getStatnum() != o2.getStatnum()) {
                return o1.getStatnum() < o2.getStatnum() ? -1 : 1;
            }

            return (o1.getOwner() <= o2.getOwner()) ? -1 : 1;
        }

        public int getDist(Sprite spr) {
            int dx1 = spr.getX() - parent.globalposx;
            int dy1 = spr.getY() - parent.globalposy;
            int dz1 = (spritesz.items[spr.getOwner()] - parent.globalposz) >> 4;

            return dx1 * dx1 + dy1 * dy1 + dz1 * dz1;
        }
    }

}
