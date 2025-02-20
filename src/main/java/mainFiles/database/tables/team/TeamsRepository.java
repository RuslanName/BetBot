package mainFiles.database.tables.team;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("teamsRepository")
public interface TeamsRepository extends CrudRepository<Team, Integer> {
    boolean existsByName(String name);
    List<Team> findByName(String name);
}
