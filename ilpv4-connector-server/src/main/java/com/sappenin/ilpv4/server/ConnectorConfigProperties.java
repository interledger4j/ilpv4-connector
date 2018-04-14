package com.sappenin.ilpv4.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * A pojo used for loading properties for a ledger plugin from the Spring properties file.
 */
// TODO: Remove all mention of ilp3 and just use ilp? or ilp-http?
@ConfigurationProperties(prefix = "ilp3.connector")
public class ConnectorConfigProperties {

    private String connectorSecret;
    private Map<String, SpringLedgerPluginConfig> ledgerPlugins;

    public Map<String, SpringLedgerPluginConfig> getLedgerPlugins() {
        return ledgerPlugins;
    }

    public void setLedgerPlugins(Map<String, SpringLedgerPluginConfig> ledgerPlugins) {
        this.ledgerPlugins = ledgerPlugins;
    }

    public String getConnectorSecret() {
        return connectorSecret;
    }

    public void setConnectorSecret(String connectorSecret) {
        this.connectorSecret = connectorSecret;
    }

    public static class SpringLedgerPluginConfig {

        private String pluginType;
        private String connectorAccount;
        private String expectedCurrencyCode;
        private Map<String, String> options;

        public String getPluginType() {
            return pluginType;
        }

        public SpringLedgerPluginConfig setPluginType(String pluginType) {
            this.pluginType = pluginType;
            return this;
        }

        public String getConnectorAccount() {
            return connectorAccount;
        }

        public void setConnectorAccount(String connectorAccount) {
            this.connectorAccount = connectorAccount;
        }

        public String getExpectedCurrencyCode() {
            return expectedCurrencyCode;
        }

        public void setExpectedCurrencyCode(String expectedCurrencyCode) {
            this.expectedCurrencyCode = expectedCurrencyCode;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options;
        }
    }
}
