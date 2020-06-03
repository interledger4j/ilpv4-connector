package org.interledger.connector.opa;

import java.util.Collection;
import java.util.Optional;

public interface InvoiceServiceFactory {

  <T, V> void register(InvoiceService<T, V> service);

  default InvoiceServiceFactory register(Collection<InvoiceService> services) {
    services.forEach(this::register);
    return this;
  }

  <T, V> Optional<InvoiceService<T, V>> get(Class<T> responseType, Class<V> requestType);

}
