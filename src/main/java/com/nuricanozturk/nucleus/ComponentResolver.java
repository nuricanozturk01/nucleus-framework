package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.core.Component;
import java.lang.reflect.Constructor;

public class ComponentResolver {

  public static Object createInstance(final Class<?> clazz) {
    final String name = resolveBeanName(clazz);

    if (NucleusContext.contains(name)) {
      return NucleusContext.getBean(name, clazz);
    }

    try {
      final Constructor<?> constructor = clazz.getConstructors()[0];
      final Class<?>[] parameterTypes = constructor.getParameterTypes();
      final Object[] dependencies = new Object[parameterTypes.length];

      for (int i = 0; i < parameterTypes.length; i++) {
        final Class<?> dependencyType = parameterTypes[i];

        if (!NucleusContext.contains(dependencyType)) {
          final Object depInstance = createInstance(dependencyType);
          final String depName = resolveBeanName(dependencyType);
          NucleusContext.registerBean(dependencyType, depName, depInstance);
        }

        dependencies[i] = NucleusContext.getBean(dependencyType);
      }

      final Object rawInstance = constructor.newInstance(dependencies);

      final Object proxiedInstance = ProxyFactory.createProxyIfNeeded(rawInstance, clazz);

      // default name should be method name
      NucleusContext.registerBean(clazz, name, proxiedInstance);

      return proxiedInstance;

    } catch (final Exception e) {
      throw new RuntimeException("Component could not be created: " + clazz.getName(), e);
    }
  }

  private static String resolveBeanName(final Class<?> clazz) {
    final Component component = clazz.getAnnotation(Component.class);
    return (component != null && !component.value().isEmpty())
        ? component.value()
        : clazz.getSimpleName();
  }
}
