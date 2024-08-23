// This file is part of BuildGDX.
// Copyright (C) 2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Serializable<T> extends java.io.Serializable {

    T readObject(InputStream is) throws IOException;

    T writeObject(OutputStream os) throws IOException;

    /*
    java.io.Serializable :
    Classes that require special handling during the serialization and
    deserialization process must implement special methods with these exact signatures:

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException;
    private void readObjectNoData() throws ObjectStreamException;
     */
}
