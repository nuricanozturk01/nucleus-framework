package com.nuricanozturk.nucleus.annotation.retry;

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;

public final class RetryInterceptor {
  private final @NotNull Object target;
  private final @NotNull Class<?> targetClass;

  public RetryInterceptor(final @NotNull Object target, final @NotNull Class<?> targetClass) {
    this.target = target;
    this.targetClass = targetClass;
  }

  @RuntimeType
  public Object intercept(
      final @This Object proxy, final @AllArguments Object[] args, final @Origin Method method)
      throws Throwable {
    final Retryable retryable = method.getAnnotation(Retryable.class);
    final Method realMethod =
        this.target.getClass().getMethod(method.getName(), method.getParameterTypes());

    if (retryable == null) {
      return realMethod.invoke(this.target, args);
    }

    final int maxAttempts = retryable.maxAttempts();
    int attempts = 0;

    while (true) {
      try {
        return realMethod.invoke(this.target, args);
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
        if (attempts >= maxAttempts) {
          final String recoverMethod = retryable.recover();
          if (recoverMethod.isEmpty()) {
            throw actual;
          }
          return this.recover(actual, recoverMethod, args);
        }
      }
    }
  }

  private Object recover(final Throwable ex, final String recoverMethodName, final Object[] args)
      throws Throwable {
    final Method[] methods = this.targetClass.getDeclaredMethods();

    for (final Method recoverMethod : methods) {
      if (recoverMethod.isAnnotationPresent(Recover.class)) {
        if (recoverMethod.getName().equals(recoverMethodName)
            || recoverMethod.getName().equals("recover")
            || recoverMethod.getName().equals("recover" + capitalize(recoverMethodName))) {

          final Class<?>[] parameterTypes = recoverMethod.getParameterTypes();
          if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(ex.getClass())) {
            final Object[] recoverArgs = new Object[parameterTypes.length];
            recoverArgs[0] = ex;

            System.arraycopy(
                args, 0, recoverArgs, 1, Math.min(args.length, parameterTypes.length - 1));
            return recoverMethod.invoke(this.target, recoverArgs);
          }
        }
      }
    }

    throw ex;
  }
}
