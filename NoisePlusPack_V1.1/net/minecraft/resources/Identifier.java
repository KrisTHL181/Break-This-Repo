package net.minecraft.resources;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.IdentifierException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public final class Identifier implements Comparable<Identifier> {
   public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(Identifier::read, Identifier::toString).stable();
   public static final StreamCodec<ByteBuf, Identifier> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString);
   public static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
   public static final char NAMESPACE_SEPARATOR = ':';
   public static final String DEFAULT_NAMESPACE = "minecraft";
   public static final String REALMS_NAMESPACE = "realms";
   private final String namespace;
   private final String path;

   private Identifier(String p_453419_, String p_457561_) {
      assert isValidNamespace(p_453419_);
      assert isValidPath(p_457561_);
      this.namespace = p_453419_;
      this.path = p_457561_;
   }

   private static Identifier createUntrusted(String p_458386_, String p_455185_) {
      return new Identifier(assertValidNamespace(p_458386_, p_455185_), assertValidPath(p_458386_, p_455185_));
   }

   public static Identifier fromNamespaceAndPath(String p_453591_, String p_452050_) {
      return createUntrusted(p_453591_, p_452050_);
   }

   public static Identifier parse(String p_450672_) {
      return bySeparator(p_450672_, ':');
   }

   public static Identifier withDefaultNamespace(String p_450610_) {
      return new Identifier("minecraft", assertValidPath("minecraft", p_450610_));
   }

   public static @Nullable Identifier tryParse(String p_456562_) {
      return tryBySeparator(p_456562_, ':');
   }

   public static @Nullable Identifier tryBuild(String p_458111_, String p_455894_) {
      return isValidNamespace(p_458111_) && isValidPath(p_455894_) ? new Identifier(p_458111_, p_455894_) : null;
   }

   public static Identifier bySeparator(String p_450597_, char p_457881_) {
      int i = p_450597_.indexOf(p_457881_);
      if (i >= 0) {
         String s = p_450597_.substring(i + 1);
         if (i != 0) {
            String s1 = p_450597_.substring(0, i);
            return createUntrusted(s1, s);
         } else {
            return withDefaultNamespace(s);
         }
      } else {
         return withDefaultNamespace(p_450597_);
      }
   }

   public static @Nullable Identifier tryBySeparator(String p_458139_, char p_459073_) {
      int i = p_458139_.indexOf(p_459073_);
      if (i >= 0) {
         String s = p_458139_.substring(i + 1);
         if (!isValidPath(s)) {
            return null;
         } else if (i != 0) {
            String s1 = p_458139_.substring(0, i);
            return isValidNamespace(s1) ? new Identifier(s1, s) : null;
         } else {
            return new Identifier("minecraft", s);
         }
      } else {
         return isValidPath(p_458139_) ? new Identifier("minecraft", p_458139_) : null;
      }
   }

   public static DataResult<Identifier> read(String p_460941_) {
      try {
         return DataResult.success(parse(p_460941_));
      } catch (IdentifierException identifierexception) {
         return DataResult.error(() -> "Not a valid resource location: " + p_460941_ + " " + identifierexception.getMessage());
      }
   }

   public String getPath() {
      return this.path;
   }

   public String getNamespace() {
      return this.namespace;
   }

   public Identifier withPath(String p_458392_) {
      return new Identifier(this.namespace, assertValidPath(this.namespace, p_458392_));
   }

   public Identifier withPath(UnaryOperator<String> p_460135_) {
      return this.withPath(p_460135_.apply(this.path));
   }

   public Identifier withPrefix(String p_455609_) {
      return this.withPath(p_455609_ + this.path);
   }

   public Identifier withSuffix(String p_460571_) {
      return this.withPath(this.path + p_460571_);
   }

   @Override
   public String toString() {
      return this.namespace + ":" + this.path;
   }

   @Override
   public boolean equals(Object p_458291_) {
      if (this == p_458291_) {
         return true;
      } else {
         return p_458291_ instanceof Identifier identifier ? this.namespace.equals(identifier.namespace) && this.path.equals(identifier.path) : false;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.namespace.hashCode() + this.path.hashCode();
   }

   public int compareTo(Identifier p_452748_) {
      int i = this.path.compareTo(p_452748_.path);
      if (i == 0) {
         i = this.namespace.compareTo(p_452748_.namespace);
      }

      return i;
   }

   public String toDebugFileName() {
      return this.toString().replace('/', '_').replace(':', '_');
   }

   public String toLanguageKey() {
      return this.namespace + "." + this.path;
   }

   public String toShortLanguageKey() {
      return this.namespace.equals("minecraft") ? this.path : this.toLanguageKey();
   }

   public String toShortString() {
      return this.namespace.equals("minecraft") ? this.path : this.toString();
   }

   public String toLanguageKey(String p_451530_) {
      return p_451530_ + "." + this.toLanguageKey();
   }

   public String toLanguageKey(String p_458514_, String p_455994_) {
      return p_458514_ + "." + this.toLanguageKey() + "." + p_455994_;
   }

   private static String readGreedy(StringReader p_458656_) {
      int i = p_458656_.getCursor();

      while (p_458656_.canRead() && isAllowedInIdentifier(p_458656_.peek())) {
         p_458656_.skip();
      }

      return p_458656_.getString().substring(i, p_458656_.getCursor());
   }

   public static Identifier read(StringReader p_458258_) throws CommandSyntaxException {
      int i = p_458258_.getCursor();
      String s = readGreedy(p_458258_);

      try {
         return parse(s);
      } catch (IdentifierException identifierexception) {
         p_458258_.setCursor(i);
         throw ERROR_INVALID.createWithContext(p_458258_);
      }
   }

   public static Identifier readNonEmpty(StringReader p_450362_) throws CommandSyntaxException {
      int i = p_450362_.getCursor();
      String s = readGreedy(p_450362_);
      if (s.isEmpty()) {
         throw ERROR_INVALID.createWithContext(p_450362_);
      }

      try {
         return parse(s);
      } catch (IdentifierException identifierexception) {
         p_450362_.setCursor(i);
         throw ERROR_INVALID.createWithContext(p_450362_);
      }
   }

   public static boolean isAllowedInIdentifier(char p_453350_) {
      return p_453350_ >= '0' && p_453350_ <= '9'
         || p_453350_ >= 'a' && p_453350_ <= 'z'
         || p_453350_ == '_'
         || p_453350_ == ':'
         || p_453350_ == '/'
         || p_453350_ == '.'
         || p_453350_ == '-';
   }

   public static boolean isValidPath(String p_450971_) {
      for (int i = 0; i < p_450971_.length(); i++) {
         if (!validPathChar(p_450971_.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   public static boolean isValidNamespace(String p_457130_) {
      for (int i = 0; i < p_457130_.length(); i++) {
         if (!validNamespaceChar(p_457130_.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   private static String assertValidNamespace(String p_457507_, String p_451506_) {
      if (!isValidNamespace(p_457507_)) {
         throw new IdentifierException("Non [a-z0-9_.-] character in namespace of location: " + p_457507_ + ":" + p_451506_);
      } else {
         return p_457507_;
      }
   }

   public static boolean validPathChar(char p_458266_) {
      return p_458266_ == '_'
         || p_458266_ == '-'
         || p_458266_ >= 'a' && p_458266_ <= 'z'
         || p_458266_ >= '0' && p_458266_ <= '9'
         || p_458266_ == '/'
         || p_458266_ == '.';
   }

   private static boolean validNamespaceChar(char p_454742_) {
      return p_454742_ == '_' || p_454742_ == '-' || p_454742_ >= 'a' && p_454742_ <= 'z' || p_454742_ >= '0' && p_454742_ <= '9' || p_454742_ == '.';
   }

   private static String assertValidPath(String p_450153_, String p_458379_) {
      if (!isValidPath(p_458379_)) {
         throw new IdentifierException("Non [a-z0-9/._-] character in path of location: " + p_450153_ + ":" + p_458379_);
      } else {
         return p_458379_;
      }
   }
}
