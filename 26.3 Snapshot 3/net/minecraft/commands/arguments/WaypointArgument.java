package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointArgument {
   public static final SimpleCommandExceptionType ERROR_NOT_A_WAYPOINT = new SimpleCommandExceptionType(Component.translatable("argument.waypoint.invalid"));

   public static WaypointTransmitter getWaypoint(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      if (((EntitySelector)context.getArgument(name, EntitySelector.class)).findSingleEntity((CommandSourceStack)context.getSource()) instanceof WaypointTransmitter waypointTransmitter
         )
       {
         return waypointTransmitter;
      } else {
         throw ERROR_NOT_A_WAYPOINT.create();
      }
   }
}
