/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.bootstrap;

import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfiguration;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigurationStore;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

public class LeshanBootstrapServerTest {

    private BootstrapHandler bsHandler;

    private LeshanBootstrapServer createBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder()
                .setLocalAddress(new InetSocketAddress(0));
        builder.setBootstrapHandlerFactory(new BootstrapHandlerFactory() {

            @Override
            public BootstrapHandler create(BootstrapConfigurationStore store, LwM2mBootstrapRequestSender sender,
                    BootstrapSessionManager sessionManager) {
                bsHandler = new DefaultBootstrapHandler(store, sender, sessionManager);
                return bsHandler;
            }
        });
        builder.setConfigStore(new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
                return new BootstrapConfiguration(new BootstrapDeleteRequest());
            }
        });
        return builder.build();

    }

    @Test
    public void testStartStopStart() throws InterruptedException {
        LeshanBootstrapServer server = createBootstrapServer();

        server.start();
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.start();
    }

    @Test
    public void testStartDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        LeshanBootstrapServer server = createBootstrapServer();
        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation();
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals("All news created threads must be destroyed", numberOfThreadbefore, Thread.activeCount());
    }

    @Test
    public void testStartStopDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        LeshanBootstrapServer server = createBootstrapServer();
        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation();
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals("All news created threads must be destroyed", numberOfThreadbefore, Thread.activeCount());
    }

    private void forceThreadsCreation() {
        SendableResponse<BootstrapResponse> bootstrap = bsHandler
                .bootstrap(Identity.unsecure(new InetSocketAddress(5683)), new BootstrapRequest("test"));
        bootstrap.sent();
    }
}
