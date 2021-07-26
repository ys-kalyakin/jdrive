package game;

public class Cmd {


	public static final int CMD_BUILD_RAILROAD_TRACK = 0;
	public static final int CMD_REMOVE_RAILROAD_TRACK = 1;
	public static final int CMD_BUILD_SINGLE_RAIL = 2;
	public static final int CMD_REMOVE_SINGLE_RAIL = 3;
	public static final int CMD_LANDSCAPE_CLEAR = 4;
	public static final int CMD_BUILD_BRIDGE = 5;
	public static final int CMD_BUILD_RAILROAD_STATION = 6;
	public static final int CMD_BUILD_TRAIN_DEPOT = 7;
	public static final int CMD_BUILD_SIGNALS = 8;
	public static final int CMD_REMOVE_SIGNALS = 9;
	public static final int CMD_TERRAFORM_LAND = 10;
	public static final int CMD_PURCHASE_LAND_AREA = 11;
	public static final int CMD_SELL_LAND_AREA = 12;
	public static final int CMD_BUILD_TUNNEL = 13;

	public static final int CMD_REMOVE_FROM_RAILROAD_STATION = 14;
	public static final int CMD_CONVERT_RAIL = 15;

	public static final int CMD_BUILD_TRAIN_WAYPOINT = 16;
	public static final int CMD_RENAME_WAYPOINT = 17;
	public static final int CMD_REMOVE_TRAIN_WAYPOINT = 18;

	public static final int CMD_BUILD_ROAD_STOP = 21;
	public static final int CMD_BUILD_LONG_ROAD = 23;
	public static final int CMD_REMOVE_LONG_ROAD = 24;
	public static final int CMD_BUILD_ROAD = 25;
	public static final int CMD_REMOVE_ROAD = 26;
	public static final int CMD_BUILD_ROAD_DEPOT = 27;

	public static final int CMD_BUILD_AIRPORT = 29;

	public static final int CMD_BUILD_DOCK = 30;

	public static final int CMD_BUILD_SHIP_DEPOT = 31;
	public static final int CMD_BUILD_BUOY = 32;

	public static final int CMD_PLANT_TREE = 33;

	public static final int CMD_BUILD_RAIL_VEHICLE = 34;
	public static final int CMD_MOVE_RAIL_VEHICLE = 35;

	public static final int CMD_START_STOP_TRAIN = 36;

	public static final int CMD_SELL_RAIL_WAGON = 38;

	public static final int CMD_TRAIN_GOTO_DEPOT = 39;
	public static final int CMD_FORCE_TRAIN_PROCEED = 40;
	public static final int CMD_REVERSE_TRAIN_DIRECTION = 41;

	public static final int CMD_MODIFY_ORDER = 42;
	public static final int CMD_SKIP_ORDER = 43;
	public static final int CMD_DELETE_ORDER = 44;
	public static final int CMD_INSERT_ORDER = 45;

	public static final int CMD_CHANGE_TRAIN_SERVICE_INT = 46;

	public static final int CMD_BUILD_INDUSTRY = 47;

	public static final int CMD_BUILD_COMPANY_HQ = 48;
	public static final int CMD_SET_PLAYER_FACE = 49;
	public static final int CMD_SET_PLAYER_COLOR = 50;

	public static final int CMD_INCREASE_LOAN = 51;
	public static final int CMD_DECREASE_LOAN = 52;

	public static final int CMD_WANT_ENGINE_PREVIEW = 53;

	public static final int CMD_NAME_VEHICLE = 54;
	public static final int CMD_RENAME_ENGINE = 55;
	public static final int CMD_CHANGE_COMPANY_NAME = 56;
	public static final int CMD_CHANGE_PRESIDENT_NAME = 57;
	public static final int CMD_RENAME_STATION = 58;

	public static final int CMD_SELL_AIRCRAFT = 59;
	public static final int CMD_START_STOP_AIRCRAFT = 60;
	public static final int CMD_BUILD_AIRCRAFT = 61;
	public static final int CMD_SEND_AIRCRAFT_TO_HANGAR = 62;
	public static final int CMD_CHANGE_AIRCRAFT_SERVICE_INT = 63;
	public static final int CMD_REFIT_AIRCRAFT = 64;

