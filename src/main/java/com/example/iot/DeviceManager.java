package com.example.iot;

import java.util.HashMap;
import java.util.Map;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class DeviceManager extends AbstractBehavior<DeviceManager.Command> {

    public interface Command {
    }

    public static final class RequestTrackDevice implements Device.Command, DeviceGroup.Command {
        public final String groupId;
        public final String deviceId;
        public final ActorRef<DeviceRegistered> replyTo;

        public RequestTrackDevice(String groupId, String deviceId, ActorRef<DeviceRegistered> replyTo) {
            this.groupId = groupId;
            this.deviceId = deviceId;
            this.replyTo = replyTo;
        }
    }

    public static final class DeviceRegistered {
        public final ActorRef<Device.Command> device;

        public DeviceRegistered(ActorRef<Device.Command> device) {
            this.device = device;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(DeviceManager::new);
    }

    private final Map<String, ActorRef<DeviceGroup.Command>> groupIdToActor = new HashMap<>();

    private DeviceManager(ActorContext<Command> context) {
        super(context);
        context.getLog().info("DeviceManager started");
    }

    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private DeviceManager onPostStop() {
        getContext().getLog().info("DeviceManager stopped");
        return this;
    }
}
