package dev.lovelace.loveduels.match;

public enum MatchEndReason {
    KILL("Победа в бою"),
    POINTS_REACHED("Набрано необходимое количество очков"),
    FORFEIT("Сдача противника"),
    DISCONNECT("Бегство / выход из игры"),
    TIMEOUT("Истечение времени поединка"),
    ADMIN_FORCE("Принудительное завершение администратором");

    private final String description;

    MatchEndReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
