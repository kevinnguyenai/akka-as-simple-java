package com.example.chatroom;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

public class ChatRoom {
    // Message RoomCommand
    public interface RoomCommand {}
    
    // GetSession command to spawn a new Session 
    public static final class GetSession implements RoomCommand {
        public final String screenName;
        public final ActorRef<SessionEvent> replyTo;

        public GetSession(String screenName, ActorRef<SessionEvent> replyTo) {
            this.screenName = screenName;
            this.replyTo = replyTo;
        }
    }
    // Message SessionEvent
    public interface SessionEvent {}

    // SessionGranted Message with Session Actor
    public static final class SessionGranted implements SessionEvent {
        public final ActorRef<PostMessage> handle;

        public SessionGranted(ActorRef<PostMessage> handle) {
            this.handle = handle;
        }
    }

    // SessionDenied Message with reason
    public static final class SessionDenied implements SessionEvent {
        public final String reason;

        public SessionDenied(String reason) {
            this.reason = reason;
        }
    }

    // MessagePosted is msg which was posted  
    public static final class MessagePosted implements SessionEvent {
        public final String screenName;
        public final String message;

        public MessagePosted(String screenName, String message) {
            this.screenName = screenName;
            this.message = message;
        }
    }
    // Message Command for Session Actor
    interface SessionCommand {}

    // PostMessage is message will be sent
    public static final class PostMessage implements SessionCommand {
        public final String message;

        public PostMessage(String message) {
            this.message = message;
        }
    }

    // NotifyClient Msg which tell to Session a msg of MessagePosted 
    public static final class NotifyClient implements SessionCommand {
        public final MessagePosted message;

        public NotifyClient(MessagePosted message) {
            this.message = message;
        }
    }

    public static final class PublishSessionMessage implements RoomCommand {
        public final String screenName;
        public final String message;

        public PublishSessionMessage(String screenName, String message) {
            this.screenName = screenName;
            this.message = message;
        }
    }

    // Constructor
    public static Behavior<RoomCommand> create() {
        return Behaviors.setup(
            ctx -> new ChatRoom(ctx).chatRoom(new ArrayList<ActorRef<SessionCommand>>()));
    }

    private final ActorContext<RoomCommand> context;

    private ChatRoom(ActorContext<RoomCommand> context) {
        this.context = context;
    }

    // Message Handler
    private Behavior<RoomCommand> chatRoom(List<ActorRef<SessionCommand>> sessions) {
        return Behaviors.receive(RoomCommand.class)
            .onMessage(GetSession.class, getSession -> onGetSession(sessions, getSession))
            .onMessage(PublishSessionMessage.class, pub -> onPublishSessionMessage(sessions, pub))
            .build();
    }

    // Action Handler

    private Behavior<RoomCommand> onGetSession(List<ActorRef<SessionCommand>> sessions, GetSession getSession) throws UnsupportedEncodingException {
        ActorRef<SessionEvent> client = getSession.replyTo;
        ActorRef<SessionCommand> ses = context.spawn(
            Session.create(context.getSelf(), getSession.screenName, client),
            URLEncoder.encode(getSession.screenName, StandardCharsets.UTF_8.name()));
        // narrow to only expose PostMessage
        client.tell(new SessionGranted(ses.narrow()));
        List<ActorRef<SessionCommand>> newSessions = new ArrayList<>(sessions);
        newSessions.add(ses);
        return chatRoom(newSessions);
    }

    private Behavior<RoomCommand> onPublishSessionMessage(
        List<ActorRef<SessionCommand>> sessions, PublishSessionMessage pub) {
      NotifyClient notification =
          new NotifyClient((new MessagePosted(pub.screenName, pub.message)));
      sessions.forEach(s -> s.tell(notification));
      return Behaviors.same();
    }
}
