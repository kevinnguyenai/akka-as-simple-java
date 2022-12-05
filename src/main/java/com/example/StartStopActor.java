package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

final class StartStopActor1 extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(StartStopActor1::new);
    }

    private StartStopActor1(ActorContext<String> context) {
        super(context);
        System.out.println("first started");

        context.spawn(StartStopActor2.create(), "second");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("stop", Behaviors::stopped)
                .onSignal(PostStop.class, singal -> onPostStop())
                .build();

    }

    private Behavior<String> onPostStop() {
        System.out.println("first stopped");
        return this;
    }

}

final class StartStopActor2 extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(StartStopActor2::new);
    }

    private StartStopActor2(ActorContext<String> context) {
        super(context);
        System.out.println("second started");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private Behavior<String> onPostStop() {
        System.out.println("second stopped");
        return this;
    }

}

final class StartStopActorMain extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(StartStopActorMain::new);
    }

    private StartStopActorMain(ActorContext<String> context) {
        super(context);
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder().onMessageEquals("start", this::start).build();
    }

    private Behavior<String> start() {
        ActorRef<String> first = getContext().spawn(StartStopActor1.create(), "first");
        first.tell("stop");
        return Behaviors.same();
    }
}

public class StartStopActor {
    public static void main(String[] args) {
        ActorRef<String> test = ActorSystem.create(StartStopActorMain.create(), "testStartStopActor");
        test.tell("start");
    }
}
