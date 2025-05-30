package com.nuricanozturk.nucleus.interceptor;

import com.nuricanozturk.nucleus.proxy.NucleusInterceptor;
import java.lang.reflect.Method;
import java.util.List;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;

public class DispatcherInterceptor {
  private final @NotNull List<NucleusInterceptor> interceptors;

  public DispatcherInterceptor(final @NotNull List<NucleusInterceptor> interceptors) {
    this.interceptors = interceptors;
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
