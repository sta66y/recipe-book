// API Base URL
const API_BASE = '/api';

// State
let currentProductId = null;
let currentDishId = null;
let dishPhotos = [];
let productPhotos = [];
let dishIngredients = [];

// ==================== Initialization ====================

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initProductFilters();
    initDishFilters();
    initProductForm();
    initDishForm();
    initMacrosValidation();
    loadProducts();
});

// ==================== Tabs ====================

function initTabs() {
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            
            tab.classList.add('active');
            const tabId = tab.dataset.tab + '-tab';
            document.getElementById(tabId).classList.add('active');
            
            if (tab.dataset.tab === 'products') {
                loadProducts();
            } else {
                loadDishes();
            }
        });
    });
}

// ==================== Products ====================

async function loadProducts() {
    const list = document.getElementById('products-list');
    list.innerHTML = '<div class="empty-state">Загрузка...</div>';
    
    try {
        const params = new URLSearchParams();
        
        const name = document.getElementById('product-search').value;
        if (name) params.append('name', name);
        
        const category = document.getElementById('product-category-filter').value;
        if (category) params.append('category', category);
        
        const cookingReq = document.getElementById('product-cooking-filter').value;
        if (cookingReq) params.append('cookingRequirement', cookingReq);
        
        if (document.getElementById('product-vegan-filter').checked) {
            params.append('vegan', 'true');
        }
        if (document.getElementById('product-gluten-free-filter').checked) {
            params.append('glutenFree', 'true');
        }
        if (document.getElementById('product-sugar-free-filter').checked) {
            params.append('sugarFree', 'true');
        }
        
        const sortBy = document.getElementById('product-sort').value;
        if (sortBy) params.append('sortBy', sortBy);
        
        const response = await fetch(`${API_BASE}/products?${params.toString()}`);
        const products = await response.json();
        
        if (products.length === 0) {
            list.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🍎</div>
                    <div class="empty-state-text">Нет продуктов</div>
                    <button class="btn-primary" onclick="openProductModal()">Добавить первый продукт</button>
                </div>
            `;
            return;
        }
        
        list.innerHTML = products.map(product => createProductCard(product)).join('');
    } catch (error) {
        list.innerHTML = `<div class="empty-state">Ошибка загрузки: ${error.message}</div>`;
        showToast('Ошибка загрузки продуктов', 'error');
    }
}

function createProductCard(product) {
    const flags = product.flags.map(f => {
        const className = f.toLowerCase().replace(' ', '-');
        return `<span class="flag-badge ${className}">${f}</span>`;
    }).join('');
    
    return `
        <div class="card" onclick="showProductDetail(${product.id})">
            <div class="card-header">
                <h3 class="card-title">${escapeHtml(product.name)}</h3>
                <span class="card-category">${product.category}</span>
            </div>
            ${product.photos && product.photos.length > 0 ? `
                <div class="card-photos">
                    <img src="${escapeHtml(product.photos[0])}" alt="${escapeHtml(product.name)}" class="card-photo">
                </div>
            ` : ''}
            <div class="card-nutrition">
                <div class="nutrition-item">
                    <div class="nutrition-value">${product.calories.toFixed(1)}</div>
                    <div class="nutrition-label">ккал</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${product.proteins.toFixed(1)}</div>
                    <div class="nutrition-label">белки</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${product.fats.toFixed(1)}</div>
                    <div class="nutrition-label">жиры</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${product.carbohydrates.toFixed(1)}</div>
                    <div class="nutrition-label">углеводы</div>
                </div>
            </div>
            ${flags ? `<div class="card-flags">${flags}</div>` : ''}
        </div>
    `;
}

function initProductFilters() {
    document.getElementById('product-search').addEventListener('input', debounce(loadProducts, 300));
    document.getElementById('product-category-filter').addEventListener('change', loadProducts);
    document.getElementById('product-cooking-filter').addEventListener('change', loadProducts);
    document.getElementById('product-vegan-filter').addEventListener('change', loadProducts);
    document.getElementById('product-gluten-free-filter').addEventListener('change', loadProducts);
    document.getElementById('product-sugar-free-filter').addEventListener('change', loadProducts);
    document.getElementById('product-sort').addEventListener('change', loadProducts);
}

// ==================== Product Modal ====================

function openProductModal(productId = null) {
    currentProductId = productId;
    productPhotos = [];
    
    // Сбрасываем форму только при создании нового продукта
    if (!productId) {
        document.getElementById('product-form').reset();
        document.getElementById('product-id').value = '';
    }
    
    document.getElementById('product-photos-container').innerHTML = '';
    document.getElementById('product-macros-sum').className = 'macros-sum';
    document.getElementById('product-macros-sum').textContent = '';
    
    if (productId) {
        document.getElementById('product-modal-title').textContent = 'Редактирование продукта';
        loadProductForEdit(productId);
    } else {
        document.getElementById('product-modal-title').textContent = 'Новый продукт';
    }
    
    document.getElementById('product-modal').classList.add('active');
}

function closeProductModal() {
    document.getElementById('product-modal').classList.remove('active');
    currentProductId = null;
}

async function loadProductForEdit(id) {
    try {
        const response = await fetch(`${API_BASE}/products/${id}`);
        if (!response.ok) {
            throw new Error('Продукт не найден');
        }
        const product = await response.json();
        
        // Заполняем форму
        document.getElementById('product-id').value = product.id;
        document.getElementById('product-name').value = product.name;
        document.getElementById('product-calories').value = product.calories;
        document.getElementById('product-proteins').value = product.proteins;
        document.getElementById('product-fats').value = product.fats;
        document.getElementById('product-carbs').value = product.carbohydrates;
        document.getElementById('product-composition').value = product.composition || '';
        document.getElementById('product-category').value = product.category;
        document.getElementById('product-cooking').value = product.cookingRequirement;
        
        productPhotos = [...product.photos];
        renderProductPhotos();
        
        document.getElementById('product-vegan').checked = product.flags.includes('Веган');
        document.getElementById('product-gluten-free').checked = product.flags.includes('Без глютена');
        document.getElementById('product-sugar-free').checked = product.flags.includes('Без сахара');
        
        updateMacrosSum();
    } catch (error) {
        showToast('Ошибка загрузки продукта: ' + error.message, 'error');
        closeProductModal();
    }
}

function initProductForm() {
    document.getElementById('product-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveProduct();
    });
}

async function saveProduct() {
    const data = {
        name: document.getElementById('product-name').value,
        photos: productPhotos,
        calories: parseFloat(document.getElementById('product-calories').value),
        proteins: parseFloat(document.getElementById('product-proteins').value),
        fats: parseFloat(document.getElementById('product-fats').value),
        carbohydrates: parseFloat(document.getElementById('product-carbs').value),
        composition: document.getElementById('product-composition').value || null,
        category: document.getElementById('product-category').value,
        cookingRequirement: document.getElementById('product-cooking').value,
        flags: []
    };
    
    if (document.getElementById('product-vegan').checked) data.flags.push('Веган');
    if (document.getElementById('product-gluten-free').checked) data.flags.push('Без глютена');
    if (document.getElementById('product-sugar-free').checked) data.flags.push('Без сахара');
    
    try {
        const url = currentProductId 
            ? `${API_BASE}/products/${currentProductId}`
            : `${API_BASE}/products`;
        const method = currentProductId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Ошибка сохранения');
        }
        
        showToast(currentProductId ? 'Продукт обновлён' : 'Продукт создан', 'success');
        closeProductModal();
        loadProducts();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ==================== Product Photos ====================

function addProductPhoto() {
    const input = document.getElementById('product-photo-input');
    const url = input.value.trim();
    
    if (!url) return;
    
    if (productPhotos.length >= 5) {
        showToast('Максимум 5 фотографий', 'error');
        return;
    }
    
    productPhotos.push(url);
    renderProductPhotos();
    input.value = '';
}

function renderProductPhotos() {
    const container = document.getElementById('product-photos-container');
    container.innerHTML = productPhotos.map((url, index) => `
        <div class="photo-preview">
            <img src="${escapeHtml(url)}" alt="Photo">
            <span>${escapeHtml(url.substring(0, 30))}...</span>
            <button type="button" class="remove-photo" onclick="removeProductPhoto(${index})">✕</button>
        </div>
    `).join('');
}

function removeProductPhoto(index) {
    productPhotos.splice(index, 1);
    renderProductPhotos();
}

// ==================== Product Detail ====================

async function showProductDetail(id) {
    currentProductId = id;
    
    try {
        const response = await fetch(`${API_BASE}/products/${id}`);
        const product = await response.json();
        
        document.getElementById('product-detail-title').textContent = product.name;
        
        const flags = product.flags.map(f => {
            const className = f.toLowerCase().replace(' ', '-');
            return `<span class="flag-badge ${className}">${f}</span>`;
        }).join('');
        
        document.getElementById('product-detail-content').innerHTML = `
            ${product.photos && product.photos.length > 0 ? `
                <div class="detail-section">
                    <h3>Фотографии</h3>
                    <div class="detail-photos">
                        ${product.photos.map(url => `<img src="${escapeHtml(url)}" class="detail-photo">`).join('')}
                    </div>
                </div>
            ` : ''}
            
            <div class="detail-section">
                <h3>Основная информация</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Категория</span>
                        <span class="detail-value">${product.category}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Готовность</span>
                        <span class="detail-value">${product.cookingRequirement}</span>
                    </div>
                    ${product.composition ? `
                        <div class="detail-row">
                            <span class="detail-label">Состав</span>
                            <span class="detail-value">${escapeHtml(product.composition)}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
            
            <div class="detail-section">
                <h3>КБЖУ на 100г</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Калорийность</span>
                        <span class="detail-value">${product.calories.toFixed(1)} ккал</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Белки</span>
                        <span class="detail-value">${product.proteins.toFixed(1)} г</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Жиры</span>
                        <span class="detail-value">${product.fats.toFixed(1)} г</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Углеводы</span>
                        <span class="detail-value">${product.carbohydrates.toFixed(1)} г</span>
                    </div>
                </div>
            </div>
            
            ${flags ? `
                <div class="detail-section">
                    <h3>Флаги</h3>
                    <div class="card-flags">${flags}</div>
                </div>
            ` : ''}
            
            <div class="detail-section">
                <h3>Даты</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Создан</span>
                        <span class="detail-value">${formatDate(product.createdAt)}</span>
                    </div>
                    ${product.updatedAt ? `
                        <div class="detail-row">
                            <span class="detail-label">Обновлён</span>
                            <span class="detail-value">${formatDate(product.updatedAt)}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
        
        document.getElementById('product-detail-modal').classList.add('active');
    } catch (error) {
        showToast('Ошибка загрузки продукта', 'error');
    }
}

function closeProductDetailModal() {
    document.getElementById('product-detail-modal').classList.remove('active');
}

function editCurrentProduct() {
    const idToEdit = currentProductId;
    closeProductDetailModal();
    currentProductId = null;
    openProductModal(idToEdit);
}

async function deleteCurrentProduct() {
    if (!currentProductId) return;
    
    if (!confirm('Вы уверены, что хотите удалить этот продукт?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/products/${currentProductId}`, {
            method: 'DELETE'
        });
        
        if (response.status === 409) {
            const error = await response.json();
            showToast(error.message, 'error');
            return;
        }
        
        if (!response.ok) {
            throw new Error('Ошибка удаления');
        }
        
        showToast('Продукт удалён', 'success');
        closeProductDetailModal();
        currentProductId = null;
        loadProducts();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ==================== Macros Sum Validation ====================

function initMacrosValidation() {
    ['product-proteins', 'product-fats', 'product-carbs'].forEach(id => {
        document.getElementById(id).addEventListener('input', updateMacrosSum);
    });
}

function updateMacrosSum() {
    const proteins = parseFloat(document.getElementById('product-proteins').value) || 0;
    const fats = parseFloat(document.getElementById('product-fats').value) || 0;
    const carbs = parseFloat(document.getElementById('product-carbs').value) || 0;
    const sum = proteins + fats + carbs;
    
    const element = document.getElementById('product-macros-sum');
    
    if (sum > 100) {
        element.className = 'macros-sum invalid';
        element.textContent = `Сумма БЖУ: ${sum.toFixed(1)}г (превышает 100г!)`;
    } else {
        element.className = 'macros-sum valid';
        element.textContent = `Сумма БЖУ: ${sum.toFixed(1)}г`;
    }
}

// ==================== Dishes ====================

async function loadDishes() {
    const list = document.getElementById('dishes-list');
    list.innerHTML = '<div class="empty-state">Загрузка...</div>';
    
    try {
        const params = new URLSearchParams();
        
        const name = document.getElementById('dish-search').value;
        if (name) params.append('name', name);
        
        const category = document.getElementById('dish-category-filter').value;
        if (category) params.append('category', category);
        
        if (document.getElementById('dish-vegan-filter').checked) {
            params.append('vegan', 'true');
        }
        if (document.getElementById('dish-gluten-free-filter').checked) {
            params.append('glutenFree', 'true');
        }
        if (document.getElementById('dish-sugar-free-filter').checked) {
            params.append('sugarFree', 'true');
        }
        
        const response = await fetch(`${API_BASE}/dishes?${params.toString()}`);
        const dishes = await response.json();
        
        if (dishes.length === 0) {
            list.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🍲</div>
                    <div class="empty-state-text">Нет блюд</div>
                    <button class="btn-primary" onclick="openDishModal()">Добавить первое блюдо</button>
                </div>
            `;
            return;
        }
        
        list.innerHTML = dishes.map(dish => createDishCard(dish)).join('');
    } catch (error) {
        list.innerHTML = `<div class="empty-state">Ошибка загрузки: ${error.message}</div>`;
        showToast('Ошибка загрузки блюд', 'error');
    }
}

function createDishCard(dish) {
    const flags = dish.flags.map(f => {
        const className = f.toLowerCase().replace(' ', '-');
        return `<span class="flag-badge ${className}">${f}</span>`;
    }).join('');
    
    return `
        <div class="card" onclick="showDishDetail(${dish.id})">
            <div class="card-header">
                <h3 class="card-title">${escapeHtml(dish.name)}</h3>
                <span class="card-category">${dish.category}</span>
            </div>
            ${dish.photos && dish.photos.length > 0 ? `
                <div class="card-photos">
                    <img src="${escapeHtml(dish.photos[0])}" alt="${escapeHtml(dish.name)}" class="card-photo">
                </div>
            ` : ''}
            <div class="card-nutrition">
                <div class="nutrition-item">
                    <div class="nutrition-value">${dish.calories.toFixed(1)}</div>
                    <div class="nutrition-label">ккал</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${dish.proteins.toFixed(1)}</div>
                    <div class="nutrition-label">белки</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${dish.fats.toFixed(1)}</div>
                    <div class="nutrition-label">жиры</div>
                </div>
                <div class="nutrition-item">
                    <div class="nutrition-value">${dish.carbohydrates.toFixed(1)}</div>
                    <div class="nutrition-label">углеводы</div>
                </div>
            </div>
            <div class="detail-row" style="border:none; padding-top: 8px;">
                <span class="detail-label">Порция: ${dish.servingSize}г</span>
                <span class="detail-label">${dish.ingredients.length} прод.</span>
            </div>
            ${flags ? `<div class="card-flags">${flags}</div>` : ''}
        </div>
    `;
}

function initDishFilters() {
    document.getElementById('dish-search').addEventListener('input', debounce(loadDishes, 300));
    document.getElementById('dish-category-filter').addEventListener('change', loadDishes);
    document.getElementById('dish-vegan-filter').addEventListener('change', loadDishes);
    document.getElementById('dish-gluten-free-filter').addEventListener('change', loadDishes);
    document.getElementById('dish-sugar-free-filter').addEventListener('change', loadDishes);
}

// ==================== Dish Modal ====================

function openDishModal(dishId = null) {
    currentDishId = dishId;
    dishPhotos = [];
    dishIngredients = [];
    
    // Сбрасываем форму только при создании нового блюда
    if (!dishId) {
        document.getElementById('dish-form').reset();
        document.getElementById('dish-id').value = '';
    }
    
    document.getElementById('dish-photos-container').innerHTML = '';
    document.getElementById('dish-ingredients-container').innerHTML = '';
    document.getElementById('dish-flags-container').innerHTML = `
        <label class="checkbox-label disabled">
            <input type="checkbox" id="dish-vegan" disabled>
            <span>🌱 Веган</span>
        </label>
        <label class="checkbox-label disabled">
            <input type="checkbox" id="dish-gluten-free" disabled>
            <span>🌾 Без глютена</span>
        </label>
        <label class="checkbox-label disabled">
            <input type="checkbox" id="dish-sugar-free" disabled>
            <span>🍬 Без сахара</span>
        </label>
    `;
    
    if (dishId) {
        document.getElementById('dish-modal-title').textContent = 'Редактирование блюда';
        loadDishForEdit(dishId);
    } else {
        document.getElementById('dish-modal-title').textContent = 'Новое блюдо';
    }
    
    document.getElementById('dish-modal').classList.add('active');
}

function closeDishModal() {
    document.getElementById('dish-modal').classList.remove('active');
    currentDishId = null;
}

async function loadDishForEdit(id) {
    try {
        const response = await fetch(`${API_BASE}/dishes/${id}`);
        if (!response.ok) {
            throw new Error('Блюдо не найдено');
        }
        const dish = await response.json();
        
        // Заполняем форму
        document.getElementById('dish-id').value = dish.id;
        document.getElementById('dish-name').value = dish.name;
        document.getElementById('dish-serving-size').value = dish.servingSize;
        document.getElementById('dish-category').value = dish.category;
        document.getElementById('dish-calories').value = dish.calories;
        document.getElementById('dish-proteins').value = dish.proteins;
        document.getElementById('dish-fats').value = dish.fats;
        document.getElementById('dish-carbs').value = dish.carbohydrates;
        
        dishPhotos = [...dish.photos];
        renderDishPhotos();
        
        dishIngredients = dish.ingredients.map(ing => ({
            productId: ing.productId,
            productName: ing.productName,
            quantity: ing.quantity
        }));
        renderDishIngredients();
        
        await checkAvailableFlags();
        
        document.getElementById('dish-vegan').checked = dish.flags.includes('Веган');
        document.getElementById('dish-gluten-free').checked = dish.flags.includes('Без глютена');
        document.getElementById('dish-sugar-free').checked = dish.flags.includes('Без сахара');
    } catch (error) {
        showToast('Ошибка загрузки блюда: ' + error.message, 'error');
        closeDishModal();
    }
}

function initDishForm() {
    document.getElementById('dish-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveDish();
    });
}

async function saveDish() {
    const data = {
        name: document.getElementById('dish-name').value,
        photos: dishPhotos,
        servingSize: parseFloat(document.getElementById('dish-serving-size').value),
        category: document.getElementById('dish-category').value || null,
        calories: parseFloat(document.getElementById('dish-calories').value) || null,
        proteins: parseFloat(document.getElementById('dish-proteins').value) || null,
        fats: parseFloat(document.getElementById('dish-fats').value) || null,
        carbohydrates: parseFloat(document.getElementById('dish-carbs').value) || null,
        ingredients: dishIngredients.map(ing => ({
            productId: ing.productId,
            quantity: ing.quantity
        })),
        flags: []
    };
    
    // Добавляем флаги только если они checked И не disabled
    const veganCheckbox = document.getElementById('dish-vegan');
    const glutenFreeCheckbox = document.getElementById('dish-gluten-free');
    const sugarFreeCheckbox = document.getElementById('dish-sugar-free');
    
    if (veganCheckbox?.checked && !veganCheckbox?.disabled) {
        data.flags.push('Веган');
    }
    if (glutenFreeCheckbox?.checked && !glutenFreeCheckbox?.disabled) {
        data.flags.push('Без глютена');
    }
    if (sugarFreeCheckbox?.checked && !sugarFreeCheckbox?.disabled) {
        data.flags.push('Без сахара');
    }
    
    try {
        const url = currentDishId 
            ? `${API_BASE}/dishes/${currentDishId}`
            : `${API_BASE}/dishes`;
        const method = currentDishId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Ошибка сохранения');
        }
        
        showToast(currentDishId ? 'Блюдо обновлено' : 'Блюдо создано', 'success');
        closeDishModal();
        loadDishes();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ==================== Dish Photos ====================

function addDishPhoto() {
    const input = document.getElementById('dish-photo-input');
    const url = input.value.trim();
    
    if (!url) return;
    
    if (dishPhotos.length >= 5) {
        showToast('Максимум 5 фотографий', 'error');
        return;
    }
    
    dishPhotos.push(url);
    renderDishPhotos();
    input.value = '';
}

function renderDishPhotos() {
    const container = document.getElementById('dish-photos-container');
    container.innerHTML = dishPhotos.map((url, index) => `
        <div class="photo-preview">
            <img src="${escapeHtml(url)}" alt="Photo">
            <span>${escapeHtml(url.substring(0, 30))}...</span>
            <button type="button" class="remove-photo" onclick="removeDishPhoto(${index})">✕</button>
        </div>
    `).join('');
}

function removeDishPhoto(index) {
    dishPhotos.splice(index, 1);
    renderDishPhotos();
}

// ==================== Ingredients ====================

async function openIngredientModal() {
    try {
        const response = await fetch(`${API_BASE}/products`);
        const products = await response.json();
        
        const select = document.getElementById('ingredient-product-select');
        select.innerHTML = products.map(p => 
            `<option value="${p.id}">${escapeHtml(p.name)} (${p.calories} ккал/100г)</option>`
        ).join('');
        
        document.getElementById('ingredient-quantity').value = '';
        document.getElementById('ingredient-modal').classList.add('active');
    } catch (error) {
        showToast('Ошибка загрузки продуктов', 'error');
    }
}

function closeIngredientModal() {
    document.getElementById('ingredient-modal').classList.remove('active');
}

async function addIngredientToDish() {
    const productId = parseInt(document.getElementById('ingredient-product-select').value);
    const productName = document.getElementById('ingredient-product-select').options[
        document.getElementById('ingredient-product-select').selectedIndex
    ].text.split(' (')[0];
    const quantity = parseFloat(document.getElementById('ingredient-quantity').value);
    
    if (!quantity || quantity <= 0) {
        showToast('Введите корректное количество', 'error');
        return;
    }
    
    // Check if product already exists
    const existing = dishIngredients.find(i => i.productId === productId);
    if (existing) {
        showToast('Этот продукт уже добавлен', 'error');
        return;
    }
    
    dishIngredients.push({ productId, productName, quantity });
    renderDishIngredients();
    await checkAvailableFlags();
    closeIngredientModal();
}

function renderDishIngredients() {
    const container = document.getElementById('dish-ingredients-container');
    
    if (dishIngredients.length === 0) {
        container.innerHTML = '<div class="empty-state" style="padding: 20px;">Нет ингредиентов</div>';
        return;
    }
    
    container.innerHTML = dishIngredients.map((ing, index) => `
        <div class="ingredient-item">
            <div class="ingredient-info">
                <div class="ingredient-name">${escapeHtml(ing.productName)}</div>
                <div class="ingredient-quantity">${ing.quantity} г</div>
            </div>
            <button type="button" class="ingredient-remove" onclick="removeIngredient(${index})">✕</button>
        </div>
    `).join('');
}

async function removeIngredient(index) {
    dishIngredients.splice(index, 1);
    renderDishIngredients();
    await checkAvailableFlags();
}

async function checkAvailableFlags() {
    if (dishIngredients.length === 0) {
        document.getElementById('dish-flags-container').innerHTML = `
            <label class="checkbox-label disabled">
                <input type="checkbox" id="dish-vegan" disabled>
                <span>🌱 Веган</span>
            </label>
            <label class="checkbox-label disabled">
                <input type="checkbox" id="dish-gluten-free" disabled>
                <span>🌾 Без глютена</span>
            </label>
            <label class="checkbox-label disabled">
                <input type="checkbox" id="dish-sugar-free" disabled>
                <span>🍬 Без сахара</span>
            </label>
        `;
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/dishes/available-flags`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dishIngredients.map(i => ({ productId: i.productId, quantity: i.quantity })))
        });
        
        const availableFlags = await response.json();
        
        // Сохраняем текущее состояние чекбоксов
        const currentVegan = document.getElementById('dish-vegan')?.checked || false;
        const currentGlutenFree = document.getElementById('dish-gluten-free')?.checked || false;
        const currentSugarFree = document.getElementById('dish-sugar-free')?.checked || false;
        
        document.getElementById('dish-flags-container').innerHTML = `
            <label class="checkbox-label ${availableFlags.includes('Веган') ? '' : 'disabled'}">
                <input type="checkbox" id="dish-vegan" ${availableFlags.includes('Веган') ? '' : 'disabled'}>
                <span>🌱 Веган</span>
            </label>
            <label class="checkbox-label ${availableFlags.includes('Без глютена') ? '' : 'disabled'}">
                <input type="checkbox" id="dish-gluten-free" ${availableFlags.includes('Без глютена') ? '' : 'disabled'}>
                <span>🌾 Без глютена</span>
            </label>
            <label class="checkbox-label ${availableFlags.includes('Без сахара') ? '' : 'disabled'}">
                <input type="checkbox" id="dish-sugar-free" ${availableFlags.includes('Без сахара') ? '' : 'disabled'}>
                <span>🍬 Без сахара</span>
            </label>
        `;
        
        // Восстанавливаем состояние только если флаг доступен
        if (availableFlags.includes('Веган')) {
            document.getElementById('dish-vegan').checked = currentVegan;
        }
        if (availableFlags.includes('Без глютена')) {
            document.getElementById('dish-gluten-free').checked = currentGlutenFree;
        }
        if (availableFlags.includes('Без сахара')) {
            document.getElementById('dish-sugar-free').checked = currentSugarFree;
        }
    } catch (error) {
        console.error('Error checking flags:', error);
    }
}

// ==================== Dish Detail ====================

async function showDishDetail(id) {
    currentDishId = id;
    
    try {
        const response = await fetch(`${API_BASE}/dishes/${id}`);
        const dish = await response.json();
        
        document.getElementById('dish-detail-title').textContent = dish.name;
        
        const flags = dish.flags.map(f => {
            const className = f.toLowerCase().replace(' ', '-');
            return `<span class="flag-badge ${className}">${f}</span>`;
        }).join('');
        
        document.getElementById('dish-detail-content').innerHTML = `
            ${dish.photos && dish.photos.length > 0 ? `
                <div class="detail-section">
                    <h3>Фотографии</h3>
                    <div class="detail-photos">
                        ${dish.photos.map(url => `<img src="${escapeHtml(url)}" class="detail-photo">`).join('')}
                    </div>
                </div>
            ` : ''}
            
            <div class="detail-section">
                <h3>Основная информация</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Категория</span>
                        <span class="detail-value">${dish.category}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Размер порции</span>
                        <span class="detail-value">${dish.servingSize} г</span>
                    </div>
                </div>
            </div>
            
            <div class="detail-section">
                <h3>КБЖУ на порцию</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Калорийность</span>
                        <span class="detail-value">${dish.calories.toFixed(1)} ккал</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Белки</span>
                        <span class="detail-value">${dish.proteins.toFixed(1)} г</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Жиры</span>
                        <span class="detail-value">${dish.fats.toFixed(1)} г</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Углеводы</span>
                        <span class="detail-value">${dish.carbohydrates.toFixed(1)} г</span>
                    </div>
                </div>
            </div>
            
            <div class="detail-section">
                <h3>Состав</h3>
                <div class="detail-ingredients">
                    ${dish.ingredients.map(ing => `
                        <div class="detail-ingredient">
                            <span>${escapeHtml(ing.productName)}</span>
                            <span>${ing.quantity} г</span>
                        </div>
                    `).join('')}
                </div>
            </div>
            
            ${flags ? `
                <div class="detail-section">
                    <h3>Флаги</h3>
                    <div class="card-flags">${flags}</div>
                </div>
            ` : ''}
            
            <div class="detail-section">
                <h3>Даты</h3>
                <div class="detail-info">
                    <div class="detail-row">
                        <span class="detail-label">Создано</span>
                        <span class="detail-value">${formatDate(dish.createdAt)}</span>
                    </div>
                    ${dish.updatedAt ? `
                        <div class="detail-row">
                            <span class="detail-label">Обновлено</span>
                            <span class="detail-value">${formatDate(dish.updatedAt)}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
        
        document.getElementById('dish-detail-modal').classList.add('active');
    } catch (error) {
        showToast('Ошибка загрузки блюда', 'error');
    }
}

function closeDishDetailModal() {
    document.getElementById('dish-detail-modal').classList.remove('active');
}

function editCurrentDish() {
    const idToEdit = currentDishId;
    closeDishDetailModal();
    currentDishId = null;
    openDishModal(idToEdit);
}

async function deleteCurrentDish() {
    if (!currentDishId) return;
    
    if (!confirm('Вы уверены, что хотите удалить это блюдо?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/dishes/${currentDishId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Ошибка удаления');
        }
        
        showToast('Блюдо удалено', 'success');
        closeDishDetailModal();
        currentDishId = null;
        loadDishes();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ==================== Utilities ====================

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function showToast(message, type = '') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}