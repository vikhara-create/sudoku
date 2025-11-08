const API_BASE = '';

let currentPuzzle = null; // puzzle object from server

document.getElementById('loadBtn').addEventListener('click', () => {
  const id = document.getElementById('puzzleSelect').value;
  loadPuzzle(id);
});

async function loadPuzzle(id) {
  const res = await fetch(`/puzzle/${id}`);
  if (!res.ok) {
    showMsg('Failed to load puzzle');
    return;
  }
  currentPuzzle = await res.json();
  renderBoard(currentPuzzle);
}

function showMsg(t) {
  document.getElementById('msg').textContent = t;
}

function renderBoard(p) {
  const boardDiv = document.getElementById('board');
  boardDiv.innerHTML = '';
  for (let i=0;i<9;i++) {
    for (let j=0;j<9;j++) {
      const val = p.grid[i][j];
      const cellDiv = document.createElement('div');
      cellDiv.className = 'cell';
      const input = document.createElement('input');
      input.maxLength = 1;
      input.dataset.row = i;
      input.dataset.col = j;
      input.setAttribute('aria-label', `cell-${i}-${j}`);
      if (val !== 0) {
        input.value = val;
        input.disabled = true;
        cellDiv.classList.add('fixed');
      } else {
        input.value = '';
        input.addEventListener('input', onCellInput);
      }
      cellDiv.appendChild(input);
      boardDiv.appendChild(cellDiv);
    }
  }
}

async function onCellInput(e) {
  const input = e.target;
  const row = Number(input.dataset.row);
  const col = Number(input.dataset.col);
  const val = input.value.trim();
  if (val === '') {
    input.parentElement.classList.remove('valid','invalid');
    return;
  }
  if (!/^[1-9]$/.test(val)) {
    input.parentElement.classList.add('invalid');
    input.parentElement.classList.remove('valid');
    showMsg('Only digits 1-9 allowed');
    return;
  }
  validateCell(row, col, Number(val));
}

// debounce simple
let lastRequestCounter = 0;
async function validateCell(row, col, value) {
  lastRequestCounter++;
  const myCounter = lastRequestCounter;
  await new Promise(r => setTimeout(r, 120));
  if (myCounter !== lastRequestCounter) return;
  const res = await fetch('/validate', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ row, col, value, puzzleId: currentPuzzle.id })
  });
  const json = await res.json();
  const input = document.querySelector(`input[data-row='${row}'][data-col='${col}']`);
  const cell = input.parentElement;
  if (json.valid) {
    cell.classList.add('valid');
    cell.classList.remove('invalid');
    showMsg('OK');
    setTimeout(()=>cell.classList.remove('valid'), 700);
  } else {
    cell.classList.add('invalid');
    cell.classList.remove('valid');
    showMsg('Invalid: ' + (json.reason || 'conflict'));
  }
}

// load initial
loadPuzzle('p1');
