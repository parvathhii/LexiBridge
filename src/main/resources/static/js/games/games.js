/**
 * LexiBridge Games Engine  â€“  games.js
 * Drives the educational games inside game-arena.html.
 * GAME_SLUG is injected by Thymeleaf before this script loads.
 */

/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
   SHARED STATE & UTILITIES
â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */
var LB_GAME = {};  // exposed globally so the Play-Again button works

var state = {
    slug:      window.GAME_SLUG || 'confusion-matrix',
    qIndex:    0,
    qTotal:    10,
    correct:   0,
    wrong:     0,
    timerSec:  30,
    timerID:   null,
    started:   false,
    finished:  false,
    startedAt: 0
};
var GAME_LEVEL = Math.max(1, Math.min(3, Number(window.GAME_LEVEL) || 1));

var $ = function(id){ return document.getElementById(id); };

function setTitle(name){
    $('game-title').textContent = name;
    document.title = name + ' - LexiBridge';
}
function setQTotal(n){
    state.qTotal = Math.max(1, GAME_LEVEL === 1 ? Math.ceil(n * 0.5) : GAME_LEVEL === 2 ? Math.ceil(n * 0.75) : n);
    $('q-total').textContent = state.qTotal;
}
function updateBoard(){
    $('q-current').textContent  = Math.min(state.qIndex + 1, state.qTotal);
    $('score-correct').textContent = state.correct;
    $('score-wrong').textContent   = state.wrong;
    $('header-score').textContent  = state.correct;
}

