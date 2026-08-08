// MARATONTRENER WEB APPLICATION LOGIC
// 100% Parity with Android app operations, designs, and calculations

function getLocalDateString(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

// Helpers to format values matching Java expectations
function formatDistance(dist) {
    const val = parseFloat(dist || 0);
    return (Math.round((val + 0.00001) * 10) / 10).toFixed(1);
}

function formatPace(pace) {
    if (!pace) return '--:--';
    if (typeof pace === 'string' && pace.includes(':')) return pace;
    const val = parseFloat(pace);
    if (isNaN(val) || val <= 0) return '--:--';
    const min = Math.floor(val);
    const sec = Math.round((val - min) * 60);
    return `${min}:${sec.toString().padStart(2, '0')}`;
}

function parsePaceToDecimal(pace) {
    if (!pace) return 0;
    if (typeof pace === 'number') return pace;
    if (typeof pace === 'string') {
        if (pace.includes(':')) {
            const parts = pace.split(':');
            const min = parseInt(parts[0], 10);
            const sec = parseInt(parts[1], 10);
            if (!isNaN(min) && !isNaN(sec)) {
                return min + (sec / 60);
            }
        }
        const val = parseFloat(pace);
        return isNaN(val) ? 0 : val;
    }
    return 0;
}

// Generate new unique Runner ID for standalone web app users
function generateNewRunnerId() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = 'MT-';
    for (let i = 0; i < 6; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

// Save profile details locally for fast startup/offline capability
// Update the profile avatar image across the UI
function updateProfileAvatarUI(base64Image) {
    if (base64Image) {
        // Large avatar container on Profile page
        const uploadEl = document.getElementById('profile-avatar-upload');
        if (uploadEl) {
            uploadEl.innerHTML = `<img src="${base64Image}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
        }
        // Top toolbar right avatar
        const toolbarAvatar = document.getElementById('toolbar-avatar');
        if (toolbarAvatar) {
            toolbarAvatar.innerHTML = `<img src="${base64Image}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
        }
    } else {
        // Default default icon
        const uploadEl = document.getElementById('profile-avatar-upload');
        if (uploadEl) {
            uploadEl.innerHTML = `<i class="fa-solid fa-user"></i>`;
        }
        const toolbarAvatar = document.getElementById('toolbar-avatar');
        if (toolbarAvatar) {
            toolbarAvatar.innerHTML = `<i class="fa-solid fa-user"></i>`;
        }
    }
}

function saveProfileLocally() {
    if (appState.readOnly) return;
    const profileData = {
        userName: appState.userName,
        fullName: appState.fullName || '',
        email: appState.email || '',
        age: appState.age,
        weight: appState.weight,
        maxHr: appState.maxHr,
        pb10k: appState.pb10k,
        pbHalf: appState.pbHalf,
        pbFull: appState.pbFull,
        currentRace: appState.userProfile.currentRace,
        eventLocation: appState.userProfile.eventLocation || '',
        gpxRoute: appState.userProfile.gpxRoute || null,
        planStartDate: appState.userProfile.planStartDate || 0,
        avatar: appState.avatar
    };
    localStorage.setItem('maratontrener_profile', JSON.stringify(profileData));
}

// Load profile details from local storage cache
function loadProfileLocally() {
    const stored = localStorage.getItem('maratontrener_profile');
    if (stored) {
        try {
            const data = JSON.parse(stored);
            if (data.userName) appState.userName = data.userName;
            if (data.fullName) appState.fullName = data.fullName;
            if (data.email) appState.email = data.email;
            if (data.age) appState.age = parseInt(data.age);
            if (data.weight) appState.weight = parseFloat(data.weight);
            if (data.maxHr) appState.maxHr = parseInt(data.maxHr);
            if (data.pb10k !== undefined) appState.pb10k = data.pb10k;
            if (data.pbHalf !== undefined) appState.pbHalf = data.pbHalf;
            if (data.pbFull !== undefined) appState.pbFull = data.pbFull;
            if (data.currentRace) appState.userProfile.currentRace = data.currentRace;
            if (data.eventLocation !== undefined) appState.userProfile.eventLocation = data.eventLocation;
            if (data.gpxRoute !== undefined) appState.userProfile.gpxRoute = data.gpxRoute;
            if (data.planStartDate !== undefined) appState.userProfile.planStartDate = data.planStartDate;
            if (data.avatar) {
                appState.avatar = data.avatar;
                // Wait slightly for DOM to be parsed and elements cached before updating UI
                setTimeout(() => {
                    updateProfileAvatarUI(data.avatar);
                }, 50);
            }
        } catch (e) {
            console.error("Failed to parse local profile:", e);
        }
    }
}

// Initialize default/initial profile in cloud database for a new web user
function initializeProfileInFirebase() {
    if (!db || !appState.userId || appState.readOnly) return;
    db.ref(`profiles/${appState.userId}`).set({
        name: appState.fullName || appState.userName || 'Runner',
        nickname: appState.userName || 'Athlete',
        email: appState.email || '',
        distance: parseFloat(appState.userProfile.distance) || 0,
        consistency: parseInt(appState.userProfile.consistency) || 0,
        workoutsDone: parseInt(appState.userProfile.workoutsDone) || 0,
        workoutsTotal: parseInt(appState.userProfile.workoutsTotal) || 0,
        currentRace: appState.userProfile.currentRace || 'Oslo Maraton - Marathon',
        eventLocation: appState.userProfile.eventLocation || '',
        age: parseInt(appState.age) || 35,
        weight: parseFloat(appState.weight) || 70,
        maxHr: parseInt(appState.maxHr) || 185,
        pb10k: appState.pb10k || '',
        pbHalf: appState.pbHalf || '',
        pbFull: appState.pbFull || '',
        avatar: appState.avatar || null,
        lastUpdate: Date.now()
    });
}

// Application State
let appState = {
    userId: localStorage.getItem('maratontrener_userId') || '',
    userName: 'Runner',
    userProfile: {
        distance: 0,
        consistency: 0,
        workoutsDone: 0,
        workoutsTotal: 0,
        currentRace: 'Oslo Maraton - Marathon'
    },
    workouts: [],
    buddies: [],
    activeTab: 'home',
    firebaseConnected: false,
    readOnly: false,
    weight: 70,
    age: 35,
    maxHr: 185,
    pb10k: '',
    pbHalf: '',
    pbFull: '',
    avatar: null,
    favoriteMeals: [],
    shoes: [],
    activeDietTab: 'week',
    scalePortions: false,
    wizardPage: 1
};

// UI Components Cache
let elements = {};

// GPX Viewer State Variables
let gpxMapInstance = null;
let gpxChartInstance = null;
let currentUploadedGpxData = undefined; // undefined = unchanged, null = deleted, object = new route

// Weather Cache
const weatherCache = {
    geocode: {}, // key: location_string, value: {lat, lon}
    forecast: {} // key: "location_mode", value: {temp, iconClass, timestamp}
};

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    cacheElements();
    initializeModeAndUser();
    setupEventListeners();
    checkFirebaseConnection();
    loadLocalFallbackData();
});

function cacheElements() {
    elements = {
        deviceWrapper: document.getElementById('device-wrapper'),
        appToolbarTitle: document.getElementById('app-toolbar-title'),
        btnToolbarLeft: document.getElementById('btn-toolbar-left'),
        toolbarLeftIcon: document.getElementById('toolbar-left-icon'),
        btnToolbarRight: document.getElementById('btn-toolbar-right'),
        toolbarAvatar: document.getElementById('toolbar-avatar'),
        appContentScroll: document.getElementById('app-content-scroll'),
        
        // Pages
        pageHome: document.getElementById('page-home'),
        pageBuddies: document.getElementById('page-buddies'),
        pageLog: document.getElementById('page-log'),
        pageStats: document.getElementById('page-stats'),
        pageProfile: document.getElementById('page-profile'),
        pageDiet: document.getElementById('page-diet'),
        
        // Bottom Nav Buttons
        navBtnHome: document.getElementById('nav-btn-home'),
        navBtnBuddies: document.getElementById('nav-btn-buddies'),
        navBtnLog: document.getElementById('nav-btn-log'),
        navBtnStats: document.getElementById('nav-btn-stats'),
        navBtnProfile: document.getElementById('nav-btn-profile'),
        
        // Home Screen Fields
        homeRaceName: document.getElementById('home-race-name'),
        homeRaceCategory: document.getElementById('home-race-category'),
        homeRaceDate: document.getElementById('home-race-date'),
        homeRaceLocation: document.getElementById('home-race-location'),
        homeProgressBar: document.getElementById('home-progress-bar'),
        homeProgressPercent: document.getElementById('home-progress-percent'),
        
        labelLatestActivity: document.getElementById('label-latest-activity'),
        cardLatestActivity: document.getElementById('card-latest-activity'),
        latestUserName: document.getElementById('latest-user-name'),
        latestTimeAgo: document.getElementById('latest-time-ago'),
        latestActivityTitle: document.getElementById('latest-activity-title'),
        latestValDist: document.getElementById('latest-val-dist'),
        latestLblDist: document.getElementById('latest-lbl-dist'),
        latestValDur: document.getElementById('latest-val-dur'),
        latestLblDur: document.getElementById('latest-lbl-dur'),
        latestValPace: document.getElementById('latest-val-pace'),
        latestLblPace: document.getElementById('latest-lbl-pace'),
        latestValHr: document.getElementById('latest-val-hr'),
        
        labelNextSession: document.getElementById('label-next-session'),
        cardNextSession: document.getElementById('card-next-session'),
        nextType: document.getElementById('next-type'),
        nextDetails: document.getElementById('next-details'),
        nextDate: document.getElementById('next-date'),
        cardNutritionLink: document.getElementById('card-nutrition-link'),
        
        // Pace Calculator Fields
        calcSpinner: document.getElementById('calc-spinner'),
        calcCustomDistContainer: document.getElementById('calc-custom-dist-container'),
        calcCustomDistInput: document.getElementById('calc-custom-dist-input'),
        calcTimeInput: document.getElementById('calc-time-input'),
        calcPaceInput: document.getElementById('calc-pace-input'),
        calcSpeedInput: document.getElementById('calc-speed-input'),
        
        // Buddies Page Fields
        textYourId: document.getElementById('text-your-id'),
        btnShareIdCopy: document.getElementById('btn-share-id-copy'),
        stravaSyncSection: document.getElementById('strava-sync-section'),
        stravaStatusBadge: document.getElementById('strava-status-badge'),
        btnStravaAction: document.getElementById('btn-strava-action'),
        formAddBuddyInline: document.getElementById('form-add-buddy-inline'),
        inputBuddyIdInline: document.getElementById('input-buddy-id-inline'),
        leaderboardList: document.getElementById('leaderboard-list'),
        
        // Log Page Fields
        planFilterDropdown: document.getElementById('plan-filter-dropdown'),
        logWorkoutsList: document.getElementById('log-workouts-list'),
        fabAddWorkout: document.getElementById('fab-add-workout'),
        
        // Stats Page Fields
        statsValPace: document.getElementById('stats-val-pace'),
        statsValHr: document.getElementById('stats-val-hr'),
        statsValDuration: document.getElementById('stats-val-duration'),
        
        // Profile Page Fields
        profileAvatarUpload: document.getElementById('profile-avatar-upload'),
        profileAvatarFile: document.getElementById('profile-avatar-file'),
        profileTitleName: document.getElementById('profile-title-name'),
        profileTitleEmail: document.getElementById('profile-title-email'),
        formProfileSetup: document.getElementById('form-profile-setup'),
        profileInputName: document.getElementById('profile-input-name'),
        profileInputNickname: document.getElementById('profile-input-nickname'),
        profileInputEmail: document.getElementById('profile-input-email'),
        profileInputAge: document.getElementById('profile-input-age'),
        profileInputWeight: document.getElementById('profile-input-weight'),
        profileInputMaxhr: document.getElementById('profile-input-maxhr'),
        profileInputPb10k: document.getElementById('profile-input-pb10k'),
        profileInputPbhalf: document.getElementById('profile-input-pbhalf'),
        profileInputPbfull: document.getElementById('profile-input-pbfull'),
        zone1Text: document.getElementById('zone-1-text'),
        zone2Text: document.getElementById('zone-2-text'),
        zone3Text: document.getElementById('zone-3-text'),
        zone4Text: document.getElementById('zone-4-text'),
        zone5Text: document.getElementById('zone-5-text'),
        btnLaunchGenerator: document.getElementById('btn-launch-generator'),
        profileSyncIdInput: document.getElementById('profile-sync-id-input'),
        btnSyncProfileId: document.getElementById('btn-sync-profile-id'),
        btnRestoreCloud: document.getElementById('btn-restore-cloud'),
        btnDeleteNuclear: document.getElementById('btn-delete-nuclear'),
        
        // Diet Sub-Screen Fields
        dietSwitchScale: document.getElementById('diet-switch-scale'),
        dietAdviceWeek: document.getElementById('diet-advice-week'),
        dietAdviceBannerText: document.getElementById('diet-advice-banner-text'),
        dietProfileDescLbl: document.getElementById('diet-profile-desc-lbl'),
        dietScaleFactorBadge: document.getElementById('diet-scale-factor-badge'),
        btnDietTabWeekVp: document.getElementById('btn-diet-tab-week-vp'),
        btnDietTabFavoritesVp: document.getElementById('btn-diet-tab-favorites-vp'),
        btnDietTabAllVp: document.getElementById('btn-diet-tab-all-vp'),
        dietEmptyRecipesState: document.getElementById('diet-empty-recipes-state'),
        dietMealsListVp: document.getElementById('diet-meals-list-vp'),
        
        // Modals
        generatorWizardModal: document.getElementById('generator-wizard-modal'),
        workoutModal: document.getElementById('workout-modal'),
        modalTitle: document.getElementById('modal-title'),
        buddyModal: document.getElementById('buddy-modal'),
        raceInfoModal: document.getElementById('race-info-modal'),
        
        // Wizard Fields
        btnCloseWizard: document.getElementById('btn-close-wizard'),
        btnWizBack: document.getElementById('btn-wiz-back'),
        btnWizNext: document.getElementById('btn-wiz-next'),
        wizEventName: document.getElementById('wiz-event-name'),
        wizEventLocation: document.getElementById('wiz-event-location'),
        wizEventDate: document.getElementById('wiz-event-date'),
        wizGoalTime: document.getElementById('wiz-goal-time'),
        wizRaceType: document.getElementById('wiz-race-type'),
        wizAge: document.getElementById('wiz-age'),
        wizWeight: document.getElementById('wiz-weight'),
        wizMaxhr: document.getElementById('wiz-maxhr'),
        wizPb10k: document.getElementById('wiz-pb10k'),
        wizPbhalf: document.getElementById('wiz-pbhalf'),
        wizPbfull: document.getElementById('wiz-pbfull'),
        wizStartDate: document.getElementById('wiz-start-date'),
        wizDaysPerWeek: document.getElementById('wiz-days-per-week'),
        wizCheckStrength: document.getElementById('wiz-check-strength'),
        
        // Workout Edit Form Fields
        workoutForm: document.getElementById('workout-form'),
        workoutId: document.getElementById('workout-id'),
        workoutDate: document.getElementById('workout-date'),
        workoutWeek: document.getElementById('workout-week'),
        workoutType: document.getElementById('workout-type'),
        workoutPlan: document.getElementById('workout-plan'),
        workoutDistance: document.getElementById('workout-distance'),
        workoutPace: document.getElementById('workout-pace'),
        workoutDuration: document.getElementById('workout-duration'),
        workoutAvgHr: document.getElementById('workout-avg-hr'),
        workoutDescription: document.getElementById('workout-description'),
        workoutNotes: document.getElementById('workout-notes'),
        workoutCompleted: document.getElementById('workout-completed'),
        btnCloseWorkoutModal: document.getElementById('btn-close-workout-modal'),
        btnCancelWorkout: document.getElementById('btn-cancel-workout'),
        workoutIntervalSection: document.getElementById('workout-interval-section'),
        workoutIntervalCount: document.getElementById('workout-interval-count'),
        workoutIntervalValue: document.getElementById('workout-interval-value'),
        workoutIntervalPace: document.getElementById('workout-interval-pace'),
        workoutMaxHr: document.getElementById('workout-max-hr'),
        
        // Race Info Modal Fields
        raceInfoForm: document.getElementById('race-info-form'),
        raceInputName: document.getElementById('race-input-name'),
        raceInputType: document.getElementById('race-input-type'),
        raceInputDate: document.getElementById('race-input-date'),
        raceInputLocation: document.getElementById('race-input-location'),
        btnCloseRaceModal: document.getElementById('btn-close-race-modal'),
        btnCancelRace: document.getElementById('btn-cancel-race'),
        gpxFileInput: document.getElementById('gpx-file-input'),
        gpxUploadBox: document.getElementById('gpx-upload-box'),
        gpxStatusContainer: document.getElementById('gpx-status-container'),
        gpxStatusText: document.getElementById('gpx-status-text'),
        btnRemoveGpx: document.getElementById('btn-remove-gpx'),
        gpxViewerSection: document.getElementById('gpx-viewer-section'),
        
        // Buddy Modal
        btnCloseBuddyModal: document.getElementById('btn-close-buddy-modal'),
        btnCancelBuddy: document.getElementById('btn-cancel-buddy'),
        buddyForm: document.getElementById('buddy-form'),
        buddyIdInput: document.getElementById('buddy-id-input'),
        
        // Navigation Drawer Elements
        navDrawer: document.getElementById('nav-drawer'),
        drawerOverlay: document.getElementById('drawer-overlay'),
        btnCloseDrawer: document.getElementById('btn-close-drawer'),
        drawerUserName: document.getElementById('drawer-user-name'),
        drawerUserId: document.getElementById('drawer-user-id'),
        drawerAvatarDisplay: document.getElementById('drawer-avatar-display'),
        drawerLinkHome: document.getElementById('drawer-link-home'),
        drawerLinkBuddies: document.getElementById('drawer-link-buddies'),
        drawerLinkLog: document.getElementById('drawer-link-log'),
        drawerLinkStats: document.getElementById('drawer-link-stats'),
        drawerLinkProfile: document.getElementById('drawer-link-profile'),
        drawerActionGenerator: document.getElementById('drawer-action-generator'),
        drawerActionSync: document.getElementById('drawer-action-sync'),
        
        // Interactive Dashboard Elements
        cardRaceOverview: document.getElementById('card-race-overview'),
        cardNextSession: document.getElementById('card-next-session'),
        cardLatestActivity: document.getElementById('card-latest-activity'),
        
        // Replicated Stats Elements
        textCountdown: document.getElementById('text-countdown'),
        statsPlannedDist: document.getElementById('stats-planned-dist'),
        statsConsistency: document.getElementById('stats-consistency'),
        statsTotalRuns: document.getElementById('stats-total-runs'),
        statsLongestRun: document.getElementById('stats-longest-run'),
        cardStatsLongestRun: document.getElementById('card-stats-longest-run'),
        statsDistance: document.getElementById('stats-distance'),
        statsAveragePace: document.getElementById('stats-average-pace'),
        statsIntervalStat: document.getElementById('stats-interval-stat'),
        statsStrengthDone: document.getElementById('stats-strength-done'),
        statsMissed: document.getElementById('stats-missed'),
        cardStatsMissed: document.getElementById('card-stats-missed'),
        statsAvgHrRuns: document.getElementById('stats-avg-hr-runs'),
        
        // Shoe Tracking Elements
        profileShoesCard: document.getElementById('profile-shoes-card'),
        btnAddShoeModal: document.getElementById('btn-add-shoe-modal'),
        shoesListContainer: document.getElementById('shoes-list-container'),
        workoutShoe: document.getElementById('workout-shoe'),
        shoeModal: document.getElementById('shoe-modal'),
        shoeModalTitle: document.getElementById('shoe-modal-title'),
        btnCloseShoeModal: document.getElementById('btn-close-shoe-modal'),
        shoeForm: document.getElementById('shoe-form'),
        shoeIdInput: document.getElementById('shoe-id-input'),
        shoeNameInput: document.getElementById('shoe-name-input'),
        shoeInitialMileageInput: document.getElementById('shoe-initial-mileage-input'),
        shoeLimitInput: document.getElementById('shoe-limit-input'),
        shoeRetiredInput: document.getElementById('shoe-retired-input'),
        btnDeleteShoe: document.getElementById('btn-delete-shoe'),
        btnCancelShoe: document.getElementById('btn-cancel-shoe'),
        
        // Admin Dashboard
        adminSyncSection: document.getElementById('admin-sync-section'),
        btnOpenAdminDashboard: document.getElementById('btn-open-admin-dashboard'),
        adminDashboardModal: document.getElementById('modal-admin-dashboard'),
        btnCloseAdminModal: document.getElementById('btn-close-admin-modal'),
        btnCloseAdminFooter: document.getElementById('btn-close-admin-footer'),
        btnRefreshAdmin: document.getElementById('btn-refresh-admin'),
        adminSearchInput: document.getElementById('admin-search-input'),
        adminRunnersTableBody: document.getElementById('admin-runners-table-body'),
        adminStatTotal: document.getElementById('admin-stat-total'),
        adminStatActive: document.getElementById('admin-stat-active'),
        adminStatRuns: document.getElementById('admin-stat-runs')
    };
}

// Setup View-Only or Edit Mode based on URL query parameters
function initializeModeAndUser() {
    const urlParams = new URLSearchParams(window.location.search);
    const viewId = urlParams.get('view') || urlParams.get('runner');
    const hasEditParam = urlParams.get('edit') === 'true' || urlParams.get('mode') === 'edit';
    const hasReadOnlyParam = urlParams.get('readOnly') === 'true' || urlParams.get('share') === 'true';

    if (viewId) {
        appState.userId = viewId.toUpperCase();
        appState.readOnly = true;
    } else if (hasReadOnlyParam) {
        appState.readOnly = true;
    } else {
        // Standalone Web App defaults to Edit Mode so users (e.g. on iPhone) can log training
        appState.readOnly = false;
        
        let storedUserId = localStorage.getItem('maratontrener_userId');
        if (!storedUserId) {
            storedUserId = generateNewRunnerId();
            localStorage.setItem('maratontrener_userId', storedUserId);
        }
        appState.userId = storedUserId;
        
        // Load local profile data from cache
        loadProfileLocally();
    }

    if (appState.readOnly) {
        document.body.classList.add('read-only-mode');
        document.querySelectorAll('.profile-read-only-msg').forEach(el => el.style.display = 'block');
        document.querySelectorAll('#form-profile-setup input').forEach(input => input.disabled = true);
    } else {
        document.body.classList.remove('read-only-mode');
        document.querySelectorAll('.profile-read-only-msg').forEach(el => el.style.display = 'none');
        document.querySelectorAll('#form-profile-setup input').forEach(input => input.disabled = false);
    }
    
    // Check if user is returning from Strava authorization
    checkStravaCallback();
}

// Viewport layout orientation control
function setViewportMode(mode) {
    elements.deviceWrapper.className = `device-simulator ${mode}`;
    document.getElementById('vp-portrait').classList.toggle('active', mode === 'portrait');
    document.getElementById('vp-landscape').classList.toggle('active', mode === 'landscape');
    document.getElementById('vp-fullscreen').classList.toggle('active', mode === 'fullscreen');
    
    // Resize charts to fit viewport dimension changes correctly
    setTimeout(() => {
        renderAnalyticsCharts();
    }, 250);
}
window.setViewportMode = setViewportMode;

