package game.net;

public interface NetServer extends NetTools, NetDefs
{

	// Is the network enabled?

	// **********
	// Sending functions
	//   void NetworkPacketSend_ ## type ## _command(NetworkClientState cs) has parameter: NetworkClientState cs
	// **********

	void NetworkPacketSend_PACKET_SERVER_CLIENT_INFO_command(NetworkClientState cs)()(NetworkClientState cs, NetworkClientInfo ci)
	{
		//
		// Packet: SERVER_CLIENT_INFO
		// Function: Sends info about a client
		// Data:
		//    int:  The index of the client (always unique on a server. 1 = server)
		//    byte:  As which player the client is playing
		//    String: The name of the client
		//    String: The unique id of the client
		//

		Packet p;

		if (ci.client_index != NETWORK_EMPTY_INDEX) {
			p = new Packet(PACKET_SERVER_CLIENT_INFO);
			NetworkSend_int(p, ci.client_index);
			NetworkSend_byte (p, ci.client_playas);
			NetworkSend_string(p, ci.client_name);
			NetworkSend_string(p, ci.unique_id);

			NetworkSend_Packet(p, cs);
		}
	}

	void NetworkPacketSend_PACKET_SERVER_COMPANY_INFO_command(NetworkClientState cs)()
	{
	//
		// Packet: SERVER_COMPANY_INFO
		// Function: Sends info about the companies
		// Data:
		//

		int i;

		Player player;
		Packet p;

		byte active = 0;

		FOR_ALL_PLAYERS(player) {
			if (player.is_active)
				active++;
		}

		if (active == 0) {
			Packet p = new Packet(PACKET_SERVER_COMPANY_INFO);

			NetworkSend_byte (p, NETWORK_COMPANY_INFO_VERSION);
			NetworkSend_byte (p, active);

			NetworkSend_Packet(p, cs);
			return;
		}

		NetworkPopulateCompanyInfo();

		FOR_ALL_PLAYERS(player) {
			if (!player.is_active)
				continue;

			p = new Packet(PACKET_SERVER_COMPANY_INFO);

			NetworkSend_byte (p, NETWORK_COMPANY_INFO_VERSION);
			NetworkSend_byte (p, active);
			NetworkSend_byte (p, player.index);

			NetworkSend_string(p, _network_player_info[player.index].company_name);
			NetworkSend_byte (p, _network_player_info[player.index].inaugurated_year);
			NetworkSend_int64(p, _network_player_info[player.index].company_value);
			NetworkSend_int64(p, _network_player_info[player.index].money);
			NetworkSend_int64(p, _network_player_info[player.index].income);
			NetworkSend_int(p, _network_player_info[player.index].performance);

			/* Send 1 if there is a passord for the company else send 0 */
			if (_network_player_info[player.index].password[0] != '\0') {
				NetworkSend_byte (p, 1);
			} else {
				NetworkSend_byte (p, 0);
			}

			for (i = 0; i < NETWORK_VEHICLE_TYPES; i++)
				NetworkSend_int(p, _network_player_info[player.index].num_vehicle[i]);

			for (i = 0; i < NETWORK_STATION_TYPES; i++)
				NetworkSend_int(p, _network_player_info[player.index].num_station[i]);

			if (_network_player_info[player.index].players[0] == '\0')
				NetworkSend_string(p, "<none>");
			else
				NetworkSend_string(p, _network_player_info[player.index].players);

			NetworkSend_Packet(p, cs);
		}

		p = new Packet(PACKET_SERVER_COMPANY_INFO);

		NetworkSend_byte (p, NETWORK_COMPANY_INFO_VERSION);
		NetworkSend_byte (p, 0);

		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_ERROR_command(NetworkClientState cs)()(NetworkClientState cs, NetworkErrorCode error)
	{
		//
		// Packet: SERVER_ERROR
		// Function: The client made an error
		// Data:
		//    byte:  ErrorID (see network_data.h, NetworkErrorCode)
		//

		NetworkClientState new_cs;
		char str[100];
		char client_name[NETWORK_CLIENT_NAME_LENGTH];

		Packet p = new Packet(PACKET_SERVER_ERROR);
		NetworkSend_byte(p, error);
		NetworkSend_Packet(p, cs);

		// Only send when the current client was in game
		if (cs.status > STATUS_AUTH) {
			NetworkGetClientName(client_name, sizeof(client_name), cs);

			Global.GetString(str, Str.STR_NETWORK_ERR_CLIENT_GENERAL + error);

			Global.DEBUG_net( 2)("[NET] %s made an error (%s) and his connection is closed", client_name, str);

			NetworkTextMessage(NETWORK_ACTION_LEAVE, 1, false, client_name, "%s", str);

			FOR_ALL_CLIENTS(new_cs) {
				if (new_cs.status > STATUS_AUTH && new_cs != cs) {
					// Some errors we filter to a more general error. Clients don't have to know the real
					//  reason a joining failed.
					if (error == NETWORK_ERROR_NOT_AUTHORIZED || error == NETWORK_ERROR_NOT_EXPECTED || error == NETWORK_ERROR_WRONG_REVISION)
						error = NETWORK_ERROR_ILLEGAL_PACKET;

					SEND_COMMAND(PACKET_SERVER_ERROR_QUIT)(new_cs, cs.index, error);
				}
			}
		} else {
			Global.DEBUG_net( 2)("[NET] Clientno %d has made an error and his connection is closed", cs.index);
		}

		cs.quited = true;

		// Make sure the data get's there before we close the connection
		NetworkSend_Packets(cs);

		// The client made a mistake, so drop his connection now!
		NetworkCloseClient(cs);
	}

	void NetworkPacketSend_PACKET_SERVER_NEED_PASSWORD_command(NetworkClientState cs)()(NetworkClientState cs, NetworkPasswordType type)
	{
		//
		// Packet: SERVER_NEED_PASSWORD
		// Function: Indication to the client that the server needs a password
		// Data:
		//    byte:  Type of password
		//

		Packet p = new Packet(PACKET_SERVER_NEED_PASSWORD);
		NetworkSend_byte(p, type);
		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_WELCOME_command(NetworkClientState cs)()
	{
		//
		// Packet: SERVER_WELCOME
		// Function: The client is joined and ready to receive his map
		// Data:
		//    int:  Own ClientID
		//

		Packet p;
		NetworkClientState new_cs;

		// Invalid packet when status is AUTH or higher
		if (cs.status >= STATUS_AUTH)
			return;

		cs.status = STATUS_AUTH;
		_network_game_info.clients_on++;

		p = new Packet(PACKET_SERVER_WELCOME);
		NetworkSend_int(p, cs.index);
		NetworkSend_Packet(p, cs);

			// Transmit info about all the active clients
		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs != cs && new_cs.status > STATUS_AUTH)
				SEND_COMMAND(PACKET_SERVER_CLIENT_INFO)(cs, DEREF_CLIENT_INFO(new_cs));
		}
		// Also send the info of the server
		SEND_COMMAND(PACKET_SERVER_CLIENT_INFO)(cs, NetworkFindClientInfoFromIndex(NETWORK_SERVER_INDEX));
	}

