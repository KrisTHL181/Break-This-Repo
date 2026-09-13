package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLookingAtEntity implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("looking_at_entity");

   @Override
   public void display(DebugScreenDisplayer p_426755_, @Nullable Level p_430099_, @Nullable LevelChunk p_425918_, @Nullable LevelChunk p_431370_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.crosshairPickEntity;
      List<String> list = new ArrayList<>();
      if (entity != null) {
         list.add(ChatFormatting.UNDERLINE + "Targeted Entity");
         list.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
      }

      p_426755_.addToGroup(GROUP, list);
   }
}