/* â”€â”€ Timer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
function startTimer(secs, onExpire){
    stopTimer();
    secs = Math.max(5, Math.round(secs * (GAME_LEVEL === 1 ? 1.5 : GAME_LEVEL === 2 ? 1 : 0.75)));
    state.timerSec = secs;
    $('timer-display').textContent = secs;
    $('timer-bar').style.width = '100%';
    state.timerID = setInterval(function(){
        state.timerSec--;
        $('timer-display').textContent = state.timerSec;
        $('timer-bar').style.width = ((state.timerSec / secs) * 100) + '%';
        if(state.timerSec <= 0){
            stopTimer();
            if(onExpire) onExpire();
        }
    }, 1000);
}
function stopTimer(){ clearInterval(state.timerID); state.timerID = null; }

/* â”€â”€ Result Modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
function calculateAccuracyPercent(correctAnswers, totalQuestions){
    var total = Number(totalQuestions);
    if(!Number.isFinite(total) || total <= 0) return 0;
    total = Math.floor(total);
    var correct = Number(correctAnswers);
    correct = Number.isFinite(correct) ? Math.max(0, Math.min(total, Math.floor(correct))) : 0;
    return Math.round((correct / total) * 100);
}

function showResult(){
    if(state.finished) return;
    stopTimer();
    state.finished = true;
    var pct = calculateAccuracyPercent(state.correct, state.qTotal);
    document.getElementById("result-emoji").textContent = "";
    $('result-title').textContent   = pct >= 80 ? 'Excellent!' : pct >= 50 ? 'Good job!' : 'Keep going!';
    $('result-msg').textContent     = state.correct + ' / ' + state.qTotal + ' correct  (' + pct + '%)';
    $('result-score').textContent   = pct + '%';
    $('result-overlay').classList.add('active');
    saveAttempt(pct);
}

/* â”€â”€ Save to backend â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
function saveAttempt(score){
    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    var headers = {'Content-Type':'application/json'};
    if (csrfToken && csrfHeader) headers[csrfHeader.content] = csrfToken.content;
    var status = $('result-save-status');
    if (status) status.textContent = 'Saving your score...';
    fetch('/api/games/attempt', {
        method:'POST',
        headers:headers,
        body: JSON.stringify({
            gameCode: state.slug.toUpperCase().replace(/-/g, '_'),
            score: state.correct,
            accuracy: score,
            difficultyLevel: GAME_LEVEL,
            timeSpentSeconds: Math.max(1, Math.round((Date.now() - state.startedAt) / 1000)),
            confusedLetters: state.slug === 'letter-detective' ? 'b-d, p-q' : null
        })
    }).then(function(response){
        if (!response.ok) throw new Error('The score could not be saved. Please try again later.');
        if (status) status.textContent = 'Score saved to your learning record.';
    }).catch(function(e){
        console.warn('Could not save game attempt:', e);
        if (status) status.textContent = 'Score could not be saved. Please try again later.';
    });
}

/* â”€â”€ Render helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
function gc(){ return $('game-container'); }
function setContainer(html){ gc().innerHTML = html; }

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 1 : CONFUSION MATRIX  (b/d/p/q)
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
var cmData = {
    rounds: [
        {show:'b', opts:['b','d','p','q'], correct:0},
        {show:'d', opts:['b','d','p','q'], correct:1},
        {show:'p', opts:['b','d','p','q'], correct:2},
        {show:'q', opts:['b','d','p','q'], correct:3},
        {show:'b', opts:['d','b','q','p'], correct:1},
        {show:'d', opts:['p','q','d','b'], correct:2},
        {show:'p', opts:['q','p','b','d'], correct:1},
        {show:'q', opts:['b','p','q','d'], correct:2},
        {show:'b', opts:['q','d','b','p'], correct:2},
        {show:'d', opts:['d','b','p','q'], correct:0}
    ]
};
function confusionMatrix(){
    setTitle('Confusion Matrix');
    setQTotal(cmData.rounds.length);
    renderCM();
}
function renderCM(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    var round = cmData.rounds[state.qIndex];
    var opts = round.opts.map(function(o, i){
        return '<button class="btn-option" style="font-size:2rem;min-width:80px;margin:.4rem" onclick="checkCM('+i+','+round.correct+',this)">'+o+'</button>';
    }).join('');
    setContainer('<p class="game-prompt">Tap the letter you see below:</p>'
        +'<div style="font-size:5rem;font-weight:800;text-align:center;margin:1.5rem 0;font-family:\'OpenDyslexic\',monospace;color:var(--accent-navy)">'+round.show+'</div>'
        +'<div style="text-align:center">'+opts+'</div>');
    updateBoard();
    startTimer(8, function(){ checkCM(-1, round.correct, null); });
}
function checkCM(chosen, correct, btn){
    stopTimer();
    if(btn){
        btn.classList.add(chosen === correct ? 'correct' : 'wrong');
        if(chosen !== correct){
            var allBtns = gc().querySelectorAll('.btn-option');
            allBtns[correct].classList.add('correct');
        }
    }
    if(chosen === correct) state.correct++; else state.wrong++;
    state.qIndex++;
    updateBoard();
    setTimeout(renderCM, 900);
}

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 2 : HEAR IT â€“ SPELL IT
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
function speakWord(word){
    if(!window.speechSynthesis) return;
    var u = new SpeechSynthesisUtterance(word);
    u.rate = 0.8; u.lang = 'en-US';
    speechSynthesis.cancel();
    speechSynthesis.speak(u);
}
var wdWords = [
    {display:'sunset', correct:2},
    {display:'rabbit', correct:2},
    {display:'banana', correct:3},
    {display:'elephant', correct:3},
    {display:'computer', correct:3},
    {display:'butterfly', correct:3},
    {display:'fantastic', correct:3},
    {display:'dinosaur', correct:3},
    {display:'helicopter', correct:4},
    {display:'caterpillar', correct:4},
    {display:'watermelon', correct:4},
    {display:'celebration', correct:4}
];
function wordDecode(){
    setTitle('Word Deconstruction');
    wdQueue = wdWords.slice().sort(function(){ return Math.random()-.5; });
    setQTotal(wdQueue.length);
    renderWD();
}
function renderWD(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    var item = wdQueue[state.qIndex];
    var options = [1,2,3,4,5].map(function(n){
        var sel = n === item.correct ? '' : '';
        return '<button class="btn-option" style="font-size:1.3rem;min-width:60px;margin:.4rem" onclick="checkWD('+n+','+item.correct+',this)">'+n+'</button>';
    }).join('');
    setContainer(
        '<p class="game-prompt">How many <strong>syllables</strong> does this word have?</p>'
        +'<div class="word-display">'+item.display+'</div>'
        +'<div class="syllable-hint">Tap the syllable count:</div>'
        +'<div style="text-align:center">'+options+'</div>'
        +'<div id="wd-ans" style="text-align:center;margin-top:.8rem;font-weight:700;min-height:1.5rem;font-size:.95rem;color:var(--accent-lavender)"></div>'
    );
    updateBoard();
    startTimer(10, function(){ checkWD(-1, item.correct, null); });
}
function checkWD(chosen, correct, btn){
    stopTimer();
    var ansEl = $('wd-ans');
    if(btn){ btn.classList.add(chosen === correct ? 'correct' : 'wrong'); }
    if(chosen !== correct && ansEl){
        var allBtns = gc().querySelectorAll('.btn-option');
        allBtns.forEach(function(b){ if(parseInt(b.textContent) === correct) b.classList.add('correct'); });
        ansEl.textContent = 'Correct answer: ' + correct;
    }
    if(chosen === correct) state.correct++; else state.wrong++;
    state.qIndex++;
    updateBoard();
    setTimeout(renderWD, 1100);
}

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 4 : WORD PARTS QUEST
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */


/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 5 : FLUENCY BUILDER  (timed word highlight)
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
var fbSentences = [
    'The quick brown fox jumps over the lazy dog.',
    'She sells seashells by the seashore every summer.',
    'Peter Piper picked a peck of pickled peppers.',
    'The cat sat on the mat and ate a fat rat.',
    'Jack and Jill ran up the tall green hill.',
    'I saw a big red bus drive down the wet road.',
    'Every morning the birds sing sweet songs in the tree.',
    'The small puppy likes to play in the warm sunshine.',
    'My sister helped me read the story about the brave knight.',
    'The stars shine bright on a cold and clear winter night.'
];
var fbQueue, fbWI, fbWordInterval;
function fluencyBuilder(){
    setTitle('Fluency Builder');
    fbQueue = fbSentences.slice().sort(function(){ return Math.random()-.5; }).slice(0,10);
    setQTotal(fbQueue.length);
    renderFB();
}
function renderFB(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    var sentence = fbQueue[state.qIndex];
    var words = sentence.split(' ');
    var spans = words.map(function(w,i){
        return '<span class="fluency-word" id="fw'+i+'">'+w+'</span>';
    }).join(' ');
    setContainer(
        '<p class="game-prompt">Read along as each word is highlighted. Press <strong>Done</strong> when finished!</p>'
        +'<div class="fluency-sentence" style="margin:1.5rem auto;max-width:600px;">'+spans+'</div>'
        +'<div style="text-align:center;margin-top:1.2rem">'
        +'  <button id="fb-btn" onclick="startFBHighlight('+words.length+')" style="background:linear-gradient(135deg,#1a2a6c,#5c5f9e);color:#fff;border:none;border-radius:12px;padding:.65rem 1.8rem;font-size:.95rem;font-weight:700;cursor:pointer">Start Reading</button>'
        +'</div>'
    );
    updateBoard();
    document.getElementById("timer-display").textContent = "--";
    $('timer-bar').style.width = '100%';
}
function startFBHighlight(wordCount){
    clearInterval(fbWordInterval);
    fbWI = 0;
    $('fb-btn').disabled = true;
    fbWordInterval = setInterval(function(){
        if(fbWI > 0) $('fw'+(fbWI-1)).classList.remove('active');
        if(fbWI < wordCount){
            $('fw'+fbWI).classList.add('active');
            fbWI++;
        } else {
            clearInterval(fbWordInterval);
            // after last word, show done button
            document.getElementById("fb-btn").textContent = "Done";
            $('fb-btn').disabled = false;
            $('fb-btn').onclick = function(){ doneFB(); };
        }
    }, 500);
}
function doneFB(){
    clearInterval(fbWordInterval);
    state.correct++;
    state.qIndex++;
    updateBoard();
    setTimeout(renderFB, 500);
}

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 6 : ERROR HUNTER  (find 5 spelling mistakes)
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
var ehPassages = [
    {
        words: 'The littel cat sat on the woarm mat. She wached the birds fly ovar the garden.',
        errors: ['littel','woarm','wached','ovar'],  // indices of misspelled words
        corrections: {'littel':'little','woarm':'warm','wached':'watched','ovar':'over'}
    },
    {
        words: 'My frend and I wlaked to the park. We plaied on the swings untill it was dark.',
        errors: ['frend','wlaked','plaied','untill'],
        corrections: {'frend':'friend','wlaked':'walked','plaied':'played','untill':'until'}
    },
    {
        words: 'She opend the beutiful box and fond a sparkling ring insid the soft cloth.',
        errors: ['opend','beutiful','fond','insid'],
        corrections: {'opend':'opened','beutiful':'beautiful','fond':'found','insid':'inside'}
    }
];
var ehCurrent, ehFound, ehTotal;
function errorHunter(){
    setTitle('Error Hunter');
    state.qTotal = ehPassages.length;
    $('q-total').textContent = ehPassages.length;
    setQTotal(ehPassages.length);
    renderEH();
}
function renderEH(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    ehCurrent = ehPassages[state.qIndex];
    ehFound   = 0;
    ehTotal   = ehCurrent.errors.length;
    var wordList = ehCurrent.words.split(/(\s+)/);
    var html = wordList.map(function(tok){
        if(/\s+/.test(tok)) return tok;
        var clean = tok.replace(/[.,!?]/g,'');
        if(ehCurrent.errors.includes(clean)){
            return '<span class="err-word" data-word="'+clean+'" onclick="clickErr(this)">'+tok+'</span>';
        }
        return '<span class="err-word-ok" style="cursor:default">'+tok+'</span>';
    }).join('');
    setContainer(
        '<p class="game-prompt">Click every <strong>misspelled word</strong> you can find! ('+ehTotal+' errors hidden)</p>'
        +'<div class="passage-text" style="margin:1rem 0;padding:1rem;background:#f5f7ff;border-radius:12px">'+html+'</div>'
        +'<div id="eh-score" style="font-size:.9rem;color:var(--text-secondary);margin-bottom:.5rem">Found: 0 / '+ehTotal+'</div>'
        +'<button onclick="finishEH()" style="background:linear-gradient(135deg,#1a2a6c,#5c5f9e);color:#fff;border:none;border-radius:12px;padding:.65rem 1.6rem;font-size:.9rem;font-weight:700;cursor:pointer">Submit Findings</button>'
    );
    updateBoard();
    startTimer(40, finishEH);
}
function clickErr(el){
    if(el.classList.contains('found')) return;
    el.classList.add('found');
    var word = el.getAttribute('data-word');
    var fix  = ehCurrent.corrections[word] || word;
    el.title = 'Correction: ' + fix;
    ehFound++;
    $('eh-score').textContent = 'Found: ' + ehFound + ' / ' + ehTotal;
}
function finishEH(){
    stopTimer();
    // Reveal any missed errors
    gc().querySelectorAll('.err-word:not(.found)').forEach(function(el){ el.classList.add('missed'); });
    if(ehFound >= ehTotal){ state.correct++; } else { state.wrong++; }
    state.qIndex++;
    updateBoard();
    setTimeout(renderEH, 1800);
}

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 7 : SENTENCE SEQUENCING  (drag-drop word order)
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
var ssData = [
    {words:['The','cat','sat','on','the','mat'], answer:'The cat sat on the mat'},
    {words:['Birds','fly','high','in','the','sky'], answer:'Birds fly high in the sky'},
    {words:['She','loves','to','read','books'], answer:'She loves to read books'},
    {words:['The','dog','runs','fast'], answer:'The dog runs fast'},
    {words:['He','ate','a','big','apple'], answer:'He ate a big apple'},
    {words:['We','play','in','the','park'], answer:'We play in the park'},
    {words:['The','sun','shines','bright'], answer:'The sun shines bright'},
    {words:['My','friend','is','very','kind'], answer:'My friend is very kind'},
    {words:['They','swam','in','the','pool'], answer:'They swam in the pool'},
    {words:['I','like','to','eat','cake'], answer:'I like to eat cake'}
];
var ssQueue, ssDragging, ssOrderArr;
function sentenceSequence(){
    setTitle('Sentence Sequencing');
    ssQueue = ssData.slice().sort(function(){ return Math.random()-.5; });
    setQTotal(ssQueue.length);
    renderSS();
}
function renderSS(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    var item = ssQueue[state.qIndex];
    // Shuffle the words
    var shuffled = item.words.slice().sort(function(){ return Math.random()-.5; });
    ssOrderArr = [];
    var tilesHtml = shuffled.map(function(w,i){
        return '<div class="word-tile" draggable="true" data-word="'+w+'" '
            +'ondragstart="ssDragStart(this)" ondragend="ssDragEnd(this)">'+w+'</div>';
    }).join('');
    setContainer(
        '<p class="game-prompt">Drag the word tiles into the correct order to form a sentence:</p>'
        +'<div style="margin-bottom:1rem"><strong>Word tiles:</strong>'
        +'<div class="tiles-area" id="ss-source">'+tilesHtml+'</div></div>'
        +'<div><strong>Your sentence:</strong>'
        +'<div class="drop-zone" id="ss-target" ondrop="ssDrop(this)" ondragover="ssDragOver(this)" ondragleave="ssDragLeave(this)">'+
        '<span style="color:#aaa;font-style:italic;font-size:.85rem">Drag words here...</span></div></div>'
        +'<div style="margin-top:1rem">'
        +'<button onclick="checkSS(\''+item.answer+'\')" style="background:linear-gradient(135deg,#1a2a6c,#5c5f9e);color:#fff;border:none;border-radius:12px;padding:.65rem 1.6rem;font-size:.9rem;font-weight:700;cursor:pointer">Check</button>'
        +'</div>'
        +'<div id="ss-fb" style="margin-top:.7rem;font-weight:700;font-size:.95rem;min-height:1.5rem"></div>'
    );
    updateBoard();
    startTimer(25, function(){ checkSS(item.answer); });
}
function ssDragStart(el){ ssDragging = el; el.classList.add('dragging'); }
function ssDragEnd(el){ el.classList.remove('dragging'); }
function ssDragOver(zone){ zone.classList.add('dragover'); event.preventDefault(); }
function ssDragLeave(zone){ zone.classList.remove('dragover'); }
function ssDrop(zone){
    zone.classList.remove('dragover');
    event.preventDefault();
    if(!ssDragging) return;
    // Remove placeholder text if any
    var placeholder = zone.querySelector('span[style]');
    if(placeholder) placeholder.remove();
    zone.appendChild(ssDragging);
}
function checkSS(answer){
    stopTimer();
    var target = $('ss-target');
    var tiles = target.querySelectorAll('.word-tile');
    var sentence = Array.from(tiles).map(function(t){ return t.getAttribute('data-word'); }).join(' ');
    var correct = sentence.trim() === answer.trim();
    var fb = $('ss-fb');
    if(correct){ state.correct++; if(fb){ fb.textContent='Correct!'; fb.style.color='#2e7d32'; } }
    else        { state.wrong++;   if(fb){ fb.textContent='Not quite. Correct order: '+answer; fb.style.color='#c62828'; } }
    state.qIndex++;
    updateBoard();
    setTimeout(renderSS, 1300);
}

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
   GAME 8 : PARAGRAPH NAVIGATOR  (match paragraph â†’ topic sentence)
