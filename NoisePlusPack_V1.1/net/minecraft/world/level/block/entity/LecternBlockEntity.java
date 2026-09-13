package net.minecraft.world.level.block.entity;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LecternBlockEntity extends BlockEntity implements Clearable, MenuProvider {
   public static final int DATA_PAGE = 0;
   public static final int NUM_DATA = 1;
   public static final int SLOT_BOOK = 0;
   public static final int NUM_SLOTS = 1;
   private final Container bookAccess = new Container() {
      @Override
      public int getContainerSize() {
         return 1;
      }

      @Override
      public boolean isEmpty() {
         return LecternBlockEntity.this.book.isEmpty();
      }

      @Override
      public ItemStack getItem(int p_59580_) {
         return p_59580_ == 0 ? LecternBlockEntity.this.book : ItemStack.EMPTY;
      }

      @Override
      public ItemStack removeItem(int p_59582_, int p_59583_) {
         if (p_59582_ == 0) {
            ItemStack itemstack = LecternBlockEntity.this.book.split(p_59583_);
            if (LecternBlockEntity.this.book.isEmpty()) {
               LecternBlockEntity.this.onBookItemRemove();
            }

            return itemstack;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public ItemStack removeItemNoUpdate(int p_59590_) {
         if (p_59590_ == 0) {
            ItemStack itemstack = LecternBlockEntity.this.book;
            LecternBlockEntity.this.book = ItemStack.EMPTY;
            LecternBlockEntity.this.onBookItemRemove();
            return itemstack;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public void setItem(int p_59585_, ItemStack p_59586_) {
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public void setChanged() {
         LecternBlockEntity.this.setChanged();
      }

      @Override
      public boolean stillValid(Player p_59588_) {
         return Container.stillValidBlockEntity(LecternBlockEntity.this, p_59588_) && LecternBlockEntity.this.hasBook();
      }

      @Override
      public boolean canPlaceItem(int p_59592_, ItemStack p_59593_) {
         return false;
      }

      @Override
      public void clearContent() {
      }
   };
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int p_59600_) {
         return p_59600_ == 0 ? LecternBlockEntity.this.page : 0;
      }

      @Override
      public void set(int p_59602_, int p_59603_) {
         if (p_59602_ == 0) {
            LecternBlockEntity.this.setPage(p_59603_);
         }
      }

      @Override
      public int getCount() {
         return 1;
      }
   };
   ItemStack book = ItemStack.EMPTY;
   int page;
   private int pageCount;

   public LecternBlockEntity(BlockPos p_155622_, BlockState p_155623_) {
      super(BlockEntityType.LECTERN, p_155622_, p_155623_);
   }

   public ItemStack getBook() {
      return this.book;
   }

   public boolean hasBook() {
      return this.book.has(DataComponents.WRITABLE_BOOK_CONTENT) || this.book.has(DataComponents.WRITTEN_BOOK_CONTENT);
   }

   public void setBook(ItemStack p_59537_) {
      this.setBook(p_59537_, null);
   }

   void onBookItemRemove() {
      this.page = 0;
      this.pageCount = 0;
      LecternBlock.resetBookState(null, this.getLevel(), this.getBlockPos(), this.getBlockState(), false);
   }

   public void setBook(ItemStack p_59539_, @Nullable Player p_59540_) {
      this.book = this.resolveBook(p_59539_, p_59540_);
      this.page = 0;
      this.pageCount = getPageCount(this.book);
      this.setChanged();
   }

   void setPage(int p_59533_) {
      int i = Mth.clamp(p_59533_, 0, this.pageCount - 1);
      if (i != this.page) {
         this.page = i;
         this.setChanged();
         LecternBlock.signalPageChange(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public int getPage() {
      return this.page;
   }

   public int getRedstoneSignal() {
      float f = this.pageCount > 1 ? this.getPage() / (this.pageCount - 1.0F) : 1.0F;
      return Mth.floor(f * 14.0F) + (this.hasBook() ? 1 : 0);
   }

   private ItemStack resolveBook(ItemStack p_59555_, @Nullable Player p_59556_) {
      if (this.level instanceof ServerLevel serverlevel) {
         WrittenBookContent.resolveForItem(p_59555_, this.createCommandSourceStack(p_59556_, serverlevel), p_59556_);
      }

      return p_59555_;
   }

   private CommandSourceStack createCommandSourceStack(@Nullable Player p_59535_, ServerLevel p_370159_) {
      String s;
      Component component;
      if (p_59535_ == null) {
         s = "Lectern";
         component = Component.literal("Lectern");
      } else {
         s = p_59535_.getPlainTextName();
         component = p_59535_.getDisplayName();
      }

      Vec3 vec3 = Vec3.atCenterOf(this.worldPosition);
      return new CommandSourceStack(
         CommandSource.NULL, vec3, Vec2.ZERO, p_370159_, LevelBasedPermissionSet.GAMEMASTER, s, component, p_370159_.getServer(), p_59535_
      );
   }

   @Override
   protected void loadAdditional(ValueInput p_406107_) {
      super.loadAdditional(p_406107_);
      this.book = p_406107_.<ItemStack>read("Book", ItemStack.CODEC).map(p_395437_ -> this.resolveBook(p_395437_, null)).orElse(ItemStack.EMPTY);
      this.pageCount = getPageCount(this.book);
      this.page = Mth.clamp(p_406107_.getIntOr("Page", 0), 0, this.pageCount - 1);
   }

   @Override
   protected void saveAdditional(ValueOutput p_408948_) {
      super.saveAdditional(p_408948_);
      if (!this.getBook().isEmpty()) {
         p_408948_.store("Book", ItemStack.CODEC, this.getBook());
         p_408948_.putInt("Page", this.page);
      }
   }

   @Override
   public void clearContent() {
      this.setBook(ItemStack.EMPTY);
   }

   @Override
   public void preRemoveSideEffects(BlockPos p_394910_, BlockState p_391731_) {
      if (p_391731_.getValue(LecternBlock.HAS_BOOK) && this.level != null) {
         Direction direction = p_391731_.getValue(LecternBlock.FACING);
         ItemStack itemstack = this.getBook().copy();
         float f = 0.25F * direction.getStepX();
         float f1 = 0.25F * direction.getStepZ();
         ItemEntity itementity = new ItemEntity(this.level, p_394910_.getX() + 0.5 + f, p_394910_.getY() + 1, p_394910_.getZ() + 0.5 + f1, itemstack);
         itementity.setDefaultPickUpDelay();
         this.level.addFreshEntity(itementity);
      }
   }

   @Override
   public AbstractContainerMenu createMenu(int p_59562_, Inventory p_59563_, Player p_59564_) {
      return new LecternMenu(p_59562_, this.bookAccess, this.dataAccess);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("container.lectern");
   }

   private static int getPageCount(ItemStack p_330049_) {
      WrittenBookContent writtenbookcontent = p_330049_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if (writtenbookcontent != null) {
         return writtenbookcontent.pages().size();
      }

      WritableBookContent writablebookcontent = p_330049_.get(DataComponents.WRITABLE_BOOK_CONTENT);
      return writablebookcontent != null ? writablebookcontent.pages().size() : 0;
   }
}
