// Constants
const VAT_RATE = 0.12; // 12% VAT
const DISCOUNT_RATE = 0.20; // 20% for PWD/Senior

// Cart state
let cart = [];

// DOM Elements
const customerSelect = document.getElementById('customerSelect');
const vatIncludedCheckbox = document.getElementById('vatIncluded');
const pwdDiscountCheckbox = document.getElementById('pwdDiscount');
const seniorDiscountCheckbox = document.getElementById('seniorDiscount');
const discountOptionsDiv = document.getElementById('discountOptions');
const cartItemsTable = document.getElementById('cartItems');
const subtotalSpan = document.getElementById('subtotal');
const vatAmountSpan = document.getElementById('vatAmount');
const discountAmountSpan = document.getElementById('discountAmount');
const totalAmountSpan = document.getElementById('totalAmount');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Product card clicks
    document.querySelectorAll('.product-card').forEach(card => {
        card.addEventListener('click', () => {
            const productId = card.dataset.id;
            const productName = card.dataset.name;
            const productPrice = parseFloat(card.dataset.price);
            addToCart(productId, productName, productPrice);
        });
    });

    // Customer selection change
    customerSelect.addEventListener('change', () => {
        const selectedOption = customerSelect.options[customerSelect.selectedIndex];
        discountOptionsDiv.style.display = selectedOption.value ? 'block' : 'none';
    });

    // Discount checkboxes (only one can be selected)
    pwdDiscountCheckbox.addEventListener('change', () => {
        if (pwdDiscountCheckbox.checked) {
            seniorDiscountCheckbox.checked = false;
        }
        updateTotals();
    });

    seniorDiscountCheckbox.addEventListener('change', () => {
        if (seniorDiscountCheckbox.checked) {
            pwdDiscountCheckbox.checked = false;
        }
        updateTotals();
    });

    // VAT checkbox
    vatIncludedCheckbox.addEventListener('change', updateTotals);

    // Process transaction button
    document.getElementById('processTransaction').addEventListener('click', processTransaction);

    // Clear cart button
    document.getElementById('clearCart').addEventListener('click', clearCart);

    // Product search
    document.getElementById('searchProduct').addEventListener('input', filterProducts);

    // Category filter
    document.getElementById('categoryFilter').addEventListener('change', filterProducts);
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

function updateTotals() {
    const subtotal = calculateSubtotal();
    const vatAmount = calculateVAT(subtotal);
    const discountAmount = calculateDiscount(subtotal);
    const total = subtotal + vatAmount - discountAmount;

    subtotalSpan.textContent = subtotal.toFixed(2);
    vatAmountSpan.textContent = vatAmount.toFixed(2);
    discountAmountSpan.textContent = discountAmount.toFixed(2);
    totalAmountSpan.textContent = total.toFixed(2);
}

// Calculation Functions
function calculateSubtotal() {
    return cart.reduce((sum, item) => sum + item.total, 0);
}

function calculateVAT(subtotal) {
    return vatIncludedCheckbox.checked ? subtotal * VAT_RATE : 0;
}

function calculateDiscount(subtotal) {
    return (pwdDiscountCheckbox.checked || seniorDiscountCheckbox.checked) ? subtotal * DISCOUNT_RATE : 0;
}

// Filter Functions
function filterProducts() {
    const searchTerm = document.getElementById('searchProduct').value.toLowerCase();
    const categoryId = document.getElementById('categoryFilter').value;
    const products = document.querySelectorAll('#productsGrid .col');

    products.forEach(product => {
        const name = product.querySelector('.card-title').textContent.toLowerCase();
        const category = product.querySelector('.text-muted').textContent;
        const matchesSearch = name.includes(searchTerm);
        const matchesCategory = !categoryId || product.dataset.categoryId === categoryId;
        
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

    const transaction = {
        customerId: customerSelect.value || null,
        items: cart.map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            subtotal: item.total
        })),
        vatIncluded: vatIncludedCheckbox.checked,
        isPwdDiscount: pwdDiscountCheckbox.checked,
        isSeniorDiscount: seniorDiscountCheckbox.checked,
        subtotal: subtotal,
        vatAmount: vatAmount,
        discountAmount: discountAmount,
        totalAmount: totalAmount
    };

    fetch('/api/transactions', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem('jwt_token')
        },
        body: JSON.stringify(transaction)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                try {
                    const error = JSON.parse(text);
                    throw new Error(error.message || 'Error processing transaction');
                } catch (e) {
                    throw new Error(text || 'Error processing transaction');
                }
            });
        }
        return response.json();
    })
    .then(data => {
        showReceipt(data);
        clearCart();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error processing transaction: ' + error.message);
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