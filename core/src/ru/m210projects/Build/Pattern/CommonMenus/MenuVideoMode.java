//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Pattern.CommonMenus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import ru.m210projects.Build.Architecture.common.ResolutionUtils;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Render.Renderer.RenderType;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.settings.GameConfig;

import java.util.List;

public abstract class MenuVideoMode extends BuildMenu {

    protected final BuildMenu mResList;
    protected final BuildMenu mRenSettingsMenu;
    protected GameConfig config;
    protected MenuConteiner mResolution;
    protected MenuConteiner mRenderer;
    protected MenuButton mRenderSettings;
    protected MenuSwitch mFullscreen;
    protected MenuButton mApplyChanges;
    protected MenuResolutionList mSlot;
    protected MenuScroller slider;
    protected Graphics.DisplayMode currentMode;
    protected boolean isFullscreen;
    protected RenderType currentRender;
    protected RenderType choosedRender;

    public MenuVideoMode(final BuildGame app, int posx, int posy, int width, int itemHeight, Font style, int nListItems, int nListWidth, int nBackground) {
        super(app.pMenu);
        this.config = app.pCfg;
        addItem(getTitle(app, "Video mode"), false);

        final GameConfig cfg = app.pCfg;
        MenuProc applyButtonCallback = (handler, pItem) -> Gdx.app.postRunnable(() -> {
            Graphics.DisplayMode displayMode = mSlot.getSelectedMode();

            if (currentRender != choosedRender) {
                cfg.setRenderType(choosedRender);
                currentRender = choosedRender;
            } else {
                cfg.setScreenMode(displayMode.width, displayMode.height, isFullscreen);
            }
        });

        mResList = getResolutionListMenu(this, app, posx + (width - nListWidth) / 2, posy + 2 * style.getSize(), nListWidth, nListItems, style, nBackground);

        mRenSettingsMenu = getRenSettingsMenu(app, posx, posy, width, itemHeight, style);

        mResolution = new MenuConteiner("Resolution", style, posx,
                posy += itemHeight, width, null, 0, null) {
            @Override
            public boolean callback(MenuHandler handler, MenuOpt opt) {
                switch (opt) {
                    case LEFT:
                    case MWDW:
                        if (mSlot.l_nFocus > 0) {
                            mSlot.l_nFocus--;
                        } else {
                            mSlot.l_nFocus = 0;
                        }
                        return false;
                    case RIGHT:
                    case MWUP:
                        if (mSlot.l_nFocus < mSlot.len - 1) {
                            mSlot.l_nFocus++;
                        } else {
                            mSlot.l_nFocus = mSlot.len - 1;
                        }
                        return false;
                    case ENTER:
                    case LMB:
                        handler.mOpen(mResList, -1);
                        return false;
                    default:
                        return m_pMenu.mNavigation(opt);
                }
            }

            @Override
            public void open() {
                mSlot.open();
                currentMode = mSlot.getSelectedMode();
                num = mSlot.l_nFocus;
            }

            @Override
            public void draw(MenuHandler handler) {
                int px = x, py = y;

                String key = ResolutionUtils.getDisplayModeAsString(mSlot.getSelectedMode());
                int pal = handler.getPal(font, this);
                int shade = handler.getShade(this);
                font.drawTextScaled(handler.getRenderer(), px, py, text, 1.0f, shade, pal, TextAlign.Left, Transparent.None, ConvertType.Normal, fontShadow);

                listFont.drawTextScaled(handler.getRenderer(), x + width - 1, py, key, 1.0f, shade, handler.getPal(listFont, this), TextAlign.Right, Transparent.None, ConvertType.Normal, listShadow);

                handler.mPostDraw(this);
            }
        };

        MenuProc renderCallback = (handler, pItem) -> Gdx.app.postRunnable(() -> {
            MenuConteiner item = (MenuConteiner) pItem;
            switch (item.num) {
                case 0:
                    choosedRender = RenderType.Software;
                    break;
                case 1:
                    choosedRender = RenderType.Polymost;
                    break;
                case 2:
                    choosedRender = RenderType.PolyGDX;
                    break;
            }
        });

        String[] renderers = new String[]{
                RenderType.Software.getName(),
                RenderType.Polymost.getName(),
                RenderType.PolyGDX.getName()
        };
        mRenderer = new MenuConteiner("Renderer", style, posx, posy += itemHeight, width, renderers, 0, renderCallback) {
            @Override
            public void open() {
                choosedRender = currentRender = app.getRenderer().getType();
                switch (currentRender) {
                    case Software:
                        num = 0;
                        break;
                    case Polymost:
                        num = 1;
                        break;
                    case PolyGDX:
                        num = 2;
                        break;
                }
            }
        };

        mFullscreen = new MenuSwitch("Fullscreen", style, posx,
                posy += itemHeight, width, cfg.isFullscreen(), new MenuProc() {
            @Override
            public void run(MenuHandler handler, MenuItem pItem) {
                MenuSwitch sw = (MenuSwitch) pItem;
                isFullscreen = sw.value;
            }
        }, null, null) {
            @Override
            public void open() {
                value = isFullscreen = (cfg.isFullscreen());
            }

            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(mSlot.l_nFocus != -1);
                super.draw(handler);
            }
        };

