package org.interledger.openpayments;

import org.springframework.http.MediaType;

/**
 * A subclass of {@link MediaType} which provides MimeType values as defined in the Open Payments specification.
 */
public class OpenPaymentsMediaType extends MediaType {

  /**
   * Should be sent as the HTTP Accept header value for connection details requests.
   */
  public static final OpenPaymentsMediaType APPLICATION_CONNECTION_JSON;
  public static final String APPLICATION_CONNECTION_JSON_VALUE = "application/connection+json";

  static {
    APPLICATION_CONNECTION_JSON = new OpenPaymentsMediaType("application", "connection+json");
  }

  public OpenPaymentsMediaType(String type, String subtype) {
    super(type, subtype);
  }
}
