/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.pubsub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.Header;
import org.jivesoftware.smackx.packet.HeadersExtension;
import org.jivesoftware.smackx.packet.PubSub;
import org.jivesoftware.smackx.packet.PubSubNamespace;
import org.jivesoftware.smackx.packet.SyncPacketSend;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;
import org.jivesoftware.smackx.pubsub.util.XmlUtils;

/**
 * The main class for the majority of pubsub functionality.  In general
 * almost all pubsub capabilities are related to the concept of a node.
 * All items are published to a node, and typically subscribed to by other
 * users.  These users then retrieve events based on this subscription.
 * 
 * @author Robin Collier
 */
public class Node
{
	private XMPPConnection con;
	private String id;
	private String to;
	
	private ConcurrentHashMap<ItemEventListener, PacketListener> itemEventToListenerMap = new ConcurrentHashMap<ItemEventListener, PacketListener>();
	private ConcurrentHashMap<ItemDeleteListener, PacketListener> itemDeleteToListenerMap = new ConcurrentHashMap<ItemDeleteListener, PacketListener>();
	private ConcurrentHashMap<NodeConfigListener, PacketListener> configEventToListenerMap = new ConcurrentHashMap<NodeConfigListener, PacketListener>();
	
	/**
	 * Construct a node associated to the supplied connection with the specified 
	 * node id.
	 * 
	 * @param connection The connection the node is associated with
	 * @param nodeName The node id
	 */
	Node(XMPPConnection connection, String nodeName)
	{
		con = connection;
		id = nodeName;
	}

	/**
	 * Some XMPP servers may require a specific service to be addressed on the 
	 * server.
	 * 
	 *   For example, OpenFire requires the server to be prefixed by <b>pubsub</b>
	 */
	void setTo(String toAddress)
	{
		to = toAddress;
	}

