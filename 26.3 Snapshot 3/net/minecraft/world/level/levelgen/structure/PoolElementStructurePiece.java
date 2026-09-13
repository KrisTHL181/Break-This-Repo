package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PoolElementStructurePiece extends StructurePiece {
   protected final StructurePoolElement element;
   protected BlockPos position;
   private final int groundLevelDelta;
   protected final Rotation rotation;
   private final List<JigsawJunction> junctions = Lists.newArrayList();
   private final StructureTemplateManager structureTemplateManager;
   private final LiquidSettings liquidSettings;

   public PoolElementStructurePiece(
      final StructureTemplateManager structureTemplateManager,
      final StructurePoolElement element,
      final BlockPos position,
      final int groundLevelDelta,
      final Rotation rotation,
      final BoundingBox boundingBox,
      final LiquidSettings liquidSettings
   ) {
      super(StructurePieceType.JIGSAW, 0, boundingBox);
      this.structureTemplateManager = structureTemplateManager;
      this.element = element;
      this.position = position;
      this.groundLevelDelta = groundLevelDelta;
      this.rotation = rotation;
      this.liquidSettings = liquidSettings;
   }

   public PoolElementStructurePiece(final StructurePieceSerializationContext context, final CompoundTag tag) {
      super(StructurePieceType.JIGSAW, tag);
      this.structureTemplateManager = context.structureTemplateManager();
      this.position = new BlockPos(tag.getIntOr("PosX", 0), tag.getIntOr("PosY", 0), tag.getIntOr("PosZ", 0));
      this.groundLevelDelta = tag.getIntOr("ground_level_delta", 0);
      DynamicOps<Tag> ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      this.element = tag.<StructurePoolElement>read("pool_element", StructurePoolElement.CODEC, ops)
         .orElseThrow(() -> new IllegalStateException("Invalid pool element found"));
      this.rotation = tag.<Rotation>read("rotation", Rotation.LEGACY_CODEC).orElseThrow();
      this.boundingBox = this.element.getBoundingBox(this.structureTemplateManager, this.position, this.rotation);
      ListTag junctionsTag = tag.getListOrEmpty("junctions");
      this.junctions.clear();
      junctionsTag.forEach(junctionTag -> this.junctions.add(JigsawJunction.deserialize(new Dynamic(ops, junctionTag))));
      this.liquidSettings = tag.<LiquidSettings>read("liquid_settings", LiquidSettings.CODEC).orElse(JigsawStructure.DEFAULT_LIQUID_SETTINGS);
   }

   @Override
   protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      tag.putInt("PosX", this.position.getX());
      tag.putInt("PosY", this.position.getY());
      tag.putInt("PosZ", this.position.getZ());
      tag.putInt("ground_level_delta", this.groundLevelDelta);
      DynamicOps<Tag> ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      tag.store("pool_element", StructurePoolElement.CODEC, ops, this.element);
      tag.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
      ListTag junctionsTag = new ListTag();

      for (JigsawJunction junction : this.junctions) {
         junctionsTag.add((Tag)junction.serialize(ops).getValue());
      }

      tag.put("junctions", junctionsTag);
      if (this.liquidSettings != JigsawStructure.DEFAULT_LIQUID_SETTINGS) {
         tag.store("liquid_settings", LiquidSettings.CODEC, ops, this.liquidSettings);
      }
   }

   @Override
   public void postProcess(
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final RandomSource random,
      final BoundingBox chunkBB,
      final ChunkPos chunkPos,
      final BlockPos referencePos
   ) {
      this.place(level, structureManager, generator, random, chunkBB, referencePos, false);
   }

   public void place(
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final RandomSource random,
      final BoundingBox chunkBB,
      final BlockPos referencePos,
      final boolean keepJigsaws
   ) {
      this.element
         .place(
            this.structureTemplateManager,
            level,
            structureManager,
            generator,
            this.position,
            referencePos,
            this.rotation,
            chunkBB,
            random,
            this.liquidSettings,
            keepJigsaws
         );
   }

   @Override
   public void move(final int dx, final int dy, final int dz) {
      super.move(dx, dy, dz);
      this.position = this.position.offset(dx, dy, dz);
   }

   @Override
   public Rotation getRotation() {
      return this.rotation;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
   }

   public StructurePoolElement getElement() {
      return this.element;
   }

   public BlockPos getPosition() {
      return this.position;
   }

   public int getGroundLevelDelta() {
      return this.groundLevelDelta;
   }

   public void addJunction(final JigsawJunction junction) {
      this.junctions.add(junction);
   }

   public List<JigsawJunction> getJunctions() {
      return this.junctions;
   }
}
