package edu.shtoiko.transactionprowider.repository;

import edu.shtoiko.transactionprowider.model.entity.AccountVo;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<AccountVo, Long> {
    Mono<AccountVo> findByAccountNumber(long accountNumber);

    @Query("UPDATE accounts SET processing_status = 'IN_PROGRESS' WHERE account_number = :accountNumber AND processing_status = 'READY' RETURNING *")
    Mono<AccountVo> findAndUpdateStatusIfReady(@Param("accountNumber") Long accountNumber);
}
