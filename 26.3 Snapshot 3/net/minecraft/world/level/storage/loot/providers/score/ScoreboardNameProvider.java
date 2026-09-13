package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;

public interface ScoreboardNameProvider extends LootContextUser {
   @Nullable ScoreHolder getScoreHolder(LootContext context);

   MapCodec<? extends ScoreboardNameProvider> codec();
}
