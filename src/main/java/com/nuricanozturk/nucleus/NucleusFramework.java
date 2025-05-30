package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.core.ComponentScan;
import com.nuricanozturk.nucleus.annotation.core.EntryPoint;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public final class NucleusFramework {
  public static void run(final @NotNull Class<?> clazz) {
    final ComponentScan scan = clazz.getAnnotation(ComponentScan.class);

    if (scan == null) {
      throw new RuntimeException(clazz.getName() + " is not annotated with @ComponentScan");
    }

    for (final String basePackage : scan.basePackages()) {
      final Set<Class<?>> components = ClassScanner.findComponentClasses(basePackage);

      for (final Class<?> componentClass : components) {
        ComponentResolver.createInstance(componentClass);
      }

      final Class<?> entryPointClass = getEntryPointClass(components);
      final Object entryPointInstance = NucleusContext.getBean(entryPointClass);
      final String methodName = entryPointClass.getAnnotation(EntryPoint.class).value();

      try {
        entryPointClass.getMethod(methodName).invoke(entryPointInstance);
      } catch (final Exception e) {
        throw new RuntimeException(
            "Failed to invoke '" + methodName + "' on entry point: " + entryPointClass.getName(),
            e);
      }
    }
  }

  private static @NotNull Class<?> getEntryPointClass(final @NotNull Set<Class<?>> components) {
    return components.stream()
        .filter(c -> c.isAnnotationPresent(EntryPoint.class))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No @EntryPoint class found."));
  }
}
