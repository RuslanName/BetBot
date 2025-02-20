package mainFiles.database.tables.matchBet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "match_bets_data", schema = "bet_bot")
public class MatchBet{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

//    @Column(name = "match_id", columnDefinition = "INTEGER")
//    private Integer matchId;

    @Column(name = "event_id", columnDefinition = "INTEGER")
    private Integer eventId;

    @Column(name = "bet", columnDefinition = "INTEGER")
    private Integer bet;
}