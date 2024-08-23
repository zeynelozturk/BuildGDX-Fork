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

package ru.m210projects.Build.Pattern.CommonMenus;

import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.TexFilter;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.settings.GameConfig;

import java.util.stream.IntStream;

public abstract class MenuRendererSettings extends BuildMenuList {

    private final MenuItem title;
    public BuildGame app;
    public Font style;
    public int width;
    public boolean fontShadow = false;
    public boolean listShadow = false;

    protected BuildMenuList GLHiresMenu;
    protected MenuSlider palettedGamma;
    protected MenuSlider fovSlider;
    protected MenuSwitch vSync;
    protected MenuSwitch useVoxels;
    protected MenuConteiner fpsLimits;
    protected DummyItem separator;
    protected MenuConteiner GLTextureFilter;
    protected MenuSwitch GLUseHighTile;
    protected MenuSwitch GLUseModels;
    protected MenuButton GLHires;
    protected MenuSwitch GLPalette;

    private Renderer currentRenderer;
    private PixelFormat currentFormat;

    public MenuRendererSettings(final BuildGame app, int x, int y, int width, int step, Font style) {
        super(app, "Renderer settings", x, y, width, step, 15);

        this.app = app;
        this.style = style;
        this.width = width;
        this.title = m_pItems[0];
    }

    @Override
    public abstract MenuTitle getTitle(BuildGame app, String text);

    @Override
    public void mDraw(MenuHandler handler) {
        Renderer renderer = app.getRenderer();
        if (currentFormat != renderer.getTexFormat()) {
            rebuild();
        }
        super.mDraw(handler);
    }

    @Override
    public boolean mLoadRes(MenuHandler handler, MenuOpt opt) {
        Renderer renderer = app.getRenderer();
        if (opt == MenuOpt.Open && (currentRenderer != app.getRenderer() || currentFormat != renderer.getTexFormat())) {
            rebuild();
        }
        return super.mLoadRes(handler, opt);
    }

    protected void rebuild() {
        this.clear();
        currentRenderer = app.getRenderer();
        currentFormat = currentRenderer.getTexFormat();
        if (title != null) {
            title.text = (currentRenderer.getType().getName() + " settings").toCharArray();
        }

        BuildRenderParameters();
        if (!app.isSoftwareRenderer()) {
            BuildGLRenderParameters();
            this.addItem(palettedGamma, true);

            this.addItem(separator, false);
            this.addItem(fovSlider, false);
            this.addItem(vSync, false);
            this.addItem(fpsLimits, false);
            this.addItem(useVoxels, false);

            this.addItem(separator, false);
            this.addItem(GLPalette, false);
            this.addItem(GLHires, false);
        } else {
            this.addItem(palettedGamma, true);

            this.addItem(separator, false);
            this.addItem(fovSlider, false);
            this.addItem(vSync, false);
            this.addItem(fpsLimits, false);
            this.addItem(useVoxels, false);
        }
    }

