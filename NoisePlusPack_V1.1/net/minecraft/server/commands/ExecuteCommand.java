package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ExecuteCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_308669_, p_308670_) -> Component.translatableEscape("commands.execute.blocks.toobig", p_308669_, p_308670_)
   );
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.execute.conditional.fail")
   );
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
      p_308680_ -> Component.translatableEscape("commands.execute.conditional.fail_count", p_308680_)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (p_308676_, p_308677_) -> Component.translatableEscape("commands.execute.function.instantiationFailure", p_308676_, p_308677_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_214435_, CommandBuildContext p_214436_) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = p_214435_.register(
         (LiteralArgumentBuilder)Commands.literal("execute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
      );
      p_214435_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                         "execute"
                                                      )
                                                      .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                                                   .then(Commands.literal("run").redirect(p_214435_.getRoot())))
                                                .then(addConditionals(literalcommandnode, Commands.literal("if"), true, p_214436_)))
                                             .then(addConditionals(literalcommandnode, Commands.literal("unless"), false, p_214436_)))
                                          .then(
                                             Commands.literal("as")
                                                .then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137299_ -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();

                                                   for (Entity entity : EntityArgument.getOptionalEntities(p_137299_, "targets")) {
                                                      list.add(((CommandSourceStack)p_137299_.getSource()).withEntity(entity));
                                                   }

                                                   return list;
                                                }))
                                          ))
                                       .then(
                                          Commands.literal("at")
                                             .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                   .fork(
                                                      literalcommandnode,
                                                      p_284653_ -> {
                                                         List<CommandSourceStack> list = Lists.newArrayList();

                                                         for (Entity entity : EntityArgument.getOptionalEntities(p_284653_, "targets")) {
                                                            list.add(
                                                               ((CommandSourceStack)p_284653_.getSource())
                                                                  .withLevel((ServerLevel)entity.level())
                                                                  .withPosition(entity.position())
                                                                  .withRotation(entity.getRotationVector())
                                                            );
                                                         }

                                                         return list;
                                                      }
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("store")
                                             .then(wrapStores(literalcommandnode, Commands.literal("result"), true)))
                                          .then(wrapStores(literalcommandnode, Commands.literal("success"), false))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("positioned")
                                             .then(
                                                Commands.argument("pos", Vec3Argument.vec3())
                                                   .redirect(
                                                      literalcommandnode,
                                                      p_137295_ -> ((CommandSourceStack)p_137295_.getSource())
                                                         .withPosition(Vec3Argument.getVec3(p_137295_, "pos"))
                                                         .withAnchor(EntityAnchorArgument.Anchor.FEET)
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("as")
                                                .then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137293_ -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();

                                                   for (Entity entity : EntityArgument.getOptionalEntities(p_137293_, "targets")) {
                                                      list.add(((CommandSourceStack)p_137293_.getSource()).withPosition(entity.position()));
                                                   }

                                                   return list;
                                                }))
                                          ))
                                       .then(
                                          Commands.literal("over")
                                             .then(
                                                Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect(literalcommandnode, p_274814_ -> {
                                                   Vec3 vec3 = ((CommandSourceStack)p_274814_.getSource()).getPosition();
                                                   ServerLevel serverlevel = ((CommandSourceStack)p_274814_.getSource()).getLevel();
                                                   double d0 = vec3.x();
                                                   double d1 = vec3.z();
                                                   if (!serverlevel.hasChunk(SectionPos.blockToSectionCoord(d0), SectionPos.blockToSectionCoord(d1))) {
                                                      throw BlockPosArgument.ERROR_NOT_LOADED.create();
                                                   }

                                                   int i = serverlevel.getHeight(
                                                      HeightmapTypeArgument.getHeightmap(p_274814_, "heightmap"), Mth.floor(d0), Mth.floor(d1)
                                                   );
                                                   return ((CommandSourceStack)p_274814_.getSource()).withPosition(new Vec3(d0, i, d1));
                                                })
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("rotated")
                                       .then(
                                          Commands.argument("rot", RotationArgument.rotation())
                                             .redirect(
                                                literalcommandnode,
                                                p_137291_ -> ((CommandSourceStack)p_137291_.getSource())
                                                   .withRotation(
                                                      RotationArgument.getRotation(p_137291_, "rot").getRotation((CommandSourceStack)p_137291_.getSource())
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("as")
                                          .then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137289_ -> {
                                             List<CommandSourceStack> list = Lists.newArrayList();

                                             for (Entity entity : EntityArgument.getOptionalEntities(p_137289_, "targets")) {
                                                list.add(((CommandSourceStack)p_137289_.getSource()).withRotation(entity.getRotationVector()));
                                             }

                                             return list;
                                          }))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("facing")
                                    .then(
                                       Commands.literal("entity")
                                          .then(
                                             Commands.argument("targets", EntityArgument.entities())
                                                .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(literalcommandnode, p_137287_ -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();
                                                   EntityAnchorArgument.Anchor entityanchorargument$anchor = EntityAnchorArgument.getAnchor(p_137287_, "anchor");

                                                   for (Entity entity : EntityArgument.getOptionalEntities(p_137287_, "targets")) {
                                                      list.add(((CommandSourceStack)p_137287_.getSource()).facing(entity, entityanchorargument$anchor));
                                                   }

                                                   return list;
                                                }))
                                          )
                                    ))
                                 .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                       .redirect(
                                          literalcommandnode,
                                          p_137285_ -> ((CommandSourceStack)p_137285_.getSource()).facing(Vec3Argument.getVec3(p_137285_, "pos"))
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("align")
                              .then(
                                 Commands.argument("axes", SwizzleArgument.swizzle())
                                    .redirect(
                                       literalcommandnode,
                                       p_137283_ -> ((CommandSourceStack)p_137283_.getSource())
                                          .withPosition(
                                             ((CommandSourceStack)p_137283_.getSource()).getPosition().align(SwizzleArgument.getSwizzle(p_137283_, "axes"))
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("anchored")
                           .then(
                              Commands.argument("anchor", EntityAnchorArgument.anchor())
                                 .redirect(
                                    literalcommandnode,
                                    p_137281_ -> ((CommandSourceStack)p_137281_.getSource()).withAnchor(EntityAnchorArgument.getAnchor(p_137281_, "anchor"))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("in")
                        .then(
                           Commands.argument("dimension", DimensionArgument.dimension())
                              .redirect(
                                 literalcommandnode,
                                 p_137279_ -> ((CommandSourceStack)p_137279_.getSource()).withLevel(DimensionArgument.getDimension(p_137279_, "dimension"))
                              )
                        )
                  ))
               .then(
                  Commands.literal("summon")
                     .then(
                        Commands.argument("entity", ResourceArgument.resource(p_214436_, Registries.ENTITY_TYPE))
                           .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                           .redirect(
                              literalcommandnode,
                              p_269759_ -> spawnEntityAndRedirect(
                                 (CommandSourceStack)p_269759_.getSource(), ResourceArgument.getSummonableEntityType(p_269759_, "entity")
                              )
                           )
                     )
               ))
            .then(createRelationOperations(literalcommandnode, Commands.literal("on")))
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
      LiteralCommandNode<CommandSourceStack> p_137094_, LiteralArgumentBuilder<CommandSourceStack> p_137095_, boolean p_137096_
   ) {
      p_137095_.then(
         Commands.literal("score")
            .then(
               Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                  .then(
                     Commands.argument("objective", ObjectiveArgument.objective())
                        .redirect(
                           p_137094_,
                           p_137271_ -> storeValue(
                              (CommandSourceStack)p_137271_.getSource(),
                              ScoreHolderArgument.getNamesWithDefaultWildcard(p_137271_, "targets"),
                              ObjectiveArgument.getObjective(p_137271_, "objective"),
                              p_137096_
                           )
                        )
                  )
            )
      );
      p_137095_.then(
         Commands.literal("bossbar")
            .then(
               ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                     .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                     .then(
                        Commands.literal("value")
                           .redirect(
                              p_137094_,
                              p_137259_ -> storeValue((CommandSourceStack)p_137259_.getSource(), BossBarCommands.getBossBar(p_137259_), true, p_137096_)
                           )
                     ))
                  .then(
                     Commands.literal("max")
                        .redirect(
                           p_137094_,
                           p_137247_ -> storeValue((CommandSourceStack)p_137247_.getSource(), BossBarCommands.getBossBar(p_137247_), false, p_137096_)
                        )
                  )
            )
      );

      for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.TARGET_PROVIDERS) {
         datacommands$dataprovider.wrap(
            p_137095_,
            p_137101_ -> p_137101_.then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                    "path", NbtPathArgument.nbtPath()
                                 )
                                 .then(
                                    Commands.literal("int")
                                       .then(
                                          Commands.argument("scale", DoubleArgumentType.doubleArg())
                                             .redirect(
                                                p_137094_,
                                                p_180216_ -> storeData(
                                                   (CommandSourceStack)p_180216_.getSource(),
                                                   datacommands$dataprovider.access(p_180216_),
                                                   NbtPathArgument.getPath(p_180216_, "path"),
                                                   p_180219_ -> IntTag.valueOf((int)(p_180219_ * DoubleArgumentType.getDouble(p_180216_, "scale"))),
                                                   p_137096_
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("float")
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .redirect(
                                             p_137094_,
                                             p_180209_ -> storeData(
                                                (CommandSourceStack)p_180209_.getSource(),
                                                datacommands$dataprovider.access(p_180209_),
                                                NbtPathArgument.getPath(p_180209_, "path"),
                                                p_180212_ -> FloatTag.valueOf((float)(p_180212_ * DoubleArgumentType.getDouble(p_180209_, "scale"))),
                                                p_137096_
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("short")
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .redirect(
                                          p_137094_,
                                          p_180199_ -> storeData(
                                             (CommandSourceStack)p_180199_.getSource(),
                                             datacommands$dataprovider.access(p_180199_),
                                             NbtPathArgument.getPath(p_180199_, "path"),
                                             p_180202_ -> ShortTag.valueOf((short)(p_180202_ * DoubleArgumentType.getDouble(p_180199_, "scale"))),
                                             p_137096_
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("long")
                              .then(
                                 Commands.argument("scale", DoubleArgumentType.doubleArg())
                                    .redirect(
                                       p_137094_,
                                       p_180189_ -> storeData(
                                          (CommandSourceStack)p_180189_.getSource(),
                                          datacommands$dataprovider.access(p_180189_),
                                          NbtPathArgument.getPath(p_180189_, "path"),
                                          p_180192_ -> LongTag.valueOf((long)(p_180192_ * DoubleArgumentType.getDouble(p_180189_, "scale"))),
                                          p_137096_
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("double")
                           .then(
                              Commands.argument("scale", DoubleArgumentType.doubleArg())
                                 .redirect(
                                    p_137094_,
                                    p_180179_ -> storeData(
                                       (CommandSourceStack)p_180179_.getSource(),
                                       datacommands$dataprovider.access(p_180179_),
                                       NbtPathArgument.getPath(p_180179_, "path"),
                                       p_180182_ -> DoubleTag.valueOf(p_180182_ * DoubleArgumentType.getDouble(p_180179_, "scale")),
                                       p_137096_
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("byte")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 p_137094_,
                                 p_180156_ -> storeData(
                                    (CommandSourceStack)p_180156_.getSource(),
                                    datacommands$dataprovider.access(p_180156_),
                                    NbtPathArgument.getPath(p_180156_, "path"),
                                    p_180165_ -> ByteTag.valueOf((byte)(p_180165_ * DoubleArgumentType.getDouble(p_180156_, "scale"))),
                                    p_137096_
                                 )
                              )
                        )
                  )
            )
         );
      }

      return p_137095_;
   }

   private static CommandSourceStack storeValue(CommandSourceStack p_137108_, Collection<ScoreHolder> p_137109_, Objective p_137110_, boolean p_137111_) {
      Scoreboard scoreboard = p_137108_.getServer().getScoreboard();
      return p_137108_.withCallback((p_137137_, p_137138_) -> {
         for (ScoreHolder scoreholder : p_137109_) {
            ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, p_137110_);
            int i = p_137111_ ? p_137138_ : (p_137137_ ? 1 : 0);
            scoreaccess.set(i);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeValue(CommandSourceStack p_137113_, CustomBossEvent p_137114_, boolean p_137115_, boolean p_137116_) {
      return p_137113_.withCallback((p_137186_, p_137187_) -> {
         int i = p_137116_ ? p_137187_ : (p_137186_ ? 1 : 0);
         if (p_137115_) {
            p_137114_.setValue(i);
         } else {
            p_137114_.setMax(i);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeData(
      CommandSourceStack p_137118_, DataAccessor p_137119_, NbtPathArgument.NbtPath p_137120_, IntFunction<Tag> p_137121_, boolean p_137122_
   ) {
      return p_137118_.withCallback((p_137154_, p_137155_) -> {
         try {
            CompoundTag compoundtag = p_137119_.getData();
            int i = p_137122_ ? p_137155_ : (p_137154_ ? 1 : 0);
            p_137120_.set(compoundtag, p_137121_.apply(i));
            p_137119_.setData(compoundtag);
         } catch (CommandSyntaxException var8) {
         }
      }, CommandResultCallback::chain);
   }

   private static boolean isChunkLoaded(ServerLevel p_265261_, BlockPos p_265260_) {
      ChunkPos chunkpos = new ChunkPos(p_265260_);
      LevelChunk levelchunk = p_265261_.getChunkSource().getChunkNow(chunkpos.x, chunkpos.z);
      return levelchunk == null ? false : levelchunk.getFullStatus() == FullChunkStatus.ENTITY_TICKING && p_265261_.areEntitiesLoaded(chunkpos.toLong());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
      CommandNode<CommandSourceStack> p_214438_, LiteralArgumentBuilder<CommandSourceStack> p_214439_, boolean p_214440_, CommandBuildContext p_214441_
   ) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)p_214439_.then(
                                       Commands.literal("block")
                                          .then(
                                             Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(
                                                   addConditional(
                                                      p_214438_,
                                                      Commands.argument("block", BlockPredicateArgument.blockPredicate(p_214441_)),
                                                      p_214440_,
                                                      p_137277_ -> BlockPredicateArgument.getBlockPredicate(p_137277_, "block")
                                                         .test(
                                                            new BlockInWorld(
                                                               ((CommandSourceStack)p_137277_.getSource()).getLevel(),
                                                               BlockPosArgument.getLoadedBlockPos(p_137277_, "pos"),
                                                               true
                                                            )
                                                         )
                                                   )
                                                )
                                          )
                                    ))
                                    .then(
                                       Commands.literal("biome")
                                          .then(
                                             Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(
                                                   addConditional(
                                                      p_214438_,
                                                      Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(p_214441_, Registries.BIOME)),
                                                      p_214440_,
                                                      p_448904_ -> ResourceOrTagArgument.getResourceOrTag(p_448904_, "biome", Registries.BIOME)
                                                         .test(
                                                            ((CommandSourceStack)p_448904_.getSource())
                                                               .getLevel()
                                                               .getBiome(BlockPosArgument.getLoadedBlockPos(p_448904_, "pos"))
                                                         )
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("loaded")
                                       .then(
                                          addConditional(
                                             p_214438_,
                                             Commands.argument("pos", BlockPosArgument.blockPos()),
                                             p_214440_,
                                             p_269757_ -> isChunkLoaded(
                                                ((CommandSourceStack)p_269757_.getSource()).getLevel(), BlockPosArgument.getBlockPos(p_269757_, "pos")
                                             )
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("dimension")
                                    .then(
                                       addConditional(
                                          p_214438_,
                                          Commands.argument("dimension", DimensionArgument.dimension()),
                                          p_214440_,
                                          p_264789_ -> DimensionArgument.getDimension(p_264789_, "dimension")
                                             == ((CommandSourceStack)p_264789_.getSource()).getLevel()
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("score")
                                 .then(
                                    Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                               "targetObjective", ObjectiveArgument.objective()
                                                            )
                                                            .then(
                                                               Commands.literal("=")
                                                                  .then(
                                                                     Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                        .then(
                                                                           addConditional(
                                                                              p_214438_,
                                                                              Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                              p_214440_,
                                                                              p_308678_ -> checkScore(
                                                                                 p_308678_, (p_308655_, p_308656_) -> p_308655_ == p_308656_
                                                                              )
                                                                           )
                                                                        )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("<")
                                                               .then(
                                                                  Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                     .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                     .then(
                                                                        addConditional(
                                                                           p_214438_,
                                                                           Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                           p_214440_,
                                                                           p_308668_ -> checkScore(p_308668_, (p_308681_, p_308682_) -> p_308681_ < p_308682_)
                                                                        )
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("<=")
                                                            .then(
                                                               Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                  .then(
                                                                     addConditional(
                                                                        p_214438_,
                                                                        Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                        p_214440_,
                                                                        p_308657_ -> checkScore(p_308657_, (p_308658_, p_308659_) -> p_308658_ <= p_308659_)
                                                                     )
                                                                  )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal(">")
                                                         .then(
                                                            Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                               .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                               .then(
                                                                  addConditional(
                                                                     p_214438_,
                                                                     Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                     p_214440_,
                                                                     p_308665_ -> checkScore(p_308665_, (p_308660_, p_308661_) -> p_308660_ > p_308661_)
                                                                  )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal(">=")
                                                      .then(
                                                         Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                            .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                            .then(
                                                               addConditional(
                                                                  p_214438_,
                                                                  Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                  p_214440_,
                                                                  p_308683_ -> checkScore(p_308683_, (p_308666_, p_308667_) -> p_308666_ >= p_308667_)
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("matches")
                                                   .then(
                                                      addConditional(
                                                         p_214438_,
                                                         Commands.argument("range", RangeArgument.intRange()),
                                                         p_214440_,
                                                         p_448905_ -> checkScore(p_448905_, RangeArgument.Ints.getRange(p_448905_, "range"))
                                                      )
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("blocks")
                              .then(
                                 Commands.argument("start", BlockPosArgument.blockPos())
                                    .then(
                                       Commands.argument("end", BlockPosArgument.blockPos())
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                                                   .then(addIfBlocksConditional(p_214438_, Commands.literal("all"), p_214440_, false)))
                                                .then(addIfBlocksConditional(p_214438_, Commands.literal("masked"), p_214440_, true))
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("entity")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("entities", EntityArgument.entities())
                                    .fork(
                                       p_214438_,
                                       p_137232_ -> expect(p_137232_, p_214440_, !EntityArgument.getOptionalEntities(p_137232_, "entities").isEmpty())
                                    ))
                                 .executes(
                                    createNumericConditionalHandler(p_214440_, p_137189_ -> EntityArgument.getOptionalEntities(p_137189_, "entities").size())
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("predicate")
                        .then(
                           addConditional(
                              p_214438_,
                              Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(p_214441_)),
                              p_214440_,
                              p_326238_ -> checkCustomPredicate(
                                 (CommandSourceStack)p_326238_.getSource(), ResourceOrIdArgument.getLootPredicate(p_326238_, "predicate")
                              )
                           )
                        )
                  ))
               .then(
                  Commands.literal("function")
                     .then(
                        Commands.argument("name", FunctionArgument.functions())
                           .suggests(FunctionCommand.SUGGEST_FUNCTION)
                           .fork(p_214438_, new ExecuteCommand.ExecuteIfFunctionCustomModifier(p_214440_))
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("items")
                     .then(
                        Commands.literal("entity")
                           .then(
                              Commands.argument("entities", EntityArgument.entities())
                                 .then(
                                    Commands.argument("slots", SlotsArgument.slots())
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(p_214441_))
                                                .fork(
                                                   p_214438_,
                                                   p_326243_ -> expect(
                                                      p_326243_,
                                                      p_214440_,
                                                      countItems(
                                                            EntityArgument.getEntities(p_326243_, "entities"),
                                                            SlotsArgument.getSlots(p_326243_, "slots"),
                                                            ItemPredicateArgument.getItemPredicate(p_326243_, "item_predicate")
                                                         )
                                                         > 0
                                                   )
                                                ))
                                             .executes(
                                                createNumericConditionalHandler(
                                                   p_214440_,
                                                   p_326239_ -> countItems(
                                                      EntityArgument.getEntities(p_326239_, "entities"),
                                                      SlotsArgument.getSlots(p_326239_, "slots"),
                                                      ItemPredicateArgument.getItemPredicate(p_326239_, "item_predicate")
                                                   )
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("block")
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .then(
                                 Commands.argument("slots", SlotsArgument.slots())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(p_214441_))
                                             .fork(
                                                p_214438_,
                                                p_326241_ -> expect(
                                                   p_326241_,
                                                   p_214440_,
                                                   countItems(
                                                         (CommandSourceStack)p_326241_.getSource(),
                                                         BlockPosArgument.getLoadedBlockPos(p_326241_, "pos"),
                                                         SlotsArgument.getSlots(p_326241_, "slots"),
                                                         ItemPredicateArgument.getItemPredicate(p_326241_, "item_predicate")
                                                      )
                                                      > 0
                                                )
                                             ))
                                          .executes(
                                             createNumericConditionalHandler(
                                                p_214440_,
                                                p_326246_ -> countItems(
                                                   (CommandSourceStack)p_326246_.getSource(),
                                                   BlockPosArgument.getLoadedBlockPos(p_326246_, "pos"),
                                                   SlotsArgument.getSlots(p_326246_, "slots"),
                                                   ItemPredicateArgument.getItemPredicate(p_326246_, "item_predicate")
                                                )
                                             )
                                          )
                                    )
                              )
                        )
                  )
            ))
         .then(
            Commands.literal("stopwatch")
               .then(
                  Commands.argument("id", IdentifierArgument.id())
                     .suggests(StopwatchCommand.SUGGEST_STOPWATCHES)
                     .then(
                        addConditional(
                           p_214438_,
                           Commands.argument("range", RangeArgument.floatRange()),
                           p_214440_,
                           p_448906_ -> checkStopwatch(p_448906_, RangeArgument.Floats.getRange(p_448906_, "range"))
                        )
                     )
               )
         );

      for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         p_214439_.then(
            datacommands$dataprovider.wrap(
               Commands.literal("data"),
               p_137092_ -> p_137092_.then(
                  ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                        .fork(
                           p_214438_,
                           p_180175_ -> expect(
                              p_180175_,
                              p_214440_,
                              checkMatchingData(datacommands$dataprovider.access(p_180175_), NbtPathArgument.getPath(p_180175_, "path")) > 0
                           )
                        ))
                     .executes(
                        createNumericConditionalHandler(
                           p_214440_, p_180152_ -> checkMatchingData(datacommands$dataprovider.access(p_180152_), NbtPathArgument.getPath(p_180152_, "path"))
                        )
                     )
               )
            )
         );
      }

      return p_214439_;
   }

   private static int countItems(Iterable<? extends SlotProvider> p_333878_, SlotRange p_329600_, Predicate<ItemStack> p_334297_) {
      int i = 0;

      for (SlotProvider slotprovider : p_333878_) {
         IntList intlist = p_329600_.slots();

         for (int j = 0; j < intlist.size(); j++) {
            int k = intlist.getInt(j);
            SlotAccess slotaccess = slotprovider.getSlot(k);
            if (slotaccess != null) {
               ItemStack itemstack = slotaccess.get();
               if (p_334297_.test(itemstack)) {
                  i += itemstack.getCount();
               }
            }
         }
      }

      return i;
   }

   private static int countItems(CommandSourceStack p_330313_, BlockPos p_329129_, SlotRange p_327989_, Predicate<ItemStack> p_331312_) throws CommandSyntaxException {
      int i = 0;
      Container container = ItemCommands.getContainer(p_330313_, p_329129_, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
      int j = container.getContainerSize();
      IntList intlist = p_327989_.slots();

      for (int k = 0; k < intlist.size(); k++) {
         int l = intlist.getInt(k);
         if (l >= 0 && l < j) {
            ItemStack itemstack = container.getItem(l);
            if (p_331312_.test(itemstack)) {
               i += itemstack.getCount();
            }
         }
      }

      return i;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean p_137167_, ExecuteCommand.CommandNumericPredicate p_137168_) {
      return p_137167_ ? p_288391_ -> {
         int i = p_137168_.test(p_288391_);
         if (i > 0) {
            ((CommandSourceStack)p_288391_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", i), false);
            return i;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : p_288393_ -> {
         int i = p_137168_.test(p_288393_);
         if (i == 0) {
            ((CommandSourceStack)p_288393_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
         }
      };
   }

   private static int checkMatchingData(DataAccessor p_137146_, NbtPathArgument.NbtPath p_137147_) throws CommandSyntaxException {
      return p_137147_.countMatching(p_137146_.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> p_137065_, ExecuteCommand.IntBiPredicate p_312767_) throws CommandSyntaxException {
      ScoreHolder scoreholder = ScoreHolderArgument.getName(p_137065_, "target");
      Objective objective = ObjectiveArgument.getObjective(p_137065_, "targetObjective");
      ScoreHolder scoreholder1 = ScoreHolderArgument.getName(p_137065_, "source");
      Objective objective1 = ObjectiveArgument.getObjective(p_137065_, "sourceObjective");
      Scoreboard scoreboard = ((CommandSourceStack)p_137065_.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
      ReadOnlyScoreInfo readonlyscoreinfo1 = scoreboard.getPlayerScoreInfo(scoreholder1, objective1);
      return readonlyscoreinfo != null && readonlyscoreinfo1 != null ? p_312767_.test(readonlyscoreinfo.value(), readonlyscoreinfo1.value()) : false;
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> p_137059_, MinMaxBounds.Ints p_455750_) throws CommandSyntaxException {
      ScoreHolder scoreholder = ScoreHolderArgument.getName(p_137059_, "target");
      Objective objective = ObjectiveArgument.getObjective(p_137059_, "targetObjective");
      Scoreboard scoreboard = ((CommandSourceStack)p_137059_.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
      return readonlyscoreinfo == null ? false : p_455750_.matches(readonlyscoreinfo.value());
   }

   private static boolean checkStopwatch(CommandContext<CommandSourceStack> p_455833_, MinMaxBounds.Doubles p_453915_) throws CommandSyntaxException {
      Identifier identifier = IdentifierArgument.getId(p_455833_, "id");
      Stopwatches stopwatches = ((CommandSourceStack)p_455833_.getSource()).getServer().getStopwatches();
      Stopwatch stopwatch = stopwatches.get(identifier);
      if (stopwatch == null) {
         throw StopwatchCommand.ERROR_DOES_NOT_EXIST.create(identifier);
      }

      long i = Stopwatches.currentTime();
      double d0 = stopwatch.elapsedSeconds(i);
      return p_453915_.matches(d0);
   }

   private static boolean checkCustomPredicate(CommandSourceStack p_137105_, Holder<LootItemCondition> p_335377_) {
      ServerLevel serverlevel = p_137105_.getLevel();
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_137105_.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, p_137105_.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      lootcontext.pushVisitedElement(LootContext.createVisitedEntry(p_335377_.value()));
      return p_335377_.value().test(lootcontext);
   }

   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> p_137071_, boolean p_137072_, boolean p_137073_) {
      return p_137073_ == p_137072_ ? Collections.singleton((CommandSourceStack)p_137071_.getSource()) : Collections.emptyList();
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
      CommandNode<CommandSourceStack> p_137075_, ArgumentBuilder<CommandSourceStack, ?> p_137076_, boolean p_137077_, ExecuteCommand.CommandPredicate p_137078_
   ) {
      return p_137076_.fork(p_137075_, p_137214_ -> expect(p_137214_, p_137077_, p_137078_.test(p_137214_))).executes(p_288396_ -> {
         if (p_137077_ == p_137078_.test(p_288396_)) {
            ((CommandSourceStack)p_288396_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
      CommandNode<CommandSourceStack> p_137080_, ArgumentBuilder<CommandSourceStack, ?> p_137081_, boolean p_137082_, boolean p_137083_
   ) {
      return p_137081_.fork(p_137080_, p_137180_ -> expect(p_137180_, p_137082_, checkRegions(p_137180_, p_137083_).isPresent()))
         .executes(p_137082_ ? p_137210_ -> checkIfRegions(p_137210_, p_137083_) : p_137165_ -> checkUnlessRegions(p_137165_, p_137083_));
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> p_137068_, boolean p_137069_) throws CommandSyntaxException {
      OptionalInt optionalint = checkRegions(p_137068_, p_137069_);
      if (optionalint.isPresent()) {
         ((CommandSourceStack)p_137068_.getSource())
            .sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", optionalint.getAsInt()), false);
         return optionalint.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> p_137194_, boolean p_137195_) throws CommandSyntaxException {
      OptionalInt optionalint = checkRegions(p_137194_, p_137195_);
      if (optionalint.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalint.getAsInt());
      }

      ((CommandSourceStack)p_137194_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
      return 1;
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> p_137221_, boolean p_137222_) throws CommandSyntaxException {
      return checkRegions(
         ((CommandSourceStack)p_137221_.getSource()).getLevel(),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "start"),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "end"),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "destination"),
         p_137222_
      );
   }

   private static OptionalInt checkRegions(ServerLevel p_137037_, BlockPos p_137038_, BlockPos p_137039_, BlockPos p_137040_, boolean p_137041_) throws CommandSyntaxException {
      BoundingBox boundingbox = BoundingBox.fromCorners(p_137038_, p_137039_);
      BoundingBox boundingbox1 = BoundingBox.fromCorners(p_137040_, p_137040_.offset(boundingbox.getLength()));
      BlockPos blockpos = new BlockPos(
         boundingbox1.minX() - boundingbox.minX(), boundingbox1.minY() - boundingbox.minY(), boundingbox1.minZ() - boundingbox.minZ()
      );
      int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
      if (i > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, i);
      }

      int j = 0;
      RegistryAccess registryaccess = p_137037_.registryAccess();

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
         for (int k = boundingbox.minZ(); k <= boundingbox.maxZ(); k++) {
            for (int l = boundingbox.minY(); l <= boundingbox.maxY(); l++) {
               for (int i1 = boundingbox.minX(); i1 <= boundingbox.maxX(); i1++) {
                  BlockPos blockpos1 = new BlockPos(i1, l, k);
                  BlockPos blockpos2 = blockpos1.offset(blockpos);
                  BlockState blockstate = p_137037_.getBlockState(blockpos1);
                  if (!p_137041_ || !blockstate.is(Blocks.AIR)) {
                     if (blockstate != p_137037_.getBlockState(blockpos2)) {
                        return OptionalInt.empty();
                     }

                     BlockEntity blockentity = p_137037_.getBlockEntity(blockpos1);
                     BlockEntity blockentity1 = p_137037_.getBlockEntity(blockpos2);
                     if (blockentity != null) {
                        if (blockentity1 == null) {
                           return OptionalInt.empty();
                        }

                        if (blockentity1.getType() != blockentity.getType()) {
                           return OptionalInt.empty();
                        }

                        if (!blockentity.components().equals(blockentity1.components())) {
                           return OptionalInt.empty();
                        }

                        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(
                           problemreporter$scopedcollector.forChild(blockentity.problemPath()), registryaccess
                        );
                        blockentity.saveCustomOnly(tagvalueoutput);
                        CompoundTag compoundtag = tagvalueoutput.buildResult();
                        TagValueOutput tagvalueoutput1 = TagValueOutput.createWithContext(
                           problemreporter$scopedcollector.forChild(blockentity1.problemPath()), registryaccess
                        );
                        blockentity1.saveCustomOnly(tagvalueoutput1);
                        CompoundTag compoundtag1 = tagvalueoutput1.buildResult();
                        if (!compoundtag.equals(compoundtag1)) {
                           return OptionalInt.empty();
                        }
                     }

                     j++;
                  }
               }
            }
         }
      }

      return OptionalInt.of(j);
   }

   private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> p_265114_) {
      return p_264786_ -> {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_264786_.getSource();
         Entity entity = commandsourcestack.getEntity();
         return entity == null
            ? List.of()
            : p_265114_.apply(entity)
               .filter(p_264783_ -> !p_264783_.isRemoved())
               .map(p_264775_ -> List.of(commandsourcestack.withEntity(p_264775_)))
               .orElse(List.of());
      };
   }

   private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> p_265496_) {
      return p_264780_ -> {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_264780_.getSource();
         Entity entity = commandsourcestack.getEntity();
         return entity == null ? List.of() : p_265496_.apply(entity).filter(p_264784_ -> !p_264784_.isRemoved()).map(commandsourcestack::withEntity).toList();
      };
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
      CommandNode<CommandSourceStack> p_265189_, LiteralArgumentBuilder<CommandSourceStack> p_265783_
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)p_265783_.then(
                              Commands.literal("owner")
                                 .fork(
                                    p_265189_,
                                    expandOneToOneEntityRelation(
                                       p_269758_ -> p_269758_ instanceof OwnableEntity ownableentity
                                          ? Optional.ofNullable(ownableentity.getOwner())
                                          : Optional.empty()
                                    )
                                 )
                           ))
                           .then(
                              Commands.literal("leasher")
                                 .fork(
                                    p_265189_,
                                    expandOneToOneEntityRelation(
                                       p_341160_ -> p_341160_ instanceof Leashable leashable
                                          ? Optional.ofNullable(leashable.getLeashHolder())
                                          : Optional.empty()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("target")
                              .fork(
                                 p_265189_,
                                 expandOneToOneEntityRelation(
                                    p_272389_ -> p_272389_ instanceof Targeting targeting ? Optional.ofNullable(targeting.getTarget()) : Optional.empty()
                                 )
                              )
                        ))
                     .then(
                        Commands.literal("attacker")
                           .fork(
                              p_265189_,
                              expandOneToOneEntityRelation(
                                 p_272388_ -> p_272388_ instanceof Attackable attackable ? Optional.ofNullable(attackable.getLastAttacker()) : Optional.empty()
                              )
                           )
                     ))
                  .then(Commands.literal("vehicle").fork(p_265189_, expandOneToOneEntityRelation(p_264776_ -> Optional.ofNullable(p_264776_.getVehicle())))))
               .then(
                  Commands.literal("controller")
                     .fork(p_265189_, expandOneToOneEntityRelation(p_274815_ -> Optional.ofNullable(p_274815_.getControllingPassenger())))
               ))
            .then(
               Commands.literal("origin")
                  .fork(
                     p_265189_,
                     expandOneToOneEntityRelation(
                        p_266631_ -> p_266631_ instanceof TraceableEntity traceableentity ? Optional.ofNullable(traceableentity.getOwner()) : Optional.empty()
                     )
                  )
            ))
         .then(Commands.literal("passengers").fork(p_265189_, expandOneToManyEntityRelation(p_264777_ -> p_264777_.getPassengers().stream())));
   }

   private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack p_270320_, Holder.Reference<EntityType<?>> p_270344_) throws CommandSyntaxException {
      Entity entity = SummonCommand.createEntity(p_270320_, p_270344_, p_270320_.getPosition(), new CompoundTag(), true);
      return p_270320_.withEntity(entity);
   }

   public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(
      T p_311643_,
      List<T> p_313143_,
      Function<T, T> p_312920_,
      IntPredicate p_311696_,
      ContextChain<T> p_311450_,
      @Nullable CompoundTag p_313177_,
      ExecutionControl<T> p_310605_,
      InCommandFunction<CommandContext<T>, Collection<CommandFunction<T>>> p_393996_,
      ChainModifiers p_312493_
   ) {
      List<T> list = new ArrayList<>(p_313143_.size());

      Collection<CommandFunction<T>> collection;
      try {
         collection = p_393996_.apply(p_311450_.getTopContext().copyFor(p_311643_));
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_311643_.handleError(commandsyntaxexception, p_312493_.isForked(), p_310605_.tracer());
         return;
      }

      int i = collection.size();
      if (i != 0) {
         List<InstantiatedFunction<T>> list1 = new ArrayList<>(i);

         try {
            for (CommandFunction<T> commandfunction : collection) {
               try {
                  list1.add(commandfunction.instantiate(p_313177_, p_311643_.dispatcher()));
               } catch (FunctionInstantiationException functioninstantiationexception) {
                  throw ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create(commandfunction.id(), functioninstantiationexception.messageComponent());
               }
            }
         } catch (CommandSyntaxException commandsyntaxexception1) {
            p_311643_.handleError(commandsyntaxexception1, p_312493_.isForked(), p_310605_.tracer());
         }

         for (T t1 : p_313143_) {
            T t = (T)p_312920_.apply(t1.clearCallbacks());
            CommandResultCallback commandresultcallback = (p_308674_, p_308675_) -> {
               if (p_311696_.test(p_308675_)) {
                  list.add(t1);
               }
            };
            p_310605_.queueNext(new IsolatedCall<>(p_308664_ -> {
               for (InstantiatedFunction<T> instantiatedfunction : list1) {
                  p_308664_.queueNext(new CallFunction<>(instantiatedfunction, p_308664_.currentFrame().returnValueConsumer(), true).bind(t));
               }

               p_308664_.queueNext(FallthroughTask.instance());
            }, commandresultcallback));
         }

         ContextChain<T> contextchain = p_311450_.nextStage();
         String s = p_311450_.getTopContext().getInput();
         p_310605_.queueNext(new BuildContexts.Continuation<>(s, contextchain, p_312493_, p_311643_, list));
      }
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   static class ExecuteIfFunctionCustomModifier implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
      private final IntPredicate check;

      ExecuteIfFunctionCustomModifier(boolean p_311621_) {
         this.check = p_311621_ ? p_311044_ -> p_311044_ != 0 : p_311222_ -> p_311222_ == 0;
      }

      public void apply(
         CommandSourceStack p_312164_,
         List<CommandSourceStack> p_311631_,
         ContextChain<CommandSourceStack> p_310925_,
         ChainModifiers p_312971_,
         ExecutionControl<CommandSourceStack> p_312212_
      ) {
         ExecuteCommand.scheduleFunctionConditionsAndTest(
            p_312164_,
            p_311631_,
            FunctionCommand::modifySenderForExecution,
            this.check,
            p_310925_,
            null,
            p_312212_,
            p_312564_ -> FunctionArgument.getFunctions(p_312564_, "name"),
            p_312971_
         );
      }
   }

   @FunctionalInterface
   interface IntBiPredicate {
      boolean test(int var1, int var2);
   }
}
