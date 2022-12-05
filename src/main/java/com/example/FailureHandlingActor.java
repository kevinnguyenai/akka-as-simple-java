package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

class SupervisingActor extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(SupervisingActor::new);
    }

    private final ActorRef<String> child;

    private SupervisingActor(ActorContext<String> context) {
        super(context);
        child = context
                .spawn(
                        Behaviors.supervise(SupervisedActor.create()).onFailure(SupervisorStrategy.restart()),
                        "supervised-actor");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("failChild", this::onFailChild)
                .onMessageEquals("stop", Behaviors::stopped)
                .onSignal(PostStop.class, signal -> onStopChild())
                .build();
    }

    private Behavior<String> onFailChild() {
        child.tell("fail");
        return this;
    }

    private Behavior<String> onStopChild() {
        System.out.println("supervising actor stopped");
        return this;
    }

}

class SupervisedActor extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(SupervisedActor::new);
    }

    private SupervisedActor(ActorContext<String> context) {
        super(context);
        System.out.println("supervised actor started");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("fail", this::fail)
                .onSignal(PreRestart.class, signal -> preRestart())
                .onSignal(PostStop.class, signal -> postStop())
                .build();
    }

    private Behavior<String> fail() {
        System.out.println("supervised actor fails now");
        throw new RuntimeException("I failed!");
    }

    private Behavior<String> preRestart() {
        System.out.println("supervised will be restarted");
        return this;
    }

    private Behavior<String> postStop() {
        System.out.println("supervised stopped");
        return this;
    }

}

class FailureHandlingMain extends AbstractBehavior<String> {

    static Behavior<String> create() {
        return Behaviors.setup(FailureHandlingMain::new);
    }

    private FailureHandlingMain(ActorContext<String> context) {
        super(context);
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("start", this::start)
                .onMessageEquals("stop", Behaviors::stopped)
                .onSignal(PostStop.class, signal -> preStop())
                .build();
    }

    private Behavior<String> start() {
        ActorRef<String> first = getContext().spawn(SupervisingActor.create(), "supervising-actor");
        first.tell("failChild");
        return Behaviors.same();
    }

    private Behavior<String> preStop() {
        System.out.println("FailureHandlingMain stopped");
        return this;
    }

}

class FailureHandlingActor {
    public static void main(String[] args) {
        ActorRef<String> first = ActorSystem.create(FailureHandlingMain.create(), "failureHandler");
        // start then error cause restart
        first.tell("start");
        // stop signal
        first.tell("stop");

    }
}
