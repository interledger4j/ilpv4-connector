package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown on a request to create static route for an already existing prefix
 */
public class StaticRouteAlreadyExistsProblem extends StaticRouteProblem {

  public StaticRouteAlreadyExistsProblem(InterledgerAddressPrefix prefix) {
    super(
        URI.create(TYPE_PREFIX + STATIC_ROUTES_PATH + "/static-route-already-exists"),
        "Static Route Already Exists (`" + prefix.getValue() + "`)",
        Status.CONFLICT,
        Objects.requireNonNull(prefix)
    );
  }
}
