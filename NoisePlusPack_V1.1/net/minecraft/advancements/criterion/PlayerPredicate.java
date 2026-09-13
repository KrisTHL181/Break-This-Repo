package net.minecraft.advancements.criterion;

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
   GameTypePredicate gameType,
   List<PlayerPredicate.StatMatcher<?>> stats,
   Object2BooleanMap<ResourceKey<Recipe<?>>> recipes,
   Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements,
   Optional<EntityPredicate> lookingAt,
   Optional<InputPredicate> input
) implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   public static final MapCodec<PlayerPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_457693_ -> p_457693_.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate::level),
            GameTypePredicate.CODEC.optionalFieldOf("gamemode", GameTypePredicate.ANY).forGetter(PlayerPredicate::gameType),
            PlayerPredicate.StatMatcher.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(PlayerPredicate::stats),
            ExtraCodecs.object2BooleanMap(Recipe.KEY_CODEC).optionalFieldOf("recipes", Object2BooleanMaps.emptyMap()).forGetter(PlayerPredicate::recipes),
            Codec.unboundedMap(Identifier.CODEC, PlayerPredicate.AdvancementPredicate.CODEC)
               .optionalFieldOf("advancements", Map.of())
               .forGetter(PlayerPredicate::advancements),
            EntityPredicate.CODEC.optionalFieldOf("looking_at").forGetter(PlayerPredicate::lookingAt),
            InputPredicate.CODEC.optionalFieldOf("input").forGetter(PlayerPredicate::input)
         )
         .apply(p_457693_, PlayerPredicate::new)
   );

   @Override
   public boolean matches(Entity p_459596_, ServerLevel p_451975_, @Nullable Vec3 p_452258_) {
      if (!(p_459596_ instanceof ServerPlayer serverplayer)) {
         return false;
      } else {
         if (!this.level.matches(serverplayer.experienceLevel)) {
            return false;
         }

         if (!this.gameType.matches(serverplayer.gameMode())) {
            return false;
         }

         StatsCounter statscounter = serverplayer.getStats();

         for (PlayerPredicate.StatMatcher<?> statmatcher : this.stats) {
            if (!statmatcher.matches(statscounter)) {
               return false;
            }
         }

         ServerRecipeBook serverrecipebook = serverplayer.getRecipeBook();
         ObjectIterator var13 = this.recipes.object2BooleanEntrySet().iterator();

         while (var13.hasNext()) {
            Entry<ResourceKey<Recipe<?>>> entry = (Entry<ResourceKey<Recipe<?>>>)var13.next();
            if (serverrecipebook.contains((ResourceKey<Recipe<?>>)entry.getKey()) != entry.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements playeradvancements = serverplayer.getAdvancements();
            ServerAdvancementManager serveradvancementmanager = serverplayer.level().getServer().getAdvancements();

            for (java.util.Map.Entry<Identifier, PlayerPredicate.AdvancementPredicate> entry1 : this.advancements.entrySet()) {
               AdvancementHolder advancementholder = serveradvancementmanager.get(entry1.getKey());
               if (advancementholder == null || !entry1.getValue().test(playeradvancements.getOrStartProgress(advancementholder))) {
                  return false;
               }
            }
         }

         if (this.lookingAt.isPresent()) {
            Vec3 vec3 = serverplayer.getEyePosition();
            Vec3 vec31 = serverplayer.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * 100.0, vec31.y * 100.0, vec31.z * 100.0);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(
               serverplayer.level(), serverplayer, vec3, vec32, new AABB(vec3, vec32).inflate(1.0), p_459574_ -> !p_459574_.isSpectator(), 0.0F
            );
            if (entityhitresult == null || entityhitresult.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity entity = entityhitresult.getEntity();
            if (!this.lookingAt.get().matches(serverplayer, entity) || !serverplayer.hasLineOfSight(entity)) {
               return false;
            }
         }

         return !this.input.isPresent() || this.input.get().matches(serverplayer.getLastClientInput());
      }
   }

   @Override
   public MapCodec<PlayerPredicate> codec() {
      return EntitySubPredicates.PLAYER;
   }

   record AdvancementCriterionsPredicate(Object2BooleanMap<String> criterions) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementCriterionsPredicate> CODEC = ExtraCodecs.object2BooleanMap(Codec.STRING)
         .xmap(PlayerPredicate.AdvancementCriterionsPredicate::new, PlayerPredicate.AdvancementCriterionsPredicate::criterions);

      public boolean test(AdvancementProgress p_456479_) {
         ObjectIterator var2 = this.criterions.object2BooleanEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<String> entry = (Entry<String>)var2.next();
            CriterionProgress criterionprogress = p_456479_.getCriterion((String)entry.getKey());
            if (criterionprogress == null || criterionprogress.isDone() != entry.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   record AdvancementDonePredicate(boolean state) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementDonePredicate> CODEC = Codec.BOOL
         .xmap(PlayerPredicate.AdvancementDonePredicate::new, PlayerPredicate.AdvancementDonePredicate::state);

      public boolean test(AdvancementProgress p_454904_) {
         return p_454904_.isDone() == this.state;
      }
   }

   interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(
            PlayerPredicate.AdvancementDonePredicate.CODEC, PlayerPredicate.AdvancementCriterionsPredicate.CODEC
         )
         .xmap(Either::unwrap, p_457675_ -> {
            if (p_457675_ instanceof PlayerPredicate.AdvancementDonePredicate playerpredicate$advancementdonepredicate) {
               return Either.left(playerpredicate$advancementdonepredicate);
            } else if (p_457675_ instanceof PlayerPredicate.AdvancementCriterionsPredicate playerpredicate$advancementcriterionspredicate) {
               return Either.right(playerpredicate$advancementcriterionspredicate);
            } else {
               throw new UnsupportedOperationException();
            }
         });
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private GameTypePredicate gameType = GameTypePredicate.ANY;
      private final com.google.common.collect.ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
      private final Object2BooleanMap<ResourceKey<Recipe<?>>> recipes = new Object2BooleanOpenHashMap();
      private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private Optional<EntityPredicate> lookingAt = Optional.empty();
      private Optional<InputPredicate> input = Optional.empty();

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints p_452423_) {
         this.level = p_452423_;
         return this;
      }

      public <T> PlayerPredicate.Builder addStat(StatType<T> p_458070_, Holder.Reference<T> p_455437_, MinMaxBounds.Ints p_450360_) {
         this.stats.add(new PlayerPredicate.StatMatcher<>(p_458070_, p_455437_, p_450360_));
         return this;
      }

      public PlayerPredicate.Builder addRecipe(ResourceKey<Recipe<?>> p_458004_, boolean p_450343_) {
         this.recipes.put(p_458004_, p_450343_);
         return this;
      }

      public PlayerPredicate.Builder setGameType(GameTypePredicate p_453859_) {
         this.gameType = p_453859_;
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(EntityPredicate.Builder p_458912_) {
         this.lookingAt = Optional.of(p_458912_.build());
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(Identifier p_460456_, boolean p_459615_) {
         this.advancements.put(p_460456_, new PlayerPredicate.AdvancementDonePredicate(p_459615_));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(Identifier p_458412_, Map<String, Boolean> p_459787_) {
         this.advancements.put(p_458412_, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap(p_459787_)));
         return this;
      }

      public PlayerPredicate.Builder hasInput(InputPredicate p_452081_) {
         this.input = Optional.of(p_452081_);
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.gameType, this.stats.build(), this.recipes, this.advancements, this.lookingAt, this.input);
      }
   }

   record StatMatcher<T>(StatType<T> type, Holder<T> value, MinMaxBounds.Ints range, Supplier<Stat<T>> stat) {
      public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = BuiltInRegistries.STAT_TYPE
         .byNameCodec()
         .dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createTypedCodec);

      public StatMatcher(StatType<T> p_455449_, Holder<T> p_450753_, MinMaxBounds.Ints p_453450_) {
         this(p_455449_, p_450753_, p_453450_, Suppliers.memoize(() -> p_455449_.get(p_450753_.value())));
      }

      private static <T> MapCodec<PlayerPredicate.StatMatcher<T>> createTypedCodec(StatType<T> p_453277_) {
         return RecordCodecBuilder.mapCodec(
            p_456352_ -> p_456352_.group(
                  p_453277_.getRegistry().holderByNameCodec().fieldOf("stat").forGetter(PlayerPredicate.StatMatcher::value),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate.StatMatcher::range)
               )
               .apply(p_456352_, (p_458515_, p_458275_) -> new PlayerPredicate.StatMatcher<>(p_453277_, p_458515_, p_458275_))
         );
      }

      public boolean matches(StatsCounter p_460904_) {
         return this.range.matches(p_460904_.getValue(this.stat.get()));
      }
   }
}
