// Calendar elements
const calendarDiv = document.getElementById('calendar');
const currentMonthYear = document.getElementById('currentMonthYear');
const prevMonthBtn = document.getElementById('prevMonth');
const nextMonthBtn = document.getElementById('nextMonth');

// Popup elements
const popup = document.getElementById('requestPopup');
const closePopup = document.querySelector('.close-popup');
const requestForm = document.getElementById('requestForm');
const requestDateInput = document.getElementById('requestDate');
const halfDayCheckbox = document.getElementById('halfDay');

let currentDate = new Date();
let selectedDate = null;

// Load existing requests
async function loadRequests() {
    try {
        const response = await fetch('/api/homeoffice/my-requests', {
            credentials: 'include'
        });
        if (response.ok) {
            const requests = await response.json();
            renderCalendar(requests);
        }
    } catch (error) {
        console.error('Error loading requests:', error);
    }
}

// Submit request
async function submitRequest(date, halfDay) {
    try {
        const csrfToken = getCookie('XSRF-TOKEN');
        if (!csrfToken) {
            throw new Error('CSRF token not found');
        }

        const response = await fetch('/api/homeoffice/request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            credentials: 'include',
            body: JSON.stringify({
                requestDate: date,
                halfDay: halfDay,
                status: 'PENDING'
            })
        });

        if (response.ok) {
            alert('Antrag erfolgreich eingereicht!');
            popup.style.display = 'none';
            await loadRequests();
        } else {
            const error = await response.text();
            alert('Fehler beim Einreichen des Antrags: ' + error);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Ein Fehler ist aufgetreten: ' + error.message);
    }
}

// Render calendar
function renderCalendar(requests = []) {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    currentMonthYear.textContent = new Date(year, month).toLocaleDateString('de-DE', { 
        month: 'long', 
        year: 'numeric' 
    });
    
    calendarDiv.innerHTML = '';
    
    // Add day headers
    const days = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];
    days.forEach(day => {
        const dayHeader = document.createElement('div');
        dayHeader.className = 'calendar-day-header';
        dayHeader.textContent = day;
        calendarDiv.appendChild(dayHeader);
    });
    
    // Get first day of month and total days
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const totalDays = lastDay.getDate();
    const startingDay = firstDay.getDay();
    
    // Add empty cells for days before first of month
    for (let i = 0; i < startingDay; i++) {
        const emptyDay = document.createElement('div');
        emptyDay.className = 'calendar-day empty';
        calendarDiv.appendChild(emptyDay);
    }
    
    // Add days of month
    for (let day = 1; day <= totalDays; day++) {
        const dayElement = document.createElement('div');
        dayElement.className = 'calendar-day';
        
        const currentDay = new Date(year, month, day);
        const isPast = currentDay < new Date(new Date().setHours(0,0,0,0));
        
        if (!isPast) {
            dayElement.classList.add('clickable');
            dayElement.setAttribute('data-date', currentDay.toISOString().split('T')[0]);
        }
        
        dayElement.textContent = day;
        
        // Add request indicators
        const dayRequests = requests.filter(r => 
            new Date(r.requestDate).toDateString() === currentDay.toDateString()
        );
        
        dayRequests.forEach(request => {
            const requestIndicator = document.createElement('div');
            requestIndicator.className = `homeoffice-entry ${request.halfDay ? 'half-day' : ''} ${request.status.toLowerCase()}`;
            requestIndicator.textContent = request.halfDay ? '½' : 'G';
            dayElement.appendChild(requestIndicator);
        });
        
        calendarDiv.appendChild(dayElement);
    }
    
    // Add click handlers for calendar days
    const clickableDays = document.querySelectorAll('.calendar-day.clickable');
    clickableDays.forEach(day => {
        day.onclick = function() {
            const date = this.getAttribute('data-date');
            showPopup(date);
        }
    });
}

// Show popup when clicking a date
function showPopup(date) {
    requestDateInput.value = date;
    popup.style.display = 'block';
}

// Close popup
closePopup.onclick = function() {
    popup.style.display = 'none';
}

// Close popup when clicking outside
window.onclick = function(event) {
    if (event.target == popup) {
        popup.style.display = 'none';
    }
}

// Handle form submission
requestForm.onsubmit = async function(e) {
    e.preventDefault();
    await submitRequest(requestDateInput.value, halfDayCheckbox.checked);
}

// Navigation buttons
prevMonthBtn.onclick = function() {
    currentDate.setMonth(currentDate.getMonth() - 1);
    loadRequests();
}

nextMonthBtn.onclick = function() {
    currentDate.setMonth(currentDate.getMonth() + 1);
    loadRequests();
}

// Helper function to get CSRF token
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Initial load
loadRequests(); 
renderCalendar(); 