	public static final int CMD_PLACE_SIGN = 65;
	public static final int CMD_RENAME_SIGN = 66;

	public static final int CMD_BUILD_ROAD_VEH = 67;
	public static final int CMD_START_STOP_ROADVEH = 68;
	public static final int CMD_SELL_ROAD_VEH = 69;
	public static final int CMD_SEND_ROADVEH_TO_DEPOT = 70;
	public static final int CMD_TURN_ROADVEH = 71;
	public static final int CMD_CHANGE_ROADVEH_SERVICE_INT = 72;

	public static final int CMD_PAUSE = 73;

	public static final int CMD_BUY_SHARE_IN_COMPANY = 74;
	public static final int CMD_SELL_SHARE_IN_COMPANY = 75;
	public static final int CMD_BUY_COMPANY = 76;

	public static final int CMD_BUILD_TOWN = 77;

	public static final int CMD_RENAME_TOWN = 80;
	public static final int CMD_DO_TOWN_ACTION = 81;

	public static final int CMD_SET_ROAD_DRIVE_SIDE = 82;

	public static final int CMD_CHANGE_DIFFICULTY_LEVEL = 85;

	public static final int CMD_START_STOP_SHIP = 86;
	public static final int CMD_SELL_SHIP = 87;
	public static final int CMD_BUILD_SHIP = 88;
	public static final int CMD_SEND_SHIP_TO_DEPOT = 89;
	public static final int CMD_CHANGE_SHIP_SERVICE_INT = 90;
	public static final int CMD_REFIT_SHIP = 91;

	public static final int CMD_CLONE_ORDER = 99;
	public static final int CMD_CLEAR_AREA = 100;

	public static final int CMD_MONEY_CHEAT = 102;
	public static final int CMD_BUILD_CANAL = 103;

	public static final int CMD_PLAYER_CTRL = 104; // used in multiplayer to create a new player etc.
	public static final int CMD_LEVEL_LAND = 105;	// level land

	public static final int CMD_REFIT_RAIL_VEHICLE = 106;
	public static final int CMD_RESTORE_ORDER_INDEX = 107;
	public static final int CMD_BUILD_LOCK = 108;

	public static final int CMD_BUILD_SIGNAL_TRACK  = 110;
	public static final int CMD_REMOVE_SIGNAL_TRACK = 111;

	public static final int CMD_GIVE_MONEY = 113;
	public static final int CMD_CHANGE_PATCH_SETTING = 114;

	public static final int CMD_REPLACE_VEHICLE = 115;

	public static final int CMD_CLONE_VEHICLE = 116;


	public static final int DC_EXEC = 1;
	public static final int DC_AUTO = 2;								// don't allow building on structures
	public static final int DC_QUERY_COST = 4;					// query cost only; don't build.
	public static final int DC_NO_WATER = 8;						// don't allow building on water
	public static final int DC_NO_RAIL_OVERLAP = 0x10;	// don't allow overlap of rails (used in buildrail)
	public static final int DC_AI_BUILDING = 0x20;			// special building rules for AI
	public static final int DC_NO_TOWN_RATING = 0x40;		// town rating does not disallow you from building
	public static final int DC_FORCETEST = 0x80;				// force test too.

	public static final int CMD_ERROR = ((int)0x80000000);

	//#define public static final int CMD_MSG(x) ((x)<<16)

	public static final int CMD_AUTO = 0x200;
	public static final int CMD_NO_WATER = 0x400;
	public static final int CMD_NETWORK_COMMAND = 0x800;		// execute the command without sending it on the network
	public static final int CMD_NO_TEST_IF_IN_NETWORK = 0x1000; // When enabled; the command will bypass the no-DC_EXEC round if in network
	public static final int CMD_SHOW_NO_ERROR = 0x2000;

	/** Command flags for the command table
	 * @see _command_proc_table
	 */
	public static final int CMD_SERVER  = 0x1; /// the command can only be initiated by the server
	public static final int CMD_OFFLINE = 0x2; /// the command cannot be executed in a multiplayer game; single-player only


