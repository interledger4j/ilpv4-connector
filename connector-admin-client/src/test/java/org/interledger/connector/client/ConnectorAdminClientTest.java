package org.interledger.connector.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import okhttp3.HttpUrl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link ConnectorAdminClient}.
 *
 * Note that this client is used for various ITs in the Connector project, so actual verification of client
 * functionality is mostly performed there.
 */
public class ConnectorAdminClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testConstructWithNullUrl() {
    expectedException.expect(NullPointerException.class);
    ConnectorAdminClient.construct(null, template -> {
    });
  }

  @Test
  public void testConstructWithNullAuthInterceptor() {
    expectedException.expect(NullPointerException.class);
    ConnectorAdminClient.construct(HttpUrl.parse("https://example.com"), null);
  }

  @Test
  public void testConstruct() {
    ConnectorAdminClient.construct(HttpUrl.parse("https://example.com"), new RequestInterceptor() {
      @Override
      public void apply(final RequestTemplate template) {
        return;
      }
    });
  }

}
