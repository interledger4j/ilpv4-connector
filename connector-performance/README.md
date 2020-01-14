# ILPv4 Connector Load Testing
This document describes a standardized load-test suite that can be used to gauge the performance and resiliency of a 
Connector under load.

## Performance Testing Considerations
Testing a connector involves several facets of the overall Interledger payment protocol stack. For example, it's 
useful to measure each of the following metrics:

1. Fulfilled Packets-per-second.
1. Rejected Packets-per-second.
1. Unauthenticated Packets-per-second.

The fulfill/reject metrics should be measured using different payment paths with varying characteristics such as
 FX-rate conversions, payment path lengths, and other variables inherent in various topologies.

# Performance Test Topolgies

## Overview
This harness utilizes three different topologies for its testing:

1. **Single Connector with Loopback Links**  
This topology contains a single connector with 2 loopback 
links. The first link always fulfills; the second link always rejects with a `T02 Peer Busy` response to simulate a peer link that is being throttled.

1. **Single Connector with SPSP Peer**  
This topology contains a single connector with 2 child links that support [SPSP](https://github.com/interledger/rfcs
/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md). 
 
1. **Two Connectors with ILP-over-HTTP**  
This type of topology involves only a two connectors, each with a single ILP-over-HTTP link. The test sends real ILP prepare packets and expects actual fulfillments back throughout the 
entire test.

1. **4 Connector Multihop**
This topology simulates four connectors (3 hops) with each operating a BLAST (ILP-over-HTTP) link simulating three total currency conversions. The test sends real ILP prepare packets and expects 
actual fulfillments back throughout the duration of the test.

## Single Connector with Loopback Links
This topology contains a single connector with 2 loopback 
links. The first link always fulfills; the second link always rejects with a `T02 Peer Busy` response to simulate a peer link that is being throttled.

### Topology Diagram
```text
                ┌───────────────────────┐                
                │                       │                
                │ https://jc.ilpv4.dev  │                
                │                       │                
                └───────────────────────┘                
                            △                            
             ┌──────────────┴──────────────┐             
             ▽                             ▽             
┌─────────────────────────┐   ┌─────────────────────────┐
│                         │   │                         │
│Account: lt-lb-fulfiller │   │ Account: lt-lb-rejector │
│                         │   │                         │
└─────────────────────────┘   └─────────────────────────┘
```

### Ingress Account
To create the `lt-ingress` account, which is used for all ingress into the connector, execute the following command:

```text
curl --location --request POST 'https://jc.ilpv4.dev/accounts' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: Basic YWRtaW46cGFzc3dvcmQ=' \
--data-raw '{
  "accountId": "lt-ingress",
  "accountRelationship": "CHILD",
  "linkType": "ILP_OVER_HTTP",
  "assetCode": "USD",
  "assetScale": "2",
  "customSettings": {
        "ilpOverHttp.incoming.auth_type": "SIMPLE",
        "ilpOverHttp.incoming.simple.auth_token": "shh",
        "ilpOverHttp.outgoing.auth_type": "SIMPLE",
        "ilpOverHttp.outgoing.simple.auth_token": "shh",
        "ilpOverHttp.outgoing.url": "https://money.ilpv4.dev/ilp"
   }
}'
```


### Fulfil Account
To create the `lt-lb-fulfiller` account, execute the following command:

```text
curl --location --request POST 'https://jc.ilpv4.dev/accounts' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: Basic YWRtaW46cGFzc3dvcmQ=' \
--data-raw '{
  "accountId": "lt-lb-fulfiller",
  "accountRelationship": "CHILD",
  "linkType": "LOOPBACK",
  "assetCode": "USD",
  "assetScale": "2",
  "customSettings": {
  	"simulatedRejectErrorCode":"T02"
  }
}'
```

### Reject Account
To create the `lt-lb-rejector` account, execute the follwoing command:

```text
curl --location --request POST 'https://jc.ilpv4.dev/accounts' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: Basic YWRtaW46cGFzc3dvcmQ=' \
--data-raw '{
  "accountId": "lt-lb-rejector",
  "accountRelationship": "CHILD",
  "linkType": "LOOPBACK",
  "assetCode": "USD",
  "assetScale": "2",
  "customSettings": {
  	"simulatedRejectErrorCode":"T02"
  }
}'
```