	//#define return_cmd_error(errcode) do { _error_message=(errcode); return CMD_ERROR; } while(0)
	//#define return_cmd_error(errcode) do { return CMD_ERROR | (errcode); } while (0)

	/**
	 * Check the return value of a DoCommand*() function
	 * @param res the resulting value from the command to be checked
	 * @return Return true if the command failed, false otherwise
	 */
	static boolean CmdFailed(int res)
	{
		// lower 16bits are the StringID of the possible error
		return res <= (CMD_ERROR | INVALID_STRING_ID);
	}




	/* The master command table */
	static final Command [] _command_proc_table = {
			new Command(CmdBuildRailroadTrack,                  0), /*   0 */
			new Command(CmdRemoveRailroadTrack,                 0), /*   1 */
			new Command(CmdBuildSingleRail,                     0), /*   2 */
			new Command(CmdRemoveSingleRail,                    0), /*   3 */
			new Command(CmdLandscapeClear,                      0), /*   4 */
			new Command(CmdBuildBridge,                         0), /*   5 */
			new Command(CmdBuildRailroadStation,                0), /*   6 */
			new Command(CmdBuildTrainDepot,                     0), /*   7 */
			new Command(CmdBuildSingleSignal,                   0), /*   8 */
			new Command(CmdRemoveSingleSignal,                  0), /*   9 */
			new Command(Clear::CmdTerraformLand,                       0), /*  10 */
			new Command(Clear::CmdPurchaseLandArea,                    0), /*  11 */
			new Command(Clear::CmdSellLandArea,                        0), /*  12 */
			new Command(CmdBuildTunnel,                         0), /*  13 */
			new Command(CmdRemoveFromRailroadStation,           0), /*  14 */
			new Command(CmdConvertRail,                         0), /*  15 */
			new Command(WayPoint::CmdBuildTrainWaypoint,                  0), /*  16 */
			new Command(WayPoint::CmdRenameWaypoint,                      0), /*  17 */
			new Command(WayPoint::CmdRemoveTrainWaypoint,                 0), /*  18 */
			new Command(null,                                   0), /*  19 */
			new Command(null,                                   0), /*  20 */
			new Command(Station::CmdBuildRoadStop,                       0), /*  21 */
			new Command(null,                                   0), /*  22 */
			new Command(CmdBuildLongRoad,                       0), /*  23 */
			new Command(CmdRemoveLongRoad,                      0), /*  24 */
			new Command(CmdBuildRoad,                           0), /*  25 */
			new Command(CmdRemoveRoad,                          0), /*  26 */
			new Command(CmdBuildRoadDepot,                      0), /*  27 */
			new Command(null,                                   0), /*  28 */
			new Command(Station::CmdBuildAirport,                        0), /*  29 */
			new Command(Station::CmdBuildDock,                           0), /*  30 */
			new Command(Station::CmdBuildShipDepot,                      0), /*  31 */
			new Command(Station::CmdBuildBuoy,                           0), /*  32 */
			new Command(Tree::CmdPlantTree,                           0), /*  33 */
			new Command(CmdBuildRailVehicle,                    0), /*  34 */
			new Command(CmdMoveRailVehicle,                     0), /*  35 */
			new Command(CmdStartStopTrain,                      0), /*  36 */
			new Command(null,                                   0), /*  37 */
			new Command(CmdSellRailWagon,                       0), /*  38 */
			new Command(CmdSendTrainToDepot,                    0), /*  39 */
			new Command(CmdForceTrainProceed,                   0), /*  40 */
			new Command(CmdReverseTrainDirection,               0), /*  41 */

			new Command(Order::CmdModifyOrder,                         0), /*  42 */
			new Command(Order::CmdSkipOrder,                           0), /*  43 */
			new Command(Order::CmdDeleteOrder,                         0), /*  44 */
			new Command(Order::CmdInsertOrder,                         0), /*  45 */

			new Command(CmdChangeTrainServiceInt,               0), /*  46 */

			new Command(CmdBuildIndustry,                       0), /*  47 */
			new Command(CmdBuildCompanyHQ,                      0), /*  48 */
			new Command(MiscCmd::CmdSetPlayerFace,                       0), /*  49 */
			new Command(MiscCmd::CmdSetPlayerColor,                      0), /*  50 */

			new Command(MiscCmd::CmdIncreaseLoan,                        0), /*  51 */
			new Command(MiscCmd::CmdDecreaseLoan,                        0), /*  52 */

			new Command(CmdWantEnginePreview,                   0), /*  53 */

			new Command(CmdNameVehicle,                         0), /*  54 */
			new Command(CmdRenameEngine,                        0), /*  55 */

			new Command(MiscCmd::CmdChangeCompanyName,                   0), /*  56 */
			new Command(MiscCmd::CmdChangePresidentName,                 0), /*  57 */

			new Command(Station::CmdRenameStation,                       0), /*  58 */

			new Command(AirCraft::CmdSellAircraft,                        0), /*  59 */
			new Command(AirCraft::CmdStartStopAircraft,                   0), /*  60 */

			new Command(AirCraft::CmdBuildAircraft,                       0), /*  61 */
			new Command(AirCraft::CmdSendAircraftToHangar,                0), /*  62 */
			new Command(AirCraft::CmdChangeAircraftServiceInt,            0), /*  63 */
			new Command(AirCraft::CmdRefitAircraft,                       0), /*  64 */

			new Command(CmdPlaceSign,                           0), /*  65 */
			new Command(CmdRenameSign,                          0), /*  66 */

			new Command(CmdBuildRoadVeh,                        0), /*  67 */
			new Command(CmdStartStopRoadVeh,                    0), /*  68 */
			new Command(CmdSellRoadVeh,                         0), /*  69 */
			new Command(CmdSendRoadVehToDepot,                  0), /*  70 */
			new Command(CmdTurnRoadVeh,                         0), /*  71 */
			new Command(CmdChangeRoadVehServiceInt,             0), /*  72 */

			new Command(MiscCmd::CmdPause,                      Cmd.CMD_SERVER), /*  73 */

			new Command(CmdBuyShareInCompany,                   0), /*  74 */
			new Command(CmdSellShareInCompany,                  0), /*  75 */
			new Command(CmdBuyCompany,                          0), /*  76 */

			new Command(Town::CmdBuildTown,                 Cmd.CMD_OFFLINE), /*  77 */
			new Command(null,                                   0), /*  78 */
			new Command(null,                                   0), /*  79 */
			new Command(Town::CmdRenameTown,                 Cmd.CMD_SERVER), /*  80 */
			new Command(Town::CmdDoTownAction,                        0), /*  81 */

			new Command(CmdSetRoadDriveSide,           Cmd.CMD_SERVER), /*  82 */
			new Command(null,                                   0), /*  83 */
			new Command(null,                                   0), /*  84 */
			new Command(MiscCmd::CmdChangeDifficultyLevel,      Cmd.CMD_SERVER), /*  85 */

			new Command(Ship::CmdStartStopShip,                       0), /*  86 */
			new Command(Ship::CmdSellShip,                            0), /*  87 */
			new Command(Ship::CmdBuildShip,                           0), /*  88 */
			new Command(Ship::CmdSendShipToDepot,                     0), /*  89 */
			new Command(Ship::CmdChangeShipServiceInt,                0), /*  90 */
			new Command(Ship::CmdRefitShip,                           0), /*  91 */

			new Command(null,                                   0), /*  92 */
			new Command(null,                                   0), /*  93 */
			new Command(null,                                   0), /*  94 */
			new Command(null,                                   0), /*  95 */
			new Command(null,                                   0), /*  96 */
			new Command(null,                                   0), /*  97 */
			new Command(null,                                   0), /*  98 */

			new Command(CmdCloneOrder,                          0), /*  99 */

			new Command(Landscape::CmdClearArea,                           0), /* 100 */
			new Command(null,                                   0), /* 101 */

			new Command(MiscCmd::CmdMoneyCheat,                Cmd.CMD_OFFLINE), /* 102 */
			new Command(CmdBuildCanal,                          0), /* 103 */
			new Command(CmdPlayerCtrl,                          0), /* 104 */

			new Command(Clear::CmdLevelLand,                           0), /* 105 */

			new Command(CmdRefitRailVehicle,                    0), /* 106 */
			new Command(CmdRestoreOrderIndex,                   0), /* 107 */
			new Command(CmdBuildLock,                           0), /* 108 */
			new Command(null,                                   0), /* 109 */
			new Command(CmdBuildSignalTrack,                    0), /* 110 */
			new Command(CmdRemoveSignalTrack,                   0), /* 111 */
			new Command(null,                                   0), /* 112 */
			new Command(MiscCmd::CmdGiveMoney,                           0), /* 113 */
			new Command(CmdChangePatchSetting,         Cmd.CMD_SERVER), /* 114 */
			new Command(CmdReplaceVehicle,                      0), /* 115 */
			new Command(CmdCloneVehicle,						 0), /* 116 */
	};

