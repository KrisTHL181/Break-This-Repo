package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChestBlockEntity extends RandomizableContainerBlockEntity implements LidBlockEntity {
   private static final int EVENT_SET_OPEN_COUNT = 1;
   private static final Component DEFAULT_NAME = Component.translatable("container.chest");
   private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
      @Override
      protected void onOpen(Level p_155357_, BlockPos p_155358_, BlockState p_155359_) {
         if (p_155359_.getBlock() instanceof ChestBlock chestblock) {
            ChestBlockEntity.playSound(p_155357_, p_155358_, p_155359_, chestblock.getOpenChestSound());
         }
      }

      @Override
      protected void onClose(Level p_155367_, BlockPos p_155368_, BlockState p_155369_) {
         if (p_155369_.getBlock() instanceof ChestBlock chestblock) {
            ChestBlockEntity.playSound(p_155367_, p_155368_, p_155369_, chestblock.getCloseChestSound());
         }
      }

      @Override
      protected void openerCountChanged(Level p_155361_, BlockPos p_155362_, BlockState p_155363_, int p_155364_, int p_155365_) {
         ChestBlockEntity.this.signalOpenCount(p_155361_, p_155362_, p_155363_, p_155364_, p_155365_);
      }

      @Override
      public boolean isOwnContainer(Player p_155355_) {
         if (!(p_155355_.containerMenu instanceof ChestMenu)) {
            return false;
         }

         Container container = ((ChestMenu)p_155355_.containerMenu).getContainer();
         return container == ChestBlockEntity.this || container instanceof CompoundContainer && ((CompoundContainer)container).contains(ChestBlockEntity.this);
      }
   };
   private final ChestLidController chestLidController = new ChestLidController();

   protected ChestBlockEntity(BlockEntityType<?> p_155327_, BlockPos p_155328_, BlockState p_155329_) {
      super(p_155327_, p_155328_, p_155329_);
   }

   public ChestBlockEntity(BlockPos p_155331_, BlockState p_155332_) {
      this(BlockEntityType.CHEST, p_155331_, p_155332_);
   }

   @Override
   public int getContainerSize() {
      return 27;
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected void loadAdditional(ValueInput p_406899_) {
      super.loadAdditional(p_406899_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_406899_)) {
         ContainerHelper.loadAllItems(p_406899_, this.items);
      }
   }

   @Override
   protected void saveAdditional(ValueOutput p_406111_) {
      super.saveAdditional(p_406111_);
      if (!this.trySaveLootTable(p_406111_)) {
         ContainerHelper.saveAllItems(p_406111_, this.items);
      }
   }

   public static void lidAnimateTick(Level p_155344_, BlockPos p_155345_, BlockState p_155346_, ChestBlockEntity p_155347_) {
      p_155347_.chestLidController.tickLid();
   }

   static void playSound(Level p_155339_, BlockPos p_155340_, BlockState p_155341_, SoundEvent p_155342_) {
      ChestType chesttype = p_155341_.getValue(ChestBlock.TYPE);
      if (chesttype != ChestType.LEFT) {
         double d0 = p_155340_.getX() + 0.5;
         double d1 = p_155340_.getY() + 0.5;
         double d2 = p_155340_.getZ() + 0.5;
         if (chesttype == ChestType.RIGHT) {
            Direction direction = ChestBlock.getConnectedDirection(p_155341_);
            d0 += direction.getStepX() * 0.5;
            d2 += direction.getStepZ() * 0.5;
         }

         p_155339_.playSound(null, d0, d1, d2, p_155342_, SoundSource.BLOCKS, 0.5F, p_155339_.random.nextFloat() * 0.1F + 0.9F);
      }
   }

   @Override
   public boolean triggerEvent(int p_59114_, int p_59115_) {
      if (p_59114_ == 1) {
         this.chestLidController.shouldBeOpen(p_59115_ > 0);
         return true;
      } else {
         return super.triggerEvent(p_59114_, p_59115_);
      }
   }

   @Override
   public void startOpen(ContainerUser p_426602_) {
      if (!this.remove && !p_426602_.getLivingEntity().isSpectator()) {
         this.openersCounter
            .incrementOpeners(p_426602_.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), p_426602_.getContainerInteractionRange());
      }
   }

   @Override
   public void stopOpen(ContainerUser p_424111_) {
      if (!this.remove && !p_424111_.getLivingEntity().isSpectator()) {
         this.openersCounter.decrementOpeners(p_424111_.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   @Override
   public List<ContainerUser> getEntitiesWithContainerOpen() {
      return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_59110_) {
      this.items = p_59110_;
   }

   @Override
   public float getOpenNess(float p_59080_) {
      return this.chestLidController.getOpenness(p_59080_);
   }

   public static int getOpenCount(BlockGetter p_59087_, BlockPos p_59088_) {
      BlockState blockstate = p_59087_.getBlockState(p_59088_);
      if (blockstate.hasBlockEntity()) {
         BlockEntity blockentity = p_59087_.getBlockEntity(p_59088_);
         if (blockentity instanceof ChestBlockEntity) {
            return ((ChestBlockEntity)blockentity).openersCounter.getOpenerCount();
         }
      }

      return 0;
   }

   public static void swapContents(ChestBlockEntity p_59104_, ChestBlockEntity p_59105_) {
      NonNullList<ItemStack> nonnulllist = p_59104_.getItems();
      p_59104_.setItems(p_59105_.getItems());
      p_59105_.setItems(nonnulllist);
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59082_, Inventory p_59083_) {
      return ChestMenu.threeRows(p_59082_, p_59083_, this);
   }

   public void recheckOpen() {
      if (!this.remove) {
         this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   protected void signalOpenCount(Level p_155333_, BlockPos p_155334_, BlockState p_155335_, int p_155336_, int p_155337_) {
      Block block = p_155335_.getBlock();
      p_155333_.blockEvent(p_155334_, block, 1, p_155337_);
   }
}
