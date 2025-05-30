package com.nuricanozturk.nucleus.interceptor;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractInterceptor {
  protected final @NotNull Object targetObject;
  protected final @NotNull Class<?> targetClass;

  public AbstractInterceptor(
      final @NotNull Object targetObject, final @NotNull Class<?> targetClass) {
    this.targetObject = targetObject;
    this.targetClass = targetClass;
  }
}
