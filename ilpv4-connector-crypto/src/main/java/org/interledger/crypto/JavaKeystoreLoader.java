package org.interledger.crypto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * A class that assist in loading {@link KeyStore} objects from the classpath.
 */
public class JavaKeystoreLoader {

  public static KeyStore loadFromClasspath(final String jksFileName, final char[] jksPassword) throws Exception {

    // Load Secret0 from Keystore.
    final KeyStore keyStore;
    try (InputStream keyStoreStream = JavaKeystoreLoader.class.getResourceAsStream("/" + jksFileName)) {
      if (keyStoreStream == null) {
        throw new FileNotFoundException("'" + "/" + jksFileName + "' not found on classpath");
      }

      keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(keyStoreStream, jksPassword);

      return keyStore;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
