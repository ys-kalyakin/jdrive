package game;

import java.util.Comparator;

import game.struct.FindRoadToChooseData;
import game.struct.OvertakeData;
import game.struct.RoadFindDepotData;
import game.struct.RoadVehFindData;
import game.tables.RoadVehCmdTables;

import game.util.BitOps;

public class RoadVehCmd extends RoadVehCmdTables {



	//void ShowRoadVehViewWindow(Vehicle v);


	static int GetRoadVehImage(final Vehicle v, int direction)
	{
		int img = v.spritenum;
		int image;

		if (Sprite.is_custom_sprite(img)) {
			image = GetCustomVehicleSprite(v, direction);
			if (image != 0) return image;
			img = orig_road_vehicle_info[v.engine_type - Global.ROAD_ENGINES_INDEX].image_index;
		}

		image = direction + _roadveh_images[img];
		if (v.cargo_count >= (v.cargo_cap >> 1))
			image += _roadveh_full_adder[img];
		return image;
	}

	void DrawRoadVehEngine(int x, int y, EngineID engine, int image_ormod)
	{
		int spritenum = Engine.RoadVehInfo(engine.id).image_index;

		if (Sprite.is_custom_sprite(spritenum)) {
			int sprite = GetCustomVehicleIcon(engine, 6);

			if (sprite) {
				Gfx.DrawSprite(sprite | image_ormod, x, y);
				return;
			}
			spritenum = orig_road_vehicle_info[engine - ROAD_ENGINES_INDEX].image_index;
		}
		Gfx.DrawSprite((6 + _roadveh_images[spritenum]) | image_ormod, x, y);
	}

	int EstimateRoadVehCost(EngineID engine_type)
	{
		return ((Global._price.roadveh_base >> 3) * Engine.RoadVehInfo(engine_type.id).base_cost) >> 5;
	}

	/** Build a road vehicle.
	 * @param x,y tile coordinates of depot where road vehicle is built
	 * @param p1 bus/truck type being built (engine)
	 * @param p2 unused
	 */
	int CmdBuildRoadVeh(int x, int y, int flags, int p1, int p2)
	{
		int cost;
		Vehicle v;
		UnitID unit_num;
		TileIndex tile = TileIndex.TileVirtXY(x, y);
		Engine e;

		if (!Engine.IsEngineBuildable(p1, Vehicle.VEH_Road)) return Cmd.CMD_ERROR;

		Player.SET_EXPENSES_TYPE(Player.EXPENSES_NEW_VEHICLES);

		cost = EstimateRoadVehCost(p1);
		if (flags & Cmd.DC_QUERY_COST) return cost;

		/* The ai_new queries the vehicle cost before building the route,
		 * so we must check against cheaters no sooner than now. --pasky */
		if (!IsTileDepotType(tile, TRANSPORT_ROAD)) return Cmd.CMD_ERROR;
		if (!IsTileOwner(tile, Global._current_player)) return Cmd.CMD_ERROR;

		v = AllocateVehicle();
		if (v == null || IsOrderPoolFull())
			return_cmd_error(Str.STR_00E1_TOO_MANY_VEHICLES_IN_GAME);

		/* find the first free roadveh id */
		unit_num = GetFreeUnitNumber(Vehicle.VEH_Road);
		if (unit_num > Global._patches.max_roadveh)
			return_cmd_error(Str.STR_00E1_TOO_MANY_VEHICLES_IN_GAME);

		if (flags & Cmd.DC_EXEC) {
			final RoadVehicleInfo rvi = RoadVehInfo(p1);

			v.unitnumber = unit_num;
			v.direction = 0;
			v.owner = Global._current_player;

			v.tile = tile;
			x = tile.TileX() * 16 + 8;
			y = tile.TileY() * 16 + 8;
			v.x_pos = x;
			v.y_pos = y;
			v.z_pos = GetSlopeZ(x,y);
			v.z_height = 6;

			v.road.state = 254;
			v.vehstatus = Vehicle.VS_HIDDEN|Vehicle.VS_STOPPED|Vehicle.VS_DEFPAL;

			v.spritenum = rvi.image_index;
			v.cargo_type = rvi.cargo_type;
			v.cargo_cap = rvi.capacity;
			//		v.cargo_count = 0;
			v.value = cost;
			//		v.day_counter = 0;
			//		v.next_order_param = v.next_order = 0;
			//		v.load_unload_time_rem = 0;
			//		v.progress = 0;

			//	v.road.unk2 = 0;
			//	v.road.overtaking = 0;

			v.road.slot = null;
			v.road.slotindex = 0;
			v.road.slot_age = 0;

			v.last_station_visited = INVALID_STATION;
			v.max_speed = rvi.max_speed;
			v.engine_type = (byte)p1;

			e = GetEngine(p1);
			v.reliability = e.reliability;
			v.reliability_spd_dec = e.reliability_spd_dec;
			v.max_age = e.lifelength * 366;
			_new_roadveh_id = v.index;
			_new_vehicle_id = v.index;

			v.string_id = Str.STR_SV_ROADVehicle.VEH_NAME;

			v.service_interval = Global._patches.servint_roadveh;

			v.date_of_last_service = _date;
			v.build_year = _cur_year;

			v.type = Vehicle.VEH_Road;
			v.cur_image = 0xC15;
			v.random_bits = VehicleRandomBits();

			VehiclePositionChanged(v);

			Window.InvalidateWindow(Window.WC_VEHICLE_DEPOT, v.tile);
			RebuildVehicleLists();
			Window.InvalidateWindow(Window.WC_COMPANY, v.owner);
			if (IsLocalPlayer())
				Window.InvalidateWindow(Window.WC_REPLACE_VEHICLE, Vehicle.VEH_Road); // updates the replace Road window
		}

		return cost;
	}

	/** Start/Stop a road vehicle.
	 * @param x,y unused
	 * @param p1 road vehicle ID to start/stop
	 * @param p2 unused
	 */
	int CmdStartStopRoadVeh(int x, int y, int flags, int p1, int p2)
	{
		Vehicle v;

		if (!IsVehicleIndex(p1)) return Cmd.CMD_ERROR;

		v = GetVehicle(p1);

		if (v.type != Vehicle.VEH_Road || !CheckOwnership(v.owner)) return Cmd.CMD_ERROR;

		if (flags & Cmd.DC_EXEC) {
			v.vehstatus ^= Vehicle.VS_STOPPED;
			InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
			Window.InvalidateWindow(Window.WC_VEHICLE_DEPOT, v.tile);
		}

		return 0;
	}

	static void ClearSlot(Vehicle v, RoadStop rs)
	{
		Global.DEBUG_ms( 3, "Multistop: Clearing slot %d at 0x%x", v.road.slotindex, rs.xy);
		v.road.slot = null;
		v.road.slot_age = 0;
		if (rs != null) {
			// check that the slot is indeed assigned to the same vehicle
			assert(rs.slot[v.road.slotindex] == v.index);
			rs.slot[v.road.slotindex] = INVALID_SLOT;
		}
	}

