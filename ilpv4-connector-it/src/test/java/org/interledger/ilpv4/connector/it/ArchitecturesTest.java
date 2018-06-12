package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.it.graph.Graph;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test to verify that the BeerCoin graph is working properly.
 */
public class ArchitecturesTest {

  private static final Logger logger = LoggerFactory.getLogger(ArchitecturesTest.class);
  private static Graph graph = Architectures.beerCoin();

  @BeforeClass
  public static void setup() {
    graph.start();
  }

  @AfterClass
  public static void shutdown() {
    graph.stop();
  }

  @Test
  public void testName() {
    logger.info("It works!");
  }
}