	/* This function range-checks a cmd, and checks if the cmd is not null */
	boolean IsValidCommand(int cmd)
	{
		cmd &= 0xFF;


		return
				cmd < lengthof(_command_proc_table) &&
				_command_proc_table[cmd].proc != null;
	}

	byte GetCommandFlags(int cmd) {return _command_proc_table[cmd & 0xFF].flags;}

	static int DoCommandByTile(TileIndex tile, int p1, int p2, int flags, int procc)
	{
		return DoCommand(TileX(tile) * 16, TileY(tile) * 16, p1, p2, flags, procc);
	}


	static int _docommand_recursive;

	int DoCommand(int x, int y, int p1, int p2, int flags, int procc)
	{
		int res;
		CommandProc proc;

		/* Do not even think about executing out-of-bounds tile-commands */
		//if (TileIndex.TileVirtXY(x, y) > MapSize()) {
		if (!TileIndex.TileVirtXY(x, y).isValid()) {
			Global._cmd_text = null;
			return Cmd.CMD_ERROR;
		}

		proc = _command_proc_table[procc].proc;

		if (_docommand_recursive == 0) Global._error_message = String.INVALID_STRING_ID;

		_docommand_recursive++;

		// only execute the test call if it's toplevel, or we're not execing.
		if (_docommand_recursive == 1 || 0 == (flags & Cmd.DC_EXEC) || 0 != (flags & Cmd.DC_FORCETEST) ) 
		{
			res = proc.exec(x, y, flags&~Cmd.DC_EXEC, p1, p2);
			if (CmdFailed(res)) {
				if (res & 0xFFFF) Global._error_message = res & 0xFFFF;
				{
					//goto error;
					_docommand_recursive--;
					Global._cmd_text = null;
					return Cmd.CMD_ERROR;
				}
			}

			if (_docommand_recursive == 1) {
				if (!(flags&Cmd.DC_QUERY_COST) && res != 0 && !CheckPlayerHasMoney(res))
				{
					//goto error;
					_docommand_recursive--;
					Global._cmd_text = null;
					return Cmd.CMD_ERROR;
				}
			}

			if (!(flags & Cmd.DC_EXEC)) {
				_docommand_recursive--;
				Global._cmd_text = null;
				return res;
			}
		}

		/* Execute the command here. All cost-relevant functions set the expenses type
		 * themselves with "Player.SET_EXPENSES_TYPE(...);" at the beginning of the function */
		res = proc(x, y, flags, p1, p2);
		if (CmdFailed(res)) {
			if (res & 0xFFFF) Global._error_message = res & 0xFFFF;
			//error:
			_docommand_recursive--;
			Global._cmd_text = null;
			return Cmd.CMD_ERROR;
		}

		// if toplevel, subtract the money.
		if (--_docommand_recursive == 0) {
			Player.SubtractMoneyFromPlayer(res);
			// XXX - Old AI hack which doesn't use DoCommandDP; update last build coord of player
			if ( (x|y) != 0 && Global._current_player < Global.MAX_PLAYERS) {
				Player.GetPlayer(Global._current_player).last_build_coordinate = TileIndex.TileVirtXY(x, y);
			}
		}

		Global._cmd_text = null;
		return res;
	}

