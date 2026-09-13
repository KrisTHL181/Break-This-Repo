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
      final PacketType<P> p_329276_, final Function<Iterable<Packet<? super T>>, P> p_265627_, final BundleDelimiterPacket<? super T> p_265373_
   ) {
      return new BundlerInfo() {
         @Override
         public void unbundlePacket(Packet<?> p_265538_, Consumer<Packet<?>> p_265064_) {
            if (p_265538_.type() == p_329276_) {
               P p = (P)p_265538_;
               p_265064_.accept(p_265373_);
               p.subPackets().forEach(p_265064_);
               p_265064_.accept(p_265373_);
            } else {
               p_265064_.accept(p_265538_);
            }
         }

         @Override
         public BundlerInfo.@Nullable Bundler startPacketBundling(Packet<?> p_265749_) {
            return p_265749_ == p_265373_ ? new BundlerInfo.Bundler() {
               private final List<Packet<? super T>> bundlePackets = new ArrayList<>();

               @Override
               public @Nullable Packet<?> addPacket(Packet<?> p_336207_) {
                  if (p_336207_ == p_265373_) {
                     return p_265627_.apply(this.bundlePackets);
                  }

                  if (this.bundlePackets.size() >= 4096) {
                     throw new IllegalStateException("Too many packets in a bundle");
                  }

                  this.bundlePackets.add((Packet<? super T>)p_336207_);
                  return null;
               }
            } : null;
         }
      };
   }

   void unbundlePacket(Packet<?> var1, Consumer<Packet<?>> var2);

   BundlerInfo.@Nullable Bundler startPacketBundling(Packet<?> var1);

   interface Bundler {
      @Nullable Packet<?> addPacket(Packet<?> var1);
   }
}
