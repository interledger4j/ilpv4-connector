package org.interledger.ilpv4.connector.support;

import java.util.ArrayList;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * @author jfulton
 */
public class Server {

    private SpringApplication application;
    private ConfigurableApplicationContext context;
    private final Properties properties;

    public Server(Class<?>... configurations) {
        this.application = new SpringApplication(configurations);
        this.properties = new Properties();

        ArrayList<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
        initializers.add(context -> context.getEnvironment().getPropertySources()
            .addFirst(new PropertiesPropertySource("node", properties)));
        application.setInitializers(initializers);
    }

    public void start() {
        context = application.run();
    }

    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    public Server setProperty(String key, String value) {
        this.properties.setProperty(key, value);
        return this;
    }

    public ApplicationContext getContext() {
        return context;
    }

    public int getPort() {
        if (context != null) {
            return context.getBean(ConnectorServerSettings.class).getPort();
        } else {
            throw new IllegalStateException("Server not started!");
        }
    }
}
