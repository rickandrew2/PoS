// Remove DataTables initialization and use a custom search filter
$(document).ready(function() {
    $('#customUserSearch').on('keyup', function() {
        var value = this.value.toLowerCase();
        var anyVisible = false;
        $('#usersTable tbody tr').each(function() {
            var rowText = $(this).text().toLowerCase();
            var match = rowText.indexOf(value) > -1;
            $(this).toggle(match);
            if (match) anyVisible = true;
        });
        // Remove any existing placeholder
        $('#usersTable tbody .no-results-row').remove();
        if (!anyVisible) {
            var colspan = $('#usersTable thead th').length;
            $('#usersTable tbody').append('<tr class="no-results-row"><td colspan="' + colspan + '" class="text-center">No employee found.</td></tr>');
        }
    });
});

// CSRF token setup
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

// User Management
function editUser(button) {
    const userId = button.dataset.id;
    const username = button.dataset.username;
    const fullName = button.dataset.fullname;
    const role = button.dataset.role;

    document.getElementById('userId').value = userId;
    document.getElementById('username').value = username;
    document.getElementById('password').value = '';
    document.getElementById('fullName').value = fullName;
    document.getElementById('role').value = role;

    const modal = new bootstrap.Modal(document.getElementById('userModal'));
    modal.show();
}

// Add a function to show a Bootstrap alert
function showAlert(message, type = 'success') {
    const alertPlaceholder = document.getElementById('alertPlaceholder');
    alertPlaceholder.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}

function deleteUser(button) {
    const userId = button.dataset.id;
    const username = button.dataset.username;

    if (confirm(`Are you sure you want to delete user ${username}?`)) {
        let headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        fetch(`/users/api/users/${userId}`, {
            method: 'DELETE',
            headers: headers
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            showAlert('The user was deleted successfully.', 'success');
            setTimeout(() => location.reload(), 1200);
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('Error deleting user: ' + error.message, 'danger');
        });
    }
}

function saveUser() {
    const userId = document.getElementById('userId').value;
    const user = {
        username: document.getElementById('username').value,
        fullName: document.getElementById('fullName').value,
        role: document.getElementById('role').value
    };

    // Only include password if it's not empty
    const password = document.getElementById('password').value;
    if (password) {
        user.password = password;
    }

    const url = userId ? `/users/api/users/${userId}` : '/users/api/users';
    const method = userId ? 'PUT' : 'POST';

    let headers = {
        'Content-Type': 'application/json',
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(url, {
        method: method,
        headers: headers,
        body: JSON.stringify(user)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error saving user: ' + error.message);
    });
}

// Modal Reset Handler
document.getElementById('userModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
}); 