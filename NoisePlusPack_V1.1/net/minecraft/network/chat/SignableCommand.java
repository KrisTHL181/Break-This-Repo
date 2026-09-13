package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.SignedArgument;
import org.jspecify.annotations.Nullable;

public record SignableCommand<S>(List<SignableCommand.Argument<S>> arguments) {
   public static <S> boolean hasSignableArguments(ParseResults<S> p_328269_) {
      return !of(p_328269_).arguments().isEmpty();
   }

   public static <S> SignableCommand<S> of(ParseResults<S> p_250316_) {
      String s = p_250316_.getReader().getString();
      CommandContextBuilder<S> commandcontextbuilder = p_250316_.getContext();
      CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder;
      List<SignableCommand.Argument<S>> list = collectArguments(s, commandcontextbuilder);

      CommandContextBuilder<S> commandcontextbuilder2;
      while (
         (commandcontextbuilder2 = commandcontextbuilder1.getChild()) != null && commandcontextbuilder2.getRootNode() != commandcontextbuilder.getRootNode()
      ) {
         list.addAll(collectArguments(s, commandcontextbuilder2));
         commandcontextbuilder1 = commandcontextbuilder2;
      }

      return new SignableCommand<>(list);
   }

   private static <S> List<SignableCommand.Argument<S>> collectArguments(String p_252055_, CommandContextBuilder<S> p_251770_) {
      List<SignableCommand.Argument<S>> list = new ArrayList<>();

      for (ParsedCommandNode<S> parsedcommandnode : p_251770_.getNodes()) {
         if (parsedcommandnode.getNode() instanceof ArgumentCommandNode<S, ?> argumentcommandnode && argumentcommandnode.getType() instanceof SignedArgument) {
            ParsedArgument<S, ?> parsedargument = (ParsedArgument<S, ?>)p_251770_.getArguments().get(argumentcommandnode.getName());
            if (parsedargument != null) {
               String s = parsedargument.getRange().get(p_252055_);
               list.add(new SignableCommand.Argument<>(argumentcommandnode, s));
            }
         }
      }

      return list;
   }

   public SignableCommand.@Nullable Argument<S> getArgument(String p_336083_) {
      for (SignableCommand.Argument<S> argument : this.arguments) {
         if (p_336083_.equals(argument.name())) {
            return argument;
         }
      }

      return null;
   }

   public record Argument<S>(ArgumentCommandNode<S, ?> node, String value) {
      public String name() {
         return this.node.getName();
      }
   }
}
