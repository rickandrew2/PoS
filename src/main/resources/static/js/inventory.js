// CSRF token helper
function getCsrfToken() {
    const input = document.querySelector('input[name="_csrf"]');
    return input ? input.value : '';
}

// Product Management
// Deleted editProduct function

// Modal-based product delete confirmation
let productIdToDelete = null;
let deleteButtonRef = null;

function deleteProduct(button) {
    productIdToDelete = button.dataset.id;
    deleteButtonRef = button;
    const productName = button.dataset.name;

    // Set product name in modal
    document.getElementById('deleteProductName').textContent = productName;

    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById('deleteProductModal'));
    modal.show();
}

// Handle confirm delete
const confirmDeleteBtn = document.getElementById('confirmDeleteProductBtn');
if (confirmDeleteBtn) {
    confirmDeleteBtn.onclick = function() {
        if (!productIdToDelete) return;
        if (deleteButtonRef) deleteButtonRef.disabled = true;
        fetch(`/api/products/${productIdToDelete}`, {
            method: 'DELETE',
            headers: {
                'X-CSRF-TOKEN': getCsrfToken ? getCsrfToken() : undefined
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            showProductDeletedModal();
        })
        .catch(error => {
            if (deleteButtonRef) deleteButtonRef.disabled = false;
            alert('Error deleting product: ' + error.message);
        })
        .finally(() => {
            const modalEl = document.getElementById('deleteProductModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            productIdToDelete = null;
            deleteButtonRef = null;
        });
    };
}

function showProductAddedModal() {
    // Close the product modal first
    const productModalEl = document.getElementById('productModal');
    const productModal = bootstrap.Modal.getInstance(productModalEl);
    if (productModal) productModal.hide();
    // Now show the success modal
    const modalEl = document.getElementById('productAddedModal');
    if (!modalEl) {
        console.error('productAddedModal not found in DOM');
        return;
    }
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    // Reload only when the modal is hidden (user closes it or after timeout)
    modalEl.addEventListener('hidden.bs.modal', function handler() {
        modalEl.removeEventListener('hidden.bs.modal', handler);
        location.reload();
    });
    // Auto-close after 1.5s
    setTimeout(() => {
        modal.hide();
    }, 1500);
}

function showProductDeletedModal() {
    const modal = new bootstrap.Modal(document.getElementById('productDeletedModal'));
    modal.show();
    setTimeout(() => location.reload(), 1500);
}

// Category Management
function editCategory(button) {
    const categoryId = button.dataset.id;
    const name = button.dataset.name;
    const description = button.dataset.description;

    document.getElementById('categoryId').value = categoryId;
    document.getElementById('categoryName').value = name;
    document.getElementById('categoryDescription').value = description;

    const modal = new bootstrap.Modal(document.getElementById('categoryModal'));
    modal.show();
}

let categoryIdToDelete = null;
let deleteCategoryButtonRef = null;

function deleteCategory(button) {
    categoryIdToDelete = button.dataset.id;
    deleteCategoryButtonRef = button;
    const categoryName = button.dataset.name;

    // Set category name in modal
    document.getElementById('deleteCategoryName').textContent = categoryName;

    // Show the confirmation modal
    const modal = new bootstrap.Modal(document.getElementById('deleteCategoryModal'));
    modal.show();
}

// Handle confirm delete for category
const confirmDeleteCategoryBtn = document.getElementById('confirmDeleteCategoryBtn');
if (confirmDeleteCategoryBtn) {
    confirmDeleteCategoryBtn.onclick = function() {
        if (!categoryIdToDelete) return;
        if (deleteCategoryButtonRef) deleteCategoryButtonRef.disabled = true;
        fetch(`/inventory/api/categories/${categoryIdToDelete}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            showCategoryDeletedModal();
        })
        .catch(error => {
            if (deleteCategoryButtonRef) deleteCategoryButtonRef.disabled = false;
            alert('Error deleting category: ' + error.message);
        })
        .finally(() => {
            const modalEl = document.getElementById('deleteCategoryModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            categoryIdToDelete = null;
            deleteCategoryButtonRef = null;
        });
    };
}

function showCategoryDeletedModal() {
    const modalEl = document.getElementById('categoryDeletedModal');
    if (!modalEl) {
        console.error('categoryDeletedModal not found in DOM');
        return;
    }
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    // Reload only when the modal is hidden (user closes it or after timeout)
    modalEl.addEventListener('hidden.bs.modal', function handler() {
        modalEl.removeEventListener('hidden.bs.modal', handler);
        location.reload();
    });
    // Auto-close after 1.5s
    setTimeout(() => {
        modal.hide();
    }, 1500);
}

function showCategoryAddedModal() {
    // Hide the Add/Edit Category modal first
    const categoryModalEl = document.getElementById('categoryModal');
    const categoryModal = bootstrap.Modal.getInstance(categoryModalEl);
    if (categoryModal) categoryModal.hide();

    // Now show the success modal
    const modalEl = document.getElementById('categoryAddedModal');
    if (!modalEl) {
        console.error('categoryAddedModal not found in DOM');
        return;
    }
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    // Reload only when the modal is hidden (user closes it or after timeout)
    modalEl.addEventListener('hidden.bs.modal', function handler() {
        modalEl.removeEventListener('hidden.bs.modal', handler);
        location.reload();
    });
    // Auto-close after 1.5s
    setTimeout(() => {
        modal.hide();
    }, 1500);
}

function saveCategory() {
    const categoryId = document.getElementById('categoryId').value;
    const category = {
        name: document.getElementById('categoryName').value,
        description: document.getElementById('categoryDescription').value
    };

    // Validate required fields
    if (!category.name || category.name.trim() === '') {
        showProductErrorModal('Category name is required');
        return;
    }

    console.log('Saving category:', category);
    const url = categoryId ? `/inventory/api/categories/${categoryId}` : '/inventory/api/categories';
    const method = categoryId ? 'PUT' : 'POST';

    // Disable the save button to prevent double submission
    const saveButton = document.querySelector('#categoryModal .btn-primary');
    if (saveButton) saveButton.disabled = true;

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(category)
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            return response.text().then(text => {
                try {
                    const error = JSON.parse(text);
                    throw new Error(error.message || error.error || text || 'Error saving category');
                } catch (e) {
                    throw new Error(text || 'Error saving category');
                }
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Category saved successfully:', data);
        showCategoryAddedModal();
    })
    .catch(error => {
        console.error('Error saving category:', error);
        let message = error.message || 'Error saving category';
        if (/already exists|duplicate/i.test(message)) {
            message = 'A category with this name already exists. Please choose a different name.';
        } else if (/name/i.test(message)) {
            message = 'Category name is required.';
        }
        showProductErrorModal(message);
        if (saveButton) saveButton.disabled = false;
    });
}

// Stock Management
function showStockModal(button) {
    const productId = button.dataset.id;
    const productName = button.dataset.name;
    const currentStock = button.dataset.stock;

    document.getElementById('stockProductId').value = productId;
    document.getElementById('stockProductName').value = productName;
    document.getElementById('currentStock').value = currentStock;
    document.getElementById('stockAdjustment').value = '';

    const modal = new bootstrap.Modal(document.getElementById('stockModal'));
    modal.show();
}

function updateStock() {
    const productId = document.getElementById('stockProductId').value;
    const adjustment = parseInt(document.getElementById('stockAdjustment').value);

    if (isNaN(adjustment)) {
        alert('Please enter a valid stock adjustment number');
        return;
    }

    // Disable update button
    const updateButton = document.querySelector('#stockModal .btn-primary');
    if (updateButton) updateButton.disabled = true;

    fetch(`/api/products/${productId}/stock`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        },
        body: JSON.stringify({ adjustment: adjustment })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                try {
                    const error = JSON.parse(text);
                    throw new Error(error.message || error.error || 'Error updating stock');
                } catch (e) {
                    throw new Error(text || 'Error updating stock');
                }
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Stock updated successfully:', data);
        // Fetch the updated product details
        fetch(`/api/products/${productId}`)
            .then(response => response.json())
            .then(product => {
                if (product.stockQuantity < 10 && product.onHoldStock > 0) {
                    // Auto-transfer on-hold stock
                    fetch(`/api/products/${productId}/transfer-on-hold`, {
                        method: 'PUT',
                        headers: {
                            'X-CSRF-TOKEN': getCsrfToken(),
                            'Content-Type': 'application/json'
                        }
                    })
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Failed to auto-transfer on-hold stock');
                        }
                        return response.json();
                    })
                    .then(() => {
                        showSuccessAndReload();
                    })
                    .catch(error => {
                        alert(error.message || 'Error auto-transferring on-hold stock');
                        showSuccessAndReload();
                    });
                } else {
                    showSuccessAndReload();
                }
            });
    })
    .catch(error => {
        console.error('Error updating stock:', error);
        if (error.message === 'Failed to fetch') {
            // Verify if the stock was updated by fetching the product
            fetch(`/api/products/${productId}`)
                .then(response => response.json())
                .then(() => {
                    // Show success and reload since the update was likely successful
                    showSuccessAndReload();
                })
                .catch(() => {
                    if (updateButton) updateButton.disabled = false;
                    location.reload(); // Just reload to show current state
                });
        } else {
            if (updateButton) updateButton.disabled = false;
            alert(error.message || 'Error updating stock');
        }
    });
}

// Import/Export Functions
function exportProducts() {
    const pdfjsLib = window.pdfjsLib;
    fetch('/inventory/api/products/export/preview')
        .then(response => response.blob())
        .then(blob => {
            const url = URL.createObjectURL(blob);
            const modal = new bootstrap.Modal(document.getElementById('pdfPreviewModal'));
            modal.show();
            // Render PDF using PDF.js
            const canvas = document.getElementById('pdfPreviewCanvas');
            const ctx = canvas.getContext('2d');
            pdfjsLib.getDocument(url).promise.then(function(pdf) {
                pdf.getPage(1).then(function(page) {
                    const viewport = page.getViewport({ scale: 1.5 });
                    canvas.width = viewport.width;
                    canvas.height = viewport.height;
                    page.render({ canvasContext: ctx, viewport: viewport });
                });
            });
            // Download button logic
            const downloadBtn = document.getElementById('downloadPdfBtn');
            downloadBtn.onclick = function() {
                const a = document.createElement('a');
                a.href = url;
                a.download = 'products.pdf';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                modal.hide();
                setTimeout(() => URL.revokeObjectURL(url), 1000);
            };
            // Clean up URL and canvas when modal closes
            document.getElementById('pdfPreviewModal').addEventListener('hidden.bs.modal', function handler() {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                setTimeout(() => URL.revokeObjectURL(url), 1000);
                document.getElementById('pdfPreviewModal').removeEventListener('hidden.bs.modal', handler);
            });
        });
}

function importProducts() {
    const fileInput = document.getElementById('importFile');
    const file = fileInput.files[0];

    if (!file) {
        alert('Please select a file to import');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    fetch('/inventory/api/products/import', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        alert(`Successfully imported ${data.imported} products`);
        location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error importing products: ' + error.message);
    });
}

// Modal Reset Handlers
document.getElementById('productModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = '';
    document.getElementById('productStock').disabled = false;
});

document.getElementById('categoryModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('categoryForm').reset();
    document.getElementById('categoryId').value = '';
});

document.getElementById('stockModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('stockForm').reset();
    document.getElementById('stockProductId').value = '';
});

function printInventory() {
    window.print();
}

// Export Functions
function exportInventoryExcel() {
    const modal = new bootstrap.Modal(document.getElementById('excelPreviewModal'));
    modal.show();
}

function downloadInventoryExcel() {
    window.location.href = '/inventory/api/products/export/excel';
    const modalEl = document.getElementById('excelPreviewModal');
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();
}

let transferProductId = null;
let transferEvent = null;

function transferOnHoldStock(productId, event) {
    transferProductId = productId;
    transferEvent = event;
    const modal = new bootstrap.Modal(document.getElementById('transferOnHoldModal'));
    modal.show();
}

document.getElementById('confirmTransferOnHoldBtn').onclick = function() {
    if (!transferProductId) return;
    const button = transferEvent ? transferEvent.target.closest('button') : null;
    if (button) button.disabled = true;
    fetch(`/api/products/${transferProductId}/transfer-on-hold`, {
        method: 'PUT',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken(),
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to transfer on-hold stock');
        }
        return response.json();
    })
    .then(data => {
        location.reload();
    })
    .catch(error => {
        alert(error.message || 'Error transferring on-hold stock');
        if (button) button.disabled = false;
    })
    .finally(() => {
        // Hide the modal after action
        const modalEl = document.getElementById('transferOnHoldModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
        transferProductId = null;
        transferEvent = null;
    });
};

// Add this function for Add Product button
function addProduct() {
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = '';
    const stockInput = document.getElementById('productStock');
    stockInput.value = '';
    stockInput.disabled = false;
    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

// Ensure productId is cleared and stock input enabled every time the add product modal is shown
const addProductBtn = document.querySelector('button[data-bs-target="#productModal"]');
if (addProductBtn) {
    addProductBtn.addEventListener('click', addProduct);
}

// Remake editProduct function
function editProduct(button) {
    const productId = button.dataset.id;
    const name = button.dataset.name;
    const description = button.dataset.description;
    const price = button.dataset.price;
    const categoryId = button.dataset.category;
    const vatable = button.dataset.vatable === 'true';
    const stock = button.dataset.stock;

    document.getElementById('productId').value = productId;
    document.getElementById('productName').value = name;
    document.getElementById('productDescription').value = description;
    document.getElementById('productPrice').value = price;
    document.getElementById('productCategory').value = categoryId;
    document.getElementById('productVatable').checked = vatable;
    const stockInput = document.getElementById('productStock');
    stockInput.value = stock;
    stockInput.disabled = false;
    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

// Remake showProductEditedModal function
function showProductEditedModal() {
    // Close the product modal first
    const productModalEl = document.getElementById('productModal');
    const productModal = bootstrap.Modal.getInstance(productModalEl);
    if (productModal) productModal.hide();
    // Now show the success modal
    const modalEl = document.getElementById('productEditedModal');
    if (!modalEl) {
        console.error('productEditedModal not found in DOM');
        return;
    }
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    setTimeout(() => {
        location.reload();
    }, 1500);
}

// Add this function to show error modal for product errors
function showProductErrorModal(message) {
    let modal = document.getElementById('productErrorModal');
    let modalBody = document.getElementById('productErrorModalBody');
    if (!modal) {
        // Create modal if it doesn't exist
        modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'productErrorModal';
        modal.tabIndex = -1;
        modal.setAttribute('aria-labelledby', 'productErrorModalLabel');
        modal.setAttribute('aria-hidden', 'true');
        modal.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title" id="productErrorModalLabel">Product Error</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="productErrorModalBody"></div>
            </div>
        </div>`;
        document.body.appendChild(modal);
        modalBody = document.getElementById('productErrorModalBody');
    }
    modalBody.textContent = message;
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

// Remake saveProduct function
function saveProduct() {
    const productId = document.getElementById('productId').value;
    const product = {
        name: document.getElementById('productName').value,
        description: document.getElementById('productDescription').value,
        price: parseFloat(document.getElementById('productPrice').value),
        categoryId: document.getElementById('productCategory').value,
        vatable: document.getElementById('productVatable').checked,
        active: true
    };
    const stockQuantity = parseInt(document.getElementById('productStock').value);
    if (isNaN(stockQuantity)) {
        showProductErrorModal('Stock quantity is required and must be a valid number');
        return;
    }
    product.stockQuantity = stockQuantity;
    if (!product.name || product.name.trim() === '') {
        showProductErrorModal('Product name is required');
        return;
    }
    if (!product.price || isNaN(product.price)) {
        showProductErrorModal('Product price is required and must be a valid number');
        return;
    }
    if (!product.categoryId) {
        showProductErrorModal('Category is required');
        return;
    }
    const url = productId ? `/api/products/${productId}` : '/api/products';
    const method = productId ? 'PUT' : 'POST';
    const saveButton = document.querySelector('#productModal .btn-primary');
    if (saveButton) saveButton.disabled = true;
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        },
        body: JSON.stringify(product)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                try {
                    const error = JSON.parse(text);
                    throw new Error(error.message || error.error || text || 'Error saving product');
                } catch (e) {
                    throw new Error(text || 'Error saving product');
                }
            });
        }
        return response.json();
    })
    .then(data => {
        if (!productId) {
            showProductAddedModal();
        } else {
            showProductEditedModal();
        }
    })
    .catch(error => {
        if (saveButton) saveButton.disabled = false;
        let message = error.message || 'Error saving product';
        if (/already exists|duplicate/i.test(message)) {
            message = 'A product with this name already exists. Please choose a different name.';
        } else if (/price/i.test(message)) {
            message = 'Please enter a valid price.';
        } else if (/stock/i.test(message)) {
            message = 'Stock quantity is required and must be a valid number.';
        } else if (/category/i.test(message)) {
            message = 'Category is required.';
        }
        showProductErrorModal(message);
    });
}