// Setup event bindings
function setupEventListeners() {
    // Bottom Nav Click Handlers
    elements.navBtnHome.addEventListener('click', () => navTo('home'));
    elements.navBtnBuddies.addEventListener('click', () => navTo('buddies'));
    elements.navBtnLog.addEventListener('click', () => navTo('log'));
    elements.navBtnStats.addEventListener('click', () => navTo('stats'));
    elements.navBtnProfile.addEventListener('click', () => navTo('profile'));
    
    // Toolbar Avatar Click -> Profile
    elements.btnToolbarRight.addEventListener('click', () => navTo('profile'));
    
    // Toolbar Left Click -> Context Action (either Menu/Start or Back arrow)
    elements.btnToolbarLeft.addEventListener('click', handleToolbarLeftClick);
    
    // Nutrition Card Click -> slide-in Diet screen
    elements.cardNutritionLink.addEventListener('click', () => navTo('diet'));
    
    // Calculator Auto Format & Live Calculations
    setupCalculatorBindings();
    
    // Profile Management
    elements.formProfileSetup.addEventListener('submit', handleProfileSave);
    setupProfileFormFormatting();
    
    elements.btnSyncProfileId.addEventListener('click', handleSyncIdClick);
    elements.btnRestoreCloud.addEventListener('click', restoreFromCloud);
    elements.btnDeleteNuclear.addEventListener('click', confirmDeleteData);
    elements.btnLaunchGenerator.addEventListener('click', openGeneratorModal);
    
    // Log Page Dropdown & FAB
    elements.planFilterDropdown.addEventListener('change', renderWorkoutsList);
    elements.fabAddWorkout.addEventListener('click', () => openWorkoutModal(null));
    
    // Wizard Form Stepper bindings
    elements.btnCloseWizard.addEventListener('click', closeWizardModal);
    elements.btnWizBack.addEventListener('click', navigateWizardBack);
    elements.btnWizNext.addEventListener('click', navigateWizardNext);
    setupWizardAutoFormatting();
    
    // Add Buddy inline form
    elements.formAddBuddyInline.addEventListener('submit', handleBuddySubmitInline);
    elements.btnShareIdCopy.addEventListener('click', copyShareId);
    if (elements.btnStravaAction) {
        elements.btnStravaAction.addEventListener('click', handleStravaAction);
    }
    
    // Diet Tab buttons
    elements.btnDietTabWeekVp.addEventListener('click', () => switchDietTab('week'));
    elements.btnDietTabFavoritesVp.addEventListener('click', () => switchDietTab('favorites'));
    elements.btnDietTabAllVp.addEventListener('click', () => switchDietTab('all'));
    elements.dietSwitchScale.addEventListener('change', handleScalePortionsChange);
    
    // Workout details Form
    elements.workoutForm.addEventListener('submit', handleWorkoutSubmit);
    elements.btnCloseWorkoutModal.addEventListener('click', closeWorkoutModal);
    elements.btnCancelWorkout.addEventListener('click', closeWorkoutModal);
    elements.workoutType.addEventListener('change', handleWorkoutTypeChange);
    
    // Race Info Modal
    if (elements.raceInfoForm) {
        elements.raceInfoForm.addEventListener('submit', handleRaceInfoSubmit);
        elements.btnCloseRaceModal.addEventListener('click', closeRaceInfoModal);
        elements.btnCancelRace.addEventListener('click', closeRaceInfoModal);
        
        // GPX File Uploading Listeners
        if (elements.gpxUploadBox) {
            elements.gpxUploadBox.addEventListener('click', () => elements.gpxFileInput.click());
            elements.gpxFileInput.addEventListener('change', handleGpxFileSelect);
            elements.btnRemoveGpx.addEventListener('click', removeUploadedGpx);
            
            // Drag and drop listeners
            elements.gpxUploadBox.addEventListener('dragover', (e) => {
                e.preventDefault();
                elements.gpxUploadBox.classList.add('dragover');
            });
            elements.gpxUploadBox.addEventListener('dragleave', () => {
                elements.gpxUploadBox.classList.remove('dragover');
            });
            elements.gpxUploadBox.addEventListener('drop', (e) => {
                e.preventDefault();
                elements.gpxUploadBox.classList.remove('dragover');
                if (e.dataTransfer.files.length > 0) {
                    const file = e.dataTransfer.files[0];
                    if (file.name.toLowerCase().endsWith('.gpx')) {
                        processGpxFile(file);
                    } else {
                        alert('Please drop a valid .gpx file.');
                    }
                }
            });
        }
    }
    
    // Buddy Modal close
    elements.btnCloseBuddyModal.addEventListener('click', closeBuddyModal);
    elements.btnCancelBuddy.addEventListener('click', closeBuddyModal);
    elements.buddyForm.addEventListener('submit', handleBuddySubmitModal);

    // Shoe Modal Listeners
    if (elements.btnAddShoeModal) {
        elements.btnAddShoeModal.addEventListener('click', () => openShoeModal(null));
    }
    if (elements.btnCloseShoeModal) {
        elements.btnCloseShoeModal.addEventListener('click', closeShoeModal);
    }
    if (elements.btnCancelShoe) {
        elements.btnCancelShoe.addEventListener('click', closeShoeModal);
    }
    if (elements.shoeForm) {
        elements.shoeForm.addEventListener('submit', handleShoeSubmit);
    }
    if (elements.btnDeleteShoe) {
        elements.btnDeleteShoe.addEventListener('click', () => {
            const id = elements.shoeIdInput.value;
            if (id) deleteShoe(id);
        });
    }
    
    // Admin Dashboard Listeners
    if (elements.btnOpenAdminDashboard) {
        elements.btnOpenAdminDashboard.addEventListener('click', handleAdminAccess);
    }
    if (elements.btnCloseAdminModal) {
        elements.btnCloseAdminModal.addEventListener('click', () => {
            elements.adminDashboardModal.classList.remove('active');
        });
    }
    if (elements.btnCloseAdminFooter) {
        elements.btnCloseAdminFooter.addEventListener('click', () => {
            elements.adminDashboardModal.classList.remove('active');
        });
    }
    if (elements.btnRefreshAdmin) {
        elements.btnRefreshAdmin.addEventListener('click', loadAdminUserDirectory);
    }
    if (elements.adminSearchInput) {
        elements.adminSearchInput.addEventListener('input', filterAdminRunnersList);
    }
    if (elements.adminRunnersTableBody) {
        elements.adminRunnersTableBody.addEventListener('click', (e) => {
            const btn = e.target.closest('.btn-delete-runner-action');
            if (btn) {
                const id = btn.getAttribute('data-id');
                if (id) handleDeleteRunner(id);
            }
        });
    }
    
    // Exit Read-Only mode links
    document.querySelectorAll('.link-exit-read-only-action').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            // Clear temporary view data from cache so it doesn't leak into the personal/blank profile
            localStorage.removeItem('maratontrener_workouts');
            localStorage.removeItem('maratontrener_profile');
            localStorage.removeItem('maratontrener_shoes');
            window.location.href = window.location.pathname;
        });
    });
    
    // Claim Profile / Enable Edit Mode links
    document.querySelectorAll('.link-claim-profile-action').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            if (appState.userId) {
                localStorage.setItem('maratontrener_userId', appState.userId);
                saveProfileLocally();
                alert(`Profilen ${appState.userId} er nå satt som din aktive profil (Redigeringsmodus aktivert).`);
                window.location.href = window.location.pathname; // Reload clear URL
            }
        });
    });
    
    // Navigation Drawer open/close & item click listeners
    if (elements.btnCloseDrawer && elements.drawerOverlay) {
        elements.btnCloseDrawer.addEventListener('click', closeDrawer);
        elements.drawerOverlay.addEventListener('click', closeDrawer);
        
        elements.drawerLinkHome.addEventListener('click', () => { closeDrawer(); navTo('home'); });
        elements.drawerLinkBuddies.addEventListener('click', () => { closeDrawer(); navTo('buddies'); });
        elements.drawerLinkLog.addEventListener('click', () => { closeDrawer(); navTo('log'); });
        elements.drawerLinkStats.addEventListener('click', () => { closeDrawer(); navTo('stats'); });
        elements.drawerLinkProfile.addEventListener('click', () => { closeDrawer(); navTo('profile'); });
        
        elements.drawerActionGenerator.addEventListener('click', () => { closeDrawer(); openGeneratorModal(); });
        elements.drawerActionSync.addEventListener('click', () => { closeDrawer(); navTo('profile'); setTimeout(() => { elements.profileSyncIdInput.focus(); }, 150); });
    }

    // Dashboard Cards Click Handlers (Parity with Android)
    if (elements.cardRaceOverview) {
        elements.cardRaceOverview.addEventListener('click', openRaceInfoModal);
    }
    if (elements.cardNextSession) {
        elements.cardNextSession.addEventListener('click', handleNextSessionCardClick);
    }
    if (elements.cardLatestActivity) {
        elements.cardLatestActivity.addEventListener('click', () => {
            const sorted = [...appState.workouts].sort((a,b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
            let latestWorkout = null;
            for (let i = sorted.length - 1; i >= 0; i--) {
                if (sorted[i].isCompleted) {
                    latestWorkout = sorted[i];
                    break;
                }
            }
            if (latestWorkout) {
                editWorkout(latestWorkout.id);
            }
        });
    }
    
    // Avatar upload click and file change listeners
    if (elements.profileAvatarUpload && elements.profileAvatarFile) {
        elements.profileAvatarUpload.addEventListener('click', () => {
            if (appState.readOnly) return;
            elements.profileAvatarFile.click();
        });
        
        elements.profileAvatarFile.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                if (file.size > 500 * 1024) {
                    alert("Bilde er for stort. Vennligst velg et bilde under 500 KB.");
                    return;
                }
                const reader = new FileReader();
                reader.onload = (event) => {
                    const base64Image = event.target.result;
                    appState.avatar = base64Image;
                    updateProfileAvatarUI(base64Image);
                    
                    if (db && appState.firebaseConnected && !appState.readOnly) {
                        db.ref(`profiles/${appState.userId}`).update({
                            avatar: base64Image
                        });
                    }
                    saveProfileLocally();
                };
                reader.readAsDataURL(file);
            }
        });
    }
}

// Toolbar Left Button Click Handler
function handleToolbarLeftClick() {
    if (appState.activeTab === 'home') {
        openDrawer();
    } else {
        navTo('home');
    }
}

