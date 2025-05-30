package com.nuricanozturk.nucleus;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.nuricanozturk.nucleus.interceptor.DispatcherInterceptor;
import com.nuricanozturk.nucleus.interceptor.RetryInterceptor;
import com.nuricanozturk.nucleus.proxy.NucleusInterceptor;
import java.lang.reflect.Method;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import org.jetbrains.annotations.NotNull;

public final class ProxyFactory {

  private ProxyFactory() {
    // Utility class
  }

  public static Object createProxyIfNeeded(
      final @NotNull Object target, final @NotNull Class<?> targetClass) {

    final List<NucleusInterceptor> interceptors =
        List.of(new RetryInterceptor(target, targetClass));

    if (!hasAnyInterceptor(interceptors, targetClass)) {
      return target;
    }

    final DispatcherInterceptor dispatcher = new DispatcherInterceptor(interceptors);

    try (final DynamicType.Unloaded<?> buddy =
        new ByteBuddy()
            .subclass(targetClass)
            .method(not(isDeclaredBy(Object.class)))
            .intercept(MethodDelegation.to(dispatcher))
            .make()) {

      return buddy
          .load(targetClass.getClassLoader())
          .getLoaded()
          .getDeclaredConstructor()
          .newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create proxy for: " + targetClass.getName(), e);
    }
  }

  private static boolean hasAnyInterceptor(
      final @NotNull List<NucleusInterceptor> interceptors, final @NotNull Class<?> targetClass) {
    for (final Method method : targetClass.getDeclaredMethods()) {
      for (final NucleusInterceptor interceptor : interceptors) {
        if (interceptor.supports(method)) {
          return true;
        }
      }
    }

    return false;
  }
}
