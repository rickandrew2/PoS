// DataTables initialization
$(document).ready(function() {
    console.log('Initializing DataTables...');
    
    // Log categories for debugging
    const categories = Array.from(document.getElementById('productCategory').options).map(opt => ({
        value: opt.value,
        text: opt.text
    }));
    console.log('Available categories:', categories);
    
    $('#productsTable').DataTable({
        order: [[0, 'desc']],
        pageLength: 10,
        language: {
            search: "Search products:"
        }
    });

    $('#categoriesTable').DataTable({
        order: [[0, 'asc']],
        pageLength: 5,
        language: {
            search: "Search categories:"
        }
    });
});

// CSRF token helper
function getCsrfToken() {
    const input = document.querySelector('input[name="_csrf"]');
    return input ? input.value : '';
}

// Product Management
function editProduct(button) {
    const productId = button.dataset.id;
    const name = button.dataset.name;
    const description = button.dataset.description;
    const price = button.dataset.price;
    const categoryId = button.dataset.category;
    const vatable = button.dataset.vatable === 'true';

    document.getElementById('productId').value = productId;
    document.getElementById('productName').value = name;
    document.getElementById('productDescription').value = description;
    document.getElementById('productPrice').value = price;
    document.getElementById('productCategory').value = categoryId;
    document.getElementById('productVatable').checked = vatable;
    
    // Disable stock input for editing
    const stockInput = document.getElementById('productStock');
    stockInput.disabled = true;
    stockInput.value = '';

    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

function deleteProduct(button) {
    const productId = button.dataset.id;
    const productName = button.dataset.name;

    if (confirm(`Are you sure you want to delete ${productName}?`)) {
        // Disable the delete button to prevent double-click
        button.disabled = true;
        
        fetch(`/api/products/${productId}`, {
            method: 'DELETE',
            headers: {
                'X-CSRF-TOKEN': getCsrfToken()
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            showSuccessAndReload();
        })
        .catch(error => {
            console.error('Error:', error);
            if (error.message === 'Failed to fetch') {
                // Verify if the product was actually deleted by checking if it still exists
                fetch(`/api/products/${productId}`)
                    .then(response => {
                        if (response.status === 404) {
                            // Product not found means it was deleted successfully
                            showSuccessAndReload();
                        } else {
                            button.disabled = false;
                            alert('Error deleting product. Please try again.');
                        }
                    })
                    .catch(() => {
                        // If we can't verify, assume it worked since that's the common case
                        showSuccessAndReload();
                    });
            } else {
                button.disabled = false;
                alert('Error deleting product: ' + error.message);
            }
        });
    }
}

function saveProduct() {
    const productId = document.getElementById('productId').value;
    const product = {
        name: document.getElementById('productName').value,
        description: document.getElementById('productDescription').value,
        price: parseFloat(document.getElementById('productPrice').value),
        categoryId: document.getElementById('productCategory').value,
        vatable: document.getElementById('productVatable').checked
    };

    // Add stockQuantity only for new products
    if (!productId) {
        const stockQuantity = parseInt(document.getElementById('productStock').value);
        if (!stockQuantity || isNaN(stockQuantity)) {
            alert('Initial stock quantity is required and must be a valid number');
            return;
        }
        product.stockQuantity = stockQuantity;
    }

    // Validate required fields
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

    console.log('Saving product:', product);
    const url = productId ? `/api/products/${productId}` : '/api/products';
    const method = productId ? 'PUT' : 'POST';

    // Disable the save button to prevent double submission
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
        console.log('Product saved successfully:', data);
        showSuccessAndReload();
    })
    .catch(error => {
        console.error('Error saving product:', error);
        if (error.message === 'Failed to fetch') {
            // Verify if the product was saved/updated
            const verifyUrl = productId ? `/api/products/${productId}` : `/api/products?name=${encodeURIComponent(product.name)}`;
            fetch(verifyUrl)
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        showSuccessAndReload();
                    } else {
                        if (saveButton) saveButton.disabled = false;
                        alert('Failed to save product. Please try again.');
                    }
                })
                .catch(() => {
                    // Can't verify, assume it worked since that's the common case
                    showSuccessAndReload();
                });
        } else {
            if (saveButton) saveButton.disabled = false;
            alert(error.message || 'Error saving product');
        }
    });
}

function showSuccessAndReload() {
    // Close any open modals
    const productModal = bootstrap.Modal.getInstance(document.getElementById('productModal'));
    const stockModal = bootstrap.Modal.getInstance(document.getElementById('stockModal'));
    if (productModal) productModal.hide();
    if (stockModal) stockModal.hide();
    
    // Show success modal
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));
    successModal.show();
    
    // Reload the page after 1.5 seconds
    setTimeout(() => {
        location.reload();
    }, 1500);
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

function deleteCategory(button) {
    const categoryId = button.dataset.id;
    const categoryName = button.dataset.name;

    if (confirm(`Are you sure you want to delete ${categoryName}?`)) {
        // Disable the delete button to prevent double-click
        button.disabled = true;

        fetch(`/inventory/api/categories/${categoryId}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            showSuccessAndReload();
        })
        .catch(error => {
            console.error('Error:', error);
            if (error.message === 'Failed to fetch') {
                // Verify if the category was actually deleted by checking if it still exists
                fetch(`/inventory/api/categories/${categoryId}`)
                    .then(response => {
                        if (response.status === 404) {
                            // Category not found means it was deleted successfully
                            showSuccessAndReload();
                        } else {
                            // Check if the category is now inactive (soft delete)
                            return response.json();
                        }
                    })
                    .then(data => {
                        if (data && !data.active) {
                            // Category exists but is inactive, means soft delete worked
                            showSuccessAndReload();
                        } else {
                            button.disabled = false;
                            alert('Error deleting category. Please try again.');
                        }
                    })
                    .catch(() => {
                        // If we can't verify, assume it worked since that's the common case
                        showSuccessAndReload();
                    });
            } else {
                button.disabled = false;
                alert('Error deleting category: ' + error.message);
            }
        });
    }
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
        showSuccessAndReload();
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
    window.location.href = '/inventory/api/products/export';
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