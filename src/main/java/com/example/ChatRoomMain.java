package com.example;

import com.example.chatroom.ChatRoom;
import com.example.chatroom.Gabbler;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;

/*
 * https://doc.akka.io/docs/akka/current/typed/actors.html
 * sample used the functional programming style where you pass a function to a factory which then constructs a behavior,
 * for stateful actors this means passing immutable state around as parameters and switching to a new behavior 
 * whenever you need to act on a changed state. An alternative way to express the same is a more object oriented style 
 * where a concrete class for the actor behavior is defined and mutable state is kept inside of it as fields.
 * 
 * Reference more class base Actor using AbstractBehavior<T> or more directly style can apply AbstractOnMessageBehavior and implements onMessage()
 */
public class ChatRoomMain {
    public static Behavior<Void> create() {
        return Behaviors.setup(
            context -> {
              ActorRef<ChatRoom.RoomCommand> chatRoom = context.spawn(ChatRoom.create(), "chatRoom");
              ActorRef<ChatRoom.SessionEvent> gabbler = context.spawn(Gabbler.create(), "gabbler");
              context.watch(gabbler);
              chatRoom.tell(new ChatRoom.GetSession("ol’ Gabbler", gabbler));
    
              return Behaviors.receive(Void.class)
                  .onSignal(Terminated.class, sig -> Behaviors.stopped())
                  .build();
            });
      }

    public static void main(String[] args) {
        // Create ActorSystem and top level supervisor
        ActorSystem.create(ChatRoomMain.create(), "chat-room-demo");
    }
}
