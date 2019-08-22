package org.interledger.connector.link.blast;

import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.interledger.connector.link.blast.BlastLinkSettings.SHARED_SECRET;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_SUBJECT;
import static org.interledger.connector.link.blast.BlastLinkSettings.AUTH_TYPE;
import static org.interledger.connector.link.blast.BlastLinkSettings.BLAST;
import static org.interledger.connector.link.blast.BlastLinkSettings.INCOMING;
import static org.interledger.connector.link.blast.BlastLinkSettings.OUTGOING;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_EXPIRY;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_ISSUER;
import static org.interledger.connector.link.blast.BlastLinkSettings.URL;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_URL;

public abstract class AbstractBlastLinkTest {

  protected Map<String, Object> customSettingsFlat() {
    return ImmutableMap.<String, Object>builder()
      .put(BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name())
      .put(BLAST_INCOMING_TOKEN_ISSUER, "https://incoming-issuer.example.com/")
      .put(BLAST_INCOMING_SHARED_SECRET, "incoming-credential")
      .put(BLAST_INCOMING_TOKEN_AUDIENCE, "https://incoming-audience.example.com/")

      .put(BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name())
      .put(BLAST_OUTGOING_TOKEN_SUBJECT, "outgoing-subject")
      .put(BLAST_OUTGOING_SHARED_SECRET, "outgoing-credential")
      .put(BLAST_OUTGOING_TOKEN_ISSUER, "https://outgoing-issuer.example.com/")
      .put(BLAST_OUTGOING_TOKEN_AUDIENCE, "https://outgoing-audience.example.com/")
      .put(BLAST_OUTGOING_TOKEN_EXPIRY, Duration.ofDays(1).toString())
      .put(BLAST_OUTGOING_URL, "https://outgoing.example.com")

      .build();
  }

  protected Map<String, Object> customSettingsHeirarchical() {
    final Map<String, Object> incomingMap = new HashMap<>();
    incomingMap.put(AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
    incomingMap.put(TOKEN_SUBJECT, "incoming-subject");
    incomingMap.put(SHARED_SECRET, "incoming-credential");
    incomingMap.put(TOKEN_ISSUER, "https://incoming-issuer.example.com/");
    incomingMap.put(TOKEN_AUDIENCE, "https://incoming-audience.example.com/");

    final Map<String, Object> outgoingMap = new HashMap<>();
    outgoingMap.put(AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name());
    outgoingMap.put(TOKEN_SUBJECT, "outgoing-subject");
    outgoingMap.put(SHARED_SECRET, "outgoing-credential");
    outgoingMap.put(TOKEN_ISSUER, "https://outgoing-issuer.example.com/");
    outgoingMap.put(TOKEN_AUDIENCE, "https://outgoing-audience.example.com/");
    outgoingMap.put(TOKEN_EXPIRY, Duration.ofDays(2).toString());
    outgoingMap.put(URL, "https://outgoing.example.com/");

    final Map<String, Object> blastMap = new HashMap<>();
    blastMap.put(INCOMING, incomingMap);
    blastMap.put(OUTGOING, outgoingMap);

    final Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(BLAST, blastMap);

    return customSettings;
  }

}
