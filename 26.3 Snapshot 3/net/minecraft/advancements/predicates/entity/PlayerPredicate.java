package net.minecraft.advancements.predicates.entity;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.predicates.FoodPredicate;
import net.minecraft.advancements.predicates.GameTypePredicate;
import net.minecraft.advancements.predicates.InputPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PlayerPredicate(
   MinMaxBounds.Ints level,
   FoodPredicate food,
   GameTypePredicate gameType,
   List<PlayerPredicate.StatMatcher<?>> stats,
   Object2BooleanMap<ResourceKey<Recipe<?>>> recipes,
   Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements,
   Optional<EntityPredicate> lookingAt,
   Optional<InputPredicate> input
) implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   public static final Codec<PlayerPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate::level),
            FoodPredicate.CODEC.optionalFieldOf("food", FoodPredicate.ANY).forGetter(PlayerPredicate::food),
            GameTypePredicate.CODEC.optionalFieldOf("gamemode", GameTypePredicate.ANY).forGetter(PlayerPredicate::gameType),
            PlayerPredicate.StatMatcher.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(PlayerPredicate::stats),
            ExtraCodecs.object2BooleanMap(Recipe.KEY_CODEC).optionalFieldOf("recipes", Object2BooleanMaps.emptyMap()).forGetter(PlayerPredicate::recipes),
            Codec.unboundedMap(Identifier.CODEC, PlayerPredicate.AdvancementPredicate.CODEC)
               .optionalFieldOf("advancements", Map.of())
               .forGetter(PlayerPredicate::advancements),
            EntityPredicate.CODEC.optionalFieldOf("looking_at").forGetter(PlayerPredicate::lookingAt),
            InputPredicate.CODEC.optionalFieldOf("input").forGetter(PlayerPredicate::input)
         )
         .apply(i, PlayerPredicate::new)
   );

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      } else {
         if (!this.level.matches(player.experienceLevel)) {
            return false;
         }

         if (!this.food.matches(player.getFoodData())) {
            return false;
         }

         if (!this.gameType.matches(player.gameMode())) {
            return false;
         }

         StatsCounter stats = player.getStats();

         for (PlayerPredicate.StatMatcher<?> stat : this.stats) {
            if (!stat.matches(stats)) {
               return false;
            }
         }

         ServerRecipeBook recipes = player.getRecipeBook();
         ObjectIterator var13 = this.recipes.object2BooleanEntrySet().iterator();

         while (var13.hasNext()) {
            Entry<ResourceKey<Recipe<?>>> e = (Entry<ResourceKey<Recipe<?>>>)var13.next();
            if (recipes.contains((ResourceKey<Recipe<?>>)e.getKey()) != e.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements advancements = player.getAdvancements();
            ServerAdvancementManager serverAdvancements = player.level().getServer().getAdvancements();

            for (java.util.Map.Entry<Identifier, PlayerPredicate.AdvancementPredicate> entry : this.advancements.entrySet()) {
               AdvancementHolder advancement = serverAdvancements.get(entry.getKey());
               if (advancement == null || !entry.getValue().test(advancements.getOrStartProgress(advancement))) {
                  return false;
               }
            }
         }

         if (this.lookingAt.isPresent()) {
            Vec3 from = player.getEyePosition();
            Vec3 viewVec = player.getViewVector(1.0F);
            Vec3 to = from.add(viewVec.x * 100.0, viewVec.y * 100.0, viewVec.z * 100.0);
            EntityHitResult lookingAtResult = ProjectileUtil.getEntityHitResult(
               player.level(), player, from, to, new AABB(from, to).inflate(1.0), e -> !e.isSpectator(), 0.0F
            );
            if (lookingAtResult == null || lookingAtResult.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity lookingAtEntity = lookingAtResult.getEntity();
            if (!this.lookingAt.get().matches(player, lookingAtEntity) || !player.hasLineOfSight(lookingAtEntity)) {
               return false;
            }
         }

         return !this.input.isPresent() || this.input.get().matches(player.getLastClientInput());
      }
   }

   private record AdvancementCriterionsPredicate(Object2BooleanMap<String> criterions) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementCriterionsPredicate> CODEC = ExtraCodecs.object2BooleanMap(Codec.STRING)
         .xmap(PlayerPredicate.AdvancementCriterionsPredicate::new, PlayerPredicate.AdvancementCriterionsPredicate::criterions);

      public boolean test(final AdvancementProgress progress) {
         ObjectIterator var2 = this.criterions.object2BooleanEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<String> e = (Entry<String>)var2.next();
            CriterionProgress criterion = progress.getCriterion((String)e.getKey());
            if (criterion == null || criterion.isDone() != e.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   private record AdvancementDonePredicate(boolean state) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementDonePredicate> CODEC = Codec.BOOL
         .xmap(PlayerPredicate.AdvancementDonePredicate::new, PlayerPredicate.AdvancementDonePredicate::state);

      public boolean test(final AdvancementProgress progress) {
         return progress.isDone() == this.state;
      }
   }

   private interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(
            PlayerPredicate.AdvancementDonePredicate.CODEC, PlayerPredicate.AdvancementCriterionsPredicate.CODEC
         )
         .xmap(Either::unwrap, predicate -> {
            if (predicate instanceof PlayerPredicate.AdvancementDonePredicate done) {
               return Either.left(done);
            } else if (predicate instanceof PlayerPredicate.AdvancementCriterionsPredicate criterions) {
               return Either.right(criterions);
            } else {
               throw new UnsupportedOperationException();
            }
         });
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private FoodPredicate food = FoodPredicate.ANY;
      private GameTypePredicate gameType = GameTypePredicate.ANY;
      private final com.google.common.collect.ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
      private final Object2BooleanMap<ResourceKey<Recipe<?>>> recipes = new Object2BooleanOpenHashMap();
      private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private Optional<EntityPredicate> lookingAt = Optional.empty();
      private Optional<InputPredicate> input = Optional.empty();

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(final MinMaxBounds.Ints level) {
         this.level = level;
         return this;
      }

      public PlayerPredicate.Builder setFood(final FoodPredicate food) {
         this.food = food;
         return this;
      }

      public <T> PlayerPredicate.Builder addStat(final StatType<T> type, final Holder.Reference<T> value, final MinMaxBounds.Ints range) {
         this.stats.add(new PlayerPredicate.StatMatcher<>(type, value, range));
         return this;
      }

      public PlayerPredicate.Builder addRecipe(final ResourceKey<Recipe<?>> recipe, final boolean present) {
         this.recipes.put(recipe, present);
         return this;
      }

      public PlayerPredicate.Builder setGameType(final GameTypePredicate gameType) {
         this.gameType = gameType;
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(final EntityPredicate.Builder lookingAt) {
         this.lookingAt = Optional.of(lookingAt.build());
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(final Identifier advancement, final boolean isDone) {
         this.advancements.put(advancement, new PlayerPredicate.AdvancementDonePredicate(isDone));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(final Identifier advancement, final Map<String, Boolean> criterions) {
         this.advancements.put(advancement, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap(criterions)));
         return this;
      }

      public PlayerPredicate.Builder hasInput(final InputPredicate input) {
         this.input = Optional.of(input);
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.food, this.gameType, this.stats.build(), this.recipes, this.advancements, this.lookingAt, this.input);
      }
   }

   private record StatMatcher<T>(StatType<T> type, Holder<T> value, MinMaxBounds.Ints range, Supplier<Stat<T>> stat) {
      public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = BuiltInRegistries.STAT_TYPE
         .byNameCodec()
         .dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createTypedCodec);

      public StatMatcher(final StatType<T> type, final Holder<T> value, final MinMaxBounds.Ints range) {
         this(type, value, range, Suppliers.memoize(() -> type.get(value.value())));
      }

      private static <T> MapCodec<PlayerPredicate.StatMatcher<T>> createTypedCodec(final StatType<T> type) {
         return RecordCodecBuilder.mapCodec(
            i -> i.group(
                  type.getRegistry().holderByNameCodec().fieldOf("stat").forGetter(PlayerPredicate.StatMatcher::value),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate.StatMatcher::range)
               )
               .apply(i, (value, range) -> new PlayerPredicate.StatMatcher<>(type, value, range))
         );
      }

      public boolean matches(final StatsCounter counter) {
         return this.range.matches(counter.getValue(this.stat.get()));
      }
   }
}
