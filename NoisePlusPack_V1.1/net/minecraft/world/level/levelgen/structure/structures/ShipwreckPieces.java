package net.minecraft.world.level.levelgen.structure.structures;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class ShipwreckPieces {
   private static final int NUMBER_OF_BLOCKS_ALLOWED_IN_WORLD_GEN_REGION = 32;
   static final BlockPos PIVOT = new BlockPos(4, 0, 15);
   private static final Identifier[] STRUCTURE_LOCATION_BEACHED = new Identifier[]{
      Identifier.withDefaultNamespace("shipwreck/with_mast"),
      Identifier.withDefaultNamespace("shipwreck/sideways_full"),
      Identifier.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      Identifier.withDefaultNamespace("shipwreck/sideways_backhalf"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_full"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      Identifier.withDefaultNamespace("shipwreck/with_mast_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   private static final Identifier[] STRUCTURE_LOCATION_OCEAN = new Identifier[]{
      Identifier.withDefaultNamespace("shipwreck/with_mast"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_full"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_fronthalf"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_backhalf"),
      Identifier.withDefaultNamespace("shipwreck/sideways_full"),
      Identifier.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      Identifier.withDefaultNamespace("shipwreck/sideways_backhalf"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_full"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      Identifier.withDefaultNamespace("shipwreck/with_mast_degraded"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_full_degraded"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_fronthalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/upsidedown_backhalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/sideways_full_degraded"),
      Identifier.withDefaultNamespace("shipwreck/sideways_fronthalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/sideways_backhalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      Identifier.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   static final Map<String, ResourceKey<LootTable>> MARKERS_TO_LOOT = Map.of(
      "map_chest", BuiltInLootTables.SHIPWRECK_MAP, "treasure_chest", BuiltInLootTables.SHIPWRECK_TREASURE, "supply_chest", BuiltInLootTables.SHIPWRECK_SUPPLY
   );

   public static ShipwreckPieces.ShipwreckPiece addRandomPiece(
      StructureTemplateManager p_334187_, BlockPos p_334016_, Rotation p_333925_, StructurePieceAccessor p_330683_, RandomSource p_331305_, boolean p_332987_
   ) {
      Identifier identifier = Util.getRandom(p_332987_ ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, p_331305_);
      ShipwreckPieces.ShipwreckPiece shipwreckpieces$shipwreckpiece = new ShipwreckPieces.ShipwreckPiece(p_334187_, identifier, p_334016_, p_333925_, p_332987_);
      p_330683_.addPiece(shipwreckpieces$shipwreckpiece);
      return shipwreckpieces$shipwreckpiece;
   }

   public static class ShipwreckPiece extends TemplateStructurePiece {
      private final boolean isBeached;

      public ShipwreckPiece(StructureTemplateManager p_229354_, Identifier p_458762_, BlockPos p_229356_, Rotation p_229357_, boolean p_229358_) {
         super(StructurePieceType.SHIPWRECK_PIECE, 0, p_229354_, p_458762_, p_458762_.toString(), makeSettings(p_229357_), p_229356_);
         this.isBeached = p_229358_;
      }

      public ShipwreckPiece(StructureTemplateManager p_229360_, CompoundTag p_229361_) {
         super(
            StructurePieceType.SHIPWRECK_PIECE,
            p_229361_,
            p_229360_,
            p_456223_ -> makeSettings(p_229361_.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow())
         );
         this.isBeached = p_229361_.getBooleanOr("isBeached", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229373_, CompoundTag p_229374_) {
         super.addAdditionalSaveData(p_229373_, p_229374_);
         p_229374_.putBoolean("isBeached", this.isBeached);
         p_229374_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
      }

      private static StructurePlaceSettings makeSettings(Rotation p_229371_) {
         return new StructurePlaceSettings()
            .setRotation(p_229371_)
            .setMirror(Mirror.NONE)
            .setRotationPivot(ShipwreckPieces.PIVOT)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void handleDataMarker(String p_229376_, BlockPos p_229377_, ServerLevelAccessor p_229378_, RandomSource p_229379_, BoundingBox p_229380_) {
         ResourceKey<LootTable> resourcekey = ShipwreckPieces.MARKERS_TO_LOOT.get(p_229376_);
         if (resourcekey != null) {
            RandomizableContainer.setBlockEntityLootTable(p_229378_, p_229379_, p_229377_.below(), resourcekey);
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229363_,
         StructureManager p_229364_,
         ChunkGenerator p_229365_,
         RandomSource p_229366_,
         BoundingBox p_229367_,
         ChunkPos p_229368_,
         BlockPos p_229369_
      ) {
         if (this.isTooBigToFitInWorldGenRegion()) {
            super.postProcess(p_229363_, p_229364_, p_229365_, p_229366_, p_229367_, p_229368_, p_229369_);
         } else {
            int i = p_229363_.getMaxY() + 1;
            int j = 0;
            Vec3i vec3i = this.template.getSize();
            Heightmap.Types heightmap$types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
            int k = vec3i.getX() * vec3i.getZ();
            if (k == 0) {
               j = p_229363_.getHeight(heightmap$types, this.templatePosition.getX(), this.templatePosition.getZ());
            } else {
               BlockPos blockpos = this.templatePosition.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);

               for (BlockPos blockpos1 : BlockPos.betweenClosed(this.templatePosition, blockpos)) {
                  int l = p_229363_.getHeight(heightmap$types, blockpos1.getX(), blockpos1.getZ());
                  j += l;
                  i = Math.min(i, l);
               }

               j /= k;
            }

            this.adjustPositionHeight(this.isBeached ? this.calculateBeachedPosition(i, p_229366_) : j);
            super.postProcess(p_229363_, p_229364_, p_229365_, p_229366_, p_229367_, p_229368_, p_229369_);
         }
      }

      public boolean isTooBigToFitInWorldGenRegion() {
         Vec3i vec3i = this.template.getSize();
         return vec3i.getX() > 32 || vec3i.getY() > 32;
      }

      public int calculateBeachedPosition(int p_332021_, RandomSource p_332823_) {
         return p_332021_ - this.template.getSize().getY() / 2 - p_332823_.nextInt(3);
      }

      public void adjustPositionHeight(int p_331508_) {
         this.templatePosition = new BlockPos(this.templatePosition.getX(), p_331508_, this.templatePosition.getZ());
      }
   }
}
