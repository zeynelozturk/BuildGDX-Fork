//This file is part of BuildGDX.
//Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Pattern.ScreenAdapters;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import ru.m210projects.Build.Architecture.MessageType;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.settings.GameKeys;

import java.util.ArrayList;
import java.util.List;

public abstract class MessageScreen extends ScreenAdapter implements InputListener {

    protected final BuildGame game;
    protected final String header, message;
    protected final MessageType type;
    protected MessageListener messageListener;
    protected float gTicks;
    protected float gShowTime;
    protected boolean maySkipped;

    protected final List<MenuItem> messageItems = new ArrayList<>();
    protected final List<MenuItem> variantItems = new ArrayList<>();
    protected final MenuVariants variants;

    public MessageScreen(BuildGame game, String header, String message, Font headerFont, Font messageFont, MessageType type) {
        this.game = game;
        this.header = header;
        this.message = message;
        this.type = type;

        messageItems.add(new MenuText(header, headerFont, 160, 40, 1).setFontShadow(true));
        int y = 55;
        String[] lines = message.split("\r");
        for(String line : lines) {
            messageItems.add(new MenuText(line, messageFont, 160, y += messageFont.getSize(), 1));
            if (y > 100) {
                break;
            }
        }
        y += messageFont.getSize();

        variantItems.add(new MenuText(type == MessageType.Info ? "Press any key" : "Do you want to sent a report?", messageFont, 160, y += messageFont.getSize(), 1));
        variantItems.add(variants = new MenuVariants(game.pEngine, type == MessageType.Info ? "to continue" : "[Y/N]", messageFont, 160, y += 2 * messageFont.getSize()) {
            @Override
            public boolean isFocused() {
                return true;
            }

            @Override
            public void positive(MenuHandler menu) {
                onApproved();
            }

            @Override
            public void negative(MenuHandler handler) {
                onCancel();
            }
        });
        y += 2 * messageFont.getSize();

        if (type != MessageType.Info) {
            variantItems.add(new MenuText("You may leave a comment", messageFont, 160, y += messageFont.getSize(), 1));
            variantItems.add(new MenuText("in the console before sending.", messageFont, 160, y + messageFont.getSize(), 1));
        }

        BuildMenu dummyMenu = new BuildMenu(game.pMenu);
        for (MenuItem item : messageItems) {
            item.m_pMenu = dummyMenu;
        }

        for (MenuItem item : variantItems) {
            item.m_pMenu = dummyMenu;
        }

        this.gShowTime = 1.0f;
        this.gTicks = 0;
        this.maySkipped = false;
    }

    public abstract void drawBackground(Renderer renderer);

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    @Override
    public void show() {
        game.pNet.ready2send = false;
        game.getProcessor().resetPollingStates();
    }

    @Override
    public void render(float delta) {
        Renderer renderer = game.getRenderer();
        renderer.clearview(0);

        // text: you might open console and write a message, what's happens.
        drawBackground(renderer);
        for (MenuItem item : messageItems) {
            item.draw(game.pMenu);
        }

        if (maySkipped) {
            for (MenuItem item : variantItems) {
                item.draw(game.pMenu);
            }
        }

        if ((gTicks += delta) >= gShowTime) {
            maySkipped = true;
        }

        game.pEngine.nextpage(delta);
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        if (GameKeys.Show_Console.equals(gameKey)) {
            Console.out.onToggle();
            return true;
        }

        if (!maySkipped) {
            return true;
        }

        if (type == MessageType.Info) {
            variants.keyDown(Input.Keys.ENTER);
            return true;
        }

        if (GameKeys.Open.equals(gameKey)) {
            return variants.keyDown(Input.Keys.ENTER);
        } else if (GameKeys.Menu_Toggle.equals(gameKey)) {
            return variants.keyDown(Input.Keys.ESCAPE);
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!maySkipped) {
            return true;
        }

        if (type == MessageType.Info) {
            variants.keyDown(Input.Keys.ENTER);
            return true;
        }

        variants.keyDown(keycode);
        // block other listeners
        return true;
    }

    protected void onApproved() {
        if (messageListener != null) {
            messageListener.onClose(type != MessageType.Info);
            return;
        }

        // if there is no listener, just set previous screen
        if (game.getScreen() instanceof MessageScreen) {
            game.show();
        } else {
            game.setPrevScreen();
        }
    }

    protected void onCancel() {
        if (type == MessageType.Info) {
            onApproved();
            return;
        }

        if (messageListener != null) {
            messageListener.onClose(false);
            return;
        }

        // if there is no listener, just set previous screen
        if (game.getScreen() instanceof MessageScreen) {
            game.show();
        } else {
            game.setPrevScreen();
        }
    }

    public interface MessageListener {
        void onClose(boolean approved);
    }
}
