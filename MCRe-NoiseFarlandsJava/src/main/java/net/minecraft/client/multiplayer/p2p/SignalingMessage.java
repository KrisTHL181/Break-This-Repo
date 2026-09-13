package net.minecraft.client.multiplayer.p2p;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.onvoid.webrtc.RTCIceCandidate;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

public sealed interface SignalingMessage permits SignalingMessage.FriendJoin, SignalingMessage.WebRtc {
   Codec<SignalingMessage> CODEC = SignalingMessage.Type.CODEC.dispatch(SignalingMessage::type, SignalingMessage.Type::codec);

   static SignalingMessage joinRequest(final String sessionId) {
      return new SignalingMessage.FriendJoin.Request(sessionId);
   }

   static SignalingMessage joinAccepted(final String sessionId) {
      return new SignalingMessage.FriendJoin.Accepted(sessionId);
   }

   static SignalingMessage joinRejected(final String sessionId) {
      return new SignalingMessage.FriendJoin.Rejected(sessionId);
   }

   static SignalingMessage inviteDeclined() {
      return new SignalingMessage.FriendJoin.InviteDeclined(UUID.randomUUID().toString());
   }

   static SignalingMessage offer(final String sessionId, final String sdp) {
      return new SignalingMessage.WebRtc.Offer(sessionId, sdp);
   }

   static SignalingMessage answer(final String sessionId, final String sdp) {
      return new SignalingMessage.WebRtc.Answer(sessionId, sdp);
   }

   static SignalingMessage iceCandidate(final String sessionId, final RTCIceCandidate candidate) {
      return new SignalingMessage.WebRtc.IceCandidate(sessionId, candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex);
   }

   SignalingMessage.Type type();

   sealed interface FriendJoin
      extends SignalingMessage
      permits SignalingMessage.FriendJoin.Request,
      SignalingMessage.FriendJoin.Accepted,
      SignalingMessage.FriendJoin.Rejected,
      SignalingMessage.FriendJoin.InviteDeclined {
      record Accepted(String sessionId) implements SignalingMessage.FriendJoin {
         private static final MapCodec<SignalingMessage.FriendJoin.Accepted> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.FriendJoin.Accepted::sessionId))
               .apply(i, SignalingMessage.FriendJoin.Accepted::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.JOIN_ACCEPTED;
         }
      }

      record InviteDeclined(String sessionId) implements SignalingMessage.FriendJoin {
         private static final MapCodec<SignalingMessage.FriendJoin.InviteDeclined> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.FriendJoin.InviteDeclined::sessionId))
               .apply(i, SignalingMessage.FriendJoin.InviteDeclined::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.INVITE_DECLINED;
         }
      }

      record Rejected(String sessionId) implements SignalingMessage.FriendJoin {
         private static final MapCodec<SignalingMessage.FriendJoin.Rejected> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.FriendJoin.Rejected::sessionId))
               .apply(i, SignalingMessage.FriendJoin.Rejected::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.JOIN_REJECTED;
         }
      }

      record Request(String sessionId) implements SignalingMessage.FriendJoin {
         private static final MapCodec<SignalingMessage.FriendJoin.Request> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.FriendJoin.Request::sessionId))
               .apply(i, SignalingMessage.FriendJoin.Request::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.JOIN_REQUEST;
         }
      }
   }

   enum Type implements StringRepresentable {
      JOIN_REQUEST(() -> SignalingMessage.FriendJoin.Request.CODEC),
      JOIN_ACCEPTED(() -> SignalingMessage.FriendJoin.Accepted.CODEC),
      JOIN_REJECTED(() -> SignalingMessage.FriendJoin.Rejected.CODEC),
      INVITE_DECLINED(() -> SignalingMessage.FriendJoin.InviteDeclined.CODEC),
      OFFER(() -> SignalingMessage.WebRtc.Offer.CODEC),
      ANSWER(() -> SignalingMessage.WebRtc.Answer.CODEC),
      ICE_CANDIDATE(() -> SignalingMessage.WebRtc.IceCandidate.CODEC);

      private static final Codec<SignalingMessage.Type> CODEC = StringRepresentable.fromEnum(SignalingMessage.Type::values);
      private final Supplier<MapCodec<? extends SignalingMessage>> codec;

      Type(final Supplier<MapCodec<? extends SignalingMessage>> codec) {
         this.codec = codec;
      }

      private MapCodec<? extends SignalingMessage> codec() {
         return this.codec.get();
      }

      @Override
      public String getSerializedName() {
         return this.name();
      }
   }

   sealed interface WebRtc extends SignalingMessage permits SignalingMessage.WebRtc.Offer, SignalingMessage.WebRtc.Answer, SignalingMessage.WebRtc.IceCandidate {
      String sessionId();

      record Answer(String sessionId, String sdp) implements SignalingMessage.WebRtc {
         private static final MapCodec<SignalingMessage.WebRtc.Answer> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.WebRtc.Answer::sessionId),
                  Codec.STRING.fieldOf("sdp").forGetter(SignalingMessage.WebRtc.Answer::sdp)
               )
               .apply(i, SignalingMessage.WebRtc.Answer::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.ANSWER;
         }
      }

      record IceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) implements SignalingMessage.WebRtc {
         private static final MapCodec<SignalingMessage.WebRtc.IceCandidate> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.WebRtc.IceCandidate::sessionId),
                  Codec.STRING.fieldOf("candidate").forGetter(SignalingMessage.WebRtc.IceCandidate::candidate),
                  Codec.STRING.fieldOf("sdpMid").forGetter(SignalingMessage.WebRtc.IceCandidate::sdpMid),
                  Codec.INT.fieldOf("sdpMLineIndex").forGetter(SignalingMessage.WebRtc.IceCandidate::sdpMLineIndex)
               )
               .apply(i, SignalingMessage.WebRtc.IceCandidate::new)
         );

         public RTCIceCandidate toRtcIceCandidate() {
            return new RTCIceCandidate(this.sdpMid, this.sdpMLineIndex, this.candidate);
         }

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.ICE_CANDIDATE;
         }
      }

      record Offer(String sessionId, String sdp) implements SignalingMessage.WebRtc {
         private static final MapCodec<SignalingMessage.WebRtc.Offer> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.STRING.fieldOf("sessionId").forGetter(SignalingMessage.WebRtc.Offer::sessionId),
                  Codec.STRING.fieldOf("sdp").forGetter(SignalingMessage.WebRtc.Offer::sdp)
               )
               .apply(i, SignalingMessage.WebRtc.Offer::new)
         );

         @Override
         public SignalingMessage.Type type() {
            return SignalingMessage.Type.OFFER;
         }
      }
   }
}
