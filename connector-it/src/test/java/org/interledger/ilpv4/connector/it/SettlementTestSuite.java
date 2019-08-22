package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.markers.Settlement;
import org.interledger.ilpv4.connector.it.settlement.TwoConnectorXrpSettlementIT;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used to run the `settlement` integration test suite.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(Settlement.class)
@Suite.SuiteClasses({TwoConnectorXrpSettlementIT.class}) // Note that Categories is a kind of Suite
public class SettlementTestSuite {
}
