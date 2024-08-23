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

package ru.m210projects.Build.Architecture;

public class DialogUtil {

    private static DialogBox dialogBox;

    public static void init(DialogBox dialogBox) {
        DialogUtil.dialogBox = dialogBox;
    }

    public static DialogResult showMessage(String header, String message, MessageType messageType) {
        return dialogBox.showMessage(header, message, messageType);
    }

    public interface DialogBox {
        DialogResult showMessage(String header, String message, MessageType messageType);
    }

    public static class DialogResult {
        private final String comment;
        private final boolean approved;

        public DialogResult(String comment, boolean approved) {
            this.comment = comment;
            this.approved = approved;
        }

        public String getComment() {
            return comment;
        }

        public boolean isApproved() {
            return approved;
        }

        @Override
        public String toString() {
            return "DialogResult{" +
                    "comment='" + comment + '\'' +
                    ", approved=" + approved +
                    '}';
        }
    }

}
