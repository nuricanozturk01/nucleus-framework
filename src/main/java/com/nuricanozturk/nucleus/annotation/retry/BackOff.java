package com.nuricanozturk.nucleus.annotation.retry;

public @interface BackOff {
  int delay() default 2;

  int multiplier() default 1;
}
