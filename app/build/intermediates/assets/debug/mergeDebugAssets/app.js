// Application Data
const appData = {
    company: {
        name: "Sleep Company",
        industry: "Mattress Manufacturing",
        primaryColor: "#1976D2",
        secondaryColor: "#03DAC6",
        logo: "SC"
    },
    sampleProducts: [
        {
            id: "MTR001",
            name: "Premium Memory Foam Mattress",
            size: "Queen",
            status: "In Production",
            currentStage: "Assembly",
            progress: 60,
            rfidTag: "RF1234567890",
            startDate: "2025-09-01",
            expectedCompletion: "2025-09-05",
            stages: [
                { "name": "Raw Material", "status": "completed", "timestamp": "2025-09-01 08:00" },
                { "name": "Cutting", "status": "completed", "timestamp": "2025-09-01 14:30" },
                { "name": "Assembly", "status": "in-progress", "timestamp": "2025-09-02 09:15" },
                { "name": "QC Check", "status": "pending", "timestamp": "" },
                { "name": "Packaging", "status": "pending", "timestamp": "" },
                { "name": "Shipping", "status": "pending", "timestamp": "" }
            ]
        },
        {
            id: "MTR002",
            name: "Hybrid Spring Mattress",
            size: "King",
            status: "QC Review",
            currentStage: "QC Check",
            progress: 75,
            rfidTag: "RF2345678901",
            startDate: "2025-08-30",
            expectedCompletion: "2025-09-04",
            stages: [
                { "name": "Raw Material", "status": "completed", "timestamp": "2025-08-30 08:00" },
                { "name": "Cutting", "status": "completed", "timestamp": "2025-08-30 15:00" },
                { "name": "Assembly", "status": "completed", "timestamp": "2025-09-01 16:30" },
                { "name": "QC Check", "status": "in-progress", "timestamp": "2025-09-02 10:00" },
                { "name": "Packaging", "status": "pending", "timestamp": "" },
                { "name": "Shipping", "status": "pending", "timestamp": "" }
            ]
        },
        {
            id: "MTR003",
            name: "Organic Cotton Mattress",
            size: "Twin",
            status: "Completed",
            currentStage: "Shipping",
            progress: 100,
            rfidTag: "RF3456789012",
            startDate: "2025-08-28",
            expectedCompletion: "2025-09-02",
            stages: [
                { "name": "Raw Material", "status": "completed", "timestamp": "2025-08-28 08:00" },
                { "name": "Cutting", "status": "completed", "timestamp": "2025-08-28 14:00" },
                { "name": "Assembly", "status": "completed", "timestamp": "2025-08-29 11:30" },
                { "name": "QC Check", "status": "completed", "timestamp": "2025-08-30 09:45" },
                { "name": "Packaging", "status": "completed", "timestamp": "2025-08-31 13:20" },
                { "name": "Shipping", "status": "completed", "timestamp": "2025-09-02 08:00" }
            ]
        }
    ],
    manufacturingStages: [
        { "id": 1, "name": "Raw Material", "description": "Initial material preparation and inspection", "color": "#FF9800" },
        { "id": 2, "name": "Cutting", "description": "Precision cutting of materials to specifications", "color": "#2196F3" },
        { "id": 3, "name": "Assembly", "description": "Assembly of mattress components", "color": "#9C27B0" },
        { "id": 4, "name": "QC Check", "description": "Quality control and inspection", "color": "#F44336" },
        { "id": 5, "name": "Packaging", "description": "Final packaging and labeling", "color": "#4CAF50" },
        { "id": 6, "name": "Shipping", "description": "Ready for shipment", "color": "#607D8B" }
    ],
    qcChecklist: [
        { "id": 1, "item": "Dimensions accurate to specification", "required": true },
        { "id": 2, "item": "No visible defects or tears", "required": true },
        { "id": 3, "item": "Proper firmness level", "required": true },
        { "id": 4, "item": "Correct labeling and tags", "required": true },
        { "id": 5, "item": "Clean and free of debris", "required": true },
        { "id": 6, "item": "RFID tag properly embedded", "required": true }
    ],
    users: [
        { "username": "admin", "password": "admin123", "role": "Administrator", "name": "Admin" },
        { "username": "operator1", "password": "pass123", "role": "Production Operator", "name": "Jane Smith" },
        { "username": "qc_manager", "password": "qc123", "role": "QC Manager", "name": "Mike Johnson" }
    ]
};

