/* This file is derived from the smack project, thus licensed under the
 * Apache 2 license.
 */

package com.googlecode.asmack.parser;

import java.io.IOException;
import java.io.StringReader;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.util.Log;

import com.googlecode.asmack.Stanza;

/**
 * <p>A smack delegation parser with bootstrap capabilities from compiled
 * android xml files.</p>
 * <p>This class uses the same singelton pattern as the smack provider manager.
 * This is needed for one-time only extension registration</p>
 */
public class SmackParser {

    /**
     * The debugging key for this class, SmackParser.
     */
    private static final String TAG = SmackParser.class.getSimpleName();

    /**
     * Internal dummy connection used during IQ parsing.
     */
    private static final DummyConnection DUMMY_CONNECTION =
                                                    new DummyConnection();

    /**
     * The xml parser factory used for stanza parsing.
     */
    private final XmlPullParserFactory xmlPullParserFactory;

    /**
     * The internal instance for the singleton pattern.
     */
    private static SmackParser INSTANCE = null;

    /**
     * Create a new instance and initialize the pull parser factory.
     */
    private SmackParser() {
        XmlPullParserFactory xmlPullParserFactoryInstance = null;
        try {
            xmlPullParserFactoryInstance = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Couldn't create sax factory!", e);
        }
        xmlPullParserFactory = xmlPullParserFactoryInstance;
        xmlPullParserFactory.setNamespaceAware(true);
        xmlPullParserFactory.setValidating(false);
    }

    /**
     * Register all providers specified by an open XML file.
     * @param parser The parser for the xml file.
     */
    public void registerProviders(XmlPullParser parser) {
        try {
            parser.next();
            String className = null;
            String elementName = null;
            String namespace = null;
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        if ("iqProvider".equals(parser.getName())||
                            "extensionProvider".equals(parser.getName())) {
                            className = null;
                            elementName = null;
                            namespace = null;
                            break;
                        }
                        if ("className".equals(parser.getName())) {
                            if (!parser.isEmptyElementTag()) {
                                className = parser.nextText();
                            }
                            break;
                        }
                        if ("namespace".equals(parser.getName())) {
                            if (!parser.isEmptyElementTag()) {
                                namespace = parser.nextText();
                            }
                            break;
                        }
                        if ("elementName".equals(parser.getName())) {
                            if (!parser.isEmptyElementTag()) {
                                elementName = parser.nextText();
                            }
                            break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("iqProvider".equals(parser.getName())) {
                            addIQProviderIfAbsent(
                                elementName,
                                namespace,
                                className
                            );
                            break;
                        }
                        if ("extensionProvider".equals(parser.getName())) {
                            addExtensionProviderIfAbsent(
                                elementName,
                                namespace,
                                className
                            );
                            break;
                        }
                        if ("smackProviders".equals(parser.getName())) {
                            return;
                        }
                        break;
                }
                parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Register all providers defined in a given resource xml. This method can
     * read android compiled xml.
     * @param c The current context for resource lookups.
     * @param id The XML id.
     */
    public void registerProviders(Context c, int id) {
        registerProviders(c.getResources().getXml(id));
    }

    /**
     * Retrieve the singleton instance of the smack parser.
     * @return The smack parser instance.
     */
    public static synchronized SmackParser getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SmackParser();
        }
        return INSTANCE;
    }

    /**
     * Retrieve the smack parsing instance. This method reads the parser on
     * create.
     * @param parser The xml parser used for configuration.
     * @return The smack parser instance.
     */
    public static synchronized SmackParser getInstance(XmlPullParser parser) {
        if (INSTANCE == null) {
            INSTANCE = new SmackParser();
            INSTANCE.registerProviders(parser);
        }
        return INSTANCE;
    }

    /**
     * Retrieve the smack parsing instance, parsing the give resource on
     * create. Note that this method won't touch the resource if the smack
     * parser is already initialized.
     * @param c The current context.
     * @param id The xml resource id.
     * @return The smack parser instance.
     */
    public static synchronized SmackParser getInstance(Context c, int id) {
        if (INSTANCE == null) {
            return getInstance(c.getResources().getXml(id));
        }
        return INSTANCE;
    }

