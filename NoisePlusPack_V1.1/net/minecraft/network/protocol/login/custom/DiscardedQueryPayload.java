package net.minecraft.network.protocol.login.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public record DiscardedQueryPayload(Identifier id) implements CustomQueryPayload {
   @Override
   public void write(FriendlyByteBuf p_299949_) {
   }
}
