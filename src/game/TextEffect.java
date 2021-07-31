package game;

public class TextEffect 
{

	StringID string_id;
	int x;
	int y;
	int right;
	int bottom;
	int duration;
	int params_1;
	int params_2;

	



	static final int MAX_CHAT_MESSAGES = 10;
	
	static TextEffect _text_effect_list[] = new TextEffect[30];
	static TextMessage _text_message_list[] = new TextMessage[MAX_CHAT_MESSAGES];
	static TileIndex _animated_tile_list[] = new TileIndex[256];


	static int _textmessage_width = 0;
	static boolean _textmessage_dirty = true;
	static boolean _textmessage_visible = false;

	static final int _textmessage_box_left = 10; // Pixels from left
	static final int _textmessage_box_y = 150;  // Height of box
	static final int _textmessage_box_bottom = 30; // Pixels from bottom
	static final int _textmessage_box_max_width = 400; // Max width of box

	//static Pixel _textmessage_backup[] = new Pixel[150 * 400]; // (y * max_width)
	static final byte _textmessage_backup[] = new byte[150 * 400]; // (y * max_width)

	//extern void memcpy_pitch(void *d, void *s, int w, int h, int spitch, int dpitch);

	// Duration is in game-days
	static void AddTextMessage(int color, int duration, final String message, Object ... args)
	{
		String buf;
		int length;
		int i;

		buf = String.format(message, args);
		

		/* Special color magic */
		if ((color & 0xFF) == 0xC9) color = 0x1CA;

		/* Cut the message till it fits inside the chatbox */
		length = buf.length();
		//while (GetStringWidth(buf) > _textmessage_width - 9) 
		//	buf[--length] = '\0';

		/* Find an empty spot and put the message there */
		for (i = 0; i < MAX_CHAT_MESSAGES; i++) {
			if (_text_message_list[i].message == null) {
				// Empty spot
				_text_message_list[i] = new TextMessage();

				_text_message_list[i].message = buf;
				_text_message_list[i].color = color;
				_text_message_list[i].end_date = Global._date + duration;

				_textmessage_dirty = true;
				return;
			}
		}

		// We did not found a free spot, trash the first one, and add to the end
		//memmove(&_text_message_list[0], &_text_message_list[1], sizeof(_text_message_list[0]) * (MAX_CHAT_MESSAGES - 1));
		//ttd_strlcpy(_text_message_list[MAX_CHAT_MESSAGES - 1].message, buf, sizeof(_text_message_list[MAX_CHAT_MESSAGES - 1].message));

		for (i = 0; i < MAX_CHAT_MESSAGES-1; i++) 
		{
			_text_message_list[i] = _text_message_list[i+1];
		}

		_text_message_list[MAX_CHAT_MESSAGES - 1] = new TextMessage();
		
		_text_message_list[MAX_CHAT_MESSAGES - 1].color = color;
		_text_message_list[MAX_CHAT_MESSAGES - 1].end_date = Global._date + duration;
		_text_message_list[MAX_CHAT_MESSAGES - 1].message = buf;

		_textmessage_dirty = true;
	}

	static void InitTextMessage()
	{
		int i;

		for (i = 0; i < MAX_CHAT_MESSAGES; i++) {
			_text_message_list[i].message = null;
		}

		_textmessage_width = _textmessage_box_max_width;
	}

	// Hide the textbox
	static void UndrawTextMessage()
	{
		CursorVars _cursor = Hal._cursor;
		if (_textmessage_visible) {
			// Sometimes we also need to hide the cursor
			//   This is because both textmessage and the cursor take a shot of the
			//   screen before drawing.
			//   Now the textmessage takes his shot and paints his data before the cursor
			//   does, so in the shot of the cursor is the screen-data of the textmessage
			//   included when the cursor hangs somewhere over the textmessage. To
			//   avoid wrong repaints, we undraw the cursor in that case, and everything
			//   looks nicely ;)
			// (and now hope this story above makes sense to you ;))

			if (_cursor.visible) {
				if (_cursor.draw_pos.x + _cursor.draw_size.x >= _textmessage_box_left &&
					_cursor.draw_pos.x <= _textmessage_box_left + _textmessage_width &&
					_cursor.draw_pos.y + _cursor.draw_size.y >= Hal._screen.height - _textmessage_box_bottom - _textmessage_box_y &&
					_cursor.draw_pos.y <= Hal._screen.height - _textmessage_box_bottom) {
					UndrawMouseCursor();
				}
			}

			_textmessage_visible = false;
			// Put our 'shot' back to the screen
			memcpy_pitch(
					Hal._screen.dst_ptr + _textmessage_box_left + (Hal._screen.height-_textmessage_box_bottom-_textmessage_box_y) * _screen.pitch,
				_textmessage_backup,
				_textmessage_width, _textmessage_box_y, _textmessage_width, Hal._screen.pitch);

			// And make sure it is updated next time
			Global.hal.make_dirty(_textmessage_box_left, Hal._screen.height-_textmessage_box_bottom-_textmessage_box_y, _textmessage_width, _textmessage_box_y);

			_textmessage_dirty = true;
		}
	}

