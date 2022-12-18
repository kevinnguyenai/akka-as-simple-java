package com.example.chatroom;


import com.example.chatroom.ChatRoom;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class Session {
    public static Behavior<ChatRoom.SessionCommand> create(ActorRef<ChatRoom.RoomCommand> room, String screenName, ActorRef<ChatRoom.SessionEvent> client) {
        return Behaviors.receive(ChatRoom.SessionCommand.class)
            .onMessage(ChatRoom.PostMessage.class, post -> onPostMessage(room, screenName, post))
            .onMessage(ChatRoom.NotifyClient.class, notification -> onNotifyClient(client, notification))
            .build();
    }   
    
    private static Behavior<ChatRoom.SessionCommand> onPostMessage(
        ActorRef<ChatRoom.RoomCommand> room, String screenName, ChatRoom.PostMessage post) {
      // from client, publish to others via the room
      room.tell(new ChatRoom.PublishSessionMessage(screenName, post.message));
      return Behaviors.same();
    }

    private static Behavior<ChatRoom.SessionCommand> onNotifyClient(
        ActorRef<ChatRoom.SessionEvent> client, ChatRoom.NotifyClient notification) {
      // published from the room
      client.tell(notification.message);
      return Behaviors.same();
    }
}
