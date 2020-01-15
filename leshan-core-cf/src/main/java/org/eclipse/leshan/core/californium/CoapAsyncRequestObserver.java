/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - redirect onSendError
 *                                                     to error callback.
 *     Simon Bernard                                 - use specific exception for onSendError
 *******************************************************************************/
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Californium message observer for a CoAP request.
 * <p>
 * Either a response or an error is raised. Results are available via callbacks.
 * <p>
 * This class also provides response timeout facility.
 * 
 * @see https://github.com/eclipse/leshan/wiki/Request-Timeout for more details.
 */
public class CoapAsyncRequestObserver extends AbstractRequestObserver {

    private static final Logger LOG = LoggerFactory.getLogger(CoapAsyncRequestObserver.class);

    protected CoapResponseCallback responseCallback;
    private final ErrorCallback errorCallback;
    private final long timeoutInMs;
    private ScheduledFuture<?> cleaningTask;
    private boolean cancelled = false;
    private ScheduledExecutorService executor;

    // The Californium API does not ensure that message callback are exclusive
    // meaning that you can get a onReponse call and a onCancel one.
    // The CoapAsyncRequestObserver ensure that you will receive only one event.
    // You get either 1 response or 1 error.
    // This boolean is used to ensure this.
    private AtomicBoolean eventRaised = new AtomicBoolean(false);

    private final AtomicBoolean responseTimedOut = new AtomicBoolean(false);

    /**
     * A Californium message observer for a CoAP request helping to get results asynchronously.
     * <p>
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. The CoapAsyncRequestObserver ensure that you will receive only one event.
     * Meaning, you get either 1 response or 1 error.
     * 
     * @param coapRequest The CoAP request to observe.
     * @param responseCallback This is called when a response is received. This MUST NOT be null.
     * @param errorCallback This is called when an error happens. This MUST NOT be null.
     * @param timeoutInMs A response timeout(in millisecond) which is raised if neither a response or error happens (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).
     * @param executor used to scheduled timeout tasks.
     */
    public CoapAsyncRequestObserver(Request coapRequest, CoapResponseCallback responseCallback,
            ErrorCallback errorCallback, long timeoutInMs, ScheduledExecutorService executor) {
        super(coapRequest);
        this.responseCallback = responseCallback;
        this.errorCallback = errorCallback;
        this.timeoutInMs = timeoutInMs;
        this.executor = executor;
    }

    @Override
    public void onResponse(Response coapResponse) {
        LOG.debug("Received coap response: {} for {}", coapResponse, coapRequest);
        coapRequest.removeMessageObserver(this);
        if (eventRaised.compareAndSet(false, true)) {
            cleaningTask.cancel(false);
            try {
                responseCallback.onResponse(coapResponse);
            } catch (RuntimeException e) {
                LOG.warn("Uncaught exception during onResponse callback");
            }

        } else {
            LOG.debug("OnResponse callback ignored because an event was already raised for this request {}",
                    coapRequest);
        }
    }

    @Override
    public void onReadyToSend() {
        scheduleCleaningTask();
    }

    @Override
    public void onTimeout() {
        cancelCleaningTask();
        if (eventRaised.compareAndSet(false, true)) {
            errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException("Request %s timed out",
                    coapRequest.getURI()));
        } else {
            LOG.debug("OnTimeout callback ignored because an event was already raised for this request {}",
                    coapRequest);
        }
    }

    @Override
    public void onCancel() {
        cancelCleaningTask();
        if (eventRaised.compareAndSet(false, true)) {
            if (responseTimedOut.get()) {
                errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException(
                        "Request %s timed out", coapRequest.getURI()));
            } else {
                errorCallback.onError(new RequestCanceledException("Request %s cancelled", coapRequest.getURI()));
            }
        } else {
            LOG.debug(
                    "OnCancel(responsetimeout={}) callback ignored because an event was already raised for this request {}",
                    responseTimedOut.get(), coapRequest);
        }
    }

    @Override
    public void onReject() {
        cancelCleaningTask();
        if (eventRaised.compareAndSet(false, true)) {
            errorCallback.onError(new RequestRejectedException("Request %s rejected", coapRequest.getURI()));
        } else {
            LOG.debug("OnReject callback ignored because an event was already raised for this request {}", coapRequest);
        }
    }

    @Override
    public void onSendError(Throwable error) {
        cancelCleaningTask();
        if (eventRaised.compareAndSet(false, true)) {
            errorCallback.onError(new SendFailedException(error, "Unable to send request %s", coapRequest.getURI()));
        } else {
            LOG.debug("onSendError callback ignored because an event was already raised for this request {}",
                    coapRequest);
        }
    }

    private synchronized void scheduleCleaningTask() {
        if (!cancelled)
            if (cleaningTask == null) {
                LOG.trace("Schedule Cleaning Task for {}", coapRequest);
                cleaningTask = executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        responseTimedOut.set(true);
                        coapRequest.cancel();
                    }
                }, timeoutInMs, TimeUnit.MILLISECONDS);
            }
    }

    private synchronized void cancelCleaningTask() {
        if (cleaningTask != null) {
            cleaningTask.cancel(false);
        }
        cancelled = true;
    }
}