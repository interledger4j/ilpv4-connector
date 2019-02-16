package org.interledger.connector.link;

/**
 * A factory for constructing instances of {@link Link} based upon configured settings.
 */
public interface LinkFactory {

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  Link<?> constructLink(LinkSettings linkSettings);

  /**
   * Helper method to apply custom settings on a per-link-type basis.
   *
   * @param linkSettings
   *
   * @return
   */
  default LinkSettings applyCustomSettings(LinkSettings linkSettings) {
    return linkSettings;
  }

  /**
   * Determines if this factory support a particular type of {@link LinkType}.
   *
   * @param linkType A {@link LinkType} to check compatibility for.
   *
   * @return {@code true} if this factory supports the specified linkType; {@code false} otherwise.
   */
  boolean supports(LinkType linkType);

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  default <PS extends LinkSettings, P extends Link<PS>> P constructLink(
    final Class<P> $, final PS linkSettings
  ) {
    return (P) this.constructLink(linkSettings);
  }
}
