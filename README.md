# PLEtter MVP

차 안에서 남긴 음성 편지를, 같은 차를 쓰는 사람이 다음에 시동을 걸 때 전달하는 차량용 앱의 MVP입니다.
기획서의 세 전달 유형 중 **유형 ① 같은 차**만 온디바이스로 구현했습니다.

## 실행 방법

1. Android Studio(Koala 이상)에서 이 폴더를 엽니다. Gradle 동기화가 끝나면 `app` 구성을 실행합니다.
2. 에뮬레이터는 Pleos Connect 에뮬레이터, Android Automotive(API 34) 또는 일반 태블릿(가로) AVD 모두 가능합니다.
3. 에뮬레이터 설정에서 마이크 입력(호스트 마이크 사용)을 켜야 실제 목소리가 녹음됩니다.

- 요구 사항: JDK 17, compileSdk 34, minSdk 28
- 명령줄 빌드: `./gradlew assembleDebug` (Gradle 8.7 래퍼 포함)

## 시연 순서 (약 1분)

1. 프로필 두 개를 만듭니다(예: A, B · 각자 PIN 4자리).
2. A로 본인 확인 → **편지 남기기** → 녹음 → 확인 → 받을 사람 B → **예약하기**.
3. 하단 바의 **새 시동(모의)** 을 누릅니다.
4. A로 다시 들어가면 편지가 보이지 않습니다(다른 사람이 먼저 타면 대기).
5. **새 시동(모의)** 후 B로 본인 확인 → "새 편지 1통이 도착했어요" → 편지함 → 열기 → **목소리로 듣기**.
6. 하단 바를 **D 주행**으로 바꾸면 편지 열람과 녹음이 보류되는 것을 확인할 수 있습니다.

## 구현된 것

| 기획서 항목 | MVP 동작 |
| --- | --- |
| 녹음·확인 | 버튼을 누른 동안만 녹음, 녹음 중 표시, 동승자 안내, 원본 다시 듣기 |
| 예약 | 받을 사람(앱 내 프로필) 지정, 전달 조건은 '다음 시동 때' 고정 |
| 통제 | 전달 전 예약 취소, 수신자의 편지 삭제 |
| 본인 확인 | 프로필 선택 + PIN. 확인 전에는 어떤 편지도 표시하지 않음 |
| 전달 판정 | 예약한 세션보다 뒤의 시동 세션에서 지정 수신자가 확인되면 전달, 편지당 1회 |
| 전달과 재생의 분리 | 도착만 알리고 자동 재생·자동 펼침 없음 |
| 주행 상황 고려 | 주행 중(P 아님) 녹음·열람 보류, 열람 중 주행 시작 시 재생 중지 |
| 오디오 | 재생 시 오디오 포커스 요청, 포커스를 잃으면 즉시 중지 |

## 모의로 처리한 것 (실제 연동 아님)

- **차량 상태**: 기어(P/D)와 시동은 화면 하단 '모의 차량 상태' 바에서 손으로 바꿉니다. Pleos Vehicle SDK는 붙어 있지 않습니다.
- **시동 세션**: 앱을 새로 실행하면 새 세션으로 봅니다(기획서의 '시동 후 첫 앱 실행' 대체 흐름). 시연에서는 '새 시동(모의)' 버튼을 씁니다.
- **전사**: 자동 전사(STT)는 없습니다. 발신자가 직접 입력한 글이 함께 전달됩니다.

## 범위 밖

유형 ② 다른 차, 유형 ③ 스마트폰(문자·모바일 웹), 백엔드, 저장 데이터 암호화, 수신 거부·차단, 보관함·비밀 일기.

## Pleos SDK를 붙일 자리

| 위치 | 할 일 |
| --- | --- |
| `settings.gradle.kts` | Pleos Playground Maven 저장소 추가 |
| `vehicle/VehicleState.kt` | `VehicleStateProvider`를 Vehicle SDK(기어·시동 구독)로 구현하고 `MainActivity`에서 주입 |
| `MainActivity.kt` | 시동 이벤트를 받을 수 있으면 `store.startNewSession()` 호출 시점을 그쪽으로 이동 |
| `ui/WriteScreens.kt`의 `ReviewScreen` | SpeechToText SDK 제공 확인 후 자동 전사 연결 |
| `ui/PletterApp.kt`의 `MockVehicleBar` | 실제 연동 후 제거 |

## 구조

```
app/src/main/java/com/jnr/pletter/
├── MainActivity.kt
├── data/        Models.kt, LetterStore.kt   (JSON 파일 저장, 전달 판정)
├── audio/       VoiceRecorder.kt, LetterPlayer.kt
├── vehicle/     VehicleState.kt             (인터페이스 + 모의 구현)
└── ui/          PletterApp.kt, DriverScreen.kt, HomeScreen.kt,
                 WriteScreens.kt, BoxScreens.kt, Common.kt
```
