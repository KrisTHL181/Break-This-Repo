package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class Attribute {
   public static final Codec<Holder<Attribute>> CODEC = BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE);
   private final double defaultValue;
   private boolean syncable;
   private final String descriptionId;
   private Attribute.Sentiment sentiment = Attribute.Sentiment.POSITIVE;

   protected Attribute(final String descriptionId, final double defaultValue) {
      this.defaultValue = defaultValue;
      this.descriptionId = descriptionId;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public boolean isClientSyncable() {
      return this.syncable;
   }

   public Attribute setSyncable(final boolean syncable) {
      this.syncable = syncable;
      return this;
   }

   public Attribute setSentiment(final Attribute.Sentiment sentiment) {
      this.sentiment = sentiment;
      return this;
   }

   public double sanitizeValue(final double value) {
      return value;
   }

   public String getDescriptionId() {
      return this.descriptionId;
   }

   public ChatFormatting getStyle(final boolean valueIncrease) {
      return this.sentiment.getStyle(valueIncrease);
   }

   public enum Sentiment {
      POSITIVE,
      NEUTRAL,
      NEGATIVE;

      public ChatFormatting getStyle(final boolean valueIncrease) {
         return switch (this) {
            case POSITIVE -> valueIncrease ? ChatFormatting.BLUE : ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GRAY;
            case NEGATIVE -> valueIncrease ? ChatFormatting.RED : ChatFormatting.BLUE;
         };
      }
   }
}
