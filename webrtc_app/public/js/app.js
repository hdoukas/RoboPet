define(["backbone", "webrtc", "touchy"], function(Backbone, SimpleWebRTC, Touchy) {

    var App = function() {

        var me = this;

        this.debug = true;
        this.debugWebRTC = false;

        me.room = RoboPet.config.room;
        var uri = RoboPet.config.uri;

        this.mode = RoboPet.mode;
        this.isPet = (this.mode == 'pet');


        var dom = {
            touchable: 'touchable',
            localVideo: 'localVideo',
            remoteVideos: 'remoteVideos'
        };

        var webrtcOptions = {
            url: uri,
            debug: this.debugWebRTC,
            // the id/element dom element that will hold "our" video
            localVideoEl: dom.localVideo,
            // the id/element dom element that will hold remote videos
            remoteVideosEl: dom.remoteVideos,
            // immediately ask for camera access
            autoRequestMedia: true
        };

        this.webrtc = new SimpleWebRTC(webrtcOptions);

        // reuse webrtc socket.io instance
        this.ws = this.webrtc.connection;

        /**
         * Methods
         */
        this.initialize = function() {

            this.setup();
            this.initWebRTC();

            if(!this.isPet) {

                var sendMessage = function(ev, pos, type) {
                    me.ws.emit('touch', {ev: ev, pos: pos, type: type});
                };

                var robotUpdate = function(ev, pos) {
                    sendMessage(ev, pos, 'robot');
                };
                var laserUpdate = function(ev, pos) {
                    sendMessage(ev, pos, 'laser');
                };

                // handles robot movement
                this.initTouchable('touchable', null, {

//                    start: function(pos) {
//                        robotUpdate('start', pos);
//                    },

//                    move: function(pos) {
//                        robotUpdate('move', pos);
//                    },

                    end: function(pos) {
                        robotUpdate('end', pos);
                    }
                });

                // handles laser movement
                this.initTouchable('laser-control', null, {
                    start: function(pos) {
                        laserUpdate('start', pos);
                    },
                    move: function(pos) {
                        laserUpdate('move', pos);
                    },
                    end: function(pos) {
                        laserUpdate('end', pos);
                    }
                });

            }

        };

        this.setup = function() {

            var w = $(window);
            w.on("resize", function() {
                setTimeout(function() {
                    $('body, #remoteVideos, #touchable').css({
                        width: w.width(),
                        height: w.height()
                    });
                }, 100);
            });
            w.trigger("resize");
        };

        this.initWebRTC = function() {
            me.webrtc.on('error', function () {
                me.log(arguments);
            });

            me.webrtc.on('localStream', function() {
                me.isPet ? me.petMode() : me.ownerMode();
            });

        };

        this.shareScreen = function(cb) {
            me.webrtc.shareScreen(function (err) {

                me.log("Screen shared");
                err && me.log(err);

                cb && cb.apply(me, arguments);
            });

        };

        this.joinRoom = function(cb) {
            me.webrtc.joinRoom(me.room, function() {

                me.log("Room " + me.room + " joined!");

                cb && cb.apply(me, arguments);
            });
        };

        this.createRoom = function(cb) {
            me.webrtc.createRoom(me.room, function (err, name) {

                me.log("Create room " + name);
                err && me.log(err);

                cb && cb.call(me, err, name);
            });
        };

        this.petMode = function() {

            me.log("Pet mode");

            $('#laser-control, #localVideo').hide();

//            me.webrtc.on('readyToCall', function () {
                me.joinRoom(function() {
                    me.shareScreen(function() {
                        me.log("screen shared!!!");
                    });
                });
//            });

        };

        this.ownerMode = function() {
           me.log("Owner mode");

//            // when it's ready, join if we got a room from the URL
//            me.webrtc.on('readyToCall', function () {
//
//                me.log("ready to call");
                me.createRoom(function() {
//                    me.log("room created");
                    me.joinRoom(function() {

//                        me.log("room joined");

                        me.shareScreen(function() {
//                            me.log("screen shared!!!");
                        });

                    });
                });
//            });


        };

        this.log = function() {
            me.debug && console.log(arguments);
        };

        this.initTouchable = function(touchableId, container, callbacks) {

            callbacks = callbacks || {};

            var touchMe = document.getElementById(touchableId);

            container = container || touchMe;

            var screen, center;

            var getContainerSize = function(obj) {
                obj = obj || container;
                var w = $(obj);
                return { width: w.width(), height : w.height() };
            };

            var getCenter = function() {
                screen = getContainerSize();
                return { x: Math.round(screen.width/2), y: Math.round(screen.height/2) }
            };

            screen = getContainerSize();
            center = getCenter();

            $(window).on('resize', function() {
                screen = getContainerSize();
                center = getCenter();
            });

            Touchy(touchMe, true, {
                one: function(hand, finger) {

                    if (hand.fingers.length > 1) {
                        return;
                    }


                    var getPos = function(point) {
//
//                        console.log("**********************************");
//                        console.log(container);
//                        console.warn(JSON.stringify(point));

                        var move = {
                            up: 0, right: 0, down: 0, left: 0
                        };

                        var normalizePoint = function(point) {

                            var o = $(container).offset();

                            point.x = point.x - o.left;
                            point.y = point.y - o.top;

                            // if out of the box skip the point
                            if(point.x < 0 || point.y < 0) return null;

                            return point;
                        };

                        point = normalizePoint(point, container);

                        if(!point) {
                            return;
                        }

//                        console.warn(JSON.stringify(point));
//                        console.log("-----------------------------");

                        var speed = function(coord) {
                            var len = coord == 'x' ? screen.width : screen.height;
                            var s = (100 * (point[coord] - center[coord])) / (len - center[coord]);
                            s = s < 0 ? s*-1 : s;
                            s = s > 100 ? 100 : s;
                            return Math.round(s);
                        }

                        var speedVal = speed('x');
                        var isRight = (point.x > center.x);
//                        var isRight = (speedVal > 0);
                        move.right = isRight ? speedVal : 0;
                        move.left = !isRight ? speedVal : 0;

                        var speedVal = speed('y');
                        var isUp = (point.y < center.y);
//                        var isUp = (speedVal < 0);
                        move.up = isUp ? speedVal : 0;
                        move.down = !isUp ? speedVal : 0;

//                        console.log(JSON.stringify(move));

                        return move;
                    };

                    finger.on('start', function(point) {
                        callbacks.start && callbacks.start(getPos(point));
                    });

                    finger.on('move', function(point) {
                        callbacks.move && callbacks.move(getPos(point));
                    });

                    finger.on('end', function(point) {
                        callbacks.end && callbacks.end(getPos(point));
                    });

                }
//                ,two: function(hand, finger1, finger2) {
//
//                    console.log("two finger");
//                    console.log(hand, finger1, finger2);
//
//                    // Only run when exactly two fingers on screen
//                    hand.on('move', function(points) {
//                        // 'points' is an Array of point objects (same as finger.on point object)
//
//                        console.log("move");
//                        console.log(points);
//
//                    });
//
//                }

            });
        };

        this.initialize();

    };

    return App;
});




