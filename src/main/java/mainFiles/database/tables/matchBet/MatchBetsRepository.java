package mainFiles.database.tables.matchBet;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("matchBetsRepository")
public interface MatchBetsRepository extends CrudRepository<MatchBet, Integer> {
}
