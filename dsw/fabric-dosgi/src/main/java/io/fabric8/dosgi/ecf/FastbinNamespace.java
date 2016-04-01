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

import org.eclipse.ecf.core.identity.URIID;

public class FastbinNamespace extends URIID.URIIDNamespace
{
    /** field <code>serialVersionUID</code> */
    private static final long serialVersionUID = -7390682488271441584L;
    public static final String NAME = "ecf.namespace.fastbin";
    public static FastbinNamespace INSTANCE;

    /**
     * the name of the configuration type (service.exported.configs)
     */
    public static final String CONFIG_NAME = "ecf.hawt.server";
    /**
     * the endpoint address of the exported service. If left empty a generated endpoint id will be used
     */
    public static final String ENDPOINT_ADDRESS = "fastbin.endpoint.address";
    /**
     * the server address to connect to
     */
    public static final String SERVER_ADDRESS = "fastbin.address";
    /**
     * the port to bind the server socket to. Defaults to 9000
     */
    public static final String PORT = "fastbin.port";

    public static final int PROTOCOL_VERSION = 1;
    public static final String PROTOCOL_VERSION_PROPERTY = "fastbin.protocol.version";


    public FastbinNamespace() {
        super(NAME, "Fastbin Namespace");
        INSTANCE = this;
    }

    @Override
    public String getScheme() {
        return "ecf.fastbin";
    }
}



