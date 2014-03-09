var express = require('express');
var config = require('./config');
var routes = require('./routes');

var uuid = require('node-uuid');

var https = require('https');
var http = require('http');

var fs = require('fs');
var path = require('path');

var privateKey = fs.readFileSync('fakekeys/privatekey.pem').toString(),
    certificate = fs.readFileSync('fakekeys/certificate.pem').toString();


var app = express();

// all environments
app.set('port', process.env.PORT || config.serverport);

app.set('host', process.env.HOST || config.serverip);

app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');


app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

app.get('/', routes.index);
app.get('/pet', routes.pet);



var server = http.createServer(app).listen(app.get('port'));
//var server = https.createServer({key: privateKey, cert: certificate}, app).listen(app.get('port'));

var io = require('socket.io').listen(server);

function describeRoom(name) {
    var clients = io.sockets.clients(name);
    var result = {
        clients: {}
    };
    clients.forEach(function (client) {
        result.clients[client.id] = client.resources;
    });
    return result;
}

function safeCb(cb) {
    if (typeof cb === 'function') {
        return cb;
    } else {
        return function () {};
    }
}

io.sockets.on('connection', function (client) {
    client.resources = {
        screen: false,
        video: true,
        audio: true
    };

    // pass a message to another id
    client.on('message', function (details) {
        var otherClient = io.sockets.sockets[details.to];
        if (!otherClient) return;
        details.from = client.id;
        otherClient.emit('message', details);
    });

    client.on('shareScreen', function () {
        client.resources.screen = true;
    });

    client.on('unshareScreen', function (type) {
        client.resources.screen = false;
        if (client.room) removeFeed('screen');
    });

    client.on('join', join);

    client.on('touch', function(message) {

        var cmd = '';

        var laser   = '5';
        var right   = '6';
        var left    = '4';
        var forward = '2';
        var back    = '8';

        if(message.ev == 'end') {
            if(message.type == 'robot') {
                if(message.pos.up && message.pos.up > message.pos.left && message.pos.up > message.pos.right) {
                    cmd = forward;
                }
                if(message.pos.right && message.pos.right > message.pos.up) {
                    cmd = right;
                }
                if(message.pos.left && message.pos.left > message.pos.down) {
                    cmd = left;
                }
                if(message.pos.down && message.pos.down > message.pos.left && message.pos.down > message.pos.right) {
                    cmd = back;
                }
            }
            if(message.type == 'laser') {
                cmd = laser;
            }
        }

//        console.log(message);
//        console.log("cmd " + cmd);
        if(cmd)
            io.sockets.emit('robot', { command: cmd });
    })

    function removeFeed(type) {
        io.sockets.in(client.room).emit('remove', {
            id: client.id,
            type: type
        });
    }

    function join(name, cb) {
        // sanity check
        if (typeof name !== 'string') return;
        // leave any existing rooms
        if (client.room) removeFeed();
        safeCb(cb)(null, describeRoom(name))
        client.join(name);
        client.room = name;
    }

    // we don't want to pass "leave" directly because the
    // event type string of "socket end" gets passed too.
    client.on('disconnect', function () {
        removeFeed();
    });
    client.on('leave', removeFeed);

    client.on('create', function (name, cb) {
        if (arguments.length == 2) {
            cb = (typeof cb == 'function') ? cb : function () {};
            name = name || uuid();
        } else {
            cb = name;
            name = uuid();
        }
        // check if exists
        if (io.sockets.clients(name).length) {
            safeCb(cb)('taken');
        } else {
            join(name);
            safeCb(cb)(null, name);
        }
    });
});

if (config.uid) process.setuid(config.uid);

server.listen(app.get('port'), app.get('host'), function() {
  console.log('Express server listening on port ' + app.get('port'));
});

