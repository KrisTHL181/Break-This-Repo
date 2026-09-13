package net.minecraft.advancements;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record AdvancementHolder(Identifier id, Advancement value) {
   public static final StreamCodec<RegistryFriendlyByteBuf, AdvancementHolder> STREAM_CODEC = StreamCodec.composite(
      Identifier.STREAM_CODEC, AdvancementHolder::id, Advancement.STREAM_CODEC, AdvancementHolder::value, AdvancementHolder::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, List<AdvancementHolder>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

   @Override
   public boolean equals(Object p_298719_) {
      return this == p_298719_ ? true : p_298719_ instanceof AdvancementHolder advancementholder && this.id.equals(advancementholder.id);
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.id.toString();
   }
}
