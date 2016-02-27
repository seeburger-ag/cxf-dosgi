package io.fabric8.dosgi.api;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.ExportResult;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.dosgi.io.ClientInvoker;
import io.fabric8.dosgi.io.ServerInvoker;
import io.fabric8.dosgi.io.ServerInvoker.ServiceFactory;
import io.fabric8.dosgi.tcp.ClientInvokerImpl;
import io.fabric8.dosgi.tcp.ServerInvokerImpl;

@Component(enabled=true)
@Service
public class FastbinConfigurationTypeHandler implements ConfigurationTypeHandler {

    /**
     * the name of the configuration type (service.exported.configs)
     */
    public static final String CONFIG_NAME = "fastbin";
    /**
     * the server address to connect to
     */
    public static final String SERVER_ADDRESS = "fastbin.address";
    /**
     * the endpoint address of the exported service. If left empty a generated endpoint id will be used
     */
    public static final String ENDPOINT_ADDRESS = "fastbin.endpoint.address";

    private static final Logger LOG = LoggerFactory.getLogger(FastbinConfigurationTypeHandler.class);


    private ServerInvoker server;
    private ClientInvoker client;
    private DispatchQueue queue;
    private ConcurrentHashMap<String, SerializationStrategy> serializationStrategies;
    private BundleContext bundleContext;

    @Activate
    public void activate(Map<String, Object> config) {
        this.bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.queue = Dispatch.createQueue();
        this.serializationStrategies = new ConcurrentHashMap<String, SerializationStrategy>();
        int port = Integer.parseInt(config.getOrDefault("fastbin", System.getProperty("fastbin.port","9000")).toString());
        //TODO: bind host and public hostname from config

        try {
            String host = Inet4Address.getLocalHost().getCanonicalHostName();
            server = new ServerInvokerImpl("tcp://"+host+":"+port, queue, serializationStrategies);
            server.start();
            client = new ClientInvokerImpl(queue, serializationStrategies);
            client.start();
        } catch (Exception e) {
            LOG.error("Failed to start the tcp server",e);
        }
    }

    @Deactivate
    public void deactivate(Map<String, ?> config) {
        server.stop();
        client.stop();
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { CONFIG_NAME };
    }

    @Override
    public ExportResult createServer(ServiceReference serviceReference, BundleContext dswContext,
            BundleContext callingContext, Map<String, Object> sd, Class<?> iClass, final Object serviceBean) {
            Map<String, Object> endpointProps = createEndpointProps(sd, iClass, getSupportedTypes(), server.getConnectAddress(), new String[0]); //TODO: handle intents properly
            final String callID = (String) endpointProps.get(RemoteConstants.ENDPOINT_ID);
            final ServiceFactory factory = new ServiceFactory() {

                @Override
                public void unget() {
                    server.unregisterService(callID);
                }

                @Override
                public Object get() {
                    return serviceBean;
                }
            };

            server.registerService(callID, factory, serviceBean.getClass().getClassLoader());
            Closeable closeable = new Closeable() {

                @Override
                public void close() throws IOException {
                    factory.unget();
                }
            };
            return new ExportResult(endpointProps, closeable);
    }

    @Override
    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext, BundleContext callingContext,
            Class<?> iClass, EndpointDescription endpoint) throws IntentUnsatisfiedException {
        String callID = (String) endpoint.getProperties().get(RemoteConstants.ENDPOINT_ID);
        InvocationHandler invocationHandler = client.getProxy((String) endpoint.getProperties().get(SERVER_ADDRESS), callID, iClass.getClassLoader());
        return Proxy.newProxyInstance(iClass.getClassLoader(), new Class[] {iClass},invocationHandler);
    }


    protected Map<String, Object> createEndpointProps(Map<String, Object> sd, Class<?> iClass,
                                                      String[] importedConfigs, String address, String[] intents) {
        Map<String, Object> props = new HashMap<String, Object>();

        copyEndpointProperties(sd, props);

        String[] sa = new String[] {
            iClass.getName()
        };
        String pkg = iClass.getPackage().getName();

        props.remove(org.osgi.framework.Constants.SERVICE_ID);
        props.put(org.osgi.framework.Constants.OBJECTCLASS, sa);
        props.put(RemoteConstants.ENDPOINT_SERVICE_ID, sd.get(org.osgi.framework.Constants.SERVICE_ID));
        props.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, OsgiUtils.getUUID(bundleContext));
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        props.put(RemoteConstants.ENDPOINT_PACKAGE_VERSION_ + pkg, OsgiUtils.getVersion(iClass, bundleContext));


        String[] allIntents = IntentUtils.mergeArrays(intents, IntentUtils.getIntentsImplementedByTheService(sd));
        props.put(RemoteConstants.SERVICE_INTENTS, allIntents);
        String fabricAddress = server.getConnectAddress();
        props.put(SERVER_ADDRESS, fabricAddress);
        String endpointID = sd.getOrDefault(ENDPOINT_ADDRESS, UUID.randomUUID()).toString();
        props.put(RemoteConstants.ENDPOINT_ID, endpointID);
        return props;
    }

    private void copyEndpointProperties(Map<String, Object> sd, Map<String, Object> endpointProps) {
        Set<Map.Entry<String, Object>> keys = sd.entrySet();
        for (Map.Entry<String, Object> entry : keys) {
            try {
                String skey = entry.getKey();
                if (!skey.startsWith(".")) {
                    endpointProps.put(skey, entry.getValue());
                }
            } catch (ClassCastException e) {
                LOG.warn("ServiceProperties Map contained non String key. Skipped " + entry + "   "
                         + e.getLocalizedMessage());
            }
        }
    }

}

