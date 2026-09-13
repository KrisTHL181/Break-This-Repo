package net.minecraft.world.level.block.entity.vault;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class VaultServerData {
   static final String TAG_NAME = "server_data";
   static Codec<VaultServerData> CODEC = RecordCodecBuilder.create(
      p_331703_ -> p_331703_.group(
            UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("rewarded_players", Set.of()).forGetter(p_331366_ -> p_331366_.rewardedPlayers),
            Codec.LONG.lenientOptionalFieldOf("state_updating_resumes_at", 0L).forGetter(p_329044_ -> p_329044_.stateUpdatingResumesAt),
            ItemStack.CODEC.listOf().lenientOptionalFieldOf("items_to_eject", List.of()).forGetter(p_328322_ -> p_328322_.itemsToEject),
            Codec.INT.lenientOptionalFieldOf("total_ejections_needed", 0).forGetter(p_329419_ -> p_329419_.totalEjectionsNeeded)
         )
         .apply(p_331703_, VaultServerData::new)
   );
   private static final int MAX_REWARD_PLAYERS = 128;
   private final Set<UUID> rewardedPlayers = new ObjectLinkedOpenHashSet();
   private long stateUpdatingResumesAt;
   private final List<ItemStack> itemsToEject = new ObjectArrayList();
   private long lastInsertFailTimestamp;
   private int totalEjectionsNeeded;
   boolean isDirty;

   VaultServerData(Set<UUID> p_334629_, long p_331265_, List<ItemStack> p_330511_, int p_333688_) {
      this.rewardedPlayers.addAll(p_334629_);
      this.stateUpdatingResumesAt = p_331265_;
      this.itemsToEject.addAll(p_330511_);
      this.totalEjectionsNeeded = p_333688_;
   }

   VaultServerData() {
   }

   void setLastInsertFailTimestamp(long p_336284_) {
      this.lastInsertFailTimestamp = p_336284_;
   }

   long getLastInsertFailTimestamp() {
      return this.lastInsertFailTimestamp;
   }

   Set<UUID> getRewardedPlayers() {
      return this.rewardedPlayers;
   }

   boolean hasRewardedPlayer(Player p_336078_) {
      return this.rewardedPlayers.contains(p_336078_.getUUID());
   }

   @VisibleForTesting
   public void addToRewardedPlayers(Player p_332874_) {
      this.rewardedPlayers.add(p_332874_.getUUID());
      if (this.rewardedPlayers.size() > 128) {
         Iterator<UUID> iterator = this.rewardedPlayers.iterator();
         if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
         }
      }

      this.markChanged();
   }

   long stateUpdatingResumesAt() {
      return this.stateUpdatingResumesAt;
   }

   void pauseStateUpdatingUntil(long p_330777_) {
      this.stateUpdatingResumesAt = p_330777_;
      this.markChanged();
   }

   List<ItemStack> getItemsToEject() {
      return this.itemsToEject;
   }

   void markEjectionFinished() {
      this.totalEjectionsNeeded = 0;
      this.markChanged();
   }

   void setItemsToEject(List<ItemStack> p_332570_) {
      this.itemsToEject.clear();
      this.itemsToEject.addAll(p_332570_);
      this.totalEjectionsNeeded = this.itemsToEject.size();
      this.markChanged();
   }

   ItemStack getNextItemToEject() {
      return this.itemsToEject.isEmpty() ? ItemStack.EMPTY : Objects.requireNonNullElse(this.itemsToEject.get(this.itemsToEject.size() - 1), ItemStack.EMPTY);
   }

   ItemStack popNextItemToEject() {
      if (this.itemsToEject.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.markChanged();
      return Objects.requireNonNullElse(this.itemsToEject.remove(this.itemsToEject.size() - 1), ItemStack.EMPTY);
   }

   void set(VaultServerData p_329637_) {
      this.stateUpdatingResumesAt = p_329637_.stateUpdatingResumesAt();
      this.itemsToEject.clear();
      this.itemsToEject.addAll(p_329637_.itemsToEject);
      this.rewardedPlayers.clear();
      this.rewardedPlayers.addAll(p_329637_.rewardedPlayers);
   }

   private void markChanged() {
      this.isDirty = true;
   }

   public float ejectionProgress() {
      return this.totalEjectionsNeeded == 1 ? 1.0F : 1.0F - Mth.inverseLerp(this.getItemsToEject().size(), 1.0F, this.totalEjectionsNeeded);
   }
}
