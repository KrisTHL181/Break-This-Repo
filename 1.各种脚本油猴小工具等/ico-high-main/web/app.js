// ICO Generator - JavaScript
// All emoji icons replaced with SVG for professional UI

// SVG Icon Templates (Lucide style)
const icons = {
    image: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>',
    folder: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>',
    trash: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>',
    download: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>',
    package: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/><path d="M10 12h4"/></svg>',
    sparkles: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>',
    loader: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="animate-spin"><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/><line x1="4.93" y1="19.07" x2="7.76" y2="16.24"/><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"/></svg>',
    target: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>',
    close: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>'
};

// Global state
let uploadedImages = [];
let generatedFiles = [];

// DOM elements
const dropZone = document.getElementById('dropZone');
const imagePreviewList = document.getElementById('imagePreviewList');
const imageCount = document.getElementById('imageCount');
const btnAddImage = document.getElementById('btnAddImage');
const btnAddFolder = document.getElementById('btnAddFolder');
const btnClearAll = document.getElementById('btnClearAll');
const btnSelectAll = document.getElementById('btnSelectAll');
const btnDeselectAll = document.getElementById('btnDeselectAll');
const btnGenerateIco = document.getElementById('btnGenerateIco');
const btnPackageZip = document.getElementById('btnPackageZip');
const btnAbout = document.getElementById('btnAbout');
const btnCloseAbout = document.getElementById('btnCloseAbout');
const btnCloseResult = document.getElementById('btnCloseResult');
const btnSaveIco = document.getElementById('btnSaveIco');
const btnDownloadZip = document.getElementById('btnDownloadZip');
const aboutModal = document.getElementById('aboutModal');
const resultModal = document.getElementById('resultModal');
const resultContent = document.getElementById('resultContent');
const customSizes = document.getElementById('customSizes');
const colorMode = document.getElementById('colorMode');

// Titlebar elements
const btnMinimize = document.getElementById('btnMinimize');
const btnClose = document.getElementById('btnClose');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    updateUI();
});

// Bind events
function bindEvents() {
    // Titlebar window controls
    if (btnMinimize) {
        btnMinimize.addEventListener('click', () => {
            if (window.pywebview && window.pywebview.api) {
                window.pywebview.api.minimize_window();
            }
        });
    }
    if (btnClose) {
        btnClose.addEventListener('click', () => {
            if (window.pywebview && window.pywebview.api) {
                window.pywebview.api.close_window();
            }
        });
    }

    // Add image button
    btnAddImage.addEventListener('click', () => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.multiple = true;
        input.onchange = (e) => {
            handleDroppedFiles(e.target.files);
        };
        input.click();
    });

    // Add folder button
    btnAddFolder.addEventListener('click', () => {
        const input = document.createElement('input');
        input.type = 'file';
        input.webkitdirectory = true;
        input.directory = true;
        input.onchange = (e) => {
            handleDroppedFiles(e.target.files);
        };
        input.click();
    });

    // Clear all button
    btnClearAll.addEventListener('click', () => {
        uploadedImages = [];
        updateImagePreview();
        updateUI();
    });

    // Drop zone events
    dropZone.addEventListener('click', () => btnAddImage.click());
    dropZone.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            btnAddImage.click();
        }
    });
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('drag-over');
    });
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        handleDroppedFiles(e.dataTransfer.files);
    });

    // Select all / deselect all
    btnSelectAll.addEventListener('click', () => {
        document.querySelectorAll('.size-checkbox input').forEach(cb => cb.checked = true);
    });
    btnDeselectAll.addEventListener('click', () => {
        document.querySelectorAll('.size-checkbox input').forEach(cb => cb.checked = false);
    });

    // Generate ICO
    btnGenerateIco.addEventListener('click', generateIco);

    // Package ZIP
    btnPackageZip.addEventListener('click', packageZip);

    // About modal
    btnAbout.addEventListener('click', () => {
        aboutModal.classList.add('show');
        btnCloseAbout.focus();
    });
    btnCloseAbout.addEventListener('click', () => {
        aboutModal.classList.remove('show');
        btnAbout.focus();
    });

    // Result modal
    btnCloseResult.addEventListener('click', () => {
        resultModal.classList.remove('show');
        btnGenerateIco.focus();
    });
    btnSaveIco.addEventListener('click', saveIcoFiles);
    btnDownloadZip.addEventListener('click', downloadZip);

    // Click modal backdrop to close
    aboutModal.addEventListener('click', (e) => {
        if (e.target === aboutModal) {
            aboutModal.classList.remove('show');
            btnAbout.focus();
        }
    });
    resultModal.addEventListener('click', (e) => {
        if (e.target === resultModal) {
            resultModal.classList.remove('show');
            btnGenerateIco.focus();
        }
    });

    // Escape key to close modals
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            if (aboutModal.classList.contains('show')) {
                aboutModal.classList.remove('show');
                btnAbout.focus();
            }
            if (resultModal.classList.contains('show')) {
                resultModal.classList.remove('show');
                btnGenerateIco.focus();
            }
        }
    });
}

