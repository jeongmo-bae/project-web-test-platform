document.addEventListener('DOMContentLoaded', () => {
    let selectedClasses = new Set();
    let currentView = 'home';
    let currentTestInfoCache = null;
    let currentTestResultsCache = null;

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
        if (viewName === 'test-results' && !currentTestResultsCache) {
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

            const methodsHtml = renderMethodItems(data.methods, className);

            currentTestInfoCache = `
                <div class="class-detail">
                    <h1 class="class-detail-title">${data.className}</h1>
                    <h2>${data.fullClassName}</h2>
                    <ul class="method-list">
                        ${methodsHtml}
                    </ul>
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

    function renderMethodItems(methods, className) {
        return methods.map(method => {
            if (method.nestedClass) {
                const childrenHtml = method.children && method.children.length > 0
                    ? renderMethodItems(method.children, className)
                    : '';
                return `
                    <li class="method-item nested-class-item">
                        <div class="nested-class-header" onclick="toggleNestedClass(this)">
                            <span class="nested-class-toggle">▼</span>
                            <span class="nested-class-badge">Nested</span>
                            <span class="nested-class-name">${escapeHtml(method.displayName)}</span>
                        </div>
                        <ul class="nested-class-methods">
                            ${childrenHtml}
                        </ul>
                    </li>
                `;
            } else {
                return `
                    <li class="method-item">
                        <div class="method-header" onclick="toggleMethodCode('${className}', '${escapeHtml(method.methodName)}', this)">
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
    window.toggleMethodCode = async function(className, methodName, headerElement) {
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
                const response = await fetch(`/api/tests/method/${encodeURIComponent(className)}/${encodeURIComponent(methodName)}/code`);
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

            if (result.status === 'COMPLETED') {
                currentTestResultsCache = null;
                await loadTestResults();
                switchView('test-results');
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

    /* ===== 테스트 결과 로드 ===== */
    async function loadTestResults() {
        try {
            const response = await fetch('/api/tests/results');
            const data = await response.json();

            const summary = data.summary;
            const results = data.results;

            const resultsHtml = results.map(result => renderTestResult(result)).join('');

            currentTestResultsCache = `
                <div class="class-detail">
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
                    <ul class="result-tree">
                        ${resultsHtml}
                    </ul>
                </div>
            `;

            testResultsContent.innerHTML = currentTestResultsCache;
        } catch (error) {
            console.error('Failed to load test results:', error);
            currentTestResultsCache = `
                <div class="empty-state">
                    <div class="empty-state-icon">X</div>
                    <p>Failed to load test results</p>
                </div>
            `;
            testResultsContent.innerHTML = currentTestResultsCache;
        }
    }

    window.loadTestResults = loadTestResults;
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
