package org.interledger.connector.server.spring.controllers.account;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.accounts.AccessTokenManager;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.server.spring.controllers.model.CreateAccessTokenRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.List;
import java.util.Optional;

/**
 * A RESTful controller account balances
 */
@RestController
public class AccountAccessTokenController {

  private final AccessTokenManager accessTokenManager;


  public AccountAccessTokenController(AccessTokenManager accessTokenManager) {
    this.accessTokenManager = accessTokenManager;
  }

  /**
   * Gets the {@link AccessToken}s for the given {@code accountId}. The returned tokens do not contain the
   * actual token because the actual token is cryptographically hashed
   *
   * @param accountId
   * @return list of access tokens
   */
  @RequestMapping(
      value = PathConstants.SLASH_ACCOUNTS_TOKENS_PATH, method = {RequestMethod.GET},
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public List<AccessToken> getTokens(@PathVariable(PathConstants.ACCOUNT_ID) String accountId) {
    return accessTokenManager.findTokensByAccountId(AccountId.of(accountId));
  }

  /**
   * Gets the {@link AccessToken}s for the given {@code accountId}. The returned tokens do not contain the
   * actual token because the actual token is cryptographically hashed
   *
   * @param accountId
   * @return list of access tokens
   */
  @RequestMapping(
    value = PathConstants.SLASH_ACCOUNTS_TOKENS_PATH + "/{id}", method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Optional<AccessToken> findById(@PathVariable(PathConstants.ACCOUNT_ID) String accountId,
                                                    @PathVariable("id") long id) {
    return accessTokenManager.findByAccountIdAndId(AccountId.of(accountId), id);
  }

  /**
   * Gets the {@link AccessToken}s for the given {@code accountId}. The returned tokens do not contain the
   * actual token because the actual token is cryptographically hashed
   *
   * @param accountId
   * @return list of access tokens
   */
  @RequestMapping(
    value = PathConstants.SLASH_ACCOUNTS_TOKENS_PATH, method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccessToken createToken(@PathVariable(PathConstants.ACCOUNT_ID) String accountId,
                                 @RequestBody CreateAccessTokenRequest request) {
    return request.token().map(token -> accessTokenManager.createToken(AccountId.of(accountId), token))
      .orElseGet(() -> accessTokenManager.createToken(AccountId.of(accountId)));
  }

  /**
   * Deletes {@link AccessToken} for the given {@code accountId} and id.
   *
   * @param accountId
   * @param id
   */
  @RequestMapping(
    value = PathConstants.SLASH_ACCOUNTS_TOKENS_PATH + "/{id}", method = {RequestMethod.DELETE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void deleteById(@PathVariable(PathConstants.ACCOUNT_ID) String accountId,
                                        @PathVariable("id") long id) {
    accessTokenManager.deleteByAccountIdAndId(AccountId.of(accountId), id);
  }

  /**
   * Deletes the {@link AccessToken}s for the given {@code accountId}. The returned tokens do not contain the
   * actual token because the actual token is cryptographically hashed
   *
   * @param accountId
   * @return list of access tokens
   */
  @RequestMapping(
    value = PathConstants.SLASH_ACCOUNTS_TOKENS_PATH, method = {RequestMethod.DELETE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void deleteTokensForAccount(@PathVariable(PathConstants.ACCOUNT_ID) String accountId) {
    accessTokenManager.deleteByAccountId(AccountId.of(accountId));
  }

}
