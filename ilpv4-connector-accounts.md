# Connector Account Model

Interledger Connectors track relationships with their peers using a concept called an `Account`. Accounts have two primary functions. The first is to track an asset balance, denominated in a single currency/asset-type, between two Interledger parties. The second purpose is to provide a conduit for exchanging ILP packets, which can enable value transfer across the Interledger.

When two ILP nodes \(two Connectors, for example\) enter into an accounting relationship, each Connector will construct a unique identifier to track the account for itself. This implementation calls this identifier an `accoundId`.

Using account Ids, a Connector will track the _concept_ of an account using three different primitives, each of which is described below in more detail. This design is preferred over a single `Account` object because Connectors must be able to support ultra-high packet throughput, and using a single account model would not scale well for all internal usages inside the Connector.

## Account Settings

The `AccountsSettings` object is the initial primitive used to track all information necessary for the Connector to operate upon an account. This includes minimum and maximum balance thresholds, link information, and information about about the underlying asset for the account \(i.e., the asset `code` and `scale`\).

This data is typically persisted to a persistent data-store, and loaded at various times in the connector's operating cycle. In general, AccountSettings information is highly cacheable, including across a cluster using a shared data-store, so this type of information can easily live in a typical RDBMS or NoSQL system.

## Balance Tracking

TODO

## Links

This implementation uses the concept of a `Link` to describe the connection between two peers. Links have their own identifier called a `LinkId` which uniquely identifies a Link. Because a Link is an abstracion over the connection between two peers, a Link can operate over any underlying transport. Examples of this include HTTP, WebSockets, UDP, or any other communications mechanism.

This implementation enforces a single Link per accountId at any given time, and also prohibits a Link from operating over more than a single underlying transport. Because of these restrictions, the `LinkId` in this implementation is always equal to the `AccountId`, which effectively means an packets are only ever being transacted over a given account using a single Link.

