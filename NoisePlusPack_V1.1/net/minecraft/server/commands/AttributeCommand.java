package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      p_308625_ -> Component.translatableEscape("commands.attribute.failed.entity", p_308625_)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
      (p_308616_, p_308617_) -> Component.translatableEscape("commands.attribute.failed.no_attribute", p_308616_, p_308617_)
   );
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
      (p_308629_, p_308630_, p_308631_) -> Component.translatableEscape("commands.attribute.failed.no_modifier", p_308630_, p_308629_, p_308631_)
   );
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
      (p_308626_, p_308627_, p_308628_) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", p_308628_, p_308627_, p_308626_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_251026_, CommandBuildContext p_250936_) {
      p_251026_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("attribute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                 "attribute", ResourceArgument.resource(p_250936_, Registries.ATTRIBUTE)
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("get")
                                       .executes(
                                          p_248109_ -> getAttributeValue(
                                             (CommandSourceStack)p_248109_.getSource(),
                                             EntityArgument.getEntity(p_248109_, "target"),
                                             ResourceArgument.getAttribute(p_248109_, "attribute"),
                                             1.0
                                          )
                                       ))
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .executes(
                                             p_248104_ -> getAttributeValue(
                                                (CommandSourceStack)p_248104_.getSource(),
                                                EntityArgument.getEntity(p_248104_, "target"),
                                                ResourceArgument.getAttribute(p_248104_, "attribute"),
                                                DoubleArgumentType.getDouble(p_248104_, "scale")
                                             )
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("base")
                                       .then(
                                          Commands.literal("set")
                                             .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg())
                                                   .executes(
                                                      p_248102_ -> setAttributeBase(
                                                         (CommandSourceStack)p_248102_.getSource(),
                                                         EntityArgument.getEntity(p_248102_, "target"),
                                                         ResourceArgument.getAttribute(p_248102_, "attribute"),
                                                         DoubleArgumentType.getDouble(p_248102_, "value")
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("get")
                                             .executes(
                                                p_248112_ -> getAttributeBase(
                                                   (CommandSourceStack)p_248112_.getSource(),
                                                   EntityArgument.getEntity(p_248112_, "target"),
                                                   ResourceArgument.getAttribute(p_248112_, "attribute"),
                                                   1.0
                                                )
                                             ))
                                          .then(
                                             Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .executes(
                                                   p_248106_ -> getAttributeBase(
                                                      (CommandSourceStack)p_248106_.getSource(),
                                                      EntityArgument.getEntity(p_248106_, "target"),
                                                      ResourceArgument.getAttribute(p_248106_, "attribute"),
                                                      DoubleArgumentType.getDouble(p_248106_, "scale")
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_374867_ -> resetAttributeBase(
                                             (CommandSourceStack)p_374867_.getSource(),
                                             EntityArgument.getEntity(p_374867_, "target"),
                                             ResourceArgument.getAttribute(p_374867_, "attribute")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("modifier")
                                    .then(
                                       Commands.literal("add")
                                          .then(
                                             Commands.argument("id", IdentifierArgument.id())
                                                .then(
                                                   ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                               "value", DoubleArgumentType.doubleArg()
                                                            )
                                                            .then(
                                                               Commands.literal("add_value")
                                                                  .executes(
                                                                     p_448890_ -> addModifier(
                                                                        (CommandSourceStack)p_448890_.getSource(),
                                                                        EntityArgument.getEntity(p_448890_, "target"),
                                                                        ResourceArgument.getAttribute(p_448890_, "attribute"),
                                                                        IdentifierArgument.getId(p_448890_, "id"),
                                                                        DoubleArgumentType.getDouble(p_448890_, "value"),
                                                                        AttributeModifier.Operation.ADD_VALUE
                                                                     )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("add_multiplied_base")
                                                               .executes(
                                                                  p_448884_ -> addModifier(
                                                                     (CommandSourceStack)p_448884_.getSource(),
                                                                     EntityArgument.getEntity(p_448884_, "target"),
                                                                     ResourceArgument.getAttribute(p_448884_, "attribute"),
                                                                     IdentifierArgument.getId(p_448884_, "id"),
                                                                     DoubleArgumentType.getDouble(p_448884_, "value"),
                                                                     AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("add_multiplied_total")
                                                            .executes(
                                                               p_448876_ -> addModifier(
                                                                  (CommandSourceStack)p_448876_.getSource(),
                                                                  EntityArgument.getEntity(p_448876_, "target"),
                                                                  ResourceArgument.getAttribute(p_448876_, "attribute"),
                                                                  IdentifierArgument.getId(p_448876_, "id"),
                                                                  DoubleArgumentType.getDouble(p_448876_, "value"),
                                                                  AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                               )
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("remove")
                                       .then(
                                          Commands.argument("id", IdentifierArgument.id())
                                             .suggests(
                                                (p_374868_, p_374869_) -> SharedSuggestionProvider.suggestResource(
                                                   getAttributeModifiers(
                                                      EntityArgument.getEntity(p_374868_, "target"), ResourceArgument.getAttribute(p_374868_, "attribute")
                                                   ),
                                                   p_374869_
                                                )
                                             )
                                             .executes(
                                                p_448886_ -> removeModifier(
                                                   (CommandSourceStack)p_448886_.getSource(),
                                                   EntityArgument.getEntity(p_448886_, "target"),
                                                   ResourceArgument.getAttribute(p_448886_, "attribute"),
                                                   IdentifierArgument.getId(p_448886_, "id")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("value")
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                                   .suggests(
                                                      (p_374862_, p_374863_) -> SharedSuggestionProvider.suggestResource(
                                                         getAttributeModifiers(
                                                            EntityArgument.getEntity(p_374862_, "target"),
                                                            ResourceArgument.getAttribute(p_374862_, "attribute")
                                                         ),
                                                         p_374863_
                                                      )
                                                   )
                                                   .executes(
                                                      p_448891_ -> getAttributeModifier(
                                                         (CommandSourceStack)p_448891_.getSource(),
                                                         EntityArgument.getEntity(p_448891_, "target"),
                                                         ResourceArgument.getAttribute(p_448891_, "attribute"),
                                                         IdentifierArgument.getId(p_448891_, "id"),
                                                         1.0
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                      .executes(
                                                         p_448885_ -> getAttributeModifier(
                                                            (CommandSourceStack)p_448885_.getSource(),
                                                            EntityArgument.getEntity(p_448885_, "target"),
                                                            ResourceArgument.getAttribute(p_448885_, "attribute"),
                                                            IdentifierArgument.getId(p_448885_, "id"),
                                                            DoubleArgumentType.getDouble(p_448885_, "scale")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static AttributeInstance getAttributeInstance(Entity p_252177_, Holder<Attribute> p_249942_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getLivingEntity(p_252177_).getAttributes().getInstance(p_249942_);
      if (attributeinstance == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_252177_.getName(), getAttributeDescription(p_249942_));
      } else {
         return attributeinstance;
      }
   }

   private static LivingEntity getLivingEntity(Entity p_136440_) throws CommandSyntaxException {
      if (!(p_136440_ instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(p_136440_.getName());
      } else {
         return (LivingEntity)p_136440_;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity p_252105_, Holder<Attribute> p_248921_) throws CommandSyntaxException {
      LivingEntity livingentity = getLivingEntity(p_252105_);
      if (!livingentity.getAttributes().hasAttribute(p_248921_)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_252105_.getName(), getAttributeDescription(p_248921_));
      } else {
         return livingentity;
      }
   }

   private static int getAttributeValue(CommandSourceStack p_251776_, Entity p_249647_, Holder<Attribute> p_250986_, double p_251395_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_249647_, p_250986_);
      double d0 = livingentity.getAttributeValue(p_250986_);
      p_251776_.sendSuccess(
         () -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(p_250986_), p_249647_.getName(), d0), false
      );
      return (int)(d0 * p_251395_);
   }

   private static int getAttributeBase(CommandSourceStack p_248780_, Entity p_251083_, Holder<Attribute> p_250388_, double p_250194_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_251083_, p_250388_);
      double d0 = livingentity.getAttributeBaseValue(p_250388_);
      p_248780_.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(p_250388_), p_251083_.getName(), d0), false
      );
      return (int)(d0 * p_250194_);
   }

   private static int getAttributeModifier(CommandSourceStack p_136464_, Entity p_136465_, Holder<Attribute> p_250680_, Identifier p_459234_, double p_136468_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_136465_, p_250680_);
      AttributeMap attributemap = livingentity.getAttributes();
      if (!attributemap.hasModifier(p_250680_, p_459234_)) {
         throw ERROR_NO_SUCH_MODIFIER.create(p_136465_.getName(), getAttributeDescription(p_250680_), p_459234_);
      }

      double d0 = attributemap.getModifierValue(p_250680_, p_459234_);
      p_136464_.sendSuccess(
         () -> Component.translatable(
            "commands.attribute.modifier.value.get.success", Component.translationArg(p_459234_), getAttributeDescription(p_250680_), p_136465_.getName(), d0
         ),
         false
      );
      return (int)(d0 * p_136468_);
   }

   private static Stream<Identifier> getAttributeModifiers(Entity p_377287_, Holder<Attribute> p_377560_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_377287_, p_377560_);
      return attributeinstance.getModifiers().stream().map(AttributeModifier::id);
   }

   private static int setAttributeBase(CommandSourceStack p_248556_, Entity p_248620_, Holder<Attribute> p_249456_, double p_252212_) throws CommandSyntaxException {
      getAttributeInstance(p_248620_, p_249456_).setBaseValue(p_252212_);
      p_248556_.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(p_249456_), p_248620_.getName(), p_252212_), false
      );
      return 1;
   }

   private static int resetAttributeBase(CommandSourceStack p_377147_, Entity p_377320_, Holder<Attribute> p_377314_) throws CommandSyntaxException {
      LivingEntity livingentity = getLivingEntity(p_377320_);
      if (!livingentity.getAttributes().resetBaseValue(p_377314_)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_377320_.getName(), getAttributeDescription(p_377314_));
      }

      double d0 = livingentity.getAttributeBaseValue(p_377314_);
      p_377147_.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.reset.success", getAttributeDescription(p_377314_), p_377320_.getName(), d0), false
      );
      return 1;
   }

   private static int addModifier(
      CommandSourceStack p_136470_,
      Entity p_136471_,
      Holder<Attribute> p_251636_,
      Identifier p_460337_,
      double p_136475_,
      AttributeModifier.Operation p_136476_
   ) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_136471_, p_251636_);
      AttributeModifier attributemodifier = new AttributeModifier(p_460337_, p_136475_, p_136476_);
      if (attributeinstance.hasModifier(p_460337_)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(p_136471_.getName(), getAttributeDescription(p_251636_), p_460337_);
      }

      attributeinstance.addPermanentModifier(attributemodifier);
      p_136470_.sendSuccess(
         () -> Component.translatable(
            "commands.attribute.modifier.add.success", Component.translationArg(p_460337_), getAttributeDescription(p_251636_), p_136471_.getName()
         ),
         false
      );
      return 1;
   }

   private static int removeModifier(CommandSourceStack p_136459_, Entity p_136460_, Holder<Attribute> p_250830_, Identifier p_456404_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_136460_, p_250830_);
      if (attributeinstance.removeModifier(p_456404_)) {
         p_136459_.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.remove.success", Component.translationArg(p_456404_), getAttributeDescription(p_250830_), p_136460_.getName()
            ),
            false
         );
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(p_136460_.getName(), getAttributeDescription(p_250830_), p_456404_);
      }
   }

   private static Component getAttributeDescription(Holder<Attribute> p_250602_) {
      return Component.translatable(p_250602_.value().getDescriptionId());
   }
}
