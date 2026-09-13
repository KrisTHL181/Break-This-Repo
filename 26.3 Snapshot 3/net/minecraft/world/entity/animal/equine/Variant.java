package net.minecraft.world.entity.animal.equine;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Variant implements StringRepresentable {
   WHITE(0, "white"),
   CREAMY(1, "creamy"),
   CHESTNUT(2, "chestnut"),
   BROWN(3, "brown"),
   BLACK(4, "black"),
   GRAY(5, "gray"),
   DARK_BROWN(6, "dark_brown");

   public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);
   private static final IntFunction<Variant> BY_ID = ByIdMap.continuous(Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::getId);
   private final int id;
   private final String name;

   Variant(final int id, final String name) {
      this.id = id;
      this.name = name;
   }

   public int getId() {
      return this.id;
   }

   public static Variant byId(final int id) {
      return BY_ID.apply(id);
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
