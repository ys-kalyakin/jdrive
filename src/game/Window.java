package game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiConsumer;

import game.util.BitOps;
import game.util.WindowConstants;
import game.util.wcustom.*;
import game.WindowClass;

public class Window extends WindowConstants
{
	int flags4;
	WindowClass window_class;
	WindowNumber window_number;

	int left, top;
	int width, height;

	Scrollbar hscroll, vscroll, vscroll2;
	ResizeInfo resize;

	byte caption_color;

	int click_state, disabled_state, hidden_state;

	ViewPort viewport;
	Widget [] original_widget;
	List<Widget> widget;
	int desc_flags;

	WindowMessage message;
	//byte custom[WINDOW_CUSTOM_SIZE];
	//byte custom[];
	AbstractWinCustom custom;

	BiConsumer<Window,WindowEvent> wndproc;

	public Window() {
		left = top = width = height = flags4 = 0;
		caption_color = 0;
		click_state = disabled_state = hidden_state = 0;
		desc_flags = 0;

		window_class = null;
		window_number = null;

		hscroll  = null;
		vscroll  = null;
		vscroll2 = null;
		resize = null;


		viewport = null;
		original_widget = null;
		//widget = null;

		message = null;
		custom = null;
		wndproc = null;

		//List<Widget> 
		widget =  new ArrayList<Widget>();

	}	


	// ------------------------------------
	// static state


	//static Window _windows[] = new Window[25];
	static List<Window> _windows = new ArrayList<Window>();



	static Point _cursorpos_drag_start = null;//new Point();

	static boolean _left_button_down;
	static boolean _left_button_clicked;

	static boolean _right_button_down;
	static boolean _right_button_clicked;

	// XXX added parameter to AllocateWindowDesc
	// int _alloc_wnd_parent_num;

	static int _scrollbar_start_pos;
	static int _scrollbar_size;
	static byte _scroller_click_timeout;

	/**
	 * TODO Controlled fro Widget
	 */
	public static boolean _scrolling_scrollbar = false;

	/**
	 * TODO control it from outside
	 */
	public static boolean _scrolling_viewport = false;
	static boolean _popup_menu_active;

	public static byte _special_mouse_mode;



	// -----------------------------------
	
	
	//enum SpecialMouseMode {
	public static final int WSM_NONE = 0;
	public static final int WSM_DRAGDROP = 1;
	public static final int WSM_SIZING = 2;
	public static final int WSM_PRESIZE = 3;
	//};
	

	
	public static Iterator<Window> getIterator() {
		return _windows.iterator();
	}

	/**
	 * 
	 * @param startw iterate starting from this window (including it) up to topmost one.
	 * @return
	 */
	public static Iterator<Window> getIterator(Window startw) 
	{
		int i = 0;

		for(; i < _windows.size(); i++ )
		{
			if( _windows.get(i) == startw )
			{
				return new Iterator<Window>() {

					int curw = i;
					
					@Override
					public boolean hasNext() {
						return _windows.get(curw) != null;
					}

					@Override
					public Window next() {
						return _windows.get(curw++);
					}
					
				};
				
			}
		}
		
		return null;
	}



	// -----------------------------------

	def_d as_def_d() { return (def_d) custom; }
	buildtrain_d as_buildtrain_d() { return (buildtrain_d) custom; }
	dropdown_d as_dropdown_d() { return (dropdown_d) custom; }
	facesel_d as_facesel_d() { return (facesel_d) custom; }
	highscore_d as_highscore_d() { return (highscore_d) custom; }
	menu_d as_menu_d() { return (menu_d) custom; }
	message_d as_message_d() { return (message_d) custom; }
	news_d as_news_d() { return (news_d) custom; }
	order_d as_order_d() { return (order_d) custom; }
	plstations_d as_plstations_d() { return (plstations_d) custom; }
	refit_d as_refit_d() { return (refit_d) custom; }
	replaceveh_d as_replaceveh_d() { return (replaceveh_d) custom; }
	scroller_d as_scroller_d() { return (scroller_d) custom; }
	smallmap_d as_smallmap_d() { return (smallmap_d) custom; }
	tooltips_d as_tooltips_d() { return (tooltips_d) custom; }
	traindepot_d as_traindepot_d() { return (traindepot_d) custom; }
	traindetails_d as_traindetails_d() { return (traindetails_d) custom; }
	tree_d as_tree_d() { return (tree_d) custom; }
	vehiclelist_d as_vehiclelist_d() { return (vehiclelist_d) custom; }
	void_d as_void_d() { return (void_d) custom; }
	vp_d as_vp_d() { return (vp_d) custom; }
	vp2_d as_vp2_d() { return (vp2_d) custom; }

	// -----------------------------------


	//WindowProc *wndproc;
	//abstract void WindowProc( WindowEvent e);
	//void wndproc(WindowEvent e) { WindowProc(e); }

	void CallWindowEventNP(WindowEvents event)
	{
		WindowEvent e = new WindowEvent();

		e.event = event;
		wndproc.accept(this,e);
	}



	/** Returns the index for the widget located at the given position
	 * relative to the window. It includes all widget-corner pixels as well.
	 * @param *w Window to look inside
	 * @param  x,y Window client coordinates
	 * @return A widget index, or -1 if no widget was found.
	 */
	int GetWidgetFromPos(int x, int y)
	{
		//Widget wi;
		int found_index = -1;

		// Go through the widgets and check if we find the widget that the coordinate is
		// inside.
		//for (index = 0,wi = widget; wi.type != WWT_LAST; index++, wi++) 
		int index = -1;
		for(Widget wi : widget)
		{
			index++;
			if (wi.type == WWT_EMPTY || wi.type == WWT_FRAME) continue;

			boolean hidden = BitOps.HASBIT(hidden_state,index);

			if (x >= wi.left && x <= wi.right && y >= wi.top &&  y <= wi.bottom &&
					!hidden) {
				found_index = index;
			}
		}

		return found_index;
	}








	// delta between mouse cursor and upper left corner of dragged window
	static Point _drag_delta;

	void HandleButtonClick(int widget)
	{
		click_state |= (1 << widget);
		flags4 |= 5 << WF_TIMEOUT_SHL;
		InvalidateWidget(widget);
	}

	static void DispatchLeftClickEvent(Window  w, int x, int y)
	{
		WindowEvent e = new WindowEvent();
		final Widget wi;

		e.pt.x = x;
		e.pt.y = y;
		e.event = WindowEvents.WE_CLICK;

		if (0 != (w.desc_flags & WindowDesc.WDF_DEF_WIDGET)) {
			e.widget = w.GetWidgetFromPos(x, y);
			if (e.widget < 0) return; /* exit if clicked outside of widgets */

			wi = w.widget.get(e.widget);

			/* don't allow any interaction if the button has been disabled */
			if (BitOps.HASBIT(w.disabled_state, e.widget))
				return;

			if (0 != (wi.type & 0xE0)) {
				/* special widget handling for buttons*/
				switch((int)wi.type) {
				case WWT_IMGBTN  | WWB_PUSHBUTTON: /* WWT_PUSHIMGBTN */
				case WWT_TEXTBTN | WWB_PUSHBUTTON: /* WWT_PUSHTXTBTN */
					w.HandleButtonClick(e.widget);
					break;
				case WWT_NODISTXTBTN:
					break;
				}
			} else if (wi.type == WWT_SCROLLBAR || wi.type == WWT_SCROLL2BAR || wi.type == WWT_HSCROLLBAR) {
				w.ScrollbarClickHandler(wi, e.pt.x, e.pt.y);
			}

			if (0 != (w.desc_flags & WindowDesc.WDF_STD_BTN)) {
				if (e.widget == 0) { /* 'X' */
					w.DeleteWindow();
					return;
				}

				if (e.widget == 1) { /* 'Title bar' */
					w.StartWindowDrag(); // if not return then w = StartWindowDrag(w); to get correct pointer
					return;
				}
			}

			if (0 != (w.desc_flags & WindowDesc.WDF_RESIZABLE) && wi.type == WWT_RESIZEBOX) {
				w.StartWindowSizing(); // if not return then w = StartWindowSizing(w); to get correct pointer
				return;
			}

			if (0 != (w.desc_flags & WindowDesc.WDF_STICKY_BUTTON) && wi.type == WWT_STICKYBOX) {
				w.flags4 ^= WF_STICKY;
				w.InvalidateWidget(e.widget);
				return;
			}
		}

		w.wndproc.accept(w, e);
	}

	static void DispatchRightClickEvent(Window  w, int x, int y)
	{
		WindowEvent e = new WindowEvent();

		/* default tooltips handler? */
		if (0 != (w.desc_flags & WindowDesc.WDF_STD_TOOLTIPS)) {
			e.widget = w.GetWidgetFromPos(x, y);
			if (e.widget < 0)
				return; /* exit if clicked outside of widgets */

			if (w.widget.get(e.widget).tooltips != 0) {
				GuiShowTooltips(w.widget.get(e.widget).tooltips);
				return;
			}
		}

		e.event = WindowEvents.WE_RCLICK;
		e.pt.x = x;
		e.pt.y = y;
		w.wndproc.accept(w, e);
	}

	/** Dispatch the mousewheel-action to the window which will scroll any
	 * compatible scrollbars if the mouse is pointed over the bar or its contents
	 * @param *w Window
	 * @param widget the widget where the scrollwheel was used
	 * @param wheel scroll up or down
	 */
	void DispatchMouseWheelEvent(int widgeti, int wheel)
	{
		Widget wi1, wi2;
		//Scrollbar sb;

		if (widgeti < 0) return;

		wi1 = widget.get(widgeti);
		wi2 = widget.get(widgeti + 1);

		/* The listbox can only scroll if scrolling was 
		 * done on the scrollbar itself,
		 * or on the listbox (and the next item 
		 * is (must be) the scrollbar)
		 * XXX - should be rewritten as a 
		 * widget-dependent scroller but that's
		 * not happening until someone rewrites 
		 * the whole widget-code */
		if(wi1.type == WWT_SCROLLBAR) 
			scrollBarDirty(this, wheel, vscroll);

		if(wi1.type == WWT_SCROLL2BAR) 
			scrollBarDirty(this, wheel, vscroll2);		

		if(wi2.type == WWT_SCROLL2BAR) 
			scrollBarDirty(this, wheel, vscroll2);

		if (wi2.type == WWT_SCROLLBAR)  
			scrollBarDirty(this, wheel, vscroll);

	}


	private static void scrollBarDirty(Window w, int wheel, Scrollbar sb) {
		if (sb.count > sb.cap) {
			int pos = BitOps.clamp(sb.pos + wheel, 0, sb.count - sb.cap);
			if (pos != sb.pos) {
				sb.pos = pos;
				w.SetWindowDirty();
			}
		}
	}

	void SetWindowDirty()
	{
		Global.hal.SetDirtyBlocks(left, top, left + width, top + height);
	}

	/*
	static void DispatchMouseWheelEvent(Window  w, int widget, int wheel)
	{
		Widget wi1, wi2;
		Scrollbar sb;

		if (widget < 0) return;

		wi1 = w.widget.get(widget);
		wi2 = w.widget.get(widget + 1);

		/* The listbox can only scroll if scrolling was done on the scrollbar itself,
	 * or on the listbox (and the next item is (must be) the scrollbar)
	 * XXX - should be rewritten as a widget-dependent scroller but that's
	 * not happening until someone rewrites the whole widget-code * /
		if ((sb = w.vscroll,  wi1.type == WWT_SCROLLBAR)  || (sb = w.vscroll2, wi1.type == WWT_SCROLL2BAR)  ||
				(sb = w.vscroll2, wi2.type == WWT_SCROLL2BAR) || (sb = w.vscroll, wi2.type == WWT_SCROLLBAR) ) {

			if (sb.count > sb.cap) {
				int pos = BitOps.clamp(sb.pos + wheel, 0, sb.count - sb.cap);
				if (pos != sb.pos) {
					sb.pos = pos;
					w.SetWindowDirty();
				}
			}
		}
	}
	 */

