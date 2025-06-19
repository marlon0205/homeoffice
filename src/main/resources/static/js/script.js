let currentDate = new Date(); // Actual current date (will be fixed for demo logic later)
const todayFixedForDemo = new Date(2025, 5, 19); // Fixed demo date for consistency (June 19, 2025)

document.addEventListener('DOMContentLoaded', () => {
    // Theme Toggle
    const themeToggle = document.getElementById('themeToggle');
    const body = document.body;

    if (localStorage.getItem('theme') === 'dark') {
        body.classList.add('dark-mode');
    } else {
        body.classList.add('light-mode');
    }

    themeToggle.addEventListener('click', () => {
        if (body.classList.contains('light-mode')) {
            body.classList.remove('light-mode');
            body.classList.add('dark-mode');
            localStorage.setItem('theme', 'dark');
        } else {
            body.classList.remove('dark-mode');
            body.classList.add('light-mode');
            localStorage.setItem('theme', 'light');
        }
    });

    // Initialisierung des Kalenders mit dem aktuellen Monat
    currentDate = todayFixedForDemo; // Startet die Demo immer mit Juni 2025
    updateCalendarHeader();

    // Event Listener für Kalender-Navigation
    document.getElementById('prevMonthBtn').addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() - 1);
        updateCalendarHeader();
    });

    document.getElementById('nextMonthBtn').addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() + 1);
        updateCalendarHeader();
    });

    // CSRF Token aus dem Hidden-Feld auslesen
    window.csrfToken = document.getElementById('csrfToken') ? document.getElementById('csrfToken').value : '';
    window.csrfHeaderName = document.getElementById('csrfToken') ? document.getElementById('csrfToken').name : '';

    // Hilfsfunktion für fetch-Requests mit CSRF
    window.makeAuthenticatedRequest = async (url, method = 'GET', body = null) => {
        const headers = {
            'Content-Type': 'application/json',
        };
        if (window.csrfToken && method !== 'GET') { // CSRF nur für schreibende Operationen
            headers[window.csrfHeaderName] = window.csrfToken;
        }

        const options = {
            method,
            headers,
        };
        if (body) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(url, options);
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                // Bei 401/403 (unautorisiert/verboten) zur Login-Seite umleiten
                alert('Sitzung abgelaufen oder keine Berechtigung. Bitte melden Sie sich erneut an.');
                window.location.href = '/login';
            }
            const errorText = await response.text();
            throw new Error(`HTTP Error ${response.status}: ${errorText}`);
        }
        // Nur versuchen, JSON zu parsen, wenn der Content-Type json ist und nicht 204 No Content
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return response.json();
        }
        return response; // Manchmal ist keine JSON-Antwort zu erwarten (z.B. DELETE 204)
    };

    // Verfügbare Homeoffice-Tage laden (Relevant für Azubis, immer laden)
    loadAvailableHomeofficeDays();

    // Sidebar list filtering - bleibt per JS, da wir keine Backend-APIs dafür erstellt haben
    document.getElementById('listFilter').addEventListener('change', (event) => {
        const filter = event.target.value;
        updateSidebarList(filter);
    });
    updateSidebarList('today'); // Initialisiere die Liste
});

// --- Kalender-Funktionen ---
async function updateCalendarHeader() {
    const options = { year: 'numeric', month: 'long' };
    document.getElementById('currentMonthYear').textContent = currentDate.toLocaleDateString('de-DE', options);
    await renderCalendarDays(currentDate.getFullYear(), currentDate.getMonth());
}

