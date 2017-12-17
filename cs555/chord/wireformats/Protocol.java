package cs555.chord.wireformats;

public class Protocol {
	// Declaring status code
		public static final byte SUCCESS = 0;
		public static final byte FAILURE = -1;
		
		// Declaring Message Types
		public static final int REGISTER_REQUEST = 0;
		public static final int REGISTER_RESPONSE = 1;
		public static final int LOOKUP_REQUEST = 2;
		public static final int LOOKUP_RESPONSE = 3;
		public static final int ASK_PREDECESSOR_REQUEST = 4;
		public static final int ASK_PREDECESSOR_RESPONSE = 5;
		public static final int UPDATE_FINGER_TABLE_REQUEST = 6;
		public static final int LOOKUP_FORWARD_REQUEST = 7;
		public static final int RANDOMNODE_SUCCESSOR_RESPONSE = 8;
		public static final int CHECK_FINGER_UPDATE = 9;
		public static final int CHECK_FINGER_UPDATE_RESPONSE = 10;
		public static final int UPDATE_LOOKUP_AFTER_ENTRY = 11;
		public static final int UPDATE_LOOKUP_AFTER_ENTRY_RESPONSE = 12;
		public static final int STORE_DATA_RANDOM_NODE_REQUEST = 13;
		public static final int STORE_DATA_RANDOM_NODE_RESPONSE = 14;
		public static final int FILE_STORE_REQUEST = 15;
		public static final int FILE_TRANSFER_REQUEST = 16;
		public static final int FILE_TRANSFER_RESPONSE = 17;
}
