package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.interledger.connector.accounts.AccountSettings;
import org.springframework.hateoas.ResourceSupport;

import java.util.Objects;

/**
 * An {@link AccountSettings} with HATEOAS links.
 */
public class AccountSettingsResource extends ResourceSupport {

  @JsonUnwrapped
  private final AccountSettings accountSettings;

  public AccountSettingsResource(final AccountSettings accountSettings) {
    this.accountSettings = Objects.requireNonNull(accountSettings);
  }

  public AccountSettings getAccountSettings() {
    return accountSettings;
  }

}
