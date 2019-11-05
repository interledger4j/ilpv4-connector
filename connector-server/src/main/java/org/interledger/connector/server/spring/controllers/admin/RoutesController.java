package org.interledger.connector.server.spring.controllers.admin;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.routing.Route;
import org.interledger.connector.server.spring.controllers.PathConstants;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Set;

@RestController(PathConstants.SLASH_ROUTES)
public class RoutesController {

  /**
   * Notes from Rust:
   *
   * GET /routes
   * - look up the entire routing table
   * - look up all the accounts associated with those routes
   * - map the routes to the accounts
   *
   * PUT /routes/static
   * - Body: Map of ILP Address prefix -> Username
   * - accepts routes: HashMap<String, Username>
   * - grabs all the values (usernames) from the map, then looks up the account ids
   * - maps all the keys (string) to the account ids
   *
   * PUT /routes/static/:prefix
   * - Body: Username
   * - looks up the account id from the username
   * - sets the prefix to that account id
   *
   * It appears that the term "prefix" is actually the full interledger address since that's the first part of the
   * payment pointer?
   *
   * Routing table defined as:
   * routing_table: Arc<RwLock<Arc<HashMap<String, AccountId>>>>,
   *
   * Where is the username factoring in? Is account in rust the same as account settings in java? Is the account id
   * just a long?
   *
   * Looks like rust has a table called username that associated account ids with username?
   *
   *
   *
   * Also in Rust...
   * /// We store a routing table for each peer we receive Route Update Requests from.
   *     /// When the peer sends us an update, we apply that update to this view of their table.
   *     /// Updates from peers are applied to our local_table if they are better than the
   *     /// existing best route and if they do not attempt to overwrite configured routes.
   *     incoming_tables: Arc<RwLock<HashMap<A::AccountId, RoutingTable<A>>>>,
   */

  @RequestMapping(
    path = PathConstants.SLASH_ROUTES,
    method = RequestMethod.GET,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<Set<Route>> getRoutes() {
    return null;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC,
      method = RequestMethod.PUT,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<Set<Route>> updateStaticRoutes(@RequestBody Set<Route> routes) {
    return null;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.PUT,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<Set<Route>> updateStaticRoutesAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix,
                                                           @RequestBody Set<Route> routes) {
    return null;
  }

  @RequestMapping(
      path = PathConstants.SLASH_ROUTES_STATIC_PREFIX,
      method = RequestMethod.DELETE,
      consumes = {APPLICATION_JSON_VALUE},
      produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<Set<Route>> deleteStaticRoutesAtPrefix(@PathVariable(PathConstants.PREFIX) String prefix) {
    return null;
  }


}
