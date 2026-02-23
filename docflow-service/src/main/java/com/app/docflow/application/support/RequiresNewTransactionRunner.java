package com.app.docflow.application.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RequiresNewTransactionRunner {

    private final PlatformTransactionManager transactionManager;

    public <T> T run(Function<TransactionStatus, T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(callback::apply);
    }

}
