package org.interledger.connector;

import org.interledger.connector.RuntimeProperties;
import org.interledger.connector.RuntimeUtils;
import org.interledger.crypto.KeyStoreType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuntimeUtils}.
 */
public class RuntimeUtilsTest {

  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";
  private static final String ERR_MESSAGE = String.format(
    "Unsupported Keystore Type. Please defined either `%s` or `%s`",
    INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED, INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED
  );

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
  public void testGetGcpProjectName() throws ReflectiveOperationException {
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(false));
    System.setProperty(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(false));
    updateEnv(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent(), is(true));
  }

  @Test(expected = RuntimeException.class)
  public void testDetermineKeystoreTypeGcpKmsNull() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(null);
    try {
      RuntimeUtils.determineKeystoreType(environmentMock);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is(ERR_MESSAGE));
      throw e;
    }
  }

  @Test(expected = RuntimeException.class)
  public void testDetermineKeystoreTypeGcpKmsFalse() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(Boolean.FALSE.toString());
    try {
      RuntimeUtils.determineKeystoreType(environmentMock);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is(ERR_MESSAGE));
      throw e;
    }
  }

  @Test
  public void testDetermineKeystoreTypeGcpKmsTrue() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(Boolean.TRUE.toString());
    assertThat(RuntimeUtils.determineKeystoreType(environmentMock), is(KeyStoreType.GCP));
  }

  @Test(expected = RuntimeException.class)
  public void testDetermineKeystoreTypeJksNull() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(null);
    try {
      RuntimeUtils.determineKeystoreType(environmentMock);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is(ERR_MESSAGE));
      throw e;
    }
  }

  @Test(expected = RuntimeException.class)
  public void testDetermineKeystoreTypeJksFalse() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(Boolean.FALSE.toString());
    try {
      RuntimeUtils.determineKeystoreType(environmentMock);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is(ERR_MESSAGE));
      throw e;
    }
  }

  @Test
  public void testDetermineKeystoreTypeJksTrue() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(Boolean.TRUE.toString());
    assertThat(RuntimeUtils.determineKeystoreType(environmentMock), is(KeyStoreType.JKS));
  }
}
