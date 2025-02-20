package mainFiles.database.tables.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("usersRepository")
public interface UsersRepository extends CrudRepository<User, Long> {
    boolean existsByUserName(String userName);
    List<User> findByUserName(String userName);
    boolean existsByBetboomId(Long betboomId);
}

