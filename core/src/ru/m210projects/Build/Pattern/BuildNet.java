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

package ru.m210projects.Build.Pattern;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.net.Mmulti.*;
import static ru.m210projects.Build.Pragmas.klabs;
import static ru.m210projects.Build.Pragmas.ksgn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import ru.m210projects.Build.Types.Serializable;
import ru.m210projects.Build.osd.Console;import ru.m210projects.Build.Pattern.BuildGame.NetMode;
import ru.m210projects.Build.Timer;
import ru.m210projects.Build.Types.LittleEndian;
import ru.m210projects.Build.osd.OsdColor;

public abstract class BuildNet {
	
	public BuildGame game;
	public interface NetInput {
		
		int GetInput(byte[] p, int offset, NetInput oldInput);
		
		int PutInput(byte[] p, int offset, NetInput oldInput);
		
		void reset();
		
		NetInput Copy(NetInput src);
		
	}
	
	public interface DisconnectCallback {
		void invoke(int nDelete);
	}

	public static final byte 	kPacketMasterFrame	= 0;
	public static final byte	kPacketSlaveFrame	= 1;
	public static final byte	kPacketDisconnect	= 7;

	public static final byte 	kPacketSlaveProfile		= (byte) 250;
	public static final byte 	kPacketMasterStart		= (byte) 251;
	public static final byte 	kPacketLevelStart 		= (byte) 252;
	public static final byte 	kPacketEmpty 			= (byte) 254;
	public static final byte 	kPacketLogout			= (byte) 255;
	
	public static final int kNetFifoSize = 256;
	public static final int kFifoMask = kNetFifoSize - 1;
	
	public boolean ready2send;
	
	public int ototalclock = 0;
	public int gNetFifoTail;
	public int[] gNetFifoHead = new int[MAXPLAYERS];
	public int gPredictTail;
	public int gNetFifoMasterTail;
	public NetInput gInput;
	public NetInput[][] gFifoInput = new NetInput[kNetFifoSize][MAXPLAYERS];

	public int MovesPerPacket = 1;
	public int[] myMinLag = new int[MAXPLAYERS];
	public int otherMinLag;
	public int myMaxLag;
	public int bufferJitter = 0;
	public byte[] playerReady = new byte[MAXPLAYERS];
	
	public byte[] packbuf = new byte[MAXPAKSIZ];
	
	public int[] gChecksum = new int[4]; //CheckSize XXX
	private final ChecksumStream gChecksumStream = new ChecksumStream();
	public final int CheckSize = 4 * 4;
	public byte[] tempCheck = new byte[CheckSize];
	public byte[][] gCheckFifo = new byte[MAXPLAYERS][CheckSize * kNetFifoSize];
	public int[] gCheckHead = new int[MAXPLAYERS];
	public int gSendCheckTail;
	public int gCheckTail;
	public boolean bOutOfSync = false;
	public int bOutOfSyncByte = 0;
	
	public int nConnected = 0;
	
	public BuildNet(BuildGame game)
	{
		this.game = game;
		gInput = newInstance();
		for(int i = 0; i < MAXPLAYERS; i++) {
			for(int j = 0; j < kNetFifoSize; j++) {
				gFifoInput[j][i] = newInstance();
			}
		}
	}
	
	public abstract NetInput newInstance();
	
	public abstract int GetPackets(byte[] data, int ptr, int len, int nPlayer);
	
	public abstract void ComputerInput(int i);
	
	public void WaitForSend()
	{
		for(int i=connecthead;i>=0;i=connectpoint2[i])
		{
			if (i != myconnectindex) {
				while(!canSend(i)) {
					;
				}
			}
		}
	}
	
