package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author jfulton
 */
public class ArchitecturesTest {

    private static final Logger logger = LoggerFactory.getLogger(ArchitecturesTest.class);
    private Graph graph = Architectures.simple();

    @BeforeClass
    public void setup() {
        graph.start();
    }

    @AfterClass
    public void shutdown() {
        graph.stop();
    }

    @Test
    public void testName() throws Exception {
        logger.info("It works!");
    }
}
