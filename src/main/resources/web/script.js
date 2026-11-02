// GZMN smart gradient editor. Talks to the plugin's /api endpoints; with a
// ?token= link from /gb web it can apply the design straight to your brush.
(function () {
    const token = new URLSearchParams(location.search).get('token');
    const state = {
        mode: 'auto', previewMaterial: 'stone_bricks', base: 'stone_bricks', generator: 'aged',
        from: 'sandstone', to: 'deepslate_tiles', custom: ['stone', 'andesite', 'deepslate'], customRaw: '',
        preset: null, steps: 0, axis: 'auto', target: 'clicked', dither: 'ordered', softness: 0.6,
        shapes: true, radius: 4
    };
    let blocks = new Map();
    let palette = null;
    const $ = (id) => document.getElementById(id);

    function api(path, options) {
        return fetch(path, options).then((r) => r.json().then((body) => {
            if (!r.ok) throw new Error(body.error || r.statusText);
            return body;
        }));
    }

    // ---- palette spec --------------------------------------------------------------------------------------------
    function spec() {
        switch (state.mode) {
            case 'auto': return 'auto';
            case 'block': return state.generator + ':' + state.base;
            case 'ramp': return state.from + '..' + state.to;
            case 'preset': return state.preset ? 'preset:' + state.preset : 'auto';
            default: return state.customRaw.trim() || state.custom.join(',');
        }
    }

    function loadSpec(text) {
        if (text === 'auto') { state.mode = 'auto'; return; }
        const colon = text.indexOf(':');
        if (text.startsWith('aged:') || text.startsWith('shades:')) {
            state.mode = 'block'; state.generator = text.slice(0, colon); state.base = text.slice(colon + 1); return;
        }
        if (text.startsWith('preset:')) { state.mode = 'preset'; state.preset = text.slice(7); return; }
        if (text.includes('..') && !text.includes('[') && !text.includes(',')) {
            const [a, b] = text.split('..'); state.mode = 'ramp'; state.from = a; state.to = b; return;
        }
        state.mode = 'custom';
        if (text.includes('[') || text.includes('%') || text.includes('#')) { state.customRaw = text; }
        else { state.custom = text.split(','); state.customRaw = ''; }
    }

    // ---- block inputs ----------------------------------------------------------------------------------------------
    function blockInputs() {
        document.querySelectorAll('.block-input').forEach((holder) => {
            const field = holder.dataset.field;
            holder.innerHTML = '';
            const sw = document.createElement('span');
            sw.className = 'swatch';
            const input = document.createElement('input');
            input.setAttribute('list', 'blocks');
            input.value = field === 'customAdd' ? '' : (state[field] || '');
            const paint = () => {
                const b = blocks.get(input.value.trim());
                sw.style.background = b ? b.color : 'transparent';
            };
            input.addEventListener('input', () => {
                paint();
                if (field !== 'customAdd' && blocks.has(input.value.trim())) {
                    state[field] = input.value.trim();
                    refresh();
                }
            });
            paint();
            holder.append(sw, input);
            holder.input = input;
        });
    }

    function renderCustom() {
        const list = $('custom-list');
        list.innerHTML = '';
        state.custom.forEach((id, i) => {
            const li = document.createElement('li');
            const sw = document.createElement('span');
            sw.className = 'swatch';
            sw.style.background = (blocks.get(id) || {}).color || 'transparent';
            li.append(sw, ' ' + id + ' ');
            const up = button('↑', () => { if (i > 0) { swap(i, i - 1); } });
            const remove = button('✕', () => { state.custom.splice(i, 1); renderCustom(); refresh(); });
            li.append(up, remove);
            list.append(li);
        });
    }

    function swap(a, b) {
        const t = state.custom[a]; state.custom[a] = state.custom[b]; state.custom[b] = t;
        renderCustom(); refresh();
    }

    function button(label, onClick) {
        const b = document.createElement('button');
        b.textContent = label;
        b.addEventListener('click', onClick);
        return b;
    }

    function renderPresets(presets) {
        const list = $('preset-list');
        list.innerHTML = '';
        presets.forEach((p) => {
            const b = button('', () => { state.preset = p.id; renderPresetsActive(); refresh(); });
            b.dataset.id = p.id;
            p.blocks.forEach((id) => {
                const sw = document.createElement('span');
                sw.className = 'swatch';
                sw.style.background = (blocks.get(id) || {}).color || '#888';
                b.append(sw);
            });
            b.append(' ' + p.name);
            b.title = p.description || '';
            list.append(b);
        });
        renderPresetsActive();
    }

    function renderPresetsActive() {
        document.querySelectorAll('#preset-list button').forEach((b) => b.classList.toggle('active', b.dataset.id === state.preset));
    }

    function renderMode() {
        document.querySelectorAll('#modes button').forEach((b) => b.classList.toggle('active', b.dataset.mode === state.mode));
        document.querySelectorAll('.mode').forEach((m) => { m.hidden = m.dataset.for !== state.mode; });
    }

    // ---- preview -----------------------------------------------------------------------------------------------------
    const G = 1.2207440846057596, A1 = 1 / G, A2 = 1 / (G * G), A3 = 1 / (G * G * G);

    function threshold(x, y, z) {
        if (state.dither === 'none') return 0.5;
        if (state.dither === 'noise') return hash(x, y, z, 99);
        const v = x * A1 + y * A2 + z * A3;
        return v - Math.floor(v);
    }

    function hash(x, y, z, seed) {
        let h = (x * 374761393 + y * 668265263 + z * 2147483647 + seed * 144269504) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
    }

    function blockAt(t, thr, roll) {
        const stops = palette.stops;
        const pick = (stop) => {
            const total = stop.reduce((s, w) => s + w.weight, 0);
            let r = roll * total;
            for (const w of stop) { r -= w.weight; if (r < 0) return w; }
            return stop[stop.length - 1];
        };
        if (stops.length === 1) return pick(stops[0]);
        const f = Math.max(0, Math.min(1, t)) * (stops.length - 1);
        const i = Math.min(stops.length - 2, Math.floor(f));
        const frac = f - i;
        const s = state.softness;
        const blend = s <= 0 ? (frac < 0.5 ? 0 : 1) : Math.max(0, Math.min(1, (frac - 0.5) / s + 0.5));
        return pick(stops[blend > thr ? i + 1 : i]);
    }

    function drawPreview() {
        const canvas = $('preview');
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        if (!palette) return;
        const w = 16, h = 20, cell = canvas.width / w;
        const flat = state.axis === 'x' || state.axis === '-x' || state.axis === 'z' || state.axis === '-z';
        for (let y = 0; y < h; y++) {
            for (let x = 0; x < w; x++) {
                // A tower outline: battlements on top, a doorway at the bottom.
                if (y === h - 1 && x % 2 === 1) continue;
                if (y < 3 && x >= 7 && x <= 8) continue;
                let t;
                switch (state.axis) {
                    case 'down': t = 1 - y / (h - 1); break;
                    case 'x': case 'z': t = x / (w - 1); break;
                    case '-x': case '-z': t = 1 - x / (w - 1); break;
                    case 'radial': t = Math.abs(x - (w - 1) / 2) / ((w - 1) / 2); break;
                    case 'inward': t = 1 - Math.abs(x - (w - 1) / 2) / ((w - 1) / 2); break;
                    default: t = y / (h - 1);
                }
                const b = blockAt(t, threshold(x, y, 0), hash(x, y, 0, 7));
                ctx.fillStyle = b.color;
                const py = canvas.height - (y + 1) * cell;
                ctx.fillRect(x * cell, py, cell, cell);
                ctx.fillStyle = 'rgba(0,0,0,0.12)';
                ctx.fillRect(x * cell, py + cell - 1, cell, 1);
                ctx.fillRect(x * cell + cell - 1, py, 1, cell);
            }
        }
        if (flat) {
            ctx.fillStyle = 'rgba(0,0,0,0.35)';
            ctx.font = '12px system-ui';
            ctx.fillText('along ' + state.axis, 6, 16);
        }
    }

    function renderSwatches() {
        const out = $('swatches');
        out.innerHTML = '';
        if (!palette) return;
        palette.stops.forEach((stop) => {
            const span = document.createElement('span');
            stop.forEach((w) => {
                const sw = document.createElement('span');
                sw.className = 'swatch';
                sw.style.background = w.color;
                span.append(sw);
            });
            span.append(stop.map((w) => w.block).join(' / '));
            out.append(span);
        });
    }

    // ---- refresh --------------------------------------------------------------------------------------------------------
    let timer = null;
    function refresh() {
        renderMode();
        $('steps-out').textContent = state.steps === 0 ? 'auto' : state.steps;
        $('softness-out').textContent = state.softness;
        $('radius-out').textContent = state.radius;
        $('command').value = command();
        clearTimeout(timer);
        timer = setTimeout(loadPalette, 150);
    }

    function loadPalette() {
        const q = new URLSearchParams({ spec: spec(), steps: state.steps, material: state.previewMaterial });
        api('/api/palette?' + q).then((p) => {
            palette = p;
            $('palette-error').textContent = '';
            drawPreview();
            renderSwatches();
        }).catch((e) => { $('palette-error').textContent = e.message; });
    }

    function command() {
        const parts = ['/gb', spec()];
        if (state.steps) parts.push('steps=' + state.steps);
        if (state.axis !== 'auto') parts.push('axis=' + state.axis);
        if (state.target !== 'clicked') parts.push('target=' + state.target);
        if (state.dither !== 'ordered') parts.push('dither=' + state.dither);
        if (state.softness !== 0.6) parts.push('softness=' + state.softness);
        if (!state.shapes) parts.push('shapes=full');
        if (state.radius !== 4) parts.push('radius=' + state.radius);
        return parts.join(' ');
    }

    function apply() {
        const body = {
            palette: spec(), steps: String(state.steps), axis: state.axis, target: state.target,
            dither: state.dither, softness: String(state.softness), shapes: state.shapes ? 'keep' : 'full',
            radius: String(state.radius)
        };
        $('apply-status').textContent = 'Sending...';
        api('/api/apply?token=' + encodeURIComponent(token), {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        }).then(() => { $('apply-status').textContent = 'Applied. Your brush in game uses it now.'; })
            .catch((e) => { $('apply-status').textContent = e.message; });
    }

    // ---- wiring ----------------------------------------------------------------------------------------------------------
    function syncControls() {
        $('steps').value = state.steps;
        $('axis').value = state.axis;
        $('target').value = state.target;
        $('dither').value = state.dither;
        $('softness').value = state.softness;
        $('shapes').checked = state.shapes;
        $('radius').value = state.radius;
        $('custom-raw').value = state.customRaw;
        document.querySelectorAll('input[name=generator]').forEach((r) => { r.checked = r.value === state.generator; });
    }

    function wire() {
        document.querySelectorAll('#modes button').forEach((b) =>
            b.addEventListener('click', () => { state.mode = b.dataset.mode; refresh(); }));
        document.querySelectorAll('input[name=generator]').forEach((r) =>
            r.addEventListener('change', () => { state.generator = r.value; refresh(); }));
        $('steps').addEventListener('input', (e) => { state.steps = parseInt(e.target.value, 10); refresh(); });
        $('softness').addEventListener('input', (e) => { state.softness = parseFloat(e.target.value); refresh(); });
        $('radius').addEventListener('input', (e) => { state.radius = parseInt(e.target.value, 10); refresh(); });
        ['axis', 'target', 'dither'].forEach((id) =>
            $(id).addEventListener('change', (e) => { state[id] = e.target.value; refresh(); }));
        $('shapes').addEventListener('change', (e) => { state.shapes = e.target.checked; refresh(); });
        $('custom-raw').addEventListener('input', (e) => { state.customRaw = e.target.value; refresh(); });
        $('custom-add-btn').addEventListener('click', () => {
            const input = document.querySelector('[data-field=customAdd]').input;
            const id = input.value.trim();
            if (blocks.has(id)) { state.custom.push(id); input.value = ''; renderCustom(); refresh(); }
        });
        $('copy-btn').addEventListener('click', () => {
            navigator.clipboard.writeText($('command').value).then(() => {
                $('copy-btn').textContent = 'Copied';
                setTimeout(() => { $('copy-btn').textContent = 'Copy'; }, 1500);
            });
        });
        $('apply-btn').addEventListener('click', apply);
    }

    function start() {
        wire();
        Promise.all([api('/api/blocks'), api('/api/presets')]).then(([blockList, presets]) => {
            blocks = new Map(blockList.map((b) => [b.id, b]));
            const datalist = $('blocks');
            blockList.forEach((b) => { const o = document.createElement('option'); o.value = b.id; datalist.append(o); });
            renderPresets(presets);
            const session = token ? api('/api/session?token=' + encodeURIComponent(token)) : Promise.resolve(null);
            return session.then((s) => {
                if (s) {
                    const st = s.settings;
                    loadSpec(st.palette);
                    Object.assign(state, {
                        steps: st.steps, axis: st.axis, target: st.target, dither: st.dither,
                        softness: st.softness, shapes: st.shapes === 'keep', radius: st.radius
                    });
                    $('radius').max = s.maxRadius;
                    $('session').textContent = 'Editing ' + s.player + "'s brush";
                    $('session').classList.add('live');
                    $('apply-btn').hidden = false;
                }
            }).catch((e) => { $('session').textContent = e.message; });
        }).then(() => {
            blockInputs();
            renderCustom();
            syncControls();
            refresh();
        }).catch((e) => { $('palette-error').textContent = 'Could not reach the server: ' + e.message; });
    }

    document.addEventListener('DOMContentLoaded', start);
})();
