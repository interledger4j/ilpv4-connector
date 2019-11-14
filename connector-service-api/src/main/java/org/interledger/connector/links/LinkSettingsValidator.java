package org.interledger.connector.links;

import org.interledger.link.LinkSettings;

public interface LinkSettingsValidator {

  <T extends LinkSettings> T validateSettings(T linkSettings);

}