async function renderCalendarDays(year, month) {
    const calendarGrid = document.getElementById('calendarGrid');
    const calendarContainer = document.querySelector('.calendar-container');
    const sidebarList = document.querySelector('.sidebar-list');

    // Entferne alle alten Kalendertage (außer den Tagesnamen)
    calendarGrid.querySelectorAll('.calendar-day').forEach(day => {
        if (!day.classList.contains('day-name')) {
            day.remove();
        }
    });

    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);

    const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7; // Monday is 0

    let dayCounter = 0;

    // Fülle leere Tage am Monatsanfang
    for (let i = 0; i < startDayOfWeek; i++) {
        const emptyDay = document.createElement('div');
        emptyDay.classList.add('calendar-day', 'empty-day');
        calendarGrid.appendChild(emptyDay);
        dayCounter++;
    }

    // Holen der Homeoffice-Anträge vom Backend
    let homeofficeRequests = [];
    try {
        // month + 1 weil JS 0-indiziert ist, Java LocalDate 1-indiziert
        homeofficeRequests = await makeAuthenticatedRequest(`/api/calendar-data?year=${year}&month=${month}`, 'GET');
    } catch (error) {
        console.error('Fehler beim Laden der Homeoffice-Daten:', error);
        alert('Fehler beim Laden der Kalenderdaten.');
    }

    const today = todayFixedForDemo; // Das feste "heutige" Datum für die Demo-Logik
    // Den eingeloggten Benutzernamen von der UI holen, um eigene Anträge zu identifizieren
    // In Produktion besser: Backend gibt für jeden Request ein Flag 'isCurrentUserRequest: true' zurück
    const loggedInUserElement = document.getElementById('loggedInUser');
    let loggedInUsername = '';
    if (loggedInUserElement) {
        loggedInUsername = loggedInUserElement.textContent.replace('Hallo, ', '').trim();
    }


    for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
        const dayElement = document.createElement('div');
        dayElement.classList.add('calendar-day');

        const currentDayFull = new Date(year, month, day);

        if (currentDayFull < new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
            dayElement.classList.add('past-day');
        }

        if (currentDayFull.toDateString() === today.toDateString()) {
            dayElement.classList.add('current-day');
        }

        dayElement.innerHTML = `<span class="day-number">${day}</span><div class="homeoffice-entries"></div>`;

        const entriesContainer = dayElement.querySelector('.homeoffice-entries');

        // Füge Homeoffice-Einträge aus den Backend-Daten hinzu
        homeofficeRequests.filter(req => {
            // req.requestDate ist jetzt ein Array [year, month, day] von LocalDate in Java
            const reqDate = new Date(req.requestDate[0], req.requestDate[1] - 1, req.requestDate[2]);
            return reqDate.toDateString() === currentDayFull.toDateString();
        }).forEach(req => {
            const entryDiv = document.createElement('div');
            let statusText = '';
            if (req.status === 'PENDING') {
                statusText = ' - ANTRAG';
            }
            let halfFull = req.halfDay ? '(H)' : '(V)';

            // Logik zur Anzeige des Namens oder "Du"
            if (req.user && req.user.username === loggedInUsername) {
                entryDiv.classList.add('entry', 'own-entry');
                if (req.status === 'PENDING') {
                    entryDiv.classList.add('pending');
                }
                entryDiv.textContent = `Du ${halfFull}${statusText}`;
            } else {
                entryDiv.classList.add('entry', 'other-entry');
                // Sicherstellen, dass user-Objekt existiert und firstName/lastName verfügbar sind
                if (req.user) {
                    entryDiv.textContent = `${req.user.firstName.charAt(0)}. ${req.user.lastName.charAt(0)}. ${halfFull}`;
                } else {
                    entryDiv.textContent = `Unbekannt ${halfFull}`;
                }
            }
            entriesContainer.appendChild(entryDiv);
        });

        // Event Listener nur für zukünftige Tage (inkl. heute)
        if (currentDayFull >= new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
            dayElement.addEventListener('click', () => openRequestModal(dayElement));
        }

        calendarGrid.appendChild(dayElement);
        dayCounter++;
    }

    // Dynamische Höhenanpassung beibehalten
    const weeks = Math.ceil(dayCounter / 7);
    const baseRowHeight = 100; // Geschätzte Höhe pro Kalender-Woche in Pixeln
    const headerHeight = 80; // Geschätzte Höhe für Kalender-Header und Tagesnamen
    const calculatedHeight = (weeks * baseRowHeight) + headerHeight;

    calendarContainer.style.height = `${calculatedHeight}px`;
    sidebarList.style.height = `${calculatedHeight}px`;

    console.log(`Kalender für ${currentDate.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })} hat ${weeks} Wochen. Berechnete Höhe: ${calculatedHeight}px`);
}

// --- Modals ---
let currentDayClicked = null;
let requestDateForModal = null; // Speichert das Date-Objekt für den Modal-Request

function toggleAuthModal() { // Nicht wirklich benutzt, Spring Security handhabt den Login
    const authModal = document.getElementById('authModal');
    authModal.style.display = authModal.style.display === 'flex' ? 'none' : 'flex';
}

