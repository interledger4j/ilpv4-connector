package org.interledger.connector.it.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;

import java.io.IOException;

/**
 * For testing purposes, provides methods to generate pubsub topics and subscriptions on pubsub emulator and
 * other pubsub operations.
 */
public class PubSubResourceGenerator {

  private final TransportChannelProvider channelProvider;
  private final CredentialsProvider credentialsProvider;
  private String projectId;
  private PubSubAdmin pubSubAdmin;


  /**
   * Creates instance that is configured to talk to pubsub emulator on a given host and port
   * @param emulatorHost hostname for the emulator (e.g. localhost or 10.1.2.3)
   * @param emulatorPort port that emulator is listening on
   * @throws IOException
   */
  public PubSubResourceGenerator(String emulatorHost, int emulatorPort) throws IOException {
    this.projectId = "integration-test";
    ManagedChannel channel = ManagedChannelBuilder.forTarget(emulatorHost + ":" + emulatorPort).usePlaintext().build();
    channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    credentialsProvider = NoCredentialsProvider.create();
    pubSubAdmin = new PubSubAdmin(() -> projectId,
      topicAdminClient(),
      subscriptionAdminClient());
  }

  /**
   * Creates a {@link PubSubAdmin} that is configured for pubsub emulator.
   * @return admin
   */
  public PubSubAdmin getPubSubAdmin() {
    return pubSubAdmin;
  }

  /**
   * Creates a {@link Publisher} that can publish to the given topic on pubsub emulator
   * @param topicName topic
   * @return
   * @throws IOException
   */
  public Publisher createPublisher(String topicName) throws IOException {
    return Publisher.newBuilder(ProjectTopicName.of(projectId, topicName))
      .setChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .build();
  }

  /**
   * Creates a {@link Subscriber} that can subscribe to the given subscriptionName on pubsub emulator
   * @param subscriptionName name
   * @param receiver callback to handle received messages
   * @return
   */
  public Subscriber createSubscriber(String subscriptionName, MessageReceiver receiver) {
    return Subscriber.newBuilder(ProjectSubscriptionName.of(projectId, subscriptionName), receiver)
      .setChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .build();
  }

  private TopicAdminClient topicAdminClient() throws IOException {
    return TopicAdminClient.create(
      TopicAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .setCredentialsProvider(credentialsProvider).build());
  }

  private SubscriptionAdminClient subscriptionAdminClient() throws IOException {
    return SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
      .setTransportChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .build());

  }

}
