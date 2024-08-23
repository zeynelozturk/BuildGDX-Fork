//This file is part of BuildGDX.
//Copyright (C) 2017-2020  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.MenuItems;

import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Pattern.Tools.NaturalComparator;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static ru.m210projects.Build.Gameutils.*;

public abstract class MenuFileBrowser extends MenuItem implements ScrollableMenuItem {

    protected final BuildGame app;
    protected final int nListItems;
    protected final int nItemHeight;
    private final int DIRECTORY = 0; // left part of filebrowser
    private final int FILE = 1; // right part of filebrowser
    private final int[] scrollX = new int[2];
    private final SliderDrawable slider;
    public String back = "..";
    public boolean[] scrollTouch = new boolean[2];
    public String path;
    public int topPal, pathPal, listPal, backgroundPal;
    public int transparent = 1;
    protected List<FileEntry> fileList;
    protected List<Directory> dirList;

    protected int oldEntryCount; // #GDX 16.07.2024 to trig refresh
    protected char[] dirs = "Directories".toCharArray();
    protected char[] ffs = "Files".toCharArray();
    protected int[] l_nMin;
    protected int[] l_nFocus;
    protected int currColumn;
    protected Directory currDir;
    protected Font topFont, pathFont;
    protected char[] buffer = new char[40];
    private final Map<String, ExtProp> extensionProperties = new HashMap<>();
    private final Map<Class<?>, ExtProp> classProperties = new HashMap<>();
    private final Comparator<FileEntry> fileComparator = (o1, o2) -> {
        ExtProp p1 = getProperty(o1);
        ExtProp p2 = getProperty(o2);
        if (p1 != null && p2 != null) {
            int c = p2.priority - p1.priority;
            if (c != 0) {
                return c;
            }
        }
        return o1.compareTo(o2);
    };
    private final int nBackground;
    private int scrollerHeight;
    private long checkDirectory; // checking for new files

    public MenuFileBrowser(BuildGame app, Font font, Font topFont, Font pathFont, int x, int y, int width, int nItemHeight, int nListItems, int nBackground) {
        super(null, font);

        this.dirList = new ArrayList<>();
        this.fileList = new ArrayList<>();

        this.flags = 3 | 4;
        this.app = app;
        this.slider = app.pSlider;

        this.x = x;
        this.y = y;
        this.width = width;
        this.nItemHeight = nItemHeight;
        this.nListItems = nListItems;
        this.topFont = topFont;
        this.pathFont = pathFont;
        this.nBackground = nBackground;

        this.l_nMin = new int[2];
        this.l_nFocus = new int[2];
        this.currColumn = FILE;

        init();
    }

    public void registerExtension(String ext, int pal, int priority) {
        extensionProperties.put(ext.toUpperCase(), new ExtProp(pal, priority));
    }

    public void registerClass(Class<?> cl, int pal, int priority) {
        classProperties.put(cl, new ExtProp(pal, priority));
    }

    public abstract void init();

    public abstract void handleFile(FileEntry file);

    public abstract void invoke(FileEntry fil);

    public abstract void handleDirectory(Directory dir);

    public int getListSize(int column) {
        if (column == FILE) {
            return fileList.size();
        }
        return dirList.size();
    }

    public String getFileName() {
        return fileList.get(l_nFocus[FILE]).getName();
    }

    public Directory getDirectory() {
        return currDir;
    }

    public int mFontOffset() {
        return font.getSize() + nItemHeight;
    }

    private void changeDir(Directory dir) {
        if (dir instanceof BackDirectory) {
            dir = ((BackDirectory) dir).getDirectory();
        }

        if (dir.equals(Directory.DUMMY_DIRECTORY) || currDir == dir && !dir.revalidate()) {
            return;
        }

        dirList.clear();
        fileList.clear();
        oldEntryCount = dir.getSize();

        currDir = dir;
        path = File.separator;
        if (!app.getCache().isGameDirectory(currDir)) {
            Path relativePath = currDir.getDirectoryEntry().getRelativePath();
            path += relativePath;
        }

        for (Entry entry : dir.getEntries()) {
            if (entry instanceof FileEntry && entry.isDirectory()) {
                dirList.add(((FileEntry) entry).getDirectory());
            }
        }

        dirList.sort((a, b) -> NaturalComparator.compare(a.getName(), b.getName()));
        if (!app.getCache().isGameDirectory(dir)) {
            dirList.add(0, getBackDirectory(currDir));
        }

        try {
            handleDirectory(dir);
        } catch (Exception ignore) {
            Console.out.println("Can't handle directory: " + dir.getName(), OsdColor.RED);
        }

        for (Entry entry : dir.getEntries()) {
            if (entry instanceof FileEntry && !entry.isDirectory()) {
                if (extensionProperties.get(entry.getExtension()) != null) {
                    try {
                        handleFile((FileEntry) entry);
                    } catch (Exception ignore) {
                        Console.out.println("Can't handle file: " + entry.getName(), OsdColor.RED);
                    }
                }
            }
        }

        sortFiles();

        l_nFocus[DIRECTORY] = l_nMin[DIRECTORY] = 0;
        l_nFocus[FILE] = l_nMin[FILE] = 0;
    }

