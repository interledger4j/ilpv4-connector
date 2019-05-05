package org.interledger.ilpv4.connector.crypto.cli;

import org.interledger.ilpv4.connector.crypto.cli.shell.CryptoCliConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * The Java <tt>main</tt> class that is executed to bootstrap the Spring Shell CLI.
 */
@SpringBootApplication
@Import(CryptoCliConfig.class)
public class CryptoCli {
  public static void main(String[] args) {
    SpringApplication.run(CryptoCli.class, args);
  }
}
