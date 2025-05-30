package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.ComponentScan;
import com.nuricanozturk.nucleus.annotation.EntryPoint;
import com.nuricanozturk.nucleus.di.ClassScanner;
import com.nuricanozturk.nucleus.di.ComponentResolver;
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
    final String[] basePackages;

    final ComponentScan scan = clazz.getAnnotation(ComponentScan.class);

    if (scan != null) {
      basePackages = scan.basePackages();
    } else {
      basePackages = new String[] {clazz.getPackage().getName()};
    }

    for (final String basePackage : basePackages) {
      final Set<Class<?>> components = ClassScanner.findComponentClasses(basePackage);
      components.forEach(ComponentResolver::createInstance);
    }

    if (clazz.isAnnotationPresent(EntryPoint.class)) {
      final Object entryPointInstance = ComponentResolver.createInstance(clazz);
      final String methodName = clazz.getAnnotation(EntryPoint.class).value();
      final String methodToCall = methodName.isEmpty() ? "run" : methodName;

      try {
        clazz.getMethod(methodToCall).invoke(entryPointInstance);
      } catch (final Exception e) {
        throw new RuntimeException(
            "Failed to invoke '" + methodToCall + "' on entry point: " + clazz.getName(), e);
      }
    }
  }
}
