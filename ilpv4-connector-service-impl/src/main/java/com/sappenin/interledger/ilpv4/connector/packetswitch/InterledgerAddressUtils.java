package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A utility class that centralizes various business-logic decisions relating to {@link InterledgerAddress} and
 * Connector packet switching.
 */
public class InterledgerAddressUtils {

  private static final boolean EXTERNAL_FORWARDING_ALLOWED = true;
  private static final boolean EXTERNAL_FORWARDING_NOT_ALLOWED = false;
  private static final boolean ALLOWED = true;
  private static final boolean NOT_ALLOWED = false;

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountManager accountManager;

  public InterledgerAddressUtils(
    final Supplier<ConnectorSettings> connectorSettingsSupplier, final AccountManager accountManager
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  /**
   * Determines whether a packet destined for a particular address can be externally forwarded (i.e., forwarded to a
   * plugin connected to an external Connector).
   *
   * @param destinationAddress The {@link InterledgerAddress} representing the final destination of an ILPv4 packet.
   *
   * @return {@code true} if an address is eligible for public/external forwarding; {@code false} otherwise.
   */
  public boolean isExternalForwardingAllowed(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);

    // Must check the operating address first since it likely will be a PaymentNetwork address.
    if (destinationAddress.startsWith(connectorSettingsSupplier.get().getOperatorAddress())) {
      return EXTERNAL_FORWARDING_NOT_ALLOWED;
    } else if (
      // Short-circuit on the happy path using startsWith for performance reasons. Even if someone uses "goo.foo", and
      // if this doesn't blow up somewhere else, the routing-table won't accept it.
      isPaymentNetworkAddress(destinationAddress)
    ) {
      return EXTERNAL_FORWARDING_ALLOWED;
    } else {
      // if (destinationAddress.startsWith(InterledgerAddressPrefix.EXAMPLE.getValue())) {
      // if (destinationAddress.startsWith(InterledgerAddressPrefix.PRIVATE.getValue())) {
      // if (destinationAddress.startsWith(InterledgerAddressPrefix.PEER.getValue())) {
      // if (destinationAddress.startsWith(InterledgerAddressPrefix.SELF.getValue())) {
      // if (destinationAddress.startsWith(InterledgerAddressPrefix.LOCAL.getValue())) {
      return EXTERNAL_FORWARDING_NOT_ALLOWED;
    }
  }

  /**
   * Determines if an incoming packet is allowed into the Packet Switch from the specified accountId, when the packet is
   * destined to be sent to the indicated {@code destinationAddress}.
   *
   * @param sourceAccountId
   * @param destinationAddress
   *
   * @return {@code true} if the specified account is allowed to send packets to this switch using the specified
   * destination; {@code false} otherwise.
   */
  public boolean isDestinationAllowedFromAccount(
    final AccountId sourceAccountId, final InterledgerAddress destinationAddress
  ) {
    if (isPaymentNetworkAddress(destinationAddress)) {
      return ALLOWED;
    } else if (destinationAddress.startsWith(connectorSettingsSupplier.get().getOperatorAddress())) {
      return ALLOWED; // Ping allowed.
    } else if (destinationAddress.startsWith(InterledgerAddressPrefix.PRIVATE.getValue())) {
      // Only internal accounts can send to a `private` address prefix.
      return accountManager.isInternal(sourceAccountId).orElse(NOT_ALLOWED);
    } else if (destinationAddress.startsWith(InterledgerAddressPrefix.PEER.getValue())) {
      // Only external accounts can send to a `peer.` address prefix.
      return accountManager.isNotInternal(sourceAccountId).orElse(NOT_ALLOWED);
    } else if (destinationAddress.startsWith(InterledgerAddressPrefix.SELF.getValue())) {
      // Only internal accounts can send to a `self.` address prefix.
      return accountManager.isInternal(sourceAccountId).orElse(NOT_ALLOWED);
    } //else if (destinationAddress.startsWith(InterledgerAddressPrefix.LOCAL.getValue())) {
    //  REJECT: For now, this isn't supported.
    //}
    else {
      // `example` or any other address-prefixes: Not accepted
      return NOT_ALLOWED;
    }
  }

  /**
   * Helper to determine if an address is a "payment network" address (i.e., GLOBAL, or TEST(1,2,3)).
   *
   * @param destinationAddress
   *
   * @return
   */
  private boolean isPaymentNetworkAddress(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);
    return
      // Short-circuit on the happy path using startsWith for performance reasons. Even if someone uses "goo.foo", and
      // if this doesn't blow up somewhere else, the routing-table won't accept it.
      destinationAddress.startsWith(InterledgerAddressPrefix.GLOBAL.getValue()) ||
        // Do full equality checks here because performance is less important (we don't expect may TEST packets).
        destinationAddress.getAllocationScheme().equals(InterledgerAddressPrefix.TEST.getValue()) ||
        destinationAddress.getAllocationScheme().equals(InterledgerAddressPrefix.TEST1.getValue()) ||
        destinationAddress.getAllocationScheme().equals(InterledgerAddressPrefix.TEST2.getValue()) ||
        destinationAddress.getAllocationScheme().equals(InterledgerAddressPrefix.TEST3.getValue());
  }

}
