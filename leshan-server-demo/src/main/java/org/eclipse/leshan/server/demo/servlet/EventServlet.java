/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessage;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageListener;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageTracer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.leshan.LwM2mId.LOCATION;

public class EventServlet extends EventSourceServlet {

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_AWAKE = "AWAKE";

    private static final String EVENT_SLEEPING = "SLEEPING";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private final Gson gson;

    private final CoapMessageTracer coapMessageTracer;

    private Set<LeshanEventSource> eventSources = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(Registration registration, Registration previousReg,
                Collection<Observation> previousObsersations) {
            String jReg = EventServlet.this.gson.toJson(registration);
            sendEvent(EVENT_REGISTRATION, jReg, registration.getEndpoint());
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            RegUpdate regUpdate = new RegUpdate();
            regUpdate.registration = updatedRegistration;
            regUpdate.update = update;
            String jReg = EventServlet.this.gson.toJson(regUpdate);
            sendEvent(EVENT_UPDATED, jReg, updatedRegistration.getEndpoint());
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            String jReg = EventServlet.this.gson.toJson(registration);
            sendEvent(EVENT_DEREGISTRATION, jReg, registration.getEndpoint());
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {

        @Override
        public void onSleeping(Registration registration) {
            String data = "{\"ep\":\"" + registration.getEndpoint() + "\"}";

            sendEvent(EVENT_SLEEPING, data, registration.getEndpoint());
        }

        @Override
        public void onAwake(Registration registration) {
            String data = "{\"ep\":\"" + registration.getEndpoint() + "\"}";
            sendEvent(EVENT_AWAKE, data, registration.getEndpoint());
        }
    };

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                        response.getContent().toString());
            }

            if (registration != null) {
                String data = "{\"ep\":\"" + registration.getEndpoint() + "\",\"res\":\"" +
                        observation.getPath().toString() + "\",\"val\":" +
                        gson.toJson(response.getContent()) + "}";

                // ********Saving into database**************************
                Gson gson = new Gson();
                JsonObject content = JsonParser.parseString(gson.toJson(response.getContent())).getAsJsonObject();

                String measurementCode = observation.getPath().toString().split("/")[1];
                String measurementName = "UNKNOWN";
                if(Integer.parseInt(measurementCode) == LOCATION) {
                    measurementName = "location";
                } else {
                    ClassLoader classLoader = getClass().getClassLoader();
                    try {
                        InputStream is = classLoader.getResourceAsStream("models/" + measurementCode + ".xml");
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        org.w3c.dom.Document doc = dBuilder.parse(is);
                        Element rootElement = doc.getDocumentElement();
                        measurementName = getString("Name", rootElement);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = toNearestWholeHour();
                String timestamp = dateFormat.format(date);

                try {
                    MongoClient mongoClient = new MongoClient("localhost", 27017);
                    MongoDatabase database = mongoClient.getDatabase("local");
                    MongoCollection<Document> collection = database.getCollection("events");
                    JsonObject resources = content.getAsJsonObject("resources");
                    String event = resources.toString();
                    Document document = new Document();
                    document.put("client_ep", registration.getEndpoint());
                    document.put("measurement", measurementName);
                    document.put("event", event);
                    document.put("timestamp", timestamp);
                    collection.insertOne(document);
                    mongoClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    System.exit(0);
                }

                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                        observation.getPath()), error);
            }
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
        }
    };

    public EventServlet(LeshanServer server, int securePort) {
        server.getRegistrationService().addListener(this.registrationListener);
        server.getObservationService().addListener(this.observationListener);
        server.getPresenceService().addListener(this.presenceListener);

        // add an interceptor to each endpoint to trace all CoAP messages
        coapMessageTracer = new CoapMessageTracer(server.getRegistrationService());
        for (Endpoint endpoint : server.coap().getServer().getEndpoints()) {
            endpoint.addInterceptor(coapMessageTracer);
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(server.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private synchronized void sendEvent(String event, String data, String endpoint) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);
        }

        for (LeshanEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                eventSource.sentEvent(event, data);
            }
        }
    }

    class ClientCoapListener implements CoapMessageListener {

        private final String endpoint;

        ClientCoapListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void trace(CoapMessage message) {
            String coapLog = EventServlet.this.gson.toJson(message);
            sendEvent(EVENT_COAP_LOG, coapLog, endpoint);
        }

    }

    private void cleanCoapListener(String endpoint) {
        // remove the listener if there is no more eventSources for this endpoint
        for (LeshanEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                return;
            }
        }
        coapMessageTracer.removeListener(endpoint);
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        return new LeshanEventSource(endpoint);
    }

    private class LeshanEventSource implements EventSource {

        private String endpoint;
        private Emitter emitter;

        public LeshanEventSource(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) {
            this.emitter = emitter;
            eventSources.add(this);
            if (endpoint != null) {
                coapMessageTracer.addListener(endpoint, new ClientCoapListener(endpoint));
            }
        }

        @Override
        public void onClose() {
            cleanCoapListener(endpoint);
            eventSources.remove(this);
        }

        public void sentEvent(String event, String data) {
            try {
                emitter.event(event, data);
            } catch (IOException e) {
                e.printStackTrace();
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    private static Date toNearestWholeHour() {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());

        if (c.get(Calendar.MINUTE) >= 30)
            c.add(Calendar.HOUR, 1);

        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        return c.getTime();
    }

    protected static String getString(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static class RegUpdate {
        public Registration registration;
        public RegistrationUpdate update;
    }
}
