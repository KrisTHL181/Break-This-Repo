package net.minecraft.util.debugchart;

import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.util.debug.ServerDebugSubscribers;

public class RemoteSampleLogger extends AbstractSampleLogger {
   private final ServerDebugSubscribers subscribers;
   private final RemoteDebugSampleType sampleType;

   public RemoteSampleLogger(final int dimensions, final ServerDebugSubscribers subscribers, final RemoteDebugSampleType sampleType) {
      this(dimensions, subscribers, sampleType, new long[dimensions]);
   }

   public RemoteSampleLogger(final int dimensions, final ServerDebugSubscribers subscribers, final RemoteDebugSampleType sampleType, final long[] defaults) {
      super(dimensions, defaults);
      this.subscribers = subscribers;
      this.sampleType = sampleType;
   }

   @Override
   protected void useSample() {
      if (this.subscribers.hasAnySubscriberFor(this.sampleType.subscription())) {
         this.subscribers.broadcastToAll(this.sampleType.subscription(), new ClientboundDebugSamplePacket((long[])this.sample.clone(), this.sampleType));
      }
   }
}