	/** Sell a road vehicle.
	 * @param x,y unused
	 * @param p1 vehicle ID to be sold
	 * @param p2 unused
	 */
	int CmdSellRoadVeh(int x, int y, int flags, int p1, int p2)
	{
		Vehicle v;

		if (!IsVehicleIndex(p1)) return Cmd.CMD_ERROR;

		v = Vehicle.GetVehicle(p1);

		if (v.type != Vehicle.VEH_Road || !CheckOwnership(v.owner)) return Cmd.CMD_ERROR;

		Player.SET_EXPENSES_TYPE(Player.EXPENSES_NEW_VEHICLES);

		if (!IsTileDepotType(v.tile, TRANSPORT_ROAD) || v.road.state != 254 || !(v.vehstatus&Vehicle.VS_STOPPED))
			return_cmd_error(Str.STR_9013_MUST_BE_STOPPED_INSIDE);

		if (flags & Cmd.DC_EXEC) {
			// Invalidate depot
			Window.InvalidateWindow(Window.WC_VEHICLE_DEPOT, v.tile);
			RebuildVehicleLists();
			Window.InvalidateWindow(Window.WC_COMPANY, v.owner);
			Window.DeleteWindowById(Window.WC_VEHICLE_VIEW, v.index);
			ClearSlot(v, v.road.slot);
			DeleteVehicle(v);
			if (IsLocalPlayer())
				Window.InvalidateWindow(Window.WC_REPLACE_VEHICLE, Vehicle.VEH_Road); // updates the replace Road window
		}

		return -(int)v.value;
	}



	static boolean EnumRoadSignalFindDepot(TileIndex tile, RoadFindDepotData rfdd, int track, int length, Object state)
	{
		tile = tile.iadd( TileIndex.TileOffsByDir(_road_pf_directions[track]) );

		if (tile.IsTileType( TileTypes.MP_STREET) &&
				BitOps.GB(tile.getMap().m5, 4, 4) == 2 &&
						tile.IsTileOwner(rfdd.owner)) {

			if (length < rfdd.best_length) {
				rfdd.best_length = length;
				rfdd.tile = tile;
			}
		}
		return false;
	}

	static Depot FindClosestRoadDepot(Vehicle v)
	{
		TileIndex tile = v.tile;
		int i;

		if (v.road.state == 255) tile = GetVehicleOutOfTunnelTile(v);

		if (Global._patches.new_pathfinding_all) {
			NPFFoundTargetData ftd;
			/* See where we are now */
			/*Trackdir*/ int trackdir = v.GetVehicleTrackdir();

			ftd = NPFRouteToDepotBreadthFirst(v.tile, trackdir, TRANSPORT_ROAD, v.owner, INVALID_RAILTYPE);
			if (ftd.best_bird_dist == 0)
				return GetDepotByTile(ftd.node.tile); /* Target found */
			else
				return null; /* Target not found */
			/* We do not search in two directions here, why should we? We can't reverse right now can we? */
		} else {
			RoadFindDepotData rfdd = new RoadFindDepotData();
			rfdd.owner = v.owner.id;
			rfdd.best_length = (int)-1;

			/* search in all directions */
			for(i=0; i!=4; i++)
				FollowTrack(tile, 0x2000 | TRANSPORT_ROAD, i, (TPFEnumProc)EnumRoadSignalFindDepot, null, rfdd);

			if (rfdd.best_length == (int)-1)
				return null;

			return Depot.GetDepotByTile(rfdd.tile);
		}
	}

	/** Send a road vehicle to the depot.
	 * @param x,y unused
	 * @param p1 vehicle ID to send to the depot
	 * @param p2 unused
	 */
	int CmdSendRoadVehToDepot(int x, int y, int flags, int p1, int p2)
	{
		Vehicle v;
		final Depot dep;

		if (!IsVehicleIndex(p1)) return Cmd.CMD_ERROR;

		v = GetVehicle(p1);

		if (v.type != Vehicle.VEH_Road || !CheckOwnership(v.owner)) return Cmd.CMD_ERROR;

		if (v.vehstatus & Vehicle.VS_CRASHED) return Cmd.CMD_ERROR;

		/* If the current orders are already goto-depot */
		if (v.current_order.type == Order.OT_GOTO_DEPOT) {
			if (flags & Cmd.DC_EXEC) {
				/* If the orders to 'goto depot' are in the orders list (forced servicing),
				 * then skip to the next order; effectively cancelling this forced service */
				if (BitOps.HASBIT(v.current_order.flags, OFB_PART_OF_ORDERS))
					v.cur_order_index++;

				v.current_order.type = Order.OT_DUMMY;
				v.current_order.flags = 0;
				InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
			}
			return 0;
		}

		dep = FindClosestRoadDepot(v);
		if (dep == null) return_cmd_error(Str.STR_9019_UNABLE_TO_FIND_LOCAL_DEPOT);

		if (flags & Cmd.DC_EXEC) {
			v.current_order.type = Order.OT_GOTO_DEPOT;
			v.current_order.flags = Order.OF_NON_STOP | Order.OF_HALT_IN_DEPOT;
			v.current_order.station = dep.index;
			v.dest_tile = dep.xy;
			InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
		}

		return 0;
	}

	/** Turn a roadvehicle around.
	 * @param x,y unused
	 * @param p1 vehicle ID to turn
	 * @param p2 unused
	 */
	int CmdTurnRoadVeh(int x, int y, int flags, int p1, int p2)
	{
		Vehicle v;

		if (!IsVehicleIndex(p1)) return Cmd.CMD_ERROR;

		v = GetVehicle(p1);

		if (v.type != Vehicle.VEH_Road || !CheckOwnership(v.owner)) return Cmd.CMD_ERROR;

		if (v.vehstatus & (Vehicle.VS_HIDDEN|Vehicle.VS_STOPPED) ||
				v.road.crashed_ctr != 0 ||
				v.breakdown_ctr != 0 ||
				v.road.overtaking != 0 ||
				v.cur_speed < 5) {
			return Cmd.CMD_ERROR;
		}

		if (flags & Cmd.DC_EXEC) {
			v.road.reverse_ctr = 180;
		}

		return 0;
	}

	/** Change the service interval for road vehicles.
	 * @param x,y unused
	 * @param p1 vehicle ID that is being service-interval-changed
	 * @param p2 new service interval
	 */
	int CmdChangeRoadVehServiceInt(int x, int y, int flags, int p1, int p2)
	{
		Vehicle v;
		int serv_int = GetServiceIntervalClamped(p2); /* Double check the service interval from the user-input */

		if (serv_int != p2 || !IsVehicleIndex(p1)) return Cmd.CMD_ERROR;

		v = GetVehicle(p1);

		if (v.type != Vehicle.VEH_Road || !CheckOwnership(v.owner)) return Cmd.CMD_ERROR;

		if (flags & Cmd.DC_EXEC) {
			v.service_interval = serv_int;
			InvalidateWindowWidget(Window.WC_VEHICLE_DETAILS, v.index, 7);
		}

		return 0;
	}


	static void MarkRoadVehDirty(Vehicle v)
	{
		v.cur_image = GetRoadVehImage(v, v.direction);
		ViewPort.MarkAllViewportsDirty(v.left_coord, v.top_coord, v.right_coord + 1, v.bottom_coord + 1);
	}

	static void UpdateRoadVehDeltaXY(Vehicle v)
	{
		int x = _delta_xy_table[v.direction];
		v.x_offs        = BitOps.GB(x,  0, 8);
		v.y_offs        = BitOps.GB(x,  8, 8);
		v.sprite_width  = (byte) BitOps.GB(x, 16, 8);
		v.sprite_height = (byte) BitOps.GB(x, 24, 8);
	}

