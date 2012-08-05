package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

public class RSMSet implements PacketExtension, PacketExtensionProvider {

    public static final byte START = 1;
    public static final byte END = 2;
    public static final byte FIRST = 3;
    public static final byte LAST = 4;
    public static final byte COUNT = 5;

    public String after = null;
    public String before = null;
    public String first = null;
    public String last = null;
    public String start = null;
    public String end = null;
    public long count = -1;
    public long max = -1;

    public RSMSet() {
    }

    @Override
    public String getElementName() {
        return "set";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/rsm";
    }

    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder(80);
        sb.append("<set xmlns='http://jabber.org/protocol/rsm'>");
        if (after != null) {
            sb.append("<after>").append(StringUtils.escapeForXML(after)).append("</after>");
        }
        if (before != null) {
            sb.append("<before>").append(StringUtils.escapeForXML(before)).append("</before>");
        }
        if (max > 0) {
            sb.append("<max>").append(max).append("</max>");
        }
        sb.append("</set>");
        return sb.toString();
    }

    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception {
        RSMSet rsm = new RSMSet();
        int balance = 1;
        byte mode = 0;
        while (balance > 0) {
            parser.next();
            switch (parser.getEventType()) {
            case XmlPullParser.END_TAG:
                balance--;
                mode = 0;
                break;
            case XmlPullParser.START_TAG:
                balance++;
                if ("start".equals(parser.getName())) {
                    mode = START;
                    break;
                }
                if ("end".equals(parser.getName())) {
                    mode = END;
                    break;
                }
                if ("first".equals(parser.getName())) {
                    mode = FIRST;
                    break;
                }
                if ("last".equals(parser.getName())) {
                    mode = LAST;
                    break;
                }
                if ("count".equals(parser.getName())) {
                    mode = COUNT;
                    break;
                }
                mode = 0;
                break;
            case XmlPullParser.TEXT:
                switch (mode) {
                case START:
                    rsm.start = parser.getText().trim();
                    break;
                case END:
                    rsm.end = parser.getText().trim();
                    break;
                case FIRST:
                    rsm.first = parser.getText().trim();
                    break;
                case LAST:
                    rsm.last = parser.getText().trim();
                    break;
                case COUNT:
                    rsm.count = Long.parseLong(parser.getText().trim());
                    break;
                }
            }
        }
        return rsm;
    }

    @Override
    public String toString() {
        return "RSMSet [after=" + after + ", before=" + before + ", first="
                + first + ", last=" + last + ", start=" + start + ", end="
                + end + ", count=" + count + ", max=" + max + "]";
    }

}
