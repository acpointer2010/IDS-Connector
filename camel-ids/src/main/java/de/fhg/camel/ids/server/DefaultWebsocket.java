/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.server;

import java.io.Serializable;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

@WebSocket
public class DefaultWebsocket implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DefaultWebsocket.class);

    private final WebsocketConsumer consumer;
    private final NodeSynchronization sync;
    private Session session;
    private String connectionKey;
	private FSM idsFsm;

    public DefaultWebsocket(NodeSynchronization sync, WebsocketConsumer consumer) {
        this.sync = sync;
        this.consumer = consumer;
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String message) {
        LOG.trace("onClose {} {}", closeCode, message);
        sync.removeSocket(this);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.trace("onConnect {}", session);
        this.session = session;
        this.connectionKey = UUID.randomUUID().toString();

        // Integrate server-side of IDS protocol
        idsFsm = new ProtocolMachine().initIDSProviderProtocol(session);
        
        sync.addSocket(this);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        LOG.debug("onMessage: {}", message);
        
        if (idsFsm.getState().equals("SUCCESS")) {//TODO Check if fsm is in its final state and successful
        	System.out.println("Successfully finished IDSP");
	        // TODO this should only be done when the IDS protocol has been finished successfully
	        if (this.consumer != null) {
	            this.consumer.sendMessage(this.connectionKey, message);
	        } else {
	            LOG.debug("No consumer to handle message received: {}", message);
	        }
        } else {
        	System.out.println("Feeding message into provider fsm: " + message);
        	idsFsm.feedEvent(new Event(message, ""));	//TODO we need to de-protobuf here and split messages into cmd and payload
        }
    }


    @OnWebSocketMessage
    public void onMessage(byte[] data, int offset, int length) {
        LOG.debug("onMessage: byte[]");
        if (this.consumer != null) {
            byte[] message = new byte[length];
            System.arraycopy(data, offset, message, 0, length);
            this.consumer.sendMessage(this.connectionKey, message);
        } else {
            LOG.debug("No consumer to handle message received: byte[]");
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }
}