	static void ClearCrashedStation(Vehicle v)
	{
		RoadStop rs = GetRoadStopByTile(v.tile, GetRoadStopType(v.tile));

		// mark station as not busy
		rs.status = BitOps.RETCLRBIT(rs.status, 7);

		// free parking bay
		rs.status = BitOps.RETSETBIT(rs.status, BitOps.HASBIT(v.road.state, 1) ? 1 : 0);
	}

	static void RoadVehDelete(Vehicle v)
	{
		Window.DeleteWindowById(Window.WC_VEHICLE_VIEW, v.index);
		Window.InvalidateWindow(Window.WC_VEHICLE_DETAILS, v.index);

		RebuildVehicleLists();
		Window.InvalidateWindow(Window.WC_COMPANY, v.owner);

		if (v.tile.IsTileType(TileTypes.MP_STATION))
			ClearCrashedStation(v);

		v.BeginVehicleMove();
		v.EndVehicleMove();

		ClearSlot(v, v.road.slot);
		Vehicle.DeleteVehicle(v);
	}

	static byte SetRoadVehPosition(Vehicle v, int x, int y)
	{
		byte new_z, old_z;

		// need this hint so it returns the right z coordinate on bridges.
		_get_z_hint = v.z_pos;
		new_z = Landscape.GetSlopeZ(v.x_pos=x, v.y_pos=y);
		_get_z_hint = 0;

		old_z = v.z_pos;
		v.z_pos = new_z;

		v.VehiclePositionChanged();
		v.EndVehicleMove();
		return old_z;
	}

	static void RoadVehSetRandomDirection(Vehicle v)
	{
		int r = Hal.Random();
		v.direction = (v.direction+_turn_prob[r&3])&7;
		BeginVehicleMove(v);
		UpdateRoadVehDeltaXY(v);
		v.cur_image = GetRoadVehImage(v, v.direction);
		SetRoadVehPosition(v, v.x_pos, v.y_pos);
	}

	static void RoadVehIsCrashed(Vehicle v)
	{
		v.road.crashed_ctr++;
		if (v.road.crashed_ctr == 2) {
			CreateEffectVehicleRel(v, 4, 4, 8, EV_EXPLOSION_LARGE);
		} else if (v.road.crashed_ctr <= 45) {
			if ((v.tick_counter&7)==0)
				RoadVehSetRandomDirection(v);
		} else if (v.road.crashed_ctr >= 2220) {
			RoadVehDelete(v);
		}
	}

	static void EnumCheckRoadVehCrashTrain(Vehicle v, Vehicle u)
	{
		if (v.type != Vehicle.VEH_Train ||
				Math.abs(v.z_pos - u.z_pos) > 6 ||
				Math.abs(v.x_pos - u.x_pos) > 4 ||
				Math.abs(v.y_pos - u.y_pos) > 4)
			return null;
		return v;
	}

	static void RoadVehCrash(Vehicle v)
	{
		int pass;

		v.road.crashed_ctr++;
		v.vehstatus |= Vehicle.VS_CRASHED;

		InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);

		pass = 1;
		if (v.cargo_type == 0)
			pass += v.cargo_count;
		v.cargo_count = 0;
		Global.SetDParam(0, pass);

		NewsItem.AddNewsItem(
				(pass == 1) ?
						Str.STR_9031_ROAD_VEHICLE_CRASH_DRIVER : Str.STR_9032_ROAD_VEHICLE_CRASH_DIE,
						NewsItem.NEWS_FLAGS(NewsItem.NM_THIN, NewsItem.NF_VIEWPORT|NewsItem.NF_VEHICLE, NewsItem.NT_ACCIDENT, 0),
						v.index,
						0);

