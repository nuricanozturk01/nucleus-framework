package com.nuricanozturk.nucleus.di;

import com.nuricanozturk.nucleus.annotation.Autowired;
import com.nuricanozturk.nucleus.annotation.Component;
import com.nuricanozturk.nucleus.annotation.Qualifier;
import com.nuricanozturk.nucleus.proxy.ProxyFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Set;
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
      injectAutowiredFields(rawInstance, clazz);
      final Object proxiedInstance = ProxyFactory.createProxyIfNeeded(rawInstance, clazz);

      NucleusContext.registerBean(name, proxiedInstance);

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

    NucleusContext.registerBean(depName, defaultDep);
    return defaultDep;
  }

  private static Object qualifierMarkedObject(
      final @NotNull Parameter parameter, final @NotNull Class<?> dependencyType) {
    final String qualifierName = parameter.getAnnotation(Qualifier.class).value();

    if (NucleusContext.contains(qualifierName)) {
      return NucleusContext.getBean(qualifierName, dependencyType);
    }

    final Set<Class<?>> candidates =
        ClassScanner.findComponentClasses(dependencyType.getPackageName());

    final Class<?> implClass =
        candidates.stream()
            .filter(c -> isComponent(c, qualifierName, dependencyType))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "No component found for qualifier '"
                            + qualifierName
                            + "' and type: "
                            + dependencyType.getName()));

    final Object qualifiedDep = createInstance(implClass);
    NucleusContext.registerBean(qualifierName, qualifiedDep);
    return qualifiedDep;
  }

  private static boolean isComponent(
      final @NotNull Class<?> c,
      final @NotNull String qualifierName,
      final @NotNull Class<?> dependencyType) {
    final Component comp = c.getAnnotation(Component.class);
    return comp != null && qualifierName.equals(comp.value()) && dependencyType.isAssignableFrom(c);
  }

  private static String resolveBeanName(final Class<?> clazz) {
    final Component component = clazz.getAnnotation(Component.class);
    return (component != null && !component.value().isEmpty())
        ? component.value()
        : clazz.getSimpleName();
  }

  private static void injectAutowiredFields(final Object instance, final Class<?> clazz) {
    for (final Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Autowired.class)) {
        final Class<?> dependencyType = field.getType();
        final Object dependency;

        if (field.isAnnotationPresent(Qualifier.class)) {
          final String qualifierName = field.getAnnotation(Qualifier.class).value();
          if (NucleusContext.contains(qualifierName)) {
            dependency = NucleusContext.getBean(qualifierName, dependencyType);
          } else {

            final Set<Class<?>> candidates =
                ClassScanner.findComponentClasses(dependencyType.getPackageName());

            final Class<?> implClass =
                candidates.stream()
                    .filter(c -> isComponent(c, qualifierName, dependencyType))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new RuntimeException(
                                "No component found for qualifier '"
                                    + qualifierName
                                    + "' and type: "
                                    + dependencyType.getName()));

            dependency = createInstance(implClass);
            NucleusContext.registerBean(qualifierName, dependency);
          }
        } else {
          dependency = nonQualifiedObject(dependencyType);
        }

        try {
          field.setAccessible(true);
          field.set(instance, dependency);
        } catch (final IllegalAccessException e) {
          throw new RuntimeException("Failed to inject field: " + field.getName(), e);
        }
      }
    }
  }
}
