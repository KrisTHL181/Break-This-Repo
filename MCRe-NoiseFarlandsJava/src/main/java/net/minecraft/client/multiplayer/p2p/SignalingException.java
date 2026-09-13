package net.minecraft.client.multiplayer.p2p;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public abstract class SignalingException extends RuntimeException {
   private final @Nullable UUID peerPmid;

   protected SignalingException(final @Nullable UUID peerPmid, final String message) {
      super(message);
      this.peerPmid = peerPmid;
   }

   public final @Nullable UUID peerPmid() {
      return this.peerPmid;
   }

   public static final class MessageUndeliveredException extends SignalingException {
      public MessageUndeliveredException(final String serverMessage) {
         super(null, serverMessage);
      }
   }

   public static final class SignalingAuthException extends SignalingException {
      public SignalingAuthException(final String serverMessage) {
         super(null, serverMessage);
      }
   }

   public static class SignalingRejectedException extends SignalingException {
      public SignalingRejectedException(final @Nullable UUID peerPmid, final String message) {
         super(peerPmid, message);
      }
   }

   public static final class TurnAuthFailedException extends SignalingException {
      public TurnAuthFailedException(final String serverMessage) {
         super(null, serverMessage);
      }
   }

   public static final class UnknownPlayerException extends SignalingException {
      public UnknownPlayerException(final @Nullable UUID peerPmid, final String serverMessage) {
         super(peerPmid, serverMessage);
      }
   }
}
