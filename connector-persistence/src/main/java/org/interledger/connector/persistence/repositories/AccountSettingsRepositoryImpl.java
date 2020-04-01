package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.InvalidAccountIdProblem;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An extension of {@link AccountSettingsRepository} for use by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public class AccountSettingsRepositoryImpl implements AccountSettingsRepositoryCustom {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccountSettingsRepositoryImpl.class);

  private final FilterAccountByValidAccountId filterAccountByValidAccountId;

  @Autowired
  private ConversionService conversionService;

  @Autowired
  private AccountSettingsRepository accountSettingsRepository;

  public AccountSettingsRepositoryImpl() {
    this.filterAccountByValidAccountId = new FilterAccountByValidAccountId();
  }

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
      // Check to see if the AccountId is going to be able to be marshalled to a valid AccountId. Generally this
      // should never be a problem, but it's possible that invalid accountId chars made their way into the datastore
      // (either manually or via old implementations). In this case, we want to WARN on this condition, but otherwise
      // ignore this entry.
      .filter(filterAccountByValidAccountId::test)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class));
  }

  @Override
  public List<AccountSettings> findByAccountRelationshipIsWithConversion(AccountRelationship relationship) {
    Objects.requireNonNull(relationship);
    return accountSettingsRepository.findByAccountRelationshipIs(relationship).stream()
      // Check to see if the AccountId is going to be able to be marshalled to a valid AccountId. Generally this
      // should never be a problem, but it's possible that invalid accountId chars made their way into the datastore
      // (either manually or via old implementations). In this case, we want to WARN on this condition, but otherwise
      // ignore this entry.
      .filter(filterAccountByValidAccountId::test)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .collect(Collectors.toList());
  }

  @Override
  public List<AccountSettings> findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion() {
    return accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue().stream()
      // Check to see if the AccountId is going to be able to be marshalled to a valid AccountId. Generally this
      // should never be a problem, but it's possible that invalid accountId chars made their way into the datastore
      // (either manually or via old implementations). In this case, we want to WARN on this condition, but otherwise
      // ignore this entry.
      .filter(filterAccountByValidAccountId::test)
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
      // Check to see if the AccountId is going to be able to be marshalled to a valid AccountId. Generally this
      // should never be a problem, but it's possible that invalid accountId chars made their way into the datastore
      // (either manually or via old implementations). In this case, we want to WARN on this condition, but otherwise
      // ignore this entry.
      .filter(filterAccountByValidAccountId::test)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class));
  }

  /**
   * A {@link Predicate} used to filter invalid Account result-set records that contain invalid Account identifiers.
   * This predicate exists because it's possible that invalid accountId chars have made their way into the datastore
   * (either manually or via old implementations). In this case, we want to WARN on this condition, but otherwise ignore
   * this entry so that queries can still function. Without this filter, invalid Account identifiers in any result-set
   * would preclude any such query from completing.
   */
  public static final class FilterAccountByValidAccountId implements Predicate<AccountSettingsEntity> {

    /**
     * Only allows Accounts whose Account identifiers contain valid characters.
     *
     * @param accountSettingsEntity the input argument
     *
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    @Override
    public boolean test(final AccountSettingsEntity accountSettingsEntity) {
      try {
        AccountId.of(accountSettingsEntity.getAccountIdAsString());
        return true;
      } catch (InvalidAccountIdProblem e) {
        LOGGER.warn(
          "Account Datastore contains an invalid AccountId: {}", accountSettingsEntity.getAccountIdAsString(),
          e
        );
        return false;
      }
    }
  }
}
