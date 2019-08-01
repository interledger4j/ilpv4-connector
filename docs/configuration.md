# Connector Configuration

Configuration of this Connector is obtained from a variety of potential sources when the connector starts up. This includes property files, environment variables, system properties and more, per [Spring Boot functionality](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).

## Runtime Configuration Properties

### Redis

Redis is used to track balances for every account operated by this Connector. The following properties may be used to configure Reds:

* `redis.host`: \(Default: `localhost`\) The host that Redis is operating on.
* `redis.port`: \(Default: `6379`\) The port that Redis is operating on. 
* `redis.password`: \(Default: none\) An encrypted password String containing the password that can be used access Redis.

_More coming soon..._

