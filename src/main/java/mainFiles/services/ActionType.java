package mainFiles.services;

import lombok.Getter;

@Getter
public enum ActionType {
    REGISTRATION(1),
    MATCH_CREATE(2),
    MATCH_FINISH(3),
    USER_UPDATE_BALANCE(4),
    USER_RESET_BALANCE(5),
    USER_SPECIFIC_RESET_BALANCE(6),
    USER_ADD_BALANCE(7),
    USER_ALL_ADD_BALANCE(8),
    USER_SPECIFIC_ADD_BALANCE(9),
    TEAM_IMAGE(10),
    TEAM_ADD_OR_UPDATE_IMAGE(11),
    TEAM_SPECIFIC_CHECK_IMAGE(12);

    private final int code;

    ActionType(int code) {
        this.code = code;
    }
}



