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

import java.util.HashMap;
import java.util.Map;


import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatisfiedException;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.junit.Assert.assertNotNull;;

public class ConfigTypeHandlerFactoryTest {

    @Test
    public void testGetDefaultHandlerNoIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(null, null);
        // assertTrue(handler instanceof PojoConfigurationTypeHandler);
        assertNotNull(handler);
    }

    @Test
    public void testGetJaxrsHandlerNoIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, null);
        // assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
        assertNotNull(handler);
    }

    @Test
    public void testGetJaxrsHandlerHttpIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, "HTTP");
        assertNotNull(handler);
        // assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
    }

    @Test
    public void testJaxrsPropertyIgnored() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, "SOAP HTTP");
        assertNotNull(handler);
        // assertTrue(handler instanceof PojoConfigurationTypeHandler);
        // assertTrue(!(handler instanceof JaxRSPojoConfigurationTypeHandler));
    }

    @Test
    public void testJaxrsPropertyIgnored2() {
        ConfigurationTypeHandler handler = getHandlerWith2(Constants.RS_CONFIG_TYPE, new String[] {"HTTP", "SOAP"});
        assertNotNull(handler);
        // assertTrue(handler instanceof PojoConfigurationTypeHandler);
        // assertTrue(!(handler instanceof JaxRSPojoConfigurationTypeHandler));
    }

    @Test
    public void testGetPojoHandler() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.WS_CONFIG_TYPE, null);
        assertNotNull(handler);
        // assertTrue(handler instanceof PojoConfigurationTypeHandler);
    }

    @Test
    public void testGetWSDLHandler() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.WSDL_CONFIG_TYPE, null);
        assertNotNull(handler);
        // assertTrue(handler instanceof WsdlConfigurationTypeHandler);
    }

    @Test(expected = RuntimeException.class)
    public void testUnsupportedConfiguration() {
        getHandlerWith("notSupportedConfig", null);
    }

    private ConfigurationTypeHandler getHandlerWith(String configType, String intents) {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        try {
            EasyMock.expect(bc.createFilter(EasyMock.anyString()))
                    .andReturn(FrameworkUtil.createFilter("(objectClass=test)"));
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        EasyMock.replay(bc);
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, configType);
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, intents);
        IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(bc);
        EasyMock.reset(bc);
        addSupportedTypes(f, bc);
        return f.getHandler(bc, serviceProps);
    }

    private ConfigurationTypeHandler getHandlerWith2(String configType, String[] intents) {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        try {
            EasyMock.expect(bc.createFilter(EasyMock.anyString()))
                    .andReturn(FrameworkUtil.createFilter("(objectClass=test)"));
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        EasyMock.replay(bc);
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, configType);
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, intents);
        IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(bc);
        addSupportedTypes(f, bc);
        return f.getHandler(bc, serviceProps);
    }

    private void addSupportedTypes(ConfigTypeHandlerFactory factory, BundleContext contextMock) {
        EasyMock.reset(contextMock);
        ServiceReference mock = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(contextMock.getService(EasyMock.anyObject(ServiceReference.class)))
                .andReturn(createConfigType(Constants.RS_CONFIG_TYPE));
        EasyMock.replay(contextMock);
        factory.handlerTracker.addingService(mock);

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getService(EasyMock.anyObject(ServiceReference.class)))
                .andReturn(createConfigType(Constants.WS_CONFIG_TYPE));
        EasyMock.replay(contextMock);
        factory.handlerTracker.addingService(mock);

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getService(EasyMock.anyObject(ServiceReference.class)))
                .andReturn(createConfigType(Constants.WSDL_CONFIG_TYPE));
        EasyMock.replay(contextMock);
        factory.handlerTracker.addingService(mock);
    }

    private ConfigurationTypeHandler createConfigType(final String... types) {
        return new ConfigurationTypeHandler() {

            @Override
            public String[] getSupportedTypes() {
                return types;
            }

            @Override
            public ExportResult createServer(ServiceReference serviceReference, BundleContext dswContext,
                    BundleContext callingContext, Map<String, Object> sd, Class<?> iClass, Object serviceBean) {
                return null;
            }

            @Override
            public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                    BundleContext callingContext, Class<?> iClass, EndpointDescription endpoint)
                            throws IntentUnsatisfiedException {
                return null;
            }
        };
    }
}
