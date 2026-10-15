(() => {
  const target = document.getElementById('target-word');
  const listenButton = document.getElementById('listen-button');
  const recordButton = document.getElementById('record-button');
  const status = document.getElementById('speech-status');
  const result = document.getElementById('speech-result');
  const recognitionType = window.SpeechRecognition || window.webkitSpeechRecognition;
  const practiceLevel = Number(document.querySelector('.speech-page').dataset.level);
  let recognition;

  function sayWord() {
    if (!('speechSynthesis' in window)) { status.textContent = 'Audio playback is not available in this browser.'; return; }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(target.textContent.trim());
    utterance.rate = 0.82;
    utterance.lang = 'en-US';
    window.speechSynthesis.speak(utterance);
    status.textContent = 'Listen carefully, then say the word.';
  }

  function paintComparison(expected, spoken, id) {
    const el = document.getElementById(id);
    const normalized = spoken.toLowerCase().replace(/[^a-z]/g, '');
    el.replaceChildren();
    [...expected].forEach((character, index) => {
      const span = document.createElement('span');
      span.textContent = character;
      span.className = normalized[index] === character ? 'char-good' : 'char-miss';
      el.append(span);
    });
    if (normalized.length > expected.length) {
      [...normalized.slice(expected.length)].forEach(character => {
        const span = document.createElement('span'); span.textContent = character; span.className = 'char-miss'; el.append(span);
      });
    }
  }

  async function saveAttempt(expected, spoken) {
    const csrf = document.querySelector('meta[name="csrf-token"]')?.content;
    const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.content;
    const headers = {'Content-Type':'application/json'};
    if (csrf && csrfHeader) headers[csrfHeader] = csrf;
    const response = await fetch('/api/speech/attempt', {method:'POST', headers, body:JSON.stringify({targetWord:expected, recognizedText:spoken, level:practiceLevel})});
    if (!response.ok) throw new Error('We could not save this attempt. Please try again.');
    return response.json();
  }

  async function showResult(spoken) {
    const expected = target.textContent.trim().toLowerCase();
    status.textContent = 'Checking your practice...';
    try {
      const data = await saveAttempt(expected, spoken);
      document.getElementById('accuracy-score').textContent = `${data.accuracy}% accuracy`;
      paintComparison(expected, expected, 'expected-display');
      paintComparison(expected, spoken, 'heard-display');
      document.getElementById('feedback').textContent = data.accuracy === 100 ? 'Wonderful work! You said every letter clearly.' : 'Nice effort. The marked letters show where you can practice again.';
      result.hidden = false;
      status.textContent = 'Your attempt has been saved.';
      const count = document.getElementById('attempt-count');
      count.textContent = Number(count.textContent) + 1;
      const average = document.getElementById('average-accuracy');
      const previousCount = Number(count.textContent) - 1;
      const oldAverage = Number(average.textContent.replace('%','')) || 0;
      average.textContent = `${Math.round((oldAverage * previousCount + data.accuracy) / Number(count.textContent))}%`;
      const emptyHistory = document.querySelector('#speech-history .speech-muted');
      if (emptyHistory) emptyHistory.remove();
      const item = document.createElement('li');
      const left = document.createElement('span');
      const word = document.createElement('strong'); word.textContent = expected;
      const heard = document.createElement('small'); heard.textContent = spoken;
      left.append(word, document.createElement('br'), heard);
      const right = document.createElement('span');
      const score = document.createElement('b'); score.textContent = `${data.accuracy}%`;
      const date = document.createElement('time'); date.textContent = 'Just now';
      right.append(score, document.createElement('br'), date); item.append(left, right);
      document.getElementById('speech-history').prepend(item);
    } catch (error) { status.textContent = error.message; }
  }

  listenButton.addEventListener('click', sayWord);
  document.querySelectorAll('.word-choice').forEach(button => button.addEventListener('click', () => {
    target.textContent = button.dataset.word;
    document.querySelectorAll('.word-choice').forEach(choice => choice.setAttribute('aria-pressed', String(choice === button)));
    result.hidden = true;
    status.textContent = 'New word selected. Listen when you are ready.';
  }));
  document.getElementById('retry-button').addEventListener('click', () => { result.hidden = true; status.textContent = 'Ready for another try.'; recordButton.focus(); });

  recordButton.addEventListener('click', () => {
    if (!recognitionType) { status.textContent = 'Speech recognition is not supported in this browser. Try the latest Chrome or Edge.'; return; }
    if (!recognition) {
      recognition = new recognitionType(); recognition.lang = 'en-US'; recognition.interimResults = false; recognition.maxAlternatives = 1;
      recognition.onresult = event => showResult(event.results[0][0].transcript);
      recognition.onerror = event => { status.textContent = event.error === 'not-allowed' ? 'Microphone permission is blocked. Allow microphone access in your browser settings.' : 'We did not catch that. Please try again.'; recordButton.disabled = false; recordButton.textContent = 'Start speaking'; };
      recognition.onend = () => { recordButton.disabled = false; recordButton.textContent = 'Start speaking'; };
    }
    result.hidden = true;
    try { recognition.start(); recordButton.disabled = true; recordButton.textContent = 'Listening...'; status.textContent = 'Listening. Say the word now.'; }
    catch (_) { status.textContent = 'Please wait a moment, then try speaking again.'; recordButton.disabled = false; }
  });
})();
