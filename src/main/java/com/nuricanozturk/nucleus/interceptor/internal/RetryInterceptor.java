package com.nuricanozturk.nucleus.interceptor.internal;

import com.nuricanozturk.nucleus.annotation.Recover;
import com.nuricanozturk.nucleus.annotation.Retryable;
import com.nuricanozturk.nucleus.interceptor.AbstractInterceptor;
import com.nuricanozturk.nucleus.interceptor.NucleusInterceptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RetryInterceptor extends AbstractInterceptor implements NucleusInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(RetryInterceptor.class);

  public RetryInterceptor(final @NotNull Object targetObject, final @NotNull Class<?> targetClass) {
    super(targetObject, targetClass);

    LOGGER.debug("RetryInterceptor created for class '{}'", targetClass.getName());
  }

  @Override
  public Object invoke(
      final @This Object proxy, final @Origin Method method, final @AllArguments Object[] args)
      throws Throwable {
    final Retryable retryable = method.getAnnotation(Retryable.class);

    final Method realMethod =
        this.targetObject.getClass().getMethod(method.getName(), method.getParameterTypes());

    if (retryable == null) {
      LOGGER.debug("Method '{}' is not annotated with @Retryable", method.getName());
      return realMethod.invoke(this.targetObject, args);
    }

    final int maxAttempts = retryable.maxAttempts();
    LOGGER.debug("> {} with {} attempts.", method.getName(), maxAttempts);
    int attempts = 0;

    while (true) {
      try {
        LOGGER.debug("Attempt {}/{}", attempts + 1, maxAttempts);
        return realMethod.invoke(this.targetObject, args);
      } catch (final Throwable ex) {
        final Throwable actual =
            (ex instanceof InvocationTargetException && ex.getCause() != null) ? ex.getCause() : ex;

        boolean isRetryable = false;

        for (final Class<?> retryClass : retryable.retryFor()) {
          if (retryClass.isAssignableFrom(actual.getClass())) {
            isRetryable = true;
            break;
          }
        }

        if (!isRetryable) {
          throw actual;
        }

        attempts++;

        final var backoff = retryable.backOff();
        long delay = backoff.delay();
        final double multiplier = backoff.multiplier();

        LOGGER.debug(
            "Attempt {}/{} failed. with delay is {}, multiplier: {}",
            attempts,
            maxAttempts,
            delay,
            multiplier);

        if (attempts >= maxAttempts) {
          final String recoverMethod = retryable.recover();
          if (recoverMethod.isEmpty()) {
            LOGGER.debug("No @Recover method found for method '{}'", method.getName());
            throw actual;
          }

          return this.recover(actual, recoverMethod, args);
        }

        if (delay > 0) {
          try {
            Thread.sleep(delay);
          } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw actual;
          }
          delay = (long) (delay * multiplier);
        }
      }
    }
  }

  @Override
  public boolean supports(final @NotNull Method method) {
    return method.isAnnotationPresent(Retryable.class);
  }

  private Object recover(
      final @NotNull Throwable ex,
      final @NotNull String recoverMethodName,
      final @NotNull Object[] args)
      throws Throwable {
    LOGGER.debug(
        "Recovering method running'{}' with exception '{}'", recoverMethodName, ex.getMessage());
    final Method[] methods = this.targetClass.getDeclaredMethods();

    for (final Method recoverMethod : methods) {
      if (!recoverMethod.isAnnotationPresent(Recover.class)
          || !recoverMethod.getName().equals(recoverMethodName)) {
        continue;
      }

      final Class<?>[] parameterTypes = recoverMethod.getParameterTypes();

      if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(ex.getClass())) {
        final Object[] recoverArgs = new Object[parameterTypes.length];
        recoverArgs[0] = ex;

        System.arraycopy(args, 0, recoverArgs, 1, Math.min(args.length, parameterTypes.length - 1));

        return recoverMethod.invoke(this.targetObject, recoverArgs);
      }
    }

    throw ex;
  }
}
