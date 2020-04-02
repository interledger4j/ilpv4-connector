package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepositoryImpl.FilterAccountByValidAccountId;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link FilterAccountByValidAccountId}.
 */
public class FilterAccountByValidAccountIdTest {

  private FilterAccountByValidAccountId filter;

  @Before
  public void setUp() {
    filter = new FilterAccountByValidAccountId();
  }

  @Test
  public void testFilterWithValidAccountId() {
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountIdAsString()).thenReturn("valid-account-id");
    assertThat(filter.test(accountSettingsEntityMock)).isTrue();
  }

  @Test
  public void testFilterWithInvalidAccountId() {
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountIdAsString()).thenReturn("invalid+account+id");
    assertThat(filter.test(accountSettingsEntityMock)).isFalse();
  }
}
