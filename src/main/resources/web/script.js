document.addEventListener('DOMContentLoaded', () => {
    const layersContainer = document.getElementById('layers-container');
    const addLayerBtn = document.getElementById('add-layer-btn');
    const directionSelect = document.getElementById('direction');
    const modeSelect = document.getElementById('mode');
    const generatedCodeInput = document.getElementById('generated-code');
    const copyBtn = document.getElementById('copy-btn');
    const patternPreview = document.getElementById('pattern-preview');

    let layerCount = 0;

    function addLayer() {
        layerCount++;
        const layerId = `layer-${Date.now()}`;

        const layerDiv = document.createElement('div');
        layerDiv.className = 'layer';
        layerDiv.id = layerId;

        layerDiv.innerHTML = `
            <div class="layer-header">
                <span>Layer ${layerCount}</span>
                <button class="remove-layer-btn" onclick="removeLayer('${layerId}')">Remove</button>
            </div>
            <div>
                <label>Blocks (comma separated, e.g. stone,andesite,cobblestone):</label>
                <input type="text" class="layer-input" placeholder="stone" value="stone">
            </div>
        `;

        layersContainer.appendChild(layerDiv);

        // Add event listener to new input
        layerDiv.querySelector('.layer-input').addEventListener('input', updateCode);
        updateCode();
    }

    // Make removeLayer globally accessible
    window.removeLayer = function(layerId) {
        const layer = document.getElementById(layerId);
        if (layer && document.querySelectorAll('.layer').length > 1) {
            layer.remove();
            updateLayerNumbers();
            updateCode();
        } else if (document.querySelectorAll('.layer').length <= 1) {
            alert("You must have at least one layer.");
        }
    };

    function updateLayerNumbers() {
        const layers = document.querySelectorAll('.layer');
        layerCount = 0;
        layers.forEach(layer => {
            layerCount++;
            layer.querySelector('.layer-header span').textContent = `Layer ${layerCount}`;
        });
    }

    function updateCode() {
        const direction = directionSelect.value;
        const mode = modeSelect.value;

        const layers = [];
        document.querySelectorAll('.layer-input').forEach(input => {
            const val = input.value.trim();
            if (val) {
                layers.push(`[${val}]`);
            }
        });

        if (layers.length === 0) {
            layers.push('[stone]');
        }

        const layersStr = layers.join('');
        const pattern = `#gradient[${direction}][${mode}]${layersStr}`;
        patternPreview.textContent = `Preview: ${pattern}`;

        // Create the compact code.
        // We will Base64 encode a simple JSON or delimited string.
        // Format: direction|mode|layer1|layer2|...
        const rawLayers = Array.from(document.querySelectorAll('.layer-input'))
            .map(input => input.value.trim().replace(/\|/g, '')) // prevent delimiter injection
            .filter(val => val.length > 0);

        if (rawLayers.length === 0) rawLayers.push('stone');

        const dataStr = `${direction}|${mode}|${rawLayers.join('|')}`;
        const base64 = btoa(dataStr);
        const code = `gzmn@${base64}`;

        generatedCodeInput.value = code;
    }

    addLayerBtn.addEventListener('click', addLayer);
    directionSelect.addEventListener('change', updateCode);
    modeSelect.addEventListener('change', updateCode);

    copyBtn.addEventListener('click', () => {
        generatedCodeInput.select();
        document.execCommand('copy');

        const originalText = copyBtn.textContent;
        copyBtn.textContent = 'Copied!';
        setTimeout(() => {
            copyBtn.textContent = originalText;
        }, 2000);
    });

    // Initialize with one layer
    addLayer();
});
