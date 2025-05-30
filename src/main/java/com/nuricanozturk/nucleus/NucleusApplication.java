package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.ComponentScan;
import com.nuricanozturk.nucleus.annotation.EntryPoint;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NucleusApplication.class);

  private NucleusApplication() {
    LOGGER.debug("NucleusFramework created");
  }

  public static void run(final @NotNull Class<?> clazz) {
    final ComponentScan scan = clazz.getAnnotation(ComponentScan.class);

    if (scan == null) {
      throw new RuntimeException(clazz.getName() + " is not annotated with @ComponentScan");
    }

    for (final String basePackage : scan.basePackages()) {
      final Set<Class<?>> components = ClassScanner.findComponentClasses(basePackage);

      components.forEach(ComponentResolver::createInstance);

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
