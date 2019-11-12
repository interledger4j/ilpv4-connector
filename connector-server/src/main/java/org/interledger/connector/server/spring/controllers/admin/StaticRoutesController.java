package org.interledger.connector.server.spring.controllers.admin;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.routing.Route;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.routing.StaticRouteUnprocessableProblem;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.core.InterledgerAddressPrefix;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Collection;
import java.util.Set;

@RestController(PathConstants.SLASH_ROUTES)
public class StaticRoutesController {

  private final ExternalRoutingService externalRoutingService;

  public StaticRoutesController(ExternalRoutingService externalRoutingService) {
    this.externalRoutingService = externalRoutingService;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES,
      method = RequestMethod.GET,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Collection<Route> getRoutes() {
    // FIXME add paging
    return externalRoutingService.getAllRoutes();
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC,
      method = RequestMethod.GET,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Set<StaticRoute> getStaticRoutes() {
    return this.externalRoutingService.getAllStaticRoutes();
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.PUT,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<StaticRoute> createStaticRouteAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix,
                                                               @RequestBody StaticRoute staticRoute) {
    if (!prefix.equals(staticRoute.addressPrefix().getValue())) {
      throw new StaticRouteUnprocessableProblem(prefix, staticRoute.addressPrefix());
    }
    return new ResponseEntity<>(this.externalRoutingService.createStaticRoute(staticRoute), HttpStatus.CREATED);
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.DELETE,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity deleteStaticRouteAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix) {
    this.externalRoutingService.deleteStaticRouteByPrefix(InterledgerAddressPrefix.of(prefix));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

}
