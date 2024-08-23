/*
 *  Sector structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import org.jetbrains.annotations.Nullable;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class Sector {
    public static final int sizeof = 40;

    private transient LinkedList<Wall> wallList = new LinkedList<>();

    private short wallptr;
    private short wallnum; // 4
    private int ceilingz;
    private int floorz; // 8
    private short ceilingstat;
    private short floorstat; // 4
    private short ceilingpicnum;
    private short ceilingheinum; // 4
    private byte ceilingshade; // 1
    private short ceilingpal;
    private short ceilingxpanning;
    private short ceilingypanning; // 3
    private short floorpicnum;
    private short floorheinum; // 4
    private byte floorshade; // 1
    private short floorpal;
    private short floorxpanning;
    private short floorypanning; // 3
    private short visibility;
    private short filler; // 2
    private short lotag;
    private short hitag;
    private short extra; // 6

    public Sector() {
    }

    public Sector(byte[] data) throws IOException {
        readObject(new ByteArrayInputStream(data));
    }

    public Sector(InputStream data) throws IOException {
        readObject(data);
    }

    public static int getSizeof() {
        return sizeof;
    }

    public int getLoopNum(int wallnum) { // jfBuild
        int numloops = 0;

        for (ListNode<Wall> wn = wallList.getFirst(); wn != null; wn = wn.getNext()) {
            Wall wall = wn.get();
            int i = wn.getIndex();
            if (i == wallnum) {
                return (numloops);
            }

            if (wall.getPoint2() < i) {
                numloops++;
            }
        }
        return (-1);
    }

    public int getEndWall() {
        return wallptr + wallnum - 1;
    }

    public boolean isParallaxCeiling() {
        return (getCeilingstat() & 1) != 0;
    }

    public boolean isCeilingSlope() {
        return (getCeilingstat() & 2) != 0;
    }

    public boolean isTexSwapedCeiling() {
        return (getCeilingstat() & 4) != 0;
    }

    public boolean isTexSmooshedCeiling() {
        return (getCeilingstat() & 8) != 0;
    }

    public boolean isTexXFlippedCeiling() {
        return (getCeilingstat() & 16) != 0;
    }

    public boolean isTexYFlippedCeiling() {
        return (getCeilingstat() & 32) != 0;
    }

    public boolean isRelativeTexCeiling() {
        return (getCeilingstat() & 64) != 0;
    }

    public boolean isMaskedTexCeiling() {
        return (getCeilingstat() & 128) != 0;
    }

    public boolean isTransparentCeiling() {
        return (getCeilingstat() & 256) != 0;
    }

    public boolean isTransparent2Ceiling() {
        return (getCeilingstat() & (128 | 256)) != 0;
    }

    public boolean isParallaxFloor() {
        return (getFloorstat() & 1) != 0;
    }

    public boolean isFloorSlope() {
        return (getFloorstat() & 2) != 0;
    }

    public boolean isTexSwapedFloor() {
        return (getFloorstat() & 4) != 0;
    }

    public boolean isTexSmooshedFloor() {
        return (getFloorstat() & 8) != 0;
    }

    public boolean isTexXFlippedFloor() {
        return (getFloorstat() & 16) != 0;
    }

    public boolean isTexYFlippedFloor() {
        return (getFloorstat() & 32) != 0;
    }

    public boolean isRelativeTexFloor() {
        return (getFloorstat() & 64) != 0;
    }

    public boolean isMaskedTexFloor() {
        return (getFloorstat() & 128) != 0;
    }

    public boolean isTransparentFloor() {
        return (getFloorstat() & 256) != 0;
    }

    public boolean isTransparent2Floor() {
        return (getFloorstat() & (128 | 256)) != 0;
    }

    public Sector readObject(InputStream is) throws IOException {
        setWallptr(StreamUtils.readShort(is));
        setWallnum(StreamUtils.readShort(is));
        setCeilingz(StreamUtils.readInt(is));
        setFloorz(StreamUtils.readInt(is));
        setCeilingstat(StreamUtils.readShort(is));
        setFloorstat(StreamUtils.readShort(is));
        setCeilingpicnum(StreamUtils.readShort(is));
        setCeilingheinum(StreamUtils.readShort(is));
        setCeilingshade(StreamUtils.readByte(is));
        setCeilingpal(StreamUtils.readUnsignedByte(is));
        setCeilingxpanning(StreamUtils.readUnsignedByte(is));
        setCeilingypanning(StreamUtils.readUnsignedByte(is));
        setFloorpicnum(StreamUtils.readShort(is));
        setFloorheinum(StreamUtils.readShort(is));
        setFloorshade(StreamUtils.readByte(is));
        setFloorpal(StreamUtils.readUnsignedByte(is));
        setFloorxpanning(StreamUtils.readUnsignedByte(is));
        setFloorypanning(StreamUtils.readUnsignedByte(is));
        setVisibility(StreamUtils.readUnsignedByte(is));
        setFiller(StreamUtils.readByte(is));
        setLotag(StreamUtils.readShort(is));
        setHitag(StreamUtils.readShort(is));
        setExtra(StreamUtils.readShort(is));

        return this;
    }

    public void writeObject(OutputStream os) throws IOException {
        StreamUtils.writeShort(os, getWallptr());
        StreamUtils.writeShort(os, getWallnum());
        StreamUtils.writeInt(os, getCeilingz());
        StreamUtils.writeInt(os, getFloorz());
        StreamUtils.writeShort(os, getCeilingstat());
        StreamUtils.writeShort(os, getFloorstat());
        StreamUtils.writeShort(os, getCeilingpicnum());
        StreamUtils.writeShort(os, getCeilingheinum());
        os.write(getCeilingshade());
        os.write(getCeilingpal());
        os.write(getCeilingxpanning());
        os.write(getCeilingypanning());
        StreamUtils.writeShort(os, getFloorpicnum());
        StreamUtils.writeShort(os, getFloorheinum());
        os.write(getFloorshade());
        os.write(getFloorpal());
        os.write(getFloorxpanning());
        os.write(getFloorypanning());
        os.write(getVisibility());
        os.write(getFiller());
        StreamUtils.writeShort(os, getLotag());
        StreamUtils.writeShort(os, getHitag());
        StreamUtils.writeShort(os, getExtra());
    }

    public void set(Sector src) {
        setWallptr(src.getWallptr());
        setWallnum(src.getWallnum());
        setCeilingz(src.getCeilingz());
        setFloorz(src.getFloorz());
        setCeilingstat(src.getCeilingstat());
        setFloorstat(src.getFloorstat());
        setCeilingpicnum(src.getCeilingpicnum());
        setCeilingheinum(src.getCeilingheinum());
        setCeilingshade(src.getCeilingshade());
        setCeilingpal(src.getCeilingpal());
        setCeilingxpanning(src.getCeilingxpanning());
        setCeilingypanning(src.getCeilingypanning());
        setFloorpicnum(src.getFloorpicnum());
        setFloorheinum(src.getFloorheinum());
        setFloorshade(src.getFloorshade());
        setFloorpal(src.getFloorpal());
        setFloorxpanning(src.getFloorxpanning());
        setFloorypanning(src.getFloorypanning());
        setVisibility(src.getVisibility());
        setFiller(src.getFiller());
        setLotag(src.getLotag());
        setHitag(src.getHitag());
        setExtra(src.getExtra());
    }

    @Override
    public String toString() {
        String out = "wallptr " + getWallptr() + " \r\n";
        out += "wallnum " + getWallnum() + " \r\n";
        out += "ceilingz " + getCeilingz() + " \r\n";
        out += "floorz " + getFloorz() + " \r\n";
        out += "ceilingstat " + getCeilingstat() + " \r\n";
        out += "floorstat " + getFloorstat() + " \r\n";
        out += "ceilingpicnum " + getCeilingpicnum() + " \r\n";
        out += "ceilingheinum " + getCeilingheinum() + " \r\n";
        out += "ceilingshade " + getCeilingshade() + " \r\n";
        out += "ceilingpal " + getCeilingpal() + " \r\n";
        out += "ceilingxpanning " + getCeilingxpanning() + " \r\n";
        out += "ceilingypanning " + getCeilingypanning() + " \r\n";
        out += "floorpicnum " + getFloorpicnum() + " \r\n";
        out += "floorheinum " + getFloorheinum() + " \r\n";
        out += "floorshade " + getFloorshade() + " \r\n";
        out += "floorpal " + getFloorpal() + " \r\n";
        out += "floorxpanning " + getFloorxpanning() + " \r\n";
        out += "floorypanning " + getFloorypanning() + " \r\n";
        out += "visibility " + getVisibility() + " \r\n";
        out += "filler " + getFiller() + " \r\n";
        out += "lotag " + getLotag() + " \r\n";
        out += "hitag " + getHitag() + " \r\n";
        out += "extra " + getExtra() + " \r\n";

        return out;
    }

    public boolean inside(int x, int y) {
        int cnt = 0;
        for (ListNode<Wall> wn = wallList.getFirst(); wn != null; wn = wn.getNext()) {
            Wall wal = wn.get();
            Wall wal2 = wal.getWall2();
            int y1 = wal.getY() - y;
            int y2 = wal2.getY() - y;

            if ((y1 ^ y2) < 0) {
                int x1 = wal.getX() - x;
                int x2 = wal2.getX() - x;
                if ((x1 ^ x2) >= 0) {
                    cnt ^= x1;
                } else {
                    cnt ^= (x1 * y2 - x2 * y1) ^ y2;
                }
            }
        }

        return (cnt >>> 31) != 0;
    }

    public boolean inside(int x, int y, int z) {
        int i = 0, j = 0;
        if (isFloorSlope() || isCeilingSlope()) {
            ListNode<Wall> wn = wallList.getFirst();
            if (wn == null) {
                return false;
            }

            Wall wal = wn.get();
            Wall wal2 = wal.getWall2();

            int dx = wal2.getX() - wal.getX();
            int dy = wal2.getY() - wal.getY();
            i = EngineUtils.sqrt(dx * dx + dy * dy) << 5;
            if (i != 0) {
                j = dx * (y - wal.getY()) - (dy * (x - wal.getX())) >> 3;
            }
        }

        int cz = calcCeilingHeight(i, j);
        int fz = calcFloorHeight(i, j);
        if (z < cz || z > fz) {
            return false;
        }

        return inside(x, y);
    }

    private int calcFloorHeight(int i, int j) {
        int fz = floorz;
        if (isFloorSlope() && i != 0) {
            fz += (int) ((long) floorheinum * j / i);
        }
        return fz;
    }

    private int calcCeilingHeight(int i, int j) {
        int cz = ceilingz;
        if (isCeilingSlope() && i != 0) {
            cz += (int) ((long) ceilingheinum * j / i);
        }
        return cz;
    }

    public void getzsofslope(int x, int y, AtomicInteger floorZ, AtomicInteger ceilingZ) {
        boolean floorSlope = isFloorSlope() && floorZ != null;
        boolean ceilingSlope = isCeilingSlope() && ceilingZ != null;
        int i = 0, j = 0;
        if (floorSlope || ceilingSlope) {
            ListNode<Wall> wn = wallList.getFirst();
            if (wn == null) {
                return;
            }

            Wall wal = wn.get();
            Wall wal2 = wal.getWall2();

            int dx = wal2.getX() - wal.getX();
            int dy = wal2.getY() - wal.getY();
            i = EngineUtils.sqrt(dx * dx + dy * dy) << 5;
            if (i != 0) {
                j = dx * (y - wal.getY()) - (dy * (x - wal.getX())) >> 3;
            }
        }

        if (floorZ != null) {
            floorZ.set(calcFloorHeight(i, j));
        }

        if (ceilingZ != null) {
            ceilingZ.set(calcCeilingHeight(i, j));
        }
    }

    public void alignSlope(int x, int y, int z, boolean isCeilingSlope) {
        if (wallList.getFirst() == null) {
            return;
        }

        final Wall wal = wallList.getFirst().get();
        final Wall wal2 = wal.getWall2();

        int dax = wal2.getX() - wal.getX();
        int day = wal2.getY() - wal.getY();

        int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
        if (i == 0) {
            return;
        }

        if (isCeilingSlope) {
            setCeilingheinum((int) (((long) (z - getCeilingz()) << 8) * EngineUtils.sqrt(dax * dax + day * day) / i));
            int ceilingstat = getCeilingstat();
            if (getCeilingheinum() == 0) {
                setCeilingstat(ceilingstat & ~2);
            } else {
                setCeilingstat(ceilingstat | 2);
            }
        } else {
            setFloorheinum((int) (((long) (z - getFloorz()) << 8) * EngineUtils.sqrt(dax * dax + day * day) / i));
            int floorstat = getFloorstat();
            if (getFloorheinum() == 0) {
                setFloorstat(floorstat & ~2);
            } else {
                setFloorstat(floorstat | 2);
            }
        }
    }

    // GETTERS AND SETTERS

    @Nullable
    public ListNode<Wall> getWallNode() {
        return wallList.getFirst();
    }

    public void setWallList(LinkedList<Wall> wallList) {
        this.wallList = LinkedList.toImmutableList(wallList);
    }

    public short getWallptr() {
        return wallptr;
    }

    public void setWallptr(int wallptr) {
        this.wallptr = (short) wallptr;
    }

    public short getWallnum() {
        return wallnum;
    }

    public void setWallnum(int wallnum) {
        this.wallnum = (short) wallnum;
    }

    public int getCeilingz() {
        return ceilingz;
    }

    public void setCeilingz(int ceilingz) {
        this.ceilingz = ceilingz;
    }

    public int getFloorz() {
        return floorz;
    }

    public void setFloorz(int floorz) {
        this.floorz = floorz;
    }

    public short getCeilingstat() {
        return ceilingstat;
    }

    public void setCeilingstat(int ceilingstat) {
        this.ceilingstat = (short) ceilingstat;
    }

    public short getFloorstat() {
        return floorstat;
    }

    public void setFloorstat(int floorstat) {
        this.floorstat = (short) floorstat;
    }

    public short getCeilingpicnum() {
        return ceilingpicnum;
    }

    public void setCeilingpicnum(int ceilingpicnum) {
        this.ceilingpicnum = (short) ceilingpicnum;
    }

    public short getCeilingheinum() {
        return ceilingheinum;
    }

    public void setCeilingheinum(int ceilingheinum) {
        this.ceilingheinum = (short) ceilingheinum;
    }

    public byte getCeilingshade() {
        return ceilingshade;
    }

    public void setCeilingshade(int ceilingshade) {
        this.ceilingshade = (byte) ceilingshade;
    }

    public short getCeilingpal() {
        return ceilingpal;
    }

    public void setCeilingpal(int ceilingpal) {
        this.ceilingpal = (short) (ceilingpal & 0xFF);
    }

    public short getCeilingxpanning() {
        return ceilingxpanning;
    }

    public void setCeilingxpanning(int ceilingxpanning) {
        this.ceilingxpanning = (short) (ceilingxpanning & 0xFF);
    }

    public short getCeilingypanning() {
        return ceilingypanning;
    }

    public void setCeilingypanning(int ceilingypanning) {
        this.ceilingypanning = (short) (ceilingypanning & 0xFF);
    }

    public short getFloorpicnum() {
        return floorpicnum;
    }

    public void setFloorpicnum(int floorpicnum) {
        this.floorpicnum = (short) floorpicnum;
    }

    public short getFloorheinum() {
        return floorheinum;
    }

    public void setFloorheinum(int floorheinum) {
        this.floorheinum = (short) floorheinum;
    }

    public byte getFloorshade() {
        return floorshade;
    }

    public void setFloorshade(int floorshade) {
        this.floorshade = (byte) floorshade;
    }

    public short getFloorpal() {
        return floorpal;
    }

    public void setFloorpal(int floorpal) {
        this.floorpal = (short) (floorpal & 0xFF);
    }

    public short getFloorxpanning() {
        return floorxpanning;
    }

    public void setFloorxpanning(int floorxpanning) {
        this.floorxpanning = (short) (floorxpanning & 0xFF);
    }

    public short getFloorypanning() {
        return floorypanning;
    }

    public void setFloorypanning(int floorypanning) {
        this.floorypanning = (short) (floorypanning & 0xFF);
    }

    public short getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = (short) (visibility & 0xFF);
    }

    public short getFiller() {
        return filler;
    }

    public void setFiller(int filler) {
        this.filler = (short) filler;
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
        this.hitag = (short) hitag;
    }

    public short getExtra() {
        return extra;
    }

    public void setExtra(int extra) {
        this.extra = (short) extra;
    }

}