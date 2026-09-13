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
   public static final MapCodec<ExplorationMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               TagKey.codec(Registries.STRUCTURE).optionalFieldOf("destination", DEFAULT_DESTINATION).forGetter(f -> f.destination),
               MapDecorationType.CODEC.optionalFieldOf("decoration", DEFAULT_DECORATION).forGetter(f -> f.mapDecoration),
               Codec.BYTE.optionalFieldOf("zoom", (byte)2).forGetter(f -> f.zoom),
               Codec.INT.optionalFieldOf("search_radius", 50).forGetter(f -> f.searchRadius),
               Codec.BOOL.optionalFieldOf("skip_existing_chunks", true).forGetter(f -> f.skipKnownStructures)
            )
         )
         .apply(i, ExplorationMapFunction::new)
   );
   private final TagKey<Structure> destination;
   private final Holder<MapDecorationType> mapDecoration;
   private final byte zoom;
   private final int searchRadius;
   private final boolean skipKnownStructures;

   private ExplorationMapFunction(
      final List<LootItemCondition> predicates,
      final TagKey<Structure> destination,
      final Holder<MapDecorationType> mapDecoration,
      final byte zoom,
      final int searchRadius,
      final boolean skipKnownStructures
   ) {
      super(predicates);
      this.destination = destination;
      this.mapDecoration = mapDecoration;
      this.zoom = zoom;
      this.searchRadius = searchRadius;
      this.skipKnownStructures = skipKnownStructures;
   }

   @Override
   public MapCodec<ExplorationMapFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (!itemStack.is(Items.MAP)) {
         return itemStack;
      }

      Vec3 lootPos = context.getOptionalParameter(LootContextParams.ORIGIN);
      if (lootPos != null) {
         ServerLevel level = context.getLevel();
         BlockPos nearestMapStructure = level.findNearestMapStructure(
            this.destination, BlockPos.containing(lootPos), this.searchRadius, this.skipKnownStructures
         );
         if (nearestMapStructure != null) {
            ItemStack map = MapItem.create(level, nearestMapStructure.getX(), nearestMapStructure.getZ(), this.zoom, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, nearestMapStructure, "+", this.mapDecoration);
            return map;
         }
      }

      return itemStack;
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

      public ExplorationMapFunction.Builder setDestination(final TagKey<Structure> destination) {
         this.destination = destination;
         return this;
      }

      public ExplorationMapFunction.Builder setMapDecoration(final Holder<MapDecorationType> mapDecoration) {
         this.mapDecoration = mapDecoration;
         return this;
      }

      public ExplorationMapFunction.Builder setZoom(final byte zoom) {
         this.zoom = zoom;
         return this;
      }

      public ExplorationMapFunction.Builder setSearchRadius(final int searchRadius) {
         this.searchRadius = searchRadius;
         return this;
      }

      public ExplorationMapFunction.Builder setSkipKnownStructures(final boolean skipKnownStructures) {
         this.skipKnownStructures = skipKnownStructures;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
      }
   }
}
