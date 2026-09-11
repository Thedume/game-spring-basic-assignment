# 내일 배움 캠프 입문 주차 프로젝트

Spring Boot와 MySQL을 기반으로 구현한 게임 서버 과제입니다.  
게임 생성 및 진행 저장, 게임 목록/상세 조회, 이름 변경 및 삭제, 전역 예외 처리, 랭킹 조회 기능을 구현했습니다.

## 실행 환경

MySQL은 Docker 컨테이너로 실행하고, Spring Boot 애플리케이션은 IntelliJ에서 실행하는 방식으로 구성했습니다.

```bash
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=12345678 -p 3306:3306 mysql:8.4
```

`application.properties` 예시:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/{database}
spring.datasource.username=root
spring.datasource.password=12345678

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 구현 기능

### 게임 생성

`POST /games`

- 플레이어 이름과 시작 덱을 전달받아 새로운 게임 생성
- `Game`과 `RunCard`를 하나의 트랜잭션으로 저장
- 생성된 게임 상세 정보를 응답
- Bean Validation을 이용한 요청값 검증

### 게임 진행 저장

`PUT /games/{gameId}/progress`

- 현재 HP, 층, 진행 단계, 상태 저장
- 기존 덱을 삭제한 뒤 요청받은 전체 덱으로 교체
- 진행 저장과 덱 저장을 하나의 트랜잭션으로 처리
- 이미 `CLEARED` 또는 `FAILED`인 게임은 수정하지 않고 `409 Conflict` 반환

### 게임 목록 조회

`GET /games`

- 저장된 모든 게임 조회
- 게임 ID 기준 내림차순 정렬
- 덱 전체 대신 `deckSize` 반환
- `createdAt`, `updatedAt` 포함
- 카드 수를 `GROUP BY` 집계 쿼리로 조회하여 N+1 문제 방지

### 게임 상세 조회

`GET /games/{gameId}`

- 특정 게임의 상세 정보 조회
- 저장된 전체 덱 포함
- 카드 ID 기준 오름차순 정렬
- 존재하지 않는 게임은 `404 Not Found` 반환

### 플레이어 이름 변경

`PATCH /games/{gameId}`

- 플레이어 이름만 변경
- `Game.rename()`과 JPA Dirty Checking을 이용해 UPDATE 처리
- 성공 시 `204 No Content`

### 게임 삭제

`DELETE /games/{gameId}`

- 지정된 게임과 해당 게임에 속한 모든 `RunCard` 삭제
- 카드 삭제와 게임 삭제를 하나의 트랜잭션으로 처리
- 성공 시 `204 No Content`

## 저장 시간 관리

`Game` 엔티티에 `BaseEntity`를 적용해 생성 시간과 마지막 수정 시간을 관리합니다.

- `createdAt`: 게임 생성 시각
- `updatedAt`: 마지막 저장 시각

Spring Data JPA Auditing을 이용해 자동으로 저장 및 갱신되도록 구성했습니다.

## 전역 예외 처리

`GlobalExceptionHandler`를 통해 API 오류 응답 형식을 통일했습니다.

- `GameNotFoundException` → `404 Not Found`
- `GameFinishedException` → `409 Conflict`
- Bean Validation 실패 → `400 Bad Request`
- 잘못된 요청 본문/파라미터 형식 → `400 Bad Request`

에러 응답 예시:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "게임을 찾을 수 없습니다.",
  "path": "/games/999"
}
```

## 랭킹 조회

`GET /rankings`

외부 랭킹 API의 데이터를 `RestClient`로 조회한 뒤 서버에서 검증, 필터링, 정렬하여 최종 랭킹을 반환합니다.

### 랭킹 대상

- `run.status == CLEARED`
- `run.clearedFloor == 10`

### 이상 기록 필터링

- 클리어 시간: 층당 최소 30초
- 남은 HP: 1~99
- 덱 크기: 9~20장
- `deck.size`와 실제 카드 개수 일치
- 모든 카드 타입이 허용된 `CardType`에 존재
- 카드 획득 층: 0~9
- 보스 페이즈: `THRONE → UNBOUND → ECLIPSE`
- 각 보스 페이즈 턴 수가 1 이상
- `bossFight.totalTurns`와 실제 페이즈 턴 합 일치
- 마무리 카드가 실제 덱에 존재

### 랭킹 정렬

정상 기록은 아래 우선순위로 정렬합니다.

1. 클리어 시간 오름차순
2. 남은 HP 내림차순
3. 기록 ID 오름차순

같은 플레이어가 여러 기록을 제출한 경우 가장 높은 순위의 기록 하나만 유지하고, 최종 결과에 1부터 순위를 부여합니다.

## API 요약

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/games` | 새 게임 생성 |
| `GET` | `/games` | 게임 목록 조회 |
| `GET` | `/games/{gameId}` | 게임 상세 조회 |
| `PUT` | `/games/{gameId}/progress` | 게임 진행 및 전체 덱 저장 |
| `PATCH` | `/games/{gameId}` | 플레이어 이름 변경 |
| `DELETE` | `/games/{gameId}` | 게임 삭제 |
| `GET` | `/rankings` | 외부 데이터를 가공한 랭킹 조회 |


## ERD

![ERD](./resources/erd.png)

- Game과 RunCard는 1:N 관계입니다.
- RunCard의 game_id는 Game의 id를 참조합니다.
- 게임 삭제 시 해당 게임에 속한 RunCard도 함께 삭제합니다.


## 프로젝트 구조

```text
com.gamebasic
├─ common
│  ├─ dto
│  ├─ entity
│  └─ exception
├─ game
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ repository
│  └─ service
├─ runcard
│  ├─ dto
│  ├─ entity
│  └─ repository
└─ ranking
   ├─ client
   ├─ controller
   ├─ dto
   └─ service
```

Controller는 HTTP 요청/응답, Service는 비즈니스 로직과 트랜잭션, Repository는 데이터베이스 접근을 담당하도록 역할을 분리했습니다.
