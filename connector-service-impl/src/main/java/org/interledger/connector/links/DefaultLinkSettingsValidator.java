package org.interledger.connector.links;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkSettings;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation that validates that links settings have required fields and that for ILP-OVER-HTTP link settings,
 * that the incoming and/or outgoing secrets are decryptable
 */
public class DefaultLinkSettingsValidator implements LinkSettingsValidator {

  private final ConnectorEncryptionService encryptionService;
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  public DefaultLinkSettingsValidator(ConnectorEncryptionService encryptionService,
                                      Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.encryptionService = encryptionService;
    this.connectorSettingsSupplier = connectorSettingsSupplier;
  }

  @Override
  public <T extends LinkSettings> T validateSettings(T linkSettings) {
    if (linkSettings instanceof IlpOverHttpLinkSettings) {
      return (T) validateIlpLinkSettings((IlpOverHttpLinkSettings) linkSettings);
    }
    return linkSettings;
  }

  private IlpOverHttpLinkSettings validateIlpLinkSettings(IlpOverHttpLinkSettings linkSettings) {
    EncryptedSecret incomingSecret =
      validate(getOrCreateEncryptedSecret(linkSettings.incomingHttpLinkSettings().encryptedTokenSharedSecret()));

    EncryptedSecret outgoingSecret =
      validate(getOrCreateEncryptedSecret(linkSettings.outgoingHttpLinkSettings().encryptedTokenSharedSecret()));

    IncomingLinkSettings incomingLinkSettings =
      IncomingLinkSettings.builder().from(linkSettings.incomingHttpLinkSettings())
        .encryptedTokenSharedSecret(incomingSecret.encodedValue())
        .build();

    OutgoingLinkSettings outgoingLinkSettings =
      OutgoingLinkSettings.builder().from(linkSettings.outgoingHttpLinkSettings())
        .encryptedTokenSharedSecret(outgoingSecret.encodedValue())
        .build();

    Map<String, Object> newCustomSettings = Maps.newHashMap(linkSettings.getCustomSettings());
    newCustomSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET,
      incomingLinkSettings.encryptedTokenSharedSecret());
    newCustomSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET,
      outgoingLinkSettings.encryptedTokenSharedSecret());

    return IlpOverHttpLinkSettings.builder().from(linkSettings)
      .incomingHttpLinkSettings(incomingLinkSettings)
      .outgoingHttpLinkSettings(outgoingLinkSettings)
      .customSettings(newCustomSettings)
      .build();
  }

  private EncryptedSecret getOrCreateEncryptedSecret(String sharedSecret) {
    if (Strings.isNullOrEmpty(sharedSecret)) {
      throw new IllegalArgumentException("sharedSecret cannot be empty");
    }
    validateSharedSecretIsAscii(sharedSecret);
    if (sharedSecret.startsWith("enc:")) {
      return EncryptedSecret.fromEncodedValue(sharedSecret);
    } else {
      byte[] secretBytes = null;
      try {
        secretBytes = Base64.getDecoder().decode(sharedSecret);
        return encryptionService.encryptWithAccountSettingsKey(secretBytes);
      } finally {
        Arrays.fill(secretBytes, (byte) 0);
      }
    }
  }

  private EncryptedSecret validate(EncryptedSecret encryptedSecret) {
    return encryptionService.getDecryptor().withDecrypted(encryptedSecret, (decrypted) -> {
      if (connectorSettingsSupplier.get().isRequire32ByteSharedSecrets() && decrypted.length < 32) {
        throw new IllegalArgumentException("shared secret must be 32 bytes");
      }
      return encryptedSecret;
    });
  }

  private void validateSharedSecretIsAscii(String sharedSecret) {
    Preconditions.checkArgument(CharMatcher.ascii().matchesAllOf(sharedSecret),
      "Shared secret must be ascii");
  }

}