function openDrawer() {
    if (elements.navDrawer && elements.drawerOverlay) {
        elements.navDrawer.classList.add('active');
        elements.drawerOverlay.classList.add('active');
        // Update dynamic details
        elements.drawerUserName.innerText = appState.userName;
        elements.drawerUserId.innerText = appState.userId;
        if (appState.avatar) {
            elements.drawerAvatarDisplay.innerHTML = `<img src="${appState.avatar}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
        } else {
            elements.drawerAvatarDisplay.innerHTML = `<i class="fa-solid fa-user"></i>`;
        }
    }
}

function closeDrawer() {
    if (elements.navDrawer && elements.drawerOverlay) {
        elements.navDrawer.classList.remove('active');
        elements.drawerOverlay.classList.remove('active');
    }
}

// Navigation Tabs Switcher
function navTo(tab) {
    if (tab === 'buddies' && appState.readOnly) return;

    // Toggle active tab buttons
    elements.navBtnHome.classList.toggle('active', tab === 'home');
    elements.navBtnBuddies.classList.toggle('active', tab === 'buddies');
    elements.navBtnLog.classList.toggle('active', tab === 'log');
    elements.navBtnStats.classList.toggle('active', tab === 'stats');
    elements.navBtnProfile.classList.toggle('active', tab === 'profile');
    
    // Toggle navigation drawer active state links
    if (elements.drawerLinkHome) {
        elements.drawerLinkHome.classList.toggle('active', tab === 'home');
        elements.drawerLinkBuddies.classList.toggle('active', tab === 'buddies');
        elements.drawerLinkLog.classList.toggle('active', tab === 'log');
        elements.drawerLinkStats.classList.toggle('active', tab === 'stats');
        elements.drawerLinkProfile.classList.toggle('active', tab === 'profile');
    }
    
    // Toggle page views
    elements.pageHome.classList.toggle('active', tab === 'home');
    elements.pageBuddies.classList.toggle('active', tab === 'buddies');
    elements.pageLog.classList.toggle('active', tab === 'log');
    elements.pageStats.classList.toggle('active', tab === 'stats');
    elements.pageProfile.classList.toggle('active', tab === 'profile');
    elements.pageDiet.classList.toggle('active', tab === 'diet');
    
    appState.activeTab = tab;
    elements.appContentScroll.scrollTop = 0; // reset scroll position
    
    // Adjust header title and buttons
    if (tab === 'diet') {
        elements.appToolbarTitle.innerText = "MÅLTIDSPLAN";
        elements.btnToolbarRight.style.visibility = "hidden";
    } else {
        elements.btnToolbarRight.style.visibility = "visible";
        
        if (tab === 'home') {
            elements.appToolbarTitle.innerText = "DASHBOARD";
        } else if (tab === 'buddies') {
            elements.appToolbarTitle.innerText = "BUDDIES";
        } else if (tab === 'log') {
            elements.appToolbarTitle.innerText = "TRAINING PLAN";
        } else if (tab === 'stats') {
            elements.appToolbarTitle.innerText = "STATISTICS";
            renderAnalyticsCharts(); // render/refresh charts
        } else if (tab === 'profile') {
            elements.appToolbarTitle.innerText = "PROFILE";
        }
    }
    
    // Toggle left button: Home shows Hamburger, all other tabs show Back Arrow
    if (tab === 'home') {
        elements.toolbarLeftIcon.className = "fa-solid fa-bars";
    } else {
        elements.toolbarLeftIcon.className = "fa-solid fa-arrow-left";
    }
}

// ----------------------------------------------------
// FIREBASE REAL-TIME INTEGRATION
// ----------------------------------------------------
function checkFirebaseConnection() {
    if (db) {
        const connectedRef = firebase.database().ref(".info/connected");
        connectedRef.on("value", (snap) => {
            if (snap.val() === true) {
                appState.firebaseConnected = true;
                setupFirebaseSync();
            } else {
                appState.firebaseConnected = false;
            }
        });
    }
}

function setupFirebaseSync() {
    if (!db || !appState.userId) return;
    
    // Detach previous listeners first to avoid duplicate bindings on connection changes
    try {
        db.ref(`profiles/${appState.userId}`).off();
        db.ref(`workouts/${appState.userId}`).off();
        db.ref(`profiles/${appState.userId}/followers`).off();
        db.ref(`shoes/${appState.userId}`).off();
    } catch (e) {}
    
    // 1. Sync profile details
    db.ref(`profiles/${appState.userId}`).on('value', (snapshot) => {
        const data = snapshot.val();
        if (data) {
            appState.userName = data.nickname || data.name || 'Runner';
            appState.fullName = data.name || 'Runner';
            appState.email = data.email || '';
            appState.userProfile = {
                distance: formatDistance(data.distance),
                consistency: parseInt(data.consistency || 0),
                workoutsDone: parseInt(data.workoutsDone || 0),
                workoutsTotal: parseInt(data.workoutsTotal || 0),
                currentRace: data.currentRace || 'Oslo Maraton - Marathon',
                eventLocation: data.eventLocation || '',
                gpxRoute: data.gpxRoute || null,
                planStartDate: data.planStartDate || 0
            };
            
            appState.weight = parseFloat(data.weight) || 70;
            appState.age = parseInt(data.age) || 35;
            appState.maxHr = parseInt(data.maxHr) || 185;
            appState.pb10k = data.pb10k || '';
            appState.pbHalf = data.pbHalf || '';
            appState.pbFull = data.pbFull || '';
            
            // Sync favorite meals
            let favs = [];
            if (data.favoriteMeals) {
                if (Array.isArray(data.favoriteMeals)) {
                    favs = data.favoriteMeals.filter(Boolean);
                } else if (typeof data.favoriteMeals === 'object') {
                    favs = Object.values(data.favoriteMeals);
                }
            }
            appState.favoriteMeals = favs;

            // Sync avatar
            if (data.avatar) {
                appState.avatar = data.avatar;
                updateProfileAvatarUI(data.avatar);
            } else {
                appState.avatar = null;
                updateProfileAvatarUI(null);
            }

            // Cache profile locally for offline fast startup
            saveProfileLocally();

            updateProfileUI();
            renderDietSection();
        } else if (!appState.readOnly) {
            // New web user: initialize profile defaults in Firebase cloud
            initializeProfileInFirebase();
        }
    });

    // 2. Sync workouts
    db.ref(`workouts/${appState.userId}`).on('value', (snapshot) => {
        const data = snapshot.val();
        const workoutsList = [];
        if (data) {
            Object.keys(data).forEach(key => {
                workoutsList.push({
                    id: key.replace("workout_", ""),
                    ...data[key]
                });
            });
        }
        
        workoutsList.sort((a, b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
        
        appState.workouts = workoutsList;
        // Cache workouts locally for offline fast startup
        saveWorkoutsLocally();
        
        renderWorkoutsList();
        updateAggregatedStats();
        populatePlanFilters();
        updateShoesUI();
    });

    // 3. Sync followed buddies
    if (!appState.readOnly) {
        db.ref(`profiles/${appState.userId}/followers`).on('value', (snapshot) => {
            const followers = snapshot.val();
            if (followers) {
                appState.buddies = [];
                Object.keys(followers).forEach(buddyId => {
                    db.ref(`profiles/${buddyId}`).on('value', (buddySnap) => {
                        const buddyData = buddySnap.val();
                        if (buddyData) {
                            updateBuddyInList({
                                id: buddyId,
                                name: buddyData.name || 'Runner',
                                distance: formatDistance(buddyData.distance),
                                consistency: parseInt(buddyData.consistency || 0),
                                currentRace: buddyData.currentRace || 'Training',
                                highFives: buddyData.highFives || {},
                                highFiveCount: buddyData.highFives ? Object.keys(buddyData.highFives).length : 0
                            });
                        }
                    });
                });
            }
        });
    }

    // 4. Sync shoes
    db.ref(`shoes/${appState.userId}`).on('value', (snapshot) => {
        const data = snapshot.val();
        const shoesList = [];
        if (data) {
            Object.keys(data).forEach(key => {
                shoesList.push({
                    id: key,
                    ...data[key]
                });
            });
        }
        appState.shoes = shoesList;
        // Cache shoes locally for offline fast startup
        if (!appState.readOnly) {
            localStorage.setItem('maratontrener_shoes', JSON.stringify(shoesList));
        }
        
        updateShoesUI();
        populateWorkoutShoeDropdown();
    });

    // 5. Listen for incoming high-fives
    if (!appState.readOnly) {
        let isWebFirstLoad = true;
        const seenWebHighFives = new Set(JSON.parse(localStorage.getItem('maratontrener_seen_highfives') || '[]'));
        
        db.ref(`profiles/${appState.userId}/highFives`).on('value', (snapshot) => {
            const data = snapshot.val();
            if (data) {
                Object.keys(data).forEach(senderId => {
                    const senderName = data[senderId];
                    if (isWebFirstLoad) {
                        seenWebHighFives.add(senderId);
                    } else {
                        if (!seenWebHighFives.has(senderId)) {
                            seenWebHighFives.add(senderId);
                            localStorage.setItem('maratontrener_seen_highfives', JSON.stringify([...seenWebHighFives]));
                            showToastNotification(`${senderName} sent you a High-Five! 🙌`);
                        }
                    }
                });
            }
            isWebFirstLoad = false;
        });
    }
}

function loadLocalFallbackData() {
    // Load local workouts from localStorage immediately if they exist (for instant UI rendering)
    const stored = localStorage.getItem('maratontrener_workouts');
    if (stored) {
        try {
            appState.workouts = JSON.parse(stored);
        } catch (e) {
            console.error("Error parsing stored workouts:", e);
        }
    } else if (!db) {
        // If there's no cache and no Firebase connection, generate sample offline workouts
        appState.workouts = generateOfflineSamplePlan();
        saveWorkoutsLocally();
    }

    if (!db && !appState.readOnly) {
        // Load offline fallback buddies
        appState.buddies = [
            { id: "BUDDY01", name: "Kristine", distance: "142.0", consistency: 92, currentRace: "Oslo Maraton - Marathon" },
            { id: "BUDDY02", name: "Marcus", distance: "98.5", consistency: 78, currentRace: "Drammen Halvmaraton" }
        ];
    }

    const storedShoes = localStorage.getItem('maratontrener_shoes');
    if (storedShoes) {
        try {
            appState.shoes = JSON.parse(storedShoes);
        } catch (e) {
            console.error("Error parsing stored shoes:", e);
        }
    }

    // Trigger initial UI rendering from local state (fast load)
    updateProfileUI();
    updateShoesUI();
    populateWorkoutShoeDropdown();
    renderWorkoutsList();
    if (!db && !appState.readOnly) renderBuddiesUI();
    populatePlanFilters();
    renderDietSection();
}

function generateOfflineSamplePlan() {
    const workouts = [];
    const baseDate = new Date();
    // Generate 12-week schedule
    for (let i = 0; i < 12; i++) {
        for (let j = 0; j < 3; j++) {
            const w = {
                id: `off_${i}_${j}`,
                weekNumber: i + 1,
                workoutType: j === 0 ? "INTERVALS" : (j === 1 ? "STEADY RUN" : "LONG RUN"),
                planName: "Oslo Maraton Plan",
                distance: j === 0 ? 8.0 : (j === 1 ? 10.0 : (12.0 + i * 1.5)),
                pace: j === 0 ? "4:45" : (j === 1 ? "5:15" : "5:45"),
                totalDuration: j === 0 ? 38 : (j === 1 ? 52 : 120),
                avgHeartRate: j === 0 ? 155 : 138,
                description: j === 0 ? "5x800m intervals" : "Conversational aerobic volume",
                notes: "",
                isCompleted: i < 3
            };
            const d = new Date(baseDate);
            d.setDate(d.getDate() - (3 - i) * 7 + j * 2);
            w.scheduledDate = d.toISOString().split('T')[0];
            workouts.push(w);
        }
    }
    return workouts;
}

// ----------------------------------------------------
// UI PRESENTATION & STATE UPDATE
// ----------------------------------------------------
function updateProfileUI() {
    elements.profileTitleName.innerText = appState.fullName || appState.userName || 'Runner';
    elements.profileTitleEmail.innerText = appState.email || (appState.userId ? `${appState.userId.toLowerCase()}@maratontrener.no` : 'athlete@maratontrener.no');
    
    // Parse currentRace to separate Name, Category, and Date
    let raceName = "Oslo Maraton";
    let raceCategory = "Marathon";
    let raceDateStr = "";
    
    const currentRace = appState.userProfile.currentRace || "";
    if (currentRace.includes(" - ")) {
        const parts = currentRace.split(" - ");
        raceName = parts[0];
        const categoryAndDate = parts[1];
        if (categoryAndDate.includes(": ")) {
            const catParts = categoryAndDate.split(": ");
            raceCategory = catParts[0];
            raceDateStr = catParts[1];
        } else {
            raceCategory = categoryAndDate;
        }
    }
    
    elements.homeRaceName.innerText = raceName;
    elements.homeRaceCategory.innerText = raceCategory;
    if (elements.homeRaceLocation) {
        elements.homeRaceLocation.innerText = appState.userProfile.eventLocation || 'Norway';
    }
    
    // Profile fields
    elements.profileInputName.value = appState.fullName || '';
    elements.profileInputNickname.value = appState.userName || '';
    elements.profileInputEmail.value = appState.email || '';
    elements.profileInputAge.value = appState.readOnly ? '' : (appState.age || '');
    elements.profileInputWeight.value = appState.readOnly ? '' : (appState.weight || '');
    elements.profileInputMaxhr.value = appState.readOnly ? '' : (appState.maxHr || '');
    elements.profileInputPb10k.value = appState.pb10k || '';
    elements.profileInputPbhalf.value = appState.pbHalf || '';
    elements.profileInputPbfull.value = appState.pbFull || '';
    
    elements.textYourId.innerText = appState.userId;
    elements.profileSyncIdInput.value = appState.userId;
    
    // Update ntfy channel guides
    document.querySelectorAll('.lbl-my-id-lowercase').forEach(el => {
        el.innerText = appState.userId.toLowerCase();
    });

    updateHRZonesDisplay();
    updateNextAndLatestUI();
    
    if (elements.adminSyncSection) {
        elements.adminSyncSection.style.display = (appState.userId === 'CH020721') ? 'block' : 'none';
    }
    if (elements.stravaSyncSection) {
        elements.stravaSyncSection.style.display = appState.readOnly ? 'none' : 'block';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}

function calculateShoeMileage(shoeId) {
    const shoe = appState.shoes.find(s => s.id === shoeId);
    if (!shoe) return 0;
    
    let mileage = parseFloat(shoe.initialMileage) || 0;
    
    // Add distance of all completed workouts using this shoe
    appState.workouts.forEach(w => {
        if (w.shoeId === shoeId && w.isCompleted) {
            mileage += parseFloat(w.distance) || 0;
        }
    });
    
    return mileage;
}

function updateShoesUI() {
    if (!elements.shoesListContainer) return;
    
    // Clear container
    elements.shoesListContainer.innerHTML = '';
    
    if (appState.shoes.length === 0) {
        elements.shoesListContainer.innerHTML = `
            <div style="text-align: center; color: var(--text-secondary); padding: 20px 0; font-size: 0.9rem;">
                No running shoes added yet. Click "+ ADD" to track your shoe mileage.
            </div>
        `;
        return;
    }
    
    // Sort active first, then by name
    const sortedShoes = [...appState.shoes].sort((a, b) => {
        if (a.retired !== b.retired) {
            return a.retired ? 1 : -1; // Active first
        }
        return (a.name || '').localeCompare(b.name || '');
    });
    
    sortedShoes.forEach(shoe => {
        const totalMileage = calculateShoeMileage(shoe.id);
        const limit = parseFloat(shoe.mileageLimit) || 800;
        const pct = limit > 0 ? Math.min(totalMileage / limit, 1.0) : 0;
        const pctPercent = Math.round(pct * 100);
        
        let colorClass = 'green'; // Green
        if (pct >= 1.0) {
            colorClass = 'red'; // Red
        } else if (pct >= 0.70) {
            colorClass = 'orange'; // Orange
        }
        
        const badgeText = shoe.retired ? 'RETIRED' : 'ACTIVE';
        const badgeClass = shoe.retired ? 'retired' : 'active';
        
        const card = document.createElement('div');
        card.className = 'shoe-item';
        card.innerHTML = `
            <div class="shoe-header">
                <span class="shoe-name">${escapeHtml(shoe.name)}</span>
                <span class="shoe-status-badge ${badgeClass}">${badgeText}</span>
            </div>
            <div class="shoe-mileage-row">
                <span>${totalMileage.toFixed(1)} km run</span>
                <span>${limit.toFixed(0)} km limit</span>
            </div>
            <div class="shoe-progress-container">
                <div class="shoe-progress-fill ${colorClass}" style="width: ${pctPercent}%;"></div>
            </div>
            <div class="shoe-actions">
                <button type="button" class="shoe-btn-edit" data-id="${shoe.id}">EDIT</button>
            </div>
        `;
        
        // Attach click listener for editing
        card.querySelector('.shoe-btn-edit').addEventListener('click', () => {
            openShoeModal(shoe);
        });
        
        elements.shoesListContainer.appendChild(card);
    });
}

function populateWorkoutShoeDropdown() {
    if (!elements.workoutShoe) return;
    
    // Keep the first default option
    const currentValue = elements.workoutShoe.value;
    elements.workoutShoe.innerHTML = '<option value="">-- No Shoe Selected --</option>';
    
    // Sort active first, then retired, then alphabetically
    const sortedShoes = [...appState.shoes].sort((a, b) => {
        if (a.retired !== b.retired) {
            return a.retired ? 1 : -1;
        }
        return (a.name || '').localeCompare(b.name || '');
    });
    
    sortedShoes.forEach(shoe => {
        const opt = document.createElement('option');
        opt.value = shoe.id;
        let suffix = shoe.retired ? ' (Retired)' : '';
        const totalMileage = calculateShoeMileage(shoe.id);
        opt.textContent = `${shoe.name}${suffix} (${totalMileage.toFixed(0)} km)`;
        elements.workoutShoe.appendChild(opt);
    });
    
    // Restore value if still valid
    elements.workoutShoe.value = currentValue;
}

function openShoeModal(shoe = null) {
    if (appState.readOnly) return;
    elements.shoeModal.classList.add('active');
    if (shoe) {
        elements.shoeModalTitle.innerText = 'Edit Running Shoe';
        elements.shoeIdInput.value = shoe.id;
        elements.shoeNameInput.value = shoe.name || '';
        elements.shoeInitialMileageInput.value = shoe.initialMileage || 0;
        elements.shoeLimitInput.value = shoe.mileageLimit || 800;
        elements.shoeRetiredInput.checked = shoe.retired || false;
        elements.btnDeleteShoe.style.display = 'inline-block';
    } else {
        elements.shoeModalTitle.innerText = 'Add Running Shoe';
        elements.shoeForm.reset();
        elements.shoeIdInput.value = '';
        elements.shoeInitialMileageInput.value = 0;
        elements.shoeLimitInput.value = 800;
        elements.shoeRetiredInput.checked = false;
        elements.btnDeleteShoe.style.display = 'none';
    }
}

function closeShoeModal() {
    elements.shoeModal.classList.remove('active');
}

function handleShoeSubmit(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    
    const id = elements.shoeIdInput.value;
    const isNew = !id;
    const shoeId = id || `SHOE_${Date.now()}`;
    
    const data = {
        name: elements.shoeNameInput.value.trim(),
        initialMileage: parseFloat(elements.shoeInitialMileageInput.value) || 0,
        mileageLimit: parseFloat(elements.shoeLimitInput.value) || 800,
        retired: elements.shoeRetiredInput.checked
    };
    
    if (db && appState.firebaseConnected) {
        db.ref(`shoes/${appState.userId}/${shoeId}`).update(data)
            .then(() => {
                closeShoeModal();
            });
    } else {
        if (isNew) {
            appState.shoes.push({ id: shoeId, ...data });
        } else {
            const idx = appState.shoes.findIndex(s => s.id === shoeId);
            if (idx !== -1) appState.shoes[idx] = { id: shoeId, ...data };
        }
        localStorage.setItem('maratontrener_shoes', JSON.stringify(appState.shoes));
        updateShoesUI();
        populateWorkoutShoeDropdown();
        closeShoeModal();
    }
}

function deleteShoe(shoeId) {
    if (appState.readOnly) return;
    if (confirm("Delete this shoe? This cannot be undone.")) {
        if (db && appState.firebaseConnected) {
            db.ref(`shoes/${appState.userId}/${shoeId}`).remove()
                .then(() => {
                    closeShoeModal();
                });
        } else {
            appState.shoes = appState.shoes.filter(s => s.id !== shoeId);
            localStorage.setItem('maratontrener_shoes', JSON.stringify(appState.shoes));
            updateShoesUI();
            populateWorkoutShoeDropdown();
            closeShoeModal();
        }
    }
}

function updateHRZonesDisplay() {
    const hrHeader = document.querySelector('#profile-hr-zones-card .section-label-inner');
    if (appState.readOnly) {
        elements.zone1Text.innerText = "Zone 1: Recovery (-- - --)";
        elements.zone2Text.innerText = "Zone 2: Aerobic (-- - --)";
        elements.zone3Text.innerText = "Zone 3: Tempo (-- - --)";
        elements.zone4Text.innerText = "Zone 4: Threshold (-- - --)";
        elements.zone5Text.innerText = "Zone 5: Anaerobic (-- - --)";
        if (hrHeader) hrHeader.innerText = "HEART RATE ZONES";
        return;
    }
    let max = appState.maxHr;
    let isEstimated = false;
    if (!max && appState.age) {
        max = Math.round(208 - 0.7 * appState.age);
        isEstimated = true;
    }
    if (hrHeader) {
        hrHeader.innerText = isEstimated ? "HEART RATE ZONES (AGE ESTIMATED)" : "HEART RATE ZONES";
    }
    if (max > 0) {
        elements.zone1Text.innerText = `Zone 1: Recovery (${Math.round(max * 0.5)} - ${Math.round(max * 0.6)} BPM)`;
        elements.zone2Text.innerText = `Zone 2: Aerobic (${Math.round(max * 0.6)} - ${Math.round(max * 0.7)} BPM)`;
        elements.zone3Text.innerText = `Zone 3: Tempo (${Math.round(max * 0.7)} - ${Math.round(max * 0.8)} BPM)`;
        elements.zone4Text.innerText = `Zone 4: Threshold (${Math.round(max * 0.8)} - ${Math.round(max * 0.9)} BPM)`;
        elements.zone5Text.innerText = `Zone 5: Anaerobic (${Math.round(max * 0.9)} - ${max} BPM)`;
    }
}

function updateAggregatedStats() {
    if (appState.workouts.length === 0) return;
    
    // Filter workouts belonging to the current active plan
    const activePlanName = appState.userProfile.currentRace ? appState.userProfile.currentRace.split(' - ')[0].trim().toLowerCase() : '';
    const planWorkouts = appState.workouts.filter(w => {
        if (!activePlanName) return true;
        return w.planName && w.planName.trim().toLowerCase() === activePlanName;
    });
    const targetWorkouts = planWorkouts.length > 0 ? planWorkouts : appState.workouts;

    // Calculate lifetime distance for the Century Club badge
    let lifetimeDistance = 0;
    appState.workouts.forEach(w => {
        if (w.isCompleted) {
            const type = (w.workoutType || "").toUpperCase().trim();
            const isInterval = type.includes("INTERVAL");
            const isStrength = type.includes("STRENGTH") || type.includes("CORE");
            if (!isInterval && !isStrength) {
                lifetimeDistance += parseFloat(w.distance || 0);
            }
        }
    });

    let completedTotal = 0;
    let completedToDate = 0;
    let missedToDate = 0;
    let shouldBeCompletedByNow = 0;
    let totalDistance = 0;
    let runningPaceSum = 0;
    let runsWithPace = 0;
    let intervalsDone = 0;
    let steadyDone = 0;
    let longDone = 0;
    let tempoDone = 0;
    let strengthDone = 0;
    let walkDone = 0;
    let intervalsTotal = 0;
    let runsTotal = 0;
    let strengthTotal = 0;
    let walkTotal = 0;
    let avgHRSum = 0;
    let runsWithHR = 0;
    let maxDistance = 0;
    let longestRunWorkoutId = null;
    
    const today = new Date();
    const todayStr = getLocalDateString(today);
    
    const completedDays = new Set();
    targetWorkouts.forEach(w => {
        if (w.isCompleted && w.scheduledDate) {
            completedDays.add(w.scheduledDate);
        }
    });

    targetWorkouts.forEach(w => {
        const type = (w.workoutType || "").toUpperCase().trim();
        const isInterval = type.includes("INTERVAL");
        const isStrength = type.includes("STRENGTH") || type.includes("CORE");
        const isWalk = type.includes("WALK");
        const isRest = type.includes("REST");
        const isRun = !isInterval && !isStrength && !isWalk && !isRest;
        
        if (isInterval) intervalsTotal++;
        else if (isStrength) strengthTotal++;
        else if (isWalk) walkTotal++;
        else if (isRun) runsTotal++;
        
        const isPast = w.scheduledDate && (w.scheduledDate < todayStr);
        const isToday = w.scheduledDate && (w.scheduledDate === todayStr);
        
        if (w.isCompleted) {
            completedTotal++;
            if (!isInterval && !isStrength) {
                const dist = parseFloat(w.distance || 0);
                totalDistance += dist;
                if (dist > maxDistance) {
                    maxDistance = dist;
                    longestRunWorkoutId = w.id;
                }
                const hr = parseInt(w.avgHeartRate || 0);
                if (hr > 0) {
                    avgHRSum += hr;
                    runsWithHR++;
                }
            }
            if (isInterval) intervalsDone++;
            else if (type.includes("STEADY")) steadyDone++;
            else if (type.includes("LONG")) longDone++;
            else if (type.includes("TEMPO")) tempoDone++;
            else if (isWalk) walkDone++;
            else if (isStrength) strengthDone++;
            
            const paceDec = parsePaceToDecimal(w.pace);
            if (isRun && paceDec > 0) {
                runsWithPace++;
                runningPaceSum += paceDec;
            }
            
            if (isPast || isToday) {
                completedToDate++;
                shouldBeCompletedByNow++;
            }
        } else {
            if (isPast) {
                if (!completedDays.has(w.scheduledDate)) {
                    missedToDate++;
                    shouldBeCompletedByNow++;
                }
            }
        }
    });
    
    const percentToDate = shouldBeCompletedByNow === 0 ? 100 : Math.round((completedToDate / shouldBeCompletedByNow) * 100);
    const avgPace = runsWithPace === 0 ? 0 : (runningPaceSum / runsWithPace);
    const avgHR = runsWithHR === 0 ? 0 : Math.round(avgHRSum / runsWithHR);
    const totalActivitiesCount = appState.workouts.length;
    
    appState.userProfile.distance = formatDistance(totalDistance);
    appState.userProfile.workoutsDone = completedTotal;
    appState.userProfile.workoutsTotal = totalActivitiesCount;
    appState.userProfile.consistency = percentToDate;
    
    const percent = totalActivitiesCount > 0 ? Math.round((completedTotal / totalActivitiesCount) * 100) : 0;
    elements.homeProgressBar.style.width = `${percent}%`;
    elements.homeProgressPercent.innerText = `${percent}% COMPLETED`;
    
    if (elements.statsPlannedDist) {
        elements.statsPlannedDist.innerText = `${completedTotal} ferdig, ${totalActivitiesCount - completedTotal} gjenstår`;
    }
    if (elements.statsConsistency) {
        elements.statsConsistency.innerText = `${percentToDate}% (${completedToDate}/${shouldBeCompletedByNow})`;
        elements.statsConsistency.className = `metric-val-replicated ${percentToDate >= 90 ? 'text-success' : 'type-red'}`;
    }
    if (elements.statsTotalRuns) {
        const runsDone = completedTotal - strengthDone - intervalsDone - walkDone;
        elements.statsTotalRuns.innerText = `${runsDone} ferdig, ${runsTotal - runsDone} gjenstår`;
    }
    if (elements.statsLongestRun) {
        elements.statsLongestRun.innerText = `${maxDistance.toFixed(1).replace('.', ',')} km`;
        elements.cardStatsLongestRun.onclick = () => {
            if (longestRunWorkoutId) {
                editWorkout(longestRunWorkoutId);
            }
        };
    }
    if (elements.statsDistance) {
        elements.statsDistance.innerText = `${totalDistance.toFixed(1).replace('.', ',')} km`;
    }
    if (elements.statsAveragePace) {
        elements.statsAveragePace.innerText = avgPace > 0 ? `${formatPace(avgPace)} min/km` : '0:00 min/km';
    }
    if (elements.statsIntervalStat) {
        elements.statsIntervalStat.innerText = `${intervalsDone} ferdig, ${intervalsTotal - intervalsDone} gjenstår`;
    }
    if (elements.statsStrengthDone) {
        elements.statsStrengthDone.innerText = `${strengthDone} ferdig, ${strengthTotal - strengthDone} gjenstår`;
    }
    if (elements.statsMissed) {
        elements.statsMissed.innerText = `${missedToDate} økter`;
        elements.statsMissed.className = `metric-val-replicated ${missedToDate > 0 ? 'type-red' : ''}`;
        elements.cardStatsMissed.onclick = () => {
            navTo('log');
        };
    }
    if (elements.statsAvgHrRuns) {
        elements.statsAvgHrRuns.innerText = avgHR > 0 ? `${avgHR} (${calculateHRZone(avgHR)})` : '--';
    }
    
    updateCountdown();
    
    if (db && appState.firebaseConnected && !appState.readOnly) {
        db.ref(`profiles/${appState.userId}`).update({
            distance: totalDistance,
            consistency: percentToDate,
            workoutsDone: completedTotal,
            workoutsTotal: totalActivitiesCount
        });
    }

    // Update achievements UI in profile page
    const badgeConsistencyIcon = document.getElementById('badge-consistency-icon');
    const badgeConsistencyTitle = document.getElementById('badge-consistency-title');
    const badgeConsistencyDesc = document.getElementById('badge-consistency-desc');
    const badgeCenturyIcon = document.getElementById('badge-century-icon');
    const badgeCenturyDesc = document.getElementById('badge-century-desc');

    if (badgeConsistencyIcon && badgeConsistencyTitle && badgeConsistencyDesc) {
        badgeConsistencyDesc.innerText = `${percentToDate}% consistency`;
        badgeConsistencyIcon.classList.remove('badge-slob', 'badge-that-all', 'badge-keep-going', 'badge-king');
        
        if (percentToDate < 50) {
            badgeConsistencyTitle.innerText = "Slob!";
            badgeConsistencyIcon.classList.add('badge-slob');
        } else if (percentToDate < 75) {
            badgeConsistencyTitle.innerText = "That all?";
            badgeConsistencyIcon.classList.add('badge-that-all');
        } else if (percentToDate < 90) {
            badgeConsistencyTitle.innerText = "Keep going!";
            badgeConsistencyIcon.classList.add('badge-keep-going');
        } else {
            badgeConsistencyTitle.innerText = "Consistency King";
            badgeConsistencyIcon.classList.add('badge-king');
        }
    }

    if (badgeCenturyIcon && badgeCenturyDesc) {
        badgeCenturyDesc.innerText = `${lifetimeDistance.toFixed(1).replace('.', ',')} / 100 km`;
        badgeCenturyIcon.classList.remove('badge-century-active', 'badge-century-locked');
        if (lifetimeDistance >= 100.0) {
            badgeCenturyIcon.classList.add('badge-century-active');
        } else {
            badgeCenturyIcon.classList.add('badge-century-locked');
        }
    }
}

function calculateHRZone(avgHR) {
    let max = appState.maxHr;
    if (!max && appState.age) {
        max = Math.round(208 - 0.7 * appState.age);
    }
    if (!max) return "Sone: ?";
    const pct = avgHR / max;
    if (pct >= 0.9) return "Sone 5";
    if (pct >= 0.8) return "Sone 4";
    if (pct >= 0.7) return "Sone 3";
    if (pct >= 0.6) return "Sone 2";
    return "Sone 1";
}

function updateCountdown() {
    let raceName = "Oslo Maraton";
    let raceCategory = "Marathon";
    let raceDateStr = "";
    
    const currentRace = appState.userProfile.currentRace || "";
    if (currentRace.includes(" - ")) {
        const parts = currentRace.split(" - ");
        raceName = parts[0];
        const categoryAndDate = parts[1];
        if (categoryAndDate.includes(": ")) {
            const catParts = categoryAndDate.split(": ");
            raceCategory = catParts[0];
            raceDateStr = catParts[1];
        } else {
            raceCategory = categoryAndDate;
        }
    }
    
    let countdownDays = 0;
    let raceDateObj = null;
    
    if (raceDateStr) {
        raceDateObj = new Date(raceDateStr);
    }
    
    if (!raceDateObj || isNaN(raceDateObj.getTime())) {
        if (appState.workouts.length > 0) {
            const sorted = [...appState.workouts].sort((a,b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
            const lastW = sorted[sorted.length - 1];
            if (lastW && lastW.scheduledDate) {
                raceDateObj = new Date(lastW.scheduledDate);
            }
        }
    }
    
    if (raceDateObj && !isNaN(raceDateObj.getTime())) {
        const today = new Date();
        today.setHours(0,0,0,0);
        const diffTime = raceDateObj.getTime() - today.getTime();
        countdownDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        if (countdownDays < 0) countdownDays = 0;
        
        if (elements.homeRaceDate) {
            elements.homeRaceDate.innerText = raceDateObj.toLocaleDateString('no-NO', { day: 'numeric', month: 'short', year: 'numeric' });
        }
    } else {
        if (elements.homeRaceDate) {
            elements.homeRaceDate.innerText = 'Sett løpsdato';
        }
    }
    
    if (elements.textCountdown) {
        elements.textCountdown.innerText = `${countdownDays} DAGER`;
    }

    // Call weather forecast update
    const eventLocation = appState.userProfile.eventLocation || "";
    updateWeatherForecast(eventLocation, raceDateObj, countdownDays);
}

// ----------------------------------------------------
// DYNAMIC WEATHER FORECAST UTILITIES
// ----------------------------------------------------
async function updateWeatherForecast(location, raceDateObj, countdownDays) {
    const container = document.getElementById('home-race-weather');
    if (!container) return;

    if (!location || !raceDateObj || isNaN(raceDateObj.getTime()) || countdownDays < 0) {
        container.style.display = 'none';
        return;
    }

    const cleanLocation = location.trim();
    if (cleanLocation.toLowerCase() === 'location' || cleanLocation.toLowerCase() === 'place, country' || cleanLocation.toLowerCase() === 'norway') {
        container.style.display = 'none';
        return;
    }

    // Determine mode: "Currently" (diff > 3 days) or "Race Day" (diff >= 0 and diff <= 3 days)
    const isRaceDayForecast = (countdownDays <= 3);
    const labelText = isRaceDayForecast ? "Race Day" : "Currently";

    // Set cache key
    const cacheKey = `${cleanLocation.toLowerCase()}_${isRaceDayForecast ? 'raceday' : 'currently'}`;
    const cached = weatherCache.forecast[cacheKey];
    const now = Date.now();
    if (cached && (now - cached.timestamp < 3600000)) { // 1 hour cache
        renderWeatherWidget(cached.temp, cached.iconClass, labelText);
        return;
    }

    try {
        // Step 1: Geocode location
        let coords = weatherCache.geocode[cleanLocation.toLowerCase()];
        if (!coords) {
            const geocodeUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(cleanLocation)}&count=1&language=en&format=json`;
            const geoRes = await fetch(geocodeUrl);
            const geoData = await geoRes.json();
            if (geoData.results && geoData.results.length > 0) {
                coords = {
                    lat: geoData.results[0].latitude,
                    lon: geoData.results[0].longitude
                };
                weatherCache.geocode[cleanLocation.toLowerCase()] = coords;
            } else {
                // Location not found
                container.style.display = 'none';
                return;
            }
        }

        // Step 2: Fetch weather
        let weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${coords.lat}&longitude=${coords.lon}&timezone=auto`;
        if (isRaceDayForecast) {
            weatherUrl += `&daily=weather_code,temperature_2m_max,temperature_2m_min`;
        } else {
            weatherUrl += `&current=temperature_2m,weather_code`;
        }

        const weatherRes = await fetch(weatherUrl);
        const weatherData = await weatherRes.json();

        let tempStr = '';
        let iconClass = 'fa-cloud-sun';

        if (isRaceDayForecast) {
            if (weatherData.daily && weatherData.daily.time) {
                // Find index matching race date
                const raceDateISO = new Date(raceDateObj.getTime() - raceDateObj.getTimezoneOffset() * 60000).toISOString().split('T')[0];
                const idx = weatherData.daily.time.indexOf(raceDateISO);
                if (idx !== -1) {
                    const minTemp = Math.round(weatherData.daily.temperature_2m_min[idx]);
                    const maxTemp = Math.round(weatherData.daily.temperature_2m_max[idx]);
                    const code = weatherData.daily.weather_code[idx];
                    tempStr = `${minTemp}° / ${maxTemp}°C`;
                    iconClass = getWeatherIconClass(code);
                } else {
                    // Fallback to current if race date is outside the 7-day forecast array but <= 3 days
                    const curUrl = `https://api.open-meteo.com/v1/forecast?latitude=${coords.lat}&longitude=${coords.lon}&current=temperature_2m,weather_code&timezone=auto`;
                    const curRes = await fetch(curUrl);
                    const curData = await curRes.json();
                    if (curData.current) {
                        const temp = Math.round(curData.current.temperature_2m);
                        const code = curData.current.weather_code;
                        tempStr = `${temp}°C`;
                        iconClass = getWeatherIconClass(code);
                    } else {
                        container.style.display = 'none';
                        return;
                    }
                }
            } else {
                container.style.display = 'none';
                return;
            }
        } else {
            if (weatherData.current) {
                const temp = Math.round(weatherData.current.temperature_2m);
                const code = weatherData.current.weather_code;
                tempStr = `${temp}°C`;
                iconClass = getWeatherIconClass(code);
            } else {
                container.style.display = 'none';
                return;
            }
        }

        // Cache the result
        weatherCache.forecast[cacheKey] = {
            temp: tempStr,
            iconClass: iconClass,
            timestamp: now
        };

        renderWeatherWidget(tempStr, iconClass, labelText);

    } catch (e) {
        console.error("Error fetching weather forecast:", e);
        container.style.display = 'none';
    }
}

function renderWeatherWidget(temp, iconClass, label) {
    const container = document.getElementById('home-race-weather');
    const labelEl = document.getElementById('weather-label');
    const iconEl = document.getElementById('weather-icon');
    const tempEl = document.getElementById('weather-temp');

    if (container && labelEl && iconEl && tempEl) {
        labelEl.innerText = label;
        iconEl.className = `fa-solid ${iconClass} weather-icon`;
        tempEl.innerText = temp;
        container.style.display = 'flex';
    }
}

function getWeatherIconClass(wmoCode) {
    // WMO Weather interpretation codes (WW)
    if (wmoCode === 0 || wmoCode === 1) {
        return "fa-sun"; // Clear sky / Mainly clear
    } else if (wmoCode === 2) {
        return "fa-cloud-sun"; // Partly cloudy
    } else if (wmoCode === 3) {
        return "fa-cloud"; // Overcast
    } else if (wmoCode === 45 || wmoCode === 48) {
        return "fa-smog"; // Fog
    } else if ([51, 53, 55, 56, 57].includes(wmoCode)) {
        return "fa-cloud-rain"; // Drizzle
    } else if ([61, 63, 65, 66, 67, 80, 81, 82].includes(wmoCode)) {
        return "fa-cloud-showers-heavy"; // Rain
    } else if ([71, 73, 75, 77, 85, 86].includes(wmoCode)) {
        return "fa-snowflake"; // Snow
    } else if ([95, 96, 99].includes(wmoCode)) {
        return "fa-cloud-bolt"; // Thunderstorm
    }
    return "fa-cloud-sun"; // Default
}

