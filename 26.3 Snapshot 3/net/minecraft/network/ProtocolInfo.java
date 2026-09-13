package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;

public interface ProtocolInfo<T extends PacketListener> {
   ConnectionProtocol id();

   PacketFlow flow();

   StreamCodec<ByteBuf, Packet<? super T>> codec();

   @Nullable BundlerInfo bundlerInfo();

   interface Details {
      ConnectionProtocol id();

      PacketFlow flow();

      @VisibleForDebug
      void listPackets(ProtocolInfo.Details.PacketVisitor output);

      @FunctionalInterface
      interface PacketVisitor {
         void accept(PacketType<?> type, int networkId);
      }
   }

   interface DetailsProvider {
      ProtocolInfo.Details details();
   }
}
