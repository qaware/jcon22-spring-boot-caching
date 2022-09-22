package de.qaware.demo.jcon22.caching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class TransactionConfiguration {

    @Bean
    public PlatformTransactionManager dummyTransactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() throws TransactionException {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
                // dummy
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
                // dummy
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                // dummy
            }
        };
    }
}
