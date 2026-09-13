package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class ExplorationMapFunction extends LootItemConditionalFunction {
   public static final TagKey<Structure> DEFAULT_DESTINATION = StructureTags.ON_TREASURE_MAPS;
   public static final Holder<MapDecorationType> DEFAULT_DECORATION = MapDecorationTypes.WOODLAND_MANSION;
   public static final byte DEFAULT_ZOOM = 2;
   public static final int DEFAULT_SEARCH_RADIUS = 50;
   public static final boolean DEFAULT_SKIP_EXISTING = true;
   public static final MapCodec<ExplorationMapFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_298090_ -> commonFields(p_298090_)
         .and(
            p_298090_.group(
               TagKey.codec(Registries.STRUCTURE).optionalFieldOf("destination", DEFAULT_DESTINATION).forGetter(p_299700_ -> p_299700_.destination),
               MapDecorationType.CODEC.optionalFieldOf("decoration", DEFAULT_DECORATION).forGetter(p_327569_ -> p_327569_.mapDecoration),
               Codec.BYTE.optionalFieldOf("zoom", (byte)2).forGetter(p_299686_ -> p_299686_.zoom),
               Codec.INT.optionalFieldOf("search_radius", 50).forGetter(p_300245_ -> p_300245_.searchRadius),
               Codec.BOOL.optionalFieldOf("skip_existing_chunks", true).forGetter(p_299770_ -> p_299770_.skipKnownStructures)
            )
         )
         .apply(p_298090_, ExplorationMapFunction::new)
   );
   private final TagKey<Structure> destination;
   private final Holder<MapDecorationType> mapDecoration;
   private final byte zoom;
   private final int searchRadius;
   private final boolean skipKnownStructures;

   ExplorationMapFunction(
      List<LootItemCondition> p_300426_, TagKey<Structure> p_210653_, Holder<MapDecorationType> p_335648_, byte p_210655_, int p_210656_, boolean p_210657_
   ) {
      super(p_300426_);
      this.destination = p_210653_;
      this.mapDecoration = p_335648_;
      this.zoom = p_210655_;
      this.searchRadius = p_210656_;
      this.skipKnownStructures = p_210657_;
   }

   @Override
   public LootItemFunctionType<ExplorationMapFunction> getType() {
      return LootItemFunctions.EXPLORATION_MAP;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   @Override
   public ItemStack run(ItemStack p_80547_, LootContext p_80548_) {
      if (!p_80547_.is(Items.MAP)) {
         return p_80547_;
      }

      Vec3 vec3 = p_80548_.getOptionalParameter(LootContextParams.ORIGIN);
      if (vec3 != null) {
         ServerLevel serverlevel = p_80548_.getLevel();
         BlockPos blockpos = serverlevel.findNearestMapStructure(this.destination, BlockPos.containing(vec3), this.searchRadius, this.skipKnownStructures);
         if (blockpos != null) {
            ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), this.zoom, true, true);
            MapItem.renderBiomePreviewMap(serverlevel, itemstack);
            MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", this.mapDecoration);
            return itemstack;
         }
      }

      return p_80547_;
   }

   public static ExplorationMapFunction.Builder makeExplorationMap() {
      return new ExplorationMapFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<ExplorationMapFunction.Builder> {
      private TagKey<Structure> destination = ExplorationMapFunction.DEFAULT_DESTINATION;
      private Holder<MapDecorationType> mapDecoration = ExplorationMapFunction.DEFAULT_DECORATION;
      private byte zoom = 2;
      private int searchRadius = 50;
      private boolean skipKnownStructures = true;

      protected ExplorationMapFunction.Builder getThis() {
         return this;
      }

      public ExplorationMapFunction.Builder setDestination(TagKey<Structure> p_210659_) {
         this.destination = p_210659_;
         return this;
      }

      public ExplorationMapFunction.Builder setMapDecoration(Holder<MapDecorationType> p_335674_) {
         this.mapDecoration = p_335674_;
         return this;
      }

      public ExplorationMapFunction.Builder setZoom(byte p_80570_) {
         this.zoom = p_80570_;
         return this;
      }

      public ExplorationMapFunction.Builder setSearchRadius(int p_165206_) {
         this.searchRadius = p_165206_;
         return this;
      }

      public ExplorationMapFunction.Builder setSkipKnownStructures(boolean p_80576_) {
         this.skipKnownStructures = p_80576_;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
      }
   }
}
