package com.gamebasic.ranking.service;

import com.gamebasic.ranking.client.RankingClient;
import com.gamebasic.ranking.dto.RankingSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RankingClient rankingClient;

    public void checkRankingCandidates(){
        RankingSource source = rankingClient.fetch();

        List<RankingSource.Record> candidates = new ArrayList<>();

        for (RankingSource.Record record : source.getRecords()){
            if (isRankingCandidate(record)){
                candidates.add(record);
            }
        }

        System.out.println("전체 기록 수: " + source.getRecords().size());
        System.out.println("순위 대상 기록 수: " + candidates.size());
    }

    private boolean isRankingCandidate(RankingSource.Record record) {
        if (record.getRun() == null){
            return false;
        }

        return "CLEARED".equals(record.getRun().getStatus())
                && Integer.valueOf(10).equals(record.getRun().getClearedFloor());
    }
}
