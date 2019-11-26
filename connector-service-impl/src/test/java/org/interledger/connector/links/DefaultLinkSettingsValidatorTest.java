package org.interledger.connector.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.util.Base64Utils.decode;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.ImmutableLinkSettings;
import org.interledger.link.LinkSettings;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.time.Duration;
import java.util.function.Function;

public class DefaultLinkSettingsValidatorTest {

  private static final EncryptedSecret ENCRYPTED_INCOMING_SECRET =
    EncryptedSecret.fromEncodedValue("enc:JKS:crypto.p12:secret0:1:aes_gcm:notARealIncomingSecret");

  private static final EncryptedSecret ENCRYPTED_OUTGOING_SECRET =
    EncryptedSecret.fromEncodedValue("enc:JKS:crypto.p12:secret0:1:aes_gcm:notARealOutgoingSecret");


  private static final String INCOMING_BASE_64 = "V2hhdHMgYmx1ZSBhbmQgc21lbGxzIGxpa2UgcmVkIHBhaW50Pw==";

  private static final String OUTGOING_BASE_64 = "Ymx1ZSBwYWludA==";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultLinkSettingsValidator validator;

  @Mock
  private ConnectorEncryptionService mockConnectorEncryptionService;

  @Mock
  private Decryptor decryptor;

  @Mock
  private ConnectorSettings connectorSettings;


  @Before
  public void setUp() {
    initMocks(this);

    when(connectorSettings.isRequire32ByteSharedSecrets()).thenReturn(false);
    when(mockConnectorEncryptionService.getDecryptor()).thenReturn(decryptor);

    when(decryptor.withDecrypted(eq(ENCRYPTED_INCOMING_SECRET), any()))
      .thenAnswer((args) -> args.getArgument(1, Function.class).apply(INCOMING_BASE_64.getBytes()));
    when(decryptor.withDecrypted(eq(ENCRYPTED_OUTGOING_SECRET), any()))
      .thenAnswer((args) -> args.getArgument(1, Function.class).apply(OUTGOING_BASE_64.getBytes()));

    when(mockConnectorEncryptionService.encryptWithAccountSettingsKey(decode(INCOMING_BASE_64.getBytes())))
      .thenReturn(ENCRYPTED_INCOMING_SECRET);
    when(mockConnectorEncryptionService.encryptWithAccountSettingsKey(decode(OUTGOING_BASE_64.getBytes())))
      .thenReturn(ENCRYPTED_OUTGOING_SECRET);

    validator = new DefaultLinkSettingsValidator(mockConnectorEncryptionService, () -> connectorSettings);
  }

  @Test
  public void validateGenericLinkSettings() {
    ImmutableLinkSettings settings = LinkSettings.builder()
      .linkType(PingLoopbackLink.LINK_TYPE)
      .putCustomSettings("foo", "bar").build();
    assertThat(validator.validateSettings(settings)).isEqualTo(settings);
  }

  @Test
  public void validateIlpOverHttpLinkSettingsFromBase64() {
    final IlpOverHttpLinkSettings linkSettings = newSettings(INCOMING_BASE_64, OUTGOING_BASE_64);

    IlpOverHttpLinkSettings expected = IlpOverHttpLinkSettings.builder().from(linkSettings)
      .incomingHttpLinkSettings(IncomingLinkSettings.builder()
        .from(linkSettings.incomingHttpLinkSettings())
        .encryptedTokenSharedSecret(ENCRYPTED_INCOMING_SECRET.encodedValue())
        .build())
      .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
        .from(linkSettings.outgoingHttpLinkSettings())
        .encryptedTokenSharedSecret(ENCRYPTED_OUTGOING_SECRET.encodedValue())
        .build())
      .build();

    assertThat(validator.validateSettings(linkSettings).incomingHttpLinkSettings())
      .isEqualTo(expected.incomingHttpLinkSettings());

