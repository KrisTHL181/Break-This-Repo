package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlockEntity extends BlockEntity {
   public CopperGolemStatueBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.COPPER_GOLEM_STATUE, worldPosition, blockState);
   }

   public void createStatue(final CopperGolem copperGolem) {
      this.setComponents(DataComponentMap.builder().addAll(this.components()).set(DataComponents.CUSTOM_NAME, copperGolem.getCustomName()).build());
      super.setChanged();
   }

   public @Nullable CopperGolem removeStatue(final BlockState state) {
      CopperGolem copperGolem = EntityTypes.COPPER_GOLEM.create(this.level, EntitySpawnReason.TRIGGERED);
      if (copperGolem != null) {
         copperGolem.setCustomName(this.components().get(DataComponents.CUSTOM_NAME));
         return this.initCopperGolem(state, copperGolem);
      } else {
         return null;
      }
   }

   private CopperGolem initCopperGolem(final BlockState state, final CopperGolem copperGolem) {
      BlockPos blockPos = this.getBlockPos();
      copperGolem.snapTo(
         Vec3.atCenterOf(blockPos).x, blockPos.getY(), Vec3.atCenterOf(blockPos).z, state.getValue(CopperGolemStatueBlock.FACING).toYRot(), 0.0F
      );
      copperGolem.yHeadRot = copperGolem.getYRot();
      copperGolem.yBodyRot = copperGolem.getYRot();
      copperGolem.playSpawnSound();
      return copperGolem;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public ItemStack getItem(final ItemStack itemStack, final CopperGolemStatueBlock.Pose pose) {
      itemStack.applyComponents(this.collectComponents());
      itemStack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, pose));
      return itemStack;
   }
}
