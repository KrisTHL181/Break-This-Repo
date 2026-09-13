package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;

public class IdentifierPattern {
   public static final Codec<IdentifierPattern> CODEC = RecordCodecBuilder.create(
      p_454730_ -> p_454730_.group(
            ExtraCodecs.PATTERN.optionalFieldOf("namespace").forGetter(p_452564_ -> p_452564_.namespacePattern),
            ExtraCodecs.PATTERN.optionalFieldOf("path").forGetter(p_460255_ -> p_460255_.pathPattern)
         )
         .apply(p_454730_, IdentifierPattern::new)
   );
   private final Optional<Pattern> namespacePattern;
   private final Predicate<String> namespacePredicate;
   private final Optional<Pattern> pathPattern;
   private final Predicate<String> pathPredicate;
   private final Predicate<Identifier> locationPredicate;

   private IdentifierPattern(Optional<Pattern> p_458581_, Optional<Pattern> p_459105_) {
      this.namespacePattern = p_458581_;
      this.namespacePredicate = p_458581_.map(Pattern::asPredicate).orElse(p_455115_ -> true);
      this.pathPattern = p_459105_;
      this.pathPredicate = p_459105_.map(Pattern::asPredicate).orElse(p_455057_ -> true);
      this.locationPredicate = p_455317_ -> this.namespacePredicate.test(p_455317_.getNamespace()) && this.pathPredicate.test(p_455317_.getPath());
   }

   public Predicate<String> namespacePredicate() {
      return this.namespacePredicate;
   }

   public Predicate<String> pathPredicate() {
      return this.pathPredicate;
   }

   public Predicate<Identifier> locationPredicate() {
      return this.locationPredicate;
   }
}
