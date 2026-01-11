# Web Test Platform

JUnit 5 기반 테스트 자동화 플랫폼. 웹 UI에서 테스트를 발견, 실행, 결과 조회.

---

## 핵심 기능

| 기능 | 설명 |
|------|------|
| 테스트 발견 | 테스트 코드 스캔 후 트리로 표시 |
| 테스트 실행 | 선택한 테스트 클래스 비동기 실행 |
| 결과 조회 | 성공/실패/에러/stdout 확인 |
| 소스 코드 보기 | 테스트 메서드 소스 코드 조회 |
| 실행 이력 | 과거 실행 결과 DB 저장 및 조회 |

---

## 왜 이 플랫폼인가?

- **웹 브라우저만 있으면** 테스트 실행 가능
- **테스트 코드 수정해도 앱 재시작 불필요** (별도 JVM에서 실행)
- **누구나** 테스트 실행 가능 (QA, 기획자 등)
- **결과가 DB에 저장**되어 이력 관리 가능

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.5.7, Java 17 |
| Frontend | Thymeleaf, Vanilla JS |
| Database | MySQL |
| Test Framework | JUnit 5 (Jupiter + Platform) |
| Build | Gradle (Kotlin DSL) |
| Code Parsing | JavaParser 3.26.3 |

---

## 빠른 시작

### 1. 데이터베이스 준비

```sql
CREATE DATABASE bng000a;
```

`sql/` 디렉토리의 DDL 스크립트 실행.

### 2. 테스트 코드 프로젝트 준비

```bash
# 템플릿 복사
cp -r testcode-template /path/to/testcodes

# 또는 환경변수로 경로 지정
export TESTCODE_PROJECT_PATH=/path/to/testcodes
export TESTCODE_ROOT_PACKAGE=testauto.testcode
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. 접속

브라우저에서 `http://localhost:9898` 접속

---

## 프로젝트 구조

```
project-web-test-platform/
├── src/main/java/testauto/
│   ├── TestAutoApplication.java      # Spring Boot 진입점
│   ├── controller/                   # REST API 컨트롤러
│   ├── service/                      # 비즈니스 로직
│   ├── repository/                   # 데이터 접근
│   ├── domain/                       # 도메인 모델
│   ├── dto/                          # 데이터 전송 객체
│   ├── runner/                       # 별도 JVM용 테스트 러너
│   └── exception/                    # 예외 처리
│
├── src/main/resources/
│   ├── application.yml               # 애플리케이션 설정
│   ├── templates/                    # Thymeleaf 템플릿
│   └── static/                       # CSS, JS, 이미지
│
├── sql/                              # DB DDL 스크립트
├── testcode-template/                # 테스트 코드 프로젝트 템플릿
└── docs/                             # 상세 문서
    ├── ARCHITECTURE.md               # 아키텍처 상세
    ├── API.md                        # API 명세
    ├── DATABASE.md                   # DB 스키마
    └── TROUBLESHOOTING.md            # 이슈 및 해결
```

---

## 설정

### application.yml 주요 설정

```yaml
server:
  port: 9898

testcode:
  project-path: ${TESTCODE_PROJECT_PATH:/path/to/testcodes}
  root-package: ${TESTCODE_ROOT_PACKAGE:testauto.testcode}

spring:
  datasource:
    url: jdbc:mysql://localhost:3306
    username: root
    password: root
```

### 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `TESTCODE_PROJECT_PATH` | 테스트 코드 프로젝트 경로 | `/path/to/testcodes` |
| `TESTCODE_ROOT_PACKAGE` | 테스트 루트 패키지 | `testauto.testcode` |

---

## 상세 문서

- [아키텍처](docs/ARCHITECTURE.md) - 시스템 구조, 핵심 설계, 데이터 흐름
- [API 명세](docs/API.md) - REST API 엔드포인트 상세
- [데이터베이스](docs/DATABASE.md) - 테이블 스키마 및 관계
- [트러블슈팅](docs/TROUBLESHOOTING.md) - 기술적 이슈 및 해결 방법
- [JUnit Platform 가이드](ABOUT_JUNIT_PLATFORM.md) - JUnit Platform API 활용 가이드

---

## 화면 구성

### 메인 화면

```
┌─────────────────────────────────────────────────────────────────┐
│  Header (로고, 타이틀)                                           │
├─────────────┬───────────────────────────────────────────────────┤
│             │                                                   │
│  Sidebar    │  Main Content                                     │
│             │                                                   │
│  - 새로고침  │  [Home] [Test Info] [Test Results]               │
│  - 실행     │                                                   │
│  - 검색     │  - Home: 최근 실행 이력                            │
│  - 테스트   │  - Test Info: 클래스/메서드 정보                    │
│    트리     │  - Test Results: 실행 결과 트리                    │
│             │                                                   │
└─────────────┴───────────────────────────────────────────────────┘
```

### 사이드바 기능

| 요소 | 기능 |
|------|------|
| 새로고침 버튼 | 테스트 코드 컴파일 + 카탈로그 갱신 |
| 실행 버튼 | 체크된 클래스 테스트 실행 |
| 검색창 | 클래스명으로 필터링 |
| 트리 뷰 | 패키지 → 클래스 계층 구조 |
| 체크박스 | 실행할 클래스 선택 |

---

## 워크플로우

```
[테스트 코드 수정]
       │
       ▼
[새로고침 버튼 클릭]
       │ POST /api/tests/refresh
       │ → gradle compileJava
       │ → 별도 JVM에서 테스트 발견
       │ → DB 저장
       ▼
[테스트 선택 → 실행 버튼]
       │ POST /api/tests/run
       │ → executionId 즉시 반환
       │ → 비동기로 별도 JVM에서 실행
       ▼
[결과 조회]
       │ GET /api/tests/executions/{id}/results
       │ → 트리 형태 결과 + 요약
       ▼
[성공/실패/stdout/에러 확인]
```
