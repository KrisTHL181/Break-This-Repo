package net.minecraft.stats;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

public class Stat<T> extends ObjectiveCriteria {
   public static final StreamCodec<RegistryFriendlyByteBuf, Stat<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.STAT_TYPE)
      .dispatch(Stat::getType, StatType::streamCodec);
   private final StatFormatter formatter;
   private final T value;
   private final StatType<T> type;

   protected Stat(StatType<T> p_12856_, T p_12857_, StatFormatter p_12858_) {
      super(buildName(p_12856_, p_12857_));
      this.type = p_12856_;
      this.formatter = p_12858_;
      this.value = p_12857_;
   }

   public static <T> String buildName(StatType<T> p_12863_, T p_12864_) {
      return locationToKey(BuiltInRegistries.STAT_TYPE.getKey(p_12863_)) + ":" + locationToKey(p_12863_.getRegistry().getKey(p_12864_));
   }

   private static String locationToKey(@Nullable Identifier p_452457_) {
      return p_452457_.toString().replace(':', '.');
   }

   public StatType<T> getType() {
      return this.type;
   }

   public T getValue() {
      return this.value;
   }

   public String format(int p_12861_) {
      return this.formatter.format(p_12861_);
   }

   @Override
   public boolean equals(Object p_12869_) {
      return this == p_12869_ || p_12869_ instanceof Stat && Objects.equals(this.getName(), ((Stat)p_12869_).getName());
   }

   @Override
   public int hashCode() {
      return this.getName().hashCode();
   }

   @Override
   public String toString() {
      return "Stat{name=" + this.getName() + ", formatter=" + this.formatter + "}";
   }
}
