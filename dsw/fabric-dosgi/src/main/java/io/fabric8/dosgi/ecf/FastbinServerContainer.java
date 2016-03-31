/**
 *  Copyright (c) 2016 Composent, Inc. and others
 *
 *  Composent, Inc. licenses this file to you under the Apache License, version
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
package io.fabric8.dosgi.ecf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;
import org.fusesource.hawtdispatch.Dispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.dosgi.api.SerializationStrategy;
import io.fabric8.dosgi.io.ServerInvoker;
import io.fabric8.dosgi.tcp.ServerInvokerImpl;

public class FastbinServerContainer extends AbstractRSAContainer
{
    private ServerInvoker invoker;
    private static final Logger LOG = LoggerFactory.getLogger(FastbinServerContainer.class);

    public FastbinServerContainer(ID id) {
    	super(id);
        try
        {
            invoker = new ServerInvokerImpl(getID().getName(), Dispatch.createQueue(), new ConcurrentHashMap<String,SerializationStrategy>());
            invoker.start();

        }
        catch (Exception e)
        {
            LOG.error("Failed to start server invoker",e);
        }
    }

    @Override
    public void dispose()
    {
        invoker.stop();
        invoker = null;
        super.dispose();
    }


    @Override
    protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration)
    {
        invoker.registerService(String.valueOf(registration.getID().getContainerRelativeID()), new ServerInvoker.ServiceFactory() {
            public Object get() {
                return registration.getService();
            }
            public void unget() {

            }
        }, registration.getService().getClass().getClassLoader());
        // Return any extra properties needed for this endpoint.
        Map<String,Object> results = new HashMap<String,Object>();
        results.put(FastbinNamespace.PROTOCOL_VERSION_PROPERTY, String.valueOf(FastbinNamespace.PROTOCOL_VERSION));

        String fastbinAddress = invoker.getConnectAddress();
        results.put(FastbinNamespace.SERVER_ADDRESS, fastbinAddress);
       // results.put("ecf.endpoint.id", "tcp://foobar.com:9999/fu/bar");
        return results;
    }

    @Override
    protected void unexportRemoteService(RSARemoteServiceRegistration registration)
    {
        invoker.unregisterService(String.valueOf(registration.getID().getContainerRelativeID()));

    }

}



