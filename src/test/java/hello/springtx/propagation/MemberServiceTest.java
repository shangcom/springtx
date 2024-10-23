package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
     * 예외 처리 없음
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
     * 예외 처리 없음
     */
    @Test
    void outerTxOff_fail() {
        //give
        String username = "로그예외_outerTxOff_fail";

        //when : Log의 save 메서드에서 예외 발생
        Assertions.assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : member는 저장되고, log는 롤백되어 저장 안됨.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }
}