	// Check if a message is expired every day
	static void TextMessageDailyLoop()
	{
		int i;

		for (i = 0; i < MAX_CHAT_MESSAGES; i++) {
			if (_text_message_list[i].message == null) continue;

			if (Global._date > _text_message_list[i].end_date) {
				/* Move the remaining messages over the current message */
				if (i != MAX_CHAT_MESSAGES - 1)
				{
					//memmove(&_text_message_list[i], &_text_message_list[i + 1], sizeof(_text_message_list[i]) * (MAX_CHAT_MESSAGES - i - 1));
					for (int j = i; j < MAX_CHAT_MESSAGES-1; j++) 
					{
						_text_message_list[j] = _text_message_list[j+1];
					}

				}
				
				/* Mark the last item as empty */
				_text_message_list[MAX_CHAT_MESSAGES - 1].message = null;
				_textmessage_dirty = true;

				/* Go one item back, because we moved the array 1 to the left */
				i--;
			}
		}
	}

	// Draw the textmessage-box
	static void DrawTextMessage()
	{
		int i, j;
		boolean has_message;

		if (!_textmessage_dirty) return;

		// First undraw if needed
		UndrawTextMessage();

		if (Console._iconsole_mode == IConsoleModes.ICONSOLE_FULL)
			return;

		/* Check if we have anything to draw at all */
		has_message = false;
		for ( i = 0; i < MAX_CHAT_MESSAGES; i++) {
			if (_text_message_list[i].message == null) break;

			has_message = true;
		}
		if (!has_message) return;

		// Make a copy of the screen as it is before painting (for undraw)
		memcpy_pitch(
			_textmessage_backup,
			Hal._screen.dst_ptr + _textmessage_box_left + (Hal._screen.height-_textmessage_box_bottom-_textmessage_box_y) * Hal._screen.pitch,
			_textmessage_width, _textmessage_box_y, Hal._screen.pitch, _textmessage_width);

		// Switch to _screen painting
		Hal._cur_dpi = Hal._screen;

		j = 0;
		// Paint the messages
		for (i = MAX_CHAT_MESSAGES - 1; i >= 0; i--) {
			if (_text_message_list[i].message == null) continue;

			j++;
			GfxFillRect(_textmessage_box_left, Hal._screen.height-_textmessage_box_bottom-j*13-2, _textmessage_box_left+_textmessage_width - 1, Hal._screen.height-_textmessage_box_bottom-j*13+10, /* black, but with some alpha */ 0x322 | Sprite.USE_COLORTABLE);

			DoDrawString(_text_message_list[i].message, _textmessage_box_left + 2, Hal._screen.height - _textmessage_box_bottom - j * 13 - 1, 0x10);
			DoDrawString(_text_message_list[i].message, _textmessage_box_left + 3, Hal._screen.height - _textmessage_box_bottom - j * 13, _text_message_list[i].color);
		}

		// Make sure the data is updated next flush
		Global.hal.make_dirty(_textmessage_box_left, Hal._screen.height-_textmessage_box_bottom-_textmessage_box_y, _textmessage_width, _textmessage_box_y);

		_textmessage_visible = true;
		_textmessage_dirty = false;
	}

	private void MarkTextEffectAreaDirty()
	{
		MarkAllViewportsDirty(
			x,
			y - 1,
			(right - x)*2 + x + 1,
			(bottom - (y - 1)) * 2 + (y - 1) + 1
		);
	}

	static void AddTextEffect(int msg, int x, int y, int duration)
	{
		AddTextEffect(new StringID( msg ), x, y, duration);
	}
	
	static void AddTextEffect(StringID msg, int x, int y, int duration)
	{
		TextEffect te;
		int w;
		String buffer;

		if (Global._game_mode == Global.GM_MENU)
			return;

		/*
		for (te = _text_effect_list; te.string_id != Global.INVALID_STRING_ID; ) {
			if (++te == endof(_text_effect_list)) return;
		}*/

		for(int i = 0 ; _text_effect_list[i].string_id != Global.INVALID_STRING_ID ; i++)
		{
			if(i >= _text_effect_list.length)
				return;
		}
		
		te.string_id = msg;
		te.duration = duration;
		te.y = y - 5;
		te.bottom = y + 5;
		te.params_1 = GetDParam(0);
		te.params_2 = GetDParam(4);

		buffer = GetString(msg);
		w = GetStringWidth(buffer);

		te.x = x - (w >> 1);
		te.right = x + (w >> 1) - 1;
		MarkTextEffectAreaDirty(te);
	}

