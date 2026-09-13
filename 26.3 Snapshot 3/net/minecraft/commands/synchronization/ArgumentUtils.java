package net.minecraft.commands.synchronization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import org.slf4j.Logger;

public class ArgumentUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte NUMBER_FLAG_MIN = 1;
   private static final byte NUMBER_FLAG_MAX = 2;

   public static int createNumberFlags(final boolean hasMin, final boolean hasMax) {
      int result = 0;
      if (hasMin) {
         result |= 1;
      }

      if (hasMax) {
         result |= 2;
      }

      return result;
   }

   public static boolean numberHasMin(final byte flags) {
      return (flags & 1) != 0;
   }

   public static boolean numberHasMax(final byte flags) {
      return (flags & 2) != 0;
   }

   private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeArgumentCap(
      final JsonObject result, final ArgumentTypeInfo<A, T> info, final ArgumentTypeInfo.Template<A> argumentType
   ) {
      info.serializeToJson((T)argumentType, result);
   }

   private static <T extends ArgumentType<?>> void serializeArgumentToJson(final JsonObject result, final T argument) {
      ArgumentTypeInfo.Template<T> template = ArgumentTypeInfos.unpack(argument);
      result.addProperty("type", "argument");
      result.addProperty("parser", String.valueOf(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(template.type())));
      JsonObject type = new JsonObject();
      serializeArgumentCap(type, template.type(), template);
      if (!type.isEmpty()) {
         result.add("properties", type);
      }
   }

   public static <S> JsonObject serializeNodeToJson(final CommandDispatcher<S> dispatcher, final CommandNode<S> node) {
      JsonObject result = new JsonObject();
      switch (node) {
         case RootCommandNode<S> rootNode:
            result.addProperty("type", "root");
            break;
         case LiteralCommandNode<S> literalNode:
            result.addProperty("type", "literal");
            break;
         case ArgumentCommandNode<S, ?> argumentNode:
            serializeArgumentToJson(result, argumentNode.getType());
            break;
         default:
            LOGGER.error("Could not serialize node {} ({})!", node, node.getClass());
            result.addProperty("type", "unknown");
      }

      Collection<CommandNode<S>> children = node.getChildren();
      if (!children.isEmpty()) {
         JsonObject childrenObject = new JsonObject();

         for (CommandNode<S> child : children) {
            childrenObject.add(child.getName(), serializeNodeToJson(dispatcher, child));
         }

         result.add("children", childrenObject);
      }

      if (node.getCommand() != null) {
         result.addProperty("executable", true);
      }

      if (node.getRequirement() instanceof PermissionProviderCheck<?> permissionCheck) {
         JsonElement permissions = (JsonElement)PermissionCheck.CODEC
            .encodeStart(JsonOps.INSTANCE, permissionCheck.test())
            .getOrThrow(error -> new IllegalStateException("Failed to serialize requirement: " + error));
         result.add("permissions", permissions);
      }

      if (node.getRedirect() != null) {
         Collection<String> path = dispatcher.getPath(node.getRedirect());
         if (!path.isEmpty()) {
            JsonArray target = new JsonArray();

            for (String piece : path) {
               target.add(piece);
            }

            result.add("redirect", target);
         }
      }

      return result;
   }

   public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(final CommandNode<T> node) {
      Set<CommandNode<T>> visitedNodes = new ReferenceOpenHashSet();
      Set<ArgumentType<?>> result = new HashSet<>();
      findUsedArgumentTypes(node, result, visitedNodes);
      return result;
   }

   private static <T> void findUsedArgumentTypes(final CommandNode<T> node, final Set<ArgumentType<?>> output, final Set<CommandNode<T>> visitedNodes) {
      if (visitedNodes.add(node)) {
         if (node instanceof ArgumentCommandNode<T, ?> arg) {
            output.add(arg.getType());
         }

         node.getChildren().forEach(child -> findUsedArgumentTypes(child, output, visitedNodes));
         CommandNode<T> redirect = node.getRedirect();
         if (redirect != null) {
            findUsedArgumentTypes(redirect, output, visitedNodes);
         }
      }
   }
}
