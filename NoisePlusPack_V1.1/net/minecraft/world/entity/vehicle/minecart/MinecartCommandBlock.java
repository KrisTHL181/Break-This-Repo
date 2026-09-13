package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartCommandBlock extends AbstractMinecart {
   static final EntityDataAccessor<String> DATA_ID_COMMAND_NAME = SynchedEntityData.defineId(MinecartCommandBlock.class, EntityDataSerializers.STRING);
   static final EntityDataAccessor<Component> DATA_ID_LAST_OUTPUT = SynchedEntityData.defineId(MinecartCommandBlock.class, EntityDataSerializers.COMPONENT);
   private final BaseCommandBlock commandBlock = new MinecartCommandBlock.MinecartCommandBase();
   private static final int ACTIVATION_DELAY = 4;
   private int lastActivated;

   public MinecartCommandBlock(EntityType<? extends MinecartCommandBlock> p_456364_, Level p_457327_) {
      super(p_456364_, p_457327_);
   }

   @Override
   protected Item getDropItem() {
      return Items.MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.COMMAND_BLOCK_MINECART);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454155_) {
      super.defineSynchedData(p_454155_);
      p_454155_.define(DATA_ID_COMMAND_NAME, "");
      p_454155_.define(DATA_ID_LAST_OUTPUT, CommonComponents.EMPTY);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_453417_) {
      super.readAdditionalSaveData(p_453417_);
      this.commandBlock.load(p_453417_);
      this.getEntityData().set(DATA_ID_COMMAND_NAME, this.getCommandBlock().getCommand());
      this.getEntityData().set(DATA_ID_LAST_OUTPUT, this.getCommandBlock().getLastOutput());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456627_) {
      super.addAdditionalSaveData(p_456627_);
      this.commandBlock.save(p_456627_);
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.COMMAND_BLOCK.defaultBlockState();
   }

   public BaseCommandBlock getCommandBlock() {
      return this.commandBlock;
   }

   @Override
   public void activateMinecart(ServerLevel p_456104_, int p_455150_, int p_452062_, int p_457225_, boolean p_459198_) {
      if (p_459198_ && this.tickCount - this.lastActivated >= 4) {
         this.getCommandBlock().performCommand(p_456104_);
         this.lastActivated = this.tickCount;
      }
   }

   @Override
   public InteractionResult interact(Player p_459569_, InteractionHand p_455676_) {
      if (!p_459569_.canUseGameMasterBlocks()) {
         return InteractionResult.PASS;
      }

      if (p_459569_.level().isClientSide()) {
         p_459569_.openMinecartCommandBlock(this);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_459244_) {
      super.onSyncedDataUpdated(p_459244_);
      if (DATA_ID_LAST_OUTPUT.equals(p_459244_)) {
         try {
            this.commandBlock.setLastOutput(this.getEntityData().get(DATA_ID_LAST_OUTPUT));
         } catch (Throwable var3) {
         }
      } else if (DATA_ID_COMMAND_NAME.equals(p_459244_)) {
         this.commandBlock.setCommand(this.getEntityData().get(DATA_ID_COMMAND_NAME));
      }
   }

   class MinecartCommandBase extends BaseCommandBlock {
      @Override
      public void onUpdated(ServerLevel p_452176_) {
         MinecartCommandBlock.this.getEntityData().set(MinecartCommandBlock.DATA_ID_COMMAND_NAME, this.getCommand());
         MinecartCommandBlock.this.getEntityData().set(MinecartCommandBlock.DATA_ID_LAST_OUTPUT, this.getLastOutput());
      }

      @Override
      public CommandSourceStack createCommandSourceStack(ServerLevel p_461029_, CommandSource p_452898_) {
         return new CommandSourceStack(
            p_452898_,
            MinecartCommandBlock.this.position(),
            MinecartCommandBlock.this.getRotationVector(),
            p_461029_,
            LevelBasedPermissionSet.GAMEMASTER,
            this.getName().getString(),
            MinecartCommandBlock.this.getDisplayName(),
            p_461029_.getServer(),
            MinecartCommandBlock.this
         );
      }

      @Override
      public boolean isValid() {
         return !MinecartCommandBlock.this.isRemoved();
      }
   }
}
