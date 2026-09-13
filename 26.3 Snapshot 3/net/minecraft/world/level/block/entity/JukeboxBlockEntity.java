package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
   public static final String SONG_ITEM_TAG_ID = "RecordItem";
   public static final String TICKS_SINCE_SONG_STARTED_TAG_ID = "ticks_since_song_started";
   private ItemStack item = ItemStack.EMPTY;
   private final JukeboxSongPlayer jukeboxSongPlayer = new JukeboxSongPlayer(this::onSongChanged, this.getBlockPos());

   public JukeboxBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.JUKEBOX, worldPosition, blockState);
   }

   public JukeboxSongPlayer getSongPlayer() {
      return this.jukeboxSongPlayer;
   }

   public void onSongChanged() {
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.setChanged();
   }

   private void notifyItemChangedInJukebox(final boolean wasInserted) {
      if (this.level != null && this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
         this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, wasInserted), 2);
         this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
      }
   }

   public void popOutTheItem() {
      if (this.level != null && !this.level.isClientSide()) {
         BlockPos pos = this.getBlockPos();
         ItemStack itemBeforePoppingOut = this.getTheItem();
         if (!itemBeforePoppingOut.isEmpty()) {
            this.removeTheItem();
            Vec3 itemPos = Vec3.atLowerCornerWithOffset(pos, 0.5, 1.01, 0.5).offsetRandomXZ(this.level.getRandom(), 0.7F);
            ItemStack itemStack = itemBeforePoppingOut.copy();
            ItemEntity entity = new ItemEntity(this.level, itemPos.x(), itemPos.y(), itemPos.z(), itemStack);
            entity.setDefaultPickUpDelay();
            this.level.addFreshEntity(entity);
            this.onSongChanged();
         }
      }
   }

   public static void tick(final Level level, final BlockPos blockPos, final BlockState blockState, final JukeboxBlockEntity jukebox) {
      jukebox.jukeboxSongPlayer.tick(level, blockState);
   }

   public int getComparatorOutput() {
      return JukeboxSong.fromStack(this.item).map(Holder::value).map(JukeboxSong::comparatorOutput).orElse(0);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      ItemStack newItem = input.<ItemStack>read("RecordItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      if (!this.item.isEmpty() && !ItemStack.isSameItemSameComponents(newItem, this.item)) {
         this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
      }

      this.item = newItem;
      input.getLong("ticks_since_song_started")
         .ifPresent(
            ticksSinceSongStarted -> JukeboxSong.fromStack(this.item)
               .ifPresent(song -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)song, ticksSinceSongStarted))
         );
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.getTheItem().isEmpty()) {
         output.store("RecordItem", ItemStack.CODEC, this.getTheItem());
      }

      if (this.jukeboxSongPlayer.getSong() != null) {
         output.putLong("ticks_since_song_started", this.jukeboxSongPlayer.getTicksSinceSongStarted());
      }
   }

   @Override
   public ItemStack getTheItem() {
      return this.item;
   }

   @Override
   public ItemStack splitTheItem(final int count) {
      ItemStack retrievedItem = this.item;
      this.setTheItem(ItemStack.EMPTY);
      return retrievedItem;
   }

   @Override
   public void setTheItem(final ItemStack itemStack) {
      this.item = itemStack;
      boolean itemWasInserted = !this.item.isEmpty();
      Optional<Holder<JukeboxSong>> maybeSong = JukeboxSong.fromStack(this.item);
      this.notifyItemChangedInJukebox(itemWasInserted);
      if (itemWasInserted && maybeSong.isPresent()) {
         this.jukeboxSongPlayer.play(this.level, maybeSong.get());
      } else {
         this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
      }
   }

   @Override
   public void setRemoved() {
      super.setRemoved();
      this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
      this.level.levelEvent(1011, this.getBlockPos(), 0);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public BlockEntity getContainerBlockEntity() {
      return this;
   }

   @Override
   public boolean canPlaceItem(final int slot, final ItemStack itemStack) {
      return itemStack.has(DataComponents.JUKEBOX_PLAYABLE) && this.getItem(slot).isEmpty();
   }

   @Override
   public boolean canTakeItem(final Container into, final int slot, final ItemStack itemStack) {
      return into.hasAnyMatching(ItemStack::isEmpty);
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
      this.popOutTheItem();
   }

   @VisibleForTesting
   public void setSongItemWithoutPlaying(final ItemStack itemStack) {
      this.item = itemStack;
      JukeboxSong.fromStack(itemStack).ifPresent(song -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)song, 0L));
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.setChanged();
   }

   @VisibleForTesting
   public void tryForcePlaySong() {
      JukeboxSong.fromStack(this.getTheItem()).ifPresent(song -> this.jukeboxSongPlayer.play(this.level, (Holder<JukeboxSong>)song));
   }
}
