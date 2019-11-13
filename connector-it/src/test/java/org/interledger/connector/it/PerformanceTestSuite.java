package org.interledger.connector.it;

import org.interledger.connector.it.ilpoverhttp.TwoConnectorIlpOverHttpPingTestIT;
import org.interledger.connector.it.markers.Performance;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used to run the `performance` integration test suite.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(Performance.class)
@Suite.SuiteClasses({TwoConnectorIlpOverHttpPingTestIT.class}) // Note that Categories is a kind of Suite
public class PerformanceTestSuite {
}