function updateNextAndLatestUI() {
    // 1. Next Workout Card selection matching Android
    const today = new Date();
    const todayStr = getLocalDateString(today);
    
    const todayStart = new Date(today);
    todayStart.setHours(0,0,0,0);
    const todayStartMs = todayStart.getTime();
    
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const tomorrowStr = getLocalDateString(tomorrow);
    
    let nextWorkout = null;
    let labelText = "NEXT SESSION";
    
    // Sort logically by date
    const sorted = [...appState.workouts].sort((a,b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
    
    // Find first uncompleted workout before today (MISSED)
    for (let w of sorted) {
        if (!w.isCompleted && w.scheduledDate) {
            if (w.scheduledDate < todayStr) {
                nextWorkout = w;
                labelText = "MISSED SESSION";
                break;
            }
        }
    }
    
    // Find uncompleted workout scheduled today
    if (!nextWorkout) {
        for (let w of sorted) {
            if (!w.isCompleted && w.scheduledDate) {
                if (w.scheduledDate === todayStr) {
                    nextWorkout = w;
                    labelText = "TODAY'S SESSION";
                    break;
                }
            }
        }
    }
    
    // Find tomorrow or future
    if (!nextWorkout) {
        for (let w of sorted) {
            if (!w.isCompleted && w.scheduledDate) {
                if (w.scheduledDate > todayStr) {
                    nextWorkout = w;
                    labelText = w.scheduledDate === tomorrowStr ? "TOMORROW'S SESSION" : "NEXT SESSION";
                    break;
                }
            }
        }
    }
    
    if (nextWorkout) {
        elements.labelNextSession.innerText = labelText;
        elements.nextType.innerText = getWorkoutTypeWithIcon(nextWorkout.workoutType);
        
        // Color text by status
        if (labelText.includes("MISSED")) {
            elements.nextType.className = "card-title-lime type-red";
        } else {
            elements.nextType.className = "card-title-lime";
        }
        
        if (nextWorkout.workoutType.toUpperCase() === 'INTERVALS' && nextWorkout.intervalCount) {
            elements.nextDetails.innerText = `${nextWorkout.intervalCount}x ${nextWorkout.intervalValue} @ ${nextWorkout.intervalPace} - ${nextWorkout.description}`;
        } else {
            elements.nextDetails.innerText = `${formatDistance(nextWorkout.distance)} km - ${nextWorkout.description}`;
        }
        
        const dateObj = new Date(nextWorkout.scheduledDate);
        elements.nextDate.innerText = dateObj.toLocaleDateString('no-NO', { weekday: 'long', day: 'numeric', month: 'short' });
    } else {
        elements.labelNextSession.innerText = "GET STARTED";
        elements.nextType.innerText = "No workouts planned";
        elements.nextDetails.innerText = "Generate a plan first";
        elements.nextDate.innerText = "";
    }
    
    // 2. Latest Activity Card selection
    let latestWorkout = null;
    // Find last completed workout in past
    for (let i = sorted.length - 1; i >= 0; i--) {
        if (sorted[i].isCompleted) {
            latestWorkout = sorted[i];
            break;
        }
    }
    
    if (latestWorkout) {
        elements.labelLatestActivity.style.display = "block";
        elements.cardLatestActivity.style.display = "block";
        
        elements.latestUserName.innerText = appState.userName;
        
        // Calculate days ago text
        const wDateMs = new Date(latestWorkout.scheduledDate).getTime();
        const diffMs = todayStartMs - wDateMs;
        const diffDays = Math.round(diffMs / (1000*60*60*24));
        
        if (diffDays === 0) elements.latestTimeAgo.innerText = "Today";
        else if (diffDays === 1) elements.latestTimeAgo.innerText = "Yesterday";
        else elements.latestTimeAgo.innerText = `${diffDays}d ago`;
        
        elements.latestActivityTitle.innerText = `Latest activity: ${getWorkoutTypeWithIcon(latestWorkout.workoutType)}`;
        
        if (latestWorkout.workoutType.toUpperCase() === 'INTERVALS') {
            elements.latestValDist.innerText = latestWorkout.intervalCount || 0;
            elements.latestLblDist.innerText = "Sets";
            elements.latestValDur.innerText = latestWorkout.intervalValue || "800m";
            elements.latestLblDur.innerText = "Work";
            elements.latestValPace.innerText = latestWorkout.intervalPace || "--:--";
            elements.latestLblPace.innerText = "Pace";
        } else {
            elements.latestValDist.innerText = formatDistance(latestWorkout.distance);
            elements.latestLblDist.innerText = "km";
            
            const dur = latestWorkout.totalDuration || 0;
            const h = Math.floor(dur / 60);
            const m = dur % 60;
            elements.latestValDur.innerText = h > 0 ? `${h}h ${m}m` : `${m}m`;
            elements.latestLblDur.innerText = "Time";
            
            elements.latestValPace.innerText = formatPace(latestWorkout.pace);
            elements.latestLblPace.innerText = "/km";
        }
        elements.latestValHr.innerText = latestWorkout.avgHeartRate > 0 ? latestWorkout.avgHeartRate : '--';
    } else {
        elements.labelLatestActivity.style.display = "none";
        elements.cardLatestActivity.style.display = "none";
    }
}

// ----------------------------------------------------
// WORKOUT LOG RENDERING
// ----------------------------------------------------
function renderWorkoutsList() {
    const listContainer = elements.logWorkoutsList;
    listContainer.innerHTML = '';
    
    const filter = elements.planFilterDropdown.value;
    const filtered = appState.workouts.filter(w => {
        if (filter === 'all') return true;
        return w.planName === filter;
    });
    
    if (filtered.length === 0) {
        listContainer.innerHTML = `
            <div class="empty-state-recipes">
                <i class="fa-solid fa-circle-exclamation"></i>
                <p>No workouts recorded. Click '+' to log manually!</p>
            </div>
        `;
        return;
    }
    
    // Group workouts by calendar week
    const grouped = {};
    filtered.forEach(w => {
        const d = new Date(w.scheduledDate);
        const { week } = getCalendarWeekAndYear(d);
        if (!grouped[week]) grouped[week] = [];
        grouped[week].push(w);
    });
    
    // Render grouped weeks chronologically
    const weeks = Object.keys(grouped).map(Number).sort((a, b) => a - b);
    
    weeks.forEach(week => {
        const weekDiv = document.createElement('div');
        weekDiv.className = 'log-week-section';
        weekDiv.innerHTML = `<div class="log-week-header">Week ${week}</div>`;
        
        grouped[week].forEach(w => {
            const card = document.createElement('div');
            card.className = 'workout-item-card';
            
            const dateObj = new Date(w.scheduledDate);
            const formattedDate = dateObj.toLocaleDateString('no-NO', {
                weekday: 'short', day: 'numeric', month: 'short'
            });
            
            let colorClass = 'type-lime';
            if (w.isCompleted) colorClass = 'type-green';
            else if (new Date(w.scheduledDate).getTime() < new Date().setHours(0,0,0,0)) colorClass = 'type-red';
            
            let detailsText = '';
            if (w.workoutType.toUpperCase() === 'INTERVALS') {
                detailsText = `${w.intervalCount || 0}x ${w.intervalValue || "800m"} @ ${w.intervalPace || "--:--"} pace`;
            } else if (w.workoutType.toUpperCase() === 'STRENGTH & CORE') {
                detailsText = 'Strength & Core session';
            } else {
                detailsText = `${formatDistance(w.distance)} km | Pace: ${formatPace(w.pace)}`;
                if (w.isCompleted && w.avgHeartRate > 0) {
                    detailsText += ` | HR: ${w.avgHeartRate}`;
                }
            }
            
            card.innerHTML = `
                <div class="workout-card-layout">
                    <div class="workout-card-details" onclick="editWorkout('${w.id}')">
                        <span class="workout-card-date">${formattedDate}</span>
                        <div class="workout-card-type ${colorClass}">${getWorkoutTypeWithIcon(w.workoutType)}</div>
                        <div class="workout-card-metrics">${detailsText}</div>
                        <div class="workout-card-desc">${w.description || ''}</div>
                    </div>
                    <div class="workout-card-actions">
                        <button class="btn-card-delete" onclick="deleteWorkout('${w.id}')" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
                        <label class="android-checkbox">
                            <input type="checkbox" ${w.isCompleted ? 'checked' : ''} onchange="toggleWorkoutCompleted('${w.id}', this.checked)" ${appState.readOnly ? 'disabled' : ''}>
                            <span class="checkbox-box"></span>
                        </label>
                    </div>
                </div>
            `;
            weekDiv.appendChild(card);
        });
        
        listContainer.appendChild(weekDiv);
    });
}

function getWorkoutTypeWithIcon(type) {
    if (!type) return '';
    const upper = type.toUpperCase().trim();
    if (upper.includes("INTERVAL")) return "⚡ " + type;
    if (upper.includes("LONG RUN")) return "🏃‍♂️ " + type;
    if (upper.includes("TEMPO")) return "🔥 " + type;
    if (upper.includes("STEADY") || upper.includes("EASY")) return "🕊️ " + type;
    if (upper.includes("RECOVERY")) return "🔋 " + type;
    if (upper.includes("STRENGTH") || upper.includes("CORE")) return "💪 " + type;
    if (upper.includes("WALK")) return "🚶‍♂️ " + type;
    if (upper.includes("REST")) return "🛋️ " + type;
    return type;
}

function populatePlanFilters() {
    const select = elements.planFilterDropdown;
    const prev = select.value;
    const plans = new Set();
    appState.workouts.forEach(w => { if (w.planName) plans.add(w.planName); });
    
    select.innerHTML = '<option value="all">All Plans</option>';
    plans.forEach(plan => {
        const opt = document.createElement('option');
        opt.value = plan;
        opt.innerText = plan;
        select.appendChild(opt);
    });
    if (plans.has(prev)) select.value = prev;
}

function toggleWorkoutCompleted(id, isChecked) {
    if (appState.readOnly) return;
    const idx = appState.workouts.findIndex(w => String(w.id) === String(id));
    if (idx !== -1) {
        appState.workouts[idx].isCompleted = isChecked;
        if (db && appState.firebaseConnected) {
            db.ref(`workouts/${appState.userId}/workout_${id}`).update({ isCompleted: isChecked })
                .then(() => updateAggregatedStats());
        } else {
            saveWorkoutsLocally();
            renderWorkoutsList();
            updateAggregatedStats();
        }
    }
}
window.toggleWorkoutCompleted = toggleWorkoutCompleted;

// ----------------------------------------------------
// PACE CALCULATOR AUTOMATED MATH
// ----------------------------------------------------
function setupCalculatorBindings() {
    elements.calcSpinner.addEventListener('change', () => {
        if (elements.calcSpinner.value === 'custom') {
            elements.calcCustomDistContainer.style.display = 'block';
        } else {
            elements.calcCustomDistContainer.style.display = 'none';
        }
        calculateMissingPaceValue();
    });

    const triggerCalc = () => {
        calculateMissingPaceValue();
    };
    elements.calcCustomDistInput.addEventListener('input', triggerCalc);
    elements.calcTimeInput.addEventListener('input', (e) => {
        autoFormatTime(e.target);
        if (document.activeElement === elements.calcTimeInput) calculateMissingPaceValue();
    });
    elements.calcPaceInput.addEventListener('input', (e) => {
        autoFormatPace(e.target);
        if (document.activeElement === elements.calcPaceInput) calculateMissingPaceValue();
    });
    elements.calcSpeedInput.addEventListener('input', () => {
        if (document.activeElement === elements.calcSpeedInput) calculateMissingPaceValue();
    });

    // Toggle Riegel Predictor fields
    const togglePredictor = document.getElementById('toggle-riegel-predictor');
    const predictorFields = document.getElementById('riegel-predictor-fields');
    if (togglePredictor && predictorFields) {
        togglePredictor.addEventListener('click', () => {
            if (predictorFields.style.display === 'none') {
                predictorFields.style.display = 'block';
                togglePredictor.querySelector('label').innerText = '▼ RIEGEL PACE PREDICTOR';
            } else {
                predictorFields.style.display = 'none';
                togglePredictor.querySelector('label').innerText = '► RIEGEL PACE PREDICTOR';
            }
        });
    }

    // Auto-format recent time
    const predictorRecentTime = document.getElementById('predictor-recent-time');
    if (predictorRecentTime) {
        predictorRecentTime.addEventListener('input', (e) => {
            autoFormatTime(e.target);
        });
    }

    // Apply Prediction
    const btnPredictorApply = document.getElementById('btn-predictor-apply');
    const predictorRecentDist = document.getElementById('predictor-recent-dist');
    if (btnPredictorApply && predictorRecentDist && predictorRecentTime) {
        btnPredictorApply.addEventListener('click', () => {
            const d1Val = predictorRecentDist.value.trim().replace(',', '.');
            const t1Val = predictorRecentTime.value.trim();
            if (!d1Val || !t1Val) {
                alert("Please enter recent distance and time.");
                return;
            }
            const d1 = parseFloat(d1Val);
            const t1Seconds = parseTimeToSeconds(t1Val);
            if (isNaN(d1) || d1 <= 0 || t1Seconds <= 0) {
                alert("Please enter valid recent distance and time.");
                return;
            }
            
            const d2 = getCalculatorDistance();
            if (d2 <= 0) {
                alert("Please select/enter target distance first.");
                return;
            }

            const t2Seconds = t1Seconds * Math.pow(d2 / d1, 1.06);
            elements.calcTimeInput.value = formatSecondsToTime(t2Seconds);
            
            calculateMissingPaceValue();
            showToastNotification("Riegel prediction applied successfully!");
        });
    }

    // Splits Card Trigger
    const btnCalcSplits = document.getElementById('btn-calc-splits');
    if (btnCalcSplits) {
        btnCalcSplits.addEventListener('click', () => {
            generateWebSplitsCard();
        });
    }

    // Close Splits Modal
    const btnCloseSplitsModal = document.getElementById('btn-close-splits-modal');
    const btnCloseSplitsAction = document.getElementById('btn-close-splits-action');
    const modalSplits = document.getElementById('modal-splits');
    if (btnCloseSplitsModal && modalSplits) {
        btnCloseSplitsModal.addEventListener('click', () => {
            modalSplits.style.display = 'none';
        });
    }
    if (btnCloseSplitsAction && modalSplits) {
        btnCloseSplitsAction.addEventListener('click', () => {
            modalSplits.style.display = 'none';
        });
    }
}

function parseTimeToSeconds(timeStr) {
    if (!timeStr.includes(':')) {
        const val = parseFloat(timeStr);
        return isNaN(val) ? 0 : val * 60;
    }
    const parts = timeStr.split(':');
    if (parts.length === 3) {
        return parseInt(parts[0], 10) * 3600 + parseInt(parts[1], 10) * 60 + parseFloat(parts[2]);
    } else if (parts.length === 2) {
        return parseInt(parts[0], 10) * 60 + parseFloat(parts[1]);
    }
    return 0;
}

function formatSecondsToTime(totalSeconds) {
    let h = Math.floor(totalSeconds / 3600);
    let m = Math.floor((totalSeconds % 3600) / 60);
    let s = Math.round(totalSeconds % 60);
    if (s === 60) { m++; s = 0; }
    if (m === 60) { h++; m = 0; }
    if (h > 0) {
        return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    } else {
        return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
}

function getCalculatorDistance() {
    const selected = document.getElementById('calc-spinner').value;
    let d = 0;
    if (selected === '5k') d = 5.0;
    else if (selected === '10k') d = 10.0;
    else if (selected === 'half') d = 21.0975;
    else if (selected === 'marathon') d = 42.195;
    else if (selected === 'custom') {
        d = parseFloat(elements.calcCustomDistInput.value.replace(',', '.'));
    }
    return isNaN(d) ? 0 : d;
}

function generateWebSplitsCard() {
    const d = getCalculatorDistance();
    const timeStr = elements.calcTimeInput.value;
    if (d <= 0 || !timeStr) {
        alert("Please enter target distance and time first.");
        return;
    }
    const totalSeconds = parseTimeToSeconds(timeStr);
    if (totalSeconds <= 0) return;

    const evenPace = totalSeconds / d;
    const tbody = document.getElementById('splits-table-body');
    tbody.innerHTML = '';

    const totalKm = Math.floor(d);
    let cumEven = 0;
    let cumNeg = 0;

    for (let k = 1; k <= totalKm; k++) {
        cumEven += evenPace;
        let negPace = evenPace;
        if (d > 1.0) {
            negPace = evenPace * (1.025 - 0.05 * ((k - 1) / (d - 1)));
        }
        cumNeg += negPace;

        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="padding: 12px 10px; font-weight: bold; color: #fff;">${k}</td>
            <td style="padding: 12px 10px; color: #ccc;">${formatSecondsToTime(evenPace)} <span style="font-size:0.75rem; color:#888;">(${formatSecondsToTime(cumEven)})</span></td>
            <td style="padding: 12px 10px; color: var(--android-lime); font-weight: bold;">${formatSecondsToTime(negPace)} <span style="font-size:0.75rem; color:rgba(204,255,0,0.6);">(${formatSecondsToTime(cumNeg)})</span></td>
        `;
        tbody.appendChild(row);
    }

    if (d > totalKm) {
        const fracDist = d - totalKm;
        const fracEvenTime = fracDist * evenPace;
        cumEven += fracEvenTime;

        const remainingNegTime = totalSeconds - cumNeg;
        cumNeg = totalSeconds;
        const finalNegSegmentPace = remainingNegTime / fracDist;

        const row = document.createElement('tr');
        row.classList.add('finish-row');
        row.innerHTML = `
            <td style="padding: 12px 10px;">${d.toFixed(2)} (Finish)</td>
            <td style="padding: 12px 10px;">${formatSecondsToTime(evenPace)} <span style="font-size:0.75rem; color:rgba(255,255,255,0.7);">(${formatSecondsToTime(cumEven)})</span></td>
            <td style="padding: 12px 10px; color: var(--android-lime);">${formatSecondsToTime(finalNegSegmentPace)} <span style="font-size:0.75rem; color:rgba(255,255,255,0.7);">(${formatSecondsToTime(cumNeg)})</span></td>
        `;
        tbody.appendChild(row);
    }

    document.getElementById('modal-splits').style.display = 'flex';
}

function autoFormatTime(el) {
    let input = el.value.replace(/[^\d]/g, '');
    if (input.length > 6) input = input.substring(0, 6);
    let formatted = '';
    if (input.length > 0) {
        if (input.length <= 2) formatted = input;
        else if (input.length <= 4) formatted = `${input.substring(0, input.length - 2)}:${input.substring(input.length - 2)}`;
        else formatted = `${input.substring(0, input.length - 4)}:${input.substring(input.length - 4, input.length - 2)}:${input.substring(input.length - 2)}`;
    }
    el.value = formatted;
}

function autoFormatPace(el) {
    let input = el.value.replace(/[^\d]/g, '');
    if (input.length > 4) input = input.substring(0, 4);
    let formatted = '';
    if (input.length > 0) {
        if (input.length <= 2) formatted = input;
        else formatted = `${input.substring(0, input.length - 2)}:${input.substring(input.length - 2)}`;
    }
    el.value = formatted;
}

function calculateMissingPaceValue() {
    try {
        let dist = 0;
        const sel = elements.calcSpinner.value;
        if (sel === '5k') dist = 5.0;
        else if (sel === '10k') dist = 10.0;
        else if (sel === 'half') dist = 21.0975;
        else if (sel === 'marathon') dist = 42.195;
        else if (sel === 'custom') {
            dist = parseFloat(elements.calcCustomDistInput.value.replace(",", ".")) || 0;
        }
        if (dist <= 0) return;

        if (document.activeElement === elements.calcSpeedInput) {
            const speed = parseFloat(elements.calcSpeedInput.value.replace(",", ".")) || 0;
            if (speed > 0) {
                const paceDec = 60.0 / speed;
                elements.calcPaceInput.value = formatPace(paceDec);
                
                const totalMins = dist * paceDec;
                const h = Math.floor(totalMins / 60);
                const m = Math.floor(totalMins % 60);
                const s = Math.round((totalMins * 60) % 60);
                elements.calcTimeInput.value = `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}`;
            }
        } else if (document.activeElement === elements.calcTimeInput || (!elements.calcPaceInput.value && !elements.calcSpeedInput.value)) {
            const timeStr = elements.calcTimeInput.value;
            if (timeStr.includes(':')) {
                const parts = timeStr.split(':');
                let totalMins = 0;
                if (parts.length === 3) {
                    totalMins = (parseInt(parts[0], 10) * 60) + parseInt(parts[1], 10) + (parseFloat(parts[2]) / 60.0);
                } else if (parts.length === 2) {
                    totalMins = parseInt(parts[0], 10) + (parseFloat(parts[1]) / 60.0);
                }
                if (totalMins > 0) {
                    const paceDec = totalMins / dist;
                    elements.calcPaceInput.value = formatPace(paceDec);
                    elements.calcSpeedInput.value = (60.0 / paceDec).toFixed(2);
                }
            }
        } else if (document.activeElement === elements.calcPaceInput) {
            const paceStr = elements.calcPaceInput.value;
            if (paceStr.includes(':')) {
                const parts = paceStr.split(':');
                const paceMins = parseInt(parts[0], 10) + (parseFloat(parts[1]) / 60.0);
                if (paceMins > 0) {
                    const totalMins = dist * paceMins;
                    const h = Math.floor(totalMins / 60);
                    const m = Math.floor(totalMins % 60);
                    const s = Math.round((totalMins * 60) % 60);
                    elements.calcTimeInput.value = `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}`;
                    elements.calcSpeedInput.value = (60.0 / paceMins).toFixed(2);
                }
            }
        }
    } catch (e) {
        console.warn("Calculator error:", e);
    }
}

// ----------------------------------------------------
// PROFILE MANAGEMENT FUNCTIONS
// ----------------------------------------------------
function setupProfileFormFormatting() {
    const formatPB = (e) => {
        let val = e.target.value.replace(/[^\d]/g, '');
        if (val.length > 6) val = val.substring(0, 6);
        let f = '';
        if (val.length > 0) {
            if (val.length <= 2) f = val;
            else if (val.length <= 4) f = `${val.substring(0, val.length - 2)}:${val.substring(val.length - 2)}`;
            else f = `${val.substring(0, val.length - 4)}:${val.substring(val.length - 4, val.length - 2)}:${val.substring(val.length - 2)}`;
        }
        e.target.value = f;
    };
    elements.profileInputPb10k.addEventListener('input', formatPB);
    elements.profileInputPbhalf.addEventListener('input', formatPB);
    elements.profileInputPbfull.addEventListener('input', formatPB);
    
    // Live update zones on Max HR input change
    elements.profileInputMaxhr.addEventListener('input', () => {
        appState.maxHr = parseInt(elements.profileInputMaxhr.value) || 0;
        updateHRZonesDisplay();
    });
}

function handleProfileSave(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    
    const nick = elements.profileInputNickname.value.trim() || 'Athlete';
    const fullName = elements.profileInputName.value.trim() || 'Runner';
    const email = elements.profileInputEmail.value.trim() || 'athlete@example.com';
    
    appState.userName = nick;
    appState.fullName = fullName;
    appState.email = email;
    appState.age = parseInt(elements.profileInputAge.value) || 35;
    appState.weight = parseFloat(elements.profileInputWeight.value.replace(',', '.')) || 70;
    appState.maxHr = parseInt(elements.profileInputMaxhr.value) || 185;
    appState.pb10k = elements.profileInputPb10k.value;
    appState.pbHalf = elements.profileInputPbhalf.value;
    appState.pbFull = elements.profileInputPbfull.value;
    
    if (db && appState.firebaseConnected) {
        db.ref(`profiles/${appState.userId}`).update({
            name: fullName,
            nickname: nick,
            email: email,
            eventLocation: appState.userProfile.eventLocation || '',
            age: appState.age,
            weight: appState.weight,
            maxHr: appState.maxHr,
            pb10k: appState.pb10k,
            pbHalf: appState.pbHalf,
            pbFull: appState.pbFull,
            avatar: appState.avatar || null,
            lastUpdate: Date.now()
        }).then(() => {
            saveProfileLocally();
            alert("Profile Saved successfully!");
            navTo('home');
        });
    } else {
        saveProfileLocally();
        alert("Profile Saved (Local Demo Mode)!");
        updateProfileUI();
        navTo('home');
    }
}

function handleSyncIdClick() {
    if (appState.readOnly) return;
    const newId = elements.profileSyncIdInput.value.trim().toUpperCase();
    if (!newId) return;
    
    const oldId = appState.userId;
    appState.userId = newId;
    localStorage.setItem('maratontrener_userId', newId);
    
    // Clear local storage workouts and profile of previous user
    localStorage.removeItem('maratontrener_workouts');
    localStorage.removeItem('maratontrener_profile');
    localStorage.removeItem('maratontrener_shoes');
    
    // Reset local memory state
    appState.workouts = [];
    appState.shoes = [];
    appState.userName = 'Runner';
    appState.userProfile = {
        distance: 0,
        consistency: 0,
        workoutsDone: 0,
        workoutsTotal: 0,
        currentRace: 'Oslo Maraton - Marathon'
    };
    
    if (db) {
        try {
            db.ref(`profiles/${oldId}`).off();
            db.ref(`workouts/${oldId}`).off();
            db.ref(`profiles/${oldId}/followers`).off();
        } catch (e) {}
        setupFirebaseSync();
    } else {
        loadLocalFallbackData();
    }
    alert(`Runner ID updated and synced: ${newId}`);
    navTo('home');
}

function restoreFromCloud() {
    if (appState.readOnly) return;
    if (!db || !appState.firebaseConnected) {
        alert("Restore only available when online.");
        return;
    }
    
    alert("Restoring data from cloud database...");
    
    // Fetch profile info
    db.ref(`profiles/${appState.userId}`).once('value', (snap) => {
        const val = snap.val();
        if (val) {
            appState.userName = val.name || 'Runner';
            if (val.currentRace) {
                appState.userProfile.currentRace = val.currentRace;
            }
            updateProfileUI();
        }
    });

    // Fetch workouts
    db.ref(`workouts/${appState.userId}`).once('value', (snap) => {
        const val = snap.val();
        if (val) {
            const list = [];
            Object.keys(val).forEach(k => {
                list.push({ id: k.replace("workout_", ""), ...val[k] });
            });
            list.sort((a, b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
            appState.workouts = list;
            saveWorkoutsLocally();
            renderWorkoutsList();
            updateAggregatedStats();
            alert("Workouts successfully restored!");
            navTo('home');
        } else {
            alert("No training plan found in the cloud.");
        }
    });
}

function confirmDeleteData() {
    if (appState.readOnly) return;
    if (confirm("PERMANENT DELETE\n\nThis will wipe all your local training plan data. This cannot be undone. Are you sure?")) {
        localStorage.clear();
        appState.workouts = [];
        appState.buddies = [];
        appState.userName = 'Runner';
        if (db && appState.firebaseConnected) {
            db.ref(`workouts/${appState.userId}`).remove();
            db.ref(`profiles/${appState.userId}`).remove();
        }
        alert("All data cleared. Reloading page...");
        window.location.reload();
    }
}

function saveWorkoutsLocally() {
    if (appState.readOnly) return;
    localStorage.setItem('maratontrener_workouts', JSON.stringify(appState.workouts));
}

// ----------------------------------------------------
// BUDDIES MANAGEMENT
// ----------------------------------------------------
function handleBuddySubmitInline(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    const buddyId = elements.inputBuddyIdInline.value.trim().toUpperCase();
    if (!buddyId) return;
    
    addBuddy(buddyId);
    elements.inputBuddyIdInline.value = '';
}

function handleBuddySubmitModal(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    const buddyId = elements.buddyIdInput.value.trim().toUpperCase();
    if (!buddyId) return;
    
    addBuddy(buddyId);
    closeBuddyModal();
}

function addBuddy(buddyId) {
    if (db && appState.firebaseConnected) {
        db.ref(`profiles/${buddyId}`).once('value', (snap) => {
            if (snap.exists()) {
                db.ref(`profiles/${appState.userId}/followers/${buddyId}`).set(true)
                    .then(() => {
                        alert(`Buddy ${buddyId} successfully followed!`);
                    });
            } else {
                alert(`Buddy ID ${buddyId} not found.`);
            }
        });
    } else {
        // Mock fallback
        const mockName = ["Eirik", "Sophia", "Lars", "Marcus"][Math.floor(Math.random()*4)];
        const mockBuddy = {
            id: buddyId,
            name: mockName,
            distance: (80 + Math.random() * 80).toFixed(1),
            consistency: Math.floor(75 + Math.random()*25),
            currentRace: "Oslo Maraton - Marathon"
        };
        updateBuddyInList(mockBuddy);
        alert(`Buddy ${mockName} added in demo mode.`);
    }
}

function updateBuddyInList(buddy) {
    const idx = appState.buddies.findIndex(b => b.id === buddy.id);
    if (idx !== -1) appState.buddies[idx] = buddy;
    else appState.buddies.push(buddy);
    renderBuddiesUI();
}

function renderBuddiesUI() {
    const list = elements.leaderboardList;
    list.innerHTML = '';
    
    if (appState.buddies.length === 0) {
        list.innerHTML = `
            <div class="empty-state-recipes">
                <i class="fa-solid fa-users-slash"></i>
                <p>No training buddies followed yet.</p>
            </div>
        `;
        return;
    }
    
    // Sort by distance descending (leaderboard!)
    const sorted = [...appState.buddies].sort((a, b) => parseFloat(b.distance) - parseFloat(a.distance));
    
    sorted.forEach((buddy, index) => {
        const item = document.createElement('div');
        item.className = 'leaderboard-item';
        item.title = "Klikk for å se treningsplan (Click to view training plan)";
        item.onclick = () => {
            if (buddy.id) {
                window.location.href = `?view=${buddy.id}`;
            }
        };
        
        const firstLetter = buddy.name ? buddy.name.charAt(0).toUpperCase() : 'R';
        
        item.innerHTML = `
            <div class="leaderboard-left">
                <span class="leaderboard-rank">#${index + 1}</span>
                <div class="leaderboard-avatar">${firstLetter}</div>
                <div class="leaderboard-info">
                    <span class="leaderboard-name">${buddy.name}</span>
                    <span class="leaderboard-race">${buddy.currentRace || 'Training'}</span>
                </div>
            </div>
            <div class="leaderboard-right">
                <div class="leaderboard-dist">${buddy.distance} km</div>
                <div class="leaderboard-consistency">${buddy.consistency}% consistency</div>
                <div class="leaderboard-highfives" style="margin-top: 6px; display: flex; justify-content: flex-end;">
                    <button class="leaderboard-highfive-btn ${buddy.highFives && buddy.highFives[appState.userId] ? 'active' : ''}" 
                            title="Send en High-Five!" 
                            onclick="handleWebHighFive(event, '${buddy.id}')">
                        <i class="${buddy.highFives && buddy.highFives[appState.userId] ? 'fa-solid' : 'fa-regular'} fa-hand"></i>
                        <span>${buddy.highFiveCount}</span>
                    </button>
                </div>
            </div>
        `;
        list.appendChild(item);
    });
}

function handleWebHighFive(event, buddyId) {
    event.stopPropagation();
    if (!db || !appState.firebaseConnected || appState.readOnly) return;
    
    const buddyRef = db.ref(`profiles/${buddyId}/highFives/${appState.userId}`);
    const myName = appState.userName || 'A buddy';
    
    db.ref(`profiles/${buddyId}/highFives`).once('value', (snap) => {
        const faves = snap.val() || {};
        if (faves[appState.userId]) {
            buddyRef.remove();
        } else {
            buddyRef.set(myName);
        }
    });
}

function showToastNotification(message) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.position = 'fixed';
        container.style.top = '20px';
        container.style.left = '50%';
        container.style.transform = 'translateX(-50%)';
        container.style.zIndex = '9999';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '10px';
        container.style.pointerEvents = 'none';
        document.body.appendChild(container);
    }
    
    const toast = document.createElement('div');
    toast.className = 'toast-notification-banner';
    toast.innerHTML = `
        <i class="fa-solid fa-hands-clapping" style="color: var(--android-lime); margin-right: 10px;"></i>
        <span>${message}</span>
    `;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-20px)';
        setTimeout(() => {
            toast.remove();
            if (container.children.length === 0) {
                container.remove();
            }
        }, 300);
    }, 3700);
}

function copyShareId() {
    const nickname = appState.userName || 'Runner';
    const inviteText = `Hei! Sjekk ut treningsfremgangen min i den nye appen min - MarathonTrainer!
Gå til https://www.maratontrener.no/?runner=${appState.userId} for å se min treningsplan!

Lyst til å prøve selv? Bli med på testen og last ned appen til Android her: https://tinyurl.com/IThoughtTheySaidRum eller https://join.maratontrener.no/ hvis du bruker iPhone.

${nickname}
------------------------------------------------------------------------------------------------------------
Hey! Check out my training progress in my new app - MarathonTrainer! 
Visit https://www.maratontrener.no/?runner=${appState.userId} to see my training plan!

Want to try it yourself? Join the test and download the Android app here: https://tinyurl.com/IThoughtTheySaidRum or visit https://join.maratontrener.no/ if you're on an iPhone.

${nickname}`;
    navigator.clipboard.writeText(inviteText)
        .then(() => alert("Your custom invite text was copied to clipboard!"))
        .catch(err => alert("Copy failed. Your ID: " + appState.userId));
}

// ----------------------------------------------------
// WORKOUT FORM POPUP EDIT ACTIONS
// ----------------------------------------------------
function openWorkoutModal(w = null) {
    if (appState.readOnly) return;
    elements.workoutModal.classList.add('active');
    
    if (w) {
        elements.modalTitle.innerText = "Edit Workout Details";
        elements.workoutId.value = w.id;
        elements.workoutDate.value = w.scheduledDate;
        elements.workoutWeek.value = w.weekNumber;
        elements.workoutType.value = w.workoutType ? w.workoutType.toUpperCase() : 'LONG RUN';
        elements.workoutPlan.value = w.planName;
        elements.workoutDistance.value = w.distance;
        elements.workoutPace.value = w.pace;
        elements.workoutDuration.value = w.totalDuration || '';
        elements.workoutAvgHr.value = w.avgHeartRate || '';
        elements.workoutDescription.value = w.description || '';
        elements.workoutNotes.value = w.notes || '';
        elements.workoutCompleted.checked = w.isCompleted;
        elements.workoutShoe.value = w.shoeId || '';
        
        // Populate intervals fields
        elements.workoutIntervalCount.value = w.intervalCount || '';
        elements.workoutIntervalValue.value = w.intervalValue || '';
        elements.workoutIntervalPace.value = w.intervalPace || '';
        elements.workoutMaxHr.value = w.maxHeartRate || '';
        
        if (w.workoutType && w.workoutType.toUpperCase() === 'INTERVALS') {
            elements.workoutIntervalSection.style.display = 'block';
        } else {
            elements.workoutIntervalSection.style.display = 'none';
        }
    } else {
        elements.modalTitle.innerText = "Log a New Workout";
        elements.workoutForm.reset();
        elements.workoutId.value = '';
        elements.workoutDate.value = new Date().toISOString().split('T')[0];
        elements.workoutWeek.value = '1';
        elements.workoutPlan.value = appState.userProfile.currentRace.split(" - ")[0] || "Oslo Maraton";
        
        // Reset interval fields
        elements.workoutIntervalSection.style.display = 'none';
        elements.workoutIntervalCount.value = '';
        elements.workoutIntervalValue.value = '';
        elements.workoutIntervalPace.value = '';
        elements.workoutMaxHr.value = '';

        // Default to first active shoe
        const firstActive = appState.shoes.find(s => !s.retired);
        elements.workoutShoe.value = firstActive ? firstActive.id : '';
    }
}

function closeWorkoutModal() {
    elements.workoutModal.classList.remove('active');
}

function editWorkout(id) {
    if (appState.readOnly) return;
    const w = appState.workouts.find(x => String(x.id) === String(id));
    if (w) openWorkoutModal(w);
}
window.editWorkout = editWorkout;

function deleteWorkout(id) {
    if (appState.readOnly) return;
    if (confirm("Delete this workout?")) {
        if (db && appState.firebaseConnected) {
            db.ref(`workouts/${appState.userId}/workout_${id}`).remove();
        } else {
            appState.workouts = appState.workouts.filter(w => String(w.id) !== String(id));
            saveWorkoutsLocally();
            renderWorkoutsList();
            updateAggregatedStats();
            populatePlanFilters();
            updateShoesUI();
        }
    }
}
window.deleteWorkout = deleteWorkout;

function handleWorkoutSubmit(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    
    const id = elements.workoutId.value;
    const data = {
        scheduledDate: elements.workoutDate.value,
        weekNumber: parseInt(elements.workoutWeek.value, 10) || 1,
        workoutType: elements.workoutType.value,
        planName: elements.workoutPlan.value,
        distance: parseFloat(elements.workoutDistance.value) || 0,
        pace: elements.workoutPace.value,
        totalDuration: parseInt(elements.workoutDuration.value, 10) || 0,
        avgHeartRate: parseInt(elements.workoutAvgHr.value, 10) || 0,
        description: elements.workoutDescription.value,
        notes: elements.workoutNotes.value,
        isCompleted: elements.workoutCompleted.checked,
        shoeId: elements.workoutShoe.value || '',
        
        // Interval Specifics
        intervalCount: parseInt(elements.workoutIntervalCount.value, 10) || 0,
        intervalValue: elements.workoutIntervalValue.value || '',
        intervalPace: elements.workoutIntervalPace.value || '',
        maxHeartRate: parseInt(elements.workoutMaxHr.value, 10) || 0
    };
    
    if (db && appState.firebaseConnected) {
        const key = id ? `workout_${id}` : `workout_${Date.now()}`;
        db.ref(`workouts/${appState.userId}/${key}`).update(data)
            .then(() => closeWorkoutModal());
    } else {
        if (id) {
            const idx = appState.workouts.findIndex(w => String(w.id) === String(id));
            if (idx !== -1) appState.workouts[idx] = { id, ...data };
        } else {
            appState.workouts.push({ id: `local_${Date.now()}`, ...data });
        }
        saveWorkoutsLocally();
        renderWorkoutsList();
        updateAggregatedStats();
        populatePlanFilters();
        updateShoesUI();
        closeWorkoutModal();
    }
}

// ----------------------------------------------------
// SETUP GENERATOR WIZARD STEPS
// ----------------------------------------------------
function openGeneratorModal() {
    if (appState.readOnly) return;
    appState.wizardPage = 1;
    elements.generatorWizardModal.classList.add('active');
    
    // Set standard dates
    elements.wizEventDate.value = new Date(Date.now() + (1000*60*60*24*84)).toISOString().split('T')[0]; // 12 weeks out
    elements.wizStartDate.value = new Date().toISOString().split('T')[0];
    
    // Populate profile inputs from state
    elements.wizAge.value = appState.age || '';
    elements.wizWeight.value = appState.weight || '';
    elements.wizMaxhr.value = appState.maxHr || '';
    elements.wizPb10k.value = appState.pb10k || '';
    elements.wizPbhalf.value = appState.pbHalf || '';
    elements.wizPbfull.value = appState.pbFull || '';
    
    updateWizardUI();
}

function closeWizardModal() {
    elements.generatorWizardModal.classList.remove('active');
}

function navigateWizardBack() {
    if (appState.wizardPage > 1) {
        appState.wizardPage--;
        updateWizardUI();
    }
}

function navigateWizardNext() {
    if (appState.wizardPage === 1) {
        if (!elements.wizEventName.value.trim() || !elements.wizEventDate.value) {
            alert("Please enter a race name and date.");
            return;
        }
        appState.wizardPage = 2;
        updateWizardUI();
    } else if (appState.wizardPage === 2) {
        appState.wizardPage = 3;
        updateWizardUI();
    } else {
        // Run Generator math!
        generateTrainingPlanFromWizard();
    }
}

function updateWizardUI() {
    document.getElementById('wiz-page-1').classList.toggle('active', appState.wizardPage === 1);
    document.getElementById('wiz-page-2').classList.toggle('active', appState.wizardPage === 2);
    document.getElementById('wiz-page-3').classList.toggle('active', appState.wizardPage === 3);
    
    elements.wizardTitleStep.innerText = `STEP ${appState.wizardPage} OF 3`;
    elements.btnWizBack.style.display = appState.wizardPage > 1 ? "block" : "none";
    elements.btnWizNext.innerText = appState.wizardPage === 3 ? "GENERATE PLAN" : "NEXT STEP";
}

function setupWizardAutoFormatting() {
    const formatPB = (e) => {
        let val = e.target.value.replace(/[^\d]/g, '');
        if (val.length > 6) val = val.substring(0, 6);
        let f = '';
        if (val.length > 0) {
            if (val.length <= 2) f = val;
            else if (val.length <= 4) f = `${val.substring(0, val.length - 2)}:${val.substring(val.length - 2)}`;
            else f = `${val.substring(0, val.length - 4)}:${val.substring(val.length - 4, val.length - 2)}:${val.substring(val.length - 2)}`;
        }
        e.target.value = f;
    };
    elements.wizPb10k.addEventListener('input', formatPB);
    elements.wizPbhalf.addEventListener('input', formatPB);
    elements.wizPbfull.addEventListener('input', formatPB);
    
    elements.wizGoalTime.addEventListener('input', formatPB);
}

// ----------------------------------------------------
// 12-WEEK PLAN GENERATOR CALCULATIONS
// ----------------------------------------------------
function generateTrainingPlanFromWizard() {
    const eventName = elements.wizEventName.value.trim();
    const eventLocation = elements.wizEventLocation.value.trim();
    const eventDateStr = elements.wizEventDate.value;
    const goalTimeStr = elements.wizGoalTime.value;
    const raceType = elements.wizRaceType.value;
    
    const age = parseInt(elements.wizAge.value, 10) || 35;
    const weight = parseFloat(elements.wizWeight.value.replace(',', '.')) || 70;
    const maxHr = parseInt(elements.wizMaxhr.value, 10) || 185;
    
    const startDateStr = elements.wizStartDate.value;
    const includeStrength = elements.wizCheckStrength.checked;
    
    // Save selections to appState and local profile
    appState.userName = appState.userName || 'Runner';
    appState.age = age;
    appState.weight = weight;
    appState.maxHr = maxHr;
    appState.pb10k = elements.wizPb10k.value;
    appState.pbHalf = elements.wizPbhalf.value;
    appState.pbFull = elements.wizPbfull.value;
    appState.userProfile.currentRace = `${eventName} - ${raceType}`;
    appState.userProfile.eventLocation = eventLocation;
    
    // Determine target race distance
    let raceDistance = 42.2;
    if (raceType.includes("Half")) raceDistance = 21.1;
    else if (raceType.includes("10K")) raceDistance = 10.0;
    else if (raceType.includes("5K")) raceDistance = 5.0;
    
    // Parse target goal time to hours
    const targetHours = parseTime(goalTimeStr);
    // Pace calculations: min/km
    const racePaceMinPerKm = (targetHours * 60.0) / raceDistance;
    
    // Workout Assignments dropdown map
    const assignments = {};
    const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const fullDaysNameMap = {
        "Mon": "Monday", "Tue": "Tuesday", "Wed": "Wednesday",
        "Thu": "Thursday", "Fri": "Friday", "Sat": "Saturday", "Sun": "Sunday"
    };
    
    const preferredDays = new Set();
    days.forEach(day => {
        const chk = document.getElementById(`wiz-check-${day}`);
        if (chk && chk.checked) {
            const fullName = fullDaysNameMap[day];
            preferredDays.add(fullName);
            assignments[fullName] = document.getElementById(`wiz-type-${day}`).value;
        }
    });
    
    if (preferredDays.size === 0) {
        alert("Please select at least one training day.");
        return;
    }
    
    // Loop dates day by day
    const startDate = new Date(startDateStr);
    const raceDate = new Date(eventDateStr);
    
    const totalDays = (raceDate - startDate) / (1000 * 60 * 60 * 24);
    let totalWeeks = Math.ceil(totalDays / 7);
    if (totalWeeks < 1) totalWeeks = 1;
    
    const planWorkouts = [];
    let current = new Date(startDate);
    let currentWeek = 0;
    
    const strengthDay = includeStrength ? "Friday" : "";
    const runningTypes = ["INTERVALS", "STEADY RUN", "LONG RUN"];
    let runCounter = 0;
    
    while (current < raceDate) {
        const dayOfWeek = current.getDay(); // Sunday is 0, Monday is 1...
        const dayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
        const dayName = dayNames[dayOfWeek];
        
        if (preferredDays.has(dayName)) {
            const w = {
                planName: eventName,
                weekNumber: currentWeek + 1,
                scheduledDate: current.toISOString().split('T')[0],
                isCompleted: false,
                notes: ""
            };
            
            let type = "";
            if (includeStrength && dayName === strengthDay) {
                type = "STRENGTH & CORE";
            } else {
                type = assignments[dayName] || runningTypes[runCounter % runningTypes.length];
                if (type !== "STRENGTH & CORE") runCounter++;
            }
            w.workoutType = type;
            
            // Phase Progression Math
            const progress = currentWeek / totalWeeks;
            const phase = progress > 0.85 ? "TAPER" : (progress > 0.4 ? "PEAK" : "BASE");
            
            if (type.toUpperCase() === 'LONG RUN') {
                let base = 12.0, max = 30.0;
                if (raceType.includes("5K")) {
                    base = 4.0; max = 7.0;
                } else if (raceType.includes("10K")) {
                    base = 6.0; max = 12.0;
                } else if (raceType.includes("Half")) {
                    base = 8.0; max = 18.0;
                }
                const pace = racePaceMinPerKm * 1.2;
                w.distance = phase === "TAPER" ? (max * 0.7) : Math.min(base + currentWeek * 1.5, max);
                w.pace = formatPace(pace);
                w.description = `Endurance: Target ${formatPace(pace)} min/km. Time on feet is key.`;
            } else if (type.toUpperCase() === 'INTERVALS') {
                const reps = 4 + Math.floor(currentWeek / 3);
                const pace = racePaceMinPerKm * 0.95;
                w.intervalCount = reps;
                w.intervalValue = "800m";
                w.intervalPace = formatPace(pace);
                w.description = `Speedwork: ${reps} sets at ${formatPace(pace)} pace. Recovery: 90s jog.`;
            } else if (type.toUpperCase() === 'STRENGTH & CORE') {
                const wk = w.weekNumber;
                if (wk <= 4) {
                    w.description = "Strength Phase 1 (Foundation): Glute Bridges (3x15), Squats (3x15), Plank (3x60s), Supermans (3x12), Bird-Dogs (3x12), Push-ups (3x10), Walking Lunges (3x10/leg). Focus on posture and form.";
                } else if (wk <= 8) {
                    w.description = "Strength Phase 2 (Strength & Back Focus): Dumbbell Rows (3x10/arm), Single-Leg Deadlifts (3x10/leg), Tricep Dips (3x12), Step-ups (3x12/leg), Side Plank (3x30s/side), Leg Raises (3x12). Target back fatigue.";
                } else {
                    w.description = "Strength Phase 3 (Peak Power & Posture): Dumbbell Rows (3x12/arm), Supermans (3x15), Single-Leg Glute Bridges (3x10/leg), Plank w/ Shoulder Taps (3x45s), Lunges w/ Twist (3x10/leg), Single-Leg Deadlifts (3x12/leg). Shock absorption for asphalt.";
                }
                w.distance = 0;
                w.pace = "";
            } else if (type.toUpperCase() === 'TEMPO RUN') {
                let base = 6.0, prog = 0.5;
                if (raceType.includes("5K")) {
                    base = 3.0; prog = 0.2;
                } else if (raceType.includes("10K")) {
                    base = 4.0; prog = 0.3;
                } else if (raceType.includes("Half")) {
                    base = 5.0; prog = 0.5;
                }
                const pace = racePaceMinPerKm * 1.05;
                w.distance = base + currentWeek * prog;
                w.pace = formatPace(pace);
                w.description = `Threshold: Target ${formatPace(pace)} min/km. Builds sustained speed.`;
            } else {
                let base = 6.0, prog = 0.5;
                if (raceType.includes("5K")) {
                    base = 3.0; prog = 0.2;
                } else if (raceType.includes("10K")) {
                    base = 4.0; prog = 0.3;
                } else if (raceType.includes("Half")) {
                    base = 5.0; prog = 0.5;
                }
                const pace = racePaceMinPerKm * 1.12;
                w.distance = base + currentWeek * prog;
                w.pace = formatPace(pace);
                w.description = `Easy Run: Target ${formatPace(pace)} min/km. Keep heart rate low.`;
            }
            planWorkouts.push(w);
        }
        
        // Sunday increments week number (0 is Sunday)
        if (dayOfWeek === 0) {
            currentWeek++;
        }
        current.setDate(current.getDate() + 1);
    }
    
    // Save in Database
    if (db && appState.firebaseConnected) {
        // Wipe old uncompleted logs in Firebase
        db.ref(`workouts/${appState.userId}`).once('value', (snap) => {
            const val = snap.val();
            const updates = {};
            if (val) {
                Object.keys(val).forEach(k => {
                    if (!val[k].isCompleted) {
                        updates[k] = null; // delete
                    }
                });
            }
            
            // Push new ones
            planWorkouts.forEach(w => {
                const key = `workout_${Date.now()}_${Math.floor(Math.random()*1000)}`;
                updates[key] = w;
            });
            
            db.ref(`workouts/${appState.userId}`).update(updates)
                .then(() => {
                    // Save profile details
                    const planStartVal = new Date(startDateStr).getTime();
                    appState.userProfile.planStartDate = planStartVal;
                    db.ref(`profiles/${appState.userId}`).update({
                        name: appState.fullName || appState.userName,
                        nickname: appState.userName,
                        currentRace: appState.userProfile.currentRace,
                        eventLocation: eventLocation,
                        age: appState.age,
                        weight: appState.weight,
                        maxHr: appState.maxHr,
                        pb10k: appState.pb10k,
                        pbHalf: appState.pbHalf,
                        pbFull: appState.pbFull,
                        planStartDate: planStartVal,
                        lastUpdate: Date.now()
                    });
                    
                    closeWizardModal();
                    alert("12-Week customized plan successfully created!");
                    navTo('log');
                });
        });
    } else {
        // Demo local state
        const planStartVal = new Date(startDateStr).getTime();
        appState.userProfile.planStartDate = planStartVal;
        appState.workouts = appState.workouts.filter(w => w.isCompleted); // keep completed
        planWorkouts.forEach(w => {
            w.id = `off_${Date.now()}_${Math.floor(Math.random()*1000)}`;
            appState.workouts.push(w);
        });
        saveWorkoutsLocally();
        
        closeWizardModal();
        updateProfileUI();
        renderWorkoutsList();
        updateAggregatedStats();
        populatePlanFilters();
        alert("12-Week customized plan successfully created!");
        navTo('log');
    }
}

function parseTime(time) {
    try {
        const parts = time.split(":");
        if (parts.length === 3) return parseInt(parts[0],10) + (parseInt(parts[1],10)/60.0) + (parseFloat(parts[2])/3600.0);
        if (parts.length === 2) {
            const first = parseInt(parts[0], 10);
            if (first >= 12) return (first / 60.0) + (parseFloat(parts[1]) / 3600.0);
            return first + (parseFloat(parts[1]) / 60.0);
        }
        return parseFloat(time) || 4.0;
    } catch(e) { return 4.0; }
}

// ----------------------------------------------------
// CHARTS & ANALYTICS RENDER
// ----------------------------------------------------
let volumeChart = null;
let distributionChart = null;
let trendsChart = null;
let monthlyChart = null;

function renderAnalyticsCharts() {
    if (typeof Chart === 'undefined') return;
    
    const completed = appState.workouts.filter(w => w.isCompleted);
    
    // Group volume by calendar week
    const weeklyVolume = {};
    completed.forEach(w => {
        const d = new Date(w.scheduledDate);
        const { week } = getCalendarWeekAndYear(d);
        if (!weeklyVolume[week]) {
            weeklyVolume[week] = { dist: 0, paceSum: 0, paceCount: 0 };
        }
        weeklyVolume[week].dist += parseFloat(w.distance || 0);
        const paceDec = parsePaceToDecimal(w.pace);
        if (paceDec > 0) {
            weeklyVolume[week].paceSum += paceDec;
            weeklyVolume[week].paceCount++;
        }
    });
    
    const sortedWeeks = Object.keys(weeklyVolume).map(Number).sort((a,b)=>a-b);
    const labelsVolume = sortedWeeks.map(w => `W${w}`);
    const dataVolume = sortedWeeks.map(w => weeklyVolume[w].dist);
    
    // 1. Weekly Volume Bar Chart
    const ctxVol = document.getElementById('chart-volume-vp');
    if (ctxVol) {
        if (volumeChart) volumeChart.destroy();
        const grad = ctxVol.getContext('2d').createLinearGradient(0, 0, 0, 200);
        grad.addColorStop(0, 'rgba(204, 255, 0, 0.5)');
        grad.addColorStop(1, 'rgba(204, 255, 0, 0.02)');
        
        volumeChart = new Chart(ctxVol, {
            type: 'bar',
            data: {
                labels: labelsVolume.length > 0 ? labelsVolume : ['No Data'],
                datasets: [{
                    data: dataVolume.length > 0 ? dataVolume : [0],
                    backgroundColor: grad,
                    borderColor: '#CCFF00',
                    borderWidth: 2,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const index = context.dataIndex;
                                const w = sortedWeeks[index];
                                let label = `Volum: ${context.parsed.y.toFixed(2)} km`;
                                const item = weeklyVolume[w];
                                if (item && item.paceCount > 0) {
                                    label += ` | Pace: ${formatPace(item.paceSum / item.paceCount)}/km`;
                                }
                                return label;
                            }
                        }
                    }
                },
                scales: {
                    x: { ticks: { color: '#8E8E93' }, grid: { display: false } },
                    y: { ticks: { color: '#8E8E93' }, grid: { color: 'rgba(255,255,255,0.05)' } }
                }
            },
            plugins: [datalabelsPlugin]
        });
    }
    
    // 2. Workout Type Doughnut
    const typesCount = {};
    completed.forEach(w => {
        const type = w.workoutType || 'Easy Run';
        typesCount[type] = (typesCount[type] || 0) + 1;
    });
    const labelsType = Object.keys(typesCount);
    const dataType = labelsType.map(t => typesCount[t]);
    const colorsMap = {
        'LONG RUN': '#3b82f6', 'INTERVALS': '#f59e0b', 'TEMPO RUN': '#ef4444',
        'STEADY RUN': '#34C759', 'EASY RUN': '#34C759', 'STRENGTH & CORE': '#a78bfa'
    };
    const bgColors = labelsType.map(t => colorsMap[t.toUpperCase()] || '#CCFF00');
    
    const ctxDist = document.getElementById('chart-distribution-vp');
    if (ctxDist) {
        if (distributionChart) distributionChart.destroy();
        distributionChart = new Chart(ctxDist, {
            type: 'doughnut',
            data: {
                labels: labelsType.length > 0 ? labelsType : ['No Data'],
                datasets: [{
                    data: dataType.length > 0 ? dataType : [0],
                    backgroundColor: bgColors,
                    borderColor: '#1C1C1E',
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { color: '#8E8E93', boxWidth: 10, font: { size: 9 } } }
                },
                cutout: '65%'
            },
            plugins: [datalabelsPlugin]
        });
    }
    
    // 3. Trends Line
    const trends = completed.slice(-7);
    const labelsTrend = trends.map(w => new Date(w.scheduledDate).toLocaleDateString('no-NO', { day: 'numeric', month: 'short' }));
    const dataPaces = trends.map(w => parsePaceToDecimal(w.pace) || null);
    const dataHrs = trends.map(w => w.avgHeartRate || null);
    
    const ctxTrends = document.getElementById('chart-trends-vp');
    if (ctxTrends) {
        if (trendsChart) trendsChart.destroy();
        trendsChart = new Chart(ctxTrends, {
            type: 'line',
            data: {
                labels: labelsTrend.length > 0 ? labelsTrend : ['No Data'],
                datasets: [
                    {
                        label: 'Pace (min/km)',
                        data: dataPaces.length > 0 ? dataPaces : [0],
                        borderColor: '#00E5FF',
                        borderWidth: 2.5,
                        tension: 0.3,
                        yAxisID: 'y'
                    },
                    {
                        label: 'HR (BPM)',
                        data: dataHrs.length > 0 ? dataHrs : [0],
                        borderColor: '#FF3B30',
                        borderWidth: 2.5,
                        tension: 0.3,
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: '#8E8E93' }, grid: { display: false } },
                    y: {
                        type: 'linear', position: 'left', ticks: {
                            color: '#8E8E93', callback: function(value) {
                                return formatPace(value);
                            }
                        }
                    },
                    y1: { type: 'linear', position: 'right', grid: { display: false }, ticks: { color: '#8E8E93' } }
                }
            }
        });
    }
    
    // 4. Monthly Progress Line
    const monthlyVolume = {};
    completed.forEach(w => {
        const mLabel = new Date(w.scheduledDate).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
        monthlyVolume[mLabel] = (monthlyVolume[mLabel] || 0) + parseFloat(w.distance || 0);
    });
    const labelsMonthly = Object.keys(monthlyVolume).sort((a,b)=> new Date(a)-new Date(b));
    const dataMonthly = labelsMonthly.map(m => monthlyVolume[m]);
    
    const ctxMonthly = document.getElementById('chart-monthly-vp');
    if (ctxMonthly) {
        if (monthlyChart) monthlyChart.destroy();
        const gradM = ctxMonthly.getContext('2d').createLinearGradient(0, 0, 0, 200);
        gradM.addColorStop(0, 'rgba(0, 229, 255, 0.4)');
        gradM.addColorStop(1, 'rgba(0, 229, 255, 0.02)');
        
        monthlyChart = new Chart(ctxMonthly, {
            type: 'line',
            data: {
                labels: labelsMonthly.length > 0 ? labelsMonthly : ['No Data'],
                datasets: [{
                    data: dataMonthly.length > 0 ? dataMonthly : [0],
                    borderColor: '#00E5FF',
                    backgroundColor: gradM,
                    fill: true,
                    tension: 0.35,
                    borderWidth: 2.5
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { color: '#8E8E93' }, grid: { display: false } },
                    y: { ticks: { color: '#8E8E93' } }
                }
            }
        });
    }
}

// Chart Datalabels plugin to display distance directly on top of bars/slices
const datalabelsPlugin = {
    id: 'datalabels',
    afterDatasetsDraw(chart) {
        const { ctx } = chart;
        ctx.save();
        chart.data.datasets.forEach((dataset, i) => {
            const meta = chart.getDatasetMeta(i);
            meta.data.forEach((element, index) => {
                const val = dataset.data[index];
                if (val === null || val === undefined || val === 0) return;
                
                let text = "";
                if (chart.config.type === 'doughnut') {
                    text = val.toString();
                } else if (chart.config.type === 'bar') {
                    text = `${parseFloat(val).toFixed(2).replace('.', ',')} km`;
                } else {
                    return;
                }
                
                ctx.fillStyle = '#FFFFFF';
                ctx.font = 'bold 9px Plus Jakarta Sans';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                
                const { x, y } = element.tooltipPosition();
                if (chart.config.type === 'bar') {
                    ctx.fillStyle = '#CCFF00';
                    ctx.fillText(text, x, y - 8);
                } else {
                    ctx.fillText(text, x, y);
                }
            });
        });
        ctx.restore();
    }
};

// ----------------------------------------------------
// DIET & NUTRITION VIEW-ONLY Lifecycles
// ----------------------------------------------------
// Exact dinner recipes database matching MealRepository.java
const MEALS = [
    {
        id: "w1_frokost", title: "Kyllingwok med brokkoli og cashewnøtter", category: "Middag", weekNumber: 1,
        ingredients: [
            { name: "Kyllingfilet (i strimler)", amount: 150, unit: "g" },
            { name: "Brokkoli (i buketter)", amount: 100, unit: "g" },
            { name: "Woksaus (lavorientert)", amount: 2, unit: "ss" },
            { name: "Cashewnøtter (usaltede)", amount: 20, unit: "g" },
            { name: "Fullkornsris (tørrvekt)", amount: 80, unit: "g" }
        ],
        instructions: "Wok kyllingstrimler og brokkoli i litt olje på sterk varme. Tilsett woksaus og rør inn cashewnøtter til slutt. Server med kokt fullkornsris.",
        calories: 520, protein: 38, carbs: 55, fat: 16,
        tip: "Kostråd #1: Bruk rikelig med grønnsaker i woken for vitaminer og fiber.",
        emoji: "🍲"
    },
    {
        id: "w1_lunsj", title: "Kremet pasta med laks og spinat", category: "Middag", weekNumber: 1,
        ingredients: [
            { name: "Laksefilet uten skinn", amount: 120, unit: "g" },
            { name: "Fullkornspasta", amount: 80, unit: "g" },
            { name: "Matfløte (lett)", amount: 1, unit: "dl" },
            { name: "Frisk spinat", amount: 50, unit: "g" },
            { name: "Hvitløk", amount: 1, unit: "fedd" }
        ],
        instructions: "Kok pasta. Stek lakseterninger og hakket hvitløk raskt. Tilsett matfløte og la det småkoke. Vend inn spinat og pasta til spinaten faller sammen.",
        calories: 610, protein: 32, carbs: 62, fat: 26,
        tip: "Kostråd #3: Fet fisk som laks gir viktige Omega-3 fettsyrer for leddene.",
        emoji: "🍝"
    },
    {
        id: "w1_middag", title: "Ovnsbakt laks med brokkoli og fullkornsris", category: "Middag", weekNumber: 1,
        ingredients: [
            { name: "Laksefilet uten skinn", amount: 150, unit: "g" },
            { name: "Brokkoli (i buketter)", amount: 120, unit: "g" },
            { name: "Fullkornsris (tørrvekt)", amount: 80, unit: "g" },
            { name: "Rapsolje", amount: 1, unit: "ss" }
        ],
        instructions: "Kok ris etter anvisning på pakken. Legg laksefilet og brokkoli i en ildfast form. Drypp rapsolje over, og krydre med litt salt og pepper. Bak i stekeovn på 200 °C i ca 12-15 minutter.",
        calories: 650, protein: 42, carbs: 62, fat: 22,
        tip: "Kostråd #3: Spis minst 300-450g fisk i uken, hvorav minst 200g bør være fet fisk som laks.",
        emoji: "🍣"
    },
    {
        id: "w2_frokost", title: "Stekt ris med kylling og grønnsaker", category: "Middag", weekNumber: 2,
        ingredients: [
            { name: "Kyllingfilet (i terninger)", amount: 120, unit: "g" },
            { name: "Kokt ris (kald ris)", amount: 150, unit: "g" },
            { name: "Erter og gulrøtter", amount: 100, unit: "g" },
            { name: "Egg", amount: 1, unit: "stk" },
            { name: "Soyasaus (redusert salt)", amount: 1, unit: "ss" }
        ],
        instructions: "Stek kyllingstrimler i en panne. Tilsett ris og grønnsaker, stek i noen minutter. Skyv alt til side og rør inn egget til det stivner. Smak til med soyasaus.",
        calories: 490, protein: 35, carbs: 52, fat: 12,
        tip: "Kostråd #2: Rester av ris egner seg perfekt til en rask stekt ris dagen etter.",
        emoji: "🍛"
    },
    {
        id: "w2_lunsj", title: "Quinoasalat med kyllingfilet", category: "Middag", weekNumber: 2,
        ingredients: [
            { name: "Quinoa (kokt)", amount: 150, unit: "g" },
            { name: "Grillet kyllingfilet", amount: 100, unit: "g" },
            { name: "Cherrytomater", amount: 60, unit: "g" },
            { name: "Olivenolje til dressing", amount: 1, unit: "ss" }
        ],
        instructions: "Bland kokt quinoa med kylling i strimler og delte cherrytomater. Ringle over litt olivenolje, sitronsaft, salt og pepper.",
        calories: 460, protein: 32, carbs: 45, fat: 14,
        tip: "Kostråd #3: Velg gjerne hvitt kjøtt (kylling) fremfor rødt kjøtt.",
        emoji: "🥗"
    },
    {
        id: "w2_middag", title: "Klassisk linsesuppe med grovbrød", category: "Middag", weekNumber: 2,
        ingredients: [
            { name: "Røde linser (tørre)", amount: 90, unit: "g" },
            { name: "Hakkede hermetiske tomater", amount: 200, unit: "g" },
            { name: "Gulrot og løk", amount: 100, unit: "g" },
            { name: "Grovt brød til servering", amount: 1, unit: "skive" }
        ],
        instructions: "Fres hakket løk og gulrot i litt olje. Tilsett linser, tomater og 4 dl grønnsaksbuljong. La koke i 15-20 minutter til linsene er møre. Server med brød.",
        calories: 510, protein: 26, carbs: 75, fat: 6,
        tip: "Kostråd #3: Velg bønner og linser som proteinkilder oftere.",
        emoji: "🥣"
    },
    {
        id: "w3_frokost", title: "Meksikansk bønnegryte med avokado", category: "Middag", weekNumber: 3,
        ingredients: [
            { name: "Svarte bønner (hermetiske)", amount: 150, unit: "g" },
            { name: "Hakkede tomater", amount: 200, unit: "g" },
            { name: "Avokado", amount: 0.5, unit: "stk" },
            { name: "Frisk koriander", amount: 5, unit: "g" },
            { name: "Fullkornsris (tørrvekt)", amount: 70, unit: "g" }
        ],
        instructions: "Varm bønner, hermetiske tomater og spisskummen i en kjele. Server over kokt fullkornsris og topp med ferske avokadoskiver og koriander.",
        calories: 470, protein: 16, carbs: 68, fat: 14,
        tip: "Kostråd #3: Belgfrukter og avokado gir sunt plantefett og rikelig med kostfiber.",
        emoji: "🍲"
    },
    {
        id: "w3_lunsj", title: "Kyllingwrap med tzatziki og salat", category: "Middag", weekNumber: 3,
        ingredients: [
            { name: "Fullkornswrap / tortilla", amount: 1, unit: "stk" },
            { name: "Kyllingfilet (i strimler)", amount: 120, unit: "g" },
            { name: "Mager yoghurt og agurk", amount: 100, unit: "g" },
            { name: "Blandet salat", amount: 30, unit: "g" }
        ],
        instructions: "Stek kyllingstrimlene i litt rapsolje. Bland agurkskiver og yoghurt til en enkel tzatziki. Fyll tortillaen med kylling, tzatziki og salat.",
        calories: 420, protein: 34, carbs: 36, fat: 12,
        tip: "Kostråd #4: Meieriprodukter med mindre fett anbefales til hverdags.",
        emoji: "🌯"
    },
    {
        id: "w3_middag", title: "Torsk i form med purre og poteter", category: "Middag", weekNumber: 3,
        ingredients: [
            { name: "Torskefilet", amount: 180, unit: "g" },
            { name: "Purre og tomat", amount: 120, unit: "g" },
            { name: "Kokte poteter", amount: 200, unit: "g" },
            { name: "Olivenolje", amount: 1, unit: "ss" }
        ],
        instructions: "Legg torskestykket i en form med purre og tomater. Drypp olivenolje over. Bak på 180 °C i 15 minutter. Server med kokte poteter.",
        calories: 480, protein: 38, carbs: 40, fat: 12,
        tip: "Kostråd #3: Fisk og sjømat bidrar med viktige proteiner og mineraler.",
        emoji: "🐟"
    },
    {
        id: "w4_frokost", title: "Pasta med linsebolognese", category: "Middag", weekNumber: 4,
        ingredients: [
            { name: "Røde linser (skylt)", amount: 80, unit: "g" },
            { name: "Hakkede tomater", amount: 200, unit: "g" },
            { name: "Fullkornsspagetti", amount: 80, unit: "g" },
            { name: "Løk og gulrot", amount: 80, unit: "g" }
        ],
        instructions: "Kok spagetti. Fres hakket løk og gulrot, tilsett linser og tomater, og la det småkoke i 15 minutter. Server over pastaen.",
        calories: 510, protein: 22, carbs: 85, fat: 4,
        tip: "Kostråd #2: Fullkornspasta og linser gir langvarig energi og jern.",
        emoji: "🍝"
    },
    {
        id: "w4_lunsj", title: "Kyllingsalat med kikerter og pesto", category: "Middag", weekNumber: 4,
        ingredients: [
            { name: "Kyllingfilet (i terninger)", amount: 100, unit: "g" },
            { name: "Kikerter (hermetiske)", amount: 100, unit: "g" },
            { name: "Grønn pesto", amount: 1, unit: "ss" },
            { name: "Cherrytomater", amount: 60, unit: "g" },
            { name: "Salatblader / ruccola", amount: 40, unit: "g" }
        ],
        instructions: "Stek kyllingfilet. Bland kylling, kikerter, pesto, cherrytomater og salat sammen i en stor skål.",
        calories: 440, protein: 32, carbs: 30, fat: 18,
        tip: "Kostråd #1: Grønnsaker og pesto forebygger betennelse under hard trening.",
        emoji: "🥗"
    },
    {
        id: "w4_middag", title: "Kyllingwok med grønnsaker og fullkorns-nudler", category: "Middag", weekNumber: 4,
        ingredients: [
            { name: "Kyllingfilet (i strimler)", amount: 140, unit: "g" },
            { name: "Wokgrønnsaker", amount: 150, unit: "g" },
            { name: "Fullkorns-nudler (tørr)", amount: 70, unit: "g" },
            { name: "Solsikkeolje", amount: 1, unit: "ss" }
        ],
        instructions: "Kok nudler. Stek kyllingstrimler i olje. Tilsett grønnsakene. Vend inn kokte nudler og soyasaus.",
        calories: 590, protein: 40, carbs: 68, fat: 13,
        tip: "Kostråd #3: Velg hvitt kjøtt fremfor rødt kjøtt.",
        emoji: "🍜"
    },
    {
        id: "w5_frokost", title: "Ovnsbakt torsk med søtpotetmos", category: "Middag", weekNumber: 5,
        ingredients: [
            { name: "Torskefilet", amount: 160, unit: "g" },
            { name: "Søtpotet", amount: 200, unit: "g" },
            { name: "Brokkoli", amount: 100, unit: "g" },
            { name: "Flytende margarin", amount: 1, unit: "ss" }
        ],
        instructions: "Bak torsk i ovnen på 180 °C i 15 minutter. Kok søtpotet og mos den. Server med dampet brokkoli.",
        calories: 450, protein: 34, carbs: 45, fat: 11,
        tip: "Kostråd #1: Søtpotet er rik på betakaroten.",
        emoji: "🐟"
    },
    {
        id: "w5_lunsj", title: "Laksesalat med quinoa og fetaost", category: "Middag", weekNumber: 5,
        ingredients: [
            { name: "Laksefilet (stekt)", amount: 100, unit: "g" },
            { name: "Quinoa (kokt)", amount: 100, unit: "g" },
            { name: "Fetaost", amount: 20, unit: "g" },
            { name: "Agurk og tomat", amount: 80, unit: "g" }
        ],
        instructions: "Stek laksen. Bland stekt laks, quinoa, fetaostterninger, agurk og tomat i en bolle.",
        calories: 530, protein: 28, carbs: 36, fat: 25,
        tip: "Kostråd #3: Laks gir Omega-3 fettsyrer.",
        emoji: "🥗"
    },
    {
        id: "w5_middag", title: "Bønnetaco med guacamole", category: "Middag", weekNumber: 5,
        ingredients: [
            { name: "Svarte bønner (hermetiske)", amount: 150, unit: "g" },
            { name: "Grove tortillas", amount: 2, unit: "stk" },
            { name: "Avokado til guacamole", amount: 0.5, unit: "stk" },
            { name: "Salat, tomat og mais", amount: 100, unit: "g" }
        ],
        instructions: "Varm bønner med tacokrydder. Fyll tortillas med bønner, grønnsaker og guacamole.",
        calories: 540, protein: 18, carbs: 64, fat: 20,
        tip: "Kostråd #3: Kutt ned på rødt kjøtt og velg næringsrike bønner.",
        emoji: "🌮"
    },
    {
        id: "w6_frokost", title: "Hjemmelaget sunn fiskegrateng", category: "Middag", weekNumber: 6,
        ingredients: [
            { name: "Torskefilet", amount: 120, unit: "g" },
            { name: "Fullkornsmakaroni", amount: 50, unit: "g" },
            { name: "Mager hvit saus", amount: 1.5, unit: "dl" },
            { name: "Strøkavring", amount: 10, unit: "g" }
        ],
        instructions: "Kok makaroni. Bland torsk, makaroni og saus i en form, dryss kavring. Stek på 200 °C i 25 minutter.",
        calories: 480, protein: 32, carbs: 48, fat: 12,
        tip: "Kostråd #3: Torsk bidrar med jod og magre proteiner.",
        emoji: "🍲"
    },
    {
        id: "w6_lunsj", title: "Pasta carbonara med kalkunbacon", category: "Middag", weekNumber: 6,
        ingredients: [
            { name: "Fullkornspasta", amount: 80, unit: "g" },
            { name: "Kalkunbacon", amount: 50, unit: "g" },
            { name: "Egg", amount: 1, unit: "stk" },
            { name: "Parmesan (revet)", amount: 10, unit: "g" },
            { name: "Lettmelk", amount: 2, unit: "ss" }
        ],
        instructions: "Kok pasta. Stek bacon. Pisk egg, parmesan og melk. Bland avrent varm pasta med bacon, fjern fra varme, rør i eggblandingen.",
        calories: 540, protein: 28, carbs: 62, fat: 16,
        tip: "Kostråd #4: Bruk magre kjøttalternativer.",
        emoji: "🍝"
    },
    {
        id: "w6_middag", title: "Stekt sei med råkost og potet", category: "Middag", weekNumber: 6,
        ingredients: [
            { name: "Seifilet", amount: 160, unit: "g" },
            { name: "Råkostsalat", amount: 130, unit: "g" },
            { name: "Kokt potet", amount: 200, unit: "g" },
            { name: "Flytende margarin", amount: 1, unit: "ss" }
        ],
        instructions: "Vend sei i mel, salt, pepper. Stek i margarin i 3-4 minutter på hver side. Server med råkostsalat og kokte poteter.",
        calories: 520, protein: 36, carbs: 46, fat: 15,
        tip: "Kostråd #3: Sei er en mager proteinkilde, rik på jod.",
        emoji: "🐟"
    },
    {
        id: "w7_frokost", title: "Linsesuppe med lett kokosmelk", category: "Middag", weekNumber: 7,
        ingredients: [
            { name: "Røde linser (tørre)", amount: 80, unit: "g" },
            { name: "Kokosmelk (lett)", amount: 1, unit: "dl" },
            { name: "Hermetiske tomater", amount: 150, unit: "g" },
            { name: "Ingefær og hvitløk", amount: 5, unit: "g" },
            { name: "Gulrot", amount: 1, unit: "stk" }
        ],
        instructions: "Fres ingefær/hvitløk. Tilsett gulrot, skylte linser, tomater og kokosmelk. La småkoke i 20 minutter.",
        calories: 430, protein: 18, carbs: 48, fat: 14,
        tip: "Kostråd #3: Ingefær og hvitløk styrker immunforsvaret.",
        emoji: "🥣"
    },
    {
        id: "w7_lunsj", title: "Speltlompe-pizza med skinke og mozzarella", category: "Middag", weekNumber: 7,
        ingredients: [
            { name: "Speltlomper", amount: 3, unit: "stk" },
            { name: "Tomatpuré / pizzasaus", amount: 3, unit: "ss" },
            { name: "Kokt skinke", amount: 60, unit: "g" },
            { name: "Revet mozzarella", amount: 40, unit: "g" },
            { name: "Oregano", amount: 2, unit: "g" }
        ],
        instructions: "Legg lompene på bakepapir. Smør saus, fordel skinke og mozzarella. Stek i ovnen på 200 °C i 8-10 minutter.",
        calories: 390, protein: 28, carbs: 38, fat: 11,
        tip: "Kostråd #2: Speltlompe-pizza er fiberrik og kjapp.",
        emoji: "🍕"
    },
    {
        id: "w7_middag", title: "Kyllinggryte med rotgrønnsaker", category: "Middag", weekNumber: 7,
        ingredients: [
            { name: "Kyllingfilet (i terninger)", amount: 150, unit: "g" },
            { name: "Søtpotet, gulrot og kålrot", amount: 180, unit: "g" },
            { name: "Hakkede tomater", amount: 200, unit: "g" },
            { name: "Olivenolje", amount: 1, unit: "ss" }
        ],
        instructions: "Fres kyllingterninger i olje. Tilsett grønnsaker og tomater og 2 dl vann. La gryten småkoke i 20 minutter.",
        calories: 550, protein: 38, carbs: 55, fat: 14,
        tip: "Kostråd #3: Kylling og rotgrønnsaker gir utmerket restitusjon.",
        emoji: "🍲"
    },
    {
        id: "w8_frokost", title: "Havrepannekaker med speilegg og kalkun", category: "Middag", weekNumber: 8,
        ingredients: [
            { name: "Havremel", amount: 80, unit: "g" },
            { name: "Egg", amount: 2, unit: "stk" },
            { name: "Lettmelk", amount: 1.5, unit: "dl" },
            { name: "Kalkunpålegg", amount: 4, unit: "stk" }
        ],
        instructions: "Lag røre av havremel, ett egg og melk. Stek pannekaker. Stek det andre egget. Server sammen.",
        calories: 460, protein: 26, carbs: 56, fat: 12,
        tip: "Kostråd #2: Protein og fiber gir jevn energifrigjøring.",
        emoji: "🥞"
    },
    {
        id: "w8_lunsj", title: "Kylling- og quinoasalat med rapsolje", category: "Middag", weekNumber: 8,
        ingredients: [
            { name: "Grillet kyllingfilet", amount: 100, unit: "g" },
            { name: "Quinoa (kokt)", amount: 120, unit: "g" },
            { name: "Paprika og agurk", amount: 80, unit: "g" },
            { name: "Rapsolje og sitron", amount: 1, unit: "ss" }
        ],
        instructions: "Bland kylling, grønnsaker og kokt quinoa i en skål, og ringle over rapsolje og sitron.",
        calories: 450, protein: 30, carbs: 42, fat: 14,
        tip: "Kostråd #1: Quinoa inneholder komplette aminosyrer.",
        emoji: "🥗"
    },
    {
        id: "w8_middag", title: "Stekt ørret med agurksalat og potet", category: "Middag", weekNumber: 8,
        ingredients: [
            { name: "Ørretfilet", amount: 150, unit: "g" },
            { name: "Agurk (i tynne skiver)", amount: 100, unit: "g" },
            { name: "Kokte poteter", amount: 180, unit: "g" },
            { name: "Lettrømme (10%)", amount: 2, unit: "ss" }
        ],
        instructions: "Stek eller bak ørreten. Lag en agurksalat med eddik, vann, salt. Server med kokte poteter og lettrømme.",
        calories: 620, protein: 36, carbs: 42, fat: 28,
        tip: "Kostråd #3: Ørret gir sunne fettsyrer som beskytter ledd og hjerte.",
        emoji: "🐟"
    },
    {
        id: "w9_frokost", title: "Chili con carne med kylling og kikerter", category: "Middag", weekNumber: 9,
        ingredients: [
            { name: "Kyllingkjøttdeig", amount: 120, unit: "g" },
            { name: "Kikerter (hermetiske)", amount: 100, unit: "g" },
            { name: "Hakkede tomater", amount: 200, unit: "g" },
            { name: "Krydderier", amount: 2, unit: "g" },
            { name: "Fullkornsris (tørrvekt)", amount: 70, unit: "g" }
        ],
        instructions: "Stek kjøttdeigen. Ha i tomater, kikerter og krydder, la småkoke i 10 minutter. Server med kokt ris.",
        calories: 580, protein: 38, carbs: 68, fat: 12,
        tip: "Kostråd #3: Kyllingkjøttdeig er fettfattig restitusjonsmat.",
        emoji: "🍲"
    },
    {
        id: "w9_lunsj", title: "Laksewrap med avokadokrem", category: "Middag", weekNumber: 9,
        ingredients: [
            { name: "Grov tortilla / wrap", amount: 1, unit: "stk" },
            { name: "Røkt laks (i skiver)", amount: 60, unit: "g" },
            { name: "Avokado", amount: 0.5, unit: "stk" },
            { name: "Frisk spinat", amount: 30, unit: "g" }
        ],
        instructions: "Mos avokado med sitron. Smør kremen på tortillaen, legg på laks og spinat, rull sammen.",
        calories: 410, protein: 20, carbs: 32, fat: 22,
        tip: "Kostråd #3: Laks og avokado smører leddene med sunt, umettet fett.",
        emoji: "🌯"
    },
    {
        id: "w9_middag", title: "Søtpotetsuppe med sprøstekte kikerter", category: "Middag", weekNumber: 9,
        ingredients: [
            { name: "Søtpotet (i terninger)", amount: 200, unit: "g" },
            { name: "Gulrot (i biter)", amount: 100, unit: "g" },
            { name: "Kikerter (hermetiske)", amount: 80, unit: "g" },
            { name: "Olivenolje", amount: 1, unit: "ss" }
        ],
        instructions: "Kok søtpotet og gulrot. Kjør glatt. Ovnsbak kikerter sprø med litt olje. Topp suppen med kikertene.",
        calories: 540, protein: 16, carbs: 78, fat: 14,
        tip: "Kostråd #1: Søtpotet gir rikelig med vitamin A og C.",
        emoji: "🍜"
    },
    {
        id: "w10_frokost", title: "Stekt sei med gyllen løk og potetmos", category: "Middag", weekNumber: 10,
        ingredients: [
            { name: "Seifilet", amount: 150, unit: "g" },
            { name: "Løk", amount: 1, unit: "stk" },
            { name: "Poteter", amount: 200, unit: "g" },
            { name: "Lettmelk", amount: 0.5, unit: "dl" }
        ],
        instructions: "Stek sei og løk. Kok poteter og mos med melk. Server sammen.",
        calories: 460, protein: 34, carbs: 48, fat: 10,
        tip: "Kostråd #3: Sei gir masse fullverdig jod og protein.",
        emoji: "🐟"
    },
    {
        id: "w10_lunsj", title: "Tuna melt (varmt tunfisksmørbrød)", category: "Middag", weekNumber: 10,
        ingredients: [
            { name: "Grovt brød", amount: 2, unit: "skiver" },
            { name: "Tunfisk i vann (boks)", amount: 90, unit: "g" },
            { name: "Lettmajones", amount: 1, unit: "ss" },
            { name: "Revet lettost", amount: 30, unit: "g" }
        ],
        instructions: "Bland tunfisk og majones. Fordel på brød, legg ost, gratiner i stekeovn på 200 °C.",
        calories: 480, protein: 35, carbs: 32, fat: 18,
        tip: "Kostråd #3: Tunfisk er en klassiker for aktive løpere.",
        emoji: "🥪"
    },
    {
        id: "w10_middag", title: "Lakseburgere i grovt burgerbrød", category: "Middag", weekNumber: 10,
        ingredients: [
            { name: "Lakseburger", amount: 130, unit: "g" },
            { name: "Grovt burgerbrød", amount: 1, unit: "stk" },
            { name: "Salat / råkost", amount: 50, unit: "g" },
            { name: "Mager hvitløksdressing", amount: 1, unit: "ss" }
        ],
        instructions: "Stek lakseburger i 3 minutter på hver side. Server i oppvarmet burgerbrød med salat og dressing.",
        calories: 530, protein: 32, carbs: 48, fat: 19,
        tip: "Kostråd #3: Lakseburger sikrer gode marine Omega-3 fettsyrer.",
        emoji: "🍔"
    },
    {
        id: "w11_frokost", title: "Pasta med kylling, hvitløk og tomat", category: "Middag", weekNumber: 11,
        ingredients: [
            { name: "Fullkornspasta", amount: 80, unit: "g" },
            { name: "Kyllingfilet (i biter)", amount: 120, unit: "g" },
            { name: "Hermetiske tomater", amount: 150, unit: "g" },
            { name: "Hvitløk og olje", amount: 1, unit: "ss" }
        ],
        instructions: "Kok pasta. Stek kylling og hvitløk i olje. Tilsett tomater. Bland med pastaen.",
        calories: 530, protein: 36, carbs: 62, fat: 12,
        tip: "Kostråd #2: Fullkornspasta fyller opp glykogenlagrene.",
        emoji: "🍝"
    },
    {
        id: "w11_lunsj", title: "Kylling quesadilla med spinat og ost", category: "Middag", weekNumber: 11,
        ingredients: [
            { name: "Grove tortillas", amount: 2, unit: "stk" },
            { name: "Grillet kyllingfilet", amount: 80, unit: "g" },
            { name: "Revet lettost", amount: 40, unit: "g" },
            { name: "Salsa", amount: 2, unit: "ss" }
        ],
        instructions: "Legg kylling, ost og salsa på en tortilla, dekk med den andre. Stek i tørr panne til osten smelter.",
        calories: 460, protein: 32, carbs: 38, fat: 15,
        tip: "Kostråd #4: Velg magre meieriprodukter.",
        emoji: "🌮"
    },
    {
        id: "w11_middag", title: "Fullkornsspagetti med kyllingkjøttdeig", category: "Middag", weekNumber: 11,
        ingredients: [
            { name: "Fullkornsspagetti", amount: 85, unit: "g" },
            { name: "Kyllingkjøttdeig", amount: 130, unit: "g" },
            { name: "Tomatsaus", amount: 150, unit: "g" },
            { name: "Revet parmesan", amount: 1, unit: "ss" }
        ],
        instructions: "Kok spagetti. Stek kyllingkjøttdeig. Tilsett saus. Topp med parmesan.",
        calories: 640, protein: 42, carbs: 78, fat: 14,
        tip: "Kostråd #3: Kyllingkjøttdeig er et magrere alternativ til storfekjøttdeig.",
        emoji: "🍝"
    },
    {
        id: "w12_frokost", title: "Matomelett med skinke, tomat og grovbrød", category: "Middag", weekNumber: 12,
        ingredients: [
            { name: "Egg", amount: 3, unit: "stk" },
            { name: "Kokt skinke", amount: 50, unit: "g" },
            { name: "Cherrytomater", amount: 50, unit: "g" },
            { name: "Grovbrød", amount: 1, unit: "skive" }
        ],
        instructions: "Visp egg. Stek skinke og tomater, hell eggene over. Stek under lokk. Server med brød.",
        calories: 440, protein: 30, carbs: 24, fat: 22,
        tip: "Kostråd #1: Egg gir førsteklasses proteiner som støtter muskelreparasjon.",
        emoji: "🍳"
    },
    {
        id: "w12_lunsj", title: "Fisketaco med torsk og mangosalsa", category: "Middag", weekNumber: 12,
        ingredients: [
            { name: "Torskefilet (i biter)", amount: 120, unit: "g" },
            { name: "Tacoskjell", amount: 2, unit: "stk" },
            { name: "Mango og løk", amount: 60, unit: "g" },
            { name: "Lime", amount: 0.5, unit: "stk" }
        ],
        instructions: "Stek torsk med tacokrydder. Bland mango, løk og koriander med lime. Fyll tacoskjellene.",
        calories: 410, protein: 26, carbs: 48, fat: 10,
        tip: "Kostråd #3: Fisketaco er sunn og næringsrik mat.",
        emoji: "🌮"
    },
    {
        id: "w12_middag", title: "Biffstrimler med ovnsbakt søtpotet", category: "Middag", weekNumber: 12,
        ingredients: [
            { name: "Ytrefilet av storfe", amount: 120, unit: "g" },
            { name: "Søtpotet (i båter)", amount: 180, unit: "g" },
            { name: "Brokkolini", amount: 100, unit: "g" },
            { name: "Olivenolje", amount: 1, unit: "ss" }
        ],
        instructions: "Bak søtpotetbåter vendt i olje på 200 °C i 25 minutter. Stek biffstrimlene raskt. Damp brokkolini og server.",
        calories: 580, protein: 36, carbs: 48, fat: 20,
        tip: "Kostråd #3: Spis høyst 350g rødt kjøtt i uken. 120g biff ytrefilet passer ypperlig i helgen.",
        emoji: "🥩"
    }
];

function getCalendarWeekAndYear(d) {
    // Copy date so we don't modify the original, working in UTC to avoid DST shifts
    const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
    // Set to nearest Thursday: current date + 4 - current day number (make Sunday = 7)
    const dayNum = date.getUTCDay() || 7;
    date.setUTCDate(date.getUTCDate() + 4 - dayNum);
    // Get first day of the year
    const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
    // Calculate full weeks to nearest Thursday
    const week = Math.ceil((((date - yearStart) / 86400000) + 1) / 7);
    return { week, year: date.getUTCFullYear() };
}

function getActiveTrainingWeek() {
    const today = new Date();
    return getCalendarWeekAndYear(today).week;
}

function handleScalePortionsChange(e) {
    appState.scalePortions = e.target.checked;
    renderDietSection();
}

function formatScaledQuantity(amount) {
    if (!appState.scalePortions) return amount;
    const factor = appState.readOnly ? 1.0 : (appState.weight / 70.0);
    const scaled = amount * factor;
    if (scaled % 1 === 0) return scaled;
    return parseFloat(scaled.toFixed(1));
}

function switchDietTab(tabName) {
    appState.activeDietTab = tabName;
    elements.btnDietTabWeekVp.classList.toggle('active', tabName === 'week');
    elements.btnDietTabFavoritesVp.classList.toggle('active', tabName === 'favorites');
    elements.btnDietTabAllVp.classList.toggle('active', tabName === 'all');
    renderMealsList();
}

function renderDietSection() {
    if (appState.activeTab !== 'diet') return;
    
    const calendarWeek = getActiveTrainingWeek();
    elements.dietAdviceWeek.innerText = calendarWeek;
    
    // Check workouts context in next 7 days for advice
    const todayStart = new Date();
    todayStart.setHours(0,0,0,0);
    const todayStartMs = todayStart.getTime();
    const oneWeekLaterMs = todayStartMs + 7 * 24 * 60 * 60 * 1000;
    
    const activeWeekWorkouts = appState.workouts.filter(w => {
        const time = new Date(w.scheduledDate).getTime();
        return time >= todayStartMs && time < oneWeekLaterMs;
    });
    
    let advice = "Uken har moderate økter eller hviledager. Spis balansert i tråd med Kostrådene. Velg råvarebasert mat og drikk vann som tørstedrikk.";
    
    const hasLongRun = activeWeekWorkouts.some(w => parseFloat(w.distance || 0) >= 15);
    const hasIntense = activeWeekWorkouts.some(w => {
        const type = (w.workoutType || "").toUpperCase();
        return type.includes("INTERVAL") || type.includes("TEMPO");
    });
    
    if (hasLongRun) {
        advice = "Uken inneholder en langtur på over 15 km. Sørg for å fylle karbohydratlagrene før økten, gjerne med laks og fullkornsris eller pasta dagen i forveien.";
    } else if (hasIntense) {
        advice = "Uken har planlagte intervaller eller tempoøkter. Prioriter magre proteiner (kyllingfilet, egg, mager meieri) for optimal muskelreparasjon etter øktene.";
    }
    
    elements.dietAdviceBannerText.innerText = advice;
    elements.dietSwitchScale.checked = appState.scalePortions;
    
    // Scale text
    if (appState.readOnly) {
        elements.dietProfileDescLbl.innerText = "Profil: Eksempel (70 kg standard)";
        elements.dietScaleFactorBadge.innerText = "Skalering: 1.0x";
    } else {
        const factor = (appState.weight / 70.0).toFixed(1);
        elements.dietProfileDescLbl.innerText = `Profil: Aktiv løper (${appState.weight} kg, ${appState.age} år)`;
        elements.dietScaleFactorBadge.innerText = `Skalering: ${factor}x`;
    }
    
    renderMealsList();
}

function renderMealsList() {
    const container = elements.dietMealsListVp;
    container.innerHTML = '';
    
    const activeWeek = getActiveTrainingWeek();
    const displayWeek = ((activeWeek - 1) % 12) + 1;
    
    let filtered = [];
    if (appState.activeDietTab === 'week') {
        filtered = MEALS.filter(m => m.weekNumber === displayWeek);
        elements.dietEmptyRecipesState.style.display = 'none';
        container.style.display = 'flex';
    } else if (appState.activeDietTab === 'favorites') {
        filtered = MEALS.filter(m => appState.favoriteMeals.includes(m.id));
        if (filtered.length === 0) {
            elements.dietEmptyRecipesState.style.display = 'block';
            container.style.display = 'none';
            return;
        } else {
            elements.dietEmptyRecipesState.style.display = 'none';
            container.style.display = 'flex';
        }
    } else {
        filtered = MEALS;
        elements.dietEmptyRecipesState.style.display = 'none';
        container.style.display = 'flex';
    }
    
    filtered.forEach(meal => {
        const isFav = appState.favoriteMeals.includes(meal.id);
        const card = document.createElement('div');
        card.className = 'meal-card';
        
        const ingredientsHtml = meal.ingredients.map(ing => `
            <li>
                <span class="ing-name">${ing.name}</span>
                <span class="ing-amount"><code>${formatScaledQuantity(ing.amount)}</code> ${ing.unit}</span>
            </li>
        `).join('');
        
        let dinnerNum = "Middag";
        if (meal.id.endsWith('_frokost')) dinnerNum = "Middag 1";
        else if (meal.id.endsWith('_lunsj')) dinnerNum = "Middag 2";
        else if (meal.id.endsWith('_middag')) dinnerNum = "Middag 3";
        
        const weekDisplayNum = (appState.activeDietTab === 'week') ? activeWeek : meal.weekNumber;
        
        card.innerHTML = `
            <div class="meal-card-header">
                <span class="meal-emoji">${meal.emoji}</span>
                <div class="meal-category-badge">${dinnerNum} • Uke ${weekDisplayNum}</div>
                <button class="meal-favorite-btn ${isFav ? 'active' : ''}" onclick="toggleFavoriteMeal('${meal.id}', event)">
                    <i class="fa-solid fa-star"></i>
                </button>
            </div>
            <div class="meal-card-body">
                <h4 class="meal-title">${meal.title}</h4>
                <div class="meal-macros">
                    <div class="macro-badge cals">
                        <span class="macro-num">${Math.round(appState.scalePortions ? meal.calories * (appState.weight/70.0) : meal.calories)}</span>
                        <span class="macro-label">kcal</span>
                    </div>
                    <div class="macro-badge protein">
                        <span class="macro-num">${Math.round(appState.scalePortions ? meal.protein * (appState.weight/70.0) : meal.protein)}g</span>
                        <span class="macro-label">Prot</span>
                    </div>
                    <div class="macro-badge carbs">
                        <span class="macro-num">${Math.round(appState.scalePortions ? meal.carbs * (appState.weight/70.0) : meal.carbs)}g</span>
                        <span class="macro-label">Karb</span>
                    </div>
                    <div class="macro-badge fat">
                        <span class="macro-num">${Math.round(appState.scalePortions ? meal.fat * (appState.weight/70.0) : meal.fat)}g</span>
                        <span class="macro-label">Fett</span>
                    </div>
                </div>
                
                <div class="meal-tap-hint">Klikk for å vise oppskrift</div>
                
                <div class="meal-card-details">
                    <div class="meal-divider"></div>
                    <div class="meal-section-title">Ingredienser</div>
                    <ul class="meal-ingredients">
                        ${ingredientsHtml}
                    </ul>
                    <div class="meal-section-title">Tilberedning</div>
                    <p class="meal-instructions">${meal.instructions}</p>
                    ${meal.tip ? `<div class="meal-tip"><i class="fa-solid fa-circle-info"></i><span>${meal.tip}</span></div>` : ''}
                </div>
            </div>
        `;
        
        card.addEventListener('click', (e) => {
            if (e.target.closest('.meal-favorite-btn')) return;
            card.classList.toggle('expanded');
            const hint = card.querySelector('.meal-tap-hint');
            if (hint) {
                hint.innerText = card.classList.contains('expanded') ? 'Klikk for å lukke oppskrift' : 'Klikk for å vise oppskrift';
            }
        });
        container.appendChild(card);
    });
}

function toggleFavoriteMeal(mealId, event) {
    if (event) event.stopPropagation();
    if (appState.readOnly) {
        alert("Favoritter er skrivebeskyttet i delt visningsmodus.");
        return;
    }
    
    const index = appState.favoriteMeals.indexOf(mealId);
    if (index === -1) {
        appState.favoriteMeals.push(mealId);
    } else {
        appState.favoriteMeals.splice(index, 1);
    }
    
    if (db && appState.firebaseConnected) {
        db.ref(`profiles/${appState.userId}/favoriteMeals`).set(appState.favoriteMeals)
            .then(() => renderMealsList());
    } else {
        renderMealsList();
    }
}
window.toggleFavoriteMeal = toggleFavoriteMeal;

// ----------------------------------------------------
// ADD BUDDY MODAL HANDLERS
// ----------------------------------------------------
function openBuddyModal() {
    if (appState.readOnly) return;
    elements.buddyModal.classList.add('active');
    elements.buddyIdInput.value = '';
}

function closeBuddyModal() {
    elements.buddyModal.classList.remove('active');
}

// ----------------------------------------------------
// SETUP GENERATOR WIZARD MULTI-PAGE NAVIGATION
// ----------------------------------------------------
function navigateWizardNextStep() {
    navigateWizardNext();
}
window.navigateWizardNextStep = navigateWizardNextStep;

// ----------------------------------------------------
// SETUP WIZARD AUTO-POPULATION DEFAULTS FOR DAYS
// ----------------------------------------------------
// Ensure spinners values match when checking checkboxes
const daysShort = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
daysShort.forEach(day => {
    const chk = document.getElementById(`wiz-check-${day}`);
    const sel = document.getElementById(`wiz-type-${day}`);
    if (chk && sel) {
        chk.addEventListener('change', () => {
            sel.style.opacity = chk.checked ? "1" : "0.5";
            sel.disabled = !chk.checked;
        });
    }
});

// ----------------------------------------------------
// NEW WORKOUT TYPE CHANGE HANDLER
// ----------------------------------------------------
function handleWorkoutTypeChange() {
    if (elements.workoutType.value === 'INTERVALS') {
        elements.workoutIntervalSection.style.display = 'block';
    } else {
        elements.workoutIntervalSection.style.display = 'none';
    }
}

// ----------------------------------------------------
// NEW NEXT SESSION CARD CLICK HANDLER
// ----------------------------------------------------
function handleNextSessionCardClick() {
    const today = new Date();
    const todayStr = getLocalDateString(today);
    const sorted = [...appState.workouts].sort((a,b) => new Date(a.scheduledDate) - new Date(b.scheduledDate));
    let nextWorkout = null;
    
    // 1. Missed session
    for (let w of sorted) {
        if (!w.isCompleted && w.scheduledDate && w.scheduledDate < todayStr) {
            nextWorkout = w;
            break;
        }
    }
    // 2. Today's session
    if (!nextWorkout) {
        for (let w of sorted) {
            if (!w.isCompleted && w.scheduledDate && w.scheduledDate === todayStr) {
                nextWorkout = w;
                break;
            }
        }
    }
    // 3. Future session
    if (!nextWorkout) {
        for (let w of sorted) {
            if (!w.isCompleted && w.scheduledDate && w.scheduledDate > todayStr) {
                nextWorkout = w;
                break;
            }
        }
    }
    
    if (nextWorkout) {
        editWorkout(nextWorkout.id);
    } else {
        navTo('log');
    }
}

// ----------------------------------------------------
// NEW RACE INFO MODAL HANDLERS
// ----------------------------------------------------
function openRaceInfoModal() {
    elements.raceInfoModal.classList.add('active');
    
    const isReadOnly = appState.readOnly;
    elements.raceInputName.disabled = isReadOnly;
    elements.raceInputType.disabled = isReadOnly;
    elements.raceInputDate.disabled = isReadOnly;
    elements.raceInputLocation.disabled = isReadOnly;
    
    const saveBtn = document.getElementById('btn-save-race');
    if (saveBtn) saveBtn.style.display = isReadOnly ? 'none' : 'block';
    
    const modalTitle = document.getElementById('race-modal-title');
    if (modalTitle) modalTitle.innerText = isReadOnly ? 'Race Details' : 'Edit Race Details';
    
    currentUploadedGpxData = undefined;
    if (elements.gpxFileInput) elements.gpxFileInput.value = '';
    
    let raceName = "Oslo Maraton";
    let raceCategory = "Marathon";
    let raceDateStr = "";
    
    const currentRace = appState.userProfile.currentRace || "";
    if (currentRace.includes(" - ")) {
        const parts = currentRace.split(" - ");
        raceName = parts[0];
        const categoryAndDate = parts[1];
        if (categoryAndDate.includes(": ")) {
            const catParts = categoryAndDate.split(": ");
            raceCategory = catParts[0];
            raceDateStr = catParts[1];
        } else {
            raceCategory = categoryAndDate;
        }
    }
    
    elements.raceInputName.value = raceName;
    elements.raceInputType.value = raceCategory;
    elements.raceInputLocation.value = appState.userProfile.eventLocation || '';
    
    let dateInputVal = "";
    if (raceDateStr) {
        const dObj = new Date(raceDateStr);
        if (!isNaN(dObj.getTime())) {
            dateInputVal = getLocalDateString(dObj);
        }
    }
    elements.raceInputDate.value = dateInputVal;
    
    const route = appState.userProfile.gpxRoute;
    if (route) {
        initGpxMapAndChart(route);
        if (isReadOnly && elements.btnRemoveGpx) {
            elements.btnRemoveGpx.style.display = 'none';
        } else if (elements.btnRemoveGpx) {
            elements.btnRemoveGpx.style.display = 'block';
        }
    } else {
        if (elements.gpxStatusContainer) elements.gpxStatusContainer.style.display = 'none';
        if (elements.gpxViewerSection) elements.gpxViewerSection.style.display = 'none';
        if (elements.gpxUploadBox) {
            elements.gpxUploadBox.style.display = isReadOnly ? 'none' : 'block';
        }
    }
}

function closeRaceInfoModal() {
    elements.raceInfoModal.classList.remove('active');
    if (gpxMapInstance) {
        gpxMapInstance.remove();
        gpxMapInstance = null;
    }
    if (gpxChartInstance) {
        gpxChartInstance.destroy();
        gpxChartInstance = null;
    }
}

function formatRaceDate(dateStr) {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    const day = d.getDate();
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const month = months[d.getMonth()];
    const year = d.getFullYear();
    return `${day} ${month} ${year}`;
}

function handleRaceInfoSubmit(e) {
    e.preventDefault();
    if (appState.readOnly) return;
    
    const eventName = elements.raceInputName.value.trim();
    const eventType = elements.raceInputType.value;
    const dateVal = elements.raceInputDate.value;
    const location = elements.raceInputLocation.value.trim();
    
    const formattedDate = formatRaceDate(dateVal);
    const currentRaceString = `${eventName} - ${eventType}: ${formattedDate}`;
    
    const updateData = {
        currentRace: currentRaceString,
        eventLocation: location,
        lastUpdate: Date.now()
    };
    
    if (currentUploadedGpxData !== undefined) {
        updateData.gpxRoute = currentUploadedGpxData;
    }
    
    if (db && appState.firebaseConnected) {
        db.ref(`profiles/${appState.userId}`).update(updateData).then(() => {
            appState.userProfile.currentRace = currentRaceString;
            appState.userProfile.eventLocation = location;
            if (currentUploadedGpxData !== undefined) {
                appState.userProfile.gpxRoute = currentUploadedGpxData;
            }
            saveProfileLocally();
            updateProfileUI();
            closeRaceInfoModal();
        });
    } else {
        appState.userProfile.currentRace = currentRaceString;
        appState.userProfile.eventLocation = location;
        if (currentUploadedGpxData !== undefined) {
            appState.userProfile.gpxRoute = currentUploadedGpxData;
        }
        saveProfileLocally();
        updateProfileUI();
        closeRaceInfoModal();
    }
}

// ----------------------------------------------------
// GPX ROUTE MAP AND ELEVATION PROFILE INTERACTIVE LOGIC
// ----------------------------------------------------
function handleGpxFileSelect(e) {
    if (e.target.files.length > 0) {
        processGpxFile(e.target.files[0]);
    }
}

function processGpxFile(file) {
    const reader = new FileReader();
    reader.onload = (event) => {
        try {
            const xmlText = event.target.result;
            const parsedData = parseGPX(xmlText, file.name);
            currentUploadedGpxData = parsedData;
            initGpxMapAndChart(parsedData);
        } catch (err) {
            alert("Error parsing GPX file: " + err.message);
        }
    };
    reader.readAsText(file);
}

function removeUploadedGpx() {
    currentUploadedGpxData = null;
    if (elements.gpxFileInput) elements.gpxFileInput.value = '';
    if (elements.gpxStatusContainer) elements.gpxStatusContainer.style.display = 'none';
    if (elements.gpxUploadBox) elements.gpxUploadBox.style.display = 'block';
    if (elements.gpxViewerSection) elements.gpxViewerSection.style.display = 'none';
    
    if (gpxMapInstance) {
        gpxMapInstance.remove();
        gpxMapInstance = null;
    }
    if (gpxChartInstance) {
        gpxChartInstance.destroy();
        gpxChartInstance = null;
    }
}

function parseGPX(xmlText, fileName) {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, "text/xml");
    const trkpts = xmlDoc.getElementsByTagName("trkpt");
    if (trkpts.length === 0) {
        throw new Error("No trackpoints found in GPX file.");
    }
    
    const rawPoints = [];
    for (let i = 0; i < trkpts.length; i++) {
        const pt = trkpts[i];
        const lat = parseFloat(pt.getAttribute("lat"));
        const lon = parseFloat(pt.getAttribute("lon"));
        const eleNode = pt.getElementsByTagName("ele")[0];
        const ele = eleNode ? parseFloat(eleNode.textContent) : 0;
        if (!isNaN(lat) && !isNaN(lon)) {
            rawPoints.push({ lat, lon, ele });
        }
    }
    
    if (rawPoints.length === 0) {
        throw new Error("No valid coordinates found.");
    }
    
    let totalDistance = 0;
    let totalElevationGain = 0;
    const processedPoints = [];
    
    processedPoints.push([rawPoints[0].lat, rawPoints[0].lon, rawPoints[0].ele, 0]);
    
    for (let i = 1; i < rawPoints.length; i++) {
        const prev = rawPoints[i - 1];
        const curr = rawPoints[i];
        const d = haversineDistance(prev.lat, prev.lon, curr.lat, curr.lon);
        totalDistance += d;
        
        const eleDiff = curr.ele - prev.ele;
        if (eleDiff > 0) {
            totalElevationGain += eleDiff;
        }
        
        processedPoints.push([curr.lat, curr.lon, curr.ele, totalDistance]);
    }
    
    const targetPointsCount = 300;
    const downsampled = [];
    downsampled.push(processedPoints[0]);
    
    if (processedPoints.length > targetPointsCount) {
        const interval = totalDistance / (targetPointsCount - 1);
        let nextTargetDist = interval;
        
        for (let i = 1; i < processedPoints.length - 1; i++) {
            const pt = processedPoints[i];
            const dist = pt[3];
            if (dist >= nextTargetDist) {
                downsampled.push(pt);
                nextTargetDist += interval;
            }
        }
        downsampled.push(processedPoints[processedPoints.length - 1]);
    } else {
        for (let i = 1; i < processedPoints.length; i++) {
            downsampled.push(processedPoints[i]);
        }
    }
    
    const avgSlope = totalDistance > 0 ? (totalElevationGain / (totalDistance * 1000)) * 100 : 0;
    
    return {
        name: fileName.replace(/\.[^/.]+$/, ""),
        distance: Math.round(totalDistance * 100) / 100,
        elevationGain: Math.round(totalElevationGain * 10) / 10,
        avgSlope: Math.round(avgSlope * 100) / 100,
        points: downsampled
    };
}

function haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

let mapMarker = null;

function initGpxMapAndChart(gpxData) {
    if (!gpxData || !gpxData.points || gpxData.points.length === 0) return;
    
    document.getElementById('gpx-stat-distance').innerText = `${gpxData.distance} km`;
    document.getElementById('gpx-stat-gain').innerText = `${gpxData.elevationGain} m`;
    document.getElementById('gpx-stat-slope').innerText = `${gpxData.avgSlope || 0}%`;
    
    if (elements.gpxStatusText) elements.gpxStatusText.innerText = `Route: ${gpxData.name} (${gpxData.distance} km)`;
    if (elements.gpxStatusContainer) elements.gpxStatusContainer.style.display = 'flex';
    if (elements.gpxUploadBox) elements.gpxUploadBox.style.display = 'none';
    if (elements.gpxViewerSection) elements.gpxViewerSection.style.display = 'block';
    
    const coordinates = gpxData.points.map(p => [p[0], p[1]]);
    
    setTimeout(() => {
        if (gpxMapInstance) {
            gpxMapInstance.remove();
            gpxMapInstance = null;
        }
        
        gpxMapInstance = L.map('gpx-map').setView(coordinates[0], 13);
        
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OpenStreetMap & &copy; CARTO',
            maxZoom: 19
        }).addTo(gpxMapInstance);
        
        const polyline = L.polyline(coordinates, {
            color: '#ccff00', // var(--android-lime) equivalent
            weight: 4,
            opacity: 0.85
        }).addTo(gpxMapInstance);
        
        gpxMapInstance.fitBounds(polyline.getBounds(), { padding: [15, 15] });
        
        L.circleMarker(coordinates[0], { radius: 6, color: '#4ade80', fillColor: '#111', fillOpacity: 0.9, weight: 3 }).addTo(gpxMapInstance).bindPopup("Start");
        L.circleMarker(coordinates[coordinates.length - 1], { radius: 6, color: '#f87171', fillColor: '#111', fillOpacity: 0.9, weight: 3 }).addTo(gpxMapInstance).bindPopup("Finish");
        
        mapMarker = L.circleMarker(coordinates[0], {
            radius: 8,
            color: '#00e5ff', // var(--android-pace) equivalent
            fillColor: '#fff',
            fillOpacity: 1.0,
            weight: 3
        }).addTo(gpxMapInstance);
        
        gpxMapInstance.invalidateSize();
    }, 200);
    
    const distances = gpxData.points.map(p => Math.round(p[3] * 100) / 100);
    const elevations = gpxData.points.map(p => p[2]);
    
    const segmentColors = [];
    for (let i = 0; i < gpxData.points.length; i++) {
        if (i === 0) {
            segmentColors.push('#00e5ff');
            continue;
        }
        const prev = gpxData.points[i - 1];
        const curr = gpxData.points[i];
        const dDist = (curr[3] - prev[3]) * 1000;
        if (dDist > 0) {
            const dEle = curr[2] - prev[2];
            const grade = (dEle / dDist) * 100;
            if (grade > 2.5) {
                segmentColors.push('#ff4d4d');
            } else if (grade < -2.5) {
                segmentColors.push('#4ade80');
            } else {
                segmentColors.push('#00e5ff');
            }
        } else {
            segmentColors.push('#00e5ff');
        }
    }
    
    if (gpxChartInstance) {
        gpxChartInstance.destroy();
        gpxChartInstance = null;
    }
    
    const canvas = document.getElementById('gpx-elevation-chart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    gpxChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: distances,
            datasets: [{
                label: 'Elevation (m)',
                data: elevations,
                borderWidth: 2.5,
                pointRadius: 0,
                pointHoverRadius: 6,
                fill: false,
                segment: {
                    borderColor: ctx => {
                        const idx = ctx.p1DataIndex;
                        return segmentColors[idx] || '#00e5ff';
                    }
                }
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    displayColors: false,
                    callbacks: {
                        title: (items) => `Distance: ${items[0].label} km`,
                        label: (item) => `Elevation: ${Math.round(item.raw)} m`
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#666', font: { size: 9 } }
                },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                    ticks: { color: '#666', font: { size: 9 } }
                }
            },
            onHover: (event, activeElements) => {
                if (activeElements && activeElements.length > 0 && mapMarker) {
                    const idx = activeElements[0].index;
                    const pt = gpxData.points[idx];
                    if (pt) {
                        mapMarker.setLatLng([pt[0], pt[1]]);
                        if (gpxMapInstance && !gpxMapInstance.getBounds().contains(mapMarker.getLatLng())) {
                            gpxMapInstance.panTo([pt[0], pt[1]]);
                        }
                    }
                }
            }
        }
    });
}

// ==========================================
// ADMIN DASHBOARD HELPER FUNCTIONS
// ==========================================

async function sha256(message) {
    const msgBuffer = new TextEncoder().encode(message);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

async function handleAdminAccess() {
    const password = prompt("Enter Admin Passcode:");
    if (!password) return;
    try {
        const hash = await sha256(password);
        if (hash === "b90389e545f6d9381395e03cf0fda167787afb5956989bbd46f4f8276b05024f") {
            elements.adminDashboardModal.classList.add('active');
            if (elements.adminSearchInput) elements.adminSearchInput.value = "";
            loadAdminUserDirectory();
        } else {
            alert("Incorrect passcode.");
        }
    } catch (e) {
        console.error("Passcode verification failed:", e);
        alert("Passcode verification failed: " + e.message);
    }
}

let adminRunnersList = [];

function loadAdminUserDirectory() {
    if (!db || !appState.firebaseConnected) {
        if (elements.adminRunnersTableBody) {
            elements.adminRunnersTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:20px; color:#ff3b30;"><i class="fa-solid fa-triangle-exclamation"></i> Not connected to database</td></tr>`;
        }
        return;
    }
    
    if (elements.adminRunnersTableBody) {
        elements.adminRunnersTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:20px; color:rgba(255,255,255,0.5);"><i class="fa-solid fa-spinner fa-spin" style="margin-right: 6px;"></i> Loading directory...</td></tr>`;
    }
    
    db.ref('profiles').once('value')
        .then(snapshot => {
            const profiles = snapshot.val();
            adminRunnersList = [];
            
            if (profiles) {
                Object.keys(profiles).forEach(userId => {
                    const p = profiles[userId];
                    adminRunnersList.push({
                        id: userId,
                        name: p.name || 'Runner',
                        nickname: p.nickname || 'Athlete',
                        email: p.email || 'No email',
                        currentRace: p.currentRace || 'No active plan',
                        workoutsDone: parseInt(p.workoutsDone || 0),
                        workoutsTotal: parseInt(p.workoutsTotal || 0),
                        consistency: parseInt(p.consistency || 0),
                        distance: parseFloat(p.distance || 0),
                        lastUpdate: parseInt(p.lastUpdate || 0)
                    });
                });
            }
            
            // Sort by lastUpdate descending (most recently active first)
            adminRunnersList.sort((a, b) => b.lastUpdate - a.lastUpdate);
            
            // Render list
            renderAdminRunnersList(adminRunnersList);
            
            // Update aggregate stats
            updateAdminAggregateStats();
        })
        .catch(err => {
            console.error("Admin fetch failed:", err);
            if (elements.adminRunnersTableBody) {
                elements.adminRunnersTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:20px; color:#ff3b30;"><i class="fa-solid fa-triangle-exclamation"></i> Fetch failed: ${err.message}</td></tr>`;
            }
        });
}

