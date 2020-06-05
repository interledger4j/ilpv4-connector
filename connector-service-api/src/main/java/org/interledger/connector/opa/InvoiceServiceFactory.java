package org.interledger.connector.opa;

import org.interledger.connector.opa.model.AuthorizablePayment;
import org.interledger.connector.opa.model.PaymentNetwork;

import java.util.Collection;
import java.util.Optional;

public interface InvoiceServiceFactory {

  <T extends AuthorizablePayment, V> void register(InvoiceService<T, V> service);

  default InvoiceServiceFactory register(Collection<InvoiceService> services) {
    services.forEach(this::register);
    return this;
  }

  <T extends  AuthorizablePayment, V> Optional<InvoiceService<T, V>> get(Class<T> responseType, Class<V> requestType);

  Optional<InvoiceService> get(PaymentNetwork paymentNetwork);


}
