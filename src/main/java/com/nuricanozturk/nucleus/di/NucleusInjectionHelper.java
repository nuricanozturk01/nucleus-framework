package com.nuricanozturk.nucleus.di;

import com.nuricanozturk.nucleus.annotation.Autowired;
import java.lang.reflect.Field;

public final class NucleusInjectionHelper {
  public static void inject(final Object instance) {
    final Class<?> clazz = instance.getClass();

    for (final Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Autowired.class)) {
        field.setAccessible(true);
        final Class<?> type = field.getType();
        final Object dep =
            NucleusContext.contains(type)
                ? NucleusContext.getBean(type)
                : ComponentResolver.createInstance(type);
        try {
          field.set(instance, dep);
        } catch (final IllegalAccessException e) {
          throw new RuntimeException("Autowired field injection failed: " + field.getName(), e);
        }
      }
    }
  }
}
