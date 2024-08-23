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

package ru.m210projects.Build.Render;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.Software.Software;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import static ru.m210projects.Build.settings.GameConfig.DUMMY_CONFIG;

public class RenderTester extends JFrame {

//    Example
//
//    RenderTester tester = new RenderTester(new BloodSoftware(DUMMY_CONFIG));
//
//    @Override
//    public void changepalette(byte[] palette) {
//        super.changepalette(palette);
//        tester.getSoftware().changepalette(palette);
//    }
//
//    @Override
//    public void init(Engine engine) {
//        tester.init(engine);
//        super.init(engine);
//    }
//
//    @Override
//    public void nextpage() {
//        super.nextpage();
//        tester.nextpage();
//    }
//
//    @Override
//    public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
//                             int cy1, int cx2, int cy2) {
//        super.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
//        tester.getSoftware().rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
//    }
//
//    @Override
//    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) {
//        super.drawrooms(daposx, daposy, daposz, daang, dahoriz, dacursectnum);
//        tester.drawrooms(daposx, daposy, daposz, daang, dahoriz, dacursectnum);
//        return 0;
//    }

    int backBufferWidth = 640;
    int backBufferHeight = 400;
    Software software;

    public RenderTester(Software software) {
        Raster canvas = new Raster(Color.BLACK);
        canvas.update(backBufferWidth, backBufferHeight);
        add(canvas);
        this.software = software;
        software.config.setgShowFPS(false);
        software.setChangeListener(new Software.ChangeListener() {
            @Override
            public void onChangePalette(byte[] palette) {
                canvas.changePalette(palette);
            }

            @Override
            public void onNextPage(byte[] frameBuffer) {
                byte[] dst = canvas.getFrameBuffer();
                System.arraycopy(frameBuffer, 0, dst, 0, Math.min(frameBuffer.length, dst.length));
                canvas.invalidate();
                canvas.repaint();
            }
        });
    }

    public void init(Engine engine) {
        software.init(engine);
        software.resize(backBufferWidth, backBufferHeight);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void nextpage() {
        SwingUtilities.invokeLater(() -> {
            if (software.isInited) {
                software.nextpage();
            }
        });
    }

    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) {
        software.drawrooms(daposx, daposy, daposz, daang, dahoriz, dacursectnum);
        return 0;
    }

    public Software getSoftware() {
        return software;
    }

    class Raster extends JPanel {

        private final Color background;
        private BufferedImage display;
        private byte[] data;
        private IndexColorModel paletteModel;
        private int width, height;

        public Raster(Color background) {
            this.background = background;
            this.paletteModel = new IndexColorModel(8, 256, new byte[768], 0, false);
            this.display = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
            this.data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            this.setFocusable(false);
        }

        @Override
        public void update(java.awt.Graphics g) {
            paint(g);
        }

        @Override
        public void paint(java.awt.Graphics g) {
            g.drawImage(display, 0, 0, null);
        }

        public byte[] getFrameBuffer() {
            return data;
        }

        public void changePalette(byte[] palette) {
            paletteModel = new IndexColorModel(8, 256, palette, 0, false);
            display = new BufferedImage(paletteModel, display.getRaster(), false, null);
        }

        public void update(int width, int height) {
            if (this.width == width && this.height == height) {
                return;
            }

            this.width = width;
            this.height = height;

            Dimension size = new Dimension(width, height);
            setSize(size);
            setPreferredSize(size);
            setBackground(background);

            display = new BufferedImage(backBufferWidth, backBufferHeight, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
            data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            validate();
        }
    }
}
