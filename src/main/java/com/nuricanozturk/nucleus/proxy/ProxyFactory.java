package com.nuricanozturk.nucleus.proxy;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.nuricanozturk.nucleus.interceptor.DispatcherInterceptor;
import com.nuricanozturk.nucleus.interceptor.InterceptorFactory;
import com.nuricanozturk.nucleus.interceptor.NucleusInterceptor;
import java.lang.reflect.Method;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProxyFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFactory.class);

  private ProxyFactory() {
    LOGGER.debug("ProxyFactory created");
  }

  public static Object createProxyIfNeeded(
      final @NotNull Object target, final @NotNull Class<?> targetClass) {
    final List<NucleusInterceptor> interceptors =
        InterceptorFactory.createInterceptorChain(target, targetClass);

    if (!hasAnyInterceptor(interceptors, targetClass)) {
      return target;
    }

    final DispatcherInterceptor dispatcher = new DispatcherInterceptor(interceptors);

    try (final DynamicType.Unloaded<?> buddy =
        new ByteBuddy()
            .subclass(targetClass)
            .method(not(isDeclaredBy(Object.class)))
            .intercept(MethodDelegation.to(dispatcher).andThen(SuperMethodCall.INSTANCE))
            .make()) {

      final Class<?> proxyClass =
          buddy
              .load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
              .getLoaded();

      return proxyClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Proxy creation failed: " + targetClass.getName(), e);
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
