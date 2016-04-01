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

import java.net.Inet4Address;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastbinInstantiator extends RemoteServiceContainerInstantiator
{
	public static final String DEFAULT_SERVER_ID = "tcp://localhost:9000";
		
    private static final Logger LOG = LoggerFactory.getLogger(FastbinInstantiator.class);

	public FastbinInstantiator(String serverProvider, String clientProvider) {
		super(Collections.singletonList(serverProvider),asMap(serverProvider,clientProvider));
	}

	private static Map<String, List<String>> asMap(String serverProvider, String clientProvider)
    {
	    Map<String, List<String>> map = new HashMap<>();
	    map.put(serverProvider, Collections.singletonList(clientProvider));
        return map;
    }

    @Override
    public IContainer createInstance(ContainerTypeDescription description, Map<String, ?> parameters)
            throws ContainerCreateException {
        return description.isServer()?
                new FastbinServerContainer(getServerIDFromParameters(parameters)):
                    new FastbinClientContainer(FastbinNamespace.INSTANCE.createInstance(new Object[] { URI.create("uuid:" + java.util.UUID.randomUUID().toString()) }));
    }

    private ID getServerIDFromParameters(Map<String, ?> parameters)
    {
    	String idString = DEFAULT_SERVER_ID;
    	if (parameters != null) {
            String portValue = parameters.get(FastbinNamespace.PORT) != null ? parameters.get(FastbinNamespace.PORT).toString() : System.getProperty(FastbinNamespace.PORT, "9000");
            int port = Integer.parseInt(portValue);
            String publicHost = (String)parameters.get(FastbinNamespace.SERVER_ADDRESS);
            if (publicHost == null)
            {
                try
                {
                    publicHost = Inet4Address.getLocalHost().getCanonicalHostName();
                    LOG.info("public server address (fastbin.address) not set. Using {} as default", publicHost);
                }
                catch (UnknownHostException e)
                {
                    publicHost = "localhost";
                    LOG.warn("Failed to resolve canoncial hostname. Reverting to localhost as server address. Try setting the "+FastbinNamespace.SERVER_ADDRESS+" property to explicitly set the public address of the server.",e);
                }
            }
    		idString = "tcp://"+publicHost+":"+port;
    	}
        return FastbinNamespace.INSTANCE.createInstance(new Object[] { idString });
    }
}