	void DrawOverlappedWindowForAll(int left, int top, int right, int bottom)
	{
		//Window w;
		DrawPixelInfo bk = new DrawPixelInfo();
		Hal._cur_dpi = bk;

		for (Window w : _windows) {
			if (right > w.left &&
					bottom > w.top &&
					left < w.left + w.width &&
					top < w.top + w.height) {
				DrawOverlappedWindow(w, left, top, right, bottom);
			}
		}
	}

	void DrawOverlappedWindow(Window w, int left, int top, int right, int bottom)
	{
		//final Window  v = w;
		int x, wi, wie;

		wi = _windows.indexOf(w);
		wie = _windows.size();
		assert wi >= 0;

		//while (++v != _last_window) 
		while (++wi < wie) 
		{
			final Window  v = _windows.get(wi);

			if (right > v.left &&
					bottom > v.top &&
					left < v.left + v.width &&
					top < v.top + v.height) {
				if (left < (x=v.left)) {
					DrawOverlappedWindow(w, left, top, x, bottom);
					DrawOverlappedWindow(w, x, top, right, bottom);
					return;
				}

				if (right > (x=v.left + v.width)) {
					DrawOverlappedWindow(w, left, top, x, bottom);
					DrawOverlappedWindow(w, x, top, right, bottom);
					return;
				}

				if (top < (x=v.top)) {
					DrawOverlappedWindow(w, left, top, right, x);
					DrawOverlappedWindow(w, left, x, right, bottom);
					return;
				}

				if (bottom > (x=v.top + v.height)) {
					DrawOverlappedWindow(w, left, top, right, x);
					DrawOverlappedWindow(w, left, x, right, bottom);
					return;
				}

				return;
			}
		}

		{
			DrawPixelInfo dp = Hal._cur_dpi;
			dp.width = right - left;
			dp.height = bottom - top;
			dp.left = left - w.left;
			dp.top = top - w.top;
			dp.pitch = Hal._screen.pitch;
			dp.dst_ptr = Hal._screen.dst_ptr + top * Hal._screen.pitch + left;
			dp.zoom = 0;
			w.CallWindowEventNP(WindowEvents.WE_PAINT);
		}
	}

	/*
	void CallWindowEventNP(Window w, int event)
	{
		WindowEvent e;

		e.event = event;
		w.wndproc(w, &e);
	}


	void SetWindowDirty(final Window  w)
	{
		if (w == null) return;
		Global.hal.SetDirtyBlocks(w.left, w.top, w.left + w.width, w.top + w.height);
	}
	 */
	void DeleteWindow()
	{
		WindowClass wc;
		WindowNumber wn;
		ViewPort vp;
		//Window v;
		//int count;

		//if (w == null) return;

		if (ViewPort._thd.place_mode != 0 && ViewPort._thd.window_class == window_class && ViewPort._thd.window_number == window_number) {
			ViewPort.ResetObjectToPlace();
		}

		wc = window_class;
		wn = window_number;

		CallWindowEventNP(WindowEvents.WE_DESTROY);

		Window w = FindWindowById(wc, wn);

		vp = w.viewport;
		w.viewport = null;
		if (vp != null) {
			//_active_viewports &= ~(1 << (vp - _viewports));
			vp.width = 0;
			vp.removeFromAll();
		}

		w.SetWindowDirty();

		//free(w.widget);

		//v = --_last_window;
		//count = (byte*)v - (byte*)w;
		//memmove(w, w + 1, count);
		_windows.remove(w);
	}

	static Window FindWindowById(WindowClass cls, WindowNumber number)
	{
		//Window w;

		for (Window w : _windows) {
			if (w.window_class == cls && w.window_number == number) return w;
		}

		return null;
	}

	static Window FindWindowById(int cls, int number)
	{
		for (Window w : _windows) {
			if (w.window_class.v == cls && w.window_number.n == number) 
				return w;
		}

		return null;
	}

	static void DeleteWindowById(WindowClass cls, WindowNumber number)
	{
		FindWindowById(cls, number).DeleteWindow();
	}

	static void DeleteWindowById(int cls, int number)
	{
		FindWindowById(cls, number).DeleteWindow();
	}

	static void DeleteWindowByClass(WindowClass cls)
	{
		DeleteWindowByClass(cls.v);
	}
	static void DeleteWindowByClass(int cls)
	{

		for(int i = 0; i < _windows.size();) 
		{
			Window w = _windows.get(i);

			if (w.window_class.v == cls) {
				w.DeleteWindow();
				i = 0;
			} else {
				i++;
			}
		}
	}

	static Window BringWindowToFrontById(WindowClass cls, WindowNumber number)
	{
		Window w = FindWindowById(cls, number);

		if (w != null) {
			w.flags4 |= WF_WHITE_BORDER_MASK;
			w.SetWindowDirty();
			w.BringWindowToFront();
		}

		return w;
	}

	static Window BringWindowToFrontById(int cls, int number)
	{
		Window w = FindWindowById(cls, number);

		if (w != null) {
			w.flags4 |= WF_WHITE_BORDER_MASK;
			w.SetWindowDirty();
			w.BringWindowToFront();
		}

		return w;
	}

	public boolean IsVitalWindow()
	{
		WindowClass wc = window_class;
		return (wc.v == WC_MAIN_TOOLBAR || wc.v == WC_STATUS_BAR || wc.v == WC_NEWS_WINDOW || wc.v == WC_SEND_NETWORK_MSG);
	}

	/** On clicking on a window, make it the frontmost window of all. However
	 * there are certain windows that always need to be on-top; these include
	 * - Toolbar, Statusbar (always on)
	 * - New window, Chatbar (only if open)
	 * @param w window that is put into the foreground
	 */
	Window BringWindowToFront()
	{
		Window v;

		int wi = _windows.indexOf(this);
		int we = _windows.size();
		assert wi >= 0;

		int i = we;
		while(true) 
		{
			i--;
			if( i < 0 ) return this;
			v = _windows.get(i);
			if( !v.IsVitalWindow() )
				break;
		}		

		assert i > wi;

		// insert w above i
		_windows.remove(this); // all windows moved a step down
		_windows.add(i, this);
		SetWindowDirty();

		return this; // TODO kill me, make void
	}
	/*
	Window BringWindowToFront(Window w)
	{
		Window v;
		Window temp;

		v = _last_window;
		do {
			if (--v < _windows) return w;
		} while (IsVitalWindow(v));

		if (w == v) return w;

		assert(w < v);

		temp = *w;
		memmove(w, w + 1, (v - w) * sizeof(Window));
	 *v = temp;

		SetWindowDirty(v);

		return v;
	} */

	/** We have run out of windows, so find a suitable candidate for replacement.
	 * Keep all important windows intact. These are
	 * - Main window (gamefield), Toolbar, Statusbar (always on)
	 * - News window, Chatbar (when on)
	 * - Any sticked windows since we wanted to keep these
	 * @return w pointer to the window that is going to be deleted
	 */
	static Window FindDeletableWindow()
	{
		//Window w;

		/*for (w = _windows; w < endof(_windows); w++) {
			if (w.window_class.v != WC_MAIN_WINDOW && !IsVitalWindow(w) && !(w.flags4 & WF_STICKY)) {
				return w;
			}
		}*/
		return null;  // in java we can't
	}

	/** A window must be freed, and all are marked as important windows. Ease the
	 * restriction a bit by allowing to delete sticky windows. Keep important/vital
	 * windows intact (Main window, Toolbar, Statusbar, News Window, Chatbar)
	 * @see FindDeletableWindow()
	 * @return w Pointer to the window that is being deleted
	 */
	static private Window ForceFindDeletableWindow()
	{
		//Window w;

		for (Window w : _windows) {
			//assert(w < _last_window);
			if (w.window_class.v != WC_MAIN_WINDOW && !w.IsVitalWindow()) 
				return w;
		}
		assert false;
		return null;
	}

	static boolean IsWindowOfPrototype(final Window  w, final Widget[] widget)
	{
		return (w.original_widget == widget);
	}

	/* Copies 'widget' to 'w.widget' to allow for resizable windows */
	void AssignWidgetToWindow(final Widget[] nwidget)
	{
		/*
		w.original_widget = widget;

		if (widget != null) {
			int index = 1;
			final Widget wi;

			for (wi = widget; wi.type != WWT_LAST; wi++) index++;

			w.widget = realloc(w.widget, sizeof(*w.widget) * index);
			memcpy(w.widget, widget, sizeof(*w.widget) * index);
		} else {
			w.widget = null;
		}
		 */
		original_widget = nwidget;
		widget.clear(); // XXX really?
		if(nwidget != null)
		{
			for( Widget ww : nwidget)
			{
				if(ww.type != WWT_LAST)
					widget.add(ww);
			}
		}
		//else			widget.clear(); // XXX really?
	}

	/** Open a new window. If there is no space for a new window, close an open
	 * window. Try to avoid stickied windows, but if there is no else, close one of
	 * those as well. Then make sure all created windows are below some always-on-top
	 * ones. Finally set all variables and call the WE_CREATE event
	 * @param x offset in pixels from the left of the screen
	 * @param y offset in pixels from the top of the screen
	 * @param width width in pixels of the window
	 * @param height height in pixels of the window
	 * @param *proc @see WindowProc function to call when any messages/updates happen to the window
	 * @param cls @see WindowClass class of the window, used for identification and grouping
	 * @param *widget @see Widget pointer to the window layout and various elements
	 * @return @see Window pointer of the newly created window
	 */
	static Window AllocateWindow(
			int x, int y, int width, int height,
			BiConsumer<Window,WindowEvent> proc, WindowClass cls, final Widget[] widget)
	{
		Window w = new Window();

		/* TODO limit windows count?
		// We have run out of windows, close one and use that as the place for our new one
		if (w >= endof(_windows)) {
			w = FindDeletableWindow();

			if (w == null) w = ForceFindDeletableWindow();

			DeleteWindow(w);
			w = _last_window;
		} */


		/* XXX - This very strange construction makes sure that the chatbar is always
		 * on top of other windows. Why? It is created as last_window (so, on top).
		 * Any other window will go below toolbar/statusbar/news window, which implicitely
		 * also means it is below the chatbar. Very likely needs heavy improvement
		 * to de-braindeadize * /
		if (w != _windows && cls != WC_SEND_NETWORK_MSG) {
			Window v;

			// * XXX - if not this order (toolbar/statusbar and then news), game would
			// * crash because it will try to copy a negative size for the news-window.
			// * Eg. window was already moved BELOW news (which is below toolbar/statusbar)
			// * and now needs to move below those too. That is a negative move. 
			v = FindWindowById(WC_MAIN_TOOLBAR, 0);
			if (v != null) {
				memmove(v+1, v, (byte*)w - (byte*)v);
				w = v;
			}

			v = FindWindowById(WC_STATUS_BAR, 0);
			if (v != null) {
				memmove(v+1, v, (byte*)w - (byte*)v);
				w = v;
			}

			v = FindWindowById(WC_NEWS_WINDOW, 0);
			if (v != null) {
				memmove(v+1, v, (byte*)w - (byte*)v);
				w = v;
			}
		} */

		// Set up window properties
		//memset(w, 0, sizeof(Window));
		w.window_class = cls;
		w.flags4 = WF_WHITE_BORDER_MASK; // just opened windows have a white border
		w.caption_color = (byte) 0xFF;
		w.left = x;
		w.top = y;
		w.width = width;
		w.height = height;
		w.wndproc = proc;
		w.AssignWidgetToWindow(widget);
		w.resize.width = width;
		w.resize.height = height;
		w.resize.step_width = 1;
		w.resize.step_height = 1;

		_windows.add(w);
		//_last_window++;

		w.SetWindowDirty();

		w.CallWindowEventNP(WindowEvents.WE_CREATE);

		return w;
	}

