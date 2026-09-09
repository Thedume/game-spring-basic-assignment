package com.gamebasic.ranking.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class RankingResponse {
    private String season;
    private int totalRecords;
    private int excludedCount;
    private List<Entry> entries;

    public RankingResponse(
            String season,
            int totalRecords,
            int excludedCount,
            List<Entry> entries
    ){
        this.season = season;
        this.totalRecords = totalRecords;
        this.excludedCount = excludedCount;
        this.entries = entries;
    }

    @Getter
    public static class Entry{
        private int rank;
        private String playerName;
        private int clearTimeSeconds;
        private int remainingHp;
        private int bossTurns;
        private int deckSize;

        public Entry(
                int rank,
                String playerName,
                int clearTimeSeconds,
                int remainingHp,
                int bossTurns,
                int deckSize
        ){
            this.rank = rank;
            this.playerName = playerName;
            this.clearTimeSeconds = clearTimeSeconds;
            this.remainingHp = remainingHp;
            this.bossTurns = bossTurns;
            this.deckSize = deckSize;
        }
    }
}
