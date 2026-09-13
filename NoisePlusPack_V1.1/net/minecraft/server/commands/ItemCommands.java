package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
   static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (p_308755_, p_308756_, p_308757_) -> Component.translatableEscape("commands.item.target.not_a_container", p_308755_, p_308756_, p_308757_)
   );
   static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (p_308752_, p_308753_, p_308754_) -> Component.translatableEscape("commands.item.source.not_a_container", p_308752_, p_308753_, p_308754_)
   );
   static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      p_308758_ -> Component.translatableEscape("commands.item.target.no_such_slot", p_308758_)
   );
   private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      p_308749_ -> Component.translatableEscape("commands.item.source.no_such_slot", p_308749_)
   );
   private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(
      p_308748_ -> Component.translatableEscape("commands.item.target.no_changes", p_308748_)
   );
   private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType(
      (p_308750_, p_308751_) -> Component.translatableEscape("commands.item.target.no_changed.known_item", p_308750_, p_308751_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_214449_, CommandBuildContext p_214450_) {
      p_214449_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("item")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("replace")
                        .then(
                           Commands.literal("block")
                              .then(
                                 Commands.argument("pos", BlockPosArgument.blockPos())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot())
                                             .then(
                                                Commands.literal("with")
                                                   .then(
                                                      ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(p_214450_))
                                                            .executes(
                                                               p_180383_ -> setBlockItem(
                                                                  (CommandSourceStack)p_180383_.getSource(),
                                                                  BlockPosArgument.getLoadedBlockPos(p_180383_, "pos"),
                                                                  SlotArgument.getSlot(p_180383_, "slot"),
                                                                  ItemArgument.getItem(p_180383_, "item").createItemStack(1, false)
                                                               )
                                                            ))
                                                         .then(
                                                            Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                               .executes(
                                                                  p_180381_ -> setBlockItem(
                                                                     (CommandSourceStack)p_180381_.getSource(),
                                                                     BlockPosArgument.getLoadedBlockPos(p_180381_, "pos"),
                                                                     SlotArgument.getSlot(p_180381_, "slot"),
                                                                     ItemArgument.getItem(p_180381_, "item")
                                                                        .createItemStack(IntegerArgumentType.getInteger(p_180381_, "count"), true)
                                                                  )
                                                               )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("from")
                                                   .then(
                                                      Commands.literal("block")
                                                         .then(
                                                            Commands.argument("source", BlockPosArgument.blockPos())
                                                               .then(
                                                                  ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                        .executes(
                                                                           p_180379_ -> blockToBlock(
                                                                              (CommandSourceStack)p_180379_.getSource(),
                                                                              BlockPosArgument.getLoadedBlockPos(p_180379_, "source"),
                                                                              SlotArgument.getSlot(p_180379_, "sourceSlot"),
                                                                              BlockPosArgument.getLoadedBlockPos(p_180379_, "pos"),
                                                                              SlotArgument.getSlot(p_180379_, "slot")
                                                                           )
                                                                        ))
                                                                     .then(
                                                                        Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                                                           .executes(
                                                                              p_326276_ -> blockToBlock(
                                                                                 (CommandSourceStack)p_326276_.getSource(),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_326276_, "source"),
                                                                                 SlotArgument.getSlot(p_326276_, "sourceSlot"),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_326276_, "pos"),
                                                                                 SlotArgument.getSlot(p_326276_, "slot"),
                                                                                 ResourceOrIdArgument.getLootModifier(p_326276_, "modifier")
                                                                              )
                                                                           )
                                                                     )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("entity")
                                                      .then(
                                                         Commands.argument("source", EntityArgument.entity())
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                     .executes(
                                                                        p_180375_ -> entityToBlock(
                                                                           (CommandSourceStack)p_180375_.getSource(),
                                                                           EntityArgument.getEntity(p_180375_, "source"),
                                                                           SlotArgument.getSlot(p_180375_, "sourceSlot"),
                                                                           BlockPosArgument.getLoadedBlockPos(p_180375_, "pos"),
                                                                           SlotArgument.getSlot(p_180375_, "slot")
                                                                        )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                                                        .executes(
                                                                           p_326280_ -> entityToBlock(
                                                                              (CommandSourceStack)p_326280_.getSource(),
                                                                              EntityArgument.getEntity(p_326280_, "source"),
                                                                              SlotArgument.getSlot(p_326280_, "sourceSlot"),
                                                                              BlockPosArgument.getLoadedBlockPos(p_326280_, "pos"),
                                                                              SlotArgument.getSlot(p_326280_, "slot"),
                                                                              ResourceOrIdArgument.getLootModifier(p_326280_, "modifier")
                                                                           )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("entity")
                           .then(
                              Commands.argument("targets", EntityArgument.entities())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot())
                                          .then(
                                             Commands.literal("with")
                                                .then(
                                                   ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(p_214450_))
                                                         .executes(
                                                            p_180371_ -> setEntityItem(
                                                               (CommandSourceStack)p_180371_.getSource(),
                                                               EntityArgument.getEntities(p_180371_, "targets"),
                                                               SlotArgument.getSlot(p_180371_, "slot"),
                                                               ItemArgument.getItem(p_180371_, "item").createItemStack(1, false)
                                                            )
                                                         ))
                                                      .then(
                                                         Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                            .executes(
                                                               p_180369_ -> setEntityItem(
                                                                  (CommandSourceStack)p_180369_.getSource(),
                                                                  EntityArgument.getEntities(p_180369_, "targets"),
                                                                  SlotArgument.getSlot(p_180369_, "slot"),
                                                                  ItemArgument.getItem(p_180369_, "item")
                                                                     .createItemStack(IntegerArgumentType.getInteger(p_180369_, "count"), true)
                                                               )
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.literal("from")
                                                .then(
                                                   Commands.literal("block")
                                                      .then(
                                                         Commands.argument("source", BlockPosArgument.blockPos())
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                     .executes(
                                                                        p_180367_ -> blockToEntities(
                                                                           (CommandSourceStack)p_180367_.getSource(),
                                                                           BlockPosArgument.getLoadedBlockPos(p_180367_, "source"),
                                                                           SlotArgument.getSlot(p_180367_, "sourceSlot"),
                                                                           EntityArgument.getEntities(p_180367_, "targets"),
                                                                           SlotArgument.getSlot(p_180367_, "slot")
                                                                        )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                                                        .executes(
                                                                           p_326277_ -> blockToEntities(
                                                                              (CommandSourceStack)p_326277_.getSource(),
                                                                              BlockPosArgument.getLoadedBlockPos(p_326277_, "source"),
                                                                              SlotArgument.getSlot(p_326277_, "sourceSlot"),
                                                                              EntityArgument.getEntities(p_326277_, "targets"),
                                                                              SlotArgument.getSlot(p_326277_, "slot"),
                                                                              ResourceOrIdArgument.getLootModifier(p_326277_, "modifier")
                                                                           )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("entity")
                                                   .then(
                                                      Commands.argument("source", EntityArgument.entity())
                                                         .then(
                                                            ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                  .executes(
                                                                     p_180363_ -> entityToEntities(
                                                                        (CommandSourceStack)p_180363_.getSource(),
                                                                        EntityArgument.getEntity(p_180363_, "source"),
                                                                        SlotArgument.getSlot(p_180363_, "sourceSlot"),
                                                                        EntityArgument.getEntities(p_180363_, "targets"),
                                                                        SlotArgument.getSlot(p_180363_, "slot")
                                                                     )
                                                                  ))
                                                               .then(
                                                                  Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                                                     .executes(
                                                                        p_326275_ -> entityToEntities(
                                                                           (CommandSourceStack)p_326275_.getSource(),
                                                                           EntityArgument.getEntity(p_326275_, "source"),
                                                                           SlotArgument.getSlot(p_326275_, "sourceSlot"),
                                                                           EntityArgument.getEntities(p_326275_, "targets"),
                                                                           SlotArgument.getSlot(p_326275_, "slot"),
                                                                           ResourceOrIdArgument.getLootModifier(p_326275_, "modifier")
                                                                        )
                                                                     )
                                                               )
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("modify")
                     .then(
                        Commands.literal("block")
                           .then(
                              Commands.argument("pos", BlockPosArgument.blockPos())
                                 .then(
                                    Commands.argument("slot", SlotArgument.slot())
                                       .then(
                                          Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                             .executes(
                                                p_326282_ -> modifyBlockItem(
                                                   (CommandSourceStack)p_326282_.getSource(),
                                                   BlockPosArgument.getLoadedBlockPos(p_326282_, "pos"),
                                                   SlotArgument.getSlot(p_326282_, "slot"),
                                                   ResourceOrIdArgument.getLootModifier(p_326282_, "modifier")
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("targets", EntityArgument.entities())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.argument("modifier", ResourceOrIdArgument.lootModifier(p_214450_))
                                          .executes(
                                             p_326281_ -> modifyEntityItem(
                                                (CommandSourceStack)p_326281_.getSource(),
                                                EntityArgument.getEntities(p_326281_, "targets"),
                                                SlotArgument.getSlot(p_326281_, "slot"),
                                                ResourceOrIdArgument.getLootModifier(p_326281_, "modifier")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int modifyBlockItem(CommandSourceStack p_180297_, BlockPos p_180298_, int p_180299_, Holder<LootItemFunction> p_332634_) throws CommandSyntaxException {
      Container container = getContainer(p_180297_, p_180298_, ERROR_TARGET_NOT_A_CONTAINER);
      if (p_180299_ >= 0 && p_180299_ < container.getContainerSize()) {
         ItemStack itemstack = applyModifier(p_180297_, p_332634_, container.getItem(p_180299_));
         container.setItem(p_180299_, itemstack);
         p_180297_.sendSuccess(
            () -> Component.translatable("commands.item.block.set.success", p_180298_.getX(), p_180298_.getY(), p_180298_.getZ(), itemstack.getDisplayName()),
            true
         );
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(p_180299_);
      }
   }

   private static int modifyEntityItem(CommandSourceStack p_180337_, Collection<? extends Entity> p_180338_, int p_180339_, Holder<LootItemFunction> p_333636_) throws CommandSyntaxException {
      Map<Entity, ItemStack> map = Maps.newHashMapWithExpectedSize(p_180338_.size());

      for (Entity entity : p_180338_) {
         SlotAccess slotaccess = entity.getSlot(p_180339_);
         if (slotaccess != null) {
            ItemStack itemstack = applyModifier(p_180337_, p_333636_, slotaccess.get().copy());
            if (slotaccess.set(itemstack)) {
               map.put(entity, itemstack);
               if (entity instanceof ServerPlayer serverplayer) {
                  serverplayer.containerMenu.broadcastChanges();
               }
            }
         }
      }

      if (map.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES.create(p_180339_);
      }

      if (map.size() == 1) {
         Entry<Entity, ItemStack> entry = map.entrySet().iterator().next();
         p_180337_.sendSuccess(
            () -> Component.translatable("commands.item.entity.set.success.single", entry.getKey().getDisplayName(), entry.getValue().getDisplayName()), true
         );
      } else {
         p_180337_.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", map.size()), true);
      }

      return map.size();
   }

   private static int setBlockItem(CommandSourceStack p_180292_, BlockPos p_180293_, int p_180294_, ItemStack p_180295_) throws CommandSyntaxException {
      Container container = getContainer(p_180292_, p_180293_, ERROR_TARGET_NOT_A_CONTAINER);
      if (p_180294_ >= 0 && p_180294_ < container.getContainerSize()) {
         container.setItem(p_180294_, p_180295_);
         p_180292_.sendSuccess(
            () -> Component.translatable("commands.item.block.set.success", p_180293_.getX(), p_180293_.getY(), p_180293_.getZ(), p_180295_.getDisplayName()),
            true
         );
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(p_180294_);
      }
   }

   static Container getContainer(CommandSourceStack p_180328_, BlockPos p_180329_, Dynamic3CommandExceptionType p_180330_) throws CommandSyntaxException {
      if (p_180328_.getLevel().getBlockEntity(p_180329_) instanceof Container container) {
         return container;
      } else {
         throw p_180330_.create(p_180329_.getX(), p_180329_.getY(), p_180329_.getZ());
      }
   }

   private static int setEntityItem(CommandSourceStack p_180332_, Collection<? extends Entity> p_180333_, int p_180334_, ItemStack p_180335_) throws CommandSyntaxException {
      List<Entity> list = Lists.newArrayListWithCapacity(p_180333_.size());

      for (Entity entity : p_180333_) {
         SlotAccess slotaccess = entity.getSlot(p_180334_);
         if (slotaccess != null && slotaccess.set(p_180335_.copy())) {
            list.add(entity);
            if (entity instanceof ServerPlayer serverplayer) {
               serverplayer.containerMenu.broadcastChanges();
            }
         }
      }

      if (list.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(p_180335_.getDisplayName(), p_180334_);
      }

      if (list.size() == 1) {
         p_180332_.sendSuccess(
            () -> Component.translatable("commands.item.entity.set.success.single", list.getFirst().getDisplayName(), p_180335_.getDisplayName()), true
         );
      } else {
         p_180332_.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", list.size(), p_180335_.getDisplayName()), true);
      }

      return list.size();
   }

   private static int blockToEntities(CommandSourceStack p_180315_, BlockPos p_180316_, int p_180317_, Collection<? extends Entity> p_180318_, int p_180319_) throws CommandSyntaxException {
      return setEntityItem(p_180315_, p_180318_, p_180319_, getBlockItem(p_180315_, p_180316_, p_180317_));
   }

   private static int blockToEntities(
      CommandSourceStack p_180321_,
      BlockPos p_180322_,
      int p_180323_,
      Collection<? extends Entity> p_180324_,
      int p_180325_,
      Holder<LootItemFunction> p_329510_
   ) throws CommandSyntaxException {
      return setEntityItem(p_180321_, p_180324_, p_180325_, applyModifier(p_180321_, p_329510_, getBlockItem(p_180321_, p_180322_, p_180323_)));
   }

   private static int blockToBlock(CommandSourceStack p_180302_, BlockPos p_180303_, int p_180304_, BlockPos p_180305_, int p_180306_) throws CommandSyntaxException {
      return setBlockItem(p_180302_, p_180305_, p_180306_, getBlockItem(p_180302_, p_180303_, p_180304_));
   }

   private static int blockToBlock(
      CommandSourceStack p_180308_, BlockPos p_180309_, int p_180310_, BlockPos p_180311_, int p_180312_, Holder<LootItemFunction> p_330732_
   ) throws CommandSyntaxException {
      return setBlockItem(p_180308_, p_180311_, p_180312_, applyModifier(p_180308_, p_330732_, getBlockItem(p_180308_, p_180309_, p_180310_)));
   }

   private static int entityToBlock(CommandSourceStack p_180258_, Entity p_180259_, int p_180260_, BlockPos p_180261_, int p_180262_) throws CommandSyntaxException {
      return setBlockItem(p_180258_, p_180261_, p_180262_, getItemInSlot(p_180259_, p_180260_));
   }

   private static int entityToBlock(
      CommandSourceStack p_180264_, Entity p_180265_, int p_180266_, BlockPos p_180267_, int p_180268_, Holder<LootItemFunction> p_327828_
   ) throws CommandSyntaxException {
      return setBlockItem(p_180264_, p_180267_, p_180268_, applyModifier(p_180264_, p_327828_, getItemInSlot(p_180265_, p_180266_)));
   }

   private static int entityToEntities(CommandSourceStack p_180271_, Entity p_180272_, int p_180273_, Collection<? extends Entity> p_180274_, int p_180275_) throws CommandSyntaxException {
      return setEntityItem(p_180271_, p_180274_, p_180275_, getItemInSlot(p_180272_, p_180273_));
   }

   private static int entityToEntities(
      CommandSourceStack p_180277_, Entity p_180278_, int p_180279_, Collection<? extends Entity> p_180280_, int p_180281_, Holder<LootItemFunction> p_333656_
   ) throws CommandSyntaxException {
      return setEntityItem(p_180277_, p_180280_, p_180281_, applyModifier(p_180277_, p_333656_, getItemInSlot(p_180278_, p_180279_)));
   }

   private static ItemStack applyModifier(CommandSourceStack p_180284_, Holder<LootItemFunction> p_334601_, ItemStack p_180286_) {
      ServerLevel serverlevel = p_180284_.getLevel();
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_180284_.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, p_180284_.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      lootcontext.pushVisitedElement(LootContext.createVisitedEntry(p_334601_.value()));
      ItemStack itemstack = p_334601_.value().apply(p_180286_, lootcontext);
      itemstack.limitSize(itemstack.getMaxStackSize());
      return itemstack;
   }

   private static ItemStack getItemInSlot(SlotProvider p_451641_, int p_458671_) throws CommandSyntaxException {
      SlotAccess slotaccess = p_451641_.getSlot(p_458671_);
      if (slotaccess == null) {
         throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(p_458671_);
      } else {
         return slotaccess.get().copy();
      }
   }

   private static ItemStack getBlockItem(CommandSourceStack p_180288_, BlockPos p_180289_, int p_180290_) throws CommandSyntaxException {
      Container container = getContainer(p_180288_, p_180289_, ERROR_SOURCE_NOT_A_CONTAINER);
      return getItemInSlot(container, p_180290_);
   }
}