	Window AllocateWindowAutoPlace2(
			WindowClass exist_class,
			WindowNumber exist_num,
			int width,
			int height,
			BiConsumer<Window,WindowEvent> proc,
			WindowClass cls,
			final Widget[] widget)
	{
		Window w;
		int x;

		w = FindWindowById(exist_class, exist_num);
		if (w == null || w.left >= (Hal._screen.width-20) || w.left <= -60 || w.top >= (Hal._screen.height-20)) {
			return AllocateWindowAutoPlace(width,height,proc,cls,widget);
		}

		x = w.left;
		if (x > Hal._screen.width - width) x = Hal._screen.width - width - 20;

		return AllocateWindow(x + 10, w.top + 10, width, height, proc, cls, widget);
	}



	static SizeRect _awap_r;

	static boolean IsGoodAutoPlace1(int left, int top)
	{
		int right,bottom;
		//Window w;

		_awap_r.left= left;
		_awap_r.top = top;
		right = _awap_r.width + left;
		bottom = _awap_r.height + top;

		if (left < 0 || top < 22 || right > Hal._screen.width || bottom > Hal._screen.height)
			return false;

		// Make sure it is not obscured by any window.
		for (Window w : _windows) {
			if (w.window_class.v == WC_MAIN_WINDOW) continue;

			if (right > w.left &&
					w.left + w.width > left &&
					bottom > w.top &&
					w.top + w.height > top) {
				return false;
			}
		}

		return true;
	}

	static boolean IsGoodAutoPlace2(int left, int top)
	{
		int width,height;
		//Window w;

		_awap_r.left= left;
		_awap_r.top = top;
		width = _awap_r.width;
		height = _awap_r.height;

		if (left < -(width>>2) || left > Hal._screen.width - (width>>1))
			return false;
		if (top < 22 || top > Hal._screen.height - (height>>2))
			return false;

		// Make sure it is not obscured by any window.
		for (Window w : _windows) {
			if (w.window_class.v == WC_MAIN_WINDOW) continue;

			if (left + width > w.left &&
					w.left + w.width > left &&
					top + height > w.top &&
					w.top + w.height > top) {
				return false;
			}
		}

		return true;
	}

	static Point GetAutoPlacePosition(int width, int height)
	{
		//Window w;
		Point pt = new Point(0, 0);

		_awap_r.width = width;
		_awap_r.height = height;

		if (IsGoodAutoPlace1(0, 24)) 
		{
			//goto ok_pos;
			pt.x = _awap_r.left;
			pt.y = _awap_r.top;
			return pt;
		}

		for (Window w : _windows) {
			if (w.window_class.v == WC_MAIN_WINDOW) continue;
			/*
			if (IsGoodAutoPlace1(w.left+w.width+2,w.top)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left-   width-2,w.top)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left,w.top+w.height+2)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left,w.top-   height-2)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left+w.width+2,w.top+w.height-height)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left-   width-2,w.top+w.height-height)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left+w.width-width,w.top+w.height+2)) goto ok_pos;
			if (IsGoodAutoPlace1(w.left+w.width-width,w.top-   height-2)) goto ok_pos;
			 */
			if (
					IsGoodAutoPlace1(w.left+w.width+2,w.top) ||
					IsGoodAutoPlace1(w.left-   width-2,w.top) ||
					IsGoodAutoPlace1(w.left,w.top+w.height+2) ||
					IsGoodAutoPlace1(w.left,w.top-   height-2) ||
					IsGoodAutoPlace1(w.left+w.width+2,w.top+w.height-height) ||
					IsGoodAutoPlace1(w.left-   width-2,w.top+w.height-height) ||
					IsGoodAutoPlace1(w.left+w.width-width,w.top+w.height+2) ||
					IsGoodAutoPlace1(w.left+w.width-width,w.top-   height-2) 
					)
			{
				pt.x = _awap_r.left;
				pt.y = _awap_r.top;
				return pt;
			}

		}

		for (Window w : _windows) {
			if (w.window_class.v == WC_MAIN_WINDOW) continue;

			/*
			if (IsGoodAutoPlace2(w.left+w.width+2,w.top)) goto ok_pos;
			if (IsGoodAutoPlace2(w.left-   width-2,w.top)) goto ok_pos;
			if (IsGoodAutoPlace2(w.left,w.top+w.height+2)) goto ok_pos;
			if (IsGoodAutoPlace2(w.left,w.top-   height-2)) goto ok_pos;
			 */

			if(
					IsGoodAutoPlace2(w.left+w.width+2,w.top) ||
					IsGoodAutoPlace2(w.left-   width-2,w.top) ||
					IsGoodAutoPlace2(w.left,w.top+w.height+2) ||
					IsGoodAutoPlace2(w.left,w.top-   height-2) 
					)
			{
				pt.x = _awap_r.left;
				pt.y = _awap_r.top;
				return pt;
			}

		}

		{
			int left=0,top=24;

			//restart:;
			while(true)
			{
				boolean again = false;
				for (Window w : _windows) {
					if (w.left == left && w.top == top) {
						left += 5;
						top += 5;
						//goto restart;
						again = true;
						break;
					}
				}
				if(!again) break;				
			}

			pt.x = left;
			pt.y = top;
			return pt;
		}

		/*
		//ok_pos:;
		pt.x = _awap_r.left;
		pt.y = _awap_r.top;
		return pt;
		*/
	}

	Window AllocateWindowAutoPlace(
			int width,
			int height,
			BiConsumer<Window,WindowEvent> proc,
			WindowClass cls,
			final Widget[] widget) {

		Point pt = GetAutoPlacePosition(width, height);
		return AllocateWindow(pt.x, pt.y, width, height, proc, cls, widget);
	}
	/**
	 * 
	 * @param desc
	 * @param value win number?
	 * @return
	 */
	static Window AllocateWindowDescFront(final WindowDesc desc, int value)
	{
		Window w;

		if (BringWindowToFrontById(desc.cls.v, value) != null) return null;
		w = AllocateWindowDesc(desc,0);
		w.window_number = new WindowNumber(value);
		return w;
	}

	
	public static Window AllocateWindowDesc(WindowDesc desc)
	{
		return AllocateWindowDesc(desc, 0 );
	}
	
	/**
	 * 
	 * @param desc
	 * @param parentWindowNum parent window number or 0 if not needed
	 * @return
	 * 
	 * @apiNote NB! _alloc_wnd_parent_num is not used anymore,
	 */

	public static Window AllocateWindowDesc(WindowDesc desc, int parentWindowNum)
	{
		Point pt = new Point(0,0);
		Window w = null;
		final DrawPixelInfo _screen = Hal._screen;

		//if (desc.parent_cls != WC_MAIN_WINDOW &&
		//		(w = FindWindowById(desc.parent_cls, _alloc_wnd_parent_num), _alloc_wnd_parent_num=0, w) != null &&
		//		w.left < _screen.width-20 && w.left > -60 && w.top < _screen.height-20) {
		//	pt.x = w.left + 10;
		//	if (pt.x > _screen.width + 10 - desc.width)
		//		pt.x = (_screen.width + 10 - desc.width) - 20;
		//	pt.y = w.top + 10;
		if (desc.parent_cls.v != WC_MAIN_WINDOW && parentWindowNum != 0)
			w = FindWindowById(desc.parent_cls.v, parentWindowNum);

		if (desc.parent_cls.v != WC_MAIN_WINDOW && 
				parentWindowNum != 0 && w != null &&
				w.left < _screen.width-20 && 
				w.left > -60 && w.top < _screen.height-20) 
		{
			pt.x = w.left + 10;
			if (pt.x > _screen.width + 10 - desc.width)
				pt.x = (_screen.width + 10 - desc.width) - 20;
			pt.y = w.top + 10;
		} else if (desc.cls.v == WC_BUILD_TOOLBAR) { // open Build Toolbars aligned
			/* Override the position if a toolbar is opened according to the place of the maintoolbar
			 * The main toolbar (WC_MAIN_TOOLBAR) is 640px in width */
			switch (Global._patches.toolbar_pos) {
			case 1:  pt.x = ((_screen.width + 640) >> 1) - desc.width; break;
			case 2:  pt.x = _screen.width - desc.width; break;
			default: pt.x = 640 - desc.width;
			}
			pt.y = desc.top;
		} else {
			pt.x = desc.left;
			pt.y = desc.top;
			if (pt.x == WDP_AUTO) {
				pt = GetAutoPlacePosition(desc.width, desc.height);
			} else {
				if (pt.x == WDP_CENTER) pt.x = (_screen.width - desc.width) >> 1;
				if (pt.y == WDP_CENTER) pt.y = (_screen.height - desc.height) >> 1;
				else if(pt.y < 0) pt.y = _screen.height + pt.y; // if y is negative, it's from the bottom of the screen
			}
		}

		w = AllocateWindow(pt.x, pt.y, desc.width, desc.height, desc.proc, desc.cls, desc.widgets);
		w.desc_flags = desc.flags;
		return w;
	}

	static public Window FindWindowFromPt(int x, int y)
	{		
		ListIterator<Window> i = _windows.listIterator(_windows.size());

		while (i.hasPrevious()) {
			Window w = i.previous();
			if (BitOps.IS_INSIDE_1D(x, w.left, w.width) &&
					BitOps.IS_INSIDE_1D(y, w.top, w.height)) {
				return w;
			}
		}

		return null;
	}

	public static void InitWindowSystem()
	{
		Console.IConsoleClose();

		//memset(&_windows, 0, sizeof(_windows));
		//_last_window = _windows;
		//memset(_viewports, 0, sizeof(_viewports));
		//_active_viewports = 0;
		ViewPort._viewports.clear();
		Global._no_scroll = 0;
	}

	static void UnInitWindowSystem()
	{

		/*
		//Window w;
		// delete all malloced widgets
		for (Window w : _windows) {
			//free(w.widget);
			w.widget = null;
		}*/
	}

	static void ResetWindowSystem()
	{
		UnInitWindowSystem();
		InitWindowSystem();
		ViewPort._thd.pos.x = 0;
		ViewPort._thd.pos.y = 0;
	}

	static void DecreaseWindowCounters()
	{
		//Window w;
		ListIterator<Window> i = _windows.listIterator(_windows.size());

		//for (w = _last_window; w != _windows;) {
		//	--w;
		while (i.hasPrevious()) {
			Window w = i.previous();
			// Unclick scrollbar buttons if they are pressed.
			if (0 != (w.flags4 & (WF_SCROLL_DOWN | WF_SCROLL_UP))) {
				w.flags4 &= ~(WF_SCROLL_DOWN | WF_SCROLL_UP);
				w.SetWindowDirty();
			}
			w.CallWindowEventNP(WindowEvents.WE_MOUSELOOP);
		}

		i = _windows.listIterator(_windows.size());
		//for (w = _last_window; w != _windows;) {
		//	--w;
		while (i.hasPrevious()) {
			Window w = i.previous();

			if ( (0 != (w.flags4&WF_TIMEOUT_MASK)) && ( 0 == (--w.flags4&WF_TIMEOUT_MASK))) {
				w.CallWindowEventNP(WindowEvents.WE_TIMEOUT);
				if (0 != (w.desc_flags & WindowDesc.WDF_UNCLICK_BUTTONS)) 
					w.UnclickWindowButtons();
			}
		}
	}

