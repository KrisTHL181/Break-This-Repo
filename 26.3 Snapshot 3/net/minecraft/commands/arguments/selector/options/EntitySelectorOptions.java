package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.predicates.MinMaxBounds;
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
import net.minecraft.world.entity.EntityTypes;
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
   private static final Predicate<EntitySelectorParser> ALWAYS_AVAILABLE = var0 -> true;
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("argument.entity.options.unknown", name)
   );
   public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("argument.entity.options.inapplicable", name)
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
      name -> Component.translatableEscape("argument.entity.options.sort.irreversible", name)
   );
   public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("argument.entity.options.mode.invalid", name)
   );
   public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType(
      type -> Component.translatableEscape("argument.entity.options.type.invalid", type)
   );

   private static void register(
      final String name, final EntitySelectorOptions.Modifier modifier, final Predicate<EntitySelectorParser> predicate, final Component description
   ) {
      OPTIONS.put(name, new EntitySelectorOptions.Option(modifier, predicate, description));
   }

   public static void bootStrap() {
      if (OPTIONS.isEmpty()) {
         register("name", parser -> {
            int start = parser.getReader().getCursor();
            boolean inverted = parser.shouldInvertValue();
            String name = parser.getReader().readString();
            InvertableSetOptionState state = parser.nameOption();
            if (!state.canParseElement(inverted)) {
               throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "name");
            }

            state.markParsedElement(inverted);
            parser.addPredicate(e -> e.getPlainTextName().equals(name) != inverted);
         }, s -> s.nameOption().canParseAny(), Component.translatable("argument.entity.options.name.description"));
         register("distance", parser -> {
            int start = parser.getReader().getCursor();
            MinMaxBounds.Doubles value = MinMaxBounds.Doubles.fromReader(parser.getReader());
            if ((!value.min().isPresent() || !((Double)value.min().get() < 0.0)) && (!value.max().isPresent() || !((Double)value.max().get() < 0.0))) {
               parser.setDistance(value);
               parser.setWorldLimited();
            } else {
               throw rollbackAndThrow(parser, start, ERROR_RANGE_NEGATIVE);
            }
         }, s -> s.getDistance() == null, Component.translatable("argument.entity.options.distance.description"));
         register("level", parser -> {
            int start = parser.getReader().getCursor();
            MinMaxBounds.Ints value = MinMaxBounds.Ints.fromReader(parser.getReader());
            if ((!value.min().isPresent() || (Integer)value.min().get() >= 0) && (!value.max().isPresent() || (Integer)value.max().get() >= 0)) {
               parser.setLevel(value);
               parser.setIncludesEntities(false);
            } else {
               throw rollbackAndThrow(parser, start, ERROR_LEVEL_NEGATIVE);
            }
         }, s -> s.getLevel() == null, Component.translatable("argument.entity.options.level.description"));
         register("x", parser -> {
            parser.setWorldLimited();
            parser.setX(parser.getReader().readDouble());
         }, s -> s.getX() == null, Component.translatable("argument.entity.options.x.description"));
         register("y", parser -> {
            parser.setWorldLimited();
            parser.setY(parser.getReader().readDouble());
         }, s -> s.getY() == null, Component.translatable("argument.entity.options.y.description"));
         register("z", parser -> {
            parser.setWorldLimited();
            parser.setZ(parser.getReader().readDouble());
         }, s -> s.getZ() == null, Component.translatable("argument.entity.options.z.description"));
         register("dx", parser -> {
            parser.setWorldLimited();
            parser.setDeltaX(parser.getReader().readDouble());
         }, s -> s.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
         register("dy", parser -> {
            parser.setWorldLimited();
            parser.setDeltaY(parser.getReader().readDouble());
         }, s -> s.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
         register("dz", parser -> {
            parser.setWorldLimited();
            parser.setDeltaZ(parser.getReader().readDouble());
         }, s -> s.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
         register(
            "x_rotation",
            parser -> parser.setRotX(MinMaxBounds.FloatDegrees.fromReader(parser.getReader())),
            s -> s.getRotX() == null,
            Component.translatable("argument.entity.options.x_rotation.description")
         );
         register(
            "y_rotation",
            parser -> parser.setRotY(MinMaxBounds.FloatDegrees.fromReader(parser.getReader())),
            s -> s.getRotY() == null,
            Component.translatable("argument.entity.options.y_rotation.description")
         );
         register("limit", parser -> {
            int start = parser.getReader().getCursor();
            int count = parser.getReader().readInt();
            if (count < 1) {
               throw rollbackAndThrow(parser, start, ERROR_LIMIT_TOO_SMALL);
            }

            parser.setMaxResults(count);
            parser.limitedOption().markParsed();
         }, s -> !s.isCurrentEntity() && s.limitedOption().canParse(), Component.translatable("argument.entity.options.limit.description"));
         register("sort", parser -> {
            int start = parser.getReader().getCursor();
            String name = parser.getReader().readUnquotedString();
            parser.setSuggestions((b, n) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), b));

            parser.setOrder(switch (name) {
               case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
               case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
               case "random" -> EntitySelectorParser.ORDER_RANDOM;
               case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
               default -> throw rollbackAndThrow(parser, start, ERROR_SORT_UNKNOWN, name);
            });
            parser.sortedOption().markParsed();
         }, s -> !s.isCurrentEntity() && s.sortedOption().canParse(), Component.translatable("argument.entity.options.sort.description"));
         register("gamemode", parser -> {
            InvertableSetOptionState state = parser.gamemodeOption();
            parser.setSuggestions((b, m) -> {
               String prefix = b.getRemaining().toLowerCase(Locale.ROOT);
               boolean addNormal = state.canParsePositiveElement();
               boolean addInverted = state.canParseNegativeElement();
               if (!prefix.isEmpty()) {
                  if (prefix.charAt(0) == '!') {
                     addNormal = false;
                     prefix = prefix.substring(1);
                  } else {
                     addInverted = false;
                  }
               }

               for (GameType type : GameType.values()) {
                  if (type.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                     if (addInverted) {
                        b.suggest("!" + type.getName());
                     }

                     if (addNormal) {
                        b.suggest(type.getName());
                     }
                  }
               }

               return b.buildFuture();
            });
            int start = parser.getReader().getCursor();
            boolean inverted = parser.shouldInvertValue();
            if (!state.canParseElement(inverted)) {
               throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "gamemode");
            }

            String name = parser.getReader().readUnquotedString();
            GameType expected = GameType.byName(name, null);
            if (expected == null) {
               throw rollbackAndThrow(parser, start, ERROR_GAME_MODE_INVALID, name);
            }

            parser.setIncludesEntities(false);
            parser.addPredicate(e -> {
               if (e instanceof ServerPlayer player) {
                  GameType current = player.gameMode();
                  return current == expected ^ inverted;
               } else {
                  return false;
               }
            });
            state.markParsedElement(inverted);
         }, s -> s.gamemodeOption().canParseAny(), Component.translatable("argument.entity.options.gamemode.description"));
         register("team", parser -> {
            InvertableSetOptionState state = parser.teamOption();
            int start = parser.getReader().getCursor();
            boolean inverted = parser.shouldInvertValue();
            String expected = parser.getReader().readUnquotedString();
            if (!state.canParseElement(inverted)) {
               throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "team");
            }

            parser.addPredicate(e -> {
               Team current = e.getTeam();
               String currentName = current == null ? "" : current.getName();
               return currentName.equals(expected) != inverted;
            });
            state.markParsedElement(inverted);
         }, s -> s.teamOption().canParseAny(), Component.translatable("argument.entity.options.team.description"));
         register(
            "type",
            parser -> {
               InvertableSetOptionState state = parser.typeOption();
               parser.setSuggestions((b, m) -> {
                  if (state.canParseNegativeElement()) {
                     SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b, String.valueOf('!'));
                  }

                  if (state.canParsePositiveElement()) {
                     SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b);
                  }

                  if (state.canParseAnyTag()) {
                     List<Identifier> allowedTags = BuiltInRegistries.ENTITY_TYPE
                        .getTags()
                        .map(tag -> tag.key().location())
                        .filter(state::canParseTag)
                        .toList();
                     if (!allowedTags.isEmpty()) {
                        SharedSuggestionProvider.suggestResource(allowedTags, b, String.valueOf('#'));
                        SharedSuggestionProvider.suggestResource(allowedTags, b, "!#");
                     }
                  }

                  return b.buildFuture();
               });
               int start = parser.getReader().getCursor();
               boolean inverted = parser.shouldInvertValue();
               if (parser.isTag()) {
                  if (!state.canParseAnyTag()) {
                     throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "type");
                  }

                  Identifier id = Identifier.read(parser.getReader());
                  if (!state.canParseTag(id)) {
                     throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "type");
                  }

                  TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, id);
                  parser.addPredicate(e -> e.is(key) != inverted);
                  state.markParsedTag(id);
               } else {
                  if (!state.canParseElement(inverted)) {
                     throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, "type");
                  }

                  Identifier id = Identifier.read(parser.getReader());
                  EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                     .getOptional(id)
                     .orElseThrow(() -> rollbackAndThrow(parser, start, ERROR_ENTITY_TYPE_INVALID, id.toString()));
                  if (Objects.equals(EntityTypes.PLAYER, type) && !inverted) {
                     parser.setIncludesEntities(false);
                  }

                  parser.addPredicate(e -> Objects.equals(type, e.getType()) != inverted);
                  if (!inverted) {
                     parser.limitToType(type);
                  }

                  state.markParsedElement(inverted);
               }
            },
            s -> s.typeOption().canParseAny(),
            Component.translatable("argument.entity.options.type.description")
         );
         register("tag", parser -> {
            boolean inverted = parser.shouldInvertValue();
            String tag = parser.getReader().readUnquotedString();
            parser.addPredicate(e -> "".equals(tag) ? e.entityTags().isEmpty() != inverted : e.entityTags().contains(tag) != inverted);
         }, ALWAYS_AVAILABLE, Component.translatable("argument.entity.options.tag.description"));
         register("nbt", parser -> {
            boolean inverted = parser.shouldInvertValue();
            CompoundTag tag = TagParser.parseCompoundAsArgument(parser.getReader());
            parser.addPredicate(e -> {
               try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(e.problemPath(), LOGGER)) {
                  TagValueOutput output = TagValueOutput.createWithContext(reporter, e.registryAccess());
                  e.saveWithoutId(output);
                  if (e instanceof ServerPlayer player) {
                     ItemStack selected = player.getInventory().getSelectedItem();
                     if (!selected.isEmpty()) {
                        output.store("SelectedItem", ItemStack.CODEC, selected);
                     }
                  }

                  return NbtUtils.compareNbt(tag, output.buildResult(), true) != inverted;
               }
            });
         }, ALWAYS_AVAILABLE, Component.translatable("argument.entity.options.nbt.description"));
         register("scores", parser -> {
            StringReader reader = parser.getReader();
            Map<String, MinMaxBounds.Ints> expected = Maps.newHashMap();
            reader.expect('{');
            reader.skipWhitespace();

            while (reader.canRead() && reader.peek() != '}') {
               reader.skipWhitespace();
               String name = reader.readUnquotedString();
               reader.skipWhitespace();
               reader.expect('=');
               reader.skipWhitespace();
               MinMaxBounds.Ints value = MinMaxBounds.Ints.fromReader(reader);
               expected.put(name, value);
               reader.skipWhitespace();
               if (reader.canRead() && reader.peek() == ',') {
                  reader.skip();
               }
            }

            reader.expect('}');
            if (!expected.isEmpty()) {
               parser.addPredicate(entity -> {
                  Scoreboard scoreboard = entity.level().getServer().getScoreboard();

                  for (Entry<String, MinMaxBounds.Ints> entry : expected.entrySet()) {
                     Objective objective = scoreboard.getObjective(entry.getKey());
                     if (objective == null) {
                        return false;
                     }

                     ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(entity, objective);
                     if (scoreInfo == null) {
                        return false;
                     }

                     if (!entry.getValue().matches(scoreInfo.value())) {
                        return false;
                     }
                  }

                  return true;
               });
            }

            parser.scoresOption().markParsed();
         }, s -> s.scoresOption().canParse(), Component.translatable("argument.entity.options.scores.description"));
         register("advancements", parser -> {
            StringReader reader = parser.getReader();
            Map<Identifier, Predicate<AdvancementProgress>> expected = Maps.newHashMap();
            reader.expect('{');
            reader.skipWhitespace();

            while (reader.canRead() && reader.peek() != '}') {
               reader.skipWhitespace();
               Identifier name = Identifier.read(reader);
               reader.skipWhitespace();
               reader.expect('=');
               reader.skipWhitespace();
               if (reader.canRead() && reader.peek() == '{') {
                  Map<String, Predicate<CriterionProgress>> progress = Maps.newHashMap();
                  reader.skipWhitespace();
                  reader.expect('{');
                  reader.skipWhitespace();

                  while (reader.canRead() && reader.peek() != '}') {
                     reader.skipWhitespace();
                     String criterion = reader.readUnquotedString();
                     reader.skipWhitespace();
                     reader.expect('=');
                     reader.skipWhitespace();
                     boolean value = reader.readBoolean();
                     progress.put(criterion, p -> p.isDone() == value);
                     reader.skipWhitespace();
                     if (reader.canRead() && reader.peek() == ',') {
                        reader.skip();
                     }
                  }

                  reader.skipWhitespace();
                  reader.expect('}');
                  reader.skipWhitespace();
                  expected.put(name, p -> {
                     for (Entry<String, Predicate<CriterionProgress>> entry : progress.entrySet()) {
                        CriterionProgress criterionx = p.getCriterion(entry.getKey());
                        if (criterionx == null || !entry.getValue().test(criterionx)) {
                           return false;
                        }
                     }

                     return true;
                  });
               } else {
                  boolean value = reader.readBoolean();
                  expected.put(name, p -> p.isDone() == value);
               }

               reader.skipWhitespace();
               if (reader.canRead() && reader.peek() == ',') {
                  reader.skip();
               }
            }

            reader.expect('}');
            if (!expected.isEmpty()) {
               parser.addPredicate(e -> {
                  if (!(e instanceof ServerPlayer player)) {
                     return false;
                  } else {
                     PlayerAdvancements advancements = player.getAdvancements();
                     ServerAdvancementManager serverAdvancements = player.level().getServer().getAdvancements();

                     for (Entry<Identifier, Predicate<AdvancementProgress>> entry : expected.entrySet()) {
                        AdvancementHolder advancement = serverAdvancements.get(entry.getKey());
                        if (advancement == null || !entry.getValue().test(advancements.getOrStartProgress(advancement))) {
                           return false;
                        }
                     }

                     return true;
                  }
               });
               parser.setIncludesEntities(false);
            }

            parser.advancementsOption().markParsed();
         }, s -> s.advancementsOption().canParse(), Component.translatable("argument.entity.options.advancements.description"));
         register(
            "predicate",
            parser -> {
               boolean inverted = parser.shouldInvertValue();
               ResourceKey<LootItemCondition> id = ResourceKey.create(Registries.PREDICATE, Identifier.read(parser.getReader()));
               parser.addPredicate(
                  entity -> {
                     if (entity.level() instanceof ServerLevel level) {
                        Optional<LootItemCondition> condition = level.getServer().reloadableRegistries().lookup().get(id).map(Holder::value);
                        if (condition.isEmpty()) {
                           return false;
                        }

                        LootParams lootParams = new LootParams.Builder(level)
                           .withParameter(LootContextParams.THIS_ENTITY, entity)
                           .withParameter(LootContextParams.ORIGIN, entity.position())
                           .create(LootContextParamSets.SELECTOR);
                        LootContext context = new LootContext.Builder(lootParams).create(Optional.empty());
                        context.pushVisitedElement(LootContext.createVisitedEntry(condition.get()));
                        return inverted ^ condition.get().test(context);
                     } else {
                        return false;
                     }
                  }
               );
            },
            ALWAYS_AVAILABLE,
            Component.translatable("argument.entity.options.predicate.description")
         );
      }
   }

   private static CommandSyntaxException rollbackAndThrow(final EntitySelectorParser parser, final int start, final SimpleCommandExceptionType type) {
      parser.getReader().setCursor(start);
      return type.createWithContext(parser.getReader());
   }

   private static CommandSyntaxException rollbackAndThrow(
      final EntitySelectorParser parser, final int start, final DynamicCommandExceptionType type, final String argument
   ) {
      parser.getReader().setCursor(start);
      return type.createWithContext(parser.getReader(), argument);
   }

   public static EntitySelectorOptions.Modifier get(final EntitySelectorParser parser, final String key, final int start) throws CommandSyntaxException {
      EntitySelectorOptions.Option option = OPTIONS.get(key);
      if (option != null) {
         if (option.canUse.test(parser)) {
            return option.modifier;
         } else {
            throw rollbackAndThrow(parser, start, ERROR_INAPPLICABLE_OPTION, key);
         }
      } else {
         throw rollbackAndThrow(parser, start, ERROR_UNKNOWN_OPTION, key);
      }
   }

   public static void suggestNames(final EntitySelectorParser parser, final SuggestionsBuilder builder) {
      String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);

      for (Entry<String, EntitySelectorOptions.Option> entry : OPTIONS.entrySet()) {
         if (entry.getValue().canUse.test(parser) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
            builder.suggest(entry.getKey() + "=", entry.getValue().description);
         }
      }
   }

   @FunctionalInterface
   public interface Modifier {
      void handle(EntitySelectorParser parser) throws CommandSyntaxException;
   }

   private record Option(EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> canUse, Component description) {
   }
}
