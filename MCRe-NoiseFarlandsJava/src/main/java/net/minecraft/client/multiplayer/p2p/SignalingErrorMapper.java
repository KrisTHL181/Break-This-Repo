package net.minecraft.client.multiplayer.p2p;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.client.multiplayer.p2p.client.JsonRpcException;
import org.jspecify.annotations.Nullable;

public final class SignalingErrorMapper {
   private static final String FIELD_CODE = "Code";
   private static final String FIELD_MESSAGE = "Message";
   private static final String DATA_MISSING_IDENTITY = "MissingOrExpiredIdentity";
   private static final String DATA_UNKNOWN_PLAYER = "UnknownPlayer";
   private static final int CODE_PLAYER_UNREACHABLE = 1;
   private static final int CODE_MESSAGE_DELIVERY_FAILED = 2;
   private static final int CODE_TURN_AUTH_FAILED = 3;

   private SignalingErrorMapper() {
   }

   public static SignalingException fromJsonRpc(final @Nullable UUID peerPmid, final JsonRpcException err) {
      String dataCode = err.dataCode();
      String msg = serviceMessage(err);
      if (dataCode != null) {
         return switch (dataCode) {
            case "MissingOrExpiredIdentity" -> new SignalingException.SignalingAuthException(msg);
            case "UnknownPlayer" -> new SignalingException.UnknownPlayerException(peerPmid, msg);
            default -> new SignalingException.SignalingRejectedException(peerPmid, msg);
         };
      } else {
         return msg.contains("not registered")
            ? new SignalingException.UnknownPlayerException(peerPmid, msg)
            : new SignalingException.SignalingRejectedException(peerPmid, msg);
      }
   }

   public static @Nullable SignalingException fromServiceEnvelope(final @Nullable JsonElement body) {
      if (body != null && body.isJsonObject()) {
         JsonObject obj = body.getAsJsonObject();
         if (obj.has("Code") && obj.get("Code").isJsonPrimitive()) {
            int code = obj.get("Code").getAsInt();
            String msg = serviceEnvelopeMessage(obj);

            return switch (code) {
               case 1 -> new SignalingException.UnknownPlayerException(null, msg);
               case 2 -> new SignalingException.MessageUndeliveredException(msg);
               case 3 -> new SignalingException.TurnAuthFailedException(msg);
               default -> new SignalingException.SignalingRejectedException(null, msg);
            };
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String serviceMessage(final JsonRpcException err) {
      JsonElement data = err.data();
      if (data != null && data.isJsonObject()) {
         String dataMessage = serviceEnvelopeMessage(data.getAsJsonObject());
         if (!dataMessage.isBlank()) {
            return dataMessage;
         }
      }

      return err.serverMessage();
   }

   private static String serviceEnvelopeMessage(final JsonObject obj) {
      JsonElement message = obj.get("Message");
      return message != null && message.isJsonPrimitive() ? message.getAsString() : "";
   }
}
