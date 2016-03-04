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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.IRemoteServiceID;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.fusesource.hawtdispatch.Dispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.dosgi.api.SerializationStrategy;
import io.fabric8.dosgi.io.ClientInvoker;
import io.fabric8.dosgi.tcp.ClientInvokerImpl;

public class FastbinClientContainer extends AbstractRSAClientContainer
{

    private ClientInvoker invoker;
    private static final Logger LOG = LoggerFactory.getLogger(FastbinClientContainer.class);

    public FastbinClientContainer(ID id)
    {
        super(id);
        invoker = new ClientInvokerImpl(Dispatch.createQueue(), new ConcurrentHashMap<String,SerializationStrategy>());
        try
        {
            invoker.start();
        }
        catch (Exception e)
        {
            LOG.error("Failed to start client invoker",e);
        }
    }

    @Override
    protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration)
    {
        return new AbstractRSAClientService(this, registration) {
            @Override
            protected Object invokeSync(RSARemoteCall call) throws ECFException
            {
        		Method method = call.getReflectMethod();
        		IRemoteServiceID rsId = getRegistration().getID();
        		int protocolVersion = FastbinNamespace.PROTOCOL_VERSION;
        		if(getRegistration().getProperty(FastbinNamespace.PROTOCOL_VERSION_PROPERTY)!=null) {
        		    protocolVersion = Integer.parseInt(getRegistration().getProperty(FastbinNamespace.PROTOCOL_VERSION_PROPERTY).toString());
        		}
                // use the highest version that is available on both server and client.
                protocolVersion = Math.min(protocolVersion, FastbinNamespace.PROTOCOL_VERSION);
        		InvocationHandler realHandler = invoker.getProxy(rsId.getContainerID().getName(), String.valueOf(rsId.getContainerRelativeID()), method.getDeclaringClass().getClassLoader(), protocolVersion);
        		try {
        			return realHandler.invoke(null, method, call.getParameters());
        		} catch (Throwable e) {
        			throw new ECFException(e.getMessage(), e);
        		}
            }

			@Override
			protected Object invokeAsync(RSARemoteCall remoteCall) throws ECFException {
				return null;
			}
        };
    }

    @Override
    public void dispose()
    {
        invoker.stop();
        super.dispose();
    }
}