	static Window GetCallbackWnd()
	{
		return FindWindowById(ViewPort._thd.window_class, ViewPort._thd.window_number);
	}

	static void HandlePlacePresize()
	{
		Window w;
		WindowEvent e = new WindowEvent();

		if (_special_mouse_mode != WSM_PRESIZE) return;

		w = GetCallbackWnd();
		if (w == null) return;

		e.pt = ViewPort.GetTileBelowCursor();
		if (e.pt.x == -1) {
			ViewPort._thd.selend.x = -1;
			return;
		}
		e.tile = TileIndex.TileVirtXY(e.pt.x, e.pt.y);
		e.event = WindowEvents.WE_PLACE_PRESIZE;
		w.wndproc.accept(w, e);
	}

	static boolean HandleDragDrop()
	{
		Window w;
		WindowEvent e = new WindowEvent();

		if (_special_mouse_mode != WSM_DRAGDROP) return true;

		if (_left_button_down) return false;

		w = GetCallbackWnd();

		ViewPort.ResetObjectToPlace();

		if (w != null) {
			// send an event in client coordinates.
			e.event = WindowEvents.WE_DRAGDROP;
			e.pt.x = Hal._cursor.pos.x - w.left;
			e.pt.y = Hal._cursor.pos.y - w.top;
			e.widget = w.GetWidgetFromPos(e.pt.x, e.pt.y);
			w.wndproc.accept(w, e);
		}
		return false;
	}

	static boolean HandlePopupMenu()
	{
		Window w;
		WindowEvent e = new WindowEvent();

		if (!_popup_menu_active) return true;

		w = FindWindowById(WC_TOOLBAR_MENU, 0);
		if (w == null) {
			_popup_menu_active = false;
			return false;
		}

		if (_left_button_down) {
			e.event = WindowEvents.WE_POPUPMENU_OVER;
			e.pt = Hal._cursor.pos;
		} else {
			_popup_menu_active = false;
			e.event = WindowEvents.WE_POPUPMENU_SELECT;
			e.pt = Hal._cursor.pos;
		}

		w.wndproc.accept(w, e);

		return false;
	}

	static Window last_w = null;
	static boolean HandleMouseOver()
	{
		Window w;
		WindowEvent e = new WindowEvent();

		w = FindWindowFromPt(Hal._cursor.pos.x, Hal._cursor.pos.y);

		// We changed window, put a MOUSEOVER event to the last window
		if (last_w != null && last_w != w) {
			e.event = WindowEvents.WE_MOUSEOVER;
			e.pt.x = -1;
			e.pt.y = -1;
			if (last_w.wndproc != null) last_w.wndproc.accept(last_w, e);
		}
		last_w = w;

		if (w != null) {
			// send an event in client coordinates.
			e.event = WindowEvents.WE_MOUSEOVER;
			e.pt.x = Hal._cursor.pos.x - w.left;
			e.pt.y = Hal._cursor.pos.y - w.top;
			if (w.widget != null) {
				e.widget = w.GetWidgetFromPos(e.pt.x, e.pt.y);
			}
			w.wndproc.accept(w, e);
		}

		// Mouseover never stops execution
		return true;
	}


	static boolean _dragging_window = false;

	static boolean HandleWindowDragging()
	{
		//Window w;
		// Get out immediately if no window is being dragged at all.
		if (!_dragging_window) return true;

		// Otherwise find the window...
		for (Window w : _windows) {
			if (0 != (w.flags4 & WF_DRAGGING)) {
				final Widget t = w.widget.get(1); // the title bar ... ugh
				//final Window v;
				int x;
				int y;
				int nx;
				int ny;

				// Stop the dragging if the left mouse button was released
				if (!_left_button_down) {
					w.flags4 &= ~WF_DRAGGING;
					break;
				}

				w.SetWindowDirty();

				x = Hal._cursor.pos.x + _drag_delta.x;
				y = Hal._cursor.pos.y + _drag_delta.y;
				nx = x;
				ny = y;

				if (Global._patches.window_snap_radius != 0) {
					int hsnap = Global._patches.window_snap_radius;
					int vsnap = Global._patches.window_snap_radius;
					int delta;

					//for (v = _windows; v != _last_window; ++v) 
					for (Window v : _windows ) 
					{
						if (v == w) continue; // Don't snap at yourself

						if (y + w.height > v.top && y < v.top + v.height) {
							// Your left border <. other right border
							delta = Math.abs(v.left + v.width - x);
							if (delta <= hsnap) {
								nx = v.left + v.width;
								hsnap = delta;
							}

							// Your right border <. other left border
							delta = Math.abs(v.left - x - w.width);
							if (delta <= hsnap) {
								nx = v.left - w.width;
								hsnap = delta;
							}
						}

						if (w.top + w.height >= v.top && w.top <= v.top + v.height) {
							// Your left border <. other left border
							delta = Math.abs(v.left - x);
							if (delta <= hsnap) {
								nx = v.left;
								hsnap = delta;
							}

							// Your right border <. other right border
							delta = Math.abs(v.left + v.width - x - w.width);
							if (delta <= hsnap) {
								nx = v.left + v.width - w.width;
								hsnap = delta;
							}
						}

						if (x + w.width > v.left && x < v.left + v.width) {
							// Your top border <. other bottom border
							delta = Math.abs(v.top + v.height - y);
							if (delta <= vsnap) {
								ny = v.top + v.height;
								vsnap = delta;
							}

							// Your bottom border <. other top border
							delta = Math.abs(v.top - y - w.height);
							if (delta <= vsnap) {
								ny = v.top - w.height;
								vsnap = delta;
							}
						}

						if (w.left + w.width >= v.left && w.left <= v.left + v.width) {
							// Your top border <. other top border
							delta = Math.abs(v.top - y);
							if (delta <= vsnap) {
								ny = v.top;
								vsnap = delta;
							}

							// Your bottom border <. other bottom border
							delta = Math.abs(v.top + v.height - y - w.height);
							if (delta <= vsnap) {
								ny = v.top + v.height - w.height;
								vsnap = delta;
							}
						}
					}
				}

				DrawPixelInfo _screen = Hal._screen;

				// Make sure the window doesn't leave the screen
				// 13 is the height of the title bar
				nx = BitOps.clamp(nx, 13 - t.right, _screen.width - 13 - t.left);
				ny = BitOps.clamp(ny, 0, _screen.height - 13);

				// Make sure the title bar isn't hidden by behind the main tool bar
				Window v = FindWindowById(WC_MAIN_TOOLBAR, 0);
				if (v != null) {
					int v_bottom = v.top + v.height;
					int v_right = v.left + v.width;
					if (ny + t.top >= v.top && ny + t.top < v_bottom) {
						if ((v.left < 13 && nx + t.left < v.left) ||
								(v_right > _screen.width - 13 && nx + t.right > v_right)) {
							ny = v_bottom;
						} else {
							if (nx + t.left > v.left - 13 &&
									nx + t.right < v_right + 13) {
								if (w.top >= v_bottom) {
									ny = v_bottom;
								} else if (w.left < nx) {
									nx = v.left - 13 - t.left;
								} else {
									nx = v_right + 13 - t.right;
								}
							}
						}
					}
				}

				if (w.viewport != null) {
					w.viewport.left += nx - w.left;
					w.viewport.top  += ny - w.top;
				}
				w.left = nx;
				w.top  = ny;

				w.SetWindowDirty();
				return false;
			} else if (0 != (w.flags4 & WF_SIZING)) {
				WindowEvent e = new WindowEvent();
				int x, y;

				/* Stop the sizing if the left mouse button was released */
				if (!_left_button_down) {
					w.flags4 &= ~WF_SIZING;
					w.SetWindowDirty();
					break;
				}

				x = Hal._cursor.pos.x - _drag_delta.x;
				y = Hal._cursor.pos.y - _drag_delta.y;

				/* X and Y has to go by step.. calculate it.
				 * The cast to int is necessary else x/y are implicitly casted to
				 * unsigned int, which won't work. */
				if (w.resize.step_width > 1) x -= x % (int)w.resize.step_width;

				if (w.resize.step_height > 1) y -= y % (int)w.resize.step_height;

				/* Check if we don't go below the minimum set size */
				if ((int)w.width + x < (int)w.resize.width)
					x = w.resize.width - w.width;
				if ((int)w.height + y < (int)w.resize.height)
					y = w.resize.height - w.height;

				/* Window already on size */
				if (x == 0 && y == 0) return false;

				/* Now find the new cursor pos.. this is NOT Hal._cursor, because
				    we move in steps. */
				_drag_delta.x += x;
				_drag_delta.y += y;

				w.SetWindowDirty();

				/* Scroll through all the windows and update the widgets if needed */
				{
					//Widget wi = w.widget;
					boolean resize_height = false;
					boolean resize_width = false;

					//while (wi.type != WWT_LAST) 
					for(Widget wi : w.widget)
					{
						if (wi.resize_flag != RESIZE_NONE) {
							/* Resize this Widget */
							if (0 != (wi.resize_flag & RESIZE_LEFT)) {
								wi.left += x;
								resize_width = true;
							}
							if (0 != (wi.resize_flag & RESIZE_RIGHT)) {
								wi.right += x;
								resize_width = true;
							}

							if (0 != (wi.resize_flag & RESIZE_TOP)) {
								wi.top += y;
								resize_height = true;
							}
							if (0 != (wi.resize_flag & RESIZE_BOTTOM)) {
								wi.bottom += y;
								resize_height = true;
							}
						}
						//wi++;
					}

					/* We resized at least 1 widget, so let's rezise the window totally */
					if (resize_width)  w.width  = x + w.width;
					if (resize_height) w.height = y + w.height;
				}

				e.event = WindowEvents.WE_RESIZE;
				e.size.x = x + w.width;
				e.size.y = y + w.height;
				e.diff.x = x;
				e.diff.y = y;
				w.wndproc.accept(w, e);

				w.SetWindowDirty();
				return false;
			}
		}

		_dragging_window = false;
		return false;
	}

	void StartWindowDrag()
	{
		flags4 |= WF_DRAGGING;
		_dragging_window = true;

		_drag_delta.x = left - Hal._cursor.pos.x;
		_drag_delta.y = top  - Hal._cursor.pos.y;

		BringWindowToFront();
		DeleteWindowById(WC_DROPDOWN_MENU, 0);
	}

	void StartWindowSizing()
	{
		flags4 |= WF_SIZING;
		_dragging_window = true;

		_drag_delta.x = Hal._cursor.pos.x;
		_drag_delta.y = Hal._cursor.pos.y;

		BringWindowToFront();
		DeleteWindowById(WC_DROPDOWN_MENU, 0);
		SetWindowDirty();
		//eturn w;
	}


	static boolean HandleScrollbarScrolling()
	{
		//Window w;
		int i;
		int pos;
		Scrollbar sb;

		// Get out quickly if no item is being scrolled
		if (!_scrolling_scrollbar) return true;

		// Find the scrolling window
		for (Window w : _windows) {
			if (0 != (w.flags4 & WF_SCROLL_MIDDLE)) {
				// Abort if no button is clicked any more.
				if (!_left_button_down) {
					w.flags4 &= ~WF_SCROLL_MIDDLE;
					w.SetWindowDirty();
					break;
				}

				if (0 != (w.flags4 & WF_HSCROLL)) {
					sb = w.hscroll;
					i = Hal._cursor.pos.x - _cursorpos_drag_start.x;
				} else if (0 != (w.flags4 & WF_SCROLL2)){
					sb = w.vscroll2;
					i = Hal._cursor.pos.y - _cursorpos_drag_start.y;
				} else {
					sb = w.vscroll;
					i = Hal._cursor.pos.y - _cursorpos_drag_start.y;
				}

				// Find the item we want to move to and make sure it's inside bounds.
				pos = Math.min(Math.max(0, i + _scrollbar_start_pos) * sb.count / _scrollbar_size, Math.max(0, sb.count - sb.cap));
				if (pos != sb.pos) {
					sb.pos = pos;
					w.SetWindowDirty();
				}
				return false;
			}
		}

		_scrolling_scrollbar = false;
		return false;
	}


