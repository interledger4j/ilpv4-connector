package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.PaymentSystemFacadeFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class DefaultPaymentSystemFacadeFactory implements PaymentSystemFacadeFactory {

  private final HashMap<Key, PaymentSystemFacade> instances = new HashMap();

  @Override
  public <T, V> void register(PaymentSystemFacade<T, V> facade) {
    instances.put(new Key(facade.getResultType(), facade.getDetailsType()), facade);
  }

  @Override
  public <T, V> Optional<PaymentSystemFacade<T, V>> get(Class<T> responseType, Class<V> requestType) {
    PaymentSystemFacade<T, V> facade = instances.get(new Key(responseType, requestType));
    return Optional.ofNullable(facade);
  }

  private static class Key {
    private final Type request;
    private final Type response;

    private Key(Type response, Type request) {
      this.response = response;
      this.request = request;
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
