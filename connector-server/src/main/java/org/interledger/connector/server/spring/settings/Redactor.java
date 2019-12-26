package org.interledger.connector.server.spring.settings;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Redactor {

  public static final String REDACTED = "[**REDACTED**]";
  private final Set<String> redactedSettings = Sets.newHashSet(
    IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET,
    IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN,
    OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET,
    OutgoingLinkSettings.HTTP_OUTGOING_SIMPLE_AUTH_TOKEN
  );

  public AccountSettings redact(AccountSettings settings) {
    return AccountSettings.builder().from(settings)
      .customSettings(redact(settings.customSettings()))
      .build();
  }

  public Map<String, Object> redact(Map<String, Object> unredacted) {
    Map<String, Object> redacted = new HashMap<>();
    unredacted
      .forEach((key, value) -> {
        if (redactedSettings.contains(key)) {
          redacted.put(key, REDACTED);
        } else {
          redacted.put(key, value);
        }
      });
    return redacted;
  }


}
