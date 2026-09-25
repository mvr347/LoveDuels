package dev.lovelace.loveduels.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous Discord Webhook notifier for Royal Duels.
 */
public final class DiscordWebhookSender {

    private final String webhookUrl;
    private final Logger logger;
    private final HttpClient httpClient;

    public DiscordWebhookSender(String webhookUrl, Logger logger) {
        this.webhookUrl = webhookUrl != null ? webhookUrl.trim() : "";
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isEnabled() {
        return !webhookUrl.isBlank() && webhookUrl.startsWith("http");
    }

    public void sendRoyalDuelCreated(String challenger, String target, long moneyBet, int honorBet) {
        if (!isEnabled()) return;

        String json = String.format("""
            {
              "embeds": [{
                "title": "👑 Объявлена Королевская Дуэль!",
                "color": 16766720,
                "description": "**%s** бросил вызов **%s** в Королевском поединке!",
                "fields": [
                  {"name": "💰 Ставка деньгами", "value": "%d монет", "inline": true},
                  {"name": "⚜ Ставка Честью", "value": "%d", "inline": true}
                ],
                "footer": {"text": "LoveDuels • Королевские турниры"}
              }]
            }
            """, escape(challenger), escape(target), moneyBet, honorBet);

        postAsync(json);
    }

    public void sendRoyalDuelEnded(String winner, String loser, long totalMoney, int honorWon) {
        if (!isEnabled()) return;

        String json = String.format("""
            {
              "embeds": [{
                "title": "🏆 Королевская Дуэль завершена!",
                "color": 65280,
                "description": "Победитель: **%s** сокрушил **%s**!",
                "fields": [
                  {"name": "💰 Призовой фонд", "value": "+%d монет", "inline": true},
                  {"name": "⚜ Получено Чести", "value": "+%d", "inline": true}
                ],
                "footer": {"text": "LoveDuels • Королевские турниры"}
              }]
            }
            """, escape(winner), escape(loser), totalMoney, honorWon);

        postAsync(json);
    }

    private void postAsync(String jsonPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        logger.log(Level.WARNING, "Failed to send Discord webhook: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not build Discord webhook request", e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
