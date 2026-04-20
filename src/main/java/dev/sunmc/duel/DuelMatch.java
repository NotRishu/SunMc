package dev.sunmc.duel;

import dev.sunmc.arena.Arena;
import dev.sunmc.kit.Kit;

import java.util.UUID;

public class DuelMatch {

    private final UUID matchId;
    private final UUID player1;
    private final UUID player2;
    private Arena arena;
    private Kit kit;
    private MatchState state;
    private UUID winner;
    private long startTime;

    public DuelMatch(UUID player1, UUID player2) {
        this.matchId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.state = MatchState.WAITING;
    }

    public boolean hasPlayer(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        return player1.equals(uuid) ? player2 : player1;
    }

    public UUID getMatchId() { return matchId; }
    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }
    public Kit getKit() { return kit; }
    public void setKit(Kit kit) { this.kit = kit; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
}
