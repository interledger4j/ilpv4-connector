package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link AccountId}.
 */
public class AccountIdTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testNull() {
    expectedException.expect(NullPointerException.class);
    AccountId.of(null);
  }

  @Test
  public void testColons() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException
      .expectMessage("AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', '.' or '~'");
    AccountId.of("foo:bar");
  }

  @Test
  public void testNonAscii() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException
      .expectMessage("AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', '.' or '~'");
    AccountId.of("hànzìBopomofoㄏㄢㄗ");
  }

  @Test
  public void testForwardSlash() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException
      .expectMessage("AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', '.' or '~'");
    AccountId.of("foo/bar");
  }

  @Test
  public void testBackSlash() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException
      .expectMessage("AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', '.' or '~'");
    AccountId.of("foo\bar");
  }

  @Test
  public void testCapitalization() {
    assertThat(AccountId.of("Foo").value()).isEqualTo("foo");
    assertThat(AccountId.of("FOO").value()).isEqualTo("foo");
    assertThat(AccountId.of("TITLE").value()).isEqualTo("title");
    assertThat(AccountId.of("ABCDEFGHIJKLMNOPQRSTUVWXYZ").value()).isEqualTo("abcdefghijklmnopqrstuvwxyz");
  }

  @Test
  public void testValidCharset() {
    assertThat(AccountId.of("abcdefghijklmnopqrstuvwxyz-_.foo.bar").value())
      .isEqualTo("abcdefghijklmnopqrstuvwxyz-_.foo.bar");
    assertThat(AccountId.of("0123456789_-.").value()).isEqualTo("0123456789_-.");
  }

  @Test
  public void testTooLong() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException.expectMessage("AccountIds ");
    AccountId.of("AccountId must not be longer than 64 characters").value();
  }
}
