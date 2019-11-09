package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class StaticRouteAlreadyExistsProblem extends StaticRouteProblem {

  public StaticRouteAlreadyExistsProblem(InterledgerAddressPrefix prefix) {
    super(
        URI.create(TYPE_PREFIX + STATIC_ROUTES_PATH + "/static-route-already-exists"),
        "Route Already Exists (`" + prefix.getValue() + "`)",
        Status.CONFLICT,
        Objects.requireNonNull(prefix)
    );
  }
}
