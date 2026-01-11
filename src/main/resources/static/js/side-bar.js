document.addEventListener('DOMContentLoaded', () => {
    let selectedClasses = new Set();
    let currentView = 'home';
    let currentTestInfoCache = null;
    let executionListCache = null;
    let selectedExecutionId = null;

    const runButton = document.getElementById('runButton');
    const refreshButton = document.getElementById('refreshButton');

    // 뷰 패널 요소들
    const viewPanels = {
        'home': document.getElementById('viewHome'),
        'test-info': document.getElementById('viewTestInfo'),
        'test-results': document.getElementById('viewTestResults')
    };

    const testInfoContent = document.getElementById('testInfoContent');
    const testResultsContent = document.getElementById('testResultsContent');

    /* ===== 헤더 네비게이션 ===== */
    const headerNavButtons = document.querySelectorAll('.header-nav-button');

    function switchView(viewName) {
        currentView = viewName;

        // 버튼 활성화 상태 변경
        headerNavButtons.forEach(btn => {
            if (btn.dataset.view === viewName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // 뷰 패널 전환
        Object.entries(viewPanels).forEach(([key, panel]) => {
            if (panel) {
                if (key === viewName) {
                    panel.classList.add('active');
                } else {
                    panel.classList.remove('active');
                }
            }
        });

        // test-results 뷰로 전환시 결과 로드
        if (viewName === 'test-results' && !executionListCache) {
            loadTestResults();
        }
    }

    // 네비게이션 버튼 클릭 이벤트
    headerNavButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            switchView(btn.dataset.view);
        });
    });

    /* ===== 접기/펼치기 (하위 노드) ===== */
    document.querySelectorAll('.tree-node').forEach(nodeEl => {
        const hasChildren = nodeEl.dataset.hasChildren === 'true';
        if (!hasChildren) {
            return;
        }

        nodeEl.addEventListener('click', (e) => {
            if (e.target.classList.contains('class-checkbox')) {
                return;
            }

            const li = nodeEl.parentElement;
            const children = li.querySelector(':scope > .tree-children');
            if (!children) return;

            const isHidden = children.style.display === 'none';
            children.style.display = isHidden ? 'block' : 'none';

            const toggle = nodeEl.querySelector('.tree-node-toggle');
            if (toggle) {
                toggle.textContent = isHidden ? '▼' : '▶';
            }
        });
    });

    /* ===== 체크박스 이벤트 ===== */
    document.querySelectorAll('.class-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function(e) {
            e.stopPropagation();
            const className = this.dataset.class || this.closest('.class-node').dataset.class;
            if (this.checked) {
                selectedClasses.add(className);
            } else {
                selectedClasses.delete(className);
            }
            updateRunButton();
        });

        checkbox.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    });

    function updateRunButton() {
        runButton.disabled = selectedClasses.size === 0;
        runButton.textContent = selectedClasses.size > 0
            ? `Run Selected Tests (${selectedClasses.size})`
            : 'Run Selected Tests';
    }

    /* ===== 클래스 클릭 이벤트 ===== */
    document.querySelectorAll('.class-node').forEach(node => {
        node.addEventListener('click', function(e) {
            if (e.target.type === 'checkbox') return;

            document.querySelectorAll('.class-node').forEach(n => n.classList.remove('selected'));
            this.classList.add('selected');

            const className = this.dataset.class;
            if (className) {
                showClassDetail(className);
            }
        });
    });

    /* ===== Run 버튼 클릭 ===== */
    runButton.addEventListener('click', function() {
        if (selectedClasses.size === 0) return;

        const classesToRun = Array.from(selectedClasses);

        // 실행 요청 시작 시 선택 초기화
        clearSelection();

        runButton.disabled = true;
        runButton.textContent = 'Running...';

        runTests(classesToRun);
    });

    /* ===== 선택 초기화 ===== */
    function clearSelection() {
        selectedClasses.clear();
        document.querySelectorAll('.class-checkbox').forEach(cb => {
            cb.checked = false;
        });
        updateRunButton();
    }

    /* ===== 클래스 상세보기 ===== */
    async function showClassDetail(className) {
        try {
            const response = await fetch(`/api/tests/class/${encodeURIComponent(className)}`);
            const data = await response.json();

            const methodsHtml = renderMethodItems(data.methods);

            currentTestInfoCache = `
                <div class="test-info-panel">
                    <div class="test-info-header">Information</div>
                    <div class="test-info-body">
                        <div class="class-detail">
                            <h1 class="class-detail-title">${data.className}</h1>
                            <h2>${data.fullClassName}</h2>
                            <ul class="method-list">
                                ${methodsHtml}
                            </ul>
                        </div>
                    </div>
                </div>
            `;

            testInfoContent.innerHTML = currentTestInfoCache;

            // Test Information 뷰로 전환
            switchView('test-info');
        } catch (error) {
            console.error('Failed to load class detail:', error);
            testInfoContent.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">X</div>
                    <p>Failed to load class details</p>
                </div>
            `;
            switchView('test-info');
        }
    }

    function renderMethodItems(methods) {
        return methods.map(method => {
            if (method.nestedClass) {
                const childrenHtml = method.children && method.children.length > 0
                    ? renderMethodItems(method.children)
                    : '';
                return `
                    <li class="method-item nested-class-item">
                        <div class="nested-class-header" onclick="toggleNestedClass(this)">
                            <span class="nested-class-toggle">▶</span>
                            <span class="nested-class-badge">Nested</span>
                            <span class="nested-class-name">${escapeHtml(method.displayName)}</span>
                        </div>
                        <ul class="nested-class-methods" style="display: none;">
                            ${childrenHtml}
                        </ul>
                    </li>
                `;
            } else {
                return `
                    <li class="method-item">
                        <div class="method-header" onclick="toggleMethodCode('${escapeHtml(method.uniqueId)}', this)">
                            <span class="method-name">✓ ${escapeHtml(method.displayName)}</span>
                            <span class="method-toggle">▶</span>
                        </div>
                        <div class="method-code-container" style="display: none;">
                            <pre><code class="java"></code></pre>
                        </div>
                    </li>
                `;
            }
        }).join('');
    }

    // Nested 클래스 토글 함수
    window.toggleNestedClass = function(headerElement) {
        const methodsContainer = headerElement.nextElementSibling;
        const toggle = headerElement.querySelector('.nested-class-toggle');

        if (methodsContainer.style.display === 'none') {
            methodsContainer.style.display = 'block';
            toggle.textContent = '▼';
        } else {
            methodsContainer.style.display = 'none';
            toggle.textContent = '▶';
        }
    };

    window.showClassDetail = showClassDetail;

    /* ===== 메서드 코드 토글 ===== */
    window.toggleMethodCode = async function(uniqueId, headerElement) {
        const methodItem = headerElement.parentElement;
        const codeContainer = methodItem.querySelector('.method-code-container');
        const codeElement = codeContainer.querySelector('code');
        const toggle = headerElement.querySelector('.method-toggle');

        if (codeContainer.style.display !== 'none') {
            codeContainer.style.display = 'none';
            toggle.textContent = '▶';
            return;
        }

        if (!codeElement.textContent) {
            try {
                const response = await fetch(`/api/tests/method/code?uniqueId=${encodeURIComponent(uniqueId)}`);
                const data = await response.json();
                codeElement.textContent = data.code;
            } catch (error) {
                console.error('Failed to load method code:', error);
                codeElement.textContent = '// Failed to load method code';
            }
        }

        codeContainer.style.display = 'block';
        toggle.textContent = '▼';
    };

    /* ===== 테스트 실행 ===== */
    async function runTests(classNames) {
        try {
            const response = await fetch('/api/tests/run', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ classNames })
            });

            const result = await response.json();

            if (result.status === 'RUNNING' || result.status === 'COMPLETED') {
                // 임시 RUNNING 항목을 캐시에 추가하여 즉시 표시
                const tempExecution = {
                    executionId: result.executionId,
                    startedAt: new Date().toISOString(),
                    status: 'RUNNING',
                    classNames: classNames.join(','),
                    totalTests: 0,
                    successCount: 0,
                    failedCount: 0,
                    skippedCount: 0,
                    totalDurationMillis: 0
                };

                // 기존 캐시가 있으면 앞에 추가, 없으면 새 배열 생성
                if (executionListCache) {
                    executionListCache = [tempExecution, ...executionListCache];
                } else {
                    executionListCache = [tempExecution];
                }

                // 즉시 화면 렌더링 및 전환
                renderExecutionListView(executionListCache);
                switchView('test-results');

                // 새로 추가된 실행 선택
                selectExecution(result.executionId);
            } else {
                alert('Test execution failed: ' + result.message);
            }
        } catch (error) {
            console.error('Failed to run tests:', error);
            alert('Failed to run tests');
        } finally {
            runButton.disabled = false;
            updateRunButton();
        }
    }

    /* ===== 테스트 실행 이력 로드 (게시판 형태) ===== */
    async function loadTestResults() {
        try {
            const response = await fetch('/api/tests/executions?limit=50');
            const executions = await response.json();
            executionListCache = executions;

            renderExecutionListView(executions);
        } catch (error) {
            console.error('Failed to load execution list:', error);
            testResultsContent.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">X</div>
                    <p>Failed to load test results</p>
                </div>
            `;
        }
    }

    // 필터 상태
    let filterState = {
        date: '',
        ip: '',
        className: '',
        status: ''
    };

    // 서버 시간 기준 오늘 날짜 (API에서 받아옴)
    let serverToday = null;

    async function fetchServerToday() {
        if (serverToday) return serverToday;
        try {
            const response = await fetch('/api/tests/server-time');
            const data = await response.json();
            serverToday = data.today;
            return serverToday;
        } catch (error) {
            console.error('Failed to fetch server time:', error);
            return new Date().toISOString().split('T')[0];
        }
    }

    function renderExecutionListView(executions) {
        const filteredExecutions = applyFilters(executions);

        const listItemsHtml = filteredExecutions.length > 0
            ? filteredExecutions.map(exec => renderExecutionItem(exec)).join('')
            : `<div class="execution-list-empty">
                   <div class="execution-list-empty-icon">📋</div>
                   <p>No test executions found</p>
               </div>`;

        testResultsContent.innerHTML = `
            <div class="result-filters">
                <div class="filter-group">
                    <label class="filter-label">실행 날짜</label>
                    <div class="filter-date-wrapper">
                        <input type="date" class="filter-date" id="filterDate" value="${filterState.date}" lang="ko">
                        <div class="today-checkbox-wrapper">
                            <input type="checkbox" id="filterToday" ${filterState.date && serverToday && filterState.date === serverToday ? 'checked' : ''}>
                            <label for="filterToday">오늘</label>
                        </div>
                    </div>
                </div>
                <div class="filter-group">
                    <label class="filter-label">요청 IP</label>
                    <input type="text" class="filter-input" id="filterIp" placeholder="IP 주소" value="${filterState.ip}">
                </div>
                <div class="filter-group">
                    <label class="filter-label">클래스명</label>
                    <input type="text" class="filter-input" id="filterClassName" placeholder="클래스명 검색" value="${filterState.className}">
                </div>
                <div class="filter-group">
                    <label class="filter-label">상태</label>
                    <select class="filter-select" id="filterStatus">
                        <option value="">전체</option>
                        <option value="RUNNING" ${filterState.status === 'RUNNING' ? 'selected' : ''}>수행중</option>
                        <option value="SUCCESS" ${filterState.status === 'SUCCESS' ? 'selected' : ''}>정상</option>
                        <option value="FAILED" ${filterState.status === 'FAILED' ? 'selected' : ''}>실패</option>
                    </select>
                </div>
                <button class="filter-search-btn" id="filterSearchBtn">검색</button>
                <button class="filter-clear-btn" id="filterClearBtn">초기화</button>
            </div>
            <div class="execution-list-container">
                <div class="execution-list-panel">
                    <div class="execution-list-header">
                        <span>History (${filteredExecutions.length})</span>
                        <button class="execution-refresh-btn" id="executionRefreshBtn" title="새로고침">↻</button>
                    </div>
                    <div class="execution-list" id="executionList">
                        ${listItemsHtml}
                    </div>
                </div>
                <div class="execution-detail-panel" id="executionDetailPanel">
                    <div class="execution-detail-header">Details</div>
                    <div class="execution-detail-body">
                        <div class="execution-detail-empty">
                            <div class="execution-detail-empty-icon">👈</div>
                            <p>Select an execution to view details</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // 필터 이벤트 바인딩
        bindFilterEvents();

        // 실행 항목 클릭 이벤트
        document.querySelectorAll('.execution-item').forEach(item => {
            item.addEventListener('click', () => {
                const executionId = item.dataset.executionId;
                selectExecution(executionId);
            });
        });

        // 가장 최근 실행 자동 선택
        if (filteredExecutions.length > 0) {
            selectExecution(filteredExecutions[0].executionId);
        }
    }

    function bindFilterEvents() {
        const filterDate = document.getElementById('filterDate');
        const filterToday = document.getElementById('filterToday');
        const filterIp = document.getElementById('filterIp');
        const filterClassName = document.getElementById('filterClassName');
        const filterStatus = document.getElementById('filterStatus');
        const filterSearchBtn = document.getElementById('filterSearchBtn');
        const filterClearBtn = document.getElementById('filterClearBtn');
        const executionRefreshBtn = document.getElementById('executionRefreshBtn');

        // 날짜 필터 값 변경 시 (검색은 안함, 값만 업데이트)
        filterDate.addEventListener('change', () => {
            filterToday.checked = serverToday && filterDate.value === serverToday;
        });

        // 오늘 체크박스
        filterToday.addEventListener('change', async () => {
            if (filterToday.checked) {
                const today = await fetchServerToday();
                filterDate.value = today;
            } else {
                filterDate.value = '';
            }
        });

        // 검색 버튼 - 필터 적용
        filterSearchBtn.addEventListener('click', () => {
            filterState.date = filterDate.value;
            filterState.ip = filterIp.value;
            filterState.className = filterClassName.value;
            filterState.status = filterStatus.value;
            renderExecutionListView(executionListCache);
        });

        // Enter 키로 검색
        [filterDate, filterIp, filterClassName].forEach(input => {
            input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    filterSearchBtn.click();
                }
            });
        });

        // 초기화 버튼
        filterClearBtn.addEventListener('click', () => {
            filterState = { date: '', ip: '', className: '', status: '' };
            renderExecutionListView(executionListCache);
        });

        // 새로고침 버튼
        executionRefreshBtn.addEventListener('click', async () => {
            executionRefreshBtn.classList.add('loading');
            executionListCache = null;
            await loadTestResults();
            executionRefreshBtn.classList.remove('loading');
        });
    }

    function applyFilters(executions) {
        return executions.filter(exec => {
            // 날짜 필터
            if (filterState.date) {
                const execDate = exec.startedAt ? exec.startedAt.split('T')[0] : '';
                if (execDate !== filterState.date) return false;
            }

            // IP 필터
            if (filterState.ip) {
                const ip = exec.requesterIp || '';
                if (!ip.toLowerCase().includes(filterState.ip.toLowerCase())) return false;
            }

            // 클래스명 필터
            if (filterState.className) {
                const classNames = exec.classNames || '';
                if (!classNames.toLowerCase().includes(filterState.className.toLowerCase())) return false;
            }

            // 상태 필터
            if (filterState.status) {
                if (filterState.status === 'RUNNING') {
                    if (exec.status !== 'RUNNING') return false;
                } else if (filterState.status === 'SUCCESS') {
                    if (exec.status === 'RUNNING' || exec.failedCount > 0) return false;
                } else if (filterState.status === 'FAILED') {
                    if (exec.status === 'RUNNING' || exec.failedCount === 0) return false;
                }
            }

            return true;
        });
    }

    function renderExecutionItem(exec) {
        const isRunning = exec.status === 'RUNNING';
        const statusClass = isRunning ? 'running' :
                           exec.failedCount > 0 ? 'failed' :
                           exec.skippedCount > 0 ? 'partial' : 'success';
        const dateStr = formatDateTime(exec.startedAt);
        const itemClass = isRunning ? 'execution-item running' : 'execution-item';

        const statsText = isRunning
            ? '수행중...'
            : `${exec.successCount} passed, ${exec.failedCount} failed, ${exec.skippedCount} skipped`;
        const durationText = isRunning ? '' : `${exec.totalDurationMillis}ms`;

        return `
            <div class="${itemClass}" data-execution-id="${exec.executionId}">
                <div class="execution-item-status ${statusClass}"></div>
                <div class="execution-item-info">
                    <div class="execution-item-time">${dateStr}</div>
                    <div class="execution-item-stats">${statsText}</div>
                </div>
                <div class="execution-item-duration">${durationText}</div>
            </div>
        `;
    }

    function formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '-';
        const dt = new Date(dateTimeStr);
        const month = String(dt.getMonth() + 1).padStart(2, '0');
        const day = String(dt.getDate()).padStart(2, '0');
        const hours = String(dt.getHours()).padStart(2, '0');
        const minutes = String(dt.getMinutes()).padStart(2, '0');
        const seconds = String(dt.getSeconds()).padStart(2, '0');
        return `${month}-${day} ${hours}:${minutes}:${seconds}`;
    }

    async function selectExecution(executionId) {
        selectedExecutionId = executionId;

        // 선택 상태 업데이트
        document.querySelectorAll('.execution-item').forEach(item => {
            if (item.dataset.executionId === executionId) {
                item.classList.add('selected');
            } else {
                item.classList.remove('selected');
            }
        });

        const detailBody = document.querySelector('#executionDetailPanel .execution-detail-body');
        if (!detailBody) return;

        detailBody.innerHTML = `
            <div class="execution-detail-empty">
                <div class="execution-detail-empty-icon">⏳</div>
                <p>Loading...</p>
            </div>
        `;

        try {
            const response = await fetch(`/api/tests/executions/${executionId}/results`);
            const data = await response.json();

            const summary = data.summary;
            const results = data.results;

            // JUnit Jupiter 필터링하고 테스트 클래스만 추출
            const testClasses = filterAndGroupByClass(results);
            const classSummaries = calculateClassSummaries(testClasses);
            const classResultsHtml = renderClassResults(testClasses);
            const classSummaryHtml = renderClassSummaries(classSummaries);

            detailBody.innerHTML = `
                <div class="execution-detail-content">
                    <div class="test-summary">
                        <div class="summary-item">
                            <span class="summary-value">${summary.total}</span>
                            <span class="summary-label">Total</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: #28a745;">${summary.success}</span>
                            <span class="summary-label">Success</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: #dc3545;">${summary.failed}</span>
                            <span class="summary-label">Failed</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: #ffc107;">${summary.skipped}</span>
                            <span class="summary-label">Skipped</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value">${summary.totalDurationMillis}ms</span>
                            <span class="summary-label">Duration</span>
                        </div>
                    </div>
                    ${classSummaryHtml}
                    <div class="class-results">
                        ${classResultsHtml}
                    </div>
                </div>
            `;

            // 클래스 접기/펼치기 이벤트 바인딩
            bindClassToggleEvents();
        } catch (error) {
            console.error('Failed to load execution results:', error);
            detailBody.innerHTML = `
                <div class="execution-detail-empty">
                    <div class="execution-detail-empty-icon">X</div>
                    <p>Failed to load results</p>
                </div>
            `;
        }
    }

    // JUnit Jupiter 필터링하고 테스트 클래스별로 그룹화
    function filterAndGroupByClass(results) {
        const classes = [];

        for (const result of results) {
            // JUnit Jupiter 엔진 레벨 스킵
            if (result.displayName === 'JUnit Jupiter') {
                // 그 안의 테스트 클래스들을 바로 추출
                if (result.children && result.children.length > 0) {
                    for (const child of result.children) {
                        classes.push(child);
                    }
                }
            } else {
                // JUnit Jupiter가 아닌 경우 그대로 추가
                classes.push(result);
            }
        }

        return classes;
    }

    // 클래스별 요약 정보 계산
    function calculateClassSummaries(testClasses) {
        return testClasses.map(testClass => {
            const stats = { success: 0, failed: 0, skipped: 0, total: 0 };
            countTestResults(testClass, stats);
            return {
                name: testClass.displayName,
                ...stats
            };
        });
    }

    function countTestResults(result, stats) {
        if (result.children && result.children.length > 0) {
            for (const child of result.children) {
                countTestResults(child, stats);
            }
        } else {
            // 리프 노드 (실제 테스트)
            stats.total++;
            if (result.status === 'SUCCESS') stats.success++;
            else if (result.status === 'FAILED') stats.failed++;
            else if (result.status === 'SKIPPED') stats.skipped++;
        }
    }

    // 클래스별 요약 정보 렌더링
    function renderClassSummaries(summaries) {
        if (summaries.length === 0) return '';

        const items = summaries.map(s => {
            const statsHtml = [];
            if (s.success > 0) statsHtml.push(`<span class="class-summary-stat success">${s.success} passed</span>`);
            if (s.failed > 0) statsHtml.push(`<span class="class-summary-stat failed">${s.failed} failed</span>`);
            if (s.skipped > 0) statsHtml.push(`<span class="class-summary-stat skipped">${s.skipped} skipped</span>`);

            return `
                <div class="class-summary-item">
                    <span class="class-summary-name" title="${escapeHtml(s.name)}">${escapeHtml(s.name)}</span>
                    <div class="class-summary-stats">${statsHtml.join('')}</div>
                </div>
            `;
        }).join('');

        return `
            <div class="class-summaries">
                <div class="class-summaries-title">클래스별 요약</div>
                <div class="class-summary-list">${items}</div>
            </div>
        `;
    }

    // 클래스별 결과 렌더링
    function renderClassResults(testClasses) {
        return testClasses.map((testClass, index) => {
            const stats = { success: 0, failed: 0, skipped: 0, total: 0 };
            countTestResults(testClass, stats);

            const hasFailure = stats.failed > 0;
            const resultsHtml = renderTestResultsFlat(testClass.children || []);

            return `
                <div class="class-result-group" data-class-index="${index}">
                    <div class="class-result-header">
                        <div class="class-result-header-left">
                            <span class="class-result-toggle">▶</span>
                            <span class="class-result-name">${escapeHtml(testClass.displayName)}</span>
                        </div>
                        <div class="class-result-stats">
                            <span class="class-stat success">✓ ${stats.success}</span>
                            <span class="class-stat failed">✗ ${stats.failed}</span>
                            <span class="class-stat skipped">⊘ ${stats.skipped}</span>
                        </div>
                    </div>
                    <div class="class-result-body collapsed">
                        <ul class="result-tree">${resultsHtml}</ul>
                    </div>
                </div>
            `;
        }).join('');
    }

    // 테스트 결과를 평면적으로 렌더링 (Nested 포함, 전부 펼쳐진 상태)
    function renderTestResultsFlat(results) {
        return results.map(result => renderTestResult(result)).join('');
    }

    // 클래스 접기/펼치기 이벤트 바인딩
    function bindClassToggleEvents() {
        document.querySelectorAll('.class-result-header').forEach(header => {
            header.addEventListener('click', () => {
                const group = header.closest('.class-result-group');
                const body = group.querySelector('.class-result-body');
                const toggle = header.querySelector('.class-result-toggle');

                if (body.classList.contains('collapsed')) {
                    body.classList.remove('collapsed');
                    toggle.textContent = '▼';
                } else {
                    body.classList.add('collapsed');
                    toggle.textContent = '▶';
                }
            });
        });
    }

    window.loadTestResults = loadTestResults;
    window.selectExecution = selectExecution;
    window.switchView = switchView;

    function renderTestResult(result) {
        const isNestedClass = result.id && /\[nested-class:[^\]]+\]$/.test(result.id);
        const icon = result.status === 'SUCCESS' ? '✓' :
                     result.status === 'FAILED' ? '✗' :
                     result.status === 'SKIPPED' ? '⊘' : '';

        const childrenHtml = result.children && result.children.length > 0
            ? `<ul class="result-children">${result.children.map(child => renderTestResult(child)).join('')}</ul>`
            : '';

        const errorHtml = result.errorMessage
            ? `<div class="result-error">${escapeHtml(result.errorMessage)}</div>`
            : '';

        const stdoutHtml = result.stdout
            ? `<div class="result-stdout"><strong>Output:</strong><pre>${escapeHtml(result.stdout)}</pre></div>`
            : '';

        const nestedBadge = isNestedClass ? '<span class="nested-class-badge">Nested</span>' : '';

        return `
            <li class="result-item ${result.status}">
                <div class="result-header">
                    <span class="result-name">${icon} ${nestedBadge} ${escapeHtml(result.displayName)}</span>
                    <span class="result-duration">${result.durationMillis}ms</span>
                </div>
                ${errorHtml}
                ${stdoutHtml}
                ${childrenHtml}
            </li>
        `;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /* ===== 검색 ===== */
    const searchInput = document.getElementById('test-search');

    function filterNode(liElement, keyword) {
        const labelEl = liElement.querySelector(':scope > .tree-node .tree-node-label');
        const childrenUl = liElement.querySelector(':scope > .tree-children');

        let selfMatch = false;
        if (labelEl) {
            const text = labelEl.textContent.toLowerCase();
            selfMatch = text.includes(keyword);
        }

        let childMatch = false;
        if (childrenUl) {
            const childLis = childrenUl.querySelectorAll(':scope > li');
            childLis.forEach(childLi => {
                const visible = filterNode(childLi, keyword);
                if (visible) childMatch = true;
            });
        }

        const visible = keyword === '' || selfMatch || childMatch;
        liElement.style.display = visible ? '' : 'none';

        if (childrenUl && visible && keyword !== '') {
            childrenUl.style.display = 'block';
        }

        return visible;
    }

    searchInput.addEventListener('input', function () {
        const keyword = this.value.toLowerCase();
        const topLevelLis = document.querySelectorAll('.tree-root > li');
        topLevelLis.forEach(li => filterNode(li, keyword));
    });

    /* ===== 테스트 목록 새로고침 ===== */
    refreshButton.addEventListener('click', async function() {
        refreshButton.classList.add('loading');

        try {
            const response = await fetch('/api/tests/refresh', {
                method: 'POST'
            });
            const data = await response.json();

            if (data.status === 'SUCCESS') {
                // 페이지 새로고침으로 트리 업데이트
                window.location.reload();
            } else {
                alert('Failed to refresh test catalog');
            }
        } catch (error) {
            console.error('Failed to refresh:', error);
            alert('Failed to refresh test catalog');
        } finally {
            refreshButton.classList.remove('loading');
        }
    });

    /* ===== 사이드바 리사이즈 ===== */
    const sidebar = document.getElementById('sidebar');
    const resizer = document.getElementById('sidebar-resizer');

    if (sidebar && resizer) {
        let isResizing = false;
        let startX = 0;
        let startWidth = 0;

        resizer.addEventListener('mousedown', (e) => {
            isResizing = true;
            startX = e.clientX;
            startWidth = sidebar.offsetWidth;
            document.body.style.cursor = 'col-resize';
            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isResizing) return;

            const dx = e.clientX - startX;
            const minWidth = 180;
            const maxWidth = 600;
            const newWidth = Math.min(maxWidth, Math.max(minWidth, startWidth + dx));

            sidebar.style.width = newWidth + 'px';
        });

        document.addEventListener('mouseup', () => {
            if (!isResizing) return;
            isResizing = false;
            document.body.style.cursor = 'default';
        });
    }
});
