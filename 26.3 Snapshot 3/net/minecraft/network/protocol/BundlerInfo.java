package net.minecraft.network.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import org.jspecify.annotations.Nullable;

public interface BundlerInfo {
   int BUNDLE_SIZE_LIMIT = 4096;

   static <T extends PacketListener, P extends BundlePacket<? super T>> BundlerInfo createForPacket(
      final PacketType<P> bundlePacketType, final Function<Iterable<Packet<? super T>>, P> constructor, final BundleDelimiterPacket<? super T> delimiterPacket
   ) {
      return new BundlerInfo() {
         @Override
         public void unbundlePacket(final Packet<?> packet, final Consumer<Packet<?>> output) {
            if (packet.type() == bundlePacketType) {
               P bundlerPacket = (P)packet;
               output.accept(delimiterPacket);
               bundlerPacket.subPackets().forEach(output);
               output.accept(delimiterPacket);
            } else {
               output.accept(packet);
            }
         }

         @Override
         public BundlerInfo.@Nullable Bundler startPacketBundling(final Packet<?> packet) {
            return packet == delimiterPacket ? new BundlerInfo.Bundler() {
               private final List<Packet<? super T>> bundlePackets = new ArrayList<>();

               @Override
               public @Nullable Packet<?> addPacket(final Packet<?> packet) {
                  if (packet == delimiterPacket) {
                     return constructor.apply(this.bundlePackets);
                  }

                  Packet<T> castPacket = (Packet<T>)packet;
                  if (this.bundlePackets.size() >= 4096) {
                     throw new IllegalStateException("Too many packets in a bundle");
                  }

                  this.bundlePackets.add(castPacket);
                  return null;
               }
            } : null;
         }
      };
   }

   void unbundlePacket(Packet<?> packet, Consumer<Packet<?>> output);

   BundlerInfo.@Nullable Bundler startPacketBundling(Packet<?> packet);

   interface Bundler {
      @Nullable Packet<?> addPacket(Packet<?> packet);
   }
}