// Application State
let currentUser = null;
let currentScreen = 'dashboard';
let scanningActive = false;
let selectedProduct = null;

// DOM Elements
const loginScreen = document.getElementById('loginScreen');
const appContainer = document.getElementById('appContainer');
const navDrawer = document.getElementById('navDrawer');
const overlay = document.getElementById('overlay');
const toast = document.getElementById('toast');
const productModal = document.getElementById('productModal');

// Initialize Application
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
    showLoginScreen();
});

function initializeEventListeners() {
    // Login form
    document.getElementById('loginBtn').addEventListener('click', handleLogin);
    document.getElementById('username').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') handleLogin();
    });
    document.getElementById('password').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') handleLogin();
    });

    // Navigation
    document.getElementById('menuBtn').addEventListener('click', toggleNavDrawer);
    document.getElementById('overlay').addEventListener('click', closeNavDrawer);
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);

    // Navigation items
    document.querySelectorAll('.nav-item[data-screen]').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            const screen = this.getAttribute('data-screen');
            switchScreen(screen);
            closeNavDrawer();
        });
    });

    // Quick actions
    document.querySelectorAll('.action-btn[data-screen]').forEach(btn => {
        btn.addEventListener('click', function () {
            const screen = this.getAttribute('data-screen');
            switchScreen(screen);
        });
    });

    // RFID Scanner
    document.getElementById('startScanBtn').addEventListener('click', startRFIDScan);
    document.getElementById('stopScanBtn').addEventListener('click', stopRFIDScan);
    document.getElementById('quickScanBtn').addEventListener('click', function () {
        switchScreen('scanner');
    });

    // QC functionality
    document.getElementById('qcProductSelect').addEventListener('change', handleQCProductSelect);
    document.getElementById('approveQCBtn').addEventListener('click', approveQC);
    document.getElementById('rejectQCBtn').addEventListener('click', rejectQC);

    // Modal
    document.getElementById('modalClose').addEventListener('click', closeProductModal);
    document.getElementById('productModal').addEventListener('click', function (e) {
        if (e.target === this) closeProductModal();
    });

    // Toast
    document.getElementById('toastClose').addEventListener('click', hideToast);

    // Refresh products
    document.getElementById('refreshProducts').addEventListener('click', loadProducts);
}

function showLoginScreen() {
    loginScreen.classList.add('active');
    appContainer.classList.remove('active');
}

function showAppScreen() {
    loginScreen.classList.remove('active');
    appContainer.classList.add('active');
    initializeDashboard();
    loadProducts();
    setupQCScreen();
}

function handleLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const loginBtn = document.getElementById('loginBtn');

    // Clear previous errors
    clearLoginErrors();

    // Validate input
    if (!username) {
        showLoginError('username', 'Username is required');
        return;
    }
    if (!password) {
        showLoginError('password', 'Password is required');
        return;
    }

    // Show loading state
    loginBtn.classList.add('loading');
    loginBtn.disabled = true;

    // Simulate API call
    setTimeout(() => {
        const user = appData.users.find(u => u.username === username && u.password === password);

        loginBtn.classList.remove('loading');
        loginBtn.disabled = false;

        if (user) {
            currentUser = user;
            updateUserInfo();
            showAppScreen();
            showToast('Login successful!', 'success');
        } else {
            showLoginError('password', 'Invalid username or password');
        }
    }, 1500);
}

function showLoginError(field, message) {
    const errorElement = document.getElementById(field + 'Error');
    errorElement.textContent = message;
}

