document.addEventListener('DOMContentLoaded', () => {
    // Theme Toggle
    const themeToggle = document.getElementById('themeToggle');
    const body = document.body;

    // Check for saved theme preference
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

    // Calendar Navigation (simplified for demo)
    const currentMonthYear = document.getElementById('currentMonthYear');
    const prevMonthBtn = document.getElementById('prevMonthBtn');
    const nextMonthBtn = document.getElementById('nextMonthBtn');

    // Set the current date to today (June 18, 2025) for demonstration purposes
    let currentDate = new Date(2025, 5, 18); // Year, Month (0-indexed), Day

    function updateCalendarHeader() {
        const options = { year: 'numeric', month: 'long' };
        currentMonthYear.textContent = currentDate.toLocaleDateString('de-DE', options);
        renderCalendarDays(currentDate.getFullYear(), currentDate.getMonth());
    }

    prevMonthBtn.addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() - 1);
        updateCalendarHeader();
    });

    nextMonthBtn.addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() + 1);
        updateCalendarHeader();
    });

    // Initial calendar render
    updateCalendarHeader();

    // Sidebar list filtering (simplified for demo)
    document.getElementById('listFilter').addEventListener('change', (event) => {
        const filter = event.target.value;
        const listContent = document.getElementById('listContent');
        // In a real application, you'd fetch data based on this filter from the backend
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
    });

    // Simulate initial list content
    document.getElementById('listFilter').dispatchEvent(new Event('change'));
});

// --- Modal Functions ---

function toggleAuthModal() {
    const authModal = document.getElementById('authModal');
    if (authModal.style.display === 'flex') {
        authModal.style.display = 'none';
    } else {
        authModal.style.display = 'flex';
    }
}

let currentDayClicked = null; // To store which day was clicked for HO request
let currentDate = new Date(2025, 5, 18); // Redefine here to be accessible globally for modal functions

function openRequestModal(dayElement) {
    // Check if the day is in the past
    const dayNumber = parseInt(dayElement.querySelector('.day-number').textContent);
    const clickedDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), dayNumber);
    const today = new Date(2025, 5, 18); // Fixed to current demo date for "past" logic

    if (clickedDate < new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
        // alert('Sie können keinen Homeoffice-Antrag für vergangene Tage stellen.');
        return; // Do nothing if it's a past day
    }

    const requestHomeofficeModal = document.getElementById('requestHomeofficeModal');
    const requestDateDisplay = document.getElementById('requestDateDisplay');
    const month = currentDate.getMonth() + 1; // currentMonth is from global currentDate
    const year = currentDate.getFullYear();
    const formattedDate = `${dayNumber}.${month}.${year}`;

    requestDateDisplay.textContent = formattedDate;
    requestHomeofficeModal.style.display = 'flex';
    currentDayClicked = dayElement; // Store the clicked day element
}

function closeRequestModal() {
    document.getElementById('requestHomeofficeModal').style.display = 'none';
    currentDayClicked = null;
    document.getElementById('halfDayCheckbox').checked = false; // Reset checkbox
}

function submitHomeofficeRequest() {
    const isHalfDay = document.getElementById('halfDayCheckbox').checked;
    const date = document.getElementById('requestDateDisplay').textContent;

    // TODO: In a real app, send this data to your Spring Boot Backend via an AJAX request (fetch API)
    console.log(`Homeoffice Request for ${date}. Half-day: ${isHalfDay}`);

    // Simulate adding pending entry to the calendar (remove in real app, let backend refresh)
    if (currentDayClicked) {
        // Remove existing "Du" pending entries for this day to avoid duplicates in demo
        currentDayClicked.querySelectorAll('.entry.own-entry.pending').forEach(el => el.remove());

        const homeofficeEntries = currentDayClicked.querySelector('.homeoffice-entries');
        const newEntry = document.createElement('div');
        newEntry.classList.add('entry', 'own-entry', 'pending');
        newEntry.textContent = `Du (${isHalfDay ? 'H' : 'V'}) - ANTRAG`;
        homeofficeEntries.appendChild(newEntry);
    }

    closeRequestModal();
    alert('Homeoffice Antrag erfolgreich gestellt! (Dies ist eine Demo-Nachricht)');
}

let currentGroupForAssignment = null; // Store the group element for user assignment