	static boolean HandleViewportScroll()
	{
		Window w;
		ViewPort vp;
		int dx,dy, x, y, sub;

		if (!_scrolling_viewport) return true;

		if (!_right_button_down) {
			//stop_capt:;
			Hal._cursor.fix_at = false;
			_scrolling_viewport = false;
			return true;
		}

		w = FindWindowFromPt(Hal._cursor.pos.x, Hal._cursor.pos.y);
		if (w == null) //goto stop_capt;
		{
			//stop_capt:;
			Hal._cursor.fix_at = false;
			_scrolling_viewport = false;
			return true;
		}

		if (Global._patches.reverse_scroll) {
			dx = -Hal._cursor.delta.x;
			dy = -Hal._cursor.delta.y;
		} else {
			dx = Hal._cursor.delta.x;
			dy = Hal._cursor.delta.y;
		}

		if (w.window_class.v != WC_SMALLMAP) {
			vp = w.IsPtInWindowViewport(Hal._cursor.pos.x, Hal._cursor.pos.y);
			if (vp == null)
				//goto stop_capt;
			{
				//stop_capt:;
				Hal._cursor.fix_at = false;
				_scrolling_viewport = false;
				return true;
			}

			((vp_d)w.custom).scrollpos_x += dx << vp.zoom;
			((vp_d)w.custom).scrollpos_y += dy << vp.zoom;

			Hal._cursor.delta.x = Hal._cursor.delta.y = 0;
			return false;
		} else {
			// scroll the smallmap ?
			int hx;
			int hy;
			int hvx;
			int hvy;

			Hal._cursor.fix_at = true;

			x = ((smallmap_d)w.custom).scroll_x;
			y = ((smallmap_d)w.custom).scroll_y;

			sub = ((smallmap_d)w.custom).subscroll + dx;

			x -= (sub >> 2) << 4;
			y += (sub >> 2) << 4;
			sub &= 3;

			x += (dy >> 1) << 4;
			y += (dy >> 1) << 4;

			if (0 != (dy & 1)) {
				x += 16;
				sub += 2;
				if (sub > 3) {
					sub -= 4;
					x -= 16;
					y += 16;
				}
			}

			hx = (w.widget.get(4).right  - w.widget.get(4).left) / 2;
			hy = (w.widget.get(4).bottom - w.widget.get(4).top ) / 2;
			hvx = hx * -4 + hy * 8;
			hvy = hx *  4 + hy * 8;
			if (x < -hvx) {
				x = -hvx;
				sub = 0;
			}
			if (x > (int)Global.MapMaxX() * 16 - hvx) {
				x = Global.MapMaxX() * 16 - hvx;
				sub = 0;
			}
			if (y < -hvy) {
				y = -hvy;
				sub = 0;
			}
			if (y > (int)Global.MapMaxY() * 16 - hvy) {
				y = Global.MapMaxY() * 16 - hvy;
				sub = 0;
			}

			((smallmap_d)w.custom).scroll_x = x;
			((smallmap_d)w.custom).scroll_y = y;
			((smallmap_d)w.custom).subscroll = sub;

			Hal._cursor.delta.x = Hal._cursor.delta.y = 0;

			w.SetWindowDirty();
			return false;
		}
	}

	static Window MaybeBringWindowToFront(Window w)
	{
		Window u;

		if (w.window_class.v == WC_MAIN_WINDOW ||
				w.IsVitalWindow() ||
				w.window_class.v == WC_TOOLTIPS ||
				w.window_class.v == WC_DROPDOWN_MENU) {
			return w;
		}

		for (u = w; ++u != _last_window;) {
			if (u.window_class.v == WC_MAIN_WINDOW ||
					w.IsVitalWindow() ||
					u.window_class.v == WC_TOOLTIPS ||
					u.window_class.v == WC_DROPDOWN_MENU) {
				continue;
			}

			if (w.left + w.width <= u.left ||
					u.left + u.width <= w.left ||
					w.top  + w.height <= u.top ||
					u.top + u.height <= w.top) {
				continue;
			}

			return w.BringWindowToFront();
		}

		return w;
	}

	/** Send a message from one window to another. The receiving window is found by
	 * @param w @see Window pointer pointing to the other window
	 * @param msg Specifies the message to be sent
	 * @param wparam Specifies additional message-specific information
	 * @param lparam Specifies additional message-specific information
	 */
	static void SendWindowMessageW(Window  w, int msg, int wparam, int lparam)
	{
		WindowEvent e = new WindowEvent();

		e.event  = WindowEvents.WE_MESSAGE;
		e.msg    = msg;
		e.wparam = wparam;
		e.lparam = lparam;

		w.wndproc.accept(w, e);
	}

	/** Send a message from one window to another. The receiving window is found by
	 * @param wnd_class @see WindowClass class AND
	 * @param wnd_num @see WindowNumber number, mostly 0
	 * @param msg Specifies the message to be sent
	 * @param wparam Specifies additional message-specific information
	 * @param lparam Specifies additional message-specific information
	 */
	static void SendWindowMessage(WindowClass wnd_class, WindowNumber wnd_num, int msg, int wparam, int lparam)
	{
		Window w = FindWindowById(wnd_class, wnd_num);
		if (w != null) SendWindowMessageW(w, msg, wparam, lparam);
	}

	static void HandleKeypress(int key)
	{
		//Window w;
		WindowEvent we = new WindowEvent();
		/* Stores if a window with a textfield for typing is open
		 * If this is the case, keypress events are only passed to windows with text fields and
		 * to thein this main toolbar. */
		boolean query_open = false;

		// Setup event
		we.event = WindowEvents.WE_KEYPRESS;
		we.ascii = (byte) (key & 0xFF);
		we.keycode = key >> 16;
			we.cont = true;

			// check if we have a query string window open before allowing hotkeys
			if (FindWindowById(WC_QUERY_STRING,     0) != null ||
					FindWindowById(WC_SEND_NETWORK_MSG, 0) != null ||
					FindWindowById(WC_CONSOLE,          0) != null ||
					FindWindowById(WC_SAVELOAD,         0) != null) {
				query_open = true;
			}

			// Call the event, start with the uppermost window.
			for (int i = _windows.size()-1; i >= 0 ; i-- ) 
			{
				Window w = _windows.get(i);
				// if a query window is open, only call the event for certain window types
				if (query_open &&
						w.window_class.v != WC_QUERY_STRING &&
						w.window_class.v != WC_SEND_NETWORK_MSG &&
						w.window_class.v != WC_CONSOLE &&
						w.window_class.v != WC_SAVELOAD) {
					continue;
				}
				w.wndproc.accept(w, we);
				if (!we.cont) break;
			}

			if (we.cont) {
				Window w = FindWindowById(WC_MAIN_TOOLBAR, 0);
				// When there is no toolbar w is null, check for that
				if (w != null) w.wndproc.accept(w, we);
			}
	}

	//extern void UpdateTileSelection();
	//extern boolean VpHandlePlaceSizingDrag();

	private static final int  scrollspeed = 3;

	static void MouseLoop(int click, int mousewheel)
	{
		int x,y;
		Window w;
		ViewPort vp;

		DecreaseWindowCounters();
		HandlePlacePresize();
		ViewPort.UpdateTileSelection();
		if (!ViewPort.VpHandlePlaceSizingDrag())  return;
		if (!HandleDragDrop())           return;
		if (!HandlePopupMenu())          return;
		if (!HandleWindowDragging())     return;
		if (!HandleScrollbarScrolling()) return;
		if (!HandleViewportScroll())     return;
		if (!HandleMouseOver())          return;

		x = Hal._cursor.pos.x;
		y = Hal._cursor.pos.y;


		if (click == 0 && mousewheel == 0) {
			if (Global._patches.autoscroll && Global._game_mode != GameModes.GM_MENU) {
				w = FindWindowFromPt(x, y);
				if (w == null || 0 != (w.flags4 & WF_DISABLE_VP_SCROLL) ) return;
				vp = w.IsPtInWindowViewport(x, y);
				if (vp != null) {
					vp_d vpd = (vp_d)w.custom;
					x -= vp.left;
					y -= vp.top;
					//here allows scrolling in both x and y axis
					//#define scrollspeed 3
					if (x - 15 < 0) {
						vpd.scrollpos_x += (x - 15) * scrollspeed << vp.zoom;
					} else if (15 - (vp.width - x) > 0) {
						vpd.scrollpos_x += (15 - (vp.width - x)) * scrollspeed << vp.zoom;
					}
					if (y - 15 < 0) {
						vpd.scrollpos_y += (y - 15) * scrollspeed << vp.zoom;
					} else if (15 - (vp.height - y) > 0) {
						vpd.scrollpos_y += (15 - (vp.height - y)) * scrollspeed << vp.zoom;
					}
					//#undef scrollspeed
				}
			}
			return;
		}

		w = FindWindowFromPt(x, y);
		if (w == null) return;
		w = MaybeBringWindowToFront(w);
		vp = w.IsPtInWindowViewport(x, y);
		if (vp != null) {
			if (Global._game_mode == GameModes.GM_MENU) return;

			// only allow zooming in-out in main window, or in viewports
			if (mousewheel != 0 &&
					(0 == (w.flags4 & WF_DISABLE_VP_SCROLL)) && (
							w.window_class.v == WC_MAIN_WINDOW ||
							w.window_class.v == WC_EXTRA_VIEW_PORT
							)) {
				Gui.ZoomInOrOutToCursorWindow(mousewheel < 0,w);
			}

			if (click == 1) {
				Global.DEBUG_misc( 2, "cursor: 0x%X (%d)", Hal._cursor.sprite, Hal._cursor.sprite);
				if (ViewPort._thd.place_mode != 0 &&
						// query button and place sign button work in pause mode
						Hal._cursor.sprite.id != Sprite.SPR_CURSOR_QUERY &&
						Hal._cursor.sprite.id != Sprite.SPR_CURSOR_SIGN &&
						Global._pause &&
						0 == Global._cheats.build_in_pause.value) {
					return;
				}

				if (ViewPort._thd.place_mode == 0) {
					vp.HandleViewportClicked(x, y);
				} else {
					ViewPort.PlaceObject();
				}
			} else if (click == 2) {
				if (0 == (w.flags4 & WF_DISABLE_VP_SCROLL)) {
					_scrolling_viewport = true;
					Hal._cursor.fix_at = true;
				}
			}
		} else {
			if (mousewheel != 0)
				w.DispatchMouseWheelEvent(w.GetWidgetFromPos(x - w.left, y - w.top), mousewheel);

			switch (click) {
			case 1: DispatchLeftClickEvent(w, x - w.left, y - w.top);  break;
			case 2: DispatchRightClickEvent(w, x - w.left, y - w.top); break;
			}
		}
	}