// Add images to preview
function addImagesToPreview(images) {
    uploadedImages = uploadedImages.concat(images);
    updateImagePreview();
    updateUI();
}

// Update image preview
function updateImagePreview() {
    imagePreviewList.innerHTML = '';

    if (uploadedImages.length === 0) {
        dropZone.classList.remove('has-images');
        return;
    }

    dropZone.classList.add('has-images');

    uploadedImages.forEach((img, index) => {
        const item = document.createElement('div');
        item.className = 'image-preview-item';
        item.dataset.index = index;
        item.setAttribute('role', 'listitem');
        item.innerHTML = `
            <img src="${img.preview}" alt="${img.name}">
            <button class="remove-btn" aria-label="移除图像 ${img.name}">${icons.close}</button>
            <div class="file-name">${img.name}</div>
        `;
        imagePreviewList.appendChild(item);
    });
}

// Delete image (event delegation)
imagePreviewList.addEventListener('click', (e) => {
    if (e.target.closest('.remove-btn')) {
        const item = e.target.closest('.image-preview-item');
        const index = parseInt(item.dataset.index);
        
        if (!isNaN(index) && index >= 0 && index < uploadedImages.length) {
            uploadedImages.splice(index, 1);
            updateImagePreview();
            updateUI();
        }
    }
});

// Update UI state
function updateUI() {
    imageCount.textContent = uploadedImages.length;
    btnGenerateIco.disabled = uploadedImages.length === 0;
    btnPackageZip.disabled = uploadedImages.length === 0 || generatedFiles.length === 0;
}

// Handle dropped files
function handleDroppedFiles(files) {
    const imageFiles = Array.from(files).filter(f => f.type.startsWith('image/'));
    if (imageFiles.length === 0) {
        showNotification('没有找到有效的图片文件', 'warning');
        return;
    }
    
    let loadedCount = 0;
    const startId = uploadedImages.length;
    const images = new Array(imageFiles.length);
    
    imageFiles.forEach((file, idx) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            images[idx] = {
                id: startId + idx,
                name: file.name,
                preview: e.target.result,
                path: file.name
            };
            loadedCount++;
            if (loadedCount === imageFiles.length) {
                addImagesToPreview(images.filter(Boolean));
            }
        };
        reader.onerror = () => {
            loadedCount++;
            if (loadedCount === imageFiles.length) {
                const validImages = images.filter(Boolean);
                if (validImages.length > 0) {
                    addImagesToPreview(validImages);
                }
            }
        };
        reader.readAsDataURL(file);
    });
}

// Get selected sizes
function getSelectedSizes() {
    const sizes = [];
    document.querySelectorAll('.size-checkbox input:checked').forEach(cb => {
        sizes.push(parseInt(cb.value));
    });

    // Add custom sizes
    const custom = customSizes.value.trim();
    if (custom) {
        const customList = custom.split(',').map(s => parseInt(s.trim())).filter(s => s && !isNaN(s));
        sizes.push(...customList);
    }

    return [...new Set(sizes)]; // Deduplicate
}

// Resize image to specified size
function resizeImage(img, size) {
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    
    // High quality scaling
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';
    
    // Draw image (keep ratio, center crop)
    const scale = Math.max(size / img.width, size / img.height);
    const x = (size - img.width * scale) / 2;
    const y = (size - img.height * scale) / 2;
    
    ctx.drawImage(img, x, y, img.width * scale, img.height * scale);
    
    return canvas;
}