	/**
	 * Get the NodeId
	 * 
	 * @return the node id
	 */
	public String getId() 
	{
		return id;
	}
	/**
	 * Returns a configuration form, from which you can create an answer form to be submitted
	 * via the {@link #sendConfigurationForm(Form)}.
	 * 
	 * @return the configuration form
	 */
	public ConfigureForm getNodeConfiguration()
		throws XMPPException
	{
		Packet reply = sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.CONFIGURE_OWNER, getId()), PubSubNamespace.OWNER);
		return NodeUtils.getFormFromPacket(reply, PubSubElementType.CONFIGURE_OWNER);
	}
	
	/**
	 * Update the configuration with the contents of the new {@link Form}
	 * 
	 * @param submitForm
	 */
	public void sendConfigurationForm(Form submitForm)
		throws XMPPException
	{
		PubSub packet = createPubsubPacket(Type.SET, new FormNode(FormNodeType.CONFIGURE_OWNER, getId(), submitForm), PubSubNamespace.OWNER);
		SyncPacketSend.getReply(con, packet);
	}
	
	/**
	 * Discover node information in standard {@link DiscoverInfo} format.
	 * 
	 * @return The discovery information about the node.
	 * 
	 * @throws XMPPException
	 */
	public DiscoverInfo discoverInfo()
		throws XMPPException
	{
		DiscoverInfo info = new DiscoverInfo();
		info.setTo(to);
		info.setNode(getId());
		return (DiscoverInfo)SyncPacketSend.getReply(con, info);
	}
	
	/**
	 * Get information on the items in the node in standard
	 * {@link DiscoverItems} format.
	 * 
	 * @return The item details in {@link DiscoverItems} format
	 * 
	 * @throws XMPPException
	 */
	public DiscoverItems discoverItems()
		throws XMPPException
	{
		DiscoverItems items = new DiscoverItems();
		items.setTo(to);
		items.setNode(getId());
		return (DiscoverItems)SyncPacketSend.getReply(con, items);
	}

	/**
	 * Get the subscriptions currently associated with this node.
	 * 
	 * @return List of {@link Subscription}
	 * 
	 * @throws XMPPException
	 */
	public List<Subscription> getSubscriptions()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.SUBSCRIPTIONS, getId()));
		SubscriptionsExtension subElem = (SubscriptionsExtension)reply.getExtension(PubSubElementType.SUBSCRIPTIONS);
		return subElem.getSubscriptions();
	}

	/**
	 * The user subscribes to the node using the supplied jid.  The
	 * bare jid portion of this one must match the jid for the connection.
	 * 
	 * Please note that the {@link Subscription.State} should be checked 
	 * on return since more actions may be required by the caller.
	 * {@link Subscription.State#pending} - The owner must approve the subscription 
	 * request before messages will be received.
	 * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true, 
	 * the caller must configure the subscription before messages will be received.  If it is false
	 * the caller can configure it but is not required to do so.
	 * @param jid The jid to subscribe as.
	 * @return The subscription
	 * @exception XMPPException
	 */
	public Subscription subscribe(String jid)
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		return (Subscription)reply.getExtension(PubSubElementType.SUBSCRIPTION);
	}
	
	/**
	 * The user subscribes to the node using the supplied jid and subscription
	 * options.  The bare jid portion of this one must match the jid for the 
	 * connection.
	 * 
	 * Please note that the {@link Subscription.State} should be checked 
	 * on return since more actions may be required by the caller.
	 * {@link Subscription.State#pending} - The owner must approve the subscription 
	 * request before messages will be received.
	 * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true, 
	 * the caller must configure the subscription before messages will be received.  If it is false
	 * the caller can configure it but is not required to do so.
	 * @param jid The jid to subscribe as.
	 * @return The subscription
	 * @exception XMPPException
	 */
	public Subscription subscribe(String jid, SubscribeForm subForm)
		throws XMPPException
	{
		PubSub request = createPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		request.addExtension(new FormNode(FormNodeType.OPTIONS, subForm));
		PubSub reply = (PubSub)PubSubManager.sendPubsubPacket(con, jid, Type.SET, request);
		return (Subscription)reply.getExtension(PubSubElementType.SUBSCRIPTION);
	}

	/**
	 * Remove the subscription related to the specified JID.  This will only 
	 * work if there is only 1 subscription.  If there are multiple subscriptions,
	 * use {@link #unsubscribe(String, String)}.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * 
	 * @throws XMPPException
	 */
	public void unsubscribe(String jid)
		throws XMPPException
	{
		unsubscribe(jid, null);
	}
	
	/**
	 * Remove the specific subscription related to the specified JID.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * @param subscriptionId The id of the subscription being removed
	 * 
	 * @throws XMPPException
	 */
	public void unsubscribe(String jid, String subscriptionId)
		throws XMPPException
	{
		sendPubsubPacket(Type.SET, new UnsubscribeExtension(jid, getId(), subscriptionId));
	}

	/**
	 * Returns a SubscribeForm for subscriptions, from which you can create an answer form to be submitted
	 * via the {@link #sendConfigurationForm(Form)}.
	 * 
	 * @return A subscription options form
	 * 
	 * @throws XMPPException
	 */
	public SubscribeForm getSubscriptionOptions(String jid)
		throws XMPPException
	{
		return getSubscriptionOptions(jid, null);
	}

	/**
	 * Get the options for configuring the specified subscription.
	 * 
	 * @param jid JID the subscription is registered under
	 * @param subscriptionId The subscription id
	 * 
	 * @return The subscription option form
	 * 
	 * @throws XMPPException
	 */
	public SubscribeForm getSubscriptionOptions(String jid, String subscriptionId)
		throws XMPPException
	{
		PubSub packet = (PubSub)sendPubsubPacket(Type.GET, new OptionsExtension(jid, getId(), subscriptionId));
		FormNode ext = (FormNode)packet.getExtension(PubSubElementType.OPTIONS);
		return new SubscribeForm(ext.getForm());
	}

	/**
	 * Get the current items stored in the node.
	 * 
	 * @return List of {@link Item} in the node
	 * 
	 * @throws XMPPException
	 */
	public List<Item> getItems()
		throws XMPPException
	{
		PubSub request = createPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.ITEMS, getId()));
		
		PubSub result = (PubSub)SyncPacketSend.getReply(con, request);
		ItemsExtension itemsElem = (ItemsExtension)result.getExtension(PubSubElementType.ITEMS);
		return (List<Item>)itemsElem.getItems();
	}
	
	/**
	 * Get the items specified from the node.  This would typically be
	 * used when the server does not return the payload due to size 
	 * constraints.  The user would be required to retrieve the payload 
	 * after the items have been retrieved via {@link #getItems()} or an
	 * event, that did not include the payload.
	 * 
	 * @param ids Item ids of the items to retrieve
	 * 
	 * @return The list of {@link Item} with payload
	 * 
	 * @throws XMPPException
	 */
	public List<Item> getItems(Collection<String> ids)
		throws XMPPException
	{
		List<Item> itemList = new ArrayList<Item>(ids.size());
		
		for (String id : ids)
		{
			itemList.add(new Item(id));
		}
		PubSub request = createPubsubPacket(Type.GET, new ItemsExtension(ItemsExtension.ItemsElementType.items, getId(), itemList, null));
		
		PubSub result = (PubSub)SyncPacketSend.getReply(con, request);
		ItemsExtension itemsElem = (ItemsExtension)result.getExtension(PubSubElementType.ITEMS);
		return (List<Item>)itemsElem.getItems();
	}

	/**
	 * Get items persisted on the node, limited to the specified number.
	 * 
	 * @param maxItems Maximum number of items to return
	 * 
	 * @return List of {@link Item}
	 * 
	 * @throws XMPPException
	 */
	public List<Item> getItems(int maxItems)
		throws XMPPException
	{
		PubSub request = createPubsubPacket(Type.GET, new ItemsExtension(getId(), Integer.valueOf(maxItems)));
		
		PubSub result = (PubSub)SyncPacketSend.getReply(con, request);
		ItemsExtension itemsElem = (ItemsExtension)result.getExtension(PubSubElementType.ITEMS);
		return (List<Item>)itemsElem.getItems();
	}
	
	/**
	 * Publishes an event to the node.  This is an empty event
	 * with no item.
	 * 
	 * This is only acceptable for nodes with {@link ConfigureForm#isPersistItems()}=false
	 * and {@link ConfigureForm#isDeliverPayloads()}=false.
	 * 
	 * This is an asynchronous call which returns as soon as the 
	 * packet has been sent.
	 * 
	 * For synchronous calls use {@link #send() send()}.
	 */
	public void publish()
	{
		PubSub packet = createPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.PUBLISH, getId()));
		
		con.sendPacket(packet);
	}
	
	/**
	 * Publishes an event to the node.  This is a simple item
	 * with no payload.
	 * 
	 * If the id is null, an empty item (one without an id) will be sent.
	 * Please note that this is not the same as {@link #send()}, which
	 * publishes an event with NO item.
	 * 
	 * This is an asynchronous call which returns as soon as the 
	 * packet has been sent.
	 * 
	 * For synchronous calls use {@link #send(Item) send(Item))}.
	 * 
	 * @param item - The item being sent
	 */
	public void publish(Item item)
	{
		Collection<Item> items = new ArrayList<Item>(1);
		items.add((item == null ? new Item() : item));
		publish(items);
	}

	/**
	 * Publishes multiple events to the node.  Same rules apply as in {@link #publish(Item)}.
	 * 
	 * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
	 * list will get stored on the node, assuming it stores the last sent item.
	 * 
	 * This is an asynchronous call which returns as soon as the 
	 * packet has been sent.
	 * 
	 * For synchronous calls use {@link #send(Collection) send(Collection))}.
	 * 
	 * @param items - The collection of items being sent
	 */
	public void publish(Collection<Item> items)
	{
		PubSub packet = createPubsubPacket(Type.SET, new PublishItem(getId(), items));
		
		con.sendPacket(packet);
	}

	/**
	 * Publishes an event to the node.  This is an empty event
	 * with no item.
	 * 
	 * This is only acceptable for nodes with {@link ConfigureForm#isPersistItems()}=false
	 * and {@link ConfigureForm#isDeliverPayloads()}=false.
	 * 
	 * This is a synchronous call which will throw an exception 
	 * on failure.
	 * 
	 * For asynchronous calls, use {@link #publish() publish()}.
	 * 
	 * @throws XMPPException
	 */
	public void send()
		throws XMPPException
	{
		PubSub packet = createPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.PUBLISH, getId()));
		
		SyncPacketSend.getReply(con, packet);
	}
	
	/**
	 * Publishes an event to the node.  This can be either a simple item
	 * with no payload, or one with it.  This is determined by the Node
	 * configuration.
	 * 
	 * If the node has <b>deliver_payload=false</b>, the Item must not
	 * have a payload.
	 * 
	 * If the id is null, an empty item (one without an id) will be sent.
	 * Please note that this is not the same as {@link #send()}, which
	 * publishes an event with NO item.
	 * 
	 * This is a synchronous call which will throw an exception 
	 * on failure.
	 * 
	 * For asynchronous calls, use {@link #publish(Item) publish(Item)}.
	 * 
	 * @param item - The item being sent
	 * 
	 * @throws XMPPException
	 */
	public void send(Item item)
		throws XMPPException
	{
		Collection<Item> items = new ArrayList<Item>(1);
		items.add((item == null ? new Item() : item));
		send(items);
	}
	
	/**
	 * Publishes multiple events to the node.  Same rules apply as in {@link #send(Item)}.
	 * 
	 * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
	 * list will get stored on the node, assuming it stores the last sent item.
	 *  
	 * This is a synchronous call which will throw an exception 
	 * on failure.
	 * 
	 * For asynchronous calls, use {@link #publish(Collection) publish(Collection))}.
	 * 
	 * @param items - The collection of {@link Item} objects being sent
	 * 
	 * @throws XMPPException
	 */
	public void send(Collection<Item> items)
		throws XMPPException
	{
		PubSub packet = createPubsubPacket(Type.SET, new PublishItem(getId(), items));
		
		SyncPacketSend.getReply(con, packet);
	}
	
	/**
	 * Purges the node of all items.
	 *   
	 * <p>Note: Some implementations may keep the last item
	 * sent.
	 * 
	 * @throws XMPPException
	 */
	public void deleteAllItems()
		throws XMPPException
	{
		PubSub request = createPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.PURGE_OWNER, getId()), PubSubElementType.PURGE_OWNER.getNamespace());
		
		SyncPacketSend.getReply(con, request);
	}
	
	/**
	 * Delete the item with the specified id from the node.
	 * 
	 * @param itemId The id of the item
	 * 
	 * @throws XMPPException
	 */
	public void deleteItem(String itemId)
		throws XMPPException
	{
		Collection<String> items = new ArrayList<String>(1);
		items.add(itemId);
		deleteItem(items);
	}
	
	/**
	 * Delete the items with the specified id's from the node.
	 * 
	 * @param itemIds The list of id's of items to delete
	 * 
	 * @throws XMPPException
	 */
	public void deleteItem(Collection<String> itemIds)
		throws XMPPException
	{
		List<Item> items = new ArrayList<Item>(itemIds.size());
		
		for (String id : itemIds)
		{
			items.add(new Item(id));
		}
		PubSub request = createPubsubPacket(Type.SET, new ItemsExtension(ItemsExtension.ItemsElementType.retract, getId(), items, null));
		SyncPacketSend.getReply(con, request);
	}

	/**
	 * Register a listener for item publication events.  This 
	 * listener will get called whenever an item is published to 
	 * this node.
	 * 
	 * @param listener The handler for the event
	 */
	public void addItemEventListener(ItemEventListener listener)
	{
		PacketListener conListener = new ItemEventTranslator(listener); 
		itemEventToListenerMap.put(listener, conListener);
		con.addPacketListener(conListener, new EventContentFilter(EventElementType.items.toString(), "item"));
	}

	/**
	 * Unregister a listener for publication events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemEventListener(ItemEventListener listener)
	{
		PacketListener conListener = itemEventToListenerMap.remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}

	/**
	 * Register a listener for configuration events.  This listener
	 * will get called whenever the node's configuration changes.
	 * 
	 * @param listener The handler for the event
	 */
	public void addConfigurationListener(NodeConfigListener listener)
	{
		PacketListener conListener = new NodeConfigTranslator(listener); 
		configEventToListenerMap.put(listener, conListener);
		con.addPacketListener(conListener, new EventContentFilter(EventElementType.configuration.toString()));
	}

	/**
	 * Unregister a listener for configuration events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeConfigurationListener(NodeConfigListener listener)
	{
		PacketListener conListener = configEventToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}
	
	/**
	 * Register an listener for item delete events.  This listener
	 * gets called whenever an item is deleted from the node.
	 * 
	 * @param listener The handler for the event
	 */
	public void addItemDeleteListener(ItemDeleteListener listener)
	{
		PacketListener delListener = new ItemDeleteTranslator(listener); 
		itemDeleteToListenerMap.put(listener, delListener);
		EventContentFilter deleteItem = new EventContentFilter(EventElementType.items.toString(), "retract");
		EventContentFilter purge = new EventContentFilter(EventElementType.purge.toString());
		
		con.addPacketListener(delListener, new OrFilter(deleteItem, purge));
	}

	/**
	 * Unregister a listener for item delete events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemDeleteListener(ItemDeleteListener listener)
	{
		PacketListener conListener = itemDeleteToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}

	
	@Override
	public String toString()
	{
		return super.toString() + " Node id: " + id;
	}

	private PubSub createPubsubPacket(Type type, PacketExtension ext)
	{
		return createPubsubPacket(type, ext, null);
	}
	
	private PubSub createPubsubPacket(Type type, PacketExtension ext, PubSubNamespace ns)
	{
		return PubSubManager.createPubsubPacket(to, type, ext, ns);
	}

	private Packet sendPubsubPacket(Type type, NodeExtension ext)
		throws XMPPException
	{
		return PubSubManager.sendPubsubPacket(con, to, type, ext);
	}

	private Packet sendPubsubPacket(Type type, NodeExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return PubSubManager.sendPubsubPacket(con, to, type, ext, ns);
	}

	private static List<String> getSubscriptionIds(Packet packet)
	{
		HeadersExtension headers = (HeadersExtension)packet.getExtension("headers", "http://jabber.org/protocol/shim");
		List<String> values = null;
		
		if (headers != null)
		{
			values = new ArrayList<String>(headers.getHeaders().size());
			
			for (Header header : headers.getHeaders())
			{
				values.add(header.getValue());
			}
		}
		return values;
	}

	/**
	 * This class translates low level item publication events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class ItemEventTranslator implements PacketListener
	{
		private ItemEventListener listener;

		public ItemEventTranslator(ItemEventListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
			DelayInformation delay = (DelayInformation)packet.getExtension("delay", "urn:xmpp:delay");
			
			// If there was no delay based on XEP-0203, then try XEP-0091 for backward compatibility
			if (delay == null)
			{
				delay = (DelayInformation)packet.getExtension("x", "jabber:x:delay");
			}
			ItemPublishEvent eventItems = new ItemPublishEvent(itemsElem.getNode(), (List<Item>)itemsElem.getItems(), getSubscriptionIds(packet), (delay == null ? null : delay.getStamp()));
			listener.handlePublishedItems(eventItems);
		}
	}

	/**
	 * This class translates low level item deletion events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class ItemDeleteTranslator implements PacketListener
	{
		private ItemDeleteListener listener;

		public ItemDeleteTranslator(ItemDeleteListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
	        
	        List<PacketExtension> extList = event.getExtensions();
	        
	        if (extList.get(0).getElementName().equals(PubSubElementType.PURGE_EVENT.getElementName()))
	        {
	        	listener.handlePurge();
	        }
	        else
	        {
				ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
				Collection<? extends PacketExtension> pubItems = itemsElem.getItems();
				Iterator<RetractItem> it = (Iterator<RetractItem>)pubItems.iterator();
				List<String> items = new ArrayList<String>(pubItems.size());

				while (it.hasNext())
				{
					RetractItem item = it.next();
					items.add(item.getId());
				}

				ItemDeleteEvent eventItems = new ItemDeleteEvent(itemsElem.getNode(), items, getSubscriptionIds(packet));
				listener.handleDeletedItems(eventItems);
	        }
		}
	}
	
	/**
	 * This class translates low level node configuration events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class NodeConfigTranslator implements PacketListener
	{
		private NodeConfigListener listener;

		public NodeConfigTranslator(NodeConfigListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ConfigurationEvent config = (ConfigurationEvent)event.getEvent();

			listener.handleNodeConfiguration(config);
		}
	}

	/**
	 * Filter for {@link PacketListener} to filter out events not specific to the 
	 * event type expected for this node.
	 * 
	 * @author Robin Collier
	 */
	class EventContentFilter implements PacketFilter
	{
		private String firstElement;
		private String secondElement;
		
		EventContentFilter(String elementName)
		{
			firstElement = elementName;
		}

		EventContentFilter(String firstLevelEelement, String secondLevelElement)
		{
			firstElement = firstLevelEelement;
			secondElement = secondLevelElement;
		}

		public boolean accept(Packet packet)
		{
			if (!(packet instanceof Message))
				return false;

			EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			
			if (event == null)
				return false;

			NodeExtension embedEvent = event.getEvent();
			
			if (embedEvent == null)
				return false;
			
			if (embedEvent.getElementName().equals(firstElement))
			{
				if (!embedEvent.getNode().equals(getId()))
					return false;
				
				if (secondElement == null)
					return true;
				
				if (embedEvent instanceof EmbeddedPacketExtension)
				{
					List<PacketExtension> secondLevelList = ((EmbeddedPacketExtension)embedEvent).getExtensions();
					
					if (secondLevelList.size() > 0 && secondLevelList.get(0).getElementName().equals(secondElement))
						return true;
				}
			}
			return false;
		}
	}
}
