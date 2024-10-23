package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        /*
        	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	        runtimeOnly 'com.h2database:h2'
	        spring-boot-starter-data-jpa 의존성을 추가하면, 스프링 부트는 데이터베이스 연결을 위한 DataSource를
	        자동으로 구성합니다. 현재 설정에서는 H2 데이터베이스를 사용하므로, 스프링 부트는 H2의 메모리 데이터베이스를
	        기본적으로 설정하고, 이를 DataSource 빈으로 등록.
         */
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }

    @Test
    void inner_commit() {
        log.info(("외부 트랜잭션 시작"));
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }
    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());
        log.info("outer.isRollbackOnly() = {}", outer.isRollbackOnly());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());
        log.info("inner.isRollbackOnly() = {}", inner.isRollbackOnly());

        /*
        내부 트랜잭션에서 롤백이 발생하면, 해당 트랜잭션은 롤백 전용(rollback-only) 상태로 설정됨.
        롤백 전용 상태란, 트랜잭션이 더 이상 커밋될 수 없는 상태를 의미하며, 커밋 시도 시 전체 트랜잭션이 롤백됨.
         */
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);
        log.info("inner.isRollbackOnly() = {}", inner.isRollbackOnly());
        log.info("outer.isRollbackOnly() = {}", outer.isRollbackOnly());

        /*
        내부 트랜잭션이 롤백 전용 상태로 설정되면, 동일한 물리적 트랜잭션을 공유하는 외부 트랜잭션도 롤백 전용 상태로 전파.
        최종적으로 외부 트랜잭션이 커밋 시도를 해도 롤백됨.
         */
        log.info("외부 트랜잭션 커밋");
//        txManager.commit(outer);
        Assertions.assertThatThrownBy(() -> txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");


        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        /*
        TransactionDefinition의 Propagation 기본값은 REQUIRED. 외부 트랜잭션(conn0)이 있으면 참여함.
        여기서는 Propagation을 PROPAGATION_REQUIRES_NEW로 설정한 뒤 새로운 트랜잭션(conn1) 열었다.
        외부 트랜잭션에 참여하는 것이 아니라, 새로운 물리적 트랜잭션을 연다.
        외부 트랜잭션은 새로운 트랜잭션이 완료될 때까지 연기된다.
         */
        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction()); //true

        /*
        내부 트랜잭션이 con1 물리적 트랜잭션을 롤백한다.
        연기됐던 외부 트랜잭션을 재개한다.
         */
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);

        /*
        외부 트랜잭션 커밋 요청이 들어오면, rollbackOnly 설정을 체크한다.
        앞의 테스트에서 내부 트랜잭션이 논리 트랜잭션을 사용하여 롤백을 했을 경우에는 rollbackOnly가
        같은 물리 트랜잭션 안의 모든 논리 트랜잭션에 전파된다.
        그러나 지금은 다른 물리 트랜잭션임으로, rollbackOnly가 전파되지 않는다.
        따라서 commit이 가능하다.
         */
        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }
}