		Station.ModifyStationRatingAround(v.tile, v.owner, -160, 22);
		//SndPlayVehicleFx(SND_12_EXPLOSION, v);
	}

	static void RoadVehCheckTrainCrash(Vehicle v)
	{
		TileIndex tile;

		if (v.road.state == 255)
			return;

		tile = v.tile;

		// Make sure it's a road/rail crossing
		if (!tile.IsTileType( TileTypes.MP_STREET) || !tile.IsLevelCrossing())
			return;

		if (VehicleFromPos(tile, v, (VehicleFromPosProc)EnumCheckRoadVehCrashTrain) != null)
			RoadVehCrash(v);
	}

	static void HandleBrokenRoadVeh(Vehicle v)
	{
		if (v.breakdown_ctr != 1) {
			v.breakdown_ctr = 1;
			v.cur_speed = 0;

			if (v.breakdowns_since_last_service != 255)
				v.breakdowns_since_last_service++;

			Window.InvalidateWindow(Window.WC_VEHICLE_VIEW, v.index);
			Window.InvalidateWindow(Window.WC_VEHICLE_DETAILS, v.index);

			//SndPlayVehicleFx((GameOptions._opt.landscape != Landscape.LT_CANDY) ?					SND_0F_VEHICLE_BREAKDOWN : SND_35_COMEDY_BREAKDOWN, v);

			if (!(v.vehstatus & Vehicle.VS_HIDDEN)) {
				Vehicle u = CreateEffectVehicleRel(v, 4, 4, 5, EV_BREAKDOWN_SMOKE);
				if (u)
					u.special.unk0 = v.breakdown_delay * 2;
			}
		}

		if (!(v.tick_counter & 1)) {
			if (!--v.breakdown_delay) {
				v.breakdown_ctr = 0;
				Window.InvalidateWindow(Window.WC_VEHICLE_VIEW, v.index);
			}
		}
	}

	static void ProcessRoadVehOrder(Vehicle v)
	{
		final Order order;
		final Station st;

		if (v.current_order.type >= Order.OT_GOTO_DEPOT && v.current_order.type <= Order.OT_LEAVESTATION) {
			// Let a depot order in the orderlist interrupt.
			if (v.current_order.type != Order.OT_GOTO_DEPOT ||
					!(v.current_order.flags & Order.OF_UNLOAD))
				return;
		}

		if (v.current_order.type == Order.OT_GOTO_DEPOT &&
				(v.current_order.flags & (Order.OF_PART_OF_ORDERS | Order.OF_SERVICE_IF_NEEDED)) == (Order.OF_PART_OF_ORDERS | Order.OF_SERVICE_IF_NEEDED) &&
				!v.VehicleNeedsService()) {
			v.cur_order_index++;
		}

		if (v.cur_order_index >= v.num_orders)
			v.cur_order_index = 0;

		order = v.GetVehicleOrder(v.cur_order_index);

		if (order == null) {
			v.current_order.type = Order.OT_NOTHING;
			v.current_order.flags = 0;
			v.dest_tile = null;
			return;
		}

		if (order.type    == v.current_order.type &&
				order.flags   == v.current_order.flags &&
				order.station == v.current_order.station)
			return;

		v.current_order = *order;

		v.dest_tile = 0;

		if (order.type == Order.OT_GOTO_STATION) {
			if (order.station == v.last_station_visited)
				v.last_station_visited = INVALID_STATION;
			st = Station.GetStation(order.station);

			{
				int mindist = 0xFFFFFFFF;
				RoadStopType type;
				RoadStop rs;

				type = (v.cargo_type == AcceptedCargo.CT_PASSENGERS) ? RoadStopType.RS_BUS : RoadStopType.RS_TRUCK;
				rs = GetPrimaryRoadStop(st, type);

				if (rs == null) {
					//There is no stop left at the station, so don't even TRY to go there
					v.cur_order_index++;
					v.InvalidateVehicleOrder();

					return;
				}

				for (rs = GetPrimaryRoadStop(st, type); rs != null; rs = rs.next) {
					if (Map.DistanceManhattan(v.tile, rs.xy) < mindist) {
						v.dest_tile = rs.xy;
					}
				}

			}
		} else if (order.type == Order.OT_GOTO_DEPOT) {
			v.dest_tile = Depot.GetDepot(order.station).xy;
		}

		v.InvalidateVehicleOrder();
	}

	static void HandleRoadVehLoading(Vehicle v)
	{
		if (v.current_order.type == Order.OT_NOTHING)
			return;

		if (v.current_order.type != Order.OT_DUMMY) {
			if (v.current_order.type != Order.OT_LOADING)
				return;

			if (--v.load_unload_time_rem > 0)
				return;

			if (0 != (v.current_order.flags & Order.OF_FULL_LOAD) && v.CanFillVehicle()) {
				Player.SET_EXPENSES_TYPE(Player.EXPENSES_ROADVEH_INC);
				if (v.LoadUnloadVehicle()) {
					Window.InvalidateWindow(Window.WC_ROADVEH_LIST, v.owner);
					MarkRoadVehDirty(v);
				}
				return;
			}

			{
				Order b = v.current_order;
				v.current_order.type = Order.OT_LEAVESTATION;
				v.current_order.flags = 0;
				if (0==(b.flags & Order.OF_NON_STOP))
					return;
			}
		}

		v.cur_order_index++;
		v.InvalidateVehicleOrder();
	}

	static void StartRoadVehSound(Vehicle v)
	{
		/*
	SoundFx s = Engine.RoadVehInfo(v.engine_type).sfx;
	if (s == SND_19_BUS_START_PULL_AWAY && (v.tick_counter & 3) == 0)
		s = SND_1A_BUS_START_PULL_AWAY_WITH_HORN;
	SndPlayVehicleFx(s, v);
		 */
	}


	static Object EnumCheckRoadVehClose(Vehicle v, RoadVehFindData rvf)
	{

		int x_diff = v.x_pos - rvf.x;
		int y_diff = v.y_pos - rvf.y;

		if (rvf.veh == v ||
				v.type != Vehicle.VEH_Road ||
				v.road.state == 254 ||
				Math.abs(v.z_pos - rvf.veh.z_pos) > 6 ||
				v.direction != rvf.dir ||
				(_dists[v.direction] < 0 && (x_diff <= _dists[v.direction] || x_diff > 0)) ||
				(_dists[v.direction] > 0 && (x_diff >= _dists[v.direction] || x_diff < 0)) ||
				(_dists[v.direction+8] < 0 && (y_diff <= _dists[v.direction+8] || y_diff > 0)) ||
				(_dists[v.direction+8] > 0 && (y_diff >= _dists[v.direction+8] || y_diff < 0)))
			return null;

		return v;
	}

	static Vehicle RoadVehFindCloseTo(Vehicle v, int x, int y, byte dir)
	{
		RoadVehFindData rvf = new RoadVehFindData();
		Vehicle u;

		if (v.road.reverse_ctr != 0)
			return null;

		rvf.x = x;
		rvf.y = y;
		rvf.dir = dir;
		rvf.veh = v;
		u = VehicleFromPos(TileIndex.TileVirtXY(x, y), rvf, (VehicleFromPosProc)EnumCheckRoadVehClose);

		// This code protects a roadvehicle from being blocked for ever
		//  If more than 1480 / 74 days a road vehicle is blocked, it will
		//  drive just through it. The ultimate backup-code of TTD.
		// It can be disabled.
		if (u == null) {
			v.road.unk2 = 0;
			return null;
		}

		if (++v.road.unk2 > 1480)
			return null;

		return u;
	}

	static void RoadVehArrivesAt(final Vehicle  v, Station  st)
	{
		if (v.cargo_type == AcceptedCargo.CT_PASSENGERS) {
			/* Check if station was ever visited before */
			if (!(st.had_vehicle_of_type & Station.HVOT_BUS)) {
				int flags;

				st.had_vehicle_of_type |= Station.HVOT_BUS;
				Global.SetDParam(0, st.index);
				flags = (v.owner == Global._local_player) ? NewsItem.NEWS_FLAGS(NewsItem.NM_THIN, NewsItem.NF_VIEWPORT|NewsItem.NF_VEHICLE, NewsItem.NT_ARRIVAL_PLAYER, 0) : NewsItem.NEWS_FLAGS(NewsItem.NM_THIN, NewsItem.NF_VIEWPORT|NewsItem.NF_VEHICLE, NewsItem.NT_ARRIVAL_OTHER, 0);
				NewsItem.AddNewsItem(
						Str.STR_902F_CITIZENS_CELEBRATE_FIRST,
						flags,
						v.index,
						0);
			}
		} else {
			/* Check if station was ever visited before */
			if (0==(st.had_vehicle_of_type & Station.HVOT_TRUCK)) {
				int flags;

				st.had_vehicle_of_type |= Station.HVOT_TRUCK;
				Global.SetDParam(0, st.index);
				flags = (v.owner == Global._local_player) ? NewsItem.NEWS_FLAGS(NewsItem.NM_THIN, NewsItem.NF_VIEWPORT|NewsItem.NF_VEHICLE, NewsItem.NT_ARRIVAL_PLAYER, 0) : NewsItem.NEWS_FLAGS(NewsItem.NM_THIN, NewsItem.NF_VIEWPORT|NewsItem.NF_VEHICLE, NewsItem.NT_ARRIVAL_OTHER, 0);
				NewsItem.AddNewsItem(
						Str.STR_9030_CITIZENS_CELEBRATE_FIRST,
						flags,
						v.index,
						0);
			}
		}
	}

	static boolean RoadVehAccelerate(Vehicle v)
	{
		int spd = v.cur_speed + 1 + ((v.road.overtaking != 0)?1:0);
		byte t;

		// Clamp
		spd = Math.min(spd, v.max_speed);

		//updates statusbar only if speed have changed to save CPU time
		if (spd != v.cur_speed) {
			v.cur_speed = spd;
			if (Global._patches.vehicle_speed)
				InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
		}

		// Decrease somewhat when turning
		if (0==(v.direction&1))
			spd = spd * 3 >> 2;

			if (spd == 0)
				return false;

			if ((byte)++spd == 0)
				return true;

			v.progress = (t = v.progress) - (byte)spd;

			return (t < v.progress);
	}

	static int RoadVehGetNewDirection(Vehicle v, int x, int y)
	{

		x = x - v.x_pos + 1;
		y = y - v.y_pos + 1;

		if ((int)x > 2 || (int)y > 2)
			return v.direction;
		return _roadveh_new_dir[y*4+x];
	}

	static int RoadVehGetSlidingDirection(Vehicle v, int x, int y)
	{
		int b = RoadVehGetNewDirection(v,x,y);
		int d = v.direction;
		if (b == d) return d;
		d = (d+1)&7;
		if (b==d) return d;
		d = (d-2)&7;
		if (b==d) return d;
		if (b==((d-1)&7)) return d;
		if (b==((d-2)&7)) return d;
		return (d+2)&7;
	}



	static Object EnumFindVehToOvertake(Vehicle v, OvertakeData od)
	{
		if (v.tile != od.tile || v.type != Vehicle.VEH_Road || v == od.u || v == od.v)
			return null;
		return v;
	}

	static boolean FindRoadVehToOvertake(OvertakeData od)
	{
		int bits;

		bits = GetTileTrackStatus(od.tile, TRANSPORT_ROAD)&0x3F;

		if (!(od.tilebits & bits) || (bits&0x3C) || (bits & 0x3F3F0000))
			return true;
		return VehicleFromPos(od.tile, od, (VehicleFromPosProc)EnumFindVehToOvertake) != null;
	}

	static void RoadVehCheckOvertake(Vehicle v, Vehicle u)
	{
		OvertakeData od = new OvertakeData();
		byte tt;

		od.v = v;
		od.u = u;

		if (u.max_speed >= v.max_speed &&
				0==(u.vehstatus&Vehicle.VS_STOPPED) &&
				u.cur_speed != 0)
			return;

		if (v.direction != u.direction || !(v.direction&1))
			return;

		if (v.road.state >= 32 || (v.road.state&7) > 1 )
			return;

		tt = GetTileTrackStatus(v.tile, TRANSPORT_ROAD) & 0x3F;
		if ((tt & 3) == 0)
			return;
		if ((tt & 0x3C) != 0)
			return;

		if (tt == 3) {
			tt = (v.direction&2)?2:1;
		}
		od.tilebits = tt;

		od.tile = v.tile;
		if (FindRoadVehToOvertake(od))
			return;

		od.tile = v.tile.iadd( TileIndex.TileOffsByDir(v.direction >> 1) );
		if (FindRoadVehToOvertake(od))
			return;

		if (od.cur_speed == 0 || od.vehstatus&Vehicle.VS_STOPPED) {
			v.road.overtaking_ctr = 0x11;
			v.road.overtaking = 0x10;
		} else {
			//		if (FindRoadVehToOvertake(&od))
			//			return;
			v.road.overtaking_ctr = 0;
			v.road.overtaking = 0x10;
		}
	}

	static void RoadZPosAffectSpeed(Vehicle v, byte old_z)
	{
		if (old_z == v.z_pos)
			return;

		if (old_z < v.z_pos) {
			v.cur_speed = v.cur_speed * 232 >> 8;
		} else {
			int spd = v.cur_speed + 2;
			if (spd <= v.max_speed)
				v.cur_speed = spd;
		}
	}

	static int PickRandomBit(int bits)
	{
		int num = 0;
		int b = bits;
		int i;

		do {
			if (b & 1)
				num++;
		} while (b >>= 1 != 0);

		num = Hal.RandomRange(num);

		for(i=0; !((bits & 1) && ((int)--num) < 0); bits>>=1,i++);
		return i;
	}


	static boolean EnumRoadTrackFindDist(TileIndex tile, FindRoadToChooseData frd, int track, int length, Object state)
	{
		int dist = Map.DistanceManhattan(tile, frd.dest);
		if (dist <= frd.mindist) {
			if (dist != frd.mindist || length < frd.maxtracklen) {
				frd.maxtracklen = length;
			}
			frd.mindist = dist;
		}
		return false;
	}

	//#define return_track(x) {best_track = x; goto found_best_track; }

	// Returns direction to choose
	// or -1 if the direction is currently blocked
	static int RoadFindPathToDest(Vehicle v, TileIndex tile, int enterdir)
	{

		int signal;
		int bitmask;
		TileIndex desttile;
		FindRoadToChooseData frd;
		int best_track;
		int best_dist, best_maxlen;
		int i;
		byte m5;

		{
			int r;
			r = GetTileTrackStatus(tile, TRANSPORT_ROAD);
			signal  = BitOps.GB(r, 16, 16);
			bitmask = BitOps.GB(r,  0, 16);
		}

		if (tile.IsTileType( TileTypes.MP_STREET)) {
			if (BitOps.GB(tile.getMap().m5, 4, 4) == 2 && IsTileOwner(tile, v.owner)) {
				/* Road depot */
				bitmask |= _road_veh_fp_ax_or[BitOps.GB(tile.getMap().m5, 0, 2)];
			}
		} else if (tile.IsTileType( TileTypes.MP_STATION)) {
			if (IsTileOwner(tile, Owner.OWNER_NONE) || IsTileOwner(tile, v.owner)) {
				/* Our station */
				final Station  st = Station.GetStation(tile.getMap().m2);
				byte val = tile.getMap().m5;
				if (v.cargo_type != AcceptedCargo.CT_PASSENGERS) {
					if (IS_BYTE_INSIDE(val, 0x43, 0x47) && (Global._patches.roadveh_queue || st.truck_stops.status&3))
						bitmask |= _road_veh_fp_ax_or[(val-0x43)&3];
				} else {
					if (IS_BYTE_INSIDE(val, 0x47, 0x4B) && (Global._patches.roadveh_queue || st.bus_stops.status&3))
						bitmask |= _road_veh_fp_ax_or[(val-0x47)&3];
				}
			}
		}
		/* The above lookups should be moved to GetTileTrackStatus in the
		 * future, but that requires more changes to the pathfinder and other
		 * stuff, probably even more arguments to GTTS.
		 */

		/* remove unreachable tracks */
		bitmask &= _road_veh_fp_ax_and[enterdir];
		if (bitmask == 0) {
			/* No reachable tracks, so we'll reverse */
			return_track(_road_reverse_table[enterdir]);
		}

		if (v.road.reverse_ctr != 0) {
			/* What happens here?? */
			v.road.reverse_ctr = 0;
			if (v.tile != tile) {
				return_track(_road_reverse_table[enterdir]);
			}
		}

		desttile = v.dest_tile;
		if (desttile == 0) {
			// Pick a random track
			return_track(PickRandomBit(bitmask));
		}

		// Only one track to choose between?
		if (!(KillFirstBit2x64(bitmask))) {
			return_track(FindFirstBit2x64(bitmask));
		}

		if (Global._patches.new_pathfinding_all) {
			NPFFindStationOrTileData fstd;
			NPFFoundTargetData ftd;
			byte trackdir;

			NPFFillWithOrderData(&fstd, v);
			trackdir = DiagdirToDiagTrackdir(enterdir);
			//debug("Finding path. Enterdir: %d, Trackdir: %d", enterdir, trackdir);

			ftd = NPFRouteToStationOrTile(tile - TileOffsByDir(enterdir), trackdir, &fstd, TRANSPORT_ROAD, v.owner, INVALID_RAILTYPE, PBS_MODE_NONE);
			if (ftd.best_trackdir == 0xff) {
				/* We are already at our target. Just do something */
				//TODO: maybe display error?
				//TODO: go straight ahead if possible?
				return_track(FindFirstBit2x64(bitmask));
			} else {
				/* If ftd.best_bird_dist is 0, we found our target and ftd.best_trackdir contains
			the direction we need to take to get there, if ftd.best_bird_dist is not 0,
			we did not find our target, but ftd.best_trackdir contains the direction leading
			to the tile closest to our target. */
				return_track(ftd.best_trackdir);
			}
		} else {
			if (IsTileType(desttile, TileTypes.MP_STREET)) {
				m5 = _m[desttile].m5;
				if ((m5&0xF0) == 0x20)
					/* We are heading for a Depot /
				goto do_it;
		} else if (IsTileType(desttile, TileTypes.MP_STATION)) {
			m5 = _m[desttile].m5;
			if (IS_BYTE_INSIDE(m5, 0x43, 0x4B)) {
				/* We are heading for a station */
					m5 -= 0x43;
				do_it:;
				/* When we are heading for a depot or station, we just
				 * pretend we are heading for the tile in front, we'll
				 * see from there */
				desttile += TileOffsByDir(m5 & 3);
				if (desttile == tile && bitmask&_road_pf_table_3[m5&3]) {
					/* If we are already in front of the
					 * station/depot and we can get in from here,
					 * we enter */
					return_track(FindFirstBit2x64(bitmask&_road_pf_table_3[m5&3]));
				}
			}
		}
		// do pathfind
		frd.dest = desttile;

		best_track = -1;
		best_dist = (int)-1;
		best_maxlen = (int)-1;
		i = 0;
		do {
			if (bitmask & 1) {
				if (best_track == -1) best_track = i; // in case we don't find the path, just pick a track
				frd.maxtracklen = (int)-1;
				frd.mindist = (int)-1;
				FollowTrack(tile, 0x3000 | TRANSPORT_ROAD, _road_pf_directions[i], (TPFEnumProc*)EnumRoadTrackFindDist, null, &frd);

				if (frd.mindist < best_dist || (frd.mindist==best_dist && frd.maxtracklen < best_maxlen)) {
					best_dist = frd.mindist;
					best_maxlen = frd.maxtracklen;
					best_track = i;
				}
			}
		} while (++i,(bitmask>>=1) != 0);
	}

	found_best_track:;

	if (BitOps.HASBIT(signal, best_track))
		return -1;

	return best_track;
}

