package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("enchant")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                .executes(
                                    c -> enchant(c.getSource(), EntityArgument.getEntities(c, "targets"), ResourceArgument.getEnchantment(c, "enchantment"), 1)
                                )
                                .then(
                                    Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(
                                            c -> enchant(
                                                c.getSource(),
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

        int success = 0;

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity target) {
                ItemStack item = target.getMainHandItem();
                if (!item.isEmpty()) {
                    if (enchantment.canEnchant(item)
                        && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(item).keySet(), enchantmentHolder)) {
                        item.enchant(enchantmentHolder, level);
                        success++;
                    } else if (targets.size() == 1) {
                        throw ERROR_INCOMPATIBLE.create(item.getHoverName().getString());
                    }
                } else if (targets.size() == 1) {
                    throw ERROR_NO_ITEM.create(target.getName().getString());
                }
            } else if (targets.size() == 1) {
                throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
            }
        }

        if (success == 0) {
            throw ERROR_NOTHING_HAPPENED.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.enchant.success.single", Enchantment.getFullname(enchantmentHolder, level), targets.iterator().next().getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.enchant.success.multiple", Enchantment.getFullname(enchantmentHolder, level), targets.size()), true
            );
        }

        return success;
    }
}