// Create ICO file (BMP format)
async function createIcoFile(images, sizes, colorModeValue) {
    const icoImages = [];
    
    for (const imgData of images) {
        const img = await loadImage(imgData.preview);
        const iconImages = [];
        
        for (const size of sizes) {
            const canvas = resizeImage(img, size);
            const bmpData = canvasToBmp(canvas, colorModeValue === 'rgba');
            iconImages.push({
                size: size,
                data: bmpData
            });
        }
        
        // Build ICO file
        const icoBuffer = buildIcoFile(iconImages);
        icoImages.push({
            name: imgData.name.replace(/\.[^/.]+$/, '') + '.ico',
            data: icoBuffer
        });
    }
    
    return icoImages;
}

// Load image
function loadImage(src) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = reject;
        img.src = src;
    });
}

// Canvas to BMP
function canvasToBmp(canvas, hasAlpha) {
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const imageData = ctx.getImageData(0, 0, width, height);
    const data = imageData.data;
    
    const bitsPerPixel = hasAlpha ? 32 : 24;
    const rowSize = Math.floor((bitsPerPixel * width + 31) / 32) * 4;
    const imageSize = rowSize * height;
    const headerSize = 40;
    const fileSize = headerSize + imageSize;
    
    const buffer = new ArrayBuffer(fileSize);
    const view = new DataView(buffer);
    
    // BMP file header
    let offset = 0;
    view.setUint32(offset, headerSize, true); offset += 4; // Header size
    view.setInt32(offset, width, true); offset += 4;       // Width
    view.setInt32(offset, height * 2, true); offset += 4;  // Height (double for XOR and AND masks)
    view.setUint16(offset, 1, true); offset += 2;          // Planes
    view.setUint16(offset, bitsPerPixel, true); offset += 2; // Bit depth
    view.setUint32(offset, hasAlpha ? 0 : 0, true); offset += 4; // Compression
    view.setUint32(offset, imageSize, true); offset += 4;  // Image size
    view.setInt32(offset, 2835, true); offset += 4;        // X resolution
    view.setInt32(offset, 2835, true); offset += 4;        // Y resolution
    view.setUint32(offset, 0, true); offset += 4;          // Colors
    view.setUint32(offset, 0, true); offset += 4;          // Important colors
    
    // Pixel data (bottom to top)
    for (let y = height - 1; y >= 0; y--) {
        for (let x = 0; x < width; x++) {
            const i = (y * width + x) * 4;
            const b = data[i];
            const g = data[i + 1];
            const r = data[i + 2];
            const a = data[i + 3];
            
            view.setUint8(offset++, b);
            view.setUint8(offset++, g);
            view.setUint8(offset++, r);
            if (hasAlpha) {
                view.setUint8(offset++, a);
            }
        }
        // Row padding
        const padding = rowSize - (width * (bitsPerPixel / 8));
        for (let p = 0; p < padding; p++) {
            view.setUint8(offset++, 0);
        }
    }
    
    return new Uint8Array(buffer);
}

// Build ICO file
function buildIcoFile(iconImages) {
    const numImages = iconImages.length;
    const headerSize = 6 + numImages * 16;
    let dataOffset = headerSize;
    
    // Calculate total size
    let totalSize = headerSize;
    for (const img of iconImages) {
        totalSize += img.data.length;
    }
    
    const buffer = new ArrayBuffer(totalSize);
    const view = new DataView(buffer);
    
    // ICO file header
    let offset = 0;
    view.setUint16(offset, 0, true); offset += 2;      // Reserved
    view.setUint16(offset, 1, true); offset += 2;      // Type (1 = ICO)
    view.setUint16(offset, numImages, true); offset += 2; // Image count
    
    // Directory entries
    for (const img of iconImages) {
        const size = img.size;
        view.setUint8(offset++, size > 255 ? 0 : size);  // Width
        view.setUint8(offset++, size > 255 ? 0 : size);  // Height
        view.setUint8(offset++, 0);                      // Colors
        view.setUint8(offset++, 0);                      // Reserved
        view.setUint16(offset, 1, true); offset += 2;    // Color planes
        view.setUint16(offset, 32, true); offset += 2;   // Bit depth
        view.setUint32(offset, img.data.length, true); offset += 4; // Data size
        view.setUint32(offset, dataOffset, true); offset += 4; // Data offset
        dataOffset += img.data.length;
    }
    
    // Image data
    for (const img of iconImages) {
        for (let i = 0; i < img.data.length; i++) {
            view.setUint8(offset++, img.data[i]);
        }
    }
    
    return new Uint8Array(buffer);
}

