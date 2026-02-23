package com.app.docflow.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeLoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint pjp, LogExecutionTime logExecutionTime) throws Throwable {
        long started = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("op={} method={} elapsedMs={} status=SUCCESS",
                    logExecutionTime.value(), pjp.getSignature().toShortString(), System.currentTimeMillis() - started);
            return result;
        } catch (Throwable throwable) {
            log.info("op={} method={} elapsedMs={} status=ERROR errorType={}",
                    logExecutionTime.value(), pjp.getSignature().toShortString(), System.currentTimeMillis() - started,
                    throwable.getClass().getSimpleName());
            throw throwable;
        }
    }

}