    public void addFile(FileEntry file) {
        fileList.add(file);
    }

    public void sortFiles() {
        fileList.sort(fileComparator);
    }

    protected void drawHeader(Renderer renderer, int x1, int x2, int y) {
        /* directories */
        topFont.drawTextScaled(renderer, x1, y, dirs, 1.0f, -32, topPal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
        /* files */
        topFont.drawTextScaled(renderer, x2, y, ffs, 1.0f, -32, topPal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
    }

    protected void drawPath(Renderer renderer, int x, int y, String path) {
        font.drawTextScaled(renderer, x, y, calcTextBounds(path, this.width - (2 * slider.getScrollerWidth()) - 7), 1.0f, -32, pathPal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
    }

    protected String calcTextBounds(String text, int allowWidth) {
        int textWidth = font.getWidth(text, 1.0f);
        if (allowWidth < textWidth) {
            int delta = textWidth - allowWidth;
            int symbols = delta / font.getCharInfo('a').getCellSize();

            text = text.substring(symbols);
            if (text.length() > 6) {
                text = "..." + text.substring(3);
            }
        }

        return text;
    }

    @Override
    public void draw(MenuHandler handler) {
        int yColNames = y + 3;
        int yPath = yColNames + topFont.getSize() + 2;
        int yList = yPath + pathFont.getSize() + 2;
        int scrollerWidth = slider.getScrollerWidth();

        handler.game.getRenderer().rotatesprite(x << 16, y << 16, 65536, 0, nBackground, 127, backgroundPal, 10 | 16 | transparent, 0, 0, coordsConvertXScaled(x + width, ConvertType.Normal), coordsConvertYScaled(yList + nListItems * mFontOffset() + 6));

        int px = x + 3;
        drawHeader(handler.getRenderer(), px, x - 3 + width - topFont.getWidth(ffs, 1.0f), yColNames);
        px += scrollerWidth + 3;
        drawPath(handler.getRenderer(), px, yPath, "path: " + path);

        int py = yList;
        for (int i = l_nMin[DIRECTORY]; i >= 0 && i < l_nMin[DIRECTORY] + nListItems && i < dirList.size(); i++) {
            int pal = listPal; // handler.getPal(font, item); //listPal;
            int shade = handler.getShade(currColumn == DIRECTORY && i == l_nFocus[DIRECTORY] ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
            if (currColumn == DIRECTORY && i == l_nFocus[DIRECTORY]) {
                pal = handler.getPal(font, m_pMenu.m_pItems[m_pMenu.m_nFocus]);
            }

            font.drawTextScaled(handler.getRenderer(), px, py, calcTextBounds(dirList.get(i).getName(), (this.width / 2) - slider.getScrollerWidth() - 4), 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);
            py += mFontOffset();
        }

        py = yList;
        for (int i = l_nMin[FILE]; i >= 0 && i < l_nMin[FILE] + nListItems && i < fileList.size(); i++) {
            int pal = listPal;
            if (currColumn == FILE && i == l_nFocus[FILE]) {
                pal = handler.getPal(font, m_pMenu.m_pItems[m_pMenu.m_nFocus]);
            }
            int shade = handler.getShade(currColumn == FILE && i == l_nFocus[FILE] ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);

            FileEntry obj = fileList.get(i);
            String text = obj.getName();
            ExtProp p = getProperty(obj);
            if (p != null) {
                int itemPal = p.pal;
                if (itemPal != 0) {
                    pal = itemPal;
                }
            }

            px = x + width - scrollerWidth - 5;
            font.drawTextScaled(handler.getRenderer(), px, py, calcTextBounds(text, (this.width / 2) - slider.getScrollerWidth()), 1.0f, shade, pal, TextAlign.Right, Transparent.None, ConvertType.Normal, fontShadow);
            py += mFontOffset();
        }

        scrollerHeight = nListItems * mFontOffset();

        // Files scroll
        int nList = BClipLow(fileList.size() - nListItems, 1);
        int posy = yList + (scrollerHeight - slider.getScrollerHeight()) * l_nMin[FILE] / nList;

        scrollX[FILE] = x + width - scrollerWidth - 1;
        slider.drawScrollerBackground(scrollX[FILE], yList, scrollerHeight, 0, 0);
        slider.drawScroller(scrollX[FILE], posy, handler.getShade(currColumn == FILE ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null), 0);

        // Directory scroll
        nList = BClipLow(dirList.size() - nListItems, 1);
        posy = yList + (scrollerHeight - slider.getScrollerHeight()) * l_nMin[DIRECTORY] / nList;

        scrollX[DIRECTORY] = x + 2;
        slider.drawScrollerBackground(scrollX[DIRECTORY], yList, scrollerHeight, 0, 0);
        slider.drawScroller(scrollX[DIRECTORY], posy, handler.getShade(currColumn == DIRECTORY ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null), 0);

        if (System.currentTimeMillis() - checkDirectory >= 2000) {
            if (currDir.revalidate() /*|| oldEntryCount != currDir.getSize()*/) {
                refreshList();
            }
            checkDirectory = System.currentTimeMillis();
        }

        handler.mPostDraw(this);
    }

    public String getText(int column, int index) {
        if (column == DIRECTORY) {
            return calcTextBounds(dirList.get(index).getName(), (this.width / 2) - slider.getScrollerWidth() - 4);
        } else if (column == FILE) {
            return calcTextBounds(fileList.get(index).getName(), (this.width / 2) - slider.getScrollerWidth());
        }
        return "";
    }

    public int getFocus() {
        int focus = l_nFocus[currColumn];
        if (focus < l_nMin[currColumn] || focus >= l_nMin[currColumn] + getListSize(currColumn)) {
            return -1;
        }

        return l_nFocus[currColumn];
    }

    public int getMin() {
        return l_nMin[currColumn];
    }

    public int getRowCount() {
        return nListItems;
    }

    public int getColumn() {
        return currColumn;
    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        switch (opt) {
            case MWUP:
                if (l_nMin[currColumn] > 0) {
                    l_nMin[currColumn]--;
                }
                return false;
            case MWDW:
                if (l_nMin[currColumn] < getListSize(currColumn) - nListItems) {
                    l_nMin[currColumn]++;
                }
                return false;
            case UP:
                l_nFocus[currColumn]--;
                if (l_nFocus[currColumn] >= 0 && l_nFocus[currColumn] < l_nMin[currColumn]) {
                    if (l_nMin[currColumn] > 0) {
                        l_nMin[currColumn]--;
                    }
                }
                if (l_nFocus[currColumn] < 0) {
                    l_nFocus[currColumn] = getListSize(currColumn) - 1;
                    l_nMin[currColumn] = getListSize(currColumn) - nListItems;
                    if (l_nMin[currColumn] < 0) {
                        l_nMin[currColumn] = 0;
                    }
                }
                return false;
            case DW:
                l_nFocus[currColumn]++;
                if (l_nFocus[currColumn] >= l_nMin[currColumn] + nListItems && l_nFocus[currColumn] < getListSize(currColumn)) {
                    l_nMin[currColumn]++;
                }
                if (l_nFocus[currColumn] >= getListSize(currColumn)) {
                    l_nFocus[currColumn] = 0;
                    l_nMin[currColumn] = 0;
                }
                return false;
            case LEFT:
                if (!dirList.isEmpty()) {
                    currColumn = DIRECTORY;
                }
                return false;
            case RIGHT:
                if (!fileList.isEmpty()) {
                    currColumn = FILE;
                }
                return false;
            case ENTER:
            case LMB:
                if (!dirList.isEmpty() && currColumn == DIRECTORY) {
                    if (l_nFocus[DIRECTORY] == -1) {
                        return false;
                    }

                    changeDir(dirList.get(l_nFocus[DIRECTORY]));
                } else if (!fileList.isEmpty() && currColumn == FILE) {
                    if (l_nFocus[FILE] == -1) {
                        return false;
                    }

                    invoke(fileList.get(l_nFocus[FILE]));
                }
                return false;
            case ESC:
            case RMB:
                return true;
            case BSPACE:
                if (!app.getCache().isGameDirectory(currDir)) {
                    changeDir(getBackDirectory(currDir));
                }
                return false;
            case PGUP:
                l_nFocus[currColumn] -= (nListItems - 1);
                if (l_nFocus[currColumn] >= 0 && l_nFocus[currColumn] < l_nMin[currColumn]) {
                    if (l_nMin[currColumn] > 0) {
                        l_nMin[currColumn] -= (nListItems - 1);
                    }
                }
                if (l_nFocus[currColumn] < 0 || l_nMin[currColumn] < 0) {
                    l_nFocus[currColumn] = 0;
                    l_nMin[currColumn] = 0;
                }
                return false;
            case PGDW:
                l_nFocus[currColumn] += (nListItems - 1);
                if (l_nFocus[currColumn] >= l_nMin[currColumn] + nListItems && l_nFocus[currColumn] < getListSize(currColumn)) {
                    l_nMin[currColumn] += (nListItems - 1);
                }
                if (l_nFocus[currColumn] >= getListSize(currColumn) || l_nMin[currColumn] > getListSize(currColumn) - nListItems) {
                    l_nFocus[currColumn] = getListSize(currColumn) - 1;
                    if (getListSize(currColumn) >= nListItems) {
                        l_nMin[currColumn] = getListSize(currColumn) - nListItems;
                    } else if (l_nFocus[currColumn] >= l_nMin[currColumn] + nListItems) {
                        l_nMin[currColumn] = getListSize(currColumn) - 1;
                    }
                }
                return false;
            case HOME:
                l_nFocus[currColumn] = 0;
                l_nMin[currColumn] = 0;
                return false;
            case END:
                l_nFocus[currColumn] = getListSize(currColumn) - 1;
                if (getListSize(currColumn) >= nListItems) {
                    l_nMin[currColumn] = getListSize(currColumn) - nListItems;
                } else if (l_nFocus[currColumn] >= l_nMin[currColumn] + nListItems) {
                    l_nMin[currColumn] = getListSize(currColumn) - 1;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean mouseAction(int mx, int my) {
        if (mx >= x + width / 2) {
            currColumn = 1;
        } else {
            currColumn = 0;
        }

        if ((!scrollTouch[DIRECTORY] && !scrollTouch[FILE]) && getListSize(currColumn) > 0) {
            int py = y + 3 + pathFont.getSize() + 2 + topFont.getSize() + 2;

            for (int i = l_nMin[currColumn]; i >= 0 && i < l_nMin[currColumn] + nListItems && i < getListSize(currColumn); i++) {
                if (mx > x && mx < scrollX[FILE]) {
                    if (my > py && my < py + font.getSize()) {
                        l_nFocus[currColumn] = i;
                        return true;
                    }
                }

                py += mFontOffset();
            }
        }
        return false;
    }

    private Directory getBackDirectory(Directory dir) {
        return new BackDirectory(dir.getDirectoryEntry().getParent());
    }

    private ExtProp getProperty(Object obj) {
        ExtProp extProp = classProperties.get(obj.getClass());
        if (extProp != null) {
            return extProp;
        }

        if (obj instanceof FileEntry) {
            return extensionProperties.get(((FileEntry) obj).getExtension());
        }

        return null;
    }

    public void refreshList() {
        Directory dir = currDir;
        currDir = null;
        changeDir(dir);
    }

    @Override
    public void open() {
        if (currDir == null) {
            changeDir(app.getCache().getGameDirectory());
        } else {
            changeDir(currDir);
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < 2; i++) {
            l_nFocus[i] = l_nMin[i] = 0;
        }
    }

    @Override
    public boolean onMoveSlider(MenuHandler handler, int mx, int my) {
        if (getListSize(currColumn) <= nListItems) {
            return false;
        }

        int nList = BClipLow(getListSize(currColumn) - nListItems, 1);
        int nRange = Math.max(1, scrollerHeight);

        int py = y + 3 + pathFont.getSize() + 2 + topFont.getSize() + 2;

        l_nFocus[currColumn] = -1;
        l_nMin[currColumn] = BClipRange(((my - py) * nList) / nRange, 0, nList);
        return true;
    }

    @Override
    public boolean onLockSlider(MenuHandler handler, int mx, int my) {
        if (mx >= x + width / 2) {
            currColumn = 1;
        } else {
            currColumn = 0;
        }

        if (mx > scrollX[currColumn] && mx < scrollX[currColumn] + slider.getScrollerWidth()) {
            scrollTouch[currColumn] = true;
            onMoveSlider(handler, mx, my);
            return true;
        }
        return false;
    }

    @Override
    public void onUnlockSlider() {
        scrollTouch[DIRECTORY] = false;
        scrollTouch[FILE] = false;
    }

    private static class ExtProp {
        int pal;
        int priority;

        public ExtProp(int pal, int priority) {
            this.pal = pal;
            this.priority = priority;
        }
    }

    private class BackDirectory extends Directory {
        private final Directory dir;

        public BackDirectory(Directory dir) {
            super();
            this.dir = dir;
        }

        @Override
        public String getName() {
            return back;
        }

        public Directory getDirectory() {
            return dir;
        }
    }

}
