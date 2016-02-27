/**
 *  Copyright 2016 SEEBURGER AG
 *
 *  SEEBURGER licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.apache.cxf.dosgi.distribution;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.dosgi.distribution.handler.HttpServiceManager;
import org.apache.cxf.dosgi.distribution.handler.JaxRSPojoConfigurationTypeHandler;
import org.apache.cxf.dosgi.distribution.handler.PojoConfigurationTypeHandler;
import org.apache.cxf.dosgi.distribution.handler.WsdlConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, ManagedService {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private HttpServiceManager httpServiceManager;
    private BundleContext bc;
    private List<ServiceRegistration<ConfigurationTypeHandler>> handlers;
    private IntentManager intentManager;
    private Map<String, Object> curConfiguration;
    private Bus bus;
    private static final String CONFIG_SERVICE_PID = "cxf-dsw";

    @Override
    public void start(BundleContext context) throws Exception {
        this.bc = context;
        curConfiguration = getDefaultConfig();
        init(curConfiguration);
        // Disable the fast infoset as it's not compatible (yet) with OSGi
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        registerManagedService(bc);

    }

    private Map<String, Object> getDefaultConfig() {
        return new HashMap<String, Object>();
    }

    private void createHandlers() {
        ConfigurationTypeHandler service = new PojoConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        handlers.add(bc.registerService(ConfigurationTypeHandler.class, service, null));
        service = new JaxRSPojoConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        handlers.add(bc.registerService(ConfigurationTypeHandler.class, service, null));
        service = new WsdlConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        handlers.add(bc.registerService(ConfigurationTypeHandler.class, service, null));
    }

    private synchronized void init(Map<String, Object> config) {
        bus = BusFactory.newInstance().createBus();

        String httpBase = (String) config.get(org.apache.cxf.dosgi.dsw.Constants.HTTP_BASE);
        String cxfServletAlias = (String) config.get(org.apache.cxf.dosgi.dsw.Constants.CXF_SERVLET_ALIAS);

        httpServiceManager = new HttpServiceManager(bc, httpBase, cxfServletAlias);
        this.handlers = new ArrayList<ServiceRegistration<ConfigurationTypeHandler>>();
        createHandlers();
    }

    private synchronized void uninit() {
        if (httpServiceManager != null) {
            httpServiceManager.close();
            httpServiceManager = null;
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.debug("CXF default distribution providers are shutting down now");
        uninit();
        shutdownCXFBus();
        for (ServiceRegistration<ConfigurationTypeHandler> serviceRegistration : handlers) {
            serviceRegistration.unregister();
        }
    }

    private void registerManagedService(BundleContext bundlecontext) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, CONFIG_SERVICE_PID);
        // No need to store the registration. Will be unregistered in stop by framework
        bundlecontext.registerService(ManagedService.class.getName(), this, props);
    }


    /**
     * Causes also the shutdown of the embedded HTTP server
     */
    private void shutdownCXFBus() {
        if (bus != null) {
            LOG.debug("Shutting down the CXF Bus");
            bus.shutdown(true);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void updated(Dictionary config) throws ConfigurationException {
        LOG.debug("RemoteServiceAdmin Implementation configuration is updated with {}", config);
        // config is null if it doesn't exist, is being deleted or has not yet been loaded
        // in which case we run with defaults (just like we do manually when bundle is first started)
        Map<String, Object> configMap = config == null ? getDefaultConfig() : Utils.toMap(config);
        if (!configMap.equals(config)) { // only if something actually changed
            curConfiguration = configMap;
            uninit();
            init(configMap);
        }
    }
}
