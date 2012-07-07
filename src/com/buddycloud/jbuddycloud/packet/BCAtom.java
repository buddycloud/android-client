package com.buddycloud.jbuddycloud.packet;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class BCAtom implements PacketExtension, PacketExtensionProvider {

    /*
     * <event xmlns='http://jabber.org/protocol/pubsub#event'>
     *     <items node='/user/jid@jabber.ccc.de/channel'>
     *         <item id='1266517381817'>
     *             <entry xmlns='http://www.w3.org/2005/Atom' xmlns:thr='http://purl.org/syndication/thread/1.0'>
     *                 <author>
     *                     <jid xmlns='http://buddycloud.com/atom-elements-0'>XXX@buddycloud.com</jid>
     *                     <affiliation xmlns='http://buddycloud.com/atom-elements-0'>publisher</affiliation>
     *                 </author>
     *                 <content type='text'>This is a comment</content>
     *                 <published>2010-02-18T18:23:01Z</published>
     *                 <updated>2010-02-18T18:23:01Z</updated>
     *                 <id>/user/jid@jabber.ccc.de/channel:1266517381817</id>
     *                 <geoloc xmlns='http://jabber.org/protocol/geoloc'>
     *                     <text>München, Germany</text>
     *                     <locality>München</locality>
     *                     <country>Germany</country>
     *                 </geoloc>
     *                 <thr:in-reply-to ref='1266517219960'/>
     *             </entry>
     *         </item>
     *     </items>
     * </event>
     */

    private String authorName;
    private String authorJid;
    private String affiliation;
    private String content;
    private String contentType;
    private Long published;
    private String id;
    private GeoLoc geoloc;
    private String parent;

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorJid() {
        return authorJid;
    }

    public void setAuthorJid(String authorJid) {
        this.authorJid = authorJid;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getPublished() {
        return published;
    }

    public void setPublished(Long published) {
        this.published = published;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoLoc getGeoloc() {
        return geoloc;
    }

    public void setGeoloc(GeoLoc geoloc) {
        this.geoloc = geoloc;
    }

    public String getElementName() {
        return "entry";
    }

    public String getNamespace() {
        return "http://www.w3.org/2005/Atom";
    }

    public String toXML() {
        if (published == null) {
            published = System.currentTimeMillis();
        }
        StringBuilder builder = new StringBuilder();
        Calendar calendar = Calendar.getInstance();

        builder.append("<entry xmlns='http://www.w3.org/2005/Atom' xmlns:thr='http://purl.org/syndication/thread/1.0'>");
        builder.append("<published>");
        builder.append(calendar.get(Calendar.YEAR));
        builder.append("-");
        int field = calendar.get(Calendar.MONTH) + 1;
        if (field < 10) { builder.append("0"); }
        builder.append(field);
        builder.append("-");
        field = calendar.get(Calendar.DAY_OF_MONTH);
        if (field < 10) { builder.append("0"); }
        builder.append(field);
        builder.append("T");
        field = calendar.get(Calendar.HOUR_OF_DAY);
        if (field < 10) { builder.append("0"); }
        builder.append(field);
        builder.append(":");
        field = calendar.get(Calendar.HOUR);
        if (field < 10) { builder.append("0"); }
        builder.append(field);
        builder.append(":");
        field = calendar.get(Calendar.MILLISECOND);
        if (field <  10) { builder.append("00"); } else
        if (field < 100) { builder.append( "0"); }
        builder.append(field);
        builder.append("Z");
        builder.append("</published>");

        builder.append("<author>");
        if (authorName != null) {
            builder.append("<name>");
            builder.append(StringUtils.escapeForXML(authorName));
            builder.append("</name>");
        }
        if (authorJid != null) {
            if (authorName == null) {
                builder.append("<name>");
                builder.append(StringUtils.escapeForXML(authorJid));
                builder.append("</name>");
            };
            builder.append("<jid xmlns=\"http://buddycloud.com/atom-elements-0\">");
            builder.append(StringUtils.escapeForXML(authorJid));
            builder.append("</jid>");
        }
        builder.append("</author>");

        if (content != null) {
            builder.append("<content type=\"text\">");
            builder.append(StringUtils.escapeForXML(content));
            builder.append("</content>");
        }

        if (geoloc != null) {
            builder.append(geoloc.toXML());
        }

        if (parent != null) {
            builder.append("<thr:in-reply-to ref='");
            builder.append(parent);
            builder.append("' />");
        }

        builder.append("</entry>");

        return builder.toString();
    }

    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        Log.d("ATOM", "try to parse an atom stanza");

        BCAtom atom = new BCAtom();

        String name = parser.getName();
        Map<String, String> attMap = new HashMap<String, String>();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            attMap.put(parser.getAttributeName(i), parser.getAttributeValue(i));
        }

        do {
            int tag = parser.next();

            if (tag == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("author")) {
                    do {
                        if ((tag = parser.next()) == XmlPullParser.START_TAG) {
                           tagName = parser.getName();
                           if (tagName.equals("name")) {
                               atom.authorName = parser.nextText();
                           } else
                           if (tagName.equals("jid")) {
                               atom.authorJid = parser.nextText();
                           } else
                           if (tagName.equals("affiliation")) {
                               atom.affiliation = parser.nextText();
                           }
                        }
                    } while (!"author".equals(parser.getName()));
                } else
                if (tagName.equals("published")) {
                    String date = parser.nextText();
                    date = date.substring(0, date.length() - 1);
                    String[] split = date.split("T");
                    String[] datePart = split[0].split("-");
                    String[] timePart = split[1].split(":");
                    Calendar cal = new GregorianCalendar(
                        TimeZone.getTimeZone("UTC")
                    );
                    cal.set(Integer.parseInt(datePart[0]),
                            Integer.parseInt(datePart[1]),
                            Integer.parseInt(datePart[2]),
                            Integer.parseInt(timePart[0]),
                            Integer.parseInt(timePart[1]),
                            (int) Double.parseDouble(timePart[2])
                    );
                    atom.published = cal.getTime().getTime();
                } else
                if (tagName.equals("in-reply-to")) {
                    if (!parser.isEmptyElementTag()) {
                        atom.parent = parser.nextText();
                    } else {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("ref")) {
                                atom.parent = parser.getAttributeValue(i);
                            }
                        }
                    }
                } else
                if (tagName.equals("content")) {
                    atom.contentType = parser.getAttributeValue("", "type");
                    atom.content = parser.nextText();
                } else
                if (tagName.equals("geoloc")) {
                    atom.geoloc = (GeoLoc)
                        PacketParserUtils.parsePacketExtension(
                                parser.getName(),
                                parser.getNamespace(),
                                parser
                            );
                } else {
                    PacketParserUtils.parsePacketExtension(
                        parser.getName(),
                        parser.getNamespace(),
                        parser
                    );
                }
            }
        } while (!name.equals(parser.getName()));

        Log.d("ATOM", "parsed atom stanza");

        return atom;
    }

}
