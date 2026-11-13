# 0.7.0-beta 검사 기록

## 완료

- 별도 패키지의 최초 설치 성공. 기존 개발판·원래 게임의 데이터는 가져오지 않음.
- APK v2/v3 서명 검증 통과. 배포용 별도 키 사용, 키는 저장소 불포함.
- 실제 Android에서 동의 후 설정 조회 HTTP 성공: dispatch_provider retcode=0 chars=3200.
- 동의 철회 후 조회: 네트워크 요청 전 `설정에서 원본 설정 서비스 사용에 동의하세요`로 차단.
- DISPATCH_VALIDATION_TESTS_OK: 유효 구조·오류코드·짧은 암호문·잘못된 Base64·누락 retcode.
- LOCAL_PROFILE_IMAGE_SIGNATURE_TESTS_OK: 합성 데이터로 다중 계정·암호화·이미지·서명 벡터 검사.
- SMALL_QR_TEST_OK: 1920×1080 화면 내 144×144 저명암 QR(130:150), baseline 실패/개선 경로 성공, 비중앙 위치 성공. 실제 거리 보장 아님.
- APK에 fixture/키/계정파일/로그 및 github.com 문자열 없음.

## 미완료

- 이 별도 패키지에서 사용자의 새 계정 인증 → 새 PC QR 승인 → 실제 B服 게임 진입. 이전 fixture 경로의 성공과 구분.
- 실제 복수 계정·SSO·만료·추가 인증·다른 게임 버전 전체 검증.
- 설정 암호문의 내용/발행자 서명 독립 검증. 구조 확인과 신뢰된 외부 서비스에 의존.

APK는 PC 실접속 검증 전 GitHub Release로 공개하지 않음.
