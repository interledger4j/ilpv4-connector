package org.interledger.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED;
import static org.mockito.Mockito.when;

import org.interledger.connector.core.ConfigConstants;
import org.interledger.crypto.KeyStoreType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Unit tests for {@link RuntimeUtils}.
 */
public class RuntimeUtilsTest {

  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";
  private static final String ERR_MESSAGE = String.format(
    "Unsupported Keystore Type. Please defined either `%s` or `%s`",
    INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED, INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED
  );

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  Environment environmentMock;

  /**
   * Helper method to simulate environment changes, which is otherwise not possible without reflection.
   */
  @SuppressWarnings( {"unchecked"})
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
    when(environmentMock.getActiveProfiles()).thenReturn(new String[] {"foo", "bar"});
    assertThat(RuntimeUtils.gcpProfileEnabled(environmentMock)).isFalse();
    when(environmentMock.getActiveProfiles()).thenReturn(new String[] {RuntimeProperties.Runtimes.GCP});
    assertThat(RuntimeUtils.gcpProfileEnabled(environmentMock)).isTrue();
  }

  @Test
  public void testGetGcpProjectName() throws ReflectiveOperationException {
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent()).isFalse();
    System.setProperty(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent()).isFalse();
    updateEnv(GOOGLE_CLOUD_PROJECT, "foo");
    assertThat(RuntimeUtils.getGoogleCloudProjectId().isPresent()).isTrue();
  }

  @Test
  public void testDetermineKeystoreTypeGcpKmsNull() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(null);
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(ERR_MESSAGE);
    RuntimeUtils.determineKeystoreType(environmentMock);
  }

  @Test
  public void testDetermineKeystoreTypeGcpKmsFalse() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(Boolean.FALSE.toString());
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(ERR_MESSAGE);
    RuntimeUtils.determineKeystoreType(environmentMock);
  }

  @Test
  public void testDetermineKeystoreTypeGcpKmsTrue() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED)).thenReturn(Boolean.TRUE.toString());
    assertThat(RuntimeUtils.determineKeystoreType(environmentMock)).isEqualTo(KeyStoreType.GCP);
  }

  @Test
  public void testDetermineKeystoreTypeJksNull() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(null);
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(ERR_MESSAGE);
    RuntimeUtils.determineKeystoreType(environmentMock);
  }

  @Test
  public void testDetermineKeystoreTypeJksFalse() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(Boolean.FALSE.toString());
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(ERR_MESSAGE);
    RuntimeUtils.determineKeystoreType(environmentMock);
  }

  @Test
  public void testDetermineKeystoreTypeJksTrue() {
    when(environmentMock.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)).thenReturn(Boolean.TRUE.toString());
    assertThat(RuntimeUtils.determineKeystoreType(environmentMock)).isEqualTo(KeyStoreType.JKS);
  }

  @Test
  public void walletModePacketSwitchModeEnabled() {
    when(environmentMock.getActiveProfiles()).thenReturn(new String[] {ConfigConstants.WALLET_MODE});
    assertThat(RuntimeUtils.walletModeEnabled(environmentMock)).isTrue();
    assertThat(RuntimeUtils.packetSwitchModeEnabled(environmentMock)).isFalse();

    when(environmentMock.getActiveProfiles()).thenReturn(new String[0]);
    assertThat(RuntimeUtils.walletModeEnabled(environmentMock)).isFalse();
    assertThat(RuntimeUtils.packetSwitchModeEnabled(environmentMock)).isTrue();
  }
}
