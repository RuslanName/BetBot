package mainFiles.database.tables.userBalance;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("userBalancesRepository")
public interface UserBalancesRepository extends CrudRepository<UserBalance, Long> {
    List<UserBalance> findAllByOrderByBalanceDesc();
}

