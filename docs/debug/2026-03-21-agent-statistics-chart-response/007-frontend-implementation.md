# 007 - 프론트엔드 구현

## 기본 정보
- **작업일**: 2026-03-21
- **모듈**: `admin` (Next.js 16 App)
- **빌드 검증**: `npx next build` 성공 (TypeScript 컴파일 + 정적 페이지 생성 통과)

## 구현 순서

### Step 1: recharts 설치
```bash
cd admin && npm install recharts
```

### Step 2: types/agent.ts 타입 추가
- `ChartData` 인터페이스: `chartType: "pie" | "bar"`, `title`, `meta`, `dataPoints`
- `ChartMeta` 인터페이스: `groupBy`, `startDate: string | null`, `endDate: string | null`, `totalCount`
- `DataPoint` 인터페이스: `label`, `value`
- `AgentExecutionResult`에 `chartData: ChartData[]` 추가
- `DisplayMessage`에 `chartData?: ChartData[]` 추가
- `ExecutionMeta`는 변경 없음 (chartData 미포함)

### Step 3: AgentChart 컴포넌트 생성
- **파일**: `components/agent/agent-chart.tsx` (신규)
- `"use client"` 지시어 (Recharts 브라우저 DOM 의존)
- `chartType === "pie"`: PieChart + Pie + Cell + Legend + Tooltip
- `chartType === "bar"`: BarChart + Bar + XAxis + YAxis + Tooltip + Cell
- 8색 팔레트 (`CHART_COLORS`) 순환 적용
- `formatPeriod()`: null 조합별 4가지 케이스 처리
- 타입 가드: `chartType` 검증 + `dataPoints` 배열/길이 검증
- 접근성: `role="img"` + `aria-label`
- Neo-Brutalism: `brutal-border bg-white p-4`, 직각

### Step 4: ChartSection 컴포넌트 생성
- **파일**: `components/agent/chart-section.tsx` (신규)
- 빈 배열 → `null` 반환
- `key={${chartType}-${title}-${index}}` 조합 key

### Step 5: agent-message-bubble.tsx 수정
- `ChartSection` import 추가
- Props에 `chartData?: ChartData[]` 추가
- ASSISTANT 분기에 `!isUser && chartData && chartData.length > 0` 가드로 ChartSection 렌더링
- 배치 순서: ReactMarkdown → (failed 블록) → **ChartSection** → AgentExecutionMeta → 타임스탬프

### Step 6: 데이터 흐름 연결
- `app/agent/page.tsx`: `assistantMessage.chartData = result.chartData`
- `agent-message-area.tsx`: `chartData={msg.chartData}` prop 전달

## 핵심 구현 패턴

### formatPeriod 4가지 케이스
```typescript
function formatPeriod(meta: ChartMeta): string {
  if (!meta.startDate && !meta.endDate) return "All period";
  if (meta.startDate && !meta.endDate) return `From ${meta.startDate}`;
  if (!meta.startDate && meta.endDate) return `Until ${meta.endDate}`;
  return `${meta.startDate} ~ ${meta.endDate}`;
}
```

### 차트 데이터가 없는 경우 (대화 이력 로드)
서버는 대화 이력에 `chartData`를 저장하지 않음 (summary만 저장). 이력 로드 시 `DisplayMessage.chartData`는 `undefined` → `ChartSection` 미렌더링 → 기존 동작 유지.