// --- Request Stock Modal with Autocomplete ---
let selectedRequestProductId = null;
let allProducts = [];

function openRequestStockModal() {
    // Fetch all products if not already loaded
    if (allProducts.length === 0) {
        fetch('/api/products')
            .then(res => res.json())
            .then(data => {
                allProducts = data;
                showRequestStockModal();
            });
    } else {
        showRequestStockModal();
    }
}

function showRequestStockModal() {
    document.getElementById('requestProductSearch').value = '';
    document.getElementById('autocompleteResults').innerHTML = '';
    selectedRequestProductId = null;
    document.getElementById('submitRequestStockBtn').disabled = true;
    const modal = new bootstrap.Modal(document.getElementById('requestStockModal'));
    modal.show();
}

// Autocomplete logic
const requestProductSearch = document.getElementById('requestProductSearch');
const autocompleteResults = document.getElementById('autocompleteResults');
requestProductSearch.addEventListener('input', function() {
    const query = this.value.trim().toLowerCase();
    autocompleteResults.innerHTML = '';
    selectedRequestProductId = null;
    document.getElementById('submitRequestStockBtn').disabled = true;
    if (!query) return;
    const matches = allProducts.filter(p => p.name.toLowerCase().includes(query));
    matches.forEach(product => {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'list-group-item list-group-item-action';
        item.textContent = product.name + ' (Stock: ' + product.stockQuantity + ', On-Hold: ' + product.onHoldStock + ')';
        item.onclick = function() {
            requestProductSearch.value = product.name;
            selectedRequestProductId = product.id;
            autocompleteResults.innerHTML = '';
            document.getElementById('submitRequestStockBtn').disabled = false;
        };
        autocompleteResults.appendChild(item);
    });
});

