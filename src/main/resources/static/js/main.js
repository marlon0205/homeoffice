// Globale Variablen
const API_BASE_URL = '/api';
let currentUserRole = null; // Rolle wird nach OIDC-Login vom Backend ermittelt

// DOM Elemente
const authSection = document.getElementById('auth-section');
const appSection = document.getElementById('app-section');
const loginWithAuthentikBtn = document.getElementById('loginWithAuthentikBtn');
const toggleThemeBtn = document.getElementById('toggleTheme');
const themeLink = document.getElementById('theme-link');
const logoutBtn = document.getElementById('logoutBtn');
const adminPanel = document.getElementById('admin-panel');

// --- Initialisierungsfunktionen ---

document.addEventListener('DOMContentLoaded', () => {
    applySavedTheme();
    // Beim Laden der Seite prüfen, ob bereits authentifiziert
    // Dies geschieht durch einen Request an einen geschützten Endpunkt
    checkAuthStatus();
});

async function checkAuthStatus() {
    try {
        // Versuche, einen geschützten Endpunkt aufzurufen (z.B. userInfo)
        // Spring Security leitet automatisch zum OAuth2-Login um, wenn nicht authentifiziert
        const response = await fetch(`${API_BASE_URL}/users/me`, {
            headers: {
                // CSRF Token für POST, PUT, DELETE benötigt
                // Für GET-Requests ist es nicht zwingend erforderlich
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
            }
        });

        if (response.ok) {
            const userData = await response.json();
            // Hier extrahieren wir die Rolle aus der Antwort
            // Je nachdem, wie dein Backend die UserDTO für /api/users/me aufbereitet
            currentUserRole = userData.roles && userData.roles.includes('ROLE_AUSBILDER') ? 'ROLE_AUSBILDER' : 'ROLE_AZUBI';
            showApp();
            if (currentUserRole === 'ROLE_AUSBILDER') {
                adminPanel.classList.remove('hidden');
                loadGroupsAndUsers();
                loadPendingRequests();
            } else {
                adminPanel.classList.add('hidden');
            }
            logoutBtn.classList.remove('hidden'); // Logout-Button anzeigen
        } else {
            // Wenn der Status 401 ist (Unauthorized), dann ist der Benutzer nicht eingeloggt
            // In diesem Fall, zeige den Login-Bereich
            if (response.status === 401) {
                showAuth();
                logoutBtn.classList.add('hidden'); // Logout-Button verstecken
            } else {
                console.error('Fehler beim Überprüfen des Auth-Status:', response.status);
                showAuth(); // Im Fehlerfall auch den Auth-Bereich zeigen
                logoutBtn.classList.add('hidden');
            }
        }
    } catch (error) {
        console.error('Netzwerkfehler beim Überprüfen des Auth-Status:', error);
        showAuth(); // Im Fehlerfall den Auth-Bereich zeigen
        logoutBtn.classList.add('hidden');
    }
}

function showApp() {
    authSection.classList.add('hidden');
    appSection.classList.remove('hidden');
    renderCalendar(); // Rendere den Kalender, sobald angemeldet
}

function showAuth() {
    authSection.classList.remove('hidden');
    appSection.classList.add('hidden');
}

// --- Theme Wechsel (unverändert) ---
function applySavedTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-mode');
        themeLink.href = '/css/dark-mode.css';
    } else {
        document.body.classList.remove('dark-mode');
        themeLink.href = '/css/style.css';
    }
}

toggleThemeBtn.addEventListener('click', () => {
    if (document.body.classList.contains('dark-mode')) {
        document.body.classList.remove('dark-mode');
        localStorage.setItem('theme', 'light');
        themeLink.href = '/css/style.css';
    } else {
        document.body.classList.add('dark-mode');
        localStorage.setItem('theme', 'dark');
        themeLink.href = '/css/dark-mode.css';
    }
});

