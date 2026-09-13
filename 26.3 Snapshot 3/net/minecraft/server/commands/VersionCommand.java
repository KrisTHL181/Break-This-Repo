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

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final boolean checkPermissions) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("version")
               .requires(Commands.hasPermission(checkPermissions ? Commands.LEVEL_GAMEMASTERS : Commands.LEVEL_ALL)))
            .executes(c -> {
               CommandSourceStack source = (CommandSourceStack)c.getSource();
               source.sendSystemMessage(HEADER);
               dumpVersion(source::sendSystemMessage);
               return 1;
            })
      );
   }

   public static void dumpVersion(final Consumer<Component> output) {
      WorldVersion version = SharedConstants.getCurrentVersion();
      output.accept(Component.translatable("commands.version.id", version.id()));
      output.accept(Component.translatable("commands.version.name", version.name()));
      output.accept(Component.translatable("commands.version.data", version.dataVersion().version()));
      output.accept(Component.translatable("commands.version.series", version.dataVersion().series()));
      output.accept(Component.translatable("commands.version.protocol", version.protocolVersion(), "0x" + Integer.toHexString(version.protocolVersion())));
      output.accept(Component.translatable("commands.version.build_time", Component.translationArg(version.buildTime())));
      output.accept(Component.translatable("commands.version.pack.resource", version.packVersion(PackType.CLIENT_RESOURCES).toString()));
      output.accept(Component.translatable("commands.version.pack.data", version.packVersion(PackType.SERVER_DATA).toString()));
      output.accept(version.stable() ? STABLE : UNSTABLE);
   }
}
