package mainFiles.services;

import mainFiles.configs.BotConfig;

import mainFiles.database.tables.differentState.DifferentState;
import mainFiles.database.tables.differentState.DifferentStatesRepository;
import lombok.extern.slf4j.Slf4j;
import mainFiles.database.tables.match.Match;
import mainFiles.database.tables.match.MatchesRepository;
import mainFiles.database.tables.matchBet.MatchBetsRepository;
import mainFiles.database.tables.matchEvent.MatchEvent;
import mainFiles.database.tables.matchEvent.MatchEventsRepository;
import mainFiles.database.tables.team.Team;
import mainFiles.database.tables.team.TeamsRepository;
import mainFiles.database.tables.user.User;
import mainFiles.database.tables.user.UsersRepository;
import mainFiles.database.tables.userBalance.UserBalance;
import mainFiles.database.tables.userBalance.UserBalancesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TelegramBot extends TelegramWebhookBot {

    @Autowired
    private DifferentStatesRepository differentStatesRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserBalancesRepository userBalancesRepository;

    @Autowired
    private MatchesRepository matchesRepository;

    @Autowired
    private MatchBetsRepository matchBetsRepository;

    @Autowired
    private MatchEventsRepository matchEventsRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    final BotConfig config;

    static final String MATCH_CREATE_KEYBOARD = "Создать матч";
    static final String MATCH_FINISH_KEYBOARD = "Завершить матч";
    static final String USER_UPDATE_BALANCE_KEYBOARD = "Обновить баланс";
    static final String SHOW_USERS_LEADERBOARD_KEYBOARD = "Список лидеров";
    static final String SHOW_STATISTIC_KEYBOARD = "Ститистика";
    private static final String TEAM_IMAGE_KEYBOARD = "Эмблема команды";

    static final String BETBOOM_ACCOUNT_REGISTRATION_BUTTON = "Зарегистрироваться в BetBoom";
    static final String NO_BUTTON = "Нет";
    static final String YES_BUTTON = "Да";
    static final String CANCEL_BUTTON = "Отмена";
    static final String USER_RESET_BALANCE_BUTTON = "Обнулить";
    static final String USER_ADD_BALANCE_BUTTON = "Добавить";
    static final String USER_ALL_BUTTON = "Всем";
    static final String USER_SPECIFIC_BUTTON = "Определённому";
    static final String TEAM_ADD_OR_UPDATE_IMAGE_BUTTON = "Добавить/Обновить";
    static final String TEAM_SPECIFIC_CHECK_IMAGE_BUTTON = "Проверить наличие";
    static final String TEAM_ALL_SHOW_IMAGE_BUTTON = "Список добавленных";

    static final String NO_CHANNEL_FOLLOW_TEXT = bold("Вы не подписаны на канал: https://t.me/roganov_hockey") +
            " \n" + italic("Подпишитесь, и сможете пользоваться ботом");

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "запуск бота"));
        listOfCommands.add(new BotCommand("/registration", "регистрация"));
        listOfCommands.add(new BotCommand("/help", "информация о возможностях бота"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotPath() {
        return config.getSiteURL();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        log.info("Received update: {}", update);

        if (update.hasMessage()) {
            Message message = update.getMessage();

            long chatId = message.getChatId();
            int messageId = message.getMessageId();

            if (isUserInChannel(chatId)) {
                if (message.hasText()) {
                    String text = message.getText();

                    if (text.equals("/start") || text.equals("/registration")) {
                        if (usersRepository.existsById(chatId)) {
                            if (text.equals("/start")) {
                                start(chatId);
                            }

                            else {
                                sendMessage(chatId, "Вы уже зарегистрированы");
                            }
                        }

                        else {
                            setState(chatId, ActionType.REGISTRATION);
                            sendRegistrationMessage(chatId);
                        }
                    }

                    else if (text.equals("/help")) {
                        text = boldAndUnderline("СПИСОК КОМАНД") + "\n\n" +
                                "/start - запустить бота \n" +
                                "/registration - зарегестрироваться \n" +
                                "/help - показать информацию о возможностях бота \n";

                        sendMessage(chatId, text);
                    }

                    else if (text.equals(MATCH_CREATE_KEYBOARD) && isOwner(chatId)) {
                        setState(chatId, ActionType.MATCH_CREATE);
                        sendMatchCreateMessage(chatId);
                    }

                    else if (text.equals(MATCH_FINISH_KEYBOARD) && isOwner(chatId)) {
                        if (matchesRepository.count() > 0) {
                            setState(chatId, ActionType.MATCH_FINISH);
                            sendMatchFinishMessage(chatId);
                        }

                        else {
                            sendMessage(chatId, "Матчи отсутствуют");
                        }
                    }

                    else if (text.equals(USER_UPDATE_BALANCE_KEYBOARD) && isOwner(chatId)) {
                        setState(chatId, ActionType.USER_UPDATE_BALANCE);
                        sendUserUpdateBalanceMessage(chatId);
                    }

                    else if (text.equals(SHOW_USERS_LEADERBOARD_KEYBOARD) && isOwner(chatId)) {
                        var leaderboard = userBalancesRepository.findAllByOrderByBalanceDesc();

                        if (leaderboard.isEmpty()) {
                            sendMessage(chatId, "Список лидеров пуст");
                        }

                        else {
                            StringBuilder leaderboardMessage = new StringBuilder(boldAndUnderline("СПИСОК ЛИДЕРОВ\n\n"));

                            int i = 1;
                            for (UserBalance userBalance : leaderboard) {
                                User user = usersRepository.findById(userBalance.getChatId()).get();
                                leaderboardMessage.append(String.format("%d) @%s - Баланс: %d | Betboom ID: %d\n",
                                        i++, user.getUserName(), userBalance.getBalance(), user.getBetboomId()));
                            }

                            sendMessage(chatId, leaderboardMessage.toString());
                        }
                    }

                    else if (text.equals(SHOW_STATISTIC_KEYBOARD) && isOwner(chatId)) {
                        var users = (List<User>) usersRepository.findAll();

                        if (users.isEmpty()) {
                            sendMessage(chatId, "Статистика отсутствует");
                        }

                        else {
                            LocalDateTime now = LocalDateTime.now();

                            long dayUsersCount = users.stream()
                                    .filter(user -> isSameDay(user.getRegisteredAt(), now))
                                    .count();

                            long weekUsersCount = users.stream()
                                    .filter(user -> isSameWeek(user.getRegisteredAt(), now))
                                    .count();

                            StringBuilder statisticMessage = new StringBuilder(boldAndUnderline("СТАТИСТИКА\n\n"));

                            statisticMessage.append(italic("Количество зарегистрировавшихся людей")).append("\n");
                            statisticMessage.append("За сегодня: ").append(dayUsersCount).append("\n");
                            statisticMessage.append("За неделю: ").append(weekUsersCount).append("\n");
                            statisticMessage.append("За все время: ").append(usersRepository.count()).append("\n");

                            sendMessage(chatId, statisticMessage.toString());
                        }
                    }

                    else if (text.equals(TEAM_IMAGE_KEYBOARD) && isOwner(chatId)) {
                        setState(chatId, ActionType.TEAM_IMAGE);
                        sendTeamImageMessage(chatId);
                    }

                    else if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.REGISTRATION.getCode()) {
                            if (usersRepository.existsById(chatId)) {
                                UserBalance userBalance = new UserBalance();

                                userBalance.setChatId(chatId);

                                userBalance.setBalance(config.getStartBalance());

                                userBalancesRepository.save(userBalance);

                                start(chatId);

                                deleteState(chatId);
                            }

                            else if (isNumeric(text)) {
                                if (isUniqueBetboomId(text)) {
                                    registration(message);

                                    deleteState(chatId);
                                }

                                else {
                                    sendMessage(chatId, "Уже есть пользователь с данным Betboom ID. Проверьте корректность ввода и введите снова");
                                }
                            }

                            else {
                                sendMessage(chatId, "Betboom ID введён неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.MATCH_CREATE.getCode()) {
                            if (isValidMatchCreateMessage(text)) {
                                createMatchDatabase(chatId, text);

                                sendMessage(chatId, "Матч создан");

                                for (UserBalance userBalance : userBalancesRepository.findAll()) {
                                    long userChatId = userBalance.getChatId();

                                    if (config.getOwnerChatId() != userChatId) {
                                        sendMessage(userChatId, "Добавлен новый матч");
                                    }
                                }

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Матч введён неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_SPECIFIC_RESET_BALANCE.getCode()) {
                            if (isValidUserNameMessage(text)) {
                                text = text.replace("@", "");
                                if (usersRepository.existsByUserName(text) ||
                                        userBalancesRepository.existsById(usersRepository.findByUserName(text).get(0).getChatId())) {
                                    UserBalance userBalance = userBalancesRepository.findById(usersRepository.findByUserName(text).get(0).getChatId()).get();
                                    userBalance.setBalance(config.getStartBalance());

                                    userBalancesRepository.save(userBalance);

                                    sendMessage(chatId, "У пользователя @" + text + " обнулён баланс");
                                    sendMessage(userBalance.getChatId(), "Ваш баланс обнулён");

                                    deleteState(chatId);
                                }

                                else {
                                    sendMessage(chatId, "Пользователя @" + text + " нет в базе данных");
                                }
                            }

                            else {
                                sendMessage(chatId, "Username введён неправильно. " +
                                        "Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_SPECIFIC_ADD_BALANCE.getCode()) {
                            if (isValidUserNameAndNumberMessage(text)) {
                                String[] lineParts = text.split(" ");

                                String userName = lineParts[0].replace("@", "");
                                int numberPoints = Integer.parseInt(lineParts[1]);

                                if (usersRepository.existsByUserName(text) ||
                                        userBalancesRepository.existsById(usersRepository.findByUserName(userName).get(0).getChatId())) {
                                    UserBalance userBalance = userBalancesRepository.findById(usersRepository.findByUserName(userName).get(0).getChatId()).get();

                                    userBalance.setBalance(userBalance.getBalance() + numberPoints);

                                    userBalancesRepository.save(userBalance);

                                    sendMessage(chatId, "У пользователя @" + userName + " пополнен баланс на "
                                            + numberPoints + " баллов");
                                    sendMessage(userBalance.getChatId(), "Ваш баланс пополнен на " + numberPoints + " баллов");

                                    deleteState(chatId);
                                }

                                else {
                                    sendMessage(chatId, "Пользователя @" + userName + " нет в базе данных");
                                }
                            }

                            else {
                                sendMessage(chatId, "Username или количество баллов введены неправильно. " +
                                        "Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_ALL_ADD_BALANCE.getCode()) {
                            if (isNumeric(text)) {
                                var userBalances = userBalancesRepository.findAll();

                                for (UserBalance userBalance : userBalances) {
                                    userBalance.setBalance(userBalance.getBalance() + Integer.valueOf(text));

                                    long userChatId = userBalance.getChatId();

                                    if (config.getOwnerChatId() != userChatId) {
                                        sendMessage(userChatId, "Ваш баланс пополнен на " + text + " баллов");
                                    }
                                }

                                userBalancesRepository.saveAll(userBalances);

                                sendMessage(chatId, "У всех пользователей пополнен баланс на " + text + " баллов");

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Количество баллов введено неправильно. " +
                                        "Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.TEAM_SPECIFIC_CHECK_IMAGE.getCode()) {
                            if (teamsRepository.existsByName(text)) {
                                Team team = teamsRepository.findByName(text).get(0);

                                sendDocument(chatId, config.getTeamsImagePath(), team.getImageName());

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Эмбелма команды отсутствует в списке");
                            }
                        }
                    }
                }

                else if (message.hasPhoto()) {

                }

                else if (message.hasDocument()) {
                    if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.TEAM_ADD_OR_UPDATE_IMAGE.getCode()) {
                            if (message.getCaption() != null) {
                                try {
                                    addOrUpdateTeamImageDatabase(message);

                                    sendMessage(chatId, "Эмблема команды была добавлена/обновлена");

                                    deleteState(chatId);
                                } catch (TelegramApiException e) {
                                    throw new RuntimeException(e);
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            else {
                                sendMessage(chatId, "Вы не ввели название команды");
                            }
                        }
                    }
                }
            }
        }

        else if (update.hasCallbackQuery()) {
            Message message = update.getCallbackQuery().getMessage();

            long chatId = message.getChatId();

            if (isUserInChannel(chatId)) {
                int messageId = message.getMessageId();

                String callbackData = update.getCallbackQuery().getData();

                if (differentStatesRepository.findById(chatId).get().getState() == ActionType.MATCH_CREATE.getCode()) {
                    if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Добавление матча отменено");

                        deleteState(chatId);
                    }
                }

                if (differentStatesRepository.findById(chatId).get().getState() == ActionType.MATCH_FINISH.getCode()) {
                    if (callbackData.matches("MATCH_\\d+_BUTTON")) {
                        int id = extractNumberFromButton(callbackData);
                        // ДОДЕЛАТЬ
                        matchesRepository.deleteById(id);
                        matchEventsRepository.deleteByMatchId(id);

                        editMessageText(chatId, messageId, "Матч завершён");
                    }

                    else if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Удаление матча отменено");
                    }

                    deleteState(chatId);
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_UPDATE_BALANCE.getCode()) {
                    if (callbackData.equals(USER_RESET_BALANCE_BUTTON)) {
                        setState(chatId, ActionType.USER_RESET_BALANCE);

                        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                        keyboard.add(createInlineKeyboardRow(USER_SPECIFIC_BUTTON, USER_ALL_BUTTON));

                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        markup.setKeyboard(keyboard);

                        String text = "Кому вы хотите обнулить баланс?";

                        editMessageText(chatId, messageId, text, markup);
                    }

                    else if (callbackData.equals(USER_ADD_BALANCE_BUTTON)) {
                        setState(chatId, ActionType.USER_ADD_BALANCE);

                        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                        keyboard.add(createInlineKeyboardRow(USER_SPECIFIC_BUTTON, USER_ALL_BUTTON));

                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        markup.setKeyboard(keyboard);

                        String text = "Кому вы хотите добавить баланс?";

                        editMessageText(chatId, messageId, text, markup);
                    }

                    else if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Обновление баланса отменено");

                        deleteState(chatId);
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_RESET_BALANCE.getCode()) {
                    if (callbackData.equals(USER_SPECIFIC_BUTTON)) {
                        setState(chatId, ActionType.USER_SPECIFIC_RESET_BALANCE);
                        editMessageText(chatId, messageId, "Введите Username пользователя, баланс которому хотите обнулить. " +
                                "Можно можно вводить с \"@\" и без");
                    }

                    else if (callbackData.equals(USER_ALL_BUTTON)) {
                        var userBalances = userBalancesRepository.findAll();

                        for (UserBalance userBalance : userBalances) {
                            userBalance.setBalance(config.getStartBalance());

                            if (config.getOwnerChatId() != userBalance.getChatId()) {
                                sendMessage(userBalance.getChatId(), "У всех пользователей обнулён баланс");
                            }
                        }

                        editMessageText(chatId, messageId, "У всех пользователей обнулён баланс");

                        userBalancesRepository.saveAll(userBalances);
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_ADD_BALANCE.getCode()) {
                    if (callbackData.equals(USER_SPECIFIC_BUTTON)) {
                        setState(chatId, ActionType.USER_SPECIFIC_ADD_BALANCE);
                        editMessageText(chatId, messageId, "Введите Username пользователя, которому хотите добавить баланс и " +
                                "количество баллов. Username можно можно вводить с \"@\" и без");
                    }

                    else if (callbackData.equals(USER_ALL_BUTTON)) {
                        setState(chatId, ActionType.USER_ALL_ADD_BALANCE);
                        editMessageText(chatId, messageId, "Введите количество баллов, которые хотите добавить в баланс");
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.TEAM_IMAGE.getCode()) {
                    if (callbackData.equals(TEAM_ADD_OR_UPDATE_IMAGE_BUTTON)) {
                        setState(chatId, ActionType.TEAM_ADD_OR_UPDATE_IMAGE);
                        editMessageText(chatId, messageId, "Введите эмблему и название команды");
                    }

                    else if (callbackData.equals(TEAM_SPECIFIC_CHECK_IMAGE_BUTTON)) {
                        setState(chatId, ActionType.TEAM_SPECIFIC_CHECK_IMAGE);
                        editMessageText(chatId, messageId, "Введите название команды");
                    }

                    else if (callbackData.equals(TEAM_ALL_SHOW_IMAGE_BUTTON)) {
                        if (teamsRepository.count() > 0) {
                            StringBuilder addedTeamsMessage = new StringBuilder(boldAndUnderline("СПИСОК ДОБАВЛЕННЫХ ЭМБЛЕМ\n\n"));

                            int i = 1;
                            for (Team team : teamsRepository.findAll()) {
                                addedTeamsMessage.append(i++).append(". ").append(team.getName()).append("\n");
                            }

                            editMessageText(chatId, messageId, addedTeamsMessage.toString());

                            deleteState(chatId);
                        }

                        else {
                            sendMessage(chatId, "Эмблемы команд отсутствуют");
                        }
                    }

                    else if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Действия с эмблемами команд отменено");

                        deleteState(chatId);
                    }
                }
            }
        }
        return null;
    }

    private void start(long chatId) {
        if (!userBalancesRepository.existsById(chatId)) {
            UserBalance userBalance = new UserBalance();

            userBalance.setChatId(chatId);
            userBalance.setBalance(config.getStartBalance());

            userBalancesRepository.save(userBalance);
        }

        if (isOwner(chatId)) {
            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(String.valueOf(chatId));

            sendMessage.setText("Здраствуйте, владелец " + usersRepository.findById(chatId).get().getFirstName());

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);

            List<KeyboardRow> keyboardRows = new ArrayList<>();

            keyboardRows.add(createKeyboardRow(MATCH_CREATE_KEYBOARD, MATCH_FINISH_KEYBOARD));
            keyboardRows.add(createKeyboardRow(USER_UPDATE_BALANCE_KEYBOARD, TEAM_IMAGE_KEYBOARD));
            keyboardRows.add(createKeyboardRow(SHOW_USERS_LEADERBOARD_KEYBOARD, SHOW_STATISTIC_KEYBOARD));

            replyKeyboardMarkup.setKeyboard(keyboardRows);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            executeFunction(sendMessage);
        }

        else {
            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(String.valueOf(chatId));

            sendMessage(chatId, "Здраствуйте, " + usersRepository.findById(chatId).get().getFirstName());
        }
    }

    private void registration(Message message) {
        long chatId = message.getChatId();
        Chat chat = message.getChat();

        User user = new User();

        user.setChatId(chatId);
        user.setUserName(chat.getUserName());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setBetboomId(Long.valueOf(message.getText()));
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

        usersRepository.save(user);

        UserBalance userBalance = new UserBalance();

        userBalance.setChatId(chatId);

        userBalance.setBalance(config.getStartBalance());

        userBalancesRepository.save(userBalance);

        start(chatId);
    }

    private void sendRegistrationMessage(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setText(BETBOOM_ACCOUNT_REGISTRATION_BUTTON);
        button.setUrl(config.getBetboomRegistrationURL());

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        markup.setKeyboard(keyboard);

        String text = "Привет! Чтоб начать проходить квиз " + bold("введи ID игрового счёта в BetBoom") +
                ", а если у тебя нет аккаунта — зарегистрируйся";

        sendPhoto(chatId, config.getRegistrationImagePath(), text, markup);
    }

    private void sendMatchCreateMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Введите матч";

        sendMessage(chatId, text, markup);
    }

    private void sendMatchFinishMessage(long chatId) {
        InlineKeyboardMarkup markup = createMatchMarkup();

        String text = "Выберите матч";

        sendMessage(chatId, text, markup);
    }

    private void sendUserUpdateBalanceMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(USER_RESET_BALANCE_BUTTON, USER_ADD_BALANCE_BUTTON));
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Что вы хотите сделать?";

        sendMessage(chatId, text, markup);
    }

    private void sendTeamImageMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(TEAM_ADD_OR_UPDATE_IMAGE_BUTTON));
        keyboard.add(createInlineKeyboardRow(TEAM_SPECIFIC_CHECK_IMAGE_BUTTON));
        keyboard.add(createInlineKeyboardRow(TEAM_ALL_SHOW_IMAGE_BUTTON));
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Что вы хотите сделать?";

        sendMessage(chatId, text, markup);
    }

    private void createMatchDatabase(long chatId, String text) {
        String[] lines = text.split("\\n");
        String[] firstDataTeam = lines[0].split(" ");
        String[] secondDataTeam = lines[1].split(" ");

        String firstTeam = String.join(" ", Arrays.copyOf(firstDataTeam, firstDataTeam.length - 1));
        String secondTeam = String.join(" ", Arrays.copyOf(secondDataTeam, secondDataTeam.length - 1));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(lines[2], formatter);

        ZonedDateTime zonedDateTimeMSK = dateTime.atZone(ZoneId.of("Europe/Moscow"));
        ZonedDateTime zonedDateTimeUTC = zonedDateTimeMSK.withZoneSameInstant(ZoneId.of("UTC"));

        Timestamp timeLimit = Timestamp.from(zonedDateTimeUTC.toInstant());

        Match match = new Match();

        match.setFirstTeam(firstTeam);
        match.setSecondTeam(secondTeam);
        match.setTimeLimit(timeLimit);

        matchesRepository.save(match);

        MatchEvent matchEvent = new MatchEvent();

        matchEvent.setMatchId((int) matchesRepository.count());
        matchEvent.setName(firstTeam);
        matchEvent.setCoefficient(Double.valueOf(firstDataTeam[firstDataTeam.length - 1]));

        matchEventsRepository.save(matchEvent);

        matchEvent = new MatchEvent();

        matchEvent.setMatchId((int) matchesRepository.count());
        matchEvent.setName(secondTeam);
        matchEvent.setCoefficient(Double.valueOf(secondDataTeam[secondDataTeam.length - 1]));

        matchEventsRepository.save(matchEvent);
    }

    private void addOrUpdateTeamImageDatabase(Message message) throws TelegramApiException, MalformedURLException {
        long chatId = message.getChatId();

        String imageName = saveFile(message, config.getTeamsImagePath());
        String name = message.getCaption();

        if (teamsRepository.existsByName(name)) {
            Team team = teamsRepository.findByName(name).get(0);

            deleteFile(config.getTeamsImagePath(), team.getImageName());

            team.setImageName(imageName);

            teamsRepository.save(team);
        }

        else {
            Team team = new Team();

            team.setName(name);
            team.setImageName(imageName);

            teamsRepository.save(team);
        }
    }

    private String saveImage(Message message, String imagePath) throws TelegramApiException, MalformedURLException {
        String imageName = "image_" + System.currentTimeMillis() + ".jpg";

        String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
        File file = this.execute(new GetFile(fileId));
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, Path.of(imagePath + imageName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return imageName;
    }

    private String saveFile(Message message, String filePath) throws TelegramApiException, MalformedURLException {
        String fileName = "file_" + System.currentTimeMillis();

        Document file = message.getDocument();
        String fileId = file.getFileId();
        File downloadedFile = this.execute(new GetFile(fileId));
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + downloadedFile.getFilePath();

        String originalFileName = file.getFileName();
        if (originalFileName != null) {
            String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
            fileName = fileName + fileExtension;
        }

        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, Path.of(filePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileName;
    }

    private void deleteFile(String filePath, String fileName) throws TelegramApiException, MalformedURLException {
        try {
            Files.delete(Path.of(filePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setState(long chatId, ActionType action) {
        if (!differentStatesRepository.existsById(chatId)) {
            DifferentState differentState = new DifferentState();
            differentState.setChatId(chatId);

            differentState.setState(action.getCode());
            differentStatesRepository.save(differentState);
        }

        else {
            DifferentState differentState = differentStatesRepository.findById(chatId).get();
            differentState.setState(action.getCode());
            differentStatesRepository.save(differentState);
        }
    }

    private void deleteState(long chatId) {
        differentStatesRepository.deleteById(chatId);
    }

    private String convertQuizLimitTimeToString(Timestamp timestamp) {
        ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("Europe/Moscow"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy в H:mm", Locale.forLanguageTag("ru-RU"));

        return zonedDateTime.format(formatter);
    }

    private int extractNumberFromButton(String callbackData) {
        Pattern pattern = Pattern.compile(".*_(\\d+)_BUTTON$");
        Matcher matcher = pattern.matcher(callbackData);

        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    private InlineKeyboardMarkup createMatchMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Match match : matchesRepository.findAll()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            button.setText(match.getFirstTeam() + " - " + match.getSecondTeam());
            button.setCallbackData("MATCH_" + match.getId() + "_BUTTON");

            row.add(button);
            rows.add(row);
        }

        rows.add(createInlineKeyboardRow(CANCEL_BUTTON));

        markup.setKeyboard(rows);
        return markup;
    }

    private boolean isOwner(long chatId) {
        return config.getOwnerChatId() == chatId;
    }

    public boolean isUserInChannel(long chatId) {
        if (config.isCheckChannelChatId()) {
            try {
                GetChatMember getChatMember = new GetChatMember();

                getChatMember.setChatId(String.valueOf(config.getChannelChatId()));
                getChatMember.setUserId(chatId);

                ChatMember chatMember = execute(getChatMember);

                String status = chatMember.getStatus();
                return !status.equals("left") && !status.equals("kicked");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                return false;
            }
        }

        else {
            return true;
        }
    }

    private boolean isUniqueBetboomId(String text) {
        return !usersRepository.existsByBetboomId(Long.parseLong(text));
    }

    private boolean isSameDay(Timestamp timestamp, LocalDateTime now) {
        ZoneId mskZone = ZoneId.of("Europe/Moscow");

        LocalDateTime resultDate = timestamp.toInstant().atZone(mskZone).toLocalDateTime();
        LocalDateTime nowInMsk = now.atZone(mskZone).toLocalDateTime();

        return resultDate.toLocalDate().isEqual(nowInMsk.toLocalDate());
    }

    private boolean isSameWeek(Timestamp timestamp, LocalDateTime now) {
        ZoneId mskZone = ZoneId.of("Europe/Moscow");

        LocalDateTime resultDate = timestamp.toInstant().atZone(mskZone).toLocalDateTime();
        LocalDateTime nowInMsk = now.atZone(mskZone).toLocalDateTime();

        int resultWeek = resultDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        int nowWeek = nowInMsk.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        return resultDate.getYear() == nowInMsk.getYear() && resultWeek == nowWeek;
    }

    private boolean isValidMatchCreateMessage(String text) {
        Pattern pattern = Pattern.compile("(?m)^[A-Za-zА-Яа-яЁё]+(?: [A-Za-zА-Яа-яЁё]+)* \\d+(?:\\.\\d+)?$");
        String[] lines = text.split("\\n");

        if (lines.length != 3) return false;

        for (int i = 0; i < 2; i++) {
            if (!pattern.matcher(lines[i]).matches()) {
                return false;
            }
        }

        return isValidMatchDateFormat(lines[2]);
    }

    private boolean isValidMatchDateFormat(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try {
            LocalDateTime.parse(text, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isNumeric(String text) {
        return text != null && text.matches("-?\\d+");
    }

    public static boolean isValidUserNameMessage(String text) {
        return text != null && text.matches("^@?[A-Za-zА-Яа-яЁё]+$");
    }


    public static boolean isValidUserNameAndNumberMessage(String text) {
        return text != null && text.matches("^(@?[A-Za-zА-Яа-яЁё]+)\\s(\\d+)$");
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        row.addAll(Arrays.asList(buttons));
        return row;
    }

    private List<InlineKeyboardButton> createInlineKeyboardRow(String... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (String textAndCallback : buttons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(textAndCallback);
            button.setCallbackData(textAndCallback);
            row.add(button);
        }
        return row;
    }

    private void sendMessage(long chatId, Object text, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());
            message.setReplyMarkup(markup);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(long chatId, Object text) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String imagePath, String imageName, String caption, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(imagePath + imageName).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));
            sendPhoto.setCaption(caption);
            sendPhoto.setReplyMarkup(markup);

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String imagePath, String caption, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(imagePath).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));
            sendPhoto.setCaption(caption);
            sendPhoto.setReplyMarkup(markup);

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String imagePath, String imageName) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(imagePath + imageName).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String imagePath) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(imagePath).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendDocument(long chatId, String documentPath, String documentName) {
        CompletableFuture.runAsync(() -> {
            SendDocument sendDocument = new SendDocument();

            sendDocument.setChatId(chatId);

            java.io.File docFile = Paths.get(documentPath + documentName).toFile();

            if (!docFile.exists()) {
                return;
            }

            sendDocument.setParseMode(ParseMode.HTML);
            sendDocument.setDocument(new InputFile(docFile));

            try {
                executeFunction(sendDocument);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendDocument(long chatId, String documentPath) {
        CompletableFuture.runAsync(() -> {
            SendDocument sendDocument = new SendDocument();

            sendDocument.setChatId(chatId);

            java.io.File docFile = Paths.get(documentPath).toFile();

            if (!docFile.exists()) {
                return;
            }

            sendDocument.setParseMode(ParseMode.HTML);
            sendDocument.setDocument(new InputFile(docFile));

            try {
                executeFunction(sendDocument);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void editMessageText(long chatId, int messageId, Object text, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            EditMessageText message = new EditMessageText();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText((String) text);
            message.setReplyMarkup(markup);
            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void editMessageText(long chatId, int messageId, Object text) {
        CompletableFuture.runAsync(() -> {
            EditMessageText message = new EditMessageText();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());
            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteMessage(long chatId, int messageId) {
        CompletableFuture.runAsync(() -> {
            DeleteMessage message = new DeleteMessage();

            message.setChatId(String.valueOf(chatId));

            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void executeFunction(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(SendPhoto message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(SendDocument message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private static String bold(String text) {
        return "<b>%s</b>".formatted(text);
    }

    private static String italic(String text) {
        return "<i>%s</i>".formatted(text);
    }

    private static String boldAndItalic(String text) {
        return "<b><i>%s</i></b>".formatted(text);
    }

    private static String boldAndUnderline(String text) {
        return "<b><u>%s</u></b>".formatted(text);
    }
}