function formatAdminTimeAgo(timestamp) {
    if (!timestamp) return '<span style="color:#888;">Never</span>';
    const seconds = Math.floor((Date.now() - timestamp) / 1000);
    if (seconds < 0) return 'Just now';
    if (seconds < 60) return 'Just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days}d ago`;
    
    const date = new Date(timestamp);
    return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
}

function getAdminStatusIndicator(timestamp) {
    if (!timestamp) return '<span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#888; margin-right:6px;"></span>';
    const diffMs = Date.now() - timestamp;
    let color = '#888';
    if (diffMs < 24 * 3600 * 1000) {
        color = '#34c759'; // Green (Active within 24h)
    } else if (diffMs < 7 * 24 * 3600 * 1000) {
        color = '#ff9500'; // Orange (Active within 7d)
    }
    return `<span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:${color}; margin-right:6px; box-shadow: 0 0 5px ${color};"></span>`;
}

function renderAdminRunnersList(list) {
    if (!elements.adminRunnersTableBody) return;
    
    if (list.length === 0) {
        elements.adminRunnersTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px; color:rgba(255,255,255,0.5);"><i class="fa-solid fa-face-frown"></i> No runners match search criteria</td></tr>`;
        return;
    }
    
    let html = '';
    list.forEach(runner => {
        const timeAgoStr = formatAdminTimeAgo(runner.lastUpdate);
        const statusDot = getAdminStatusIndicator(runner.lastUpdate);
        const goalStr = runner.currentRace || 'No active plan';
        const runsStr = runner.workoutsTotal > 0 
            ? `${runner.workoutsDone} / ${runner.workoutsTotal} <span style="color:#ff9500; font-size:0.75rem; font-weight:bold;">(${runner.consistency}%)</span>` 
            : '<span style="color:#666;">No plan</span>';
        
        const deleteBtn = runner.id === 'CH020721' 
            ? '<span style="color:#666; font-size:0.8rem;">-</span>' 
            : `<button type="button" class="btn-delete-runner-action" data-id="${runner.id}" style="background: none; border: none; color: #ff3b30; cursor: pointer; padding: 4px 8px; font-size: 0.95rem; line-height: 1;" title="Delete Runner">
                  <i class="fa-solid fa-trash-can"></i>
               </button>`;
        
        html += `
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                <td style="padding: 10px; vertical-align: middle; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <div style="font-weight: bold; color: #fff;">${escapeHtml(runner.name)}</div>
                    <div style="font-size: 0.7rem; color: rgba(255,255,255,0.5);">${escapeHtml(runner.email)}</div>
                </td>
                <td style="padding: 10px; vertical-align: middle; color: #ddd; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    ${escapeHtml(goalStr)}
                </td>
                <td style="padding: 10px; vertical-align: middle; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    ${runsStr}
                </td>
                <td style="padding: 10px; vertical-align: middle; text-align: center; white-space: nowrap; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <div style="display: flex; align-items: center; justify-content: center;">
                        ${statusDot} ${timeAgoStr}
                    </div>
                </td>
                <td style="padding: 10px; vertical-align: middle; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <code style="background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 4px; color: var(--android-lime); font-size: 0.75rem; font-family: monospace;">${escapeHtml(runner.id)}</code>
                </td>
                <td style="padding: 10px; vertical-align: middle; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05);">
                    ${deleteBtn}
                </td>
            </tr>
        `;
    });
    
    elements.adminRunnersTableBody.innerHTML = html;
}

