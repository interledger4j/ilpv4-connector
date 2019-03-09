package org.interledger.connector.link.blast;

import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactory;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerAddress;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating BTP Links.
 */
public class BlastLinkFactory implements LinkFactory {

  private final RestTemplate restTemplate;

  public BlastLinkFactory(final RestTemplate restTemplate) {
    this.restTemplate = Objects.requireNonNull(restTemplate);
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(linkSettings);

    if (!this.supports(linkSettings.getLinkType())) {
      throw new RuntimeException(
        String.format("LinkType `%s` not supported by this factory!", linkSettings.getLinkType())
      );
    }

    // Translate from Link.customSettings, being sure to apply custom settings from the incoming link.
    final ImmutableBlastLinkSettings.Builder builder = BlastLinkSettings.builder().from(linkSettings);
    final BlastLinkSettings blastLinkSettings =
      BlastLinkSettings.applyCustomSettings(builder, linkSettings.getCustomSettings()).build();

    final BlastLink blastLink = new BlastLink(
      operatorAddressSupplier,
      ModifiableBlastLinkSettings.create().from(blastLinkSettings), // Modifiable for testing
      restTemplate
    );

    // TODO
    //        // Alert any link listeners that a new link was constructed...
    //        eventPublisher.publishEvent(LinkConstructedEvent.builder()
    //          .message(String.format("Link constructed for `%s`", linkSettings.getAccountAddress().getValue()))
    //          .object(link)
    //          .build());

    return blastLink;
  }

  @Override
  public boolean supports(LinkType linkType) {
    return BlastLink.LINK_TYPE.equals(linkType);
  }

}
