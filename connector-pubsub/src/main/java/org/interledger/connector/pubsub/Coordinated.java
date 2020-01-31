package org.interledger.connector.pubsub;

/**
 * Marker interface used to make sure events received via external topics aren't reprocessed since it would
 * create a feedback loop.
 */
interface Coordinated {}
