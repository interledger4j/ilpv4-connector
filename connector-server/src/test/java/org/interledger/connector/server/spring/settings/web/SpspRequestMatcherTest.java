package org.interledger.connector.server.spring.settings.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class SpspRequestMatcherTest {

  private RequestMatcher spspRequestMatcher;

  private MockHttpServletRequest servletRequest;

  @Before
  public void setUp() {
    servletRequest = new MockHttpServletRequest();
  }

  @Test
  public void testWhenDisabled() {
    this.spspRequestMatcher = new SpspRequestMatcher(false, "/p");

    // Invalid Accept Header
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "text/plain");
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();

    // Valid Accept Header
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "application/spsp4+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();

    // Valid Path
    servletRequest.setServletPath("/p/bob");
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();

    // Invalid Path
    servletRequest.setServletPath("/q/bob"); // invalid path
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();
  }

  @Test
  public void testWhenEnabled() {
    this.spspRequestMatcher = new SpspRequestMatcher(true, "/p");

    // Invalid Accept Header
    servletRequest.setRequestURI("/p/bob");
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "text/plain");
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();

    // Valid Accept Header
    servletRequest.setRequestURI("/p/bob"); // valid path
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "application/spsp4+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isTrue();

    // Multiple Accept Headers
    servletRequest.setRequestURI("/p/bob"); // valid path
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "text/html, application/spsp4+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isTrue();

    // Multiple Accept Headers
    servletRequest.setRequestURI("/p/bob"); // valid path
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "application/spsp4+json,application/spsp+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isTrue();

    // Valid Path
    servletRequest.setRequestURI("/p/bob"); // valid path
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "application/spsp4+json,application/spsp+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isTrue();

    // Invalid Path
    servletRequest.setRequestURI("/q/bob"); // valid path
    servletRequest.removeHeader("Accept");
    servletRequest.addHeader("Accept", "application/spsp4+json,application/spsp+json");
    assertThat(spspRequestMatcher.matches(servletRequest)).isFalse();
  }

}
