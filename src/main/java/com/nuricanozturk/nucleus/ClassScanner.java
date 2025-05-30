package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.Component;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

public final class ClassScanner {

  public static @NotNull Set<Class<?>> findComponentClasses(final @NotNull String basePackage) {
    final Reflections reflections = new Reflections(basePackage);

    return reflections.getTypesAnnotatedWith(Component.class);
  }
}
