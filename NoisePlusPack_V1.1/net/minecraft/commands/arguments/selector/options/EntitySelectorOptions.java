package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public class EntitySelectorOptions {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, EntitySelectorOptions.Option> OPTIONS = Maps.newHashMap();
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType(
      p_308416_ -> Component.translatableEscape("argument.entity.options.unknown", p_308416_)
   );
   public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType(
      p_308412_ -> Component.translatableEscape("argument.entity.options.inapplicable", p_308412_)
   );
   public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.distance.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.level.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.limit.toosmall")
   );
   public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType(
      p_308411_ -> Component.translatableEscape("argument.entity.options.sort.irreversible", p_308411_)
   );
   public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType(
      p_308419_ -> Component.translatableEscape("argument.entity.options.mode.invalid", p_308419_)
   );
   public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType(
      p_308410_ -> Component.translatableEscape("argument.entity.options.type.invalid", p_308410_)
   );

   private static void register(String p_121454_, EntitySelectorOptions.Modifier p_121455_, Predicate<EntitySelectorParser> p_121456_, Component p_121457_) {
      OPTIONS.put(p_121454_, new EntitySelectorOptions.Option(p_121455_, p_121456_, p_121457_));
   }

   public static void bootStrap() {
      if (OPTIONS.isEmpty()) {
         register("name", p_368973_ -> {
            int i = p_368973_.getReader().getCursor();
            boolean flag = p_368973_.shouldInvertValue();
            String s = p_368973_.getReader().readString();
            if (p_368973_.hasNameNotEquals() && !flag) {
               p_368973_.getReader().setCursor(i);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_368973_.getReader(), "name");
            }

            if (flag) {
               p_368973_.setHasNameNotEquals(true);
            } else {
               p_368973_.setHasNameEquals(true);
            }

            p_368973_.addPredicate(p_448547_ -> p_448547_.getPlainTextName().equals(s) != flag);
         }, p_121423_ -> !p_121423_.hasNameEquals(), Component.translatable("argument.entity.options.name.description"));
         register(
            "distance",
            p_406959_ -> {
               int i = p_406959_.getReader().getCursor();
               MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromReader(p_406959_.getReader());
               if (minmaxbounds$doubles.min().isPresent() && (Double)minmaxbounds$doubles.min().get() < 0.0
                  || minmaxbounds$doubles.max().isPresent() && (Double)minmaxbounds$doubles.max().get() < 0.0) {
                  p_406959_.getReader().setCursor(i);
                  throw ERROR_RANGE_NEGATIVE.createWithContext(p_406959_.getReader());
               }

               p_406959_.setDistance(minmaxbounds$doubles);
               p_406959_.setWorldLimited();
            },
            p_448553_ -> p_448553_.getDistance() == null,
            Component.translatable("argument.entity.options.distance.description")
         );
         register(
            "level",
            p_121417_ -> {
               int i = p_121417_.getReader().getCursor();
               MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromReader(p_121417_.getReader());
               if (minmaxbounds$ints.min().isPresent() && (Integer)minmaxbounds$ints.min().get() < 0
                  || minmaxbounds$ints.max().isPresent() && (Integer)minmaxbounds$ints.max().get() < 0) {
                  p_121417_.getReader().setCursor(i);
                  throw ERROR_LEVEL_NEGATIVE.createWithContext(p_121417_.getReader());
               }

               p_121417_.setLevel(minmaxbounds$ints);
               p_121417_.setIncludesEntities(false);
            },
            p_448550_ -> p_448550_.getLevel() == null,
            Component.translatable("argument.entity.options.level.description")
         );
         register("x", p_121413_ -> {
            p_121413_.setWorldLimited();
            p_121413_.setX(p_121413_.getReader().readDouble());
         }, p_121411_ -> p_121411_.getX() == null, Component.translatable("argument.entity.options.x.description"));
         register("y", p_121409_ -> {
            p_121409_.setWorldLimited();
            p_121409_.setY(p_121409_.getReader().readDouble());
         }, p_121407_ -> p_121407_.getY() == null, Component.translatable("argument.entity.options.y.description"));
         register("z", p_121405_ -> {
            p_121405_.setWorldLimited();
            p_121405_.setZ(p_121405_.getReader().readDouble());
         }, p_121403_ -> p_121403_.getZ() == null, Component.translatable("argument.entity.options.z.description"));
         register("dx", p_121401_ -> {
            p_121401_.setWorldLimited();
            p_121401_.setDeltaX(p_121401_.getReader().readDouble());
         }, p_121399_ -> p_121399_.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
         register("dy", p_121397_ -> {
            p_121397_.setWorldLimited();
            p_121397_.setDeltaY(p_121397_.getReader().readDouble());
         }, p_121395_ -> p_121395_.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
         register("dz", p_121562_ -> {
            p_121562_.setWorldLimited();
            p_121562_.setDeltaZ(p_121562_.getReader().readDouble());
         }, p_121560_ -> p_121560_.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
         register(
            "x_rotation",
            p_448542_ -> p_448542_.setRotX(MinMaxBounds.FloatDegrees.fromReader(p_448542_.getReader())),
            p_448551_ -> p_448551_.getRotX() == null,
            Component.translatable("argument.entity.options.x_rotation.description")
         );
         register(
            "y_rotation",
            p_448543_ -> p_448543_.setRotY(MinMaxBounds.FloatDegrees.fromReader(p_448543_.getReader())),
            p_448552_ -> p_448552_.getRotY() == null,
            Component.translatable("argument.entity.options.y_rotation.description")
         );
         register("limit", p_121550_ -> {
            int i = p_121550_.getReader().getCursor();
            int j = p_121550_.getReader().readInt();
            if (j < 1) {
               p_121550_.getReader().setCursor(i);
               throw ERROR_LIMIT_TOO_SMALL.createWithContext(p_121550_.getReader());
            }

            p_121550_.setMaxResults(j);
            p_121550_.setLimited(true);
         }, p_121548_ -> !p_121548_.isCurrentEntity() && !p_121548_.isLimited(), Component.translatable("argument.entity.options.limit.description"));
         register(
            "sort",
            p_247983_ -> {
               int i = p_247983_.getReader().getCursor();
               String s = p_247983_.getReader().readUnquotedString();
               p_247983_.setSuggestions(
                  (p_175153_, p_175154_) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), p_175153_)
               );

               p_247983_.setOrder(switch (s) {
                  case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
                  case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
                  case "random" -> EntitySelectorParser.ORDER_RANDOM;
                  case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
                  default -> {
                     p_247983_.getReader().setCursor(i);
                     throw ERROR_SORT_UNKNOWN.createWithContext(p_247983_.getReader(), s);
                  }
               });
               p_247983_.setSorted(true);
            },
            p_121544_ -> !p_121544_.isCurrentEntity() && !p_121544_.isSorted(),
            Component.translatable("argument.entity.options.sort.description")
         );
         register("gamemode", p_121542_ -> {
            p_121542_.setSuggestions((p_175193_, p_175194_) -> {
               String s1 = p_175193_.getRemaining().toLowerCase(Locale.ROOT);
               boolean flag1 = !p_121542_.hasGamemodeNotEquals();
               boolean flag2 = true;
               if (!s1.isEmpty()) {
                  if (s1.charAt(0) == '!') {
                     flag1 = false;
                     s1 = s1.substring(1);
                  } else {
                     flag2 = false;
                  }
               }

               for (GameType gametype1 : GameType.values()) {
                  if (gametype1.getName().toLowerCase(Locale.ROOT).startsWith(s1)) {
                     if (flag2) {
                        p_175193_.suggest("!" + gametype1.getName());
                     }

                     if (flag1) {
                        p_175193_.suggest(gametype1.getName());
                     }
                  }
               }

               return p_175193_.buildFuture();
            });
            int i = p_121542_.getReader().getCursor();
            boolean flag = p_121542_.shouldInvertValue();
            if (p_121542_.hasGamemodeNotEquals() && !flag) {
               p_121542_.getReader().setCursor(i);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_121542_.getReader(), "gamemode");
            }

            String s = p_121542_.getReader().readUnquotedString();
            GameType gametype = GameType.byName(s, null);
            if (gametype == null) {
               p_121542_.getReader().setCursor(i);
               throw ERROR_GAME_MODE_INVALID.createWithContext(p_121542_.getReader(), s);
            }

            p_121542_.setIncludesEntities(false);
            p_121542_.addPredicate(p_175190_ -> {
               if (p_175190_ instanceof ServerPlayer serverplayer) {
                  GameType gametype1 = serverplayer.gameMode();
                  return gametype1 == gametype ^ flag;
               } else {
                  return false;
               }
            });
            if (flag) {
               p_121542_.setHasGamemodeNotEquals(true);
            } else {
               p_121542_.setHasGamemodeEquals(true);
            }
         }, p_121540_ -> !p_121540_.hasGamemodeEquals(), Component.translatable("argument.entity.options.gamemode.description"));
         register("team", p_121538_ -> {
            boolean flag = p_121538_.shouldInvertValue();
            String s = p_121538_.getReader().readUnquotedString();
            p_121538_.addPredicate(p_389651_ -> {
               Team team = p_389651_.getTeam();
               String s1 = team == null ? "" : team.getName();
               return s1.equals(s) != flag;
            });
            if (flag) {
               p_121538_.setHasTeamNotEquals(true);
            } else {
               p_121538_.setHasTeamEquals(true);
            }
         }, p_121536_ -> !p_121536_.hasTeamEquals(), Component.translatable("argument.entity.options.team.description"));
         register(
            "type",
            p_121534_ -> {
               p_121534_.setSuggestions(
                  (p_358070_, p_358071_) -> {
                     SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_358070_, String.valueOf('!'));
                     SharedSuggestionProvider.suggestResource(
                        BuiltInRegistries.ENTITY_TYPE.getTags().map(p_448548_ -> p_448548_.key().location()), p_358070_, "!#"
                     );
                     if (!p_121534_.isTypeLimitedInversely()) {
                        SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_358070_);
                        SharedSuggestionProvider.suggestResource(
                           BuiltInRegistries.ENTITY_TYPE.getTags().map(p_448549_ -> p_448549_.key().location()), p_358070_, String.valueOf('#')
                        );
                     }

                     return p_358070_.buildFuture();
                  }
               );
               int i = p_121534_.getReader().getCursor();
               boolean flag = p_121534_.shouldInvertValue();
               if (p_121534_.isTypeLimitedInversely() && !flag) {
                  p_121534_.getReader().setCursor(i);
                  throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_121534_.getReader(), "type");
               }

               if (flag) {
                  p_121534_.setTypeLimitedInversely();
               }

               if (p_121534_.isTag()) {
                  TagKey<EntityType<?>> tagkey = TagKey.create(Registries.ENTITY_TYPE, Identifier.read(p_121534_.getReader()));
                  p_121534_.addPredicate(p_205691_ -> p_205691_.getType().is(tagkey) != flag);
               } else {
                  Identifier identifier = Identifier.read(p_121534_.getReader());
                  EntityType<?> entitytype = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElseThrow(() -> {
                     p_121534_.getReader().setCursor(i);
                     return ERROR_ENTITY_TYPE_INVALID.createWithContext(p_121534_.getReader(), identifier.toString());
                  });
                  if (Objects.equals(EntityType.PLAYER, entitytype) && !flag) {
                     p_121534_.setIncludesEntities(false);
                  }

                  p_121534_.addPredicate(p_175151_ -> Objects.equals(entitytype, p_175151_.getType()) != flag);
                  if (!flag) {
                     p_121534_.limitToType(entitytype);
                  }
               }
            },
            p_121532_ -> !p_121532_.isTypeLimited(),
            Component.translatable("argument.entity.options.type.description")
         );
         register("tag", p_121530_ -> {
            boolean flag = p_121530_.shouldInvertValue();
            String s = p_121530_.getReader().readUnquotedString();
            p_121530_.addPredicate(p_175166_ -> "".equals(s) ? p_175166_.getTags().isEmpty() != flag : p_175166_.getTags().contains(s) != flag);
         }, p_121528_ -> true, Component.translatable("argument.entity.options.tag.description"));
         register("nbt", p_389652_ -> {
            boolean flag = p_389652_.shouldInvertValue();
            CompoundTag compoundtag = TagParser.parseCompoundAsArgument(p_389652_.getReader());
            p_389652_.addPredicate(p_405047_ -> {
               try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_405047_.problemPath(), LOGGER)) {
                  TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_405047_.registryAccess());
                  p_405047_.saveWithoutId(tagvalueoutput);
                  if (p_405047_ instanceof ServerPlayer serverplayer) {
                     ItemStack itemstack = serverplayer.getInventory().getSelectedItem();
                     if (!itemstack.isEmpty()) {
                        tagvalueoutput.store("SelectedItem", ItemStack.CODEC, itemstack);
                     }
                  }

                  return NbtUtils.compareNbt(compoundtag, tagvalueoutput.buildResult(), true) != flag;
               }
            });
         }, p_121524_ -> true, Component.translatable("argument.entity.options.nbt.description"));
         register("scores", p_121522_ -> {
            StringReader stringreader = p_121522_.getReader();
            Map<String, MinMaxBounds.Ints> map = Maps.newHashMap();
            stringreader.expect('{');
            stringreader.skipWhitespace();

            while (stringreader.canRead() && stringreader.peek() != '}') {
               stringreader.skipWhitespace();
               String s = stringreader.readUnquotedString();
               stringreader.skipWhitespace();
               stringreader.expect('=');
               stringreader.skipWhitespace();
               MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromReader(stringreader);
               map.put(s, minmaxbounds$ints);
               stringreader.skipWhitespace();
               if (stringreader.canRead() && stringreader.peek() == ',') {
                  stringreader.skip();
               }
            }

            stringreader.expect('}');
            if (!map.isEmpty()) {
               p_121522_.addPredicate(p_308418_ -> {
                  Scoreboard scoreboard = p_308418_.level().getServer().getScoreboard();

                  for (Entry<String, MinMaxBounds.Ints> entry : map.entrySet()) {
                     Objective objective = scoreboard.getObjective(entry.getKey());
                     if (objective == null) {
                        return false;
                     }

                     ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_308418_, objective);
                     if (readonlyscoreinfo == null) {
                        return false;
                     }

                     if (!entry.getValue().matches(readonlyscoreinfo.value())) {
                        return false;
                     }
                  }

                  return true;
               });
            }

            p_121522_.setHasScores(true);
         }, p_121518_ -> !p_121518_.hasScores(), Component.translatable("argument.entity.options.scores.description"));
         register("advancements", p_121514_ -> {
            StringReader stringreader = p_121514_.getReader();
            Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
            stringreader.expect('{');
            stringreader.skipWhitespace();

            while (stringreader.canRead() && stringreader.peek() != '}') {
               stringreader.skipWhitespace();
               Identifier identifier = Identifier.read(stringreader);
               stringreader.skipWhitespace();
               stringreader.expect('=');
               stringreader.skipWhitespace();
               if (stringreader.canRead() && stringreader.peek() == '{') {
                  Map<String, Predicate<CriterionProgress>> map1 = Maps.newHashMap();
                  stringreader.skipWhitespace();
                  stringreader.expect('{');
                  stringreader.skipWhitespace();

                  while (stringreader.canRead() && stringreader.peek() != '}') {
                     stringreader.skipWhitespace();
                     String s = stringreader.readUnquotedString();
                     stringreader.skipWhitespace();
                     stringreader.expect('=');
                     stringreader.skipWhitespace();
                     boolean flag1 = stringreader.readBoolean();
                     map1.put(s, p_175186_ -> p_175186_.isDone() == flag1);
                     stringreader.skipWhitespace();
                     if (stringreader.canRead() && stringreader.peek() == ',') {
                        stringreader.skip();
                     }
                  }

                  stringreader.skipWhitespace();
                  stringreader.expect('}');
                  stringreader.skipWhitespace();
                  map.put(identifier, p_175169_ -> {
                     for (Entry<String, Predicate<CriterionProgress>> entry : map1.entrySet()) {
                        CriterionProgress criterionprogress = p_175169_.getCriterion(entry.getKey());
                        if (criterionprogress == null || !entry.getValue().test(criterionprogress)) {
                           return false;
                        }
                     }

                     return true;
                  });
               } else {
                  boolean flag = stringreader.readBoolean();
                  map.put(identifier, p_175183_ -> p_175183_.isDone() == flag);
               }

               stringreader.skipWhitespace();
               if (stringreader.canRead() && stringreader.peek() == ',') {
                  stringreader.skip();
               }
            }

            stringreader.expect('}');
            if (!map.isEmpty()) {
               p_121514_.addPredicate(p_358074_ -> {
                  if (!(p_358074_ instanceof ServerPlayer serverplayer)) {
                     return false;
                  } else {
                     PlayerAdvancements $$4 = serverplayer.getAdvancements();
                     ServerAdvancementManager $$5x = serverplayer.level().getServer().getAdvancements();

                     for (Entry<Identifier, Predicate<AdvancementProgress>> entry : map.entrySet()) {
                        AdvancementHolder advancementholder = $$5x.get(entry.getKey());
                        if (advancementholder == null || !entry.getValue().test($$4.getOrStartProgress(advancementholder))) {
                           return false;
                        }
                     }

                     return true;
                  }
               });
               p_121514_.setIncludesEntities(false);
            }

            p_121514_.setHasAdvancements(true);
         }, p_121506_ -> !p_121506_.hasAdvancements(), Component.translatable("argument.entity.options.advancements.description"));
         register(
            "predicate",
            p_448544_ -> {
               boolean flag = p_448544_.shouldInvertValue();
               ResourceKey<LootItemCondition> resourcekey = ResourceKey.create(Registries.PREDICATE, Identifier.read(p_448544_.getReader()));
               p_448544_.addPredicate(
                  p_421096_ -> {
                     if (p_421096_.level() instanceof ServerLevel serverlevel) {
                        Optional<LootItemCondition> optional = serverlevel.getServer().reloadableRegistries().lookup().get(resourcekey).map(Holder::value);
                        if (optional.isEmpty()) {
                           return false;
                        }

                        LootParams lootparams = new LootParams.Builder(serverlevel)
                           .withParameter(LootContextParams.THIS_ENTITY, p_421096_)
                           .withParameter(LootContextParams.ORIGIN, p_421096_.position())
                           .create(LootContextParamSets.SELECTOR);
                        LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
                        lootcontext.pushVisitedElement(LootContext.createVisitedEntry(optional.get()));
                        return flag ^ optional.get().test(lootcontext);
                     } else {
                        return false;
                     }
                  }
               );
            },
            p_121435_ -> true,
            Component.translatable("argument.entity.options.predicate.description")
         );
      }
   }

   public static EntitySelectorOptions.Modifier get(EntitySelectorParser p_121448_, String p_121449_, int p_121450_) throws CommandSyntaxException {
      EntitySelectorOptions.Option entityselectoroptions$option = OPTIONS.get(p_121449_);
      if (entityselectoroptions$option != null) {
         if (entityselectoroptions$option.canUse.test(p_121448_)) {
            return entityselectoroptions$option.modifier;
         } else {
            throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_121448_.getReader(), p_121449_);
         }
      } else {
         p_121448_.getReader().setCursor(p_121450_);
         throw ERROR_UNKNOWN_OPTION.createWithContext(p_121448_.getReader(), p_121449_);
      }
   }

   public static void suggestNames(EntitySelectorParser p_121441_, SuggestionsBuilder p_121442_) {
      String s = p_121442_.getRemaining().toLowerCase(Locale.ROOT);

      for (Entry<String, EntitySelectorOptions.Option> entry : OPTIONS.entrySet()) {
         if (entry.getValue().canUse.test(p_121441_) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(s)) {
            p_121442_.suggest(entry.getKey() + "=", entry.getValue().description);
         }
      }
   }

   @FunctionalInterface
   public interface Modifier {
      void handle(EntitySelectorParser var1) throws CommandSyntaxException;
   }

   record Option(EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> canUse, Component description) {
   }
}
