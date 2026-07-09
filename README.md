# 알바 관리 · 사장 앱 (PTManagerEmployer)

사장(관리자)용 매장 운영 안드로이드 앱. 매장 현황을 한눈에 보고, 스케줄을 편성하고, 대타 승인·근태·인건비를 관리하는 데 초점을 둔 구성입니다.

공통 백엔드([PTManagerBackend](../PTManagerBackend), Spring Boot · JWT)에 연결되어 매장 생성부터 근무 편성·대타 승인·가입 승인·인건비 집계까지 실제 REST API로 동작합니다. (RBAC로 사장 전용 API 강제)

> 알바(직원)용은 [PTManagerEmployee](../PTManagerEmployee)를 참고하세요. 같은 백엔드·데이터를 공유하되 화면은 역할별로 분리되어 있으며, 사장 앱은 **보라(#7048E8)**를 메인 컬러로 사용합니다.

## 화면 구성 (하단 5탭)

| 탭 | 설명 |
|---|---|
| **홈** | 지금 근무 중(현재 근무자)·스케줄 짜기, 오늘/금주 인건비, 소식(공지·인수인계·대타요청) |
| **스케줄** | 주간 스케줄 편성, 자동 편성, 발행 |
| **승인** | 대타요청 승인/거절. 대기 중은 인라인 노출, 헤더 → 대타요청 내역(대기 중·처리 완료 함께 표시) |
| **통계** | 인건비 통계 — 이번 달 누적(실근태 기준), 알바별 인건비 |
| **내 정보** | 프로필 수정, 인건비 확인, 멤버·승인 관리, 알림 설정, 로그아웃 |

### 주요 화면 (탭 외)
- **로그인 / 회원가입** — 이메일·비밀번호 기반(JWT), EMPLOYER 역할로 가입. 매장 미생성 시 매장 생성 화면으로 진입
- **공지 작성** — 제목·내용 입력 후 발행(`POST /notices`)
- **근태 현황** — 오늘 근무의 출근/지각/결근 집계 및 직원별 상태
- **매장 생성 · 멤버 관리** — 매장 생성 시 초대코드 자동 발급, 가입 신청 승인/거절(`PATCH /join-requests/{id}`)
- **인건비** — 실제 근태 기반 월 인건비 집계(`GET /payroll`)

## 기술 스택
- Kotlin, View 기반 XML 레이아웃 (Compose 미사용)
- 하단 탭 네비게이션 (`BottomNavigationView` + Fragment 전환)
- 네트워킹: Retrofit2 + OkHttp(로깅·인증 인터셉터) + Gson, Kotlin Coroutines
- 인증: JWT 액세스/리프레시 토큰을 `SharedPreferences`(`TokenStore`)에 저장, 요청 시 `Authorization: Bearer` 자동 부착
- `applicationId` : `com.example.ptmanageremployer`
- minSdk 35 / targetSdk 36, 라이트 전용 테마

## 백엔드 연동
- 데이터 계층: `com.example.ptmanageremployer.data` (`Network`·`ApiService`·`TokenStore`·`Dtos`)
- Base URL은 `local.properties`(버전관리 제외)의 `base.url` 값을 빌드 시 `BuildConfig.BASE_URL`로 주입합니다. 키가 없으면 기본값으로 폴백합니다.
  ```properties
  # local.properties
  base.url=http://10.0.2.2:8080/   # 에뮬레이터 → 호스트 PC의 localhost
  ```
  실제 기기/운영에서는 이 값을 서버 주소로 바꾸면 됩니다(소스 수정 불필요). 평문 HTTP 허용을 위해 `usesCleartextTraffic=true` 설정
- 메신저·FCM 푸시, 스케줄 편성 폼은 미연동 단계입니다(스케줄 탭의 "+" 버튼은 첫 알바를 데모 근무로 편성하는 임시 동작).

## 빌드 & 실행
```bash
# 1) 백엔드 먼저 기동 (별도 터미널)
cd ../PTManagerBackend && ./gradlew bootRun   # H2 인메모리, 시드 데이터 자동 생성

# 2) 앱 빌드/설치
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # 연결된 기기/에뮬레이터에 설치
```

시드 계정으로 바로 로그인할 수 있습니다 — 사장: `employer@ptmanager.test` / `password` (시드 매장 보유). 새 매장을 만들려면 회원가입 후 매장 생성 화면에서 진행하세요.
