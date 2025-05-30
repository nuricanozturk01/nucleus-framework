# Nucleus Framework

**Nucleus Framework** was developed during **Repsy’s 2-Day Innovation Day**, where employees were encouraged
to create and
ship their own projects. The result is a lightweight Dependency Injection (DI) framework built with **core Java**,
inspired by Spring but designed to be simple and minimal.

Nucleus currently supports **constructor-based injection only**, along with a few core annotations to reduce boilerplate
and automate common patterns. Due to the limited Innovation Day timeline, the feature set is intentionally minimal but
well-structured for future growth.

---

## ✨ Features

- Constructor-based dependency injection
- ByteBuddy-based proxy and method interception

---

## 📦 Supported Annotations

### Core Annotations

These annotations mimic common Spring-style behaviors:

- `@Component` – Marks a class as a managed component.
- `@ComponentScan` – Specifies which base packages to scan for components.
- `@NucleusFramework` – The entry annotation to bootstrap the framework (implicitly includes `@Component`).
- `@Qualifier` – For resolving ambiguities when multiple beans of the same type exist.
- `@EntryPoint` – Defines the entry point method to execute after DI is complete. Defaults to calling `run()`, but a
  custom method name can be specified.

### Method Interceptors

#### `@Retryable`

Automatically retries a method on failure based on configuration.

```java

@Retryable(
        retryFor = {IOException.class},
        maxAttempts = 3,
        backOff = @BackOff(delay = 1000, multiplier = 2.0),
        recover = "recoverMethod"
)
public void unstableOperation() {
  // ...
}

@Recover
public void recoverMethod(IOException e) {
  // recovery logic
}
```

**Annotation Definition:**:

```java

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Retryable {
  Class<? extends Throwable>[] retryFor() default {Exception.class};

  int maxAttempts() default 3;

  BackOff backOff() default @BackOff;

  String recover() default "recover";
}
```

```java

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface BackOff {
  long delay() default 0;

  double multiplier() default 1.0;
}
```

#### `@Repeat`

Executes a method multiple times with optional delay.

```java

@Repeat(value = 5, delay = 2000)
public void pollData() {
  // ...
}
```

**Annotation Definition:**:

```java

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repeat {
  int value() default 3;

  long delay() default 1000L;
}
```

#### `@Scheduled`

Schedules a method to run on a UNIX-style cron schedule.

```java

@Scheduled(cron = "0 */10 * * * *")
public void periodicCleanup() {
  // ...
}
```

**Annotation Definition:**:

```java

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {
  String cron() default "0 */10 * * * *";
}
```

## Quick Start

```java

@NucleusFramework
@EntryPoint("start")
@Component
public class Main {

  public static void main(final String[] args) {
    NucleusApplication.run(Main.class);
  }

  public void start() {
    System.out.println("Application started");
  }
}
```

## ✅ What Happens When You Run the `Main` Class?

When you run the `Main` class, the framework will:

- 🔍 Scan the package for all components
- 🧩 Perform constructor-based dependency injection
- 🚀 Execute the specified `@EntryPoint` method automatically

---

## ⚙️ Architecture Overview

- **`NucleusApplication`**: The main bootstrap class that triggers scanning and wiring.
- **`ComponentResolver`**: Resolves constructor dependencies and creates component instances, applying proxies where
  needed.
- **`NucleusContext`**: A simple container that stores and retrieves singleton beans by type or name.
- **`ProxyFactory`**: Uses ByteBuddy to generate proxies that apply logic like retries, scheduling, or repetition
  dynamically.
- **`DispatcherInterceptor`**: Coordinates which method interceptors should apply, such as `@Retryable`, `@Repeat`, or
  `@Scheduled`.

---

## 🧪 Notes

- 📝 **SLF4J** is used for logging. To actually see logs, include `logback-classic` in your classpath.
- ☝️ Currently supports **singleton beans** and **constructor-based injection** only.
- 🔮 Future development plans include:
    - Field/property injection
    - Bean lifecycle callbacks
    - Configuration-based bean registration
    - Profile and conditional support

---

## ☁️ Maven Usage

This framework is published on **Repsy**. You can easily use it in your Maven project by adding the following dependency
and repository definition:

### 📦 Dependency

```xml

<dependency>
    <groupId>com.nuricanozturk.nucleus</groupId>
    <artifactId>nucleus</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 🗂️ Repository

```xml

<repositories>
    <repository>
        <id>repsy</id>
        <name>My Public Maven Repository on Repsy</name>
        <url>https://repo.repsy.io/mvn/nuricanozturk/nucleus</url>
    </repository>
</repositories>
```

> **Note:** Place the `<dependency>` and `<repository>` blocks inside the `<project>` section of your `pom.xml` file.

## 📄 License

**MIT License**
Free to use for personal or commercial projects. No warranty or liability is implied.

