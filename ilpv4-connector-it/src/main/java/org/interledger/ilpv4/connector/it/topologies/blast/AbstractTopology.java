package org.interledger.ilpv4.connector.it.topologies.blast;

import okhttp3.HttpUrl;
import org.interledger.core.InterledgerAddress;

/**
 * An abstract class for all Topologies.
 */
public abstract class AbstractTopology {

  public static final String XRP = "XRP";
  public static final String EXPIRY_2MIN = "PT2M";

  public static final String ALICE = "alice";
  public static final InterledgerAddress ALICE_ADDRESS = InterledgerAddress.of("test." + ALICE);
  public static final String BOB = "bob";
  public static final InterledgerAddress BOB_ADDRESS = InterledgerAddress.of("test." + BOB);
  public static final String ALICE_TOKEN_ISSUER = HttpUrl.parse("https://" + ALICE + ".example.com").toString();
  public static final String BOB_TOKEN_ISSUER = HttpUrl.parse("https://" + BOB + ".example.com").toString();
  public static final int ALICE_PORT = 8080;
  public static final int BOB_PORT = 8081;


  /**
   * The String `shh`, encrypted with the `secret0` key in the `crypto/crypto.p12` keystore.
   */
  public static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  static {
    // This is set to 0 so that the "port" value is used instead...
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }
}
