package org.interledger.connector.accounts;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * <p>A wrapper that defines a unique identifier for an account. Because accountIds need to be usable in URL paths,
   * this implementation only allows characters that are allowed in section 5 of RFC-4648 called "Base 64 Encoding with
   * URL and Filename Safe Alphabet," plus periods. In other words, the following US-ASCII characters are allowed:
   * ('A–Z', 'a–z', '0–9', '-', '_', '.' and '~') with the total character count not to exceed 64 characters.</p>
   *
   * <p>It is important to note that while capitalized US-ASCII characters are allowed when constructing an AccountId,
   * this implementation lower-cases these characters. Thus, even though capital letters are allowed in initial input,
   * they are not ultimately preserved once an {@link AccountId} is created.</p>
   *
   * <p>Likewise, dot characters ('.') are accepted but then removed from any accountId. This is to replicate the
   * thinking in Google's decition to make dots not matter in gmail. This helps prevent user confusion in use-cases
   * where PaymentPointers directly map to a Connector's account identifier (even though this is an optional deployment
   * strategy).</p>
   *
   * <p>These design choices were made to ensure that any account identifier can be easily and correctly used
   * in a URL path regardless of capitalization, such as when operating on the identifier using HTTP APIs.</p>
   *
   * <p>Finally, it should be noted that '+' characters are not allowed in an AccountId. This is because it is
   * anticipated that plus-symbols will be used in payment-pointers to provide user-facing context, such as a currency
   * or for anti-spam measures similar to the gmail's use of these symbols in email addresses. Thus, this character is
   * disallowed to ensure clarity between user-facing augmentations of an existing accountId and the id itself.</p>
   *
   * @see "https://tools.ietf.org/html/rfc4648#section-5"
   */
  @Value.Immutable(intern = true)
  @Wrapped
  @JsonSerialize(as = AccountId.class)
  @JsonDeserialize(as = AccountId.class)
  static abstract class _AccountId extends Wrapper<String> implements Serializable {

    // Represents section 5 of RFC-4684, "Base 64 Encoding with URL and Filename Safe Alphabet", plus periods and tilde.
    // Capital letters are allowed, but later lower-cased during normalization.
    private static final Pattern ALLOWED_CHARS_PATTERN = Pattern.compile("^([A-Za-z0-9\\-_\\.\\~])+$");

    @Override
    public String toString() {
      return this.value();
    }

    @Value.Check
    public _AccountId enforceSize() {
      if (this.value().length() > 64) {
        throw new InvalidAccountIdProblem("AccountId must not be longer than 64 characters");
      } else {
        return this;
      }
    }

    /**
     * Ensures that an accountId only ever contains lower-cased US-ASCII letters (not upper-cased).
     *
     * @return A normalized {@link AccountId}.
     *
     * @see "https://github.com/interledger4j/ilpv4-connector/issues/623"
     */
    @Value.Check
    public _AccountId normalizeToDotsDontMatter() {
      if (this.value().contains(".")) {
        return AccountId.of(this.value().replace(".", ""));
      } else {
        return this;
      }
    }

    /**
     * Ensures that an accountId only ever contains lower-cased US-ASCII letters (not upper-cased).
     *
     * @return A normalized {@link AccountId}.
     *
     * @see "https://github.com/interledger4j/ilpv4-connector/issues/623"
     */
    @Value.Check
    public _AccountId normalizeToLowercase() {
      if (this.value().chars().anyMatch(Character::isUpperCase)) {
        return AccountId.of(this.value().toLowerCase(Locale.ENGLISH));
      } else {
        return this;
      }
    }

    /**
     * Ensures that an accountId only contains valid characters. This implementation only allows characters that are
     * allowed in section 5 of RFC-4684 ("Base 64 Encoding with URL and Filename Safe Alphabet"), plus periods. In other
     * words, the following US-ASCII characters are allowed: ('A–Z', 'a–z', '0–9', '-', '_', '.' and '~').
     *
     * @return A normalized {@link AccountId}.
     *
     * @see "https://tools.ietf.org/html/rfc4648#section-5"
     * @see "https://github.com/interledger4j/ilpv4-connector/issues/623"
     */
    @Value.Check
    public _AccountId enforceValidChars() {
      Matcher m = ALLOWED_CHARS_PATTERN.matcher(this.value());
      if (m.matches()) {
        return this;
      } else {
        throw new InvalidAccountIdProblem(
          "AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', or '~'"
        );
      }
    }
  }

  /**
   * A wrapper that defines a unique identifier for a settlement engine account.
   */
  @Value.Immutable(intern = true)
  @Wrapped
  @JsonSerialize(as = SettlementEngineAccountId.class)
  @JsonDeserialize(as = SettlementEngineAccountId.class)
  static abstract class _SettlementEngineAccountId extends Wrapper<String> implements Serializable {

    @Override
    public String toString() {
      return this.value();
    }

    @Value.Check
    public _SettlementEngineAccountId enforceSize() {
      Preconditions.checkArgument(
        this.value().length() < 64,
        "SettlementEngineAccountId must not be longer than 64 characters!"
      );
      return this;
    }

  }

}
