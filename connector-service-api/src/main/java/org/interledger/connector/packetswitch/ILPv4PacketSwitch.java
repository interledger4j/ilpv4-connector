package org.interledger.connector.packetswitch;

import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.concurrent.CompletableFuture;

/**
 * <p>A switching fabric for mapping incoming ILP packets to outgoing links.</p>
 *
 * <p>A packet switch allows for one or more instances of {@link PacketSwitchFilter} to be applied to every Packet
 * that traverses the switch in order to adjust the packet or apply custom processing logic before the packet reaches
 * each section of the routing fabric.</p>
 *
 * <p>In the happy path, a packet will come into the connector from a particular {@link Link} and then will
 * be handed off to this switch to be routed to a (typically) different target {@link Link}. This call will result in
 * either an {@link InterledgerFulfillPacket} or an {@link InterledgerRejectPacket}, which can then be returned back to
 * the original caller via the {@link Link} that originated the {@link InterledgerPreparePacket}.</p>
 *
 * <p>The following diagram illustrates this flow:</p>
 * <pre>
 *                              ┌────────────────────────────────────────────────────────────────────────────────────────────┐
 *                              │                                 INCOMING LINK FILTER CHAIN                                 │
 *                              │                         Incoming Account: `abc` (`example.alice`)                          │
 *                              └────────────────────────────────────────────────────────────────────────────────────────────┘
 *                        ┌────────────┐        ┌──────────┐        ┌──────────┐        ┌──────────┐        ┌──────────┐
 *                    Prepare          │        │          │        │          │        │          │        │          │
 *                        │            ▽        │          ▽        │          ▽        │          ▽        │          ▽
 *                        │     ┌────────────┐  │   ┌────────────┐  │   ┌────────────┐  │   ┌────────────┐  │   ┌────────────┐
 * ┌─────────────────┐    │     │            │  │   │            │  │   │ Max Packet │  │   │  Allowed   │  │   │Adjust `abc`│   To Packet
 * │  ILP-over-HTTP  │────┘ ─ ─ │ Rate Limit │──┘┌ ─│Expiry Check│──┘┌ ─│   Amount   │──┘┌ ─│Destination │──┘┌ ─│  Balance   │────Switch────▷
 * └─────────────────┘     │    │            │      │            │      │            │      │            │      │            │
 *          △                   └────────────┘   │  └────────────┘   │  └────────────┘   │  └────────────┘   │  └────────────┘
 *                   Fulfill /         △                   △                   △                   △                   △
 *          └ ─ ─ ─ ─ ─Reject          │   Fulfill /       │   Fulfill /       │Fulfill /          │   Fulfill /       │
 *                                      ─ ─ ─Reject         ─ ─ ─Reject         ─ Reject─           ─ ─ ─Reject
 *                                                                                                                     │
 *
 *                                                                                                                     │
 *
 *            Fulfill /                                                                                                │
 *      ─ ─ ─ ─ Reject─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 *     │                          ┌───────────────────────────────────────────────────────────────────────────────────────────┐
 *                                │                                       PACKET SWITCH                                       │
 *     │                          │                                  IncomingAccount: `abc`                                   │
 *                                │                                  OutgoingAccount: `123`                                   │
 *     │                          └───────────────────────────────────────────────────────────────────────────────────────────┘
 *                           ┌───────────┐                ┌──────────────────┐                    ┌─────────────────────┐
 *     │                 Prepare         │                │                  │                    │                     │
 *                           │           ▼                │                  ▽                    │                     ▼
 *     │  ┌──────────────┐   │    ┌────────────┐          │           ┌────────────┐              │              ┌────────────┐
 *        │     From     │   │    │Get Next Hop│          │           │            │              │              │  Compute   │
 *     └ ─│ FilterChain  │───┘ ─ ─│Account from│──────────┘┌ ─ ─ ─ ─ ─│ Compute FX │──────────────┼ ─ ─ ─ ─ ─ ─ ─│   Expiry   │─To Outgoing──▷
 *        │              │    │   │   Router   │                      │            │                             │            │
 *        └──────────────┘        └────────────┘           │          └────────────┘              │              └────────────┘
 *                △           │          ▲                                   △                                          △
 *                    Fulfill /          │            Fulfill /                         Fulfill / │
 *                └ ─ ─ Reject┘           ─ ─ ─ ─ ─ ─ ─ Reject               └ ─ ─ ─ ─ ─ ─Reject ─                      │
 *
 *                                                                                                                      │
 *
 *                                                                                                                      │
 *                                                         Fulfill /
 *        ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─Reject ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *       │
 *                                ┌────────────────────────────────────────────────────────────────────────────────────────────┐
 *       │                        │                                 OUTGOING LINK FILTER CHAIN                                 │
 *                                │                               Account: `123` (`example.bob`)                               │
 *       │                        └────────────────────────────────────────────────────────────────────────────────────────────┘
 *                             ┌──────────┐                  ┌────────────────────┐                  ┌────────────────────┐
 *       │                 Prepare        │              Prepare                  │                  │                    │
 *                             │          ▼                  │                    ▼                  │                    ▼
 *       │  ┌──────────────┐   │   ┌────────────┐            │             ┌────────────┐        Prepare           ┌────────────┐
 *          │     From     │   │   │            │            │             │Adjust `123`│            │             │ Forward on │
 *       └ ─│ PacketSwitch │───┘─ ─│ Throughput │────────────┘┌ ─ ─ ─ ─ ─ ─│  Balance   │────────────┘┌ ─ ─ ─ ─ ─ ─│    Link    │────────────▶
 *          │              │   │   │            │                          │            │                          │            │
 *          └──────────────┘       └────────────┘             │            └────────────┘             │            └────────────┘
 *                  △          │          △         Fulfill /                     △       Fulfill /
 *                  │ Fulfill /           │           Reject  │                   │        Reject     │
 *                   ─ ─Reject ┘           ─ ─ ─ ─ ─ ─ ─ ─ ─ ─                     ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 *
 * </pre>
 */
public interface ILPv4PacketSwitch {

  /**
   * <p>Routes an incoming ILPv4 request packet to a connected peer and returns the response packet (if one is
   * returned).</p>
   *
   * <p>This method supports one of two responses, which can be handled by using utility classes such as {@link
   * InterledgerResponsePacketMapper} or {@link InterledgerResponsePacketHandler}:
   * </p>
   *
   * <pre>
   * <ol>
   *   <ul>An instance of {@link InterledgerFulfillPacket}, which means the packet was fulfilled by the receiver.</ul>
   *   <ul>An instance of {@link InterledgerRejectPacket}, which means the packet was rejected by one of the nodes in
   *   the payment path.</ul>
   * </ol>
   * </pre>
   *
   * Note that in the ILPv4 protocol flow, a packet might timeout before the switching fabric receives an affirmative
   * fulfill or reject response. In these cases, an exception will be thrown (e.g., by an instance of
   * ExpiryPacketFilter). The packet switch will catch this exception and map it to an {@link InterledgerRejectPacket}
   * using this Connector's ILP address as the triggered-by property. Such a rejection will have a special error code
   * (i.e., {@link InterledgerErrorCode#R00_TRANSFER_TIMED_OUT}) which will allow a downstream connector to distinguish
   * the difference.
   *
   * @param accountId             The {@link AccountId} to send this packet from.
   * @param incomingPreparePacket An incoming {@link InterledgerPreparePacket} that should be routed to the most
   *                              appropriate peer connected to this Connector.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  InterledgerResponsePacket switchPacket(
    AccountId accountId, InterledgerPreparePacket incomingPreparePacket
  );
}
