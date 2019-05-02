package org.interledger.connector.accounts;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

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
      assertThat(e.getMessage(), is("Invalid `weight`: -1"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidWeight() {
    try {
      AccountRelationship.fromWeight(4);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Invalid `weight`: 4"));
      throw e;
    }
  }

  @Test
  public void fromWeight() {
    assertThat(AccountRelationship.fromWeight(0), is(AccountRelationship.PARENT));
    assertThat(AccountRelationship.fromWeight(1), is(AccountRelationship.PEER));
    assertThat(AccountRelationship.fromWeight(2), is(AccountRelationship.CHILD));
    assertThat(AccountRelationship.fromWeight(3), is(AccountRelationship.LOCAL));
  }

  @Test
  public void getWeight() {
    assertThat(AccountRelationship.PARENT.getWeight(), is(0));
    assertThat(AccountRelationship.PEER.getWeight(), is(1));
    assertThat(AccountRelationship.CHILD.getWeight(), is(2));
    assertThat(AccountRelationship.LOCAL.getWeight(), is(3));
  }
}