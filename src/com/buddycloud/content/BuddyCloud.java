package com.buddycloud.content;

import android.net.Uri;
import android.provider.BaseColumns;

public class BuddyCloud {

    public static interface CacheColumns extends BaseColumns{
        public static final String CACHE_UPDATE_TIMESTAMP =
            "cache_update_timestamp";
    }

    public static class Places implements CacheColumns{

        public static final Uri CONTENT_URI =
            Uri.parse("content://com.buddycloud/places");

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
            _ID,
            NAME,
            DESCRIPTION,
            LAT,
            LON,
            STREET,
            AREA,
            REGION,
            COUNTRY,
            POSTALCODE,
            POPULATION,
            REVISION,
            SHARED
        };

    }

    public static class Roster implements CacheColumns{

        public static final Uri CONTENT_URI =
            Uri.parse("content://com.buddycloud/roster");

        public static final Uri VIEW_CONTENT_URI=
            Uri.parse("content://com.buddycloud/roster_view");

        public static final String JID="jid";
        public static final String NAME="name";
        public static final String ENTRYTYPE = "type";
        public static final String STATUS="status";
        public static final String GEOLOC_PREV="geoloc_prev";
        public static final String GEOLOC="geoloc";
        public static final String LAST_UPDATED = "last_updated";
        public static final String UNREAD_MESSAGES = "unread_messages";
        public static final String UNREAD_REPLIES = "unread_replies";

        public static final String  GEOLOC_NEXT="geoloc_next";
        public static final String[] PROJECTION_MAP = {
            _ID,
            JID,
            NAME,
            ENTRYTYPE,
            STATUS,
            GEOLOC,
            GEOLOC_NEXT,
            GEOLOC_PREV,
            UNREAD_MESSAGES,
            UNREAD_REPLIES,
            LAST_UPDATED
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

        public static final String UNREAD = "unread";
    }

    public static class ChannelData extends Item {

        public static final Uri CONTENT_URI =
            Uri.parse("content://com.buddycloud/channeldata");

        public static final String  NODE_NAME = "node_name";

        public static final String[] PROJECTION_MAP = {
            NODE_NAME,
            _ID,
            CACHE_UPDATE_TIMESTAMP,

            ITEM_ID,
            PARENT,
            LAST_UPDATED,

            AUTHOR,
            AUTHOR_JID,
            AUTHOR_AFFILIATION,

            CONTENT,
            CONTENT_TYPE,

            GEOLOC_LAT,
            GEOLOC_LON,
            GEOLOC_ACCURACY,

            GEOLOC_AREA,
            GEOLOC_COUNTRY,
            GEOLOC_LOCALITY,
            GEOLOC_REGION,

            GEOLOC_TEXT,
            GEOLOC_TYPE,
            UNREAD,
            PUBLISHED
        };
    }

}
