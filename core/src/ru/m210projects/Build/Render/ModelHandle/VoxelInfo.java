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

import ru.m210projects.Build.Render.ModelHandle.Voxel.VoxelData;

public class VoxelInfo extends ModelInfo {

	protected VoxelData vox;
	public VoxelInfo(VoxelData vox) {
		super(null, Type.Voxel);
		this.vox = vox;
		this.scale = 65536.0f;
	}

	public VoxelData getData() {
		return vox;
	}

}