function clearLoginErrors() {
    document.getElementById('usernameError').textContent = '';
    document.getElementById('passwordError').textContent = '';
}

function handleLogout() {
    currentUser = null;
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    showLoginScreen();
    showToast('Logged out successfully', 'info');
}

function updateUserInfo() {
    if (currentUser) {
        document.getElementById('userName').textContent = currentUser.name;
        document.getElementById('userRole').textContent = currentUser.role;
    }
}

function toggleNavDrawer() {
    navDrawer.classList.toggle('open');
    overlay.classList.toggle('show');
}

function closeNavDrawer() {
    navDrawer.classList.remove('open');
    overlay.classList.remove('show');
}

function switchScreen(screenName) {
    // Update navigation
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-screen') === screenName) {
            item.classList.add('active');
        }
    });

    // Update content screens
    document.querySelectorAll('.content-screen').forEach(screen => {
        screen.classList.remove('active');
    });

    const targetScreen = document.getElementById(screenName + 'Screen');
    if (targetScreen) {
        targetScreen.classList.add('active');
        currentScreen = screenName;
        updateAppTitle(screenName);
    }
}

function updateAppTitle(screenName) {
    const titles = {
        'dashboard': 'Dashboard',
        'products': 'Production Tracking',
        'scanner': 'RFID Scanner',
        'qc': 'Quality Control'
    };
    document.querySelector('.app-title').textContent = titles[screenName] || 'Dashboard';
}

function initializeDashboard() {
    updateDashboardStats();
    loadRecentActivity();
}

function updateDashboardStats() {
    const totalProducts = appData.sampleProducts.length;
    const inProgress = appData.sampleProducts.filter(p => p.status !== 'Completed').length;
    const completed = appData.sampleProducts.filter(p => p.status === 'Completed').length;

    document.getElementById('totalProducts').textContent = totalProducts;
    document.getElementById('inProgress').textContent = inProgress;
    document.getElementById('completed').textContent = completed;
}

function loadRecentActivity() {
    const activities = [
        {
            icon: 'check_circle',
            iconColor: '#4CAF50',
            title: 'QC Check Completed',
            description: 'MTR003 - Organic Cotton Mattress',
            time: '2 hours ago'
        },
        {
            icon: 'build',
            iconColor: '#9C27B0',
            title: 'Assembly Stage Started',
            description: 'MTR001 - Premium Memory Foam Mattress',
            time: '4 hours ago'
        },
        {
            icon: 'nfc',
            iconColor: '#2196F3',
            title: 'RFID Tag Scanned',
            description: 'RF2345678901 - Stage updated to QC Check',
            time: '6 hours ago'
        }
    ];

    const activityList = document.getElementById('activityList');
    activityList.innerHTML = activities.map(activity => `
        <div class="activity-item">
            <div class="activity-icon" style="background-color: ${activity.iconColor}">
                <i class="material-icons">${activity.icon}</i>
            </div>
            <div class="activity-content">
                <h4>${activity.title}</h4>
                <p>${activity.description}</p>
            </div>
            <div class="activity-time">${activity.time}</div>
        </div>
    `).join('');
}

function loadProducts() {
    const productsList = document.getElementById('productsList');

    productsList.innerHTML = appData.sampleProducts.map(product => `
        <div class="product-card" onclick="showProductDetails('${product.id}')">
            <div class="product-header">
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>ID: ${product.id} | Size: ${product.size}</p>
                </div>
                <div class="product-status status-${product.status.toLowerCase().replace(/\s+/g, '-')}">
                    ${product.status}
                </div>
            </div>
            
            <div class="progress-container">
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${product.progress}%"></div>
                    <span class="progress-text">${product.progress}%</span>
                </div>
            </div>
            
            <div class="product-stages">
                ${product.stages.map(stage => `
                    <span class="stage-badge stage-${stage.status}">
                        ${stage.name}
                    </span>
                `).join('')}
            </div>
        </div>
    `).join('');
}

