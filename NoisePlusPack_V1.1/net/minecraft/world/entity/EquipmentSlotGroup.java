package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum EquipmentSlotGroup implements StringRepresentable, Iterable<EquipmentSlot> {
   ANY(0, "any", p_335585_ -> true),
   MAINHAND(1, "mainhand", EquipmentSlot.MAINHAND),
   OFFHAND(2, "offhand", EquipmentSlot.OFFHAND),
   HAND(3, "hand", p_330375_ -> p_330375_.getType() == EquipmentSlot.Type.HAND),
   FEET(4, "feet", EquipmentSlot.FEET),
   LEGS(5, "legs", EquipmentSlot.LEGS),
   CHEST(6, "chest", EquipmentSlot.CHEST),
   HEAD(7, "head", EquipmentSlot.HEAD),
   ARMOR(8, "armor", EquipmentSlot::isArmor),
   BODY(9, "body", EquipmentSlot.BODY),
   SADDLE(10, "saddle", EquipmentSlot.SADDLE);

   public static final IntFunction<EquipmentSlotGroup> BY_ID = ByIdMap.continuous(p_331450_ -> p_331450_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final Codec<EquipmentSlotGroup> CODEC = StringRepresentable.fromEnum(EquipmentSlotGroup::values);
   public static final StreamCodec<ByteBuf, EquipmentSlotGroup> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_330886_ -> p_330886_.id);
   private final int id;
   private final String key;
   private final Predicate<EquipmentSlot> predicate;
   private final List<EquipmentSlot> slots;

   EquipmentSlotGroup(final int p_335419_, final String p_332223_, final Predicate<EquipmentSlot> p_333500_) {
      this.id = p_335419_;
      this.key = p_332223_;
      this.predicate = p_333500_;
      this.slots = EquipmentSlot.VALUES.stream().filter(p_333500_).toList();
   }

   EquipmentSlotGroup(final int p_334344_, final String p_328996_, final EquipmentSlot p_332147_) {
      this(p_334344_, p_328996_, p_330757_ -> p_330757_ == p_332147_);
   }

   public static EquipmentSlotGroup bySlot(EquipmentSlot p_331051_) {
      return switch (p_331051_) {
         case MAINHAND -> MAINHAND;
         case OFFHAND -> OFFHAND;
         case FEET -> FEET;
         case LEGS -> LEGS;
         case CHEST -> CHEST;
         case HEAD -> HEAD;
         case BODY -> BODY;
         case SADDLE -> SADDLE;
      };
   }

   @Override
   public String getSerializedName() {
      return this.key;
   }

   public boolean test(EquipmentSlot p_328114_) {
      return this.predicate.test(p_328114_);
   }

   public List<EquipmentSlot> slots() {
      return this.slots;
   }

   @Override
   public Iterator<EquipmentSlot> iterator() {
      return this.slots.iterator();
   }
}
