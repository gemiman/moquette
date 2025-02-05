package io.moquette.persistence;

import io.moquette.broker.SessionRegistry;
import io.moquette.broker.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentedPersistentQueueSerDesTest {

    @Test
    public void givenEnqueuedMessageWithoutMqttPropertyThenItCanBeProperlySerialized() {
        SegmentedPersistentQueueSerDes sut = new SegmentedPersistentQueueSerDes();

        final Topic topic = Topic.asTopic("/metering/temperature");
        ByteBuf payload = Unpooled.wrappedBuffer("Some fancy things".getBytes(StandardCharsets.UTF_8));
        SessionRegistry.EnqueuedMessage messageToSerialize = new SessionRegistry.PublishedMessage(
            topic, MqttQoS.AT_MOST_ONCE, payload, true);

        ByteBuffer serialized = sut.toBytes(messageToSerialize);

        final SessionRegistry.EnqueuedMessage decoded = sut.fromBytes(serialized);
        assertTrue(decoded instanceof SessionRegistry.PublishedMessage);
        final SessionRegistry.PublishedMessage casted = (SessionRegistry.PublishedMessage) decoded;
        assertEquals(topic, casted.getTopic());
    }

    @Test
    public void givenEnqueuedMessageContainingMqttPropertyThenItCanBeProperlySerialized() {
        SegmentedPersistentQueueSerDes sut = new SegmentedPersistentQueueSerDes();

        final Topic topic = Topic.asTopic("/metering/temperature");
        ByteBuf payload = Unpooled.wrappedBuffer("Some fancy things".getBytes(StandardCharsets.UTF_8));
        int subscriptionId = 123;
        MqttProperties.IntegerProperty intProperty = new MqttProperties.IntegerProperty(
            MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value(), subscriptionId);
        SessionRegistry.EnqueuedMessage messageToSerialize = new SessionRegistry.PublishedMessage(
            topic, MqttQoS.AT_MOST_ONCE, payload, true, intProperty);

        ByteBuffer serialized = sut.toBytes(messageToSerialize);

        final SessionRegistry.EnqueuedMessage decoded = sut.fromBytes(serialized);
        assertTrue(decoded instanceof SessionRegistry.PublishedMessage);
        final SessionRegistry.PublishedMessage casted = (SessionRegistry.PublishedMessage) decoded;
        Optional<MqttProperties.MqttProperty> subscriptionIdProp = Arrays.stream(casted.getMqttProperties())
            .filter(this::isSubscriptionIdentifier).findFirst();
        assertTrue(subscriptionIdProp.isPresent());
        final int propValue = subscriptionIdProp
            .map(MqttProperties.MqttProperty::value)
            .map(v -> (Integer) v)
            .orElse(-1);
        assertEquals(subscriptionId, propValue);
    }

    private boolean isSubscriptionIdentifier(MqttProperties.MqttProperty mqttProperty) {
        return mqttProperty.propertyId() == MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value();
    }
}