function updateAdminAggregateStats() {
    if (!elements.adminStatTotal || !elements.adminStatActive || !elements.adminStatRuns) return;
    
    const total = adminRunnersList.length;
    let active = 0;
    let runs = 0;
    const sevenDaysAgo = Date.now() - 7 * 24 * 3600 * 1000;
    
    adminRunnersList.forEach(r => {
        if (r.lastUpdate && r.lastUpdate >= sevenDaysAgo) {
            active++;
        }
        runs += r.workoutsDone;
    });
    
    elements.adminStatTotal.innerText = total;
    elements.adminStatActive.innerText = active;
    elements.adminStatRuns.innerText = runs;
}

function filterAdminRunnersList() {
    if (!elements.adminSearchInput) return;
    const query = elements.adminSearchInput.value.toLowerCase().trim();
    
    if (!query) {
        renderAdminRunnersList(adminRunnersList);
        return;
    }
    
    const filtered = adminRunnersList.filter(runner => {
        return runner.name.toLowerCase().includes(query) ||
               runner.nickname.toLowerCase().includes(query) ||
               runner.email.toLowerCase().includes(query) ||
               runner.id.toLowerCase().includes(query) ||
               runner.currentRace.toLowerCase().includes(query);
    });
    
    renderAdminRunnersList(filtered);
}

