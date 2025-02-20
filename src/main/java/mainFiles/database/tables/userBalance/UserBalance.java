package mainFiles.database.tables.userBalance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_balances_data", schema = "bet_bot")
public class UserBalance {

    @Id
    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

    @Column(name = "balance", columnDefinition = "INTEGER")
    private Integer balance;
}
