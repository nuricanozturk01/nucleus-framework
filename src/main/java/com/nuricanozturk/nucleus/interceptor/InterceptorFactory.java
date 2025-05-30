package com.nuricanozturk.nucleus.interceptor;

import com.nuricanozturk.nucleus.interceptor.internal.RepeatInterceptor;
import com.nuricanozturk.nucleus.interceptor.internal.RetryInterceptor;
import com.nuricanozturk.nucleus.interceptor.internal.ScheduledInterceptor;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class InterceptorFactory {
  public static List<NucleusInterceptor> createInterceptorChain(
      final @NotNull Object target, final @NotNull Class<?> targetClass) {

    return List.of(
        new RetryInterceptor(target, targetClass),
        new RepeatInterceptor(target, targetClass),
        new ScheduledInterceptor(target, targetClass));
  }
}
