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

import static com.badlogic.gdx.graphics.GL20.GL_BLEND;
import static com.badlogic.gdx.graphics.GL20.GL_CCW;
import static com.badlogic.gdx.graphics.GL20.GL_CULL_FACE;
import static com.badlogic.gdx.graphics.GL20.GL_CW;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Pragmas.mulscale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;

import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager;
import ru.m210projects.Build.Render.GdxRender.Shaders.ShaderManager.Shader;
import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.ModelInfo.Type;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDModel;
import ru.m210projects.Build.Render.ModelHandle.Voxel.GLVoxel;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Render.Types.Tile2model;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.filehandle.art.ArtEntry;

public class GDXModelRenderer {

	private final Matrix4 transform;
	private final GDXRenderer parent;
	private final Engine engine;
	private final BoardService boardService;

	public GDXModelRenderer(GDXRenderer parent) {
		this.transform = new Matrix4();
		this.parent = parent;
		this.engine = parent.getEngine();
		this.boardService = engine.getBoardService();
	}

	public boolean mddraw(GLModel m, Sprite tspr) {
		if (m == null) {
            return false;
        }

		boolean isFloorAligned = (tspr.getCstat() & 48) == 32;
		if (m.getType() == Type.Voxel && isFloorAligned) {
            return false;
        }

		Shader shader = Shader.RGBWorldShader;
		if (m.getType() == Type.Voxel && parent.getTexFormat() == PixelFormat.Pal8) {
            shader = Shader.IndexedWorldShader;
        }
		parent.switchShader(shader);

		DefScript defs = parent.defs;
		ShaderManager manager = parent.manager;

		Matrix4 transport = prepareTransform(m, tspr);
		if (m.getType() == Type.Voxel) {
			GLVoxel vox = (GLVoxel) m;
			transform.translate(-vox.xpiv / 64.0f, -vox.ypiv / 64.0f, -vox.zpiv / 64.0f);
			if (isFloorAligned) {
				transform.rotate(-1.0f, 0.0f, 0.0f, 90);
				transform.translate(0, -vox.ypiv / 64.0f, -vox.zpiv / 64.0f);
			}
		} else if (m instanceof MDModel) {
			((MDModel) m).updateanimation(defs.mdInfo, tspr, engine.getTimer().getTicsPerSecond(), parent.getConfig().isAnimSmoothing());
			if (m.getType() == Type.Md3) {
                transform.scale(1 / 64.0f, 1 / 64.0f, -1 / 64.0f);
            }
		}

		manager.transform(transport);
		manager.frustum(null);

		int shade = tspr.getShade();
		int pal = tspr.getPal() & 0xFF;

		float r, g, b, alpha = 1.0f;
		int numshades = engine.getPaletteManager().getShadeCount();
		r = g = b = (numshades - min(max((shade), 0), numshades)) / (float) numshades;

		if (parent.defs != null) {
			Color p = parent.defs.texInfo.getTints(pal);
			r *= p.r / 255.0f;
			g *= p.g / 255.0f;
			b *= p.b / 255.0f;
		}

		if ((tspr.getCstat() & 2) != 0) {
			if ((tspr.getCstat() & 512) == 0) {
                alpha = parent.TRANSLUSCENT1;
            } else {
                alpha = parent.TRANSLUSCENT2;
            }
		}

		manager.color(r, g, b, alpha);
		manager.textureTransform(parent.texture_transform.idt(), 0);
		int vis = getVisibility(tspr);
		if (m.getType() != Type.Voxel) {
            parent.calcFog(pal, shade, vis);
        }

		Gdx.gl.glEnable(GL_BLEND);
		Tile2model t2m = defs.mdInfo.getParams(tspr.getPicnum());
		m.render(pal, shade, t2m != null ? t2m.skinnum : 0, vis, alpha);

		Gdx.gl.glFrontFace(GL_CW);

		return true;
	}

	private int getVisibility(Sprite tspr) {
		int vis = parent.globalvisibility;
		if (boardService.getSector(tspr.getSectnum()).getVisibility() != 0) {
            vis = mulscale(parent.globalvisibility, (boardService.getSector(tspr.getSectnum()).getVisibility() + 16) & 0xFF, 4);
        }
		return vis;
	}

