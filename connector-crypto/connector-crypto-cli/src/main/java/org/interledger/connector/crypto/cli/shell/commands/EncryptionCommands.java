package org.interledger.connector.crypto.cli.shell.commands;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionException;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.JavaKeystoreLoader;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.impl.GcpEncryptionService;
import org.interledger.crypto.impl.JksEncryptionService;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.kms.v1.stub.KeyManagementServiceStubSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * Commands for encryption.
 */
@ShellComponent
@ShellCommandGroup("Encryption Commands")
public class EncryptionCommands {

  private static final String GCP_CONFIGURATION = "GCP Configuration";
  private static final String JKS_CONFIGURATION = "JKS Configuration";

  private static final String UNSET = "[unset]";

  private final Logger blankLineLogger = LoggerFactory.getLogger("blank.line.logger");

  private KeyStoreType keyStoreType = KeyStoreType.JKS;

  // JKS Properties (defaults used for development purposes only)
  private String jksFileName = "crypto.p12";
  private String jksPassword = "password";
  private String secret0KeyAlias = "secret0";
  private String secret0KeyPassword = "password";

  // GCP Properties.
  private String gcpProjectId;
  private Optional<String> gcpKeyringLocationId = Optional.of("global");
  private Optional<String> keyringIdentifier = Optional.of("connector");
  private EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithm.GOOGLE_SYMMETRIC;
  private Optional<String> encryptionKeyIdentifier = Optional.of("secret0");
  private String encryptionKeyVersion = "1";
  private CredentialsProvider credentialsProvider =
    KeyManagementServiceStubSettings.defaultCredentialsProviderBuilder().build();

  public EncryptionCommands() {
    lookForGcpCredentials();
    if (this.gcpProjectId != null) {
      blankLineLogger.info("Using GCP as the default keystore platform");
      setKeystorePlatform(KeyStoreType.GCP.name());
    }
    else {
      blankLineLogger.info("Using JKS as the default keystore platform");
      setKeystorePlatform(KeyStoreType.JKS.name());
    }
  }

