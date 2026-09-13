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

   public JigsawBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.JIGSAW, worldPosition, blockState);
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

   public void setName(final Identifier name) {
      this.name = name;
   }

   public void setTarget(final Identifier target) {
      this.target = target;
   }

   public void setPool(final ResourceKey<StructureTemplatePool> pool) {
      this.pool = pool;
   }

   public void setFinalState(final String finalState) {
      this.finalState = finalState;
   }

   public void setJoint(final JigsawBlockEntity.JointType joint) {
      this.joint = joint;
   }

   public void setPlacementPriority(final int placementPriority) {
      this.placementPriority = placementPriority;
   }

   public void setSelectionPriority(final int selectionPriority) {
      this.selectionPriority = selectionPriority;
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.store("name", Identifier.CODEC, this.name);
      output.store("target", Identifier.CODEC, this.target);
      output.store("pool", POOL_CODEC, this.pool);
      output.putString("final_state", this.finalState);
      output.store("joint", JigsawBlockEntity.JointType.CODEC, this.joint);
      output.putInt("placement_priority", this.placementPriority);
      output.putInt("selection_priority", this.selectionPriority);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.name = input.<Identifier>read("name", Identifier.CODEC).orElse(EMPTY_ID);
      this.target = input.<Identifier>read("target", Identifier.CODEC).orElse(EMPTY_ID);
      this.pool = input.<ResourceKey<StructureTemplatePool>>read("pool", POOL_CODEC).orElse(Pools.EMPTY);
      this.finalState = input.getStringOr("final_state", "minecraft:air");
      this.joint = input.<JigsawBlockEntity.JointType>read("joint", JigsawBlockEntity.JointType.CODEC)
         .orElseGet(() -> StructureTemplate.getDefaultJointType(this.getBlockState()));
      this.placementPriority = input.getIntOr("placement_priority", 0);
      this.selectionPriority = input.getIntOr("selection_priority", 0);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public void generate(final ServerLevel level, final int levels, final boolean keepJigsaws) {
      BlockPos position = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
      Registry<StructureTemplatePool> poolRegistry = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> pool = poolRegistry.getOrThrow(this.pool);
      JigsawPlacement.generateJigsaw(level, pool, this.target, levels, position, keepJigsaws);
   }

   public enum JointType implements StringRepresentable {
      ROLLABLE("rollable"),
      ALIGNED("aligned");

      public static final StringRepresentable.EnumCodec<JigsawBlockEntity.JointType> CODEC = StringRepresentable.fromEnum(JigsawBlockEntity.JointType::values);
      private final String name;

      JointType(final String name) {
         this.name = name;
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
