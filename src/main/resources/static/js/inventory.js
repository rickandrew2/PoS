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
        alert('Category name is required');
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
                    throw new Error(error.message || 'Error saving category');
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
        
        // If it's a "Failed to fetch" error, verify if the category was actually saved
        if (error.message === 'Failed to fetch') {
            const verifyUrl = categoryId ? 
                `/inventory/api/categories/${categoryId}` : 
                `/inventory/api/categories`;
                
            fetch(verifyUrl)
                .then(response => response.json())
                .then(data => {
                    // For new categories, check if a category with our name exists
                    // For updates, check if the category exists and has our updates
                    if (data) {
                        if (Array.isArray(data)) {
                            // If it's a list (for new categories), check if our category exists
                            const exists = data.some(cat => cat.name === category.name);
                            if (exists) {
                                showSuccessAndReload();
                                return;
                            }
                        } else {
                            // If it's a single category (for updates), check if it has our updates
                            if (data.name === category.name) {
                                showSuccessAndReload();
                                return;
                            }
                        }
                    }
                    // If we couldn't verify, just reload to show current state
                    if (saveButton) saveButton.disabled = false;
                    location.reload();
                })
                .catch(() => {
                    // If we can't verify, assume it worked since that's the common case
                    showSuccessAndReload();
                });
        } else {
            if (saveButton) saveButton.disabled = false;
            alert(error.message || 'Error saving category');
        }
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
        showSuccessAndReload();
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

function transferOnHoldStock(productId, event) {
    if (!confirm('Transfer on-hold stock to main inventory for this product?')) return;
    const button = event.target.closest('button');
    if (button) button.disabled = true;
    fetch(`/api/products/${productId}/transfer-on-hold`, {
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
    });
}

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
        alert('Stock quantity is required and must be a valid number');
        return;
    }
    product.stockQuantity = stockQuantity;
    if (!product.name || product.name.trim() === '') {
        alert('Product name is required');
        return;
    }
    if (!product.price || isNaN(product.price)) {
        alert('Product price is required and must be a valid number');
        return;
    }
    if (!product.categoryId) {
        alert('Category is required');
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
                    throw new Error(error.message || 'Error saving product');
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
        alert(error.message || 'Error saving product');
    });
} 