function openUserAssignment(buttonElement) {
    const userAssignmentModal = document.getElementById('userAssignmentModal');
    // In a real app, you'd fetch users for this group from the backend
    // For demo, just show placeholder users (already in HTML for this demo)
    userAssignmentModal.style.display = 'flex';
    currentGroupForAssignment = buttonElement.closest('.group-item');
}

function closeUserAssignment() {
    document.getElementById('userAssignmentModal').style.display = 'none';
    currentGroupForAssignment = null;
}

function saveUserAssignment() {
    // TODO: Collect checked users and send to backend with currentGroupForAssignment ID
    console.log('Saving user assignments for group:', currentGroupForAssignment ? currentGroupForAssignment.querySelector('.group-name').textContent : 'N/A');
    closeUserAssignment();
    alert('Benutzerzuweisung gespeichert! (Demo)');
}


// --- Placeholder Backend Interaction Functions (Frontend Simulation) ---

function renderCalendarDays(year, month) {
    const calendarGrid = document.getElementById('calendarGrid');
    const calendarContainer = document.querySelector('.calendar-container');
    const sidebarList = document.querySelector('.sidebar-list');

    // Keep the day names, remove only the generated days
    calendarGrid.querySelectorAll('.calendar-day, .empty-day').forEach(day => {
        if (!day.classList.contains('day-name')) {
            day.remove();
        }
    });

    const today = new Date(2025, 5, 18); // Fixed current date for consistent demo behavior
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);

    const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7; // Monday is 0

    let dayCounter = 0;
    let weeks = 0; // Zählt die Wochen

    // Add empty days for the start of the month
    for (let i = 0; i < startDayOfWeek; i++) {
        const emptyDay = document.createElement('div');
        emptyDay.classList.add('calendar-day', 'empty-day');
        calendarGrid.appendChild(emptyDay);
        dayCounter++;
    }

    for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
        const dayElement = document.createElement('div');
        dayElement.classList.add('calendar-day');

        const currentDayFull = new Date(year, month, day);

        // Add past-day class if the day is before today
        if (currentDayFull < new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
            dayElement.classList.add('past-day');
        }

        // Add current-day class if it's today
        if (currentDayFull.toDateString() === today.toDateString()) {
            dayElement.classList.add('current-day');
        }

        dayElement.innerHTML = `<span class="day-number">${day}</span><div class="homeoffice-entries"></div>`;

        // Simulate homeoffice entries for demo
        const entriesContainer = dayElement.querySelector('.homeoffice-entries');
        if (currentDayFull.toDateString() === today.toDateString()) { // Example for today (June 18, 2025)
            entriesContainer.innerHTML += '<div class="entry own-entry">Max M. (V)</div>';
            entriesContainer.innerHTML += '<div class="entry other-entry">Anna S. (H)</div>';
        } else if (currentDayFull.toDateString() === new Date(2025, 5, 19).toDateString()) { // Example for future day
            entriesContainer.innerHTML += '<div class="entry own-entry pending">Du (V) - ANTRAG</div>';
        } else if (currentDayFull.toDateString() === new Date(2025, 5, 25).toDateString()) {
            entriesContainer.innerHTML += '<div class="entry other-entry">Tim B. (V)</div>';
        } else if (currentDayFull.toDateString() === new Date(2025, 6, 1).toDateString()) { // Example for July 1st (if current month is June)
            entriesContainer.innerHTML += '<div class="entry other-entry">Neu! (V)</div>';
        }


        // Add event listener to open request modal for future days (including today)
        if (currentDayFull >= new Date(today.getFullYear(), today.getMonth(), today.getDate())) {
            dayElement.addEventListener('click', () => openRequestModal(dayElement));
        }

        calendarGrid.appendChild(dayElement);
        dayCounter++;
    }

    // Bugfix: Dynamische Höhenanpassung basierend auf der Anzahl der Wochen
    // Die erste Reihe sind die Tagesnamen, dann die Wochen
    weeks = Math.ceil(dayCounter / 7); // Anzahl der Wochen im Kalender

    // Basis-Höhe für Kalender und Sidebar
    // ca. 40px für den Header, 30px für die Tagesnamen
    // plus (Anzahl der Wochen * geschätzte Zeilenhöhe)
    const baseRowHeight = 100; // Eine geschätzte Höhe pro Kalender-Woche in Pixeln
    const headerHeight = 120; // Geschätzte Höhe für Kalender-Header und Tagesnamen
    const calculatedHeight = (weeks * baseRowHeight) + headerHeight;

    calendarContainer.style.height = `${calculatedHeight}px`;
    sidebarList.style.height = `${calculatedHeight}px`;

    console.log(`Kalender für ${currentDate.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' })} hat ${weeks} Wochen. Berechnete Höhe: ${calculatedHeight}px`);
}