    /**
     * Helper to add a single IQ provider or provide usefull log output.
     * @param elementName The element name during parsing.
     * @param namespace The namespace of the element.
     * @param className The handling class.
     */
    private void addIQProviderIfAbsent(
        String elementName,
        String namespace,
        String className
    ) {
        if (className == null || namespace == null || elementName == null) {
            Log.d(TAG, "Incomplete definition IQrovider["
                    + "elemnentName=" + elementName
                    + ",namespace=" + namespace
                    + "className=" + className
                    + "]");
            return;
        }
        ProviderManager providerManager = ProviderManager.getInstance();
        if (providerManager.getIQProvider(elementName, namespace) != null) {
            Log.d(TAG, "There is already an IQProvider["
                    + "elemnentName=" + elementName
                    + ",namespace=" + namespace
                    + "]"
            );
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            if (!IQProvider.class.isAssignableFrom(clazz)) {
                Log.d(TAG, "Not an IQProvider " + className);
                providerManager.addIQProvider(elementName, namespace, clazz);
            } else {
                providerManager.addIQProvider(
                    elementName,
                    namespace,
                    clazz.newInstance()
                );
            }

            Log.d(TAG, "Registered IQProvider["
                    + elementName + "," + namespace + "," + className + "]");

        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Could not find IQProvider " + className);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Could not instatiate IQProvider " + className);
        } catch (InstantiationException e) {
            Log.d(TAG, "Could not instatiate IQProvider " + className);
        }
    }

    /**
     * Helper to add a single extension provider or provide usefull log output.
     * @param elementName The element name during parsing.
     * @param namespace The namespace of the element.
     * @param className The handling class.
     */
    private void addExtensionProviderIfAbsent(
        String elementName,
        String namespace,
        String className
    ) {
        if (className == null || namespace == null || elementName == null) {
            Log.d(TAG, "Incomplete definition ExtensionProvider["
                        + "elemnentName=" + elementName
                        + ",namespace=" + namespace
                        + "className=" + className
                        + "]");
            return;
        }
        ProviderManager providerManager = ProviderManager.getInstance();
        if (providerManager.getExtensionProvider(elementName, namespace)
                != null) {
            Log.d(TAG, "There is already an ExtensionProvider["
                    + "elemnentName=" + elementName
                    + ",namespace=" + namespace
                    + "]"
            );
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            if (!PacketExtensionProvider.class.isAssignableFrom(clazz)) {
                Log.d(TAG, "Not a PacketExtensionProvider " + className);
                providerManager.addExtensionProvider(
                    elementName,
                    namespace,
                    clazz
                );
            } else {
                providerManager.addExtensionProvider(
                    elementName,
                    namespace,
                    clazz.newInstance()
                );
            }

            Log.d(TAG, "Registered ExtensionProvider["
                + elementName + "," + namespace + "," + className + "]");

        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Could not find ExtensionProvider " + className);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Could not instatiate ExtensionProvider " + className);
        } catch (InstantiationException e) {
            Log.d(TAG, "Could not instatiate ExtensionProvider " + className);
        }
    }

    /**
     * Parse a stanza into a smack packet. Return null on failure.
     * @param stanza The stanza to parse.
     * @return A parsed stanza, or null.
     * @throws Exception In case of errors.
     */
    public Packet parse(Stanza stanza) throws Exception {
        if ("presence".equals(stanza.getName())) {
            return PacketParserUtils.parsePresence(getParser(stanza));
        }
        if ("iq".equals(stanza.getName())) {
            return PacketParserUtils.parseIQ(
                    getParser(stanza),
                    DUMMY_CONNECTION
            );
        }
        if ("message".equals(stanza.getName())) {
            return PacketParserUtils.parseMessage(getParser(stanza));
        }
        return null;
    }

    /**
     * Create a new xml pull parser for a given stanza.
     * @param stanza The stanza to use as input.
     * @return A new pull parser, seeked to the first START_TAG.
     * @throws XmlPullParserException In case of a XML error.
     * @throws IOException In case of a read error.
     */
    private XmlPullParser getParser(Stanza stanza)
        throws XmlPullParserException, IOException {
        XmlPullParser parser = xmlPullParserFactory.newPullParser();
        parser.setInput(new StringReader(stanza.getXml()));
        parser.nextTag();
        return parser;
    }

}
