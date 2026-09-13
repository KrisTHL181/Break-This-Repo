package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import org.jspecify.annotations.Nullable;

public class CelebrateVillagersSurvivedRaid extends Behavior<Villager> {
   private @Nullable Raid currentRaid;

   public CelebrateVillagersSurvivedRaid(final int minDuration, final int maxDuration) {
      super(ImmutableMap.of(), minDuration, maxDuration);
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Villager body) {
      BlockPos testPos = body.blockPosition();
      this.currentRaid = level.getRaidAt(testPos);
      return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(level, body, testPos);
   }

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return this.currentRaid != null && !this.currentRaid.isStopped();
   }

   protected void stop(final ServerLevel level, final Villager body, final long timestamp) {
      this.currentRaid = null;
      body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
   }

   protected void tick(final ServerLevel level, final Villager body, final long timestamp) {
      RandomSource random = body.getRandom();
      if (random.nextInt(100) == 0) {
         body.playCelebrateSound();
      }

      if (random.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(level, body, body.blockPosition())) {
         DyeColor color = Util.getRandom(DyeColor.values(), random);
         int flightDuration = random.nextInt(3);
         ItemStack firework = this.getFirework(color, flightDuration);
         Projectile.spawnProjectile(new FireworkRocketEntity(body.level(), body, body.getX(), body.getEyeY(), body.getZ(), firework), level, firework);
      }
   }

   private ItemStack getFirework(final DyeColor color, final int flightDuration) {
      ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
      rocket.set(
         DataComponents.FIREWORKS,
         new Fireworks(
            (byte)flightDuration,
            List.of(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(color.getFireworkColor()), IntList.of(), false, false))
         )
      );
      return rocket;
   }
}
