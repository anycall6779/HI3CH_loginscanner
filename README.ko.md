# Honkai Link 0.6.0 — Android 개발 프리뷰

모바일 게임 실행 없이 중국 붕괴3 Bilibili 채널의 PC QR 로그인을 준비하는 비공식 앱입니다. Android 8.0 이상. 네이비·민트 카드형 UI와 홈 / 스캔 / 기록 / 설정 탭으로 구성했습니다.

## 0.6 변경

고해상도 프리뷰·다중 영역·명암 처리·자동 확대를 추가했습니다. PC 표시명은 HONKAI QR이며 설정에 개발자 블로그를 넣었습니다. 앱 내 소스 저장소 링크는 넣지 않고 라이선스 고지만 유지합니다. 상세 검사는 SCAN-0.6.ko.md에 있습니다.

## 호환 기능

- 계정·SMS 인증, Bilibili SSO/구형 호환 경로, 서버 제공 시 활성화되는 Apple 인증.
- 암호화 보관 계정 최대 20개, 전환·개별 삭제·로컬 별칭.
- 카메라 / 선택한 이미지 / 주소 붙여넣기로 QR 판독. 명시적으로 확인한 후 승인.
- 최근 50개의 승인 API 결과와 시각만 암호화 저장. 토큰·계정 ID·QR 원문 기록 안 함.
- 원본 지원 채널 호환표. 실제 이식되지 않은 채널을 사용 가능으로 표시하지 않음.

자세한 이식 범위는 FEATURES.ko.md를 참고하세요. Apple은 현재 확인한 서버에서 제공하지 않으며 SSO 오류 해결은 미확인입니다. 다른 채널의 실제 인증 어댑터는 아직 구현되지 않았습니다.

## 공개 배포 준비 미완료

현재 개발 APK는 기기 로그에서 얻은 9.1.0 dispatch fixture에 의존합니다. 독립 갱신과 배포용 서명이 남아 있어 일반 사용자용 완성판으로 배포할 수 있는 상태가 아닙니다.

공개 준비용 HonkaiLink-public-source.zip에서는 fixture, 서명 키, 로그 원문, 계정 파일을 제외했습니다. 소스는 컴파일할 수 있지만 공식 dispatch 공급 기능 구현 전에는 PC 승인 기능이 설정 누락으로 중단됩니다. RELEASE-BLOCKERS.ko.md를 확인하세요.

## 빌드

필요: JDK 17, Android SDK Platform 34, Build Tools 36.0.0, PowerShell.

```powershell
.\build.ps1
# SDK 경로가 다르면
.\build.ps1 -Sdk 'C:\PATH\TO\Android\Sdk'
adb install -r .\BiliLight-debug.apk
adb shell am start -S -W -n dev.local.bililight/.MainActivity
# 실제 계정 파일 대신 별도 합성 자료로 검사
adb shell am start -S -W -n dev.local.bililight/.MainActivity --ez local_tests true
# 실제 프로필을 읽지 않고 인증 네트워크를 차단하는 디자인 미리보기
adb shell am start -S -W -n dev.local.bililight/.MainActivity --ez ui_preview true
```

빌드 스크립트는 ZXing core 3.5.3을 Maven Central에서 받고 SHA-256을 확인합니다. Gradle 없이 javac/D8/aapt2/apksigner를 사용합니다. 중간 파일과 로컬 디버그 키는 작업공간의 work/bililight-build에 두고 매번 새 컴파일 디렉터리를 사용합니다. 같은 설치를 업데이트하려면 서명 키를 유지해야 합니다.

## 검사

- Android 테스트 기기 설치·APK v2/v3 서명 검증 통과.
- 합성 계정의 기존 형식 변환·전환·삭제·별칭·암호화 저장 검사 통과.
- 합성 이미지 QR 판독 및 SDK/게임 서명 벡터 검사 통과.
- 실제 보관 계정이 앱 재시작 후 유지되는 것 확인.
- 홈·계정·스캔 화면을 실제 기기의 개인정보 없는 preview 모드로 렌더링해 확인.
- 이전 개발판의 PC B服 실제 접속은 사용자가 확인. 새 기능 전체를 실제 여러 계정으로 검증한 것은 아닙니다.

## 개인정보와 고지

현재 앱의 전용 보관함만 사용하고 원래 게임 계정 파일을 읽지 않습니다. PRIVACY.ko.md를 참고하세요. 비밀번호와 SMS 코드는 저장하지 않습니다.

HonkaiScanner/bh3_login_simulation-memories의 공개/과거 구현과 설치본 9.1.0 SDK 5.9.0을 참고한 별도 최소 구현입니다. ZXing은 Apache-2.0이며 참고 프로젝트 MIT 고지를 보존했습니다. 고지는 licenses/와 앱 설정에 포함됩니다. 실제 배포 주체·연락처·업데이트 경로와 배포 조건은 공개 배포 전에 확정해야 합니다.
