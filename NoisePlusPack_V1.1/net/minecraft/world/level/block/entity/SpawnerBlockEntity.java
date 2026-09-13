package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpawnerBlockEntity extends BlockEntity implements Spawner {
   private final BaseSpawner spawner = new BaseSpawner() {
      @Override
      public void broadcastEvent(Level p_155767_, BlockPos p_155768_, int p_155769_) {
         p_155767_.blockEvent(p_155768_, Blocks.SPAWNER, p_155769_, 0);
      }

      @Override
      public void setNextSpawnData(@Nullable Level p_155771_, BlockPos p_155772_, SpawnData p_155773_) {
         super.setNextSpawnData(p_155771_, p_155772_, p_155773_);
         if (p_155771_ != null) {
            BlockState blockstate = p_155771_.getBlockState(p_155772_);
            p_155771_.sendBlockUpdated(p_155772_, blockstate, blockstate, 260);
         }
      }
   };

   public SpawnerBlockEntity(BlockPos p_155752_, BlockState p_155753_) {
      super(BlockEntityType.MOB_SPAWNER, p_155752_, p_155753_);
   }

   @Override
   protected void loadAdditional(ValueInput p_408182_) {
      super.loadAdditional(p_408182_);
      this.spawner.load(this.level, this.worldPosition, p_408182_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_410150_) {
      super.saveAdditional(p_410150_);
      this.spawner.save(p_410150_);
   }

   public static void clientTick(Level p_155755_, BlockPos p_155756_, BlockState p_155757_, SpawnerBlockEntity p_155758_) {
      p_155758_.spawner.clientTick(p_155755_, p_155756_);
   }

   public static void serverTick(Level p_155762_, BlockPos p_155763_, BlockState p_155764_, SpawnerBlockEntity p_155765_) {
      p_155765_.spawner.serverTick((ServerLevel)p_155762_, p_155763_);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_329063_) {
      CompoundTag compoundtag = this.saveCustomOnly(p_329063_);
      compoundtag.remove("SpawnPotentials");
      return compoundtag;
   }

   @Override
   public boolean triggerEvent(int p_59797_, int p_59798_) {
      return this.spawner.onEventTriggered(this.level, p_59797_) ? true : super.triggerEvent(p_59797_, p_59798_);
   }

   @Override
   public void setEntityId(EntityType<?> p_254530_, RandomSource p_253719_) {
      this.spawner.setEntityId(p_254530_, this.level, p_253719_, this.worldPosition);
      this.setChanged();
   }

   public BaseSpawner getSpawner() {
      return this.spawner;
   }
}
