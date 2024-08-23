/*
 *  Sprite structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import ru.m210projects.Build.filehandle.StreamUtils;

import static ru.m210projects.Build.Engine.MAXSTATUS;

public class Sprite implements Serializable<Sprite> {
	private int x;
	private int y;
	private int z; //12
	private short cstat = 0;
	private short picnum; //4
	private byte shade; //1
	private short pal;
	private short detail; //3
	private int clipdist = 32;
	private short xrepeat = 32;
	private short yrepeat = 32; //2
	private short xoffset;
	private short yoffset; //2
	private short sectnum = -1;
	private short statnum = MAXSTATUS; //4
	private short ang;
	private short owner = -1;
	private short xvel;
	private short yvel;
	private short zvel; //10
	private short lotag;
	private short hitag;
	private short extra = -1;

	@Override
	public Sprite readObject(InputStream is) throws IOException {
		setX(StreamUtils.readInt(is));
    	setY(StreamUtils.readInt(is));
    	setZ(StreamUtils.readInt(is));
    	setCstat(StreamUtils.readShort(is));
    	setPicnum(StreamUtils.readShort(is));
    	setShade(StreamUtils.readByte(is));
    	setPal(StreamUtils.readUnsignedByte(is));
    	setClipdist(StreamUtils.readUnsignedByte(is));
    	setDetail(StreamUtils.readByte(is));
    	setXrepeat(StreamUtils.readUnsignedByte(is));
    	setYrepeat(StreamUtils.readUnsignedByte(is));
    	setXoffset(StreamUtils.readByte(is));
    	setYoffset(StreamUtils.readByte(is));
    	setSectnum(StreamUtils.readShort(is));
    	setStatnum(StreamUtils.readShort(is));
    	setAng(StreamUtils.readShort(is));
    	setOwner(StreamUtils.readShort(is));
    	setXvel(StreamUtils.readShort(is));
    	setYvel(StreamUtils.readShort(is));
    	setZvel(StreamUtils.readShort(is));
    	setLotag(StreamUtils.readShort(is));
    	setHitag(StreamUtils.readShort(is));
    	setExtra(StreamUtils.readShort(is));
		return this;
	}

	@Override
	public Sprite writeObject(OutputStream os) throws IOException {
		StreamUtils.writeInt(os, getX());
		StreamUtils.writeInt(os, getY());
		StreamUtils.writeInt(os, getZ());
		StreamUtils.writeShort(os, getCstat());
		StreamUtils.writeShort(os, getPicnum());
		os.write(getShade());
		os.write(getPal());
		os.write(getClipdist());
		os.write(getDetail());
		os.write(getXrepeat());
		os.write(getYrepeat());
		os.write(getXoffset());
		os.write(getYoffset());
		StreamUtils.writeShort(os, getSectnum());
		StreamUtils.writeShort(os, getStatnum());
		StreamUtils.writeShort(os, getAng());
		StreamUtils.writeShort(os, getOwner());
		StreamUtils.writeShort(os, getXvel());
		StreamUtils.writeShort(os, getYvel());
		StreamUtils.writeShort(os, getZvel());
		StreamUtils.writeShort(os, getLotag());
		StreamUtils.writeShort(os, getHitag());
		StreamUtils.writeShort(os, getExtra());

		return this;
	}

	@Override
	public String toString()
	{
		String out = "x " + getX() + " \r\n";
		out += "y " + getY() + " \r\n";
		out += "z " + getZ() + " \r\n";
		out += "cstat " + getCstat() + " \r\n";
		out += "picnum " + getPicnum() + " \r\n";
		out += "shade " + getShade() + " \r\n";
		out += "pal " + getPal() + " \r\n";
		out += "clipdist " + getClipdist() + " \r\n";
		out += "detail " + getDetail() + " \r\n";
		out += "xrepeat " + getXrepeat() + " \r\n";
		out += "yrepeat " + getYrepeat() + " \r\n";
		out += "xoffset " + getXoffset() + " \r\n";
		out += "yoffset " + getYoffset() + " \r\n";
		out += "sectnum " + getSectnum() + " \r\n";
		out += "statnum " + getStatnum() + " \r\n";
		out += "ang " + getAng() + " \r\n";
		out += "owner " + getOwner() + " \r\n";
		out += "xvel " + getXvel() + " \r\n";
		out += "yvel " + getYvel() + " \r\n";
		out += "zvel " + getZvel() + " \r\n";
		out += "lotag " + getLotag() + " \r\n";
		out += "hitag " + getHitag() + " \r\n";
		out += "extra " + getExtra() + " \r\n";

		return out;
	}

	public void reset()
	{
		reset((byte)0);
		this.setClipdist(32);
		this.setXrepeat(32);
		this.setYrepeat(32);
		this.setOwner(-1);
		this.setExtra(-1);
	}

	public void reset(byte var) {
		this.setX(var);
		this.setY(var);
		this.setZ(var);
		this.setCstat(var);
		this.setPicnum(var);
		this.setShade(var);
		this.setPal(var);

		this.setClipdist(var);
		this.setDetail(var);
		this.setXrepeat(var);
		this.setYrepeat(var);
		this.setXoffset(var);
		this.setYoffset(var);
		this.setSectnum(var);
		this.setStatnum(var);
		this.setAng(var);
		this.setOwner(var);
		this.setXvel(var);
		this.setYvel(var);
		this.setZvel(var);
		this.setLotag(var);
		this.setHitag(var);
		this.setExtra(var);
	}

	public void set(Sprite src) {
		this.setX(src.getX());
		this.setY(src.getY());
		this.setZ(src.getZ());
		this.setCstat(src.getCstat());
		this.setPicnum(src.getPicnum());
		this.setShade(src.getShade());
		this.setPal(src.getPal());

		this.setClipdist(src.getClipdist());
		this.setDetail(src.getDetail());
		this.setXrepeat(src.getXrepeat());
		this.setYrepeat(src.getYrepeat());
		this.setXoffset(src.getXoffset());
		this.setYoffset(src.getYoffset());
		this.setSectnum(src.getSectnum());
		this.setStatnum(src.getStatnum());
		this.setAng(src.getAng());
		this.setOwner(src.getOwner());
		this.setXvel(src.getXvel());
		this.setYvel(src.getYvel());
		this.setZvel(src.getZvel());
		this.setLotag(src.getLotag());
		this.setHitag(src.getHitag());
		this.setExtra(src.getExtra());
	}

	public short getSectnum() {
		return sectnum;
	}

	public void setSectnum(int sectnum) {
		this.sectnum = (short) sectnum;
	}

	public short getStatnum() {
		return statnum;
	}

	public void setStatnum(int statnum) {
		this.statnum = (short) statnum;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Sprite)) {
			return false;
		}
		Sprite sprite = (Sprite) o;

		boolean[] b = new boolean[23];
		b[0] = getX() == sprite.getX();
		b[1] = getY() == sprite.getY();
		b[2] = getZ() == sprite.getZ();
		b[3] = getCstat() == sprite.getCstat();
		b[4] = getPicnum() == sprite.getPicnum();
		b[5] = getShade() == sprite.getShade();
		b[6] = getPal() == sprite.getPal();
		b[7] = getDetail() == sprite.getDetail();
		b[8] = getClipdist() == sprite.getClipdist();
		b[9] = getXrepeat() == sprite.getXrepeat();
		b[10] = getYrepeat() == sprite.getYrepeat();
		b[11] = getXoffset() == sprite.getXoffset();
		b[12] = getYoffset() == sprite.getYoffset();
		b[13] = getSectnum() == sprite.getSectnum();
		b[14] = getStatnum() == sprite.getStatnum();
		b[15] = getAng() == sprite.getAng();
		b[16] = getOwner() == sprite.getOwner();
		b[17] = getXvel() == sprite.getXvel();
		b[18] = getYvel() == sprite.getYvel();
		b[19] = getZvel() == sprite.getZvel();
		b[20] = getLotag() == sprite.getLotag();
		b[21] = getHitag() == sprite.getHitag();
		b[22] = getExtra() == sprite.getExtra();

		boolean result = true;
		for (int i = 0; i < 23; i++) {
			if (!b[i]) {
				System.err.print("Unsync in ");
				switch (i) {
					case 0: System.err.println("x: " + getX() + " != " + sprite.getX()); break;
					case 1: System.err.println("y: " + getY() + " != " + sprite.getY()); break;
					case 2: System.err.println("z: " + getZ() + " != " + sprite.getZ()); break;
					case 3: System.err.println("cstat: " + getCstat() + " != " + sprite.getCstat()); break;
					case 4: System.err.println("picnum: " + getPicnum() + " != " + sprite.getPicnum()); break;
					case 5: System.err.println("shade: " + getShade() + " != " + sprite.getShade()); break;
					case 6: System.err.println("pal: " + getPal() + " != " + sprite.getPal()); break;
					case 7: System.err.println("detail: " + getDetail() + " != " + sprite.getDetail()); break;
					case 8: System.err.println("clipdist: " + getClipdist() + " != " + sprite.getClipdist()); break;
					case 9: System.err.println("xrepeat: " + getXrepeat() + " != " + sprite.getXrepeat()); break;
					case 10: System.err.println("yrepeat: " + getYrepeat() + " != " + sprite.getYrepeat()); break;
					case 11: System.err.println("xoffset: " + getXoffset() + " != " + sprite.getXoffset()); break;
					case 12: System.err.println("yoffset: " + getYoffset() + " != " + sprite.getYoffset()); break;
					case 13: System.err.println("sectnum: " + getSectnum() + " != " + sprite.getSectnum()); break;
					case 14: System.err.println("statnum: " + getStatnum() + " != " + sprite.getStatnum()); break;
					case 15: System.err.println("ang: " + getAng() + " != " + sprite.getAng()); break;
					case 16: System.err.println("owner: " + getOwner() + " != " + sprite.getOwner()); break;
					case 17: System.err.println("xvel: " + getXvel() + " != " + sprite.getXvel()); break;
					case 18: System.err.println("yvel: " + getYvel() + " != " + sprite.getYvel()); break;
					case 19: System.err.println("zvel: " + getZvel() + " != " + sprite.getZvel()); break;
					case 20: System.err.println("lotag: " + getLotag() + " != " + sprite.getLotag()); break;
					case 21: System.err.println("hitag: " + getHitag() + " != " + sprite.getHitag()); break;
					case 22: System.err.println("extra: " + getExtra() + " != " + sprite.getExtra()); break;
				}
				result = false;
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getX(), getY(), getZ(), getCstat(), getPicnum(), getShade(), getPal(), getDetail(), getClipdist(), getXrepeat(), getYrepeat(), getXoffset(), getYoffset(), getSectnum(), getStatnum(), getAng(), getOwner(), getXvel(), getYvel(), getZvel(), getLotag(), getHitag(), getExtra());
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

//	cstat, bit 0: 1 = Blocking sprite (use with clipmove, getzrange) "B"
//	       bit 1: 1 = 50/50 transluscence, 0 = normal                   "T"
//	       bit 2: 1 = x-flipped, 0 = normal                             "F"
//	       bit 3: 1 = y-flipped, 0 = normal                             "F"
//	       bits 5-4: 00 = FACE sprite (default)                         "R"
//	                 01 = WALL sprite (like masked walls)
//	                 10 = FLOOR sprite (parallel to ceilings&floors)
//	                 11 = SPIN sprite (face sprite that can spin 2draw style - not done yet)
//	       bit 6: 1 = 1-sided sprite, 0 = normal                        "1"
//	       bit 7: 1 = Real centered centering, 0 = foot center          "C"
//	       bit 8: 1 = Blocking sprite (use with hitscan)                "H"
//	       bit 9: reserved
//	       bit 10: reserved
//	       bit 11: reserved
//	       bit 12: reserved
//	       bit 13: reserved
//	       bit 14: reserved
//	       bit 15: 1 = Invisible sprite, 0 = not invisible

	public short getCstat() {
		return cstat;
	}

	public void setCstat(int cstat) {
		this.cstat = (short) cstat;
	}

	public short getPicnum() {
		return picnum;
	}

	public void setPicnum(int picnum) {
		this.picnum = (short) picnum;
	}

	public byte getShade() {
		return shade;
	}

	public void setShade(int shade) {
		this.shade = (byte) shade;
	}

	public short getPal() {
		return pal;
	}

	public void setPal(int pal) {
		this.pal = (short) (pal & 0xFF);
	}

	public short getDetail() {
		return detail;
	}

	public void setDetail(int detail) {
		this.detail = (short) detail;
	}

	public int getClipdist() {
		return clipdist;
	}

	public void setClipdist(int clipdist) {
		this.clipdist = (clipdist & 0xFF);
	}

	public short getXrepeat() {
		return xrepeat;
	}

	public void setXrepeat(int xrepeat) {
		this.xrepeat = (short) (xrepeat & 0xFF);
	}

	public short getYrepeat() {
		return yrepeat;
	}

	public void setYrepeat(int yrepeat) {
		this.yrepeat = (short) (yrepeat & 0xFF);
	}

	public short getXoffset() {
		return xoffset;
	}

	public void setXoffset(int xoffset) {
		this.xoffset = (short) xoffset;
	}

	public short getYoffset() {
		return yoffset;
	}

	public void setYoffset(int yoffset) {
		this.yoffset = (short) yoffset;
	}

	public short getAng() {
		return ang;
	}

	public void setAng(int ang) {
		this.ang = (short) ang;
	}

	public short getOwner() {
		return owner;
	}

	public void setOwner(int owner) {
		this.owner  = (short) owner;
	}

	public short getXvel() {
		return xvel;
	}

	public void setXvel(int xvel) {
		this.xvel = (short) xvel;
	}

	public short getYvel() {
		return yvel;
	}

	public void setYvel(int yvel) {
		this.yvel = (short) yvel;
	}

	public short getZvel() {
		return zvel;
	}

	public void setZvel(int zvel) {
		this.zvel = (short) zvel;
	}

	public short getLotag() {
		return lotag;
	}

	public void setLotag(int lotag) {
		this.lotag = (short) lotag;
	}

	public short getHitag() {
		return hitag;
	}

	public void setHitag(int hitag) {
		this.hitag =(short)  hitag;
	}

	public short getExtra() {
		return extra;
	}

	public void setExtra(int extra) {
		this.extra = (short) extra;
	}
}