	private Matrix4 prepareTransform(GLModel m, Sprite tspr) {
		BuildCamera cam = parent.cam;

		ArtEntry pic = parent.getTile(tspr.getPicnum());
		int orientation = tspr.getCstat();

		boolean xflip = (orientation & 4) != 0;
		boolean yflip = (orientation & 8) != 0;
		boolean isWallAligned = (orientation & 48) == 16;
		boolean isFloorAligned = (orientation & 48) == 32;

		float xoff = tspr.getXoffset();
		float yoff = yflip ? -tspr.getYoffset() : tspr.getYoffset();
		float posx = tspr.getX();
		float posy = tspr.getY();
		float posz = tspr.getZ();

		if (m.getType() == Type.Voxel) {
			GLVoxel vox = (GLVoxel) m;
			if ((orientation & 128) == 0) {
                posz -= ((vox.zsiz * tspr.getYrepeat()) << 1);
            }
			if (yflip && (orientation & 16) == 0) {
                posz += ((pic.getHeight() * 0.5f) - vox.zpiv) * tspr.getYrepeat() * 8.0f;
            }
		} else {
			if ((orientation & 128) != 0 && !isFloorAligned) {
                posz += (pic.getHeight() * tspr.getYrepeat()) << 1;
            }
			if (yflip) {
                posz -= (pic.getHeight() * tspr.getYrepeat()) << 2;
            }
		}

		float f = (tspr.getXrepeat() / 32.0f) * m.getScale();
		float g = (tspr.getYrepeat() / 32.0f) * m.getScale();
		if (!isWallAligned && !isFloorAligned) {
            f *= 0.8f;
        }

		transform.setToTranslation(posx / cam.xscale, posy / cam.xscale, posz / cam.yscale);
		float yoffset = 0;
		if (m instanceof MDModel) {
			MDModel md = (MDModel) m;
			transform.translate(0, 0, -md.getYOffset(false) * g);
			yoffset = md.getYOffset(true);
		}
		transform.scale(f, f, yflip ? -g : g);

		Spriteext sprext = parent.defs.mapInfo.getSpriteInfo(tspr.getOwner());
		float ang = tspr.getAng() + (sprext != null ? sprext.angoff : 0);
		transform.rotate(0, 0, 1, (float) Gameutils.AngleToDegrees(ang));
		transform.scale(isFloorAligned ? -1.0f : 1.0f, xflip ^ isFloorAligned ? -1.0f : 1.0f, 1.0f);
		if (m.getType() == Type.Voxel) {
            transform.rotate(0, 0, -1, 90.0f);
        }
		transform.translate(-xoff / 64.0f, 0, (-yoff / 64.0f) - yoffset);
		if (m.isRotating()) {
            transform.rotate(0, 0, -1, engine.getTotalClock() % 360);
        }
		if (m instanceof MDModel) {
			transform.scale(0.01f, 0.01f, 0.01f);
			transform.rotate(1, 0, 0, -90.0f);
		}

		Gdx.gl.glEnable(GL_CULL_FACE);
		if (yflip ^ xflip) {
            Gdx.gl.glFrontFace(m.getType() != Type.Md2 ? GL_CCW : GL_CW);
        } else {
            Gdx.gl.glFrontFace(m.getType() != Type.Md2 ? GL_CW : GL_CCW);
        }
		return transform;
	}

