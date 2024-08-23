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

package ru.m210projects.Build.Render.Software;

public interface A {
	
	void setframeplace(byte[] newframeplace);
	
	byte[] getframeplace();
	
	void clearframe(byte col);

	// Global variable functions
    void setvlinebpl(int dabpl);

	void fixtransluscence(byte[] datrans);

	void settransnormal();

	void settransreverse();
	
	void drawpixel(int ptr, byte col);

	// Ceiling/floor horizontal line functions

	void sethlinesizes(int logx, int logy, byte[] bufplc);

	void setpalookupaddress(byte[] paladdr);

	void setuphlineasm4(int bxinc, int byinc);

	void hlineasm4(int cnt, int skiploadincs, int paloffs, int by, int bx, int p);

	// Sloped ceiling/floor vertical line functions

	void setupslopevlin(int logylogx, byte[] bufplc, int pinc, int bzinc);

	void slopevlin(int p, byte[] pal, int slopaloffs, int cnt, int bx, int by, int x3, int y3, int[] slopalookup, int bz);

	// Wall,face sprite/wall sprite vertical line functions

	void setupvlineasm(int neglogy);

	void vlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p);

	void setupmvlineasm(int neglogy);

	void mvlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p);

	void setuptvlineasm(int neglogy);

	void tvlineasm1(int vinc, byte[] pal, int shade, int cnt, long vplc, byte[] bufplc, int bufoffs, int p);

	// Floor sprite horizontal line functions

	void sethlineincs(int x, int y);

	void setuphline(byte[] pal, int shade);

	void msethlineshift(int logx, int logy);

	void mhline(byte[] bufplc, long bx, int cntup16, int junk, long by, int p);

	void tsethlineshift(int logx, int logy);

	void thline(byte[] bufplc, long bx, int cntup16, int junk, long by, int p);

	// Rotatesprite vertical line functions

	void setupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz);

	void spritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p);

	void msetupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz);

	void mspritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p);

	void tsetupspritevline(byte[] pal, int shade, int bxinc, int byinc, int ysiz);

	void tspritevline(int bx, int by, int cnt, byte[] bufplc, int bufoffs, int p);

	// Voxel functions
	
	void setupdrawslab(int dabpl, byte[] pal, int shade, int trans);

	void drawslab(int dx, int v, int dy, int vi, byte[] data, int vptr, int p);

	int krecipasm(int i);
}