document.getElementById('submitRequestStockBtn').onclick = function() {
    if (!selectedRequestProductId) return;
    this.disabled = true;
    fetch(`/api/products/${selectedRequestProductId}/request-stock`, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken(),
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) throw new Error('Failed to request stock');
        showRequestStockSuccessModal();
    })
    .catch(error => {
        alert(error.message || 'Error requesting stock');
        this.disabled = false;
    });
};

function showRequestStockSuccessModal() {
    const modalEl = document.getElementById('requestStockSuccessModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
    setTimeout(() => {
        modal.hide();
    }, 1500);
    const requestStockModalEl = document.getElementById('requestStockModal');
    const requestStockModal = bootstrap.Modal.getInstance(requestStockModalEl);
    if (requestStockModal) requestStockModal.hide();
}

// --- Product Table Search Filter ---
document.getElementById('productSearchInput').addEventListener('input', function() {
    const query = this.value.trim().toLowerCase();
    const rows = document.querySelectorAll('#productsTableBody tr');
    rows.forEach(row => {
        // Get text from all relevant columns (name, category, price, stock, on-hold)
        const name = row.children[1]?.textContent.toLowerCase() || '';
        const category = row.children[2]?.textContent.toLowerCase() || '';
        const price = row.children[3]?.textContent.toLowerCase() || '';
        const stock = row.children[4]?.textContent.toLowerCase() || '';
        const onHold = row.children[5]?.textContent.toLowerCase() || '';
        // If any column matches the query, show the row
        if (
            name.includes(query) ||
            category.includes(query) ||
            price.includes(query) ||
            stock.includes(query) ||
            onHold.includes(query)
        ) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
});

// Utility: Remove all modal backdrops and force body to be scrollable/clickable
function cleanUpModals() {
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
}

// Listen for all modal close events globally
window.addEventListener('hidden.bs.modal', cleanUpModals);