	// ---------------------------------------------------------------

//	private boolean voxdraw(GLVoxel m, SPRITE tspr) {
//		boolean isFloorAligned = (tspr.cstat & 48) == 32;
//		if (isFloorAligned)
//			return false;
//
//		parent.switchShader(
//				parent.getTexFormat() != PixelFormat.Pal8 ? Shader.RGBWorldShader : Shader.IndexedWorldShader);
//
//		ShaderManager manager = parent.manager;
//		Matrix4 transport = prepare(m, tspr);
//		transform.translate(-m.xpiv / 64.0f, -m.ypiv / 64.0f, -m.zpiv / 64.0f);
//		if (isFloorAligned) {
//			transform.rotate(-1.0f, 0.0f, 0.0f, 90);
//			transform.translate(0, -m.ypiv / 64.0f, -m.zpiv / 64.0f);
//		}
//		manager.transform(transport);
//		manager.frustum(null);
//
//		float alpha = 1.0f;
//		if ((tspr.cstat & 2) != 0) {
//			if ((tspr.cstat & 512) == 0)
//				alpha = TRANSLUSCENT1;
//			else
//				alpha = TRANSLUSCENT2;
//		}
//
//		int vis = getVisibility(tspr);
//		m.render(tspr.pal & 0xFF, tspr.shade, 0, vis, alpha);
//
//		Gdx.gl.glFrontFace(GL_CW);
//		return true;
//	}
//
//	private boolean mddraw(MDModel m, SPRITE tspr) {
//		DefScript defs = parent.defs;
//		m.updateanimation(defs.mdInfo, tspr);
//
//		parent.switchShader(Shader.RGBWorldShader);
//
//		ShaderManager manager = parent.manager;
//		manager.transform(prepareTransform(m, tspr));
//		manager.frustum(null);
//
//		float alpha = 1.0f;
//		if ((tspr.cstat & 2) != 0) {
//			if ((tspr.cstat & 512) == 0)
//				alpha = TRANSLUSCENT1;
//			else
//				alpha = TRANSLUSCENT2;
//		}
//		manager.color(1.0f, 1.0f, 1.0f, alpha);
//
//		int vis = getVisibility(tspr);
//		m.render(tspr.pal & 0xFF, tspr.shade, defs.mdInfo.getParams(tspr.picnum).skinnum, vis, alpha);
//
//		Gdx.gl.glFrontFace(GL_CW);
//		return true;
//	}
//
//	private void modelPrepare(MDModel m, SPRITE tspr) {
//		ShaderManager manager = parent.manager;
//		BuildCamera cam = parent.cam;
//
//		ArtEntry pic = engine.getTile(tspr.picnum);
//		int orientation = tspr.cstat;
//
//		boolean xflip = (orientation & 4) != 0;
//		boolean yflip = (orientation & 8) != 0;
//		boolean isWallAligned = (orientation & 48) == 16;
//		boolean isFloorAligned = (orientation & 48) == 32;
//
//		float xoff = tspr.xoffset;
//		float yoff = yflip ? -tspr.yoffset : tspr.yoffset;
//		float posx = tspr.x;
//		float posy = tspr.y;
//		float posz = tspr.z;
//
//		if ((orientation & 128) != 0 && !isFloorAligned)
//			posz += (pic.getHeight() * tspr.yrepeat) << 1;
//		if (yflip)
//			posz -= (pic.getHeight() * tspr.yrepeat) << 2;
//
//		float f = (tspr.xrepeat / 32.0f) * m.getScale();
//		float g = (tspr.yrepeat / 32.0f) * m.getScale();
//		if (!isWallAligned && !isFloorAligned)
//			f /= 1.25f;
//
//		transform.setToTranslation(posx / cam.xscale, posy / cam.xscale, posz / cam.yscale);
//		transform.translate(0, 0, -m.getYOffset(false) * g); // MD only
//		transform.scale(f, f, yflip ? -g : g);
//		transform.rotate(0, 0, 1, (float) Gameutils.AngleToDegrees(tspr.ang));
//		transform.scale(isFloorAligned ? -1.0f : 1.0f, xflip ^ isFloorAligned ? -1.0f : 1.0f, 1.0f);
//		transform.translate(xoff / 64.0f, 0, (-yoff / 64.0f) - m.getYOffset(true));
//		if (m.isRotating())
//			transform.rotate(0, 0, -1, totalclock % 360);
//		transform.scale(0.01f, 0.01f, 0.01f);
//		transform.rotate(1, 0, 0, -90.0f);
//
//		Gdx.gl.glEnable(GL_CULL_FACE);
//		if (yflip ^ xflip)
//			Gdx.gl.glFrontFace(GL_CW);
//		else
//			Gdx.gl.glFrontFace(GL_CCW);
//
//		parent.switchShader(
//				parent.getTexFormat() != PixelFormat.Pal8 ? Shader.RGBWorldShader : Shader.IndexedWorldShader);
//		manager.transform(transform);
//		manager.frustum(null);
//
//		float alpha = 1.0f;
//		if ((tspr.cstat & 2) != 0) {
//			if ((tspr.cstat & 512) == 0)
//				alpha = TRANSLUSCENT1;
//			else
//				alpha = TRANSLUSCENT2;
//		}
//
//		manager.color(1.0f, 1.0f, 1.0f, alpha);
//	}
//
//	public int voxdraw_old(GLVoxel m, SPRITE tspr) {
//		if (m == null)
//			return 0;
//
//		if ((boardService.getSprite()[tspr.owner].cstat & 48) == 32)
//			return 0;
//
//		ShaderManager manager = parent.manager;
//		BuildCamera cam = parent.cam;
//
//		ArtEntry pic = engine.getTile(tspr.picnum);
//		int orientation = tspr.cstat;
//
//		boolean xflip = (orientation & 4) != 0;
//		boolean yflip = (orientation & 8) != 0;
//		boolean isWallAligned = (orientation & 48) == 16;
//		boolean isFloorAligned = (orientation & 48) == 32;
//		tspr.xoffset = -64;
//
//		float xoff = tspr.xoffset;
//		float yoff = yflip ? -tspr.yoffset : tspr.yoffset;
//		float posx = tspr.x;
//		float posy = tspr.y;
//		float posz = tspr.z;
//
//		if ((orientation & 128) == 0)
//			posz -= ((m.zsiz * tspr.yrepeat) << 1);
//		if (yflip && (orientation & 16) == 0)
//			posz += ((pic.getHeight() * 0.5f) - m.zpiv) * tspr.yrepeat * 8.0f;
//
//		float f = (tspr.xrepeat / 32.0f) * m.getScale();
//		float g = (tspr.yrepeat / 32.0f) * m.getScale();
//		if (!isWallAligned && !isFloorAligned)
//			f /= 1.25f;
//
//		transform.setToTranslation(posx / cam.xscale, posy / cam.xscale, posz / cam.yscale);
//		transform.scale(f, f, yflip ? -g : g);
//		transform.rotate(0, 0, 1, (float) Gameutils.AngleToDegrees(tspr.ang));
//		transform.scale(isFloorAligned ? -1.0f : 1.0f, xflip ^ isFloorAligned ? -1.0f : 1.0f, 1.0f);
//		transform.rotate(0, 0, -1, 90.0f); // Voxels only
//		transform.translate(-xoff / 64.0f, 0, -yoff / 64.0f);
//		if (m.isRotating())
//			transform.rotate(0, 0, -1, totalclock % 360);
//		transform.translate(-m.xpiv / 64.0f, -m.ypiv / 64.0f, -m.zpiv / 64.0f); // Voxels only
//		if (isFloorAligned) { // Voxels only
//			transform.rotate(-1.0f, 0.0f, 0.0f, 90);
//			transform.translate(0, -m.ypiv / 64.0f, -m.zpiv / 64.0f);
//		}
//
//		Gdx.gl.glEnable(GL_CULL_FACE);
//		if (yflip ^ xflip)
//			Gdx.gl.glFrontFace(GL_CCW);
//		else
//			Gdx.gl.glFrontFace(GL_CW);
//
//		int vis = globalvisibility;
//		if (boardService.getSector()[tspr.sectnum].visibility != 0)
//			vis = mulscale(globalvisibility, (boardService.getSector()[tspr.sectnum].visibility + 16) & 0xFF, 4);
//
//		parent.switchShader(
//				parent.getTexFormat() != PixelFormat.Pal8 ? Shader.RGBWorldShader : Shader.IndexedWorldShader);
//		manager.transform(transform);
//		manager.frustum(null);
//
//		float alpha = 1.0f;
//		if ((tspr.cstat & 2) != 0) {
//			if ((tspr.cstat & 512) == 0)
//				alpha = TRANSLUSCENT1;
//			else
//				alpha = TRANSLUSCENT2;
//		}
//
//		m.render(tspr.pal & 0xFF, tspr.shade, 0, vis, alpha);
//
//		Gdx.gl.glFrontFace(GL_CW);
//		return 1;
//	}
}
