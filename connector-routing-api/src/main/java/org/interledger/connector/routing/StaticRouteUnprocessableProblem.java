package org.interledger.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class StaticRouteUnprocessableProblem extends StaticRouteProblem {

  public StaticRouteUnprocessableProblem(String urlPrefix, InterledgerAddressPrefix entityPrefix) {
    super(
        URI.create(TYPE_PREFIX + STATIC_ROUTES_PATH + "/static-route-unprocessable"),
        "Static Route Unprocessable [entityPrefix: `" + entityPrefix.getValue() + "`, urlPrefix: `" +
            urlPrefix + "`]",
        Status.UNPROCESSABLE_ENTITY,
        Objects.requireNonNull(entityPrefix)
    );
  }

}