	void InputLoop()
	{
		int click;
		int mousewheel;

		Global._current_player = Global._local_player;

		// Handle pressed keys
		if (Global._pressed_key != 0) {
			int key = Global._pressed_key; Global._pressed_key = 0;
			HandleKeypress(key);
		}

		// Mouse event?
		click = 0;
		if (_left_button_down && !_left_button_clicked) {
			_left_button_clicked = true;
			click = 1;
		} else if (_right_button_clicked) {
			_right_button_clicked = false;
			click = 2;
		}

		mousewheel = 0;
		if (0 != Hal._cursor.wheel) {
			mousewheel = Hal._cursor.wheel;
			Hal._cursor.wheel = 0;
		}

		MouseLoop(click, mousewheel);
	}


	static int _we4_timer;

	static void UpdateWindows()
	{
		Window w;
		int t,i;

		t = _we4_timer + 1;
		if (t >= 100) 
		{
			for (i = _windows.size()-1; i >= 0; i--) 
			{
				w = _windows.get(i);
				w.CallWindowEventNP(WindowEvents.WE_4);
			}
			t = 0;
		}
		_we4_timer = t;

		for (i = _windows.size()-1; i >= 0; i--) 
		{
			w = _windows.get(i);
			if (0 != (w.flags4 & WF_WHITE_BORDER_MASK)) {
				w.flags4 -= WF_WHITE_BORDER_ONE;
				if ( 0 == (w.flags4 & WF_WHITE_BORDER_MASK)) {
					w.SetWindowDirty();
				}
			}
		}

		Gfx.DrawDirtyBlocks();

		for (Window ww : _windows) {
			if (ww.viewport != null) 
				ViewPort.UpdateViewportPosition(ww);
		}
		TextEffect.DrawTextMessage();
		// Redraw mouse cursor in case it was hidden
		Gfx.DrawMouseCursor();
	}


	int GetMenuItemIndex(final Window w, int x, int y)
	{
		if ((x -= w.left) >= 0 && x < w.width && (y -= w.top + 1) >= 0) {
			y /= 10;

			menu_d md = (menu_d) w.custom;
			//if (y < WP(w, final menu_d).item_count &&
			//		!BitOps.HASBIT(WP(w, final menu_d).disabled_items, y)) 
			if (y < md.item_count &&
					!BitOps.HASBIT(md.disabled_items, y)) 
			{
				return y;
			}
		}
		return -1;
	}

	static void InvalidateWindow(WindowClass cls, WindowNumber number)
	{
		//final Window  w;

		for (Window w : _windows) {
			if (w.window_class == cls && w.window_number == number) 
				w.SetWindowDirty();
		}
	}

	static void InvalidateWindow(int cls, int number)
	{
		//final Window  w;

		for (Window w : _windows) {
			if (w.window_class.v == cls && w.window_number.n == number) 
				w.SetWindowDirty();
		}
	}

	void InvalidateWidget(int widget_index)
	{
		final Widget wi = widget.get(widget_index);

		/* Don't redraw the window if the widget is invisible or of no-type */
		if (wi.type == WWT_EMPTY || BitOps.HASBIT(hidden_state, widget_index)) return;

		Global.hal.SetDirtyBlocks(left + wi.left, top + wi.top, left + wi.right + 1, top + wi.bottom + 1);
	}

	static void InvalidateWindowWidget(WindowClass cls, WindowNumber number, int widget_index)
	{
		for (Window w : _windows) {
			if (w.window_class == cls && w.window_number == number) {
				w.InvalidateWidget(widget_index);
			}
		}
	}

	static void InvalidateWindowWidget(int cls, int number, int widget_index)
	{
		for (Window w : _windows) {
			if (w.window_class.v == cls && w.window_number.n == number) {
				w.InvalidateWidget(widget_index);
			}
		}
	}

	static void InvalidateWindowClasses(WindowClass cls)
	{
		//final Window  w;

		for (Window w : _windows) {
			if (w.window_class == cls) w.SetWindowDirty();
		}
	}

	static void InvalidateWindowClasses(int cls)
	{
		//final Window  w;

		for (Window w : _windows) {
			if (w.window_class.v == cls) w.SetWindowDirty();
		}
	}


	static void CallWindowTickEvent()
	{
		int i;
		for (i = _windows.size()-1; i >= 0; i--) {
			Window w = _windows.get(i);
			w.CallWindowEventNP(WindowEvents.WE_TICK);
		}
	}

	static void DeleteNonVitalWindows()
	{
		for (int i = 0; i < _windows.size();) 
		{
			Window w = _windows.get(i);
			if (w.window_class.v != WC_MAIN_WINDOW &&
					w.window_class.v != WC_SELECT_GAME &&
					w.window_class.v != WC_MAIN_TOOLBAR &&
					w.window_class.v != WC_STATUS_BAR &&
					w.window_class.v != WC_TOOLBAR_MENU &&
					w.window_class.v != WC_TOOLTIPS &&
					(w.flags4 & WF_STICKY) == 0) { // do not delete windows which are 'pinned'
				w.DeleteWindow();
				i = 0;
			} else {
				i++;
			}
		}
	}

	/* It is possible that a stickied window gets to a position where the
	 * 'close' button is outside the gaming area. You cannot close it then; except
	 * with this function. It closes all windows calling the standard function,
	 * then, does a little hacked loop of closing all stickied windows. Note
	 * that standard windows (status bar, etc.) are not stickied, so these aren't affected */
	static void DeleteAllNonVitalWindows()
	{
		//Window w;

		// Delete every window except for stickied ones
		DeleteNonVitalWindows();
		// Delete all sticked windows
		//for (w = _windows; w != _last_window;) {
		for (int i = 0; i < _windows.size();) 
		{
			Window w = _windows.get(i);
			if (0 != (w.flags4 & WF_STICKY)) {
				w.DeleteWindow();
				i = 0;
			} else
				i++;
		}
	}

	/* Delete all always on-top windows to get an empty screen */
	static void HideVitalWindows()
	{
		DeleteWindowById(WC_MAIN_TOOLBAR, 0);
		DeleteWindowById(WC_STATUS_BAR, 0);
	}

	static int PositionMainToolbar(Window w)
	{
		Global.DEBUG_misc( 1, "Repositioning Main Toolbar...");

		if (w == null || w.window_class.v != WC_MAIN_TOOLBAR)
			w = FindWindowById(WC_MAIN_TOOLBAR, 0);

		switch (Global._patches.toolbar_pos) {
		case 1:  w.left = (Hal._screen.width - w.width) >> 1; break;
		case 2:  w.left = Hal._screen.width - w.width; break;
		default: w.left = 0;
		}
		Global.hal.SetDirtyBlocks(0, 0, Hal._screen.width, w.height); // invalidate the whole top part
		return w.left;
	}

	static void RelocateAllWindows(int neww, int newh)
	{
		//Window w;

		for (Window w : _windows) {
			int left, top;

			if (w.window_class.v == WC_MAIN_WINDOW) {
				ViewPort vp = w.viewport;
				vp.width = w.width = neww;
				vp.height = w.height = newh;
				vp.virtual_width = neww << vp.zoom;
				vp.virtual_height = newh << vp.zoom;
				continue; // don't modify top,left
			}

			Console.IConsoleResize();

			if (w.window_class.v == WC_MAIN_TOOLBAR) {
				top = w.top;
				left = PositionMainToolbar(w); // changes toolbar orientation
			} else if (w.window_class.v == WC_SELECT_GAME || w.window_class.v == WC_GAME_OPTIONS || w.window_class.v == WC_NETWORK_WINDOW){
				top = (newh - w.height) >> 1;
				left = (neww - w.width) >> 1;
			} else if (w.window_class.v == WC_NEWS_WINDOW) {
				top = newh - w.height;
				left = (neww - w.width) >> 1;
			} else if (w.window_class.v == WC_STATUS_BAR) {
				top = newh - w.height;
				left = (neww - w.width) >> 1;
			} else if (w.window_class.v == WC_SEND_NETWORK_MSG) {
				top = (newh - 26); // 26 = height of status bar + height of chat bar
				left = (neww - w.width) >> 1;
			} else {
				left = w.left;
				if (left + (w.width>>1) >= neww) left = neww - w.width;
				top = w.top;
				if (top + (w.height>>1) >= newh) top = newh - w.height;
			}

			if (w.viewport != null) {
				w.viewport.left += left - w.left;
				w.viewport.top += top - w.top;
			}

			w.left = left;
			w.top = top;
		}
	}



	ViewPort IsPtInWindowViewport(int x, int y)
	{
		ViewPort vp = viewport;

		if (vp != null &&
				BitOps.IS_INT_INSIDE(x, vp.left, vp.left + vp.width) &&
				BitOps.IS_INT_INSIDE(y, vp.top, vp.top + vp.height))
			return vp;

		return null;
	}




	void UnclickSomeWindowButtons(int mask)
	{
		int x = click_state & mask;
		int i = 0;

		click_state ^= x;

		do {
			if (0 != (x & 1)) 
				InvalidateWidget(i);
			i++;
		} while( 0 != (x >>= 1));
	}


	void UnclickWindowButtons()
	{
		UnclickSomeWindowButtons(-1);
	}









	// ------------------- Widgets




	private static Point HandleScrollbarHittest(final Scrollbar sb, int top, int bottom)
	{
		int height, count, pos, cap;

		top += 10;
		bottom -= 9;

		height = (bottom - top);

		pos = sb.pos;
		count = sb.count;
		cap = sb.cap;

		if (count != 0) top += height * pos / count;

		if (cap > count) cap = count;
		if (count != 0) bottom -= (count - pos - cap) * height / count;

		Point pt = new Point(top, bottom - 1);
		return pt;
	}

	/*****************************************************
	 * Special handling for the scrollbar widget type.
	 * Handles the special scrolling buttons and other
	 * scrolling.
	 * Parameters:
	 *   w   - Window.
	 *   wi  - Pointer to the scrollbar widget.
	 *   x   - The X coordinate of the mouse click.
	 *   y   - The Y coordinate of the mouse click.
	 */

	void ScrollbarClickHandler(final Widget wi, int x, int y)
	{
		int mi, ma, pos;
		Scrollbar sb;

		switch (wi.type) {
		case WWT_SCROLLBAR: {
			// vertical scroller
			flags4 &= ~WF_HSCROLL;
			flags4 &= ~WF_SCROLL2;
			mi = wi.top;
			ma = wi.bottom;
			pos = y;
			sb = vscroll;
			break;
		}
		case WWT_SCROLL2BAR: {
			// 2nd vertical scroller
			flags4 &= ~WF_HSCROLL;
			flags4 |= WF_SCROLL2;
			mi = wi.top;
			ma = wi.bottom;
			pos = y;
			sb = vscroll2;
			break;
		}
		case  WWT_HSCROLLBAR: {
			// horizontal scroller
			flags4 &= ~WF_SCROLL2;
			flags4 |= WF_HSCROLL;
			mi = wi.left;
			ma = wi.right;
			pos = x;
			sb = hscroll;
			break;
		}
		default: assert false; return; //this should never happen
		}
		if (pos <= mi+9) {
			// Pressing the upper button?
			flags4 |= WF_SCROLL_UP;
			if (_scroller_click_timeout == 0) {
				_scroller_click_timeout = 6;
				if (sb.pos != 0) sb.pos--;
			}
			_left_button_clicked = false;
		} else if (pos >= ma-10) {
			// Pressing the lower button?
			flags4 |= WF_SCROLL_DOWN;

			if (_scroller_click_timeout == 0) {
				_scroller_click_timeout = 6;
				if ((byte)(sb.pos + sb.cap) < sb.count)
					sb.pos++;
			}
			_left_button_clicked = false;
		} else {
			//
			Point pt = HandleScrollbarHittest(sb, mi, ma);

			if (pos < pt.x) {
				sb.pos = Math.max(sb.pos - sb.cap, 0);
			} else if (pos > pt.y) {
				sb.pos = Math.min(
						sb.pos + sb.cap,
						Math.max(sb.count - sb.cap, 0)
						);
			} else {
				_scrollbar_start_pos = pt.x - mi - 9;
				_scrollbar_size = ma - mi - 23;
				flags4 |= WF_SCROLL_MIDDLE;
				_scrolling_scrollbar = true;
				_cursorpos_drag_start = Hal._cursor.pos;
			}
		}

		SetWindowDirty();
	}


