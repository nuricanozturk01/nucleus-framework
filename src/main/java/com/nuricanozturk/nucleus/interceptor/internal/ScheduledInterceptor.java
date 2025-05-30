package com.nuricanozturk.nucleus.interceptor.internal;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.nuricanozturk.nucleus.annotation.Scheduled;
import com.nuricanozturk.nucleus.interceptor.AbstractInterceptor;
import com.nuricanozturk.nucleus.interceptor.NucleusInterceptor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledInterceptor extends AbstractInterceptor implements NucleusInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledInterceptor.class);
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);

  public ScheduledInterceptor(
      final @NotNull Object targetObject, final @NotNull Class<?> targetClass) {
    super(targetObject, targetClass);

    LOGGER.debug("ScheduledInterceptor created for class '{}'", targetClass.getName());

    this.scheduleAllCronJobsOnStartup();
  }

  @Override
  public boolean supports(final @NotNull Method method) {
    return method.isAnnotationPresent(Scheduled.class);
  }

  private void scheduleAllCronJobsOnStartup() {
    for (final Method method : this.targetClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Scheduled.class)) {
        final Scheduled scheduled = method.getAnnotation(Scheduled.class);
        this.scheduleMethod(method, scheduled.cron());
      }
    }
  }

  private void scheduleMethod(final @NotNull Method method, final @NotNull String cronExpr) {
    final CronParser parser =
        new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));
    final Cron cron = parser.parse(cronExpr);
    final ExecutionTime executionTime = ExecutionTime.forCron(cron);

    LOGGER.debug("Scheduling method '{}' with cron expression '{}'", method.getName(), cronExpr);

    final Runnable task =
        new Runnable() {
          @Override
          public void run() {
            try {
              method.setAccessible(true);
              method.invoke(ScheduledInterceptor.this.targetObject);

              final ZonedDateTime now = ZonedDateTime.now();
              final ZonedDateTime nextExecution = executionTime.nextExecution(now).orElseThrow();
              final long delay = Duration.between(now, nextExecution).toMillis();

              EXECUTOR.schedule(this, delay, TimeUnit.MILLISECONDS);

            } catch (final Exception e) {
              e.printStackTrace();
            }
          }
        };

    final ZonedDateTime now = ZonedDateTime.now();
    final ZonedDateTime nextExecution = executionTime.nextExecution(now).orElseThrow();
    final long delay = Duration.between(now, nextExecution).toMillis();

    EXECUTOR.schedule(task, delay, TimeUnit.MILLISECONDS);
  }

  @Override
  public Object invoke(
      final @NotNull Object proxy, final @NotNull Method method, final @NotNull Object[] args)
      throws Throwable {
    return null;
  }
}
