package com.nuricanozturk.nucleus.interceptor;

import com.nuricanozturk.nucleus.annotation.retry.Recover;
import com.nuricanozturk.nucleus.annotation.retry.Retryable;
import com.nuricanozturk.nucleus.proxy.NucleusInterceptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;

public final class RetryInterceptor implements NucleusInterceptor {
  private final @NotNull Object targetObject;
  private final @NotNull Class<?> targetClass;

  public RetryInterceptor(final @NotNull Object targetObject, final @NotNull Class<?> targetClass) {
    this.targetObject = targetObject;
    this.targetClass = targetClass;
  }

  @Override
  public Object invoke(
      final @This Object proxy, final @Origin Method method, final @AllArguments Object[] args)
      throws Throwable {
    final Retryable retryable = method.getAnnotation(Retryable.class);

    final Method realMethod =
        this.targetObject.getClass().getMethod(method.getName(), method.getParameterTypes());

    if (retryable == null) {
      return realMethod.invoke(this.targetObject, args);
    }

    final int maxAttempts = retryable.maxAttempts();

    int attempts = 0;

    while (true) {
      try {
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

  @Override
  public boolean supports(final @NotNull Method method) {
    return method.isAnnotationPresent(Retryable.class);
  }

  private Object recover(
      final @NotNull Throwable ex,
      final @NotNull String recoverMethodName,
      final @NotNull Object[] args)
      throws Throwable {
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
