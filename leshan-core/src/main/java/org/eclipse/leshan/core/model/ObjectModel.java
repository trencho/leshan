/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.model;

import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object description.
 * 
 * @see "Lwm2M specification D.1 Object Template"
 * @see <a href="http://openmobilealliance.org/tech/profiles/LWM2M.xsd">LWM2M Editor Schema</a>
 */
public class ObjectModel {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectModel.class);

    public static final String DEFAULT_VERSION = "1.0";

    private static final int OMA_OBJECT_MIN_ID = 0;
    private static final int OMA_OBJECT_MAX_ID = 1023;

    // TODO in version 2.0 all field should be null-able and ObjectModelValidator should be responsible to validate it.
    public final int id;
    public final String name;
    public final String description;
    public final String version;
    public final boolean multiple;
    public final boolean mandatory;
    /** @since 1.1 */
    public final String urn;
    /** @since 1.1 */
    public final String lwm2mVersion;
    /** @since 1.1 */
    public final String description2;

    public final Map<Integer, ResourceModel> resources; // resources by ID

    public ObjectModel(int id, String name, String description, String version, boolean multiple, boolean mandatory,
            ResourceModel... resources) {
        this(id, name, description, version, multiple, mandatory, Arrays.asList(resources));
    }

    public ObjectModel(int id, String name, String description, String version, boolean multiple, boolean mandatory,
            Collection<ResourceModel> resources) {
        this(id, name, description, version, multiple, mandatory, resources, URN.generateURN(id, version), null, "");
    }

    /**
     * @since 1.1
     */
    public ObjectModel(int id, String name, String description, String version, boolean multiple, boolean mandatory,
            Collection<ResourceModel> resources, String urn, String lwm2mVersion, String description2) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.multiple = multiple;
        this.mandatory = mandatory;
        this.urn = urn;
        this.lwm2mVersion = lwm2mVersion;
        this.description2 = description2;

        Map<Integer, ResourceModel> resourcesMap = new HashMap<>(resources.size());
        for (ResourceModel resource : resources) {
            ResourceModel old = resourcesMap.put(resource.id, resource);
            if (old != null) {
                LOG.debug("Model already exists for resource {} of object {}. Overriding it.", resource.id, id);
            }
            resourcesMap.put(resource.id, resource);
        }
        this.resources = Collections.unmodifiableMap(resourcesMap);
    }

    public boolean isOmaObject() {
        return id >= OMA_OBJECT_MIN_ID && id <= OMA_OBJECT_MAX_ID;
    }

    /**
     * @return the version and if the version is null or empty return the default value 1.0
     * @see ObjectModel#DEFAULT_VERSION
     */
    public String getVersion() {
        if (version == null || version.isEmpty()) {
            return ObjectModel.DEFAULT_VERSION;
        }
        return version;
    }

    /**
     * @return the LWM2M version and if the LWM2M version is null or empty return the default value 1.0
     * @see LwM2m#VERSION
     * @since 1.1
     */
    public String getLwM2mVersion() {
        if (lwm2mVersion == null || lwm2mVersion.isEmpty()) {
            return LwM2m.VERSION;
        }
        return lwm2mVersion;
    }

    @Override
    public String toString() {
        return String.format(
                "ObjectModel [id=%s, name=%s, description=%s, version=%s, multiple=%s, mandatory=%s, urn=%s, lwm2mVersion=%s, description2=%s, resources=%s]",
                id, name, description, version, multiple, mandatory, urn, lwm2mVersion, description2, resources);
    }
}