  private void lookForGcpCredentials() {
    if (Files.exists(Paths.get(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")))) {
      try {
        Object credentials = credentialsProvider.getCredentials();
        if (credentials instanceof ServiceAccountCredentials) {
          this.gcpProjectId = ((ServiceAccountCredentials) credentials).getProjectId();
        }
        else {
          this.gcpProjectId = ServiceOptions.getDefaultProjectId();
        }
      } catch (Exception e) {
        blankLineLogger.warn("Failed to load GCP credentials. Cause: " + e.getMessage());
      }
    }
  }

  /**
   * Encrypt a plaintext value using the selected key information.
   */
  @ShellMethod(value = "Encrypt a plaintext value", key = {"e", "encrypt"})
  String encrypt(final String plaintext) throws Exception {
    return encryptHelper(plaintext.getBytes());
  }

  /**
   * Encrypt a plaintext value that is Base64-encoded using the selected key information.
   */
  @ShellMethod(value = "Encrypt a plaintext value encoded that is Base64-encoded", key = {"eb64", "encrypt-base64"})
  String encryptB64(final String plaintextB64) throws Exception {
    return encryptHelper(Base64.getDecoder().decode(plaintextB64));
  }

  /**
   * Encrypt some bytes.
   */
  private String encryptHelper(final byte[] plainBytes) throws Exception {
    Objects.requireNonNull(plainBytes);
    if (plainBytes.length == 0) {
      throw new RuntimeException("Secret must not be empty!");
    }

    if (this.keyStoreType.equals(KeyStoreType.GCP)) {
      final GcpEncryptionService gcpSecretsManager = new GcpEncryptionService(
        gcpProjectId, gcpKeyringLocationId.get(), credentialsProvider
      );

      final KeyMetadata keyMetadata = KeyMetadata.builder()
        .keyIdentifier(encryptionKeyIdentifier.get())
        .keyVersion(encryptionKeyVersion)
        .keyringIdentifier(keyringIdentifier.get())
        .platformIdentifier(keyStoreType.name())
        .build();

      final EncryptedSecret encryptedSecret =
        gcpSecretsManager.encrypt(keyMetadata, encryptionAlgorithm, plainBytes);
      return "Encoded Encrypted Secret: " + encryptedSecret.encodedValue();
    } else if (this.keyStoreType.equals(KeyStoreType.JKS)) {
      // Load Secret0 from Keystore.
      final KeyStore keyStore = JavaKeystoreLoader.loadFromClasspath(this.jksFileName, jksPassword.toCharArray());

      final SecretKey secret0Key = loadSecretKeyFromJavaKeystore(keyStore);
      final EncryptionService encryptionService = new JksEncryptionService(secret0Key);

      final KeyMetadata keyMetadata = KeyMetadata.builder()
        .platformIdentifier(keyStoreType.name())
        .keyringIdentifier(jksFileName)
        .keyIdentifier(secret0KeyAlias)
        .keyVersion("1")
        .build();

      final EncryptedSecret encryptedSecret =
        encryptionService.encrypt(keyMetadata, encryptionAlgorithm, plainBytes);
      return "Encoded Encrypted Secret: " + encryptedSecret.encodedValue();
    } else {
      throw new RuntimeException("Please select a valid Keystore Platform! Unsupported Platform: " + keyStoreType);
    }
  }

  /**
   * Encrypt a secret value using the selected key information.
   */
  @ShellMethod(value = "Decrypt an encrypted value", key = {"d", "decrypt"})
  String decrypt(final String encodedValue) throws Exception {
    if (encodedValue.length() <= 0) {
      throw new RuntimeException("cipherMessageB64 must not be empty!");
    }

    if (this.keyStoreType.equals(KeyStoreType.GCP)) {
      final GcpEncryptionService gcpSecretsManager = new GcpEncryptionService(
        gcpProjectId, gcpKeyringLocationId.get(), credentialsProvider
      );

      final EncryptedSecret encryptedSecret = EncryptedSecret.fromEncodedValue(encodedValue);
      final byte[] plainTextBytes = gcpSecretsManager.decrypt(encryptedSecret);
      return "PlainText: " + new String(plainTextBytes);
    } else if (this.keyStoreType.equals(KeyStoreType.JKS)) {
      // Load Secret0 from Keystore.
      final KeyStore keyStore = JavaKeystoreLoader.loadFromClasspath(this.jksFileName, jksPassword.toCharArray());
      final SecretKey secret0Key = loadSecretKeyFromJavaKeystore(keyStore);
      final EncryptionService encryptionService = new JksEncryptionService(secret0Key);
      final EncryptedSecret encryptedSecret = EncryptedSecret.fromEncodedValue(encodedValue);
      final byte[] utfPlainText = encryptionService.decrypt(encryptedSecret);
      return "PlainText: " + new String(utfPlainText);
    } else {
      throw new RuntimeException("Please select a valid Keystore Platform! Unsupported Platform: " + keyStoreType);
    }
  }

  /**
   * Set the keystore platform.
   *
   * @param keystorePlatform {@link String} representing the Keystore platform.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the keystore platform (defaults to Google KMS)",
    key = {"pid", "keystore-platform-id"}
  )
  public String setKeystorePlatform(final String keystorePlatform) {
    this.keyStoreType = KeyStoreType.valueOf(keystorePlatform.toUpperCase());

    if (KeyStoreType.GCP.equals(this.keyStoreType)) {
      this.encryptionAlgorithm = EncryptionAlgorithm.GOOGLE_SYMMETRIC;
    } else if (KeyStoreType.JKS.equals(this.keyStoreType)) {
      this.encryptionAlgorithm = EncryptionAlgorithm.AES_GCM;
    }

    return String.format("Keystore Platform set to: `%s`", keystorePlatform);
  }

  /**
   * Display current values.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Show current values",
    key = {"cv", "current-values"}
  )
  public void showCurrentValues() {
    if (KeyStoreType.GCP.equals(keyStoreType)) {
      blankLineLogger.info("Keystore Type         : " + this.keyStoreType);
      blankLineLogger.info("GCP Project Id        : " + this.gcpProjectId);
      blankLineLogger.info("Keyring Location      : " + this.gcpKeyringLocationId.orElse(UNSET));
      blankLineLogger.info("Keyring Id            : " + this.keyringIdentifier.orElse(UNSET));
      blankLineLogger.info("Encryption Algorithm  : " + this.encryptionAlgorithm);
      blankLineLogger.info("Encryption Key Id     : " + this.encryptionKeyIdentifier.orElse(UNSET));
      blankLineLogger.info("Encryption Key Version: " + this.encryptionKeyVersion);
    } else if (KeyStoreType.JKS.equals(keyStoreType)) {
      blankLineLogger.info("Keystore Type    : " + this.keyStoreType);
      blankLineLogger.info("JKS File         : " + this.jksFileName);
      blankLineLogger.info("JKS File Password: " + "********");
      blankLineLogger.info("Secret0 Alias    : " + this.secret0KeyAlias);
      blankLineLogger.info("Secret0 Password : " + "********");
    }
  }

  /**
   * Centralizes all logic for enabling/disabling shell commands.
   */
  @ShellMethodAvailability({"e", "encrypt"})
  Availability availabilityCheck() {
    if (KeyStoreType.GCP.equals(this.keyStoreType)) {
      if (!this.gcpKeyringLocationId.isPresent()) {
        return Availability.unavailable("You must specify the GCP Keyring Location Id!");
      } else if (!this.keyringIdentifier.isPresent()) {
        return Availability.unavailable("You must specify the keyring identifier!");
      } else if (StringUtils.isEmpty(this.encryptionKeyIdentifier)) {
        return Availability.unavailable("You must specify the encryption key identifier!");
      } else if (StringUtils.isEmpty(this.encryptionKeyVersion)) {
        return Availability.unavailable("You must specify the encryption key version!");
      } else if (!this.encryptionAlgorithm.equals(EncryptionAlgorithm.GOOGLE_SYMMETRIC)) {
        return Availability.unavailable("GCP currently only supports " + EncryptionAlgorithm.GOOGLE_SYMMETRIC);
      }
    } else if (KeyStoreType.JKS.equals(this.keyStoreType)) {
      if (StringUtils.isEmpty(this.jksFileName)) {
        return Availability.unavailable("You must specify the JKS filename!");
      } else if (this.jksPassword == null) {
        return Availability.unavailable("You must specify the JKS password!");
      } else if (StringUtils.isEmpty(this.secret0KeyAlias)) {
        return Availability.unavailable("You must specify the Secret0 Key alias!");
      } else if (this.secret0KeyPassword == null) {
        return Availability.unavailable("You must specify the Secret0 Key password!");
      } else if (!this.encryptionAlgorithm.equals(EncryptionAlgorithm.AES_GCM)) {
        return Availability.unavailable("JKS currently only supports " + EncryptionAlgorithm.AES_GCM);
      }
    }

    return Availability.available();
  }

  ///////////////
  // JKS Commands
  ///////////////

  /**
   * Set the JKS Filename.
   *
   * @param jksFileName {@link String} representing the JKS Filename.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the JKS Filename",
    key = {"jksfn", "jks-filename"},
    group = JKS_CONFIGURATION
  )
  public String setJksFileName(final String jksFileName) {
    this.jksFileName = jksFileName;
    return String.format("JKS Filename set to: `%s`", jksFileName);
  }

  public Availability setJksFileNameAvailability() {
    return jksAvailability();
  }

  /**
   * Set the JKS Password.
   *
   * @param jksPassword {@link String} representing the GCP Project Identifier.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the JKS Password",
    key = {"jkspw", "jks-password"},
    group = JKS_CONFIGURATION
  )
  public String setJksPassword(final String jksPassword) {
    this.jksPassword = jksPassword;
    // These are the same by default...
    this.secret0KeyPassword = jksPassword;
    return String.format("JKS Password set to: `%s`", "********");
  }

  public Availability setJksPasswordAvailability() {
    return jksAvailability();
  }

  /**
   * Set the Secret0 Alias
   *
   * @param secret0KeyAlias {@link String} representing the secret0 Key Alias
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the JKS Secret0 Alias",
    key = {"jkss0a", "jks-secret0-alias"},
    group = JKS_CONFIGURATION
  )
  public String setSecret0KeyAlias(final String secret0KeyAlias) {
    this.secret0KeyAlias = secret0KeyAlias;
    return String.format("JKS secret0KeyAlias set to: `%s`", secret0KeyAlias);
  }

  public Availability setSecret0KeyAliasAvailability() {
    return jksAvailability();
  }

  /**
   * Set the Secret0 Key Password
   *
   * @param secret0KeyPassword {@link String} representing the secret0 Key Password
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the JKS Secret0 Password",
    key = {"jkss0pw", "jks-secret0-password"},
    group = JKS_CONFIGURATION
  )
  public String setSecret0KeyPassword(final String secret0KeyPassword) {
    this.secret0KeyAlias = secret0KeyPassword;
    return String.format("JKS secret0KeyPassword set to: `%s`", secret0KeyPassword);
  }

  public Availability setSecret0KeyAvailability() {
    return jksAvailability();
  }

  ///////////////
  // GCP Commands
  ///////////////

  public Availability setGcpProjectIdAvailability() {
    return gcpAvailability();
  }

  /**
   * Set the GCP Location Id.
   *
   * @param gcpLocationId {@link String} representing the GCP Location Identifier.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the GCP Location Id",
    key = {"gcplid", "gcp-location-id"},
    group = GCP_CONFIGURATION
  )
  public String setGcpLocationId(final String gcpLocationId) {
    this.gcpKeyringLocationId = Optional.ofNullable(gcpLocationId);
    return String.format("GCP LocationId set to: `%s`", this.gcpKeyringLocationId.orElse(UNSET));
  }

  public Availability setGcpLocationIdAvailability() {
    return gcpAvailability();
  }

  /**
   * Set the Keyring identifier, if appropriate.
   *
   * @param keyringIdentifier {@link String} representing the Keyring.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the Keyring platform",
    key = {"krid", "keyring-id"},
    group = GCP_CONFIGURATION
  )
  public String setKeyringIdentifier(final String keyringIdentifier) {
    this.keyringIdentifier = Optional.ofNullable(keyringIdentifier);
    return String.format("Keyring Identifier set to: `%s`", this.keyringIdentifier.orElse(UNSET));
  }

  public Availability setKeyringIdentifierAvailability() {
    return gcpAvailability();
  }

  /**
   * Set the Encryption algorithm.
   *
   * @param encryptionAlgorithm {@link String}.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the Encryption Algorithm",
    key = {"ea", "encryption-alg"},
    group = GCP_CONFIGURATION
  )
  public String setEncryptionAlgorithm(final String encryptionAlgorithm) {
    this.encryptionAlgorithm = EncryptionAlgorithm.valueOf(encryptionAlgorithm);
    return String.format("Encryption algorithm set to: `%s`", this.encryptionAlgorithm);
  }

  public Availability setEncryptionAlgorithmAvailability() {
    return gcpAvailability();
  }

  /**
   * Set the Encryption Key Identifier
   *
   * @param encryptionKeyIdentifier {@link String}.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the Key Identifier to use for encryption",
    key = {"ekid", "encryption-key-id"},
    group = GCP_CONFIGURATION
  )
  public String setEncryptionKeyIdentifier(final String encryptionKeyIdentifier) {
    this.encryptionKeyIdentifier = Optional.ofNullable(encryptionKeyIdentifier);
    return String.format("Encryption Key Identifier set to: `%s`", this.encryptionKeyIdentifier.orElse(UNSET));
  }

  public Availability setEncryptionKeyIdentifierAvailability() {
    return gcpAvailability();
  }

  /**
   * Set the Encryption Key Version
   *
   * @param encryptionKeyVersion {@link String}.
   *
   * @return A message displayable to the CLI user.
   */
  @ShellMethod(
    value = "Set the Key version to use for encryption",
    key = {"ekv", "encryption-key-version"},
    group = GCP_CONFIGURATION
  )
  public String setEncryptionKeyVersion(final String encryptionKeyVersion) {
    this.encryptionKeyVersion = Objects.requireNonNull(encryptionKeyVersion);
    return String.format("Encryption Key Version set to: `%s`", this.encryptionKeyVersion);
  }

  public Availability setEncryptionKeyVersionAvailability() {
    return gcpAvailability();
  }

  private Availability jksAvailability() {
    return KeyStoreType.JKS.equals(keyStoreType) ? Availability.available() :
      Availability.unavailable("Only available for JKS Keystores!");
  }

  private Availability gcpAvailability() {
    return KeyStoreType.GCP.equals(keyStoreType) ? Availability.available() :
      Availability.unavailable("Only available for GCP KMS Keystore!");
  }

  private SecretKey loadSecretKeyFromJavaKeystore(final KeyStore keyStore) throws Exception {
    Objects.requireNonNull(keyStore);

    // Password-protected keys are not yet supported
    if (keyStore.isKeyEntry(this.secret0KeyAlias)) {
      Key key = keyStore.getKey(this.secret0KeyAlias, this.secret0KeyPassword.toCharArray());
      return (SecretKey) key;
    } else {
      throw new EncryptionException("No KeyEntry found for secret0Alias: " + this.secret0KeyAlias);
    }
  }
}