/*
static int RoadFindPathToStation(final Vehicle v, TileIndex tile)
{
  NPFFindStationOrTileData fstd;
  byte trackdir = GetVehicleTrackdir(v);
	assert(trackdir != 0xFF);

  fstd.dest_coords = tile;
  fstd.station_index = -1;	// indicates that the destination is a tile, not a station

  return NPFRouteToStationOrTile(v.tile, trackdir, &fstd, TRANSPORT_ROAD, v.owner, INVALID_RAILTYPE, PBS_MODE_NONE).best_path_dist;
}
 */
/*
class RoadDriveEntry {
	byte x,y;
} RoadDriveEntry;
 */



static void RoadVehController(Vehicle v)
{
	GetNewVehiclePosResult gp = new GetNewVehiclePosResult();
	byte new_dir, old_dir;
	RoadDriveEntry rd;
	int x,y;
	Station st;
	int r;
	Vehicle u;

	// decrease counters
	v.tick_counter++;
	if (v.road.reverse_ctr != 0)
		v.road.reverse_ctr--;

	// handle crashed
	if (v.road.crashed_ctr != 0) {
		RoadVehIsCrashed(v);
		return;
	}

	RoadVehCheckTrainCrash(v);

	// road vehicle has broken down?
	if (v.breakdown_ctr != 0) {
		if (v.breakdown_ctr <= 2) {
			HandleBrokenRoadVeh(v);
			return;
		}
		v.breakdown_ctr--;
	}

	// exit if vehicle is stopped
	if (v.vehstatus & Vehicle.VS_STOPPED)
		return;

	ProcessRoadVehOrder(v);
	HandleRoadVehLoading(v);

	if (v.current_order.type == Order.OT_LOADING)
		return;

	if (v.road.state == 254) {
		int dir;
		//final RoadDriveEntry rdp;
		final Point rdp;
		byte rd2;

		v.cur_speed = 0;

		dir = BitOps.GB(_m[v.tile].m5, 0, 2);
		v.direction = dir*2+1;

		rd2 = _roadveh_data_2[dir];
		rdp = _road_drive_data[(GameOptions._opt.road_side<<4) + rd2];

		x = TileX(v.tile) * 16 + (rdp[6].x & 0xF);
		y = TileY(v.tile) * 16 + (rdp[6].y & 0xF);

		if (RoadVehFindCloseTo(v,x,y,v.direction))
			return;

		VehicleServiceInDepot(v);

		StartRoadVehSound(v);

		BeginVehicleMove(v);

		v.vehstatus &= ~Vehicle.VS_HIDDEN;
		v.road.state = rd2;
		v.road.frame = 6;

		v.cur_image = GetRoadVehImage(v, v.direction);
		UpdateRoadVehDeltaXY(v);
		SetRoadVehPosition(v,x,y);

		Window.InvalidateWindow(Window.WC_VEHICLE_DEPOT, v.tile);
		return;
	}

	if (!RoadVehAccelerate(v))
		return;

	if (v.road.overtaking != 0)  {
		if (++v.road.overtaking_ctr >= 35)
			v.road.overtaking = 0;
	}

	BeginVehicleMove(v);

	if (v.road.state == 255) {
		GetNewVehiclePos(v, gp);

		if (RoadVehFindCloseTo(v, gp.x, gp.y, v.direction)) {
			v.cur_speed = 0;
			return;
		}

		if (IsTileType(gp.new_tile, TileTypes.MP_TUNNELBRIDGE) &&
				BitOps.GB(_m[gp.new_tile].m5, 4, 4) == 0 &&
				(VehicleEnterTile(v, gp.new_tile, gp.x, gp.y)&4)) {

			//new_dir = RoadGetNewDirection(v, gp.x, gp.y)
			v.cur_image = GetRoadVehImage(v, v.direction);
			UpdateRoadVehDeltaXY(v);
			SetRoadVehPosition(v,gp.x,gp.y);
			return;
		}

		v.x_pos = gp.x;
		v.y_pos = gp.y;
		VehiclePositionChanged(v);
		return;
	}

	rd = _road_drive_data[(v.road.state + (GameOptions._opt.road_side<<4)) ^ v.road.overtaking][v.road.frame+1];

	// switch to another tile
	if (rd.x & 0x80) {
		TileIndex tile = v.tile + TileOffsByDir(rd.x & 3);
		int dir = RoadFindPathToDest(v, tile, rd.x&3);
		int tmp;
		int r;
		byte newdir;
		//final RoadDriveEntry rdp;
		final Point rdp;

		if (dir == -1) {
			v.cur_speed = 0;
			return;
		}

		again:
			if ((dir & 7) >= 6) {
				/* Turning around */
				tile = v.tile;
			}

		tmp = (dir+(GameOptions._opt.road_side<<4))^v.road.overtaking;
		rdp = _road_drive_data[tmp];

		tmp &= ~0x10;

		x = tile.TileX() * 16 + rdp[0].x;
		y = tile.TileY() * 16 + rdp[0].y;

		if (RoadVehFindCloseTo(v, x, y, newdir=RoadVehGetSlidingDirection(v, x, y)))
			return;

		r = VehicleEnterTile(v, tile, x, y);
		if (r & 8) {
			if (!tile.IsTileType( TileTypes.MP_TUNNELBRIDGE)) {
				v.cur_speed = 0;
				return;
			}
			dir = _road_reverse_table[rd.x&3];
			goto again;
		}

		if (IS_BYTE_INSIDE(v.road.state, 0x20, 0x30) && IsTileType(v.tile, TileTypes.MP_STATION)) {
			if ((tmp&7) >= 6) { v.cur_speed = 0; return; }
			if (IS_BYTE_INSIDE(_m[v.tile].m5, 0x43, 0x4B)) {
				RoadStop *rs = GetRoadStopByTile(v.tile, GetRoadStopType(v.tile));
				byte *b = &rs.status;

				//we have reached a loading bay, mark it as used
				//and clear the usage bit (0x80) of the stop
				*b = (*b | ((v.road.state&2)?2:1)) & ~0x80;
			}
		}

		if (!(r & 4)) {
			v.tile = tile;
			v.road.state = (byte)tmp;
			v.road.frame = 0;
		}
		if (newdir != v.direction) {
			v.direction = newdir;
			v.cur_speed -= v.cur_speed >> 2;
		}

		v.cur_image = GetRoadVehImage(v, newdir);
		UpdateRoadVehDeltaXY(v);
		RoadZPosAffectSpeed(v, SetRoadVehPosition(v, x, y));
		return;
	}

	if (rd.x & 0x40) {
		int dir = RoadFindPathToDest(v, v.tile,	rd.x&3);
		int r;
		int tmp;
		byte newdir;
		final RoadDriveEntry *rdp;

		if (dir == -1) {
			v.cur_speed = 0;
			return;
		}

		tmp = (GameOptions._opt.road_side<<4) + dir;
		rdp = _road_drive_data[tmp];

		x = TileX(v.tile) * 16 + rdp[1].x;
		y = TileY(v.tile) * 16 + rdp[1].y;

		if (RoadVehFindCloseTo(v, x, y, newdir=RoadVehGetSlidingDirection(v, x, y)))
			return;

		r = VehicleEnterTile(v, v.tile, x, y);
		if (r & 8) {
			v.cur_speed = 0;
			return;
		}

		v.road.state = tmp & ~16;
		v.road.frame = 1;

		if (newdir != v.direction) {
			v.direction = newdir;
			v.cur_speed -= v.cur_speed >> 2;
		}

		v.cur_image = GetRoadVehImage(v, newdir);
		UpdateRoadVehDeltaXY(v);
		RoadZPosAffectSpeed(v, SetRoadVehPosition(v, x, y));
		return;
	}

	x = (v.x_pos&~15)+(rd.x&15);
	y = (v.y_pos&~15)+(rd.y&15);

	new_dir = RoadVehGetSlidingDirection(v, x, y);

	if (!IS_BYTE_INSIDE(v.road.state, 0x20, 0x30) && (u=RoadVehFindCloseTo(v, x, y, new_dir)) != null) {
		if (v.road.overtaking == 0)
			RoadVehCheckOvertake(v, u);
		return;
	}

	old_dir = v.direction;
	if (new_dir != old_dir) {
		v.direction = new_dir;
		v.cur_speed -= (v.cur_speed >> 2);
		if (old_dir != v.road.state) {
			v.cur_image = GetRoadVehImage(v, new_dir);
			UpdateRoadVehDeltaXY(v);
			SetRoadVehPosition(v, v.x_pos, v.y_pos);
			return;
		}
	}

	if (v.road.state >= 0x20 &&
			_road_veh_data_1[v.road.state - 0x20 + (GameOptions._opt.road_side<<4)] == v.road.frame) {
		RoadStop *rs = GetRoadStopByTile(v.tile, GetRoadStopType(v.tile));
		byte *b = &rs.status;

		st = Station.GetStation(_m[v.tile].m2);

		if (v.current_order.type != Order.OT_LEAVESTATION &&
				v.current_order.type != Order.OT_GOTO_DEPOT) {
			Order old_order;

			*b &= ~0x80;

			v.last_station_visited = _m[v.tile].m2;

			RoadVehArrivesAt(v, st);

			old_order = v.current_order;
			v.current_order.type = Order.OT_LOADING;
			v.current_order.flags = 0;

			if (old_order.type == Order.OT_GOTO_STATION &&
					v.current_order.station == v.last_station_visited) {
				v.current_order.flags =
						(old_order.flags & (Order.OF_FULL_LOAD | Order.OF_UNLOAD | Order.OF_TRANSFER)) | Order.OF_NON_STOP;
			}

			Player.SET_EXPENSES_TYPE(Player.EXPENSES_ROADVehicle.VEH_INC);
			if (LoadUnloadVehicle(v)) {
				Window.InvalidateWindow(Window.WC_ROADVehicle.VEH_LIST, v.owner);
				MarkRoadVehDirty(v);
			}
			InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
			return;
		}

		if (v.current_order.type != Order.OT_GOTO_DEPOT) {
			if (*b&0x80) {
				v.cur_speed = 0;
				return;
			}
			v.current_order.type = Order.OT_NOTHING;
			v.current_order.flags = 0;
		}
		*b |= 0x80;

		if (rs == v.road.slot) {
			//we have arrived at the correct station
			ClearSlot(v, rs);
		} else if (v.road.slot != null) {
			//we have arrived at the wrong station
			//XXX The question is .. what to do? Actually we shouldn't be here
			//but I guess we need to clear the slot
			Global.DEBUG_ms( 1, "Multistop: Wrong station, force a slot clearing. Vehicle %d at 0x%x, should go to 0x%x of station %d (%x), destination 0x%x", v.unitnumber, v.tile, v.road.slot.xy, st.index, st.xy, v.dest_tile);
			ClearSlot(v, v.road.slot);
		}

		StartRoadVehSound(v);
		InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
	}

	r = VehicleEnterTile(v, v.tile, x, y);
	if (r & 8) {
		v.cur_speed = 0;
		return;
	}

	if ((r & 4) == 0) {
		v.road.frame++;
	}

	v.cur_image = GetRoadVehImage(v, v.direction);
	UpdateRoadVehDeltaXY(v);
	RoadZPosAffectSpeed(v, SetRoadVehPosition(v, x, y));
}