	public int GetPackets()
	{
		int nPlayer, packbufleng;
		byte[] p;
		
		while ((packbufleng = getpacket(packbuf)) > 0)
		{
			int ptr = 0;
			nPlayer = otherpacket;
			p = packbuf;
			
			switch( p[ptr] )
			{
				case kPacketMasterFrame:
					ptr = GetMasterPacket(p, ptr + 1, packbufleng);
					break;
				case kPacketSlaveFrame:
					ptr = GetSlavePacket(p, ptr + 1, packbufleng, nPlayer);
					break;
				case kPacketEmpty:
					break;
					// ready/profile packet
				case kPacketSlaveProfile: 
					playerReady[nPlayer]++;
					break;
		
				case kPacketTick:
					nConnected = p[++ptr] & 0xFF;
					inet.message = "Waiting for other players [" + nConnected + " / " + numplayers + "]";
					return 2;
				default:
					ptr = GetPackets(p, ptr, packbufleng, nPlayer);
					break;
			}
		}
		
		return 1;
	}

	public abstract void UpdatePrediction(NetInput input);
	
	public abstract void CorrectPrediction();
	
	public abstract void CalcChecksum();

 	protected int GetMasterPacket(byte[] p, int ptr, int len)
	{
		for (int i = connecthead; i >= 0; i = connectpoint2[i])
		{
			if ( i != myconnectindex )
			{
				gFifoInput[gNetFifoHead[i] & kFifoMask][i].reset();
				ptr = gFifoInput[gNetFifoHead[i] & kFifoMask][i].GetInput(p, ptr, gFifoInput[(gNetFifoHead[i] - 1) & kFifoMask][i]);
				gNetFifoHead[i]++;
			}
			else
			{
				// skip over my own input data
				// was gInput GDX 06.04.2019
				ptr = gFifoInput[gNetFifoHead[i] & kFifoMask][i].GetInput(p, ptr, gFifoInput[(gNetFifoHead[i] - 1) & kFifoMask][i]);
			}
		}

		// check timer lag info every 16 packets
		if ( ( (gNetFifoHead[connecthead] - 1) & 15) == 0 )
		{
			for ( int i = connectpoint2[connecthead]; i >= 0; i = connectpoint2[i] )
			{
				int lag = p[ptr++];
				if ( i == myconnectindex ) {
					otherMinLag = lag;
				}
			}
		}
		
		while ( ptr < len )
		{
			ptr = GetPacket(p, ptr, tempCheck, 0, CheckSize);
			// store check in all fifos but my own
			for ( int i = connecthead; i >= 0; i = connectpoint2[i] )
			{
				if ( i != myconnectindex )
				{
					System.arraycopy(tempCheck, 0, gCheckFifo[i], CheckSize * (gCheckHead[i] & kFifoMask), CheckSize);
					gCheckHead[i]++;
				}
			}
		}
		
		
		return ptr;
	}
	
 	protected int GetSlavePacket(byte[] p, int ptr, int len, int nPlayer)
	{
		gFifoInput[gNetFifoHead[nPlayer] & kFifoMask][nPlayer].reset();
		ptr = gFifoInput[gNetFifoHead[nPlayer] & kFifoMask][nPlayer].GetInput(p, ptr, gFifoInput[(gNetFifoHead[nPlayer] - 1) & kFifoMask][nPlayer]);
		gNetFifoHead[nPlayer]++;

		while ( ptr < len )
		{
			ptr = GetPacket(p, ptr, gCheckFifo[nPlayer], CheckSize * (gCheckHead[nPlayer] & kFifoMask), CheckSize);
			gCheckHead[nPlayer]++;
		}
		
		return ptr;
	}
	