    protected MenuConteiner BuildConteiner(String text, final SettingsListProvider settingsProvider) {
        MenuConteiner conteiner = new MenuConteiner(text, style, 0, 0, width, settingsProvider.getValues(), 0,
                (h, pItem) -> settingsProvider.setValue(((MenuConteiner) pItem).num)) {
            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(settingsProvider.checkEnable());
                this.num = settingsProvider.getValue();
                super.draw(handler);
            }
        };
        conteiner.fontShadow = fontShadow;
        conteiner.listShadow = listShadow;
        return conteiner;
    }

    protected MenuSlider BuildSlider(String text, SettingsSliderProvider var) {
        MenuSlider slider = new MenuSlider(app.pSlider, text, style, 0, 0, width, 0, var.getMinValue(), var.getMaxValue(), var.getStep(), (handler, pItem) -> var.setValue(((MenuSlider) pItem).value), true) {
            @Override
            public void draw(MenuHandler handler) {
                this.value = var.getValue();
                super.draw(handler);
            }
        };
        slider.fontShadow = fontShadow;
        return slider;
    }

    protected MenuSwitch BuildSwitch(String text, SettingsProvider<Boolean> provider) {
        MenuSwitch sw = new MenuSwitch(text, style, 0, 0, width, false,
                (h, pItem) -> provider.setValue(((MenuSwitch) pItem).value), null, null) {
            @Override
            public void draw(MenuHandler handler) {
                this.value = provider.getValue();
                super.draw(handler);
            }
        };

        sw.fontShadow = fontShadow;
        return sw;
    }

    protected MenuButton BuildButton(MenuProc callback) {
        MenuButton sw = new MenuButton("Hires settings", style, 0, 0, width, 0, 0, null, 0, callback, -1);
        sw.fontShadow = fontShadow;
        return sw;
    }

    protected void BuildGLRenderParameters() {
        GLHires = BuildButton((handler, p) -> handler.mOpen(GLHiresMenu, -1));
        GLPalette = BuildSwitch("Palette emulation", new PaletteEmulationSettingsProvider(app.pCfg));

        { // Hires menu
            GLHiresMenu = new BuildMenuList(app, "Hires settings", this.list.x, this.list.y, width, this.list.mFontOffset(), 10) {
                @Override
                public MenuTitle getTitle(BuildGame app, String text) {
                    return MenuRendererSettings.this.getTitle(app, text);
                }
            };
            GLTextureFilter = BuildConteiner("Texture filtering", new TextureFilterSettingsProvider(app.pCfg));
            GLUseHighTile = BuildSwitch("True color textures", new UseHighTileSettingsProvider(app.pCfg));
            GLUseModels = BuildSwitch("3d models", new UseModelsSettingsProvider(app.pCfg));
            GLHiresMenu.addItem(GLTextureFilter, true);
            GLHiresMenu.addItem(GLUseHighTile, false);
            GLHiresMenu.addItem(GLUseModels, false);
        }
    }

    protected void BuildRenderParameters() {
        separator = new DummyItem();
        palettedGamma = BuildSlider("Gamma", new GammaSettingsProvider(app));
        fovSlider = BuildSlider("Field of view", new FovSettingsProvider(app.pCfg));
        vSync = BuildSwitch("Vsync", new VsyncSettingsProvider(app.pCfg));
        fpsLimits = BuildConteiner("Framerate limit", new FpsSettingsProvider(app.pCfg));
        useVoxels = BuildSwitch("Voxels", new VoxelSettingsProvider(app.pCfg));
    }

    public interface SettingsProvider<T> {
        T getValue();

        void setValue(T value);

        default boolean checkEnable() {
            return true;
        }
    }

    public interface SettingsListProvider extends SettingsProvider<Integer> {
        String[] getValues();
    }

    public interface SettingsSliderProvider extends SettingsProvider<Integer> {
        int getMinValue();

        int getMaxValue();

        int getStep();
    }

    private static class UseHighTileSettingsProvider implements SettingsProvider<Boolean> {
        private final GameConfig config;

        public UseHighTileSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Boolean getValue() {
            return config.isUseHighTiles();
        }

        @Override
        public void setValue(Boolean value) {
            config.setUseHighTiles(value);
        }
    }

    private static class UseModelsSettingsProvider implements SettingsProvider<Boolean> {
        private final GameConfig config;

        public UseModelsSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Boolean getValue() {
            return config.isUseModels();
        }

        @Override
        public void setValue(Boolean value) {
            config.setUseModels(value);
        }
    }

    private static class PaletteEmulationSettingsProvider implements SettingsProvider<Boolean> {
        private final GameConfig config;

        public PaletteEmulationSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Boolean getValue() {
            return config.isPaletteEmulation();
        }

        @Override
        public void setValue(Boolean value) {
            config.setPaletteEmulation(value);
        }
    }

    private static class GammaSettingsProvider implements SettingsSliderProvider {
        private final BuildGame app;

        public GammaSettingsProvider(BuildGame app) {
            this.app = app;
        }

        @Override
        public Integer getValue() {
            return app.pCfg.getPaletteGamma();
        }

        @Override
        public void setValue(Integer value) {
            app.pCfg.setPaletteGamma(value);
            app.pEngine.setbrightness(app.pCfg.getPaletteGamma(), app.pEngine.getPaletteManager().getBasePalette());
        }

        @Override
        public int getMinValue() {
            return 0;
        }

        @Override
        public int getMaxValue() {
            return 15;
        }

        @Override
        public int getStep() {
            return 1;
        }
    }

    private static class FovSettingsProvider implements SettingsSliderProvider {
        private final GameConfig config;

        public FovSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Integer getValue() {
            return config.getgFov();
        }

        @Override
        public void setValue(Integer value) {
            config.setgFov(value);
        }

        @Override
        public int getMinValue() {
            return 60;
        }

        @Override
        public int getMaxValue() {
            return 140;
        }

        @Override
        public int getStep() {
            return 5;
        }
    }

    private static class VsyncSettingsProvider implements SettingsProvider<Boolean> {
        private final GameConfig config;

        public VsyncSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Boolean getValue() {
            return config.isVSync();
        }

        @Override
        public void setValue(Boolean value) {
            config.setgVSync(value);
        }
    }

    private static class VoxelSettingsProvider implements SettingsProvider<Boolean> {
        private final GameConfig config;

        public VoxelSettingsProvider(GameConfig config) {
            this.config = config;
        }

        @Override
        public Boolean getValue() {
            return config.isUseVoxels();
        }

        @Override
        public void setValue(Boolean value) {
            config.setUseVoxels(value);
        }
    }

    private static class FpsSettingsProvider implements SettingsListProvider {

        private final Integer[] fpslimits = {0, 30, 60, 120, 144, 240, 320, 480};
        private final String[] values;
        private final GameConfig config;

        public FpsSettingsProvider(GameConfig config) {
            this.config = config;

            values = new String[fpslimits.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = i == 0 ? "None" : fpslimits[i] + " fps";
            }
        }

        @Override
        public Integer getValue() {
            int currentFps = config.getFpslimit();
            return IntStream.range(0, values.length).boxed().filter(e -> fpslimits[e] == currentFps).findAny().orElse(-1);
        }

        @Override
        public void setValue(Integer value) {
            config.setFpslimit(fpslimits[value]);
        }

        @Override
        public String[] getValues() {
            return values;
        }
    }

    private static class TextureFilterSettingsProvider implements SettingsListProvider {
        private final GameConfig gameConfig;
        private final String[] filters;

        public TextureFilterSettingsProvider(GameConfig gameConfig) {
            this.gameConfig = gameConfig;
            this.filters = new String[TexFilter.values().length];

            for (int i = 0; i < filters.length; i++) {
                filters[i] = TexFilter.valueOf(i).name();
            }
        }

        @Override
        public boolean checkEnable() {
            return true; //!gameConfig.isPaletteEmulation();
        }

        @Override
        public Integer getValue() {
            TexFilter currentFilter = gameConfig.getGlfilter();
            return IntStream.range(0, filters.length).boxed().filter(e -> TexFilter.valueOf(e).equals(currentFilter)).findAny().orElse(-1);
        }

        @Override
        public void setValue(Integer value) {
            gameConfig.setGlfilter(TexFilter.valueOf(value));
        }

        @Override
        public String[] getValues() {
            return filters;
        }
    }

}