void RoadVehEnterDepot(Vehicle v)
{
	v.road.state = 254;
	v.vehstatus |= Vehicle.VS_HIDDEN;

	Window.InvalidateWindow(Window.WC_VEHICLE_DETAILS, v.index);

	VehicleServiceInDepot(v);

	TriggerVehicle(v, VEHICLE_TRIGGER_DEPOT);

	if (v.current_order.type == Order.OT_GOTO_DEPOT) {
		Order t;

		Window.InvalidateWindow(Window.WC_VEHICLE_VIEW, v.index);

		t = v.current_order;
		v.current_order.type = Order.OT_DUMMY;
		v.current_order.flags = 0;

		// Part of the orderlist?
		if (BitOps.HASBIT(t.flags, OFB_PART_OF_ORDERS)) {
			v.cur_order_index++;
		} else if (BitOps.HASBIT(t.flags, OFB_HALandscape.LT_IN_DEPOT)) {
			v.vehstatus |= Vehicle.VS_STOPPED;
			if (v.owner == Global._local_player) {
				Global.SetDParam(0, v.unitnumber);
				AddNewsItem(
						Str.STR_9016_ROAD_VEHICLE_IS_WAITING,
						NEWS_FLAGS(NM_SMALL, NF_VIEWPORT|NF_VEHICLE, NT_ADVICE, 0),
						v.index,
						0);
			}
		}
	}

	Window.InvalidateWindow(Window.WC_VEHICLE_DEPOT, v.tile);
	InvalidateWindowClasses(Window.WC_ROADVehicle.VEH_LIST);
}

