package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
   private final RegistryAccess registryAccess;

   public RegistryFriendlyByteBuf(final ByteBuf source, final RegistryAccess registryAccess) {
      super(source);
      this.registryAccess = registryAccess;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(final RegistryAccess registryAccess) {
      return buf -> new RegistryFriendlyByteBuf(buf, registryAccess);
   }
}