// Generate ICO
async function generateIco() {
    const sizes = getSelectedSizes();
    if (sizes.length === 0) {
        showNotification('请至少选择一个尺寸', 'error');
        return;
    }

    btnGenerateIco.disabled = true;
    btnGenerateIco.innerHTML = `<span class="btn-icon">${icons.loader}</span><span>生成中...</span>`;

    try {
        const icoFiles = await createIcoFile(uploadedImages, sizes, colorMode.value);
        
        generatedFiles = icoFiles.map((file, i) => ({
            name: file.name,
            data: file.data,
            sizes: sizes
        }));
        
        showResultModal(generatedFiles);
        updateUI();
        showNotification('ICO 文件生成成功', 'success');
    } catch (error) {
        showNotification('生成失败：' + error.message, 'error');
    } finally {
        btnGenerateIco.disabled = false;
        btnGenerateIco.innerHTML = `<span class="btn-icon">${icons.sparkles}</span><span>生成 ICO</span>`;
    }
}

// Show result modal
function showResultModal(files) {
    resultContent.innerHTML = `
        <div class="result-file-list" role="list" aria-label="生成的文件列表">
            ${files.map(f => `
                <div class="result-file-item" role="listitem">
                    <span class="result-file-icon">${icons.target}</span>
                    <div class="result-file-info">
                        <div class="result-file-name">${f.name}</div>
                        <div class="result-file-sizes">尺寸: ${f.sizes.join(', ')}px</div>
                    </div>
                </div>
            `).join('')}
        </div>
        <p style="color: var(--color-text-secondary); font-size: var(--font-size-base);">共生成 ${files.length} 个 ICO 文件</p>
    `;
    resultModal.classList.add('show');
    btnSaveIco.focus();
}

