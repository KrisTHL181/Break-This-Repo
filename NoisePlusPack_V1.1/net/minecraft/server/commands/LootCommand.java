package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
   private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType(
      p_308774_ -> Component.translatableEscape("commands.drop.no_held_items", p_308774_)
   );
   private static final DynamicCommandExceptionType ERROR_NO_ENTITY_LOOT_TABLE = new DynamicCommandExceptionType(
      p_308775_ -> Component.translatableEscape("commands.drop.no_loot_table.entity", p_308775_)
   );
   private static final DynamicCommandExceptionType ERROR_NO_BLOCK_LOOT_TABLE = new DynamicCommandExceptionType(
      p_358608_ -> Component.translatableEscape("commands.drop.no_loot_table.block", p_358608_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_214516_, CommandBuildContext p_214517_) {
      p_214516_.register(
         addTargets(
            (LiteralArgumentBuilder & ArgumentBuilder)Commands.literal("loot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)),
            (p_405178_, p_405179_) -> p_405178_.then(
                  Commands.literal("fish")
                     .then(
                        Commands.argument("loot_table", ResourceOrIdArgument.lootTable(p_214517_))
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                             "pos", BlockPosArgument.blockPos()
                                          )
                                          .executes(
                                             p_326309_ -> dropFishingLoot(
                                                p_326309_,
                                                ResourceOrIdArgument.getLootTable(p_326309_, "loot_table"),
                                                BlockPosArgument.getLoadedBlockPos(p_326309_, "pos"),
                                                ItemStack.EMPTY,
                                                p_405179_
                                             )
                                          ))
                                       .then(
                                          Commands.argument("tool", ItemArgument.item(p_214517_))
                                             .executes(
                                                p_326303_ -> dropFishingLoot(
                                                   p_326303_,
                                                   ResourceOrIdArgument.getLootTable(p_326303_, "loot_table"),
                                                   BlockPosArgument.getLoadedBlockPos(p_326303_, "pos"),
                                                   ItemArgument.getItem(p_326303_, "tool").createItemStack(1, false),
                                                   p_405179_
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("mainhand")
                                          .executes(
                                             p_326295_ -> dropFishingLoot(
                                                p_326295_,
                                                ResourceOrIdArgument.getLootTable(p_326295_, "loot_table"),
                                                BlockPosArgument.getLoadedBlockPos(p_326295_, "pos"),
                                                getSourceHandItem((CommandSourceStack)p_326295_.getSource(), EquipmentSlot.MAINHAND),
                                                p_405179_
                                             )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("offhand")
                                       .executes(
                                          p_326299_ -> dropFishingLoot(
                                             p_326299_,
                                             ResourceOrIdArgument.getLootTable(p_326299_, "loot_table"),
                                             BlockPosArgument.getLoadedBlockPos(p_326299_, "pos"),
                                             getSourceHandItem((CommandSourceStack)p_326299_.getSource(), EquipmentSlot.OFFHAND),
                                             p_405179_
                                          )
                                       )
                                 )
                           )
                     )
               )
               .then(
                  Commands.literal("loot")
                     .then(
                        Commands.argument("loot_table", ResourceOrIdArgument.lootTable(p_214517_))
                           .executes(p_326301_ -> dropChestLoot(p_326301_, ResourceOrIdArgument.getLootTable(p_326301_, "loot_table"), p_405179_))
                     )
               )
               .then(
                  Commands.literal("kill")
                     .then(
                        Commands.argument("target", EntityArgument.entity())
                           .executes(p_180406_ -> dropKillLoot(p_180406_, EntityArgument.getEntity(p_180406_, "target"), p_405179_))
                     )
               )
               .then(
                  Commands.literal("mine")
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                                    .executes(
                                       p_180403_ -> dropBlockLoot(p_180403_, BlockPosArgument.getLoadedBlockPos(p_180403_, "pos"), ItemStack.EMPTY, p_405179_)
                                    ))
                                 .then(
                                    Commands.argument("tool", ItemArgument.item(p_214517_))
                                       .executes(
                                          p_180400_ -> dropBlockLoot(
                                             p_180400_,
                                             BlockPosArgument.getLoadedBlockPos(p_180400_, "pos"),
                                             ItemArgument.getItem(p_180400_, "tool").createItemStack(1, false),
                                             p_405179_
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("mainhand")
                                    .executes(
                                       p_180397_ -> dropBlockLoot(
                                          p_180397_,
                                          BlockPosArgument.getLoadedBlockPos(p_180397_, "pos"),
                                          getSourceHandItem((CommandSourceStack)p_180397_.getSource(), EquipmentSlot.MAINHAND),
                                          p_405179_
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("offhand")
                                 .executes(
                                    p_180394_ -> dropBlockLoot(
                                       p_180394_,
                                       BlockPosArgument.getLoadedBlockPos(p_180394_, "pos"),
                                       getSourceHandItem((CommandSourceStack)p_180394_.getSource(), EquipmentSlot.OFFHAND),
                                       p_405179_
                                    )
                                 )
                           )
                     )
               )
         )
      );
   }

   private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(T p_137903_, LootCommand.TailProvider p_137904_) {
      return (T)p_137903_.then(
            ((LiteralArgumentBuilder)Commands.literal("replace")
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("entities", EntityArgument.entities())
                              .then(
                                 p_137904_.construct(
                                       Commands.argument("slot", SlotArgument.slot()),
                                       (p_138032_, p_138033_, p_138034_) -> entityReplace(
                                          EntityArgument.getEntities(p_138032_, "entities"),
                                          SlotArgument.getSlot(p_138032_, "slot"),
                                          p_138033_.size(),
                                          p_138033_,
                                          p_138034_
                                       )
                                    )
                                    .then(
                                       p_137904_.construct(
                                          Commands.argument("count", IntegerArgumentType.integer(0)),
                                          (p_138025_, p_138026_, p_138027_) -> entityReplace(
                                             EntityArgument.getEntities(p_138025_, "entities"),
                                             SlotArgument.getSlot(p_138025_, "slot"),
                                             IntegerArgumentType.getInteger(p_138025_, "count"),
                                             p_138026_,
                                             p_138027_
                                          )
                                       )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("block")
                     .then(
                        Commands.argument("targetPos", BlockPosArgument.blockPos())
                           .then(
                              p_137904_.construct(
                                    Commands.argument("slot", SlotArgument.slot()),
                                    (p_138018_, p_138019_, p_138020_) -> blockReplace(
                                       (CommandSourceStack)p_138018_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_138018_, "targetPos"),
                                       SlotArgument.getSlot(p_138018_, "slot"),
                                       p_138019_.size(),
                                       p_138019_,
                                       p_138020_
                                    )
                                 )
                                 .then(
                                    p_137904_.construct(
                                       Commands.argument("count", IntegerArgumentType.integer(0)),
                                       (p_138011_, p_138012_, p_138013_) -> blockReplace(
                                          (CommandSourceStack)p_138011_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_138011_, "targetPos"),
                                          IntegerArgumentType.getInteger(p_138011_, "slot"),
                                          IntegerArgumentType.getInteger(p_138011_, "count"),
                                          p_138012_,
                                          p_138013_
                                       )
                                    )
                                 )
                           )
                     )
               )
         )
         .then(
            Commands.literal("insert")
               .then(
                  p_137904_.construct(
                     Commands.argument("targetPos", BlockPosArgument.blockPos()),
                     (p_138004_, p_138005_, p_138006_) -> blockDistribute(
                        (CommandSourceStack)p_138004_.getSource(), BlockPosArgument.getLoadedBlockPos(p_138004_, "targetPos"), p_138005_, p_138006_
                     )
                  )
               )
         )
         .then(
            Commands.literal("give")
               .then(
                  p_137904_.construct(
                     Commands.argument("players", EntityArgument.players()),
                     (p_137992_, p_137993_, p_137994_) -> playerGive(EntityArgument.getPlayers(p_137992_, "players"), p_137993_, p_137994_)
                  )
               )
         )
         .then(
            Commands.literal("spawn")
               .then(
                  p_137904_.construct(
                     Commands.argument("targetPos", Vec3Argument.vec3()),
                     (p_137918_, p_137919_, p_137920_) -> dropInWorld(
                        (CommandSourceStack)p_137918_.getSource(), Vec3Argument.getVec3(p_137918_, "targetPos"), p_137919_, p_137920_
                     )
                  )
               )
         );
   }

   private static Container getContainer(CommandSourceStack p_137951_, BlockPos p_137952_) throws CommandSyntaxException {
      BlockEntity blockentity = p_137951_.getLevel().getBlockEntity(p_137952_);
      if (!(blockentity instanceof Container)) {
         throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create(p_137952_.getX(), p_137952_.getY(), p_137952_.getZ());
      } else {
         return (Container)blockentity;
      }
   }

   private static int blockDistribute(CommandSourceStack p_137961_, BlockPos p_137962_, List<ItemStack> p_137963_, LootCommand.Callback p_137964_) throws CommandSyntaxException {
      Container container = getContainer(p_137961_, p_137962_);
      List<ItemStack> list = Lists.newArrayListWithCapacity(p_137963_.size());

      for (ItemStack itemstack : p_137963_) {
         if (distributeToContainer(container, itemstack.copy())) {
            container.setChanged();
            list.add(itemstack);
         }
      }

      p_137964_.accept(list);
      return list.size();
   }

   private static boolean distributeToContainer(Container p_137886_, ItemStack p_137887_) {
      boolean flag = false;

      for (int i = 0; i < p_137886_.getContainerSize() && !p_137887_.isEmpty(); i++) {
         ItemStack itemstack = p_137886_.getItem(i);
         if (p_137886_.canPlaceItem(i, p_137887_)) {
            if (itemstack.isEmpty()) {
               p_137886_.setItem(i, p_137887_);
               flag = true;
               break;
            }

            if (canMergeItems(itemstack, p_137887_)) {
               int j = p_137887_.getMaxStackSize() - itemstack.getCount();
               int k = Math.min(p_137887_.getCount(), j);
               p_137887_.shrink(k);
               itemstack.grow(k);
               flag = true;
            }
         }
      }

      return flag;
   }

   private static int blockReplace(
      CommandSourceStack p_137954_, BlockPos p_137955_, int p_137956_, int p_137957_, List<ItemStack> p_137958_, LootCommand.Callback p_137959_
   ) throws CommandSyntaxException {
      Container container = getContainer(p_137954_, p_137955_);
      int i = container.getContainerSize();
      if (p_137956_ >= 0 && p_137956_ < i) {
         List<ItemStack> list = Lists.newArrayListWithCapacity(p_137958_.size());

         for (int j = 0; j < p_137957_; j++) {
            int k = p_137956_ + j;
            ItemStack itemstack = j < p_137958_.size() ? p_137958_.get(j) : ItemStack.EMPTY;
            if (container.canPlaceItem(k, itemstack)) {
               container.setItem(k, itemstack);
               list.add(itemstack);
            }
         }

         p_137959_.accept(list);
         return list.size();
      } else {
         throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create(p_137956_);
      }
   }

   private static boolean canMergeItems(ItemStack p_137895_, ItemStack p_137896_) {
      return p_137895_.getCount() <= p_137895_.getMaxStackSize() && ItemStack.isSameItemSameComponents(p_137895_, p_137896_);
   }

   private static int playerGive(Collection<ServerPlayer> p_137985_, List<ItemStack> p_137986_, LootCommand.Callback p_137987_) throws CommandSyntaxException {
      List<ItemStack> list = Lists.newArrayListWithCapacity(p_137986_.size());

      for (ItemStack itemstack : p_137986_) {
         for (ServerPlayer serverplayer : p_137985_) {
            if (serverplayer.getInventory().add(itemstack.copy())) {
               list.add(itemstack);
            }
         }
      }

      p_137987_.accept(list);
      return list.size();
   }

   private static void setSlots(Entity p_137889_, List<ItemStack> p_137890_, int p_137891_, int p_137892_, List<ItemStack> p_137893_) {
      for (int i = 0; i < p_137892_; i++) {
         ItemStack itemstack = i < p_137890_.size() ? p_137890_.get(i) : ItemStack.EMPTY;
         SlotAccess slotaccess = p_137889_.getSlot(p_137891_ + i);
         if (slotaccess != null && slotaccess.set(itemstack.copy())) {
            p_137893_.add(itemstack);
         }
      }
   }

   private static int entityReplace(
      Collection<? extends Entity> p_137979_, int p_137980_, int p_137981_, List<ItemStack> p_137982_, LootCommand.Callback p_137983_
   ) throws CommandSyntaxException {
      List<ItemStack> list = Lists.newArrayListWithCapacity(p_137982_.size());

      for (Entity entity : p_137979_) {
         if (entity instanceof ServerPlayer serverplayer) {
            setSlots(entity, p_137982_, p_137980_, p_137981_, list);
            serverplayer.containerMenu.broadcastChanges();
         } else {
            setSlots(entity, p_137982_, p_137980_, p_137981_, list);
         }
      }

      p_137983_.accept(list);
      return list.size();
   }

   private static int dropInWorld(CommandSourceStack p_137946_, Vec3 p_137947_, List<ItemStack> p_137948_, LootCommand.Callback p_137949_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_137946_.getLevel();
      p_137948_.forEach(p_137884_ -> {
         ItemEntity itementity = new ItemEntity(serverlevel, p_137947_.x, p_137947_.y, p_137947_.z, p_137884_.copy());
         itementity.setDefaultPickUpDelay();
         serverlevel.addFreshEntity(itementity);
      });
      p_137949_.accept(p_137948_);
      return p_137948_.size();
   }

   private static void callback(CommandSourceStack p_137966_, List<ItemStack> p_137967_) {
      if (p_137967_.size() == 1) {
         ItemStack itemstack = p_137967_.get(0);
         p_137966_.sendSuccess(() -> Component.translatable("commands.drop.success.single", itemstack.getCount(), itemstack.getDisplayName()), false);
      } else {
         p_137966_.sendSuccess(() -> Component.translatable("commands.drop.success.multiple", p_137967_.size()), false);
      }
   }

   private static void callback(CommandSourceStack p_137969_, List<ItemStack> p_137970_, ResourceKey<LootTable> p_327853_) {
      if (p_137970_.size() == 1) {
         ItemStack itemstack = p_137970_.get(0);
         p_137969_.sendSuccess(
            () -> Component.translatable(
               "commands.drop.success.single_with_table", itemstack.getCount(), itemstack.getDisplayName(), Component.translationArg(p_327853_.identifier())
            ),
            false
         );
      } else {
         p_137969_.sendSuccess(
            () -> Component.translatable("commands.drop.success.multiple_with_table", p_137970_.size(), Component.translationArg(p_327853_.identifier())),
            false
         );
      }
   }

   private static ItemStack getSourceHandItem(CommandSourceStack p_137939_, EquipmentSlot p_137940_) throws CommandSyntaxException {
      Entity entity = p_137939_.getEntityOrException();
      if (entity instanceof LivingEntity) {
         return ((LivingEntity)entity).getItemBySlot(p_137940_);
      } else {
         throw ERROR_NO_HELD_ITEMS.create(entity.getDisplayName());
      }
   }

   private static int dropBlockLoot(CommandContext<CommandSourceStack> p_137913_, BlockPos p_137914_, ItemStack p_137915_, LootCommand.DropConsumer p_137916_) throws CommandSyntaxException {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137913_.getSource();
      ServerLevel serverlevel = commandsourcestack.getLevel();
      BlockState blockstate = serverlevel.getBlockState(p_137914_);
      BlockEntity blockentity = serverlevel.getBlockEntity(p_137914_);
      Optional<ResourceKey<LootTable>> optional = blockstate.getBlock().getLootTable();
      if (optional.isEmpty()) {
         throw ERROR_NO_BLOCK_LOOT_TABLE.create(blockstate.getBlock().getName());
      }

      LootParams.Builder lootparams$builder = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p_137914_))
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, commandsourcestack.getEntity())
         .withParameter(LootContextParams.TOOL, p_137915_);
      List<ItemStack> list = blockstate.getDrops(lootparams$builder);
      return p_137916_.accept(p_137913_, list, p_358604_ -> callback(commandsourcestack, p_358604_, optional.get()));
   }

   private static int dropKillLoot(CommandContext<CommandSourceStack> p_137906_, Entity p_137907_, LootCommand.DropConsumer p_137908_) throws CommandSyntaxException {
      Optional<ResourceKey<LootTable>> optional = p_137907_.getLootTable();
      if (optional.isEmpty()) {
         throw ERROR_NO_ENTITY_LOOT_TABLE.create(p_137907_.getDisplayName());
      }

      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137906_.getSource();
      LootParams.Builder lootparams$builder = new LootParams.Builder(commandsourcestack.getLevel());
      Entity entity = commandsourcestack.getEntity();
      if (entity instanceof Player player) {
         lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
      }

      lootparams$builder.withParameter(LootContextParams.DAMAGE_SOURCE, p_137907_.damageSources().magic());
      lootparams$builder.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, entity);
      lootparams$builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, entity);
      lootparams$builder.withParameter(LootContextParams.THIS_ENTITY, p_137907_);
      lootparams$builder.withParameter(LootContextParams.ORIGIN, commandsourcestack.getPosition());
      LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
      LootTable loottable = commandsourcestack.getServer().reloadableRegistries().getLootTable(optional.get());
      List<ItemStack> list = loottable.getRandomItems(lootparams);
      return p_137908_.accept(p_137906_, list, p_358607_ -> callback(commandsourcestack, p_358607_, optional.get()));
   }

   private static int dropChestLoot(CommandContext<CommandSourceStack> p_137933_, Holder<LootTable> p_333711_, LootCommand.DropConsumer p_137935_) throws CommandSyntaxException {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137933_.getSource();
      LootParams lootparams = new LootParams.Builder(commandsourcestack.getLevel())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, commandsourcestack.getEntity())
         .withParameter(LootContextParams.ORIGIN, commandsourcestack.getPosition())
         .create(LootContextParamSets.CHEST);
      return drop(p_137933_, p_333711_, lootparams, p_137935_);
   }

   private static int dropFishingLoot(
      CommandContext<CommandSourceStack> p_137927_, Holder<LootTable> p_334748_, BlockPos p_137929_, ItemStack p_137930_, LootCommand.DropConsumer p_137931_
   ) throws CommandSyntaxException {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137927_.getSource();
      LootParams lootparams = new LootParams.Builder(commandsourcestack.getLevel())
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p_137929_))
         .withParameter(LootContextParams.TOOL, p_137930_)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, commandsourcestack.getEntity())
         .create(LootContextParamSets.FISHING);
      return drop(p_137927_, p_334748_, lootparams, p_137931_);
   }

   private static int drop(CommandContext<CommandSourceStack> p_287721_, Holder<LootTable> p_330660_, LootParams p_287728_, LootCommand.DropConsumer p_287770_) throws CommandSyntaxException {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_287721_.getSource();
      List<ItemStack> list = p_330660_.value().getRandomItems(p_287728_);
      return p_287770_.accept(p_287721_, list, p_137997_ -> callback(commandsourcestack, p_137997_));
   }

   @FunctionalInterface
   interface Callback {
      void accept(List<ItemStack> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface DropConsumer {
      int accept(CommandContext<CommandSourceStack> var1, List<ItemStack> var2, LootCommand.Callback var3) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface TailProvider {
      ArgumentBuilder<CommandSourceStack, ?> construct(ArgumentBuilder<CommandSourceStack, ?> var1, LootCommand.DropConsumer var2);
   }
}
