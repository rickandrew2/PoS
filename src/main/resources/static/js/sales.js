// Constants
const VAT_RATE = 0.12; // 12% VAT
const DISCOUNT_RATE = 0.20; // 20% for PWD/Senior

// Cart state
let cart = [];

// DOM Elements
// const customerSelect = document.getElementById('customerSelect'); // Removed because not present in HTML
const vatIncludedCheckbox = document.getElementById('vatIncluded');
const pwdDiscountCheckbox = document.getElementById('pwdDiscount');
const seniorDiscountCheckbox = document.getElementById('seniorDiscount');
const discountOptionsDiv = document.getElementById('discountOptions');
const cartItemsTable = document.getElementById('cartItems');
const subtotalSpan = document.getElementById('subtotal');
const vatAmountSpan = document.getElementById('vatAmount');
const discountAmountSpan = document.getElementById('discountAmount');
const totalAmountSpan = document.getElementById('totalAmount');
const customerTypeSelect = document.getElementById('customerTypeSelect');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Product card clicks
    document.querySelectorAll('.product-card').forEach(card => {
        card.addEventListener('click', () => {
            if (card.classList.contains('out-of-stock') || card.getAttribute('data-out-of-stock') === 'true') {
                alert('This product is out of stock.');
                return;
            }
            const productId = card.dataset.id;
            const productName = card.dataset.name;
            const productPrice = parseFloat(card.dataset.price);
            addToCart(productId, productName, productPrice);
        });
    });

    // Process transaction button
    document.getElementById('processTransaction').addEventListener('click', processTransaction);

    // Clear cart button
    document.getElementById('clearCart').addEventListener('click', clearCart);

    // Product search
    document.getElementById('searchProduct').addEventListener('input', filterProducts);

    // Category filter
    document.getElementById('categoryFilter').addEventListener('change', filterProducts);

    // Customer type select change
    if (customerTypeSelect) {
        customerTypeSelect.addEventListener('change', () => {
            const selectedOption = customerTypeSelect.options[customerTypeSelect.selectedIndex];
            if (!selectedOption.value) {
                // Walk-in Customer: remove all discounts
                pwdDiscountCheckbox.checked = false;
                seniorDiscountCheckbox.checked = false;
            } else {
                const isPwd = selectedOption.getAttribute('data-ispwd') === 'true';
                const isSenior = selectedOption.getAttribute('data-issenior') === 'true';
            }
            updateTotals();
        });
    }

    // Ensure filter is applied on load
    filterProducts();

    // Transaction History Modal logic
    if (document.getElementById('viewTransactionHistoryBtn')) {
        // Add date filter and pagination controls
        const modalBody = document.querySelector('#transactionHistoryModal .modal-body');
        if (modalBody && !document.getElementById('historyFilters')) {
            const filterDiv = document.createElement('div');
            filterDiv.id = 'historyFilters';
            filterDiv.className = 'mb-3 d-flex gap-2 align-items-center';
            filterDiv.innerHTML = `
                <label>Start Date: <input type="date" id="historyStartDate" class="form-control form-control-sm d-inline-block" style="width:auto;"></label>
                <label>End Date: <input type="date" id="historyEndDate" class="form-control form-control-sm d-inline-block" style="width:auto;"></label>
                <button class="btn btn-sm btn-primary" id="historyFilterBtn">Filter</button>
                <div class="ms-auto" id="historyPagination"></div>
            `;
            modalBody.prepend(filterDiv);
        }

        let currentPage = 0;
        let totalPages = 1;
        let lastStartDate = '';
        let lastEndDate = '';

        function fetchAndRenderHistory(page = 0) {
            const startDate = document.getElementById('historyStartDate').value;
            const endDate = document.getElementById('historyEndDate').value;
            lastStartDate = startDate;
            lastEndDate = endDate;
            let url = `/sales/api/history?page=${page}`;
            if (startDate) url += `&startDate=${startDate}`;
            if (endDate) url += `&endDate=${endDate}`;
            fetch(url)
                .then(res => res.json())
                .then(data => {
                    const tbody = document.querySelector('#transactionHistoryTable tbody');
                    tbody.innerHTML = '';
                    if (!data.content || data.content.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="5" class="text-center">No transactions found.</td></tr>';
                    } else {
                        data.content.forEach(tx => {
                            const date = new Date(tx.transactionDate).toLocaleString();
                            const receipt = tx.receiptNumber;
                            const customer = tx.customerName || 'Walk-in';
                            const total = tx.totalAmount ? `₱${parseFloat(tx.totalAmount).toFixed(2)}` : '';
                            const cashier = tx.cashierName || '';
                            tbody.innerHTML += `
                                <tr>
                                    <td>${date}</td>
                                    <td>${receipt}</td>
                                    <td>${customer}</td>
                                    <td>${total}</td>
                                    <td>${cashier}</td>
                                </tr>
                            `;
                        });
                    }
                    // Pagination controls
                    currentPage = data.page;
                    totalPages = data.totalPages;
                    renderHistoryPagination();
                });
        }

        function renderHistoryPagination() {
            const pagDiv = document.getElementById('historyPagination');
            if (!pagDiv) return;
            let html = '';
            if (totalPages > 1) {
                html += `<button class="btn btn-sm btn-outline-secondary me-1" ${currentPage === 0 ? 'disabled' : ''} id="historyPrevBtn">&laquo; Prev</button>`;
                for (let i = 0; i < totalPages; i++) {
                    html += `<button class="btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-outline-primary'} me-1" data-page="${i}">${i + 1}</button>`;
                }
                html += `<button class="btn btn-sm btn-outline-secondary" ${currentPage === totalPages - 1 ? 'disabled' : ''} id="historyNextBtn">Next &raquo;</button>`;
            }
            pagDiv.innerHTML = html;
            // Add event listeners
            if (totalPages > 1) {
                if (document.getElementById('historyPrevBtn')) {
                    document.getElementById('historyPrevBtn').onclick = () => fetchAndRenderHistory(currentPage - 1);
                }
                if (document.getElementById('historyNextBtn')) {
                    document.getElementById('historyNextBtn').onclick = () => fetchAndRenderHistory(currentPage + 1);
                }
                pagDiv.querySelectorAll('button[data-page]').forEach(btn => {
                    btn.onclick = () => fetchAndRenderHistory(Number(btn.getAttribute('data-page')));
                });
            }
        }

        document.getElementById('viewTransactionHistoryBtn').addEventListener('click', function() {
            fetchAndRenderHistory(0);
            new bootstrap.Modal(document.getElementById('transactionHistoryModal')).show();
        });
        if (document.getElementById('historyFilterBtn')) {
            document.getElementById('historyFilterBtn').onclick = () => fetchAndRenderHistory(0);
        }
    }
});

