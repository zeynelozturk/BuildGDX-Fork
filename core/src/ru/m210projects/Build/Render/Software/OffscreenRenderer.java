package ru.m210projects.Build.Render.Software;

import static ru.m210projects.Build.settings.GameConfig.DUMMY_CONFIG;

public class OffscreenRenderer extends Software {

    public OffscreenRenderer() {
        super(DUMMY_CONFIG);
    }

    @Override
    protected boolean isShowFPS() {
        return false;
    }

    @Override
    protected void updateFov() {
        /* nothing */
    }

    @Override
    protected boolean isWidescreen() {
        return false;
    }

    @Override
    protected boolean isUseVoxels() {
        return false;
    }

}
