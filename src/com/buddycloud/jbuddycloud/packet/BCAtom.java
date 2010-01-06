package com.buddycloud.jbuddycloud.packet;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class BCAtom implements PacketExtension, PacketExtensionProvider {

    /*
     * <entry xmlns='http://www.w3.org/2005/Atom'>
     *     <author>
     *         <name>user@buddycloud.com</name>
     *         <jid>user@buddycloud.com</jid>
     *         <affiliation>publisher</affiliation>
     *     </author>
     *     <content type='text'>Uh, no.</content>
     *     <published>2009-12-20T07:53:07Z</published>
     *     <geoloc xmlns='http://jabber.org/protocol/geoloc'>
     *         <text>Bremen, Germany</text>
     *         <locality>Bremen</locality>
     *         <country>Germany</country>
     *     </geoloc>
     *     <headers xmlns='http://jabber.org/protocol/shim'>
     *         <header name='In-Reply-To'>1261295132420</header>
     *     </headers>
     * </entry>
     */

    private String authorName;
    private String authorJid;
    private String affiliation;
    private String content;
    private String contentType;
    private long published;
    private long parentId;
    private long id;
    private GeoLoc geoloc;

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

    public void setPublished(long published) {
        this.published = published;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GeoLoc getGeoloc() {
        return geoloc;
    }

    public void setGeoloc(GeoLoc geoloc) {
        this.geoloc = geoloc;
    }

    @Override
    public String getElementName() {
        return "entry";
    }

    @Override
    public String getNamespace() {
        return "http://www.w3.org/2005/Atom";
    }

    @Override
    public String toXML() {
        return null;
    }

    @Override
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
                            Integer.parseInt(timePart[2])
                    );
                    atom.published = cal.getTime().getTime();
                } else
                if (tagName.equals("headers")) {
                    do {
                        if ((tag = parser.next()) == XmlPullParser.START_TAG) {
                           tagName = parser.getName();
                           if (tagName.equals("header")) {
                               if (parser.getAttributeValue("", "name")
                                       .equals("In-Reply-To")) {
                                   atom.parentId =
                                       Long.parseLong(parser.nextText());
                               }
                           }
                        }
                    } while (!"headers".equals(parser.getName()));
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
