package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class JigsawBlockEntity extends BlockEntity {
   public static final Codec<ResourceKey<StructureTemplatePool>> POOL_CODEC = ResourceKey.codec(Registries.TEMPLATE_POOL);
   public static final Identifier EMPTY_ID = Identifier.withDefaultNamespace("empty");
   private static final int DEFAULT_PLACEMENT_PRIORITY = 0;
   private static final int DEFAULT_SELECTION_PRIORITY = 0;
   public static final String TARGET = "target";
   public static final String POOL = "pool";
   public static final String JOINT = "joint";
   public static final String PLACEMENT_PRIORITY = "placement_priority";
   public static final String SELECTION_PRIORITY = "selection_priority";
   public static final String NAME = "name";
   public static final String FINAL_STATE = "final_state";
   public static final String DEFAULT_FINAL_STATE = "minecraft:air";
   private Identifier name = EMPTY_ID;
   private Identifier target = EMPTY_ID;
   private ResourceKey<StructureTemplatePool> pool = Pools.EMPTY;
   private JigsawBlockEntity.JointType joint = JigsawBlockEntity.JointType.ROLLABLE;
   private String finalState = "minecraft:air";
   private int placementPriority = 0;
   private int selectionPriority = 0;

   public JigsawBlockEntity(BlockPos p_155605_, BlockState p_155606_) {
      super(BlockEntityType.JIGSAW, p_155605_, p_155606_);
   }

   public Identifier getName() {
      return this.name;
   }

   public Identifier getTarget() {
      return this.target;
   }

   public ResourceKey<StructureTemplatePool> getPool() {
      return this.pool;
   }

   public String getFinalState() {
      return this.finalState;
   }

   public JigsawBlockEntity.JointType getJoint() {
      return this.joint;
   }

   public int getPlacementPriority() {
      return this.placementPriority;
   }

   public int getSelectionPriority() {
      return this.selectionPriority;
   }

   public void setName(Identifier p_456288_) {
      this.name = p_456288_;
   }

   public void setTarget(Identifier p_460260_) {
      this.target = p_460260_;
   }

   public void setPool(ResourceKey<StructureTemplatePool> p_222764_) {
      this.pool = p_222764_;
   }

   public void setFinalState(String p_59432_) {
      this.finalState = p_59432_;
   }

   public void setJoint(JigsawBlockEntity.JointType p_59425_) {
      this.joint = p_59425_;
   }

   public void setPlacementPriority(int p_312425_) {
      this.placementPriority = p_312425_;
   }

   public void setSelectionPriority(int p_309491_) {
      this.selectionPriority = p_309491_;
   }

   @Override
   protected void saveAdditional(ValueOutput p_409500_) {
      super.saveAdditional(p_409500_);
      p_409500_.store("name", Identifier.CODEC, this.name);
      p_409500_.store("target", Identifier.CODEC, this.target);
      p_409500_.store("pool", POOL_CODEC, this.pool);
      p_409500_.putString("final_state", this.finalState);
      p_409500_.store("joint", JigsawBlockEntity.JointType.CODEC, this.joint);
      p_409500_.putInt("placement_priority", this.placementPriority);
      p_409500_.putInt("selection_priority", this.selectionPriority);
   }

   @Override
   protected void loadAdditional(ValueInput p_409857_) {
      super.loadAdditional(p_409857_);
      this.name = p_409857_.<Identifier>read("name", Identifier.CODEC).orElse(EMPTY_ID);
      this.target = p_409857_.<Identifier>read("target", Identifier.CODEC).orElse(EMPTY_ID);
      this.pool = p_409857_.<ResourceKey<StructureTemplatePool>>read("pool", POOL_CODEC).orElse(Pools.EMPTY);
      this.finalState = p_409857_.getStringOr("final_state", "minecraft:air");
      this.joint = p_409857_.<JigsawBlockEntity.JointType>read("joint", JigsawBlockEntity.JointType.CODEC)
         .orElseGet(() -> StructureTemplate.getDefaultJointType(this.getBlockState()));
      this.placementPriority = p_409857_.getIntOr("placement_priority", 0);
      this.selectionPriority = p_409857_.getIntOr("selection_priority", 0);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_333585_) {
      return this.saveCustomOnly(p_333585_);
   }

   public void generate(ServerLevel p_59421_, int p_59422_, boolean p_59423_) {
      BlockPos blockpos = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
      Registry<StructureTemplatePool> registry = p_59421_.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> holder = registry.getOrThrow(this.pool);
      JigsawPlacement.generateJigsaw(p_59421_, holder, this.target, p_59422_, blockpos, p_59423_);
   }

   public enum JointType implements StringRepresentable {
      ROLLABLE("rollable"),
      ALIGNED("aligned");

      public static final StringRepresentable.EnumCodec<JigsawBlockEntity.JointType> CODEC = StringRepresentable.fromEnum(JigsawBlockEntity.JointType::values);
      private final String name;

      JointType(final String p_59455_) {
         this.name = p_59455_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public Component getTranslatedName() {
         return Component.translatable("jigsaw_block.joint." + this.name);
      }
   }
}
