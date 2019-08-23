package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An extension of {@link AccountSettingsRepository} for use by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public class AccountSettingsRepositoryImpl implements AccountSettingsRepositoryCustom {

  @Autowired
  private ConversionService conversionService;

  @Autowired
  private AccountSettingsRepository accountSettingsRepository;

  @Override
  public Optional<AccountSettings> findByAccountIdWithConversion(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return accountSettingsRepository.findByAccountId(accountId)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class));
  }

  @Override
  public Optional<AccountSettings> findFirstByAccountRelationshipWithConversion(AccountRelationship relationship) {
    Objects.requireNonNull(relationship);
    return accountSettingsRepository.findFirstByAccountRelationship(relationship)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class));
  }

  @Override
  public List<AccountSettings> findByAccountRelationshipIsWithConversion(AccountRelationship relationship) {
    Objects.requireNonNull(relationship);
    return accountSettingsRepository.findByAccountRelationshipIs(relationship).stream()
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .collect(Collectors.toList());
  }

  @Override
  public List<AccountSettings> findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion() {
    return accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue().stream()
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .collect(Collectors.toList());
  }

  @Override
  public Optional<AccountSettings> findBySettlementEngineAccountIdWithConversion(
    SettlementEngineAccountId seAccountId
  ) {
    Objects.requireNonNull(seAccountId);
    return accountSettingsRepository
      .findAccountSettingsEntityBySettlementEngineDetailsSettlementEngineAccountId(seAccountId.value())
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class));
  }
}
