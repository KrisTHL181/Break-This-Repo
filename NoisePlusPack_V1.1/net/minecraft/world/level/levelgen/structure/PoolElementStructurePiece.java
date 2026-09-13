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
      StructureTemplateManager p_226495_,
      StructurePoolElement p_226496_,
      BlockPos p_226497_,
      int p_226498_,
      Rotation p_226499_,
      BoundingBox p_226500_,
      LiquidSettings p_345422_
   ) {
      super(StructurePieceType.JIGSAW, 0, p_226500_);
      this.structureTemplateManager = p_226495_;
      this.element = p_226496_;
      this.position = p_226497_;
      this.groundLevelDelta = p_226498_;
      this.rotation = p_226499_;
      this.liquidSettings = p_345422_;
   }

   public PoolElementStructurePiece(StructurePieceSerializationContext p_192406_, CompoundTag p_192407_) {
      super(StructurePieceType.JIGSAW, p_192407_);
      this.structureTemplateManager = p_192406_.structureTemplateManager();
      this.position = new BlockPos(p_192407_.getIntOr("PosX", 0), p_192407_.getIntOr("PosY", 0), p_192407_.getIntOr("PosZ", 0));
      this.groundLevelDelta = p_192407_.getIntOr("ground_level_delta", 0);
      DynamicOps<Tag> dynamicops = p_192406_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      this.element = p_192407_.<StructurePoolElement>read("pool_element", StructurePoolElement.CODEC, dynamicops)
         .orElseThrow(() -> new IllegalStateException("Invalid pool element found"));
      this.rotation = p_192407_.<Rotation>read("rotation", Rotation.LEGACY_CODEC).orElseThrow();
      this.boundingBox = this.element.getBoundingBox(this.structureTemplateManager, this.position, this.rotation);
      ListTag listtag = p_192407_.getListOrEmpty("junctions");
      this.junctions.clear();
      listtag.forEach(p_204943_ -> this.junctions.add(JigsawJunction.deserialize(new Dynamic(dynamicops, p_204943_))));
      this.liquidSettings = p_192407_.<LiquidSettings>read("liquid_settings", LiquidSettings.CODEC).orElse(JigsawStructure.DEFAULT_LIQUID_SETTINGS);
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext p_192425_, CompoundTag p_192426_) {
      p_192426_.putInt("PosX", this.position.getX());
      p_192426_.putInt("PosY", this.position.getY());
      p_192426_.putInt("PosZ", this.position.getZ());
      p_192426_.putInt("ground_level_delta", this.groundLevelDelta);
      DynamicOps<Tag> dynamicops = p_192425_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      p_192426_.store("pool_element", StructurePoolElement.CODEC, dynamicops, this.element);
      p_192426_.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
      ListTag listtag = new ListTag();

      for (JigsawJunction jigsawjunction : this.junctions) {
         listtag.add((Tag)jigsawjunction.serialize(dynamicops).getValue());
      }

      p_192426_.put("junctions", listtag);
      if (this.liquidSettings != JigsawStructure.DEFAULT_LIQUID_SETTINGS) {
         p_192426_.store("liquid_settings", LiquidSettings.CODEC, dynamicops, this.liquidSettings);
      }
   }

   @Override
   public void postProcess(
      WorldGenLevel p_226502_,
      StructureManager p_226503_,
      ChunkGenerator p_226504_,
      RandomSource p_226505_,
      BoundingBox p_226506_,
      ChunkPos p_226507_,
      BlockPos p_226508_
   ) {
      this.place(p_226502_, p_226503_, p_226504_, p_226505_, p_226506_, p_226508_, false);
   }

   public void place(
      WorldGenLevel p_226510_,
      StructureManager p_226511_,
      ChunkGenerator p_226512_,
      RandomSource p_226513_,
      BoundingBox p_226514_,
      BlockPos p_226515_,
      boolean p_226516_
   ) {
      this.element
         .place(
            this.structureTemplateManager,
            p_226510_,
            p_226511_,
            p_226512_,
            this.position,
            p_226515_,
            this.rotation,
            p_226514_,
            p_226513_,
            this.liquidSettings,
            p_226516_
         );
   }

   @Override
   public void move(int p_72616_, int p_72617_, int p_72618_) {
      super.move(p_72616_, p_72617_, p_72618_);
      this.position = this.position.offset(p_72616_, p_72617_, p_72618_);
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

   public void addJunction(JigsawJunction p_209917_) {
      this.junctions.add(p_209917_);
   }

   public List<JigsawJunction> getJunctions() {
      return this.junctions;
   }
}