// Save ICO files
async function saveIcoFiles() {
    for (const file of generatedFiles) {
        const blob = new Blob([file.data], { type: 'image/x-icon' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = file.name;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
    showNotification('ICO 文件已下载', 'success');
    resultModal.classList.remove('show');
}

// Package ZIP
async function packageZip() {
    if (generatedFiles.length === 0) {
        showNotification('请先生成 ICO 文件', 'error');
        return;
    }

    btnPackageZip.disabled = true;
    btnPackageZip.innerHTML = `<span class="btn-icon">${icons.loader}</span><span>打包中...</span>`;

    try {
        const zipBlob = await createZip(generatedFiles);
        const url = URL.createObjectURL(zipBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'icons.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showNotification('ZIP 打包成功', 'success');
    } catch (error) {
        showNotification('打包失败：' + error.message, 'error');
    } finally {
        btnPackageZip.disabled = false;
        btnPackageZip.innerHTML = `<span class="btn-icon">${icons.package}</span><span>打包 ZIP</span>`;
    }
}

// Create ZIP file (simplified implementation)
async function createZip(files) {
    const zipParts = [];
    let centralDir = [];
    let offset = 0;
    
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const nameBytes = new TextEncoder().encode(file.name);
        const data = file.data;
        
        // Local file header
        const localHeader = new ArrayBuffer(30 + nameBytes.length);
        const lhView = new DataView(localHeader);
        let pos = 0;
        
        lhView.setUint32(pos, 0x04034b50, true); pos += 4;  // Signature
        lhView.setUint16(pos, 20, true); pos += 2;          // Version
        lhView.setUint16(pos, 0, true); pos += 2;           // Flags
        lhView.setUint16(pos, 0, true); pos += 2;           // Compression method (store)
        lhView.setUint16(pos, 0, true); pos += 2;           // Time
        lhView.setUint16(pos, 0, true); pos += 2;           // Date
        lhView.setUint32(pos, crc32(data), true); pos += 4; // CRC32
        lhView.setUint32(pos, data.length, true); pos += 4; // Compressed size
        lhView.setUint32(pos, data.length, true); pos += 4; // Uncompressed size
        lhView.setUint16(pos, nameBytes.length, true); pos += 2; // Name length
        lhView.setUint16(pos, 0, true); pos += 2;           // Extra field length
        
        // Write filename
        const lhBytes = new Uint8Array(localHeader);
        lhBytes.set(nameBytes, pos);
        
        zipParts.push(new Uint8Array(localHeader));
        zipParts.push(data);
        
        // Central directory record
        const cdHeader = new ArrayBuffer(46 + nameBytes.length);
        const cdView = new DataView(cdHeader);
        pos = 0;
        
        cdView.setUint32(pos, 0x02014b50, true); pos += 4;  // Signature
        cdView.setUint16(pos, 20, true); pos += 2;          // Created version
        cdView.setUint16(pos, 20, true); pos += 2;          // Needed version
        cdView.setUint16(pos, 0, true); pos += 2;           // Flags
        cdView.setUint16(pos, 0, true); pos += 2;           // Compression method
        cdView.setUint16(pos, 0, true); pos += 2;           // Time
        cdView.setUint16(pos, 0, true); pos += 2;           // Date
        cdView.setUint32(pos, crc32(data), true); pos += 4; // CRC32
        cdView.setUint32(pos, data.length, true); pos += 4; // Compressed size
        cdView.setUint32(pos, data.length, true); pos += 4; // Uncompressed size
        cdView.setUint16(pos, nameBytes.length, true); pos += 2; // Name length
        cdView.setUint16(pos, 0, true); pos += 2;           // Extra field length
        cdView.setUint16(pos, 0, true); pos += 2;           // Comment length
        cdView.setUint16(pos, 0, true); pos += 2;           // Disk number
        cdView.setUint16(pos, 0, true); pos += 2;           // Internal attributes
        cdView.setUint32(pos, 0, true); pos += 4;           // External attributes
        cdView.setUint32(pos, offset, true); pos += 4;      // Local header offset
        
        const cdBytes = new Uint8Array(cdHeader);
        cdBytes.set(nameBytes, pos);
        
        centralDir.push(new Uint8Array(cdHeader));
        offset += localHeader.byteLength + data.length;
    }
    
    const centralDirOffset = offset;
    let centralDirSize = 0;
    
    for (const cd of centralDir) {
        centralDirSize += cd.length;
    }
    
    // End of central directory record
    const eocd = new ArrayBuffer(22);
    const eocdView = new DataView(eocd);
    eocdView.setUint32(0, 0x06054b50, true);           // Signature
    eocdView.setUint16(4, 0, true);                    // Disk number
    eocdView.setUint16(6, 0, true);                    // Central directory disk
    eocdView.setUint16(8, files.length, true);         // Records on disk
    eocdView.setUint16(10, files.length, true);        // Total records
    eocdView.setUint32(12, centralDirSize, true);      // Central directory size
    eocdView.setUint32(16, centralDirOffset, true);    // Central directory offset
    eocdView.setUint16(20, 0, true);                   // Comment length
    
    // Merge all parts
    const totalSize = offset + centralDirSize + 22;
    const zipData = new Uint8Array(totalSize);
    let zipPos = 0;
    
    for (const part of zipParts) {
        zipData.set(part, zipPos);
        zipPos += part.length;
    }
    
    for (const cd of centralDir) {
        zipData.set(cd, zipPos);
        zipPos += cd.length;
    }
    
    zipData.set(new Uint8Array(eocd), zipPos);
    
    return new Blob([zipData], { type: 'application/zip' });
}

// Simple CRC32 implementation
function crc32(data) {
    const table = new Uint32Array(256);
    for (let i = 0; i < 256; i++) {
        let c = i;
        for (let j = 0; j < 8; j++) {
            c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
        }
        table[i] = c;
    }
    
    let crc = 0xFFFFFFFF;
    for (let i = 0; i < data.length; i++) {
        crc = table[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
    }
    return (crc ^ 0xFFFFFFFF) >>> 0;
}

// Download ZIP (from result modal)
async function downloadZip() {
    if (generatedFiles.length === 0) {
        showNotification('没有可下载的文件', 'error');
        return;
    }

    try {
        const zipBlob = await createZip(generatedFiles);
        const url = URL.createObjectURL(zipBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'icons.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showNotification('ZIP 文件已下载', 'success');
        resultModal.classList.remove('show');
    } catch (error) {
        showNotification('下载失败：' + error.message, 'error');
    }
}

// Show notification
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.setAttribute('role', 'alert');
    notification.setAttribute('aria-live', 'polite');
    notification.textContent = message;
    document.body.appendChild(notification);

    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}