package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLookingAtFluid implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("looking_at_fluid");

   @Override
   public void display(DebugScreenDisplayer p_423650_, @Nullable Level p_431251_, @Nullable LevelChunk p_429048_, @Nullable LevelChunk p_427954_) {
      Entity entity = Minecraft.getInstance().getCameraEntity();
      Level level = SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES ? p_431251_ : Minecraft.getInstance().level;
      if (entity != null && level != null) {
         HitResult hitresult = entity.pick(20.0, 0.0F, true);
         List<String> list = new ArrayList<>();
         if (hitresult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            FluidState fluidstate = level.getFluidState(blockpos);
            list.add(ChatFormatting.UNDERLINE + "Targeted Fluid: " + blockpos.getX() + ", " + blockpos.getY() + ", " + blockpos.getZ());
            list.add(String.valueOf(BuiltInRegistries.FLUID.getKey(fluidstate.getType())));

            for (Entry<Property<?>, Comparable<?>> entry : fluidstate.getValues().entrySet()) {
               list.add(this.getPropertyValueString(entry));
            }

            fluidstate.getTags().map(p_447979_ -> "#" + p_447979_.location()).forEach(list::add);
         }

         p_423650_.addToGroup(GROUP, list);
      }
   }

   private String getPropertyValueString(Entry<Property<?>, Comparable<?>> p_430751_) {
      Property<?> property = p_430751_.getKey();
      Comparable<?> comparable = p_430751_.getValue();
      String s = Util.getPropertyName(property, comparable);
      if (Boolean.TRUE.equals(comparable)) {
         s = ChatFormatting.GREEN + s;
      } else if (Boolean.FALSE.equals(comparable)) {
         s = ChatFormatting.RED + s;
      }

      return property.getName() + ": " + s;
   }
}
