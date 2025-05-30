package com.nuricanozturk.nucleus.di;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

public class NucleusContext {
  private static final ConcurrentMap<String, Object> BEAN_REGISTRY = new ConcurrentHashMap<>();

  public static void registerBean(final @NotNull String name, final @NotNull Object instance) {
    BEAN_REGISTRY.putIfAbsent(name, instance);
  }

  public static <T> T getBean(final @NotNull String name, final @NotNull Class<T> type) {
    final Object bean = BEAN_REGISTRY.get(name);
    if (!type.isInstance(bean)) {
      throw new RuntimeException("No bean named " + name + " of type " + type.getName());
    }
    return type.cast(bean);
  }

  public static <T> T getBean(final @NotNull Class<T> type) {
    return BEAN_REGISTRY.values().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No bean of type " + type.getName() + " found"));
  }

  public static boolean contains(final @NotNull String name) {
    return BEAN_REGISTRY.containsKey(name);
  }

  public static boolean contains(final @NotNull Class<?> type) {
    return BEAN_REGISTRY.values().stream().anyMatch(type::isInstance);
  }
}
