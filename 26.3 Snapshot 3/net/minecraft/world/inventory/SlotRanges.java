package net.minecraft.world.inventory;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.commands.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public class SlotRanges {
   public static final int MOB_INVENTORY_SLOT_OFFSET = 300;
   public static final int MOB_INVENTORY_SIZE = 8;
   private static final List<SlotRange> SLOTS = Util.make(new ArrayList<>(), values -> {
      addSingleSlot(values, "contents", 0);
      addSlotRange(values, "container.", 0, 54);
      addSlotRange(values, "hotbar.", 0, 9);
      addSlotRange(values, "inventory.", 9, 27);
      addSlotRange(values, "enderchest.", 200, 27);
      addSlotRange(values, "mob.inventory.", 300, 8);
      addSlotRange(values, "horse.", 500, 15);
      int mainhand = EquipmentSlot.MAINHAND.getIndex(98);
      int offhand = EquipmentSlot.OFFHAND.getIndex(98);
      addSingleSlot(values, "weapon", mainhand);
      addSingleSlot(values, "weapon.mainhand", mainhand);
      addSingleSlot(values, "weapon.offhand", offhand);
      addSlots(values, "weapon.*", mainhand, offhand);
      mainhand = EquipmentSlot.HEAD.getIndex(100);
      offhand = EquipmentSlot.CHEST.getIndex(100);
      int legs = EquipmentSlot.LEGS.getIndex(100);
      int feet = EquipmentSlot.FEET.getIndex(100);
      int body = EquipmentSlot.BODY.getIndex(105);
      addSingleSlot(values, "armor.head", mainhand);
      addSingleSlot(values, "armor.chest", offhand);
      addSingleSlot(values, "armor.legs", legs);
      addSingleSlot(values, "armor.feet", feet);
      addSingleSlot(values, "armor.body", body);
      addSlots(values, "armor.*", mainhand, offhand, legs, feet, body);
      addSingleSlot(values, "saddle", EquipmentSlot.SADDLE.getIndex(106));
      addSingleSlot(values, "horse.chest", 499);
      addSingleSlot(values, "player.cursor", 499);
      addSlotRange(values, "player.crafting.", 500, 4);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN = new DynamicCommandExceptionType(id -> Component.translatableEscape("slot.unknown", id));
   public static final Codec<SlotRange> CODEC = StringRepresentable.fromValues(() -> SLOTS.toArray(SlotRange[]::new));
   private static final Function<String, @Nullable SlotRange> NAME_LOOKUP = StringRepresentable.createNameLookup(SLOTS.toArray(SlotRange[]::new));

   private static SlotRange create(final String name, final int id) {
      return SlotRange.of(name, IntLists.singleton(id));
   }

   private static SlotRange create(final String name, final IntList ids) {
      return SlotRange.of(name, IntLists.unmodifiable(ids));
   }

   private static SlotRange create(final String name, final int... ids) {
      return SlotRange.of(name, IntList.of(ids));
   }

   private static void addSingleSlot(final List<SlotRange> output, final String name, final int id) {
      output.add(create(name, id));
   }

   private static void addSlotRange(final List<SlotRange> output, final String prefix, final int offset, final int size) {
      IntList allSlots = new IntArrayList(size);

      for (int i = 0; i < size; i++) {
         int slotId = offset + i;
         output.add(create(prefix + i, slotId));
         allSlots.add(slotId);
      }

      output.add(create(prefix + "*", allSlots));
   }

   private static void addSlots(final List<SlotRange> output, final String name, final int... values) {
      output.add(create(name, values));
   }

   public static @Nullable SlotRange nameToIds(final String name) {
      return NAME_LOOKUP.apply(name);
   }

   public static Stream<String> allNames() {
      return SLOTS.stream().map(StringRepresentable::getSerializedName);
   }

   public static Stream<String> singleSlotNames() {
      return SLOTS.stream().filter(e -> e.size() == 1).map(StringRepresentable::getSerializedName);
   }

   public static @Nullable SlotRange tryRead(final StringReader reader) {
      String name = ParserUtils.readWhile(reader, c -> c != ' ');
      return nameToIds(name);
   }

   public static SlotRange read(final StringReader reader) throws CommandSyntaxException {
      String name = ParserUtils.readWhile(reader, c -> c != ' ');
      SlotRange result = nameToIds(name);
      if (result == null) {
         throw ERROR_UNKNOWN.createWithContext(reader, name);
      } else {
         return result;
      }
   }
}
