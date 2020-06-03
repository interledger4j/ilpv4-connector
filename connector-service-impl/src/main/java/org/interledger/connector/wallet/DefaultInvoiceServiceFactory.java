package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.InvoiceServiceFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class DefaultInvoiceServiceFactory implements InvoiceServiceFactory {

  private final HashMap<Key, InvoiceService> instances = new HashMap();

  @Override
  public <T, V> void register(InvoiceService<T, V> service) {
    Type[] genericTypes = getParameterizedTypes(service.getClass());
    instances.put(new Key(genericTypes[0].getClass(), genericTypes[1].getClass()), service);
  }

  @Override
  public <T, V> Optional<InvoiceService<T, V>> get(Class<T> responseType, Class<V> requestType) {
    InvoiceService<T, V> service = instances.get(new Key(responseType, requestType));
    return Optional.ofNullable(service);
  }

  private Type[] getParameterizedTypes(Class clazz) {
    return ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
  }

  private static class Key {
    private final Class response;
    private final Class request;

    private Key(Class response, Class request) {
      this.request = request;
      this.response = response;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key key = (Key) o;
      return Objects.equals(request, key.request) &&
        Objects.equals(response, key.response);
    }

    @Override
    public int hashCode() {
      return Objects.hash(request, response);
    }
  }
}