function openRequestModal(dayElement) {
    const dayNumber = parseInt(dayElement.querySelector('.day-number').textContent);
    const clickedDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), dayNumber);
    const today = todayFixedForDemo;

    if (clickedDate < new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
        return; // Vergangene Tage können nicht beantragt werden
    }

    const requestHomeofficeModal = document.getElementById('requestHomeofficeModal');
    const requestDateDisplay = document.getElementById('requestDateDisplay');
    const month = currentDate.getMonth(); // 0-indexed month
    const year = currentDate.getFullYear();

    // requestDateForModal als Date-Objekt speichern
    requestDateForModal = new Date(year, month, dayNumber);
    const formattedDate = `${String(dayNumber).padStart(2, '0')}.${String(month + 1).padStart(2, '0')}.${year}`; // Für Anzeige

    requestDateDisplay.textContent = formattedDate;
    requestHomeofficeModal.style.display = 'flex';
    currentDayClicked = dayElement;
}

function closeRequestModal() {
    document.getElementById('requestHomeofficeModal').style.display = 'none';
    currentDayClicked = null;
    requestDateForModal = null;
    document.getElementById('halfDayCheckbox').checked = false;
}

async function submitHomeofficeRequest() {
    const isHalfDay = document.getElementById('halfDayCheckbox').checked;

    if (!requestDateForModal) {
        alert('Kein Datum für den Homeoffice-Antrag ausgewählt.');
        return;
    }

    // Datum im dd.MM.yyyy Format für das Backend DTO vorbereiten
    const year = requestDateForModal.getFullYear();
    const month = String(requestDateForModal.getMonth() + 1).padStart(2, '0'); // +1 weil 0-indiziert
    const day = String(requestDateForModal.getDate()).padStart(2, '0');
    const formattedDate = `${day}.${month}.${year}`;

    const payload = {
        requestDate: formattedDate,
        isHalfDay: isHalfDay
    };

    try {
        const response = await makeAuthenticatedRequest('/api/homeoffice-request', 'POST', payload);
        console.log('Antrag erfolgreich gestellt:', response);
        alert('Homeoffice Antrag erfolgreich gestellt!');
        closeRequestModal();
        updateCalendarHeader(); // Kalender nach Erfolg aktualisieren
        loadPendingRequests(); // Antragsliste im Admin-Bereich aktualisieren (falls sichtbar)
        loadAvailableHomeofficeDays(); // Verfügbare Tage aktualisieren
    } catch (error) {
        console.error('Fehler beim Antrag stellen:', error);
        alert(`Fehler beim Homeoffice Antrag: ${error.message}`);
    }
}

let currentGroupForAssignment = null;
let allUsersForAssignment = []; // Speichert alle Azubis für das Zuweisungs-Modal

async function openUserAssignment(buttonElement) {
    currentGroupForAssignment = buttonElement.closest('.group-item');
    const groupId = currentGroupForAssignment.dataset.groupId; // Hol die ID der Gruppe

    const userAssignmentModal = document.getElementById('userAssignmentModal');
    const userListAssignment = document.getElementById('userListAssignment');
    userListAssignment.innerHTML = ''; // Leere vorherige Einträge

    // Hol alle Azubis vom Backend
    try {
        // Endpoint gibt jetzt User-Objekte zurück, die nur Azubi-Rolle haben
        allUsersForAssignment = await makeAuthenticatedRequest('/api/admin/users?role=AZUBI', 'GET');

        // Hol die aktuell zugewiesenen User für diese Gruppe
        // ACHTUNG: Der /api/admin/groups/{groupId} Endpoint muss die User-Liste enthalten!
        // Hier gehen wir davon aus, dass das Group-JSON die 'users' (UserSummaryDto) enthält.
        const currentGroupData = await makeAuthenticatedRequest(`/api/admin/groups/${groupId}`, 'GET');
        // Die `users` Property in Group-DTO ist ein Set von UserSummaryDto, wir brauchen deren IDs
        const assignedUserIds = new Set(currentGroupData.users.map(u => u.id));

        allUsersForAssignment.forEach(user => {
            const userItem = document.createElement('div');
            userItem.classList.add('user-assignment-item');
            const isChecked = assignedUserIds.has(user.id);

            userItem.innerHTML = `
                <input type="checkbox" id="user_assign_${user.id}" data-user-id="${user.id}" ${isChecked ? 'checked' : ''}>
                <label for="user_assign_${user.id}">${user.firstName} ${user.lastName}</label>
            `;
            userListAssignment.appendChild(userItem);
        });

        userAssignmentModal.style.display = 'flex';
    } catch (error) {
        console.error('Fehler beim Laden der Benutzer für Zuweisung:', error);
        alert('Fehler beim Laden der Benutzerliste für Zuweisung.');
    }
}

