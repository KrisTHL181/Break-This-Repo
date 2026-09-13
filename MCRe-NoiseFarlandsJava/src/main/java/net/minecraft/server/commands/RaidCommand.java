package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RaidCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("raid")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.literal("start")
                        .then(
                            Commands.argument("omenlvl", IntegerArgumentType.integer(0))
                                .executes(c -> start(c.getSource(), IntegerArgumentType.getInteger(c, "omenlvl")))
                        )
                )
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("check").executes(c -> check(c.getSource())))
                .then(
                    Commands.literal("sound")
                        .then(
                            Commands.argument("type", ComponentArgument.textComponent(context))
                                .executes(c -> playSound(c.getSource(), ComponentArgument.getResolvedComponent(c, "type")))
                        )
                )
                .then(Commands.literal("spawnleader").executes(c -> spawnLeader(c.getSource())))
                .then(
                    Commands.literal("setomen")
                        .then(
                            Commands.argument("level", IntegerArgumentType.integer(0))
                                .executes(c -> setRaidOmenLevel(c.getSource(), IntegerArgumentType.getInteger(c, "level")))
                        )
                )
                .then(Commands.literal("glow").executes(c -> glow(c.getSource())))
        );
    }

    private static int glow(final CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            for (Raider raider : raid.getAllRaiders()) {
                raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
            }
        }

        return 1;
    }

    private static int setRaidOmenLevel(final CommandSourceStack source, final int level) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            int max = raid.getMaxRaidOmenLevel();
            if (level > max) {
                source.sendFailure(Component.literal("Sorry, the max raid omen level you can set is " + max));
            } else {
                int before = raid.getRaidOmenLevel();
                raid.setRaidOmenLevel(level);
                source.sendSuccess(() -> Component.literal("Changed village's raid omen level from " + before + " to " + level), false);
            }
        } else {
            source.sendFailure(Component.literal("No raid found here"));
        }

        return 1;
    }

    private static int spawnLeader(final CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Spawned a raid captain"), false);
        Raider raider = EntityTypes.PILLAGER.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (raider == null) {
            source.sendFailure(Component.literal("Pillager failed to spawn"));
            return 0;
        } else {
            raider.setPatrolLeader(true);
            raider.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(source.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
            raider.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
            raider.finalizeSpawn(
                source.getLevel(), source.getLevel().getCurrentDifficultyAt(BlockPos.containing(source.getPosition())), EntitySpawnReason.COMMAND, null
            );
            source.getLevel().addFreshEntityWithPassengers(raider);
            return 1;
        }
    }

    private static int playSound(final CommandSourceStack source, final @Nullable Component type) {
        if (type != null && type.getString().equals("local")) {
            ServerLevel level = source.getLevel();
            Vec3 pos = source.getPosition().add(5.0, 0.0, 0.0);
            level.playSeededSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F, level.getRandom().nextLong());
        }

        return 1;
    }

    private static int start(final CommandSourceStack source, final int raidOmenLevel) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = player.blockPosition();
        if (player.level().isRaided(pos)) {
            source.sendFailure(Component.literal("Raid already started close by"));
            return -1;
        }

        Raids raids = player.level().getRaids();
        Raid raid = raids.createOrExtendRaid(player, player.blockPosition());
        if (raid != null) {
            raid.setRaidOmenLevel(raidOmenLevel);
            raids.setDirty();
            source.sendSuccess(() -> Component.literal("Created a raid in your local village"), false);
        } else {
            source.sendFailure(Component.literal("Failed to create a raid in your local village"));
        }

        return 1;
    }

    private static int stop(final CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = player.blockPosition();
        Raid raid = player.level().getRaidAt(pos);
        if (raid != null) {
            raid.stop();
            source.sendSuccess(() -> Component.literal("Stopped raid"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("No raid here"));
            return -1;
        }
    }

    private static int check(final CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            StringBuilder status = new StringBuilder();
            status.append("Found a started raid! ");
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            StringBuilder status2 = new StringBuilder();
            status2.append("Num groups spawned: ");
            status2.append(raid.getGroupsSpawned());
            status2.append(" Raid omen level: ");
            status2.append(raid.getRaidOmenLevel());
            status2.append(" Num mobs: ");
            status2.append(raid.getTotalRaidersAlive());
            status2.append(" Raid health: ");
            status2.append(raid.getHealthOfLivingRaiders());
            status2.append(" / ");
            status2.append(raid.getTotalHealth());
            source.sendSuccess(() -> Component.literal(status2.toString()), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Found no started raids"));
            return 0;
        }
    }

    private static @Nullable Raid getRaid(final ServerPlayer player) {
        return player.level().getRaidAt(player.blockPosition());
    }
}