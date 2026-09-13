package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLocalDifficulty implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_422323_, @Nullable Level p_426366_, @Nullable LevelChunk p_423491_, @Nullable LevelChunk p_431288_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && p_431288_ != null && p_426366_ instanceof ServerLevel serverlevel) {
         BlockPos $$8 = entity.blockPosition();
         if (serverlevel.isInsideBuildHeight($$8.getY())) {
            float f = serverlevel.getMoonBrightness($$8);
            long i = p_431288_.getInhabitedTime();
            DifficultyInstance difficultyinstance = new DifficultyInstance(serverlevel.getDifficulty(), serverlevel.getDayTime(), i, f);
            p_422323_.addLine(
               String.format(
                  Locale.ROOT,
                  "Local Difficulty: %.2f // %.2f (Day %d)",
                  difficultyinstance.getEffectiveDifficulty(),
                  difficultyinstance.getSpecialMultiplier(),
                  serverlevel.getDayCount()
               )
            );
         }
      }
   }
}