function closeUserAssignment() {
    document.getElementById('userAssignmentModal').style.display = 'none';
    currentGroupForAssignment = null;
    allUsersForAssignment = [];
}

async function saveUserAssignment() {
    const groupId = currentGroupForAssignment.dataset.groupId;
    const selectedUserIds = Array.from(document.querySelectorAll('#userListAssignment input[type="checkbox"]:checked'))
        .map(checkbox => checkbox.dataset.userId); // userId ist bereits UUID-String

    // Convert UUID strings to proper UUID format if backend expects that, but usually just strings are fine
    // const selectedUserUUIDs = selectedUserIds.map(id => UUID.parse(id)); // If you use a UUID library

    try {
        await makeAuthenticatedRequest(`/api/admin/groups/${groupId}/users`, 'PUT', selectedUserIds);
        alert('Benutzerzuweisung gespeichert!');
        closeUserAssignment();
        loadAllGroups(); // Gruppenliste aktualisieren
    } catch (error) {
        console.error('Fehler beim Speichern der Benutzerzuweisung:', error);
        alert(`Fehler beim Speichern der Zuweisung: ${error.message}`);
    }
}

// --- Admin Funktionen (rollenbasierte Initialisierung erfolgt über Thymeleaf im HTML) ---

async function loadAllGroups() {
    const groupList = document.getElementById('groupList');
    if (!groupList) return; // Stellen Sie sicher, dass das Element existiert
    groupList.innerHTML = '<div>Lade Gruppen...</div>';
    try {
        const groups = await makeAuthenticatedRequest('/api/admin/groups', 'GET');
        groupList.innerHTML = ''; // Leere Lade-Nachricht
        if (groups.length === 0) {
            groupList.innerHTML = '<div>Keine Gruppen vorhanden.</div>';
            return;
        }
        groups.forEach(group => {
            const groupItem = document.createElement('div');
            groupItem.classList.add('group-item');
            groupItem.dataset.groupId = group.id; // Speichere die ID für spätere Operationen
            groupItem.innerHTML = `
                <span class="group-name">${group.name}</span>
                <div class="group-actions">
                    <input type="number" class="group-max-days" value="${group.maxHoDaysPerWeek}" min="0" onchange="updateGroupMaxDays(this)">
                    <button class="btn icon-btn" onclick="openUserAssignment(this)"><span class="icon user-icon">👥</span></button>
                    <button class="btn icon-btn delete-btn" onclick="deleteGroup(this)"><span class="icon delete-icon">❌</span></button>
                </div>
            `;
            groupList.appendChild(groupItem);
        });
    } catch (error) {
        console.error('Fehler beim Laden der Gruppen:', error);
        groupList.innerHTML = `<div class="error-message">Fehler beim Laden der Gruppen: ${error.message}</div>`;
    }
}

async function createGroup() {
    const newGroupNameInput = document.getElementById('newGroupName');
    const newGroupMaxDaysInput = document.getElementById('newGroupMaxDays');
    const groupName = newGroupNameInput.value.trim();
    const maxDays = parseInt(newGroupMaxDaysInput.value);

    if (!groupName) {
        alert('Bitte Gruppennamen eingeben.');
        return;
    }
    if (isNaN(maxDays) || maxDays < 0 || maxDays > 7) {
        alert('Bitte eine gültige Zahl (0-7) für Max HO/Woche eingeben.');
        return;
    }

    try {
        const payload = {
            name: groupName,
            maxHoDaysPerWeek: maxDays
        };
        await makeAuthenticatedRequest('/api/admin/groups', 'POST', payload);
        alert(`Gruppe "${groupName}" erfolgreich erstellt.`);
        newGroupNameInput.value = '';
        newGroupMaxDaysInput.value = '0';
        loadAllGroups(); // Gruppenliste aktualisieren
    } catch (error) {
        console.error('Fehler beim Erstellen der Gruppe:', error);
        alert(`Fehler beim Erstellen der Gruppe: ${error.message}`);
    }
}