	private void drawOneWidget(Widget wi, int cur_click, int cur_disabled, int cur_hidden)
	{
		final DrawPixelInfo dpi = Hal._cur_dpi;
		Rect r = new Rect();

		boolean clicked = 0 != (cur_click & 1);
		boolean disabled = 0 != (cur_disabled & 1);

		int clickshift = clicked ? 1 : 0;

		if (dpi.left > (r.right=/*w.left + */wi.right) ||
				dpi.left + dpi.width <= (r.left=wi.left/* + w.left*/) ||
				dpi.top > (r.bottom=/*w.top +*/ wi.bottom) ||
				dpi.top + dpi.height <= (r.top = /*w.top +*/ wi.top) ||
				(0 != (cur_hidden & 1))) {
			return; // TODO check continue;
		}

		switch (wi.type & WWT_MASK) {
		case WWT_PANEL: /* WWT_IMGBTN */
		case WWT_PANEL_2: {
			int img;

			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);

			img = wi.unkA;
			if (img != 0) { // has an image
				// show diff image when clicked
				if ((wi.type & WWT_MASK) == WWT_PANEL_2 && clicked) img++;

				Gfx.DrawSprite(img, r.left + 1 + clickshift, r.top + 1 + clickshift);
			}
			//goto draw_default;
			if (disabled) 
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);			
			break;
		}

		case WWT_TEXTBTN: /* WWT_TEXTBTN */
		case WWT_4: {
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
		}
		/* fall through */

