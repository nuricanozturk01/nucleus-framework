package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.retry.RetryInterceptor;
import com.nuricanozturk.nucleus.annotation.retry.Retryable;
import java.lang.reflect.Method;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public final class ProxyFactory {

  private ProxyFactory() {
    // Utility class; prevent instantiation
  }

  public static Object createProxyIfNeeded(final Object target, final Class<?> targetClass) {
    boolean hasRetryable = false;

    for (final Method method : targetClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Retryable.class)) {
        hasRetryable = true;
        break;
      }
    }

    if (!hasRetryable) {
      return target;
    }

    try {
      return new ByteBuddy()
          .subclass(targetClass)
          .method(ElementMatchers.isAnnotatedWith(Retryable.class))
          .intercept(MethodDelegation.to(new RetryInterceptor(target, targetClass)))
          .make()
          .load(targetClass.getClassLoader())
          .getLoaded()
          .getDeclaredConstructor()
          .newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create proxy for: " + targetClass.getName(), e);
    }
  }
}
