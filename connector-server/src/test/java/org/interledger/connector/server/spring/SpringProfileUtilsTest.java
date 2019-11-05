package org.interledger.connector.server.spring;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

public class SpringProfileUtilsTest {

  @Mock
  private Environment environment;

  @Before
  public void setUp() {
    environment = mock(Environment.class);
  }

  @Test
  public void activeProfilesEmpty() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    assertThat(SpringProfileUtils.isProfileActive(environment, "foo")).isFalse();
    assertThat(SpringProfileUtils.isProfileActive(environment, "")).isFalse();
  }

  @Test
  public void activeProfiles() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"Foo", "BAR", "baz"});
    assertThat(SpringProfileUtils.isProfileActive(environment, "Foo")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "foo")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "FOO")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "BAR")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "Bar")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "bar")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "baz")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "Baz")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "BAZ")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, "foobar")).isFalse();
  }

  @Test
  public void activeProfilesTrimmed() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"foo"});
    assertThat(SpringProfileUtils.isProfileActive(environment, "foo ")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, " foo")).isTrue();
    assertThat(SpringProfileUtils.isProfileActive(environment, " foo ")).isTrue();
  }


}