â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
var pnData = [
    {
        paragraph: 'Dogs make wonderful pets. They are loyal, friendly, and love to play. Many families enjoy having a dog as a companion.',
        topics:    ['Dogs are great companions.', 'Cats are independent animals.', 'Fish need special tanks.', 'Birds can sing beautifully.'],
        correct:   0
    },
    {
        paragraph: 'The sun is a star at the center of our solar system. It gives us light and heat every day. Without the sun, life on Earth would not be possible.',
        topics:    ['Planets orbit around stars.', 'The sun is essential for life on Earth.', 'The moon reflects sunlight.', 'Stars are very far away.'],
        correct:   1
    },
    {
        paragraph: 'Reading every day helps your brain grow stronger. It improves vocabulary and concentration. Good readers often become good writers too.',
        topics:    ['Writing is important in school.', 'Dictionaries help with spelling.', 'Daily reading improves your mind.', 'Libraries have many books.'],
        correct:   2
    },
    {
        paragraph: 'Exercise keeps your body healthy and strong. It makes your heart work better and helps you feel happier. Even a short walk every day is good for you.',
        topics:    ['Sports require special equipment.', 'Eating vegetables is important.', 'Swimming is a popular sport.', 'Exercise is good for your health.'],
        correct:   3
    },
    {
        paragraph: 'Water covers about 70 percent of Earth\'s surface. Oceans, rivers, and lakes are all made of water. Every living thing needs water to survive.',
        topics:    ['Water is essential for all living things.', 'Fish live in the sea.', 'Rain falls from clouds.', 'Deserts are very dry places.'],
        correct:   0
    },
    {
        paragraph: 'Fruits and vegetables give your body important vitamins. Eating a variety of colours ensures good health. Nutritionists recommend five servings every day.',
        topics:    ['Cooking makes food taste better.', 'Eating fruits and vegetables is healthy.', 'Sugar is found in many foods.', 'Breakfast is the first meal of the day.'],
        correct:   1
    },
    {
        paragraph: 'Music can change how we feel in an instant. A happy tune can lift our mood, while a slow melody can calm us down. Many people listen to music when they study or exercise.',
        topics:    ['Guitars have six strings.', 'Singing requires practise.', 'Music has a powerful effect on emotions.', 'Concerts attract large crowds.'],
        correct:   2
    },
    {
        paragraph: 'Trees produce the oxygen we breathe and help clean the air. They provide homes for birds and animals. Planting trees helps fight climate change.',
        topics:    ['Forests cover large areas of land.', 'Autumn leaves change colour.', 'Paper is made from wood pulp.', 'Trees are vital for our environment.'],
        correct:   3
    },
    {
        paragraph: 'Friendship is very important in life. Good friends listen to each other and offer help when needed. True friends make us feel valued and understood.',
        topics:    ['Good friends support each other.', 'School can be a busy place.', 'Families go on holiday together.', 'Pets can be loyal companions.'],
        correct:   0
    },
    {
        paragraph: 'Recycling helps reduce waste and protect the planet. When we reuse materials like paper, glass, and plastic, fewer trees are cut down and less energy is used.',
        topics:    ['Rubbish bins come in different sizes.', 'Recycling is good for the environment.', 'Factories produce many products.', 'Plastic bottles are very common.'],
        correct:   1
    }
];
var pnQueue;
function paragraphNavigator(){
    setTitle('Paragraph Navigator');
    pnQueue = pnData.slice().sort(function(){ return Math.random()-.5; }).slice(0,10);
    setQTotal(pnQueue.length);
    renderPN();
}
function renderPN(){
    if(state.qIndex >= state.qTotal){ showResult(); return; }
    var item = pnQueue[state.qIndex];
    var optionsHtml = item.topics.map(function(t, i){
        return '<button class="btn-option" style="display:block;width:100%;text-align:left;margin-bottom:.5rem" onclick="checkPN('+i+','+item.correct+',this)">'+t+'</button>';
    }).join('');
    setContainer(
        '<p class="game-prompt">Choose the best <strong>topic sentence</strong> for this paragraph:</p>'
        +'<div class="para-card" style="cursor:default;margin-bottom:1.5rem">'+item.paragraph+'</div>'
        +'<div id="pn-opts">'+optionsHtml+'</div>'
        +'<div id="pn-fb" style="margin-top:.7rem;font-weight:700;font-size:.95rem;min-height:1.5rem"></div>'
    );
    updateBoard();
    startTimer(15, function(){ checkPN(-1, item.correct, null); });
}
function checkPN(chosen, correct, btn){
    stopTimer();
    var opts = gc().querySelectorAll('.btn-option');
    opts.forEach(function(b){ b.disabled = true; });
    if(btn){ btn.classList.add(chosen === correct ? 'correct' : 'wrong'); }
    opts[correct].classList.add('correct');
    var fb = $('pn-fb');
    if(chosen === correct){ state.correct++; if(fb){ fb.textContent='Correct!'; fb.style.color='#2e7d32'; } }
    else                  { state.wrong++;   if(fb){ fb.textContent='Not quite. The answer was: '+pnQueue[state.qIndex].topics[correct]; fb.style.color='#c62828'; } }
    state.qIndex++;
    updateBoard();
    setTimeout(renderPN, 1400);
}

/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
   ROUTER â€” pick game by GAME_SLUG
â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */
var GAME_MAP = {
    'confusion-matrix':   confusionMatrix,
    'word-decode':        wordDecode,
    'fluency-builder':    fluencyBuilder,
    'error-hunter':       errorHunter,
    'sentence-sequence':  sentenceSequence,
    'paragraph-navigator':paragraphNavigator
};

function resetState(){
    stopTimer();
    state.qIndex  = 0;
    state.correct = 0;
    state.wrong   = 0;
    state.finished= false;
    state.startedAt = Date.now();
    updateBoard();
    $('result-overlay').classList.remove('active');
}

function launchGame(slug){
    resetState();
    state.slug = slug;
    var fn = GAME_MAP[slug];
    if(fn){ fn(); }
    else  {
        setContainer('<p style="text-align:center;padding:3rem;color:var(--text-secondary)">Unknown game: '+slug+'</p>');
    }
}

/* Global restart â€” called by Play Again button */
LB_GAME.restart = function(){ launchGame(state.slug); };

/* Boot */
document.addEventListener('DOMContentLoaded', function(){
    launchGame(window.GAME_SLUG || 'confusion-matrix');
});



