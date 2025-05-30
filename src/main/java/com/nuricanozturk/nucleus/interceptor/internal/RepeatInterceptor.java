package com.nuricanozturk.nucleus.interceptor.internal;

import com.nuricanozturk.nucleus.annotation.Repeat;
import com.nuricanozturk.nucleus.interceptor.AbstractInterceptor;
import com.nuricanozturk.nucleus.interceptor.NucleusInterceptor;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RepeatInterceptor extends AbstractInterceptor implements NucleusInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepeatInterceptor.class);

  public RepeatInterceptor(
      final @NotNull Object targetObject, final @NotNull Class<?> targetClass) {
    super(targetObject, targetClass);

    LOGGER.debug("RepeatInterceptor created for class '{}'", targetClass.getName());
  }

  @Override
  public boolean supports(final @NotNull Method method) {
    return method.isAnnotationPresent(Repeat.class);
  }

  @Override
  public Object invoke(
      final @This @NotNull Object proxy,
      final @Origin @NotNull Method method,
      final @AllArguments @NotNull Object[] args)
      throws Throwable {
    final Repeat repeat = method.getAnnotation(Repeat.class);
    final int times = repeat.value();
    final long delay = repeat.delay();

    LOGGER.debug("Repeating method '{}' {} times with delay {} ms", method.getName(), times, delay);

    final Method realMethod =
        this.targetClass.getMethod(method.getName(), method.getParameterTypes());

    Object result = null;

    for (int i = 0; i < times; i++) {
      result = realMethod.invoke(this.targetObject, args);

      if (delay > 0 && i < times - 1) {
        Thread.sleep(delay);
      }
    }

    LOGGER.debug("Method '{}' finished", method.getName());
    return result;
  }
}
