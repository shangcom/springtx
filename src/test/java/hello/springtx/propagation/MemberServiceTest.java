package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LogRepository logRepository;

    /**
     * memberService    @Transactional : OFF
     * memberRepository @Transactional : ON (save 메서드)
     * logRepository    @Transactional : ON (save 메서드)
     * 예외 처리 없음 (joinV1)
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Transactional : OFF
     * memberRepository @Transactional : ON (save 메서드)
     * logRepository    @Transactional : ON (save 메서드), 예외 발생
     * 예외 처리 없음 (joinV1)
     */
    @Test
    void outerTxOff_fail() {
        //give
        String username = "로그예외_outerTxOff_fail";

        //when : Log의 save 메서드에서 예외 발생
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : member는 저장되고, log는 롤백되어 저장 안됨.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }


    /**
     * memberService    @Transactional : ON
     * memberRepository @Transactional : OFF (save 메서드)
     * logRepository    @Transactional : OFF (save 메서드)
     * 예외 처리 없음 (joinV1)
     */
    @Test
    void singleTx() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Transactional : ON
     * memberRepository @Transactional : ON (save 메서드)
     * logRepository    @Transactional : ON (save 메서드), 예외 발생
     * 예외 처리 없음 (joinV1)
     */
    @Test
    void outerTxOn_fail() {
        //give
        String username = "로그예외_outerTxOn_fail";

        //when : Log의 save 메서드에서 예외 발생
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        /*
        then : joinV1은 외부(논리) 트랜잭션으로, 물리 트랜잭션을 시작하고 종료하는 역할.
               memberRepository.save는 내부 트랜잭션(논리 트랜잭션)으로 joinV1의 물리 트랜잭션에 참여.
               메서드 성공, 내부(논리) 트랜잭션 commit().
               신규 (물리)트랜잭션이 아니므로 실제 커밋이나 롤백 불가능.
               정상 상태를 나타낼 뿐, 반영되려면 물리 트랜잭션이 커밋해야. 그때까지 대기.
               logRepository.save는 내부(논리) 트랜잭션으로 외부 트랜잭션인 joinV1의 물리 트랜잭션에 참여.
               예외 발생, 논리 트랜잭션 rollback().
               신규 (물리) 트랜잭션이 아니므로 실제 롤백이 아니라, 같은 물리 트랜잭션의
               모든 논리 트랜잭션에 rollbackOnly 전파.
               joinV1으로 예외 전달, 앞서 rollbackOnly로 설정되었으므로 같은 물리 트랜잭션의 모든 작업을 호출 역순으로 롤백.
               물리 트랜잭션의 커밋을 기다리던 memberRepository.save가 롤백됨.
               모두 롤백되고 joinV1이 @Transactional을 통해 AOP Proxy에 예외를 던짐.
               트랜잭션 매니저가 롤백 호출, 물리 트랜잭션 종료, 메서드 종료.
         */

        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService    @Transactional : ON
     * memberRepository @Transactional : ON (save 메서드)
     * logRepository    @Transactional : ON (save 메서드), 예외 발생
     * 예외 처리 있음 (joinV2)
     */
    @Test
    void recoverException_fail() {
        //give
        String username = "로그예외_recoverException_fail";

        //when : Log의 save 메서드에서 예외 발생
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        //then : logRepository에서 던진 예외를 memberService에서 처리하여 물리 트랜잭션에 대한 commit을 호출하지만,
        //       트랜잭션매니저가 rollbackOnly를 탐지하고 rollback을 호출함.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService    @Transactional : ON
     * memberRepository @Transactional : ON (save 메서드)
     * logRepository    @Transactional : ON(REQUIRES_NEW) (save 메서드), 예외 발생
     * 예외 처리 있음 (joinV2)
     */
    @Test
    void recoverException_success() {
        //give
        String username = "로그예외_recoverException_success";

        //when : Log의 save 메서드에서 예외 발생
         memberService.joinV2(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }
}