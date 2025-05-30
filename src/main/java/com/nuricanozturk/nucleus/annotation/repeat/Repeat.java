package com.nuricanozturk.nucleus.annotation.repeat;

public @interface Repeat {
  int value() default 1;

  long delay() default 0;
}
