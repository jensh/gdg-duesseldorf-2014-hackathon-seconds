package com.jens.voicemsg;

public class ConfigurationTemplate {
	/**
	 * Location of the chat server
	 */
	static final String CHAT_SERVER = "http://example.com:7893";

	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */
	static String GCM_SENDER_ID = "############";

	/**
	 * URL used to POST a new message
	 */
	static final String URL_CHAT_UPLOAD = CHAT_SERVER + "/send_message";

	/**
	 * URL used to join/leave a room
	 */
	static final String URL_CHAT_GCM_SWITCH_ROOM = CHAT_SERVER + "/gcm_switch_room";

	/**
	 * Tag used for logging
	 */
	static final String LOG_TAG = "VoiceMsg";
}
