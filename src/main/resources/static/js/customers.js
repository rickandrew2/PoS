// DataTable initialization
let customersTable;

// Get CSRF token
function getCsrfToken() {
    let token = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];

    if (!token) {
        const metaToken = document.querySelector('meta[name="_csrf"]');
        if (metaToken) {
            token = metaToken.getAttribute('content');
        }
    }
    return token ? decodeURIComponent(token) : null;
}

// Get CSRF header name
function getCsrfHeader() {
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    return metaHeader ? metaHeader.getAttribute('content') : 'X-XSRF-TOKEN';
}

document.addEventListener('DOMContentLoaded', function() {
    // Add CSRF token to all AJAX requests
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    if (csrfToken) {
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

    customersTable = $('#customersTable').DataTable({
        ajax: {
            url: '/customers/list',
            dataSrc: ''
        },
        columns: [
            { data: 'name' },
            { data: 'phoneNumber' },
            { data: 'email' },
            { data: 'address' },
            {
                data: null,
                render: function(data) {
                    let badges = [];
                    if (data.isPwd) badges.push('<span class="badge bg-info">PWD</span>');
                    if (data.isSenior) badges.push('<span class="badge bg-warning">Senior</span>');
                    return badges.join(' ');
                }
            },
            {
                data: null,
                render: function(data) {
                    return `
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-info" onclick="viewPurchaseHistory(${data.id})">
                                <i class="bi bi-clock-history"></i>
                            </button>
                            <button class="btn btn-outline-primary" onclick="editCustomer(${data.id})">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-danger" onclick="deleteCustomer(${data.id})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    `;
                }
            }
        ],
        order: [[0, 'asc']]
    });
});

// Customer Modal Functions
function openCustomerModal(customerId = null) {
    resetCustomerForm();
    if (customerId) {
        // Edit mode
        fetch(`/customers/${customerId}`, {
            headers: {
                [getCsrfHeader()]: getCsrfToken()
            }
        })
            .then(response => response.json())
            .then(customer => {
                document.getElementById('customerId').value = customer.id;
                document.getElementById('customerName').value = customer.name;
                document.getElementById('customerEmail').value = customer.email;
                document.getElementById('customerPhone').value = customer.phoneNumber;
                document.getElementById('customerAddress').value = customer.address;
                document.getElementById('isPwd').checked = customer.isPwd;
                document.getElementById('isSenior').checked = customer.isSenior;
            });
    }
    new bootstrap.Modal(document.getElementById('customerModal')).show();
}

function resetCustomerForm() {
    document.getElementById('customerForm').reset();
    document.getElementById('customerId').value = '';
}

function saveCustomer() {
    const customerId = document.getElementById('customerId').value;
    const customerData = {
        name: document.getElementById('customerName').value,
        email: document.getElementById('customerEmail').value,
        phoneNumber: document.getElementById('customerPhone').value,
        address: document.getElementById('customerAddress').value,
        isPwd: document.getElementById('isPwd').checked,
        isSenior: document.getElementById('isSenior').checked
    };

    const url = customerId ? `/customers/${customerId}` : '/customers';
    const method = customerId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            [getCsrfHeader()]: getCsrfToken()
        },
        body: JSON.stringify(customerData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        bootstrap.Modal.getInstance(document.getElementById('customerModal')).hide();
        customersTable.ajax.reload();
        Swal.fire({
            icon: 'success',
            title: 'Success!',
            text: `Customer successfully ${customerId ? 'updated' : 'created'}!`
        });
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Failed to save customer. Please try again.'
        });
    });
}

function deleteCustomer(customerId) {
    Swal.fire({
        title: 'Are you sure?',
        text: "This customer will be deactivated. You can reactivate them later.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Yes, deactivate'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/customers/${customerId}`, {
                method: 'DELETE',
                headers: {
                    [getCsrfHeader()]: getCsrfToken()
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                customersTable.ajax.reload();
                Swal.fire(
                    'Deactivated!',
                    'The customer has been deactivated.',
                    'success'
                );
            })
            .catch(error => {
                console.error('Error:', error);
                Swal.fire(
                    'Error!',
                    'Failed to deactivate customer.',
                    'error'
                );
            });
        }
    });
}

// Purchase History Functions
function viewPurchaseHistory(customerId) {
    fetch(`/customers/${customerId}/transactions`, {
        headers: {
            [getCsrfHeader()]: getCsrfToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            displayPurchaseHistory(data.customer, data.transactions);
            calculateAnalytics(data.transactions);
            new bootstrap.Modal(document.getElementById('purchaseHistoryModal')).show();
        })
        .catch(error => {
            console.error('Error:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Failed to load purchase history.'
            });
        });
}

function displayPurchaseHistory(customer, transactions) {
    // Display customer info
    document.getElementById('customerInfo').innerHTML = `
        <div class="row">
            <div class="col-md-6">
                <p><strong>Name:</strong> ${customer.name}</p>
                <p><strong>Email:</strong> ${customer.email || 'N/A'}</p>
            </div>
            <div class="col-md-6">
                <p><strong>Phone:</strong> ${customer.phoneNumber || 'N/A'}</p>
                <p><strong>Address:</strong> ${customer.address || 'N/A'}</p>
            </div>
        </div>
    `;

    // Initialize purchase history table
    if ($.fn.DataTable.isDataTable('#purchaseHistoryTable')) {
        $('#purchaseHistoryTable').DataTable().destroy();
    }

    $('#purchaseHistoryTable').DataTable({
        data: transactions,
        columns: [
            { 
                data: 'transactionDate',
                render: function(data) {
                    return new Date(data).toLocaleString();
                }
            },
            { data: 'receiptNumber' },
            {
                data: 'items',
                render: function(data) {
                    return data.length + ' items';
                }
            },
            {
                data: 'totalAmount',
                render: function(data) {
                    return '₱' + parseFloat(data).toFixed(2);
                }
            },
            {
                data: 'receiptNumber',
                render: function(data) {
                    return `
                        <button class="btn btn-sm btn-outline-primary" onclick="viewReceipt('${data}')">
                            View Receipt
                        </button>
                    `;
                }
            }
        ],
        order: [[0, 'desc']]
    });
}

function calculateAnalytics(transactions) {
    if (!transactions.length) {
        document.getElementById('totalPurchases').textContent = '₱0.00';
        document.getElementById('avgPurchase').textContent = '₱0.00';
        document.getElementById('visitFrequency').textContent = '0 visits/month';
        return;
    }

    // Calculate total purchases
    const total = transactions.reduce((sum, t) => sum + parseFloat(t.totalAmount), 0);
    document.getElementById('totalPurchases').textContent = '₱' + total.toFixed(2);

    // Calculate average purchase
    const avg = total / transactions.length;
    document.getElementById('avgPurchase').textContent = '₱' + avg.toFixed(2);

    // Calculate visit frequency
    const firstDate = new Date(transactions[transactions.length - 1].transactionDate);
    const lastDate = new Date(transactions[0].transactionDate);
    const monthsDiff = (lastDate - firstDate) / (1000 * 60 * 60 * 24 * 30);
    const frequency = monthsDiff > 0 ? (transactions.length / monthsDiff).toFixed(1) : transactions.length;
    document.getElementById('visitFrequency').textContent = `${frequency} visits/month`;
}

function viewReceipt(receiptNumber) {
    window.open(`/sales/receipt/${receiptNumber}`, '_blank');
} 