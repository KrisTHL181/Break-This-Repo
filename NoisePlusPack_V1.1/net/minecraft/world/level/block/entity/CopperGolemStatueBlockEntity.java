package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlockEntity extends BlockEntity {
   public CopperGolemStatueBlockEntity(BlockPos p_431485_, BlockState p_426299_) {
      super(BlockEntityType.COPPER_GOLEM_STATUE, p_431485_, p_426299_);
   }

   public void createStatue(CopperGolem p_457012_) {
      this.setComponents(DataComponentMap.builder().addAll(this.components()).set(DataComponents.CUSTOM_NAME, p_457012_.getCustomName()).build());
      super.setChanged();
   }

   public @Nullable CopperGolem removeStatue(BlockState p_422664_) {
      CopperGolem coppergolem = EntityType.COPPER_GOLEM.create(this.level, EntitySpawnReason.TRIGGERED);
      if (coppergolem != null) {
         coppergolem.setCustomName(this.components().get(DataComponents.CUSTOM_NAME));
         return this.initCopperGolem(p_422664_, coppergolem);
      } else {
         return null;
      }
   }

   private CopperGolem initCopperGolem(BlockState p_425669_, CopperGolem p_460019_) {
      BlockPos blockpos = this.getBlockPos();
      p_460019_.snapTo(blockpos.getCenter().x, blockpos.getY(), blockpos.getCenter().z, p_425669_.getValue(CopperGolemStatueBlock.FACING).toYRot(), 0.0F);
      p_460019_.yHeadRot = p_460019_.getYRot();
      p_460019_.yBodyRot = p_460019_.getYRot();
      p_460019_.playSpawnSound();
      return p_460019_;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public ItemStack getItem(ItemStack p_428379_, CopperGolemStatueBlock.Pose p_431587_) {
      p_428379_.applyComponents(this.collectComponents());
      p_428379_.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, p_431587_));
      return p_428379_;
   }
}
