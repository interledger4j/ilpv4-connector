package com.sappenin.interledger.ilpv4.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;

@Value.Immutable
public interface AccountBalance {

  static ImmutableAccountBalance.Builder builder() {
    return ImmutableAccountBalance.builder();
  }

  /**
   * The {@link AccountId} for this account balance.
   *
   * @return
   */
  AccountId accountId();

  /**
   * The amount of units representing the net position this Connector operator holds with the account owner. A positive
   * balance indicates the Connector operator has an outstanding liability (i.e., owes money) to the account holder. A
   * negative balance represents an asset (i.e., the account holder owes money to the operator). This value is actually
   * the sum of the balance and the prepaid balance.
   *
   * @return An {@link BigInteger} representing the net balance of this account.
   */
  @Value.Derived
  default BigInteger netBalance() {
    final BigInteger netBalance = BigInteger.valueOf(balance());
    return netBalance.add(BigInteger.valueOf(prepaidAmount()));
  }

  /**
   * The amount of units representing the net position this Connector operator holds with the account owner. A positive
   * balance indicates the Connector operator has an outstanding liability (i.e., owes money) to the account holder. A
   * negative balance represents an asset (i.e., the account holder owes money to the operator).
   *
   * @return An {@link BigInteger} representing the net balance of this account.
   */
  long balance();

  /**
   * The number of units that the account holder has prepaid. This value is factored into the value returned by {@link
   * #netBalance()}, and is generally never negative.
   *
   * @return An {@link BigInteger} representing the number of units the counterparty (i.e., owner of this account) has
   * prepaid with this Connector.
   */
  long prepaidAmount();
}
