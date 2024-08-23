package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Board;
import ru.m210projects.Build.BoardService;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Serializable;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.DynamicArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class SaveReader {

    protected final String signature;
    protected final int version;
    protected final Engine engine;
    protected final List<Savable> components = new ArrayList<>();

    public SaveReader(Engine engine, String signature, int version) {
        this.engine = engine;
        this.signature = signature;
        this.version = version;
    }

    protected abstract SaveHeader getNewHeader(String saveName);

    protected void updateScreenshot(SaveHeader header) {
        ArtEntry pic = engine.getTile(SaveManager.Screenshot);
        if (!(pic instanceof DynamicArtEntry) || !pic.exists() || pic.getWidth() != header.picWidth || pic.getHeight() != header.picHeight) {
            pic = engine.allocatepermanenttile(SaveManager.Screenshot, header.picWidth, header.picHeight);
        }
        ((DynamicArtEntry) pic).copyData(header.picData);
    }

    public SaveHeader loadHeader(Entry file) {
        SaveHeader header = getNewHeader("");
        file.load(header::readObject);
        if (header.hasScreenshot()) {
            updateScreenshot(header);
        }
        return header;
    }

    public boolean checkVersion(SaveHeader header) {
        return header.getSignature().equals(signature) && header.getVersion() == version;
    }

    public SaveHeader saveGame(Directory dir, String saveName, String fileName) throws IOException {
        FileEntry file = dir.getEntry(fileName);
        if (file.exists()) {
            if (!file.delete()) {
                throw new AccessDeniedException("Game not saved. Access denied!");
            }
        }

        Path path = dir.getPath().resolve(fileName);
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(path))) {
            SaveHeader header = getNewHeader(saveName);
            header.writeObject(os);
            for (Savable component : components) {
                component.writeObject(os);
            }

            file = dir.addEntry(path);
            if (!file.exists()) {
                throw new FileNotFoundException(fileName);
            }

            return header;
        }
    }

    public boolean loadGame(Entry file) {
        if (file.exists() && file.getSize() > 0) {
            try (InputStream is = file.getInputStream()) {
                SaveHeader header = getNewHeader("");
                header.readObject(is);
                if (checkVersion(header)) {
                    return false;
                }

                for (Savable component : components) {
                    component.readObject(is);
                }

                // CRC32 check
                if (is.available() == 0) {
                    for (Savable component : components) {
                        component.apply();
                    }
                    return true;
                }

            } catch (Exception e) {
                Console.out.println(String.format("Failed to load saved game file %s: %s", file.getName(), e), OsdColor.RED);
            }
        }

        return false;
    }

    public interface Savable<T> extends Serializable<T> {
        void apply();
    }

    public static class MapReader implements Savable<MapReader> {

        protected Board board;
        protected String boardfilename;
        protected BoardService boardService;

        public MapReader(BoardService boardService, String boardfilename) {
            this.board = boardService.getBoard();
            this.boardService = boardService;
            this.boardfilename = boardfilename;
        }

        @Override
        public MapReader readObject(InputStream is) throws IOException {
            boardfilename = StreamUtils.readDataString(is);

            int numsectors = StreamUtils.readInt(is);
            Sector[] sector = new Sector[numsectors];
            for (int i = 0; i < sector.length; i++) {
                sector[i] = new Sector().readObject(is);
            }

            Wall[] wall = new Wall[StreamUtils.readInt(is)];
            for (int i = 0; i < wall.length; i++) {
                wall[i] = new Wall().readObject(is);
            }

            int numSprites = StreamUtils.readInt(is);
            List<Sprite> sprite = new ArrayList<>(numSprites * 2);
            for (int i = 0; i < numSprites; i++) {
                sprite.add(new Sprite().readObject(is));
            }

            this.board = new Board(null, sector, wall, sprite);

            return this;
        }

        @Override
        public MapReader writeObject(OutputStream os) throws IOException {
            StreamUtils.writeDataString(os, boardfilename);

            Sector[] sectors = board.getSectors();
            StreamUtils.writeInt(os, sectors.length);
            for (Sector s : sectors) {
                s.writeObject(os);
            }

            Wall[] walls = board.getWalls();
            StreamUtils.writeInt(os, walls.length);
            for (Wall wal : walls) {
                wal.writeObject(os);
            }

            List<Sprite> sprites = board.getSprites();
            StreamUtils.writeInt(os, sprites.size());
            for (Sprite s : sprites) {
                s.writeObject(os);
            }

            return this;
        }

        public Board getBoard() {
            return board;
        }

        public String getBoardfilename() {
            return boardfilename;
        }

        @Override
        public void apply() {
            boardService.setBoard(board);
        }
    }

    public static class SaveHeader implements Serializable<SaveHeader> {

        protected String signature;
        protected int version;
        protected long time = System.currentTimeMillis();
        protected String name;
        protected int picWidth, picHeight;
        protected byte[] picData;

        public void setScreenshot(int width, int height) {
            this.picWidth = width;
            this.picHeight = height;
            this.picData = new byte[width * height];
        }

        @Override
        public SaveHeader readObject(InputStream is) throws IOException {
            this.signature = StreamUtils.readString(is, 4);
            this.version = StreamUtils.readInt(is);
            this.time = StreamUtils.readLong(is);
            this.name = StreamUtils.readDataString(is);
            int picWidth = StreamUtils.readInt(is);
            int picHeight = StreamUtils.readInt(is);
            this.picData = StreamUtils.readBytes(is, picWidth * picHeight);

            return this;
        }

        @Override
        public SaveHeader writeObject(OutputStream os) throws IOException {
            StreamUtils.writeString(os, signature, 4);
            StreamUtils.writeInt(os, version);
            StreamUtils.writeLong(os, time);
            StreamUtils.writeDataString(os, name);
            StreamUtils.writeInt(os, picWidth);
            StreamUtils.writeInt(os, picHeight);
            StreamUtils.writeBytes(os, picData);

            return this;
        }

        public String getSignature() {
            return signature;
        }

        public int getVersion() {
            return version;
        }

        public long getTime() {
            return time;
        }

        public String getName() {
            return name;
        }

        public boolean hasScreenshot() {
            return picWidth > 0 && picHeight > 0 && picData != null;
        }
    }
}
