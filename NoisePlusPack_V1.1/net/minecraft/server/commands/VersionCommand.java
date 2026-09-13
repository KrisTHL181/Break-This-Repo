package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;

public class VersionCommand {
   private static final Component HEADER = Component.translatable("commands.version.header");
   private static final Component STABLE = Component.translatable("commands.version.stable.yes");
   private static final Component UNSTABLE = Component.translatable("commands.version.stable.no");

   public static void register(CommandDispatcher<CommandSourceStack> p_407666_, boolean p_407986_) {
      p_407666_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("version")
               .requires(Commands.hasPermission(p_407986_ ? Commands.LEVEL_GAMEMASTERS : Commands.LEVEL_ALL)))
            .executes(p_406487_ -> {
               CommandSourceStack commandsourcestack = (CommandSourceStack)p_406487_.getSource();
               commandsourcestack.sendSystemMessage(HEADER);
               dumpVersion(commandsourcestack::sendSystemMessage);
               return 1;
            })
      );
   }

   public static void dumpVersion(Consumer<Component> p_406346_) {
      WorldVersion worldversion = SharedConstants.getCurrentVersion();
      p_406346_.accept(Component.translatable("commands.version.id", worldversion.id()));
      p_406346_.accept(Component.translatable("commands.version.name", worldversion.name()));
      p_406346_.accept(Component.translatable("commands.version.data", worldversion.dataVersion().version()));
      p_406346_.accept(Component.translatable("commands.version.series", worldversion.dataVersion().series()));
      p_406346_.accept(
         Component.translatable("commands.version.protocol", worldversion.protocolVersion(), "0x" + Integer.toHexString(worldversion.protocolVersion()))
      );
      p_406346_.accept(Component.translatable("commands.version.build_time", Component.translationArg(worldversion.buildTime())));
      p_406346_.accept(Component.translatable("commands.version.pack.resource", worldversion.packVersion(PackType.CLIENT_RESOURCES).toString()));
      p_406346_.accept(Component.translatable("commands.version.pack.data", worldversion.packVersion(PackType.SERVER_DATA).toString()));
      p_406346_.accept(worldversion.stable() ? STABLE : UNSTABLE);
   }
}