function handleDeleteRunner(runnerId) {
    if (runnerId === 'CH020721') {
        alert("You cannot delete the primary admin account!");
        return;
    }
    
    if (!confirm(`Are you sure you want to permanently delete Runner ID: ${runnerId}?\nThis will delete their profile, workouts, and shoes from the database.`)) {
        return;
    }
    
    if (!db || !appState.firebaseConnected) return;
    
    // Show temporary inline feedback
    if (elements.adminRunnersTableBody) {
        elements.adminRunnersTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px; color:#ff9500;"><i class="fa-solid fa-spinner fa-spin" style="margin-right: 6px;"></i> Deleting runner ${runnerId}...</td></tr>`;
    }
    
    // Remove related profile, workouts, and shoes nodes
    Promise.all([
        db.ref(`profiles/${runnerId}`).remove(),
        db.ref(`workouts/${runnerId}`).remove(),
        db.ref(`shoes/${runnerId}`).remove()
    ])
    .then(() => {
        alert(`Runner ID ${runnerId} has been successfully deleted.`);
        loadAdminUserDirectory();
    })
    .catch(err => {
        console.error("Failed to delete runner:", err);
        alert(`Failed to delete runner: ${err.message}`);
        loadAdminUserDirectory();
    });
}

// ====================================================
// STRAVA SYNC INTEGRATION
// ====================================================

