document.addEventListener('DOMContentLoaded', () => {
    let selectedClasses = new Set();
    let currentView = 'dashboard';
    let currentTestInfoCache = null;
    let executionListCache = null;
    let selectedExecutionId = null;
    let dashboardCache = null;
    let dashboardPollingInterval = null;
    let selectedClassForSummary = null;

    const runButton = document.getElementById('runButton');
    const refreshButton = document.getElementById('refreshButton');

    // 뷰 패널 요소들
    const viewPanels = {
        'dashboard': document.getElementById('viewDashboard'),
        'test-info': document.getElementById('viewTestInfo'),
        'test-results': document.getElementById('viewTestResults')
    };

    const dashboardContent = document.getElementById('dashboardContent');
    const testInfoContent = document.getElementById('testInfoContent');
    const testResultsContent = document.getElementById('testResultsContent');

    // 초기 대시보드 로드
    loadDashboard();

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

        // Results 탭에서는 사이드바 숨김
        const sidebar = document.getElementById('sidebar');
        const resizer = document.getElementById('sidebar-resizer');
        if (viewName === 'test-results') {
            sidebar.classList.add('hidden');
            resizer.classList.add('hidden');
        } else {
            sidebar.classList.remove('hidden');
            resizer.classList.remove('hidden');
        }

        // 현재 선택된 클래스 확인
        const selectedNode = document.querySelector('.class-node.selected');
        const selectedClassName = selectedNode ? selectedNode.dataset.class : null;

        // test-results 뷰로 전환시
        if (viewName === 'test-results') {
            if (!executionListCache) {
                loadTestResults();
            }
        }

        // dashboard 뷰로 전환시
        if (viewName === 'dashboard') {
            if (!dashboardCache) {
                loadDashboard();
            }
            // 선택된 클래스가 있으면 테스트 요약 표시
            if (selectedClassName) {
                setTimeout(() => showTestSummary(selectedClassName), 100);
            }
        }

        // test-info 뷰로 전환시 (skipViewSwitch 플래그로 무한루프 방지)
        if (viewName === 'test-info' && selectedClassName) {
            showClassDetailOnly(selectedClassName);
        }
    }

    /* ===== 대시보드 로드 ===== */
    async function loadDashboard() {
        try {
            const response = await fetch('/api/tests/dashboard');
            const data = await response.json();
            dashboardCache = data;
            renderDashboard(data);
        } catch (error) {
            console.error('Failed to load dashboard:', error);
            dashboardContent.innerHTML = `
                <div class="dashboard-loading">
                    <div class="empty-state-icon">&#x26A0;</div>
                    <p>Failed to load dashboard</p>
                </div>
            `;
        }
    }

    function renderDashboard(data) {
        const { todayStats, recentExecutions, totalTestClasses } = data;
        const successRate = todayStats.successRate.toFixed(1);

        // 최근 실행결과 그리드
        const recentExecutionsHtml = recentExecutions && recentExecutions.length > 0
            ? recentExecutions.map(exec => {
                const isRunning = exec.status === 'RUNNING';
                const statusClass = isRunning ? 'running' :
                                   exec.failedCount > 0 ? 'failed' : 'success';
                const statusText = isRunning ? '수행중' :
                                  exec.failedCount > 0 ? '실패' : '성공';
                const dateStr = exec.startedAt ? formatDateTime(exec.startedAt) : '-';
                const requesterName = exec.requesterName || exec.requesterIp || '-';

                // 클래스명 포맷팅
                const classNames = exec.classNames ? exec.classNames.split(',').filter(c => c.trim()) : [];
                let classText = '-';
                if (classNames.length === 1) {
                    classText = classNames[0].split('.').pop();
                } else if (classNames.length > 1) {
                    classText = `${classNames[0].split('.').pop()} 외 ${classNames.length - 1}개`;
                }

                const durationStr = formatDuration(exec.totalDurationMillis);

                return `
                    <div class="recent-exec-item ${statusClass}" onclick="selectExecution('${exec.executionId}'); switchView('test-results');">
                        <div class="recent-exec-status ${statusClass}">${statusText}</div>
                        <div class="recent-exec-class" title="${escapeHtml(exec.classNames || '')}">${escapeHtml(classText)}</div>
                        <div class="recent-exec-totals">${exec.totalTests}</div>
                        <div class="recent-exec-successes">${exec.successCount}</div>
                        <div class="recent-exec-failures">${exec.failedCount}</div>
                        <div class="recent-exec-skipped">${exec.skippedCount}</div>
                        <div class="recent-exec-duration">${durationStr}</div>
                        <div class="recent-exec-requester">${escapeHtml(requesterName)}</div>
                        <div class="recent-exec-time">${dateStr}</div>
                    </div>
                `;
            }).join('')
            : '<div class="recent-exec-empty">실행 이력이 없습니다</div>';

        dashboardContent.innerHTML = `
            <div class="dashboard-header">
                <div class="dashboard-header-left">
                    <h1>Dashboard</h1>
                    <p>Test execution overview and statistics</p>
                </div>
                <button class="dashboard-refresh-btn" onclick="dashboardCache = null; loadDashboard();" title="Refresh Dashboard">
                    &#x1F504;
                </button>
            </div>

            <div class="dashboard-grid">
                <div class="stat-card">
                    <div class="stat-card-header">
                        <div class="stat-card-icon blue">&#x1F4CA;</div>
                        <span class="stat-card-label">Total Classes</span>
                    </div>
                    <div class="stat-card-value">${totalTestClasses}</div>
                    <div class="stat-card-subtitle">Test classes registered</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-header">
                        <div class="stat-card-icon green">&#x2714;</div>
                        <span class="stat-card-label">Today Passed</span>
                    </div>
                    <div class="stat-card-value green">${todayStats.successCount}</div>
                    <div class="stat-card-subtitle">${successRate}% success rate</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-header">
                        <div class="stat-card-icon red">&#x2717;</div>
                        <span class="stat-card-label">Today Failed</span>
                    </div>
                    <div class="stat-card-value red">${todayStats.failedCount}</div>
                    <div class="stat-card-subtitle">${todayStats.totalExecutions} executions today</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-header">
                        <div class="stat-card-icon yellow">&#x23F1;</div>
                        <span class="stat-card-label">Today Tests</span>
                    </div>
                    <div class="stat-card-value">${todayStats.totalTests}</div>
                    <div class="stat-card-subtitle">${todayStats.skippedCount} skipped</div>
                </div>
            </div>

            <div class="dashboard-row">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title">&#x1F4D6; 테스트 정보 요약</span>
                        <button class="dashboard-detail-btn" id="goToTestInfoBtn" style="display: none;">자세히보기 →</button>
                    </div>
                    <div class="dashboard-card-body" id="testSummaryContent">
                        <div class="test-summary-empty">
                            <div class="test-summary-empty-icon">&#128196;</div>
                            <p>좌측에서 테스트 클래스를 선택하세요</p>
                        </div>
                    </div>
                </div>
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title">&#x1F4CB; 최근 실행 결과</span>
                        <button class="dashboard-detail-btn" id="goToResultsBtn">이력 더보기 →</button>
                    </div>
                    <div class="dashboard-card-body dashboard-card-body-scroll">
                        <div class="recent-exec-grid">
                            <div class="recent-exec-header">
                                <div class="recent-exec-status">상태</div>
                                <div class="recent-exec-class">테스트 클래스</div>
                                <div class="recent-exec-totals">Totals</div>
                                <div class="recent-exec-successes">Success</div>
                                <div class="recent-exec-failures">Fail</div>
                                <div class="recent-exec-skipped">Skip</div>
                                <div class="recent-exec-duration">Duration</div>
                                <div class="recent-exec-requester">실행자</div>
                                <div class="recent-exec-time">실행 시간</div>
                            </div>
                            ${recentExecutionsHtml}
                        </div>
                    </div>
                </div>
            </div>

        `;

        // 버튼 이벤트
        setTimeout(() => {
            const goToTestInfoBtn = document.getElementById('goToTestInfoBtn');
            if (goToTestInfoBtn) {
                goToTestInfoBtn.addEventListener('click', () => {
                    if (selectedClassForSummary) {
                        showClassDetail(selectedClassForSummary);
                    }
                });
            }

            const goToResultsBtn = document.getElementById('goToResultsBtn');
            if (goToResultsBtn) {
                goToResultsBtn.addEventListener('click', () => {
                    switchView('test-results');
                });
            }
        }, 0);

        // RUNNING 상태가 있으면 자동 업데이트 시작
        startDashboardPollingIfNeeded(recentExecutions);

        // 선택된 클래스가 있으면 테스트 요약 다시 표시
        if (selectedClassForSummary) {
            setTimeout(() => showTestSummary(selectedClassForSummary), 0);
        }

        // 컬럼 리사이징 이벤트 바인딩
        setTimeout(() => bindColumnResizeEvents(), 0);
    }

    // 최근실행결과 컬럼 리사이징
    function bindColumnResizeEvents() {
        const grid = document.querySelector('.recent-exec-grid');
        if (!grid) return;

        const header = grid.querySelector('.recent-exec-header');
        if (!header) return;

        const columns = header.querySelectorAll(':scope > div');
        const columnVars = [
            '--col-status',
            '--col-class',
            '--col-totals',
            '--col-successes',
            '--col-failures',
            '--col-skipped',
            '--col-duration',
            '--col-requester',
            '--col-time'
        ];

        columns.forEach((col, index) => {
            if (index >= columns.length - 1) return; // 마지막 컬럼은 리사이즈 안함

            col.addEventListener('mousedown', (e) => {
                const rect = col.getBoundingClientRect();
                // 오른쪽 8px 영역에서만 리사이즈 시작
                if (e.clientX < rect.right - 8) return;

                e.preventDefault();
                col.classList.add('resizing');

                const startX = e.clientX;
                const startWidth = col.offsetWidth;

                const onMouseMove = (moveEvent) => {
                    const dx = moveEvent.clientX - startX;
                    const newWidth = Math.max(40, startWidth + dx);
                    grid.style.setProperty(columnVars[index], newWidth + 'px');
                };

                const onMouseUp = () => {
                    col.classList.remove('resizing');
                    document.removeEventListener('mousemove', onMouseMove);
                    document.removeEventListener('mouseup', onMouseUp);
                    document.body.style.cursor = '';
                    document.body.style.userSelect = '';
                };

                document.body.style.cursor = 'col-resize';
                document.body.style.userSelect = 'none';
                document.addEventListener('mousemove', onMouseMove);
                document.addEventListener('mouseup', onMouseUp);
            });
        });
    }

    // Dashboard 자동 업데이트 (RUNNING 상태 감지)
    function startDashboardPollingIfNeeded(recentExecutions) {
        // 기존 폴링 중지
        if (dashboardPollingInterval) {
            clearInterval(dashboardPollingInterval);
            dashboardPollingInterval = null;
        }

        // RUNNING 상태가 있는지 확인
        const hasRunning = recentExecutions && recentExecutions.some(exec => exec.status === 'RUNNING');

        if (hasRunning && currentView === 'dashboard') {
            // 3초마다 업데이트
            dashboardPollingInterval = setInterval(async () => {
                if (currentView !== 'dashboard') {
                    clearInterval(dashboardPollingInterval);
                    dashboardPollingInterval = null;
                    return;
                }

                try {
                    const response = await fetch('/api/tests/dashboard');
                    const data = await response.json();
                    dashboardCache = data;
                    renderDashboard(data);
                } catch (error) {
                    console.error('Failed to poll dashboard:', error);
                }
            }, 3000);
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

            const isAlreadySelected = this.classList.contains('selected');
            const className = this.dataset.class;

            // 모든 선택 해제
            document.querySelectorAll('.class-node').forEach(n => n.classList.remove('selected'));

            if (isAlreadySelected) {
                // 이미 선택된 걸 다시 클릭 → 선택 해제
                if (currentView === 'dashboard') {
                    clearTestSummary();
                } else {
                    clearTestInfo();
                }
            } else {
                // 새로 선택
                this.classList.add('selected');
                if (className) {
                    if (currentView === 'dashboard') {
                        showTestSummary(className);
                    } else {
                        showClassDetail(className);
                    }
                }
            }
        });
    });

    /* ===== 클래스명으로 Results 필터링 ===== */
    async function filterByClassName(className) {
        const simpleClassName = className.split('.').pop();
        filterState.className = simpleClassName;
        filterState.date = '';
        filterState.ip = '';
        filterState.status = '';

        if (!executionListCache) {
            await loadTestResults();
        } else {
            renderExecutionListView(executionListCache);
        }
    }

    /* ===== 클래스명 필터 초기화 ===== */
    async function clearClassNameFilter() {
        filterState.className = '';
        if (executionListCache) {
            renderExecutionListView(executionListCache);
        }
    }

    /* ===== Test Info 초기화 ===== */
    function clearTestInfo() {
        currentTestInfoCache = null;
        testInfoContent.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">&#128196;</div>
                <p>Select a test class from the left panel</p>
            </div>
        `;
    }

    /* ===== 대시보드 테스트 요약 표시 ===== */
    async function showTestSummary(className) {
        selectedClassForSummary = className;
        const summaryContent = document.getElementById('testSummaryContent');
        const detailBtn = document.getElementById('goToTestInfoBtn');

        if (!summaryContent) return;

        summaryContent.innerHTML = `
            <div class="test-summary-loading">
                <div class="loading-spinner"></div>
            </div>
        `;

        try {
            const response = await fetch(`/api/tests/class/${encodeURIComponent(className)}`);
            const data = await response.json();

            const methodTreeHtml = renderMethodTreeSimple(data.methods);

            summaryContent.innerHTML = `
                <div class="test-summary-info">
                    <h3 class="test-summary-class-name">${escapeHtml(data.className)}</h3>
                    <p class="test-summary-full-name">${escapeHtml(data.fullClassName)}</p>
                    <div class="test-summary-tree">
                        ${methodTreeHtml}
                    </div>
                </div>
            `;

            if (detailBtn) {
                detailBtn.style.display = 'inline-block';
            }
        } catch (error) {
            console.error('Failed to load test summary:', error);
            summaryContent.innerHTML = `
                <div class="test-summary-empty">
                    <div class="test-summary-empty-icon">&#x26A0;</div>
                    <p>Failed to load test summary</p>
                </div>
            `;
        }
    }

    /* ===== 대시보드 테스트 요약 초기화 ===== */
    function clearTestSummary() {
        selectedClassForSummary = null;
        const summaryContent = document.getElementById('testSummaryContent');
        const detailBtn = document.getElementById('goToTestInfoBtn');

        if (summaryContent) {
            summaryContent.innerHTML = `
                <div class="test-summary-empty">
                    <div class="test-summary-empty-icon">&#128196;</div>
                    <p>좌측에서 테스트 클래스를 선택하세요</p>
                </div>
            `;
        }
        if (detailBtn) {
            detailBtn.style.display = 'none';
        }
    }

    /* ===== 메서드 트리 간단 렌더링 (코드 없이) ===== */
    function renderMethodTreeSimple(methods) {
        if (!methods || methods.length === 0) {
            return '<p class="test-summary-no-methods">No test methods</p>';
        }

        return `<ul class="test-summary-method-list">${methods.map(method => {
            if (method.nestedClass) {
                const childrenHtml = method.children && method.children.length > 0
                    ? renderMethodTreeSimple(method.children)
                    : '';
                return `
                    <li class="test-summary-nested">
                        <span class="test-summary-nested-badge">Nested</span>
                        <span class="test-summary-nested-name">${escapeHtml(method.displayName)}</span>
                        ${childrenHtml}
                    </li>
                `;
            } else {
                return `
                    <li class="test-summary-method">
                        <span class="test-summary-method-icon">✓</span>
                        <span class="test-summary-method-name">${escapeHtml(method.displayName)}</span>
                    </li>
                `;
            }
        }).join('')}</ul>`;
    }

    /* ===== Run 버튼 클릭 ===== */
    runButton.addEventListener('click', async function() {
        if (selectedClasses.size === 0) return;

        const classesToRun = Array.from(selectedClasses);

        // 확인 다이얼로그
        if (!confirm('실행하시겠습니까?')) {
            return; // No 선택 시 아무것도 안함
        }

        // 선택 즉시 초기화
        clearSelection();

        // 요청만 보내고 끝 (화면 전환 없음)
        try {
            await fetch('/api/tests/run', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ classNames: classesToRun })
            });
        } catch (error) {
            console.error('Failed to run tests:', error);
        }
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
        await showClassDetailOnly(className);
        switchView('test-info');
    }

    /* ===== 클래스 상세보기 (뷰 전환 없이) ===== */
    async function showClassDetailOnly(className) {
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
        } catch (error) {
            console.error('Failed to load class detail:', error);
            testInfoContent.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">X</div>
                    <p>Failed to load class details</p>
                </div>
            `;
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
                <div class="execution-list-panel" id="executionListPanel">
                    <div class="execution-list-header">
                        <span>History (${filteredExecutions.length})</span>
                        <button class="execution-refresh-btn" id="executionRefreshBtn" title="새로고침">↻</button>
                    </div>
                    <div class="execution-list" id="executionList">
                        ${listItemsHtml}
                    </div>
                </div>
                <div class="execution-resizer" id="executionResizer"></div>
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

        // History-Details 리사이저
        const executionResizer = document.getElementById('executionResizer');
        const executionListPanel = document.getElementById('executionListPanel');

        if (executionResizer && executionListPanel) {
            let isResizing = false;
            let startX = 0;
            let startWidth = 0;

            executionResizer.addEventListener('mousedown', (e) => {
                isResizing = true;
                startX = e.clientX;
                startWidth = executionListPanel.offsetWidth;
                document.body.style.cursor = 'col-resize';
                document.body.style.userSelect = 'none';
                e.preventDefault();
            });

            document.addEventListener('mousemove', (e) => {
                if (!isResizing) return;

                const dx = e.clientX - startX;
                const minWidth = 250;
                const maxWidth = 500;
                const newWidth = Math.min(maxWidth, Math.max(minWidth, startWidth + dx));

                executionListPanel.style.width = newWidth + 'px';
            });

            document.addEventListener('mouseup', () => {
                if (!isResizing) return;
                isResizing = false;
                document.body.style.cursor = 'default';
                document.body.style.userSelect = '';
            });
        }
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

        // 직원명 (없으면 IP 표시)
        const requesterName = exec.requesterName || exec.requesterIp || '-';

        // 클래스명 포맷팅 (예: "ClassA 외 4개")
        const classNames = exec.classNames ? exec.classNames.split(',').filter(c => c.trim()) : [];
        let classText = '-';
        if (classNames.length === 1) {
            classText = classNames[0].split('.').pop(); // 패키지 제거하고 클래스명만
        } else if (classNames.length > 1) {
            const firstName = classNames[0].split('.').pop();
            classText = `${firstName} 외 ${classNames.length - 1}개`;
        }

        const statusText = isRunning ? '수행중...' :
                          exec.failedCount > 0 ? `실패 ${exec.failedCount}` : '성공';

        return `
            <div class="${itemClass}" data-execution-id="${exec.executionId}">
                <div class="execution-item-status ${statusClass}"></div>
                <div class="execution-item-info">
                    <div class="execution-item-class" title="${escapeHtml(exec.classNames || '')}">${escapeHtml(classText)}</div>
                    <div class="execution-item-requester">${escapeHtml(requesterName)}</div>
                </div>
                <div class="execution-item-meta">
                    <div class="execution-item-status-text">${statusText}</div>
                    <div class="execution-item-time">${dateStr}</div>
                </div>
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

    function formatDuration(millis) {
        if (!millis || millis <= 0) return '-';
        if (millis < 1000) return `${millis}ms`;
        const seconds = Math.floor(millis / 1000);
        const ms = millis % 1000;
        if (ms === 0) return `${seconds}s`;
        return `${seconds}s ${ms}ms`;
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

        const detailPanel = document.getElementById('executionDetailPanel');
        const detailHeader = detailPanel?.querySelector('.execution-detail-header');
        const detailBody = detailPanel?.querySelector('.execution-detail-body');
        if (!detailBody) return;

        // 헤더에 재실행 버튼 추가
        if (detailHeader) {
            detailHeader.innerHTML = `
                <span>Details</span>
                <button class="rerun-btn" onclick="rerunExecution('${executionId}')" title="Rerun this execution">
                    &#x1F504; Rerun
                </button>
            `;
        }

        detailBody.innerHTML = `
            <div class="execution-detail-empty">
                <div class="execution-detail-empty-icon">&#x23F3;</div>
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
            const classResultsHtml = renderClassResults(testClasses);

            detailBody.innerHTML = `
                <div class="execution-detail-content">
                    <div class="test-summary">
                        <div class="summary-item">
                            <span class="summary-value">${summary.total}</span>
                            <span class="summary-label">Total</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: var(--accent-green);">${summary.success}</span>
                            <span class="summary-label">Success</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: var(--accent-red);">${summary.failed}</span>
                            <span class="summary-label">Failed</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value" style="color: var(--accent-yellow);">${summary.skipped}</span>
                            <span class="summary-label">Skipped</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-value">${summary.totalDurationMillis}ms</span>
                            <span class="summary-label">Duration</span>
                        </div>
                    </div>
                    <div class="class-results-section">
                        <div class="class-results-title">클래스별 상세 결과</div>
                        <div class="class-results">
                            ${classResultsHtml}
                        </div>
                    </div>
                </div>
            `;

            // 클래스 접기/펼치기 이벤트 바인딩
            bindClassToggleEvents();
        } catch (error) {
            console.error('Failed to load execution results:', error);
            detailBody.innerHTML = `
                <div class="execution-detail-empty">
                    <div class="execution-detail-empty-icon">&#x2717;</div>
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
    window.loadDashboard = loadDashboard;
    window.dashboardCache = null;

    // 재실행 함수
    window.rerunExecution = async function(executionId) {
        const execution = executionListCache?.find(e => e.executionId === executionId);
        if (!execution || !execution.classNames) {
            alert('Cannot rerun: execution data not found');
            return;
        }

        const classNames = execution.classNames.split(',').filter(c => c.trim());
        if (classNames.length === 0) {
            alert('Cannot rerun: no classes found');
            return;
        }

        try {
            const response = await fetch('/api/tests/run', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ classNames })
            });

            const result = await response.json();

            if (result.status === 'RUNNING' || result.status === 'COMPLETED') {
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

                if (executionListCache) {
                    executionListCache = [tempExecution, ...executionListCache];
                } else {
                    executionListCache = [tempExecution];
                }

                renderExecutionListView(executionListCache);
                selectExecution(result.executionId);
            } else {
                alert('Failed to rerun: ' + result.message);
            }
        } catch (error) {
            console.error('Failed to rerun:', error);
            alert('Failed to rerun tests');
        }
    };

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
