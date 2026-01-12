# REST API 레퍼런스

이 문서는 Web Test Platform의 REST API 엔드포인트를 설명합니다.

---

## 기본 정보

- **Base URL**: `http://localhost:9898`
- **Content-Type**: `application/json`
- **인코딩**: UTF-8

---

## 엔드포인트 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/tests/tree` | 테스트 트리 조회 |
| POST | `/api/tests/refresh` | 카탈로그 새로고침 |
| GET | `/api/tests/class/{className}` | 클래스 상세 정보 |
| POST | `/api/tests/run` | 테스트 실행 |
| GET | `/api/tests/method/code` | 메서드 소스 코드 |
| GET | `/api/tests/server-time` | 서버 현재 시간 |
| GET | `/api/tests/executions` | 실행 이력 목록 |
| GET | `/api/tests/executions/{executionId}` | 특정 실행 조회 |
| GET | `/api/tests/executions/{executionId}/results` | 실행 결과 조회 |

---

## 1. 테스트 트리 조회

발견된 테스트를 패키지/클래스 트리 구조로 조회합니다.

### Request

```http
GET /api/tests/tree
```

### Response

```json
{
  "name": "testcode",
  "type": "PACKAGE",
  "children": [
    {
      "name": "e2e",
      "type": "PACKAGE",
      "children": [
        {
          "name": "LoginTest",
          "type": "CLASS",
          "className": "testauto.testcode.e2e.LoginTest",
          "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]",
          "children": []
        },
        {
          "name": "CheckoutTest",
          "type": "CLASS",
          "className": "testauto.testcode.e2e.CheckoutTest",
          "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.CheckoutTest]",
          "children": []
        }
      ]
    },
    {
      "name": "unit",
      "type": "PACKAGE",
      "children": [
        {
          "name": "CalculatorTest",
          "type": "CLASS",
          "className": "testauto.testcode.unit.CalculatorTest",
          "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.unit.CalculatorTest]",
          "children": []
        }
      ]
    }
  ]
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 노드 이름 (패키지명 또는 클래스명) |
| `type` | string | 노드 타입: `PACKAGE` 또는 `CLASS` |
| `className` | string? | 클래스의 풀 네임 (CLASS 타입인 경우) |
| `uniqueId` | string? | JUnit Platform UniqueId (CLASS 타입인 경우) |
| `children` | array | 자식 노드 목록 |

---

## 2. 카탈로그 새로고침

테스트 코드를 컴파일하고 테스트를 다시 발견하여 DB에 저장합니다.

### Request

```http
POST /api/tests/refresh
```

### Response

```http
HTTP/1.1 200 OK
```

### 동작

1. 테스트 코드 프로젝트에서 `gradle compileJava` 실행
2. 별도 JVM에서 테스트 발견 (`TestRunner discover`)
3. 기존 카탈로그 삭제 후 새 결과 저장

### 에러

```json
{
  "status": 500,
  "message": "Failed to refresh test catalog: Compilation failed",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 3. 클래스 상세 정보

특정 테스트 클래스의 상세 정보 (메서드 목록, nested class 등)를 조회합니다.

### Request

```http
GET /api/tests/class/{className}
```

**Path Parameters:**
- `className`: 클래스의 풀 네임 (예: `testauto.testcode.e2e.LoginTest`)

### Response

```json
{
  "className": "LoginTest",
  "fullClassName": "testauto.testcode.e2e.LoginTest",
  "methods": [
    {
      "methodName": "testLoginSuccess",
      "displayName": "로그인 성공 테스트",
      "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[method:testLoginSuccess()]",
      "isNestedClass": false,
      "children": []
    },
    {
      "methodName": "testLoginFailure",
      "displayName": "로그인 실패 테스트",
      "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[method:testLoginFailure()]",
      "isNestedClass": false,
      "children": []
    },
    {
      "methodName": "EdgeCases",
      "displayName": "Edge Cases",
      "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[nested-class:EdgeCases]",
      "isNestedClass": true,
      "children": [
        {
          "methodName": "testEmptyPassword",
          "displayName": "빈 비밀번호 테스트",
          "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[nested-class:EdgeCases]/[method:testEmptyPassword()]",
          "isNestedClass": false,
          "children": []
        }
      ]
    }
  ]
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `className` | string | 간단한 클래스명 |
| `fullClassName` | string | 패키지를 포함한 풀 클래스명 |
| `methods` | array | 테스트 메서드 및 nested class 목록 |
| `methods[].methodName` | string | 메서드/클래스 이름 |
| `methods[].displayName` | string | 표시 이름 (@DisplayName) |
| `methods[].uniqueId` | string | JUnit Platform UniqueId |
| `methods[].isNestedClass` | boolean | nested class 여부 |
| `methods[].children` | array | nested class인 경우 내부 메서드 목록 |

---

## 4. 테스트 실행

지정된 테스트 클래스들을 비동기로 실행합니다.

### Request

```http
POST /api/tests/run
Content-Type: application/json

{
  "classNames": [
    "testauto.testcode.e2e.LoginTest",
    "testauto.testcode.e2e.CheckoutTest"
  ]
}
```

**Request Body:**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `classNames` | array | O | 실행할 테스트 클래스들의 풀 네임 목록 |

### Response

```json
{
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Test execution started"
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `executionId` | string | 실행 ID (UUID). 결과 조회시 사용 |
| `status` | string | 현재 상태: `RUNNING` |
| `message` | string | 메시지 |

### 동작

1. UUID 생성
2. 실행 기록 DB 저장 (status=RUNNING)
3. 비동기로 테스트 실행 시작
4. executionId 즉시 반환 (실행 완료를 기다리지 않음)

### 에러

```json
{
  "status": 400,
  "message": "At least one class name is required",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 5. 메서드 소스 코드 조회

테스트 메서드의 소스 코드를 조회합니다.

### Request

```http
GET /api/tests/method/code?uniqueId={uniqueId}
```

**Query Parameters:**
- `uniqueId`: 메서드의 JUnit Platform UniqueId (URL 인코딩 필요)

### 예시

```http
GET /api/tests/method/code?uniqueId=%5Bengine%3Ajunit-jupiter%5D%2F%5Bclass%3Atestauto.testcode.e2e.LoginTest%5D%2F%5Bmethod%3AtestLoginSuccess()%5D
```

### Response

```json
{
  "code": "@Test\n@DisplayName(\"로그인 성공 테스트\")\nvoid testLoginSuccess() {\n    // 테스트 로직\n    String result = loginService.login(\"user\", \"password\");\n    assertThat(result).isEqualTo(\"success\");\n}"
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | string | 메서드 소스 코드 (어노테이션 포함) |

---

## 6. 서버 현재 시간

서버의 현재 날짜를 조회합니다.

### Request

```http
GET /api/tests/server-time
```

### Response

```json
{
  "date": "2024-01-15"
}
```

---

## 7. 실행 이력 목록

최근 테스트 실행 이력을 조회합니다.

### Request

```http
GET /api/tests/executions
GET /api/tests/executions?limit=20
```

**Query Parameters:**
- `limit` (optional): 조회할 최대 개수 (기본값: 10)

### Response

```json
[
  {
    "executionId": "550e8400-e29b-41d4-a716-446655440000",
    "startedAt": "2024-01-15T10:30:00",
    "finishedAt": "2024-01-15T10:30:15",
    "totalTests": 10,
    "successCount": 9,
    "failedCount": 1,
    "skippedCount": 0,
    "totalDurationMillis": 15234,
    "requesterIp": "192.168.1.100",
    "classNames": "testauto.testcode.e2e.LoginTest,testauto.testcode.e2e.CheckoutTest",
    "status": "COMPLETED"
  },
  {
    "executionId": "660f9500-f30c-52e5-b827-557766551111",
    "startedAt": "2024-01-15T09:00:00",
    "finishedAt": "2024-01-15T09:00:05",
    "totalTests": 5,
    "successCount": 5,
    "failedCount": 0,
    "skippedCount": 0,
    "totalDurationMillis": 5123,
    "requesterIp": "192.168.1.101",
    "classNames": "testauto.testcode.unit.CalculatorTest",
    "status": "COMPLETED"
  }
]
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `executionId` | string | 실행 ID (UUID) |
| `startedAt` | string | 시작 시간 (ISO 8601) |
| `finishedAt` | string? | 종료 시간 (실행 중이면 null) |
| `totalTests` | number | 총 테스트 수 |
| `successCount` | number | 성공한 테스트 수 |
| `failedCount` | number | 실패한 테스트 수 |
| `skippedCount` | number | 건너뛴 테스트 수 |
| `totalDurationMillis` | number | 총 실행 시간 (밀리초) |
| `requesterIp` | string | 요청자 IP 주소 |
| `classNames` | string | 실행한 클래스명들 (쉼표 구분) |
| `status` | string | 상태: `RUNNING`, `COMPLETED`, `FAILED` |

---

## 8. 특정 실행 조회

특정 실행의 기본 정보를 조회합니다.

### Request

```http
GET /api/tests/executions/{executionId}
```

**Path Parameters:**
- `executionId`: 실행 ID (UUID)

### Response

```json
{
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "startedAt": "2024-01-15T10:30:00",
  "finishedAt": "2024-01-15T10:30:15",
  "totalTests": 10,
  "successCount": 9,
  "failedCount": 1,
  "skippedCount": 0,
  "totalDurationMillis": 15234,
  "requesterIp": "192.168.1.100",
  "classNames": "testauto.testcode.e2e.LoginTest,testauto.testcode.e2e.CheckoutTest",
  "status": "COMPLETED"
}
```

### 에러

```json
{
  "status": 404,
  "message": "Execution not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 9. 실행 결과 조회

특정 실행의 상세 결과를 트리 구조로 조회합니다.

### Request

```http
GET /api/tests/executions/{executionId}/results
```

**Path Parameters:**
- `executionId`: 실행 ID (UUID)

### Response

```json
{
  "summary": {
    "total": 10,
    "success": 9,
    "failed": 1,
    "skipped": 0,
    "totalDurationMillis": 15234
  },
  "results": [
    {
      "id": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]",
      "displayName": "LoginTest",
      "status": "FAILED",
      "durationMillis": 8234,
      "errorMessage": null,
      "stackTrace": null,
      "stdout": null,
      "children": [
        {
          "id": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[method:testLoginSuccess()]",
          "displayName": "로그인 성공 테스트",
          "status": "SUCCESS",
          "durationMillis": 234,
          "errorMessage": null,
          "stackTrace": null,
          "stdout": "Login attempt for user: testuser\n",
          "children": []
        },
        {
          "id": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.LoginTest]/[method:testLoginFailure()]",
          "displayName": "로그인 실패 테스트",
          "status": "FAILED",
          "durationMillis": 156,
          "errorMessage": "expected: <failure> but was: <success>",
          "stackTrace": "org.opentest4j.AssertionFailedError: expected: <failure> but was: <success>\n\tat org.junit.jupiter.api...",
          "stdout": "Login attempt for user: baduser\n",
          "children": []
        }
      ]
    },
    {
      "id": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.CheckoutTest]",
      "displayName": "CheckoutTest",
      "status": "SUCCESS",
      "durationMillis": 7000,
      "errorMessage": null,
      "stackTrace": null,
      "stdout": null,
      "children": [
        {
          "id": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.CheckoutTest]/[method:testCheckout()]",
          "displayName": "체크아웃 테스트",
          "status": "SUCCESS",
          "durationMillis": 7000,
          "errorMessage": null,
          "stackTrace": null,
          "stdout": "Processing checkout...\nCheckout complete.\n",
          "children": []
        }
      ]
    }
  ]
}
```

### 필드 설명

**Summary:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `total` | number | 총 테스트 수 |
| `success` | number | 성공한 테스트 수 |
| `failed` | number | 실패한 테스트 수 |
| `skipped` | number | 건너뛴 테스트 수 |
| `totalDurationMillis` | number | 총 실행 시간 (밀리초) |

**Results (트리 구조):**

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | string | JUnit Platform UniqueId |
| `displayName` | string | 표시 이름 |
| `status` | string | 상태: `SUCCESS`, `FAILED`, `SKIPPED`, `RUNNING` |
| `durationMillis` | number | 실행 시간 (밀리초) |
| `errorMessage` | string? | 에러 메시지 (실패한 경우) |
| `stackTrace` | string? | 스택트레이스 (실패한 경우) |
| `stdout` | string? | 캡처된 표준 출력 |
| `children` | array | 자식 결과 (메서드의 경우 빈 배열) |

---

## 에러 응답 형식

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "status": 400,
  "message": "에러 메시지",
  "timestamp": "2024-01-15T10:30:00"
}
```

### HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 (파라미터 오류 등) |
| 404 | 리소스를 찾을 수 없음 |
| 500 | 서버 내부 오류 |

---

## 사용 예시 (cURL)

### 테스트 트리 조회

```bash
curl -X GET http://localhost:9898/api/tests/tree
```

### 카탈로그 새로고침

```bash
curl -X POST http://localhost:9898/api/tests/refresh
```

### 테스트 실행

```bash
curl -X POST http://localhost:9898/api/tests/run \
  -H "Content-Type: application/json" \
  -d '{"classNames": ["testauto.testcode.e2e.LoginTest"]}'
```

### 결과 조회

```bash
# 실행 후 반환된 executionId로 조회
curl -X GET http://localhost:9898/api/tests/executions/550e8400-e29b-41d4-a716-446655440000/results
```

### 메서드 소스 코드 조회

```bash
# uniqueId는 URL 인코딩 필요
curl -X GET "http://localhost:9898/api/tests/method/code?uniqueId=%5Bengine%3Ajunit-jupiter%5D%2F%5Bclass%3Atestauto.testcode.e2e.LoginTest%5D%2F%5Bmethod%3AtestLoginSuccess()%5D"
```

---

## 사용 예시 (JavaScript)

```javascript
// 테스트 트리 조회
const tree = await fetch('/api/tests/tree').then(r => r.json());

// 카탈로그 새로고침
await fetch('/api/tests/refresh', { method: 'POST' });

// 테스트 실행
const { executionId } = await fetch('/api/tests/run', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    classNames: ['testauto.testcode.e2e.LoginTest']
  })
}).then(r => r.json());

// 폴링으로 결과 조회
const pollResults = async (executionId) => {
  while (true) {
    const execution = await fetch(`/api/tests/executions/${executionId}`)
      .then(r => r.json());

    if (execution.status !== 'RUNNING') {
      return fetch(`/api/tests/executions/${executionId}/results`)
        .then(r => r.json());
    }

    await new Promise(r => setTimeout(r, 1000)); // 1초 대기
  }
};

const results = await pollResults(executionId);
console.log(results);
```
