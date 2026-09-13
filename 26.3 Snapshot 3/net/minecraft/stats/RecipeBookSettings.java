package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
   public static final StreamCodec<FriendlyByteBuf, RecipeBookSettings> STREAM_CODEC = StreamCodec.composite(
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      o -> o.crafting,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      o -> o.furnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      o -> o.blastFurnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      o -> o.smoker,
      RecipeBookSettings::new
   );
   public static final MapCodec<RecipeBookSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            RecipeBookSettings.TypeSettings.CRAFTING_MAP_CODEC.forGetter(o -> o.crafting),
            RecipeBookSettings.TypeSettings.FURNACE_MAP_CODEC.forGetter(o -> o.furnace),
            RecipeBookSettings.TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter(o -> o.blastFurnace),
            RecipeBookSettings.TypeSettings.SMOKER_MAP_CODEC.forGetter(o -> o.smoker)
         )
         .apply(i, RecipeBookSettings::new)
   );
   private RecipeBookSettings.TypeSettings crafting;
   private RecipeBookSettings.TypeSettings furnace;
   private RecipeBookSettings.TypeSettings blastFurnace;
   private RecipeBookSettings.TypeSettings smoker;

   public RecipeBookSettings() {
      this(
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT
      );
   }

   private RecipeBookSettings(
      final RecipeBookSettings.TypeSettings crafting,
      final RecipeBookSettings.TypeSettings furnace,
      final RecipeBookSettings.TypeSettings blastFurnace,
      final RecipeBookSettings.TypeSettings smoker
   ) {
      this.crafting = crafting;
      this.furnace = furnace;
      this.blastFurnace = blastFurnace;
      this.smoker = smoker;
   }

   @VisibleForTesting
   public RecipeBookSettings.TypeSettings getSettings(final RecipeBookType type) {
      return switch (type) {
         case CRAFTING -> this.crafting;
         case FURNACE -> this.furnace;
         case BLAST_FURNACE -> this.blastFurnace;
         case SMOKER -> this.smoker;
      };
   }

   private void updateSettings(final RecipeBookType recipeBookType, final UnaryOperator<RecipeBookSettings.TypeSettings> operator) {
      switch (recipeBookType) {
         case CRAFTING:
            this.crafting = operator.apply(this.crafting);
            break;
         case FURNACE:
            this.furnace = operator.apply(this.furnace);
            break;
         case BLAST_FURNACE:
            this.blastFurnace = operator.apply(this.blastFurnace);
            break;
         case SMOKER:
            this.smoker = operator.apply(this.smoker);
      }
   }

   public boolean isOpen(final RecipeBookType type) {
      return this.getSettings(type).open;
   }

   public void setOpen(final RecipeBookType type, final boolean open) {
      this.updateSettings(type, s -> s.setOpen(open));
   }

   public boolean isFiltering(final RecipeBookType type) {
      return this.getSettings(type).filtering;
   }

   public void setFiltering(final RecipeBookType type, final boolean filtering) {
      this.updateSettings(type, s -> s.setFiltering(filtering));
   }

   public RecipeBookSettings copy() {
      return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker);
   }

   public void replaceFrom(final RecipeBookSettings other) {
      this.crafting = other.crafting;
      this.furnace = other.furnace;
      this.blastFurnace = other.blastFurnace;
      this.smoker = other.smoker;
   }

   public record TypeSettings(boolean open, boolean filtering) {
      public static final RecipeBookSettings.TypeSettings DEFAULT = new RecipeBookSettings.TypeSettings(false, false);
      public static final MapCodec<RecipeBookSettings.TypeSettings> CRAFTING_MAP_CODEC = codec("isGuiOpen", "isFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> FURNACE_MAP_CODEC = codec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> BLAST_FURNACE_MAP_CODEC = codec(
         "isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"
      );
      public static final MapCodec<RecipeBookSettings.TypeSettings> SMOKER_MAP_CODEC = codec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
      public static final StreamCodec<ByteBuf, RecipeBookSettings.TypeSettings> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL,
         RecipeBookSettings.TypeSettings::open,
         ByteBufCodecs.BOOL,
         RecipeBookSettings.TypeSettings::filtering,
         RecipeBookSettings.TypeSettings::new
      );

      @Override
      public String toString() {
         return "[open=" + this.open + ", filtering=" + this.filtering + "]";
      }

      public RecipeBookSettings.TypeSettings setOpen(final boolean open) {
         return new RecipeBookSettings.TypeSettings(open, this.filtering);
      }

      public RecipeBookSettings.TypeSettings setFiltering(final boolean filtering) {
         return new RecipeBookSettings.TypeSettings(this.open, filtering);
      }

      private static MapCodec<RecipeBookSettings.TypeSettings> codec(final String openFieldName, final String filteringFieldName) {
         return RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.BOOL.optionalFieldOf(openFieldName, false).forGetter(RecipeBookSettings.TypeSettings::open),
                  Codec.BOOL.optionalFieldOf(filteringFieldName, false).forGetter(RecipeBookSettings.TypeSettings::filtering)
               )
               .apply(i, RecipeBookSettings.TypeSettings::new)
         );
      }
   }
}