	public int GetDisconnectPacket(byte[] p, int ptr, int len, int nPlayer, DisconnectCallback deletePlayerSprite)
	{
		retransmit(nPlayer, packbuf, len);
		
		nPlayer = LittleEndian.getInt(p, ptr);
		if(nPlayer == myconnectindex) {
			game.ThrowError("nPlayer != myconnectindex");
		}
		
		WaitForAllPlayers(1000);

		if(deletePlayerSprite != null && game.nNetMode != NetMode.Single) {
			deletePlayerSprite.invoke(nPlayer);
		}
		
		if ( nPlayer == connecthead ) {
			connecthead = connectpoint2[connecthead];
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					NetDisconnect(myconnectindex);
				}
			});
			return 1;
		}
		else {
			for ( int j = connecthead; j >= 0; j = connectpoint2[j] )
			{
				if ( connectpoint2[j] == nPlayer )
				{
					connectpoint2[j] = connectpoint2[nPlayer];
					break;
				}
			}
		}

		if(numplayers > 1) {
			numplayers--;
		}
		
		if(numplayers < 2) {
			game.nNetMode = NetMode.Single;
		}

		if(!WaitForAllPlayers(0)) {
			return -1;
		}
		
		return 1;
	}
	
	public int GetPacket(byte[] p, int pptr, byte[] v, int vptr, int size)
	{
		if(pptr + size >= p.length) {
			game.ThrowError("ptr + size < packbuf.length");
		}
		System.arraycopy(p, pptr, v, vptr, size); //memcpy(v, p, size);
		return pptr + size;
	}
	
	public int PutPacketByte(byte[] p, int ptr, int value)
	{
		p[ptr] = (byte) value;
		return ptr + 1;
	}

	public int PutPacket(byte[] p, int ptr, Object v, int vptr, int size)
	{
		if(ptr + size > p.length) {
			game.ThrowError("ptr + size < packbuf.length");
		}
		if(v instanceof byte[])
		{
			byte[] array = (byte[]) v;
			System.arraycopy(array, vptr, p, ptr, size); //memcpy(p, v, size);
			return ptr + size;
		} 
		else if(v instanceof int[])
		{
			int[] array = (int[]) v;
			for(int i = 0; i < size / 4; i++) {
				LittleEndian.putInt(p, ptr, array[vptr + i]);
				ptr += 4;
			}
		} 

		return -1;
	}
	
	public void sendtoall(byte[] bufptr, int messleng)
	{
		for(int i=connecthead;i>=0;i=connectpoint2[i])
		{
			if (i != myconnectindex) {
				sendpacket(i,bufptr,messleng);
			}
			if (myconnectindex != connecthead) {
				break; //slaves in M/S mode only send to master
			}
		}
	}

	public void retransmit(int nPlayer, byte[] bufptr, int messleng)
	{
		//Slaves in M/S mode only send to master
		//Master re-transmits message to all others
		if (myconnectindex == connecthead) {
			for(int i=connectpoint2[connecthead];i>=0;i=connectpoint2[i]) {
				if (i != nPlayer) {
					sendpacket(i, bufptr, messleng);
				}
			}
		}
	}
	
	public void GetNetworkInput()
	{
		if(numplayers < 2) {
			return;
		}
		
		for ( int nPlayer = connecthead; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] )
		{
			int n = (gNetFifoHead[myconnectindex] - 1) - gNetFifoHead[nPlayer];
			myMinLag[nPlayer] = Math.min(myMinLag[nPlayer], n);
			myMaxLag = Math.max(myMaxLag, n);
		}

		// recalculate buffer value every few frames
		if ( (gNetFifoHead[myconnectindex] & 15) == 0 )
		{
			int i = myMaxLag - bufferJitter; myMaxLag = 0;
			if ( i > 0 ) {
				bufferJitter += (2 + i) >> 2;
			} else if ( i < 0 ) {
				bufferJitter -= (2 - i) >> 2;
			}
		}

		// am I a slave
		if ( myconnectindex != connecthead )
		{
			// build slave packet
			int ptr = PutPacketByte(packbuf, 0, kPacketSlaveFrame);
			ptr = gFifoInput[(gNetFifoHead[myconnectindex] - 1) & kFifoMask][myconnectindex].PutInput(packbuf, ptr, gFifoInput[(gNetFifoHead[myconnectindex]-2)&kFifoMask][myconnectindex]);
			if(ptr == 0)
			{
				System.err.println("Error! Input.PutInput not implemented!");
				ptr = 1;
			}

			// calculate timer lag info every 16 packets
			if ( (gNetFifoHead[myconnectindex] & 15) == 0 )
			{
				int i = myMinLag[connecthead] - otherMinLag;
				if (klabs(i) > 8) {
					i >>= 1;
				} else if (klabs(i) > 2) {
					i = ksgn(i);
				} else {
					i = 0;
				}

				Timer timer = game.pEngine.getTimer();
				timer.setTotalClock(timer.getTotalClock() - game.pEngine.getTimer().getFrameTicks() * i);
	            otherMinLag += i;

				for ( int nPlayer = connecthead; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] ) {
					myMinLag[nPlayer] = 0x7FFFFFFF;
				}
			}

			while ( gSendCheckTail < gCheckHead[myconnectindex] )
			{
				ptr = PutPacket(packbuf, ptr, gCheckFifo[myconnectindex], CheckSize * (gSendCheckTail & kFifoMask), CheckSize);
				gSendCheckTail++;
			}
			
			// send packet to master
			sendpacket(connecthead, packbuf, ptr);
			return;
		}

		for ( int i = connecthead; i >= 0; i = connectpoint2[i] )
		{
			if ( gNetFifoHead[i] <= gNetFifoMasterTail )
			{
				// send empty packets to slaves that are behind
				// this fixes (?) problems resulting from missed slave packets
				PutPacketByte(packbuf, 0, kPacketEmpty);
				for (i=connectpoint2[connecthead]; i >= 0; i = connectpoint2[i] ) {
					sendpacket(i, packbuf, 1);
				}
				return;
			}
		}

		// I must be the Master
		while(true)
		{
			for ( int i = connecthead; i >= 0; i = connectpoint2[i] ) {
				if ( gNetFifoHead[i] <= gNetFifoMasterTail ) {
					return;
				}
			}

			// build master packet
			int ptr = PutPacketByte(packbuf, 0, kPacketMasterFrame);

			for ( int nPlayer = connecthead; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] ) {
				ptr = gFifoInput[gNetFifoMasterTail & kFifoMask][nPlayer].PutInput(packbuf, ptr, gFifoInput[(gNetFifoMasterTail - 1) & kFifoMask][nPlayer]);
			}

			// include timer lag info every 16 packets
			if ( (gNetFifoMasterTail & 15) == 0 )
			{
				for ( int nPlayer = connectpoint2[connecthead]; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] ) {
					ptr = PutPacketByte(packbuf, ptr, BClipRange(myMinLag[nPlayer], -128, 127));
				}
	
				for ( int nPlayer = connecthead; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] ) {
					myMinLag[nPlayer] = 0x7FFFFFFF;
				}
			}
	
			while ( gSendCheckTail < gCheckHead[myconnectindex] )
			{
				ptr = PutPacket(packbuf, ptr, gCheckFifo[myconnectindex], CheckSize * (gSendCheckTail & kFifoMask), CheckSize);
				gSendCheckTail++;
			}
	
			for ( int nPlayer = connectpoint2[connecthead]; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] ) {
				sendpacket(nPlayer, packbuf, ptr);
			}
	
			gNetFifoMasterTail++;
		}
	}
	
	public void NetDisconnect(int nPlayer)
	{
		Console.out.println("Disconnected!", OsdColor.YELLOW);
		if ( numplayers > 1 )
		{
		    packbuf[0] = kPacketDisconnect;
		    LittleEndian.putInt(packbuf, 1, nPlayer);
		    
		    sendtoall(packbuf, 5);
		    WaitForAllPlayers(1000);
		}
		
		ResetNetwork();
		game.nNetMode = NetMode.Single;
		ready2send = false;
	}
	
	public void ResetNetwork()
	{
		uninitmultiplayer();
		Arrays.fill(playerReady, (byte)0);
		Arrays.fill(packbuf, (byte)0);
		Arrays.fill(gChecksum, 0);
		Arrays.fill(gCheckHead, 0);
		Arrays.fill(tempCheck, (byte)0);
		for(int i = 0; i < MAXPLAYERS; i++) {
			Arrays.fill(gCheckFifo[i], (byte)0);
		}
		ready2send = false;
	}
	
	public void ResetTimers() {
//		game.pEngine.getTimer().update(0); //update timer before reset
		game.pEngine.getTimer().reset();
		game.getProcessor().resetPollingStates();
		ototalclock = 0;
		gNetFifoMasterTail = 0;
		gPredictTail = 0;
		gNetFifoTail = 0;
		gInput.reset();
		Arrays.fill(gNetFifoHead, 0);
		for(int i = 0; i < MAXPLAYERS; i++) {
			Arrays.fill(gCheckFifo[i], (byte)0);
			for(int j = 0; j < kNetFifoSize; j++) {
				gFifoInput[j][i].reset();
			}
		}
		Arrays.fill(gCheckHead, 0);
		gCheckTail = 0;
		gSendCheckTail = 0;
		Arrays.fill(myMinLag, 0);
		otherMinLag = 0;
		myMaxLag = 0;
		bufferJitter = 0;
		
		bOutOfSync = false;
		bOutOfSyncByte = 0;
	}
	
	public boolean WaitForAllPlayers(int timeout)
	{
		if (numplayers < 2) {
			return true;
		}

		for(int i=connecthead;i>=0;i=connectpoint2[i])
		{
			if (i != myconnectindex) {
				while(!canSend(i)) {
					;
				}
			}
		}
		
		packbuf[0] = kPacketSlaveProfile;
		sendtoall(packbuf,1);
		playerReady[myconnectindex]++;

		long starttime = System.currentTimeMillis();
		while (true)
		{
//			game.pEngine.handleevents();
			long time = System.currentTimeMillis() - starttime;
			
			if (/*ctrlKeyStatusOnce(Keys.ESCAPE) || */(timeout != 0 && time > timeout)) 
			{
				Console.out.println("Connection timed out!", OsdColor.YELLOW);
				return false;
			}
			
			switch(GetPackets())
			{
				case 0: return false; //disconnect
				case 1: break; //waiting
				case 2:
					starttime = System.currentTimeMillis();
					break; //tick
			}

			int i;
			for(i=connecthead;i>=0;i=connectpoint2[i])
			{
				if (playerReady[i] < playerReady[myconnectindex]) {
					break;
				}
				if (myconnectindex != connecthead) { i = -1; break; } //slaves in M/S mode only wait for master
			}
			if (i < 0) {
				Arrays.fill(playerReady, (byte)0);
				return true;
			}
		}
	}

	public long checksum(Serializable obj) {
		return gChecksumStream.checksum(obj);
	}

	public long Checksum( byte[] p, int length )
	{
		int ptr = 0;
		length >>= 2;
		long sum = 0;
		while ( (--length) != -1 ) {
			sum += LittleEndian.getInt(p, ptr);
			ptr += 4;
		}

		return sum;
	}
	
	public void CheckSync()
	{
		int nPlayer;

		if ( numplayers == 1 ) {
			return;
		}

		while ( true )
		{
			for ( nPlayer = connecthead; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] )
			{
				if ( gCheckHead[nPlayer] <= gCheckTail ) {
					return;
				}
			}
			
			bOutOfSync = false;
			
			for ( nPlayer = connectpoint2[connecthead]; nPlayer >= 0; nPlayer = connectpoint2[nPlayer] )
			{
				for (int i = 0; i < CheckSize; i++)
				{
					if ( gCheckFifo[nPlayer][(CheckSize * (gCheckTail & kFifoMask)) + i] != gCheckFifo[connecthead][(CheckSize * (gCheckTail & kFifoMask)) + i] )
					{
						bOutOfSyncByte = i;
						bOutOfSync = true;
					}
				}
			}

			gCheckTail++;
		}
	}
	
	public void StartWaiting(final int timeout)
	{
		inet.waitThread = new Thread(new Runnable() 
		{
			public void run()
			{
				if(!WaitForAllPlayers(timeout)) {
					game.pNet.NetDisconnect(myconnectindex);
				}
			}
		});
		inet.waitThread.start();
	}

	private static class ChecksumStream extends ByteArrayOutputStream {

		public long checksum(Serializable obj) {
			this.count = 0;
			try {
				obj.writeObject(this);
			} catch (IOException e) {
				return 0;
			}

			int ptr = 0;
			count >>= 2;
			long sum = 0;
			while ( (--count) != -1 ) {
				int b0 = buf[ptr++] & 0xFF;
				int b1 = buf[ptr++] & 0xFF;
				int b2 = buf[ptr++] & 0xFF;
				long b3 = buf[ptr++] & 0xFF;

				sum += (b3 << 24 ) + ( b2 << 16 ) + ( b1 << 8 ) + (b0);
			}

			return sum;
		}
	}
}
