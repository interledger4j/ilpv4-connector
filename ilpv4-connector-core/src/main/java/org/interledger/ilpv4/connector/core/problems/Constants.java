package org.interledger.ilpv4.connector.core.problems;

import org.interledger.core.InterledgerAddress;

public interface Constants {

  // The address given to a Connector that
  InterledgerAddress UNSET_OPERATOR_ADDRESS =
    InterledgerAddress.of(InterledgerAddress.AllocationScheme.PRIVATE.getValue() + ".unset-operator-address");

}
