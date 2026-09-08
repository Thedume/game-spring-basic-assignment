package com.gamebasic.game.dto;

import com.gamebasic.game.entity.GamePhase;
import com.gamebasic.game.entity.GameStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GameSummaryResponse {
    private final Long id;
    private final String playerName;
    private final int currentHp;
    private final int currentFloor;
    private final GamePhase phase;
    private final GameStatus status;
    private final int DeckSize;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public GameSummaryResponse(
            Long id,
            String playerName,
            int currentFloor,
            int currentHp,
            GamePhase phase,
            GameStatus status,
            int deckSize,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.playerName = playerName;
        this.currentHp = currentHp;
        this.currentFloor = currentFloor;
        this.phase = phase;
        this.status = status;
        this.DeckSize = deckSize;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
