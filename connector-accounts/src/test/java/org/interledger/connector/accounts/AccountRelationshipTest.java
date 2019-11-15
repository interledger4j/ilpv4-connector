package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link AccountRelationship}.
 */
public class AccountRelationshipTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fromInvalidNegativeWeight() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid `weight`: -1");
    AccountRelationship.fromWeight(-1);
  }

  @Test
  public void fromInvalidWeight() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid `weight`: 4");
    AccountRelationship.fromWeight(4);
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
