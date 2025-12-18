# API 통합 테스트 리팩터링 TODO (2025-12-17)

## 📋 개요
API 통합 테스트(47개) 구현 완료했으나, 코드 품질 개선을 위한 리팩터링 필요함

## 🎯 리팩터링 목표
- 코드 중복 감소 (목표: 60% 이상)
- 일관된 테스트 아키텍처 통합
- 유지보수성 및 재사용성 향상

## 🏗️ 구현 계획

### 1단계: 기반 구조 개선 (1시간)
- [x] AbstractIntegrationTest 생성 (AbstractRepositoryTest 상속)
- [ ] @DynamicPropertySource로 Testcontainer 통합
- [ ] testutils 패키지 구조 생성

### 2단계: 공통 코드 분리 (1.5시간)
- [ ] TestDataBuilder → EntityBuilder/ResponseBuilder 분리
- [ ] MockServiceSetup 유틸리티 구현
- [ ] Assertion 클래스 체계화

### 3단계: 기존 테스트 리팩터링 (1.5시간)
- [ ] SettlementControllerTest 정리
- [ ] AdminSettlementControllerTest 정리
- [ ] 불필요한 코드 제거

## 📁 주요 파일 목록

### 신규 생성
```
src/test/java/com/company/settlement/testutils/
├── config/TestConfig.java
├── builder/EntityBuilder.java
├── builder/ResponseBuilder.java
├── mock/MockServiceSetup.java
├── assertion/ControllerAssertions.java
└── fixture/TestAuthentication.java
```

### 기존 수정
- AbstractControllerTest.java
- SettlementControllerTest.java
- AdminSettlementControllerTest.java

## ✅ 성공 기준
- [ ] 모든 기존 테스트 정상 실행 (47개)
- [ ] 코드 중복률 60% 이상 감소
- [ ] 일관된 테스트 아키텍처 확보

## 💡 기타 메모
- 총 예상 시간: 3-4시간
- 리스크 관리: 점진적 리팩터링으로 regression 방지
- 참고: C:\Users\luzta\.claude\plans\mossy-moseying-babbage.md