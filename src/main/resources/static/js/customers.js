// DataTables initialization
$(document).ready(function() {
    $('#customersTable').DataTable({
        order: [[0, 'asc']],
        pageLength: 10,
        language: {
            search: "Search customers:"
        }
    });
});

// Customer Management
function editCustomer(button) {
    const customerId = button.dataset.id;
    const name = button.dataset.name;
    const email = button.dataset.email;
    const phone = button.dataset.phone;
    const address = button.dataset.address;
    const isPwd = button.dataset.pwd === 'true';
    const isSenior = button.dataset.senior === 'true';

    document.getElementById('customerId').value = customerId;
    document.getElementById('customerName').value = name;
    document.getElementById('customerEmail').value = email;
    document.getElementById('customerPhone').value = phone;
    document.getElementById('customerAddress').value = address;
    document.getElementById('customerPwd').checked = isPwd;
    document.getElementById('customerSenior').checked = isSenior;

    const modal = new bootstrap.Modal(document.getElementById('customerModal'));
    modal.show();
}

function deleteCustomer(button) {
    const customerId = button.dataset.id;
    const customerName = button.dataset.name;

    if (confirm(`Are you sure you want to delete ${customerName}?`)) {
        fetch(`/customers/api/customers/${customerId}`, {
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
            alert('Error deleting customer: ' + error.message);
        });
    }
}

function saveCustomer() {
    const customerId = document.getElementById('customerId').value;
    const customer = {
        name: document.getElementById('customerName').value,
        email: document.getElementById('customerEmail').value,
        phoneNumber: document.getElementById('customerPhone').value,
        address: document.getElementById('customerAddress').value,
        isPwd: document.getElementById('customerPwd').checked,
        isSenior: document.getElementById('customerSenior').checked
    };

    const url = customerId ? `/customers/api/customers/${customerId}` : '/customers/api/customers';
    const method = customerId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(customer)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error saving customer: ' + error.message);
    });
}

// Modal Reset Handler
document.getElementById('customerModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('customerForm').reset();
    document.getElementById('customerId').value = '';
}); 