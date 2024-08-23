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

package com.badlogic.gdx.backends.lwjgl3;

import com.badlogic.gdx.backends.LwjglApplicationContext;
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Platform;
import ru.m210projects.Build.Architecture.common.RenderChanger;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Render.Types.GL10;

import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;

public class Lwjgl3ApplicationGL10 extends Lwjgl3Application {

    private RenderChanger gameListener;

    public Lwjgl3ApplicationGL10(RenderChanger listener, Lwjgl3ApplicationConfiguration config) {
        super(listener, config);
    }

    @Override
    protected void createWindow(Lwjgl3Window window, Lwjgl3ApplicationConfiguration config, long sharedContext) {
        this.gameListener = ((RenderChanger) window.listener);

        long windowHandle;
        try {
            windowHandle = createGlfwWindow(config, sharedContext);
            // LibGDX's stupidest exception can be caught
        } catch (GdxRuntimeException e) {
            windowHandle = GLFW.glfwGetCurrentContext();
            if (windowHandle == 0) {
                throw new GdxRuntimeException(e);
            }
        }

        window.create(windowHandle);
        window.setVisible(config.initialVisible);

        GL10.instance = new LwjglGL10();
        this.gameListener.setApplicationContext(new LwjglApplicationContext(this));
        BuildGame game = gameListener.getGame();
        game.setRenderer(game.getFactory().renderer(game.pCfg.getRenderType()));

        if (Platform.get() == Platform.MACOSX) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        }
        
        for (int i = 0; i < 2; i++) {
            GL11.glClearColor(config.initialBackgroundColor.r, config.initialBackgroundColor.g, config.initialBackgroundColor.b, config.initialBackgroundColor.a);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GLFW.glfwSwapBuffers(windowHandle);
        }
    }

    @Override
    protected void cleanup() {
        // to avoid GLFW reinit bug
        if (gameListener.isRendererChanging()) {
            return;
        }
        super.cleanup();
    }

    @Override
    public Lwjgl3Input createInput(Lwjgl3Window window) {
        if (Platform.get() == Platform.MACOSX) {
            return new DefaultLwjgl3Input(window);
        }
        return new RawMouseLwjgl3Input(window);
    }

    @Override
    public Lwjgl3Audio createAudio(Lwjgl3ApplicationConfiguration config) {
        return new MockAudio();
    }
}
