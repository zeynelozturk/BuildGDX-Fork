// This file is part of BuildGDX.
// Copyright (C) 2017-2019  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Script;

import java.util.HashMap;
import java.util.Map;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Strhandler.toLowerCase;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

import com.badlogic.gdx.utils.Array;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Types.collections.DynamicArray;
import ru.m210projects.Build.filehandle.Entry;

public class Maphack extends Scriptfile {

	private long MapCRC;
	private final Array<Spriteext> spriteext = new DynamicArray<>(MAXSPRITESV7, Spriteext.class);

	private enum Token {
		MapCRC, Sprite,

		AngleOffset, XOffset, YOffset, ZOffset, NoModel,

		Error, EOF
    }

	private final static Map<String, Token> basetokens = new HashMap<String, Token>() {
		private static final long serialVersionUID = 1L;
		{
			put("sprite", Token.Sprite);
			put("crc32", Token.MapCRC);
		}
	};

	private final static Map<String, Token> sprite_tokens = new HashMap<String, Token>() {
		private static final long serialVersionUID = 1L;
		{
			put("angoff", Token.AngleOffset);
			put("mdxoff", Token.XOffset);
			put("mdyoff", Token.YOffset);
			put("mdzoff", Token.ZOffset);
			put("notmd", Token.NoModel);
		}
	};

	public Maphack() { // new maphack
		super("", DUMMY_ENTRY);
	}

	public Maphack(Entry entry) {
		super(entry.getName(), entry);

		Integer value;
		while (true) {
			switch (gettoken(basetokens)) {
			case MapCRC:
				Integer crc32 = getsymbol();
				if (crc32 == null) {
					break;
				}

				MapCRC = crc32 & 0xFFFFFFFFL;
				break;
			case Sprite:
				Integer sprnum = getsymbol();
				if (sprnum == null) {
					break;
				}

				switch (gettoken(sprite_tokens)) {
				default:
					break;
				case AngleOffset:
					if ((value = getsymbol()) != null) {
						spriteext.get(sprnum).angoff = value.shortValue();
					}
					break;
				case XOffset:
					if ((value = getsymbol()) != null) {
						spriteext.get(sprnum).xoff = value;
					}
					break;
				case YOffset:
					if ((value = getsymbol()) != null) {
						spriteext.get(sprnum).yoff = value;
					}
					break;
				case ZOffset:
					if ((value = getsymbol()) != null) {
						spriteext.get(sprnum).zoff = value;
					}
					break;
				case NoModel:
					spriteext.get(sprnum).flags |= 1; // SPREXT_NOTMD;
					break;
				}

				break;
			case Error:
				break;
			case EOF:
				return;
			default:
				break;
			}
		}
	}

	private Token gettoken(Map<String, Token> list) {
		int tok;
		if ((tok = gettoken()) == -2) {
			return Token.EOF;
		}

		Token out = list.get(toLowerCase(textbuf.substring(tok, textptr)));
		if (out != null) {
			return out;
		}

		errorptr = textptr;
		return Token.Error;
	}

	public Spriteext getSpriteInfo(int spriteid) {
		if (spriteid < 0 || spriteid >= spriteext.size) {
			return null;
		}
		return spriteext.items[spriteid];
	}

	public long getMapCRC() {
		return MapCRC;
	}
}
