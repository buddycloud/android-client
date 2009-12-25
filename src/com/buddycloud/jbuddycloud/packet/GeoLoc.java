package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class GeoLoc extends IQ implements PacketExtensionProvider, PacketExtension {

    public static enum Type {
        CURRENT, NEXT, PREV
    };

    /* <geoloc xmlns='http://jabber.org/protocol/geoloc' xml:lang='en'>
     *   <text>Friedenheim</text>
     *   <area>Friedenheim</area>
     *   <locality>München</locality>
     *   <region>Bayern</region>
     *   <country>Germany</country>
     *   <lat>48.145978</lat>
     *   <lon>11.50995</lon>
     *   <accuracy>591.0</accuracy>
     * </geoloc>
     */

    private String text;
    private String area;
    private String locality;
    private String region;
    private String country;
    private String postalcode;
    private String uri;
    private Type locType;

    public Type getLocType() {
        return locType;
    }

    public void setLocType(Type type) {
        this.locType = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private double lat;
    private double lon;
    private double accuracy;

    @Override
    public String getChildElementXML() {
        return null;
    }

    public void fromXML() {
        
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        GeoLoc loc = new GeoLoc();

        for(;;) {
            switch (parser.next()) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                if (name.equals("text")) {
                    loc.text = parser.nextText();
                    break;
                }
                if (name.equals("area")) {
                    loc.area = parser.nextText();
                    break;
                }
                if (name.equals("locality")) {
                    loc.locality = parser.nextText();
                    break;
                }
                if (name.equals("region")) {
                    loc.region = parser.nextText();
                    break;
                }
                if (name.equals("country")) {
                    loc.country = parser.nextText();
                    break;
                }
                if (name.equals("postalcode")) {
                    loc.postalcode = parser.nextText();
                    break;
                }
                if (name.equals("uri")) {
                    loc.uri = parser.nextText();
                    break;
                }
                if (name.equals("lat")) {
                    loc.lat = Double.parseDouble(parser.nextText().trim());
                    break;
                }
                if (name.equals("lon")) {
                    loc.lon = Double.parseDouble(parser.nextText().trim());
                    break;
                }
                if (name.equals("accuracy")) {
                    loc.accuracy = Double.parseDouble(parser.nextText().trim());
                    break;
                }
                // OK, we didn't get it :-(
                Log.e("SMACK", "Unknown geoloc-type " + name);
                int stack = 1;
                do {
                    switch (parser.next()) {
                    case XmlPullParser.END_TAG: stack--; break;
                    case XmlPullParser.START_TAG: stack++; break;
                    }
                } while (stack > 0);
                break;
            case XmlPullParser.TEXT:
                break;
            case XmlPullParser.END_TAG:
                if (parser.getName().equals("geoloc")) {
                    Log.d("GEO", "parsed geoloc");
                    return loc;
                }
            }
       }
    }

    @Override
    public String getElementName() {
        return "geoloc";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/geoloc";
    }

}
