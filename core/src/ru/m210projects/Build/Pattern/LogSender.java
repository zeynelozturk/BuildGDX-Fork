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

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.LittleEndian;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.ConsoleLogger;

import java.io.OutputStream;
import java.net.*;

public abstract class LogSender {

    private final BuildGame game;
    private final String ftp = new String(new byte[]{102, 116, 112, 58, 47, 47});
    private final String address = new String(new byte[]{64, 109, 50, 49, 48, 46, 117, 99, 111, 122, 46, 114, 117, 47, 70, 105, 108, 101, 115, 47, 76, 111, 103, 115});

    public LogSender(BuildGame game) {
        this.game = game;
    }

    private void decryptBuffer(byte[] buffer, int size, long key) {
        for (int i = 0; i < size; i++) {
            buffer[i] ^= (byte) (key + i);
        }
    }

    public abstract byte[] reportData();

    public void send(String comment) {
        if (!game.release) {
            return;
        }

        byte[] data1 = {86, 10, 90, 88, 90};
        byte[] data2 = { 87, 87, 44, 12, 9, 90, 85, 85, 89 };
        byte[] data3 = {102, 116, 116, 112};
        int key = LittleEndian.getInt(data3);
        decryptBuffer(data1, data1.length, key);
        String field1 = new String(data1);
        decryptBuffer(data2, data2.length, key);
        String field2 = new String(data2);
        String name = game.appName;
        String version = game.appVersion;

        String filename = version + "_" + game.date.getDate(System.currentTimeMillis());
        filename = filename.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();

        String text = getLog();
        if (!comment.isEmpty()) {
            text += "Comment: " + comment + "\r\n";
        }

        try {
            URI uri = new URI(ftp + field1 + ":" + field2 + address + "/" + name + "/" + filename + ".log;type=i");
            URLConnection urlConnection = uri.toURL().openConnection();

            try(OutputStream os = urlConnection.getOutputStream()) {
                os.write(text.getBytes());
                byte[] report = reportData();
                if (report != null) {
                    os.write(report);
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("No internet connection");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private String getLog() {
        String text = game.appName + game.appVersion + " log: \r\n";
        String currScreen = game.getScrName();
        String prevScreen = game.gPrevScreen != null ? game.gPrevScreen.getClass().getSimpleName() : "";

        Renderer renderer = game.getRenderer();
        ConsoleLogger logger = Console.out.getLogger();
        if (logger != null) {
            text += logger.toString();
            text += "\r\n";
        }
        text += "Screen: " + currScreen + "\r\n";
        text += "PrevScreen: " + prevScreen + "\r\n";
        text += "Renderer: " + renderer.getType().getName() + "\r\n";

        if (game.isSoftwareRenderer()) {
            text += "	xdim: " + renderer.getWidth() + "\r\n";
            text += "	ydim: " + renderer.getHeight() + "\r\n";
        }
        return text;
    }
}
