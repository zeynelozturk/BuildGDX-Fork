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

package ru.m210projects.Build.net;

public class NetInfo {
    public String myip;
    public String extip;
    public String serverip;
    public String message;
    public int port;
    public int netready;
    public int plready = 1;
    public Thread waitThread;
    public boolean useUPnP;
    public int uPnPPort = -1;

    private String lastMessage;

    public void clear() {
        myip = extip = serverip = message = null;
        port = netready = 0;
        plready = 1;
    }

    public boolean waiting() {
        return waitThread.isAlive();
    }

    public void cancel() {
        waitThread.interrupt();
    }

    public boolean IsNewMessage() {
        boolean out = !message.equals(lastMessage);
        lastMessage = message;
        return out;
    }
}
