package com.sappenin.ilpv4.connector.ccp.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Dynamic Configuration Protocol Core Codecs
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.sappenin.ilpv4.connector.ccp.CcpMode;
import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRouteControlRequest;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUintCodec;

import java.util.Objects;

/**
 * A Codec instances of {@link CcpRouteControlRequest} to and from ASN.1 OER.
 */
public class AsnCcpRouteControlRequestCodec extends AsnSequenceCodec<CcpRouteControlRequest> {

  /**
   * Default constructor.
   */
  public AsnCcpRouteControlRequestCodec() {
    super(
      new AsnUint8Codec(), // Mode
      new AsnUuidCodec(), // RoutingTableId (UUID)
      new AsnUint32Codec(), // The epoch
      new AsnFeaturesCodec() // Feature List.
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRouteControlRequest decode() {
    return ImmutableCcpRouteControlRequest.builder()
      .mode(CcpMode.fromInt(getValueAt(0)))
      .lastKnownRoutingTableId(getValueAt(1))
      .lastKnownEpoch(getValueAt(2))
      .features(getValueAt(3))
      .build();
  }

  /**
   * Encode the provided {@link CcpRouteControlRequest} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteControlRequest} to encode.
   */
  @Override
  public void encode(final CcpRouteControlRequest value) {
    Objects.requireNonNull(value);

    setValueAt(0, value.getMode().getValue());
    setValueAt(1, value.lastKnownRoutingTableId());
    setValueAt(2, value.lastKnownEpoch());
    setValueAt(3, value.features());
  }
}
