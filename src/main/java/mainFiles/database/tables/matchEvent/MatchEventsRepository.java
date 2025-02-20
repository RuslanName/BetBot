package mainFiles.database.tables.matchEvent;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("matchEventsRepository")
public interface MatchEventsRepository extends CrudRepository<MatchEvent, Integer> {
    void deleteByMatchId(Integer matchId);
}
