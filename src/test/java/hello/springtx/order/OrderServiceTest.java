package hello.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;


    @Test
    void complete() throws NotEnoughMoneyException {
        // Arrange
        // TODO: Initialize test data
        Order order = new Order();
        order.setUserName("정상");

        // Act
        // TODO: Call the method to be tested
        orderService.order(order);

        // Assert
        // TODO: Verify the results
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("완료");
    }

    @Test
    void runtimeException() throws NotEnoughMoneyException {
        // Arrange
        // TODO: Initialize test data
        Order order = new Order();
        order.setUserName("예외");

        // Act
        // TODO: Call the method to be tested
        assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        // Assert
        // TODO: Verify the results
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }

    @Test
    void bizException() {
        // Arrange
        // TODO: Initialize test data
        Order order = new Order();
        order.setUserName("잔고부족");

        // Act
        // TODO: Call the method to be tested
        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }

        // Assert
        // TODO: Verify the results
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("대기");
    }
}