function showProductDetails(productId) {
    const product = appData.sampleProducts.find(p => p.id === productId);
    if (!product) return;

    selectedProduct = product;

    // Update modal content
    document.getElementById('modalProductName').textContent = product.name;
    document.getElementById('modalProductId').textContent = product.id;
    document.getElementById('modalProductSize').textContent = product.size;
    document.getElementById('modalProductRfid').textContent = product.rfidTag;
    document.getElementById('modalProductStatus').textContent = product.status;

    // Update progress
    document.getElementById('modalProgressFill').style.width = product.progress + '%';
    document.getElementById('modalProgressText').textContent = product.progress + '%';

    // Update stages timeline
    const timeline = document.getElementById('modalStagesTimeline');
    timeline.innerHTML = product.stages.map(stage => `
        <div class="timeline-item ${stage.status}">
            <div class="timeline-content">
                <h4>${stage.name}</h4>
                <p>${stage.timestamp || 'Not started'}</p>
            </div>
        </div>
    `).join('');

    // Show modal
    productModal.classList.remove('hidden');
}

function closeProductModal() {
    productModal.classList.add('hidden');
    selectedProduct = null;
}

function startRFIDScan() {
    scanningActive = true;
    document.getElementById('startScanBtn').classList.add('hidden');
    document.getElementById('stopScanBtn').classList.remove('hidden');
    document.getElementById('scanAnimation').classList.add('active');
    document.getElementById('scanResult').classList.add('hidden');

    // Simulate scan after 3 seconds
    setTimeout(() => {
        if (scanningActive) {
            simulateRFIDScan();
        }
    }, 3000);

    showToast('Scanning for RFID tags...', 'info');
}

function stopRFIDScan() {
    scanningActive = false;
    document.getElementById('startScanBtn').classList.remove('hidden');
    document.getElementById('stopScanBtn').classList.add('hidden');
    document.getElementById('scanAnimation').classList.remove('active');

    showToast('Scanning stopped', 'info');
}

function simulateRFIDScan() {
    if (!scanningActive) return;

    // Pick a random product to simulate scan
    const randomProduct = appData.sampleProducts[Math.floor(Math.random() * appData.sampleProducts.length)];

    document.getElementById('scannedTag').textContent = randomProduct.rfidTag;
    document.getElementById('scannedProduct').textContent = randomProduct.name;
    document.getElementById('scannedStage').textContent = randomProduct.currentStage;

    document.getElementById('scanResult').classList.remove('hidden');
    stopRFIDScan();

    showToast('RFID tag scanned successfully!', 'success');

    // Setup stage update functionality
    document.getElementById('updateStageBtn').onclick = function () {
        updateProductStage(randomProduct);
    };

    document.getElementById('logProcessBtn').onclick = function () {
        logProcess(randomProduct);
    };
}

function updateProductStage(product) {
    const currentStageIndex = appData.manufacturingStages.findIndex(stage => stage.name === product.currentStage);
    const nextStageIndex = Math.min(currentStageIndex + 1, appData.manufacturingStages.length - 1);

    if (nextStageIndex > currentStageIndex) {
        const nextStage = appData.manufacturingStages[nextStageIndex];
        product.currentStage = nextStage.name;
        product.progress = Math.min(product.progress + 15, 100);

        // Update stage status
        product.stages[currentStageIndex].status = 'completed';
        product.stages[currentStageIndex].timestamp = new Date().toLocaleString();

        if (nextStageIndex < product.stages.length) {
            product.stages[nextStageIndex].status = 'in-progress';
            product.stages[nextStageIndex].timestamp = new Date().toLocaleString();
        }

        showToast(`Stage updated to ${nextStage.name}`, 'success');
        loadProducts();
        updateDashboardStats();
    } else {
        showToast('Product is already at final stage', 'info');
    }
}

function logProcess(product) {
    const logEntry = `Process logged for ${product.name} at stage ${product.currentStage} - ${new Date().toLocaleString()}`;
    console.log(logEntry);
    showToast('Process logged successfully', 'success');
}

