package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RaidCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_180469_, CommandBuildContext p_334392_) {
      p_180469_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                    "raid"
                                 )
                                 .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                              .then(
                                 Commands.literal("start")
                                    .then(
                                       Commands.argument("omenlvl", IntegerArgumentType.integer(0))
                                          .executes(
                                             p_180502_ -> start((CommandSourceStack)p_180502_.getSource(), IntegerArgumentType.getInteger(p_180502_, "omenlvl"))
                                          )
                                    )
                              ))
                           .then(Commands.literal("stop").executes(p_180500_ -> stop((CommandSourceStack)p_180500_.getSource()))))
                        .then(Commands.literal("check").executes(p_180496_ -> check((CommandSourceStack)p_180496_.getSource()))))
                     .then(
                        Commands.literal("sound")
                           .then(
                              Commands.argument("type", ComponentArgument.textComponent(p_334392_))
                                 .executes(
                                    p_390080_ -> playSound((CommandSourceStack)p_390080_.getSource(), ComponentArgument.getResolvedComponent(p_390080_, "type"))
                                 )
                           )
                     ))
                  .then(Commands.literal("spawnleader").executes(p_180488_ -> spawnLeader((CommandSourceStack)p_180488_.getSource()))))
               .then(
                  Commands.literal("setomen")
                     .then(
                        Commands.argument("level", IntegerArgumentType.integer(0))
                           .executes(
                              p_326325_ -> setRaidOmenLevel((CommandSourceStack)p_326325_.getSource(), IntegerArgumentType.getInteger(p_326325_, "level"))
                           )
                     )
               ))
            .then(Commands.literal("glow").executes(p_180471_ -> glow((CommandSourceStack)p_180471_.getSource())))
      );
   }

   private static int glow(CommandSourceStack p_180473_) throws CommandSyntaxException {
      Raid raid = getRaid(p_180473_.getPlayerOrException());
      if (raid != null) {
         for (Raider raider : raid.getAllRaiders()) {
            raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
         }
      }

      return 1;
   }

   private static int setRaidOmenLevel(CommandSourceStack p_180475_, int p_180476_) throws CommandSyntaxException {
      Raid raid = getRaid(p_180475_.getPlayerOrException());
      if (raid != null) {
         int i = raid.getMaxRaidOmenLevel();
         if (p_180476_ > i) {
            p_180475_.sendFailure(Component.literal("Sorry, the max raid omen level you can set is " + i));
         } else {
            int j = raid.getRaidOmenLevel();
            raid.setRaidOmenLevel(p_180476_);
            p_180475_.sendSuccess(() -> Component.literal("Changed village's raid omen level from " + j + " to " + p_180476_), false);
         }
      } else {
         p_180475_.sendFailure(Component.literal("No raid found here"));
      }

      return 1;
   }

   private static int spawnLeader(CommandSourceStack p_180483_) {
      p_180483_.sendSuccess(() -> Component.literal("Spawned a raid captain"), false);
      Raider raider = EntityType.PILLAGER.create(p_180483_.getLevel(), EntitySpawnReason.COMMAND);
      if (raider == null) {
         p_180483_.sendFailure(Component.literal("Pillager failed to spawn"));
         return 0;
      } else {
         raider.setPatrolLeader(true);
         raider.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(p_180483_.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
         raider.setPos(p_180483_.getPosition().x, p_180483_.getPosition().y, p_180483_.getPosition().z);
         raider.finalizeSpawn(
            p_180483_.getLevel(), p_180483_.getLevel().getCurrentDifficultyAt(BlockPos.containing(p_180483_.getPosition())), EntitySpawnReason.COMMAND, null
         );
         p_180483_.getLevel().addFreshEntityWithPassengers(raider);
         return 1;
      }
   }

   private static int playSound(CommandSourceStack p_180478_, @Nullable Component p_180479_) {
      if (p_180479_ != null && p_180479_.getString().equals("local")) {
         ServerLevel serverlevel = p_180478_.getLevel();
         Vec3 vec3 = p_180478_.getPosition().add(5.0, 0.0, 0.0);
         serverlevel.playSeededSound(null, vec3.x, vec3.y, vec3.z, SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F, serverlevel.random.nextLong());
      }

      return 1;
   }

   private static int start(CommandSourceStack p_180485_, int p_180486_) throws CommandSyntaxException {
      ServerPlayer serverplayer = p_180485_.getPlayerOrException();
      BlockPos blockpos = serverplayer.blockPosition();
      if (serverplayer.level().isRaided(blockpos)) {
         p_180485_.sendFailure(Component.literal("Raid already started close by"));
         return -1;
      }

      Raids raids = serverplayer.level().getRaids();
      Raid raid = raids.createOrExtendRaid(serverplayer, serverplayer.blockPosition());
      if (raid != null) {
         raid.setRaidOmenLevel(p_180486_);
         raids.setDirty();
         p_180485_.sendSuccess(() -> Component.literal("Created a raid in your local village"), false);
      } else {
         p_180485_.sendFailure(Component.literal("Failed to create a raid in your local village"));
      }

      return 1;
   }

   private static int stop(CommandSourceStack p_180490_) throws CommandSyntaxException {
      ServerPlayer serverplayer = p_180490_.getPlayerOrException();
      BlockPos blockpos = serverplayer.blockPosition();
      Raid raid = serverplayer.level().getRaidAt(blockpos);
      if (raid != null) {
         raid.stop();
         p_180490_.sendSuccess(() -> Component.literal("Stopped raid"), false);
         return 1;
      } else {
         p_180490_.sendFailure(Component.literal("No raid here"));
         return -1;
      }
   }

   private static int check(CommandSourceStack p_180494_) throws CommandSyntaxException {
      Raid raid = getRaid(p_180494_.getPlayerOrException());
      if (raid != null) {
         StringBuilder stringbuilder = new StringBuilder();
         stringbuilder.append("Found a started raid! ");
         p_180494_.sendSuccess(() -> Component.literal(stringbuilder.toString()), false);
         StringBuilder stringbuilder1 = new StringBuilder();
         stringbuilder1.append("Num groups spawned: ");
         stringbuilder1.append(raid.getGroupsSpawned());
         stringbuilder1.append(" Raid omen level: ");
         stringbuilder1.append(raid.getRaidOmenLevel());
         stringbuilder1.append(" Num mobs: ");
         stringbuilder1.append(raid.getTotalRaidersAlive());
         stringbuilder1.append(" Raid health: ");
         stringbuilder1.append(raid.getHealthOfLivingRaiders());
         stringbuilder1.append(" / ");
         stringbuilder1.append(raid.getTotalHealth());
         p_180494_.sendSuccess(() -> Component.literal(stringbuilder1.toString()), false);
         return 1;
      } else {
         p_180494_.sendFailure(Component.literal("Found no started raids"));
         return 0;
      }
   }

   private static @Nullable Raid getRaid(ServerPlayer p_180467_) {
      return p_180467_.level().getRaidAt(p_180467_.blockPosition());
   }
}
