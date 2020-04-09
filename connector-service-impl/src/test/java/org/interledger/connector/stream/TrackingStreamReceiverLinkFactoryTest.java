package org.interledger.connector.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.FulfillmentGeneratedEventAggregator;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.PacketRejector;
import org.interledger.link.exceptions.LinkException;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link TrackingStreamReceiverLinkFactory}.
 */
public class TrackingStreamReceiverLinkFactoryTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private final LinkId linkId = LinkId.of("foo");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private LinkSettings linkSettingsMock;

  @Mock
  private PacketRejector packetRejectorMock;

  @Mock
  private TrackingStreamReceiverSupplier trackingStreamReceiverSupplierMock;

  private TrackingStreamReceiverLinkFactory trackingStreamReceiverLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.trackingStreamReceiverLinkFactory = new TrackingStreamReceiverLinkFactory(
      packetRejectorMock,
      trackingStreamReceiverSupplierMock
    );
  }

  @Test
  public void constructWithNulPacketRejector() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("packetRejector must not be null");

    new TrackingStreamReceiverLinkFactory(null, trackingStreamReceiverSupplierMock);
  }

  @Test
  public void constructWithNulTrackingStreamReceiver() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("trackingStreamReceiverSupplier must not be null");

    new TrackingStreamReceiverLinkFactory(packetRejectorMock, null);
  }

  @Test
  public void supports() {
    assertThat(trackingStreamReceiverLinkFactory.supports(TrackingStreamReceiverLink.LINK_TYPE)).isEqualTo(true);
    assertThat(trackingStreamReceiverLinkFactory.supports(LinkType.of("foo"))).isEqualTo(false);
  }

  @Test
  public void constructLinkWithNullOperator() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("operatorAddressSupplier must not be null");

    trackingStreamReceiverLinkFactory.constructLink(null, linkSettingsMock);
  }

  @Test
  public void constructLinkWithNullLinkSettings() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("linkSettings must not be null");

    trackingStreamReceiverLinkFactory.constructLink(() -> OPERATOR_ADDRESS, null);
  }

  @Test
  public void constructLinkWithUnsupportedLinkType() {
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("LinkType not supported by this factory. linkType=LinkType(FOO)");

    LinkSettings linkSettings = LinkSettings.builder()
      .linkType(LinkType.of("foo"))
      .build();
    trackingStreamReceiverLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
  }

  @Test
  public void constructLink() {
    AccountId bob = AccountId.of("bob");
    TrackingStreamReceiverLinkSettings linkSettings = TrackingStreamReceiverLinkSettings.builder()
      .accountId(bob)
      .assetCode("USD")
      .assetScale((short) 9)
      .build();

    when(trackingStreamReceiverSupplierMock.get(linkSettings)).thenReturn(new TrackingStreamReceiver(
      mock(ServerSecretSupplier.class),
      mock(StreamConnectionGenerator.class),
      mock(StreamEncryptionService.class),
      mock(CodecContext.class),
      bob,
      mock(FulfillmentGeneratedEventAggregator.class)
    ));

    TrackingStreamReceiverLink link = (TrackingStreamReceiverLink) trackingStreamReceiverLinkFactory
      .constructLink(() -> OPERATOR_ADDRESS, linkSettings);

    assertThat(link.getLinkSettings().accountId()).isEqualTo(bob);
    assertThat(link.getLinkSettings().assetCode()).isEqualTo("USD");
    assertThat(link.getLinkSettings().assetScale()).isEqualTo(9);
    assertThat(link.getOperatorAddressSupplier().get()).isEqualTo(OPERATOR_ADDRESS);
  }
}
