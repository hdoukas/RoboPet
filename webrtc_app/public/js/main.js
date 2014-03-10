require.config({
    paths: {
        //tries to load jQuery from Google's CDN first and falls back
        //to load locally
        "jquery": ["//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min",
                    "libs/jquery/jquery"],
        "underscore": "/libs/underscore/underscore",
        "backbone": "/libs/backbone/backbone",
        "foundation": "/libs/foundation/js/foundation",
        "touchy": "/js/touchy",
        "socketio": "/socket.io/socket.io",
        "webrtc": "/js/simplewebrtc.bundle"
    },
    shim: {
        "backbone": {
            //loads dependencies first
            deps: ["jquery", "underscore"],
            //custom export name, this would be lowercase otherwise
            exports: "Backbone"
        },
        "socketio": {
            exports: "io"
        },
        "touchy": {
            deps: ['jquery'],
            exports: "Touchy"
        },
        "webrtc": {
            deps: ["socketio"],
            exports: "SimpleWebRTC"
        },
        "foundation": {
            deps: ["jquery"]
        },
        "app": {
            deps: ["backbone", "webrtc", "touchy"]
        }
    },
    //how long the it tries to load a script before giving up, the default is 7
    waitSeconds: 10
});

require(['backbone', 'app'], function(Backbone, App){

    jQuery.fn.foundation && (document).foundation();

    new App;
});