// --- Login & Logout ---
loginWithAuthentikBtn.addEventListener('click', () => {
    // Leite den Benutzer zum Spring Security OAuth2 Login-Endpunkt weiter
    // Dieser wird dann den Authentik-Login initiieren
    window.location.href = '/oauth2/authorization/authentik';
});

logoutBtn.addEventListener('click', async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/logout`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') // CSRF Token senden
            }
        });
        if (response.ok) {
            alert('Erfolgreich ausgeloggt!');
            currentUserRole = null; // Rolle zurücksetzen
            showAuth(); // Zurück zum Login-Bildschirm
            logoutBtn.classList.add('hidden');
            // Optional: Weiterleitung zur Authentik Logout-Seite, falls konfiguriert
            // window.location.href = "https://your-authentik-domain.com/application/o/logout/homeoffice/";
        } else {
            alert('Fehler beim Logout.');
        }
    } catch (error) {
        console.error('Logout Fehler:', error);
        alert('Ein Fehler ist aufgetreten beim Logout.');
    }
});

// Hilfsfunktion zum Abrufen des CSRF-Tokens aus dem Cookie
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}


// --- Homeoffice Antrag abschicken (angepasst für CSRF) ---
document.getElementById('submitRequestBtn').addEventListener('click', async () => {
    const requestDate = document.getElementById('requestDateInput').value;
    const halfDay = document.getElementById('halfDayCheckbox').checked;

    if (!requestDate) {
        alert('Bitte ein Datum auswählen.');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/homeoffice/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') // CSRF Token senden
            },
            body: JSON.stringify({ requestDate, halfDay })
        });

        if (response.ok) {
            alert('Homeoffice Antrag erfolgreich gestellt!');
            renderCalendar(); // Kalender aktualisieren
        } else {
            const errorText = await response.text();
            alert('Fehler beim Antrag: ' + errorText);
        }
    } catch (error) {
        console.error('Fehler beim Senden des Antrags:', error);
        alert('Ein Fehler ist aufgetreten.');
    }
});

// --- Admin Panel Funktionen (für Ausbilder, angepasst für CSRF) ---

async function loadGroupsAndUsers() {
    const groupSelect = document.getElementById('groupSelect');
    const userSelect = document.getElementById('userSelect');
    groupSelect.innerHTML = '';
    userSelect.innerHTML = '';

    try {
        const groupsResponse = await fetch(`${API_BASE_URL}/groups`, {
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
        });
        const groups = await groupsResponse.json();
        groups.forEach(group => {
            const option = document.createElement('option');
            option.value = group.id;
            option.textContent = group.name;
            groupSelect.appendChild(option);
        });
        if (groups.length > 0) {
            groupSelect.dispatchEvent(new Event('change'));
        }

        const usersResponse = await fetch(`${API_BASE_URL}/users`, {
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
        });
        const users = await usersResponse.json();
        users.filter(user => user.roles && user.roles.includes('ROLE_AZUBI')).forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.username;
            userSelect.appendChild(option);
        });
        if (users.length > 0) {
            userSelect.dispatchEvent(new Event('change'));
        }

    } catch (error) {
        console.error('Fehler beim Laden von Gruppen/Benutzern:', error);
    }
}

document.getElementById('groupSelect').addEventListener('change', async (e) => {
    const groupId = e.target.value;
    if (groupId) {
        try {
            const response = await fetch(`${API_BASE_URL}/groups/${groupId}`, {
                headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
            });
            const group = await response.json();
            document.getElementById('groupMaxHoDays').value = group.maxHomeofficeDaysPerWeek;
        } catch (error) {
            console.error('Fehler beim Laden der Gruppen-Einstellungen:', error);
        }
    }
});

document.getElementById('saveGroupSettings').addEventListener('click', async () => {
    const groupId = document.getElementById('groupSelect').value;
    const maxDays = document.getElementById('groupMaxHoDays').value;

    if (!groupId || maxDays === '') {
        alert('Bitte Gruppe und maximale Tage eingeben.');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/groups/${groupId}/settings`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
            },
            body: JSON.stringify({ maxHomeofficeDaysPerWeek: parseInt(maxDays) })
        });
        if (response.ok) {
            alert('Gruppen-Einstellungen gespeichert!');
        } else {
            alert('Fehler beim Speichern der Gruppen-Einstellungen.');
        }
    } catch (error) {
        console.error('Fehler beim Speichern der Gruppen-Einstellungen:', error);
    }
});

