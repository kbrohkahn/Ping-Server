package com.brohkahn.pingserver;

class Constants {
	static final int PING_SUCCESS = 1;
	static final int PING_FAIL = 0;
	static final int PING_ERROR_HOST = 10;
	static final int PING_ERROR_IO = 11;

	static final int NOTIFICATION_ID = 84746401;

	static final int BROADCAST_PING_CODE = 17594944;

	private static final String PACKAGE_ACTION = "com.brohkahn.pingserver.action.";
	static final String ACTION_RESCHEDULE_PINGS = PACKAGE_ACTION + "reschedule_pings";
	static final String ACTION_PING = PACKAGE_ACTION + "ping";
	static final String ACTION_PINGS_UPDATED = PACKAGE_ACTION + "pings_updated";

	static final String KEY_PING_INTENT_SOURCE = "intentSource";

}
