package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link AccountRelationship}.
 */
public class AccountRelationshipTest {

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidNegativeWeight() {
    try {
      AccountRelationship.fromWeight(-1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid `weight`: -1");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidWeight() {
    try {
      AccountRelationship.fromWeight(4);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid `weight`: 4");
      throw e;
    }
  }

  @Test
  public void fromWeight() {
    assertThat(AccountRelationship.fromWeight(0)).isEqualTo(AccountRelationship.PARENT);
    assertThat(AccountRelationship.fromWeight(1)).isEqualTo(AccountRelationship.PEER);
    assertThat(AccountRelationship.fromWeight(2)).isEqualTo(AccountRelationship.CHILD);
  }

  @Test
  public void getWeight() {
    assertThat(AccountRelationship.PARENT.getWeight()).isEqualTo(0);
    assertThat(AccountRelationship.PEER.getWeight()).isEqualTo(1);
    assertThat(AccountRelationship.CHILD.getWeight()).isEqualTo(2);
  }
}