function setupQCScreen() {
    const qcProductSelect = document.getElementById('qcProductSelect');
    const qcProducts = appData.sampleProducts.filter(p => p.currentStage === 'QC Check' || p.status === 'QC Review');

    qcProductSelect.innerHTML = '<option value="">Select Product for QC</option>' +
        qcProducts.map(product =>
            `<option value="${product.id}">${product.name} (${product.id})</option>`
        ).join('');
}

function handleQCProductSelect() {
    const selectedProductId = document.getElementById('qcProductSelect').value;
    const qcChecklist = document.getElementById('qcChecklist');

    if (selectedProductId) {
        selectedProduct = appData.sampleProducts.find(p => p.id === selectedProductId);
        loadQCChecklist();
        qcChecklist.classList.remove('hidden');
    } else {
        qcChecklist.classList.add('hidden');
        selectedProduct = null;
    }
}

function loadQCChecklist() {
    const qcItems = document.getElementById('qcItems');

    qcItems.innerHTML = appData.qcChecklist.map(item => `
        <div class="qc-item">
            <input type="checkbox" id="qc_${item.id}" ${item.required ? 'required' : ''}>
            <label for="qc_${item.id}">${item.item}</label>
        </div>
    `).join('');
}

function approveQC() {
    if (!selectedProduct) return;

    const checkboxes = document.querySelectorAll('#qcItems input[type="checkbox"]');
    const allChecked = Array.from(checkboxes).every(cb => cb.checked);

    if (!allChecked) {
        showToast('Please complete all QC checklist items', 'error');
        return;
    }

    // Update product status
    selectedProduct.currentStage = 'Packaging';
    selectedProduct.status = 'In Production';
    selectedProduct.progress = Math.min(selectedProduct.progress + 15, 100);

    // Update stages
    const qcStageIndex = selectedProduct.stages.findIndex(s => s.name === 'QC Check');
    if (qcStageIndex !== -1) {
        selectedProduct.stages[qcStageIndex].status = 'completed';
        selectedProduct.stages[qcStageIndex].timestamp = new Date().toLocaleString();

        if (qcStageIndex + 1 < selectedProduct.stages.length) {
            selectedProduct.stages[qcStageIndex + 1].status = 'in-progress';
            selectedProduct.stages[qcStageIndex + 1].timestamp = new Date().toLocaleString();
        }
    }

    showToast('QC approved - Product moved to Packaging', 'success');
    loadProducts();
    updateDashboardStats();
    setupQCScreen();

    // Clear selection
    document.getElementById('qcProductSelect').value = '';
    document.getElementById('qcChecklist').classList.add('hidden');
}

function rejectQC() {
    if (!selectedProduct) return;

    showToast('QC rejected - Product returned for rework', 'error');

    // In a real app, you might move the product back to a previous stage
    selectedProduct.status = 'Rework Required';

    loadProducts();
    updateDashboardStats();
    setupQCScreen();

    // Clear selection
    document.getElementById('qcProductSelect').value = '';
    document.getElementById('qcChecklist').classList.add('hidden');
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    toastMessage.textContent = message;

    // Update toast styling based on type
    toast.style.background = type === 'success' ? '#4CAF50' :
        type === 'error' ? '#F44336' :
            type === 'warning' ? '#FF9800' : '#2196F3';

    toast.classList.add('show');

    // Auto hide after 4 seconds
    setTimeout(() => {
        hideToast();
    }, 4000);
}

function hideToast() {
    document.getElementById('toast').classList.remove('show');
}

// Utility functions
function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString();
}

function formatTime(timeString) {
    return new Date(timeString).toLocaleTimeString();
}
// Password view icon
function togglePassword() {
  const passwordInput = document.getElementById("password");
  const toggleIcon = document.querySelector(".toggle-password i");

  if (passwordInput.type === "password") {
    passwordInput.type = "text";
    toggleIcon.textContent = "visibility_off"; // change to eye-off icon
  } else {
    passwordInput.type = "password";
    toggleIcon.textContent = "visibility"; // change back to eye icon
  }
}
