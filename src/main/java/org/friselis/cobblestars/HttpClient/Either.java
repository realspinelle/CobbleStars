package org.friselis.cobblestars.HttpClient;

import java.util.function.Consumer;
import java.util.function.Function;

public final class Either<T1, T2> {
   private final Object value;
   private final boolean isLeft;

   public static <T1, T2> Either<T1, T2> left(T1 value) {
      return new Either(value, true);
   }

   public static <T1, T2> Either<T1, T2> right(T2 value) {
      return new Either(value, false);
   }

   private Either(Object value, boolean isLeft) {
      this.value = value;
      this.isLeft = isLeft;
   }

   public boolean isLeft() {
      return this.isLeft;
   }

   public boolean isRight() {
      return !this.isLeft;
   }

   public T1 left() {
      if (!this.isLeft) {
         throw new IllegalStateException("This Either does not represent a left value");
      } else {
         return (T1) this.value;
      }
   }

   public T2 right() {
      if (this.isLeft) {
         throw new IllegalStateException("This Either does not represent a right value");
      } else {
         return (T2) this.value;
      }
   }

   public void apply(Consumer<? super T1> lFunc, Consumer<? super T2> rFunc) {
      if (this.isLeft) {
         relax(lFunc).accept(this.value);
      } else {
         relax(rFunc).accept(this.value);
      }

   }

   public <T3> T3 fold(Function<? super T1, ? extends T3> lFunc, Function<? super T2, ? extends T3> rFunc) {
      return this.isLeft ? relax(lFunc).apply(this.value) : relax(rFunc).apply(this.value);
   }

   public <T3> Either<T3, T2> mapLeft(Function<? super T1, ? extends T3> func) {
      return this.isLeft ? left(relax(func).apply(this.value)) : (Either<T3, T2>) right(this.value);
   }

   public <T3> Either<T1, T3> mapRight(Function<? super T2, ? extends T3> func) {
      return this.isLeft ? (Either<T1, T3>) left(this.value) : right(relax(func).apply(this.value));
   }

   private static Consumer<Object> relax(Consumer<?> func) {
      return (Consumer<Object>) func;
   }

   private static <T> Function<Object, T> relax(Function<?, T> func) {
      return (Function<Object, T>) func;
   }
}
