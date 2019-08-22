package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.blast.TwoConnectorBlastPingTestIT;
import org.interledger.ilpv4.connector.it.blast.TwoConnectorIldcpTestIT;
import org.interledger.ilpv4.connector.it.markers.IlpOverHttp;
import org.interledger.ilpv4.connector.it.markers.Performance;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used to run the `default` integration test suite.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory({Performance.class, IlpOverHttp.class})
// Note that Categories is a kind of Suite
@Suite.SuiteClasses({TwoConnectorBlastPingTestIT.class, TwoConnectorIldcpTestIT.class})
public class DefaultTestSuite {
}
