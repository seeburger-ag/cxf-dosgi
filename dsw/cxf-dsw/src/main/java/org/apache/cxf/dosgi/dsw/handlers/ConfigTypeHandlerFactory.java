/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.dosgi.dsw.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTypeHandlerFactory {

    protected static final String DEFAULT_CONFIGURATION_TYPE = Constants.WS_CONFIG_TYPE;
    private static final Logger LOG = LoggerFactory.getLogger(ConfigTypeHandlerFactory.class);

    // protected because of tests
    protected final List<String> supportedConfigurationTypes;

    // protected because of tests
    protected ConfigTypeHandlerTracker handlerTracker;

    public ConfigTypeHandlerFactory(BundleContext bc) {
        this(bc, new ConfigTypeHandlerTracker(bc));
        handlerTracker.open(true);
    }

    /**
     * for unit tests
     * @param bc
     * @param intentManager
     * @param handlerTracker
     */
    public ConfigTypeHandlerFactory(BundleContext bc, ConfigTypeHandlerTracker handlerTracker) {
        supportedConfigurationTypes = new ArrayList<String>();
        this.handlerTracker = handlerTracker;
    }


    public ConfigurationTypeHandler getHandler(BundleContext dswBC,
            Map<String, Object> serviceProperties) {
        List<String> configurationTypes = determineConfigurationTypes(serviceProperties);
        return getHandler(dswBC, configurationTypes, serviceProperties);
    }

    public ConfigurationTypeHandler getHandler(BundleContext dswBC, EndpointDescription endpoint) {
        List<String> configurationTypes = determineConfigTypesForImport(endpoint);
        return getHandler(dswBC, configurationTypes, endpoint.getProperties());
    }

    private ConfigurationTypeHandler getHandler(BundleContext dswBC,
                                               List<String> configurationTypes,
                                               Map<String, Object> serviceProperties) {
        ConfigurationTypeHandler handler = handlerTracker.getConfigTypeHandler(configurationTypes);
        if (handler != null) {
            return handler;
        }
        throw new RuntimeException("None of the configuration types in " + configurationTypes + " is supported.");
    }

    /**
     * determine which configuration types should be used / if the requested are
     * supported
     */
    private List<String> determineConfigurationTypes(Map<String, Object> serviceProperties) {
        String[] requestedConfigurationTypes = Utils.normalizeStringPlus(serviceProperties
                .get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        if (requestedConfigurationTypes == null || requestedConfigurationTypes.length == 0) {
            return Collections.singletonList(DEFAULT_CONFIGURATION_TYPE);
        }

        List<String> configurationTypes = new ArrayList<String>();
        for (String rct : requestedConfigurationTypes) {
            if (getAllSupportedConfigurationTypes().contains(rct)) {
                configurationTypes.add(rct);
            }
        }
        LOG.info("configuration types selected for export: " + configurationTypes);
        if (configurationTypes.isEmpty()) {
            throw new RuntimeException("the requested configuration types are not supported");
        }
        return configurationTypes;
    }

    private List<String> determineConfigTypesForImport(EndpointDescription endpoint) {
        List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

        if (remoteConfigurationTypes == null) {
            throw new RuntimeException("The supplied endpoint has no configuration type");
        }

        List<String> usableConfigurationTypes = new ArrayList<String>();
        for (String ct : getAllSupportedConfigurationTypes()) {
            if (remoteConfigurationTypes.contains(ct)) {
                usableConfigurationTypes.add(ct);
            }
        }

        if (usableConfigurationTypes.isEmpty()) {
            throw new RuntimeException("The supplied endpoint has no compatible configuration type. "
                    + "Supported types are: " + getAllSupportedConfigurationTypes()
                    + "    Types needed by the endpoint: " + remoteConfigurationTypes);
        }
        return usableConfigurationTypes;
    }

    public List<String> getSupportedConfigurationTypes() {
        return supportedConfigurationTypes;
    }

    /**
     * returns a list of all supported configuration types.
     * The list includes the built-in and contributed types.
     * @return the supported configuration types
     */
    public List<String> getAllSupportedConfigurationTypes() {
        List<String> knownTypes = new ArrayList<String>(supportedConfigurationTypes);
        knownTypes.addAll(handlerTracker.getSupportedTypes());
        return knownTypes;
    }
}
