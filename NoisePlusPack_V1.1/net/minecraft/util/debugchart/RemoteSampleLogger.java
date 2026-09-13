package net.minecraft.util.debugchart;

import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.util.debug.ServerDebugSubscribers;

public class RemoteSampleLogger extends AbstractSampleLogger {
   private final ServerDebugSubscribers subscribers;
   private final RemoteDebugSampleType sampleType;

   public RemoteSampleLogger(int p_334352_, ServerDebugSubscribers p_430157_, RemoteDebugSampleType p_332243_) {
      this(p_334352_, p_430157_, p_332243_, new long[p_334352_]);
   }

   public RemoteSampleLogger(int p_329489_, ServerDebugSubscribers p_431695_, RemoteDebugSampleType p_331596_, long[] p_423548_) {
      super(p_329489_, p_423548_);
      this.subscribers = p_431695_;
      this.sampleType = p_331596_;
   }

   @Override
   protected void useSample() {
      if (this.subscribers.hasAnySubscriberFor(this.sampleType.subscription())) {
         this.subscribers.broadcastToAll(this.sampleType.subscription(), new ClientboundDebugSamplePacket((long[])this.sample.clone(), this.sampleType));
      }
   }
}