const STRAVA_CONFIG = {
    clientId: '270755', 
    clientSecret: '9b9f7ba496b93180e1624405e4e4872c12a22cfb',
    redirectUri: window.location.origin + window.location.pathname
};

let stravaTokens = {
    accessToken: null,
    refreshToken: null,
    expiresAt: null
};

function checkStravaCallback() {
    if (appState.readOnly) return;
    
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const error = urlParams.get('error');
    
    // Load existing credentials cache
    const savedTokens = localStorage.getItem('strava_tokens');
    if (savedTokens) {
        try {
            stravaTokens = JSON.parse(savedTokens);
            updateStravaUI(true);
        } catch (e) {
            console.error("Failed to parse Strava token cache:", e);
        }
    }
    
    // Handle user declining authorization
    if (error === 'access_denied') {
        window.history.replaceState({}, document.title, window.location.pathname);
        alert("Strava authorization request was declined.");
        updateStravaUI(false);
        return;
    }
    
    if (code) {
        // Clean URL parameters immediately to prevent multiple exchange triggers
        window.history.replaceState({}, document.title, window.location.pathname);
        exchangeStravaCode(code);
    }
}

function exchangeStravaCode(code) {
    if (elements.stravaStatusBadge) {
        elements.stravaStatusBadge.innerText = "Connecting...";
    }
    
    const body = new URLSearchParams({
        client_id: STRAVA_CONFIG.clientId,
        client_secret: STRAVA_CONFIG.clientSecret,
        code: code,
        grant_type: 'authorization_code'
    });
    
    fetch('https://www.strava.com/oauth/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body
    })
    .then(res => res.json())
    .then(data => {
        if (data.access_token) {
            stravaTokens = {
                accessToken: data.access_token,
                refreshToken: data.refresh_token,
                expiresAt: data.expires_at * 1000 // Convert to ms
            };
            localStorage.setItem('strava_tokens', JSON.stringify(stravaTokens));
            updateStravaUI(true);
            alert("Strava successfully connected!");
            syncStravaActivities();
        } else {
            throw new Error(data.message || "Invalid authentication response.");
        }
    })
    .catch(err => {
        console.error("Strava token exchange failed:", err);
        alert("Failed to connect to Strava: " + err.message);
        updateStravaUI(false);
    });
}

async function getValidStravaToken() {
    if (!stravaTokens.accessToken) return null;
    
    // Refresh token if it expires in less than 5 minutes
    if (Date.now() < (stravaTokens.expiresAt - 5 * 60 * 1000)) {
        return stravaTokens.accessToken;
    }
    
    const body = new URLSearchParams({
        client_id: STRAVA_CONFIG.clientId,
        client_secret: STRAVA_CONFIG.clientSecret,
        grant_type: 'refresh_token',
        refresh_token: stravaTokens.refreshToken
    });
    
    try {
        const res = await fetch('https://www.strava.com/oauth/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        });
        const data = await res.json();
        if (data.access_token) {
            stravaTokens = {
                accessToken: data.access_token,
                refreshToken: data.refresh_token,
                expiresAt: data.expires_at * 1000
            };
            localStorage.setItem('strava_tokens', JSON.stringify(stravaTokens));
            return stravaTokens.accessToken;
        }
    } catch (e) {
        console.error("Failed to refresh expired Strava token:", e);
    }
    return null;
}

function updateStravaUI(connected) {
    if (!elements.btnStravaAction || !elements.stravaStatusBadge) return;
    
    if (connected) {
        elements.stravaStatusBadge.innerText = "Connected";
        elements.stravaStatusBadge.style.background = "rgba(52, 199, 89, 0.2)";
        elements.stravaStatusBadge.style.color = "#34c759";
        
        elements.btnStravaAction.innerHTML = `<i class="fa-solid fa-rotate"></i> SYNC FROM STRAVA`;
        elements.btnStravaAction.style.background = "#FC5200";
    } else {
        elements.stravaStatusBadge.innerText = "Disconnected";
        elements.stravaStatusBadge.style.background = "rgba(255,255,255,0.1)";
        elements.stravaStatusBadge.style.color = "rgba(255,255,255,0.6)";
        
        elements.btnStravaAction.innerHTML = `<i class="fa-brands fa-strava"></i> CONNECT WITH STRAVA`;
        elements.btnStravaAction.style.background = "#FC5200";
    }
}

async function handleStravaAction() {
    if (appState.readOnly) return;
    
    if (!stravaTokens.accessToken) {
        // Direct user to Strava authorize page
        const authUrl = `https://www.strava.com/oauth/authorize?client_id=${STRAVA_CONFIG.clientId}&redirect_uri=${encodeURIComponent(STRAVA_CONFIG.redirectUri)}&response_type=code&scope=activity:read_all`;
        window.location.href = authUrl;
    } else {
        syncStravaActivities();
    }
}

async function syncStravaActivities() {
    if (appState.readOnly) return;
    
    const token = await getValidStravaToken();
    if (!token) {
        alert("Strava login session has expired. Please connect again.");
        localStorage.removeItem('strava_tokens');
        stravaTokens = { accessToken: null, refreshToken: null, expiresAt: null };
        updateStravaUI(false);
        return;
    }
    
    if (elements.btnStravaAction) {
        elements.btnStravaAction.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> SYNCING...`;
    }
    
    // Query activities for the last 7 days
    const epochOneWeekAgo = Math.floor((Date.now() - 7 * 24 * 3600 * 1000) / 1000);
    
    fetch(`https://www.strava.com/api/v3/athlete/activities?after=${epochOneWeekAgo}&per_page=100`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => res.json())
    .then(activities => {
        if (Array.isArray(activities)) {
            const syncReport = processStravaActivities(activities);
            alert(`Sync Finished!\n\nMatched Workouts: ${syncReport.matched}\nRuns Skipped (Already Sync): ${syncReport.skipped}`);
        } else if (activities && activities.message) {
            throw new Error(activities.message);
        } else {
            throw new Error("Strava returned an invalid response format.");
        }
    })
    .catch(err => {
        console.error("Sync failed:", err);
        alert("Failed to sync: " + err.message);
    })
    .finally(() => {
        updateStravaUI(!!stravaTokens.accessToken);
    });
}

function parseLocalDateString(dateStr) {
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        // Zero-based index month index
        return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
    }
    return new Date(dateStr);
}

function isWorkoutRunnable(w) {
    const type = (w.workoutType || '').toUpperCase();
    return type !== 'STRENGTH & CORE' && type !== 'REST';
}

function processStravaActivities(activities) {
    let matchedCount = 0;
    let skippedCount = 0;
    let dbUpdated = false;
    
    // Filter out hikes/rides/swims. Capture Run, VirtualRun, and Treadmills
    const runs = activities.filter(a => {
        const type = a.type || '';
        const sportType = a.sport_type || '';
        const isRunType = type === 'Run' || type === 'VirtualRun' || type === 'Treadmill';
        const isSportType = sportType === 'Run' || sportType === 'VirtualRun' || sportType === 'Treadmill';
        return isRunType || isSportType;
    });
    
    runs.forEach(run => {
        const runId = String(run.id);
        
        // 1. Duplicate protection check
        const alreadySynced = appState.workouts.some(w => String(w.stravaActivityId) === runId);
        if (alreadySynced) {
            skippedCount++;
            return;
        }
        
        // 2. Perform localized date proximity matching
        const runDate = new Date(run.start_date_local);
        const activityLocalDateStr = getLocalDateString(runDate);
        const match = findMatchingWorkout(activityLocalDateStr, run.distance / 1000);
        
        if (match) {
            // Backup the original target guidelines in notes before writing
            const originalTargetText = `Original Target: ${match.distance}km @ ${match.pace || "N/A"}`;
            const previousWorkoutState = { ...match };
            
            // Overwrite details inside real schema properties
            match.isCompleted = true;
            match.distance = parseFloat((run.distance / 1000).toFixed(2)) || 0;
            match.totalDuration = Math.round(run.moving_time / 60) || 0;
            match.pace = formatStravaPace(run.average_speed);
            match.avgHeartRate = run.average_heartrate ? Math.round(run.average_heartrate) : 0;
            match.stravaActivityId = runId;
            
            // Combined title description formatting
            match.description = `${match.description || ''} (${run.name})`;
            
            // Build rich metadata block inside Notes
            let extraNotes = `\n\n--- Strava Sync ---\n${originalTargetText}\nDuration: ${formatStravaDuration(run.moving_time)}`;
            if (run.total_elevation_gain) extraNotes += `\nElevation Gain: ${run.total_elevation_gain}m`;
            if (run.average_cadence) extraNotes += `\nAvg Cadence: ${Math.round(run.average_cadence * 2)} spm`;
            
            match.notes = (match.notes || "") + extraNotes;
            
            // 3. Write to database (Firebase or Local)
            if (db && appState.firebaseConnected) {
                db.ref(`workouts/${appState.userId}/workout_${match.id}`).update({
                    isCompleted: true,
                    distance: match.distance,
                    totalDuration: match.totalDuration,
                    pace: match.pace,
                    avgHeartRate: match.avgHeartRate,
                    description: match.description,
                    notes: match.notes,
                    stravaActivityId: match.stravaActivityId
                })
                .catch(err => {
                    console.error("Firebase save failed for run:", runId, err);
                    // Revert local memory state in case of database rejection
                    Object.assign(match, previousWorkoutState);
                    alert(`Failed to sync workout to Cloud database: ${err.message}`);
                });
            }
            
            matchedCount++;
            dbUpdated = true;
        }
    });
    
    if (dbUpdated) {
        saveWorkoutsLocally();
        
        // If not connected to Firebase, we must reload the UI manually
        if (!appState.firebaseConnected) {
            renderWorkoutsList();
            updateAggregatedStats();
            populatePlanFilters();
            updateShoesUI();
        }
    }
    
    return { matched: matchedCount, skipped: skippedCount };
}

function findMatchingWorkout(activityLocalStr, distanceKm) {
    const activityMidnight = parseLocalDateString(activityLocalStr);
    
    // Find uncompleted runnable scheduled workout on the exact calendar day
    let exactDayMatch = appState.workouts.find(w => {
        return !w.isCompleted && w.scheduledDate === activityLocalStr && isWorkoutRunnable(w);
    });
    
    if (exactDayMatch) return exactDayMatch;
    
    // Fallback: Check strictly backward (run was done today to complete yesterday's scheduled workout)
    const dayInMs = 24 * 3600 * 1000;
    const candidates = appState.workouts.filter(w => {
        if (w.isCompleted || !w.scheduledDate || !isWorkoutRunnable(w)) return false;
        const wMidnight = parseLocalDateString(w.scheduledDate);
        const diff = activityMidnight.getTime() - wMidnight.getTime();
        return diff >= 0 && diff <= dayInMs;
    });
    
    if (candidates.length === 1) {
        return candidates[0];
    } else if (candidates.length > 1) {
        // If multiple matches are found within the range, pair with the closest distance
        candidates.sort((a, b) => {
            const diffA = Math.abs(parseFloat(a.distance || 0) - distanceKm);
            const diffB = Math.abs(parseFloat(b.distance || 0) - distanceKm);
            return diffA - diffB;
        });
        return candidates[0];
    }
    
    return null;
}

function formatStravaDuration(seconds) {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    const pad = (n) => String(n).padStart(2, '0');
    
    if (hrs > 0) {
        return `${hrs}:${pad(mins)}:${pad(secs)}`;
    }
    return `${mins}:${pad(secs)}`;
}

function formatStravaPace(metersPerSec) {
    if (!metersPerSec || metersPerSec <= 0) return "--:--";
    const totalSecondsPerKm = 1000 / metersPerSec;
    const mins = Math.floor(totalSecondsPerKm / 60);
    const secs = Math.round(totalSecondsPerKm % 60);
    return `${mins}:${String(secs).padStart(2, '0')}`;
}
