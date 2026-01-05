document.addEventListener('DOMContentLoaded', () => {
    let selectedClasses = new Set();
    let currentTab = 'test-info';
    let currentTestInfoCache = null; // 테스트 정보 캐시
    let currentTestResultsCache = null; // 테스트 결과 캐시

    const runButton = document.getElementById('runButton');
    const contentArea = document.getElementById('contentArea');
    const tabHeader = document.getElementById('tabHeader');

    /* ===== 탭 전환 ===== */
    function switchTab(tabName) {
        currentTab = tabName;

        // 탭 버튼 활성화 상태 변경
        document.querySelectorAll('.tab-button').forEach(btn => {
            if (btn.dataset.tab === tabName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // 컨텐츠 표시
        if (tabName === 'test-info' && currentTestInfoCache) {
            contentArea.innerHTML = currentTestInfoCache;
        } else if (tabName === 'test-results' && currentTestResultsCache) {
            contentArea.innerHTML = currentTestResultsCache;
        } else if (tabName === 'test-results' && !currentTestResultsCache) {
            showTestResults();
        }
    }

    // 탭 버튼 클릭 이벤트
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.addEventListener('click', () => {
            switchTab(btn.dataset.tab);
        });
    });

    /* ===== 접기/펼치기 (하위 노드) ===== */
    document.querySelectorAll('.tree-node').forEach(nodeEl => {
        const hasChildren = nodeEl.dataset.hasChildren === 'true';
        if (!hasChildren) {
            return;
        }

        nodeEl.addEventListener('click', (e) => {
            // 체크박스 클릭은 무시
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
            console.log('Checkbox changed:', {
                checked: this.checked,
                className: className,
                datasetClass: this.dataset.class,
                selectedClasses: Array.from(selectedClasses)
            });
            if (this.checked) {
                selectedClasses.add(className);
            } else {
                selectedClasses.delete(className);
            }
            updateRunButton();
        });

        // 클릭 이벤트도 처리 (이벤트 전파 방지)
        checkbox.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    });

    function updateRunButton() {
        console.log('updateRunButton called:', {
            selectedClassesSize: selectedClasses.size,
            selectedClassesArray: Array.from(selectedClasses),
            runButtonExists: !!runButton,
            currentDisabled: runButton ? runButton.disabled : 'N/A'
        });
        runButton.disabled = selectedClasses.size === 0;
        runButton.textContent = selectedClasses.size > 0
            ? `Run Selected Tests (${selectedClasses.size})`
            : 'Run Selected Tests';
        console.log('After update:', {
            disabled: runButton.disabled,
            text: runButton.textContent
        });
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

        runButton.disabled = true;
        runButton.textContent = 'Running...';

        runTests(Array.from(selectedClasses));
    });

    /* ===== 클래스 상세보기 ===== */
    async function showClassDetail(className) {
        try {
            // 탭 헤더 표시
            tabHeader.style.display = 'flex';

            const response = await fetch(`/api/tests/class/${encodeURIComponent(className)}`);
            const data = await response.json();

            const methodsHtml = data.methods.map(method => `
                <li class="method-item">
                    <div class="method-header" onclick="toggleMethodCode('${className}', '${escapeHtml(method.methodName)}', this)">
                        <span class="method-name">✓ ${method.displayName}</span>
                        <span class="method-toggle">▶</span>
                    </div>
                    <div class="method-code-container" style="display: none;">
                        <pre><code class="java"></code></pre>
                    </div>
                </li>
            `).join('');

            currentTestInfoCache = `
                <div class="class-detail">
                    <h1 class="class-detail-title">${data.className}</h1>
                    <h2>${data.fullClassName}</h2>
                    <ul class="method-list">
                        ${methodsHtml}
                    </ul>
                </div>
            `;

            // Test Information 탭으로 전환
            switchTab('test-info');
        } catch (error) {
            console.error('Failed to load class detail:', error);
            currentTestInfoCache = `
                <div class="empty-state">
                    <div class="empty-state-icon">❌</div>
                    <p>Failed to load class details</p>
                </div>
            `;
            switchTab('test-info');
        }
    }

    // 전역 함수로 노출
    window.showClassDetail = showClassDetail;

    /* ===== 메서드 코드 토글 ===== */
    window.toggleMethodCode = async function(className, methodName, headerElement) {
        const methodItem = headerElement.parentElement;
        const codeContainer = methodItem.querySelector('.method-code-container');
        const codeElement = codeContainer.querySelector('code');
        const toggle = headerElement.querySelector('.method-toggle');

        // 이미 열려있으면 닫기
        if (codeContainer.style.display !== 'none') {
            codeContainer.style.display = 'none';
            toggle.textContent = '▶';
            return;
        }

        // 코드가 아직 로드되지 않았으면 로드
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

        // 코드 표시
        codeContainer.style.display = 'block';
        toggle.textContent = '▼';
    };

    /* ===== 테스트 실행 ===== */
    async function runTests(classNames) {
        try {
            // 탭 헤더 표시
            tabHeader.style.display = 'flex';

            const response = await fetch('/api/tests/run', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ classNames })
            });

            const result = await response.json();

            if (result.status === 'COMPLETED') {
                // 결과 캐시 초기화 (새로운 테스트 실행이므로)
                currentTestResultsCache = null;
                await showTestResults();
                // Test Results 탭으로 자동 전환
                switchTab('test-results');
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

    /* ===== 테스트 결과 보기 ===== */
    async function showTestResults() {
        try {
            // 탭 헤더 표시
            tabHeader.style.display = 'flex';

            const response = await fetch('/api/tests/results');
            const data = await response.json();

            const summary = data.summary;
            const results = data.results;

            const resultsHtml = results.map(result => renderTestResult(result)).join('');

            currentTestResultsCache = `
                <div class="class-detail">
                    <h1 class="class-detail-title">Test Results</h1>
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

            // 현재 탭이 test-results이면 바로 표시
            if (currentTab === 'test-results') {
                contentArea.innerHTML = currentTestResultsCache;
            }
        } catch (error) {
            console.error('Failed to load test results:', error);
            currentTestResultsCache = `
                <div class="empty-state">
                    <div class="empty-state-icon">❌</div>
                    <p>Failed to load test results</p>
                </div>
            `;
            if (currentTab === 'test-results') {
                contentArea.innerHTML = currentTestResultsCache;
            }
        }
    }

    // 전역 함수로 노출
    window.showTestResults = showTestResults;

    function renderTestResult(result) {
        const icon = result.status === 'SUCCESS' ? '✓' :
                     result.status === 'FAILED' ? '✗' :
                     result.status === 'SKIPPED' ? '⊘' : '';

        const childrenHtml = result.children && result.children.length > 0
            ? `<ul class="result-children">${result.children.map(child => renderTestResult(child)).join('')}</ul>`
            : '';

        const errorHtml = result.errorMessage
            ? `<div class="result-error">${escapeHtml(result.errorMessage)}</div>`
            : '';

        return `
            <li class="result-item ${result.status}">
                <div class="result-header">
                    <span class="result-name">${icon} ${escapeHtml(result.displayName)}</span>
                    <span class="result-duration">${result.durationMillis}ms</span>
                </div>
                ${errorHtml}
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

        // 검색어가 있으면 일치하는 쪽은 펼쳐주기
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

/* ===== 네비게이션 함수들 (전역) ===== */
function showWelcome() {
    const contentArea = document.getElementById('contentArea');
    const tabHeader = document.getElementById('tabHeader');

    // 탭 헤더 숨기기
    tabHeader.style.display = 'none';

    contentArea.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">📋</div>
            <p>Select a test class to view details or check tests and run them</p>
        </div>
    `;

    // 선택 해제
    document.querySelectorAll('.class-node').forEach(n => n.classList.remove('selected'));
}

async function showLatestResults() {
    if (window.showTestResults) {
        await window.showTestResults();
        // Test Results 탭으로 전환
        const tabButtons = document.querySelectorAll('.tab-button');
        tabButtons.forEach(btn => {
            if (btn.dataset.tab === 'test-results') {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }
}