async function deleteGroup(buttonElement) {
    if (!confirm('Sind Sie sicher, dass Sie diese Gruppe löschen möchten?')) {
        return;
    }
    const groupItem = buttonElement.closest('.group-item');
    const groupId = groupItem.dataset.groupId;
    const groupName = groupItem.querySelector('.group-name').textContent;

    try {
        await makeAuthenticatedRequest(`/api/admin/groups/${groupId}`, 'DELETE');
        alert(`Gruppe "${groupName}" erfolgreich gelöscht.`);
        groupItem.remove(); // Entferne das Element aus der UI
        loadAllGroups(); // Nur zur Sicherheit, oder wenn andere Gruppen betroffen sein könnten
    } catch (error) {
        console.error('Fehler beim Löschen der Gruppe:', error);
        alert(`Fehler beim Löschen der Gruppe: ${error.message}`);
    }
}

async function updateGroupMaxDays(inputElement) {
    const groupItem = inputElement.closest('.group-item');
    const groupId = groupItem.dataset.groupId;
    const maxDays = parseInt(inputElement.value);

    if (isNaN(maxDays) || maxDays < 0 || maxDays > 7) {
        alert('Bitte eine gültige Zahl (0-7) eingeben.');
        // Optional: Reset value to previous valid or display error prominently
        return;
    }

    try {
        // PUT Request mit Query-Parameter
        await makeAuthenticatedRequest(`/api/admin/groups/${groupId}/max-days?maxDays=${maxDays}`, 'PUT');
        console.log(`Max. Tage für Gruppe ${groupId} auf ${maxDays} aktualisiert.`);
        alert('Maximale Tage erfolgreich aktualisiert.');
    } catch (error) {
        console.error('Fehler beim Aktualisieren der Gruppentage:', error);
        alert(`Fehler beim Aktualisieren der Gruppentage: ${error.message}`);
    }
}

async function loadPendingRequests() {
    const requestList = document.getElementById('requestList');
    if (!requestList) return; // Stellen Sie sicher, dass das Element existiert
    requestList.innerHTML = '<div>Lade Anträge...</div>';
    try {
        const requests = await makeAuthenticatedRequest('/api/admin/requests/pending', 'GET');
        requestList.innerHTML = '';
        if (requests.length === 0) {
            requestList.innerHTML = '<div>Keine ausstehenden Anträge.</div>';
            return;
        }
        requests.forEach(req => {
            const requestItem = document.createElement('div');
            requestItem.classList.add('request-item');
            requestItem.dataset.requestId = req.id;

            // req.requestDate ist [year, month, day]
            const requestDate = new Date(req.requestDate[0], req.requestDate[1] - 1, req.requestDate[2]);
            const formattedDate = `${String(requestDate.getDate()).padStart(2, '0')}.${String(requestDate.getMonth() + 1).padStart(2, '0')}.${requestDate.getFullYear()}`;

            let halfFull = req.halfDay ? '(Halbtags)' : '(Ganztags)';

            // Annahme: Backend sendet 'exceedsLimit: true/false' ODER wir berechnen es hier (besser Backend)
            // Für Demo: Wir haben kein `exceedsLimit` im HomeofficeRequestResponseDto definiert
            // Also ist der Warning-Text standardmäßig hidden.
            const warningClass = 'hidden'; // Hier müsste echte Logik/Daten vom Backend sein.

            requestItem.innerHTML = `
                <div class="request-info">
                    <span class="request-user">${req.user.firstName} ${req.user.lastName}</span>
                    <span class="request-date">${formattedDate}</span>
                    <span class="request-type">${halfFull}</span>
                </div>
                <div class="request-actions">
                    <span class="warning-text ${warningClass}">Überschreitet max. Tage!</span>
                    <button class="btn accept-btn" onclick="acceptRequest(this)">Annehmen</button>
                    <button class="btn reject-btn" onclick="rejectRequest(this)">Ablehnen</button>
                </div>
            `;
            requestList.appendChild(requestItem);
        });
    } catch (error) {
        console.error('Fehler beim Laden der Anträge:', error);
        requestList.innerHTML = `<div class="error-message">Fehler beim Laden der Anträge: ${error.message}</div>`;
    }
}

