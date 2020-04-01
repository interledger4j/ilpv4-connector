package org.interledger.connector.server.spring.controllers;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.balances.AccountBalance;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.balances.AccountBalanceService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Objects;

/**
 * A RESTful controller account balances
 */
@RestController
public class AccountBalanceController {

  private final AccountBalanceService balanceService;

  public AccountBalanceController(final AccountBalanceService balanceService) {
    this.balanceService = Objects.requireNonNull(balanceService);
  }

  /**
   * Gets the {@link AccountBalance} for the given {@code accountId}
   *
   * @param accountId
   *
   * @return balance for account
   */
  @RequestMapping(
    value = PathConstants.SLASH_ACCOUNTS_BALANCE_PATH, method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccountBalanceResponse getBalance(@PathVariable(PathConstants.ACCOUNT_ID) AccountId accountId) {
    return balanceService.getAccountBalance(accountId);
  }

}
