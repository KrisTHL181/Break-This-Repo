package com.mojang.datafixers;

import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.profunctors.Cartesian;
import com.mojang.datafixers.optics.profunctors.Profunctor;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Tag;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class FieldFinder<FT> implements OpticFinder<FT> {
   @Nullable
   private final String name;
   private final Type<FT> type;

   public FieldFinder(@Nullable String name, Type<FT> type) {
      this.name = name;
      this.type = type;
   }

   @Override
   public Type<FT> type() {
      return this.type;
   }

   @Override
   public <A, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findType(Type<A> containerType, Type<FR> resultType, boolean recurse) {
      return containerType.findTypeCached(this.type, resultType, new FieldFinder.Matcher<>(this.name, this.type, resultType), recurse);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof FieldFinder<?> that) ? false : Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type);
      }
   }

   @Override
   public int hashCode() {
      int result = this.name != null ? this.name.hashCode() : 0;
      return 31 * result + this.type.hashCode();
   }

   private static final class Matcher<FT, FR> implements Type.TypeMatcher<FT, FR> {
      private final Type<FR> resultType;
      @Nullable
      private final String name;
      private final Type<FT> type;

      public Matcher(@Nullable String name, Type<FT> type, Type<FR> resultType) {
         this.resultType = resultType;
         this.name = name;
         this.type = type;
      }

      @Override
      public <S> Either<TypedOptic<S, ?, FT, FR>, Type.FieldNotFoundException> match(Type<S> targetType) {
         // ===== 修改：显式指定 TypedOptic 类型参数 =====
         if (this.name == null && this.type.equals(targetType, true, false)) {
            return Either.left(new TypedOptic<>(
                Profunctor.Mu.TYPE_TOKEN,
                targetType,
                this.resultType,
                (Type<FT>) targetType,
                this.resultType,
                Optics.id()
            ));
         }

         if (targetType instanceof Tag.TagType<S> tagType) {
            if (!Objects.equals(tagType.name(), this.name)) {
               return Either.right(new Type.FieldNotFoundException(String.format("Not found: \"%s\" (in type: %s)", this.name, targetType)));
            } else {
               if (!Objects.equals(this.type, tagType.element())) {
                  return Either.right(
                     new Type.FieldNotFoundException(
                        String.format("Type error for field \"%s\": expected type: %s, actual type: %s)", this.name, this.type, tagType.element())
                     )
                  );
               }
               // ===== 修改：显式指定 TypedOptic 类型参数 =====
               return Either.left(
                  new TypedOptic<>(
                     Profunctor.Mu.TYPE_TOKEN,
                     tagType,
                     DSL.field(tagType.name(), this.resultType),
                     this.type,
                     this.resultType,
                     Optics.id()
                  )
               );
            }
         }

         // ===== 修改：使用通配符进行 instanceof 检查，然后手动处理 =====
         if (!(targetType instanceof TaggedChoice.TaggedChoiceType<?> choiceType) || !Objects.equals(this.name, choiceType.getName())) {
            return Either.right(new Type.Continue());
         }

         @SuppressWarnings("unchecked")
         TaggedChoice.TaggedChoiceType<FT> ftChoiceType = (TaggedChoice.TaggedChoiceType<FT>) choiceType;

         if (!Objects.equals(this.type, ftChoiceType.getKeyType())) {
            return Either.right(
               new Type.FieldNotFoundException(
                  String.format("Type error for field \"%s\": expected type: %s, actual type: %s)", this.name, this.type, ftChoiceType.getKeyType())
               )
            );
         }

         if (!Objects.equals(this.type, this.resultType)) {
            return Either.right(new Type.FieldNotFoundException("TaggedChoiceType key type change is unsupported."));
         }

         // ===== 修改：显式指定 capChoice 的 V 类型 =====
         return Either.left(this.<Object>capChoice(ftChoiceType));
      }

      // ===== 修改：显式指定 TypedOptic 类型参数 =====
      private <V> TypedOptic<Pair<FT, V>, Pair<FT, V>, FT, FT> capChoice(TaggedChoice.TaggedChoiceType<FT> choiceType) {
         @SuppressWarnings("unchecked")
         Type<Pair<FT, V>> pairType = (Type<Pair<FT, V>>) (Type<?>) choiceType;
         return new TypedOptic<>(Cartesian.Mu.TYPE_TOKEN, pairType, pairType, this.type, this.type, Optics.proj1());
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            FieldFinder.Matcher<?, ?> matcher = (FieldFinder.Matcher<?, ?>)o;
            return Objects.equals(this.resultType, matcher.resultType) && Objects.equals(this.name, matcher.name) && Objects.equals(this.type, matcher.type);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.resultType.hashCode();
         result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
         return 31 * result + this.type.hashCode();
      }
   }
}