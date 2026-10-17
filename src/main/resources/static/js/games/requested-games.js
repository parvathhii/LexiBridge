/* Four additional dyslexia-friendly games, loaded after games.js. */
(function () {
    var activeRoundScored = false;

    function finishRound(correct, feedbackId, renderNext) {
        if (activeRoundScored || state.finished) return;
        activeRoundScored = true;
        stopTimer();
        if (correct) state.correct++; else state.wrong++;
        var feedback = $(feedbackId);
        if (feedback) {
            feedback.textContent = correct ? 'That is right! Great work.' : 'Good try. Keep practicing!';
            feedback.style.color = correct ? '#2e7d32' : '#c62828';
        }
        gc().querySelectorAll('button, input').forEach(function (control) { control.disabled = true; });
        state.qIndex++;
        updateBoard();
        setTimeout(renderNext, 950);
    }

    var letterRounds = [
        {word:'_at', answer:'b', choices:['b','d'], hint:'b has a tall line before its round belly.'},
        {word:'_og', answer:'d', choices:['b','d'], hint:'d has its round belly first, then a tall line.'},
        {word:'_ig', answer:'p', choices:['p','q'], hint:'p has a tall line before its round part.'},
        {word:'_ueen', answer:'q', choices:['p','q'], hint:'q has a small tail that points down.'},
        {word:'_ell', answer:'b', choices:['b','d'], hint:'It begins with b, like bell.'},
        {word:'_uck', answer:'d', choices:['b','d'], hint:'It begins with d, like duck.'},
        {word:'_ark', answer:'p', choices:['p','q'], hint:'It begins with p, like park.'},
        {word:'_ig', answer:'p', choices:['p','q'], hint:'It begins with p, like pig.'}
    ];
    function letterDetective() { setTitle('Letter Detective'); setQTotal(letterRounds.length); renderLetter(); }
    function renderLetter() {
        if (state.qIndex >= state.qTotal) return showResult();
        activeRoundScored = false;
        var item = letterRounds[state.qIndex];
        setContainer('<p class="game-prompt">Choose the letter that completes this word.</p><div class="word-display" aria-live="polite">'+item.word+'</div>'
            +'<div style="text-align:center">'+item.choices.map(function (letter) { return '<button class="btn-option" type="button" style="font-size:2.2rem;min-width:90px" data-choice="'+letter+'">'+letter+'</button>'; }).join('')+'</div>'
            +'<div style="text-align:center;margin-top:1rem"><button class="btn-option" type="button" id="letter-hint">Show a hint</button></div><p id="letter-hint-text" class="game-prompt" aria-live="polite"></p><p id="letter-feedback" role="status" aria-live="polite"></p>');
        gc().querySelectorAll('[data-choice]').forEach(function (button) { button.addEventListener('click', function () { finishRound(button.dataset.choice === item.answer, 'letter-feedback', renderLetter); }); });
        $('letter-hint').addEventListener('click', function () { $('letter-hint-text').textContent = item.hint; });
        updateBoard(); startTimer(25, function () { finishRound(false, 'letter-feedback', renderLetter); });
    }

    var buildWords = [
        {word:'planet', letters:'t a n p e l', hint:'It begins with p and shines in the night sky.'},
        {word:'garden', letters:'d e g a r n', hint:'A place where flowers and vegetables grow.'},
        {word:'rabbit', letters:'b t r a i b', hint:'A furry animal with long ears.'},
        {word:'window', letters:'w d o n i w', hint:'You can look through it.'},
        {word:'pencil', letters:'c i l p e n', hint:'You write with it.'},
        {word:'basket', letters:'k a s t e b', hint:'You can carry things in it.'},
        {word:'sunset', letters:'t u s n e s', hint:'The sky colors at the end of the day.'},
        {word:'bridge', letters:'g r e b i d', hint:'A path that crosses a river or road.'}
    ];
    function wordBuilder() { setTitle('Word Builder'); setQTotal(buildWords.length); renderWord(); }
    function renderWord() {
        if (state.qIndex >= state.qTotal) return showResult();
        activeRoundScored = false;
        var item = buildWords[state.qIndex];
        setContainer('<p class="game-prompt">Put these letters in order to make a word.</p><div class="word-display" aria-label="Scrambled letters">'+item.letters+'</div>'
            +'<div style="text-align:center"><label for="build-answer" class="game-prompt">Your word</label><br><input id="build-answer" class="spell-input" autocomplete="off" autocapitalize="none" aria-describedby="build-hint-text build-feedback">'
            +'<div style="margin-top:1rem"><button class="btn-option" type="button" id="build-check">Check word</button> <button class="btn-option" type="button" id="build-hint">Show a hint</button></div></div>'
            +'<p id="build-hint-text" class="game-prompt" aria-live="polite"></p><p id="build-feedback" role="status" aria-live="polite"></p>');
        var submit = function () { finishRound($('build-answer').value.trim().toLowerCase() === item.word, 'build-feedback', renderWord); };
        $('build-check').addEventListener('click', submit);
        $('build-answer').addEventListener('keydown', function (event) { if (event.key === 'Enter') submit(); });
        $('build-hint').addEventListener('click', function () { $('build-hint-text').textContent = item.hint + ' First letter: ' + item.word.charAt(0).toUpperCase(); });
        $('build-answer').focus(); updateBoard(); startTimer(45, function () { finishRound(false, 'build-feedback', renderWord); });
    }

    var syllableChallenges = [
        {word:'fantastic', parts:['fan','tas','tic'], mixed:['tic','fan','tas'], clue:'A fun way to say something is very good.'},
        {word:'remember', parts:['re','mem','ber'], mixed:['ber','re','mem'], clue:'To keep something in your mind.'},
        {word:'computer', parts:['com','pu','ter'], mixed:['pu','ter','com'], clue:'A machine used for work, games, and learning.'},
        {word:'yesterday', parts:['yes','ter','day'], mixed:['day','yes','ter'], clue:'The day before today.'},
        {word:'wonderful', parts:['won','der','ful'], mixed:['ful','won','der'], clue:'Very good; full of wonder.'},
        {word:'adventure', parts:['ad','ven','ture'], mixed:['ven','ture','ad'], clue:'An exciting experience or journey.'},
        {word:'important', parts:['im','por','tant'], mixed:['tant','im','por'], clue:'Something that matters a lot.'},
        {word:'butterfly', parts:['but','ter','fly'], mixed:['fly','but','ter'], clue:'A colorful insect with large wings.'}
    ];
    function syllableSwitch() { setTitle('Syllable Switch'); setQTotal(syllableChallenges.length); renderSyllable(); }
    function renderSyllable() {
        if (state.qIndex >= state.qTotal) return showResult();
        activeRoundScored = false;
        var item = syllableChallenges[state.qIndex], selected = [];
        setContainer('<p class="game-prompt">Put the syllable chunks in order to build the word that matches the clue.</p>'
            +'<p class="game-prompt"><strong>Clue:</strong> '+item.clue+'</p>'
            +'<p class="game-prompt">Choose '+item.parts.length+' chunks in the right order:</p>'
            +'<div id="syllable-answer" class="drop-zone" aria-label="Your word" aria-live="polite"></div>'
            +'<div id="syllable-tiles" class="tiles-area" aria-label="Mixed-up syllable chunks">'+item.mixed.map(function (part, index) { return '<button class="word-tile" type="button" data-part-index="'+index+'">'+part+'</button>'; }).join('')+'</div>'
            +'<div style="text-align:center;margin-top:1rem"><button class="btn-option" type="button" id="syllable-check">Check word</button> <button class="btn-option" type="button" id="syllable-hint">Show a hint</button> <button class="btn-option" type="button" id="syllable-clear">Start over</button></div>'
            +'<p id="syllable-hint-text" class="game-prompt" aria-live="polite"></p><p id="syllable-feedback" role="status" aria-live="polite"></p>');
        function drawAnswer() {
            $('syllable-answer').innerHTML = selected.map(function (index, position) { return '<button type="button" class="word-tile" data-remove-syllable="'+position+'">'+item.mixed[index]+' (remove)</button>'; }).join('');
            $('syllable-answer').querySelectorAll('[data-remove-syllable]').forEach(function (button) { button.addEventListener('click', function () { selected.splice(Number(button.dataset.removeSyllable), 1); drawAnswer(); }); });
            gc().querySelectorAll('#syllable-tiles [data-part-index]').forEach(function (button) { button.disabled = selected.indexOf(Number(button.dataset.partIndex)) !== -1; });
        }
        gc().querySelectorAll('#syllable-tiles [data-part-index]').forEach(function (button) { button.addEventListener('click', function () { selected.push(Number(button.dataset.partIndex)); drawAnswer(); }); });
        $('syllable-clear').addEventListener('click', function () { selected = []; drawAnswer(); });
        $('syllable-hint').addEventListener('click', function () { $('syllable-hint-text').textContent = 'This word has '+item.parts.length+' syllables. It starts with “'+item.parts[0]+'”.'; });
        $('syllable-check').addEventListener('click', function () {
            var answer = selected.map(function (index) { return item.mixed[index]; });
            finishRound(answer.join('') === item.word, 'syllable-feedback', renderSyllable);
        });
        updateBoard(); startTimer(55, function () { finishRound(false, 'syllable-feedback', renderSyllable); });
    }

    var wordPartRounds = [
        {root:'play', clue:'To play again', answer:'replay', choices:['playful','replay','player'], hint:'The prefix re- means again.'},
        {root:'fair', clue:'Not fair', answer:'unfair', choices:['fairly','unfair','fairness'], hint:'The prefix un- can mean not.'},
        {root:'kind', clue:'Without kindness', answer:'unkind', choices:['kindly','unkind','kindness'], hint:'Add un- to mean not kind.'},
        {root:'help', clue:'A person who helps', answer:'helper', choices:['helpful','helper','helpless'], hint:'The suffix -er can mean a person who does something.'},
        {root:'care', clue:'Without care', answer:'careless', choices:['careful','caring','careless'], hint:'The suffix -less means without.'},
        {root:'hope', clue:'Full of hope', answer:'hopeful', choices:['hoping','hopeful','hopeless'], hint:'The suffix -ful means full of.'},
        {root:'agree', clue:'To have a different opinion', answer:'disagree', choices:['agreement','disagree','agreeable'], hint:'The prefix dis- can mean opposite or not.'},
        {root:'use', clue:'Able to be used again', answer:'reusable', choices:['useful','reuse','reusable'], hint:'Re- means again, and -able means can be.'}
    ];
    function wordPartsQuest() { setTitle('Word Parts Quest'); setQTotal(wordPartRounds.length); renderWordParts(); }
    function renderWordParts() {
        if (state.qIndex >= state.qTotal) return showResult();
        activeRoundScored = false;
        var item = wordPartRounds[state.qIndex];
        setContainer('<p class="game-prompt">Use the word parts to solve the clue. Listen or ask for a hint if you need help.</p>'
            +'<div class="word-display" aria-label="Root word">'+item.root+'</div>'
            +'<p class="game-prompt"><strong>Clue:</strong> '+item.clue+'</p>'
            +'<div style="text-align:center"><button class="btn-option" type="button" id="parts-listen">Listen to the clue</button></div>'
            +'<div style="text-align:center;margin-top:.7rem">'+item.choices.map(function (word) { return '<button class="btn-option" type="button" style="font-size:1.2rem;min-width:125px" data-choice="'+word+'">'+word+'</button>'; }).join('')+'</div>'
            +'<div style="text-align:center;margin-top:.6rem"><button class="btn-option" type="button" id="parts-hint">Show a hint</button></div>'
            +'<p id="parts-hint-text" class="game-prompt" aria-live="polite"></p><p id="parts-feedback" role="status" aria-live="polite"></p>');
        gc().querySelectorAll('[data-choice]').forEach(function (button) { button.addEventListener('click', function () { finishRound(button.dataset.choice === item.answer, 'parts-feedback', renderWordParts); }); });
        $('parts-listen').addEventListener('click', function () { speakWord(item.root + '. ' + item.clue); });
        $('parts-hint').addEventListener('click', function () { $('parts-hint-text').textContent = item.hint; });
        updateBoard(); startTimer(55, function () { finishRound(false, 'parts-feedback', renderWordParts); });
    }

    GAME_MAP['letter-detective'] = letterDetective;
    GAME_MAP['word-builder'] = wordBuilder;
    GAME_MAP['syllable-switch'] = syllableSwitch;
    var deconstructionWords = [
        {word:'sunset', syllables:2}, {word:'rabbit', syllables:2},
        {word:'banana', syllables:3}, {word:'elephant', syllables:3},
        {word:'computer', syllables:3}, {word:'butterfly', syllables:3},
        {word:'fantastic', syllables:3}, {word:'dinosaur', syllables:3},
        {word:'helicopter', syllables:4}, {word:'caterpillar', syllables:4},
        {word:'watermelon', syllables:4}, {word:'celebration', syllables:4}
    ];
    function wordDeconstruction() {
        setTitle('Word Deconstruction');
        setQTotal(deconstructionWords.length);
        renderDeconstruction();
    }
    function renderDeconstruction() {
        if (state.qIndex >= state.qTotal) return showResult();
        activeRoundScored = false;
        var item = deconstructionWords[state.qIndex];
        var choices = [1, 2, 3, 4, 5].map(function (count) {
            return '<button class="btn-option" type="button" data-syllable-count="'+count+'">'+count+'</button>';
        }).join('');
        setContainer('<p class="game-prompt">Read the word and choose how many syllables you hear.</p>'
            +'<div class="word-display" aria-live="polite">'+item.word+'</div>'
            +'<p class="game-prompt">Tap the syllable count:</p>'
            +'<div style="text-align:center">'+choices+'</div>'
            +'<p id="deconstruction-feedback" role="status" aria-live="polite"></p>');
        gc().querySelectorAll('[data-syllable-count]').forEach(function (button) {
            button.addEventListener('click', function () {
                finishRound(Number(button.dataset.syllableCount) === item.syllables, 'deconstruction-feedback', renderDeconstruction);
            });
        });
        updateBoard();
        startTimer(20, function () { finishRound(false, 'deconstruction-feedback', renderDeconstruction); });
    }
    GAME_MAP['word-parts-quest'] = wordPartsQuest;
    GAME_MAP['word-decode'] = wordDeconstruction;
})();
