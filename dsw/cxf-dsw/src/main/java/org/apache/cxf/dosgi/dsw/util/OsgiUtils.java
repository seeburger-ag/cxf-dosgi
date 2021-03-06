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
package org.apache.cxf.dosgi.dsw.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public final class OsgiUtils {

    public static final Logger LOG = LoggerFactory.getLogger(OsgiUtils.class);

    private OsgiUtils() {
    }

    public static boolean getBooleanProperty(Map<String, Object> sd, String name) {
        return toBoolean(sd.get(name));
    }

    public static boolean toBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value
            || value instanceof String && Boolean.parseBoolean((String)value);
    }

    @SuppressWarnings("unchecked")
    public static Collection<String> getMultiValueProperty(Object property) {
        if (property == null) {
            return null;
        } else if (property instanceof Collection) {
            return (Collection<String>)property;
        } else if (property instanceof String[]) {
            return Arrays.asList((String[])property);
        } else {
            return Collections.singleton(property.toString());
        }
    }

    public static String getProperty(EndpointDescription endpoint, String name) {
        return getProperty(endpoint.getProperties(), name);
    }

    public static String getProperty(Map<String, Object> dict, String name) {
        Object value = dict.get(name);
        return value instanceof String ? (String) value : null;
    }

    public static String getFirstNonEmptyStringProperty(Map<String, Object> dict, String ... keys) {
        for (String key : keys) {
            String value = getProperty(dict, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Tries to retrieve the version of iClass via the PackageAdmin.
     *
     * @param iClass tThe interface for which the version should be found
     * @param bc any valid BundleContext
     * @return the version of the interface or "0.0.0" if no version information could be found or an error
     *         occurred during the retrieval
     */
    public static String getVersion(Class<?> iClass, BundleContext bc) {
        ServiceReference<PackageAdmin> paRef = bc.getServiceReference(PackageAdmin.class);
        if (paRef != null) {
            PackageAdmin pa = bc.getService(paRef);
            try {
                Bundle b = pa.getBundle(iClass);
                if (b == null) {
                    LOG.info("Unable to find interface version for interface " + iClass.getName()
                            + ". Falling back to 0.0.0");
                    return "0.0.0";
                }
                LOG.debug("Interface source bundle: {}", b.getSymbolicName());

                ExportedPackage[] ep = pa.getExportedPackages(b);
                LOG.debug("Exported Packages of the source bundle: {}", (Object)ep);

                String pack = iClass.getPackage().getName();
                LOG.debug("Looking for Package: {}", pack);
                if (ep != null) {
                    for (ExportedPackage p : ep) {
                        if (p != null
                            && pack.equals(p.getName())) {
                            LOG.debug("found package -> Version: {}", p.getVersion());
                            return p.getVersion().toString();
                        }
                    }
                }
            } finally {
                if (pa != null) {
                    bc.ungetService(paRef);
                }
            }
        } else {
            LOG.error("Was unable to obtain the package admin service -> can't resolve interface versions");
        }

        LOG.info("Unable to find interface version for interface " + iClass.getName()
                 + ". Falling back to 0.0.0");
        return "0.0.0";
    }

    public static String getUUID(BundleContext bc) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bc.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    public static void overlayProperties(Map<String, Object> serviceProperties,
                                         Map<String, Object> additionalProperties) {
        Map<String, String> keysLowerCase = new HashMap<String, String>();
        for (String key : serviceProperties.keySet()) {
            keysLowerCase.put(key.toLowerCase(), key);
        }

        for (Map.Entry<String, Object> e : additionalProperties.entrySet()) {
            String key = e.getKey();
            String lowerKey = key.toLowerCase();
            if (org.osgi.framework.Constants.SERVICE_ID.toLowerCase().equals(lowerKey)
                || org.osgi.framework.Constants.OBJECTCLASS.toLowerCase().equals(lowerKey)) {
                // objectClass and service.id must not be overwritten
                LOG.info("exportService called with additional properties map that contained illegal key: "
                          + key + ", the key is ignored");
            } else {
                String origKey = keysLowerCase.get(lowerKey);
                if (origKey != null) {
                    LOG.debug("Overwriting property [{}] with value [{}]", origKey, e.getValue());
                } else {
                    origKey = key;
                    keysLowerCase.put(lowerKey, origKey);
                }
                serviceProperties.put(origKey, e.getValue());
            }
        }
    }

    /**
     * Returns a service's properties as a map.
     *
     * @param serviceReference a service reference
     * @return the service's properties as a map
     */
    public static Map<String, Object> getProperties(ServiceReference<?> serviceReference) {
        String[] keys = serviceReference.getPropertyKeys();
        Map<String, Object> props = new HashMap<String, Object>(keys.length);
        for (String key : keys) {
            Object val = serviceReference.getProperty(key);
            props.put(key, val);
        }
        return props;
    }

    public static void checkPermission(EndpointPermission permission) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(permission);
        }
    }
}
