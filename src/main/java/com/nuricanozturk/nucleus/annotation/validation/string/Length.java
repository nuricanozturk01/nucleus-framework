package com.nuricanozturk.nucleus.annotation.validation.string;

public @interface Length {
  int min() default 0;

  int max() default Integer.MAX_VALUE;
}