static void AgeRoadVehCargo(Vehicle v)
{
	if (_age_cargo_skip_counter != 0)
		return;
	if (v.cargo_days != 255)
		v.cargo_days++;
}

void RoadVeh_Tick(Vehicle v)
{
	AgeRoadVehCargo(v);
	RoadVehController(v);
}

static void CheckIfRoadVehNeedsService(Vehicle v)
{
	Depot depot;

	if (Global._patches.servint_roadveh == 0)
		return;

	if (!VehicleNeedsService(v))
		return;

	if (v.vehstatus & Vehicle.VS_STOPPED)
		return;

	if (Global._patches.gotodepot && VehicleHasDepotOrders(v))
		return;

	// Don't interfere with a depot visit scheduled by the user, or a
	// depot visit by the order list.
	if (v.current_order.type == Order.OT_GOTO_DEPOT &&
			(v.current_order.flags & (Order.OF_HALT_IN_DEPOT | Order.OF_PART_OF_ORDERS)) != 0)
		return;

	//If we already got a slot at a stop, use that FIRST, and go to a depot later
	if (v.road.slot != null)
		return;

	depot = FindClosestRoadDepot(v);

	if (depot == null || DistanceManhattan(v.tile, depot.xy) > 12) {
		if (v.current_order.type == Order.OT_GOTO_DEPOT) {
			v.current_order.type = Order.OT_DUMMY;
			v.current_order.flags = 0;
			InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
		}
		return;
	}

	if (v.current_order.type == Order.OT_GOTO_DEPOT &&
			v.current_order.flags & Order.OF_NON_STOP &&
			!BitOps.CHANCE16(1,20))
		return;

	v.current_order.type = Order.OT_GOTO_DEPOT;
	v.current_order.flags = Order.OF_NON_STOP;
	v.current_order.station = depot.index;
	v.dest_tile = depot.xy;
	InvalidateWindowWidget(Window.WC_VEHICLE_VIEW, v.index, STATUS_BAR);
}

