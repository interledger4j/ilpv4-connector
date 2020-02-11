package org.interledger.connector.server.spring.controllers.admin;

import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ENCRYPTION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.crypto.ConnectorEncryptionService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Allows an admin to do encryption things.
 */
@RestController(SLASH_ENCRYPTION)
public class EncryptionController {

  private final AccountManager accountManager;

  private final ConnectorEncryptionService connectorEncryptionService;

  public EncryptionController(final AccountManager accountManager, ConnectorEncryptionService encryptionService) {
    this.accountManager = Objects.requireNonNull(accountManager);
    this.connectorEncryptionService = Objects.requireNonNull(encryptionService);
  }

  /**
   * Encrypts a provided message using the server's current encryption key for secret0
   * @param message text to encrypt
   * @return encoded encrypted secret
   */
  @RequestMapping(
    path = SLASH_ENCRYPTION + "/encrypt",
    method = RequestMethod.POST,
    consumes = TEXT_PLAIN_VALUE,
    produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public String encrypt(
    @RequestBody final String message
  ) {
    return connectorEncryptionService.encryptWithSecret0(message.getBytes()).encodedValue();
  }

  /**
   * Loads and reencrypts all account settings that are not using the latest encryption settings. This can be useful
   * if switching from JKS to KMS, or if a newer version of a key has been generated but existing accounts secrets
   * are using an older version of the key.
   *
   * @return number of accounts refreshed
   */
  @RequestMapping(
    path = SLASH_ENCRYPTION + "/refresh",
    method = RequestMethod.POST,
    produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public String refreshEncryption() {
    long accountUpdateCount = accountManager.getAccounts().stream()
      .map(
        accountSettings -> accountManager.updateAccount(accountSettings.accountId(), accountSettings)
      )
      .collect(Collectors.toList())
      .size();
    return  "" + accountUpdateCount;
  }

}
