package net.minecraft.world.level.dimension.end;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;

public enum DragonRespawnStage implements StringRepresentable {
   START("start") {
      @Override
      public void tick(final ServerLevel level, final EnderDragonFight fight, final List<EndCrystal> crystals, final int time) {
         BlockPos beamPos = new BlockPos(0, 128, 0);

         for (EndCrystal respawnCrystal : crystals) {
            respawnCrystal.setBeamTarget(beamPos);
         }

         fight.setRespawnStage(PREPARING_TO_SUMMON_PILLARS);
      }
   },
   PREPARING_TO_SUMMON_PILLARS("preparing_to_summon_pillars") {
      @Override
      public void tick(final ServerLevel level, final EnderDragonFight fight, final List<EndCrystal> crystals, final int time) {
         if (time < 100) {
            if (time == 0 || time == 50 || time == 51 || time == 52 || time >= 95) {
               level.levelEvent(3001, new BlockPos(0, 128, 0), 0);
            }
         } else {
            fight.setRespawnStage(SUMMONING_PILLARS);
         }
      }
   },
   SUMMONING_PILLARS("summoning_pillars") {
      @Override
      public void tick(final ServerLevel level, final EnderDragonFight fight, final List<EndCrystal> crystals, final int time) {
         int interval = 40;
         boolean startOfBeam = time % 40 == 0;
         boolean endOfBeam = time % 40 == 39;
         if (startOfBeam || endOfBeam) {
            List<EndSpikeFeature.EndSpike> spikes = EndSpikeFeature.getSpikesForLevel(level);
            int index = time / 40;
            if (index < spikes.size()) {
               EndSpikeFeature.EndSpike spike = spikes.get(index);
               if (startOfBeam) {
                  for (EndCrystal respawnCrystal : crystals) {
                     respawnCrystal.setBeamTarget(new BlockPos(spike.getCenterX(), spike.getHeight() + 1, spike.getCenterZ()));
                  }
               } else {
                  int radius = 10;

                  for (BlockPos pos : BlockPos.betweenClosed(
                     new BlockPos(spike.getCenterX() - 10, spike.getHeight() - 10, spike.getCenterZ() - 10),
                     new BlockPos(spike.getCenterX() + 10, spike.getHeight() + 10, spike.getCenterZ() + 10)
                  )) {
                     level.removeBlock(pos, false);
                  }

                  level.explode(null, spike.getCenterX() + 0.5F, spike.getHeight(), spike.getCenterZ() + 0.5F, 5.0F, Level.ExplosionInteraction.BLOCK);
                  EndSpikeFeature feature = new EndSpikeFeature(List.of(spike), true, Optional.of(new BlockPos(0, 128, 0)));
                  feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(), new BlockPos(spike.getCenterX(), 45, spike.getCenterZ()));
               }
            } else if (startOfBeam) {
               fight.setRespawnStage(SUMMONING_DRAGON);
            }
         }
      }
   },
   SUMMONING_DRAGON("summoning_dragon") {
      @Override
      public void tick(final ServerLevel level, final EnderDragonFight fight, final List<EndCrystal> crystals, final int time) {
         if (time >= 100) {
            fight.setRespawnStage(END);
            fight.resetSpikeCrystals();

            for (EndCrystal crystal : crystals) {
               crystal.setBeamTarget(null);
               level.explode(crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
               crystal.discard();
            }
         } else if (time >= 80) {
            level.levelEvent(3001, new BlockPos(0, 128, 0), 0);
         } else if (time == 0) {
            for (EndCrystal crystal : crystals) {
               crystal.setBeamTarget(new BlockPos(0, 128, 0));
            }
         } else if (time < 5) {
            level.levelEvent(3001, new BlockPos(0, 128, 0), 0);
         }
      }
   },
   END("end") {
      @Override
      public void tick(final ServerLevel level, final EnderDragonFight fight, final List<EndCrystal> crystals, final int time) {
      }
   };

   public static final Codec<DragonRespawnStage> CODEC = StringRepresentable.fromEnum(DragonRespawnStage::values);
   private final String name;

   DragonRespawnStage(final String name) {
      this.name = name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public abstract void tick(ServerLevel level, EnderDragonFight fight, List<EndCrystal> crystals, int time);
}