document.getElementById('userSelect').addEventListener('change', async (e) => {
    const userId = e.target.value;
    if (userId) {
        try {
            const response = await fetch(`${API_BASE_URL}/users/${userId}/homeoffice-settings`, {
                headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
            });
            if (response.ok) {
                const settings = await response.json();
                document.getElementById('userMaxHoDays').value = settings.maxHomeofficeDaysPerWeek || '';
            } else if (response.status === 404) {
                document.getElementById('userMaxHoDays').value = '';
            }
        } catch (error) {
            console.error('Fehler beim Laden der Benutzer-Einstellungen:', error);
        }
    }
});

document.getElementById('saveUserSettings').addEventListener('click', async () => {
    const userId = document.getElementById('userSelect').value;
    const maxDays = document.getElementById('userMaxHoDays').value;

    if (!userId) {
        alert('Bitte einen Benutzer auswählen.');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/users/${userId}/homeoffice-settings`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
            },
            body: JSON.stringify({ maxHomeofficeDaysPerWeek: maxDays === '' ? null : parseInt(maxDays) })
        });
        if (response.ok) {
            alert('Benutzer-Einstellungen gespeichert!');
        } else {
            alert('Fehler beim Speichern der Benutzer-Einstellungen.');
        }
    } catch (error) {
        console.error('Fehler beim Speichern der Benutzer-Einstellungen:', error);
    }
});

async function loadPendingRequests() {
    const pendingRequestsList = document.getElementById('pendingRequestsList');
    pendingRequestsList.innerHTML = '<li>Lade offene Anträge...</li>';

    try {
        const response = await fetch(`${API_BASE_URL}/homeoffice/all`, {
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
        });
        const requests = await response.json();

        pendingRequestsList.innerHTML = '';
        const pending = requests.filter(req => req.status === 'PENDING');

        if (pending.length === 0) {
            pendingRequestsList.innerHTML = '<li>Keine offenen Anträge.</li>';
        } else {
            pending.forEach(request => {
                const li = document.createElement('li');
                li.innerHTML = `
                    <span>${request.username} - ${request.requestDate} ${request.halfDay ? '(halbtags)' : ''}</span>
                    <button data-id="${request.id}" data-status="APPROVED" class="action-btn approve-btn">Annehmen</button>
                    <button data-id="${request.id}" data-status="REJECTED" class="action-btn reject-btn">Ablehnen</button>
                `;
                pendingRequestsList.appendChild(li);
            });

            pendingRequestsList.querySelectorAll('.action-btn').forEach(button => {
                button.addEventListener('click', async (e) => {
                    const requestId = e.target.dataset.id;
                    const status = e.target.dataset.status;
                    try {
                        const res = await fetch(`${API_BASE_URL}/homeoffice/request/${requestId}/status?status=${status}`, {
                            method: 'PUT',
                            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
                        });
                        if (res.ok) {
                            alert('Antrag aktualisiert!');
                            loadPendingRequests();
                            renderCalendar();
                        } else {
                            const errorText = await res.text();
                            alert('Fehler beim Aktualisieren: ' + errorText);
                        }
                    } catch (error) {
                        console.error('Fehler beim Aktualisieren des Antrags:', error);
                    }
                });
            });
        }
    } catch (error) {
        console.error('Fehler beim Laden offener Anträge:', error);
        pendingRequestsList.innerHTML = '<li>Fehler beim Laden der Anträge.</li>';
    }
}