        mRenderSettings = new MenuButton("Renderer settings", style, posx, posy += itemHeight, width, 0, 0, mRenSettingsMenu, -1, null, 0);

        mApplyChanges = new MenuButton("Apply changes", style, 0, posy += 2 * itemHeight, 320, 1, 0, null, -1, applyButtonCallback, 0) {
            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem((!ResolutionUtils.getDisplayModeAsString(mSlot.getSelectedMode()).equalsIgnoreCase(ResolutionUtils.getDisplayModeAsString(currentMode)) || (isFullscreen != cfg.isFullscreen() && mSlot.l_nFocus != -1) || currentRender != choosedRender));
                super.draw(handler);
            }

            @Override
            public void mCheckEnableItem(boolean nEnable) {
                if (nEnable) {
                    flags = 3 | 4;
                } else {
                    flags = 3;
                }
            }
        };

        addItem(mResolution, true);
        addItem(mRenderer, false);
        addItem(mFullscreen, false);
        addItem(mRenderSettings, false);
        addItem(mApplyChanges, false);
    }

    public abstract MenuTitle getTitle(BuildGame app, String text);

//    public abstract void setDisplayMode(int width, int height);

    public BuildMenu getResolutionListMenu(final MenuVideoMode parent, final BuildGame app, int posx, int posy, int width, int nListItems, Font style, int nListBackground) {
        BuildMenu menu = new BuildMenu(app.pMenu);

        menu.addItem(parent.getTitle(app, "Resolution"), false);
        int bpp = Gdx.graphics.getDisplayMode().bitsPerPixel;

        List<Graphics.DisplayMode> resolutions = ResolutionUtils.getBestDisplayModes(app.pCfg.getResolutions(), bpp);

        MenuProc resolutionListApplyCallback = (handler, pItem) -> {
            final MenuResolutionList item = (MenuResolutionList) pItem;
            if (item.l_nFocus == -1) {
                return;
            }

            Gdx.app.postRunnable(() -> {
                Graphics.DisplayMode selectedMode = item.getSelectedMode();
                app.pCfg.setScreenMode(selectedMode.width, selectedMode.height, app.pCfg.isFullscreen());
                parent.mLoadRes(app.pMenu, MenuOpt.Open);
                app.pMenu.mMenuBack();
            });
        };

        mSlot = new MenuResolutionList(app, resolutions, style, posx, posy, width, 1, resolutionListApplyCallback, nListItems, nListBackground);

        slider = new MenuScroller(app.pSlider, mSlot, width + posx - app.pSlider.getScrollerWidth());

        menu.addItem(mSlot, true);
        menu.addItem(slider, false);

        return menu;
    }

    public abstract MenuRendererSettings getRenSettingsMenu(final BuildGame app, int posx, int posy, int width, int nHeight, Font style);

    public void onResize(int width, int height) {
        // reinit resolution
//        currentMode = ResolutionUtils.getDisplayModeAsString(width, height);
        mResolution.open();
    }

    public void onRenderChanged(RenderType renderType) {
        int bpp = Gdx.graphics.getDisplayMode().bitsPerPixel;

        mSlot.setDisplayModes(ResolutionUtils.getBestDisplayModes(config.getResolutions(), bpp));
    }
}
