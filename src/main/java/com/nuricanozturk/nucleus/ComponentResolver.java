package com.nuricanozturk.nucleus;

import com.nuricanozturk.nucleus.annotation.Component;
import com.nuricanozturk.nucleus.annotation.Qualifier;
import com.nuricanozturk.nucleus.proxy.ProxyFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.jetbrains.annotations.NotNull;

public class ComponentResolver {
  public static Object createInstance(final Class<?> clazz) {
    final String name = resolveBeanName(clazz);

    if (NucleusContext.contains(name)) {
      return NucleusContext.getBean(name, clazz);
    }

    try {
      final Constructor<?> ctor = clazz.getConstructors()[0];
      final Parameter[] parameters = ctor.getParameters();
      final Object[] dependencies = new Object[parameters.length];

      for (int i = 0; i < parameters.length; i++) {
        final Parameter parameter = parameters[i];
        final Class<?> dependencyType = parameter.getType();

        final Object dependency =
            parameter.isAnnotationPresent(Qualifier.class)
                ? qualifierMarkedObject(parameter, dependencyType)
                : nonQualifiedObject(dependencyType);

        dependencies[i] = dependency;
      }

      final Object rawInstance = ctor.newInstance(dependencies);
      final Object proxiedInstance = ProxyFactory.createProxyIfNeeded(rawInstance, clazz);

      NucleusContext.registerBean(clazz, name, proxiedInstance);

      return proxiedInstance;
    } catch (final Exception e) {
      throw new RuntimeException("Component could not be created: " + clazz.getName(), e);
    }
  }

  private static Object nonQualifiedObject(final @NotNull Class<?> dependencyType) {
    if (NucleusContext.contains(dependencyType)) {
      return NucleusContext.getBean(dependencyType);
    }

    final Object defaultDep = createInstance(dependencyType);
    final String depName = resolveBeanName(dependencyType);

    NucleusContext.registerBean(dependencyType, depName, defaultDep);
    return defaultDep;
  }

  private static Object qualifierMarkedObject(
      final @NotNull Parameter parameter, final @NotNull Class<?> dependencyType) {
    final String qualifierName = parameter.getAnnotation(Qualifier.class).value();

    if (NucleusContext.contains(qualifierName)) {
      return NucleusContext.getBean(qualifierName, dependencyType);
    }

    final Object qualifiedDep = createInstance(dependencyType);
    NucleusContext.registerBean(dependencyType, qualifierName, qualifiedDep);

    return qualifiedDep;
  }

  private static String resolveBeanName(final Class<?> clazz) {
    final Component component = clazz.getAnnotation(Component.class);
    return (component != null && !component.value().isEmpty())
        ? component.value()
        : clazz.getSimpleName();
  }
}