async function acceptRequest(buttonElement) {
    const requestItem = buttonElement.closest('.request-item');
    const requestId = requestItem.dataset.requestId;
    const userName = requestItem.querySelector('.request-user').textContent;

    try {
        await makeAuthenticatedRequest(`/api/admin/requests/${requestId}/approve`, 'PUT');
        alert(`Antrag von ${userName} angenommen.`);
        requestItem.remove(); // Entferne Element aus der UI
        updateCalendarHeader(); // Kalender aktualisieren, falls genehmigter Eintrag sichtbar wird
        loadAvailableHomeofficeDays(); // Verfügbare Tage des Azubis könnten sich ändern
        loadPendingRequests(); // Antragsliste aktualisieren
    } catch (error) {
        console.error('Fehler beim Annehmen des Antrags:', error);
        alert(`Fehler beim Annehmen des Antrags: ${error.message}`);
    }
}

async function rejectRequest(buttonElement) {
    if (!confirm('Möchten Sie diesen Antrag wirklich ablehnen?')) {
        return;
    }
    const requestItem = buttonElement.closest('.request-item');
    const requestId = requestItem.dataset.requestId;
    const userName = requestItem.querySelector('.request-user').textContent;

    // Optional: Prompt für Kommentar (für diese Demo deaktiviert)
    // const comment = prompt('Kommentar zur Ablehnung (optional):');

    try {
        // Sende Request, ggf. mit Kommentar als Query-Parameter oder im Body
        await makeAuthenticatedRequest(`/api/admin/requests/${requestId}/reject`, 'PUT'); // Ohne Kommentar für Demo
        alert(`Antrag von ${userName} abgelehnt.`);
        requestItem.remove(); // Entferne Element aus der UI
        loadPendingRequests(); // Antragsliste aktualisieren
    } catch (error) {
        console.error('Fehler beim Ablehnen des Antrags:', error);
        alert(`Fehler beim Ablehnen des Antrags: ${error.message}`);
    }
}

async function loadAllAzubis() {
    const azubiList = document.getElementById('azubiList');
    if (!azubiList) return; // Stellen Sie sicher, dass das Element existiert
    azubiList.innerHTML = '<div>Lade Azubis...</div>';
    try {
        // Der Endpoint /api/admin/users?role=AZUBI liefert User-Objekte mit relevanter Info
        const azubis = await makeAuthenticatedRequest('/api/admin/users?role=AZUBI', 'GET');
        azubiList.innerHTML = '';
        if (azubis.length === 0) {
            azubiList.innerHTML = '<div>Keine Azubis vorhanden.</div>';
            return;
        }
        azubis.forEach(azubi => {
            const azubiItem = document.createElement('div');
            azubiItem.classList.add('azubi-item');
            azubiItem.dataset.userId = azubi.id;

            // value="" wenn personalMaxHoDaysPerWeek null ist, sonst der Wert
            const inputValue = azubi.personalMaxHoDaysPerWeek !== null ? azubi.personalMaxHoDaysPerWeek : '';
            const placeholderText = azubi.personalMaxHoDaysPerWeek === null ? 'Gruppenstandard' : '';

            azubiItem.innerHTML = `
                <span class="azubi-name">${azubi.firstName} ${azubi.lastName}</span>
                <div class="azubi-actions">
                    <label>Max HO/Woche:</label>
                    <input type="number" class="azubi-max-days" value="${inputValue}" placeholder="${placeholderText}" min="0" onchange="updateAzubiMaxDays(this)">
                </div>
            `;
            azubiList.appendChild(azubiItem);
        });
    } catch (error) {
        console.error('Fehler beim Laden der Azubis:', error);
        azubiList.innerHTML = `<div class="error-message">Fehler beim Laden der Azubis: ${error.message}</div>`;
    }
}