// Cart Functions
function addToCart(productId, productName, price) {
    const existingItem = cart.find(item => item.productId === productId);
    
    if (existingItem) {
        existingItem.quantity++;
        existingItem.total = existingItem.quantity * existingItem.unitPrice;
    } else {
        cart.push({
            productId: productId,
            name: productName,
            quantity: 1,
            unitPrice: price,
            total: price
        });
    }
    
    updateCartDisplay();
    updateTotals();
}

function removeFromCart(index) {
    cart.splice(index, 1);
    updateCartDisplay();
    updateTotals();
}

function updateQuantity(index, newQuantity) {
    if (newQuantity > 0) {
        cart[index].quantity = newQuantity;
        cart[index].total = newQuantity * cart[index].unitPrice;
        updateCartDisplay();
        updateTotals();
    }
}

function clearCart() {
    cart = [];
    updateCartDisplay();
    updateTotals();
}

// Display Functions
function updateCartDisplay() {
    const tbody = cartItemsTable.querySelector('tbody');
    tbody.innerHTML = '';
    
    cart.forEach((item, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${item.name}</td>
            <td>
                <input type="number" value="${item.quantity}" min="1" 
                       class="form-control form-control-sm" style="width: 60px"
                       onchange="updateQuantity(${index}, this.value)">
            </td>
            <td>₱${item.unitPrice.toFixed(2)}</td>
            <td>₱${item.total.toFixed(2)}</td>
            <td>
                <button class="btn btn-sm btn-danger" onclick="removeFromCart(${index})">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function getSelectedCustomerType() {
    const selectedOption = customerTypeSelect.options[customerTypeSelect.selectedIndex];
    return {
        isPwd: selectedOption.getAttribute('data-ispwd') === 'true',
        isSenior: selectedOption.getAttribute('data-issenior') === 'true'
    };
}

function updateTotals() {
    const subtotal = calculateSubtotal();
    let discount = 0;
    let discountLabel = 'Discount:';

    const { isPwd, isSenior } = getSelectedCustomerType();
    if (isPwd) {
        discount = subtotal * DISCOUNT_RATE;
        discountLabel = 'PWD Discount (20%):';
    } else if (isSenior) {
        discount = subtotal * DISCOUNT_RATE;
        discountLabel = 'Senior Discount (20%):';
    }

    const discountedSubtotal = subtotal - discount;
    const vatAmount = discountedSubtotal * VAT_RATE;
    const total = discountedSubtotal + vatAmount;

    subtotalSpan.textContent = subtotal.toFixed(2);
    vatAmountSpan.textContent = vatAmount.toFixed(2);
    discountAmountSpan.textContent = discount.toFixed(2);
    totalAmountSpan.textContent = total.toFixed(2);
    document.getElementById('discountLabel').textContent = discountLabel;
}

// Calculation Functions
function calculateSubtotal() {
    return cart.reduce((sum, item) => sum + item.total, 0);
}

function calculateVAT(subtotal) {
    return vatIncludedCheckbox.checked ? subtotal * VAT_RATE : 0;
}

function calculateDiscount(subtotal) {
    const { isPwd, isSenior } = getSelectedCustomerType();
    return (isPwd || isSenior) ? subtotal * DISCOUNT_RATE : 0;
}

// Filter Functions
function filterProducts() {
    const searchTerm = document.getElementById('searchProduct').value.toLowerCase();
    const categoryId = document.getElementById('categoryFilter').value;
    const products = document.querySelectorAll('#productsGrid .col');

    products.forEach(product => {
        const name = product.querySelector('.card-title').textContent.toLowerCase();
        const productCategoryId = product.dataset.categoryId || "";
        const matchesSearch = name.includes(searchTerm);
        // If categoryId is empty, show all. Otherwise, match by string.
        const matchesCategory = !categoryId || productCategoryId === categoryId;
        product.style.display = matchesSearch && matchesCategory ? '' : 'none';
    });
}

// Transaction Processing
function processTransaction() {
    if (cart.length === 0) {
        alert('Cart is empty!');
        return;
    }

    const subtotal = calculateSubtotal();
    const vatAmount = calculateVAT(subtotal);
    const discountAmount = calculateDiscount(subtotal);
    const totalAmount = subtotal + vatAmount - discountAmount;

    const { isPwd, isSenior } = getSelectedCustomerType();

    const transaction = {
        customerTypeId: customerTypeSelect.value || null,
        items: cart.map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            subtotal: item.total
        })),
        vatIncluded: vatIncludedCheckbox.checked,
        subtotal: subtotal,
        vatAmount: vatAmount,
        discountAmount: discountAmount,
        totalAmount: totalAmount,
        isPwdDiscount: isPwd,
        isSeniorDiscount: isSenior
    };

    // Disable the process button and show loading state
    const processButton = document.getElementById('processTransaction');
    const originalText = processButton.innerHTML;
    processButton.disabled = true;
    processButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';

    // Get CSRF token from cookie or meta tag
    let csrfToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];

    // If not found in cookie, try meta tag
    if (!csrfToken) {
        const metaToken = document.querySelector('meta[name="_csrf"]');
        if (metaToken) {
            csrfToken = metaToken.getAttribute('content');
        }
    }

    // Prepare headers
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    // Add CSRF token if available
    if (csrfToken) {
        // Try to get header name from meta tag, default to X-XSRF-TOKEN
        const metaHeader = document.querySelector('meta[name="_csrf_header"]');
        const headerName = metaHeader ? metaHeader.getAttribute('content') : 'X-XSRF-TOKEN';
        headers[headerName] = decodeURIComponent(csrfToken);
    }

    fetch('/sales/process', {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin',
        body: JSON.stringify(transaction)
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || 'Transaction failed');
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Transaction response:', data);
        if (data.success) {
            // Show success message
            Swal.fire({
                icon: 'success',
                title: 'Success!',
                text: `Transaction processed successfully. Receipt #${data.receiptNumber}`,
                confirmButtonText: 'View Receipt'
            }).then((result) => {
                if (result.isConfirmed) {
                    // Ensure receiptUrl is properly formatted
                    const receiptUrl = data.receiptUrl;
                    console.log('Receipt URL:', receiptUrl);
                    if (!receiptUrl) {
                        console.error('Receipt URL is undefined');
                        Swal.fire({
                            icon: 'error',
                            title: 'Error',
                            text: 'Could not generate receipt URL. Please contact support.'
                        });
                        return;
                    }
                    // Redirect to receipt page
                    console.log('Redirecting to:', receiptUrl);
                    window.location.href = receiptUrl;
                } else {
                    // Clear the cart and reset the form
                    clearCart();
                    customerTypeSelect.value = '';
                    pwdDiscountCheckbox.checked = false;
                    seniorDiscountCheckbox.checked = false;
                    discountOptionsDiv.style.display = 'none';
                }
            });
        } else {
            throw new Error(data.message || 'Transaction failed');
        }
    })
    .catch(error => {
        // Show error message
        Swal.fire({
            icon: 'error',
            title: 'Error!',
            text: error.message || 'Failed to process transaction. Please try again.',
            confirmButtonText: 'OK'
        });
    })
    .finally(() => {
        // Re-enable the process button
        processButton.disabled = false;
        processButton.innerHTML = originalText;
    });
}

