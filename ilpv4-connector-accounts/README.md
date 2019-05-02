# Connector Account Model
Connectors track accountRelationship with their peers using a concept called an `Account`. In Interledger, a Connector 
account has two primary functions. The first is to track an asset balance, denominated in a single currency/asset-type between two Interledger parties. The second purpose is to provide a conduit for exchanging ILP packets in order to facilitate a variety of messaging, financial, and other (potentially multi-hop) operations related to Interledger.
                                                                                    
Thus, when two ILP nodes (e.g., two Connectors) enter into an accounting accountRelationship, each Connector will construct 
a unique identifier to track the account for itself. This implementation calls this an `accoundId`.

Because account tracking involves various performance incongruitites, we track various portions of an overall account
 using three different primitives, each of which is described below in more detail.
 
## Balance Tracking
TODO

## Account Settings
Account settings track configuration for each particular account. This includes information about the underlying 
asset (e.g., code and `scale`) plus other settings, including custom information related to the underlying ling used 
by the Account. This data is typically persisted to an underlying datastore, and loaded at various times in the 
connector's operating cycle. In general, Account Setting information is highly cacheable, including across a cluster,
 so this type of information can easily live in a typical RDBMS data-store. 

## Runtime Link
While certain aspects of an Account do not change frequently, the Link that an Account uses is not determined 
until Runtime, and there needs to be a linkage between an Account and the Link that it runs on. This information is 
typically accessed from the AccountManager.