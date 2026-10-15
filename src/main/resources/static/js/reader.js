/**
 * LexiBridge – Smart Reader Engine
 * Powers dyslexia typography, reading ruler, text-to-speech assistant, and session analytics.
 */

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const body = document.getElementById('readerBody');
    const paper = document.getElementById('readerPaper');
    const textContent = document.getElementById('readerTextContent');
    const ruler = document.getElementById('readingRuler');
    const progressBar = document.getElementById('scrollProgressBar');
    const toolbar = document.getElementById('readerToolbar');
    const btnToggleToolbar = document.getElementById('btnToggleToolbar');

    // Controls
    const fontSelect = document.getElementById('fontSelect');
    const btnFontDec = document.getElementById('btnFontDec');
    const btnFontInc = document.getElementById('btnFontInc');
    const fontSizeDisplay = document.getElementById('fontSizeDisplay');
    const btnLineDec = document.getElementById('btnLineDec');
    const btnLineInc = document.getElementById('btnLineInc');
    const lineSpacingDisplay = document.getElementById('lineSpacingDisplay');
    const letterSpacingSelect = document.getElementById('letterSpacingSelect');
    const btnToggleRuler = document.getElementById('btnToggleRuler');
    const themeDots = document.querySelectorAll('.theme-dot');

    // TTS Controls
    const btnTtsPlay = document.getElementById('btnTtsPlay');
    const btnTtsPause = document.getElementById('btnTtsPause');
    const btnTtsStop = document.getElementById('btnTtsStop');
    const ttsRateInput = document.getElementById('ttsRate');
    const ttsRateDisplay = document.getElementById('ttsRateDisplay');
    const ttsVoiceSelect = document.getElementById('ttsVoiceSelect');

    // Timer & Metrics
    const timerDisplay = document.getElementById('readingTimer');
    const wpmDisplay = document.getElementById('readingWpm');
    const btnCompleteReading = document.getElementById('btnCompleteReading');
    const celebrationModal = document.getElementById('celebrationModal');
    const btnStartSpeechTrack = document.getElementById('btnStartSpeechTrack');
    const speechTrackStatus = document.getElementById('speechTrackStatus');

    // Metadata
    const materialId = textContent ? textContent.getAttribute('data-material-id') : null;
    const totalWords = textContent ? parseInt(textContent.getAttribute('data-word-count') || '100', 10) : 100;

    // State
    let currentFontSize = textContent ? (parseInt(textContent.dataset.fontSize, 10) || 20) : 20;
    let currentLineSpacing = textContent ? (parseFloat(textContent.dataset.lineSpacing) || 1.8) : 1.8;
    let rulerActive = textContent ? textContent.dataset.ruler === 'true' : false;
    const preferredTextColor = textContent ? (textContent.dataset.textColor || '#26364a') : '#26364a';
    let readingSeconds = 0;
    let timerInterval = null;
    let ttsUsed = false;
    let voices = [];
    let currentUtterance = null;
    let isTtsPlaying = false;
    let isTtsPaused = false;
    let recognizedReading = '';
    let speechRecognition = null;
    let isListeningToReading = false;
    let completeWhenRecognitionEnds = false;
    let speechTrackingRequested = false;
    let speechTrackStartedOnce = false;

    // 1. Initial Styles Setup
    function applyTypography() {
        if (!textContent) return;
        textContent.style.fontSize = `${currentFontSize}px`;
        textContent.style.lineHeight = `${currentLineSpacing}`;
        textContent.style.letterSpacing = textContent.dataset.letterSpacing || '0.06em';
        const darkTheme = body && body.classList.contains('theme-dark');
        textContent.style.color = darkTheme ? '#f1f5f9' : (preferredTextColor === '#f1f5f9' ? '#26364a' : preferredTextColor);
        fontSizeDisplay.textContent = `${currentFontSize}px`;
        lineSpacingDisplay.textContent = `${currentLineSpacing.toFixed(1)}x`;
    }
    applyTypography();

    function applyFont(font) {
        if (!textContent) return;
        const families = {
            'OpenDyslexic': "'OpenDyslexic', 'Lexend', sans-serif",
            'Lexend': "'Lexend', sans-serif",
            'Comic Neue': "'Comic Neue', cursive",
            'Arial': 'Arial, sans-serif'
        };
        const value = font || textContent.dataset.font || 'OpenDyslexic';
        const family = families[value] || value;
        textContent.style.fontFamily = family;
        textContent.dataset.font = value;
    }
    applyFont(fontSelect ? fontSelect.value : null);

    // 2. Toolbar Toggle
    if (btnToggleToolbar && toolbar) {
        btnToggleToolbar.addEventListener('click', () => {
            toolbar.classList.toggle('open');
        });
    }

    // 3. Font Switching
    if (fontSelect && textContent) {
        fontSelect.addEventListener('change', (e) => {
            applyFont(e.target.value);
        });
    }

    // 4. Font Size Stepper
    if (btnFontDec) {
        btnFontDec.addEventListener('click', () => {
            if (currentFontSize > 14) {
                currentFontSize -= 2;
                applyTypography();
            }
        });
    }
    if (btnFontInc) {
        btnFontInc.addEventListener('click', () => {
            if (currentFontSize < 34) {
                currentFontSize += 2;
                applyTypography();
            }
        });
    }

    // 5. Line Spacing Stepper
    if (btnLineDec) {
        btnLineDec.addEventListener('click', () => {
            if (currentLineSpacing > 1.4) {
                currentLineSpacing = Math.round((currentLineSpacing - 0.2) * 10) / 10;
                applyTypography();
            }
        });
    }
    if (btnLineInc) {
        btnLineInc.addEventListener('click', () => {
            if (currentLineSpacing < 2.6) {
                currentLineSpacing = Math.round((currentLineSpacing + 0.2) * 10) / 10;
                applyTypography();
            }
        });
    }

    // 6. Letter Spacing
    if (letterSpacingSelect && textContent) {
        letterSpacingSelect.value = textContent.dataset.letterSpacing || '0.06em';
        letterSpacingSelect.addEventListener('change', (e) => {
            textContent.style.letterSpacing = e.target.value;
            textContent.dataset.letterSpacing = e.target.value;
        });
    }

    // 7. Reading Ruler
    if (btnToggleRuler && ruler) {
        ruler.style.display = rulerActive ? 'block' : 'none';
        btnToggleRuler.textContent = rulerActive ? 'Guide: ON' : 'Guide: OFF';
        btnToggleRuler.classList.toggle('active', rulerActive);
        btnToggleRuler.addEventListener('click', () => {
            rulerActive = !rulerActive;
            ruler.style.display = rulerActive ? 'block' : 'none';
            btnToggleRuler.textContent = rulerActive ? 'Guide: ON' : 'Guide: OFF';
            btnToggleRuler.classList.toggle('active', rulerActive);
        });

        document.addEventListener('mousemove', (e) => {
            if (!rulerActive) return;
            const rulerHeight = 44;
            const topPos = e.clientY - (rulerHeight / 2);
            ruler.style.top = `${topPos}px`;
        });
    }

    // 8. Theme Palette
    themeDots.forEach(dot => {
        dot.addEventListener('click', () => {
            themeDots.forEach(d => d.classList.remove('active'));
            dot.classList.add('active');
            const themeClass = dot.getAttribute('data-theme');
            body.className = `reader-mode ${themeClass}`;
            applyTypography();
        });
    });

    // 9. Interactive Word Click-to-Speak
    const wordElements = document.querySelectorAll('.read-word');
    wordElements.forEach(wordEl => {
        wordEl.addEventListener('click', (e) => {
            e.stopPropagation();
            const textToSpeak = wordEl.textContent.trim().replace(/[.,!?;:"]/g, '');
            if (textToSpeak.length > 0) {
                speakWord(textToSpeak, wordEl);
            }
        });
    });

    function speakWord(word, element) {
        if (!('speechSynthesis' in window)) return;
        window.speechSynthesis.cancel();

        ttsUsed = true;
        element.classList.add('word-pronounced');
        setTimeout(() => element.classList.remove('word-pronounced'), 900);

        const utter = new SpeechSynthesisUtterance(word);
        utter.rate = parseFloat(ttsRateInput.value) || 0.85;

        const selectedVoiceIdx = ttsVoiceSelect ? ttsVoiceSelect.value : '';
        if (selectedVoiceIdx && voices[selectedVoiceIdx]) {
            utter.voice = voices[selectedVoiceIdx];
        }
        window.speechSynthesis.speak(utter);
    }

    // 10. Web Speech API (TTS Full Reader)
    function populateVoices() {
        if (!('speechSynthesis' in window) || !ttsVoiceSelect) return;
        voices = window.speechSynthesis.getVoices();
        ttsVoiceSelect.innerHTML = '<option value="">Browser Default Voice</option>';

        voices.forEach((voice, i) => {
            if (voice.lang.startsWith('en')) {
                const opt = document.createElement('option');
                opt.value = i;
                opt.textContent = `${voice.name} (${voice.lang})`;
                ttsVoiceSelect.appendChild(opt);
            }
        });
    }

    if ('speechSynthesis' in window) {
        populateVoices();
        window.speechSynthesis.onvoiceschanged = populateVoices;
    }

    if (ttsRateInput && ttsRateDisplay) {
        ttsRateInput.addEventListener('input', () => {
            ttsRateDisplay.textContent = `${ttsRateInput.value}x`;
            if (isTtsPlaying && currentUtterance) {
                // Rate will take effect on next phrase
            }
        });
    }

    if (btnTtsPlay && textContent) {
        btnTtsPlay.addEventListener('click', () => {
            if (!('speechSynthesis' in window)) {
                alert('Your browser does not support Web Speech Text-to-Speech.');
                return;
            }

            ttsUsed = true;

            if (isTtsPaused) {
                window.speechSynthesis.resume();
                isTtsPaused = false;
                isTtsPlaying = true;
                updateTtsButtons();
                return;
            }

            window.speechSynthesis.cancel();
            const fullStoryText = textContent.innerText;
            currentUtterance = new SpeechSynthesisUtterance(fullStoryText);
            currentUtterance.rate = parseFloat(ttsRateInput.value) || 0.85;

            const selectedVoiceIdx = ttsVoiceSelect ? ttsVoiceSelect.value : '';
            if (selectedVoiceIdx && voices[selectedVoiceIdx]) {
                currentUtterance.voice = voices[selectedVoiceIdx];
            }

            currentUtterance.onstart = () => {
                isTtsPlaying = true;
                isTtsPaused = false;
                updateTtsButtons();
            };

            currentUtterance.onend = () => {
                isTtsPlaying = false;
                isTtsPaused = false;
                updateTtsButtons();
            };

            currentUtterance.onerror = () => {
                isTtsPlaying = false;
                isTtsPaused = false;
                updateTtsButtons();
            };

            window.speechSynthesis.speak(currentUtterance);
        });
    }

    if (btnTtsPause) {
        btnTtsPause.addEventListener('click', () => {
            if (isTtsPlaying && !isTtsPaused) {
                window.speechSynthesis.pause();
                isTtsPaused = true;
                updateTtsButtons();
            }
        });
    }

    if (btnTtsStop) {
        btnTtsStop.addEventListener('click', () => {
            window.speechSynthesis.cancel();
            isTtsPlaying = false;
            isTtsPaused = false;
            updateTtsButtons();
        });
    }

    function updateTtsButtons() {
        if (!btnTtsPlay || !btnTtsPause || !btnTtsStop) return;
        if (isTtsPlaying && !isTtsPaused) {
            btnTtsPlay.style.display = 'none';
            btnTtsPause.style.display = 'inline-block';
            btnTtsStop.style.display = 'inline-block';
        } else if (isTtsPaused) {
            btnTtsPlay.style.display = 'inline-block';
            btnTtsPlay.textContent = '▶ Resume';
            btnTtsPause.style.display = 'none';
            btnTtsStop.style.display = 'inline-block';
        } else {
            btnTtsPlay.style.display = 'inline-block';
            btnTtsPlay.textContent = '▶ Read Aloud';
            btnTtsPause.style.display = 'none';
            btnTtsStop.style.display = 'none';
        }
    }

    // 11. Reading Timer & WPM Tracker
    timerInterval = setInterval(() => {
        readingSeconds++;
        const mins = Math.floor(readingSeconds / 60);
        const secs = readingSeconds % 60;
        const timeFormatted = `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;

        if (timerDisplay) {
            timerDisplay.textContent = timeFormatted;
        }

        if (wpmDisplay && readingSeconds >= 5) {
            const calculatedWpm = Math.round((totalWords / readingSeconds) * 60);
            wpmDisplay.textContent = `${calculatedWpm} WPM`;
        }
    }, 1000);

    // 12. Scroll Progress Bar
    window.addEventListener('scroll', () => {
        if (!progressBar) return;
        const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
        if (totalHeight > 0) {
            const progress = (window.scrollY / totalHeight) * 100;
            progressBar.style.width = `${Math.min(100, Math.max(0, progress))}%`;
        }
    });

    // 13. Complete Reading Session
    if (btnCompleteReading && celebrationModal) {
        function completeReadingSession() {
            btnCompleteReading.disabled = true;
            clearInterval(timerInterval);
            if ('speechSynthesis' in window) {
                window.speechSynthesis.cancel();
            }

            const finalMins = Math.max(0.1, readingSeconds / 60);
            const calculatedWpm = Math.round(totalWords / finalMins);

            const payload = {
                materialId: materialId ? parseInt(materialId, 10) : 1,
                durationSeconds: readingSeconds,
                wordsRead: totalWords,
                wordsPerMinute: calculatedWpm,
                ttsUsed: ttsUsed,
                completed: true,
                spokenText: recognizedReading.trim()
            };

            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            const headers = {'Content-Type': 'application/json'};
            if (csrfToken && csrfHeader) headers[csrfHeader.content] = csrfToken.content;

            // Submit session to backend
            fetch('/api/reading/session/complete', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(payload)
            })
            .then(res => res.json())
            .then(data => {
                console.log('Session saved:', data);
            })
            .catch(err => {
                console.warn('Session save failed:', err);
            });

            // Display celebration modal
            const mins = Math.floor(readingSeconds / 60);
            const secs = readingSeconds % 60;
            document.getElementById('modalTimeSpent').textContent = `${mins}m ${secs}s`;
            document.getElementById('modalWpm').textContent = calculatedWpm;
            celebrationModal.style.display = 'flex';
        }

        btnCompleteReading.addEventListener('click', () => {
            if (isListeningToReading && speechRecognition) {
                completeWhenRecognitionEnds = true;
                speechTrackingRequested = false;
                speechTrackStatus.textContent = 'Saving your last spoken words...';
                speechRecognition.stop();
                return;
            }
            completeReadingSession();
        });

        if (btnStartSpeechTrack) btnStartSpeechTrack.addEventListener('click', () => {
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            if (!SpeechRecognition) {
                speechTrackStatus.textContent = 'Speech recognition is not available in this browser. You can still finish your reading session.';
                return;
            }
            if (isListeningToReading) {
                speechTrackingRequested = false;
                speechRecognition.stop();
                return;
            }
            speechRecognition = new SpeechRecognition();
            speechRecognition.lang = 'en-US';
            speechRecognition.continuous = true;
            speechRecognition.interimResults = true;
            speechRecognition.onresult = event => {
                let interim = '';
                for (let i = event.resultIndex; i < event.results.length; i++) {
                    const text = event.results[i][0].transcript.trim();
                    if (event.results[i].isFinal) recognizedReading += `${text} `;
                    else interim += `${text} `;
                }
                const wordCount = recognizedReading.trim() ? recognizedReading.trim().split(/\s+/).length : 0;
                speechTrackStatus.textContent = `${wordCount} words captured${interim ? `; hearing: ${interim.trim()}` : ''}. Select the button again to stop.`;
            };
            speechRecognition.onerror = event => {
                isListeningToReading = false;
                speechTrackingRequested = false;
                btnStartSpeechTrack.textContent = 'Read aloud to track accuracy';
                speechTrackStatus.textContent = event.error === 'not-allowed' ? 'Microphone permission was denied. Allow it in your browser to track accuracy.' : 'We could not hear that reading. You can try again or finish without speech tracking.';
            };
            speechRecognition.onend = () => {
                isListeningToReading = false;
                btnStartSpeechTrack.textContent = 'Read aloud to track accuracy';
                if (speechTrackingRequested && !completeWhenRecognitionEnds) {
                    try {
                        speechRecognition.start();
                        isListeningToReading = true;
                        btnStartSpeechTrack.textContent = 'Stop speech tracking';
                        speechTrackStatus.textContent = 'Listening. Keep reading at your own pace.';
                        return;
                    } catch (_) { speechTrackingRequested = false; }
                }
                if (recognizedReading.trim()) speechTrackStatus.textContent = 'Speech captured. Finish the session to analyze accuracy and missed words.';
                if (completeWhenRecognitionEnds) {
                    completeWhenRecognitionEnds = false;
                    completeReadingSession();
                }
            };
            try {
                speechRecognition.start();
                isListeningToReading = true;
                speechTrackingRequested = true;
                if (!speechTrackStartedOnce) recognizedReading = '';
                speechTrackStartedOnce = true;
                btnStartSpeechTrack.textContent = 'Stop speech tracking';
                speechTrackStatus.textContent = 'Listening. Read the passage aloud at your own pace.';
            } catch (_) {
                speechTrackStatus.textContent = 'The microphone could not start. Please try again.';
            }
        });
    }
});
