package edu.shtoiko.transactionprowider.repository;

import edu.shtoiko.transactionprowider.model.entity.AccountVo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<AccountVo, Long> {
    Mono<AccountVo> findByAccountNumber(long accountNumber);
}
