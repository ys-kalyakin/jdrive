package game.ai;

public enum AiState {
	STARTUP,
	FIRST_TIME,
	NOTHING,
	WAKE_UP,
	LOCATE_ROUTE,
	FIND_STATION,
	FIND_PATH,
	FIND_DEPOT,
	VERIFY_ROUTE,
	BUILD_STATION,
	BUILD_PATH,
	BUILD_DEPOT,
	BUILD_VEHICLE,
	GIVE_ORDERS,
	START_VEHICLE,
	REPAY_MONEY,
	CHECK_ALL_VEHICLES,
	ACTION_DONE,
	STOP, // Temporary function to stop the AI
}