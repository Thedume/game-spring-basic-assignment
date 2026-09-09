package com.gamebasic.ranking.service;

import com.gamebasic.ranking.client.RankingClient;
import com.gamebasic.ranking.dto.RankingSource;
import com.gamebasic.runcard.entity.CardType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RankingClient rankingClient;

    private static final Set<String> VALID_CARD_TYPES =
            Arrays.stream(CardType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    private boolean isRankingCandidate(RankingSource.Record record) {
        if (record.getRun() == null){
            return false;
        }

        return "CLEARED".equals(record.getRun().getStatus())
                && Integer.valueOf(10).equals(record.getRun().getClearedFloor());
    }

    private boolean isValidRecord(
            RankingSource.Record record
    ){
        return hasValidDuration(record)
                && hasValidHp(record)
                && hasValidDeck(record)
                && hasValidBossFight(record)
                && hadValidFinishingCard(record);
    }

    // 클리어 기록 계산 및 조건 만족 여부 확인
    // ex. 10층 / 299초 -> false
    // ex. 10층 / 300초 -> true
    // ex. 10층 / 1200초 -> true
    private boolean hasValidDuration(RankingSource.Record record) {
        RankingSource.Run run = record.getRun();

        if (run == null || run.getDurationSeconds() == null || run.getClearedFloor() == null){
            return false;
        }

        return run.getDurationSeconds() >= run.getClearedFloor() * 30;
    }

    // 클리어 HP 계산 및 조건 만족 여부 확인
    // 0 -> 이상
    // 1 -> 정상
    // 99 -> 정상
    // 100 -> 비정상
    private boolean hasValidHp(RankingSource.Record record) {
        RankingSource.Run run = record.getRun();

        if (run == null || run.getFinalHp() == null)
            return false;

        return run.getFinalHp() >= 1 && run.getFinalHp() <= 99;
    }

    // 클리어 덱 검증
    // 조건 1. 실제 카드의 개수 : 9~20
    // 조건 2. 제공된 카드와 실제 카드 개수가 같은지
    // 조건 3. 모든 카드 타입이 CardType enum에 존재
    // 조건 4. 모든 acquiredFloor가 0~9
    private boolean hasValidDeck(RankingSource.Record record) {
        RankingSource.Deck deck = record.getDeck();

        if (deck == null || deck.getSize() == null || deck.getCards() == null)
            return false;

        int actualSize = deck.getCards().size();

        if (actualSize < 9 || actualSize > 20) {
            return false;
        }

        if (deck.getSize() != actualSize)
            return false;

        for (RankingSource.DeckCard card : deck.getCards()){
            if (card == null || card.getCardType() == null || card.getAcquiredFloor() == null)
                return false;

            if (!VALID_CARD_TYPES.contains(card.getCardType()))
                return false;

            if (card.getAcquiredFloor() < 0 || card.getAcquiredFloor() > 9)
                return false;
        }

        return true;
    }

    // 보스전
    // 조건 1. 페이즈가 순서대로냐
    // 조건 2. 각 턴이 1 이상인지
    // 조건 3. 토탈 턴이 3 페이즈 턴의 합이랑 같아야한다.
    private boolean hasValidBossFight(RankingSource.Record record) {
        RankingSource.BossFight bossFight = record.getBossFight();

        if (bossFight == null || bossFight.getPhases() == null || bossFight.getTotalTurns() == null)
            return false;

        List<RankingSource.BossPhase> phases = bossFight.getPhases();

        if (phases.size() != 3)
            return false;

        String[] expectedPhases = {
                "THRONE",
                "UNBOUND",
                "ECLIPSE"
        };

        int totalTurns = 0;

        for (int i = 0; i < 3; i++){
            RankingSource.BossPhase phase = phases.get(i);

            if (phase == null || phase.getPhase() == null || phase.getTurns() == null)
                return false;

            if (!expectedPhases[i].equals(phase.getPhase()))
                return false;

            if (phase.getTurns() < 1)
                return false;

            totalTurns += phase.getTurns();
        }

        return bossFight.getTotalTurns() == totalTurns;
    }

    // 마무리 카드
    // 조건. 마무리 카드가 기록의 cards 안에 있어야 함.
    private boolean hadValidFinishingCard(RankingSource.Record record) {
        if (record.getBossFight() == null
            || record.getBossFight().getFinishingCard() == null
            || record.getDeck() == null
            || record.getDeck().getCards() == null) {
            return false;
        }

        String finishingCard = record.getBossFight().getFinishingCard();

        for (RankingSource.DeckCard card : record.getDeck().getCards()){
            if (card != null && finishingCard.equals(card.getCardType()))
                return true;
        }

        return false;
    }

    // 정렬 작업
    private void sortRecords(List<RankingSource.Record> records){
        records.sort(
                Comparator
                        .comparingInt(
                                (RankingSource.Record record) ->
                                        record.getRun().getDurationSeconds()
                        )
                        .thenComparing(
                                (RankingSource.Record record) ->
                                        record.getRun().getFinalHp(),
                                Comparator.reverseOrder()
                        )
                        .thenComparingLong(RankingSource.Record::getId)
        );
    }

    // 테스트 확인용 코드
    public void checkRankingRecords() {

        RankingSource source = rankingClient.fetch();

        List<RankingSource.Record> candidates = new ArrayList<>();
        List<RankingSource.Record> validRecords = new ArrayList<>();

        int excludedCount = 0;

        for (RankingSource.Record record : source.getRecords()) {

            // 순위 대상 자체가 아니면 제외
            if (!isRankingCandidate(record)) {
                continue;
            }

            candidates.add(record);

            // 순위 대상이지만 이상 기록이면 excludedCount 증가
            if (!isValidRecord(record)) {
                excludedCount++;
                continue;
            }

            // 정상 기록
            validRecords.add(record);
        }

        System.out.println("===== 랭킹 검증 =====");
        System.out.println("전체 기록: " + source.getRecords().size());
        System.out.println("순위 대상: " + candidates.size());
        System.out.println("이상 기록 제외: " + excludedCount);
        System.out.println("정상 기록: " + validRecords.size());
        System.out.println("====================");
    }
}
