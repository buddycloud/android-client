/*
 * Created on 2009-03-25
 */
package org.jivesoftware.smackx.pubsub;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.PubSub;
import org.jivesoftware.smackx.packet.PubSubNamespace;
import org.jivesoftware.smackx.packet.SyncPacketSend;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;

/**
 * This is the starting point for access to the pubsub service.  It
 * will provide access to general information about the service, as
 * well as create or retrieve pubsub {@link Node} instances.  These 
 * instances provide the bulk of the functionality as defined in the 
 * pubsub specification <a href="http://xmpp.org/extensions/xep-0060.html">XEP-0060</a>.
 * 
 * @author Robin Collier
 */
final public class PubSubManager
{
	private XMPPConnection con;
	private String to;
	private ConcurrentHashMap<String, Node> nodeMap = new ConcurrentHashMap<String, Node>();
	
	/**
	 * Create a pubsub manager associated to the specified connection.
	 * 
	 * @param connection The XMPP connection
	 */
	public PubSubManager(XMPPConnection connection)
	{
		con = connection;
	}
	
	/**
	 * Create a pubsub manager associated to the specified connection where
	 * the pubsub requests require a specific to address for packets.
	 * 
	 * @param connection The XMPP connection
	 * @param toAddress The pubsub specific to address (required for some servers)
	 */
	public PubSubManager(XMPPConnection connection, String toAddress)
	{
		con = connection;
		to = toAddress;
	}
	