// Receipt Generation
function showReceipt(transaction) {
    const receiptModal = new bootstrap.Modal(document.getElementById('receiptModal'));
    const receiptContent = document.getElementById('receiptContent');
    const date = new Date(transaction.transactionDate);
    
    receiptContent.innerHTML = `
        <div class="text-center mb-3">
            <h4>POS SYSTEM</h4>
            <p>THIS SERVES AS AN OFFICIAL RECEIPT</p>
            <p>123 Main Street, City<br>
            Tel: (123) 456-7890</p>
        </div>
        
        <div class="mb-3">
            <div>Receipt #: ${transaction.receiptNumber}</div>
            <div>Date: ${date.toLocaleString()}</div>
            <div>Cashier: ${transaction.cashier.name}</div>
            ${transaction.customer ? `<div>Customer: ${transaction.customer.name}</div>` : ''}
        </div>
        
        <div class="mb-3">
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Qty</th>
                        <th>Price</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    ${transaction.items.map(item => `
                        <tr>
                            <td>${item.product.name}</td>
                            <td>${item.quantity}</td>
                            <td>₱${item.unitPrice.toFixed(2)}</td>
                            <td>₱${item.subtotal.toFixed(2)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
        
        <div class="mb-3">
            <div class="d-flex justify-content-between">
                <span>Subtotal:</span>
                <span>₱${transaction.subtotal.toFixed(2)}</span>
            </div>
            ${transaction.vatAmount > 0 ? `
                <div class="d-flex justify-content-between">
                    <span>VAT (12%):</span>
                    <span>₱${transaction.vatAmount.toFixed(2)}</span>
                </div>
            ` : ''}
            ${transaction.discountAmount > 0 ? `
                <div class="d-flex justify-content-between">
                    <span>Discount (20%):</span>
                    <span>₱${transaction.discountAmount.toFixed(2)}</span>
                </div>
            ` : ''}
            <div class="d-flex justify-content-between fw-bold">
                <span>Total Amount:</span>
                <span>₱${transaction.totalAmount.toFixed(2)}</span>
            </div>
        </div>
        
        <div class="text-center mt-4">
            <p>Thank you for your purchase!</p>
        </div>
    `;
    
    receiptModal.show();
} 