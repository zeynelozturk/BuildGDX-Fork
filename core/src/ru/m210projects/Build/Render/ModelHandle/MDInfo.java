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

package ru.m210projects.Build.Render.ModelHandle;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;

import ru.m210projects.Build.Render.ModelHandle.MDModel.MDAnimation;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDSkinmap;
import ru.m210projects.Build.filehandle.Entry;

public class MDInfo extends ModelInfo {

	protected MDSkinmap skinmap;
	protected MDAnimation animations;
	protected String[] frames;
	protected int numframes;

	public MDInfo(Entry file, Type type) {
		super(file, type);
	}

	public int getFrames() {
		return numframes;
	}

	public MDSkinmap getSkins() {
		return skinmap;
	}

	public MDAnimation getAnimations() {
		return animations;
	}

	public int getFrameIndex(String framename) {
		for (int i = 0; i < numframes; i++) {
			String fr = frames[i];
			if (fr != null && fr.equalsIgnoreCase(framename)) {
				return i;
			}
		}

		return (-3); // frame name invalid
	}

	protected MDSkinmap getSkin(int palnum, int skinnum, int surfnum) {
		for (MDSkinmap sk = skinmap; sk != null; sk = sk.next) {
			if (sk.palette == palnum && skinnum == sk.skinnum && surfnum == sk.surfnum) {
                return sk;
            }
		}

		return null;
	}

	protected void addSkin(MDSkinmap sk) {
		sk.next = skinmap;
		skinmap = sk;
	}

	public int setSkin(String skinfn, int palnum, int skinnum, int surfnum, double param, double specpower,
			double specfactor) {
		if (skinfn == null) {
            return -2;
        }
		if (palnum >= MAXPALOOKUPS) {
            return -3;
        }

		if (type == Type.Md2) {
            surfnum = 0;
        }

		MDSkinmap sk = getSkin(palnum, skinnum, surfnum);
		if (sk == null) // no replacement yet defined
		{
			addSkin(sk = new MDSkinmap());
		}

		sk.palette = palnum;
		sk.skinnum = skinnum;
		sk.surfnum = surfnum;
		sk.param = (float) param;
		sk.specpower = (float) specpower;
		sk.specfactor = (float) specfactor;
		sk.fn = skinfn;

		return 0;
	}

	public int setAnimation(String framestart, String frameend, int fpssc, int flags) {
		MDAnimation ma = new MDAnimation();
		int i;

		// find index of start frame
		i = getFrameIndex(framestart);
		if (i == numframes) {
            return -2;
        }
		ma.startframe = i;

		// find index of finish frame which must trail start frame
		i = getFrameIndex(frameend);
		if (i == numframes) {
            return -3;
        }
		ma.endframe = i;

		ma.fpssc = fpssc;
		ma.flags = flags;

		ma.next = animations;
		animations = ma;

		return 0;
	}
}
