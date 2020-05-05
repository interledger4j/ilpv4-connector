package org.interledger.connector.settings.properties;

import org.springframework.http.MediaType;

import java.nio.charset.Charset;
import java.util.Map;

public class OpenPaymentsMediaType extends MediaType {

  public static final OpenPaymentsMediaType APPLICATION_INVOICE_JSON;
  public static final String APPLICATION_INVOICE_JSON_VALUE = "application/invoice+json";

  public static final OpenPaymentsMediaType APPLICATION_MANDATE_JSON;
  public static final String APPLICATION_MANDATE_JSON_VALUE = "application/mandate+json";

  public static final OpenPaymentsMediaType APPLICATION_CONNECTION_JSON;
  public static final String APPLICATION_CONNECTION_JSON_VALUE = "application/connection+json";

  static {
    APPLICATION_INVOICE_JSON = new OpenPaymentsMediaType("application", "invoice+json");
    APPLICATION_MANDATE_JSON = new OpenPaymentsMediaType("application", "mandate+json");
    APPLICATION_CONNECTION_JSON = new OpenPaymentsMediaType("application", "connection+json");
  }

  public OpenPaymentsMediaType(String type) {
    super(type);
  }

  public OpenPaymentsMediaType(String type, String subtype) {
    super(type, subtype);
  }

  public OpenPaymentsMediaType(String type, String subtype, Charset charset) {
    super(type, subtype, charset);
  }

  public OpenPaymentsMediaType(String type, String subtype, double qualityValue) {
    super(type, subtype, qualityValue);
  }

  public OpenPaymentsMediaType(MediaType other, Charset charset) {
    super(other, charset);
  }

  public OpenPaymentsMediaType(MediaType other, Map<String, String> parameters) {
    super(other, parameters);
  }

  public OpenPaymentsMediaType(String type, String subtype, Map<String, String> parameters) {
    super(type, subtype, parameters);
  }
}
