/*
 * ExtraInvocationTest.java
 *
 * created at 28.02.2016 by utzig <j.utzig@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package io.fabric8.dosgi;


import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceException;

import io.fabric8.dosgi.InvocationTest.HelloImpl;
import io.fabric8.dosgi.api.SerializationStrategy;
import io.fabric8.dosgi.io.ServerInvoker;
import io.fabric8.dosgi.tcp.ClientInvokerImpl;
import io.fabric8.dosgi.tcp.ServerInvokerImpl;


public class ExtraInvocationTest
{

    private ServerInvokerImpl server;
    private ClientInvokerImpl client;
    private TestService testService;


    @Before
    public void setup() throws Exception
    {
        DispatchQueue queue = Dispatch.createQueue();
        HashMap<String, SerializationStrategy> map = new HashMap<String, SerializationStrategy>();
        server = new ServerInvokerImpl("tcp://localhost:0", queue, map);
        server.start();

        client = new ClientInvokerImpl(queue, map);
        client.start();
        server.registerService("service-id", new ServerInvoker.ServiceFactory()
        {
            public Object get()
            {
                return new TestServiceImpl();
            }


            public void unget()
            {}
        }, TestServiceImpl.class.getClassLoader());

        InvocationHandler handler = client.getProxy(server.getConnectAddress(), "service-id", TestServiceImpl.class.getClassLoader());
        testService = (TestService)Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[]{TestService.class}, handler);
    }


    @After
    public void tearDown()
    {
        server.stop();
        client.stop();
    }


    @Test
    public void testInvokeWithComplexObject() throws Exception
    {

        ComplexObject complexObject = testService.create("test", 3);
        // tests that a complex (serializable object can be transfered correctly)
        assertEquals("test/test/test/test", complexObject.toString());

        // tests that the other way around works as well
        assertEquals("test2/test2/test2", testService.print(new ComplexObject("test2", 2)));

    }

    @Test
    public void testInvokeWithCheckedException() throws Exception
    {

        try
        {
            testService.throwException("Message");
            fail("Must throw an IOException");
        }
        catch (IOException e)
        {
            assertEquals("Message", e.getMessage());
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvokeWithComplexObjectAndRegularExcpetion() throws Exception
    {
        testService.create("test", -1);
    }

    @Test(expected=ServiceException.class)
    public void testInvokeWithComplexObjectWithConnectionRefused() throws Exception
    {
        // must throw a service exception
        server.stop();
        testService.create("test", 1);
    }

    public static class ComplexObject implements Serializable
    {
        /** field <code>serialVersionUID</code> */
        private static final long serialVersionUID = -8914690966403397046L;
        String name;
        ComplexObject child;


        public ComplexObject(String name, int level)
        {
            this.name = name;
            if(level<0)
                throw new IllegalArgumentException("Level < 0 is not allowed");
            if (level > 0)
                child = new ComplexObject(name, level - 1);

        }


        @Override
        public String toString()
        {
            StringBuilder result = new StringBuilder(name);
            if (child != null)
            {
                result.append("/");
                result.append(child);
            }
            return result.toString();
        }
    }

    public interface TestService
    {
        String print(ComplexObject object);


        ComplexObject create(String name, int level);

        void throwException(String message) throws IOException;
    }

    public class TestServiceImpl implements TestService
    {

        @Override
        public String print(ComplexObject object)
        {
            return object.toString();
        }


        @Override
        public ComplexObject create(String name, int level)
        {
            return new ComplexObject(name, level);
        }


        @Override
        public void throwException(String message) throws IOException
        {
            throw new IOException(message);
        }

    }
}
