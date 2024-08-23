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

package com.badlogic.gdx.backends.awt;

import ru.m210projects.Build.Architecture.DialogUtil;
import ru.m210projects.Build.Architecture.MessageType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AWTDialog implements DialogUtil.DialogBox {

    private final List<Image> icons = new ArrayList<>();

    public AWTDialog(String[] iconPaths) {
        if (iconPaths != null) {
            for(String icon : iconPaths) {
                icons.add(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource(icon)));
            }
        }
    }

    public void setIconImages(List<Image> icons) {
        this.icons.addAll(icons);
    }

    @Override
    public DialogUtil.DialogResult showMessage(String header, String message, MessageType type) {
        int messageType = JOptionPane.INFORMATION_MESSAGE;
        int optionType = JOptionPane.DEFAULT_OPTION;
        switch (type) {
            case Question:
                messageType = JOptionPane.QUESTION_MESSAGE;
                optionType = JOptionPane.YES_NO_OPTION;
                break;
            case Error:
                messageType = JOptionPane.ERROR_MESSAGE;
                break;
            case Crash:
                messageType = JOptionPane.ERROR_MESSAGE;
                optionType = JOptionPane.YES_NO_OPTION;
                break;
        }

        JOptionPane panel = new JOptionPane();
        panel.setMessageType(messageType);
        if (type == MessageType.Crash) {
            panel.setMessage(message + "\r\n" + "You might leave comment what's happen.");
            panel.setWantsInput(true);
        } else {
            panel.setMessage(message);
        }
        panel.setOptionType(optionType);
        final JDialog dialog = panel.createDialog(header);
        if (!icons.isEmpty()) {
            dialog.setIconImages(icons);
        }
        panel.setBackground(dialog.getBackground());
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);
        // waiting for answer here
        dialog.dispose();

        String comment = "";
        boolean approved = false;
        if (Objects.equals(JOptionPane.YES_OPTION, panel.getValue())) {
            Object inputValue = panel.getInputValue();
            if (inputValue instanceof String) {
                comment = (String) inputValue;
            }
            approved = true;
        }

        return new DialogUtil.DialogResult(comment, approved);
    }

}