	private void MoveTextEffect()
	{
		if (duration < 8) {
			string_id = Global.INVALID_STRING_ID;
		} else {
			duration -= 8;
			y--;
			bottom--;
		}
		MarkTextEffectAreaDirty();
	}

	static void MoveAllTextEffects()
	{
		//TextEffect te;

		//for (te = _text_effect_list; te != endof(_text_effect_list); te++) 
		for( TextEffect te : _text_effect_list) 
		{
			if (te.string_id != Global.INVALID_STRING_ID) 
				te.MoveTextEffect();
		}
	}

	static void InitTextEffects()
	{
		//TextEffect te;

		//for (te = _text_effect_list; te != endof(_text_effect_list); te++) 
		for( TextEffect te : _text_effect_list) 
		{
			te.string_id = Global.INVALID_STRING_ID;
		}
	}

	static void DrawTextEffects(DrawPixelInfo dpi)
	{
		//TextEffect te;

		if (dpi.zoom < 1) {
			//for (te = _text_effect_list; te != endof(_text_effect_list); te++) 
			for( TextEffect te : _text_effect_list )
			{
				if (te.string_id == Global.INVALID_STRING_ID)
					continue;

				/* intersection? */
				if (dpi.left > te.right ||
						dpi.top > te.bottom ||
						dpi.left + dpi.width <= te.x ||
						dpi.top + dpi.height <= te.y)
							continue;
				AddStringToDraw(te.x, te.y, te.string_id, te.params_1, te.params_2, 0);
			}
		} else if (dpi.zoom == 1) {
			//for (te = _text_effect_list; te != endof(_text_effect_list); te++) 
			for( TextEffect te : _text_effect_list )
			{
				if (te.string_id == Global.INVALID_STRING_ID)
					continue;

				/* intersection? */
				if (dpi.left > te.right*2 -  te.x ||
						dpi.top > te.bottom*2 - te.y ||
						(dpi.left + dpi.width) <= te.x ||
						(dpi.top + dpi.height) <= te.y)
							continue;
				AddStringToDraw(te.x, te.y, (StringID)(te.string_id-1), te.params_1, te.params_2, 0);
			}

		}
	}

	static void DeleteAnimatedTile(TileIndex tile)
	{
		//TileIndex ti;

		//for (ti = _animated_tile_list; ti != endof(_animated_tile_list); ti++) {
		for(int i = 0 ; i < _animated_tile_list.length; i++)
		{
			if (tile == _animated_tile_list[i]) 
			{
				/* remove the hole */
				//memmove(ti, ti + 1, endof(_animated_tile_list) - 1 - ti);
				System.arraycopy(_animated_tile_list, i+1, _animated_tile_list, i, _animated_tile_list.length - i - 1);
				/* and clear last item */
				//endof(_animated_tile_list)[-1] = 0;
				_animated_tile_list[_animated_tile_list.length-1] = null;
				MarkTileDirtyByTile(tile);
				return;
			}
		}
	}

	static boolean AddAnimatedTile(TileIndex tile)
	{
		//TileIndex ti;

		//for (ti = _animated_tile_list; ti != endof(_animated_tile_list); ti++) {
		//for( TileIndex ti : _animated_tile_list)
		for(int i = 0 ; i < _animated_tile_list.length; i++)
		{
			if (tile == _animated_tile_list[i] || _animated_tile_list[i] == null) {
				_animated_tile_list[i] = tile;
				MarkTileDirtyByTile(tile);
				return true;
			}
		}

		return false;
	}

	static void AnimateAnimatedTiles()
	{
		//final TileIndex ti;

		//for (ti = _animated_tile_list; ti != endof(_animated_tile_list) && ti != null; ti++) {
		for( TileIndex ti : _animated_tile_list)
			AnimateTile(ti);
		}
	}

	static void InitializeAnimatedTiles()
	{
		//memset(_animated_tile_list, 0, sizeof(_animated_tile_list));
	}

	static void SaveLoad_ANIT()
	{
		// In pre version 6, we has 16bit per tile, now we have 32bit per tile, convert it ;)
		if (CheckSavegameVersion(6)) {
			SlArray(_animated_tile_list, lengthof(_animated_tile_list), SLE_FILE_U16 | SLE_VAR_U32);
		} else {
			SlArray(_animated_tile_list, lengthof(_animated_tile_list), SLE_int);
		}
	}

	/*
	final ChunkHandler _animated_tile_chunk_handlers[] = {
		{ 'ANIT', SaveLoad_ANIT, SaveLoad_ANIT, CH_RIFF | CH_LAST},
	};*/
	
	
}



class TextMessage 
{
	String message;
	int color;
	int end_date;
}

