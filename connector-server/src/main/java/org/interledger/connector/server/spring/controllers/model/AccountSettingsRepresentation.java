package org.interledger.connector.server.spring.controllers.model;

import org.interledger.connector.accounts.AccountSettings;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * A RESTful representation of an {@link AccountSettings}, with HATEOAS links.
 */
public class AccountSettingsRepresentation extends RepresentationModel {

  @JsonUnwrapped
  private final AccountSettings accountSettings;

  public AccountSettingsRepresentation(final AccountSettings accountSettings) {
    this.accountSettings = Objects.requireNonNull(accountSettings);
  }

  public AccountSettings getAccountSettings() {
    return accountSettings;
  }

}
