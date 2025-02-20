package mainFiles.database.tables.match;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "matches_data", schema = "bet_bot")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "first_team", columnDefinition = "VARCHAR(255)")
    private String firstTeam;

    @Column(name = "second_team", columnDefinition = "VARCHAR(255)")
    private String secondTeam;

    @Column(name = "time_limit", columnDefinition = "TIMESTAMP")
    private Timestamp timeLimit;
}