    assertThat(validator.validateSettings(linkSettings).outgoingHttpLinkSettings())
      .isEqualTo(expected.outgoingHttpLinkSettings());
  }

  @Test
  public void ilpOverHttpLinkSettingsFromEncryptedSecret() {
    final IlpOverHttpLinkSettings linkSettings = newSettings(ENCRYPTED_INCOMING_SECRET, ENCRYPTED_OUTGOING_SECRET);

    IlpOverHttpLinkSettings expected = IlpOverHttpLinkSettings.builder().from(linkSettings)
      .incomingHttpLinkSettings(IncomingLinkSettings.builder()
        .from(linkSettings.incomingHttpLinkSettings())
        .encryptedTokenSharedSecret(ENCRYPTED_INCOMING_SECRET.encodedValue())
        .build())
      .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
        .from(linkSettings.outgoingHttpLinkSettings())
        .encryptedTokenSharedSecret(ENCRYPTED_OUTGOING_SECRET.encodedValue())
        .build())
      .build();

    assertThat(validator.validateSettings(linkSettings).incomingHttpLinkSettings())
      .isEqualTo(expected.incomingHttpLinkSettings());

    assertThat(validator.validateSettings(linkSettings).outgoingHttpLinkSettings())
      .isEqualTo(expected.outgoingHttpLinkSettings());
  }

  @Test
  public void require32ByteSecretFailsIfConfigured() {
    when(connectorSettings.isRequire32ByteSharedSecrets()).thenReturn(true);

    final IlpOverHttpLinkSettings linkSettings = newSettings(INCOMING_BASE_64, OUTGOING_BASE_64);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("shared secret must be 32 bytes");
    validator.validateSettings(linkSettings);
  }

  @Test
  public void validationFailsDecryption() {
    reset(decryptor);
    when(decryptor.withDecrypted(any(), any())).thenThrow(new IllegalArgumentException("couldn't decrypt"));

    final IlpOverHttpLinkSettings linkSettings = newSettings(ENCRYPTED_INCOMING_SECRET, ENCRYPTED_OUTGOING_SECRET);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("couldn't decrypt");
    validator.validateSettings(linkSettings);
  }

  @Test
  public void validationFailsEncryption() {
    when(mockConnectorEncryptionService.encryptWithAccountSettingsKey(any()))
      .thenThrow(new IllegalArgumentException("couldn't encrypt"));

    final IlpOverHttpLinkSettings linkSettings = newSettings(INCOMING_BASE_64, OUTGOING_BASE_64);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("couldn't encrypt");
    validator.validateSettings(linkSettings);
  }

  @Test
  public void sharedSecretFailsWhenNotAscii() {
    String zalgoIsTonyThePonyHeComes = "ZA̡͊͠͝LGΌ ISͮ̂҉̯͈͕̹̘̱ TO͇̹̺ͅƝ̴ȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚N̐Y̡ H̸̡̪̯ͨ͊̽̅̾̎Ȩ̬̩̾͛ͪ̈́̀́͘ ̶̧̨̱̹̭̯ͧ̾ͬC̷̙̲̝͖ͭ̏ͥͮ͟Oͮ͏̮̪̝͍M̲̖͊̒ͪͩͬ̚̚͜Ȇ̴̟̟͙̞ͩ͌͝S̨̥̫͎̭ͯ̿̔̀ͅ";
    final IlpOverHttpLinkSettings linkSettings = newSettings(zalgoIsTonyThePonyHeComes, zalgoIsTonyThePonyHeComes);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Shared secret must be ascii");
    validator.validateSettings(linkSettings);
  }

  private IlpOverHttpLinkSettings newSettings(EncryptedSecret incomingSecret, EncryptedSecret outgoingSecret) {
    return newSettings(incomingSecret.encodedValue(), outgoingSecret.encodedValue());
  }

  private IlpOverHttpLinkSettings newSettings(String incomingSecret, String outgoingSecret) {
    final IncomingLinkSettings incomingLinkSettings = IncomingLinkSettings.builder()
      .encryptedTokenSharedSecret(incomingSecret)
      .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience(HttpUrl.parse("https://connie.example.com/"))
      .build();

    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
      .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
      .tokenSubject("bob")
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience(HttpUrl.parse("https://connie.example.com/"))
      .url(HttpUrl.parse("http://alice.example.com"))
      .tokenExpiry(Duration.ofMinutes(5))
      .encryptedTokenSharedSecret(outgoingSecret)
      .build();

    return IlpOverHttpLinkSettings.builder()
      .incomingHttpLinkSettings(incomingLinkSettings)
      .outgoingHttpLinkSettings(outgoingLinkSettings)
      .build();
  }

}