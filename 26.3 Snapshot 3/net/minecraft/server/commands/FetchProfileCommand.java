package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ResolvableProfile;

public class FetchProfileCommand {
   private static final DynamicCommandExceptionType NO_PROFILE = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.fetchprofile.no_profile", id)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fetchprofile")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.literal("name")
                        .then(
                           Commands.argument("name", StringArgumentType.greedyString())
                              .executes(c -> resolveName((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "name")))
                        )
                  ))
               .then(
                  Commands.literal("id")
                     .then(
                        Commands.argument("id", UuidArgument.uuid()).executes(c -> resolveId((CommandSourceStack)c.getSource(), UuidArgument.getUuid(c, "id")))
                     )
               ))
            .then(
               Commands.literal("entity")
                  .then(
                     Commands.argument("entity", EntityArgument.entity())
                        .executes(c -> printForEntity((CommandSourceStack)c.getSource(), EntityArgument.getEntity(c, "entity")))
                  )
            )
      );
   }

   private static void reportResolvedProfile(final CommandSourceStack sender, final GameProfile gameProfile, final String messageId, final Component argument) {
      reportResolvedProfile(sender, messageId, argument, ResolvableProfile.createResolved(gameProfile));
   }

   private static void reportResolvedProfile(final CommandSourceStack sender, final String messageId, final Component argument, final ResolvableProfile profile) {
      ResolvableProfile.CODEC
         .encodeStart(NbtOps.INSTANCE, profile)
         .ifSuccess(
            encodedProfile -> {
               String encodedProfileAsString = encodedProfile.toString();
               MutableComponent headComponent = Component.object(new PlayerSprite(profile, true));
               ComponentSerialization.CODEC
                  .encodeStart(NbtOps.INSTANCE, headComponent)
                  .ifSuccess(
                     encodedComponent -> {
                        String encodedComponentAsString = encodedComponent.toString();
                        sender.sendSuccess(
                           () -> {
                              Component clickable = ComponentUtils.formatList(
                                 List.of(
                                    Component.translatable("commands.fetchprofile.copy_component")
                                       .withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(encodedProfileAsString))),
                                    Component.translatable("commands.fetchprofile.give_item")
                                       .withStyle(
                                          s -> s.withClickEvent(
                                             new ClickEvent.RunCommand("give @s minecraft:player_head[profile=" + encodedProfileAsString + "]")
                                          )
                                       ),
                                    Component.translatable("commands.fetchprofile.summon_mannequin")
                                       .withStyle(
                                          s -> s.withClickEvent(
                                             new ClickEvent.RunCommand("summon minecraft:mannequin ~ ~ ~ {profile:" + encodedProfileAsString + "}")
                                          )
                                       ),
                                    Component.translatable("commands.fetchprofile.copy_text", headComponent.withStyle(ChatFormatting.WHITE))
                                       .withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(encodedComponentAsString)))
                                 ),
                                 CommonComponents.SPACE,
                                 c -> ComponentUtils.wrapInSquareBrackets(c.withStyle(ChatFormatting.GREEN))
                              );
                              return Component.translatable(messageId, argument, clickable);
                           },
                           false
                        );
                     }
                  )
                  .ifError(
                     componentEncodingError -> sender.sendFailure(
                        Component.translatable("commands.fetchprofile.failed_to_serialize", componentEncodingError.message())
                     )
                  );
            }
         )
         .ifError(error -> sender.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", error.message())));
   }

   private static int resolveName(final CommandSourceStack source, final String name) {
      MinecraftServer server = source.getServer();
      ProfileResolver resolver = server.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component nameComponent = Component.literal(name);
               Optional<GameProfile> result = resolver.fetchByName(name);
               server.execute(
                  () -> result.ifPresentOrElse(
                     profile -> reportResolvedProfile(source, profile, "commands.fetchprofile.name.success", nameComponent),
                     () -> source.sendFailure(Component.translatable("commands.fetchprofile.name.failure", nameComponent))
                  )
               );
            }
         );
      return 1;
   }

   private static int resolveId(final CommandSourceStack source, final UUID id) {
      MinecraftServer server = source.getServer();
      ProfileResolver resolver = server.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component idComponent = Component.translationArg(id);
               Optional<GameProfile> result = resolver.fetchById(id);
               server.execute(
                  () -> result.ifPresentOrElse(
                     profile -> reportResolvedProfile(source, profile, "commands.fetchprofile.id.success", idComponent),
                     () -> source.sendFailure(Component.translatable("commands.fetchprofile.id.failure", idComponent))
                  )
               );
            }
         );
      return 1;
   }

   private static int printForEntity(final CommandSourceStack source, final Entity entity) throws CommandSyntaxException {
      if (entity instanceof Avatar avatar) {
         printForAvatar(source, avatar);
         return 1;
      } else {
         throw NO_PROFILE.create(entity.getDisplayName());
      }
   }

   public static void printForAvatar(final CommandSourceStack source, final Avatar avatar) {
      reportResolvedProfile(source, "commands.fetchprofile.entity.success", avatar.getDisplayName(), avatar.getProfile());
   }
}
