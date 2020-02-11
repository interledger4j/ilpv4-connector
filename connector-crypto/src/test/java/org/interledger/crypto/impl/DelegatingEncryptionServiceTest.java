package org.interledger.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyStoreType;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.function.Function;

public class DelegatingEncryptionServiceTest {

  private static final EncryptedSecret GCP_SECRET = EncryptedSecret.fromEncodedValue("enc:gcpkms:ring:bar:1:gs:AAAA");
  private static final EncryptedSecret JKS_SECRET = EncryptedSecret.fromEncodedValue("enc:jks:file:foo:1:aes_gcm:BBBB");
  private static final byte[] GCP_DECRYPTED = "gcp decrypted".getBytes();
  private static final byte[] JKS_DECRYPTED = "jks decrypted".getBytes();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public MockitoRule mockitoJUnit = MockitoJUnit.rule();

  @Mock
  private GcpEncryptionService mockGcp;

  @Mock
  private JksEncryptionService mockJks;

  @Before
  public void setUp() {
    when(mockJks.keyStoreType()).thenReturn(KeyStoreType.JKS);
    when(mockGcp.keyStoreType()).thenReturn(KeyStoreType.GCP);
    mockEncryptDecrypt(mockJks, JKS_SECRET, JKS_DECRYPTED);
    mockEncryptDecrypt(mockGcp, GCP_SECRET, GCP_DECRYPTED);
  }

  @Test
  public void keyStoreType() {
    assertThat(new DelegatingEncryptionService(Sets.newHashSet(mockGcp)).keyStoreType()).isEqualTo(KeyStoreType.GCP);
    assertThat(new DelegatingEncryptionService(Sets.newHashSet(mockJks)).keyStoreType()).isEqualTo(KeyStoreType.JKS);
  }

  @Test
  public void decrypt() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockGcp, mockJks));
    assertThat(service.decrypt(GCP_SECRET)).isEqualTo(GCP_DECRYPTED);
    assertThat(service.decrypt(JKS_SECRET)).isEqualTo(JKS_DECRYPTED);
  }

  @Test
  public void decryptThrowsExceptionIfNoProvider() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockGcp));
    expectedException.expect(RuntimeException.class);
    service.decrypt(JKS_SECRET);
  }

  @Test
  public void encrypt() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockGcp, mockJks));
    assertThat(service.encrypt(GCP_SECRET.keyMetadata(), GCP_SECRET.encryptionAlgorithm(), GCP_DECRYPTED))
      .isEqualTo(GCP_SECRET);
    assertThat(service.encrypt(JKS_SECRET.keyMetadata(), JKS_SECRET.encryptionAlgorithm(), JKS_DECRYPTED))
      .isEqualTo(JKS_SECRET);  
  }

  @Test
  public void encryptThrowsExceptionIfNoProvider() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockJks));
    expectedException.expect(RuntimeException.class);
    service.encrypt(GCP_SECRET.keyMetadata(), GCP_SECRET.encryptionAlgorithm(), GCP_DECRYPTED);
  }

  @Test
  public void withDecrypted() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockGcp, mockJks));
    assertThat(service.withDecrypted(GCP_SECRET, regurgitate)).isEqualTo(GCP_DECRYPTED);
    assertThat(service.withDecrypted(JKS_SECRET,regurgitate)).isEqualTo(JKS_DECRYPTED);
  }

  @Test
  public void withDecryptedThrowsExceptionIfNoProvider() {
    DelegatingEncryptionService service = new DelegatingEncryptionService(Sets.newHashSet(mockJks));
    expectedException.expect(RuntimeException.class);
    service.withDecrypted(GCP_SECRET, regurgitate);
  }

  private void mockEncryptDecrypt(EncryptionService service, EncryptedSecret secret, byte[] decryptedValue) {
    when(service.decrypt(secret.keyMetadata(), secret.encryptionAlgorithm(), secret.cipherMessage()))
      .thenReturn(decryptedValue);
    when(service.withDecrypted(secret, regurgitate)).thenReturn(regurgitate.apply(decryptedValue));
    when(service.encrypt(secret.keyMetadata(), secret.encryptionAlgorithm(), decryptedValue))
      .thenReturn(secret);
  }

  Function<byte[], byte[]> regurgitate = (input) -> input;

}