async function updateAzubiMaxDays(inputElement) {
    const azubiItem = inputElement.closest('.azubi-item');
    const userId = azubiItem.dataset.userId;
    let maxDays = inputElement.value.trim();

    let payloadValue = null; // Standard ist null für Gruppenstandard
    if (maxDays !== "") {
        const parsedMaxDays = parseInt(maxDays);
        if (isNaN(parsedMaxDays) || parsedMaxDays < 0 || parsedMaxDays > 7) {
            alert("Bitte geben Sie eine gültige positive Zahl (0-7) ein oder lassen Sie das Feld leer für den Gruppenstandard.");
            // Hier wäre es besser, das Feld auf den vorherigen gültigen Wert zurückzusetzen,
            // anstatt alle Azubis neu zu laden. Für diese Demo lassen wir es so.
            loadAllAzubis();
            return;
        }
        payloadValue = parsedMaxDays;
    }

    const payload = {
        personalMaxHoDaysPerWeek: payloadValue
    };

    try {
        // Der Request Body ist ein JSON-Objekt mit dem Feld personalMaxHoDaysPerWeek
        await makeAuthenticatedRequest(`/api/admin/users/${userId}/personal-max-ho-days`, 'PUT', payload);
        alert('Maximale HO-Tage für Azubi erfolgreich aktualisiert.');
        loadAllAzubis(); // Um sicherzustellen, dass Placeholder richtig angezeigt wird
        loadAvailableHomeofficeDays(); // Für den Azubi selbst könnte sich das ändern
    } catch (error) {
        console.error('Fehler beim Aktualisieren der Azubi-Tage:', error);
        alert(`Fehler beim Aktualisieren der Azubi-Tage: ${error.message}`);
    }
}

async function loadAvailableHomeofficeDays() {
    const availableDaysDiv = document.querySelector('.available-days');
    availableDaysDiv.innerHTML = 'Verfügbar: ... KW | ... NKW | Max: ...'; // Setze Ladezustand

    try {
        // Die API gibt aktuell nur das "max mögliche" zurück.
        // Für "KW" und "NKW" müsstest du weitere API-Endpunkte oder Logik implementieren.
        const maxDays = await makeAuthenticatedRequest('/api/available-homeoffice-days', 'GET');

        // Annahme: Die Antwort ist einfach die Zahl des maximal möglichen Limits
        // Für eine vollständige Anzeige müsstest du ein DTO vom Backend erhalten:
        // { currentWeekUsed: 1, nextWeekUsed: 0, maxAllowed: 5 }
        const currentWeekUsed = "-"; // Platzhalter
        const nextWeekUsed = "-";   // Platzhalter

        availableDaysDiv.innerHTML = `Verfügbar: <span id="currentWeekDays">${currentWeekUsed}</span> KW | <span id="nextWeekDays">${nextWeekUsed}</span> NKW | Max: <span id="maxDaysPerWeek">${maxDays}</span>`;
    } catch (error) {
        console.error('Fehler beim Laden der verfügbaren HO-Tage:', error);
        // Zeige einen Fehler im Frontend an, aber leite nicht um, wenn es nur diese Info betrifft
        availableDaysDiv.innerHTML = `<span class="error-message">Fehler.</span>`;
    }
}


// Sidebar List Funktionen (Behalten ihren Demo-Status, da keine Backend-APIs dafür erstellt wurden)
function updateSidebarList(filter) {
    const listContent = document.getElementById('listContent');
    if (!listContent) return; // Sicherstellen, dass Element existiert
    switch (filter) {
        case 'today':
            listContent.innerHTML = `
                <div class="list-item">
                    <span class="list-name">Max Mustermann</span>
                    <span class="list-info">(Ganztags)</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Tim Becker</span>
                    <span class="list-info">(Halbtags)</span>
                </div>
            `;
            break;
        case 'this-week':
            listContent.innerHTML = `
                <div class="list-item">
                    <span class="list-name">Max Mustermann</span>
                    <span class="list-info">(Mo: Ganztags)</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Anna Schmidt</span>
                    <span class="list-info">(Di: Halbtags)</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Tim Becker</span>
                    <span class="list-info">(Do: Ganztags)</span>
                </div>
            `;
            break;
        case 'next-week':
            listContent.innerHTML = `
                <div class="list-item">
                    <span class="list-name">Lena Mayer</span>
                    <span class="list-info">(Mo: Ganztags)</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Max Mustermann</span>
                    <span class="list-info">(Mi: Halbtags)</span>
                </div>
            `;
            break;
        case 'overview':
            listContent.innerHTML = `
                <div class="list-item">
                    <span class="list-name">Anna Schmidt</span>
                    <span class="list-info">Nächster HO: 20.06.</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Lena Mayer</span>
                    <span class="list-info">Nächster HO: 23.06.</span>
                </div>
                 <div class="list-item">
                    <span class="list-name">Max Mustermann</span>
                    <span class="list-info">Nächster HO: 19.06.</span>
                </div>
                <div class="list-item">
                    <span class="list-name">Tim Becker</span>
                    <span class="list-info">...</span>
                </div>
            `;
            break;
    }
}