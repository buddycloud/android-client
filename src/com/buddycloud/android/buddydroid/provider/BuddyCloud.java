package com.buddycloud.android.buddydroid.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class BuddyCloud {

	public static interface CacheColumns extends BaseColumns{
		public static final String CACHE_UPDATE_TIMESTAMP="cache_update_timestamp";
	}
	
	public static class Places implements CacheColumns{
		
		public static final Uri CONTENT_URI=Uri.parse("");
		
		public static final String MIME_TYPE="";
		
		public static final String PLACE_ID="place_id";
		public static final String NAME="place_name";
		public static final String DESCRIPTION="description";
		public static final String LAT="lat";
		public static final String LON="lon";
		public static final String STREET="street";
		public static final String AREA="area";
		public static final String REGION="region";
		public static final String COUNTRY="country";
		public static final String POSTALCODE="postalcode";
		public static final String POPULATION="population";
		public static final String REVISION="revision";
		public static final String SHARED="shared";
		
		public static final String[] PROJECTION_MAP={
			Places._ID,
			Places.NAME,
			Places.DESCRIPTION,
			Places.LAT,
			Places.LON,
			Places.STREET,
			Places.AREA,
			Places.REGION,
			Places.COUNTRY,
			Places.POSTALCODE,
			Places.POPULATION,
			Places.REVISION,
			Places.SHARED
		};
		
	}
	
	public static class Roster implements CacheColumns{
	
		public static final Uri CONTENT_URI=Uri.parse("content://com.buddycloud/roster");
		public static final String  JID="jid";
		
		public static final String  NAME="name";
		
        public static final String ENTRYTYPE = "type";

		public static final String  STATUS="status";
		
		public static final String  GEOLOC_PREV="geoloc_prev";
		
		public static final String  GEOLOC="geoloc";
		
        public static final String LAST_UPDATED = "last_updated";

		public static final String  GEOLOC_NEXT="geoloc_next";
		public static final String[] PROJECTION_MAP = {
			Roster._ID,
			Roster.JID,
			Roster.NAME,
			Roster.STATUS,
			Roster.GEOLOC,
			Roster.GEOLOC_NEXT,
			Roster.GEOLOC_PREV
			
		};
	}
	
	private static class Item implements CacheColumns{

		public static final String  PARENT = "parent_id";
		public static final String  ITEM_ID = "item_id";

		public static final String  AUTHOR = "author";
		public static final String  AUTHOR_JID = "author_jid";
		public static final String  AUTHOR_AFFILIATION = "author_affiliation";
		
		public static final String  PUBLISHED="published";
		public static final String  LAST_UPDATED="last_updated";

		public static final String  CONTENT_TYPE="content_type";
		public static final String  CONTENT="content";
		
		public static final String  GEOLOC_LAT="geoloc_lat";
		public static final String  GEOLOC_LON="geoloc_lon";
		public static final String  GEOLOC_ACCURACY="geoloc_accuracy";

		public static final String  GEOLOC_AREA="geoloc_area";
		public static final String  GEOLOC_LOCALITY="geoloc_locality";
		public static final String  GEOLOC_TEXT="geoloc_text";
		public static final String  GEOLOC_REGION="geoloc_region";
		public static final String  GEOLOC_COUNTRY="geoloc_country";

		public static final String  GEOLOC_TYPE="geoloc_type";
	}

	public static class ChannelData extends Item{
		
		public static final Uri CONTENT_URI=Uri.parse("content://com.buddycloud/channeldata");
		
		public static final String  NODE_NAME = "node_name";
		
		public static final String[] PROJECTION_MAP={
			ChannelData.NODE_NAME,
			ChannelData._ID,
			ChannelData.CACHE_UPDATE_TIMESTAMP,

			ChannelData.ITEM_ID,
			ChannelData.PARENT,
			ChannelData.LAST_UPDATED,

			ChannelData.AUTHOR,
			ChannelData.AUTHOR_JID,
			ChannelData.AUTHOR_AFFILIATION,

			ChannelData.CONTENT,
			ChannelData.CONTENT_TYPE,

			ChannelData.GEOLOC_LAT,
			ChannelData.GEOLOC_LON,
			ChannelData.GEOLOC_ACCURACY,

			ChannelData.GEOLOC_AREA,
			ChannelData.GEOLOC_COUNTRY,
			ChannelData.GEOLOC_LOCALITY,
			ChannelData.GEOLOC_REGION,

			ChannelData.GEOLOC_TEXT,
			ChannelData.GEOLOC_TYPE,

			ChannelData.PUBLISHED
		};
	}
	
	
	
}
