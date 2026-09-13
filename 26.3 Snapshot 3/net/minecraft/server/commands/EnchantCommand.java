package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      target -> Component.translatableEscape("commands.enchant.failed.entity", target)
   );
   private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType(
      target -> Component.translatableEscape("commands.enchant.failed.itemless", target)
   );
   private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType(
      item -> Component.translatableEscape("commands.enchant.failed.incompatible", item)
   );
   private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType(
      (level, max) -> Component.translatableEscape("commands.enchant.failed.level", level, max)
   );
   private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(Component.translatable("commands.enchant.failed"));
   private static final CommandResponseTracker.MessagesWithArgs<Entity, Holder<Enchantment>, Integer> RESPONSE_ENCHANT = CommandResponseTracker.messages(
      ERROR_NOTHING_HAPPENED,
      (entity, var1, enchantment, level) -> Component.translatable(
         "commands.enchant.success.single", Enchantment.getFullname(enchantment, level), entity.getDisplayName()
      ),
      (entityCount, var1, enchantment, level) -> Component.translatable(
         "commands.enchant.success.multiple", Enchantment.getFullname(enchantment, level), entityCount
      )
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("enchant").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                           .executes(
                              c -> enchant(
                                 (CommandSourceStack)c.getSource(),
                                 EntityArgument.getEntities(c, "targets"),
                                 ResourceArgument.getEnchantment(c, "enchantment"),
                                 1
                              )
                           ))
                        .then(
                           Commands.argument("level", IntegerArgumentType.integer(0))
                              .executes(
                                 c -> enchant(
                                    (CommandSourceStack)c.getSource(),
                                    EntityArgument.getEntities(c, "targets"),
                                    ResourceArgument.getEnchantment(c, "enchantment"),
                                    IntegerArgumentType.getInteger(c, "level")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int enchant(
      final CommandSourceStack source, final Collection<? extends Entity> targets, final Holder<Enchantment> enchantmentHolder, final int level
   ) throws CommandSyntaxException {
      Enchantment enchantment = enchantmentHolder.value();
      if (level > enchantment.getMaxLevel()) {
         throw ERROR_LEVEL_TOO_HIGH.create(level, enchantment.getMaxLevel());
      }

      CommandResponseTracker<Entity> tracker = CommandResponseTracker.create();

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity target) {
            ItemStack item = target.getMainHandItem();
            if (!item.isEmpty()) {
               if (enchantment.canEnchant(item)
                  && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(item).keySet(), enchantmentHolder)) {
                  item.enchant(enchantmentHolder, level);
                  tracker.track(entity);
               } else if (targets.size() == 1) {
                  throw ERROR_INCOMPATIBLE.create(item.getHoverName().getString());
               }
            } else if (targets.size() == 1) {
               throw ERROR_NO_ITEM.create(target.getDisplayName().getString());
            }
         } else if (targets.size() == 1) {
            throw ERROR_NOT_LIVING_ENTITY.create(entity.getDisplayName().getString());
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_ENCHANT, enchantmentHolder, level);
   }
}
