package org.interledger.connector.it.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.AlreadyExistsException;
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
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;

import java.io.IOException;

public class PubSubResourceGenerator {

  private final TransportChannelProvider channelProvider;
  private final CredentialsProvider credentialsProvider;
  private final TopicAdminClient topicAdminClient;
  private final SubscriptionAdminClient subscriptionAdminClient;
  private String projectId;
  private PubSubAdmin pubSubAdmin;

  public PubSubResourceGenerator(String emulatorHost) throws IOException {
    this.projectId = "integration-test";
    ManagedChannel channel = ManagedChannelBuilder.forTarget(emulatorHost + ":38085").usePlaintext().build();
    channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    credentialsProvider = NoCredentialsProvider.create();
    topicAdminClient = topicAdminClient();
    subscriptionAdminClient = subscriptionAdminClient();
    pubSubAdmin = new PubSubAdmin(() -> projectId,
      topicAdminClient(),
      subscriptionAdminClient());
  }

  public Subscription createSubscription(String topicName, String subscriptionName) {
    return pubSubAdmin.createSubscription(subscriptionName, topicName);
  }

  public Topic createTopic(String topicName) {
    return pubSubAdmin.createTopic(topicName);
  }

  public Publisher createPublisher(String topicName) throws IOException {
    return Publisher.newBuilder(ProjectTopicName.of(projectId, topicName))
      .setChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .build();
  }

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
