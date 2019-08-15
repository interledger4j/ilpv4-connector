package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.blast.TwoConnectorBlastPingTestIT;
import org.interledger.ilpv4.connector.it.markers.Performance;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used to run the `performance` integration test suite.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(Performance.class)
@Suite.SuiteClasses({TwoConnectorBlastPingTestIT.class}) // Note that Categories is a kind of Suite
public class PerformanceTestSuite {
}
