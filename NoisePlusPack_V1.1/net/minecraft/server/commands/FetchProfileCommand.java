package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;

public class FetchProfileCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_429146_) {
      p_429146_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fetchprofile")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("name")
                     .then(
                        Commands.argument("name", StringArgumentType.greedyString())
                           .executes(p_425416_ -> resolveName((CommandSourceStack)p_425416_.getSource(), StringArgumentType.getString(p_425416_, "name")))
                     )
               ))
            .then(
               Commands.literal("id")
                  .then(
                     Commands.argument("id", UuidArgument.uuid())
                        .executes(p_429536_ -> resolveId((CommandSourceStack)p_429536_.getSource(), UuidArgument.getUuid(p_429536_, "id")))
                  )
            )
      );
   }

   private static void reportResolvedProfile(CommandSourceStack p_424685_, GameProfile p_427506_, String p_425213_, Component p_428902_) {
      ResolvableProfile resolvableprofile = ResolvableProfile.createResolved(p_427506_);
      ResolvableProfile.CODEC
         .encodeStart(NbtOps.INSTANCE, resolvableprofile)
         .ifSuccess(
            p_429334_ -> {
               String s = p_429334_.toString();
               MutableComponent mutablecomponent = Component.object(new PlayerSprite(resolvableprofile, true));
               ComponentSerialization.CODEC
                  .encodeStart(NbtOps.INSTANCE, mutablecomponent)
                  .ifSuccess(
                     p_423093_ -> {
                        String s1 = p_423093_.toString();
                        p_424685_.sendSuccess(
                           () -> {
                              Component component = ComponentUtils.formatList(
                                 List.of(
                                    Component.translatable("commands.fetchprofile.copy_component")
                                       .withStyle(p_424407_ -> p_424407_.withClickEvent(new ClickEvent.CopyToClipboard(s))),
                                    Component.translatable("commands.fetchprofile.give_item")
                                       .withStyle(
                                          p_429257_ -> p_429257_.withClickEvent(new ClickEvent.RunCommand("give @s minecraft:player_head[profile=" + s + "]"))
                                       ),
                                    Component.translatable("commands.fetchprofile.summon_mannequin")
                                       .withStyle(
                                          p_423865_ -> p_423865_.withClickEvent(
                                             new ClickEvent.RunCommand("summon minecraft:mannequin ~ ~ ~ {profile:" + s + "}")
                                          )
                                       ),
                                    Component.translatable("commands.fetchprofile.copy_text", mutablecomponent.withStyle(ChatFormatting.WHITE))
                                       .withStyle(p_429547_ -> p_429547_.withClickEvent(new ClickEvent.CopyToClipboard(s1)))
                                 ),
                                 CommonComponents.SPACE,
                                 p_423276_ -> ComponentUtils.wrapInSquareBrackets(p_423276_.withStyle(ChatFormatting.GREEN))
                              );
                              return Component.translatable(p_425213_, p_428902_, component);
                           },
                           false
                        );
                     }
                  )
                  .ifError(p_426412_ -> p_424685_.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", p_426412_.message())));
            }
         )
         .ifError(p_423123_ -> p_424685_.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", p_423123_.message())));
   }

   private static int resolveName(CommandSourceStack p_428534_, String p_424294_) {
      MinecraftServer minecraftserver = p_428534_.getServer();
      ProfileResolver profileresolver = minecraftserver.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component component = Component.literal(p_424294_);
               Optional<GameProfile> optional = profileresolver.fetchByName(p_424294_);
               minecraftserver.execute(
                  () -> optional.ifPresentOrElse(
                     p_424658_ -> reportResolvedProfile(p_428534_, p_424658_, "commands.fetchprofile.name.success", component),
                     () -> p_428534_.sendFailure(Component.translatable("commands.fetchprofile.name.failure", component))
                  )
               );
            }
         );
      return 1;
   }

   private static int resolveId(CommandSourceStack p_427177_, UUID p_429704_) {
      MinecraftServer minecraftserver = p_427177_.getServer();
      ProfileResolver profileresolver = minecraftserver.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component component = Component.translationArg(p_429704_);
               Optional<GameProfile> optional = profileresolver.fetchById(p_429704_);
               minecraftserver.execute(
                  () -> optional.ifPresentOrElse(
                     p_427325_ -> reportResolvedProfile(p_427177_, p_427325_, "commands.fetchprofile.id.success", component),
                     () -> p_427177_.sendFailure(Component.translatable("commands.fetchprofile.id.failure", component))
                  )
               );
            }
         );
      return 1;
   }
}
