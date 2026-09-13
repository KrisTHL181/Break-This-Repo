package net.minecraft.world.level.block.entity;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TrialSpawnerBlockEntity extends BlockEntity implements Spawner, TrialSpawner.StateAccessor {
   private final TrialSpawner trialSpawner = this.createDefaultSpawner();

   public TrialSpawnerBlockEntity(BlockPos p_309527_, BlockState p_312341_) {
      super(BlockEntityType.TRIAL_SPAWNER, p_309527_, p_312341_);
   }

   private TrialSpawner createDefaultSpawner() {
      PlayerDetector playerdetector = SharedConstants.DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS ? PlayerDetector.SHEEP : PlayerDetector.NO_CREATIVE_PLAYERS;
      PlayerDetector.EntitySelector playerdetector$entityselector = PlayerDetector.EntitySelector.SELECT_FROM_LEVEL;
      return new TrialSpawner(TrialSpawner.FullConfig.DEFAULT, this, playerdetector, playerdetector$entityselector);
   }

   @Override
   protected void loadAdditional(ValueInput p_406809_) {
      super.loadAdditional(p_406809_);
      this.trialSpawner.load(p_406809_);
      if (this.level != null) {
         this.markUpdated();
      }
   }

   @Override
   protected void saveAdditional(ValueOutput p_410550_) {
      super.saveAdditional(p_410550_);
      this.trialSpawner.store(p_410550_);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_335483_) {
      return this.trialSpawner.getStateData().getUpdateTag(this.getBlockState().getValue(TrialSpawnerBlock.STATE));
   }

   @Override
   public void setEntityId(EntityType<?> p_312357_, RandomSource p_313173_) {
      if (this.level == null) {
         Util.logAndPauseIfInIde("Expected non-null level");
      } else {
         this.trialSpawner.overrideEntityToSpawn(p_312357_, this.level);
         this.setChanged();
      }
   }

   public TrialSpawner getTrialSpawner() {
      return this.trialSpawner;
   }

   @Override
   public TrialSpawnerState getState() {
      return !this.getBlockState().hasProperty(BlockStateProperties.TRIAL_SPAWNER_STATE)
         ? TrialSpawnerState.INACTIVE
         : this.getBlockState().getValue(BlockStateProperties.TRIAL_SPAWNER_STATE);
   }

   @Override
   public void setState(Level p_313150_, TrialSpawnerState p_310751_) {
      this.setChanged();
      p_313150_.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(BlockStateProperties.TRIAL_SPAWNER_STATE, p_310751_));
   }

   @Override
   public void markUpdated() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }
}
