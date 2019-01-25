package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;

import java.util.Comparator;
import java.util.Objects;

/**
 * A {@link Comparator} for comparing two instances of {@link IncomingRoute}.
 */
class RoutingTableEntryComparator implements Comparator<IncomingRoute> {
  private final AccountManager accountManager;

  public RoutingTableEntryComparator(final AccountManager accountManager) {
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  @Override
  public int compare(IncomingRoute entryA, IncomingRoute entryB) {
    // Null checks...
    if (entryA == null && entryB == null) {
      return 0;
    } else if (entryA == null) {
      return 1;
    } else if (entryB == null) {
      return -1;
    }

    // First sort by peer weight
    int weight1 = getWeight(entryA);
    int weight2 = getWeight(entryB);

    if (weight1 != weight2) {
      return weight2 - weight1;
    }

    // Then sort by path length
    int sizePathA = entryA.getPath().size();
    int sizePathB = entryB.getPath().size();

    if (sizePathA != sizePathB) {
      return sizePathA - sizePathB;
    }

    // Finally, tie-break by AccountId
    return entryA.getPeerAccountId().compareTo(entryB.getPeerAccountId());
  }

  /**
   * @param route
   *
   * @return
   *
   * @deprecated This check is relatively expensive since it involves loading the account details just to discover a
   * relationship, and then a weight. Consider a more efficient way to arrive at a similar tie-breaking mechanism. E
   * .g., consider storing the Weight in the route instead of in the account, as an optimization?
   */
  @Deprecated
  @VisibleForTesting
  protected int getWeight(final IncomingRoute route) {
    return this.accountManager.getAccount(route.getPeerAccountId())
      .orElseThrow(() -> new RuntimeException(
        String.format("Account should have existed: %s", route.getPeerAccountId())
      ))
      .getAccountSettings().getRelationship().getWeight();
  }
}
