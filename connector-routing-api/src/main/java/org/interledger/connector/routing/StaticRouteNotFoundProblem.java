package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if no static route exists for the given prefix
 */
public class StaticRouteNotFoundProblem extends StaticRouteProblem {

  public StaticRouteNotFoundProblem(InterledgerAddressPrefix prefix) {
    super(
        URI.create(TYPE_PREFIX + STATIC_ROUTES_PATH + "/static-route-not-found"),
        "Static Route Does Not Exist (`" + prefix.getValue() + "`)",
        Status.NOT_FOUND,
        Objects.requireNonNull(prefix)
    );
  }
}
