/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.BootstrapIntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.TestObjectsInitializer;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.SecureIntegrationTestHelper.GOOD_PSK_KEY;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BootstrapTest {

    private final BootstrapIntegrationTestHelper helper = new BootstrapIntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
    }

    @After
    public void stop() {
        helper.client.destroy(true);
        helper.bootstrapServer.destroy();
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void bootstrap() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrap_tlv_only() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        ContentFormat noPreferredFormat = null; // if no preferred content format server should use TLV
        ContentFormat supportedFormat = ContentFormat.TLV;
        helper.createClient(noPreferredFormat, supportedFormat);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrap_senmlcbor_only() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        ContentFormat preferredFormat = ContentFormat.SENML_CBOR;
        ContentFormat supportedFormat = ContentFormat.SENML_CBOR;
        helper.createClient(preferredFormat, supportedFormat);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapWithAdditionalAttributes() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null);
        helper.bootstrapServer.start();

        // Create Client with additional attributes
        // and check it is not already registered
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("imei", "2136872368");
        helper.createClient(additionalAttributes);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();

        // assert session contains additional attributes
        assertEquals(additionalAttributes, helper.lastBootstrapSession.getBootstrapRequest().getAdditionalAttributes());
    }

    @Test
    public void bootstrapWithDiscoverOnRoot() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, null, new BootstrapDiscoverRequest());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
        assertNotNull(helper.lastDiscoverAnswer);
        assertEquals(ResponseCode.CONTENT, helper.lastDiscoverAnswer.getCode());
        assertEquals("</>;lwm2m=1.0,</0>;ver=1.1,</0/0>,</1>;ver=1.1,</3>;ver=1.1,</3/0>",
                Link.serialize(helper.lastDiscoverAnswer.getObjectLinks()));
    }

    @Test
    public void bootstrapWithDiscoverOnDevice() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, null, new BootstrapDiscoverRequest(3));
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
        assertNotNull(helper.lastDiscoverAnswer);
        assertEquals(ResponseCode.CONTENT, helper.lastDiscoverAnswer.getCode());
        assertEquals("</>;lwm2m=1.0,</3>;ver=1.1,</3/0>", Link.serialize(helper.lastDiscoverAnswer.getObjectLinks()));
    }

    @Test
    public void bootstrapDeleteSecurity() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null,
                helper.deleteSecurityStore(LwM2mId.ACCESS_CONTROL, LwM2mId.CONNECTIVITY_STATISTICS));
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        ObjectsInitializer initializer = new TestObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.ACCESS_CONTROL, new SimpleInstanceEnabler());
        initializer.setInstancesForObject(LwM2mId.CONNECTIVITY_STATISTICS, new SimpleInstanceEnabler());
        helper.createClient(helper.withoutSecurity(), initializer);
        helper.assertClientNotRegisterered();

        // Start it and wait for bootstrap finished
        helper.client.start();
        helper.waitForBootstrapFinishedAtClientSide(1);

        // ensure instances are deleted
        ReadResponse response = helper.client.getObjectTree().getObjectEnabler(LwM2mId.ACCESS_CONTROL)
                .read(ServerIdentity.SYSTEM, new ReadRequest(LwM2mId.ACCESS_CONTROL));
        assertTrue("ACL instance is not deleted", ((LwM2mObject) response.getContent()).getInstances().isEmpty());

        response = helper.client.getObjectTree().getObjectEnabler(LwM2mId.CONNECTIVITY_STATISTICS)
                .read(ServerIdentity.SYSTEM, new ReadRequest(LwM2mId.CONNECTIVITY_STATISTICS));
        assertTrue("Connectvity instance is not deleted",
                ((LwM2mObject) response.getContent()).getInstances().isEmpty());
    }

    @Test
    public void bootstrapDeleteAll() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.deleteSecurityStore("/"));
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        ObjectsInitializer initializer = new TestObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.ACCESS_CONTROL, new SimpleInstanceEnabler());
        initializer.setInstancesForObject(LwM2mId.CONNECTIVITY_STATISTICS, new SimpleInstanceEnabler());
        helper.createClient(helper.withoutSecurity(), initializer);
        helper.assertClientNotRegisterered();

        // Start it and wait for bootstrap finished
        helper.client.start();
        helper.waitForBootstrapFinishedAtClientSide(1);

        // ensure instances are deleted except device instance and bootstrap server
        for (LwM2mObjectEnabler enabler : helper.client.getObjectTree().getObjectEnablers().values()) {
            ReadResponse response = enabler.read(ServerIdentity.SYSTEM, new ReadRequest(enabler.getId()));
            LwM2mObject responseValue = (LwM2mObject) response.getContent();
            if (enabler.getId() == LwM2mId.DEVICE) {
                assertTrue("The Device instance should still be here", responseValue.getInstances().size() == 1);
            } else if (enabler.getId() == LwM2mId.SECURITY) {
                assertTrue("Only bootstrap security instance should be here",
                        ((LwM2mObject) response.getContent()).getInstances().size() == 1);
                LwM2mObjectInstance securityInstance = responseValue.getInstances().values().iterator().next();
                assertTrue("Only bootstrap security instance should be here",
                        securityInstance.getResource(1).getValue() == Boolean.TRUE);
            } else {
                assertTrue(enabler.getObjectModel().name + " instance is not deleted",
                        responseValue.getInstances().isEmpty());
            }
        }
    }

    @Test
    public void bootstrapWithAcl() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.unsecuredWithAclBootstrapStore());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        ObjectsInitializer initializer = new TestObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.ACCESS_CONTROL, new SimpleInstanceEnabler());
        helper.createClient(helper.withoutSecurity(), initializer);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();

        // ensure ACL is correctly set
        ReadResponse response = helper.client.getObjectTree().getObjectEnabler(2).read(ServerIdentity.SYSTEM,
                new ReadRequest(2));
        LwM2mObject acl = (LwM2mObject) response.getContent();
        assertThat(acl.getInstances().keySet(), hasItems(0, 1));
        LwM2mObjectInstance instance = acl.getInstance(0);
        assertEquals(3l, instance.getResource(0).getValue());
        assertEquals(0l, instance.getResource(1).getValue());
        assertEquals(1l, instance.getResource(2).getValue(3333));
        assertEquals(2222l, instance.getResource(3).getValue());
        instance = acl.getInstance(1);
        assertEquals(4l, instance.getResource(0).getValue());
        assertEquals(0l, instance.getResource(1).getValue());
        assertEquals(2222l, instance.getResource(3).getValue());
    }

    @Test
    public void bootstrapSecureWithPSK() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.PSK));
        helper.bootstrapServer.start();

        // Create PSK Client and check it is not already registered
        helper.createPSKClient(GOOD_PSK_ID, GOOD_PSK_KEY);
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapSecureWithBadPSKKey() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.PSK));
        helper.bootstrapServer.start();

        // Create PSK Client with bad credentials and check it is not already registered
        helper.createRPKClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.ensureNoRegistration(1);

        // check the client is not registered
        helper.assertClientNotRegisterered();
    }

    @Test
    public void bootstrapSecureWithRPK() {
        // Create DM Server without security & start it
        helper.createServer();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(helper.bsSecurityStore(SecurityMode.RPK));
        helper.bootstrapServer.start();

        // Create RPK Client and check it is not already registered
        helper.createRPKClient();
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapToPSKServer() throws NonUniqueSecurityInfoException {
        // Create DM Server & start it
        helper.createServer(); // default server support PSK
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.pskBootstrapStore());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newPreSharedKeyInfo(helper.getCurrentEndpoint(), GOOD_PSK_ID, GOOD_PSK_KEY));

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }

    @Test
    public void bootstrapToRPKServer() throws NonUniqueSecurityInfoException {
        // Create DM Server with RPK support & start it
        helper.createServerWithRPK();
        helper.server.start();

        // Create and start bootstrap server
        helper.createBootstrapServer(null, helper.rpkBootstrapStore());
        helper.bootstrapServer.start();

        // Create Client and check it is not already registered
        helper.createClient();
        helper.assertClientNotRegisterered();

        // Add client credentials to the server
        helper.getSecurityStore()
                .add(SecurityInfo.newRawPublicKeyInfo(helper.getCurrentEndpoint(), helper.clientPublicKey));

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        // check the client is registered
        helper.assertClientRegisterered();
    }
}
