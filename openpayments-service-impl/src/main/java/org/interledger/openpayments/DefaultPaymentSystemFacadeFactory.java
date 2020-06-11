package org.interledger.openpayments;

import org.interledger.connector.payments.StreamPayment;
import org.interledger.openpayments.xrpl.XrplTransaction;

import org.interleger.openpayments.PaymentSystemFacade;
import org.interleger.openpayments.PaymentSystemFacadeFactory;

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
  public Optional<PaymentSystemFacade> get(Class responseType, Class requestType) {
    PaymentSystemFacade facade = instances.get(new Key(responseType, requestType));
    return Optional.ofNullable(facade);
  }

  @Override
  public Optional<PaymentSystemFacade> get(PaymentNetwork paymentNetwork) {
    switch (paymentNetwork) {
      case INTERLEDGER: return get(StreamPayment.class, IlpPaymentDetails.class);
      case XRPL: return get(XrplTransaction.class, XrpPaymentDetails.class);
      default: return Optional.empty();
    }
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
