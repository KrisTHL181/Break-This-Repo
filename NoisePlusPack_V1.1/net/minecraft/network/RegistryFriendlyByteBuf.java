package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
   private final RegistryAccess registryAccess;

   public RegistryFriendlyByteBuf(ByteBuf p_333796_, RegistryAccess p_330009_) {
      super(p_333796_);
      this.registryAccess = p_330009_;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess p_336066_) {
      return p_328649_ -> new RegistryFriendlyByteBuf(p_328649_, p_336066_);
   }
}
