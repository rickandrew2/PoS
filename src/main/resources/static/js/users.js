// DataTables initialization
$(document).ready(function() {
    $('#usersTable').DataTable({
        order: [[0, 'asc']],
        pageLength: 10,
        language: {
            search: "Search users:"
        }
    });
});

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

function deleteUser(button) {
    const userId = button.dataset.id;
    const username = button.dataset.username;

    if (confirm(`Are you sure you want to delete user ${username}?`)) {
        fetch(`/users/api/users/${userId}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            location.reload();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting user: ' + error.message);
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

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
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