package com.sappenin.interledger.ilpv4.connector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuntimeUtils}.
 */
public class RuntimeUtilsTest {

  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";

  @Mock
  Environment environmentMock;

  /**
   * Helper method to simulate environment changes, which is otherwise not possible without reflection.
   */
  @SuppressWarnings({"unchecked"})
  private static void updateEnv(String name, String val) throws ReflectiveOperationException {
    Map<String, String> env = System.getenv();
    Field field = env.getClass().getDeclaredField("m");
    field.setAccessible(true);
    ((Map<String, String>) field.get(env)).put(name, val);
  }

  @Before
  public void setUp() throws ReflectiveOperationException {
    MockitoAnnotations.initMocks(this);
    updateEnv(GOOGLE_CLOUD_PROJECT, "");
    System.setProperty(GOOGLE_CLOUD_PROJECT, "");
  }

  @Test
  public void testGcpProfileEnabled() {
    when(environmentMock.getActiveProfiles()).thenReturn(new String[]{"foo", "bar"});
    assertThat(RuntimeUtils.gcpProfileEnabled(environmentMock), is(false));
    when(environmentMock.getActiveProfiles()).thenReturn(new String[]{RuntimeProperties.Runtimes.GCP});
    assertThat(RuntimeUtils.gcpProfileEnabled(environmentMock), is(true));
  }

  @Test
  public void testRunningInGcp() throws ReflectiveOperationException {
    assertThat(RuntimeUtils.runningInGcp(), is(false));
    System.setProperty(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.runningInGcp(), is(false));

    updateEnv(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.runningInGcp(), is(true));
  }

  @Test
  public void testGetGcpProjectName() throws ReflectiveOperationException {
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(false));
    System.setProperty(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(false));
    updateEnv(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(true));
  }

}