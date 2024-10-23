package hello.springtx.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    private String username;

    /*
    JPA에서 기본 생성자를 요구한다.
     */
    public Member() {
    }

    public Member(String username) {
        this.username = username;
    }
}
