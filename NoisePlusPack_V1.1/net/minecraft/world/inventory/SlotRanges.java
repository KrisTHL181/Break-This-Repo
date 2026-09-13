package net.minecraft.world.inventory;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public class SlotRanges {
   private static final List<SlotRange> SLOTS = Util.make(new ArrayList<>(), p_329672_ -> {
      addSingleSlot(p_329672_, "contents", 0);
      addSlotRange(p_329672_, "container.", 0, 54);
      addSlotRange(p_329672_, "hotbar.", 0, 9);
      addSlotRange(p_329672_, "inventory.", 9, 27);
      addSlotRange(p_329672_, "enderchest.", 200, 27);
      addSlotRange(p_329672_, "villager.", 300, 8);
      addSlotRange(p_329672_, "horse.", 500, 15);
      int i = EquipmentSlot.MAINHAND.getIndex(98);
      int j = EquipmentSlot.OFFHAND.getIndex(98);
      addSingleSlot(p_329672_, "weapon", i);
      addSingleSlot(p_329672_, "weapon.mainhand", i);
      addSingleSlot(p_329672_, "weapon.offhand", j);
      addSlots(p_329672_, "weapon.*", i, j);
      i = EquipmentSlot.HEAD.getIndex(100);
      j = EquipmentSlot.CHEST.getIndex(100);
      int k = EquipmentSlot.LEGS.getIndex(100);
      int l = EquipmentSlot.FEET.getIndex(100);
      int i1 = EquipmentSlot.BODY.getIndex(105);
      addSingleSlot(p_329672_, "armor.head", i);
      addSingleSlot(p_329672_, "armor.chest", j);
      addSingleSlot(p_329672_, "armor.legs", k);
      addSingleSlot(p_329672_, "armor.feet", l);
      addSingleSlot(p_329672_, "armor.body", i1);
      addSlots(p_329672_, "armor.*", i, j, k, l, i1);
      addSingleSlot(p_329672_, "saddle", EquipmentSlot.SADDLE.getIndex(106));
      addSingleSlot(p_329672_, "horse.chest", 499);
      addSingleSlot(p_329672_, "player.cursor", 499);
      addSlotRange(p_329672_, "player.crafting.", 500, 4);
   });
   public static final Codec<SlotRange> CODEC = StringRepresentable.fromValues(() -> SLOTS.toArray(SlotRange[]::new));
   private static final Function<String, @Nullable SlotRange> NAME_LOOKUP = StringRepresentable.createNameLookup(SLOTS.toArray(SlotRange[]::new));

   private static SlotRange create(String p_328484_, int p_335544_) {
      return SlotRange.of(p_328484_, IntLists.singleton(p_335544_));
   }

   private static SlotRange create(String p_330835_, IntList p_333821_) {
      return SlotRange.of(p_330835_, IntLists.unmodifiable(p_333821_));
   }

   private static SlotRange create(String p_333478_, int... p_336035_) {
      return SlotRange.of(p_333478_, IntList.of(p_336035_));
   }

   private static void addSingleSlot(List<SlotRange> p_332328_, String p_334715_, int p_328171_) {
      p_332328_.add(create(p_334715_, p_328171_));
   }

   private static void addSlotRange(List<SlotRange> p_328374_, String p_331284_, int p_329588_, int p_336322_) {
      IntList intlist = new IntArrayList(p_336322_);

      for (int i = 0; i < p_336322_; i++) {
         int j = p_329588_ + i;
         p_328374_.add(create(p_331284_ + i, j));
         intlist.add(j);
      }

      p_328374_.add(create(p_331284_ + "*", intlist));
   }

   private static void addSlots(List<SlotRange> p_329581_, String p_328279_, int... p_332253_) {
      p_329581_.add(create(p_328279_, p_332253_));
   }

   public static @Nullable SlotRange nameToIds(String p_328330_) {
      return NAME_LOOKUP.apply(p_328330_);
   }

   public static Stream<String> allNames() {
      return SLOTS.stream().map(StringRepresentable::getSerializedName);
   }

   public static Stream<String> singleSlotNames() {
      return SLOTS.stream().filter(p_336128_ -> p_336128_.size() == 1).map(StringRepresentable::getSerializedName);
   }
}
