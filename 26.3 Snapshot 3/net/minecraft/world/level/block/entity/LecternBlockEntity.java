package net.minecraft.world.level.block.entity;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ResolutionContext;
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
      public ItemStack getItem(final int slot) {
         return slot == 0 ? LecternBlockEntity.this.book : ItemStack.EMPTY;
      }

      @Override
      public ItemStack removeItem(final int slot, final int count) {
         if (slot == 0) {
            ItemStack result = LecternBlockEntity.this.book.split(count);
            if (LecternBlockEntity.this.book.isEmpty()) {
               LecternBlockEntity.this.onBookItemRemove();
            }

            return result;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public ItemStack removeItemNoUpdate(final int slot) {
         if (slot == 0) {
            ItemStack prev = LecternBlockEntity.this.book;
            LecternBlockEntity.this.book = ItemStack.EMPTY;
            LecternBlockEntity.this.onBookItemRemove();
            return prev;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public void setItem(final int slot, final ItemStack itemStack) {
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
      public boolean stillValid(final Player player) {
         return Container.stillValidBlockEntity(LecternBlockEntity.this, player) && LecternBlockEntity.this.hasBook();
      }

      @Override
      public boolean canPlaceItem(final int slot, final ItemStack itemStack) {
         return false;
      }

      @Override
      public void clearContent() {
      }
   };
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(final int dataId) {
         return dataId == 0 ? LecternBlockEntity.this.page : 0;
      }

      @Override
      public void set(final int dataId, final int value) {
         if (dataId == 0) {
            LecternBlockEntity.this.setPage(value);
         }
      }

      @Override
      public int getCount() {
         return 1;
      }
   };
   private ItemStack book = ItemStack.EMPTY;
   private int page;
   private int pageCount;

   public LecternBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.LECTERN, worldPosition, blockState);
   }

   public ItemStack getBook() {
      return this.book;
   }

   public boolean hasBook() {
      return this.book.has(DataComponents.WRITABLE_BOOK_CONTENT) || this.book.has(DataComponents.WRITTEN_BOOK_CONTENT);
   }

   public void setBook(final ItemStack book) {
      this.setBook(book, null);
   }

   private void onBookItemRemove() {
      this.page = 0;
      this.pageCount = 0;
      LecternBlock.resetBookState(null, this.getLevel(), this.getBlockPos(), this.getBlockState(), false);
   }

   public void setBook(final ItemStack book, final @Nullable Player resolutionContext) {
      this.book = this.resolveBook(book, resolutionContext);
      this.page = 0;
      this.pageCount = getPageCount(this.book);
      this.setChanged();
   }

   private void setPage(final int page) {
      int newPage = Mth.clamp(page, 0, this.pageCount - 1);
      if (newPage != this.page) {
         this.page = newPage;
         this.setChanged();
         LecternBlock.signalPageChange(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public int getPage() {
      return this.page;
   }

   public int getRedstoneSignal() {
      float pageProgress = this.pageCount > 1 ? this.getPage() / (this.pageCount - 1.0F) : 1.0F;
      return Mth.floor(pageProgress * 14.0F) + (this.hasBook() ? 1 : 0);
   }

   private ItemStack resolveBook(final ItemStack book, final @Nullable Player player) {
      if (this.level instanceof ServerLevel serverLevel) {
         ResolutionContext context = ResolutionContext.create(this.createCommandSourceStack(player, serverLevel));
         WrittenBookContent.resolveForItem(book, context, this.level.registryAccess());
      }

      return book;
   }

   private CommandSourceStack createCommandSourceStack(final @Nullable Player player, final ServerLevel level) {
      String textName;
      Component displayName;
      if (player == null) {
         textName = "Lectern";
         displayName = Component.literal("Lectern");
      } else {
         textName = player.getPlainTextName();
         displayName = player.getDisplayName();
      }

      Vec3 pos = Vec3.atCenterOf(this.worldPosition);
      return new CommandSourceStack(
         CommandSource.NULL, pos, Vec2.ZERO, level, LevelBasedPermissionSet.GAMEMASTER, textName, displayName, level.getServer(), player
      );
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.book = input.<ItemStack>read("Book", ItemStack.CODEC).map(book -> this.resolveBook(book, null)).orElse(ItemStack.EMPTY);
      this.pageCount = getPageCount(this.book);
      this.page = Mth.clamp(input.getIntOr("Page", 0), 0, this.pageCount - 1);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.getBook().isEmpty()) {
         output.store("Book", ItemStack.CODEC, this.getBook());
         output.putInt("Page", this.page);
      }
   }

   @Override
   public void clearContent() {
      this.setBook(ItemStack.EMPTY);
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
      if (state.getValue(LecternBlock.HAS_BOOK) && this.level != null) {
         Direction direction = state.getValue(LecternBlock.FACING);
         ItemStack book = this.getBook().copy();
         float xo = 0.25F * direction.getStepX();
         float zo = 0.25F * direction.getStepZ();
         ItemEntity entity = new ItemEntity(this.level, pos.getX() + 0.5 + xo, pos.getY() + 1, pos.getZ() + 0.5 + zo, book);
         entity.setDefaultPickUpDelay();
         this.level.addFreshEntity(entity);
      }
   }

   @Override
   public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
      return new LecternMenu(containerId, this.bookAccess, this.dataAccess);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("container.lectern");
   }

   private static int getPageCount(final ItemStack book) {
      WrittenBookContent writtenContent = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if (writtenContent != null) {
         return writtenContent.pages().size();
      }

      WritableBookContent writableContent = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
      return writableContent != null ? writableContent.pages().size() : 0;
   }
}