	void NetworkPacketSend_PACKET_SERVER_WAIT_command(NetworkClientState cs)()
	{
		//
		// Packet: PACKET_SERVER_WAIT
		// Function: The client can not receive the map at the moment because
		//             someone else is already receiving the map
		// Data:
		//    byte:  Clients awaiting map
		//
		int waiting = 0;
		NetworkClientState new_cs;
		Packet p;

		// Count how many players are waiting in the queue
		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs.status == STATUS_MAP_WAIT)
				waiting++;
		}

		p = new Packet(PACKET_SERVER_WAIT);
		NetworkSend_byte(p, waiting);
		NetworkSend_Packet(p, cs);
	}

	// This sends the map to the client
	void NetworkPacketSend_PACKET_SERVER_MAP_command(NetworkClientState cs)()
	{
		//
		// Packet: SERVER_MAP
		// Function: Sends the map to the client, or a part of it (it is splitted in
		//   a lot of multiple packets)
		// Data:
		//    byte:  packet-type (MAP_PACKET_START, MAP_PACKET_NORMAL and MAP_PACKET_END)
		//  if MAP_PACKET_START:
		//    int: The current FrameCounter
		//  if MAP_PACKET_NORMAL:
		//    piece of the map (till max-size of packet)
		//  if MAP_PACKET_END:
		//    int: seed0 of player
		//    int: seed1 of player
		//      last 2 are repeated Global.MAX_PLAYERS time
		//

		char filename[256];
		static FILE *file_pointer;
		static int sent_packets; // How many packets we did send succecfully last time

		if (cs.status < STATUS_AUTH) {
			// Illegal call, return error and ignore the packet
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_AUTHORIZED);
			return;
		}
		if (cs.status == STATUS_AUTH) {
			Packet p;

			// Make a dump of the current game
			sprintf(filename, "%s%snetwork_server.tmp",  _path.autosave_dir, PATHSEP);
			if (SaveOrLoad(filename, SL_SAVE) != SL_OK) error("network savedump failed");

			file_pointer = fopen(filename, "rb");
			fseek(file_pointer, 0, SEEK_END);

			// Now send the _frame_counter and how many packets are coming
			p = new Packet(PACKET_SERVER_MAP);
			NetworkSend_byte(p, MAP_PACKET_START);
			NetworkSend_int(p, _frame_counter);
			NetworkSend_int(p, ftell(file_pointer));
			NetworkSend_Packet(p, cs);

			fseek(file_pointer, 0, SEEK_SET);

			sent_packets = 4; // We start with trying 4 packets

			cs.status = STATUS_MAP;
			/* Mark the start of download */
			cs.last_frame = _frame_counter;
			cs.last_frame_server = _frame_counter;
		}

		if (cs.status == STATUS_MAP) {
			int i;
			int res;
			for (i = 0; i < sent_packets; i++) {
				Packet p = new Packet(PACKET_SERVER_MAP);
				NetworkSend_byte(p, MAP_PACKET_NORMAL);
				res = fread(p.buffer + p.size, 1, SEND_MTU - p.size, file_pointer);
				if (ferror(file_pointer)) {
					error("Error reading temporary network savegame!");
				}
				p.size += res;
				NetworkSend_Packet(p, cs);
				if (feof(file_pointer)) {
					// Done reading!
					Packet p;

					// XXX - Delete this when patch-settings are saved in-game
					NetworkSendPatchSettings(cs);
					p = new Packet(PACKET_SERVER_MAP);
					NetworkSend_byte(p, MAP_PACKET_END);
					NetworkSend_Packet(p, cs);

					// Set the status to DONE_MAP, no we will wait for the client
					//  to send it is ready (maybe that happens like never ;))
					cs.status = STATUS_DONE_MAP;
					fclose(file_pointer);

					{
						NetworkClientState new_cs;
						boolean new_map_client = false;
						// Check if there is a client waiting for receiving the map
						//  and start sending him the map
						FOR_ALL_CLIENTS(new_cs) {
							if (new_cs.status == STATUS_MAP_WAIT) {
								// Check if we already have a new client to send the map to
								if (!new_map_client) {
									// If not, this client will get the map
									new_cs.status = STATUS_AUTH;
									new_map_client = true;
									SEND_COMMAND(PACKET_SERVER_MAP)(new_cs);
								} else {
									// Else, send the other clients how many clients are in front of them
									SEND_COMMAND(PACKET_SERVER_WAIT)(new_cs);
								}
							}
						}
					}

					// There is no more data, so break the for
					break;
				}
			}

			// Send all packets (forced) and check if we have send it all
			NetworkSend_Packets(cs);
			if (cs.packet_queue == null) {
				// All are sent, increase the sent_packets
				sent_packets *= 2;
			} else {
				// Not everything is sent, decrease the sent_packets
				if (sent_packets > 1) sent_packets /= 2;
			}
		}
	}

	void NetworkPacketSend_PACKET_SERVER_JOIN_command(NetworkClientState cs)()(NetworkClientState cs, int client_index)
	{
		//
		// Packet: SERVER_JOIN
		// Function: A client is joined (all active clients receive this after a
		//     PACKET_CLIENT_MAP_OK) Mostly what directly follows is a
		//     PACKET_SERVER_CLIENT_INFO
		// Data:
		//    int:  Client-Index
		//

		Packet p = new Packet(PACKET_SERVER_JOIN);

		NetworkSend_int(p, client_index);

		NetworkSend_Packet(p, cs);
	}


	void NetworkPacketSend_PACKET_SERVER_FRAME_command(NetworkClientState cs)()
	{
		//
		// Packet: SERVER_FRAME
		// Function: Sends the current frame-counter to the client
		// Data:
		//    int: Frame Counter
		//    int: Frame Counter Max (how far may the client walk before the server?)
		//    [int: general-seed-1]
		//    [int: general-seed-2]
		//      (last two depends on compile-settings, and are not default settings)
		//

		Packet p = new Packet(PACKET_SERVER_FRAME);
		NetworkSend_int(p, _frame_counter);
		NetworkSend_int(p, _frame_counter_max);
	#ifdef ENABLE_NETWORK_SYNC_EVERY_FRAME
		NetworkSend_int(p, _sync_seed_1);
	#ifdef NETWORK_SEND_DOUBLE_SEED
		NetworkSend_int(p, _sync_seed_2);
	#endif
	#endif
		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_SYNC_command(NetworkClientState cs)()
	{
		//
		// Packet: SERVER_SYNC
		// Function: Sends a sync-check to the client
		// Data:
		//    int: Frame Counter
		//    int: General-seed-1
		//    [int: general-seed-2]
		//      (last one depends on compile-settings, and are not default settings)
		//

		Packet p = new Packet(PACKET_SERVER_SYNC);
		NetworkSend_int(p, _frame_counter);
		NetworkSend_int(p, _sync_seed_1);

	#ifdef NETWORK_SEND_DOUBLE_SEED
		NetworkSend_int(p, _sync_seed_2);
	#endif
		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_COMMAND_command(NetworkClientState cs)()(NetworkClientState cs, CommandPacket cp)
	{
		//
		// Packet: SERVER_COMMAND
		// Function: Sends a DoCommand to the client
		// Data:
		//    byte:  PlayerID (0..Global.MAX_PLAYERS-1)
		//    int: CommandID (see command.h)
		//    int: P1 (free variables used in DoCommand)
		//    int: P2
		//    int: Tile
		//    string: text
		//    byte:  CallBackID (see callback_table.c)
		//    int: Frame of execution
		//

		Packet p = new Packet(PACKET_SERVER_COMMAND);

		NetworkSend_byte(p, cp.player);
		NetworkSend_int(p, cp.cmd);
		NetworkSend_int(p, cp.p1);
		NetworkSend_int(p, cp.p2);
		NetworkSend_int(p, cp.tile);
		NetworkSend_string(p, cp.text);
		NetworkSend_byte(p, cp.callback);
		NetworkSend_int(p, cp.frame);

		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_CHAT_command(NetworkClientState cs)()(NetworkClientState cs, NetworkAction action, int client_index, boolean self_send, final char *msg)
	{
		//
		// Packet: SERVER_CHAT
		// Function: Sends a chat-packet to the client
		// Data:
		//    byte:  ActionID (see network_data.h, NetworkAction)
		//    int:  Client-index
		//    String: Message (max MAX_TEXT_MSG_LEN)
		//

		Packet p = new Packet(PACKET_SERVER_CHAT);

		NetworkSend_byte(p, action);
		NetworkSend_int(p, client_index);
		NetworkSend_byte(p, self_send);
		NetworkSend_string(p, msg);

		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_ERROR_QUIT_command(NetworkClientState cs)()(NetworkClientState cs, int client_index, NetworkErrorCode errorno)
	{
		//
		// Packet: SERVER_ERROR_QUIT
		// Function: One of the clients made an error and is quiting the game
		//      This packet informs the other clients of that.
		// Data:
		//    int:  Client-index
		//    byte:  ErrorID (see network_data.h, NetworkErrorCode)
		//

		Packet p = new Packet(PACKET_SERVER_ERROR_QUIT);

		NetworkSend_int(p, client_index);
		NetworkSend_byte(p, errorno);

		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_QUIT_command(NetworkClientState cs)()(NetworkClientState cs, int client_index, final char *leavemsg)
	{
		//
		// Packet: SERVER_ERROR_QUIT
		// Function: A client left the game, and this packets informs the other clients
		//      of that.
		// Data:
		//    int:  Client-index
		//    String: leave-message
		//

		Packet p = new Packet(PACKET_SERVER_QUIT);

		NetworkSend_int(p, client_index);
		NetworkSend_string(p, leavemsg);

		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_SHUTDOWN_command(NetworkClientState cs)()
	{
		//
		// Packet: SERVER_SHUTDOWN
		// Function: Let the clients know that the server is closing
		// Data:
		//     <none>
		//

		Packet p = new Packet(PACKET_SERVER_SHUTDOWN);
		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_NEWGAME_command(NetworkClientState cs)
	{
		//
		// Packet: PACKET_SERVER_NEWGAME
		// Function: Let the clients know that the server is loading a new map
		// Data:
		//     <none>
		//

		Packet p = new Packet(PACKET_SERVER_NEWGAME);
		NetworkSend_Packet(p, cs);
	}

	void NetworkPacketSend_PACKET_SERVER_RCON_command(NetworkClientState cs)()(NetworkClientState cs, int color, final char *command)
	{
		Packet p = new Packet(PACKET_SERVER_RCON);

		NetworkSend_int(p, color);
		NetworkSend_string(p, command);
		NetworkSend_Packet(p, cs);
	}

	// **********
	// Receiving functions
	//   void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p) has parameter: NetworkClientState cs, Packet p
	// **********

	void NetworkPacketReceive_PACKET_CLIENT_COMPANY_INFO_command(NetworkClientState *cs, Packet *p)()
	{
		SEND_COMMAND(PACKET_SERVER_COMPANY_INFO)(cs);
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_JOIN)
	{
		char name[NETWORK_NAME_LENGTH];
		char unique_id[NETWORK_NAME_LENGTH];
		NetworkClientInfo ci;
		char test_name[NETWORK_NAME_LENGTH];
		byte playas;
		NetworkLanguage client_lang;
		char client_revision[NETWORK_REVISION_LENGTH];


		NetworkRecv_string(cs, p, client_revision, sizeof(client_revision));

	#if defined(WITH_REV) || defined(WITH_REV_HACK)
		// Check if the client has revision control enabled
		if (strncmp(NOREV_STRING, client_revision, sizeof(client_revision)) != 0) {
			if (strncmp(_network_game_info.server_revision, client_revision, sizeof(_network_game_info.server_revision) - 1) != 0) {
				// Different revisions!!
				SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_WRONG_REVISION);

				return;
			}
		}
	#endif

		NetworkRecv_string(cs, p, name, sizeof(name));
		playas = NetworkRecv_byte(cs, p);
		client_lang = NetworkRecv_byte(cs, p);
		NetworkRecv_string(cs, p, unique_id, sizeof(unique_id));

		if (cs.quited)
			return;

		// Check if someone else already has that name
		snprintf(test_name, sizeof(test_name), "%s", name);

		if (test_name[0] == '\0') {
			// We need a valid name.. make it Player
			snprintf(test_name, sizeof(test_name), "Player");
		}

		if (!NetworkFindName(test_name)) {
			// We could not create a name for this player
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NAME_IN_USE);
			return;
		}

		ci = DEREF_CLIENT_INFO(cs);

		snprintf(ci.client_name, sizeof(ci.client_name), "%s", test_name);
		snprintf(ci.unique_id, sizeof(ci.unique_id), "%s", unique_id);
		ci.client_playas = playas;
		ci.client_lang = client_lang;

		// We now want a password from the client
		//  else we do not allow him in!
		if (_network_game_info.use_password)
			SEND_COMMAND(PACKET_SERVER_NEED_PASSWORD)(cs, NETWORK_GAME_PASSWORD);
		else {
			if (ci.client_playas > 0 && ci.client_playas <= Global.MAX_PLAYERS && _network_player_info[ci.client_playas - 1].password[0] != '\0') {
				SEND_COMMAND(PACKET_SERVER_NEED_PASSWORD)(cs, NETWORK_COMPANY_PASSWORD);
			}
			else {
				SEND_COMMAND(PACKET_SERVER_WELCOME)(cs);
			}
		}

		/* Make sure companies to who people try to join are not autocleaned */
		if (playas >= 1 && playas <= Global.MAX_PLAYERS)
			_network_player_info[playas-1].months_empty = 0;
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_PASSWORD)
	{
		NetworkPasswordType type;
		char password[NETWORK_PASSWORD_LENGTH];
		NetworkClientInfo ci;

		type = NetworkRecv_byte(cs, p);
		NetworkRecv_string(cs, p, password, sizeof(password));

		if (cs.status == STATUS_INACTIVE && type == NETWORK_GAME_PASSWORD) {
			// Check game-password
			if (strncmp(password, _network_game_info.server_password, sizeof(password)) != 0) {
				// Password is invalid
				SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_WRONG_PASSWORD);
				return;
			}

			ci = DEREF_CLIENT_INFO(cs);

			if (ci.client_playas <= Global.MAX_PLAYERS && _network_player_info[ci.client_playas - 1].password[0] != '\0') {
				SEND_COMMAND(PACKET_SERVER_NEED_PASSWORD)(cs, NETWORK_COMPANY_PASSWORD);
				return;
			}

			// Valid password, allow user
			SEND_COMMAND(PACKET_SERVER_WELCOME)(cs);
			return;
		} else if (cs.status == STATUS_INACTIVE && type == NETWORK_COMPANY_PASSWORD) {
			ci = DEREF_CLIENT_INFO(cs);

			if (strncmp(password, _network_player_info[ci.client_playas - 1].password, sizeof(password)) != 0) {
				// Password is invalid
				SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_WRONG_PASSWORD);
				return;
			}

			SEND_COMMAND(PACKET_SERVER_WELCOME)(cs);
			return;
		}


		SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_EXPECTED);
		return;
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_GETMAP)
	{
		NetworkClientState new_cs;

		// The client was never joined.. so this is impossible, right?
		//  Ignore the packet, give the client a warning, and close his connection
		if (cs.status < STATUS_AUTH || cs.quited) {
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_AUTHORIZED);
			return;
		}

		// Check if someone else is receiving the map
		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs.status == STATUS_MAP) {
				// Tell the new client to wait
				cs.status = STATUS_MAP_WAIT;
				SEND_COMMAND(PACKET_SERVER_WAIT)(cs);
				return;
			}
		}

		// We receive a request to upload the map.. give it to the client!
		SEND_COMMAND(PACKET_SERVER_MAP)(cs);
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_MAP_OK)
	{
		// Client has the map, now start syncing
		if (cs.status == STATUS_DONE_MAP && !cs.quited) {
			char client_name[NETWORK_CLIENT_NAME_LENGTH];
			NetworkClientState new_cs;

			NetworkGetClientName(client_name, sizeof(client_name), cs);

			NetworkTextMessage(NETWORK_ACTION_JOIN, 1, false, client_name, "");

			// Mark the client as pre-active, and wait for an ACK
			//  so we know he is done loading and in sync with us
			cs.status = STATUS_PRE_ACTIVE;
			NetworkHandleCommandQueue(cs);
			SEND_COMMAND(PACKET_SERVER_FRAME)(cs);
			SEND_COMMAND(PACKET_SERVER_SYNC)(cs);

			// This is the frame the client receives
			//  we need it later on to make sure the client is not too slow
			cs.last_frame = _frame_counter;
			cs.last_frame_server = _frame_counter;

			FOR_ALL_CLIENTS(new_cs) {
				if (new_cs.status > STATUS_AUTH) {
					SEND_COMMAND(PACKET_SERVER_CLIENT_INFO)(new_cs, DEREF_CLIENT_INFO(cs));
					SEND_COMMAND(PACKET_SERVER_JOIN)(new_cs, cs.index);
				}
			}

			if (_network_pause_on_join) {
				/* Now pause the game till the client is in sync */
				DoCommandP(0, 1, 0, null, Cmd.CMD_PAUSE);

				NetworkServer_HandleChat(NETWORK_ACTION_CHAT, DESTTYPE_BROADCAST, 0, "Game paused (incoming client)", NETWORK_SERVER_INDEX);
			}
		} else {
			// Wrong status for this packet, give a warning to client, and close connection
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_EXPECTED);
		}
	}

	/** Enforce the command flags.
	 * Eg a server-only command can only be executed by a server, etc.
	 * @param *cp the commandpacket that is going to be checked
	 * @param *ci client information for debugging output to console
	 */
	static boolean CheckCommandFlags(final CommandPacket cp, final NetworkClientInfo ci)
	{
		byte flags = GetCommandFlags(cp.cmd);

		if (flags & Cmd.CMD_SERVER && ci.client_index != NETWORK_SERVER_INDEX) {
			IConsolePrintF(_icolour_err, "WARNING: server only command from player %d (IP: %s), kicking...", ci.client_playas, GetPlayerIP(ci));
			return false;
		}

		if (flags & Cmd.CMD_OFFLINE) {
			IConsolePrintF(_icolour_err, "WARNING: offline only command from player %d (IP: %s), kicking...", ci.client_playas, GetPlayerIP(ci));
			return false;
		}
		return true;
	}

	/** The client has done a command and wants us to handle it
	 * @param *cs the connected client that has sent the command
	 * @param *p the packet in which the command was sent
	 */
	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_COMMAND)
	{
		NetworkClientState new_cs;
		final NetworkClientInfo ci;
		byte callback;

		CommandPacket cp = malloc(sizeof(CommandPacket));

		// The client was never joined.. so this is impossible, right?
		//  Ignore the packet, give the client a warning, and close his connection
		if (cs.status < STATUS_DONE_MAP || cs.quited) {
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_EXPECTED);
			return;
		}

		cp.player = NetworkRecv_byte(cs, p);
		cp.cmd    = NetworkRecv_int(cs, p);
		cp.p1     = NetworkRecv_int(cs, p);
		cp.p2     = NetworkRecv_int(cs, p);
		cp.tile   = NetworkRecv_int(cs, p);
		NetworkRecv_string(cs, p, cp.text, lengthof(cp.text));

		callback = NetworkRecv_byte(cs, p);

		if (cs.quited) return;

		ci = DEREF_CLIENT_INFO(cs);

		/* Check if cp.cmd is valid */
		if (!IsValidCommand(cp.cmd)) {
			IConsolePrintF(_icolour_err, "WARNING: invalid command from player %d (IP: %s).", ci.client_playas, GetPlayerIP(ci));
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_NOT_EXPECTED);
			return;
		}

		if (!CheckCommandFlags(cp, ci)) {
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_KICKED);
			return;
		}

		/** Only Cmd.CMD_PLAYER_CTRL is always allowed, for the rest, playas needs
		 * to match the player in the packet. If it doesn't, the client has done
		 * something pretty naughty (or a bug), and will be kicked
		 */
		if (!(cp.cmd == Cmd.CMD_PLAYER_CTRL && cp.p1 == 0) && ci.client_playas - 1 != cp.player) {
			IConsolePrintF(_icolour_err, "WARNING: player %d (IP: %s) tried to execute a command as player %d, kicking...",
										 ci.client_playas - 1, GetPlayerIP(ci), cp.player);
			SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_PLAYER_MISMATCH);
			return;
		}

		/** @todo Cmd.CMD_PLAYER_CTRL with p1 = 0 announces a new player to the server. To give the
		 * player the correct ID, the server injects p2 and executes the command. Any other p1
		 * is prohibited. Pretty ugly and should be redone together with its function.
		 * @see CmdPlayerCtrl() players.c:655
		 */
		if (cp.cmd == Cmd.CMD_PLAYER_CTRL) {
			if (cp.p1 != 0) {
				SEND_COMMAND(PACKET_SERVER_ERROR)(cs, NETWORK_ERROR_CHEATER);
				return;
			}

			// XXX - UGLY! p2 is mis-used to get the client-id in CmdPlayerCtrl
			cp.p2 = cs - _clients;
		}

		// The frame can be executed in the same frame as the next frame-packet
		//  That frame just before that frame is saved in _frame_counter_max
		cp.frame = _frame_counter_max + 1;
		cp.next  = null;

		// Queue the command for the clients (are send at the end of the frame
		//   if they can handle it ;))
		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs.status > STATUS_AUTH) {
				// Callbacks are only send back to the client who sent them in the
				//  first place. This filters that out.
				cp.callback = (new_cs != cs) ? 0 : callback;
				NetworkAddCommandQueue(new_cs, cp);
			}
		}

		cp.callback = 0;
		// Queue the command on the server
		if (_local_command_queue == null) {
			_local_command_queue = cp;
		} else {
			// Find last packet
			CommandPacket c = _local_command_queue;
			while (c.next != null) c = c.next;
			c.next = cp;
		}
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_ERROR)
	{
		// This packets means a client noticed an error and is reporting this
		//  to us. Display the error and report it to the other clients
		NetworkClientState new_cs;
		byte errorno = NetworkRecv_byte(cs, p);
		char str[100];
		char client_name[NETWORK_CLIENT_NAME_LENGTH];

		// The client was never joined.. thank the client for the packet, but ignore it
		if (cs.status < STATUS_DONE_MAP || cs.quited) {
			cs.quited = true;
			return;
		}

		NetworkGetClientName(client_name, sizeof(client_name), cs);

		Global.GetString(str, Str.STR_NETWORK_ERR_CLIENT_GENERAL + errorno);

		Global.DEBUG_net( 2)("[NET] %s reported an error and is closing his connection (%s)", client_name, str);

		NetworkTextMessage(NETWORK_ACTION_LEAVE, 1, false, client_name, "%s", str);

		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs.status > STATUS_AUTH) {
				SEND_COMMAND(PACKET_SERVER_ERROR_QUIT)(new_cs, cs.index, errorno);
			}
		}

		cs.quited = true;
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_QUIT)
	{
		// The client wants to leave. Display this and report it to the other
		//  clients.
		NetworkClientState new_cs;
		char str[100];
		char client_name[NETWORK_CLIENT_NAME_LENGTH];

		// The client was never joined.. thank the client for the packet, but ignore it
		if (cs.status < STATUS_DONE_MAP || cs.quited) {
			cs.quited = true;
			return;
		}

		NetworkRecv_string(cs, p, str, lengthof(str));

		NetworkGetClientName(client_name, sizeof(client_name), cs);

		NetworkTextMessage(NETWORK_ACTION_LEAVE, 1, false, client_name, "%s", str);

		FOR_ALL_CLIENTS(new_cs) {
			if (new_cs.status > STATUS_AUTH) {
				SEND_COMMAND(PACKET_SERVER_QUIT)(new_cs, cs.index, str);
			}
		}

		cs.quited = true;
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_ACK)
	{
		int frame = NetworkRecv_int(cs, p);

		/* The client is trying to catch up with the server */
		if (cs.status == STATUS_PRE_ACTIVE) {
			/* The client is not yet catched up? */
			if (frame + DAY_TICKS < _frame_counter)
				return;

			/* Now he is! Unpause the game */
			cs.status = STATUS_ACTIVE;

			if (_network_pause_on_join) {
				DoCommandP(0, 0, 0, null, Cmd.CMD_PAUSE);
				NetworkServer_HandleChat(NETWORK_ACTION_CHAT, DESTTYPE_BROADCAST, 0, "Game unpaused", NETWORK_SERVER_INDEX);
			}
		}

		// The client received the frame, make note of it
		cs.last_frame = frame;
		// With those 2 values we can calculate the lag realtime
		cs.last_frame_server = _frame_counter;
	}



	void NetworkServer_HandleChat(NetworkAction action, DestType desttype, int dest, final char *msg, int from_index)
	{
		NetworkClientState cs;
		NetworkClientInfo ci, *ci_own, *ci_to;

		switch (desttype) {
		case DESTTYPE_CLIENT:
			/* Are we sending to the server? */
			if (dest == NETWORK_SERVER_INDEX) {
				ci = NetworkFindClientInfoFromIndex(from_index);
				/* Display the text locally, and that is it */
				if (ci != null)
					NetworkTextMessage(action, GetDrawStringPlayerColor(ci.client_playas-1), false, ci.client_name, "%s", msg);
			} else {
				/* Else find the client to send the message to */
				FOR_ALL_CLIENTS(cs) {
					if (cs.index == dest) {
						SEND_COMMAND(PACKET_SERVER_CHAT)(cs, action, from_index, false, msg);
						break;
					}
				}
			}

			// Display the message locally (so you know you have sent it)
			if (from_index != dest) {
				if (from_index == NETWORK_SERVER_INDEX) {
					ci = NetworkFindClientInfoFromIndex(from_index);
					ci_to = NetworkFindClientInfoFromIndex(dest);
					if (ci != null && ci_to != null)
						NetworkTextMessage(action, GetDrawStringPlayerColor(ci.client_playas-1), true, ci_to.client_name, "%s", msg);
				} else {
					FOR_ALL_CLIENTS(cs) {
						if (cs.index == from_index) {
							SEND_COMMAND(PACKET_SERVER_CHAT)(cs, action, dest, true, msg);
							break;
						}
					}
				}
			}
			break;
		case DESTTYPE_PLAYER: {
			boolean show_local = true; // If this is false, the message is already displayed
															// on the client who did sent it.
			/* Find all clients that belong to this player */
			ci_to = null;
			FOR_ALL_CLIENTS(cs) {
				ci = DEREF_CLIENT_INFO(cs);
				if (ci.client_playas == dest) {
					SEND_COMMAND(PACKET_SERVER_CHAT)(cs, action, from_index, false, msg);
					if (cs.index == from_index) {
						show_local = false;
					}
					ci_to = ci; // Remember a client that is in the company for company-name
				}
			}

			ci = NetworkFindClientInfoFromIndex(from_index);
			ci_own = NetworkFindClientInfoFromIndex(NETWORK_SERVER_INDEX);
			if (ci != null && ci_own != null && ci_own.client_playas == dest) {
				NetworkTextMessage(action, GetDrawStringPlayerColor(ci.client_playas-1), false, ci.client_name, "%s", msg);
				if (from_index == NETWORK_SERVER_INDEX)
					show_local = false;
				ci_to = ci_own;
			}

			/* There is no such player */
			if (ci_to == null) break;

			// Display the message locally (so you know you have sent it)
			if (ci != null && show_local) {
				if (from_index == NETWORK_SERVER_INDEX) {
					char name[NETWORK_NAME_LENGTH];
					Global.GetString(name, GetPlayer(ci_to.client_playas-1).name_1);
					NetworkTextMessage(action, GetDrawStringPlayerColor(ci_own.client_playas-1), true, name, "%s", msg);
				} else {
					FOR_ALL_CLIENTS(cs) {
						if (cs.index == from_index) {
							SEND_COMMAND(PACKET_SERVER_CHAT)(cs, action, ci_to.client_index, true, msg);
						}
					}
				}
			}
			}
			break;
		default:
			Global.DEBUG_net( 0)("[NET][Server] Received unknown destination type %d. Doing broadcast instead.");
			/* fall-through to next case */
		case DESTTYPE_BROADCAST:
			FOR_ALL_CLIENTS(cs) {
				SEND_COMMAND(PACKET_SERVER_CHAT)(cs, action, from_index, false, msg);
			}
			ci = NetworkFindClientInfoFromIndex(from_index);
			if (ci != null)
				NetworkTextMessage(action, GetDrawStringPlayerColor(ci.client_playas-1), false, ci.client_name, "%s", msg);
			break;
		}
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_CHAT)
	{
		NetworkAction action = NetworkRecv_byte(cs, p);
		DestType desttype = NetworkRecv_byte(cs, p);
		int dest = NetworkRecv_byte(cs, p);
		char msg[MAX_TEXT_MSG_LEN];

		NetworkRecv_string(cs, p, msg, MAX_TEXT_MSG_LEN);

		NetworkServer_HandleChat(action, desttype, dest, msg, cs.index);
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_SET_PASSWORD)
	{
		char password[NETWORK_PASSWORD_LENGTH];
		NetworkClientInfo ci;

		NetworkRecv_string(cs, p, password, sizeof(password));
		ci = DEREF_CLIENT_INFO(cs);

		if (ci.client_playas <= Global.MAX_PLAYERS) {
			ttd_strlcpy(_network_player_info[ci.client_playas - 1].password, password, sizeof(_network_player_info[ci.client_playas - 1].password));
		}
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_SET_NAME)
	{
		char client_name[NETWORK_CLIENT_NAME_LENGTH];
		NetworkClientInfo ci;

		NetworkRecv_string(cs, p, client_name, sizeof(client_name));
		ci = DEREF_CLIENT_INFO(cs);

		if (cs.quited)
			return;

		if (ci != null) {
			// Display change
			if (NetworkFindName(client_name)) {
				NetworkTextMessage(NETWORK_ACTION_NAME_CHANGE, 1, false, ci.client_name, "%s", client_name);
				ttd_strlcpy(ci.client_name, client_name, sizeof(ci.client_name));
				NetworkUpdateClientInfo(ci.client_index);
			}
		}
	}

	void NetworkPacketReceive_ ## type ## _command(NetworkClientState *cs, Packet *p)(PACKET_CLIENT_RCON)
	{
		char pass[NETWORK_PASSWORD_LENGTH];
		char command[NETWORK_RCONCOMMAND_LENGTH];

		if (_network_game_info.rcon_password[0] == '\0')
			return;

		NetworkRecv_string(cs, p, pass, sizeof(pass));
		NetworkRecv_string(cs, p, command, sizeof(command));

		if (strncmp(pass, _network_game_info.rcon_password, sizeof(pass)) != 0) {
			Global.DEBUG_net( 0)("[RCon] Wrong password from client-id %d", cs.index);
			return;
		}

		Global.DEBUG_net( 0)("[RCon] Client-id %d executed: %s", cs.index, command);

		_redirect_console_to_client = cs.index;
		IConsoleCmdExec(command);
		_redirect_console_to_client = 0;
		return;
	}

	// The layout for the receive-functions by the server
	typedef void NetworkServerPacket(NetworkClientState cs, Packet p);


	// This array matches PacketType. At an incoming
	//  packet it is matches against this array
	//  and that way the right function to handle that
	//  packet is found.
	static NetworkServerPacket* final _network_server_packet[] = {
		null, /*PACKET_SERVER_FULL,*/
		null, /*PACKET_SERVER_BANNED,*/
		RECEIVE_COMMAND(PACKET_CLIENT_JOIN),
		null, /*PACKET_SERVER_ERROR,*/
		RECEIVE_COMMAND(PACKET_CLIENT_COMPANY_INFO),
		null, /*PACKET_SERVER_COMPANY_INFO,*/
		null, /*PACKET_SERVER_CLIENT_INFO,*/
		null, /*PACKET_SERVER_NEED_PASSWORD,*/
		RECEIVE_COMMAND(PACKET_CLIENT_PASSWORD),
		null, /*PACKET_SERVER_WELCOME,*/
		RECEIVE_COMMAND(PACKET_CLIENT_GETMAP),
		null, /*PACKET_SERVER_WAIT,*/
		null, /*PACKET_SERVER_MAP,*/
		RECEIVE_COMMAND(PACKET_CLIENT_MAP_OK),
		null, /*PACKET_SERVER_JOIN,*/
		null, /*PACKET_SERVER_FRAME,*/
		null, /*PACKET_SERVER_SYNC,*/
		RECEIVE_COMMAND(PACKET_CLIENT_ACK),
		RECEIVE_COMMAND(PACKET_CLIENT_COMMAND),
		null, /*PACKET_SERVER_COMMAND,*/
		RECEIVE_COMMAND(PACKET_CLIENT_CHAT),
		null, /*PACKET_SERVER_CHAT,*/
		RECEIVE_COMMAND(PACKET_CLIENT_SET_PASSWORD),
		RECEIVE_COMMAND(PACKET_CLIENT_SET_NAME),
		RECEIVE_COMMAND(PACKET_CLIENT_QUIT),
		RECEIVE_COMMAND(PACKET_CLIENT_ERROR),
		null, /*PACKET_SERVER_QUIT,*/
		null, /*PACKET_SERVER_ERROR_QUIT,*/
		null, /*PACKET_SERVER_SHUTDOWN,*/
		null, /*PACKET_SERVER_NEWGAME,*/
		null, /*PACKET_SERVER_RCON,*/
		RECEIVE_COMMAND(PACKET_CLIENT_RCON),
	};

	// If this fails, check the array above with network_data.h
	assert_compile(lengthof(_network_server_packet) == PACKET_END);


	extern final SettingDesc patch_settings[];

	// This is a TEMPORARY solution to get the patch-settings
	//  to the client. When the patch-settings are saved in the savegame
	//  this should be removed!!
	void NetworkSendPatchSettings(NetworkClientState cs)
	{
		final SettingDesc *item;
		Packet p = new Packet(PACKET_SERVER_MAP);
		NetworkSend_byte(p, MAP_PACKET_PATCH);
		// Now send all the patch-settings in a pretty order..

		item = patch_settings;

		while (item.name != null) {
			switch (item.flags) {
				case SDT_BOOL:
				case SDT_INT8:
				case SDT_UINT8:
					NetworkSend_byte(p, *(byte *)item.ptr);
					break;
				case SDT_INT16:
				case SDT_UINT16:
					NetworkSend_int(p, *(int *)item.ptr);
					break;
				case SDT_INT32:
				case SDT_UINT32:
					NetworkSend_int(p, *(int *)item.ptr);
					break;
			}
			item++;
		}

		NetworkSend_Packet(p, cs);
	}

	// This update the company_info-stuff
	void NetworkPopulateCompanyInfo()
	{
		char password[NETWORK_PASSWORD_LENGTH];
		Player p;
		Vehicle v;
		Station s;
		NetworkClientState cs;
		NetworkClientInfo ci;
		int i;
		int months_empty;

		FOR_ALL_PLAYERS(p) {
			if (!p.is_active) {
				memset(&_network_player_info[p.index], 0, sizeof(NetworkPlayerInfo));
				continue;
			}

			// Clean the info but not the password
			ttd_strlcpy(password, _network_player_info[p.index].password, sizeof(password));
			months_empty = _network_player_info[p.index].months_empty;
			memset(&_network_player_info[p.index], 0, sizeof(NetworkPlayerInfo));
			_network_player_info[p.index].months_empty = months_empty;
			ttd_strlcpy(_network_player_info[p.index].password, password, sizeof(_network_player_info[p.index].password));

			// Grap the company name
			Global.SetDParam(0, p.name_1);
			Global.SetDParam(1, p.name_2);
			Global.GetString(_network_player_info[p.index].company_name, Str.STR_JUST_STRING);

			// Check the income
			if (_cur_year - 1 == p.inaugurated_year)
				// The player is here just 1 year, so display [2], else display[1]
				for (i = 0; i < 13; i++)
					_network_player_info[p.index].income -= p.yearly_expenses[2][i];
			else
				for (i = 0; i < 13; i++)
					_network_player_info[p.index].income -= p.yearly_expenses[1][i];

			// Set some general stuff
			_network_player_info[p.index].inaugurated_year = p.inaugurated_year;
			_network_player_info[p.index].company_value = p.old_economy[0].company_value;
			_network_player_info[p.index].money = p.money64;
			_network_player_info[p.index].performance = p.old_economy[0].performance_history;
		}

		// Go through all vehicles and count the type of vehicles
		FOR_ALL_VEHICLES(v) {
			if (v.owner < Global.MAX_PLAYERS)
				switch (v.type) {
					case Vehicle.VEH_Train:
						if (IsFrontEngine(v))
							_network_player_info[v.owner].num_vehicle[0]++;
						break;
					case Vehicle.VEH_Road:
						if (v.cargo_type != AcceptedCargo.CT_PASSENGERS)
							_network_player_info[v.owner].num_vehicle[1]++;
						else
							_network_player_info[v.owner].num_vehicle[2]++;
						break;
					case Vehicle.VEH_Aircraft:
						if (v.subtype <= 2)
							_network_player_info[v.owner].num_vehicle[3]++;
						break;
					case Vehicle.VEH_Ship:
						_network_player_info[v.owner].num_vehicle[4]++;
						break;
					case Vehicle.VEH_Special:
					case Vehicle.VEH_Disaster:
						break;
				}
		}

		// Go through all stations and count the types of stations
		FOR_ALL_STATIONS(s) {
			if (s.owner < Global.MAX_PLAYERS) {
				if ((s.facilities & FACIL_TRAIN))
					_network_player_info[s.owner].num_station[0]++;
				if ((s.facilities & FACIL_TRUCK_STOP))
					_network_player_info[s.owner].num_station[1]++;
				if ((s.facilities & FACIL_BUS_STOP))
					_network_player_info[s.owner].num_station[2]++;
				if ((s.facilities & FACIL_AIRPORT))
					_network_player_info[s.owner].num_station[3]++;
				if ((s.facilities & FACIL_DOCK))
					_network_player_info[s.owner].num_station[4]++;
			}
		}

		ci = NetworkFindClientInfoFromIndex(NETWORK_SERVER_INDEX);
		// Register local player (if not dedicated)
		if (ci != null && ci.client_playas > 0  && ci.client_playas <= Global.MAX_PLAYERS)
			ttd_strlcpy(_network_player_info[ci.client_playas-1].players, ci.client_name, sizeof(_network_player_info[ci.client_playas-1].players));

		FOR_ALL_CLIENTS(cs) {
			char client_name[NETWORK_CLIENT_NAME_LENGTH];

			NetworkGetClientName(client_name, sizeof(client_name), cs);

			ci = DEREF_CLIENT_INFO(cs);
			if (ci != null && ci.client_playas > 0 && ci.client_playas <= Global.MAX_PLAYERS) {
				if (strlen(_network_player_info[ci.client_playas-1].players) != 0)
					ttd_strlcat(_network_player_info[ci.client_playas - 1].players, ", ", lengthof(_network_player_info[ci.client_playas - 1].players));

				ttd_strlcat(_network_player_info[ci.client_playas - 1].players, client_name, lengthof(_network_player_info[ci.client_playas - 1].players));
			}
		}
	}

	// Send a packet to all clients with updated info about this client_index
	void NetworkUpdateClientInfo(int client_index)
	{
		NetworkClientState cs;
		NetworkClientInfo ci;

		ci = NetworkFindClientInfoFromIndex(client_index);

		if (ci == null)
			return;

		FOR_ALL_CLIENTS(cs) {
			SEND_COMMAND(PACKET_SERVER_CLIENT_INFO)(cs, ci);
		}
	}

	extern void SwitchMode(int new_mode);

	/* Check if we want to restart the map */
	static void NetworkCheckRestartMap()
	{
		if (_network_restart_game_date != 0 && _cur_year + MAX_YEAR_BEGIN_REAL >= _network_restart_game_date) {
			Global.DEBUG_net( 0)("Auto-restarting map. Year %d reached.", _cur_year + MAX_YEAR_BEGIN_REAL);

			_random_seeds[0][0] = Hal.Random();
			_random_seeds[0][1] = InteractiveHal.Random();

			SwitchMode(SM_NEWGAME);
		}
	}

	/* Check if the server has autoclean_companies activated
	    Two things happen:
	      1) If a company is not protected, it is closed after 1 year (for example)
	      2) If a company is protected, protection is disabled after 3 years (for example)
	           (and item 1. happens a year later) */
	static void NetworkAutoCleanCompanies()
	{
		NetworkClientState cs;
		NetworkClientInfo ci;
		Player p;
		boolean clients_in_company[Global.MAX_PLAYERS];

		if (!_network_autoclean_companies)
			return;

		memset(clients_in_company, 0, sizeof(clients_in_company));

		/* Detect the active companies */
		FOR_ALL_CLIENTS(cs) {
			ci = DEREF_CLIENT_INFO(cs);
			if (ci.client_playas >= 1 && ci.client_playas <= Global.MAX_PLAYERS) {
				clients_in_company[ci.client_playas-1] = true;
			}
		}
		if (!_network_dedicated) {
			ci = NetworkFindClientInfoFromIndex(NETWORK_SERVER_INDEX);
			if (ci.client_playas >= 1 && ci.client_playas <= Global.MAX_PLAYERS) {
				clients_in_company[ci.client_playas-1] = true;
			}
		}

		/* Go through all the comapnies */
		FOR_ALL_PLAYERS(p) {
			/* Skip the non-active once */
			if (!p.is_active || p.is_ai)
				continue;

			if (!clients_in_company[p.index]) {
				/* The company is empty for one month more */
				_network_player_info[p.index].months_empty++;

				/* Is the company empty for autoclean_unprotected-months, and is there no protection? */
				if (_network_player_info[p.index].months_empty > _network_autoclean_unprotected && _network_player_info[p.index].password[0] == '\0') {
					/* Shut the company down */
					DoCommandP(0, 2, p.index, null, Cmd.CMD_PLAYER_CTRL);
					IConsolePrintF(_icolour_def, "Auto-cleaned company #%d", p.index+1);
				}
				/* Is the compnay empty for autoclean_protected-months, and there is a protection? */
				if (_network_player_info[p.index].months_empty > _network_autoclean_protected && _network_player_info[p.index].password[0] != '\0') {
					/* Unprotect the company */
					_network_player_info[p.index].password[0] = '\0';
					IConsolePrintF(_icolour_def, "Auto-removed protection from company #%d", p.index+1);
					_network_player_info[p.index].months_empty = 0;
				}
			} else {
				/* It is not empty, reset the date */
				_network_player_info[p.index].months_empty = 0;
			}
		}
	}

	// This function changes new_name to a name that is unique (by adding #1 ...)
	//  and it returns true if that succeeded.
	boolean NetworkFindName(char new_name[NETWORK_CLIENT_NAME_LENGTH])
	{
		NetworkClientState new_cs;
		NetworkClientInfo ci;
		boolean found_name = false;
		byte number = 0;
		char original_name[NETWORK_CLIENT_NAME_LENGTH];

		// We use NETWORK_NAME_LENGTH in here, because new_name is really a pointer
		ttd_strlcpy(original_name, new_name, NETWORK_CLIENT_NAME_LENGTH);

		while (!found_name) {
			found_name = true;
			FOR_ALL_CLIENTS(new_cs) {
				ci = DEREF_CLIENT_INFO(new_cs);
				if (strncmp(ci.client_name, new_name, NETWORK_CLIENT_NAME_LENGTH) == 0) {
					// Name already in use
					found_name = false;
					break;
				}
			}
			// Check if it is the same as the server-name
			ci = NetworkFindClientInfoFromIndex(NETWORK_SERVER_INDEX);
			if (ci != null) {
				if (strncmp(ci.client_name, new_name, NETWORK_CLIENT_NAME_LENGTH) == 0) {
					// Name already in use
					found_name = false;
				}
			}

			if (!found_name) {
				// Try a new name (<name> #1, <name> #2, and so on)

				// Stop if we tried for more than 50 times..
				if (number++ > 50) break;
				snprintf(new_name, NETWORK_CLIENT_NAME_LENGTH, "%s #%d", original_name, number);
			}
		}

		return found_name;
	}

	// Reads a packet from the stream
	boolean NetworkServer_ReadPackets(NetworkClientState cs)
	{
		Packet p;
		NetworkRecvStatus res;
		while((p = NetworkRecv_Packet(cs, &res)) != null) {
			byte type = NetworkRecv_byte(cs, p);
			if (type < PACKET_END && _network_server_packet[type] != null && !cs.quited)
				_network_server_packet[type](cs, p);
			else
				Global.DEBUG_net( 0)("[NET][Server] Received invalid packet type %d", type);
			free(p);
		}

		return true;
	}

	// Handle the local command-queue
	void NetworkHandleCommandQueue(NetworkClientState cs) {
		CommandPacket cp;

		while ( (cp = cs.command_queue) != null) {
			SEND_COMMAND(PACKET_SERVER_COMMAND)(cs, cp);

			cs.command_queue = cp.next;
			free(cp);
		}
	}

	// This is called every tick if this is a _network_server
	void NetworkServer_Tick(boolean send_frame)
	{
		NetworkClientState cs;
	//#ifndef ENABLE_NETWORK_SYNC_EVERY_FRAME
		boolean send_sync = false;
	//#endif

	//#ifndef ENABLE_NETWORK_SYNC_EVERY_FRAME
		if (_frame_counter >= _last_sync_frame + _network_sync_freq) {
			_last_sync_frame = _frame_counter;
			send_sync = true;
		}
	//#endif

		// Now we are done with the frame, inform the clients that they can
		//  do their frame!
		FOR_ALL_CLIENTS(cs) {
			// Check if the speed of the client is what we can expect from a client
			if (cs.status == STATUS_ACTIVE) {
				// 1 lag-point per day
				int lag = NetworkCalculateLag(cs) / DAY_TICKS;
				if (lag > 0) {
					if (lag > 3) {
						// Client did still not report in after 4 game-day, drop him
						//  (that is, the 3 of above, + 1 before any lag is counted)
						IConsolePrintF(_icolour_err,"Client #%d is dropped because the client did not respond for more than 4 game-days", cs.index);
						NetworkCloseClient(cs);
						continue;
					}

					// Report once per time we detect the lag
					if (cs.lag_test == 0) {
						IConsolePrintF(_icolour_warn,"[%d] Client #%d is slow, try increasing *net_frame_freq to a higher value!", _frame_counter, cs.index);
						cs.lag_test = 1;
					}
				} else {
					cs.lag_test = 0;
				}
			} else if (cs.status == STATUS_PRE_ACTIVE) {
				int lag = NetworkCalculateLag(cs);
				if (lag > _network_max_join_time) {
					IConsolePrintF(_icolour_err,"Client #%d is dropped because it took longer than %d ticks for him to join", cs.index, _network_max_join_time);
					NetworkCloseClient(cs);
				}
			}

			if (cs.status >= STATUS_PRE_ACTIVE) {
				// Check if we can send command, and if we have anything in the queue
				NetworkHandleCommandQueue(cs);

				// Send an updated _frame_counter_max to the client
				if (send_frame)
					SEND_COMMAND(PACKET_SERVER_FRAME)(cs);

	#ifndef ENABLE_NETWORK_SYNC_EVERY_FRAME
				// Send a sync-check packet
				if (send_sync)
					SEND_COMMAND(PACKET_SERVER_SYNC)(cs);
	#endif
			}
		}

		/* See if we need to advertise */
		NetworkUDPAdvertise();
	}

	void NetworkServerYearlyLoop()
	{
		NetworkCheckRestartMap();
	}

	void NetworkServerMonthlyLoop()
	{
		NetworkAutoCleanCompanies();
	}

	
	
}
