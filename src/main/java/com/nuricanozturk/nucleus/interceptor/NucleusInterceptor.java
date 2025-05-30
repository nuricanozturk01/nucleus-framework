package com.nuricanozturk.nucleus.interceptor;

import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;

public interface NucleusInterceptor {
  boolean supports(@NotNull Method method);

  Object invoke(@This Object proxy, @Origin Method method, @AllArguments Object[] args)
      throws Throwable;
}
