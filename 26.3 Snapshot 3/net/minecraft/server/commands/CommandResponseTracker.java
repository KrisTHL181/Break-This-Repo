package net.minecraft.server.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class CommandResponseTracker<Element> {
   private int totalValue;
   private @Nullable Element onlyElement;
   private int elementCount;
   private @Nullable Element onlyNonZeroElement;
   private int nonZeroElementCount;

   public static <T> CommandResponseTracker<T> create() {
      return new CommandResponseTracker();
   }

   public void track(final Element element, final int value) {
      this.totalValue += value;
      if (++this.elementCount == 1) {
         this.onlyElement = element;
      } else {
         this.onlyElement = null;
      }

      if (value != 0) {
         if (++this.nonZeroElementCount == 1) {
            this.onlyNonZeroElement = element;
         } else {
            this.onlyNonZeroElement = null;
         }
      }
   }

   public void track(final Element element, final boolean value) {
      this.track(element, value ? 1 : 0);
   }

   public void track(final Element element) {
      this.track(element, 1);
   }

   private @Nullable Element firstElement(final CommandResponseTracker.ElementType type) {
      return (Element)(switch (type) {
         case ANY -> this.onlyElement;
         case NON_ZERO -> this.onlyNonZeroElement;
      });
   }

   private int elementCount(final CommandResponseTracker.ElementType type) {
      return switch (type) {
         case ANY -> this.elementCount;
         case NON_ZERO -> this.nonZeroElementCount;
      };
   }

   public int totalValue() {
      return this.totalValue;
   }

   public static <Element> CommandResponseTracker.Messages<Element> messages(
      final CommandResponseTracker.SingleHandler<Component, Element> onSingle, final CommandResponseTracker.MultipleHandler<Component> onMultiple
   ) {
      return new CommandResponseTracker.Messages<>(null, new CommandResponseTracker.Dispatch<>(onSingle, onMultiple));
   }

   public static <Element> CommandResponseTracker.Messages<Element> messages(
      final CommandResponseTracker.ErrorHandler onZero,
      final CommandResponseTracker.SingleHandler<Component, Element> onSingle,
      final CommandResponseTracker.MultipleHandler<Component> onMultiple
   ) {
      return new CommandResponseTracker.Messages<>(onZero, new CommandResponseTracker.Dispatch<>(onSingle, onMultiple));
   }

   public static <Element> CommandResponseTracker.Messages<Element> messages(
      final SimpleCommandExceptionType onZero,
      final CommandResponseTracker.SingleHandler<Component, Element> onSingle,
      final CommandResponseTracker.MultipleHandler<Component> onMultiple
   ) {
      return new CommandResponseTracker.Messages<>(onZero::create, new CommandResponseTracker.Dispatch<>(onSingle, onMultiple));
   }

   public <Result> Result dispatch(
      final CommandResponseTracker.ElementType elementType, final CommandResponseTracker.Dispatch<Result, ? super Element> dispatch
   ) {
      Element firstElement = this.firstElement(elementType);
      return firstElement != null
         ? dispatch.onSingle().create(firstElement, this.totalValue)
         : dispatch.onMultiple().create(this.elementCount(elementType), this.totalValue);
   }

   public int sendFeedback(
      final CommandSourceStack sourceStack,
      final boolean broadcast,
      final CommandResponseTracker.ElementType elementType,
      final CommandResponseTracker.Messages<? super Element> messages
   ) throws CommandSyntaxException {
      messages.throwIfZero(this.elementCount(elementType));
      sourceStack.sendSuccess(() -> this.dispatch(elementType, messages.onSuccess()), broadcast);
      return this.totalValue;
   }

   public int sendFeedback(final CommandSourceStack sourceStack, final boolean broadcast, final CommandResponseTracker.Messages<? super Element> messages) throws CommandSyntaxException {
      return this.sendFeedback(sourceStack, broadcast, CommandResponseTracker.ElementType.NON_ZERO, messages);
   }

   public static <Element, Arg> CommandResponseTracker.MessagesWithArg<Element, Arg> messages(
      final CommandResponseTracker.SingleHandlerWithArg<Component, Element, Arg> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArg<Component, Arg> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArg<>(null, new CommandResponseTracker.DispatchWithArg<>(onSingle, onMultiple));
   }

   public static <Element, Arg> CommandResponseTracker.MessagesWithArg<Element, Arg> messages(
      final CommandResponseTracker.ErrorHandlerWithArg<Arg> onZero,
      final CommandResponseTracker.SingleHandlerWithArg<Component, Element, Arg> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArg<Component, Arg> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArg<>(onZero, new CommandResponseTracker.DispatchWithArg<>(onSingle, onMultiple));
   }

   public static <Element, Arg> CommandResponseTracker.MessagesWithArg<Element, Arg> messages(
      final SimpleCommandExceptionType onZero,
      final CommandResponseTracker.SingleHandlerWithArg<Component, Element, Arg> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArg<Component, Arg> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArg<>(var1 -> onZero.create(), new CommandResponseTracker.DispatchWithArg<>(onSingle, onMultiple));
   }

   public <Result, Arg> Result dispatch(
      final CommandResponseTracker.ElementType elementType,
      final CommandResponseTracker.DispatchWithArg<Result, ? super Element, Arg> dispatch,
      final Arg argument
   ) {
      Element firstElement = this.firstElement(elementType);
      return firstElement != null
         ? dispatch.onSingle().create(firstElement, this.totalValue, argument)
         : dispatch.onMultiple().create(this.elementCount(elementType), this.totalValue, argument);
   }

   public <Arg> int sendFeedback(
      final CommandSourceStack sourceStack,
      final boolean broadcast,
      final CommandResponseTracker.ElementType elementType,
      final CommandResponseTracker.MessagesWithArg<? super Element, Arg> messages,
      final Arg argument
   ) throws CommandSyntaxException {
      messages.throwIfZero(this.elementCount(elementType), argument);
      sourceStack.sendSuccess(() -> this.dispatch(elementType, messages.onSuccess(), argument), broadcast);
      return this.totalValue;
   }

   public <Arg> int sendFeedback(
      final CommandSourceStack sourceStack,
      final boolean broadcast,
      final CommandResponseTracker.MessagesWithArg<? super Element, Arg> messages,
      final Arg argument
   ) throws CommandSyntaxException {
      return this.sendFeedback(sourceStack, broadcast, CommandResponseTracker.ElementType.NON_ZERO, messages, argument);
   }

   public static <Element, Arg0, Arg1> CommandResponseTracker.MessagesWithArgs<Element, Arg0, Arg1> messages(
      final CommandResponseTracker.SingleHandlerWithArgs<Component, Element, Arg0, Arg1> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArgs<Component, Arg0, Arg1> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArgs<>(null, new CommandResponseTracker.DispatchWithArgs<>(onSingle, onMultiple));
   }

   public static <Element, Arg0, Arg1> CommandResponseTracker.MessagesWithArgs<Element, Arg0, Arg1> messages(
      final CommandResponseTracker.ErrorHandlerWithArgs<Arg0, Arg1> onZero,
      final CommandResponseTracker.SingleHandlerWithArgs<Component, Element, Arg0, Arg1> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArgs<Component, Arg0, Arg1> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArgs<>(onZero, new CommandResponseTracker.DispatchWithArgs<>(onSingle, onMultiple));
   }

   public static <Element, Arg0, Arg1> CommandResponseTracker.MessagesWithArgs<Element, Arg0, Arg1> messages(
      final SimpleCommandExceptionType onZero,
      final CommandResponseTracker.SingleHandlerWithArgs<Component, Element, Arg0, Arg1> onSingle,
      final CommandResponseTracker.MultipleHandlerWithArgs<Component, Arg0, Arg1> onMultiple
   ) {
      return new CommandResponseTracker.MessagesWithArgs<>((var1, var2) -> onZero.create(), new CommandResponseTracker.DispatchWithArgs<>(onSingle, onMultiple));
   }

   public <Result, Arg0, Arg1> Result dispatch(
      final CommandResponseTracker.ElementType elementType,
      final CommandResponseTracker.DispatchWithArgs<Result, ? super Element, Arg0, Arg1> messages,
      final Arg0 argument0,
      final Arg1 argument1
   ) {
      Element firstElement = this.firstElement(elementType);
      return firstElement != null
         ? messages.onSingle().create(firstElement, this.totalValue, argument0, argument1)
         : messages.onMultiple().create(this.elementCount(elementType), this.totalValue, argument0, argument1);
   }

   public <Arg0, Arg1> int sendFeedback(
      final CommandSourceStack sourceStack,
      final boolean broadcast,
      final CommandResponseTracker.ElementType elementType,
      final CommandResponseTracker.MessagesWithArgs<? super Element, Arg0, Arg1> messages,
      final Arg0 argument0,
      final Arg1 argument1
   ) throws CommandSyntaxException {
      messages.throwIfZero(this.elementCount(elementType), argument0, argument1);
      sourceStack.sendSuccess(() -> this.dispatch(elementType, messages.onSuccess(), argument0, argument1), broadcast);
      return this.totalValue;
   }

   public <Arg0, Arg1> int sendFeedback(
      final CommandSourceStack sourceStack,
      final boolean broadcast,
      final CommandResponseTracker.MessagesWithArgs<? super Element, Arg0, Arg1> messages,
      final Arg0 argument0,
      final Arg1 argument1
   ) throws CommandSyntaxException {
      return this.sendFeedback(sourceStack, broadcast, CommandResponseTracker.ElementType.NON_ZERO, messages, argument0, argument1);
   }

   public record Dispatch<Result, Element>(
      CommandResponseTracker.SingleHandler<Result, Element> onSingle, CommandResponseTracker.MultipleHandler<Result> onMultiple
   ) {
   }

   public record DispatchWithArg<Result, Element, Arg>(
      CommandResponseTracker.SingleHandlerWithArg<Result, Element, Arg> onSingle, CommandResponseTracker.MultipleHandlerWithArg<Result, Arg> onMultiple
   ) {
   }

   public record DispatchWithArgs<Result, Element, Arg0, Arg1>(
      CommandResponseTracker.SingleHandlerWithArgs<Result, Element, Arg0, Arg1> onSingle,
      CommandResponseTracker.MultipleHandlerWithArgs<Result, Arg0, Arg1> onMultiple
   ) {
   }

   public enum ElementType {
      ANY,
      NON_ZERO;
   }

   @FunctionalInterface
   public interface ErrorHandler {
      CommandSyntaxException get();
   }

   @FunctionalInterface
   public interface ErrorHandlerWithArg<Arg> {
      CommandSyntaxException get(Arg argument);
   }

   @FunctionalInterface
   public interface ErrorHandlerWithArgs<Arg0, Arg1> {
      CommandSyntaxException get(Arg0 argument0, Arg1 argument1);
   }

   public record Messages<Element>(CommandResponseTracker.@Nullable ErrorHandler onZero, CommandResponseTracker.Dispatch<Component, Element> onSuccess) {
      public void throwIfZero(final int value) throws CommandSyntaxException {
         if (this.onZero != null && value == 0) {
            throw this.onZero.get();
         }
      }
   }

   public record MessagesWithArg<Element, Arg>(
      CommandResponseTracker.@Nullable ErrorHandlerWithArg<Arg> onZero, CommandResponseTracker.DispatchWithArg<Component, Element, Arg> onSuccess
   ) {
      public void throwIfZero(final int value, final Arg argument) throws CommandSyntaxException {
         if (this.onZero != null && value == 0) {
            throw this.onZero.get(argument);
         }
      }
   }

   public record MessagesWithArgs<Element, Arg0, Arg1>(
      CommandResponseTracker.@Nullable ErrorHandlerWithArgs<Arg0, Arg1> onZero,
      CommandResponseTracker.DispatchWithArgs<Component, Element, Arg0, Arg1> onSuccess
   ) {
      public void throwIfZero(final int value, final Arg0 argument0, final Arg1 argument1) throws CommandSyntaxException {
         if (this.onZero != null && value == 0) {
            throw this.onZero.get(argument0, argument1);
         }
      }
   }

   @FunctionalInterface
   public interface MultipleHandler<Result> {
      Result create(int elementCount, int totalValue);
   }

   @FunctionalInterface
   public interface MultipleHandlerWithArg<Result, Arg> {
      Result create(int elementCount, int totalValue, Arg argument);
   }

   @FunctionalInterface
   public interface MultipleHandlerWithArgs<Result, Arg0, Arg1> {
      Result create(int elementCount, int totalValue, Arg0 argument0, Arg1 argument1);
   }

   @FunctionalInterface
   public interface SingleHandler<Result, Element> {
      Result create(Element element, int totalValue);
   }

   @FunctionalInterface
   public interface SingleHandlerWithArg<Result, Element, Arg> {
      Result create(Element element, int totalValue, Arg argument);
   }

   @FunctionalInterface
   public interface SingleHandlerWithArgs<Result, Element, Arg0, Arg1> {
      Result create(Element element, int totalValue, Arg0 argument0, Arg1 argument1);
   }
}