		case WWT_5: {
			StringID str = new StringID(wi.unkA);

			if ((wi.type&WWT_MASK) == WWT_4 && clicked) str.id++;

			Gfx.DrawStringCentered(((r.left + r.right + 1) >> 1) + clickshift, ((r.top + r.bottom + 1) >> 1) - 5 + clickshift, str, 0);
			//DrawStringCentered((r.left + r.right+1)>>1, ((r.top+r.bottom + 1)>>1) - 5, str, 0);
			//goto draw_default;
			if (disabled) 
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);			
			break;
		}

		case WWT_6: {
			//StringID str;
			int str;
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, FR_LOWERED | FR_DARKENED);

			str = wi.unkA;
			if (str != 0) Gfx.DrawString(r.left + 2, r.top + 1, new StringID( str ), 0);
			//goto draw_default;
			if (disabled) 
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);			
			break;
		}

		case WWT_MATRIX: {
			int c, d, ctr;
			int x, amt1, amt2;
			int color;

			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);

			c = BitOps.GB(wi.unkA, 0, 8);
			amt1 = (wi.right - wi.left + 1) / c;

			d = BitOps.GB(wi.unkA, 8, 8);
			amt2 = (wi.bottom - wi.top + 1) / d;

			color = Global._color_list[wi.color & 0xF].window_color_bgb;

			x = r.left;
			for (ctr = c; ctr > 1; ctr--) {
				x += amt1;
				Gfx.GfxFillRect(x, r.top + 1, x, r.bottom - 1, color);
			}

			x = r.top;
			for (ctr = d; ctr > 1; ctr--) {
				x += amt2;
				Gfx.GfxFillRect(r.left + 1, x, r.right - 1, x, color);
			}

			color = Global._color_list[wi.color&0xF].window_color_1b;

			x = r.left - 1;
			for (ctr = c; ctr > 1; ctr--) {
				x += amt1;
				Gfx.GfxFillRect(x, r.top + 1, x, r.bottom - 1, color);
			}

			x = r.top - 1;
			for (ctr = d; ctr > 1; ctr--) {
				x += amt2;
				Gfx.GfxFillRect(r.left+1, x, r.right-1, x, color);
			}

			//goto draw_default;
			if (disabled) 
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);			
			break;
		}

		// vertical scrollbar
		case WWT_SCROLLBAR: {
			Point pt;
			int c1,c2;

			assert(r.right - r.left == 11); // XXX - to ensure the same sizes are used everywhere!

			// draw up/down buttons
			clicked = !!((flags4 & (WF_SCROLL_UP | WF_HSCROLL | WF_SCROLL2)) == WF_SCROLL_UP);
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.top + 9, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DoDrawString(Gfx.UPARROW, r.left + 2 + clickshift, r.top + clickshift, 0x10);

			clicked = !!(((flags4 & (WF_SCROLL_DOWN | WF_HSCROLL | WF_SCROLL2)) == WF_SCROLL_DOWN));
			Gfx.DrawFrameRect(r.left, r.bottom - 9, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DoDrawString(Gfx.DOWNARROW, r.left + 2 + clickshift, r.bottom - 9 + clickshift, 0x10);

			c1 = Global._color_list[wi.color&0xF].window_color_1a;
			c2 = Global._color_list[wi.color&0xF].window_color_2;

			// draw "shaded" background
			Gfx.GfxFillRect(r.left, r.top+10, r.right, r.bottom-10, c2);
			Gfx.GfxFillRect(r.left, r.top+10, r.right, r.bottom-10, c1 | Sprite.PALETTE_MODIFIER_GREYOUT);

			// draw shaded lines
			Gfx.GfxFillRect(r.left+2, r.top+10, r.left+2, r.bottom-10, c1);
			Gfx.GfxFillRect(r.left+3, r.top+10, r.left+3, r.bottom-10, c2);
			Gfx.GfxFillRect(r.left+7, r.top+10, r.left+7, r.bottom-10, c1);
			Gfx.GfxFillRect(r.left+8, r.top+10, r.left+8, r.bottom-10, c2);

			pt = HandleScrollbarHittest(vscroll, r.top, r.bottom);
			Gfx.DrawFrameRect(r.left, pt.x, r.right, pt.y, wi.color, (flags4 & (WF_SCROLL_MIDDLE | WF_HSCROLL | WF_SCROLL2)) == WF_SCROLL_MIDDLE ? FR_LOWERED : 0);
			break;
		}
		case WWT_SCROLL2BAR: {
			Point pt;
			int c1,c2;

			assert(r.right - r.left == 11); // XXX - to ensure the same sizes are used everywhere!

			// draw up/down buttons
			clicked = !!((flags4 & (WF_SCROLL_UP | WF_HSCROLL | WF_SCROLL2)) == (WF_SCROLL_UP | WF_SCROLL2));
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.top + 9, wi.color,  (clicked) ? FR_LOWERED : 0);
			Gfx.DoDrawString(Gfx.UPARROW, r.left + 2 + (clicked ? 1 : 0), r.top + (clicked ? 1 : 0), 0x10);

			clicked = !!((flags4 & (WF_SCROLL_DOWN | WF_HSCROLL | WF_SCROLL2)) == (WF_SCROLL_DOWN | WF_SCROLL2));
			Gfx.DrawFrameRect(r.left, r.bottom - 9, r.right, r.bottom, wi.color,  (clicked) ? FR_LOWERED : 0);
			Gfx.DoDrawString(Gfx.DOWNARROW, r.left + 2 + (clicked ? 1 : 0), r.bottom - 9 + (clicked ? 1 : 0), 0x10);

			c1 = Global._color_list[wi.color&0xF].window_color_1a;
			c2 = Global._color_list[wi.color&0xF].window_color_2;

			// draw "shaded" background
			Gfx.GfxFillRect(r.left, r.top+10, r.right, r.bottom-10, c2);
			Gfx.GfxFillRect(r.left, r.top+10, r.right, r.bottom-10, c1 | Sprite.PALETTE_MODIFIER_GREYOUT);

			// draw shaded lines
			Gfx.GfxFillRect(r.left+2, r.top+10, r.left+2, r.bottom-10, c1);
			Gfx.GfxFillRect(r.left+3, r.top+10, r.left+3, r.bottom-10, c2);
			Gfx.GfxFillRect(r.left+7, r.top+10, r.left+7, r.bottom-10, c1);
			Gfx.GfxFillRect(r.left+8, r.top+10, r.left+8, r.bottom-10, c2);

			pt = HandleScrollbarHittest(vscroll2, r.top, r.bottom);
			Gfx.DrawFrameRect(r.left, pt.x, r.right, pt.y, wi.color, (flags4 & (WF_SCROLL_MIDDLE | WF_HSCROLL | WF_SCROLL2)) == (WF_SCROLL_MIDDLE | WF_SCROLL2) ? FR_LOWERED : 0);
			break;
		}

		// horizontal scrollbar
		case WWT_HSCROLLBAR: {
			Point pt;
			int c1,c2;

			assert(r.bottom - r.top == 11); // XXX - to ensure the same sizes are used everywhere!

			clicked = !!((flags4 & (WF_SCROLL_UP | WF_HSCROLL)) == (WF_SCROLL_UP | WF_HSCROLL));
			Gfx.DrawFrameRect(r.left, r.top, r.left + 9, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DrawSprite(Sprite.SPR_ARROW_LEFT, r.left + 1 + (clicked ? 1 : 0), r.top + 1 + (clicked ? 1 : 0));

			clicked = !!((flags4 & (WF_SCROLL_DOWN | WF_HSCROLL)) == (WF_SCROLL_DOWN | WF_HSCROLL));
			Gfx.DrawFrameRect(r.right-9, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DrawSprite(Sprite.SPR_ARROW_RIGHT, r.right - 8 + (clicked ? 1 : 0), r.top + 1 + (clicked ? 1 : 0));

			c1 = Global._color_list[wi.color&0xF].window_color_1a;
			c2 = Global._color_list[wi.color&0xF].window_color_2;

			// draw "shaded" background
			Gfx.GfxFillRect(r.left+10, r.top, r.right-10, r.bottom, c2);
			Gfx.GfxFillRect(r.left+10, r.top, r.right-10, r.bottom, c1 | Sprite.PALETTE_MODIFIER_GREYOUT);

			// draw shaded lines
			Gfx.GfxFillRect(r.left+10, r.top+2, r.right-10, r.top+2, c1);
			Gfx.GfxFillRect(r.left+10, r.top+3, r.right-10, r.top+3, c2);
			Gfx.GfxFillRect(r.left+10, r.top+7, r.right-10, r.top+7, c1);
			Gfx.GfxFillRect(r.left+10, r.top+8, r.right-10, r.top+8, c2);

			// draw actual scrollbar
			pt = HandleScrollbarHittest(hscroll, r.left, r.right);
			Gfx.DrawFrameRect(pt.x, r.top, pt.y, r.bottom, wi.color, (flags4 & (WF_SCROLL_MIDDLE | WF_HSCROLL)) == (WF_SCROLL_MIDDLE | WF_HSCROLL) ? FR_LOWERED : 0);

			break;
		}

		case WWT_FRAME: {
			int c1,c2;
			int x2 = r.left; // by default the left side is the left side of the widget

			if (wi.unkA != 0) x2 = Gfx.DrawString(r.left + 6, r.top, wi.unkA, 0);

			c1 = Global._color_list[wi.color].window_color_1a;
			c2 = Global._color_list[wi.color].window_color_2;

			//Line from upper left corner to start of text
			Gfx.GfxFillRect(r.left, r.top+4, r.left+4,r.top+4, c1);
			Gfx.GfxFillRect(r.left+1, r.top+5, r.left+4,r.top+5, c2);

			// Line from end of text to upper right corner
			Gfx.GfxFillRect(x2, r.top+4, r.right-1,r.top+4,c1);
			Gfx.GfxFillRect(x2, r.top+5, r.right-2,r.top+5,c2);

			// Line from upper left corner to bottom left corner
			Gfx.GfxFillRect(r.left, r.top+5, r.left, r.bottom-1, c1);
			Gfx.GfxFillRect(r.left+1, r.top+6, r.left+1, r.bottom-2, c2);

			//Line from upper right corner to bottom right corner
			Gfx.GfxFillRect(r.right-1, r.top+5, r.right-1, r.bottom-2, c1);
			Gfx.GfxFillRect(r.right, r.top+4, r.right, r.bottom-1, c2);

			Gfx.GfxFillRect(r.left+1, r.bottom-1, r.right-1, r.bottom-1, c1);
			Gfx.GfxFillRect(r.left, r.bottom, r.right, r.bottom, c2);

			//goto draw_default;
			if (disabled) 
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);			
			break;
		}

		case WWT_STICKYBOX: {
			assert(r.right - r.left == 11); // XXX - to ensure the same sizes are used everywhere!

			clicked = 0 != (flags4 & WF_STICKY);
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DrawSprite((clicked) ? Sprite.SPR_PIN_UP : Sprite.SPR_PIN_DOWN, r.left + 2 + (clicked ? 1 : 0), r.top + 3 + (clicked ? 1 : 0));
			break;
		}

		case WWT_RESIZEBOX: {
			assert(r.right - r.left == 11); // XXX - to ensure the same sizes are used everywhere!

			clicked = 0 != (flags4 & WF_SIZING);
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, (clicked) ? FR_LOWERED : 0);
			Gfx.DrawSprite(Sprite.SPR_WINDOW_RESIZE, r.left + 3 + (clicked ? 1 : 0), r.top + 3 + (clicked ? 1 : 0));
			break;
		}

		case WWT_CLOSEBOX: {
			assert(r.right - r.left == 10); // ensure the same sizes are used everywhere

			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, 0);
			Gfx.DrawString(r.left + 2, r.top + 2, new StringID( Str.STR_00C5 ), 0);
			break;
		}

		case WWT_CAPTION: {
			assert(r.bottom - r.top == 13); // XXX - to ensure the same sizes are used everywhere!
			Gfx.DrawFrameRect(r.left, r.top, r.right, r.bottom, wi.color, FR_BORDERONLY);
			Gfx.DrawFrameRect(r.left+1, r.top+1, r.right-1, r.bottom-1, wi.color, (caption_color == 0xFF) ? FR_LOWERED | FR_DARKENED : FR_LOWERED | FR_DARKENED | FR_BORDERONLY);

			if (caption_color != 0xFF) {
				Gfx.GfxFillRect(r.left+2, r.top+2, r.right-2, r.bottom-2, Global._color_list[Global._player_colors[caption_color]].window_color_1b);
			}

			Gfx.DrawStringCentered( (r.left+r.right+1)>>1, r.top+2, new StringID( wi.unkA ), 0x84);
			//draw_default:;
			if(0 != (cur_disabled & 1)) {
				Gfx.GfxFillRect(r.left+1, r.top+1, r.right-1, r.bottom-1, Global._color_list[wi.color&0xF].unk2 | Sprite.PALETTE_MODIFIER_GREYOUT);
			}
		}
		}

	}

	void DrawWindowWidgets()
	{
		int cur_click, cur_disabled, cur_hidden;

		cur_click = click_state;
		cur_disabled = disabled_state;
		cur_hidden = hidden_state;

		for(Widget cw : widget) {
			drawOneWidget(cw, cur_click, cur_disabled, cur_hidden);
			cur_click>>=1; 
			cur_disabled>>=1; 
			cur_hidden >>= 1;
		} 

		if (0 != (flags4 & WF_WHITE_BORDER_MASK)) {
			//DrawFrameRect(w.left, w.top, w.left + w.width-1, w.top+w.height-1, 0xF, 0x10);
			Gfx.DrawFrameRect(0, 0, width-1, height-1, 0xF, FR_BORDERONLY);
		}

	}

	static final Widget _dropdown_menu_widgets[] = {
			new Widget(     WWT_IMGBTN,   RESIZE_NONE,     0,     0, 0,     0, 0, 0x0, Str.STR_NULL),
	};

	static int GetDropdownItem(final Window w)
	{
		byte item, counter;
		int y;

		if (w.GetWidgetFromPos(Hal._cursor.pos.x - w.left, Hal._cursor.pos.y - w.top) < 0)
			return -1;

		y = Hal._cursor.pos.y - w.top - 2;

		if (y < 0)
			return - 1;

		item = (byte) (y / 10);
		if (item >= w.as_dropdown_d().num_items || (BitOps.HASBIT(w.as_dropdown_d().disabled_state, item) && !BitOps.HASBIT(w.as_dropdown_d().hidden_state, item)) || w.as_dropdown_d().items[item].id == 0)
			return - 1;

		// Skip hidden items -- +1 for each hidden item before the clicked item.
		for (counter = 0; item >= counter; ++counter)
			if (BitOps.HASBIT(w.as_dropdown_d().hidden_state, counter)) item++;

		return item;
	}

	static void DropdownMenuWndProc(Window w, WindowEvent e)
	{
		int item;

		switch(e.event) {
		case WE_PAINT: {
			int x,y,i,sel;

			w.DrawWindowWidgets();

			x = 1;
			y = 2;
			sel = w.as_dropdown_d().selected_index;

			for (i = 0; w.as_dropdown_d().items[i] != Global.INVALID_STRING_ID; i++) {
				if (BitOps.HASBIT(w.as_dropdown_d().hidden_state, i)) {
					sel--;
					continue;
				}
				if (w.as_dropdown_d().items[i].id != 0) {
					if (sel == 0) Gfx.GfxFillRect(x + 1, y, x + w.width - 4, y + 9, 0);
					Gfx.DrawString(x + 2, y, w.as_dropdown_d().items[i], sel == 0 ? 12 : 16);

					if (BitOps.HASBIT(w.as_dropdown_d().disabled_state, i)) {
						Gfx.GfxFillRect(x, y, x + w.width - 3, y + 9,
								Sprite.PALETTE_MODIFIER_GREYOUT | Global._color_list[_dropdown_menu_widgets[0].color].window_color_bga
								);
					}
				} else {
					int c1 = Global._color_list[_dropdown_menu_widgets[0].color].window_color_1a;
					int c2 = Global._color_list[_dropdown_menu_widgets[0].color].window_color_2;

					Gfx.GfxFillRect(x + 1, y + 3, x + w.width - 5, y + 3, c1);
					Gfx.GfxFillRect(x + 1, y + 4, x + w.width - 5, y + 4, c2);
				}
				y += 10;
				sel--;
			}
		} break;

		case WE_CLICK: {
			item = GetDropdownItem(w);
			if (item >= 0) {
				w.as_dropdown_d().click_delay = 4;
				w.as_dropdown_d().selected_index = item;
				w.SetWindowDirty();
			}
		} break;

		case WE_MOUSELOOP: {
			Window w2 = FindWindowById(w.as_dropdown_d().parent_wnd_class, w.as_dropdown_d().parent_wnd_num);
			if (w2 == null) {
				w.DeleteWindow();
				return;
			}

			if (w.as_dropdown_d().click_delay != 0 && --w.as_dropdown_d().click_delay == 0) {
				WindowEvent ee = new WindowEvent();
				ee.event = WindowEvents.WE_DROPDOWN_SELECT;
				ee.button = w.as_dropdown_d().parent_button;
				ee.index = w.as_dropdown_d().selected_index;
				w2.wndproc.accept(w2, ee);
				w.DeleteWindow();
				return;
			}

			if (w.as_dropdown_d().drag_mode) {
				item = GetDropdownItem(w);

				if (!_left_button_clicked) {
					w.as_dropdown_d().drag_mode = false;
					if (item < 0) return;
					w.as_dropdown_d().click_delay = 2;
				} else {
					if (item < 0) return;
				}

				w.as_dropdown_d().selected_index = (byte) item;
				w.SetWindowDirty();
			}
		} break;

		case WE_DESTROY: {
			Window w2 = FindWindowById(w.as_dropdown_d().parent_wnd_class, w.as_dropdown_d().parent_wnd_num);
			if (w2 != null) {
				w2.click_state = BitOps.RETCLRBIT(w2.click_state, w.as_dropdown_d().parent_button);
				w2.InvalidateWidget(w.as_dropdown_d().parent_button);
			}
		} break;
		}
	}

	//void ShowDropDownMenu(Window w, final StringID []strings, int selected, int button, int disabled_mask, int hidden_mask)
	static void ShowDropDownMenu(Window w, final int []strings, int selected, int button, int disabled_mask, int hidden_mask)
	{
		WindowNumber num;
		WindowClass cls;
		int i;
		final Widget wi;
		Window w2;
		int old_click_state = w.click_state;

		cls = w.window_class;
		num = w.window_number;
		DeleteWindowById(WC_DROPDOWN_MENU, 0);
		w = FindWindowById(cls, num);

		if (BitOps.HASBIT(old_click_state, button)) return;

		w.click_state = BitOps.RETSETBIT(w.click_state, button);

		w.InvalidateWidget(button);

		//for (i = 0; strings[i] != Global.INVALID_STRING_ID; i++) {}
		for (i = 0; strings[i] != Global.INVALID_STRING_ID.id; i++) {}
		if (i == 0) return;

		wi = w.widget.get(button);

		if (hidden_mask != 0) {
			int j;

			//for (j = 0; strings[j] != Global.INVALID_STRING_ID; j++) {
			for (j = 0; strings[j] != Global.INVALID_STRING_ID.id; j++) {
				if (BitOps.HASBIT(hidden_mask, j)) i--;
			}
		}

		final Widget wi_prev = w.widget.get(button-1);
		
		w2 = AllocateWindow(
//				w.left + wi[-1].left + 1,
				w.left + wi_prev.left + 1,
				w.top + wi.bottom + 2,
//				wi.right - wi[-1].left + 1,
				wi.right - wi_prev.left + 1,
				i * 10 + 4,
				Window::DropdownMenuWndProc,
				0x3F,
				_dropdown_menu_widgets);

		w2.widget.get(0).color = wi.color;
//		w2.widget.get(0).right = wi.right - wi[-1].left;
		w2.widget.get(0).right = wi.right - wi_prev.left;
		w2.widget.get(0).bottom = i * 10 + 3;

		w2.flags4 &= ~WF_WHITE_BORDER_MASK;

		w2.as_dropdown_d().disabled_state = disabled_mask;
		w2.as_dropdown_d().hidden_state = hidden_mask;

		w2.as_dropdown_d().parent_wnd_class = w.window_class;
		w2.as_dropdown_d().parent_wnd_num = w.window_number;
		w2.as_dropdown_d().parent_button = (byte) button;

		w2.as_dropdown_d().num_items = i;
		w2.as_dropdown_d().selected_index = (byte) selected;
		w2.as_dropdown_d().items = strings;

		w2.as_dropdown_d().click_delay = 0;
		w2.as_dropdown_d().drag_mode = true;
	}
	
	public void DrawWindowViewport() {
		ViewPort.DrawWindowViewport(this);
		
	}












}










class WindowMessage {
	int msg;
	int wparam;
	int lparam;
}


class ResizeInfo {
	int width; /* Minimum width and height */
	int height;

	int step_width; /* In how big steps the width and height go */
	int step_height;
} 


class Scrollbar {
	int count, cap, pos;
} ;



class SizeRect {
	int left,top,width,height;
} 

