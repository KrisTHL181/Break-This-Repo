package net.minecraft.world.level.gamerules;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

public final class GameRuleMap {
   public static final Codec<GameRuleMap> CODEC = Codec.dispatchedMap(BuiltInRegistries.GAME_RULE.byNameCodec(), GameRule::valueCodec)
      .xmap(GameRuleMap::ofTrusted, GameRuleMap::map);
   private final Reference2ObjectMap<GameRule<?>, Object> map;

   GameRuleMap(Reference2ObjectMap<GameRule<?>, Object> p_457749_) {
      this.map = p_457749_;
   }

   private static GameRuleMap ofTrusted(Map<GameRule<?>, Object> p_452999_) {
      return new GameRuleMap(new Reference2ObjectOpenHashMap(p_452999_));
   }

   public static GameRuleMap of() {
      return new GameRuleMap(new Reference2ObjectOpenHashMap());
   }

   public static GameRuleMap of(Stream<GameRule<?>> p_455066_) {
      Reference2ObjectOpenHashMap<GameRule<?>, Object> reference2objectopenhashmap = new Reference2ObjectOpenHashMap();
      p_455066_.forEach(p_457994_ -> reference2objectopenhashmap.put(p_457994_, p_457994_.defaultValue()));
      return new GameRuleMap(reference2objectopenhashmap);
   }

   public static GameRuleMap copyOf(GameRuleMap p_450450_) {
      return new GameRuleMap(new Reference2ObjectOpenHashMap(p_450450_.map));
   }

   public boolean has(GameRule<?> p_450351_) {
      return this.map.containsKey(p_450351_);
   }

   public <T> @Nullable T get(GameRule<T> p_459632_) {
      return (T)this.map.get(p_459632_);
   }

   public <T> void set(GameRule<T> p_458714_, T p_451593_) {
      this.map.put(p_458714_, p_451593_);
   }

   public <T> @Nullable T remove(GameRule<T> p_452000_) {
      return (T)this.map.remove(p_452000_);
   }

   public Set<GameRule<?>> keySet() {
      return this.map.keySet();
   }

   public int size() {
      return this.map.size();
   }

   @Override
   public String toString() {
      return this.map.toString();
   }

   public GameRuleMap withOther(GameRuleMap p_459512_) {
      GameRuleMap gamerulemap = copyOf(this);
      gamerulemap.setFromIf(p_459512_, p_452520_ -> true);
      return gamerulemap;
   }

   public void setFromIf(GameRuleMap p_456541_, Predicate<GameRule<?>> p_451766_) {
      for (GameRule<?> gamerule : p_456541_.keySet()) {
         if (p_451766_.test(gamerule)) {
            setGameRule(p_456541_, gamerule, this);
         }
      }
   }

   private static <T> void setGameRule(GameRuleMap p_457384_, GameRule<T> p_459809_, GameRuleMap p_455366_) {
      p_455366_.set(p_459809_, Objects.requireNonNull(p_457384_.get(p_459809_)));
   }

   private Reference2ObjectMap<GameRule<?>, Object> map() {
      return this.map;
   }

   @Override
   public boolean equals(Object p_455638_) {
      if (p_455638_ == this) {
         return true;
      } else if (p_455638_ != null && p_455638_.getClass() == this.getClass()) {
         GameRuleMap gamerulemap = (GameRuleMap)p_455638_;
         return Objects.equals(this.map, gamerulemap.map);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.map);
   }

   public static class Builder {
      final Reference2ObjectMap<GameRule<?>, Object> map = new Reference2ObjectOpenHashMap();

      public <T> GameRuleMap.Builder set(GameRule<T> p_458474_, T p_454932_) {
         this.map.put(p_458474_, p_454932_);
         return this;
      }

      public GameRuleMap build() {
         return new GameRuleMap(this.map);
      }
   }
}
