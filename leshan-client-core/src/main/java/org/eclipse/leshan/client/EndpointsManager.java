/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client;

import org.eclipse.leshan.client.servers.Server;
import org.eclipse.leshan.client.servers.ServerInfo;

import java.util.Collection;

public interface EndpointsManager {

    Server createEndpoint(ServerInfo serverInfo);

    Collection<Server> createEndpoints(Collection<? extends ServerInfo> serverInfo);

    long getMaxCommunicationPeriodFor(Server server, long lifetimeInSeconds);

    void forceReconnection(Server server, boolean resume);

    void start();

    void stop();

    void destroy();
}