	static int GetAvailableMoneyForCommand()
	{
		PlayerID pid = Global._current_player;
		if (pid >= Global.MAX_PLAYERS) return 0x7FFFFFFF; // max int
		return GetPlayer(pid).player_money;
	}

	// toplevel network safe docommand function for the current player. must not be called recursively.
	// the callback is called when the command succeeded or failed.
	boolean DoCommandP(TileIndex tile, int p1, int p2, CommandCallback callback, int cmd)
	{
		int res = 0,res2;
		CommandProc *proc;
		int flags;
		boolean notest;

		int x = TileX(tile) * 16;
		int y = TileY(tile) * 16;

		/* Do not even think about executing out-of-bounds tile-commands */
		if (tile > Global.MapSize()) {
			Global._cmd_text = null;
			return false;
		}

		assert(_docommand_recursive == 0);

		Global._error_message = INVALID_STRING_ID;
		Global._error_message_2 = cmd >> 16;
		_additional_cash_required = 0;

		/** Spectator has no rights except for the dedicated server which
		 * is a spectator but is the server, so can do anything */
		if (Global._current_player.id == Owner.OWNER_SPECTATOR && !Global._network_dedicated) {
			Global.ShowErrorMessage(Global._error_message, Global._error_message_2, x, y);
			Global._cmd_text = null;
			return false;
		}

		flags = 0;
		if (cmd & Cmd.CMD_AUTO) flags |= Cmd.DC_AUTO;
		if (cmd & Cmd.CMD_NO_WATER) flags |= Cmd.DC_NO_WATER;

		// get pointer to command handler
		assert((cmd & 0xFF) < _command_proc_table.length);
		proc = _command_proc_table[cmd & 0xFF].proc;
		if (proc == null) {
			Global._cmd_text = null;
			return false;
		}

		// Some commands have a different output in dryrun than the realrun
		//  e.g.: if you demolish a whole town, the dryrun would say okay.
		//  but by really destroying, your rating drops and at a certain point
		//  it will fail. so res and res2 are different
		// Cmd.CMD_REMOVE_ROAD: This command has special local authority
		// restrictions which may cause the test run to fail (the previous
		// road fragments still stay there and the town won't let you
		// disconnect the road system), but the exec will succeed and this
		// fact will trigger an assertion failure. --pasky
		notest =
				(cmd & 0xFF) == Cmd.CMD_CLEAR_AREA ||
				(cmd & 0xFF) == Cmd.CMD_CONVERT_RAIL ||
				(cmd & 0xFF) == Cmd.CMD_LEVEL_LAND ||
				(cmd & 0xFF) == Cmd.CMD_REMOVE_ROAD ||
				(cmd & 0xFF) == Cmd.CMD_REMOVE_LONG_ROAD;

		_docommand_recursive = 1;

		// cost estimation only?
		if (Global._shift_pressed && Player.IsLocalPlayer() && !(cmd & (Cmd.CMD_NETWORK_COMMAND | Cmd.CMD_SHOW_NO_ERROR))) {
			// estimate the cost.
			res = proc.exec(x, y, flags, p1, p2);
			if (CmdFailed(res)) {
				if (res & 0xFFFF) Global._error_message = res & 0xFFFF;
				Global.ShowErrorMessage(Global._error_message, Global._error_message_2, x, y);
			} else {
				ShowEstimatedCostOrIncome(res, x, y);
			}

			_docommand_recursive = 0;
			Global._cmd_text = null;
			return false;
		}


		if (!((cmd & Cmd.CMD_NO_TEST_IF_IN_NETWORK) && Global._networking)) {
			// first test if the command can be executed.
			res = proc.exec(x,y, flags, p1, p2);
			if (CmdFailed(res)) {
				if (res & 0xFFFF) Global._error_message = res & 0xFFFF;
				{
					//goto show_error;
					// show error message if the command fails?
					if (IsLocalPlayer() && Global._error_message_2 != 0)
						ShowErrorMessage(Global._error_message, Global._error_message_2, x,y);

					//callb_err:
					_docommand_recursive = 0;

					if (callback) callback(false, tile, p1, p2);
					Global._cmd_text = null;
					return false;
				}
			}
			// no money? Only check if notest is off
			if (!notest && res != 0 && !CheckPlayerHasMoney(res)) goto show_error;
		}

		/*#ifdef ENABLE_NETWORK
		//** If we are in network, and the command is not from the network
		// * send it to the command-queue and abort execution
		// * If we are a dedicated server temporarily switch local player, otherwise
		// * the other parties won't be able to execute our command and will desync.
		// * @todo Rewrite dedicated server to something more than a dirty hack!
		if (_networking && !(cmd & Cmd.CMD_NETWORK_COMMAND)) {
			if (_network_dedicated) Global._local_player = 0;
			NetworkSend_Command(tile, p1, p2, cmd, callback);
			if (_network_dedicated) Global._local_player = Owner.OWNER_SPECTATOR;
			_docommand_recursive = 0;
			Global._cmd_text = null;
			return true;
		}
		#endif /* ENABLE_NETWORK */

		// update last build coordinate of player.
		if ( tile != 0 && Global._current_player < Global.MAX_PLAYERS) GetPlayer(Global._current_player).last_build_coordinate = tile;

		/* Actually try and execute the command. If no cost-type is given
		 * use the finalruction one */
		_yearly_expenses_type = EXPENSES_CONSTRUCTION;
		res2 = proc(x,y, flags|Cmd.DC_EXEC, p1, p2);

		// If notest is on, it means the result of the test can be different than
		//   the real command.. so ignore the test
		if (!notest && !((cmd & Cmd.CMD_NO_TEST_IF_IN_NETWORK) && _networking)) {
			assert(res == res2); // sanity check
		} else {
			if (CmdFailed(res2)) {
				if (res2 & 0xFFFF) Global._error_message = res2 & 0xFFFF;
				{
					//goto show_error;
					// show error message if the command fails?
					if (IsLocalPlayer() && Global._error_message_2 != 0)
						ShowErrorMessage(Global._error_message, Global._error_message_2, x,y);

					//callb_err:
					_docommand_recursive = 0;

					if (callback) callback(false, tile, p1, p2);
					Global._cmd_text = null;
					return false;
				}
			}
		}

		SubtractMoneyFromPlayer(res2);

		if (IsLocalPlayer() && Global._game_mode != GameModes.GM_EDITOR) {
			if (res2 != 0)
				ShowCostOrIncomeAnimation(x, y, GetSlopeZ(x, y), res2);
			if (_additional_cash_required) {
				Global.SetDParam(0, _additional_cash_required);
				ShowErrorMessage(Str.STR_0003_NOT_ENOUGH_CASH_REQUIRES, Global._error_message_2, x,y);
				if (res2 == 0) 
				{
					//goto callb_err;
					_docommand_recursive = 0;

					if (callback) callback(false, tile, p1, p2);
					Global._cmd_text = null;
					return false;
				}
			}
		}

		_docommand_recursive = 0;

		if (callback) callback(true, tile, p1, p2);
		Global._cmd_text = null;
		return true;

		//show_error:
		// show error message if the command fails?
		if (IsLocalPlayer() && Global._error_message_2 != 0)
			ShowErrorMessage(Global._error_message, Global._error_message_2, x,y);

		//callb_err:
		_docommand_recursive = 0;

		if (callback) callback(false, tile, p1, p2);
		Global._cmd_text = null;
		return false;
	}

	public static int return_cmd_error(int errcode) {
		return CMD_ERROR | (errcode);		
	}




}


/*
//typedef int32 CommandProc(int x, int y, uint32 flags, uint32 p1, uint32 p2);
@FunctionalInterface
interface CommandProc {
	int exec(int x, int y, int flags, int p1, int p2);
}
*/

class Command 
{
	CommandProc proc;
	byte flags;

	public Command(CommandProc p, int f) {
		proc = p;
		flags = f;
	}

} 
