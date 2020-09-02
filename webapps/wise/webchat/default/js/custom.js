(function($) {
    "use strict";

    const VOICE_INPUT_NORMAL_MSG = '按住，並簡短說出您的問題。';
    const VOICE_INPUT_ERROR_MSG = '聲音訊息過短，請按住並說出您的問題。';
    const MIN_VOICE_INPUT_SECONDS = 0.5;

    var mainApp = {
            siriWave: '',
            voiceInputTimer: '',
            elapsedSeconds: 0,
            recognizerRecording: false,
            recognizer: false,
            resetWaveform: function() {
                $('#waveform-container').html('');
                this.siriWave = new SiriWave({
                    container: document.getElementById('waveform-container'),
                    width: $('#waveform-container').width(),
                    height: 120,
                    frequency: 3,
                    speed: 0.04,
                    color: '#2296F3',
                });
            },
            main_func: function() {
                $('.skip-tips-btn').click(function(e) {
                    e.stopPropagation();
                    mainApp.closeTutorial();
                });
                $('.btn_help').click(function() {
                    $('#tutorial-overlay').addClass('active');
                    $('.tutorial-1').addClass('active');
                });
                $('.mobile-menu-btn').click(function() {
                    if ($('body').hasClass('mobile-menu-opened')) {
                        mainApp.closeMobileMenu();
                    } else {
                        mainApp.openMobileMenu();
                    }
                });
                $('.voice-input-panel-btn').click(function() {
                    if (typeof(webkitSpeechRecognition) === 'function') {
	                    if ($('body').hasClass('voice-input-opened')) {
	                        mainApp.closeVoiceInput();
	                    } else {
	                        mainApp.openVoiceInput();
	                    }
                    }
                });

                $('.voice-input-btn').pressAndHold({
                    holdTime: 500
                });
                this.resetWaveform();

                $('.voice-input-btn').on('start.pressAndHold', function(event) {
                    console.log("start");
                    $(this).addClass('active');
                    mainApp.startVoiceInput();
                });
                $('.voice-input-btn').on('end.pressAndHold', function(event) {
                    console.log("end");
                    $(this).removeClass('active');
                    mainApp.stopVoiceInput();
                });

                $('.chat-input-form .txt_input').focus(function() {
                    mainApp.closeMobileMenu();
                });
                $('.icon-block-container *, .text-link-container *').click(function() {
                    mainApp.closeMobileMenu();
                });
                $('.survey-link').click(function(e) {
                    e.preventDefault();
                    $('#survey-overlay').addClass('active');
                });
                $('.survey .close-btn, .survey input[type="submit"]').click(function(e) {
                    e.preventDefault();
                    $('#survey-overlay').removeClass('active');
                });
                $('#tutorial-overlay, .tutorial-1, .tutorial-2').click(function() {
                    if ($('.tutorial-1').hasClass('active')) {
                        $('.tutorial-1').removeClass('active');
                        $('.tutorial-2').addClass('active');

                        var top = $('.grid-icon-tip-highlight').offset().top;
                        $('.grid-icon-tip').css('top', top);
                    } else if ($('.tutorial-2').hasClass('active')) {
                        mainApp.closeTutorial();
                    }
                });

                $(document).on('click', '.swiper-control-button.next', function() {
                    mainApp.scrollToNext(this);
                });
                $(document).on('click', '.swiper-control-button.prev', function() {
                    mainApp.scrollToPrev(this);
                });

                $('.tab').click(function() {
                    $('.tab').removeClass('active');
                    $(this).addClass('active');
                    var tabIndex = $(this).index();
                    $('.tab-content').removeClass('active');
                    $('.tab-content:eq(' + tabIndex + ')').addClass('active');
                })

                $('.mobile-menu-overlay').click(function() {
                    mainApp.closeMobileMenu();
                });
                $('#voice-input-desktop-overlay, .voice-input-close-btn').click(function() {
                    mainApp.closeVoiceInput();
                })

                $('.slide-container').scroll(function(event) {
                    clearTimeout($.data(this, 'scrollTimer'));
                    $.data(this, 'scrollTimer', setTimeout(function() {
                        var $sections = $('section', event.target);
                        var $dots = $(event.target).siblings('.slide-indicator-container').find('.slide-indicator');
                        $dots.removeClass('active');
                        $sections.each(function(index, section) {
                            if (mainApp.checkInView($(event.target), $(section))) {
                                $dots.eq(index).addClass('active');
                            }
                        });
                    }, 100));
                });

                $(document).on('click', '.message-row .msg img', function() {
                    
                });

                if ($('body').hasClass('ipad') || $('body').hasClass('smallscreen')) {
                    setTimeout(function() {
                        $('.menu-li-qa').click();
                    }, 600);
                }
            },
            initialization: function() {
                mainApp.main_func();
            },
            resetAllDrawer: function() {
                $('body')
                    .removeClass('voice-input-opened')
                    .removeClass('mobile-menu-opened');
            },
            openMobileMenu: function() {
                mainApp.resetAllDrawer();
                $('body').addClass('mobile-menu-opened');
                $('.mobile-menu-overlay').addClass('active');
            },
            closeMobileMenu: function() {
                $('body')
                    .removeClass('voice-input-opened')
                    .removeClass('mobile-menu-opened');
                $('.mobile-menu-overlay').removeClass('active');
            },
            openVoiceInput: function() {
                mainApp.resetAllDrawer();
                $('.voice-input-txt').removeClass('voice-input-error').text(VOICE_INPUT_NORMAL_MSG);
                $('body').addClass('voice-input-opened');
                if ($('body').hasClass('mobile')) {
                    $('.mobile-menu-overlay').addClass('active');
                } else {
                    $('#voice-input-desktop-overlay').addClass('active');
                }
            },
            closeVoiceInput: function() {
                $('body').removeClass('voice-input-opened');
                if ($('body').hasClass('mobile')) {
                    $('.mobile-menu-overlay').removeClass('active');
                } else {
                    $('#voice-input-desktop-overlay').removeClass('active');
                }
            },
            startVoiceInput: function() {
                if (this.elapsedSeconds > 0) {
                    return;
                }
                if (this.recognizerRecording) {
                	return;
                }
                $('.voice-input-txt').removeClass('voice-input-error');
                $('.voice-input-txt').text('0"');
                var $this = this;
                this.voiceInputTimer = setInterval(function() {
                    mainApp.elapsedSeconds+=0.5;
                    $('.voice-input-txt').text(mainApp.elapsedSeconds + '"');
                }, 500);
                this.siriWave.start();
                this.recognizer = new webkitSpeechRecognition();
                this.recognizer.continuous = true;
          		this.recognizer.interimResults = true;
          		this.recognizer.lang = "zh-TW";
      	        this.recognizer.onresult = function(event) {
  		      	    if (event.results.length > 0) {
  		      	        var result = event.results[event.results.length-1];
  		      	        $('.voice-input-txt').text(result[0].transcript);
  		      	        //$('#CSChatMessage').val(result[0].transcript);
  		      	        
  		      	        if (result.isFinal) {
  	   	 		            $this.stopVoiceInput();
	   	 		            submitQuestion(result[0].transcript, 'mic');
  		      	        }
  		      	    }
  		      	};
  		    	this.recognizerRecording = true;
  		      	this.recognizer.start();
            },
            stopVoiceInput: function() {
                clearInterval(this.voiceInputTimer);

                mainApp.closeVoiceInput();
                this.siriWave.stop();
                this.resetWaveform();
  	      		
  	      		if (this.recognizer) {
  	      			this.recognizer.stop();
  	      		}
  	      		
                if (this.elapsedSeconds < MIN_VOICE_INPUT_SECONDS) {
                    $('.voice-input-txt').addClass('voice-input-error').text(VOICE_INPUT_ERROR_MSG);
                } else {
                    $('.voice-input-txt').removeClass('voice-input-error');
                    $('.voice-input-txt').text(VOICE_INPUT_NORMAL_MSG);
                }
                this.elapsedSeconds = 0;
  	      		this.recognizerRecording = false;
            },
            closeTutorial: function() {
                $('.overlay').removeClass('active');
                $('.tutorial-1').removeClass('active');
                $('.tutorial-2').removeClass('active');
            },
            checkInView: function(container, elem) {
            	//console.log('checkInView');
                //console.log(elem);
                var contWidth = container.width();
                var contLeft = container.scrollLeft();
                var contEnd = contLeft + contWidth;

                var elemLeft = elem.offset().left - container.offset().left;
                var elemEnd = elemLeft + elem.width();
                var isTotal = (elemLeft >= -1 && elemEnd <= contWidth);
                return isTotal;
            },
            scrollToNext: function(ctx) {
                var container = $(ctx).siblings('.swiper-scroll-container')[0];
                var target = $(container).find('.buttons-message-block:last')[0];
                if (!mainApp.checkInView($(container), $(target))) {
                    var current = container.scrollLeft;
                    $(container).animate({ scrollLeft: current + 246 }, 300);
                }
            },
            scrollToPrev: function(ctx) {
                var container = $(ctx).siblings('.swiper-scroll-container')[0];
                var target = $(container).find('.buttons-message-block:first')[0];
                if (!mainApp.checkInView($(container), $(target))) {
                    var current = container.scrollLeft;
                    $(container).animate({ scrollLeft: current - 246 }, 300);
                }
            },
            detectOS: function() {
                var OS = 'unknown';
                if (navigator.appVersion.indexOf('Win') != -1) {
                    OS = 'windows';
                }
                if (navigator.appVersion.indexOf('Mac') != -1) {
                    OS = 'macos';
                }
                $('body').addClass(OS);
                console.log(OS);
            },
            detectBrowser: function() {
                var browser = '';
                var ua = navigator.userAgent;
                if (ua.indexOf('MSIE ') != -1) {
                    browser = 'ie';
                }
                if (ua.indexOf('Trident/') != -1) {
                    browser = 'ie';
                }
                if (ua.indexOf('Edge') != -1) {
                    browser = 'ie';
                }
                if (ua.indexOf('iPad') != -1) {
                    browser = 'ipad';
                }
                $('body').addClass(browser);
            },
            detectSmallScreen: function() {
                if ($(window).height() < 650) {
                    $('body').addClass('smallscreen');
                }
            },
            detectMobile: function() {
                if ($(window).width() <= 800) {
                    $('body').addClass('mobile');
                }
            }
        }
        // Initializing ///
    $(document).ready(function() {
        mainApp.detectOS();
        mainApp.detectBrowser();
        mainApp.detectMobile();
        // mainApp.detectSmallScreen();
        mainApp.main_func();
    });

}(jQuery));