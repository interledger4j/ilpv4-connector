package org.interledger.connector.caching;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.event.AccountUpdatedEvent;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

public class AccountSettingsLoadingCacheTest {

  private AccountSettingsLoadingCache accountSettingsLoadingCache;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private AccountSettingsRepository repository;

  @Mock
  private Cache<AccountId, Optional<AccountSettings>> accountSettingsCache;

  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() {
    accountSettingsLoadingCache = new AccountSettingsLoadingCache(repository, accountSettingsCache, eventBus);
  }

  @Test
  public void accountUpdatedEventInvalidates() {
    accountSettingsLoadingCache._handleAccountUpdated(
      AccountUpdatedEvent.builder()
        .accountId(AccountId.of("ricketycricket"))
        .build()
    );
    verify(accountSettingsCache, times(1)).invalidate(AccountId.of("ricketycricket"));
  }
}
