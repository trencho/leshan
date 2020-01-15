/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *    Bosch Software Innovations GmbH - initial implementation
 ******************************************************************************/
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;

import javax.security.auth.x500.X500Principal;
import java.security.Principal;

/**
 * LWM2M principal based endpoint context matcher.
 * 
 * Matches DTLS based on the used principal. Requires unique credentials.
 * 
 * For x.509, only the CN is checked, because the other parts of the distinguished names are removed when converting it
 * into a {@Link Identity}.
 * 
 * For more detailed about why this is needed, see https://github.com/eclipse/leshan/wiki/LWM2M-Observe#for-dtls
 */
public class Lwm2mEndpointContextMatcher extends PrincipalEndpointContextMatcher {

    public Lwm2mEndpointContextMatcher() {
    }

    @Override
    public String getName() {
        return "lwm2m correlation";
    }

    /**
     * {@inheritDoc}
     * 
     * For LWM2M x.509 principals consider only the common name for matching.
     */
    @Override
    protected boolean matchPrincipals(Principal requestedPrincipal, Principal availablePrincipal) {
        if (requestedPrincipal instanceof X500Principal || availablePrincipal instanceof X500Principal) {
            try {
                String requestedCommonName = EndpointContextUtil.extractCN(requestedPrincipal.getName());
                String availableCommonName = EndpointContextUtil.extractCN(availablePrincipal.getName());
                return requestedCommonName.equals(availableCommonName);
            } catch (IllegalStateException e) {
                return false;
            }
        } else {
            return super.matchPrincipals(requestedPrincipal, availablePrincipal);
        }
    }
}
