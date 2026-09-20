# Play 스토어 등록 에셋

## hi_res_icon_512.png
Play Console 등록정보의 "Hi-res icon" 필드용. 512x512, 32비트 PNG(알파 포함). 앱의 실제 adaptive icon(`ic_launcher_foreground.xml` + `ic_launcher_background.xml`)과 동일한 벡터 좌표를 그대로 스케일링해 렌더링했습니다.

## screenshots/
실기기(Samsung SM-S711N, 1080x2340)에서 촬영한 실제 스크린샷입니다.

| 파일 | 내용 |
|---|---|
| `01_main_dark.png` | 메인 화면, 다크 칠판 테마, 샘플 할 일(완료/중요 표시 포함) |
| `02_settings_dark.png` | 설정 화면, 다크 테마 |
| `03_main_light.png` | 메인 화면, 라이트(줄공책) 테마 |
| `04_settings_light.png` | 설정 화면, 라이트 테마 |
| `05_widget.png` | 홈 화면 위젯 |

**⚠️ 업로드 전 확인할 것**: 원본 해상도는 1080x2340(비율 약 1:2.17)입니다. Play Console의 스토어 등록정보 스크린샷 규격은 일반적으로 긴 변이 짧은 변의 2배를 넘지 않아야 합니다(16:9~9:16). 업로드 시 Play Console이 거부하면 위/아래를 살짝 크롭하거나 레터박스를 추가해 비율을 맞춰야 합니다.

샘플 할 일("Buy groceries" 등)은 영문 placeholder입니다 — `adb shell input text`가 이 기기에서 한글 입력을 지원하지 않아 데모용으로만 영문을 사용했습니다. 실제 한글 샘플로 다시 찍고 싶다면 기기에서 직접 할 일을 입력한 뒤 스크린샷을 다시 촬영하면 됩니다.
