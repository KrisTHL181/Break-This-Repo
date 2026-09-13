package net.minecraft.world.level.block.entity.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class VaultSharedData {
   static final String TAG_NAME = "shared_data";
   static Codec<VaultSharedData> CODEC = RecordCodecBuilder.create(
      p_332167_ -> p_332167_.group(
            ItemStack.lenientOptionalFieldOf("display_item").forGetter(p_328885_ -> p_328885_.displayItem),
            UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("connected_players", Set.of()).forGetter(p_333733_ -> p_333733_.connectedPlayers),
            Codec.DOUBLE
               .lenientOptionalFieldOf("connected_particles_range", VaultConfig.DEFAULT.deactivationRange())
               .forGetter(p_333675_ -> p_333675_.connectedParticlesRange)
         )
         .apply(p_332167_, VaultSharedData::new)
   );
   private ItemStack displayItem = ItemStack.EMPTY;
   private Set<UUID> connectedPlayers = new ObjectLinkedOpenHashSet();
   private double connectedParticlesRange = VaultConfig.DEFAULT.deactivationRange();
   boolean isDirty;

   VaultSharedData(ItemStack p_336127_, Set<UUID> p_328242_, double p_334724_) {
      this.displayItem = p_336127_;
      this.connectedPlayers.addAll(p_328242_);
      this.connectedParticlesRange = p_334724_;
   }

   VaultSharedData() {
   }

   public ItemStack getDisplayItem() {
      return this.displayItem;
   }

   public boolean hasDisplayItem() {
      return !this.displayItem.isEmpty();
   }

   public void setDisplayItem(ItemStack p_328271_) {
      if (!ItemStack.matches(this.displayItem, p_328271_)) {
         this.displayItem = p_328271_.copy();
         this.markDirty();
      }
   }

   boolean hasConnectedPlayers() {
      return !this.connectedPlayers.isEmpty();
   }

   Set<UUID> getConnectedPlayers() {
      return this.connectedPlayers;
   }

   double connectedParticlesRange() {
      return this.connectedParticlesRange;
   }

   void updateConnectedPlayersWithinRange(ServerLevel p_335653_, BlockPos p_328626_, VaultServerData p_333530_, VaultConfig p_327683_, double p_332168_) {
      Set<UUID> set = p_327683_.playerDetector()
         .detect(p_335653_, p_327683_.entitySelector(), p_328626_, p_332168_, false)
         .stream()
         .filter(p_335249_ -> !p_333530_.getRewardedPlayers().contains(p_335249_))
         .collect(Collectors.toSet());
      if (!this.connectedPlayers.equals(set)) {
         this.connectedPlayers = set;
         this.markDirty();
      }
   }

   private void markDirty() {
      this.isDirty = true;
   }

   void set(VaultSharedData p_334535_) {
      this.displayItem = p_334535_.displayItem;
      this.connectedPlayers = p_334535_.connectedPlayers;
      this.connectedParticlesRange = p_334535_.connectedParticlesRange;
   }
}
