package org.interleger.openpayments;

import java.util.Collection;
import java.util.Optional;

public interface PaymentSystemFacadeFactory {

  <T, V> void register(PaymentSystemFacade<T, V> facade);

  default PaymentSystemFacadeFactory register(Collection<PaymentSystemFacade> facades) {
    facades.forEach(this::register);
    return this;
  }

  <T, V> Optional<PaymentSystemFacade<T, V>> get(Class<T> responseType, Class<V> requestType);

}
