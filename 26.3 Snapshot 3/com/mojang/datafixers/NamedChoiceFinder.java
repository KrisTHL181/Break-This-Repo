package com.mojang.datafixers;

import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Tag;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Either;
import java.util.Objects;

final class NamedChoiceFinder<FT> implements OpticFinder<FT> {
   private final String name;
   private final Type<FT> type;

   public NamedChoiceFinder(String name, Type<FT> type) {
      this.name = name;
      this.type = type;
   }

   @Override
   public Type<FT> type() {
      return this.type;
   }

   @Override
   public <A, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findType(Type<A> containerType, Type<FR> resultType, boolean recurse) {
      return containerType.findTypeCached(this.type, resultType, new NamedChoiceFinder.Matcher<>(this.name, this.type, resultType), recurse);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof NamedChoiceFinder<?> that) ? false : Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type);
      }
   }

   @Override
   public int hashCode() {
      int result = this.name.hashCode();
      return 31 * result + this.type.hashCode();
   }

   private static class Matcher<FT, FR> implements Type.TypeMatcher<FT, FR> {
      private final Type<FR> resultType;
      private final String name;
      private final Type<FT> type;

      public Matcher(String name, Type<FT> type, Type<FR> resultType) {
         this.resultType = resultType;
         this.name = name;
         this.type = type;
      }

      @Override
      public <S> Either<TypedOptic<S, ?, FT, FR>, Type.FieldNotFoundException> match(Type<S> targetType) {
         if (targetType instanceof TaggedChoice.TaggedChoiceType<?> choiceType) {
            Type<?> elementType = choiceType.types().get(this.name);
            if (elementType != null) {
               if (!Objects.equals(this.type, elementType)) {
                  return Either.right(
                     new Type.FieldNotFoundException(
                        String.format("Type error for choice type \"%s\": expected type: %s, actual type: %s)", this.name, targetType, elementType)
                     )
                  );
               } else {
                  // ===== 修改：强制转换为 TaggedChoiceType<String>，因为键类型是 String =====
                  @SuppressWarnings("unchecked")
                  TaggedChoice.TaggedChoiceType<String> stringChoiceType = (TaggedChoice.TaggedChoiceType<String>) choiceType;
                  return Either.left(TypedOptic.tagged(stringChoiceType, this.name, this.type, this.resultType));
               }
            } else {
               return Either.right(new Type.Continue());
            }
         } else {
            return targetType instanceof Tag.TagType ? Either.right(new Type.FieldNotFoundException("in tag")) : Either.right(new Type.Continue());
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            NamedChoiceFinder.Matcher<?, ?> matcher = (NamedChoiceFinder.Matcher<?, ?>)o;
            return Objects.equals(this.resultType, matcher.resultType) && Objects.equals(this.name, matcher.name) && Objects.equals(this.type, matcher.type);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.resultType.hashCode();
         result = 31 * result + this.name.hashCode();
         return 31 * result + this.type.hashCode();
      }
   }
}