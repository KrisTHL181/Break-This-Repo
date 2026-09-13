package net.minecraft.client.telemetry.events;

import com.mojang.serialization.Codec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public class P2PTelemetryEvent {
   public static final P2PTelemetryEvent INSTANCE = new P2PTelemetryEvent();

   public void send(
      final boolean successful,
      final P2PTelemetryEvent.State state,
      final @Nullable Instant connectionStartTime,
      final @Nullable Instant signalingDoneTime,
      final @Nullable Instant connectionEstablishedTime
   ) {
      P2PTelemetryEvent.State.Snapshot snapshot = state.snapshot();
      Long totalTimeMs = millisBetween(connectionStartTime, connectionEstablishedTime);
      Long signalingTimeMs = millisBetween(connectionStartTime, signalingDoneTime);
      Long iceConnectTimeMs = millisBetween(signalingDoneTime, connectionEstablishedTime);
      P2PTelemetryEvent.IcePath icePath = snapshot.localCandidateType() != null && snapshot.remoteCandidateType() != null
         ? P2PTelemetryEvent.IcePath.classify(snapshot.localCandidateType(), snapshot.remoteCandidateType())
         : null;
      Minecraft.getInstance().getTelemetryManager().getOutsideSessionSender().send(TelemetryEventType.P2P_CONNECTION, properties -> {
         properties.put(TelemetryProperty.P2P_CONNECTION_SUCCESSFUL, successful);
         if (icePath != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_ICE_PATH, icePath);
         }

         if (snapshot.localCandidateType() != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_LOCAL_CANDIDATE_TYPE, snapshot.localCandidateType());
         }

         if (snapshot.remoteCandidateType() != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_REMOTE_CANDIDATE_TYPE, snapshot.remoteCandidateType());
         }

         if (totalTimeMs != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_TOTAL_TIME_MS, totalTimeMs);
         }

         if (signalingTimeMs != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_SIGNALING_TIME_MS, signalingTimeMs);
         }

         if (iceConnectTimeMs != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_ICE_CONNECT_TIME_MS, iceConnectTimeMs);
         }

         if (!successful && snapshot.failureStage() != null) {
            properties.put(TelemetryProperty.P2P_CONNECTION_FAILURE_STAGE, snapshot.failureStage());
         }
      });
   }

   private static @Nullable Long millisBetween(final @Nullable Instant from, final @Nullable Instant to) {
      return from != null && to != null ? from.until(to, ChronoUnit.MILLIS) : null;
   }

   public enum FailureStage implements StringRepresentable {
      SIGNALING("SIGNALING"),
      ICE_CONNECT("ICE_CONNECT"),
      TIMEOUT("TIMEOUT");

      public static final Codec<P2PTelemetryEvent.FailureStage> CODEC = StringRepresentable.fromEnum(P2PTelemetryEvent.FailureStage::values);
      private final String name;

      FailureStage(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public enum IceCandidateType implements StringRepresentable {
      HOST("host"),
      SRFLX("srflx"),
      PRFLX("prflx"),
      RELAY("relay");

      public static final Codec<P2PTelemetryEvent.IceCandidateType> CODEC = StringRepresentable.fromEnum(P2PTelemetryEvent.IceCandidateType::values);
      private static final Map<String, P2PTelemetryEvent.IceCandidateType> BY_NAME = Arrays.stream(values())
         .collect(Collectors.toUnmodifiableMap(P2PTelemetryEvent.IceCandidateType::getSerializedName, Function.identity()));
      private final String name;

      IceCandidateType(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static Optional<P2PTelemetryEvent.IceCandidateType> byName(final String name) {
         return Optional.ofNullable(BY_NAME.get(name));
      }
   }

   public enum IcePath implements StringRepresentable {
      LOCAL("LOCAL"),
      DIRECT("DIRECT"),
      RELAY("RELAY"),
      UNKNOWN("UNKNOWN");

      public static final Codec<P2PTelemetryEvent.IcePath> CODEC = StringRepresentable.fromEnum(P2PTelemetryEvent.IcePath::values);
      private final String name;

      IcePath(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static P2PTelemetryEvent.IcePath classify(final P2PTelemetryEvent.IceCandidateType local, final P2PTelemetryEvent.IceCandidateType remote) {
         if (local == P2PTelemetryEvent.IceCandidateType.RELAY || remote == P2PTelemetryEvent.IceCandidateType.RELAY) {
            return RELAY;
         } else if (local == P2PTelemetryEvent.IceCandidateType.SRFLX
            || local == P2PTelemetryEvent.IceCandidateType.PRFLX
            || remote == P2PTelemetryEvent.IceCandidateType.SRFLX
            || remote == P2PTelemetryEvent.IceCandidateType.PRFLX) {
            return DIRECT;
         } else {
            return local == P2PTelemetryEvent.IceCandidateType.HOST && remote == P2PTelemetryEvent.IceCandidateType.HOST ? LOCAL : UNKNOWN;
         }
      }
   }

   public static final class State {
      private P2PTelemetryEvent.@Nullable IceCandidateType localCandidateType;
      private P2PTelemetryEvent.@Nullable IceCandidateType remoteCandidateType;
      private P2PTelemetryEvent.@Nullable FailureStage failureStage;

      public synchronized P2PTelemetryEvent.State.Snapshot snapshot() {
         return new P2PTelemetryEvent.State.Snapshot(this.localCandidateType, this.remoteCandidateType, this.failureStage);
      }

      public synchronized void setIceInfo(final P2PTelemetryEvent.IceCandidateType local, final P2PTelemetryEvent.IceCandidateType remote) {
         this.localCandidateType = local;
         this.remoteCandidateType = remote;
      }

      public synchronized void setFailureStage(final P2PTelemetryEvent.FailureStage failureStage) {
         if (this.failureStage == null) {
            this.failureStage = failureStage;
         }
      }

      public record Snapshot(
         P2PTelemetryEvent.@Nullable IceCandidateType localCandidateType,
         P2PTelemetryEvent.@Nullable IceCandidateType remoteCandidateType,
         P2PTelemetryEvent.@Nullable FailureStage failureStage
      ) {
      }
   }
}