	/**
	 * Creates an instant node, if supported.
	 * 
	 * @return The node that was created
	 * @exception XMPPException
	 */
	public Node createNode()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.CREATE));
		NodeExtension elem = (NodeExtension)reply.getExtension("create", PubSubNamespace.BASIC.getXmlns());
		
		Node newNode = new Node(con, elem.getNode());
		newNode.setTo(to);
		nodeMap.put(newNode.getId(), newNode);
		
		return newNode;
	}
	
	/**
	 * Creates a node with default configuration.
	 * 
	 * @param id The id of the node, which must be unique within the 
	 * pubsub service
	 * @return The node that was created
	 * @exception XMPPException
	 */
	public Node createNode(String id)
		throws XMPPException
	{
		return createNode(id, null);
	}
	
	/**
	 * Creates a node with specified configuration.
	 * 
	 * @param name The name of the node, which must be unique within the 
	 * pubsub service
	 * @param config The configuration for the node
	 * @return The node that was created
	 * @exception XMPPException
	 */
	public Node createNode(String name, Form config)
		throws XMPPException
	{
		PubSub request = createPubsubPacket(to, Type.SET, new NodeExtension(PubSubElementType.CREATE, name));
		
		if (config != null)
			request.addExtension(new FormNode(FormNodeType.CONFIGURE, config));

		// Errors will cause exceptions in getReply, so it only returns
		// on success.
		sendPubsubPacket(con, to, Type.SET, request);
		Node newNode = new Node(con, name); 
		newNode.setTo(to);
		nodeMap.put(newNode.getId(), newNode);
		
		return newNode;
	}

	/**
	 * Retrieves the requested node, if it exists.  It will throw an 
	 * exception if it does not.
	 * 
	 * @param id - The unique id of the node
	 * @return the node
	 * @throws XMPPException The node does not exist
	 */
	public Node getNode(String id)
		throws XMPPException
	{
		Node node = nodeMap.get(id);
		
		if (node == null)
		{
			DiscoverInfo info = new DiscoverInfo();
			info.setTo(to);
			info.setNode(id);
			
			SyncPacketSend.getReply(con, info);
			node = new Node(con, id);
			node.setTo(to);
			nodeMap.put(id, node);
		}
		return node;
	}
	
	/**
	 * Get all the nodes that currently exist.
	 * 
	 * @return Collection of nodes
	 * 
	 * @throws XMPPException
	 */
	public List<Node> getNodes()
		throws XMPPException
	{
		DiscoverItems items = new DiscoverItems();
		items.setTo(to);
		DiscoverItems nodeItems = (DiscoverItems)SyncPacketSend.getReply(con, items);

		Iterator<Item> it = nodeItems.getItems();
		List<Node> results = new LinkedList<Node>();
		
		while (it.hasNext())
		{
			Item item = it.next();
			
			String nodeId = item.getNode();
			Node node = nodeMap.get(nodeId);
			
			if (node == null)
			{
				node = new Node(con, nodeId);
				nodeMap.put(nodeId, node);
			}
			results.add(node);
		}
		return results;
	}
	
	/**
	 * Get all the nodes that currently exist as standard {@link DiscoverItems}.
	 * 
	 * @return {@link DiscoverItems} representing the existing nodes
	 * 
	 * @throws XMPPException
	 */
	public DiscoverItems discoverNodes()
		throws XMPPException
	{
		DiscoverItems items = new DiscoverItems();
		items.setTo(to);
		DiscoverItems nodeItems = (DiscoverItems)SyncPacketSend.getReply(con, items);
		return nodeItems;
	}
	
	/**
	 * Gets the subscriptions on the root node.
	 * 
	 * @return List of exceptions
	 * 
	 * @throws XMPPException
	 */
	public List<Subscription> getSubscriptions()
		throws XMPPException
	{
		Packet reply = sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.SUBSCRIPTIONS));
		SubscriptionsExtension subElem = (SubscriptionsExtension)reply.getExtension(PubSubElementType.SUBSCRIPTIONS.getElementName(), PubSubElementType.SUBSCRIPTIONS.getNamespace().getXmlns());
		return subElem.getSubscriptions();
	}
	
	/**
	 * Gets the affiliations on the root node.
	 * 
	 * @return List of affiliations
	 * 
	 * @throws XMPPException
	 */
	public List<Affiliation> getAffiliations()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.AFFILIATIONS));
		AffiliationsExtension listElem = (AffiliationsExtension)reply.getExtension(PubSubElementType.AFFILIATIONS);
		return listElem.getAffiliations();
	}

	/**
	 * Delete the specified node
	 * 
	 * @param nodeId
	 * @throws XMPPException
	 */
	public void deleteNode(String nodeId)
		throws XMPPException
	{
		sendPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.DELETE, nodeId), PubSubElementType.DELETE.getNamespace());
		nodeMap.remove(nodeId);
	}
	
	/**
	 * Returns the default settings for Node configuration.
	 * 
	 * @return configuration form containing the default settings.
	 */
	public ConfigureForm getDefaultConfiguration()
		throws XMPPException
	{
		// Errors will cause exceptions in getReply, so it only returns
		// on success.
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.DEFAULT), PubSubElementType.DEFAULT.getNamespace());
		return NodeUtils.getFormFromPacket(reply, PubSubElementType.DEFAULT);
	}
	
	/**
	 * Gets the supported features of the servers pubsub implementation
	 * as a standard {@link DiscoverInfo} instance.
	 * 
	 * @return The supported features
	 * 
	 * @throws XMPPException
	 */
	public DiscoverInfo getSupportedFeatures()
		throws XMPPException
	{
		ServiceDiscoveryManager mgr = ServiceDiscoveryManager.getInstanceFor(con);
		return mgr.discoverInfo(to);
	}
	
	private Packet sendPubsubPacket(Type type, PacketExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, ext, ns);
	}

	private Packet sendPubsubPacket(Type type, PacketExtension ext)
		throws XMPPException
	{
		return sendPubsubPacket(type, ext, null);
	}

	static PubSub createPubsubPacket(String to, Type type, PacketExtension ext)
	{
		return createPubsubPacket(to, type, ext, null);
	}
	
	static PubSub createPubsubPacket(String to, Type type, PacketExtension ext, PubSubNamespace ns)
	{
		PubSub request = new PubSub();
		request.setTo(to);
		request.setType(type);
		
		if (ns != null)
		{
			request.setPubSubNamespace(ns);
		}
		request.addExtension(ext);
		
		return request;
	}

	static Packet sendPubsubPacket(XMPPConnection con, String to, Type type, PacketExtension ext)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, ext, null);
	}
	
	static Packet sendPubsubPacket(XMPPConnection con, String to, Type type, PacketExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return SyncPacketSend.getReply(con, createPubsubPacket(to, type, ext, ns));
	}

	static Packet sendPubsubPacket(XMPPConnection con, String to, Type type, PubSub packet)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, packet, null);
	}

	static Packet sendPubsubPacket(XMPPConnection con, String to, Type type, PubSub packet, PubSubNamespace ns)
		throws XMPPException
	{
		return SyncPacketSend.getReply(con, packet);
	}

}
