package mainFiles.database.tables.match;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("matchesRepository")
public interface MatchesRepository extends CrudRepository<Match, Integer> {
}