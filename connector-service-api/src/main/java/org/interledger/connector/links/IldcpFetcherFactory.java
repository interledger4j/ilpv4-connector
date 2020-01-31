package org.interledger.connector.links;

import org.interledger.ildcp.IldcpFetcher;
import org.interledger.link.Link;

/**
 * Factory to create {@link IldcpFetcher}
 */
public interface IldcpFetcherFactory {

  /**
   * Create {@link IldcpFetcher} for a give link
   * @param link
   * @return
   */
  IldcpFetcher construct(Link link);
}
