package org.interledger.connector.it;

import org.interledger.connector.it.ilpoverhttp.TwoConnectorIlpOverHttpPingTestIT;
import org.interledger.connector.it.ilpoverhttp.TwoConnectorIldcpTestIT;
import org.interledger.connector.it.ilpoverhttp.TwoConnectorMixedAssetCodeTestIT;
import org.interledger.connector.it.markers.IlpOverHttp;
import org.interledger.connector.it.markers.Performance;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used to run the `default` integration test suite.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory({Performance.class, IlpOverHttp.class})
// Note that Categories is a kind of Suite
@Suite.SuiteClasses({TwoConnectorIlpOverHttpPingTestIT.class, TwoConnectorIldcpTestIT.class,
    TwoConnectorMixedAssetCodeTestIT.class})
public class DefaultTestSuite {
}
