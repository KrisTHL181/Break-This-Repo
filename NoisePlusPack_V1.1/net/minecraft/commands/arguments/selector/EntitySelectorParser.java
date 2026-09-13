package net.minecraft.commands.arguments.selector;

import com.google.common.primitives.Doubles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntitySelectorParser {
   public static final char SYNTAX_SELECTOR_START = '@';
   private static final char SYNTAX_OPTIONS_START = '[';
   private static final char SYNTAX_OPTIONS_END = ']';
   public static final char SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR = '=';
   private static final char SYNTAX_OPTIONS_SEPARATOR = ',';
   public static final char SYNTAX_NOT = '!';
   public static final char SYNTAX_TAG = '#';
   private static final char SELECTOR_NEAREST_PLAYER = 'p';
   private static final char SELECTOR_ALL_PLAYERS = 'a';
   private static final char SELECTOR_RANDOM_PLAYERS = 'r';
   private static final char SELECTOR_CURRENT_ENTITY = 's';
   private static final char SELECTOR_ALL_ENTITIES = 'e';
   private static final char SELECTOR_NEAREST_ENTITY = 'n';
   public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID = new SimpleCommandExceptionType(Component.translatable("argument.entity.invalid"));
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE = new DynamicCommandExceptionType(
      p_308409_ -> Component.translatableEscape("argument.entity.selector.unknown", p_308409_)
   );
   public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.selector.not_allowed")
   );
   public static final SimpleCommandExceptionType ERROR_MISSING_SELECTOR_TYPE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.selector.missing")
   );
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.unterminated")
   );
   public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE = new DynamicCommandExceptionType(
      p_308408_ -> Component.translatableEscape("argument.entity.options.valueless", p_308408_)
   );
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_NEAREST = (p_121313_, p_121314_) -> p_121314_.sort(
      (p_175140_, p_175141_) -> Doubles.compare(p_175140_.distanceToSqr(p_121313_), p_175141_.distanceToSqr(p_121313_))
   );
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_FURTHEST = (p_121298_, p_121299_) -> p_121299_.sort(
      (p_175131_, p_175132_) -> Doubles.compare(p_175132_.distanceToSqr(p_121298_), p_175131_.distanceToSqr(p_121298_))
   );
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_RANDOM = (p_121264_, p_121265_) -> Collections.shuffle(p_121265_);
   public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (p_121363_, p_121364_) -> p_121363_.buildFuture();
   private final StringReader reader;
   private final boolean allowSelectors;
   private int maxResults;
   private boolean includesEntities;
   private boolean worldLimited;
   private MinMaxBounds.@Nullable Doubles distance;
   private MinMaxBounds.@Nullable Ints level;
   private @Nullable Double x;
   private @Nullable Double y;
   private @Nullable Double z;
   private @Nullable Double deltaX;
   private @Nullable Double deltaY;
   private @Nullable Double deltaZ;
   private MinMaxBounds.@Nullable FloatDegrees rotX;
   private MinMaxBounds.@Nullable FloatDegrees rotY;
   private final List<Predicate<Entity>> predicates = new ArrayList<>();
   private BiConsumer<Vec3, List<? extends Entity>> order = EntitySelector.ORDER_ARBITRARY;
   private boolean currentEntity;
   private @Nullable String playerName;
   private int startPosition;
   private @Nullable UUID entityUUID;
   private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
   private boolean hasNameEquals;
   private boolean hasNameNotEquals;
   private boolean isLimited;
   private boolean isSorted;
   private boolean hasGamemodeEquals;
   private boolean hasGamemodeNotEquals;
   private boolean hasTeamEquals;
   private boolean hasTeamNotEquals;
   private @Nullable EntityType<?> type;
   private boolean typeInverse;
   private boolean hasScores;
   private boolean hasAdvancements;
   private boolean usesSelectors;

   public EntitySelectorParser(StringReader p_121220_, boolean p_121221_) {
      this.reader = p_121220_;
      this.allowSelectors = p_121221_;
   }

   public static <S> boolean allowSelectors(S p_345546_) {
      return p_345546_ instanceof PermissionSetSupplier permissionsetsupplier
         && permissionsetsupplier.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS);
   }

   @Deprecated
   public static boolean allowSelectors(PermissionSetSupplier p_456659_) {
      return p_456659_.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS);
   }

   public EntitySelector getSelector() {
      AABB aabb;
      if (this.deltaX == null && this.deltaY == null && this.deltaZ == null) {
         if (this.distance != null && this.distance.max().isPresent()) {
            double d0 = (Double)this.distance.max().get();
            aabb = new AABB(-d0, -d0, -d0, d0 + 1.0, d0 + 1.0, d0 + 1.0);
         } else {
            aabb = null;
         }
      } else {
         aabb = this.createAabb(this.deltaX == null ? 0.0 : this.deltaX, this.deltaY == null ? 0.0 : this.deltaY, this.deltaZ == null ? 0.0 : this.deltaZ);
      }

      Function<Vec3, Vec3> function;
      if (this.x == null && this.y == null && this.z == null) {
         function = p_121292_ -> p_121292_;
      } else {
         function = p_121258_ -> new Vec3(this.x == null ? p_121258_.x : this.x, this.y == null ? p_121258_.y : this.y, this.z == null ? p_121258_.z : this.z);
      }

      return new EntitySelector(
         this.maxResults,
         this.includesEntities,
         this.worldLimited,
         List.copyOf(this.predicates),
         this.distance,
         function,
         aabb,
         this.order,
         this.currentEntity,
         this.playerName,
         this.entityUUID,
         this.type,
         this.usesSelectors
      );
   }

   private AABB createAabb(double p_121234_, double p_121235_, double p_121236_) {
      boolean flag = p_121234_ < 0.0;
      boolean flag1 = p_121235_ < 0.0;
      boolean flag2 = p_121236_ < 0.0;
      double d0 = flag ? p_121234_ : 0.0;
      double d1 = flag1 ? p_121235_ : 0.0;
      double d2 = flag2 ? p_121236_ : 0.0;
      double d3 = (flag ? 0.0 : p_121234_) + 1.0;
      double d4 = (flag1 ? 0.0 : p_121235_) + 1.0;
      double d5 = (flag2 ? 0.0 : p_121236_) + 1.0;
      return new AABB(d0, d1, d2, d3, d4, d5);
   }

   private void finalizePredicates() {
      if (this.rotX != null) {
         this.predicates.add(this.createRotationPredicate(this.rotX, Entity::getXRot));
      }

      if (this.rotY != null) {
         this.predicates.add(this.createRotationPredicate(this.rotY, Entity::getYRot));
      }

      if (this.level != null) {
         this.predicates.add(p_448538_ -> p_448538_ instanceof ServerPlayer serverplayer && this.level.matches(serverplayer.experienceLevel));
      }
   }

   private Predicate<Entity> createRotationPredicate(MinMaxBounds.FloatDegrees p_458419_, ToFloatFunction<Entity> p_431417_) {
      float f = Mth.wrapDegrees(p_458419_.min().orElse(0.0F));
      float f1 = Mth.wrapDegrees(p_458419_.max().orElse(359.0F));
      return p_175137_ -> {
         float f2 = Mth.wrapDegrees(p_431417_.applyAsFloat(p_175137_));
         return f > f1 ? f2 >= f || f2 <= f1 : f2 >= f && f2 <= f1;
      };
   }

   protected void parseSelector() throws CommandSyntaxException {
      this.usesSelectors = true;
      this.suggestions = this::suggestSelector;
      if (!this.reader.canRead()) {
         throw ERROR_MISSING_SELECTOR_TYPE.createWithContext(this.reader);
      }

      label28: {
         int i = this.reader.getCursor();
         char c0 = this.reader.read();
         switch (c0) {
            case 'a':
               this.maxResults = Integer.MAX_VALUE;
               this.includesEntities = false;
               this.order = EntitySelector.ORDER_ARBITRARY;
               this.limitToType(EntityType.PLAYER);
               break label28;
            case 'b':
            case 'c':
            case 'd':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'o':
            case 'q':
            default:
               this.reader.setCursor(i);
               throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(this.reader, "@" + c0);
            case 'e':
               this.maxResults = Integer.MAX_VALUE;
               this.includesEntities = true;
               this.order = EntitySelector.ORDER_ARBITRARY;
               break;
            case 'n':
               this.maxResults = 1;
               this.includesEntities = true;
               this.order = ORDER_NEAREST;
               break;
            case 'p':
               this.maxResults = 1;
               this.includesEntities = false;
               this.order = ORDER_NEAREST;
               this.limitToType(EntityType.PLAYER);
               break label28;
            case 'r':
               this.maxResults = 1;
               this.includesEntities = false;
               this.order = ORDER_RANDOM;
               this.limitToType(EntityType.PLAYER);
               break label28;
            case 's':
               this.maxResults = 1;
               this.includesEntities = true;
               this.currentEntity = true;
               break label28;
         }

         this.predicates.add(Entity::isAlive);
      }

      this.suggestions = this::suggestOpenOptions;
      if (this.reader.canRead() && this.reader.peek() == '[') {
         this.reader.skip();
         this.suggestions = this::suggestOptionsKeyOrClose;
         this.parseOptions();
      }
   }

   protected void parseNameOrUUID() throws CommandSyntaxException {
      if (this.reader.canRead()) {
         this.suggestions = this::suggestName;
      }

      int i = this.reader.getCursor();
      String s = this.reader.readString();

      try {
         this.entityUUID = UUID.fromString(s);
         this.includesEntities = true;
      } catch (IllegalArgumentException illegalargumentexception) {
         if (s.isEmpty() || s.length() > 16) {
            this.reader.setCursor(i);
            throw ERROR_INVALID_NAME_OR_UUID.createWithContext(this.reader);
         }

         this.includesEntities = false;
         this.playerName = s;
      }

      this.maxResults = 1;
   }

   protected void parseOptions() throws CommandSyntaxException {
      this.suggestions = this::suggestOptionsKey;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int i = this.reader.getCursor();
         String s = this.reader.readString();
         EntitySelectorOptions.Modifier entityselectoroptions$modifier = EntitySelectorOptions.get(this, s, i);
         this.reader.skipWhitespace();
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            this.reader.setCursor(i);
            throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(this.reader, s);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = SUGGEST_NOTHING;
         entityselectoroptions$modifier.handle(this);
         this.reader.skipWhitespace();
         this.suggestions = this::suggestOptionsNextOrClose;
         if (this.reader.canRead()) {
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestOptionsKey;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
         this.suggestions = SUGGEST_NOTHING;
      } else {
         throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
      }
   }

   public boolean shouldInvertValue() {
      this.reader.skipWhitespace();
      if (this.reader.canRead() && this.reader.peek() == '!') {
         this.reader.skip();
         this.reader.skipWhitespace();
         return true;
      } else {
         return false;
      }
   }

   public boolean isTag() {
      this.reader.skipWhitespace();
      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.reader.skip();
         this.reader.skipWhitespace();
         return true;
      } else {
         return false;
      }
   }

   public StringReader getReader() {
      return this.reader;
   }

   public void addPredicate(Predicate<Entity> p_121273_) {
      this.predicates.add(p_121273_);
   }

   public void setWorldLimited() {
      this.worldLimited = true;
   }

   public MinMaxBounds.@Nullable Doubles getDistance() {
      return this.distance;
   }

   public void setDistance(MinMaxBounds.Doubles p_460881_) {
      this.distance = p_460881_;
   }

   public MinMaxBounds.@Nullable Ints getLevel() {
      return this.level;
   }

   public void setLevel(MinMaxBounds.Ints p_451798_) {
      this.level = p_451798_;
   }

   public MinMaxBounds.@Nullable FloatDegrees getRotX() {
      return this.rotX;
   }

   public void setRotX(MinMaxBounds.FloatDegrees p_456277_) {
      this.rotX = p_456277_;
   }

   public MinMaxBounds.@Nullable FloatDegrees getRotY() {
      return this.rotY;
   }

   public void setRotY(MinMaxBounds.FloatDegrees p_450903_) {
      this.rotY = p_450903_;
   }

   public @Nullable Double getX() {
      return this.x;
   }

   public @Nullable Double getY() {
      return this.y;
   }

   public @Nullable Double getZ() {
      return this.z;
   }

   public void setX(double p_121232_) {
      this.x = p_121232_;
   }

   public void setY(double p_121283_) {
      this.y = p_121283_;
   }

   public void setZ(double p_121306_) {
      this.z = p_121306_;
   }

   public void setDeltaX(double p_121319_) {
      this.deltaX = p_121319_;
   }

   public void setDeltaY(double p_121332_) {
      this.deltaY = p_121332_;
   }

   public void setDeltaZ(double p_121340_) {
      this.deltaZ = p_121340_;
   }

   public @Nullable Double getDeltaX() {
      return this.deltaX;
   }

   public @Nullable Double getDeltaY() {
      return this.deltaY;
   }

   public @Nullable Double getDeltaZ() {
      return this.deltaZ;
   }

   public void setMaxResults(int p_121238_) {
      this.maxResults = p_121238_;
   }

   public void setIncludesEntities(boolean p_121280_) {
      this.includesEntities = p_121280_;
   }

   public BiConsumer<Vec3, List<? extends Entity>> getOrder() {
      return this.order;
   }

   public void setOrder(BiConsumer<Vec3, List<? extends Entity>> p_121269_) {
      this.order = p_121269_;
   }

   public EntitySelector parse() throws CommandSyntaxException {
      this.startPosition = this.reader.getCursor();
      this.suggestions = this::suggestNameOrSelector;
      if (this.reader.canRead() && this.reader.peek() == '@') {
         if (!this.allowSelectors) {
            throw ERROR_SELECTORS_NOT_ALLOWED.createWithContext(this.reader);
         }

         this.reader.skip();
         this.parseSelector();
      } else {
         this.parseNameOrUUID();
      }

      this.finalizePredicates();
      return this.getSelector();
   }

   private static void fillSelectorSuggestions(SuggestionsBuilder p_121248_) {
      p_121248_.suggest("@p", Component.translatable("argument.entity.selector.nearestPlayer"));
      p_121248_.suggest("@a", Component.translatable("argument.entity.selector.allPlayers"));
      p_121248_.suggest("@r", Component.translatable("argument.entity.selector.randomPlayer"));
      p_121248_.suggest("@s", Component.translatable("argument.entity.selector.self"));
      p_121248_.suggest("@e", Component.translatable("argument.entity.selector.allEntities"));
      p_121248_.suggest("@n", Component.translatable("argument.entity.selector.nearestEntity"));
   }

   private CompletableFuture<Suggestions> suggestNameOrSelector(SuggestionsBuilder p_121287_, Consumer<SuggestionsBuilder> p_121288_) {
      p_121288_.accept(p_121287_);
      if (this.allowSelectors) {
         fillSelectorSuggestions(p_121287_);
      }

      return p_121287_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestName(SuggestionsBuilder p_121310_, Consumer<SuggestionsBuilder> p_121311_) {
      SuggestionsBuilder suggestionsbuilder = p_121310_.createOffset(this.startPosition);
      p_121311_.accept(suggestionsbuilder);
      return p_121310_.add(suggestionsbuilder).buildFuture();
   }

   private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder p_121323_, Consumer<SuggestionsBuilder> p_121324_) {
      SuggestionsBuilder suggestionsbuilder = p_121323_.createOffset(p_121323_.getStart() - 1);
      fillSelectorSuggestions(suggestionsbuilder);
      p_121323_.add(suggestionsbuilder);
      return p_121323_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder p_121334_, Consumer<SuggestionsBuilder> p_121335_) {
      p_121334_.suggest(String.valueOf('['));
      return p_121334_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(SuggestionsBuilder p_121342_, Consumer<SuggestionsBuilder> p_121343_) {
      p_121342_.suggest(String.valueOf(']'));
      EntitySelectorOptions.suggestNames(this, p_121342_);
      return p_121342_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOptionsKey(SuggestionsBuilder p_121348_, Consumer<SuggestionsBuilder> p_121349_) {
      EntitySelectorOptions.suggestNames(this, p_121348_);
      return p_121348_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOptionsNextOrClose(SuggestionsBuilder p_121354_, Consumer<SuggestionsBuilder> p_121355_) {
      p_121354_.suggest(String.valueOf(','));
      p_121354_.suggest(String.valueOf(']'));
      return p_121354_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder p_175144_, Consumer<SuggestionsBuilder> p_175145_) {
      p_175144_.suggest(String.valueOf('='));
      return p_175144_.buildFuture();
   }

   public boolean isCurrentEntity() {
      return this.currentEntity;
   }

   public void setSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> p_121271_) {
      this.suggestions = p_121271_;
   }

   public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder p_121250_, Consumer<SuggestionsBuilder> p_121251_) {
      return this.suggestions.apply(p_121250_.createOffset(this.reader.getCursor()), p_121251_);
   }

   public boolean hasNameEquals() {
      return this.hasNameEquals;
   }

   public void setHasNameEquals(boolean p_121303_) {
      this.hasNameEquals = p_121303_;
   }

   public boolean hasNameNotEquals() {
      return this.hasNameNotEquals;
   }

   public void setHasNameNotEquals(boolean p_121316_) {
      this.hasNameNotEquals = p_121316_;
   }

   public boolean isLimited() {
      return this.isLimited;
   }

   public void setLimited(boolean p_121329_) {
      this.isLimited = p_121329_;
   }

   public boolean isSorted() {
      return this.isSorted;
   }

   public void setSorted(boolean p_121337_) {
      this.isSorted = p_121337_;
   }

   public boolean hasGamemodeEquals() {
      return this.hasGamemodeEquals;
   }

   public void setHasGamemodeEquals(boolean p_121345_) {
      this.hasGamemodeEquals = p_121345_;
   }

   public boolean hasGamemodeNotEquals() {
      return this.hasGamemodeNotEquals;
   }

   public void setHasGamemodeNotEquals(boolean p_121351_) {
      this.hasGamemodeNotEquals = p_121351_;
   }

   public boolean hasTeamEquals() {
      return this.hasTeamEquals;
   }

   public void setHasTeamEquals(boolean p_121357_) {
      this.hasTeamEquals = p_121357_;
   }

   public boolean hasTeamNotEquals() {
      return this.hasTeamNotEquals;
   }

   public void setHasTeamNotEquals(boolean p_121360_) {
      this.hasTeamNotEquals = p_121360_;
   }

   public void limitToType(EntityType<?> p_121242_) {
      this.type = p_121242_;
   }

   public void setTypeLimitedInversely() {
      this.typeInverse = true;
   }

   public boolean isTypeLimited() {
      return this.type != null;
   }

   public boolean isTypeLimitedInversely() {
      return this.typeInverse;
   }

   public boolean hasScores() {
      return this.hasScores;
   }

   public void setHasScores(boolean p_121366_) {
      this.hasScores = p_121366_;
   }

   public boolean hasAdvancements() {
      return this.hasAdvancements;
   }

   public void setHasAdvancements(boolean p_121369_) {
      this.hasAdvancements = p_121369_;
   }
}
