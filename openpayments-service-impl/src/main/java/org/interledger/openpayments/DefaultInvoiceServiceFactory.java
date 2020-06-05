package org.interledger.openpayments;

import org.interledger.connector.payments.StreamPayment;
import org.interledger.openpayments.xrpl.XrplTransaction;

import org.interleger.openpayments.InvoiceService;
import org.interleger.openpayments.InvoiceServiceFactory;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class DefaultInvoiceServiceFactory implements InvoiceServiceFactory {

  private final HashMap<Key, InvoiceService> instances = new HashMap();

  @Override
  public <T extends AuthorizablePayment, V> void register(InvoiceService<T, V> service) {
    instances.put(new Key(service.getResultType(), service.getRequestType()), service);
  }

  @Override
  public Optional<InvoiceService> get(Class responseType, Class requestType) {
    InvoiceService service = instances.get(new Key(responseType, requestType));
    return Optional.ofNullable(service);
  }

  @Override
  public Optional<InvoiceService> get(PaymentNetwork paymentNetwork) {
    switch (paymentNetwork) {
      case INTERLEDGER: return get(StreamPayment.class, IlpPaymentDetails.class);
      case XRPL: return get(XrplTransaction.class, XrpPaymentDetails.class);
      default: return Optional.empty();
    }
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