function createGroup() {
    const newGroupNameInput = document.getElementById('newGroupName');
    const groupName = newGroupNameInput.value.trim();
    if (groupName) {
        const groupList = document.getElementById('groupList');
        const newGroupItem = document.createElement('div');
        newGroupItem.classList.add('group-item');
        newGroupItem.innerHTML = `
            <span class="group-name">${groupName}</span>
            <div class="group-actions">
                <input type="number" class="group-max-days" value="0" min="0" onchange="updateGroupMaxDays(this)">
                <button class="btn icon-btn" onclick="openUserAssignment(this)"><span class="icon user-icon">👥</span></button>
                <button class="btn icon-btn delete-btn" onclick="deleteGroup(this)"><span class="icon delete-icon">❌</span></button>
            </div>
        `;
        groupList.appendChild(newGroupItem);
        newGroupNameInput.value = '';
        alert(`Gruppe "${groupName}" erstellt! (Demo)`);
    } else {
        alert('Bitte geben Sie einen Gruppennamen ein.');
    }
}

function deleteGroup(buttonElement) {
    if (confirm('Sind Sie sicher, dass Sie diese Gruppe löschen möchten?')) {
        const groupItem = buttonElement.closest('.group-item');
        const groupName = groupItem.querySelector('.group-name').textContent;
        groupItem.remove();
        alert(`Gruppe "${groupName}" gelöscht! (Demo)`);
    }
}

function updateGroupMaxDays(inputElement) {
    const groupItem = inputElement.closest('.group-item');
    const groupName = groupItem.querySelector('.group-name').textContent;
    const maxDays = inputElement.value;
    console.log(`Max. Tage für Gruppe "${groupName}" aktualisiert auf ${maxDays}. (Demo)`);
    // TODO: Send to backend
}

// Neue Funktion für Azubi-Einstellungen
function updateAzubiMaxDays(inputElement) {
    const azubiItem = inputElement.closest('.azubi-item');
    const azubiName = azubiItem.querySelector('.azubi-name').textContent;
    let maxDays = inputElement.value.trim(); // Trim, falls Leerzeichen eingegeben werden
    
    // Optional: Wenn das Feld leer ist, soll es den Gruppenstandard nutzen
    if (maxDays === "") {
        maxDays = "Standard"; // Oder ein Wert, der im Backend "null" oder "nicht gesetzt" bedeutet
        inputElement.placeholder = "Gruppenstandard"; // Platzhalter setzen
    } else {
        maxDays = parseInt(maxDays); // In Zahl umwandeln
        if (isNaN(maxDays) || maxDays < 0) {
            alert("Bitte geben Sie eine gültige positive Zahl ein oder lassen Sie das Feld leer für den Gruppenstandard.");
            inputElement.value = ""; // Feld zurücksetzen
            inputElement.placeholder = "Gruppenstandard";
            return;
        }
        inputElement.placeholder = ""; // Platzhalter entfernen, wenn Wert gesetzt
    }

    console.log(`Max. Homeoffice-Tage für Azubi "${azubiName}" aktualisiert auf: ${maxDays}. (Demo)`);
    // TODO: Send to backend (mit Azubi-ID und maxDays Wert)
}


function acceptRequest(buttonElement) {
    const requestItem = buttonElement.closest('.request-item');
    const userName = requestItem.querySelector('.request-user').textContent;
    const date = requestItem.querySelector('.request-date').textContent;
    console.log(`Antrag von ${userName} für ${date} angenommen. (Demo)`);
    requestItem.remove();
    alert(`Antrag von ${userName} angenommen!`);
}

function rejectRequest(buttonElement) {
    const requestItem = buttonElement.closest('.request-item');
    const userName = requestItem.querySelector('.request-user').textContent;
    const date = requestItem.querySelector('.request-date').textContent;
    console.log(`Antrag von ${userName} für ${date} abgelehnt. (Demo)`);
    requestItem.remove();
    alert(`Antrag von ${userName} abgelehnt!`);
}
