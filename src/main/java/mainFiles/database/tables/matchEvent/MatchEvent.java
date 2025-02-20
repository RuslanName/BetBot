package mainFiles.database.tables.matchEvent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "match_events_data", schema = "bet_bot")
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "match_id", columnDefinition = "INTEGER")
    private Integer matchId;

    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(name = "coefficient", columnDefinition = "DOUBLE PRECISION")
    private Double coefficient;
}
