package org.interledger.connector.server.spring.controllers.admin;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.routes.StaticRoutesManager;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.core.InterledgerAddressPrefix;

import com.google.common.collect.Sets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Set;

@RestController(PathConstants.SLASH_ROUTES)
public class StaticRoutesController {

  private final StaticRoutesManager staticRoutesManager;

  private final ExternalRoutingService externalRoutingService;

  public StaticRoutesController(StaticRoutesManager staticRoutesManager, ExternalRoutingService externalRoutingService) {
    this.staticRoutesManager = staticRoutesManager;
    this.externalRoutingService = externalRoutingService;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES,
      method = RequestMethod.GET,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Set<StaticRoute> getRoutes() {
    Set<StaticRoute> routes = Sets.newHashSet(this.staticRoutesManager.getAllRoutesUncached());
    // FIXME probably shouldn't be mapping non-static routes as a static route, but makes transport easier for meow
    externalRoutingService.getLocalRoutingTable()
        .forEach((prefix, route) -> routes.add(StaticRoute.builder().prefix(prefix).accountId(route.nextHopAccountId()).build()));
    return routes;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC,
      method = RequestMethod.GET,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Set<StaticRoute> getStaticRoutes() {
    return this.staticRoutesManager.getAllRoutesUncached();
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.PUT,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<StaticRoute> updateStaticRouteAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix,
                                                               @RequestBody StaticRoute staticRoute) {
    return new ResponseEntity<>(this.staticRoutesManager.update(staticRoute), HttpStatus.CREATED);
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.DELETE,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void deleteStaticRouteAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix) {
    this.staticRoutesManager.deleteByPrefix(InterledgerAddressPrefix.of(prefix));
  }

}
