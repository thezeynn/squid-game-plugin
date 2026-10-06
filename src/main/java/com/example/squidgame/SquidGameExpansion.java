package com.example.squidgame;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class SquidGameExpansion extends PlaceholderExpansion {

    private final RedLightGreenLight game;
    private final LeaderboardManager leaderboardManager;

    public SquidGameExpansion(RedLightGreenLight game, LeaderboardManager leaderboardManager) {
        this.game = game;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public String getIdentifier() {
        return "squidgame";
    }

    @Override
    public String getAuthor() {
        return "ZeyNN";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("players")) {
            return String.valueOf(game.getActivePlayerCount());
        }
        if (identifier.equals("top_wins_name")) {
            return leaderboardManager.getTopWinnerName();
        }
        if (identifier.equals("top_wins_score")) {
            return String.valueOf(leaderboardManager.getTopWinnerScore());
        }
        return null;
    }
}
