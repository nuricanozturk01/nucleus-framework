package com.nuricanozturk.nucleus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

public class NucleusContext {
  private static final ConcurrentMap<Class<?>, Object> NUCLEUS_BEANS = new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, Object> NAMED_NUCLEUS_BEANS =
      new ConcurrentHashMap<>();

  public static void registerBean(
      final @NotNull Class<?> originalType,
      final @NotNull String name,
      final @NotNull Object instance) {
    /*if (NAMED_NUCLEUS_BEANS.containsKey(name) || NUCLEUS_BEANS.containsKey(originalType)) {
      throw new RuntimeException(
          "Bean already registered: " + name + " / " + originalType.getName());
    }*/

    NAMED_NUCLEUS_BEANS.put(name, instance);
    NUCLEUS_BEANS.put(originalType, instance);
  }

  public static <T> T getBean(final @NotNull Class<T> type) {
    return type.cast(NUCLEUS_BEANS.get(type));
  }

  public static <T> T getBean(final @NotNull String name, final @NotNull Class<T> type) {
    return type.cast(NAMED_NUCLEUS_BEANS.get(name));
  }

  public static boolean contains(final @NotNull Class<?> type) {
    return NUCLEUS_BEANS.containsKey(type);
  }

  public static boolean contains(final @NotNull String name) {
    return NAMED_NUCLEUS_BEANS.containsKey(name);
  }
}
