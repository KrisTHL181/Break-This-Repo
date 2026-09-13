package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

@SuppressForbidden(reason = "System.out setup")
public class Bootstrap {
   public static final PrintStream STDOUT = System.out;
   private static volatile boolean isBootstrapped;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final AtomicLong bootstrapDuration = new AtomicLong(-1L);

   public static void bootStrap() {
      if (!isBootstrapped) {
         isBootstrapped = true;
         Instant instant = Instant.now();
         if (BuiltInRegistries.REGISTRY.keySet().isEmpty()) {
            throw new IllegalStateException("Unable to load registries");
         }

         FireBlock.bootStrap();
         ComposterBlock.bootStrap();
         if (EntityType.getKey(EntityType.PLAYER) == null) {
            throw new IllegalStateException("Failed loading EntityTypes");
         }

         EntitySelectorOptions.bootStrap();
         DispenseItemBehavior.bootStrap();
         CauldronInteraction.bootStrap();
         BuiltInRegistries.bootStrap();
         CreativeModeTabs.validate();
         wrapStreams();
         bootstrapDuration.set(Duration.between(instant, Instant.now()).toMillis());
      }
   }

   private static <T> void checkTranslations(Iterable<T> p_135872_, Function<T, String> p_135873_, Set<String> p_135874_) {
      Language language = Language.getInstance();
      p_135872_.forEach(p_135883_ -> {
         String s = p_135873_.apply((T)p_135883_);
         if (!language.has(s)) {
            p_135874_.add(s);
         }
      });
   }

   private static void checkGameruleTranslations(final Set<String> p_135878_) {
      final Language language = Language.getInstance();
      GameRules gamerules = new GameRules(FeatureFlags.REGISTRY.allFlags());
      gamerules.visitGameRuleTypes(new GameRuleTypeVisitor() {
         @Override
         public <T> void visit(GameRule<T> p_453084_) {
            if (!language.has(p_453084_.getDescriptionId())) {
               p_135878_.add(p_453084_.id());
            }
         }
      });
   }

   public static Set<String> getMissingTranslations() {
      Set<String> set = new TreeSet<>();
      checkTranslations(BuiltInRegistries.ATTRIBUTE, Attribute::getDescriptionId, set);
      checkTranslations(BuiltInRegistries.ENTITY_TYPE, EntityType::getDescriptionId, set);
      checkTranslations(BuiltInRegistries.MOB_EFFECT, MobEffect::getDescriptionId, set);
      checkTranslations(BuiltInRegistries.ITEM, Item::getDescriptionId, set);
      checkTranslations(BuiltInRegistries.BLOCK, BlockBehaviour::getDescriptionId, set);
      checkTranslations(BuiltInRegistries.CUSTOM_STAT, p_448807_ -> "stat." + p_448807_.toString().replace(':', '.'), set);
      checkGameruleTranslations(set);
      return set;
   }

   public static void checkBootstrapCalled(Supplier<String> p_179913_) {
      if (!isBootstrapped) {
         throw createBootstrapException(p_179913_);
      }
   }

   private static RuntimeException createBootstrapException(Supplier<String> p_179917_) {
      try {
         String s = p_179917_.get();
         return new IllegalArgumentException("Not bootstrapped (called from " + s + ")");
      } catch (Exception exception) {
         RuntimeException runtimeexception = new IllegalArgumentException("Not bootstrapped (failed to resolve location)");
         runtimeexception.addSuppressed(exception);
         return runtimeexception;
      }
   }

   public static void validate() {
      checkBootstrapCalled(() -> "validate");
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         getMissingTranslations().forEach(p_179915_ -> LOGGER.error("Missing translations: {}", p_179915_));
         Commands.validate();
      }

      DefaultAttributes.validate();
   }

   private static void wrapStreams() {
      if (LOGGER.isDebugEnabled()) {
         System.setErr(new DebugLoggedPrintStream("STDERR", System.err));
         System.setOut(new DebugLoggedPrintStream("STDOUT", STDOUT));
      } else {
         System.setErr(new LoggedPrintStream("STDERR", System.err));
         System.setOut(new LoggedPrintStream("STDOUT", STDOUT));
      }
   }

   public static void realStdoutPrintln(String p_135876_) {
      STDOUT.println(p_135876_);
   }
}
