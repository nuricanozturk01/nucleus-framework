package com.nuricanozturk.nucleus.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherInterceptor.class);
  private final @NotNull List<NucleusInterceptor> interceptors;

  public DispatcherInterceptor(final @NotNull List<NucleusInterceptor> interceptors) {
    this.interceptors = interceptors;

    LOGGER.debug("Dispatcher Interceptor created for {} interceptors", interceptors.size());
  }

  @RuntimeType
  public Object dispatch(
      final @This Object proxy, @Origin final Method method, final @AllArguments Object[] args)
      throws Throwable {
    for (final NucleusInterceptor interceptor : this.interceptors) {
      if (interceptor.supports(method)) {
        return interceptor.invoke(proxy, method, args);
      }
    }

    return method.invoke(proxy, args);
  }
}
