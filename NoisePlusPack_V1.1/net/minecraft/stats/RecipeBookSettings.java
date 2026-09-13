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
      p_405231_ -> p_405231_.crafting,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_405234_ -> p_405234_.furnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_405229_ -> p_405229_.blastFurnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_405228_ -> p_405228_.smoker,
      RecipeBookSettings::new
   );
   public static final MapCodec<RecipeBookSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_405232_ -> p_405232_.group(
            RecipeBookSettings.TypeSettings.CRAFTING_MAP_CODEC.forGetter(p_405235_ -> p_405235_.crafting),
            RecipeBookSettings.TypeSettings.FURNACE_MAP_CODEC.forGetter(p_405230_ -> p_405230_.furnace),
            RecipeBookSettings.TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter(p_405233_ -> p_405233_.blastFurnace),
            RecipeBookSettings.TypeSettings.SMOKER_MAP_CODEC.forGetter(p_405236_ -> p_405236_.smoker)
         )
         .apply(p_405232_, RecipeBookSettings::new)
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
      RecipeBookSettings.TypeSettings p_408344_,
      RecipeBookSettings.TypeSettings p_409931_,
      RecipeBookSettings.TypeSettings p_406632_,
      RecipeBookSettings.TypeSettings p_410694_
   ) {
      this.crafting = p_408344_;
      this.furnace = p_409931_;
      this.blastFurnace = p_406632_;
      this.smoker = p_410694_;
   }

   @VisibleForTesting
   public RecipeBookSettings.TypeSettings getSettings(RecipeBookType p_361337_) {
      return switch (p_361337_) {
         case CRAFTING -> this.crafting;
         case FURNACE -> this.furnace;
         case BLAST_FURNACE -> this.blastFurnace;
         case SMOKER -> this.smoker;
      };
   }

   private void updateSettings(RecipeBookType p_363317_, UnaryOperator<RecipeBookSettings.TypeSettings> p_364138_) {
      switch (p_363317_) {
         case CRAFTING:
            this.crafting = p_364138_.apply(this.crafting);
            break;
         case FURNACE:
            this.furnace = p_364138_.apply(this.furnace);
            break;
         case BLAST_FURNACE:
            this.blastFurnace = p_364138_.apply(this.blastFurnace);
            break;
         case SMOKER:
            this.smoker = p_364138_.apply(this.smoker);
      }
   }

   public boolean isOpen(RecipeBookType p_12735_) {
      return this.getSettings(p_12735_).open;
   }

   public void setOpen(RecipeBookType p_12737_, boolean p_12738_) {
      this.updateSettings(p_12737_, p_358758_ -> p_358758_.setOpen(p_12738_));
   }

   public boolean isFiltering(RecipeBookType p_12755_) {
      return this.getSettings(p_12755_).filtering;
   }

   public void setFiltering(RecipeBookType p_12757_, boolean p_12758_) {
      this.updateSettings(p_12757_, p_358756_ -> p_358756_.setFiltering(p_12758_));
   }

   public RecipeBookSettings copy() {
      return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker);
   }

   public void replaceFrom(RecipeBookSettings p_12733_) {
      this.crafting = p_12733_.crafting;
      this.furnace = p_12733_.furnace;
      this.blastFurnace = p_12733_.blastFurnace;
      this.smoker = p_12733_.smoker;
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

      public RecipeBookSettings.TypeSettings setOpen(boolean p_363040_) {
         return new RecipeBookSettings.TypeSettings(p_363040_, this.filtering);
      }

      public RecipeBookSettings.TypeSettings setFiltering(boolean p_366242_) {
         return new RecipeBookSettings.TypeSettings(this.open, p_366242_);
      }

      private static MapCodec<RecipeBookSettings.TypeSettings> codec(String p_408682_, String p_410390_) {
         return RecordCodecBuilder.mapCodec(
            p_407394_ -> p_407394_.group(
                  Codec.BOOL.optionalFieldOf(p_408682_, false).forGetter(RecipeBookSettings.TypeSettings::open),
                  Codec.BOOL.optionalFieldOf(p_410390_, false).forGetter(RecipeBookSettings.TypeSettings::filtering)
               )
               .apply(p_407394_, RecipeBookSettings.TypeSettings::new)
         );
      }
   }
}
