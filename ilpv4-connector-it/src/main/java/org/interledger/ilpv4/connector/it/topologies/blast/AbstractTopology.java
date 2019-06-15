package org.interledger.ilpv4.connector.it.topologies.blast;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

/**
 * An abstract class for all Topologies.
 */
public abstract class AbstractTopology {

  public static final String DOT = ".";
  public static final String XRP = "XRP";
  public static final String EXPIRY_2MIN = "PT2M";

  public static final String ALICE = "alice";
  public static final String BOB = "bob";
  public static final String PAUL = "paul";
  public static final AccountId ALICE_ACCOUNT = AccountId.of(ALICE);
  public static final AccountId BOB_ACCOUNT = AccountId.of(BOB);
  public static final AccountId PAUL_ACCOUNT = AccountId.of(PAUL);

  public static final String TEST = InterledgerAddress.AllocationScheme.TEST.getValue();

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
