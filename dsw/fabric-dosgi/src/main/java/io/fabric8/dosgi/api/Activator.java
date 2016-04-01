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
package io.fabric8.dosgi.api;

import org.eclipse.ecf.core.provider.IContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.fabric8.dosgi.ecf.FastbinInstantiator;
import io.fabric8.dosgi.ecf.FastbinNamespace;

public class Activator implements BundleActivator {

	private static BundleContext context;

    public static final String CLIENT_PROVIDER_NAME = "ecf.hawt.client";
    public static final String SERVER_PROVIDER_NAME = "ecf.hawt.server";

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		// Create and register the Namespace
		context.registerService(org.eclipse.ecf.core.identity.Namespace.class.getName(), new FastbinNamespace(),  null);
        IContainerInstantiator instantiator = new FastbinInstantiator(SERVER_PROVIDER_NAME, CLIENT_PROVIDER_NAME);
        RemoteServiceDistributionProvider.Builder serverBuilder = new RemoteServiceDistributionProvider.Builder().setName(SERVER_PROVIDER_NAME).setServer(true).setHidden(false).setInstantiator(instantiator).setDescription("Fabric Hawt.io Server Provider");

        context.registerService(IRemoteServiceDistributionProvider.class.getName(), serverBuilder.build(), null);

        RemoteServiceDistributionProvider.Builder clientBuilder = new RemoteServiceDistributionProvider.Builder().setName(CLIENT_PROVIDER_NAME).setServer(false).setHidden(false).setInstantiator(instantiator).setDescription("Fabric Hawt.io Client Provider");
		context.registerService(IRemoteServiceDistributionProvider.class.getName(), clientBuilder.build(), null);

	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}