// Die Kalenderlogik bleibt im Wesentlichen gleich.
// Sie wird von main.js aufgerufen, sobald der Benutzer authentifiziert ist.
// Änderungen nur in der Fetch-Logik, um CSRF-Token zu senden.

let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth();

const monthNames = [
    "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember"
];

const calendarDiv = document.getElementById('calendar');
const currentMonthYearHeader = document.getElementById('currentMonthYear');
const prevMonthBtn = document.getElementById('prevMonth');
const nextMonthBtn = document.getElementById('nextMonth');

prevMonthBtn.addEventListener('click', () => {
    currentMonth--;
    if (currentMonth < 0) {
        currentMonth = 11;
        currentYear--;
    }
    renderCalendar();
});

nextMonthBtn.addEventListener('click', () => {
    currentMonth++;
    if (currentMonth > 11) {
        currentMonth = 0;
        currentYear++;
    }
    renderCalendar();
});

async function renderCalendar() {
    currentMonthYearHeader.textContent = `${monthNames[currentMonth]} ${currentYear}`;
    calendarDiv.innerHTML = '';

    const dayNames = ["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"];
    dayNames.forEach(day => {
        const dayHeader = document.createElement('div');
        dayHeader.classList.add('calendar-day-header');
        dayHeader.textContent = day;
        calendarDiv.appendChild(dayHeader);
    });

    const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
    const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();

    let firstDayOfWeek = firstDayOfMonth.getDay();
    if (firstDayOfWeek === 0) firstDayOfWeek = 7;
    firstDayOfWeek = firstDayOfWeek -1;

    for (let i = 0; i < firstDayOfWeek; i++) {
        const emptyDiv = document.createElement('div');
        emptyDiv.classList.add('calendar-day', 'empty');
        calendarDiv.appendChild(emptyDiv);
    }

    let homeofficeRequests = [];
    try {
        const response = await fetch(`${API_BASE_URL}/homeoffice/month/${currentYear}/${currentMonth + 1}`, {
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') } // CSRF Token senden
        });
        if (response.ok) {
            homeofficeRequests = await response.json();
        } else {
            console.error('Fehler beim Laden der Homeoffice-Anträge:', await response.text());
        }
    } catch (error) {
        console.error('Netzwerkfehler beim Laden der Homeoffice-Anträge:', error);
    }

    for (let day = 1; day <= daysInMonth; day++) {
        const dayDiv = document.createElement('div');
        dayDiv.classList.add('calendar-day');
        dayDiv.innerHTML = `<div class="day-number">${day}</div>`;

        const currentDate = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

        const requestsForDay = homeofficeRequests.filter(req => req.requestDate === currentDate);

        requestsForDay.forEach(req => {
            const entryDiv = document.createElement('div');
            entryDiv.classList.add('homeoffice-entry');
            entryDiv.textContent = `${req.username} ${req.halfDay ? '(H)' : ''}`;

            entryDiv.classList.add(req.status.toLowerCase());
            if (req.halfDay) {
                entryDiv.classList.add('half-day');
            }

            dayDiv.appendChild(entryDiv);
        });

        calendarDiv.appendChild(dayDiv);
    }
}

// getCookie Funktion muss auch hier verfügbar sein, oder global definiert in main.js
// Wenn main.js zuerst geladen wird, ist es bereits verfügbar.
// Wenn nicht, kopiere die Funktion hierher:
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}