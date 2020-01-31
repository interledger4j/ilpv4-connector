package org.interledger.connector.pubsub;

import java.lang.reflect.Proxy;

public class CoordinationProxyGeneratorImpl implements CoordinationProxyGenerator {

  public Object createCoordinatedProxy(Object instance) {
    if (instance.getClass().getInterfaces().length == 0) {
      throw new IllegalArgumentException("Cannot proxy instance with zero interfaces");
    }

    Class<?>[] interfaces = instance.getClass().getInterfaces();

    Class<?>[] proxyInterfaces = new Class<?>[interfaces.length + 1];
    System.arraycopy(interfaces, 0, proxyInterfaces, 0, interfaces.length);
    proxyInterfaces[interfaces.length] = Coordinated.class;

    Object proxiedMessage = Proxy.newProxyInstance(
      CoordinationProxyGeneratorImpl.class.getClassLoader(),
      proxyInterfaces,
      (proxy, method, methodArgs) -> method.invoke(instance, methodArgs)
    );
    return proxiedMessage;
  }
}
