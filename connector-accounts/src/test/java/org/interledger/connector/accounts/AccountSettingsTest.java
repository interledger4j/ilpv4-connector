package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.link.LinkType;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link AccountSettings}.
 */
public class AccountSettingsTest {

  // NOTE: This test validates the method in the abstract base-class.
  @Test
  public void testDefaultChildSettingsOnAbstractClass() {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(LinkType.of("foo"))
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("USD")
      .assetScale(9)
      .build();

    assertThat(accountSettings.sendRoutes()).isTrue();
    assertThat(accountSettings.receiveRoutes()).isFalse();
  }

  // NOTE: This test validates the method in the interface.
  @Test
  public void testDefaultChildSettingsOnInterface() {
    final AccountSettings accountSettings = this.asInterface(AccountRelationship.CHILD);

    assertThat(accountSettings.sendRoutes()).isTrue();
    assertThat(accountSettings.receiveRoutes()).isFalse();
  }

  // NOTE: This test validates the method in the abstract base-class.
  @Test
  public void testDefaultPeerAccountSettingsOnAbstractClass() {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(LinkType.of("foo"))
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(9)
      .build();

    assertThat(accountSettings.sendRoutes()).isTrue();
    assertThat(accountSettings.receiveRoutes()).isTrue();
  }

  // NOTE: This test validates the method in the interface.
  @Test
  public void testDefaultPeerSettingsOnInterface() {
    final AccountSettings accountSettings = this.asInterface(AccountRelationship.PEER);

    assertThat(accountSettings.sendRoutes()).isTrue();
    assertThat(accountSettings.receiveRoutes()).isTrue();
  }

  // NOTE: This test validates the method in the abstract base-class.
  @Test
  public void testDefaultParentAccountSettingsOnAbstractClass() {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(LinkType.of("foo"))
      .accountRelationship(AccountRelationship.PARENT)
      .assetCode("USD")
      .assetScale(9)
      .build();

    assertThat(accountSettings.sendRoutes()).isFalse();
    assertThat(accountSettings.receiveRoutes()).isTrue();
  }

  // NOTE: This test validates the method in the interface.
  @Test
  public void testDefaultParentSettingsOnInterface() {
    final AccountSettings accountSettings = this.asInterface(AccountRelationship.PARENT);

    assertThat(accountSettings.sendRoutes()).isFalse();
    assertThat(accountSettings.receiveRoutes()).isTrue();
  }

  private AccountSettings asInterface(final AccountRelationship accountRelationship) {
    return new AccountSettings() {
      @Override
      public AccountId accountId() {
        return null;
      }

      @Override
      public boolean isInternal() {
        return false;
      }


      @Override
      public AccountRelationship accountRelationship() {
        return accountRelationship;
      }


      @Override
      public LinkType linkType() {
        return null;
      }


      @Override
      public String assetCode() {
        return null;
      }


      @Override
      public int assetScale() {
        return 0;
      }


      @Override
      public Optional<UnsignedLong> maximumPacketAmount() {
        return Optional.empty();
      }


      @Override
      public AccountBalanceSettings balanceSettings() {
        return null;
      }


      @Override
      public Optional<SettlementEngineDetails> settlementEngineDetails() {
        return Optional.empty();
      }


      @Override
      public AccountRateLimitSettings rateLimitSettings() {
        return null;
      }


      @Override
      public Map<String, Object> customSettings() {
        return null;
      }
    };
  }
}