class dist_compare implements Comparator<Integer> {
	public int compare(Integer a, Integer b) {
		return a - b;
	}
}

void OnNewDay_RoadVeh(Vehicle v)
{
	int cost;
	Station st;

	if ((++v.day_counter & 7) == 0)
		v.DecreaseVehicleValue();

	if (v.road.unk2 == 0)
		v.CheckVehicleBreakdown();

	v.AgeVehicle();
	CheckIfRoadVehNeedsService(v);

	CheckOrders(v.index, OC_INIT);

	/* update destination */
	if (v.current_order.type == Order.OT_GOTO_STATION && !(v.vehstatus & Vehicle.VS_CRASHED)) {
		RoadStopType type = (v.cargo_type == AcceptedCargo.CT_PASSENGERS) ? RS_BUS : RS_TRUCK;

		st = Station.GetStation(v.current_order.station);

		//Current slot has expired
		if ( (v.road.slot_age++ <= 0) && (v.road.slot != null))
			ClearSlot(v, v.road.slot);

		//We do not have a slot, so make one
		if (v.road.slot == null) {
			RoadStop rs = GetPrimaryRoadStop(st, type);
			RoadStop first_stop = rs;
			RoadStop best_stop = null;
			int mindist = 12, dist; // 12 is threshold distance.

			//first we need to find out how far our stations are away.
			Global.DEBUG_ms( 2, "Multistop: Attempting to obtain a slot for vehicle %d at station %d (0x%x)", v.unitnumber, st.index, st.xy);
			for(; rs != null; rs = rs.next) {
				// Only consider those with at least a free slot.
				if (!(rs.slot[0] == INVALID_SLOT || rs.slot[1] == INVALID_SLOT))
					continue;

				// Previously the NPF pathfinder was used here even if NPF is OFF.. WTF?
				assert(NUM_SLOTS == 2);
				dist = DistanceManhattan(v.tile, rs.xy);

				// Check if the station is located BEHIND the vehicle..
				// In that case, add penalty.
				switch(v.direction) {
				case 1: // going north east,x position decreasing
					if (v.x_pos <= (int)TileX(rs.xy) * 16 + 15)
						dist += 6;
					break;
				case 3: // Going south east, y position increasing
					if (v.y_pos >= (int)TileY(rs.xy) * 16)
						dist += 6;
					break;
				case 5: // Going south west, x position increasing
					if (v.x_pos >= (int)TileX(rs.xy) * 16)
						dist += 6;
					break;
				case 7: // Going north west, y position decrasing.
					if (v.y_pos <= (int)TileY(rs.xy) * 16 + 15)
						dist += 6;
					break;
				}

				// Remember the one with the shortest distance
				if (dist < mindist) {
					mindist = dist;
					best_stop = rs;
				}
				Global.DEBUG_ms( 3, "Multistop: Distance to stop at 0x%x is %d", rs.xy, dist);
			}

			// best_stop now contains the best stop we found.
			if (best_stop) {
				int slot;
				// Find a free slot in this stop. We know that at least one is free.
				assert(best_stop.slot[0] == INVALID_SLOT || best_stop.slot[1] == INVALID_SLOT);
				slot = (best_stop.slot[0] == INVALID_SLOT) ? 0 : 1;
				best_stop.slot[slot] = v.index;
				v.road.slot = best_stop;
				v.dest_tile = best_stop.xy;
				v.road.slot_age = -5;
				v.road.slotindex = slot;
				Global.DEBUG_ms( 1, "Multistop: Slot %d at 0x%x assigned to vehicle %d (0x%x)", slot, best_stop.xy, v.unitnumber, v.tile);
			} else if (first_stop) {
				//now we couldn't assign a slot for one reason or another.
				//so we just go towards the first station
				Global.DEBUG_ms( 1, "Multistop: No free slot found for vehicle %d, going to default station", v.unitnumber);
				v.dest_tile = first_stop.xy;
			}
		}
	}

	if (v.vehstatus & Vehicle.VS_STOPPED)
		return;

	cost = RoadVehInfo(v.engine_type).running_cost * Global._price.roadveh_running / 364;

	v.profit_this_year -= cost >> 8;

				Player.SET_EXPENSES_TYPE(Player.EXPENSES_ROADVehicle.VEH_RUN);
				SubtractMoneyFromPlayerFract(v.owner, cost);

				Window.InvalidateWindow(Window.WC_VEHICLE_DETAILS, v.index);
				InvalidateWindowClasses(Window.WC_ROADVehicle.VEH_LIST);
}


void RoadVehiclesYearlyLoop()
{
	//Vehicle v;

	//FOR_ALL_VEHICLES(v)
	Vehicle.forEach( (v) ->
	{
		if (v.type == Vehicle.VEH_Road) {
			v.profit_last_year = v.profit_this_year;
			v.profit_this_year = 0;
			Window.InvalidateWindow(Window.WC_VEHICLE_DETAILS, v.index);
		}
	});
}

}