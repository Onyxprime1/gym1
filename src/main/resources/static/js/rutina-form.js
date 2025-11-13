/**
 * rutina-form.js
 * Maneja el formulario de creación/edición de rutinas:
 * - checkbox list de ejercicios -> crea filas con inputs reales (ejercicioId, serie, repeticiones, dia)
 * - soporta pre-poblar desde existingDetails JSON (modo editar)
 * - mantiene el orden que se envía al servidor
 * - permite agregar por select y quitar filas
 *
 * Requiere en la plantilla:
 * - checkboxes con class="exercise-checkbox" y data-eid, data-nombre, data-musculo
 * - contenedor <div id="selected-exercises"></div>
 * - un elemento oculto con id="existing-details-json" que contenga JSON si existe (opcional)
 */
document.addEventListener('DOMContentLoaded', () => {
    const selectedContainer = document.getElementById('selected-exercises');
    if (!selectedContainer) return;

    // Selecciona checkboxes (si los hay)
    const checkboxes = Array.from(document.querySelectorAll('.exercise-checkbox'));

    // Map para filas por id
    const rowById = new Map();

    function createRow(meta) {
        // meta: { id, nombre, musculo, serie, repeticiones, dia }
        const id = meta.id;
        const row = document.createElement('div');
        row.className = 'selected-row';
        row.dataset.eid = id;
        row.style.display = 'flex';
        row.style.alignItems = 'center';
        row.style.gap = '.5rem';

        // Hidden ejercicioId
        const hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = 'ejercicioId';
        hid.value = id;
        row.appendChild(hid);

        // Nombre
        const lbl = document.createElement('div');
        lbl.textContent = `${meta.nombre}${meta.musculo ? ' — ' + meta.musculo : ''}`;
        lbl.style.width = '260px';
        row.appendChild(lbl);

        // Serie
        const serie = document.createElement('input');
        serie.type = 'number';
        serie.min = '0';
        serie.name = 'serie';
        serie.placeholder = 'Series';
        serie.className = 'small-input input';
        serie.style.width = '90px';
        if (meta.serie != null) serie.value = meta.serie;
        row.appendChild(serie);

        // Repeticiones
        const reps = document.createElement('input');
        reps.type = 'number';
        reps.min = '0';
        reps.name = 'repeticiones';
        reps.placeholder = 'Reps';
        reps.className = 'small-input input';
        reps.style.width = '90px';
        if (meta.repeticiones != null) reps.value = meta.repeticiones;
        row.appendChild(reps);

        // Día
        const dia = document.createElement('input');
        dia.type = 'text';
        dia.name = 'dia';
        dia.placeholder = 'Día (ej. Lunes)';
        dia.className = 'day-input input';
        dia.style.width = '140px';
        if (meta.dia != null) dia.value = meta.dia;
        row.appendChild(dia);

        // Botón Quitar
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'button is-small is-danger';
        btn.textContent = 'Quitar';
        btn.addEventListener('click', () => {
            // desmarcar checkbox si existe
            const cb = document.querySelector(`.exercise-checkbox[data-eid="${id}"]`);
            if (cb) cb.checked = false;
            removeRow(id);
        });
        row.appendChild(btn);

        return row;
    }

    function addRow(meta) {
        const id = String(meta.id);
        if (rowById.has(id)) return;
        const row = createRow(meta);
        selectedContainer.appendChild(row);
        rowById.set(id, row);
    }

    function removeRow(id) {
        id = String(id);
        const r = rowById.get(id);
        if (!r) return;
        r.remove();
        rowById.delete(id);
    }

    // Si existen checkboxes, atar listeners
    checkboxes.forEach(cb => {
        const eid = String(cb.dataset.eid || cb.value);
        // si checkbox ya marcado (modo editar), crear fila
        if (cb.checked) {
            addRow({
                id: eid,
                nombre: cb.dataset.nombre || cb.dataset.label || cb.value,
                musculo: cb.dataset.musculo
            });
        }
        cb.addEventListener('change', (ev) => {
            if (ev.target.checked) {
                addRow({
                    id: eid,
                    nombre: cb.dataset.nombre || cb.dataset.label || cb.value,
                    musculo: cb.dataset.musculo
                });
            } else {
                removeRow(eid);
            }
        });
    });

    // Pre-popular desde existingDetails JSON (si existe)
    const existingEl = document.getElementById('existing-details-json');
    if (existingEl && existingEl.textContent.trim().length > 0) {
        try {
            // Espera un array de objetos con {id, nombre, musculo, serie, repeticiones, dia}
            const details = JSON.parse(existingEl.textContent);
            if (Array.isArray(details)) {
                details.forEach(d => {
                    // marcar checkbox si existe
                    const cb = document.querySelector(`.exercise-checkbox[data-eid="${d.id}"]`);
                    if (cb) cb.checked = true;
                    addRow({
                        id: d.id,
                        nombre: d.nombre || (cb ? cb.dataset.nombre : ''),
                        musculo: d.musculo || (cb ? cb.dataset.musculo : ''),
                        serie: d.serie,
                        repeticiones: d.repeticiones,
                        dia: d.dia
                    });
                });
            }
        } catch (err) {
            console.warn('existing-details-json parse error', err);
        }
    }

    // Acciones por select + botón (opcional)
    const addBtn = document.getElementById('btn-add-exercise');
    const allSelect = document.getElementById('all-exercises-select');
    if (addBtn && allSelect) {
        addBtn.addEventListener('click', () => {
            const eid = allSelect.value;
            if (!eid) return;
            const opt = allSelect.querySelector(`option[value="${eid}"]`);
            addRow({
                id: eid,
                nombre: opt ? opt.textContent : ('Ejercicio ' + eid)
            });
            // marcar checkbox correspondiente si existe
            const cb = document.querySelector(`.exercise-checkbox[data-eid="${eid}"]`);
            if (cb) cb.checked = true;
        });
    }

}); // DOMContentLoaded end