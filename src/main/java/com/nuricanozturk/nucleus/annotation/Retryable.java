package com.nuricanozturk.nucleus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Retryable {
  Class<? extends Throwable>[] retryFor() default {Exception.class};

  int maxAttempts() default 3;

  BackOff backOff() default @BackOff;

  String recover() default "recover";
}
