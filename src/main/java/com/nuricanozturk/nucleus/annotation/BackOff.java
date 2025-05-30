package com.nuricanozturk.nucleus.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface BackOff {
  long delay() default 0;

  double multiplier() default 1.0;
}
